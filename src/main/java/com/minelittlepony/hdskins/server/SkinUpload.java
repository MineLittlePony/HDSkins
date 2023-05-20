package com.minelittlepony.hdskins.server;

import net.minecraft.client.util.Session;

import com.minelittlepony.hdskins.profile.SkinType;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public sealed interface SkinUpload permits SkinUpload.FileUpload, SkinUpload.UriUpload, SkinUpload.Delete {

    Session session();

    SkinType type();

    record FileUpload(Session session, SkinType type, Path file, Map<String, String> metadata)
            implements SkinUpload {
    }

    record UriUpload(Session session, SkinType type, URI uri, Map<String, String> metadata)
            implements SkinUpload {
    }

    record Delete(Session session, SkinType type)
            implements SkinUpload {
    }
}
