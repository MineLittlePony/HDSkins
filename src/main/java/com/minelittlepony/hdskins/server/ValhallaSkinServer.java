package com.minelittlepony.hdskins.server;

import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.nio.file.Path;
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
    public boolean ownsUrl(String url) {
        try {
            url = new URI(url).getHost();
        } catch (final URISyntaxException ignored) {
        }

        return address.contentEquals(url);
    }

    private String getApiPrefix() {
        return address + API_PREFIX;
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(getTexturesURI(profile))
                .GET()
                .build();
        try (MoreHttpResponses response = MoreHttpResponses.execute(HTTP_CLIENT, req)) {
            return response
                    .requireOK()
                    .json(TexturePayload.class);
        }
    }

    @Override
    public void performSkinUpload(SkinUpload upload) throws IOException, InterruptedException, AuthenticationException {
        try {
            uploadPlayerSkin(upload);
        } catch (IOException e) {
            if (e.getMessage().contains("Authorization failed")) {
                accessToken = null;
                uploadPlayerSkin(upload);
            }

            throw e;
        }
    }

    private void uploadPlayerSkin(SkinUpload upload) throws IOException, InterruptedException, AuthenticationException {
        authorize(upload.session());

        var req = HttpRequest.newBuilder(buildUserTextureUri(upload));
        req.header("Authorization", this.accessToken);

        switch (upload.getSchemaAction()) {
            case "none" -> req.DELETE();
            case "file" -> {
                var multipartBody = MultipartBodyPublisher.newBuilder()
                        .filePart("file", Path.of(upload.image()), MediaType.IMAGE_PNG);
                upload.metadata().forEach(multipartBody::textPart);
                req.PUT(multipartBody.build());
            }
            case "http", "https" -> {
                var formBody = FormBodyPublisher.newBuilder()
                        .query("file", upload.image().toString());
                upload.metadata().forEach(formBody::query);
                req.POST(formBody.build());
            }
            default -> throw new IOException("Unsupported URI scheme: " + upload.getSchemaAction());
        }

        try (var response = MoreHttpResponses.execute(HTTP_CLIENT, req.build())) {
            response
                    .requireOK(this::getErrorMessage)
                    .consume();
        }
    }

    private void authorize(Session session) throws IOException, InterruptedException, AuthenticationException {
        if (this.accessToken != null) {
            return;
        }
        GameProfile profile = session.getProfile();
        AuthHandshake handshake = authHandshake(profile.getName());

        if (handshake.offline) {
            return;
        }

        // join the session server
        MinecraftClient.getInstance().getSessionService().joinServer(profile, session.getAccessToken(), handshake.serverId);

        AuthResponse response = authResponse(profile.getName(), handshake.verifyToken);
        if (!response.userId.equals(profile.getId())) {
            throw new IOException("UUID mismatch!"); // probably won't ever throw
        }
        this.accessToken = response.accessToken;
    }

    private AuthHandshake authHandshake(String name) throws IOException, InterruptedException {
        var body = FormBodyPublisher.newBuilder()
                .query("name", name)
                .build();

        var req = HttpRequest.newBuilder(getHandshakeURI())
                .POST(body)
                .build();

        try (var response = MoreHttpResponses.execute(HTTP_CLIENT, req)) {
            return response
                    .requireOK(this::getErrorMessage)
                    .json(AuthHandshake.class);
        }
    }

    private AuthResponse authResponse(String name, long verifyToken) throws IOException, InterruptedException {
        var body = FormBodyPublisher.newBuilder()
                .query("name", name)
                .query("verifyToken", String.valueOf(verifyToken))
                .build();

        var req = HttpRequest.newBuilder(getResponseURI())
                .POST(body)
                .build();

        try (var response = MoreHttpResponses.execute(HTTP_CLIENT, req)) {
            return response
                    .requireOK(this::getErrorMessage)
                    .json(AuthResponse.class);
        }
    }

    private String getErrorMessage(MoreHttpResponses response) throws IOException {
        return response.json().getAsJsonObject().get("message").getAsString();
    }

    private URI buildUserTextureUri(SkinUpload upload) {
        String user = UUIDTypeAdapter.fromUUID(upload.session().getProfile().getId());
        String skinType = upload.type().getParameterizedName();
        return URI.create(String.format("%s/user/%s/%s", this.getApiPrefix(), user, skinType));
    }

    private URI getTexturesURI(GameProfile profile) {
        Preconditions.checkNotNull(profile.getId(), "profile id required for skins");
        return URI.create(String.format("%s/user/%s", this.getApiPrefix(), UUIDTypeAdapter.fromUUID(profile.getId())));
    }

    private URI getHandshakeURI() {
        return URI.create(String.format("%s/auth/handshake", this.getApiPrefix()));
    }

    private URI getResponseURI() {
        return URI.create(String.format("%s/auth/response", this.getApiPrefix()));
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
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .toString();
    }

    private static class AuthHandshake {
        boolean offline;
        String serverId;
        long verifyToken;
    }

    private static class AuthResponse {
        String accessToken;
        UUID userId;
    }
}
