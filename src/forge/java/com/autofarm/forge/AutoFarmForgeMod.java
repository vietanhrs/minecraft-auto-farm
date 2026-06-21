package com.autofarm.forge;

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

@Mod(AutoFarmForgeMod.MOD_ID)
public class AutoFarmForgeMod {

    public static final String MOD_ID = "autofarm";

    public AutoFarmForgeMod(FMLJavaModLoadingContext context) {
        AutoFarmConfig.configure(
                ForgeAutoFarmConfig.ATTACK_INTERVAL_SECONDS::get,
                ForgeAutoFarmConfig.CRITICAL_FOOD_LEVEL::get
        );

        context.registerConfig(ModConfig.Type.CLIENT, ForgeAutoFarmConfig.SPEC);
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);

        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(AutoFarmForgeMod::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event$ -> AutoAttackHandler.onClientTick());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event$ -> AutoFishingHandler.onClientTick());
        ClientChatEvent.BUS.addListener((Predicate<ClientChatEvent>) AutoFarmForgeMod::onClientChat);
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
