package com.minelittlepony.hdskins.skins.valhalla;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.minelittlepony.hdskins.client.HDSkinsClient;
import com.minelittlepony.hdskins.skins.Feature;
import com.minelittlepony.hdskins.skins.GameSession;
import com.minelittlepony.hdskins.skins.SkinType;
import com.minelittlepony.hdskins.skins.SkinUpload;
import com.minelittlepony.hdskins.skins.TexturePayload;
import com.minelittlepony.hdskins.skins.api.SkinServer;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.util.UUIDTypeAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ValhallaSkinServer implements SkinServer {

    private static final String API_PREFIX = "/api/v1";
    private static final CloseableHttpClient httpClient = HttpClients.createSystem();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .create();

    private final String address;

    private transient String accessToken;

    public ValhallaSkinServer(String address) {
        this.address = address;
    }

    private String getApiPrefix() {
        return address + API_PREFIX;
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(httpClient, new HttpGet(getTexturesURI(profile)))) {

            if (response.ok()) {
                return gson.fromJson(response.json(), TexturePayload.class);
            }

            throw new HttpException(response.getResponse());
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
                .setUri(buildUserTextureUri(upload.getSession(), upload.getType()))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .build());
    }

    private void uploadFile(SkinUpload upload) throws IOException {
        final File file = new File(upload.getImage());

        MultipartEntityBuilder b = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.create("image/png"), file.getName());

        upload.getMetadata().forEach(b::addTextBody);

        upload(RequestBuilder.put()
                .setUri(buildUserTextureUri(upload.getSession(), upload.getType()))
                .addHeader(HttpHeaders.AUTHORIZATION, this.accessToken)
                .setEntity(b.build())
                .build());
    }

    private void uploadUrl(SkinUpload upload) throws IOException {
        upload(RequestBuilder.post()
                .setUri(buildUserTextureUri(upload.getSession(), upload.getType()))
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
        try (MoreHttpResponses response = MoreHttpResponses.execute(httpClient, request)) {
            if (response.ok()) {
                return;
            }
            try {
                JsonObject error = response.json().getAsJsonObject();
                throw new IOException(error.get("message").getAsString());
            } catch (JsonParseException e) {
                String text = response.text();
                HDSkinsClient.logger.error("Server error wasn't in json: {}", text);
                throw new IOException(e);
            }
        }
    }

    private void authorize(GameSession session) throws IOException, AuthenticationException {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            throw new UnsupportedOperationException("Server cannot upload a skin.");
        }

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

    private String jsonErrorMsg(JsonElement e) {
        return e.getAsJsonObject().get("message").getAsString();
    }

    private AuthHandshake authHandshake(String name) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(httpClient, RequestBuilder.post()
                .setUri(getHandshakeURI())
                .addParameter("name", name)
                .build())) {
            if (resp.ok()) {
                return gson.fromJson(resp.json(), AuthHandshake.class);
            }
            throw new IOException(jsonErrorMsg(resp.json()));
        }
    }

    private AuthResponse authResponse(String name, long verifyToken) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(httpClient, RequestBuilder.post()
                .setUri(getResponseURI())
                .addParameter("name", name)
                .addParameter("verifyToken", String.valueOf(verifyToken))
                .build())) {
            if (resp.ok()) {
                return gson.fromJson(resp.json(), AuthResponse.class);
            }
            throw new IOException(jsonErrorMsg(resp.json()));
        }
    }

    private URI buildUserTextureUri(GameSession session, SkinType textureType) {
        String user = session.getUniqueId();
        String skinType = textureType.name().toLowerCase(Locale.US);
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
        return ImmutableSet.of(
                Feature.DOWNLOAD_USER_SKIN,
                Feature.UPLOAD_USER_SKIN,
                Feature.DELETE_USER_SKIN,
                Feature.MODEL_VARIANTS,
                Feature.MODEL_TYPES,
                Feature.MODEL_METADATA
        );
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
