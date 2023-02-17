package com.minelittlepony.hdskins.client.dummy;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.common.client.gui.dimension.Padding;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.util.render.ClippingSpace;
import com.minelittlepony.hdskins.client.*;
import com.minelittlepony.hdskins.client.dummy.DummyPlayerRenderer.BedHead;
import com.minelittlepony.hdskins.client.dummy.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.gui.SkinListWidget;
import com.minelittlepony.hdskins.client.resources.*;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.*;
import net.minecraft.util.math.Vec3f;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * Player previewer that renders the models to the screen.
 */
public class PlayerPreview extends DrawableHelper implements Closeable, PlayerSkins.Posture, ITextContext {
    private static final int MARGIN = 30;
    private static final int LABEL_BACKGROUND = 0xB0000000;

    public static final Identifier NO_SKIN_STEVE = new Identifier("hdskins", "textures/mob/noskin.png");
    public static final Identifier NO_SKIN_ALEX = new Identifier("hdskins", "textures/mob/noskin_alex.png");
    public static final Identifier NO_SKIN_CAPE = new Identifier("hdskins", "textures/mob/noskin_cape.png");

    public static final Map<SkinType, Identifier> NO_TEXTURES = Util.make(new HashMap<>(), map -> {
        map.put(SkinType.SKIN, NO_SKIN_STEVE);
        map.put(SkinType.CAPE, NO_SKIN_CAPE);
        map.put(SkinType.ELYTRA, new Identifier("textures/entity/elytra.png"));
    });
    public static final Map<SkinType, Identifier> NO_TEXTURES_ALEX = Util.make(new HashMap<>(), map -> {
        map.put(SkinType.SKIN, NO_SKIN_ALEX);
    });

    public static Identifier getDefaultTexture(SkinType type, boolean slimArms) {
        if (slimArms && NO_TEXTURES_ALEX.containsKey(type)) {
            return NO_TEXTURES_ALEX.get(type);
        }
        return NO_TEXTURES.getOrDefault(type, NO_SKIN_STEVE);
    }

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    protected final GameProfile profile = minecraft.getSession().getProfile();

    private Optional<DummyPlayer> localPlayer = Optional.empty();
    protected final LocalPlayerSkins localTextures = new LocalPlayerSkins(this);

    private Optional<DummyPlayer> remotePlayer = Optional.empty();
    protected final ServerPlayerSkins remoteTextures = new ServerPlayerSkins(this);

    public final Bounds localFrameBounds = new Bounds(MARGIN, MARGIN, 0, 0);
    public final Bounds serverFrameBounds = new Bounds(MARGIN, MARGIN, 0, 0);
    private int horizon;
    private float scale;

    private final SkinListWidget skinList;

    private int pose;
    private SkinType activeSkinType = SkinType.SKIN;

    private final Iterator<EquipmentSet> equipmentSets;
    private EquipmentSet activeEquipmentSet;

    public PlayerPreview() {
        activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
        equipmentSets = HDSkins.getInstance().getDummyPlayerEquipmentList().getCycler();
        skinList = new SkinListWidget(this, serverFrameBounds);

        DummyWorld.getOrDummyFuture().thenAcceptAsync(w -> {
            try {
                remotePlayer = Optional.of(createEntity(w, remoteTextures));
                localPlayer = Optional.of(createEntity(w, localTextures));
                minecraft.getEntityRenderDispatcher().targetedEntity = localPlayer.get();
            } catch (Throwable t) {
                HDSkins.LOGGER.error("Error creating players", t);
            }
        }, MinecraftClient.getInstance());
    }

    protected DummyPlayer createEntity(ClientWorld world, PlayerSkins<?> textures) {
        return new DummyPlayer(world, textures);
    }

    public ItemStack cycleEquipment() {
        activeEquipmentSet = equipmentSets.next();
        applyEquipment();
        return activeEquipmentSet.getStack();
    }

    @Override
    public EquipmentSet getEquipment() {
        return activeEquipmentSet;
    }

    public void applyEquipment() {
        apply(activeEquipmentSet::apply);
    }

    @Override
    public GameProfile getProfile() {
        return profile;
    }

    public void setPose(int pose) {
        this.pose = pose;
    }

    @Override
    public int getPose() {
        return pose;
    }

    public void setModelType(String model) {
        localTextures.setPreviewThinArms(VanillaModels.isSlim(model));
        localTextures.close();
    }

    public void setSkinType(SkinType type) {
        activeSkinType = type;
    }

    @Override
    public SkinType getActiveSkinType() {
        return activeSkinType;
    }

    @Override
    public Identifier getDefaultSkin(SkinType type, boolean slim) {
        Identifier skin = getBlankSkin(type, slim);
        return DefaultSkinGenerator.generateGreyScale(type == SkinType.SKIN ? DefaultSkinHelper.getTexture(profile.getId()) : skin, skin, getExclusion());
    }

    protected TextureLoader.Exclusion getExclusion() {
        return TextureLoader.Exclusion.NULL;
    }

    public Identifier getBlankSkin(SkinType type, boolean slim) {
        return getDefaultTexture(type, slim);
    }

    public void setJumping(boolean jumping) {
        apply(p -> p.setJumping(jumping));
    }

    public void setSneaking(boolean sneaking) {
        apply(p -> p.setSneaking(sneaking));
    }

    public void setSprinting(boolean walking) {
        apply(p -> p.setSprinting(walking));
    }

    public void apply(Consumer<DummyPlayer> action) {
        getLocal().ifPresent(action);
        getRemote().ifPresent(action);
    }

    public void init(GuiSkins screen) {
        localFrameBounds.left = MARGIN;
        localFrameBounds.height = screen.height - 70;
        localFrameBounds.width = (screen.width / 2) - 70;

        serverFrameBounds.copy(localFrameBounds);
        serverFrameBounds.left = screen.width - MARGIN - serverFrameBounds.width;

        horizon = screen.height / 2 + screen.height / 5;

        scale = screen.height / 4F;

        Padding padding = new Padding(0, 5, 0, 0);

        Label label;
        screen.addButton(label = new Label(0, 0))
            .getStyle()
            .setText("hdskins.local")
            .setColor(0xffffff);
        label.setBounds(localFrameBounds.offset(padding));
        screen.addButton(label = new Label(0, 0)).getStyle().setText("hdskins.server").setColor(0xffffff);
        label.setBounds(serverFrameBounds.offset(padding));

        skinList.init(screen);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, int ticks, float partialTick, SkinChooser chooser, SkinUploader uploader) {
        MatrixStack matrixStack = new MatrixStack();

        enableDepthTest();

        renderWorldAndPlayer(getLocal(), localFrameBounds, horizon, mouseX, mouseY, ticks, partialTick, scale,
                matrixStack, null);

        renderWorldAndPlayer(getRemote(), serverFrameBounds, horizon, mouseX, mouseY, ticks, partialTick, scale,
                matrixStack, remote -> {
                    skinList.render(remote, matrixStack, mouseX, mouseY);
                });

        disableDepthTest();

        if (chooser.hasStatus()) {
            matrices.push();
            localFrameBounds.translate(matrices);
            fill(matrices, 10, localFrameBounds.height / 2 - 12, localFrameBounds.width - 10, localFrameBounds.height / 2 + 12, LABEL_BACKGROUND);
            drawCenteredLabel(matrices, chooser.getStatus(), localFrameBounds.width / 2, localFrameBounds.height / 2 - 4, 0xFFFFFF, 0);
            matrices.pop();
        }

        if (uploader.hasStatus()) {
            matrices.push();
            serverFrameBounds.translate(matrices);

            int lineHeight = uploader.isThrottled() ? 18 : 12;

            fill(matrices, 10, serverFrameBounds.height / 2 - lineHeight, serverFrameBounds.width - 10, serverFrameBounds.height / 2 + lineHeight, LABEL_BACKGROUND);

            Text status = uploader.getStatus();

            if (status == SkinUploader.STATUS_MOJANG) {
                drawCenteredLabel(matrices, status, serverFrameBounds.width / 2, serverFrameBounds.height / 2 - 10, 0xff5555, 0);
                drawCenteredLabel(matrices, Text.translatable(SkinUploader.ERR_MOJANG_WAIT, uploader.getRetries()), serverFrameBounds.width / 2, serverFrameBounds.height / 2 + 2, 0xff5555, 0);
            } else {
                drawCenteredLabel(matrices, status, serverFrameBounds.width / 2, serverFrameBounds.height / 2 - 4, status == SkinUploader.STATUS_OFFLINE ? 0xff5555 : 0xffffff, 0);
            }

            matrices.pop();
        }

    }

    public boolean mouseClicked(SkinUploader uploader, int width, int height, double mouseX, double mouseY, int button) {

        if (skinList.mouseClicked(uploader, mouseX, mouseY, button)) {
            return true;
        }

        if (localFrameBounds.contains(mouseX, mouseY)) {
            getLocal().ifPresent(p -> p.swingHand(button == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND));
        }

        if (serverFrameBounds.contains(mouseX, mouseY)) {
            getRemote().ifPresent(p -> p.swingHand(button == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND));
        }

        return false;
    }

    public void renderWorldAndPlayer(Optional<DummyPlayer> thePlayer,
            Bounds frame,
            int horizon, int mouseX, int mouseY, int ticks, float partialTick, float scale,
            MatrixStack matrixStack, @Nullable Consumer<DummyPlayer> postAction) {

        ClippingSpace.renderClipped(frame.left, frame.top, frame.width, frame.height, () -> {
            Immediate context = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            drawBackground(matrixStack, frame.left, frame.left + frame.width, frame.top + frame.height, frame.top, horizon);

            thePlayer.ifPresent(player -> {
                try {
                    DummyPlayerRenderer.wrap(() -> {
                        renderPlayerModel(player, frame.left + frame.width / 2, frame.top + frame.height * 0.8F, scale, horizon - mouseY, mouseX, ticks, partialTick, matrixStack, context);

                        if (postAction != null) {
                            postAction.accept(player);
                        }
                    });
                } catch (Exception e) {
                    HDSkins.LOGGER.error("Exception whilst rendering player preview.", e);
                }
            });

            context.draw();
        });
    }

    protected void drawBackground(MatrixStack matrices, int frameLeft, int frameRight, int frameBottom, int frameTop, int horizon) {
        fill(matrices,         frameLeft, frameTop, frameRight, frameBottom,                        0xA0000000);
        fillGradient(matrices, frameLeft, horizon,  frameRight, frameBottom, 0x05FFFFFF, 0x40FFFFFF);
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
    protected void renderPlayerModel(DummyPlayer thePlayer, float xPosition, float yPosition, float scale, float mouseY, float mouseX, int ticks, float partialTick, MatrixStack matrixStack, VertexConsumerProvider renderContext) {

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();

        if (dispatcher.getRenderer(thePlayer) == null) {
            HDSkins.LOGGER.warn("Entity " + thePlayer.toString() + " does not have a valid renderer. Did resource loading fail?");
            return;
        }

        float rot = ((ticks + partialTick) * 2.5F) % 360;
        float lookFactor = (float)Math.sin((rot * (Math.PI / 180)) + 45);
        float lookX = (float)Math.atan((xPosition - mouseX) / 20) * -30;

        thePlayer.setHeadYaw(lookX * lookFactor);
        thePlayer.setPitch(thePlayer.isSleeping() ? 10 : (float)Math.atan(mouseY / 40) * -20);

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate(xPosition, yPosition, 1050);
        modelStack.scale(1, 1, -1);
        RenderSystem.applyModelViewMatrix();

        matrixStack.push();
        matrixStack.translate(0, 0, 1000);
        matrixStack.scale(scale, scale, scale);

        matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(15));
        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rot));

        DiffuseLighting.method_34742();

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderPlayerEntity(matrixStack, thePlayer, immediate, dispatcher);

        immediate.draw();

        matrixStack.pop();
        modelStack.pop();
        RenderSystem.applyModelViewMatrix();
        DiffuseLighting.enableGuiDepthLighting();
    }

    protected void renderPlayerEntity(MatrixStack matrixStack, DummyPlayer thePlayer, VertexConsumerProvider renderContext, EntityRenderDispatcher dispatcher) {
        if (thePlayer.isSleeping()) {
            BedHead.instance.render(thePlayer, matrixStack, renderContext);
        }

        if (thePlayer.hasVehicle()) {
            thePlayer.boat.render(matrixStack, renderContext);
        }

        double offset = thePlayer.getY();

        if (thePlayer.hasVehicle()) {
            offset = thePlayer.getMountedHeightOffset() - thePlayer.getHeight();
        }

        float x = 0;
        float y = 0;
        float z = 0;

        if (thePlayer.isSneaking()) {
            y += 0.125D;
        }

        matrixStack.push();
        matrixStack.translate(0.001, offset, 0.001);

        if (thePlayer.isSleeping()) {
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));

            y += 0.7F;
            x += 1;
        }
        if (thePlayer.isSwimming()) {
            DummyWorld.fillWith(Blocks.WATER.getDefaultState());
            matrixStack.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(45));

            if (thePlayer.getVelocity().x < 100) {
                thePlayer.addVelocity(100, 0, 0);
            }

            y += 0.5F;
            x += 0;
            z += 1;
        } else {
            DummyWorld.fillWith(Blocks.AIR.getDefaultState());

            if (thePlayer.getVelocity().x >= 100) {
                thePlayer.addVelocity(-100, 0, 0);
            }
        }

        Entity camera = minecraft.getCameraEntity();
        minecraft.setCameraEntity(thePlayer);

        dispatcher.render(thePlayer, x, y, z, 0, 1, matrixStack, renderContext, 0xF000F0);

        minecraft.setCameraEntity(camera);

        matrixStack.pop();
    }

    public Optional<DummyPlayer> getRemote() {
        return remotePlayer;
    }

    public ServerPlayerSkins getServerTextures() {
        return remoteTextures;
    }

    public LocalPlayerSkins getClientTextures() {
        return localTextures;
    }

    public Optional<DummyPlayer> getLocal() {
        return localPlayer;
    }

    @Override
    public void close() {
        remoteTextures.close();
        localTextures.close();
    }
}
