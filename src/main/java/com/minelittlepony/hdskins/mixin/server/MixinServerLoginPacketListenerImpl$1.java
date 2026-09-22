package com.minelittlepony.hdskins.mixin.server;

import com.minelittlepony.hdskins.HDSkinsServer;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.server.network.ServerLoginPacketListenerImpl$1")
public class MixinServerLoginPacketListenerImpl$1 {
    @ModifyArg(method = "run", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;startClientVerification(Lcom/mojang/authlib/GameProfile;)V",
        ordinal = 0
    ))
    private GameProfile hdskins$authenticationThreadRun(GameProfile profile) {
        return HDSkinsServer.getInstance().getServers().fillProfileServerSide(profile);
    }
}
