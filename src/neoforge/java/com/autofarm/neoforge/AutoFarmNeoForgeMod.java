package com.autofarm.neoforge;

import com.autofarm.config.AutoFarmConfig;
import com.autofarm.handler.AutoAttackHandler;
import com.autofarm.handler.AutoFishingHandler;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.event.TickEvent;

import java.util.function.Predicate;

@Mod(AutoFarmNeoForgeMod.MOD_ID)
public class AutoFarmNeoForgeMod {

    public static final String MOD_ID = "autofarm";

    public AutoFarmNeoForgeMod(FMLJavaModLoadingContext context) {
        AutoFarmConfig.configure(
                NeoForgeAutoFarmConfig.ATTACK_INTERVAL_SECONDS::get,
                NeoForgeAutoFarmConfig.CRITICAL_FOOD_LEVEL::get
        );

        context.registerConfig(ModConfig.Type.CLIENT, NeoForgeAutoFarmConfig.SPEC);
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);

        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(AutoFarmNeoForgeMod::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event$ -> AutoAttackHandler.onClientTick());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event$ -> AutoFishingHandler.onClientTick());
        ClientChatEvent.BUS.addListener((Predicate<ClientChatEvent>) AutoFarmNeoForgeMod::onClientChat);
    }

    private static boolean onClientChat(ClientChatEvent event) {
        String msg = event.getMessage().trim().toLowerCase();

        if (msg.startsWith("!attack")) {
            AutoAttackHandler.handleCommand(msg);
            return true;
        }
        if (msg.startsWith("!fish")) {
            AutoFishingHandler.handleCommand(msg);
            return true;
        }

        return false;
    }
}
