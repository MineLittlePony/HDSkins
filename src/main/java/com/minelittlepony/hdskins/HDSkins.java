package com.minelittlepony.hdskins;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.minelittlepony.common.client.IModUtilities;
import com.minelittlepony.common.util.MoreStreams;
import com.minelittlepony.hdskins.ducks.INetworkPlayerInfo;
import com.minelittlepony.hdskins.dummy.DummyPlayer;
import com.minelittlepony.hdskins.dummy.RenderDummyPlayer;
import com.minelittlepony.hdskins.gui.GuiSkins;
import com.minelittlepony.hdskins.mixin.MixinClientPlayer;
import com.minelittlepony.hdskins.net.BethlehemSkinServer;
import com.minelittlepony.hdskins.net.LegacySkinServer;
import com.minelittlepony.hdskins.net.ServerType;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.net.ValhallaSkinServer;
import com.minelittlepony.hdskins.resources.SkinResourceManager;
import com.minelittlepony.hdskins.resources.TextureLoader;
import com.minelittlepony.hdskins.resources.texture.ImageBufferDownloadHD;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.minelittlepony.hdskins.util.ProfileTextureUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.util.Identifier;

public final class HDSkins {
    public static final String MOD_ID = "hdskins";

    public static final Logger logger = LogManager.getLogger();

    public static final ExecutorService skinUploadExecutor = Executors.newSingleThreadExecutor();
    public static final ExecutorService skinDownloadExecutor = Executors.newFixedThreadPool(8);
    public static final CloseableHttpClient httpClient = HttpClients.createSystem();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    private final List<ISkinCacheClearListener> clearListeners = Lists.newArrayList();

    private final BiMap<String, Class<? extends SkinServer>> skinServerTypes = HashBiMap.create(2);

    private final List<SkinServer> skinServers = Lists.newArrayList();

    private LoadingCache<GameProfile, CompletableFuture<Map<Type, MinecraftProfileTexture>>> skins = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::loadProfileData));

    private final List<ISkinModifier> skinModifiers = Lists.newArrayList();

    private final List<ISkinParser> skinParsers = Lists.newArrayList();

    private AbstractConfig config;

    private final SkinResourceManager resources = new SkinResourceManager();

    private Function<List<SkinServer>, GuiSkins> skinsGuiFunc = GuiSkins::new;

    private final IModUtilities utils;

    public HDSkins(IModUtilities utils) {
        instance = this;
        this.utils = utils;

        // register default skin server types
        addSkinServerType(LegacySkinServer.class);
        addSkinServerType(ValhallaSkinServer.class);
        addSkinServerType(BethlehemSkinServer.class);

        this.config = Config.of(utils.getConfigDirectory().resolve("hdskins.json"));
    }

    public IModUtilities getUtils() {
        return utils;
    }

    public AbstractConfig getConfig() {
        return config;
    }

    void postInit(MinecraftClient client) {
        ReloadableResourceManager irrm = (ReloadableResourceManager) client.getResourceManager();
        irrm.registerListener(resources);

        getUtils().addRenderer(DummyPlayer.class, RenderDummyPlayer::new);

        // register skin servers.
        config.skin_servers.forEach(this::addSkinServer);
    }

    public void setSkinsGui(Function<List<SkinServer>, GuiSkins> skinsGuiFunc) {
        Preconditions.checkNotNull(skinsGuiFunc, "skinsGuiFunc");
        this.skinsGuiFunc = skinsGuiFunc;
    }

    public GuiSkins createSkinsGui() {
        return skinsGuiFunc.apply(ImmutableList.copyOf(this.skinServers));
    }

    private CompletableFuture<Map<Type, MinecraftProfileTexture>> loadProfileData(GameProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            if (profile.getId() == null) {
                return Collections.emptyMap();
            }

            Map<Type, MinecraftProfileTexture> textureMap = Maps.newEnumMap(Type.class);

            for (SkinServer server : skinServers) {
                try {
                    server.loadProfileData(profile).getTextures().forEach(textureMap::putIfAbsent);
                    if (textureMap.size() == Type.values().length) {
                        break;
                    }
                } catch (IOException e) {
                    logger.trace(e);
                }

            }
            return textureMap;
        }, skinDownloadExecutor);
    }

    public CompletableFuture<Map<Type, MinecraftProfileTexture>> loadProfileTextures(GameProfile profile) {
        try {
            // try to recreate a broken gameprofile
            // happens when server sends a random profile with skin and displayname
            Property textures = Iterables.getFirst(profile.getProperties().get("textures"), null);

            if (textures != null) {
                String json = new String(Base64.getDecoder().decode(textures.getValue()), StandardCharsets.UTF_8);

                MinecraftTexturesPayload texturePayload = SkinServer.gson.fromJson(json, MinecraftTexturesPayload.class);

                if (texturePayload != null) {

                    String name = texturePayload.getProfileName(); // name is optional
                    UUID uuid = texturePayload.getProfileId();

                    if (uuid != null) {
                        profile = new GameProfile(uuid, name); // uuid is required
                    }

                    // probably uses this texture for a reason. Don't mess with it.
                    if (!texturePayload.getTextures().isEmpty() && texturePayload.getProfileId() == null) {
                        return CompletableFuture.completedFuture(Collections.emptyMap());
                    }
                }
            }
        } catch (Exception e) {
            if (profile.getId() == null) { // Something broke server-side probably
                logger.warn("{} had a null UUID and was unable to recreate it from texture profile.", profile.getName(), e);
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
        }
        return skins.getUnchecked(profile);
    }

    public void fetchAndLoadSkins(GameProfile profile, SkinTextureAvailableCallback callback) {
        loadProfileTextures(profile).thenAcceptAsync(m -> m.forEach((type, pp) -> {
            loadTexture(type, pp, (typeIn, location, profileTexture) -> {
                parseSkin(profile, typeIn, location, profileTexture)
                        .thenRun(() -> callback.onSkinTextureAvailable(typeIn, location, profileTexture));
            });
        }), MinecraftClient.getInstance()::execute);
    }

    public Identifier loadTexture(Type type, MinecraftProfileTexture texture, @Nullable SkinTextureAvailableCallback callback) {
        String skinDir = type.toString().toLowerCase() + "s/";

        final Identifier resource = new Identifier("hdskins", skinDir + texture.getHash());
        Texture texObj = MinecraftClient.getInstance().getTextureManager().getTexture(resource);

        //noinspection ConstantConditions
        if (texObj != null) {
            if (callback != null) {
                callback.onSkinTextureAvailable(type, resource, texture);
            }
        } else {

            // schedule texture loading on the main thread.
            TextureLoader.loadTexture(resource, new PlayerSkinTexture(
                    getUtils().getAssetsDirectory().resolveSibling("hd/" + skinDir + texture.getHash().substring(0, 2) + "/" + texture.getHash()).toFile(),
                    texture.getUrl(),
                    DefaultSkinHelper.getTexture(),
                    new ImageBufferDownloadHD(type, () -> {
                        if (callback != null) {
                            callback.onSkinTextureAvailable(type, resource, texture);
                        }
                    })));
        }

        return resource;
    }

    public Map<Type, Identifier> getTextures(GameProfile profile) {
        Map<Type, Identifier> map = new HashMap<>();

        for (Map.Entry<Type, MinecraftProfileTexture> e : loadProfileTextures(profile).getNow(Collections.emptyMap()).entrySet()) {
            map.put(e.getKey(), loadTexture(e.getKey(), e.getValue(), null));
        }

        return map;
    }

    private void addSkinServerType(Class<? extends SkinServer> type) {
        Preconditions.checkArgument(!type.isInterface(), "type cannot be an interface");
        Preconditions.checkArgument(!Modifier.isAbstract(type.getModifiers()), "type cannot be abstract");

        ServerType st = type.getAnnotation(ServerType.class);

        if (st == null) {
            throw new IllegalArgumentException("class is not annotated with @ServerType");
        }

        this.skinServerTypes.put(st.value(), type);
    }

    public void clearSkinCache() {
        logger.info("Clearing local player skin cache");

        try {
            Files.deleteIfExists(getUtils().getAssetsDirectory().resolve("hd"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        skins.invalidateAll();
        parseSkins();
        clearListeners.removeIf(this::onSkinCacheCleared);
    }

    private boolean onSkinCacheCleared(ISkinCacheClearListener callback) {
        try {
            return !callback.onSkinCacheCleared();
        } catch (Exception e) {
            logger.warn("Exception encountered calling skin listener '{}'. It will be removed.", callback.getClass().getName(), e);
            return true;
        }
    }

    public Class<? extends SkinServer> getSkinServerClass(String type) {
        return this.skinServerTypes.get(type);
    }

    public void addClearListener(ISkinCacheClearListener listener) {
        clearListeners.add(listener);
    }

    public void addSkinServer(SkinServer skinServer) {
        skinServers.add(skinServer);
    }

    public void addSkinModifier(ISkinModifier modifier) {
        skinModifiers.add(modifier);
    }

    public void addSkinParser(ISkinParser parser) {
        skinParsers.add(parser);
    }

    public Identifier getConvertedSkin(Identifier res) {
        Identifier loc = resources.getConvertedResource(res);
        return loc == null ? res : loc;
    }

    public void convertSkin(ISkinModifier.IDrawer drawer) {
        for (ISkinModifier skin : skinModifiers) {
            skin.convertSkin(drawer);
        }
    }

    public void parseSkins() {
        MinecraftClient mc = MinecraftClient.getInstance();

        Streams.concat(getNPCs(mc), getPlayers(mc))

                // filter nulls
                .filter(Objects::nonNull)
                .map(INetworkPlayerInfo.class::cast)
                .distinct()

                // and clear skins
                .forEach(INetworkPlayerInfo::reloadTextures);

    }

    private Stream<PlayerListEntry> getNPCs(MinecraftClient mc) {
        return MoreStreams.ofNullable(mc.world)
                .flatMap(w -> w.getPlayers().stream())
                .filter(AbstractClientPlayerEntity.class::isInstance)
                .map(MixinClientPlayer.class::cast)
                .map(MixinClientPlayer::getBackingClientData);
    }

    private Stream<PlayerListEntry> getPlayers(MinecraftClient mc) {
        return MoreStreams.ofNullable(mc.getNetworkHandler())
                .flatMap(a -> a.getPlayerList().stream());
    }

    public CompletableFuture<Void> parseSkin(GameProfile profile, Type type, Identifier resource, MinecraftProfileTexture texture) {

        return CallableFutures.scheduleTask(() -> {

            // grab the metadata object via reflection. Object is live.
            Map<String, String> metadata = ProfileTextureUtil.getMetadata(texture);

            boolean wasNull = metadata == null;

            if (wasNull) {
                metadata = new HashMap<>();
            } else if (metadata.containsKey("model")) {
                // try to reset the model.
                metadata.put("model", VanillaModels.of(metadata.get("model")));
            }

            for (ISkinParser parser : skinParsers) {
                try {
                    parser.parse(profile, type, resource, metadata);
                } catch (Throwable t) {
                    logger.error("Exception thrown while parsing skin: ", t);
                }
            }

            if (wasNull && !metadata.isEmpty()) {
                ProfileTextureUtil.setMetadata(texture, metadata);
            }

        });
    }
}
