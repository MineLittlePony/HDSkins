package com.minelittlepony.hdskins.mixin;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

@Mixin(SkullBlockEntity.class)
abstract class MixinSkullBlockEntity extends BlockEntity {
    MixinSkullBlockEntity() {super(null, null, null);}

    @Inject(method = "loadProperties(Lcom/mojang/authlib/GameProfile;Ljava/util/function/Consumer;)V",
            cancellable = true,
            at = @At(value = "HEAD"))
    private static void onLoadProperties(@Nullable GameProfile profile, Consumer<GameProfile> consumer, CallbackInfo info) {
        if (profile != null && profile.getProperties().containsKey("hd_textures")) {
            info.cancel();
            consumer.accept(profile);
        }
    }
}
