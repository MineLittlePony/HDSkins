package com.minelittlepony.hdskins.mixin;

import com.minelittlepony.hdskins.HDSkins;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;

@Mixin(targets = "net.minecraft.server.network.ServerLoginNetworkHandler$1")
public class MixinServerLoginNetworkHandler$1 {

    @Redirect(method = "run",
            at = @At(value = "INVOKE",
                    ordinal = 0,
                    target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;hasJoinedServer(Lcom/mojang/authlib/GameProfile;Ljava/lang/String;Ljava/net/InetAddress;)Lcom/mojang/authlib/GameProfile;"))
    private GameProfile onHasJoinedServer(MinecraftSessionService minecraftSessionService, GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
        GameProfile profile = minecraftSessionService.hasJoinedServer(user, serverId, address);
        if (profile != null && profile.isComplete()) {
            HDSkins.getInstance().getSkinServerList().fillProfileServerTextures(profile);
        }
        return profile;
    }

}
