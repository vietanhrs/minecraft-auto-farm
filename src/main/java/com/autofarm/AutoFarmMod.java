package com.autofarm;

import com.autofarm.config.AutoFarmConfig;
import com.autofarm.handler.AutoAttackHandler;
import com.autofarm.handler.AutoFishingHandler;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Predicate;

@Mod(AutoFarmMod.MOD_ID)
public class AutoFarmMod {

    public static final String MOD_ID = "autofarm";

    public AutoFarmMod(FMLJavaModLoadingContext context) {
        // Register client config (generates autofarm-client.toml in the config folder)
        context.registerConfig(ModConfig.Type.CLIENT, AutoFarmConfig.SPEC);

        // Mark as client-only so servers do not block players who have this mod installed
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);

        // Register game event listeners during FMLClientSetupEvent (client side only)
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(AutoFarmMod::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // ── Tick listeners ─────────────────────────────────────────────────────
        // ClientTickEvent is now a sealed record hierarchy; subscribe to Post (after game tick)
        TickEvent.ClientTickEvent.Post.BUS.addListener(AutoAttackHandler::onClientTick);
        TickEvent.ClientTickEvent.Post.BUS.addListener(AutoFishingHandler::onClientTick);

        // ── Chat command listener ───────────────────────────────────────────────
        // ClientChatEvent uses a CancellableEventBus — register a Predicate so we can
        // return true (cancel, do not send to server) for our commands and false otherwise.
        ClientChatEvent.BUS.addListener((Predicate<ClientChatEvent>) AutoFarmMod::onClientChat);
    }

    /**
     * Intercepts chat messages that start with {@code !attack} or {@code !fish}.
     * Returns {@code true} to cancel (not sent to server), {@code false} to pass through.
     */
    private static boolean onClientChat(ClientChatEvent event) {
        String msg = event.getMessage().trim().toLowerCase();

        if (msg.startsWith("!attack")) {
            AutoAttackHandler.handleCommand(msg);
            return true; // cancel — do not send to server
        }
        if (msg.startsWith("!fish")) {
            AutoFishingHandler.handleCommand(msg);
            return true; // cancel
        }

        return false; // unrelated message — let it through
    }
}
