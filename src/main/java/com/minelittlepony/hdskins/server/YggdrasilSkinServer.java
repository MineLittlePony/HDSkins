package com.minelittlepony.hdskins.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.FileTypes;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Session;

@ServerType("mojang")
public class YggdrasilSkinServer implements SkinServer {

    static final SkinServer INSTANCE = new YggdrasilSkinServer();

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.SYNTHETIC,
            Feature.UPLOAD_USER_SKIN,
            Feature.DOWNLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES);

    private transient final String address = "https://api.mojang.com";
    private transient final String verify = "https://authserver.mojang.com/validate";

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
            HDSkins.LOGGER.error(e);
        }

        return new TexturePayload(profile, textures.entrySet().stream().collect(Collectors.toMap(
                entry -> SkinType.forVanilla(entry.getKey()),
                Map.Entry::getValue
        )));
    }

    @Override
    public void performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException {
        authorize(upload.session());

        switch (upload.getSchemaAction()) {
            case "none":
                execute(HttpRequest.newBuilder(createProfileUri(upload))
                        .DELETE()
                        .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().getAccessToken())
                        .build());
                break;
            case "file":
                execute(HttpRequest.newBuilder(createProfileUri(upload))
                        .PUT(FileTypes.multiPart(mapMetadata(upload.metadata()))
                                .field("file", upload.image())
                                .build())
                        .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                        .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                        .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().getAccessToken())
                        .build());
                break;
            case "http":
            case "https":
                execute(HttpRequest.newBuilder(createProfileUri(upload))
                        .PUT(FileTypes.multiPart(mapMetadata(upload.metadata()))
                                .field("file", upload.image().toString())
                                .build())
                        .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                        .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                        .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().getAccessToken())
                        .build());
                break;
            default:
                throw new IOException("Unsupported URI scheme: " + upload.getSchemaAction());
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.getSessionProperties().clear();
    }

    private URI createProfileUri(SkinUpload upload) {
        return URI.create(String.format("%s/user/profile/%s/%s", address,
                UUIDTypeAdapter.fromUUID(upload.session().getProfile().getId()),
                upload.type().getParameterizedName())
        );
    }

    private Map<String, String> mapMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            String value = entry.getValue();
            return "model".contentEquals(entry.getKey()) && "default".contentEquals(value) ? "classic" : value;
        }));
    }

    private void authorize(Session session) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("accessToken", session.getAccessToken());
        execute(HttpRequest.newBuilder(URI.create(verify))
                .POST(BodyPublishers.ofString(json.toString()))
                .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.APPLICATION_JSON)
                .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                .build());
    }

    private void execute(HttpRequest request) throws IOException {
        MoreHttpResponses response = MoreHttpResponses.execute(request);
        if (!response.ok()) {
            throw new IOException(response.json(ErrorResponse.class, "Server did not respond correctly. Status Code " + response.response().statusCode()).toString());
        }
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .append("secured", requireSecure)
                .toString();
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
