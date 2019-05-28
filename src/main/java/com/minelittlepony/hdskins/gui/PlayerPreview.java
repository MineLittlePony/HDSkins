package com.minelittlepony.hdskins.gui;

import static com.mojang.blaze3d.platform.GlStateManager.disableColorMaterial;
import static com.mojang.blaze3d.platform.GlStateManager.enableColorMaterial;
import static com.mojang.blaze3d.platform.GlStateManager.popMatrix;
import static com.mojang.blaze3d.platform.GlStateManager.pushMatrix;
import static com.mojang.blaze3d.platform.GlStateManager.rotatef;
import static com.mojang.blaze3d.platform.GlStateManager.scalef;
import static com.mojang.blaze3d.platform.GlStateManager.translatef;

import com.minelittlepony.hdskins.SkinUploader.IPreviewModel;
import com.minelittlepony.hdskins.VanillaModels;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class PlayerPreview implements IPreviewModel {

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    
    private final EntityPlayerModel localPlayer = new EntityPlayerModel(minecraft.getSession().getProfile());
    private final EntityPlayerModel remotePlayer = new EntityPlayerModel(minecraft.getSession().getProfile());
    
    protected PlayerPreview() {
        EntityRenderDispatcher rm = minecraft.getEntityRenderManager();
        rm.gameOptions = minecraft.options;
        rm.targetedEntity = localPlayer;
    }

    public void setPose(int pose) {
        boolean sleeping = pose == 1;
        boolean riding = pose == 2;
        
        getLocal().setSleeping(sleeping);
        getRemote().setSleeping(sleeping);
        
        getLocal().setRiding(riding);
        getRemote().setRiding(riding);
    }
    
    public int getPose() {
        return getLocal().isSleeping() ? 1 : 0;
    }
    
    public void swingHand() {
        getLocal().swingHand(Hand.MAIN_HAND);
        getRemote().swingHand(Hand.MAIN_HAND);
    }
    
    public void setModelType(String model) {
        boolean thinArmType = VanillaModels.isSlim(model);

        getLocal().setPreviewThinArms(thinArmType);
        getRemote().setPreviewThinArms(thinArmType);
    }
    
    public void setJumping(boolean jumping) {
        getLocal().setJumping(jumping);
        getRemote().setJumping(jumping);
    }
    
    public void setSneaking(boolean sneaking) {
        getLocal().setSneaking(sneaking);
        getRemote().setSneaking(sneaking);
    }
    
    public void render(int width, int height, int horizon, int mouseX, int mouseY, int ticks, float partialTick) {
        float yPos = height * 0.75F;
        float xPos1 = width / 4F;
        float xPos2 = width * 0.75F;
        float scale = height / 4F;

        renderPlayerModel(getLocal(), xPos1, yPos, scale, horizon - mouseY, mouseX, ticks, partialTick);
        renderPlayerModel(getRemote(), xPos2, yPos, scale, horizon - mouseY, mouseX, ticks, partialTick);
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
    private void renderPlayerModel(EntityPlayerModel thePlayer, float xPosition, float yPosition, float scale, float mouseY, float mouseX, int ticks, float partialTick) {
        minecraft.getTextureManager().bindTexture(thePlayer.getTexture(Type.SKIN).getTexture());

        enableColorMaterial();
        pushMatrix();
        translatef(xPosition, yPosition, 300);

        scalef(scale, scale, scale);
        rotatef(-15, 1, 0, 0);

        GuiLighting.enableForItems();

        float rot = ((ticks + partialTick) * 2.5F) % 360;

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

    public void setSkinType(Type type) {
        ItemStack stack = type == Type.ELYTRA ? new ItemStack(Items.ELYTRA) : ItemStack.EMPTY;
        // put on or take off the elytra
        getLocal().setEquippedStack(EquipmentSlot.CHEST, stack);
        getRemote().setEquippedStack(EquipmentSlot.CHEST, stack);
    }

    @Override
    public EntityPlayerModel getRemote() {
        return remotePlayer;
    }


    @Override
    public EntityPlayerModel getLocal() {
        return localPlayer;
    }
}
