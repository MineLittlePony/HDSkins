package com.minelittlepony.hdskins;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.event.ScreenInitCallback;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.net.SkinServerList;
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

import com.minelittlepony.hdskins.dummy.DummyPlayer;
import com.minelittlepony.hdskins.dummy.DummyPlayerRenderer;
import com.minelittlepony.hdskins.dummy.EquipmentList;
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

    private HDConfig config;

    private final SkinServerList skinServerList = new SkinServerList();
    private final EquipmentList equipmentList = new EquipmentList();
    private final SkinResourceManager resources = new SkinResourceManager();
    private final ProfileRepository repository = new ProfileRepository(this);

    public HDSkins() {
        instance = this;
    }

    public HDConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));
        config.load();

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(resources);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(skinServerList);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(equipmentList);
        EntityRendererRegistry.INSTANCE.register(DummyPlayer.class, DummyPlayerRenderer::new);

        ScreenInitCallback.EVENT.register(this::onScreenInit);
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (screen instanceof TitleScreen) {
            Button button = buttons.add(new Button(screen.width - 50, screen.height - 50, 20, 20))
                .onClick(sender -> MinecraftClient.getInstance().openScreen(getSkinServerList().createSkinsGui(screen)));
            button.getStyle()
                    .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb);
            button.y = screen.height - 50; // ModMenu;
        }
    }

    public void fetchAndLoadSkins(GameProfile profile, SkinTextureAvailableCallback callback) {
        repository.fetchSkins(profile, callback);
    }

    public void clearSkinCache() {
        logger.info("Clearing local player skin cache");
        repository.clear();
        SkinCacheClearCallback.EVENT.invoker().onSkinCacheCleared();
    }

    public Map<Type, Identifier> getTextures(GameProfile profile) {
        return repository.getTextures(profile);
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public SkinServerList getSkinServerList() {
        return skinServerList;
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }
}
