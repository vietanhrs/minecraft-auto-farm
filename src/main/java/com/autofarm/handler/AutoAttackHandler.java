package com.autofarm.handler;

import com.autofarm.config.AutoFarmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Weapon;
import net.minecraftforge.event.TickEvent;

/**
 * Handles automatic sword combat with food management.
 *
 * Chat commands (intercepted by {@link com.autofarm.AutoFarmMod}, never sent to server):
 *   {@code !attack}      — toggle on/off
 *   {@code !attack on}   — enable
 *   {@code !attack off}  — disable
 *
 * Behaviour each tick while enabled:
 *   1. If food ≤ critical level: find food in hotbar, eat it, repeat until full.
 *   2. Otherwise: ensure a sword is selected and attack every N seconds.
 */
public final class AutoAttackHandler {

    // ── State ─────────────────────────────────────────────────────────────────

    private static boolean enabled = false;

    /** Ticks remaining before the next attack swing. */
    private static int attackCooldownTicks = 0;

    // Eating sub-state
    private static boolean isEating = false;
    /** Hotbar slot we were holding before food was needed. */
    private static int preFoodSlot = -1;
    /** Ticks remaining before we consider the current food item consumed. */
    private static int eatTimeoutTicks = 0;
    /** True once we have called useItem to start eating the current food item. */
    private static boolean eatStarted = false;

    // ── Command handler (called from AutoFarmMod) ─────────────────────────────

    public static void handleCommand(String msg) {
        boolean turnOn;
        if (msg.equals("!attack on")) {
            turnOn = true;
        } else if (msg.equals("!attack off")) {
            turnOn = false;
        } else {
            // bare "!attack" or any other !attack... variant → toggle
            turnOn = !enabled;
        }
        setEnabled(turnOn);
    }

    // ── Tick handler (registered in AutoFarmMod.onClientSetup) ───────────────

    /**
     * Called every game tick (after the tick runs) via
     * {@link TickEvent.ClientTickEvent.Post#BUS}.
     */
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null) return;

        LocalPlayer player = mc.player;

        if (isEating) {
            tickEating(mc, player);
        } else {
            tickAttack(mc, player);
        }
    }

    // ── Eating sub-state ──────────────────────────────────────────────────────

    private static void tickEating(Minecraft mc, LocalPlayer player) {
        int foodLevel = player.getFoodData().getFoodLevel();

        if (eatTimeoutTicks <= 0 || foodLevel >= 20) {
            finishEating(player);
            return;
        }

        eatTimeoutTicks--;

        // Trigger the first right-click to start eating
        if (!eatStarted) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            eatStarted = true;
            return;
        }

        // Once the item is consumed (isUsingItem flips to false), check if we need more
        if (!player.isUsingItem() && eatTimeoutTicks < 45) {
            if (foodLevel < 20) {
                int next = findFoodSlot(player);
                if (next >= 0) {
                    player.getInventory().setSelectedSlot(next);
                    mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                    eatStarted = true;
                    eatTimeoutTicks = 50;
                } else {
                    finishEating(player); // no food left
                }
            } else {
                finishEating(player);
            }
        }
    }

    private static void finishEating(LocalPlayer player) {
        isEating = false;
        eatStarted = false;
        eatTimeoutTicks = 0;

        // Return to the sword slot we left
        if (preFoodSlot >= 0 && preFoodSlot < 9) {
            player.getInventory().setSelectedSlot(preFoodSlot);
            preFoodSlot = -1;
        }
        attackCooldownTicks = 10; // brief pause before attacking again
    }

    // ── Attack sub-state ──────────────────────────────────────────────────────

    private static void tickAttack(Minecraft mc, LocalPlayer player) {
        int foodLevel = player.getFoodData().getFoodLevel();
        int critical = AutoFarmConfig.CRITICAL_FOOD_LEVEL.get();

        // ── Food check ────────────────────────────────────────────────────────
        if (foodLevel <= critical) {
            int foodSlot = findFoodSlot(player);
            if (foodSlot >= 0) {
                preFoodSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(foodSlot);
                isEating = true;
                eatStarted = false;
                eatTimeoutTicks = 50; // generous budget (≥ 32 ticks per item)
                notify(player, "§eFood low (" + foodLevel + "). Eating...");
                return;
            }
            // No food in hotbar — fall through and keep attacking
        }

        // ── Make sure a sword is selected ─────────────────────────────────────
        Inventory inv = player.getInventory();
        int swordSlot = findSwordSlot(player);
        if (swordSlot >= 0 && inv.getSelectedSlot() != swordSlot) {
            inv.setSelectedSlot(swordSlot);
        }

        // ── Attack cooldown ───────────────────────────────────────────────────
        if (attackCooldownTicks > 0) {
            attackCooldownTicks--;
            return;
        }

        // ── Perform attack ────────────────────────────────────────────────────
        attackCooldownTicks = AutoFarmConfig.ATTACK_INTERVAL_SECONDS.get() * 20;

        if (mc.crosshairPickEntity != null) {
            mc.gameMode.attack(player, mc.crosshairPickEntity);
        }
        // Always play the swing animation so the player can see it's working
        player.swing(InteractionHand.MAIN_HAND);
    }

    // ── Inventory helpers ─────────────────────────────────────────────────────

    /**
     * Returns the hotbar slot (0–8) of the first sword-like item, or -1.
     *
     * <p>In 1.21.x, swords no longer have a dedicated {@code SwordItem} class.
     * They are identified by the {@link DataComponents#WEAPON} data component with
     * {@code disableBlockingForSeconds == 0} (axes have {@code 5.0f}, spears have
     * the {@link DataComponents#KINETIC_WEAPON} component instead).</p>
     */
    private static int findSwordSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            Weapon weapon = stack.get(DataComponents.WEAPON);
            if (weapon == null) continue;

            // Exclude axes (disable blocking > 0) and spears/maces (kinetic weapon)
            if (weapon.disableBlockingForSeconds() != 0.0f) continue;
            if (stack.get(DataComponents.KINETIC_WEAPON) != null) continue;
            if (stack.get(DataComponents.PIERCING_WEAPON) != null) continue;

            return i; // found a sword
        }
        return -1;
    }

    /**
     * Returns the hotbar slot (0–8) of the first edible item that can restore
     * food, or -1 if none is available.
     *
     * <p>Uses the {@link DataComponents#FOOD} data component (introduced in MC 1.20.5).</p>
     */
    private static int findFoodSlot(LocalPlayer player) {
        int foodLevel = player.getFoodData().getFoodLevel();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            FoodProperties fp = stack.get(DataComponents.FOOD);
            if (fp == null || fp.nutrition() <= 0) continue;

            // canAlwaysEat items (like golden apples) can be eaten even when full
            if (foodLevel >= 20 && !fp.canAlwaysEat()) continue;

            return i;
        }
        return -1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            isEating = false;
            eatStarted = false;
            eatTimeoutTicks = 0;
            attackCooldownTicks = 0;
            preFoodSlot = -1;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            notify(mc.player, "Auto-attack " + (value ? "§aENABLED" : "§cDISABLED"));
        }
    }

    private static void notify(LocalPlayer player, String text) {
        // displayClientMessage(component, hotbar): false = chat window, true = action bar
        player.displayClientMessage(Component.literal("§6[AutoFarm]§r " + text), false);
    }

    public static boolean isEnabled() { return enabled; }
}
