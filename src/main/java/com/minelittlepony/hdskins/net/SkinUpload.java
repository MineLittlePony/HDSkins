package com.minelittlepony.hdskins.net;

import com.google.common.base.MoreObjects;
import com.minelittlepony.hdskins.profile.SkinType;
import net.minecraft.client.util.Session;

import java.net.URI;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

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

    public static class Response {

        private final String message;

        public Response(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("message", message)
                    .toString();
        }
    }
}
