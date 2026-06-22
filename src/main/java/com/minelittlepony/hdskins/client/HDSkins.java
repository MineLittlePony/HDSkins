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
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.FriendsButton;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

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

        PictureInPictureRendererRegistry.register(PlayerPreviewSpecialGuiElementRenderer::new);

        HDSkinsServer.getInstance().setSessionService(() -> Minecraft.getInstance().services().sessionService());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(SkinResourceManager.ID, resources);
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(SkinServerList.SKIN_SERVERS, HDSkinsServer.getInstance().getServers());
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(EquipmentList.EQUIPMENT, equipmentList);
        ScreenInitCallback.EVENT.register(this::onScreenInit);

        FabricLoader.getInstance().getEntrypoints("hdskins", ClientModInitializer.class).forEach(ClientModInitializer::onInitializeClient);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        config.onChangedExternally(_ -> configDirty = true);
    }

    private void onTick(Minecraft client) {
        if (configDirty && client.gui.screen() instanceof SettingsScreen screen) {
            screen.init(screen.width, screen.height);
        }
        configDirty = false;
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (!(screen instanceof TitleScreen || screen instanceof PauseScreen)) {
            return;
        }
        VisibilityMode visibility = config.pantsButtonVisibility.get();
        if (visibility == VisibilityMode.OFF || (visibility == VisibilityMode.AUTO && FabricLoader.getInstance().isModLoaded("modmenu"))) {
            return;
        }

        Vec3i pantsButtonPosition = getPantsButtonPosition(screen);

        Button button = buttons.addButton(new Button(pantsButtonPosition.getX(), pantsButtonPosition.getY(), 20, 20))
            .onClick(_ -> Minecraft.getInstance().gui.setScreen(GuiSkins.create(screen, HDSkinsServer.getInstance().getServers())));
        button.getStyle()
                .setIcon(new ItemStackTemplate(Items.LEATHER_LEGGINGS), 0x3c5dcb)
                .setTooltip("hdskins.manager", 0, 0);
        if (pantsButtonPosition.getZ() > 0) {
            button.setX(pantsButtonPosition.getX());
            button.setY(pantsButtonPosition.getY());
        }
    }

    private Vec3i getPantsButtonPosition(Screen screen) {
        var location = config.pantsButtonLocation.get();
        return switch (location) {
            case ICON -> getEndOfListPlacement(screen)
                .orElseGet(() -> getDefaultLocation(screen));
            case REPLACE_FRIENDS -> getReplacing(screen, child -> child instanceof FriendsButton)
                .or(() -> getEndOfListPlacement(screen))
                .orElse(getDefaultLocation(screen));
            case REPLACE_LANGUAGE -> getReplacing(screen, child -> child instanceof SpriteIconButton && child.getMessage().equals(Component.translatable("options.language")))
                .or(() -> getEndOfListPlacement(screen))
                .orElse(getDefaultLocation(screen));
            case REPLACE_ACCESSIBILITY -> getReplacing(screen, child -> child instanceof SpriteIconButton
                    && (child.getMessage().equals(Component.translatable("options.accessibility")) || child.getMessage().equals(Component.translatable("accessibility.onboarding.accessibility.button"))))
                .or(() -> getEndOfListPlacement(screen))
                .orElse(getDefaultLocation(screen));
            default -> new Vec3i(location.x.pick(50, screen.width - 50), location.y.pick(50, screen.height - 50), 99);
        };
    }

    private Optional<Vec3i> getReplacing(Screen screen, Predicate<AbstractButton> predicate) {
        return screen.children().stream()
                .filter(child -> child instanceof AbstractButton button && predicate.test(button)).map(AbstractButton.class::cast)
                .findFirst()
                .map(friendsButton -> {
                    friendsButton.visible = false;
                    friendsButton.active = false;
            return new Vec3i(friendsButton.getX(), friendsButton.getY(), 0);
        });
    }

    private Optional<Vec3i> getEndOfListPlacement(Screen screen) {
        return screen.children().stream()
                .filter(child -> child instanceof FriendsButton).map(FriendsButton.class::cast)
                .findFirst()
                .map(friendsButton -> {
            var friendButtons = screen.children().stream()
                    .filter(i -> i instanceof AbstractButton sib
                            && sib.getWidth() == friendsButton.getWidth()
                            && sib.getHeight() == friendsButton.getHeight()
                            && sib.getY() == friendsButton.getY())
                    .map(AbstractButton.class::cast)
                    .map(button -> {
                        button.setX(button.getX() - 10);
                        return button;
                    })
                    .sorted(Comparator.comparing(AbstractButton::getX))
                    .toList();

            return new Vec3i(friendButtons.getLast().getRight() + 5, friendsButton.getY(), 0);
        });
    }

    private Vec3i getDefaultLocation(Screen screen) {
        return new Vec3i(screen.width - 50, screen.height - 50, 99);
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
