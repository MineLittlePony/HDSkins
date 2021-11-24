package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.event.ScreenInitCallback;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.dummy.EquipmentList;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.profile.ProfileRepository;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
import com.minelittlepony.hdskins.server.SkinServerList;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.ResourceType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class HDSkins implements ClientModInitializer {
    public static final String MOD_ID = "hdskins";

    public static final Logger LOGGER = LogManager.getLogger();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    private final HDConfig config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));

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
        config.load();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(resources);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(skinServerList);
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(equipmentList);
        ScreenInitCallback.EVENT.register(this::onScreenInit);

        FabricLoader.getInstance().getEntrypoints("hdskins", ClientModInitializer.class).forEach(ClientModInitializer::onInitializeClient);
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (screen instanceof TitleScreen) {
            Button button = buttons.addButton(new Button(screen.width - 50, screen.height - 50, 20, 20))
                .onClick(sender -> MinecraftClient.getInstance().setScreen(GuiSkins.create(screen, skinServerList)));
            button.getStyle()
                    .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)
                    .setTooltip("hdskins.manager", 0, 10);
            button.y = screen.height - 50; // ModMenu;
        }
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public ProfileRepository getProfileRepository() {
        return repository;
    }

    public SkinServerList getSkinServerList() {
        return skinServerList;
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }
}
