package com.minelittlepony.hdskins.mixin;

import com.minelittlepony.hdskins.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(SkullBlockEntityRenderer.class)
public abstract class MixinSkullRenderer extends BlockEntityRenderer<SkullBlockEntity> {

    @Inject(method = "method_3578(Lnet/minecraft/block/SkullBlock$SkullType;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/util/Identifier;",
            at = @At(value = "HEAD"))
    private void onGetSkullTexture(SkullBlock.SkullType type, @Nullable GameProfile profile, CallbackInfoReturnable<Identifier> info) {
        if (type == SkullBlock.Type.PLAYER && profile != null) {
            Identifier skin = HDSkins.getInstance().getTextures(profile).get(Type.SKIN);

            if (skin != null) {
                info.setReturnValue(skin);
            }
        }
    }
}
