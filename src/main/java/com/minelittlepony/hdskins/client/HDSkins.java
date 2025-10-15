package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.client.gui.VisibilityMode;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.event.ScreenInitCallback;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.gui.PlayerPreviewSpecialGuiElementRenderer;
import com.minelittlepony.hdskins.client.gui.SettingsScreen;
import com.minelittlepony.hdskins.client.profile.SkinLoader;
import com.minelittlepony.hdskins.client.resources.EquipmentList;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
import com.minelittlepony.hdskins.server.SkinServerList;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HDSkins implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    public static Identifier id(String name) {
        return HDSkinsServer.id(name);
    }

    private final HDConfig config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));
    private final EquipmentList equipmentList = new EquipmentList();
    private final SkinResourceManager resources = new SkinResourceManager();
    private final SkinLoader repository = new SkinLoader();

    private final PrioritySorter skinPrioritySorter = new PrioritySorter();

    private boolean configDirty;

    public HDSkins() {
        instance = this;
    }

    public HDConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config.load();

        SpecialGuiElementRegistry.register(PlayerPreviewSpecialGuiElementRenderer::new);

        HDSkinsServer.getInstance().setSessionService(() -> MinecraftClient.getInstance().getApiServices().sessionService());
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(SkinResourceManager.ID, resources);
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(SkinServerList.SKIN_SERVERS, HDSkinsServer.getInstance().getServers());
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(EquipmentList.EQUIPMENT, equipmentList);
        ScreenInitCallback.EVENT.register(this::onScreenInit);

        FabricLoader.getInstance().getEntrypoints("hdskins", ClientModInitializer.class).forEach(ClientModInitializer::onInitializeClient);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        config.onChangedExternally(config -> configDirty = true);
    }

    private void onTick(MinecraftClient client) {
        if (configDirty && client.currentScreen instanceof SettingsScreen screen) {
            screen.init(client, screen.width, screen.height);
        }
        configDirty = false;
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (!(screen instanceof TitleScreen)) {
            return;
        }
        VisibilityMode visibility = config.pantsButtonVisibility.get();
        if (visibility == VisibilityMode.OFF || (visibility == VisibilityMode.AUTO && FabricLoader.getInstance().isModLoaded("modmenu"))) {
            return;
        }
        Button button = buttons.addButton(new Button(screen.width - 50, screen.height - 50, 20, 20))
            .onClick(sender -> MinecraftClient.getInstance().setScreen(GuiSkins.create(screen, HDSkinsServer.getInstance().getServers())));
        button.getStyle()
                .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)
                .setTooltip("hdskins.manager", 0, 10);
        button.setY(screen.height - 50); // ModMenu;
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public SkinLoader getProfileRepository() {
        return repository;
    }

    @Deprecated
    public SkinServerList getSkinServerList() {
        return HDSkinsServer.getInstance().getServers();
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }

    public PrioritySorter getSkinPrioritySorter() {
        return skinPrioritySorter;
    }
}
