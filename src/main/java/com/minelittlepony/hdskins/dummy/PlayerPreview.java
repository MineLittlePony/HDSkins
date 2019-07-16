package com.minelittlepony.hdskins.dummy;

import static com.mojang.blaze3d.platform.GlStateManager.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.minelittlepony.hdskins.SkinUploader.IPreviewModel;
import com.minelittlepony.hdskins.resources.LocalTexture.IBlankSkinSupplier;
import com.minelittlepony.common.util.render.ClippingSpace;
import com.minelittlepony.hdskins.VanillaModels;
import com.minelittlepony.hdskins.dummy.EquipmentList.EquipmentSet;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

/**
 * Player previewer that renders the models to the screen.
 */
public class PlayerPreview extends DrawableHelper implements IPreviewModel, IBlankSkinSupplier {

    public static final Identifier NO_SKIN = new Identifier("hdskins", "textures/mob/noskin.png");
    public static final Identifier NO_ELYTRA = new Identifier("textures/entity/elytra.png");

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    protected final GameProfile profile = minecraft.getSession().getProfile();

    protected final TextureProxy localTextures = new TextureProxy(profile, this);
    protected final TextureProxy remoteTextures = new TextureProxy(profile, this);

    private final DummyPlayer localPlayer = new DummyPlayer(localTextures);
    private final DummyPlayer remotePlayer = new DummyPlayer(remoteTextures);

    private int pose;

    public PlayerPreview() {
        EntityRenderDispatcher rm = minecraft.getEntityRenderManager();
        rm.gameOptions = minecraft.options;
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
    public Identifier getBlankSkin(Type type) {
        return type == Type.SKIN ? NO_SKIN : NO_ELYTRA;
    }

    public void render(int width, int height, int mouseX, int mouseY, int ticks, float partialTick) {
        enableRescaleNormal();

        int mid = width / 2;
        int horizon = height / 2 + height / 5;
        int frameBottom = height - 40;

        float yPos = height * 0.75F;
        float scale = height / 4F;

        renderWorldAndPlayer(getLocal(), 30, mid - 30, frameBottom, 30,
                width / 4F,    yPos, horizon, mouseX, mouseY, ticks, partialTick, scale);

        renderWorldAndPlayer(getRemote(), mid + 30, width - 30, frameBottom, 30,
                width * 0.75F, yPos, horizon, mouseX, mouseY, ticks, partialTick, scale);

        disableDepthTest();
    }

    public void renderWorldAndPlayer(DummyPlayer thePlayer,
            int frameLeft, int frameRight, int frameBottom, int frameTop,
            float xPos, float yPos, int horizon, int mouseX, int mouseY, int ticks, float partialTick, float scale) {

        ClippingSpace.renderClipped(frameLeft, frameTop, frameRight - frameLeft, frameBottom - frameTop, () -> {
            drawBackground(frameLeft, frameRight, frameBottom, frameTop, horizon);

            renderPlayerModel(thePlayer, xPos, yPos, scale, horizon - mouseY, mouseX, ticks, partialTick);
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
    protected void renderPlayerModel(DummyPlayer thePlayer, float xPosition, float yPosition, float scale, float mouseY, float mouseX, int ticks, float partialTick) {
        minecraft.getTextureManager().bindTexture(thePlayer.getTextures().get(Type.SKIN).getId());

        pushMatrix();

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

        pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        disableAlphaTest();
        dispatcher.render(thePlayer, 0, 0, 0, 0, 1, false);
        GL11.glPopAttrib();
        popMatrix();


        pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL14.glBlendColor(1, 1, 1, 0.3F);
        blendFuncSeparate(
                SourceFactor.DST_COLOR, DestFactor.ONE_MINUS_CONSTANT_ALPHA,
                SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
        enableBlend();
        scalef(0.99F, -1, 0.99F);

        dispatcher.render(thePlayer, 0, 0, 0, 0, 1, false);

        GL11.glPopAttrib();
        popMatrix();
        GuiLighting.disable();
        disableColorMaterial();

        popMatrix();
    }

    @Override
    public void setSkinType(Type type) {
        ItemStack stack = type == Type.ELYTRA ? new ItemStack(Items.ELYTRA) : ItemStack.EMPTY;
        // put on or take off the elytra
        getLocal().setEquippedStack(EquipmentSlot.CHEST, stack);
        getRemote().setEquippedStack(EquipmentSlot.CHEST, stack);
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
