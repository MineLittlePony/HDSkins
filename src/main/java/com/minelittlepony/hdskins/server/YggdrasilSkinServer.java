package com.minelittlepony.hdskins.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.*;
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
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;

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

    private transient final String address = "https://api.minecraftservices.com";
    private transient final String verify = "https://authserver.mojang.com/validate";

    private transient final String skinUploadAddress = address + "/minecraft/profile/skins";
    private transient final String activeSkinAddress = skinUploadAddress + "/active";

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
    public TexturePayload loadSkins(GameProfile profile) throws IOException, AuthenticationException {

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
        } catch (InsecurePublicKeyException e) {
            HDSkins.LOGGER.error(e);
        }

        return new TexturePayload(profile, textures.entrySet().stream().collect(Collectors.toMap(
                entry -> SkinType.forVanilla(entry.getKey()),
                Map.Entry::getValue
        )));
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException {
        authorize(upload.session());

        if (upload instanceof SkinUpload.Delete) {
            execute(HttpRequest.newBuilder(URI.create(activeSkinAddress))
                    .DELETE()
                    .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().getAccessToken())
                    .build());
        } else if (upload instanceof SkinUpload.FileUpload fileUpload) {
            execute(HttpRequest.newBuilder(URI.create(skinUploadAddress))
                    .PUT(FileTypes.multiPart(mapMetadata(fileUpload.metadata()))
                            .field("file", fileUpload.file())
                            .build())
                    .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                    .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                    .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().getAccessToken())
                    .build());
        } else if (upload instanceof SkinUpload.UriUpload) {
            // TODO yes it does! https://wiki.vg/Mojang_API#Change_Skin
            // The question is whether it supports non-minecraft urls
            throw new IOException("Yggdrasil does not support URI uploads");
        } else {
            throw new IllegalArgumentException("Unsupported SkinUpload type: " + upload.getClass());
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.getSessionProperties().clear();
    }

    private Map<String, String> mapMetadata(Map<String, String> metadata) {
        Map<String, String> result = new HashMap<>();
        String model = metadata.getOrDefault("model", "classic");
        result.put("variant", "default".contentEquals(model) ? "classic" : model);
        return result;
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
    public Optional<SkinServerProfile<?>> loadProfile(GameProfile profile) throws IOException, AuthenticationException {

        MoreHttpResponses response = MoreHttpResponses.execute(HttpRequest.newBuilder(URI.create(activeSkinAddress))
                .GET()
                .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + MinecraftClient.getInstance().getSession().getAccessToken())
                .build());

        if (!response.ok()) {
            return Optional.empty();
        }

        ProfileResponse prof = response.json(ProfileResponse.class, "Server send invalid profile response");
        prof.profile = profile;
        return Optional.of(prof);
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

    static class ProfileResponse implements SkinServerProfile<ProfileResponse.Skin> {
        public String id;
        public String name;
        public List<Skin> skins;
        public List<Skin> capes;

        transient GameProfile profile;

        static class Skin implements SkinServerProfile.Skin {
            public String id;
            public State state;
            public String url;
            public String alias;

            @Override
            public String getModel() {
                return alias;
            }

            @Override
            public boolean isActive() {
                return state == State.ACTIVE;
            }

            @Override
            public String getUri() {
                return url;
            }
        }

        enum State {
            ACTIVE,
            INACTIVE
        }

        @Override
        public GameProfile getGameProfile() {
            return profile;
        }

        @Override
        public List<Skin> getSkins(SkinType type) {
            if (type == SkinType.SKIN) {
                return skins;
            }
            if (type == SkinType.CAPE) {
                return capes;
            }
            return List.of();
        }

        @Override
        public void setActive(SkinType type, Skin texture) {

        }
    }
}
