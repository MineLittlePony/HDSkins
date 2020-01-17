package com.minelittlepony.hdskins.skins;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.Map;

@Immutable
public class SkinUpload {

    private final GameSession session;
    private final URI image;
    private final Map<String, String> metadata;
    private final SkinType type;

    public SkinUpload(GameSession session, SkinType type, @Nullable URI image, Map<String, String> metadata) {
        this.session = session;
        this.image = image;
        this.metadata = metadata;
        this.type = type;
    }

    public GameSession getSession() {
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
