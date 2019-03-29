package com.minelittlepony.hdskins.mixin;

import com.minelittlepony.hdskins.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.block.BlockSkull;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(TileEntitySkullRenderer.class)
public abstract class MixinSkullRenderer extends TileEntityRenderer<TileEntitySkull> {

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/tileentity/TileEntitySkullRenderer;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
                    ordinal = 4))
    private void onBindTexture(TileEntitySkullRenderer tesr, ResourceLocation rl, float x, float y, float z, EnumFacing facing, float rotation, BlockSkull.ISkullType type, @Nullable GameProfile profile, int destroyStage, float animationProgress) {
        if (profile != null) {
            ResourceLocation skin = HDSkins.getInstance().getTextures(profile).get(Type.SKIN);

            if (skin != null) {
                rl = skin;
            }
        }

        bindTexture(rl);
    }
}
