package com.minelittlepony.hdskins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minelittlepony.hdskins.skins.SkinServerList;
import net.fabricmc.api.ModInitializer;

public class HDSkins implements ModInitializer {

    public static final String MOD_ID = "hdskins";

    public static final Logger logger = LogManager.getLogger();

    private final SkinServerList skinServerList = new SkinServerList();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    public HDSkins() {
        instance = this;
    }

    @Override
    public void onInitialize() {
        skinServerList.load();
    }

    public SkinServerList getSkinServerList() {
        return skinServerList;
    }

}
