package com.minelittlepony.hdskins.client.gui;

import com.google.common.base.Preconditions;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Cycler;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDrop;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins.Posture.SkinVariant;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

/**
 * The top-level interface for the skin uploader.
 */
public class GuiSkins extends GameGui {
    public static final Identifier WIDGETS_TEXTURE = new Identifier("hdskins", "textures/gui/widgets.png");
    public static final Text HD_SKINS_TITLE = Text.translatable("hdskins.gui.title");
    public static final Text HD_SKINS_OPTION_DISABLED_DESC = Text.translatable("hdskins.warning.disabled.description");

    private static BiFunction<Screen, SkinServerList, GuiSkins> skinsGuiFunc = GuiSkins::new;

    public static void setSkinsGui(BiFunction<Screen, SkinServerList, GuiSkins> skinsGuiFunc) {
        Preconditions.checkNotNull(skinsGuiFunc, "skinsGuiFunc");
        GuiSkins.skinsGuiFunc = skinsGuiFunc;
    }

    public static GuiSkins create(Screen parent, SkinServerList servers) {
        return skinsGuiFunc.apply(parent, servers);
    }

    public static TextureSprite createIcon(int u, int v) {
        return new TextureSprite()
                .setTexture(WIDGETS_TEXTURE)
                .setPosition(2, 2)
                .setSize(16, 16)
                .setTextureOffset(u, v);
    }

    public static Tooltip createFeatureTooltip(Tooltip originalTooltip, BooleanSupplier isEnabled) {
        Tooltip disabledTooltip = Tooltip.of(
                Text.translatable("hdskins.warning.disabled.title",
                        originalTooltip.getString(),
                        GuiSkins.HD_SKINS_OPTION_DISABLED_DESC
                )
        );
        return () -> !isEnabled.getAsBoolean() ? disabledTooltip.getLines() : originalTooltip.getLines();
    }

    @Nullable
    private Cycler btnSkinType;

    private final RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(new CubeMapRenderer(getBackground()));

    protected final DualCarouselWidget previewer;
    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final StatusBanner banner;
    private final FileDrop dropper;

    private final SkinUpload.Session session = new SkinUpload.Session(
            MinecraftClient.getInstance().getGameProfile(),
            MinecraftClient.getInstance().getSession().getAccessToken(),
            SkinUpload.Session.validator((session, serverId) -> {
                // join the session server
                client.getSessionService().joinServer(session.profile().getId(), session.accessToken(), serverId);
            })
    );

    public GuiSkins(Screen parent, SkinServerList servers) {
        super(HD_SKINS_TITLE, parent);
        client = MinecraftClient.getInstance();
        previewer = createPreviewer();
        chooser = new SkinChooser(previewer);
        uploader = new SkinUploader(servers.getCycler(), previewer, session);
        banner = new StatusBanner(uploader);
        dropper = FileDrop.newDropEvent(paths -> paths.stream().findFirst().ifPresent(chooser::selectFile));

        uploader.addSkinTypeChangedEventListener(type -> {
            playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
        });
        uploader.addSkinLoadedEventListener((type, location, profileTexture) -> {
            playSound(SoundEvents.ENTITY_VILLAGER_YES);
            setupSkinToggler();
        });
    }

    protected DualCarouselWidget createPreviewer() {
        return new DualCarouselWidget(this);
    }

    protected Identifier getBackground() {
        return new Identifier(HDSkins.MOD_ID, "textures/cubemaps/panorama");
    }

    @Override
    public void tick() {
        previewer.update();
        chooser.update();
        uploader.update();
    }

    @Override
    public void init() {
        previewer.init();

        addButton(new Label(width / 2, 5)).setCentered().getStyle().setText("hdskins.manager").setColor(0xffffff);

        int typeSelectorWidth = Math.max(previewer.local.bounds.width, 200);
        addButton(btnSkinType = new Cycler((width - typeSelectorWidth) / 2, previewer.local.bounds.top - 25, typeSelectorWidth, 20))
                .onChange(i -> {
                    List<SkinType> types = uploader.getSupportedSkinTypes().toList();
                    i %= types.size();
                    uploader.setSkinType(types.get(i));
                    return i;
                })
                .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.MODEL_TYPES)));
        setupSkinToggler();

        addButton(new Button(width / 2 - 10, height / 2 - 20, 20, 40))
            .onUpdate(sender -> {
                sender.setEnabled(uploader.canUpload(previewer.getActiveSkinType()) && chooser.hasSelection());
            })
            .setEnabled(uploader.canUpload(previewer.getActiveSkinType()) && chooser.hasSelection())
            .onClick(sender -> {
                if (uploader.canUpload(previewer.getActiveSkinType()) && chooser.hasSelection()) {
                    uploader.uploadSkin(StatusBanner.HD_SKINS_UPLOAD, SkinUpload.create(chooser.getSelection(), previewer.getActiveSkinType(), uploader.getMetadata(), session));
                }
            })
            .getStyle().setIcon(new TextureSprite()
                    .setTexture(WIDGETS_TEXTURE)
                    .setPosition(2, 11)
                    .setSize(16, 16)
                    .setTextureOffset(16, 48)).setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.chevy.title"), () -> uploader.getFeatures().contains(Feature.UPLOAD_USER_SKIN)));

        initLocalPreviewButtons(previewer.local.bounds);
        initServerPreviewButtons(previewer.remote.bounds);

        addButton(new Button(width / 2 - 25, previewer.remote.bounds.bottom() + 10, 50, 20))
            .onClick(sender -> finish())
            .getStyle().setText("hdskins.options.close");
    }

    protected void initLocalPreviewButtons(Bounds area) {
        area = addButton(new Button(area.left, area.bottom() + 5, 50, 20))
            .onUpdate(sender -> sender.setEnabled(!chooser.pickingInProgress()))
            .onClick(sender -> chooser.openBrowsePNG(I18n.translate("hdskins.open.title")))
            .styled(s -> s.setText("hdskins.options.browse"))
            .getBounds();

        List<EquipmentSet> equipments = HDSkins.getInstance().getDummyPlayerEquipmentList().getValues().toList();
        area = addButton(new Cycler(area.right() + 5, area.top, 20, 20))
                .setStyles(equipments.stream()
                        .map(equipment -> new Style()
                            .setIcon(equipment.getStack())
                            .setTooltip(equipment.getTooltip(), 0, 10))
                        .toArray(Style[]::new))
                .setValue(Math.max(0, equipments.indexOf(previewer.getEquipment())))
                .onChange(i -> {
                    previewer.setEquipment(equipments.get(i % equipments.size()));
                    GameGui.playSound(previewer.getEquipment().getSound());
                    return i;
                })
                .getBounds();

        List<SkinVariant> variants = previewer.getSkinVariants();
        area = addButton(new Cycler(area.right() + 5, area.top, 20, 20))
            .setStyles(variants.stream()
                    .map(variant -> new Style()
                            .setIcon(variant.icon())
                            .setTooltip(createFeatureTooltip(Tooltip.of(variant.tooltip()), () -> uploader.getFeatures().contains(Feature.MODEL_VARIANTS)))
                            .setTooltipOffset(0, 10))
                    .toArray(Style[]::new))
            .setValue(Math.max(0, previewer.getSkinVariant().map(variants::indexOf).orElse(0)))
            .onChange(i -> {
                playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
                SkinVariant variant = variants.get(i % variants.size());
                uploader.setMetadataField("model", variant.name());
                previewer.setSkinVariant(variant);
                return i;
            })
            .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.MODEL_VARIANTS)))
            .getBounds();

        addButton(new Cycler(area.right() + 5, area.top, 20, 20))
            .setStyles(PlayerSkins.Posture.Pose.STYLES)
            .setValue(previewer.getPose().ordinal())
            .onChange(i -> {
                playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
                previewer.setPose(PlayerSkins.Posture.Pose.VALUES[i % PlayerSkins.Posture.Pose.VALUES.length]);
                return i;
            });
    }

    protected void initServerPreviewButtons(Bounds area) {
        area = addButton(new Button(area.right() - 20, area.bottom() + 5, 20, 20))
            .onClick(sender -> {
                uploader.cycleGateway();
                playSound(SoundEvents.ENTITY_VILLAGER_YES);
                sender.getStyle().setTooltip(uploader.getGatewayText());
            })
            .styled(s -> s.setIcon(createIcon(80, 0)).setTooltip(uploader.getGatewayText(), 0, 10))
            .getBounds();

        area = addButton(new Button(area.left - 25, area.top, 20, 20))
            .onUpdate(sender -> sender.setEnabled(uploader.canClearAny()))
            .onClick(sender -> {
                SkinType.REGISTRY.forEach(type -> {
                    if (uploader.canClear(type)) {
                        uploader.uploadSkin(StatusBanner.HD_SKINS_REQUEST, SkinUpload.delete(previewer.getActiveSkinType(), session));
                    }
                });
            })
            .styled(s -> s
                    .setIcon(createIcon(48, 16))
                    .setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.clear_all"), () -> uploader.getFeatures().contains(Feature.DELETE_USER_SKIN))))
            .getBounds();

        area = addButton(new Button(area.left - 25, area.top, 20, 20))
                .onUpdate(sender -> sender.setEnabled(uploader.canClear(previewer.getActiveSkinType())))
                .onClick(sender -> {
                    if (uploader.canClear(previewer.getActiveSkinType())) {
                        uploader.uploadSkin(StatusBanner.HD_SKINS_REQUEST, SkinUpload.delete(previewer.getActiveSkinType(), session));
                    }
                })
                .styled(s -> s
                        .setIcon(createIcon(48, 0))
                        .setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.clear"), () -> uploader.getFeatures().contains(Feature.DELETE_USER_SKIN))))
                .getBounds();

        addButton(new Button(area.left - 25, area.top, 20, 20))
                .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.DOWNLOAD_USER_SKIN) && uploader.canClear(previewer.getActiveSkinType()) && !chooser.pickingInProgress()))
                .onClick(sender -> {
                    if (uploader.canClear(previewer.getActiveSkinType())) {
                        chooser.openSavePNG(uploader, I18n.translate("hdskins.save.title"), client.getSession().getUsername());
                    }
                })
                .getStyle()
                    .setIcon(createIcon(0, 0))
                    .setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.download.title"), () -> uploader.getFeatures().contains(Feature.DOWNLOAD_USER_SKIN)));

    }

    private void setupSkinToggler() {
        if (btnSkinType != null) {
            List<SkinType> types = uploader.getSupportedSkinTypes().toList();
            btnSkinType
                .setStyles(types.stream().map(SkinType::getStyle).toArray(Style[]::new))
                .setValue(Math.max(0, types.indexOf(previewer.getActiveSkinType())));
        }
    }

    @Override
    public void close() {
        super.close();

        try {
            uploader.close();
        } catch (IOException e) {
            HDSkins.LOGGER.error("Could not dispose of the uploader", e);
        }

        HDSkins.getInstance().getProfileRepository().clear();
    }

    @Override
    public void removed() {
        dropper.cancel();
    }

    @Override
    public void onDisplayed() {
        dropper.subscribe();
    }

    protected boolean canTakeEvents() {
        return !chooser.pickingInProgress() && uploader.tryClearStatus() && !banner.isVisible();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return canTakeEvents()
                && !super.mouseClicked(mouseX, mouseY, button)
                && previewer.mouseClicked(uploader, width, height, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double changeX, double changeY) {
        return canTakeEvents()
                && previewer.mouseDragged(mouseX, mouseY, button, changeX, changeY)
                && super.mouseDragged(mouseX, mouseY, button, changeX, changeY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (modifiers == (GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL) && keyCode == GLFW.GLFW_KEY_R) {
            client.reloadResources();
            return true;
        }
        return keyCode != GLFW.GLFW_KEY_SPACE
                && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char keyChar, int keyCode) {
        return canTakeEvents()
                && !chooser.pickingInProgress()
                && !uploader.isBusy()
                && super.charTyped(keyChar, keyCode);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        if (client.world == null) {
            panorama.render(tickDelta, 1);
        } else {
            renderInGameBackground(context);
        }
        previewer.render(context, mouseX, mouseY, tickDelta, chooser, uploader);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        RenderSystem.disableCull();
        super.render(context, mouseX, mouseY, tickDelta);
        banner.render(context, tickDelta, width, height);
    }
}
