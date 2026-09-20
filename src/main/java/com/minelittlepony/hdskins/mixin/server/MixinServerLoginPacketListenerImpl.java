package com.minelittlepony.hdskins.mixin.server;

import com.minelittlepony.hdskins.HDSkinsServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerLoginPacketListenerImpl.class)
public class MixinServerLoginPacketListenerImpl {
    @ModifyArg(method = "handleHello", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;startClientVerification(Lcom/mojang/authlib/GameProfile;)V",
        ordinal = 0
    ))
    private GameProfile hdskins$authenticationThreadRun(GameProfile singleplayerProfile) {
        return HDSkinsServer.getInstance().getServers().fillProfileServerSide(singleplayerProfile);
    }
}
