package com.minelittlepony.hdskins.client.profile;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.hdskins.Memoize;
import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.client.ducks.ClientPlayerInfo;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.player.PlayerSkin;

public final class HDSkinCacheEntry implements ClientPlayerInfo {
    private final PlayerSkinRenderCache.RenderInfo owner;
    private final ClientPlayerInfo dynamicSkins;
    private final PlayerSkin.Patch patch;

    private long cacheTime = System.currentTimeMillis();
    private PlayerSkin vanillaTextures;
    @Nullable
    private PlayerSkin cachedTextures;
    @Nullable
    private PlayerSkin textures;
    private final Memoize<RenderType> renderType = Memoize.basic(() -> SkullBlockRenderer.getPlayerSkinRenderType(textures.body().texturePath()));
    private final Memoize<GpuTextureView> textureView = Memoize.basic(() -> Minecraft.getInstance().getTextureManager().getTexture(textures.body().texturePath()).getTextureView());
    private final Memoize<GlyphRenderTypes> glyphRenderTypes = Memoize.basic(() -> GlyphRenderTypes.createForColorTexture(this.textures.body().texturePath()));

    public HDSkinCacheEntry(PlayerSkinRenderCache.RenderInfo owner, GameProfile profile, PlayerSkin textures, PlayerSkin.Patch patch) {
        this.owner = owner;
        this.patch = patch;
        this.vanillaTextures = textures.with(patch);
        dynamicSkins = PlayerSkins.create(profile, () -> vanillaTextures);
    }

    @Override
    public PlayerSkins getSkins() {
        return dynamicSkins.getSkins();
    }

    public PlayerSkin playerSkin(PlayerSkin vanillaTextures) {
        if (!this.vanillaTextures.equals(vanillaTextures)) {
            this.vanillaTextures = vanillaTextures;
        }
        if (cachedTextures == null || getSkins().layers().isNewer(cacheTime)) {
            cachedTextures = getSkins().sorted().getSkinTextures();
            cacheTime = System.currentTimeMillis();
            this.textures = cachedTextures.with(patch);
            renderType.expireNow();
            textureView.expireNow();
            glyphRenderTypes.expireNow();
        }
        return this.textures;
    }

    private boolean checkState() {
        return owner.playerSkin().equals(cachedTextures) && cachedTextures != null;
    }

    public RenderType renderType(RenderType layer) {
        return checkState() ? renderType.get() : layer;
    }

    public GpuTextureView textureView(GpuTextureView view) {
        return checkState() ? textureView.get() : view;
    }

    public GlyphRenderTypes glyphRenderTypes(GlyphRenderTypes layers) {
        return checkState() ? glyphRenderTypes.get() : layers;
    }
}
