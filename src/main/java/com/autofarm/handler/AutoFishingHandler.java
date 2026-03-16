package com.autofarm.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.event.TickEvent;

import java.lang.reflect.Field;

/**
 * Automatic fishing: casts the rod, detects bites via the server-synced
 * {@code FishingHook.biting} field, reels in, and re-casts automatically.
 *
 * Enabled by default. Toggle with {@code !fish} in chat (never sent to server).
 */
public final class AutoFishingHandler {

    // ── Reflection ────────────────────────────────────────────────────────────

    private static Field bitingField;
    private static boolean reflectionFailed = false;

    private static Field getBitingField() {
        if (bitingField != null || reflectionFailed) return bitingField;
        try {
            Field f = FishingHook.class.getDeclaredField("biting");
            f.setAccessible(true);
            bitingField = f;
        } catch (NoSuchFieldException e) {
            reflectionFailed = true;
        }
        return bitingField;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Auto-fishing is on by default — no chat command needed to start. */
    private static boolean enabled = true;

    /** Ticks to wait before the next action (reel-in or re-cast). */
    private static int cooldown = 0;

    // ── Command handler (called from AutoFarmMod) ─────────────────────────────

    public static void handleCommand(String msg) {
        boolean turnOn;
        if (msg.equals("!fish on")) {
            turnOn = true;
        } else if (msg.equals("!fish off")) {
            turnOn = false;
        } else {
            turnOn = !enabled; // bare "!fish" → toggle
        }
        setEnabled(turnOn);
    }

    // ── Tick handler (registered in AutoFarmMod.onClientSetup) ───────────────

    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        LocalPlayer player = mc.player;

        // Must be holding a fishing rod in the main hand
        if (!(player.getMainHandItem().getItem() instanceof FishingRodItem)) return;

        FishingHook hook = player.fishing;

        // No active hook — cast the rod
        if (hook == null) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            cooldown = 20; // 1 s before we start monitoring the new hook
            return;
        }

        // Hook is out — check for a fish bite
        Field f = getBitingField();
        if (f == null) return; // reflection unavailable

        boolean biting;
        try {
            biting = f.getBoolean(hook);
        } catch (IllegalAccessException e) {
            return;
        }

        if (biting) {
            // Reel in the catch
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            // After cooldown the hook will be null → auto-cast fires automatically
            cooldown = 60; // 3 s: time for item to land before re-casting
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setEnabled(boolean value) {
        enabled = value;
        cooldown = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal("§6[AutoFarm]§r Auto-fishing " + (value ? "§aENABLED" : "§cDISABLED")),
                    false
            );
        }
    }

    public static boolean isEnabled() { return enabled; }
}
