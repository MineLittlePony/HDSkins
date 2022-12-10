package com.minelittlepony.hdskins.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

@Mixin(AbstractClientPlayerEntity.class)
public interface MixinClientPlayer {
    @Nullable
    @Accessor("playerListEntry")
    PlayerListEntry getBackingClientData();
}
