package com.voxelmodpack.hdskins;

import com.voxelmodpack.hdskins.server.SkinServer;

import java.util.List;

public class Config {

    public List<SkinServer> skinServers = SkinServer.defaultServers;

    public String lastChosenFile = "";
}
