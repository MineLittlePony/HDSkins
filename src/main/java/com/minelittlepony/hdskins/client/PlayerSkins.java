package com.minelittlepony.hdskins.client;

import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PlayerSkins {

    private final PlayerListEntry playerListEntry;

    private final Map<SkinType, Identifier> vanillaTextures = new HashMap<>();
    private final Map<SkinType, Identifier> customTextures = new HashMap<>();
    private final Map<SkinType, MinecraftProfileTexture> customProfiles = new HashMap<>();
    private final Map<SkinType, MinecraftProfileTexture> vanillaProfiles = new HashMap<>();

    public PlayerSkins(PlayerListEntry playerListEntry) {
        this.playerListEntry = playerListEntry;
    }

    @Nullable
    public Identifier getSkin(SkinType type) {
        return HDSkinsClient.getInstance().getResourceManager()
                .getCustomPlayerTexture(playerListEntry.getProfile(), type)
                .orElseGet(() -> Optional.ofNullable(customTextures.get(type))
                        .orElseGet(() -> Optional.ofNullable(vanillaTextures.get(type))
                                .orElse(null)));
    }

    @Nullable
    public String getModel() {
        return HDSkinsClient.getInstance().getResourceManager()
                .getCustomPlayerModel(playerListEntry.getProfile())
                .orElseGet(() -> getModelFrom(customProfiles)
                        .orElseGet(() -> getModelFrom(vanillaProfiles)
                                .orElse(null)));
    }

    public void load(PlayerSkinProvider provider, GameProfile profile, boolean requireSecure) {
        provider.loadSkin(profile, this::onVanillaTextureLoaded, requireSecure);
        // Load the skins on a separate thread.
        HDSkins.getInstance().getSkinServerList().loadProfileTextures(profile)
                .thenAccept(m -> m.forEach((type, texture) -> {
                    // Download the skins
                    HDSkinsClient.getInstance().getProfileRepository().loadTexture(type, texture, this::onCustomTextureLoaded);
                }));
    }

    private void onCustomTextureLoaded(SkinType type, Identifier location, MinecraftProfileTexture profileTexture) {
        customTextures.put(type, location);
        customProfiles.put(type, profileTexture);
    }

    private void onVanillaTextureLoaded(Type type, Identifier location, MinecraftProfileTexture profileTexture) {
        vanillaTextures.put(SkinType.of(type.name()), location);
        vanillaProfiles.put(SkinType.of(type.name()), profileTexture);
    }

    private Optional<String> getModelFrom(Map<SkinType, MinecraftProfileTexture> texture) {
        return Optional.ofNullable(texture.get(SkinType.SKIN))
                .map(t -> VanillaModels.of(t.getMetadata("model")));
    }
}
