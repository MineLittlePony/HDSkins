package com.minelittlepony.hdskins.mixin;

import com.minelittlepony.hdskins.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.block.BlockSkull;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(TileEntitySkullRenderer.class)
public abstract class MixinSkullRenderer extends TileEntityRenderer<TileEntitySkull> {

    @Inject(method = "func_199356_a(Lnet/minecraft/block/BlockSkull$ISkullType;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/util/ResourceLocation;",
            at = @At(value = "HEAD"))
    private void onGetSkullTexture(BlockSkull.ISkullType type, @Nullable GameProfile profile, CallbackInfoReturnable<ResourceLocation> info) {
        if (type == BlockSkull.Types.PLAYER && profile != null) {
            ResourceLocation skin = HDSkins.getInstance().getTextures(profile).get(Type.SKIN);

            if (skin != null) {
                info.setReturnValue(skin);
            }
        }
    }
}
