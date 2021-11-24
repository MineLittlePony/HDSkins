package com.minelittlepony.hdskins.client.dummy;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.common.util.render.ClippingSpace;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.DummyPlayerRenderer.BedHead;
import com.minelittlepony.hdskins.client.dummy.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Player previewer that renders the models to the screen.
 */
public class PlayerPreview extends DrawableHelper {

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

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    protected final GameProfile profile = minecraft.getSession().getProfile();

    protected final TextureProxy localTextures = new TextureProxy(profile, this::getBlankSteveSkin, this::getBlankAlexSkin);
    protected final TextureProxy remoteTextures = new TextureProxy(profile, this::getBlankSteveSkin, this::getBlankAlexSkin);

    private Optional<DummyPlayer> localPlayer = Optional.empty();
    private Optional<DummyPlayer> remotePlayer = Optional.empty();

    private int pose;

    private final Iterator<EquipmentSet> equipmentSets;
    private EquipmentSet activeEquipmentSet;

    public PlayerPreview() {
        activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
        equipmentSets = HDSkins.getInstance().getDummyPlayerEquipmentList().getCycler();

        DummyWorld.FUTURE_INSTANCE.get().thenAcceptAsync(w -> {
            remotePlayer = Optional.of(new DummyPlayer(remoteTextures));
            localPlayer = Optional.of(new DummyPlayer(localTextures));
            minecraft.getEntityRenderDispatcher().targetedEntity = localPlayer.get();
        });
    }

    public ItemStack cycleEquipment() {
        activeEquipmentSet = equipmentSets.next();

        apply(activeEquipmentSet::apply);

        return activeEquipmentSet.getStack();
    }

    public EquipmentSet getEquipment() {
        return activeEquipmentSet;
    }

    public void setPose(int pose) {
        this.pose = pose;

        localTextures.setPose(pose);
        remoteTextures.setPose(pose);
    }

    public int getPose() {
        return pose;
    }

    public void setModelType(String model) {
        boolean thinArmType = VanillaModels.isSlim(model);

        localTextures.setPreviewThinArms(thinArmType);
        remoteTextures.setPreviewThinArms(thinArmType);
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

    public Identifier getBlankSteveSkin(SkinType type) {
        return NO_TEXTURES.get(type);
    }

    public Identifier getBlankAlexSkin(SkinType type) {
        if (NO_TEXTURES_ALEX.containsKey(type)) {
            return NO_TEXTURES_ALEX.get(type);
        }
        return getBlankSteveSkin(type);
    }

    public void render(int width, int height, int mouseX, int mouseY, int ticks, float partialTick) {

        int mid = width / 2;
        int horizon = height / 2 + height / 5;
        int frameBottom = height - 40;

        float yPos = height * 0.75F;
        float scale = height / 4F;

        MatrixStack matrixStack = new MatrixStack();

        renderWorldAndPlayer(getLocal(), 30, mid - 30, frameBottom, 30,
                width / 4F,    yPos, horizon, mouseX, mouseY, ticks, partialTick, scale,
                matrixStack);

        renderWorldAndPlayer(getRemote(), mid + 30, width - 30, frameBottom, 30,
                width * 0.75F, yPos, horizon, mouseX, mouseY, ticks, partialTick, scale,
                matrixStack);

        disableDepthTest();
    }

    public void renderWorldAndPlayer(Optional<DummyPlayer> thePlayer,
            int frameLeft, int frameRight, int frameBottom, int frameTop,
            float xPos, float yPos, int horizon, int mouseX, int mouseY, int ticks, float partialTick, float scale,
            MatrixStack matrixStack) {
        ClippingSpace.renderClipped(frameLeft, frameTop, frameRight - frameLeft, frameBottom - frameTop, () -> {
            Immediate context = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            drawBackground(matrixStack, frameLeft, frameRight, frameBottom, frameTop, horizon);

            thePlayer.ifPresent(player -> {
                try {
                    DummyPlayerRenderer.wrap(() -> {
                        renderPlayerModel(player, xPos, yPos, scale, horizon - mouseY, mouseX, ticks, partialTick, matrixStack, context);
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
        }

        minecraft.getTextureManager().bindTexture(thePlayer.getTextures().get(SkinType.SKIN).getId());

        float rot = ((ticks + partialTick) * 2.5F) % 360 + 180;
        float lookFactor = (float)Math.sin((rot * (Math.PI / 180)) + 45);
        float lookX = (float)Math.atan((xPosition - mouseX) / 20) * 30;

        thePlayer.setHeadYaw(lookX * lookFactor);
        thePlayer.setPitch(thePlayer.isSleeping() ? 10 : (float)Math.atan(mouseY / 40) * -20);

        // actual player
        matrixStack.push();
        DiffuseLighting.enableForLevel(matrixStack.peek().getPositionMatrix());
        matrixStack.translate(xPosition, yPosition, 300);
        matrixStack.scale(scale, scale, scale);
        matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-15));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rot));
        matrixStack.scale(1, -1, -1);
        renderPlayerEntity(matrixStack, thePlayer, renderContext, dispatcher);
        matrixStack.pop();
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
            y -= 0.125D;
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

        matrixStack.translate(x, y, z);

        Entity camera = minecraft.getCameraEntity();
        minecraft.setCameraEntity(thePlayer);

        dispatcher.render(thePlayer, 0, 0, 0, 0, 1, matrixStack, renderContext, 0xF000F0);

        minecraft.setCameraEntity(camera);

        matrixStack.pop();
    }

    public void setSkinType(SkinType type) {
        localTextures.setSkinType(type);
        remoteTextures.setSkinType(type);
    }

    public Optional<DummyPlayer> getRemote() {
        return remotePlayer;
    }

    public Optional<DummyPlayer> getLocal() {
        return localPlayer;
    }
}
