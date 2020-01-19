package com.minelittlepony.hdskins.mixin.client;

import com.minelittlepony.hdskins.client.PlayerSkins;
import com.minelittlepony.hdskins.skins.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    private PlayerSkins skins = new PlayerSkins((PlayerListEntry) (Object) this);

    @Redirect(method = {"getSkinTexture", "getCapeTexture", "getElytraTexture"},
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", remap = false)
    )
    // synthetic
    private Object getSkin(Map<Type, Identifier> sender, Object key) {
        return skins.getSkin(SkinType.of(((Type) key).name()));
    }

    @Nullable
    @Redirect(method = "getModel",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/PlayerListEntry;model:Ljava/lang/String;")
    )
    private String getTextureModel(PlayerListEntry sender) {
        return skins.getModel();
    }

    @Redirect(method = "loadTextures",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/texture/PlayerSkinProvider;loadSkin("
                            + "Lcom/mojang/authlib/GameProfile;"
                            + "Lnet/minecraft/client/texture/PlayerSkinProvider$SkinTextureAvailableCallback;"
                            + "Z)V")
    )
    private void redirectLoadPlayerTextures(PlayerSkinProvider sender, GameProfile profile, PlayerSkinProvider.SkinTextureAvailableCallback callback, boolean requireSecure) {
        skins.load(sender, profile, requireSecure);
    }
}
