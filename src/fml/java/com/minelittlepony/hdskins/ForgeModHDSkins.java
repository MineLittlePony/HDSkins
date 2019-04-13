package com.minelittlepony.hdskins;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("hdskins")
class ForgeModHDSkins {

    public ForgeModHDSkins() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void initOnClient(FMLClientSetupEvent event) {
        new ForgeModHDSkinsClient();
    }
}
