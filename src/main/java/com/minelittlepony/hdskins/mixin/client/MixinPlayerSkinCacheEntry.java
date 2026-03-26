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

import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.player.PlayerSkin;

@Mixin(PlayerSkinRenderCache.RenderInfo.class)
abstract class MixinPlayerSkinCacheEntry implements ClientPlayerInfo {
    @Shadow
    private @Final GameProfile profile;
    @Shadow
    private @Final PlayerSkin textures;

    @Nullable
    private Supplier<PlayerSkins> dynamicSkins;

    @Unique
    private HDSkinCacheEntry hdSkinsCacheEntry;

    @Override
    public PlayerSkins getSkins() {
        return hdSkinsCacheEntry.getSkins();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(PlayerSkinRenderCache $0$this, final GameProfile profile, final PlayerSkin textures, final PlayerSkin.Patch patch, CallbackInfo info) {
        hdSkinsCacheEntry = new HDSkinCacheEntry((PlayerSkinRenderCache.RenderInfo)(Object)this, profile, textures, patch);
    }

    @ModifyReturnValue(method = "playerSkin", at = @At("RETURN"))
    private PlayerSkin modifySkin(PlayerSkin textures) {
        return hdSkinsCacheEntry.playerSkin(textures);
    }

    @ModifyReturnValue(method = "renderType", at = @At("RETURN"))
    private RenderType modifyRenderType(RenderType layer) {
        return hdSkinsCacheEntry.renderType(layer);
    }

    @ModifyReturnValue(method = "textureView", at = @At("RETURN"))
    private GpuTextureView modifyTextureView(GpuTextureView view) {
        return hdSkinsCacheEntry.textureView(view);
    }

    @ModifyReturnValue(method = "glyphRenderTypes", at = @At("RETURN"))
    private GlyphRenderTypes modifyGlyphRenderTypes(GlyphRenderTypes layers) {
        return hdSkinsCacheEntry.glyphRenderTypes(layers);
    }
}
