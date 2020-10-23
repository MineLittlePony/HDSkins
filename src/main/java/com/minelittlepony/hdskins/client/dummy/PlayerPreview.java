package com.minelittlepony.hdskins.client.dummy;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import java.util.HashMap;
import java.util.Map;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinUploader.IPreviewModel;
import com.minelittlepony.common.util.render.ClippingSpace;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.DummyPlayerRenderer.BedHead;
import com.minelittlepony.hdskins.client.dummy.DummyPlayerRenderer.MrBoaty;
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
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Player previewer that renders the models to the screen.
 */
public class PlayerPreview extends DrawableHelper implements IPreviewModel {

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

    private final DummyPlayer localPlayer = new DummyPlayer(localTextures);
    private final DummyPlayer remotePlayer = new DummyPlayer(remoteTextures);

    private int pose;

    public PlayerPreview() {
        minecraft.getEntityRenderDispatcher().targetedEntity = localPlayer;
    }

    public void setPose(int pose) {
        this.pose = pose;

        localTextures.setPose(pose);
        remoteTextures.setPose(pose);
    }

    public int getPose() {
        return pose;
    }

    public void swingHand() {
        getLocal().swingHand(Hand.MAIN_HAND);
        getRemote().swingHand(Hand.MAIN_HAND);
    }

    public void setModelType(String model) {
        boolean thinArmType = VanillaModels.isSlim(model);

        localTextures.setPreviewThinArms(thinArmType);
        remoteTextures.setPreviewThinArms(thinArmType);
    }

    public void setJumping(boolean jumping) {
        getLocal().setJumping(jumping);
        getRemote().setJumping(jumping);
    }

    public void setSneaking(boolean sneaking) {
        getLocal().setSneaking(sneaking);
        getRemote().setSneaking(sneaking);
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
        DummyPlayerRenderer.wrap(() -> {
            enableRescaleNormal();

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
        });
    }

    public void renderWorldAndPlayer(DummyPlayer thePlayer,
            int frameLeft, int frameRight, int frameBottom, int frameTop,
            float xPos, float yPos, int horizon, int mouseX, int mouseY, int ticks, float partialTick, float scale,
            MatrixStack matrixStack) {
        ClippingSpace.renderClipped(frameLeft, frameTop, frameRight - frameLeft, frameBottom - frameTop, () -> {
            Immediate context = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            drawBackground(matrixStack, frameLeft, frameRight, frameBottom, frameTop, horizon);

            renderPlayerModel(thePlayer, xPos, yPos, scale, horizon - mouseY, mouseX, ticks, partialTick, matrixStack, context);

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
            HDSkins.logger.warn("Entity " + thePlayer.toString() + " does not have a valid renderer. Did resource loading fail?");
        }

        minecraft.getTextureManager().bindTexture(thePlayer.getTextures().get(SkinType.SKIN).getId());

        DiffuseLighting.enableForLevel(matrixStack.peek().getModel());

        matrixStack.push();
        matrixStack.translate(xPosition, yPosition, 300);
        matrixStack.scale(scale, scale, scale);
        matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-15));

        float rot = ((ticks + partialTick) * 2.5F) % 360;

        matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(rot));

        float lookFactor = (float)Math.sin((rot * (Math.PI / 180)) + 45);
        float lookX = (float)Math.atan((xPosition - mouseX) / 20) * 30;

        thePlayer.headYaw = lookX * lookFactor;
        thePlayer.pitch = thePlayer.isSleeping() ? 10 : (float)Math.atan(mouseY / 40) * -20;

        matrixStack.push();
        matrixStack.scale(1, -1, -1);

        renderPlayerEntity(matrixStack, thePlayer, renderContext, dispatcher);
        matrixStack.pop();

        matrixStack.push();
        matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180));
        matrixStack.scale(0.99F, 1, 0.99F);

        renderPlayerEntity(matrixStack, thePlayer, renderContext, dispatcher);
        matrixStack.pop();

        matrixStack.pop();
    }

    protected void renderPlayerEntity(MatrixStack matrixStack, DummyPlayer thePlayer, VertexConsumerProvider renderContext, EntityRenderDispatcher dispatcher) {
        if (thePlayer.isSleeping()) {
            BedHead.instance.render(thePlayer, matrixStack, renderContext);
        }

        if (thePlayer.hasVehicle()) {
            MrBoaty.instance.render(matrixStack, renderContext);
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
            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(90));

            y += 0.7F;
            x += 1;
        }
        if (thePlayer.isSwimming()) {
            DummyWorld.INSTANCE.fillWith(Blocks.WATER.getDefaultState());
            matrixStack.multiply(Vector3f.NEGATIVE_X.getDegreesQuaternion(45));

            if (thePlayer.getVelocity().x < 100) {
                thePlayer.addVelocity(100, 0, 0);
            }

            y += 0.5F;
            x += 0;
            z += 1;
        } else {
            DummyWorld.INSTANCE.fillWith(Blocks.AIR.getDefaultState());

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

    @Override
    public void setSkinType(SkinType type) {
        localTextures.setSkinType(type);
        remoteTextures.setSkinType(type);
    }

    @Override
    public DummyPlayer getRemote() {
        return remotePlayer;
    }


    @Override
    public DummyPlayer getLocal() {
        return localPlayer;
    }

    @Override
    public ItemStack setEquipment(EquipmentSet set) {
        set.apply(getLocal());
        set.apply(getRemote());

        return set.getStack();
    }
}
