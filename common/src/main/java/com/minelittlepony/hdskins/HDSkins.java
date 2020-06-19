package com.minelittlepony.hdskins;

import com.minelittlepony.hdskins.client.dummy.EquipmentList;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
import com.minelittlepony.hdskins.config.Config;
import com.minelittlepony.hdskins.config.SkinConfig;
import com.minelittlepony.hdskins.skins.SkinCache;
import com.minelittlepony.hdskins.skins.SkinServerList;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class HDSkins {
    public static final String MOD_ID = "hdskins";

    public static final Logger logger = LogManager.getLogger();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    protected SkinCache skinCache;
    protected final EquipmentList equipmentList = new EquipmentList();
    protected final SkinResourceManager resources = new SkinResourceManager();

    public HDSkins() {
        instance = this;
    }

    protected void init() {
        Config.FILE.load();
        SkinConfig.FILE.load();
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public void resetCache(MinecraftSessionService sessionService) {
        skinCache = new SkinCache(new SkinServerList(SkinConfig.FILE.get().servers), sessionService);
    }

    public SkinCache getSkinCache() {
        return skinCache;
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }
}
