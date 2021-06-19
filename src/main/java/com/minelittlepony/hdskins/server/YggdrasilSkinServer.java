package com.minelittlepony.hdskins.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

@ServerType("mojang")
public class YggdrasilSkinServer implements SkinServer {

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.SYNTHETIC,
            Feature.UPLOAD_USER_SKIN,
            Feature.DOWNLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES);

    private transient final URI address = URI.create("https://api.mojang.com");
    private transient final URI verify = URI.create("https://authserver.mojang.com/validate");

    private transient final boolean requireSecure = true;

    @Override
    public boolean ownsUrl(String url) {
        return false;
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public boolean supportsSkinType(SkinType skinType) {
        return skinType.isVanilla() && skinType != SkinType.CAPE;
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = new HashMap<>();

        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftSessionService session = client.getSessionService();

        profile.getProperties().clear();
        GameProfile newProfile = session.fillProfileProperties(profile, requireSecure);

        if (newProfile == profile) {
            throw new IOException("Mojang API error occurred. You may be throttled.");
        }
        profile = newProfile;

        try {
            textures.putAll(session.getTextures(profile, requireSecure));
        } catch (InsecureTextureException e) {
            HDSkins.LOGGER.error(e);
        }

        return new TexturePayload(profile, textures.entrySet().stream().collect(Collectors.toMap(
                entry -> SkinType.forVanilla(entry.getKey()),
                Map.Entry::getValue
        )));
    }

    @Override
    public void performSkinUpload(SkinUpload upload) throws IOException, InterruptedException {
        authorize(upload.session());

        send(createUploadRequest(upload));

        MinecraftClient client = MinecraftClient.getInstance();
        client.getSessionProperties().clear();
    }

    private HttpRequest createUploadRequest(SkinUpload upload) throws IOException {
        var userId = UUIDTypeAdapter.fromUUID(upload.session().getProfile().getId());
        var texTyp = upload.type().getId().getPath();
        var uri = (URI.create(String.format("%s/user/profile/%s/%s", address, userId, texTyp)));

        var request = HttpRequest.newBuilder(uri)
                .header("authorization", "Bearer " + upload.session().getAccessToken());
        switch (upload.getSchemaAction()) {
            case "none" -> request.DELETE();
            case "file" -> {
                var multipartBodyBuilder = MultipartBodyPublisher.newBuilder()
                        .filePart("file", Path.of(upload.image()), MediaType.IMAGE_PNG);
                mapMetadata(upload.metadata()).forEach(multipartBodyBuilder::textPart);
                request.PUT(multipartBodyBuilder.build());
            }
            case "http", "https" -> {
                var formBodyBuilder = FormBodyPublisher.newBuilder()
                        .query("file", upload.image().toString());
                mapMetadata(upload.metadata()).forEach(formBodyBuilder::query);
                request.POST(formBodyBuilder.build());
            }
            default -> throw new IOException("Unsupported URI scheme: " + upload.getSchemaAction());
        }

        return request.build();
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

    private void authorize(Session session) throws IOException, InterruptedException {
        var token = new TokenRequest(session);
        var request = HttpRequest.newBuilder(verify)
                .POST(MoreBodyPublishers.ofObject(token, MediaType.APPLICATION_JSON))
                .build();

        send(request);
    }

    private void send(HttpRequest request) throws IOException, InterruptedException {
        try (MoreHttpResponses response = MoreHttpResponses.execute(HTTP_CLIENT, request)) {
            response
                    .requireOK(r -> r.json(ErrorResponse.class).toString())
                    .consume();
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

        @NotNull
        final String accessToken;

        TokenRequest(Session session) {
            accessToken = session.getAccessToken();
        }
    }

    static class ErrorResponse {
        String error;
        String errorMessage;

        @Override
        public String toString() {
            return String.format("%s: %s", error, errorMessage);
        }
    }
}
