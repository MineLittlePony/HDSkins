package com.minelittlepony.hdskins.dummy;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.minelittlepony.hdskins.SkinUploader.IPreviewModel;
import com.minelittlepony.hdskins.resources.LocalTexture.IBlankSkinSupplier;
import com.minelittlepony.common.client.gui.OutsideWorldRenderer;
import com.minelittlepony.common.util.render.ClippingSpace;
import com.minelittlepony.hdskins.VanillaModels;
import com.minelittlepony.hdskins.dummy.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Player previewer that renders the models to the screen.
 */
public class PlayerPreview extends DrawableHelper implements IPreviewModel, IBlankSkinSupplier {

    public static final Map<SkinType, Identifier> NO_TEXTURES = Util.create(new HashMap<>(), map -> {
        map.put(SkinType.SKIN, new Identifier("hdskins", "textures/mob/noskin.png"));
        map.put(SkinType.ELYTRA, new Identifier("textures/entity/elytra.png"));
    });

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    protected final GameProfile profile = minecraft.getSession().getProfile();

    protected final TextureProxy localTextures = new TextureProxy(profile, this);
    protected final TextureProxy remoteTextures = new TextureProxy(profile, this);

    private final DummyPlayer localPlayer = new DummyPlayer(localTextures);
    private final DummyPlayer remotePlayer = new DummyPlayer(remoteTextures);

    private int pose;

    public PlayerPreview() {
        EntityRenderDispatcher rm = minecraft.getEntityRenderManager();
        rm.targetedEntity = localPlayer;
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

    @Override
    public Identifier getBlankSkin(SkinType type) {
        return NO_TEXTURES.get(type);
    }

    public void render(int width, int height, int mouseX, int mouseY, int ticks, float partialTick) {
        enableRescaleNormal();

        int mid = width / 2;
        int horizon = height / 2 + height / 5;
        int frameBottom = height - 40;

        float yPos = height * 0.75F;
        float scale = height / 4F;

        MatrixStack matrixStack = new MatrixStack();
        Immediate context = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderWorldAndPlayer(getLocal(), 30, mid - 30, frameBottom, 30,
                width / 4F,    yPos, horizon, mouseX, mouseY, ticks, partialTick, scale,
                matrixStack, context);

        renderWorldAndPlayer(getRemote(), mid + 30, width - 30, frameBottom, 30,
                width * 0.75F, yPos, horizon, mouseX, mouseY, ticks, partialTick, scale,
                matrixStack, context);

        disableDepthTest();
    }

    public void renderWorldAndPlayer(DummyPlayer thePlayer,
            int frameLeft, int frameRight, int frameBottom, int frameTop,
            float xPos, float yPos, int horizon, int mouseX, int mouseY, int ticks, float partialTick, float scale,
            MatrixStack matrixStack, VertexConsumerProvider renderContext) {

        OutsideWorldRenderer.configure(thePlayer.world);
        ClippingSpace.renderClipped(frameLeft, frameTop, frameRight - frameLeft, frameBottom - frameTop, () -> {
            drawBackground(frameLeft, frameRight, frameBottom, frameTop, horizon);

            renderPlayerModel(thePlayer, xPos, yPos, scale, horizon - mouseY, mouseX, ticks, partialTick, matrixStack, renderContext);
        });
    }

    protected void drawBackground(int frameLeft, int frameRight, int frameBottom, int frameTop, int horizon) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        fill(        frameLeft, frameTop, frameRight, frameBottom,                        0xA0000000);
        fillGradient(frameLeft, horizon,  frameRight, frameBottom, 0x05FFFFFF, 0x40FFFFFF);
        GL11.glPopAttrib();
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
        minecraft.getTextureManager().bindTexture(thePlayer.getTextures().get(SkinType.SKIN).getId());

        matrixStack.push();

        enableColorMaterial();
        GuiLighting.enable();

        translatef(xPosition, yPosition, 300);
        scalef(scale, scale, scale);
        rotatef(-15, 1, 0, 0);

        float rot = ((ticks + partialTick) * 2.5F) % 360;

        rotatef(rot, 0, 1, 0);

        float lookFactor = (float)Math.sin((rot * (Math.PI / 180)) + 45);
        float lookX = (float)Math.atan((xPosition - mouseX) / 20) * 30;

        thePlayer.headYaw = lookX * lookFactor;
        thePlayer.pitch = thePlayer.isSleeping() ? 10 : (float)Math.atan(mouseY / 40) * -20;

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderManager();

        matrixStack.push();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        disableAlphaTest();
        dispatcher.render(thePlayer, 0, 0, 0, 0, 1, matrixStack, renderContext, 1);
        GL11.glPopAttrib();
        matrixStack.pop();


        matrixStack.push();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL14.glBlendColor(1, 1, 1, 0.3F);
        blendFuncSeparate(
                SourceFactor.DST_COLOR, DestFactor.ONE_MINUS_CONSTANT_ALPHA,
                SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
        enableBlend();
        matrixStack.scale(0.99F, -1, 0.99F);

        dispatcher.render(thePlayer, 0, 0, 0, 0, 1, matrixStack, renderContext, 1);

        GL11.glPopAttrib();
        matrixStack.pop();
        GuiLighting.disable();
        disableColorMaterial();

        matrixStack.pop();
    }

    @Override
    public void setSkinType(SkinType type) {
        ItemStack stack = type == SkinType.ELYTRA ? new ItemStack(Items.ELYTRA) : ItemStack.EMPTY;
        // put on or take off the elytra
        getLocal().equipStack(EquipmentSlot.CHEST, stack);
        getRemote().equipStack(EquipmentSlot.CHEST, stack);
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
