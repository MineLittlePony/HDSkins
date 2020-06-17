package com.minelittlepony.hdskins.skins;

import com.minelittlepony.hdskins.skins.types.ValhallaSkinServerType;
import com.minelittlepony.hdskins.skins.types.YggdrasilSkinServerType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SkinServerTypes {

    public static final SkinServerTypes instance = new SkinServerTypes();

    private final Map<String, SkinServerType> names = new HashMap<>();
    private final Map<Type, SkinServerType> types = new HashMap<>();

    private SkinServerTypes() {
        // register default skin server types
        registerServerType(new ValhallaSkinServerType());
        registerServerType(new YggdrasilSkinServerType());
    }

    private void registerServerType(SkinServerType type) {
        names.put(type.getName(), type);
        types.put(type.getType(), type);
    }

    public Type getType(String name) {
        SkinServerType factory = names.get(name);
        if (factory == null) {
            return null;
        }
        return factory.getType();
    }

    public String getName(SkinServer server) {
        SkinServerType factory = types.get(server.getClass());
        if (factory == null) {
            return null;
        }
        return factory.getName();
    }

}