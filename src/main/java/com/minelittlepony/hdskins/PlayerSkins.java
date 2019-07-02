package com.minelittlepony.hdskins;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.minelittlepony.hdskins.ducks.INetworkPlayerInfo;
import com.minelittlepony.hdskins.profile.skin.SkinParsingService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;

public class PlayerSkins {

    private final INetworkPlayerInfo playerInfo;

    private final Map<Type, Identifier> customTextures = new HashMap<>();

    private final Map<Type, MinecraftProfileTexture> customProfiles = new HashMap<>();

    private final Map<Type, MinecraftProfileTexture> vanillaProfiles = new HashMap<>();

    public PlayerSkins(INetworkPlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public Identifier getSkin(Type type) {
        if (customTextures.containsKey(type)) {
            return customTextures.get(type);
        }

        return playerInfo.getVanillaTextures().get(type);
    }

    public String getModel() {
        return getModelFrom(customProfiles).orElseGet(() -> {
            return getModelFrom(vanillaProfiles).orElse(null);
        });
    }

    public void load(PlayerSkinProvider provider, GameProfile profile, boolean requireSecure) {

        HDSkins.getInstance().fetchAndLoadSkins(profile, this::onCustomTextureLoaded);

        provider.loadSkin(profile, this::onVanillaTextureLoaded, requireSecure);
    }

    private void onCustomTextureLoaded(Type type, Identifier location, MinecraftProfileTexture profileTexture) {
        customTextures.put(type, location);
        customProfiles.put(type, profileTexture);
    }

    private void onVanillaTextureLoaded(Type type, Identifier location, MinecraftProfileTexture profileTexture) {
        HDSkins.getInstance().getSkinParser().parseSkin(playerInfo.getGameProfile(), type, location, profileTexture);
        playerInfo.getVanillaTextures().put(type, location);
        vanillaProfiles.put(type, profileTexture);
    }

    public void reload() {
        synchronized (this) {

            GameProfile profile = playerInfo.getGameProfile();

            SkinParsingService parser = HDSkins.getInstance().getSkinParser();

            customProfiles.entrySet().forEach(entry -> {
                parser.parseSkinAsync(profile, entry.getKey(),
                        customTextures.get(entry.getKey()), entry.getValue());
            });

            vanillaProfiles.entrySet().forEach(entry -> {
                parser.parseSkinAsync(profile, entry.getKey(),
                        playerInfo.getVanillaTextures().get(entry.getKey()), entry.getValue());
            });
        }
    }

    @Nullable
    private Optional<String> getModelFrom(Map<Type, MinecraftProfileTexture> texture) {
        if (texture.containsKey(Type.SKIN)) {
            String model = texture.get(Type.SKIN).getMetadata("model");

            return Optional.of(model != null ? model : "default");
        }

        return Optional.empty();
    }
}
