package com.minelittlepony.hdskins.skins;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.minelittlepony.hdskins.skins.api.SkinServer;
import com.minelittlepony.hdskins.skins.api.SkinServerFactory;

import java.lang.reflect.Type;
import java.util.ServiceLoader;

public class SkinServerSerializer implements JsonSerializer<SkinServer>, JsonDeserializer<SkinServer> {

    public static final SkinServerSerializer instance = new SkinServerSerializer();

    private final BiMap<String, Class<? extends SkinServer>> types = HashBiMap.create(2);

    private SkinServerSerializer() {
        for (SkinServerFactory f : ServiceLoader.load(SkinServerFactory.class)) {
            types.put(f.getServerType(), f.getServerClass());
        }
    }

    @Override
    public JsonElement serialize(SkinServer src, Type typeOfSrc, JsonSerializationContext context) {
        String name = types.inverse().get(src.getClass());
        JsonObject obj = context.serialize(src).getAsJsonObject();
        obj.addProperty("type", name);

        return obj;
    }

    @Override
    public SkinServer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String type = json.getAsJsonObject().get("type").getAsString();
        return context.deserialize(json, types.get(type));
    }
}
