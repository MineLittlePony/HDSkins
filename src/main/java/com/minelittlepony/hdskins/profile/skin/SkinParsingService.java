package com.minelittlepony.hdskins.profile.skin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.minelittlepony.common.util.MoreStreams;
import com.minelittlepony.common.util.TextureConverter;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.ISkinParser;
import com.minelittlepony.hdskins.VanillaModels;
import com.minelittlepony.hdskins.ducks.INetworkPlayerInfo;
import com.minelittlepony.hdskins.mixin.MixinClientPlayer;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.minelittlepony.hdskins.util.ProfileTextureUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;

public class SkinParsingService {

    private final List<ISkinParser> skinParsers = Lists.newArrayList();

    private final List<TextureConverter> skinModifiers = Lists.newArrayList();

    public void addParser(ISkinParser parser) {
        skinParsers.add(parser);
    }

    public void addModifier(TextureConverter modifier) {
        skinModifiers.add(modifier);
    }

    public void modifySkin(TextureConverter.Drawer drawer) {
        skinModifiers.forEach(converter -> converter.convertTexture(drawer));
    }

    public CompletableFuture<Void> parseSkinAsync(GameProfile profile, Type type, Identifier resource, MinecraftProfileTexture texture) {
        return CallableFutures.scheduleTask(() -> {
            // grab the metadata object via reflection. Object is live.
            Map<String, String> metadata = ProfileTextureUtil.getMetadata(texture);

            boolean wasNull = metadata == null;

            if (wasNull) {
                metadata = new HashMap<>();
            } else if (metadata.containsKey("model")) {
                // try to reset the model.
                metadata.put("model", VanillaModels.of(metadata.get("model")));
            }

            for (ISkinParser parser : skinParsers) {
                try {
                    parser.parse(profile, type, resource, metadata);
                } catch (Throwable t) {
                    HDSkins.logger.error("Exception thrown while parsing skin: ", t);
                }
            }

            if (wasNull && !metadata.isEmpty()) {
                ProfileTextureUtil.setMetadata(texture, metadata);
            }
        });
    }

    public void execute() {
        MinecraftClient mc = MinecraftClient.getInstance();

        Streams.concat(getNPCs(mc), getPlayers(mc))

                // filter nulls
                .filter(Objects::nonNull)
                .map(INetworkPlayerInfo.class::cast)
                .distinct()

                // and clear skins
                .forEach(INetworkPlayerInfo::reloadTextures);

    }

    private Stream<PlayerListEntry> getNPCs(MinecraftClient mc) {
        return MoreStreams.ofNullable(mc.world)
                .flatMap(w -> w.getPlayers().stream())
                .filter(AbstractClientPlayerEntity.class::isInstance)
                .map(MixinClientPlayer.class::cast)
                .map(MixinClientPlayer::getBackingClientData);
    }

    private Stream<PlayerListEntry> getPlayers(MinecraftClient mc) {
        return MoreStreams.ofNullable(mc.getNetworkHandler())
                .flatMap(a -> a.getPlayerList().stream());
    }

}
