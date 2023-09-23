package com.minelittlepony.hdskins.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinUpload.Session;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UndashedUuid;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.Set;
import java.util.UUID;

@ServerType("valhalla")
public class ValhallaSkinServer implements SkinServer {

    private static final String API_PREFIX = "/api/v1";

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.DOWNLOAD_USER_SKIN,
            Feature.UPLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES
    );

    private final String address;

    private transient String accessToken;

    public ValhallaSkinServer(String address) {
        this.address = address;
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public boolean supportsSkinType(SkinType skinType) {
        return skinType.isKnown() && skinType != SkinType.CAPE;
    }

    @Override
    public boolean ownsUrl(String url) {
        try {
            url = new URI(url).getHost();
        } catch (final URISyntaxException ignored) { }

        return address.contentEquals(url);
    }

    private String getApiPrefix() {
        return address + API_PREFIX;
    }

    @Override
    public TexturePayload loadSkins(GameProfile profile) throws IOException, AuthenticationException {
        Preconditions.checkNotNull(profile.getId(), "profile id required for skins");
        return MoreHttpResponses.execute(HttpRequest.newBuilder(URI.create(String.format("%s/user/%s", getApiPrefix(), UndashedUuid.toString(profile.getId()))))
                    .GET()
                    .build())
                .requireOk()
                .json(TexturePayload.class, "Invalid texture payload");
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException {
        try {
            uploadPlayerSkin(upload);
        } catch (HttpException e) {
            if (e.getStatusCode() != 401) {
                throw e;
            }

            accessToken = null;
            uploadPlayerSkin(upload);
        } catch (IOException e) {
            if (e.getMessage().equals("Authorization failed")) {
                accessToken = null;
                uploadPlayerSkin(upload);
            }

            throw e;
        }
    }

    private void uploadPlayerSkin(SkinUpload upload) throws IOException, AuthenticationException {
        authorize(upload.session());

        if (upload instanceof SkinUpload.Delete) {
            MoreHttpResponses.execute(HttpRequest.newBuilder(buildUserTextureUri(upload))
                            .DELETE()
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
        } else if (upload instanceof SkinUpload.FileUpload fileUpload) {
            MoreHttpResponses.execute(HttpRequest.newBuilder(buildUserTextureUri(upload))
                            .PUT(FileTypes.multiPart(fileUpload.metadata())
                                    .field("file", fileUpload.file())
                                    .build())
                            .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                            .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
        } else if (upload instanceof SkinUpload.UriUpload uriUpload) {
            MoreHttpResponses.execute(HttpRequest.newBuilder(buildUserTextureUri(upload))
                            .POST(FileTypes.multiPart(uriUpload.metadata())
                                    .field("file", uriUpload.uri().toString())
                                    .build())
                            .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                            .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
        } else {
            throw new IllegalArgumentException("Unsupported Upload type: " + upload.getClass().getName());
        }
    }

    private URI buildUserTextureUri(SkinUpload upload) {
        return URI.create(String.format("%s/user/%s/%s", getApiPrefix(),
                UndashedUuid.toString(upload.session().profile().getId()),
                upload.type().getParameterizedName()
        ));
    }

    @Override
    public void authorize(Session session) throws IOException, AuthenticationException {
        if (accessToken != null) {
            return;
        }
        GameProfile profile = session.profile();
        AuthHandshake handshake = authHandshake(profile.getName());

        if (handshake.offline) {
            return;
        }

        session.validate(handshake.serverId);

        AuthResponse response = authResponse(profile.getName(), handshake.verifyToken);
        if (!response.userId.equals(profile.getId())) {
            throw new IOException("UUID mismatch!"); // probably won't ever throw
        }
        accessToken = response.accessToken;
    }

    private AuthHandshake authHandshake(String name) throws IOException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(URI.create(String.format("%s/auth/handshake", getApiPrefix())))
                .POST(FileTypes.multiPart()
                        .field("name", name)
                        .build())
                .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                .build())
                .requireOk()
                .json(AuthHandshake.class, "Invalid handshake response");
    }

    private AuthResponse authResponse(String name, long verifyToken) throws IOException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(URI.create(String.format("%s/auth/response", getApiPrefix())))
                .POST(FileTypes.multiPart()
                        .field("name", name)
                        .field("verifyToken", verifyToken)
                        .build())
                .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                .build())
                .requireOk()
                .json(AuthResponse.class, "Invalid auth response");
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .toString();
    }

    private static class AuthHandshake {
        private boolean offline;
        private String serverId;
        private long verifyToken;
    }

    private static class AuthResponse {
        private String accessToken;
        private UUID userId;
    }
}
