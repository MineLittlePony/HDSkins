package com.minelittlepony.hdskins.client.profile;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.Memoize;
import com.minelittlepony.hdskins.client.PlayerSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.entity.player.SkinTextures;

public final class HDSkinCacheEntry {
    private final PlayerSkinCache.Entry owner;
    private final Supplier<PlayerSkins> dynamicSkins;
    private final SkinTextures.SkinOverride override;

    private SkinTextures vanillaTextures;
    @Nullable
    private SkinTextures cachedTextures;
    @Nullable
    private SkinTextures textures;
    private final Memoize<RenderLayer> renderLayer = Memoize.basic(() -> SkullBlockEntityRenderer.getTranslucentRenderLayer(textures.body().texturePath()));
    private final Memoize<GpuTextureView> textureView = Memoize.basic(() -> MinecraftClient.getInstance().getTextureManager().getTexture(textures.body().texturePath()).getGlTextureView());
    private final Memoize<TextRenderLayerSet> textRenderLayers = Memoize.basic(() -> TextRenderLayerSet.of(this.textures.body().texturePath()));

    public HDSkinCacheEntry(PlayerSkinCache.Entry owner, GameProfile profile, SkinTextures textures, SkinTextures.SkinOverride override) {
        this.owner = owner;
        this.override = override;
        this.vanillaTextures = textures.withOverride(override);
        dynamicSkins = PlayerSkins.create(profile, () -> vanillaTextures);
    }

    public SkinTextures getTextures(SkinTextures vanillaTextures) {
        if (!this.vanillaTextures.equals(vanillaTextures)) {
            this.vanillaTextures = vanillaTextures;
        }
        if (cachedTextures == null || dynamicSkins.get().layers().hasChanged()) {
            cachedTextures = dynamicSkins.get().sorted().getSkinTextures();
            this.textures = cachedTextures.withOverride(override);
            renderLayer.expireNow();
            textureView.expireNow();
            textRenderLayers.expireNow();
        }
        return this.textures;
    }

    private boolean checkState() {
        return owner.getTextures().equals(cachedTextures) && cachedTextures != null;
    }

    public RenderLayer getRenderLayer(RenderLayer layer) {
        return checkState() ? renderLayer.get() : layer;
    }

    public GpuTextureView getTextureView(GpuTextureView view) {
        return checkState() ? textureView.get() : view;
    }

    public TextRenderLayerSet getTextRenderLayers(TextRenderLayerSet layers) {
        return checkState() ? textRenderLayers.get() : layers;
    }
}
