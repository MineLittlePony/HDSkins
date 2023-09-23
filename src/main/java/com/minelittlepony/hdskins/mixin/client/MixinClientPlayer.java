package com.minelittlepony.hdskins.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

@Mixin(AbstractClientPlayerEntity.class)
public interface MixinClientPlayer {
    @Invoker("getPlayerListEntry")
    PlayerListEntry getBackingClientData();
}
