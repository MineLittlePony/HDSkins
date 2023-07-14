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
import com.minelittlepony.hdskins.client.FileDrop;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinChooser;
import com.minelittlepony.hdskins.client.SkinUploader;
import com.minelittlepony.hdskins.client.SkinUploader.SkinChangeListener;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.PlayerPreview;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import com.minelittlepony.hdskins.util.Edge;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class GuiSkins extends GameGui implements SkinChangeListener, FileDrop.Callback {
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

    private int updateCounter = 72;

    private Button btnBrowse;
    private FeatureButton btnUpload;
    private FeatureButton btnDownload;
    private FeatureButton btnClear;

    private FeatureCycler btnSkinVariant;

    private Cycler btnSkinType;

    private double lastMouseX = 0;

    private boolean jumpState = false;
    private boolean sneakState = false;

    protected final PlayerPreview previewer;
    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(new CubeMapRenderer(getBackground()));

    private final StatusBanner banner;

    private final FileDrop dropper = FileDrop.newDropEvent(this);

    private final Edge ctrlKey = new Edge(this::ctrlToggled, Screen::hasControlDown);
    private final Edge jumpKey = new Edge(this::jumpToggled, () -> client.options.jumpKey.isPressed());
    private final Edge sneakKey = new Edge(this::sneakToggled, () -> client.options.sneakKey.isPressed());
    private final Edge walkKey = new Edge(this::walkingToggled, () -> client.options.forwardKey.isPressed() || client.options.backKey.isPressed());

    public GuiSkins(Screen parent, SkinServerList servers) {
        super(HD_SKINS_TITLE, parent);

        client = MinecraftClient.getInstance();
        previewer = createPreviewer();
        chooser = new SkinChooser(previewer, this);
        uploader = new SkinUploader(servers, previewer, this);
        banner = new StatusBanner(uploader);
    }

    public PlayerPreview createPreviewer() {
        return new PlayerPreview();
    }

    protected Identifier getBackground() {
        return new Identifier(HDSkins.MOD_ID, "textures/cubemaps/panorama");
    }

    @Override
    public void tick() {
        KeyBinding.updatePressedStates();

        boolean left = client.options.leftKey.isPressed();
        boolean right = client.options.rightKey.isPressed();

        ctrlKey.update();
        jumpKey.update();
        sneakKey.update();
        walkKey.update();

        if (!(left && right)) {
            if (left) {
                updateCounter -= 5;
            } else if (right) {
                updateCounter += 5;
            } else if (!isDragging()) {
                updateCounter++;
            }
        }

        chooser.update();
        uploader.update();

        updateButtons();
    }

    @Override
    public void init() {
        dropper.subscribe();
        previewer.init(this);

        addButton(new Label(width / 2, 5)).setCentered().getStyle().setText("hdskins.manager").setColor(0xffffff);

        int typeSelectorWidth = Math.max(previewer.localFrameBounds.width, 200);
        addButton(btnSkinType = new LabelledCycler(
                (width - typeSelectorWidth) / 2,
                previewer.localFrameBounds.top - 25,
                typeSelectorWidth, 20
            ))
                .onChange(i -> {
                    List<SkinType> types = uploader.getSupportedSkinTypes().toList();
                    i %= types.size();
                    uploader.setSkinType(types.get(i));
                    return i;
                });
        setupSkinToggler();

        addButton(btnUpload = new FeatureButton(width / 2 - 10, height / 2 - 20, 20, 40))
            .setEnabled(uploader.canUpload() && chooser.hasSelection())
            .onClick(sender -> {
                if (uploader.canUpload() && chooser.hasSelection()) {
                    punchServer(StatusBanner.HD_SKINS_UPLOAD, chooser.getSelection());
                }
            })
            .getStyle()
            .setIcon(new TextureSprite()
                    .setTexture(WIDGETS_TEXTURE)
                    .setPosition(2, 12)
                    .setSize(16, 16)
                    .setTextureOffset(16, 48))
                .setTooltip("hdskins.options.chevy.title");

        initLocalPreviewButtons(previewer.localFrameBounds);
        initServerPreviewButtons(previewer.serverFrameBounds);

        addButton(new Button(width / 2 - 25, previewer.serverFrameBounds.bottom() + 10, 50, 20))
            .onClick(sender -> finish())
            .getStyle()
                .setText("hdskins.options.close");
    }

    protected void initLocalPreviewButtons(Bounds area) {
        addButton(btnBrowse = new Button(area.left, area.bottom() + 5, 50, 20))
            .onClick(sender -> chooser.openBrowsePNG(I18n.translate("hdskins.open.title")))
            .setEnabled(!client.getWindow().isFullscreen())
            .getStyle().setText("hdskins.options.browse");

        Button clothingCycler;
        addButton(clothingCycler = new Button(btnBrowse.getBounds().right() + 5, btnBrowse.getBounds().top, 20, 20))
                .onClick(sender -> {
                    sender.getStyle()
                        .setIcon(previewer.cycleEquipment())
                        .setTooltip(Text.translatable("hdskins.equipment", I18n.translate("hdskins.equipment." + previewer.getEquipment().getId().getPath())));
                    playSound(previewer.getEquipment().getSound());
                })
                .getStyle()
                    .setIcon(previewer.getEquipment().getStack())
                    .setTooltip(Text.translatable("hdskins.equipment", I18n.translate("hdskins.equipment." + previewer.getEquipment().getId().getPath())), 0, 10);

        addButton(btnSkinVariant = new FeatureCycler(clothingCycler.getBounds().right() + 5, clothingCycler.getBounds().top))
            .setStyles(
                    new FeatureStyle(btnSkinVariant)
                        .setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(32, 0))
                        .setTooltip(Text.translatable("hdskins.arm_style", Text.translatable("hdskins.mode.steve")), 0, 10),
                    new FeatureStyle(btnSkinVariant)
                        .setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(32, 16))
                        .setTooltip(Text.translatable("hdskins.arm_style", Text.translatable("hdskins.mode.alex")), 0, 10)
            )
            .setValue(VanillaModels.isFat(uploader.getMetadataField("model")) ? 2 : 1)
            .onChange(i -> {
                switchSkinMode(i == 1 ? VanillaModels.SLIM : VanillaModels.DEFAULT);
                return i;
            });

        addButton(new Cycler(btnSkinVariant.getBounds().right() + 5, btnSkinVariant.getBounds().top, 20, 20))
            .setStyles(
                    new Style().setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 0)).setTooltip(Text.translatable("hdskins.mode", Text.translatable("hdskins.mode.stand")), 0, 10),
                    new Style().setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 16)).setTooltip(Text.translatable("hdskins.mode", Text.translatable("hdskins.mode.sleep")), 0, 10),
                    new Style().setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 32)).setTooltip(Text.translatable("hdskins.mode", Text.translatable("hdskins.mode.ride")), 0, 10),
                    new Style().setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 48)).setTooltip(Text.translatable("hdskins.mode", Text.translatable("hdskins.mode.swim")), 0, 10),
                    new Style().setIcon(new TextureSprite()
                            .setTexture(WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 64)).setTooltip(Text.translatable("hdskins.mode", Text.translatable("hdskins.mode.riptide")), 0, 10))
            .setValue(previewer.getPose())
            .onChange(i -> {
                playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
                previewer.setPose(i);
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
            .getStyle()
                .setIcon(new TextureSprite()
                        .setTexture(WIDGETS_TEXTURE)
                        .setPosition(2, 2)
                        .setSize(16, 16)
                        .setTextureOffset(80, 0))
                .setTooltip(uploader.getGatewayText(), 0, 10);


        Button btnClearAll;
        addButton(btnClearAll = new FeatureButton(serverSelector.getBounds().left - 25, serverSelector.getBounds().top, 20, 20))
            .onClick(sender -> {
                if (uploader.canClear()) {
                    punchServer(StatusBanner.HD_SKINS_REQUEST, null);
                }
            })
            .getStyle()
                .setIcon(new TextureSprite()
                    .setTexture(WIDGETS_TEXTURE)
                    .setPosition(2, 2)
                    .setSize(16, 16)
                    .setTextureOffset(48, 16))
                .setTooltip("hdskins.options.clear_all");

        addButton(btnClear = new FeatureButton(btnClearAll.getBounds().left - 25, btnClearAll.getBounds().top, 20, 20))
                .setEnabled(uploader.canClear())
                .onClick(sender -> {
                    if (uploader.canClear()) {
                        punchServer(StatusBanner.HD_SKINS_REQUEST, null);
                    }
                })
                .getStyle()
                    .setIcon(new TextureSprite()
                        .setTexture(WIDGETS_TEXTURE)
                        .setPosition(2, 2)
                        .setSize(16, 16)
                        .setTextureOffset(48, 0))
                    .setTooltip("hdskins.options.clear");


        addButton(btnDownload = new FeatureButton(btnClear.getBounds().left - 25, btnClear.getBounds().top, 20, 20))
                .setEnabled(uploader.canClear())
                .onClick(sender -> {
                    if (uploader.canClear()) {
                        chooser.openSavePNG(uploader, I18n.translate("hdskins.save.title"), client.getSession().getUsername());
                    }
                })
                .getStyle()
                    .setIcon(new TextureSprite()
                        .setTexture(WIDGETS_TEXTURE)
                        .setPosition(2, 2)
                        .setSize(16, 16)
                        .setTextureOffset(0, 0))
                    .setTooltip("hdskins.options.download.title");

    }

    private void setupSkinToggler() {
        List<SkinType> types = uploader.getSupportedSkinTypes().toList();
        btnSkinType
            .setStyles(types.stream().map(SkinType::getStyle).toArray(Style[]::new))
            .setValue(types.indexOf(previewer.getActiveSkinType()));

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

        dropper.cancel();
    }

    @Override
    public void onDrop(List<Path> paths) {
        paths.stream().findFirst().ifPresent(path -> {
            chooser.selectFile(path);
            updateButtons();
        });
    }

    @Override
    public void onSkinTypeChanged(SkinType newType) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
    }

    @Override
    public void onSetRemoteSkin(SkinType type, Identifier location, MinecraftProfileTexture profileTexture) {
        playSound(SoundEvents.ENTITY_VILLAGER_YES);
    }

    protected void switchSkinMode(String model) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

        uploader.setMetadataField("model", model);
        previewer.setModelType(model);
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
        if (canTakeEvents()) {
            if (!chooser.pickingInProgress() && !uploader.isBusy()) {
                return super.charTyped(keyChar, keyCode);
            }
        }

        return false;
    }

    private void walkingToggled(boolean walking) {
        previewer.setSprinting(walking);
    }

    private void jumpToggled(boolean jumping) {
        if (jumping && ctrlKey.getState()) {
            jumpState = !jumpState;
        }

        jumping |= jumpState;

        previewer.setJumping(jumping);
    }

    private void sneakToggled(boolean sneaking) {
        if (sneaking && ctrlKey.getState()) {
            sneakState = !sneakState;
        }

        sneaking |= sneakState;

        previewer.setSneaking(sneaking);

    }

    private void ctrlToggled(boolean ctrl) {
        if (ctrl) {
            if (sneakKey.getState()) {
                sneakState = !sneakState;
            }

            if (jumpKey.getState()) {
                jumpState = !jumpState;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
        RenderSystem.disableCull();

        if (client.world == null) {
            panorama.render(partialTick, 1);
        } else {
           renderBackground(context);
        }

        previewer.render(context, mouseX, mouseY, updateCounter, partialTick, chooser, uploader);
        super.render(context, mouseX, mouseY, partialTick);
        banner.render(context, partialTick, width, height);
    }

    private CompletableFuture<?> punchServer(Text uploadMsg, @Nullable URI file) {
        try {
            return uploader.uploadSkin(uploadMsg, file).whenComplete((o, t) -> {
                if (t != null) {
                    t.printStackTrace();
                }
                updateButtons();
            });
        } finally {
            updateButtons();
        }
    }

    private void updateButtons() {
        Set<Feature> features = uploader.getFeatures();

        btnClear.active = uploader.canClear();
        btnUpload.active = uploader.canUpload() && chooser.hasSelection() && features.contains(Feature.UPLOAD_USER_SKIN);
        btnDownload.active = uploader.canClear() && !chooser.pickingInProgress();
        btnBrowse.active = !chooser.pickingInProgress();

        boolean types = features.contains(Feature.MODEL_TYPES);
        boolean variants = !features.contains(Feature.MODEL_VARIANTS);

        btnSkinType.setEnabled(types);
        btnSkinVariant.setLocked(variants);

        btnClear.setLocked(!features.contains(Feature.DELETE_USER_SKIN));
        btnUpload.setLocked(!features.contains(Feature.UPLOAD_USER_SKIN));
        btnDownload.setLocked(!features.contains(Feature.DOWNLOAD_USER_SKIN));
    }

    protected class FeatureButton extends Button {
        public FeatureButton(int x, int y, int width, int height) {
            super(x, y, width, height);
            setStyle(new FeatureStyle(this));
        }

        public void setLocked(boolean lock) {
            ((FeatureStyle)getStyle()).setLocked(lock);
        }
    }

    protected class FeatureCycler extends Cycler {
        public FeatureCycler(int x, int y) {
            super(x, y, 20, 20);

            setStyle(new FeatureStyle(this));
            setStyles(getStyle());
        }

        public void setLocked(boolean lock) {
            for (Style i : getStyles()) {
                ((FeatureStyle)i).setLocked(lock);
            }
        }
    }

    protected class FeatureSwitch extends Button {
        public FeatureSwitch(int x, int y) {
            super(x, y, 20, 20);

            setStyle(new FeatureStyle(this));
        }

        public void setLocked(boolean lock) {
            ((FeatureStyle)getStyle()).setLocked(lock);
        }
    }

    protected static class FeatureStyle extends Style {

        private final Button element;

        private Optional<Tooltip> disabledTooltip = Optional.of(Tooltip.of(HD_SKINS_OPTION_DISABLED_DESC));

        private boolean locked;

        public FeatureStyle(Button element) {
            this.element = element;
        }

        public FeatureStyle setLocked(boolean locked) {
            this.locked = locked;
            element.active &= !locked;

            return this;
        }

        @Override
        public Optional<Tooltip> getTooltip() {
            if (locked) {
                return disabledTooltip;
            }
            return super.getTooltip();
        }

        @Override
        public Style setTooltip(Tooltip tooltip) {
            disabledTooltip = Optional.of(Tooltip.of(
                    Text.translatable("hdskins.warning.disabled.title",
                            tooltip.getString(),
                            HD_SKINS_OPTION_DISABLED_DESC
                    )
            ));
            return super.setTooltip(tooltip);
        }
    }
}
