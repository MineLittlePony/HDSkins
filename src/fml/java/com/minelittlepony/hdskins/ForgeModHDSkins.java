package com.minelittlepony.hdskins;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("hdskins")
public class ForgeModHDSkins {
    @SubscribeEvent
    public void initOnClient(FMLClientSetupEvent event) {
        FMLJavaModLoadingContext.get().getModEventBus().register(new ForgeModHDSkinsClient());
    }
}
