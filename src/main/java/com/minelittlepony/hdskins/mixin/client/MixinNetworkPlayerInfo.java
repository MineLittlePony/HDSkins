package com.minelittlepony.hdskins.mixin.client;

import java.util.Map;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.client.ducks.INetworkPlayerInfo;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;

@Mixin(PlayerListEntry.class)
public abstract class MixinNetworkPlayerInfo implements INetworkPlayerInfo {

    private PlayerSkins hdskinsPlayerSkins = new PlayerSkins(this);

    @Override
    @Accessor("textures")
    public abstract Map<Type, Identifier> getVanillaTextures();

    @Override
    @Accessor("profile")
    public abstract GameProfile getGameProfile();

    @Override
    public PlayerSkins getSkins() {
        return hdskinsPlayerSkins;
    }

    @Redirect(method = {"getSkinTexture", "getCapeTexture", "getElytraTexture"},
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", remap = false)
    )
    // synthetic
    private Object getSkin(Map<Type, Identifier> sender, Object key) {
        return getSkins().getSkin(SkinType.forVanilla((Type)key));
    }

    @Nullable
    @Redirect(method = "getModel",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/PlayerListEntry;model:Ljava/lang/String;")
    )
    private String getTextureModel(PlayerListEntry sender) {
        return getSkins().getModel();
    }

    @Redirect(method = "loadTextures",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/texture/PlayerSkinProvider;loadSkin("
                            + "Lcom/mojang/authlib/GameProfile;"
                            + "Lnet/minecraft/client/texture/PlayerSkinProvider$SkinTextureAvailableCallback;"
                            + "Z)V")
    )
    private void redirectLoadPlayerTextures(PlayerSkinProvider sender, GameProfile profile, PlayerSkinProvider.SkinTextureAvailableCallback callback, boolean requireSecure) {
        getSkins().load(sender, profile, requireSecure);
    }
}
