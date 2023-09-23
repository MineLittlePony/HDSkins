package com.minelittlepony.hdskins.mixin.client;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;

@Mixin(PlayerListEntry.class)
abstract class MixinPlayerListEntry implements ClientPlayerInfo {
    @Shadow
    private @Final GameProfile profile;
    @Shadow
    private @Final Supplier<SkinTextures> texturesSupplier;

    private PlayerSkins hdskinsPlayerSkins;

    @Override
    public PlayerSkins getSkins() {
        if (hdskinsPlayerSkins == null) {
            hdskinsPlayerSkins = new PlayerSkins(profile, texturesSupplier);
        }
        return hdskinsPlayerSkins;
    }

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> info) {
        info.setReturnValue(getSkins().getSkinTextures());
    }
}
