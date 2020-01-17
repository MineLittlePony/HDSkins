package com.minelittlepony.hdskins.skins.api;

import com.minelittlepony.hdskins.skins.SkinServerList;

/**
 * Factory service file for creating entries for {@link SkinServer}. Loaded from {@link SkinServerList}.
 */
public interface SkinServerFactory {

    /**
     * The type of skin server. This value will be read from the "type" json property.
     */
    String getServerType();

    /**
     * The actual class that should be created. The type should be able to be serialized using gson.
     */
    Class<? extends SkinServer> getServerClass();

}
