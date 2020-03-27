package com.minelittlepony.hdskins.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
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
            Feature.MODEL_TYPES);

    private final String address;

    private transient String accessToken;

    public ValhallaSkinServer(String address) {
        this.address = address;
    }

    private String getApiPrefix() {
        return address + API_PREFIX;
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException, AuthenticationException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(HTTP_CLIENT, new HttpGet(getTexturesURI(profile)))) {
            if (response.ok()) {
                return response.unwrapAsJson(TexturePayload.class);
            }

            throw new HttpException(response.response());
        }
    }

    @Override
    public void performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException {
        try {
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
        authorize(upload.getSession());

        switch (upload.getSchemaAction()) {
            case "none":
                resetSkin(upload);
                break;
            case "file":
                uploadFile(upload);
                break;
            case "http":
            case "https":
                uploadUrl(upload);
                break;
            default:
                throw new IOException("Unsupported URI scheme: " + upload.getSchemaAction());
        }
    }

    private void resetSkin(SkinUpload upload) throws IOException {
        upload(RequestBuilder.delete()
                .setUri(buildUserTextureUri(upload))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .build());
    }

    private void uploadFile(SkinUpload upload) throws IOException {
        final File file = new File(upload.getImage());

        MultipartEntityBuilder b = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.create("image/png"), file.getName());

        upload.getMetadata().forEach(b::addTextBody);

        upload(RequestBuilder.put()
                .setUri(buildUserTextureUri(upload))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .setEntity(b.build())
                .build());
    }

    private void uploadUrl(SkinUpload upload) throws IOException {
        upload(RequestBuilder.post()
                .setUri(buildUserTextureUri(upload))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .addParameter("file", upload.getImage().toString())
                .addParameters(mapAsParameters(upload.getMetadata()))
                .build());
    }

    static NameValuePair[] mapAsParameters(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                .toArray(NameValuePair[]::new);
    }

    private void upload(HttpUriRequest request) throws IOException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(HTTP_CLIENT, request)) {
            if (!response.ok()) {
                throw response.exception();
            }
        }
    }

    private void authorize(Session session) throws IOException, AuthenticationException {
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

    private AuthHandshake authHandshake(String name) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(HTTP_CLIENT, RequestBuilder.post()
                .setUri(getHandshakeURI())
                .addParameter("name", name)
                .build())) {
            return resp.unwrapAsJson(AuthHandshake.class);
        }
    }

    private AuthResponse authResponse(String name, long verifyToken) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(HTTP_CLIENT, RequestBuilder.post()
                .setUri(getResponseURI())
                .addParameter("name", name)
                .addParameter("verifyToken", String.valueOf(verifyToken))
                .build())) {
            return resp.unwrapAsJson(AuthResponse.class);
        }
    }

    private URI buildUserTextureUri(SkinUpload upload) {
        String user = UUIDTypeAdapter.fromUUID(upload.getSession().getProfile().getId());
        String skinType = upload.getType().name().toLowerCase(Locale.US);
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
