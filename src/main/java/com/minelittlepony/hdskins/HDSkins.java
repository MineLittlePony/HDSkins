package com.minelittlepony.hdskins;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.common.event.ClientReadyCallback;
import com.minelittlepony.common.event.ScreenInitCallback;
import com.minelittlepony.common.util.GamePaths;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.minelittlepony.hdskins.dummy.DummyPlayer;
import com.minelittlepony.hdskins.dummy.RenderDummyPlayer;
import com.minelittlepony.hdskins.gui.GuiSkins;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.profile.ProfileRepository;
import com.minelittlepony.hdskins.resources.SkinResourceManager;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider.SkinTextureAvailableCallback;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class HDSkins implements ClientModInitializer {
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

    private final SkinResourceManager resources = new SkinResourceManager();
    private final ProfileRepository repository = new ProfileRepository(this);

    private Function<List<SkinServer>, GuiSkins> skinsGuiFunc = GuiSkins::new;

    public HDSkins() {
        instance = this;
    }

    public AbstractConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config = Config.of(GamePaths.getConfigDirectory().resolve("hdskins.json"));

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(resources);
        EntityRendererRegistry.INSTANCE.register(DummyPlayer.class, RenderDummyPlayer::new);
        ClientReadyCallback.Handler.register();
        ClientReadyCallback.EVENT.register(this::postInit);
        ScreenInitCallback.EVENT.register(this::onScreenInit);
    }

    private void postInit(MinecraftClient client) {
        // register skin servers.
        config.skin_servers.forEach(this::addSkinServer);
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (screen instanceof TitleScreen) {
            buttons.add(new Button(screen.width - 50, screen.height - 50, 20, 20).onClick(sender -> {
                MinecraftClient.getInstance().openScreen(this.createSkinsGui());
            }).setStyle(new Style().setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)));
        }
    }

    public void setSkinsGui(Function<List<SkinServer>, GuiSkins> skinsGuiFunc) {
        Preconditions.checkNotNull(skinsGuiFunc, "skinsGuiFunc");
        this.skinsGuiFunc = skinsGuiFunc;
    }

    public GuiSkins createSkinsGui() {
        return skinsGuiFunc.apply(getSkinServers());
    }

    public void fetchAndLoadSkins(GameProfile profile, SkinTextureAvailableCallback callback) {
        repository.fetchSkins(profile, callback);
    }

    public void clearSkinCache() {
        logger.info("Clearing local player skin cache");
        repository.clear();
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

    public SkinResourceManager getResourceManager() {
        return resources;
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

}
