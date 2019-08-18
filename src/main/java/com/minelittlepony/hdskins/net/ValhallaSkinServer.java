package com.minelittlepony.hdskins.net;

import com.google.common.base.Preconditions;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@ServerType("valhalla")
public class ValhallaSkinServer implements SkinServer {

    private final String address;

    private transient String accessToken;

    public ValhallaSkinServer(String address) {
        this.address = address;
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(HDSkins.httpClient, new HttpGet(getTexturesURI(profile)))) {

            if (response.ok()) {
                return response.unwrapAsJson(TexturePayload.class);
            }

            throw new HttpException(response.getResponse());
        }
    }

    @Override
    public SkinUpload.Response performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException {
        try {
            return uploadPlayerSkin(upload);
        } catch (IOException e) {
            if (e.getMessage().equals("Authorization failed")) {
                accessToken = null;
                return uploadPlayerSkin(upload);
            }

            throw e;
        }
    }

    private SkinUpload.Response uploadPlayerSkin(SkinUpload upload) throws IOException, AuthenticationException {
        authorize(upload.getSession());

        switch (upload.getSchemaAction()) {
            case "none":
                return resetSkin(upload);
            case "file":
                return uploadFile(upload);
            case "http":
            case "https":
                return uploadUrl(upload);
            default:
                throw new IOException("Unsupported URI scheme: " + upload.getSchemaAction());
        }
    }

    private SkinUpload.Response resetSkin(SkinUpload upload) throws IOException {
        return upload(RequestBuilder.delete()
                .setUri(buildUserTextureUri(upload.getSession().getProfile(), upload.getType()))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .build());
    }

    private SkinUpload.Response uploadFile(SkinUpload upload) throws IOException {
        final File file = new File(upload.getImage());

        MultipartEntityBuilder b = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.create("image/png"), file.getName());

        upload.getMetadata().forEach(b::addTextBody);

        return upload(RequestBuilder.put()
                .setUri(buildUserTextureUri(upload.getSession().getProfile(), upload.getType()))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .setEntity(b.build())
                .build());
    }

    private SkinUpload.Response uploadUrl(SkinUpload upload) throws IOException {
        return upload(RequestBuilder.post()
                .setUri(buildUserTextureUri(upload.getSession().getProfile(), upload.getType()))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .addParameter("file", upload.getImage().toString())
                .addParameters(MoreHttpResponses.mapAsParameters(upload.getMetadata()))
                .build());
    }

    private SkinUpload.Response upload(HttpUriRequest request) throws IOException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(HDSkins.httpClient, request)) {
            return response.unwrapAsJson(SkinUpload.Response.class);
        }
    }

    private void authorize(Session session) throws IOException, AuthenticationException {
        if (this.accessToken != null) {
            return;
        }
        GameProfile profile = session.getProfile();
        String token = session.getAccessToken();
        AuthHandshake handshake = authHandshake(profile.getName());

        if (handshake.offline) {
            return;
        }

        // join the session server
        MinecraftClient.getInstance().getSessionService().joinServer(profile, token, handshake.serverId);

        AuthResponse response = authResponse(profile.getName(), handshake.verifyToken);
        if (!response.userId.equals(profile.getId())) {
            throw new IOException("UUID mismatch!"); // probably won't ever throw
        }
        this.accessToken = response.accessToken;
    }

    private AuthHandshake authHandshake(String name) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(HDSkins.httpClient, RequestBuilder.post()
                .setUri(getHandshakeURI())
                .addParameter("name", name)
                .build())) {
            return resp.unwrapAsJson(AuthHandshake.class);
        }
    }

    private AuthResponse authResponse(String name, long verifyToken) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(HDSkins.httpClient, RequestBuilder.post()
                .setUri(getResponseURI())
                .addParameter("name", name)
                .addParameter("verifyToken", String.valueOf(verifyToken))
                .build())) {
            return resp.unwrapAsJson(AuthResponse.class);
        }
    }

    private URI buildUserTextureUri(GameProfile profile, SkinType textureType) {
        String user = UUIDTypeAdapter.fromUUID(profile.getId());
        String skinType = textureType.name().toLowerCase(Locale.US);
        return URI.create(String.format("%s/user/%s/%s", this.address, user, skinType));
    }

    private URI getTexturesURI(GameProfile profile) {
        Preconditions.checkNotNull(profile.getId(), "profile id required for skins");
        return URI.create(String.format("%s/user/%s", this.address, UUIDTypeAdapter.fromUUID(profile.getId())));
    }

    private URI getHandshakeURI() {
        return URI.create(String.format("%s/auth/handshake", this.address));
    }

    private URI getResponseURI() {
        return URI.create(String.format("%s/auth/response", this.address));
    }

    @Override
    public boolean supportsFeature(Feature feature) {
        switch (feature) {
            case DOWNLOAD_USER_SKIN:
            case UPLOAD_USER_SKIN:
            case DELETE_USER_SKIN:
            case MODEL_VARIANTS:
            case MODEL_TYPES:
                return true;
            default: return false;
        }
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
