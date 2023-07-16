package com.minelittlepony.hdskins.server;

import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public sealed interface SkinUpload permits SkinUpload.FileUpload, SkinUpload.UriUpload, SkinUpload.Delete {

    Session session();

    SkinType type();

    record Session (GameProfile profile, String accessToken, ValidateFunction validateFunction) {
        public void validate(String serverId) throws AuthenticationException {
            validateFunction.accept(this, serverId);
        }

        public interface ValidateFunction {
            void accept(Session session, String serverId) throws AuthenticationException;
        }
    }

    record FileUpload(Session session, SkinType type, Path file, Map<String, String> metadata)
            implements SkinUpload {
    }

    record UriUpload(Session session, SkinType type, URI uri, Map<String, String> metadata)
            implements SkinUpload {
    }

    record Delete(Session session, SkinType type)
            implements SkinUpload {
    }

    static SkinUpload delete(SkinType type, Session session) {
        return create(null, type, Map.of(), session);
    }

    static SkinUpload create(@Nullable URI skin, SkinType type, Map<String, String> metadata, Session session) {
        if (skin == null) {
            return new Delete(session, type);
        }
        if ("file".equals(skin.getScheme())) {
            return new FileUpload(session, type, Paths.get(skin), metadata);
        }
        if (Set.of("http", "https").contains(skin.getScheme())) {
            return new UriUpload(session, type, skin, metadata);
        }
        throw new IllegalArgumentException("URI scheme not supported for skin upload: " + skin.getScheme());
    }
}
