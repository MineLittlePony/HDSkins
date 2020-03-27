package com.minelittlepony.hdskins.server;

import net.minecraft.client.util.Session;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.minelittlepony.hdskins.profile.SkinType;

import java.net.URI;
import java.util.Map;

@Immutable
public class SkinUpload {

    private final Session session;
    private final URI image;
    private final Map<String, String> metadata;
    private final SkinType type;

    public SkinUpload(Session session, SkinType type, @Nullable URI image, Map<String, String> metadata) {
        this.session = session;
        this.image = image;
        this.metadata = metadata;
        this.type = type;
    }

    public Session getSession() {
        return session;
    }

    @Nullable
    public URI getImage() {
        return image;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public SkinType getType() {
        return type;
    }

    public String getSchemaAction() {
        return image == null ? "none" : image.getScheme();
    }

}
