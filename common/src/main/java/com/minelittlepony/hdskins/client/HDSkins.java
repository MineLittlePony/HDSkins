package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.dummy.EquipmentList;
import com.minelittlepony.hdskins.client.profile.ProfileRepository;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
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

    protected final HDConfig config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));

    protected final SkinServerList skinServerList = new SkinServerList();
    protected final EquipmentList equipmentList = new EquipmentList();
    protected final SkinResourceManager resources = new SkinResourceManager();
    protected final ProfileRepository repository = new ProfileRepository(this);

    public HDSkins() {
        instance = this;
    }

    public HDConfig getConfig() {
        return config;
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
