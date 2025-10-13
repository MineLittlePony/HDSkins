package com.minelittlepony.hdskins.mixin.client;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.minelittlepony.hdskins.client.profile.HDSkinCacheEntry;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.entity.player.SkinTextures;

@Mixin(PlayerSkinCache.Entry.class)
abstract class MixinPlayerSkinCacheEntry implements ClientPlayerInfo {
    @Shadow
    private @Final GameProfile profile;
    @Shadow
    private @Final SkinTextures textures;

    @Nullable
    private Supplier<PlayerSkins> dynamicSkins;

    @Unique
    private HDSkinCacheEntry hdSkinsCacheEntry;

    @Override
    public PlayerSkins getSkins() {
        return hdSkinsCacheEntry.getSkins();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(PlayerSkinCache $0$this, final GameProfile profile, final SkinTextures textures, final SkinTextures.SkinOverride skinOverride, CallbackInfo info) {
        hdSkinsCacheEntry = new HDSkinCacheEntry((PlayerSkinCache.Entry)(Object)this, profile, textures, skinOverride);
    }

    @ModifyReturnValue(method = "getTextures", at = @At("RETURN"))
    private SkinTextures modifyTextures(SkinTextures textures) {
        return hdSkinsCacheEntry.getTextures(textures);
    }

    @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
    private RenderLayer modifyRenderLayer(RenderLayer layer) {
        return hdSkinsCacheEntry.getRenderLayer(layer);
    }

    @ModifyReturnValue(method = "getTextureView", at = @At("RETURN"))
    private GpuTextureView modifyTextureView(GpuTextureView view) {
        return hdSkinsCacheEntry.getTextureView(view);
    }

    @ModifyReturnValue(method = "getTextRenderLayers", at = @At("RETURN"))
    private TextRenderLayerSet modifyTextRenderLayers(TextRenderLayerSet layers) {
        return hdSkinsCacheEntry.getTextRenderLayers(layers);
    }
}
