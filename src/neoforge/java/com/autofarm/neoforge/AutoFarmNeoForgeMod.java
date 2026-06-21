package com.autofarm.neoforge;

import com.autofarm.config.AutoFarmConfig;
import com.autofarm.handler.AutoAttackHandler;
import com.autofarm.handler.AutoFishingHandler;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AutoFarmNeoForgeMod.MOD_ID)
public class AutoFarmNeoForgeMod {

    public static final String MOD_ID = "autofarm";

    public AutoFarmNeoForgeMod(ModContainer modContainer) {
        AutoFarmConfig.configure(
                NeoForgeAutoFarmConfig.ATTACK_INTERVAL_SECONDS::get,
                NeoForgeAutoFarmConfig.CRITICAL_FOOD_LEVEL::get
        );

        modContainer.registerConfig(ModConfig.Type.CLIENT, NeoForgeAutoFarmConfig.SPEC);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AutoAttackHandler.onClientTick());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AutoFishingHandler.onClientTick());
        NeoForge.EVENT_BUS.addListener(AutoFarmNeoForgeMod::onClientChat);
    }

    private static void onClientChat(ClientChatEvent event) {
        String msg = event.getMessage().trim().toLowerCase();

        if (msg.startsWith("!attack")) {
            AutoAttackHandler.handleCommand(msg);
            event.setCanceled(true);
            return;
        }
        if (msg.startsWith("!fish")) {
            AutoFishingHandler.handleCommand(msg);
            event.setCanceled(true);
        }
    }
}
