package com.minelittlepony.hdskins.skins;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class SkinServerSerializer implements JsonSerializer<SkinServer>, JsonDeserializer<SkinServer> {

    @Override
    public JsonElement serialize(SkinServer src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = context.serialize(src).getAsJsonObject();
        String name = SkinServerTypes.instance.getName(src);
        obj.addProperty("type", name);
        return obj;
    }

    @Override
    public SkinServer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String name = obj.get("type").getAsString();
        obj.remove("type");

        Type type = SkinServerTypes.instance.getType(name);
        return context.deserialize(obj, type);
    }
}
