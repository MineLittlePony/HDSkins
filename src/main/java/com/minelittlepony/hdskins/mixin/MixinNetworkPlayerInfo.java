package com.minelittlepony.hdskins.mixin;

import com.minelittlepony.hdskins.PlayerSkins;
import com.minelittlepony.hdskins.ducks.INetworkPlayerInfo;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import javax.annotation.Nullable;

@Mixin(NetworkPlayerInfo.class)
public abstract class MixinNetworkPlayerInfo implements INetworkPlayerInfo {

    private PlayerSkins hdskinsPlayerSkins = new PlayerSkins(this);

    @Override
    @Accessor("playerTextures")
    public abstract Map<Type, ResourceLocation> getVanillaTextures();

    @Override
    @Accessor("gameProfile")
    public abstract GameProfile getGameProfile();

    @Redirect(method = {"getLocationSkin", "getLocationCape", "getLocationElytra"},
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;",
                    remap = false)
    )
    // synthetic
    private Object getSkin(Map<Type, ResourceLocation> playerTextures, Object key) {
        return hdskinsPlayerSkins.getSkin(playerTextures, (Type) key);
    }

    @Nullable
    @Redirect(method = "getSkinType",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/NetworkPlayerInfo;skinType:Ljava/lang/String;")
    )
    private String getTextureModel(NetworkPlayerInfo self) {
        return hdskinsPlayerSkins.getModel();
    }

    @Inject(method = "loadPlayerTextures",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/SkinManager;loadProfileTextures("
                            + "Lcom/mojang/authlib/GameProfile;"
                            + "Lnet/minecraft/client/resources/SkinManager$SkinAvailableCallback;"
                            + "Z)V",
                    shift = At.Shift.BEFORE)
    )
    private void onLoadTexture(CallbackInfo ci) {
        hdskinsPlayerSkins.fetch();
    }

    @Redirect(method = "loadPlayerTextures",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/SkinManager;loadProfileTextures("
                            + "Lcom/mojang/authlib/GameProfile;"
                            + "Lnet/minecraft/client/resources/SkinManager$SkinAvailableCallback;"
                            + "Z)V")
    )
    private void redirectLoadPlayerTextures(SkinManager skinManager, GameProfile profile, SkinManager.SkinAvailableCallback callback, boolean requireSecure) {
        hdskinsPlayerSkins.load(skinManager, profile, callback, requireSecure);
    }

    @Override
    public void reloadTextures() {
        hdskinsPlayerSkins.reload();
    }
}
