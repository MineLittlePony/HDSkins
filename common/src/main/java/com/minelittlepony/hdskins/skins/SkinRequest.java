package com.minelittlepony.hdskins.skins;

import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.util.Session;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.util.Map;

@Immutable
public abstract class SkinRequest {

    private final Session session;
    private final Type type;

    public SkinRequest(Session session, Type type) {
        this.session = session;
        this.type = type;
    }

    public Session getSession() {
        return session;
    }

    public Type getType() {
        return type;
    }

    @Immutable
    public static class Upload extends SkinRequest {
        private final URI image;
        private final Map<String, String> metadata;

        public Upload(Session session, Type type, URI image, Map<String, String> metadata) {
            super(session, type);
            this.image = image;
            this.metadata = metadata;
        }

        public URI getImage() {
            return image;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    @Immutable
    public static class Delete extends SkinRequest {
        public Delete(Session session, Type type) {
            super(session, type);
        }
    }
}
