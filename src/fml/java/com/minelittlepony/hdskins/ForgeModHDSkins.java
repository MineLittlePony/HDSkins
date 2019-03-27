package com.minelittlepony.hdskins;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("hdskins")
public class ForgeModHDSkins {

    public ForgeModHDSkins() {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> this::initOnClient);
    }

    void initOnClient() {
        FMLJavaModLoadingContext.get().getModEventBus().register(new ForgeModHDSkinsClient());
    }
}
