package com.minelittlepony.hdskins.skins;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.skins.SkinType;
import com.minelittlepony.hdskins.skins.api.SkinServer;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;

public class YggdrasilSkinServer implements SkinServer {

    static final SkinServer INSTANCE = new YggdrasilSkinServer();

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.UPLOAD_USER_SKIN,
            Feature.DOWNLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES);

    private transient final String address = "https://api.mojang.com";
    private transient final String verify = "https://authserver.mojang.com/validate";

    private transient final boolean requireSecure = true;

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException, AuthenticationException {

        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = new HashMap<>();

        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftSessionService session = client.getSessionService();

        profile.getProperties().clear();
        GameProfile newProfile = session.fillProfileProperties(profile, requireSecure);

        if (newProfile == profile) {
            throw new AuthenticationException("Mojang API error occured. You may be throttled.");
        }
        profile = newProfile;

        try {
            textures.putAll(session.getTextures(profile, requireSecure));
        } catch (InsecureTextureException e) {
            HDSkins.logger.error(e);
        }

        return new TexturePayload(profile, textures.entrySet().stream().collect(Collectors.toMap(
                entry -> SkinType.of(entry.getKey().name()),
                Map.Entry::getValue
        )));
    }

    @Override
    public void performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException {
        authorize(upload.getSession());

        switch (upload.getSchemaAction()) {
            case "none":
                send(appendHeaders(upload, RequestBuilder.delete()));
                break;
            default:
                send(prepareUpload(upload, RequestBuilder.put()));
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.getSessionProperties().clear();
    }

    private RequestBuilder prepareUpload(SkinUpload upload, RequestBuilder request) throws IOException {
        request = appendHeaders(upload, request);
        switch (upload.getSchemaAction()) {
            case "file":
                final File file = new File(upload.getImage());

                MultipartEntityBuilder b = MultipartEntityBuilder.create()
                        .addBinaryBody("file", file, ContentType.create("image/png"), file.getName());

                mapMetadata(upload.getMetadata()).forEach(b::addTextBody);

                return request.setEntity(b.build());
            case "http":
            case "https":
                return request
                        .addParameter("file", upload.getImage().toString())
                        .addParameters(MoreHttpResponses.mapAsParameters(mapMetadata(upload.getMetadata())));
            default:
                throw new IOException("Unsupported URI scheme: " + upload.getSchemaAction());
        }
    }

    private RequestBuilder appendHeaders(SkinUpload upload, RequestBuilder request) {
        return request
                .setUri(URI.create(String.format("%s/user/profile/%s/%s", address,
                        UUIDTypeAdapter.fromUUID(upload.getSession().getProfile().getId()),
                        upload.getType().name().toLowerCase(Locale.US))))
                .addHeader("authorization", "Bearer " + upload.getSession().getAccessToken());
    }

    private Map<String, String> mapMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> {
                    String value = entry.getValue();
                    if ("model".contentEquals(entry.getKey()) && "default".contentEquals(value)) {
                        return "classic";
                    }
                    return value;
                })
        );
    }

    private void authorize(GameSession session) throws IOException {
        RequestBuilder request = RequestBuilder.post().setUri(verify);
        request.setEntity(new TokenRequest(session).toEntity());

        send(request);
    }

    private void send(RequestBuilder request) throws IOException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(HTTP_CLIENT, request.build())) {
            if (!response.ok()) {
                throw new IOException(response.json(ErrorResponse.class, "Server error wasn't in json: {}").toString());
            }
        }
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .append("secured", requireSecure)
                .toString();
    }

    static class TokenRequest {
        static final Gson GSON = new Gson();

        @Nonnull
        private final String accessToken;

        TokenRequest(GameSession session) {
            accessToken = session.getAccessToken();
        }

        public StringEntity toEntity() throws IOException {
            return new StringEntity(GSON.toJson(this), ContentType.APPLICATION_JSON);
        }
    }

    class ErrorResponse {
        String error;
        String errorMessage;

        @Override
        public String toString() {
            return String.format("%s: %s", error, errorMessage);
        }
    }
}
