package com.minelittlepony.hdskins.server;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinUpload.Session;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.function.Function;

@ServerType("valhalla")
public class ValhallaSkinServer implements SkinServer {

    private static final String API_PREFIX = "/api/v1";
    private static final String SRC = "https://github.com/MineLittlePony/ValhallaSkinServer";

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

    private static NameValuePair param(String name, String value) {
        return new BasicNameValuePair(name, value);
    }

    private URI buildBackendUri(String path, NameValuePair... params) {
        try {
            return new URIBuilder(address + API_PREFIX + "/" + path).setParameters(params).build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to build URI", e);
        }
    }

    private URI buildBackendUserUri(UUID uuid) {
        return buildBackendUri(String.format("user/%s", uuid.toString()));
    }

    private URI buildBackendHistoryUri(UUID uuid) {
        return buildBackendUri(String.format("history/%s", uuid.toString()));
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
            String domain = new URI(address).getHost();

            return domain.contentEquals(url) || (url.startsWith("textures") && domain.contentEquals(url.replace("textures", "skins")));
        } catch (final URISyntaxException ignored) { }

        return false;
    }

    @Override
    public TexturePayload loadSkins(GameProfile profile) throws IOException {
        var path = buildBackendUserUri(profile.getId());
        return MoreHttpResponses.execute(HttpRequest.newBuilder(path)
                    .GET()
                    .build())
                .requireOk()
                .json(TexturePayload.class, "Invalid texture payload");
    }

    @Override
    public List<TexturePayload> loadSkins(Collection<GameProfile> profiles) throws IOException {
        var data = new BulkTextures(profiles.stream().map(GameProfile::getId).toList());
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("bulk_textures"))
                        .POST(FileTypes.json(data))
                        .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.APPLICATION_JSON)
                        .build())
                .requireOk()
                .json(BulkTexturesResponse.class, "Invalid texture payload")
                .users;
    }

    @Override
    public TexturePayload loadSkins(Session session) throws IOException, AuthenticationException {
        return doAuthorizedRequest(session, (accessToken) -> new TexturePayload(
                session.profile(),
                MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures"))
                        .GET()
                        .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                        .build())
                    .requireOk()
                    .json(TexturePayload.Textures.class, "Invalid texture payload")
        ));
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException {
        doAuthorizedRequest(upload.session(), (accessToken) -> switch (upload) {
            // TODO: (@Killjoy) Use namespaced ids and translate old unnamespaced to namespaced
            case SkinUpload.Delete ignored ->
                    MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures", param("type", upload.type().getParameterizedName())))
                            .DELETE()
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
            case SkinUpload.FileUpload fileUpload ->
                    MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures"))
                            .PUT(FileTypes.multiPart()
                                    .field("type", fileUpload.type().getParameterizedName())
                                    .field("file", fileUpload.file())
                                    .field("meta", new Gson().toJson(fileUpload.metadata()))
                                    .build())
                            .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.MULTI_PART_FORM_DATA)
                            .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
            case SkinUpload.UriUpload uriUpload ->
                    MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("textures"))
                            .POST(FileTypes.json(Map.of(
                                "type", uriUpload.type().getParameterizedName(),
                                "file", uriUpload.uri().toString(),
                                "meta", uriUpload.metadata()
                            )))
                            .header(FileTypes.HEADER_CONTENT_TYPE, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_ACCEPT, FileTypes.APPLICATION_JSON)
                            .header(FileTypes.HEADER_AUTHORIZATION, accessToken)
                            .build())
                    .requireOk();
        });
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

    @Override
    public Optional<SkinServerProfile<?>> loadProfile(Session session) throws IOException, AuthenticationException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendHistoryUri(session.profile().getId()))
                .GET()
                .build()).accept(r -> r.json(Textures.class, "Server sent invalid profile response")).map(p -> {
                    // TODO: (@Killjoy) Remove duplicates and sort by upload time
            Function<SkinType, List<Texture>> textures = Util.memoize(type -> {
                Set<String> visited = new HashSet<>();
                return p.textures().getOrDefault(type, List.of())
                        .stream()
                        .filter(texture -> visited.add(texture.url + texture.getModel()))
                        .sorted(Comparator.comparing(t -> -t.startTime))
                        .toList();
            });
            return new SkinServerProfile<Texture>() {
                @Override
                public List<Texture> getSkins(SkinType type) {
                    return textures.apply(type);
                }

                @Override
                public void setActive(SkinType type, Texture texture) throws IOException, AuthenticationException {
                    // TODO: (@Killjoy) Swap active skins rather than upload a copy
                    uploadSkin(new SkinUpload.UriUpload(session, type, URI.create(texture.getUri()), texture.metadata));
                }
            };
        });
    }

    private interface AuthorizedRequest<T> {
        T doRequest(String accessToken) throws IOException, AuthenticationException;
    }

    private <T> T doAuthorizedRequest(Session session, AuthorizedRequest<T> requester) throws IOException, AuthenticationException {
        authorize(session);
        try {
            return requester.doRequest(accessToken);
        } catch (HttpException e) {
            if (e.getStatusCode() != 401) {
                throw e;
            }

            accessToken = null;
            authorize(session);
            return requester.doRequest(accessToken);
        } catch (IOException e) {
            if (e.getMessage().equals("Authorization failed")) {
                accessToken = null;
                authorize(session);
                return requester.doRequest(accessToken);
            }

            throw e;
        }
    }

    private AuthHandshake authHandshake(String name) throws IOException {
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("auth/minecraft"))
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
        return MoreHttpResponses.execute(HttpRequest.newBuilder(buildBackendUri("auth/minecraft/callback"))
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

    @Override
    public Map<Text, Text> getMetadata() {
        return Map.of(
            Text.translatable("hdskins.label.documentation"), Text.literal(address + "/docs").formatted(Formatting.UNDERLINE).withColor(Colors.BLUE).styled(style -> {
                return style.withClickEvent(new ClickEvent(Action.OPEN_URL, address + "/docs"));
            }),
            Text.translatable("hdskins.label.source"), Text.literal(SRC).formatted(Formatting.UNDERLINE).withColor(Colors.BLUE).styled(style -> {
                return style.withClickEvent(new ClickEvent(Action.OPEN_URL, SRC));
            }),
            Text.translatable("hdskins.label.author"), Text.literal("Killjoy")
        );
    }

    private record AuthHandshake(boolean offline, String serverId, long verifyToken) {}
    private record AuthResponse(String accessToken, UUID userId) {}

    private record BulkTextures(List<UUID> uuids) {}
    private record BulkTexturesResponse(List<TexturePayload> users) {}

    // TODO: (@Killjoy) Response does not match the documentation
    private record Textures (String profileId, String profilename, Map<SkinType, List<Texture>> textures) {}
    private record Texture (long startTime, String endTime, Map<String, String> metadata, String url) implements SkinServerProfile.Skin {
        @Override
        public String getModel() {
            return VanillaModels.of(metadata.get("model"));
        }

        @Override
        public boolean isActive() {
            return endTime == null || "null".equalsIgnoreCase(endTime);
        }

        @Override
        public String getUri() {
            return url;
        }
    }
}
