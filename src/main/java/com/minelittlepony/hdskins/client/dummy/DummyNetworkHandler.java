package com.minelittlepony.hdskins.client.dummy;

import com.mojang.authlib.GameProfile;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.SetTag;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

class DummyNetworkHandler extends ClientPlayNetworkHandler {
    public DummyNetworkHandler(GameProfile profile) {
        super(MinecraftClient.getInstance(),
                null,
                new ClientConnection(NetworkSide.CLIENTBOUND), profile);

        try {
            BlockTags.CLIMBABLE.contains(Blocks.LADDER);
        } catch (IllegalStateException ignored) {
            BlockTags.setContainer(new EmptyTagContainer<>(Registry.BLOCK));
            ItemTags.setContainer(new EmptyTagContainer<>(Registry.ITEM));
            FluidTags.setContainer(new EmptyTagContainer<>(Registry.FLUID));
            EntityTypeTags.setContainer(new EmptyTagContainer<>(Registry.ENTITY_TYPE));
        }
    }

    static final class EmptyTagContainer<T> extends TagContainer<T> {
        private final Tag<T> empty = SetTag.empty();
        public EmptyTagContainer(Registry<T> registry) {
            super(registry::getOrEmpty, "", "");
         }

        @Override
        public Tag<T> get(Identifier id) {
            return empty;
        }
    }

}
