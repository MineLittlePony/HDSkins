package com.minelittlepony.hdskins.client.gui;

import com.google.common.base.Preconditions;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Cycler;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.element.Selector;
import com.minelittlepony.common.client.gui.sprite.ItemStackSprite;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDrop;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins.Posture.SkinVariant;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.Panorama;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.CubeMapTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

/**
 * The top-level interface for the skin uploader.
 */
public class GuiSkins extends GameGui {
    public static final Identifier WIDGETS_TEXTURE = HDSkins.id("textures/gui/widgets.png");
    public static final Identifier PANORAMA_TEXTURE = HDSkins.id("textures/cubemaps/panorama");
    public static final Component HD_SKINS_TITLE = Component.translatable("hdskins.gui.title");
    public static final Component HD_SKINS_OPTION_DISABLED_DESC = Component.translatable("hdskins.warning.disabled.description");

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
                Component.translatable("hdskins.warning.disabled.title",
                        originalTooltip.getString(),
                        GuiSkins.HD_SKINS_OPTION_DISABLED_DESC
                )
        );
        return () -> !isEnabled.getAsBoolean() ? disabledTooltip.getLines() : originalTooltip.getLines();
    }

    private final Identifier background = getBackground();
    private final CubeMap cubemap = new CubeMap(background);
    private final Panorama panorama = new Panorama();

    protected final DualCarouselWidget<?> previewer;
    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final StatusBanner banner;
    private final FileDrop dropper;

    private final SkinUpload.Session session = new SkinUpload.Session(
            Minecraft.getInstance().getGameProfile(),
            Minecraft.getInstance().getUser().getAccessToken(),
            SkinUpload.Session.validator((session, serverId) -> {
                // join the session server
                minecraft.services().sessionService().joinServer(session.profile().id(), session.accessToken(), serverId);
            })
    );

    @Nullable
    private Selector<SkinType> typeSelector;

    public GuiSkins(Screen parent, SkinServerList servers) {
        super(HD_SKINS_TITLE, parent);
        previewer = createPreviewer();
        chooser = new SkinChooser(previewer);
        uploader = new SkinUploader(servers.getCycler(), previewer, session);
        banner = new StatusBanner(uploader);
        dropper = FileDrop.newDropEvent(paths -> paths.stream().findFirst().ifPresent(chooser::selectFile));

        uploader.addSkinTypeChangedEventListener(_ -> {
            playSound(SoundEvents.BREWING_STAND_BREW);
        });
        uploader.addSkinLoadedEventListener((_, _, _) -> {
            playSound(SoundEvents.VILLAGER_YES);
            if (typeSelector != null) {
                typeSelector.setValue(previewer.getActiveSkinType());
            }
        });
        // ensure faces are loaded
        minecraft.getTextureManager().register(background, new CubeMapTexture(background));
    }

    protected DualCarouselWidget<?> createPreviewer() {
        return new DualCarouselWidget<>(this) {
            @Override
            protected PlayerBodyWidget<AvatarRenderState> createEntity(PlayerSkins<?> skins) {
                return new PlayerBodyWidget<>(skins, new AvatarRenderState());
            }
        };
    }

    protected Identifier getBackground() {
        return PANORAMA_TEXTURE;
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
        addButton(new Selector<>((width - typeSelectorWidth) / 2, previewer.local.bounds.top - 25, typeSelectorWidth, 20, SkinType.SKIN, type -> {
            if (type.isUnsupported()) {
                return new Style()
                        .setIcon(new TextureSprite()
                                .setTexture(SkinType.UNKNOWN.icon())
                                .setPosition(2, 2)
                                .setSize(16, 16)
                                .setTextureSize(16, 16))
                        .setText(Component.translatable("skin_type.hdskins.unknown", type.getId().toString()))
                        .setTooltip(type.getId().toString(), 0, 10);
            }

            return new Style()
                    .setIcon(Minecraft.getInstance().getResourceManager().getResource(type.icon()).isEmpty()
                            ? new ItemStackSprite().setStack(type.iconStack().orElseThrow()) // TODO:
                            : new TextureSprite().setTexture(type.icon()).setPosition(2, 2).setSize(16, 16).setTextureSize(16, 16))
                    .setText(Component.translatable("hdskins.skin_type", Component.translatable(Util.makeDescriptionId("skin_type", type.getId()))))
                    .setTooltip(type.getId().toString(), 0, 10);
        })).setValue(previewer.getActiveSkinType())
                .onChange(type -> {
                    List<SkinType> types = uploader.getSupportedSkinTypes().toList();
                    int index = types.indexOf(type);
                    type = index < 0 ? SkinType.SKIN : types.get((index + 1) % types.size());
                    uploader.setSkinType(type);
                    uploader.scheduleReload();
                    return type;
                })
                .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.MODEL_TYPES)));

        addButton(new Button(width / 2 - 10, height / 2 - 20, 20, 40))
            .onUpdate(sender -> {
                sender.setEnabled(uploader.canUpload(previewer.getActiveSkinType()) && chooser.hasSelection());
            })
            .setEnabled(uploader.canUpload(previewer.getActiveSkinType()) && chooser.hasSelection())
            .onClick(_ -> {
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
            .onClick(_ -> finish())
            .getStyle().setText("hdskins.options.close");
    }

    protected void initLocalPreviewButtons(Bounds area) {
        area = addButton(new Button(area.left, area.bottom() + 5, 50, 20))
            .onUpdate(sender -> sender.setEnabled(!chooser.pickingInProgress()))
            .onClick(_ -> chooser.openBrowsePNG(I18n.get("hdskins.open.title")))
            .styled(s -> s.setText("hdskins.options.browse"))
            .getBounds();

        area = addButton(new Selector<>(area.right() + 5, area.top, 20, 20, HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault(), equipment -> new Style()
                    .setIcon(equipment.getStack())
                    .setTooltip(equipment.getTooltip(), 0, 10)))
                .setValue(previewer.getEquipment())
                .onChange(value -> {
                    List<EquipmentSet> equipments = HDSkins.getInstance().getDummyPlayerEquipmentList().getValues().toList();
                    int index = equipments.indexOf(value);
                    value = index < 0 ? value : equipments.get((index + 1) % equipments.size());

                    previewer.setEquipment(value);
                    GameGui.playSound(value.getSound());
                    return value;
                })
                .getBounds();

        area = addButton(new Selector<>(area.right() + 5, area.top, 20, 20, SkinVariant.DEFAULT, variant -> new Style()
                .setIcon(variant.icon())
                .setTooltip(createFeatureTooltip(Tooltip.of(variant.tooltip()), () -> uploader.getFeatures().contains(Feature.MODEL_VARIANTS)))
                .setTooltipOffset(0, 10)))
            .setValue(previewer.getSkinVariant().orElse(SkinVariant.DEFAULT))
            .onChange(variant -> {
                List<SkinVariant> variants = previewer.getSkinVariants();
                int index = variants.indexOf(variant);
                variant = index < 0 ? variant : variants.get((index + 1) % variants.size());
                playSound(SoundEvents.BREWING_STAND_BREW);
                uploader.setMetadataField("model", variant.name());
                previewer.setSkinVariant(variant);
                return variant;
            })
            .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.MODEL_VARIANTS)))
            .getBounds();

        addButton(new Cycler(area.right() + 5, area.top, 20, 20))
            .setStyles(PlayerSkins.Posture.Pose.STYLES)
            .setValue(previewer.getPose().ordinal())
            .onChange(i -> {
                playSound(SoundEvents.BREWING_STAND_BREW);
                previewer.setPose(PlayerSkins.Posture.Pose.VALUES[i % PlayerSkins.Posture.Pose.VALUES.length]);
                return i;
            });
    }

    protected void initServerPreviewButtons(Bounds area) {
        area = addButton(new Button(area.right() - 16, area.bottom() + 5, 16, 20))
            .onClick(sender -> {
                uploader.cycleGateway();
                playSound(SoundEvents.VILLAGER_YES);
                sender.getStyle().setTooltip(uploader.getGatewayText());
            })
            .styled(s -> s.setIcon(createIcon(81, 16)).setTooltip(Tooltip.of(uploader.getGatewayText(), 400)).setTooltipOffset(0, 10))
            .getBounds();

        area = addButton(new Button(area.left - 19, area.top, 20, 20))
            .onClick(_ -> minecraft.setScreen(new SettingsScreen(this, panorama)))
            .styled(s -> s.setIcon(createIcon(80, 0)).setTooltip("options.title", 0, 10))
            .getBounds();

        area = addButton(new Button(area.left - 25, area.top, 20, 20))
            .onUpdate(sender -> sender.setEnabled(uploader.canClearAny()))
            .onClick(_ -> {
                SkinType.REGISTRY.forEach(type -> {
                    uploader.uploadSkin(StatusBanner.HD_SKINS_REQUEST, SkinUpload.delete(previewer.getActiveSkinType(), session));
                });
            })
            .styled(s -> s
                    .setIcon(createIcon(48, 16))
                    .setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.clear_all"), () -> uploader.getFeatures().contains(Feature.DELETE_USER_SKIN))))
            .getBounds();

        area = addButton(new Button(area.left - 25, area.top, 20, 20))
                .onUpdate(sender -> sender.setEnabled(uploader.canClear(previewer.getActiveSkinType())))
                .onClick(_ -> {
                    if (uploader.canClear(previewer.getActiveSkinType())) {
                        uploader.uploadSkin(StatusBanner.HD_SKINS_REQUEST, SkinUpload.delete(previewer.getActiveSkinType(), session));
                    }
                })
                .styled(s -> s
                        .setIcon(createIcon(48, 0))
                        .setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.clear"), () -> uploader.getFeatures().contains(Feature.DELETE_USER_SKIN))))
                .getBounds();

        addButton(new Button(area.left - 25, area.top, 20, 20))
                .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.DOWNLOAD_USER_SKIN) && uploader.hasUploaded(previewer.getActiveSkinType()) && !chooser.pickingInProgress()))
                .onClick(_ -> {
                    if (uploader.hasUploaded(previewer.getActiveSkinType())) {
                        chooser.openSavePNG(uploader, I18n.get("hdskins.save.title"), minecraft.getUser().getName());
                    }
                })
                .getStyle()
                    .setIcon(createIcon(0, 0))
                    .setTooltip(createFeatureTooltip(Tooltip.of("hdskins.options.download.title"), () -> uploader.getFeatures().contains(Feature.DOWNLOAD_USER_SKIN)));

    }

    @Override
    public void removed() {
        dropper.cancel();
    }

    @Override
    public void onClose() {
        super.onClose();
        try {
            uploader.close();
        } catch (IOException e) {
            HDSkins.LOGGER.error("Could not dispose of the uploader", e);
        }
        HDSkins.getInstance().getProfileRepository().clear();
    }

    @Override
    public void added() {
        dropper.subscribe();
        uploader.scheduleReload();
    }

    protected boolean canTakeEvents() {
        return !chooser.pickingInProgress() && uploader.tryClearStatus() && !banner.isVisible();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return canTakeEvents()
                && !super.mouseClicked(click, doubled)
                && previewer.mouseClicked(uploader, width, height, click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double changeX, double changeY) {
        return canTakeEvents()
                && previewer.mouseDragged(click, changeX, changeY)
                && super.mouseDragged(click, changeX, changeY);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.hasAltDown() && input.hasControlDown() && input.key() == InputConstants.KEY_R) {
            minecraft.reloadResourcePacks();
            return true;
        }
        return !input.isConfirmation() && super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        return canTakeEvents()
                && !chooser.pickingInProgress()
                && !uploader.isBusy()
                && super.charTyped(input);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        super.extractBackground(context, mouseX, mouseY, tickDelta);
        previewer.extractRenderState(context, mouseX, mouseY, tickDelta, chooser, uploader);
    }

    @Override
    protected void extractPanorama(GuiGraphicsExtractor context, float tickDelta) {
        panorama.extractRenderState(context, width, height, panoramaShouldSpin());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(context, mouseX, mouseY, tickDelta);
        banner.extractRenderState(context, tickDelta, width, height);
    }
}
