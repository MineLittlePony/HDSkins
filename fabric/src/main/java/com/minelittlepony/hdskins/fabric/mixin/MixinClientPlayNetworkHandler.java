package com.minelittlepony.hdskins.fabric.mixin;

import com.minelittlepony.hdskins.fabric.callback.ClientLogInCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Shadow
    private MinecraftClient client;

    @Inject(method = "onGameJoin", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;afterSpawn()V"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        ClientLogInCallback.EVENT.invoker().onClientLogIn((ClientPlayNetworkHandler) (Object) this);
    }
}
