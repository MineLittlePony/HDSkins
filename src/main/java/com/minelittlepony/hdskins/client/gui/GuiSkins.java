package com.minelittlepony.hdskins.client.gui;

import com.google.common.base.Preconditions;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Cycler;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.FileDrop;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinChooser;
import com.minelittlepony.hdskins.client.SkinUploader;
import com.minelittlepony.hdskins.client.SkinUploader.ISkinUploadHandler;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.PlayerPreview;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.Feature;
import com.minelittlepony.hdskins.server.SkinServerList;
import com.minelittlepony.hdskins.util.Edge;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public class GuiSkins extends GameGui implements ISkinUploadHandler, FileDrop.Callback {

    private static BiFunction<Screen, SkinServerList, GuiSkins> skinsGuiFunc = GuiSkins::new;

    public static void setSkinsGui(BiFunction<Screen, SkinServerList, GuiSkins> skinsGuiFunc) {
        Preconditions.checkNotNull(skinsGuiFunc, "skinsGuiFunc");
        GuiSkins.skinsGuiFunc = skinsGuiFunc;
    }

    public static GuiSkins create(Screen parent, SkinServerList servers) {
        return skinsGuiFunc.apply(parent, servers);
    }

    private int updateCounter = 0;
    private float lastPartialTick;

    private Button btnBrowse;
    private FeatureButton btnUpload;
    private FeatureButton btnDownload;
    private FeatureButton btnClear;

    private FeatureSwitch btnModeSteve;
    private FeatureSwitch btnModeAlex;

    private Cycler btnSkinType;

    private float msgFadeOpacity = 0;

    private double lastMouseX = 0;

    private boolean jumpState = false;
    private boolean sneakState = false;

    protected final PlayerPreview previewer;
    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(new CubeMapRenderer(getBackground()));

    private final FileDrop dropper = FileDrop.newDropEvent(this);

    private final Edge ctrlKey = new Edge(this::ctrlToggled, Screen::hasControlDown);
    private final Edge jumpKey = new Edge(this::jumpToggled, () -> client.options.keyJump.isPressed());
    private final Edge sneakKey = new Edge(this::sneakToggled, () -> client.options.keySneak.isPressed());
    private final Edge walkKey = new Edge(this::walkingToggled, () -> client.options.keyForward.isPressed() || client.options.keyBack.isPressed());

    public GuiSkins(Screen parent, SkinServerList servers) {
        super(new TranslatableText("hdskins.gui.title"), parent);

        client = MinecraftClient.getInstance();
        previewer = createPreviewer();
        uploader = new SkinUploader(servers, previewer, this);
        chooser = new SkinChooser(uploader);
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

        boolean left = client.options.keyLeft.isPressed();
        boolean right = client.options.keyRight.isPressed();

        ctrlKey.update();
        jumpKey.update();
        sneakKey.update();
        walkKey.update();

        if (!(left && right)) {
            if (left) {
                updateCounter -= 5;
            } else if (right) {
                updateCounter += 5;
            } else {
                updateCounter++;
            }
        }

        uploader.update();

        updateButtons();
    }

    @Override
    public void init() {
        dropper.subscribe();

        addButton(new Label(width / 2, 5)).setCentered().getStyle().setText("hdskins.manager").setColor(0xffffff);
        addButton(new Label(34, 29)).getStyle().setText("hdskins.local").setColor(0xffffff);
        addButton(new Label(width / 2 + 34, 29)).getStyle().setText("hdskins.server").setColor(0xffffff);

        addButton(btnBrowse = new Button(width / 2 - 150, height - 27, 90, 20))
                .onClick(sender -> chooser.openBrowsePNG(I18n.translate("hdskins.open.title")))
                .setEnabled(!client.getWindow().isFullscreen())
                .getStyle().setText("hdskins.options.browse");

        addButton(btnUpload = new FeatureButton(width / 2 - 24, height / 2 - 40, 48, 20))
                .setEnabled(uploader.canUpload())
                .onClick(sender -> {
                    if (uploader.canUpload()) {
                        punchServer(new TranslatableText("hdskins.upload"));
                    }
                })
                .getStyle()
                .setText("hdskins.options.chevy")
                .setTooltip("hdskins.options.chevy.title");

        addButton(btnDownload = new FeatureButton(width / 2 - 24, height / 2 + 20, 48, 20))
                .setEnabled(uploader.canClear())
                .onClick(sender -> {
                    if (uploader.canClear()) {
                        chooser.openSavePNG(I18n.translate("hdskins.save.title"), client.getSession().getUsername());
                    }
                })
                .getStyle()
                .setText("hdskins.options.download")
                .setTooltip("hdskins.options.download.title");

        addButton(btnClear = new FeatureButton(width / 2 + 60, height - 27, 90, 20))
                .setEnabled(uploader.canClear())
                .onClick(sender -> {
                    if (uploader.canClear()) {
                        punchServer(new TranslatableText("hdskins.request"));
                    }
                })
                .getStyle()
                .setText("hdskins.options.clear");

        addButton(new Button(width / 2 - 50, height - 25, 100, 20))
                .onClick(sender -> finish())
                .getStyle()
                .setText("hdskins.options.close");


        int row = 32;

        addButton(btnModeSteve = new FeatureSwitch(width - 25, row))
                .onClick(sender -> switchSkinMode(VanillaModels.DEFAULT))
                .setEnabled(VanillaModels.isSlim(uploader.getMetadataField("model")))
                .getStyle()
                .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)
                .setTooltip("hdskins.mode.steve", 0, 10);


        row += 19;
        addButton(btnModeAlex = new FeatureSwitch(width - 25, row))
                .onClick(sender -> switchSkinMode(VanillaModels.SLIM))
                .setEnabled(VanillaModels.isFat(uploader.getMetadataField("model")))
                .getStyle()
                .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0xfff500)
                .setTooltip("hdskins.mode.alex", 0, 10);

        row += 24;
        addButton(btnSkinType = new Cycler(width - 25, row, 20, 20))
                .onChange(i -> {
                    List<SkinType> types = uploader.getSupportedSkinTypes();
                    i %= types.size();
                    uploader.setSkinType(types.get(i));
                    return i;
                });
        setupSkinToggler();

        row += 24;
        addButton(new Cycler(width - 25, row, 20, 20))
                .setStyles(
                        new Style().setIcon(Items.IRON_BOOTS).setTooltip("hdskins.mode.stand", 0, 10),
                        new Style().setIcon(Items.CLOCK).setTooltip("hdskins.mode.sleep", 0, 10),
                        new Style().setIcon(Items.OAK_BOAT).setTooltip("hdskins.mode.ride", 0, 10),
                        new Style().setIcon(Items.CAULDRON).setTooltip("hdskins.mode.swim", 0, 10),
                        new Style().setIcon(Items.HEART_OF_THE_SEA).setTooltip("hdskins.mode.riptide", 0, 10))
                .setValue(previewer.getPose())
                .onChange(i -> {
                    playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
                    previewer.setPose(i);
                    return i;
                });

        addButton(new Button(width - 25, height - 40, 20, 20))
                .onClick(sender -> {
                    sender.getStyle()
                        .setIcon(previewer.cycleEquipment())
                        .setTooltip(new TranslatableText("hdskins.equipment", I18n.translate("hdskins.equipment." + previewer.getEquipment().getId().getPath())));
                    playSound(previewer.getEquipment().getSound());
                })
                .getStyle()
                .setIcon(previewer.getEquipment().getStack())
                .setTooltip(new TranslatableText("hdskins.equipment", I18n.translate("hdskins.equipment." + previewer.getEquipment().getId().getPath())), 0, 10);

        addButton(new Button(width - 25, height - 65, 20, 20))
                .onClick(sender -> {
                    uploader.cycleGateway();
                    playSound(SoundEvents.ENTITY_VILLAGER_YES);
                    setupSkinToggler();
                    sender.getStyle().setTooltip(uploader.getGatewayText());
                })
                .getStyle()
                .setText("?")
                .setTooltip(uploader.getGatewayText(), 0, 10);
    }

    private void setupSkinToggler() {
        List<SkinType> types = uploader.getSupportedSkinTypes();
        btnSkinType
            .setStyles(types.stream().map(SkinType::getStyle).toArray(Style[]::new))
            .setValue(types.indexOf(uploader.getSkinType()));

    }

    @Override
    public void onClose() {
        super.onClose();

        try {
            uploader.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    protected void switchSkinMode(String model) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

        boolean thinArmType = VanillaModels.isSlim(model);

        btnModeSteve.active = thinArmType;
        btnModeAlex.active = !thinArmType;

        uploader.setMetadataField("model", model);
        previewer.setModelType(model);
    }

    protected boolean canTakeEvents() {
        return !chooser.pickingInProgress() && uploader.tryClearStatus() && msgFadeOpacity == 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;

        if (canTakeEvents() && !super.mouseClicked(mouseX, mouseY, button)) {
            int bottom = height - 40;
            int mid = width / 2;

            if (mouseY > 30 && mouseY < bottom) {
                if (mouseX > 30 && mouseX < mid - 30) {
                    previewer.getLocal().ifPresent(p -> p.swingHand(button == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND));
                }

                if (mouseX > mid + 30 && mouseX < width - 30) {
                    previewer.getRemote().ifPresent(p -> p.swingHand(button == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND));
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double changeX, double changeY) {

        if (canTakeEvents() && !super.mouseDragged(mouseX, mouseY, button, changeX, changeY)) {
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
            if (!chooser.pickingInProgress() && !uploader.uploadInProgress()) {
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTick) {
        RenderSystem.disableCull();

        float deltaTime = updateCounter + partialTick - lastPartialTick;
        lastPartialTick = updateCounter + partialTick;

        panorama.render(partialTick, 1);
        previewer.render(width, height, mouseX, mouseY, updateCounter, partialTick);

        float xPos1 = width / 4F;
        float xPos2 = width * 0.75F;

        if (chooser.getStatus() != null && !uploader.canUpload()) {
            fill(matrices, 40, height / 2 - 12, width / 2 - 40, height / 2 + 12, 0xB0000000);
            drawCenteredLabel(matrices, chooser.getStatus(), (int)xPos1, height / 2 - 4, 0xffffff, 0);
        }

        if (uploader.hasStatus()) {

            int lineHeight = uploader.isThrottled() ? 18 : 12;

            fill(matrices, (int)(xPos2 - width / 4 + 40), height / 2 - lineHeight, width - 40, height / 2 + lineHeight, 0xB0000000);

            Text status = uploader.getStatus();

            if (status == SkinUploader.ERR_MOJANG) {
                drawCenteredLabel(matrices, status, (int)xPos2, height / 2 - 10, 0xff5555, 0);
                drawCenteredLabel(matrices, new TranslatableText(SkinUploader.ERR_MOJANG_WAIT, uploader.getRetries()), (int)xPos2, height / 2 + 2, 0xff5555, 0);
            } else {
                drawCenteredLabel(matrices, status, (int)xPos2, height / 2 - 4, status == SkinUploader.ERR_OFFLINE ? 0xff5555 : 0xffffff, 0);
            }
        }

        super.render(matrices, mouseX, mouseY, partialTick);

        boolean uploadInProgress = uploader.uploadInProgress();
        boolean showError = uploader.hasError();

        if (uploadInProgress || showError || msgFadeOpacity > 0) {
            if (!uploadInProgress && !showError) {
                msgFadeOpacity -= deltaTime / 10;
            } else if (msgFadeOpacity < 1) {
                msgFadeOpacity += deltaTime / 10;
            }

            msgFadeOpacity = MathHelper.clamp(msgFadeOpacity, 0, 1);
        }

        if (msgFadeOpacity > 0) {
            int opacity = (Math.min(180, (int)(msgFadeOpacity * 180)) & 255) << 24;

            fill(matrices, 0, 0, width, height, opacity);

            Text errorMsg = uploader.getError();

            if (uploadInProgress) {
                drawCenteredLabel(matrices, errorMsg, width / 2, height / 2, 0xffffff, 0);
            } else if (showError) {
                int blockHeight = (height - getFont().getWrappedLinesHeight(errorMsg.getString(), width - 10)) / 2;

                drawCenteredLabel(matrices, new TranslatableText("hdskins.failed"), width / 2, blockHeight - getFont().fontHeight * 2, 0xffff55, 0);
                drawTextBlock(matrices, errorMsg, 5, blockHeight, width - 10, 0xff5555);
            }
        }
    }

    private void punchServer(Text uploadMsg) {
        uploader.uploadSkin(uploadMsg).whenComplete((o, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
            updateButtons();
        });

        updateButtons();
    }

    private void updateButtons() {

        Set<Feature> features = uploader.getFeatures();

        btnClear.active = uploader.canClear();
        btnUpload.active = uploader.canUpload() && features.contains(Feature.UPLOAD_USER_SKIN);
        btnDownload.active = uploader.canClear() && !chooser.pickingInProgress();
        btnBrowse.active = !chooser.pickingInProgress();

        boolean types = features.contains(Feature.MODEL_TYPES);
        boolean variants = !features.contains(Feature.MODEL_VARIANTS);

        btnSkinType.setEnabled(types);

        btnModeSteve.setLocked(variants);
        btnModeAlex.setLocked(variants);

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

    protected class FeatureSwitch extends Button {
        public FeatureSwitch(int x, int y) {
            super(x, y, 20, 20);

            setStyle(new FeatureStyle(this));
        }

        public void setLocked(boolean lock) {
            ((FeatureStyle)getStyle()).setLocked(lock);
        }
    }

    protected class FeatureStyle extends Style {

        private final Button element;

        private Optional<Tooltip> disabledTooltip = Optional.of(Tooltip.of(new TranslatableText("hdskins.warning.disabled.description")));

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
                    new TranslatableText("hdskins.warning.disabled.title",
                            tooltip.getString(),
                            new TranslatableText("hdskins.warning.disabled.description")
                    )
            ));
            return super.setTooltip(tooltip);
        }
    }
}
