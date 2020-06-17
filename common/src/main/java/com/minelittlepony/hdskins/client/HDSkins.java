package com.minelittlepony.hdskins.client;

import com.minelittlepony.hdskins.client.dummy.EquipmentList;
import com.minelittlepony.hdskins.client.profile.ProfileRepository;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
import com.minelittlepony.hdskins.config.Config;
import com.minelittlepony.hdskins.config.SkinConfig;
import com.minelittlepony.hdskins.skins.SkinServerList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class HDSkins {
    public static final String MOD_ID = "hdskins";

    public static final Logger logger = LogManager.getLogger();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    protected SkinServerList skinServerList;
    protected final EquipmentList equipmentList = new EquipmentList();
    protected final SkinResourceManager resources = new SkinResourceManager();
    protected final ProfileRepository repository = new ProfileRepository(this);

    public HDSkins() {
        instance = this;
    }

    protected void init() {
        Config.FILE.load();
        SkinConfig.FILE.load();

        skinServerList = new SkinServerList(SkinConfig.FILE.get().servers);
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public ProfileRepository getProfileRepository() {
        return repository;
    }

    public SkinServerList getSkinServerList() {
        return skinServerList;
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }
}
