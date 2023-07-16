package com.minelittlepony.hdskins.client.gui;

import com.google.common.base.Preconditions;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Cycler;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.element.FeatureButton;
import com.minelittlepony.hdskins.client.gui.element.FeatureCycler;
import com.minelittlepony.hdskins.client.gui.element.FeatureStyle;
import com.minelittlepony.hdskins.client.gui.element.LabelledCycler;
import com.minelittlepony.hdskins.client.gui.element.ReactiveUiElement;
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

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;

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

    private LabelledCycler btnSkinType;

    private int updateCounter = 72;

    private double lastMouseX = 0;

    private final RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(new CubeMapRenderer(getBackground()));

    protected final DualCarouselWidget previewer;
    protected final Controls controls;
    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final StatusBanner banner;
    private final FileDrop dropper;

    private final SkinUpload.Session session = new SkinUpload.Session(
            MinecraftClient.getInstance().getSession().getProfile(),
            MinecraftClient.getInstance().getSession().getAccessToken(),
            (session, serverId) -> {
                // join the session server
                client.getSessionService().joinServer(session.profile(), session.accessToken(), serverId);
            }
    );

    public GuiSkins(Screen parent, SkinServerList servers) {
        super(HD_SKINS_TITLE, parent);
        client = MinecraftClient.getInstance();
        previewer = createPreviewer();
        controls = new Controls(previewer);
        chooser = new SkinChooser(previewer);
        uploader = new SkinUploader(servers.getCycler(), previewer);
        banner = new StatusBanner(uploader);
        dropper = FileDrop.newDropEvent(paths -> paths.stream().findFirst().ifPresent(chooser::selectFile));

        uploader.addSkinTypeChangedEventListener(type -> {
            playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
        });
        uploader.addSkinUploadedEventListener((type, location, profileTexture) -> {
            playSound(SoundEvents.ENTITY_VILLAGER_YES);
        });
    }

    protected DualCarouselWidget createPreviewer() {
        return new DualCarouselWidget();
    }

    protected Identifier getBackground() {
        return new Identifier(HDSkins.MOD_ID, "textures/cubemaps/panorama");
    }

    @Override
    public void tick() {
        controls.update();
        chooser.update();
        uploader.update();

        boolean left = client.options.leftKey.isPressed();
        boolean right = client.options.rightKey.isPressed();

        if (!(left && right)) {
            if (left) {
                updateCounter -= 5;
            } else if (right) {
                updateCounter += 5;
            } else if (!isDragging()) {
                updateCounter++;
            }
        }

        children().forEach(i -> {
            if (i instanceof ReactiveUiElement r) {
                r.update();
            }
        });
    }

    @Override
    public void init() {
        dropper.subscribe();
        previewer.init(this);

        addButton(new Label(width / 2, 5)).setCentered().getStyle().setText("hdskins.manager").setColor(0xffffff);

        int typeSelectorWidth = Math.max(previewer.local.bounds.width, 200);
        addButton(btnSkinType = new LabelledCycler((width - typeSelectorWidth) / 2, previewer.local.bounds.top - 25, typeSelectorWidth, 20))
                .onUpdate(sender -> sender.setEnabled(uploader.getFeatures().contains(Feature.MODEL_TYPES)))
                .onChange(i -> {
                    List<SkinType> types = uploader.getSupportedSkinTypes().toList();
                    i %= types.size();
                    uploader.setSkinType(types.get(i));
                    return i;
                });
        setupSkinToggler();

        addButton(new FeatureButton(width / 2 - 10, height / 2 - 20, 20, 40))
            .onUpdate(sender -> {
                sender.setEnabled(uploader.canUpload(previewer.getActiveSkinType()) && chooser.hasSelection());
                sender.setLocked(!uploader.getFeatures().contains(Feature.UPLOAD_USER_SKIN));
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
                    .setTextureOffset(16, 48)).setTooltip("hdskins.options.chevy.title");

        initLocalPreviewButtons(previewer.local.bounds);
        initServerPreviewButtons(previewer.remote.bounds);

        addButton(new Button(width / 2 - 25, previewer.remote.bounds.bottom() + 10, 50, 20))
            .onClick(sender -> finish())
            .getStyle().setText("hdskins.options.close");
    }

    protected void initLocalPreviewButtons(Bounds area) {
        FeatureButton btnBrowse;
        addButton(btnBrowse = new FeatureButton(area.left, area.bottom() + 5, 50, 20))
            .onUpdate(sender -> sender.setEnabled(!chooser.pickingInProgress()))
            .onClick(sender -> chooser.openBrowsePNG(I18n.translate("hdskins.open.title")))
            .getStyle().setText("hdskins.options.browse");

        FeatureCycler clothingCycler;
        List<EquipmentSet> equipments = HDSkins.getInstance().getDummyPlayerEquipmentList().getValues().toList();
        addButton(clothingCycler = new FeatureCycler(btnBrowse.getBounds().right() + 5, btnBrowse.getBounds().top))
                .setStyles(equipments.stream()
                        .map(equipment -> new FeatureStyle(clothingCycler)
                            .setIcon(equipment.getStack())
                            .setTooltip(equipment.getTooltip(), 0, 10))
                        .toArray(Style[]::new))
                .setValue(equipments.indexOf(previewer.getEquipment()))
                .onChange(i -> {
                    previewer.setEquipment(equipments.get(i % equipments.size()));
                    GameGui.playSound(previewer.getEquipment().getSound());
                    return i;
                });

        FeatureCycler btnSkinVariant;
        List<SkinVariant> variants = previewer.getSkinVariants();
        addButton(btnSkinVariant = new FeatureCycler(clothingCycler.getBounds().right() + 5, clothingCycler.getBounds().top))
            .onUpdate(sender -> sender.setLocked(!uploader.getFeatures().contains(Feature.MODEL_VARIANTS)))
            .setStyles(variants.stream()
                    .map(variant -> new FeatureStyle(btnSkinVariant)
                            .setIcon(variant.icon())
                            .setTooltip(variant.tooltip(), 0, 10))
                    .toArray(Style[]::new))
            .setValue(previewer.getSkinVariant().map(variants::indexOf).orElse(0))
            .onChange(i -> {
                playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
                SkinVariant variant = variants.get(i % variants.size());
                uploader.setMetadataField("model", variant.name());
                previewer.setSkinVariant(variant);
                return i;
            });

        addButton(new Cycler(btnSkinVariant.getBounds().right() + 5, btnSkinVariant.getBounds().top, 20, 20))
            .setStyles(PlayerSkins.Posture.Pose.STYLES)
            .setValue(previewer.getPose().ordinal())
            .onChange(i -> {
                playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
                previewer.setPose(PlayerSkins.Posture.Pose.VALUES[i % PlayerSkins.Posture.Pose.VALUES.length]);
                return i;
            });
    }

    protected void initServerPreviewButtons(Bounds area) {
        Button serverSelector;
        addButton(serverSelector = new Button(area.right() - 20, area.bottom() + 5, 20, 20))
            .onClick(sender -> {
                uploader.cycleGateway();
                playSound(SoundEvents.ENTITY_VILLAGER_YES);
                setupSkinToggler();
                sender.getStyle().setTooltip(uploader.getGatewayText());
            })
            .getStyle().setIcon(createIcon(80, 0)).setTooltip(uploader.getGatewayText(), 0, 10);


        FeatureButton btnClearAll;
        addButton(btnClearAll = new FeatureButton(serverSelector.getBounds().left - 25, serverSelector.getBounds().top, 20, 20))
            .onUpdate(sender -> {
                sender.setEnabled(uploader.canClearAny());
                sender.setLocked(!uploader.getFeatures().contains(Feature.DELETE_USER_SKIN));
            })
            .onClick(sender -> {
                SkinType.REGISTRY.forEach(type -> {
                    if (uploader.canClear(type)) {
                        uploader.uploadSkin(StatusBanner.HD_SKINS_REQUEST, SkinUpload.delete(previewer.getActiveSkinType(), session));
                    }
                });
            })
            .getStyle().setIcon(createIcon(48, 16)).setTooltip("hdskins.options.clear_all");

        FeatureButton btnClear;
        addButton(btnClear = new FeatureButton(btnClearAll.getBounds().left - 25, btnClearAll.getBounds().top, 20, 20))
                .onUpdate(sender -> {
                    sender.setEnabled(uploader.canClear(previewer.getActiveSkinType()));
                    sender.setLocked(!uploader.getFeatures().contains(Feature.DELETE_USER_SKIN));
                })
                .onClick(sender -> {
                    if (uploader.canClear(previewer.getActiveSkinType())) {
                        uploader.uploadSkin(StatusBanner.HD_SKINS_REQUEST, SkinUpload.delete(previewer.getActiveSkinType(), session));
                    }
                })
                .getStyle().setIcon(createIcon(48, 0)).setTooltip("hdskins.options.clear");

        addButton(new FeatureButton(btnClear.getBounds().left - 25, btnClear.getBounds().top, 20, 20))
                .onUpdate(sender -> {
                    sender.setEnabled(uploader.canClear(previewer.getActiveSkinType()) && !chooser.pickingInProgress());
                    sender.setLocked(!uploader.getFeatures().contains(Feature.DOWNLOAD_USER_SKIN));
                })
                .onClick(sender -> {
                    if (uploader.canClear(previewer.getActiveSkinType())) {
                        chooser.openSavePNG(uploader, I18n.translate("hdskins.save.title"), client.getSession().getUsername());
                    }
                })
                .getStyle().setIcon(createIcon(0, 0)).setTooltip("hdskins.options.download.title");

    }

    private void setupSkinToggler() {
        List<SkinType> types = uploader.getSupportedSkinTypes().toList();
        btnSkinType
            .setStyles(types.stream().map(SkinType::getStyle).toArray(Style[]::new))
            .setValue(types.indexOf(previewer.getActiveSkinType()));
    }

    @Override
    public void removed() {
        super.removed();

        try {
            uploader.close();
        } catch (IOException e) {
            HDSkins.LOGGER.error("Could not dispose of the uploader", e);
        }

        HDSkins.getInstance().getProfileRepository().clear();

        dropper.cancel();
    }

    protected boolean canTakeEvents() {
        return !chooser.pickingInProgress() && uploader.tryClearStatus() && !banner.isVisible();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setDragging(true);
        }

        return canTakeEvents()
                && !super.mouseClicked(mouseX, mouseY, button)
                && previewer.mouseClicked(uploader, width, height, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double changeX, double changeY) {
        super.mouseDragged(mouseX, mouseY, button, changeX, changeY);

        if (canTakeEvents()) {
            updateCounter -= (lastMouseX - mouseX);
            lastMouseX = mouseX;

            return true;
        }

        lastMouseX = mouseX;

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyCode != GLFW.GLFW_KEY_SPACE && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char keyChar, int keyCode) {
        return canTakeEvents() && !chooser.pickingInProgress() && !uploader.isBusy() && super.charTyped(keyChar, keyCode);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        RenderSystem.disableCull();

        if (client.world == null) {
            panorama.render(tickDelta, 1);
        } else {
           renderBackground(context);
        }

        previewer.render(context, mouseX, mouseY, updateCounter, tickDelta, chooser, uploader);
        super.render(context, mouseX, mouseY, tickDelta);
        banner.render(context, tickDelta, width, height);
    }
}
