package com.minelittlepony.hdskins.server;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.*;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.profile.ProfileUtils;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinUpload.Session;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.FileTypes;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

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
    private static final Set<Feature> READ_ONLY_FEATURES = Sets.newHashSet(
            Feature.SYNTHETIC,
            Feature.DOWNLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES);

    private transient final String address = "https://api.minecraftservices.com";
    //private transient final String verify = "https://authserver.mojang.com/validate";

    private transient final String profileAddress = address + "/minecraft/profile";
    private transient final String skinUploadAddress = address + "/minecraft/profile/skins";
    private transient final String activeSkinAddress = skinUploadAddress + "/active";
    private transient final String activeCapeAddress = address + "/minecraft/profile/capes/active";

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
    public Set<Feature> getFeatures(SkinType skinType) {
        return skinType != SkinType.SKIN ? READ_ONLY_FEATURES : FEATURES;
    }

    @Override
    public boolean supportsSkinType(SkinType skinType) {
        return skinType.isVanilla();
    }

    @Override
    public TexturePayload loadSkins(GameProfile profile) throws IOException, AuthenticationException {
        MinecraftSessionService service = HDSkinsServer.getInstance().getSessionService();

        @Nullable
        ProfileResult result = service.fetchProfile(profile.getId(), requireSecure);

        if (result == null) {
            throw new AuthenticationException("Mojang API error occured. You may be throttled.");
        }

        try {
            profile = result.profile();
            return new TexturePayload(profile, ProfileUtils.readVanillaTexturesBlob(service, profile).findFirst().orElseGet(HashMap::new));
        } catch (InsecurePublicKeyException e) {
            throw new AuthenticationException(e);
        }
    }

    @Override
    public TexturePayload loadSkins(Session session) throws IOException, AuthenticationException {
        @Nullable
        TexturePayload payload = loadProfile(session).map(profile -> {
            Map<SkinType, MinecraftProfileTexture> textures = new HashMap<>();
            profile.skins.stream().filter(i -> i.isActive()).findFirst().ifPresent(skin -> {
                textures.put(SkinType.SKIN, new MinecraftProfileTexture(skin.url, Map.of("model", "classic".equalsIgnoreCase(skin.variant) ? "default" : skin.variant.toLowerCase(Locale.ROOT))));
            });
            profile.capes.stream().filter(i -> i.isActive()).findFirst().ifPresent(skin -> {
                textures.put(SkinType.CAPE, new MinecraftProfileTexture(skin.url, Map.of()));
                textures.put(SkinType.ELYTRA, new MinecraftProfileTexture(skin.url, Map.of()));
            });
            return new TexturePayload(session.profile(), textures);
        }).orElse(null);
        if (payload == null) {
            return loadSkins(session.profile());
        }
        return payload;
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException {
        authorize(upload.session());

        if (upload instanceof SkinUpload.Delete) {
            execute(HttpRequest.newBuilder(URI.create(upload.type() == SkinType.SKIN ? activeSkinAddress : activeCapeAddress))
                    .DELETE()
                    .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().accessToken()));
        } else if (upload instanceof SkinUpload.FileUpload fileUpload) {
            execute(FileTypes.multiPart(mapMetadata(fileUpload.metadata()))
                        .field("file", fileUpload.file())
                    .build(HttpRequest.newBuilder(URI.create(skinUploadAddress))::POST)
                        .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                        .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().accessToken()));
        } else if (upload instanceof SkinUpload.UriUpload uriUpload) {
            // https://wiki.vg/Mojang_API#Change_Skin
            execute(FileTypes.multiPart(mapMetadata(Util.make(uriUpload.metadata(), metadata -> {
                metadata.put("url", uriUpload.uri().toString());
            }))).build(HttpRequest.newBuilder(URI.create(skinUploadAddress))::POST)
                    .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                    .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + upload.session().accessToken()));
        } else {
            throw new IllegalArgumentException("Unsupported SkinUpload type: " + upload.getClass());
        }
    }

    private Map<String, String> mapMetadata(Map<String, String> metadata) {
        Map<String, String> result = new HashMap<>();
        String model = metadata.getOrDefault("model", "classic");
        result.put("variant", "default".contentEquals(model) ? "classic" : model);
        return result;
    }

    @Override
    public void authorize(Session session) throws IOException {
        /*JsonObject json = new JsonObject();
        json.addProperty("accessToken", session.accessToken());
        execute(HttpRequest.newBuilder(URI.create(verify))
                .POST(BodyPublishers.ofString(json.toString()))
                .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.APPLICATION_JSON)
                .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                .build());*/
    }

    private void execute(HttpRequest.Builder request) throws IOException {
        MoreHttpResponses response = MoreHttpResponses.execute(request);
        if (!response.ok()) {
            throw new IOException(response.json(ErrorResponse.class, "Server did not respond correctly. Status Code " + response.response().statusCode()).toString());
        }
    }

    public void setActiveCape(Session session, String capeId) throws IOException, AuthenticationException {
        authorize(session);
        execute(HttpRequest.newBuilder(URI.create(activeCapeAddress))
                .PUT(FileTypes.json(Map.of("capeId", capeId)))
                .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + session.accessToken()));
    }

    @Override
    public Optional<ProfileResponse> loadProfile(Session session) throws IOException, AuthenticationException {
        MoreHttpResponses response = MoreHttpResponses.execute(HttpRequest.newBuilder(URI.create(profileAddress))
                .GET()
                .header(FileTypes.HEADER_AUTHORIZATION, "Bearer " + session.accessToken())
                .build());

        if (!response.ok()) {
            return Optional.empty();
        }

        ProfileResponse prof = response.json(ProfileResponse.class, "Server send invalid profile response");
        prof.session = session;
        prof.server = this;
        return Optional.of(prof);
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .append("secured", requireSecure)
                .toString();
    }

    @Override
    public Map<Text, Text> getMetadata() {
        return Map.of(
            Text.translatable("hdskins.label.website"), Text.literal("https://www.minecraft.net/en-us/login").formatted(Formatting.UNDERLINE).withColor(Colors.BLUE).styled(style -> {
                return style.withClickEvent(new ClickEvent(Action.OPEN_URL, "https://www.minecraft.net/en-us/login"));
            }),
            Text.translatable("hdskins.label.author"), Text.literal("Mojang")
        );
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

        transient Session session;
        transient YggdrasilSkinServer server;

        static class Skin implements SkinServerProfile.Skin {
            public String id;
            public State state;
            public String url;
            public String textureKey;
            public String variant;

            @Override
            public String getModel() {
                return variant;
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
        public void setActive(SkinType type, Skin texture) throws IOException, AuthenticationException {
            if (texture.state == State.ACTIVE) {
                return;
            }
            getSkins(type).forEach(s -> s.state = State.INACTIVE);
            texture.state = State.ACTIVE;
            if (type == SkinType.CAPE) {
                server.setActiveCape(session, texture.id);
            }
        }
    }
}
