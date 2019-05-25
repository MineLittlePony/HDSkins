package com.minelittlepony.hdskins.gui;

import com.google.common.base.Splitter;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.IconicButton;
import com.minelittlepony.common.client.gui.element.IconicToggle;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.SkinChooser;
import com.minelittlepony.hdskins.SkinUploader;
import com.minelittlepony.hdskins.SkinUploader.ISkinUploadHandler;
import com.minelittlepony.hdskins.VanillaModels;
import com.minelittlepony.hdskins.net.SkinServer;
import com.minelittlepony.hdskins.upload.FileDrop;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.minelittlepony.hdskins.util.Edge;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.MainMenuScreen;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static com.mojang.blaze3d.platform.GlStateManager.*;

public class GuiSkins extends GameGui implements ISkinUploadHandler, FileDrop.IDropCallback {

    private int updateCounter = 0;
    private float lastPartialTick;

    private Button btnBrowse;
    private FeatureButton btnUpload;
    private FeatureButton btnDownload;
    private FeatureButton btnClear;

    private FeatureSwitch btnModeSteve;
    private FeatureSwitch btnModeAlex;

    private FeatureSwitch btnModeSkin;
    private FeatureSwitch btnModeElytra;

    protected EntityPlayerModel localPlayer;
    protected EntityPlayerModel remotePlayer;

    private DoubleBuffer doubleBuffer;

    private float msgFadeOpacity = 0;

    private double lastMouseX = 0;

    private boolean jumpState = false;
    private boolean sneakState = false;

    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(new CubeMapRenderer(getBackground()));

    private final FileDrop dropper = FileDrop.newDropEvent(this);

    private final Edge ctrlKey = new Edge(this::ctrlToggled) {
        @Override
        protected boolean nextState() {
            return Screen.hasControlDown();
        }
    };
    private final Edge jumpKey = new Edge(this::jumpToggled) {
        @Override
        protected boolean nextState() {
            return InputUtil.isKeyPressed(minecraft.window.getHandle(), GLFW.GLFW_KEY_SPACE);
        }
    };
    private final Edge sneakKey = new Edge(this::sneakToggled) {
        @Override
        protected boolean nextState() {
            return Screen.hasShiftDown();
        }
    };

    public GuiSkins(List<SkinServer> servers) {
        super(new TranslatableComponent("hdskins.gui.title"));

        minecraft = MinecraftClient.getInstance();
        GameProfile profile = minecraft.getSession().getProfile();

        localPlayer = getModel(profile);
        remotePlayer = getModel(profile);

        EntityRenderDispatcher rm = minecraft.getEntityRenderManager();
        rm.gameOptions = minecraft.options;
        rm.targetedEntity = localPlayer;

        uploader = new SkinUploader(servers, localPlayer, remotePlayer, this);
        chooser = new SkinChooser(uploader);
    }

    protected Identifier getBackground() {
        return new Identifier(HDSkins.MOD_ID, "textures/cubemaps/cubemap0");
    }

    protected EntityPlayerModel getModel(GameProfile profile) {
        return new EntityPlayerModel(profile);
    }

    @Override
    public void tick() {

        if (!( InputUtil.isKeyPressed(minecraft.window.getHandle(), GLFW.GLFW_KEY_LEFT)
            || InputUtil.isKeyPressed(minecraft.window.getHandle(), GLFW.GLFW_KEY_RIGHT))) {
            updateCounter++;
        }

        uploader.update();

        updateButtons();
    }

    @Override
    public void init() {
        dropper.subscribe();

        addButton(new Label(width / 2, 10)).setCentered().getStyle().setText("hdskins.manager").setColor(0xffffff);
        addButton(new Label(34, 34)).getStyle().setText("hdskins.local").setColor(0xffffff);
        addButton(new Label(width / 2 + 34, 34)).getStyle().setText("hdskins.net").setColor(0xffffff);

        addButton(btnBrowse = new Button(width / 2 - 150, height - 27, 90, 20))
                .onClick(sender -> chooser.openBrowsePNG(I18n.translate("hdskins.open.title")))
                .setEnabled(!minecraft.window.isFullscreen())
                .getStyle().setText("hdskins.options.browse");

        addButton(btnUpload = new FeatureButton(width / 2 - 24, height / 2 - 20, 48, 20))
                .setEnabled(uploader.canUpload())
                .onClick(sender -> {
                    if (uploader.canUpload()) {
                        punchServer("hdskins.upload");
                    }
                })
                .getStyle()
                .setText("hdskins.options.chevy")
                .setTooltip("hdskins.options.chevy.title");

        addButton(btnDownload = new FeatureButton(width / 2 - 24, height / 2 + 20, 48, 20))
                .setEnabled(uploader.canClear())
                .onClick(sender -> {
                    if (uploader.canClear()) {
                        chooser.openSavePNG(I18n.translate("hdskins.save.title"), minecraft.getSession().getUsername());
                    }
                })
                .getStyle()
                .setText("hdskins.options.download")
                .setTooltip("hdskins.options.download.title");

        addButton(btnClear = new FeatureButton(width / 2 + 60, height - 27, 90, 20))
                .setEnabled(uploader.canClear())
                .onClick(sender -> {
                    if (uploader.canClear()) {
                        punchServer("hdskins.request");
                    }
                })
                .getStyle()
                .setText("hdskins.options.clear");

        addButton(btnBrowse = new Button(width / 2 - 150, height - 27, 90, 20))
                .onClick(sender -> chooser.openBrowsePNG(I18n.translate("hdskins.open.title")))
                .setEnabled(!minecraft.window.isFullscreen())
                .getStyle()
                .setText("hdskins.options.browse");

        addButton(new Button(width / 2 - 50, height - 25, 100, 20))
                .onClick(sender -> minecraft.disconnect(new MainMenuScreen()))
                .getStyle()
                .setText("hdskins.options.close");

        addButton(btnModeSteve = new FeatureSwitch(width - 25, 32))
                .onClick(sender -> switchSkinMode(VanillaModels.DEFAULT))
                .setEnabled(VanillaModels.isSlim(uploader.getMetadataField("model")))
                .getStyle()
                .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)
                .setTooltip("hdskins.mode.steve")
                .setTooltipOffset(0, 10);

        addButton(btnModeAlex = new FeatureSwitch(width - 25, 51))
                .onClick(sender -> switchSkinMode(VanillaModels.SLIM))
                .setEnabled(VanillaModels.isFat(uploader.getMetadataField("model")))
                .getStyle()
                .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0xfff500)
                .setTooltip("hdskins.mode.alex")
                .setTooltipOffset(0, 10);

        addButton(btnModeSkin = new FeatureSwitch(width - 25, 75))
                .onClick(sender -> uploader.setSkinType(Type.SKIN))
                .setEnabled(uploader.getSkinType() == Type.ELYTRA)
                .getStyle()
                .setIcon(new ItemStack(Items.LEATHER_CHESTPLATE))
                .setTooltip("hdskins.mode." + Type.SKIN.name().toLowerCase())
                .setTooltipOffset(0, 10);

        addButton(btnModeElytra = new FeatureSwitch(width - 25, 94))
                .onClick(sender -> uploader.setSkinType(Type.ELYTRA))
                .setEnabled(uploader.getSkinType() == Type.SKIN)
                .getStyle()
                .setIcon(new ItemStack(Items.ELYTRA))
                .setTooltip("hdskins.mode." + Type.ELYTRA.name().toLowerCase())
                .setTooltipOffset(0, 10);

        addButton(new IconicToggle(width - 25, 118))
                .setValue(localPlayer.isSleeping() ? 1 : 0)
                .setStyles(
                        new Style().setIcon(Items.IRON_BOOTS).setTooltip("hdskins.mode.stand", 0, 10),
                        new Style().setIcon(Items.CLOCK).setTooltip("hdskins.mode.sleep", 0, 10),
                        new Style().setIcon(Items.OAK_BOAT).setTooltip("hdskins.mode.ride", 0, 10))
                .onClick((Consumer<IconicToggle>)sender -> {
                    playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

                    boolean sleep = sender.getValue() == 1;
                    boolean ride = sender.getValue() == 2;
                    localPlayer.setSleeping(sleep);
                    remotePlayer.setSleeping(sleep);

                    localPlayer.setRiding(ride);
                    remotePlayer.setRiding(ride);
                });

        addButton(new Button(width - 25, height - 65, 20, 20))
                .onClick(sender -> {
                    uploader.cycleGateway();
                    playSound(SoundEvents.ENTITY_VILLAGER_YES);
                    sender.getStyle().setTooltip(uploader.getGateway());
                })
                .getStyle()
                .setText("?")
                .setTooltip(uploader.getGateway(), 0, 10);
    }

    @Override
    public void onClose() {
        super.onClose();

        try {
            uploader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HDSkins.getInstance().clearSkinCache();

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
    public void onSkinTypeChanged(Type newType) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

        btnModeSkin.active = newType == Type.ELYTRA;
        btnModeElytra.active = newType == Type.SKIN;
    }

    protected void switchSkinMode(String model) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

        boolean thinArmType = VanillaModels.isSlim(model);

        btnModeSteve.active = thinArmType;
        btnModeAlex.active = !thinArmType;

        uploader.setMetadataField("model", model);
        localPlayer.setPreviewThinArms(thinArmType);
        remotePlayer.setPreviewThinArms(thinArmType);
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

            if ((mouseX > 30 && mouseX < mid - 30 || mouseX > mid + 30 && mouseX < width - 30) && mouseY > 30 && mouseY < bottom) {
                localPlayer.swingHand(Hand.MAIN_HAND);
                remotePlayer.swingHand(Hand.MAIN_HAND);
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
    public boolean charTyped(char keyChar, int keyCode) {
        if (canTakeEvents()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                updateCounter -= 5;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                updateCounter += 5;
            }

            if (!chooser.pickingInProgress() && !uploader.uploadInProgress()) {
                return super.charTyped(keyChar, keyCode);
            }
        }

        return false;
    }

    private void jumpToggled(boolean jumping) {
        if (jumping && ctrlKey.getState()) {
            jumpState = !jumpState;
        }

        jumping |= jumpState;

        localPlayer.setJumping(jumping);
        remotePlayer.setJumping(jumping);
    }

    private void sneakToggled(boolean sneaking) {
        if (sneaking && ctrlKey.getState()) {
            sneakState = !sneakState;
        }

        sneaking |= sneakState;

        localPlayer.setSneaking(sneaking);
        remotePlayer.setSneaking(sneaking);
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
    public void render(int mouseX, int mouseY, float partialTick) {
        ctrlKey.update();
        jumpKey.update();
        sneakKey.update();

        panorama.render(partialTick, 0);

        float deltaTime = updateCounter + partialTick - lastPartialTick;
        lastPartialTick = updateCounter + partialTick;

        int bottom = height - 40;
        int mid = width / 2;
        int horizon = height / 2 + height / 5;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

        fill(30, 30, mid - 30, bottom, Integer.MIN_VALUE);
        fill(mid + 30, 30, width - 30, bottom, Integer.MIN_VALUE);

        fillGradient(30, horizon, mid - 30, bottom, 0x80FFFFFF, 0xffffff);
        fillGradient(mid + 30, horizon, width - 30, bottom, 0x80FFFFFF, 0xffffff);

        enableClipping(bottom);

        float yPos = height * 0.75F;
        float xPos1 = width / 4F;
        float xPos2 = width * 0.75F;
        float scale = height / 4F;

        renderPlayerModel(localPlayer, xPos1, yPos, scale, horizon - mouseY, mouseX, partialTick);
        renderPlayerModel(remotePlayer, xPos2, yPos, scale, horizon - mouseY, mouseX, partialTick);

        disableClipping();

        super.render(mouseX, mouseY, partialTick);

        if (chooser.getStatus() != null && !uploader.canUpload()) {
            fill(40, height / 2 - 12, width / 2 - 40, height / 2 + 12, 0xB0000000);
            drawCenteredString(font, I18n.translate(chooser.getStatus()), (int)xPos1, height / 2 - 4, 0xffffff);
        }

        if (uploader.downloadInProgress() || uploader.isThrottled() || uploader.isOffline()) {

            int lineHeight = uploader.isThrottled() ? 18 : 12;

            fill((int)(xPos2 - width / 4 + 40), height / 2 - lineHeight, width - 40, height / 2 + lineHeight, 0xB0000000);

            if (uploader.isThrottled()) {
                drawCenteredString(font, I18n.translate(SkinUploader.ERR_MOJANG), (int)xPos2, height / 2 - 10, 0xff5555);
                drawCenteredString(font, I18n.translate(SkinUploader.ERR_WAIT, uploader.getRetries()), (int)xPos2, height / 2 + 2, 0xff5555);
            } else if (uploader.isOffline()) {
                drawCenteredString(font, I18n.translate(SkinUploader.ERR_OFFLINE), (int)xPos2, height / 2 - 4, 0xff5555);
            } else {
                drawCenteredString(font, I18n.translate(SkinUploader.STATUS_FETCH), (int)xPos2, height / 2 - 4, 0xffffff);
            }
        }

        boolean uploadInProgress = uploader.uploadInProgress();
        boolean showError = uploader.hasStatus();

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

            fill(0, 0, width, height, opacity);

            String errorMsg = I18n.translate(uploader.getStatusMessage());

            if (uploadInProgress) {
                drawCenteredString(font, errorMsg, width / 2, height / 2, 0xffffff);
            } else if (showError) {
                int blockHeight = (height - font.getStringBoundedHeight(errorMsg, width - 10)) / 2;

                drawCenteredString(font, I18n.translate("hdskins.failed"), width / 2, blockHeight - font.fontHeight * 2, 0xffff55);
                font.drawStringBounded(errorMsg, 5, blockHeight, width - 10, 0xff5555);
            }
        }

        depthMask(true);
        enableDepthTest();
    }

    private void renderPlayerModel(EntityPlayerModel thePlayer, float xPosition, float yPosition, float scale, float mouseY, float mouseX, float partialTick) {
        minecraft.getTextureManager().bindTexture(thePlayer.getLocal(Type.SKIN).getTexture());

        enableColorMaterial();
        pushMatrix();
        translatef(xPosition, yPosition, 300);

        scalef(scale, scale, scale);
        rotatef(-15, 1, 0, 0);

        GuiLighting.enableForItems();

        float rot = ((updateCounter + partialTick) * 2.5F) % 360;

        rotatef(rot, 0, 1, 0);

        float lookFactor = (float)Math.sin((rot * (Math.PI / 180)) + 45);
        float lookX = (float)Math.atan((xPosition - mouseX) / 20) * 30;

        thePlayer.headYaw = lookX * lookFactor;
        thePlayer.pitch = (float)Math.atan(mouseY / 40) * -20;

        minecraft.getEntityRenderManager().render(thePlayer, 0, 0, 0, 0, 1, false);

        popMatrix();
        GuiLighting.disable();
        disableColorMaterial();
    }

    /*
     *       /   |
     *     1/    |o      Q = t + q
     *     /q    |       x = xPosition - mouseX
     *     *-----*       sin(q) = o             cos(q) = x        tan(q) = o/x
     *   --|--x------------------------------------
     *     |
     *      mouseX
     */

    private void enableClipping(int yBottom) {
        GL11.glPopAttrib();

        if (doubleBuffer == null) {
            doubleBuffer = BufferUtils.createByteBuffer(32).asDoubleBuffer();
        }

        doubleBuffer.clear();
        doubleBuffer.put(0).put(1).put(0).put(-30).flip();

        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, doubleBuffer);
        doubleBuffer.clear();
        doubleBuffer.put(0).put(-1).put(0).put(yBottom).flip();

        GL11.glClipPlane(GL11.GL_CLIP_PLANE1, doubleBuffer);
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        GL11.glEnable(GL11.GL_CLIP_PLANE1);
    }

    private void disableClipping() {
        GL11.glDisable(GL11.GL_CLIP_PLANE1);
        GL11.glDisable(GL11.GL_CLIP_PLANE0);

        disableDepthTest();
        enableBlend();
        depthMask(false);
    }

    private void punchServer(String uploadMsg) {
        uploader.uploadSkin(uploadMsg).handle(CallableFutures.callback(this::updateButtons));

        updateButtons();
    }

    private void updateButtons() {
        btnClear.active = uploader.canClear();
        btnUpload.active = uploader.canUpload() && uploader.supportsFeature(Feature.UPLOAD_USER_SKIN);
        btnDownload.active = uploader.canClear() && !chooser.pickingInProgress();
        btnBrowse.active = !chooser.pickingInProgress();

        boolean types = !uploader.supportsFeature(Feature.MODEL_TYPES);
        boolean variants = !uploader.supportsFeature(Feature.MODEL_VARIANTS);

        btnModeSkin.setLocked(types);
        btnModeElytra.setLocked(types);

        btnModeSteve.setLocked(variants);
        btnModeAlex.setLocked(variants);

        btnClear.setLocked(!uploader.supportsFeature(Feature.DELETE_USER_SKIN));
        btnUpload.setLocked(!uploader.supportsFeature(Feature.UPLOAD_USER_SKIN));
        btnDownload.setLocked(!uploader.supportsFeature(Feature.DOWNLOAD_USER_SKIN));
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

    protected class FeatureSwitch extends IconicButton {

        public FeatureSwitch(int x, int y) {
            super(x, y);

            setStyle(new FeatureStyle(this));
        }

        public void setLocked(boolean lock) {
            ((FeatureStyle)getStyle()).setLocked(lock);
        }
    }

    protected class FeatureStyle extends Style {

        private final Button element;

        private List<String> disabledTooltip = Splitter.onPattern("\r?\n|\\\\n").splitToList(I18n.translate("hdskins.warning.disabled.description"));

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
        public List<String> getTooltip() {
            if (locked) {
                return disabledTooltip;
            }
            return super.getTooltip();
        }

        @Override
        public Style setTooltip(String tooltip) {
            disabledTooltip = Splitter.onPattern("\r?\n|\\\\n").splitToList(
                    I18n.translate("hdskins.warning.disabled.title",
                    I18n.translate(tooltip),
                    I18n.translate("hdskins.warning.disabled.description")));
            return super.setTooltip(tooltip);
        }
    }
}
