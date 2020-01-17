package com.minelittlepony.hdskins.skins.valhalla;

import com.minelittlepony.hdskins.skins.api.SkinServer;
import com.minelittlepony.hdskins.skins.api.SkinServerFactory;

public class ValhallaSkinServerFactory implements SkinServerFactory {

    @Override
    public String getServerType() {
        return "valhalla";
    }

    @Override
    public Class<? extends SkinServer> getServerClass() {
        return ValhallaSkinServer.class;
    }
}
