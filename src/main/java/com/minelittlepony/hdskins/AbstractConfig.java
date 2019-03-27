package com.minelittlepony.hdskins;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.minelittlepony.hdskins.net.SkinServer;

public abstract class AbstractConfig {
    @Expose
    public List<SkinServer> skin_servers = SkinServer.defaultServers;

    @Expose
    public String lastChosenFile = "";
    
    public abstract void save();
}
