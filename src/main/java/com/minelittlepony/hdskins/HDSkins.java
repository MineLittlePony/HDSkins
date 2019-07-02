package com.minelittlepony.hdskins;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.minelittlepony.common.client.IModUtilities;
import com.minelittlepony.common.util.TextureConverter;
import com.minelittlepony.hdskins.dummy.DummyPlayer;
import com.minelittlepony.hdskins.dummy.RenderDummyPlayer;
import com.minelittlepony.hdskins.gui.GuiSkins;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.profile.ProfileRepository;
import com.minelittlepony.hdskins.profile.skin.SkinParsingService;
import com.minelittlepony.hdskins.resources.SkinResourceManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
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

    private final List<SkinServer> builtInSkinServers = Lists.newArrayList();

    private AbstractConfig config;

    @Deprecated
    private final SkinResourceManager resources = new SkinResourceManager();
    private final SkinParsingService skinParser = new SkinParsingService();
    private final ProfileRepository repository = new ProfileRepository(this);

    private Function<List<SkinServer>, GuiSkins> skinsGuiFunc = GuiSkins::new;

    private final IModUtilities utils;

    public HDSkins(IModUtilities utils) {
        instance = this;
        this.utils = utils;
        this.config = Config.of(utils.getConfigDirectory().resolve("hdskins.json"));
    }

    public IModUtilities getUtils() {
        return utils;
    }

    public AbstractConfig getConfig() {
        return config;
    }

    void postInit(MinecraftClient client) {
        ((ReloadableResourceManager) client.getResourceManager()).registerListener(resources);

        utils.addRenderer(DummyPlayer.class, RenderDummyPlayer::new);

        // register skin servers.
        config.skin_servers.forEach(this::addSkinServer);
    }

    public void setSkinsGui(Function<List<SkinServer>, GuiSkins> skinsGuiFunc) {
        Preconditions.checkNotNull(skinsGuiFunc, "skinsGuiFunc");
        this.skinsGuiFunc = skinsGuiFunc;
    }

    public GuiSkins createSkinsGui() {
        return skinsGuiFunc.apply(getSkinServers());
    }

    public void fetchAndLoadSkins(GameProfile profile, SkinTextureAvailableCallback callback) {
        repository.fetchSkins(profile, (type, location, profileTexture) -> {
            skinParser.parseSkin(profile, type, location, profileTexture);

            callback.onSkinTextureAvailable(type, location, profileTexture);
        });
    }

    public void clearSkinCache() {
        logger.info("Clearing local player skin cache");
        repository.clear();
        skinParser.execute();
        clearListeners.removeIf(this::onCacheCleared);
    }

    private boolean onCacheCleared(ISkinCacheClearListener callback) {
        try {
            return !callback.onSkinCacheCleared();
        } catch (Exception e) {
            logger.warn("Exception encountered calling skin listener '{}'. It will be removed.", callback.getClass().getName(), e);
            return true;
        }
    }

    public Map<Type, Identifier> getTextures(GameProfile profile) {
        return repository.getTextures(profile);
    }

    public SkinParsingService getSkinParser() {
        return skinParser;
    }

    public void addClearListener(ISkinCacheClearListener listener) {
        clearListeners.add(listener);
    }

    public List<SkinServer> getSkinServers() {
        return ImmutableList.copyOf(builtInSkinServers);
    }

    public void addSkinServer(SkinServer skinServer) {
        builtInSkinServers.add(skinServer);
    }

    public void addSkinModifier(TextureConverter modifier) {
        skinParser.addModifier(modifier);
    }

    public void addSkinParser(ISkinParser parser) {
        skinParser.addParser(parser);
    }

    @Deprecated
    public Identifier getConvertedSkin(Identifier res) {
        Identifier loc = resources.getConvertedResource(res);
        return loc == null ? res : loc;
    }
}
