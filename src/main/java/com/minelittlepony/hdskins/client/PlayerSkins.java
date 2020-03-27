package com.minelittlepony.hdskins.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.minelittlepony.hdskins.client.ducks.INetworkPlayerInfo;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;

public class PlayerSkins {

    private final INetworkPlayerInfo playerInfo;

    private final Map<SkinType, Identifier> customTextures = new HashMap<>();

    private final Map<SkinType, MinecraftProfileTexture> customProfiles = new HashMap<>();

    private final Map<SkinType, MinecraftProfileTexture> vanillaProfiles = new HashMap<>();

    public PlayerSkins(INetworkPlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    @Nullable
    public Identifier getSkin(SkinType type) {
        return HDSkins.getInstance().getResourceManager()
                .getCustomPlayerTexture(playerInfo.getGameProfile(), type)
                .orElseGet(() -> Optional.ofNullable(customTextures.get(type))
                .orElseGet(() -> type.getEnum().map(playerInfo.getVanillaTextures()::get)
                .orElse(null)));
    }

    @Nullable
    public String getModel() {
        return HDSkins.getInstance().getResourceManager()
                .getCustomPlayerModel(playerInfo.getGameProfile())
                .orElseGet(() -> getModelFrom(customProfiles)
                .orElseGet(() -> getModelFrom(vanillaProfiles)
                .orElse(null)));
    }

    public void load(PlayerSkinProvider provider, GameProfile profile, boolean requireSecure) {

        HDSkins.getInstance().getProfileRepository().fetchSkins(profile, this::onCustomTextureLoaded);

        provider.loadSkin(profile, this::onVanillaTextureLoaded, requireSecure);
    }

    private void onCustomTextureLoaded(SkinType type, Identifier location, MinecraftProfileTexture profileTexture) {
        customTextures.put(type, location);
        customProfiles.put(type, profileTexture);
    }

    private void onVanillaTextureLoaded(Type type, Identifier location, MinecraftProfileTexture profileTexture) {
        playerInfo.getVanillaTextures().put(type, location);
        vanillaProfiles.put(SkinType.forVanilla(type), profileTexture);
    }

    private Optional<String> getModelFrom(Map<SkinType, MinecraftProfileTexture> texture) {
        return Optional.ofNullable(texture.get(SkinType.SKIN))
                .map(t -> VanillaModels.of(t.getMetadata("model")));
    }
}
