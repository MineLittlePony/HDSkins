package com.minelittlepony.hdskins.server.de.hdskins;

import com.google.common.collect.Sets;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.*;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import de.hdskins.protocol.PacketBase;
import de.hdskins.protocol.client.NetworkClient;
import de.hdskins.protocol.client.handler.auth.MemoryCustomAuthHandler;
import de.hdskins.protocol.client.provider.ClientProvider;
import de.hdskins.protocol.concurrent.FutureListener;
import de.hdskins.protocol.listener.PacketListener;
import de.hdskins.protocol.packets.HDSkinsPacketRegistrar;
import de.hdskins.protocol.packets.core.dashboard.PacketClientRequestDashboardUrl;
import de.hdskins.protocol.packets.core.dashboard.PacketServerResponseDashboardUrl;
import de.hdskins.protocol.packets.core.texture.PacketServerTextureResponse;
import de.hdskins.protocol.packets.core.texture.PacketServerUpdateTexture;
import de.hdskins.protocol.packets.core.view.PacketClientViewEnter;
import de.hdskins.protocol.packets.core.view.PacketClientViewLeave;
import de.hdskins.protocol.packets.texture.Texture;
import de.hdskins.protocol.packets.texture.TextureType;
import de.hdskins.protocol.packets.texture.meta.SkinTextureMeta;
import io.netty.channel.Channel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

@ServerType("hdskins.de")
public class HDSkinsDESkinServer implements SkinServer {

    private static final Set<Feature> FEATURES = Sets.newHashSet(
            Feature.DOWNLOAD_USER_SKIN,
            Feature.UPLOAD_USER_SKIN,
            Feature.DELETE_USER_SKIN,
            Feature.MODEL_VARIANTS,
            Feature.MODEL_TYPES
    );

    private final ClientProvider clientProvider = new ClientProvider(
            NetworkClient.create(
                    "bridge.hdskins.de",
                    7008,
                    new HDSkinsDEClientLogger(),
                    () -> MinecraftClient.getInstance().getSession().getUsername(),
                    serverId -> {
                        try {
                            MinecraftClient.getInstance().getSessionService().joinServer(
                                    MinecraftClient.getInstance().getSession().getUuidOrNull(),
                                    MinecraftClient.getInstance().getSession().getAccessToken(),
                                    serverId
                            );

                            return true;
                        } catch (AuthenticationException exception) {
                            HDSkinsServer.LOGGER.error("Unable to authenticate to account", exception);
                            return false;
                        }
                    }
            ),
            "Fabric",
            SharedConstants.getGameVersion().getName(),
            7
    );

    public HDSkinsDESkinServer() {
        this.clientProvider.getClient().setCustomAuthHandler(new MemoryCustomAuthHandler());
        this.clientProvider.getClient().registerPackets(new HDSkinsPacketRegistrar());

        this.clientProvider.getClient().getPacketListenerRegistry().registerListeners(this);

        this.clientProvider.connect();

        AtomicReference<String> uuid = new AtomicReference<>();
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            UUID currentUniqueId = MinecraftClient.getInstance().getSession().getUuidOrNull();
            if (currentUniqueId == null) {
                return; // no account
            }

            String last = uuid.get();
            if (last == null) {
                uuid.set(currentUniqueId.toString());
            } else if (!last.equals(uuid.get())) {
                this.clientProvider.reconnect();
            } else {
                this.clientProvider.tick();
            }

        });

        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!this.clientProvider.getClient().isConnected()) {
                return;
            }

            this.clientProvider.getClient().sendPacket(new PacketClientViewLeave(entity.getUuid()));
        });
    }

    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public boolean supportsSkinType(SkinType skinType) {
        return skinType.isKnown() && skinType == SkinType.SKIN;
    }

    @Override
    public boolean ownsUrl(String url) {
        return true;
    }

    @Override
    public TexturePayload loadSkins(GameProfile profile) throws IOException, AuthenticationException {
        Map<SkinType, MinecraftProfileTexture> textureMap = new HashMap<>();

        TexturePayload emptyPayload = new TexturePayload(profile, textureMap);
        if (!this.clientProvider.getClient().isConnected()) {
            return emptyPayload;
        }

        CompletableFuture<PacketBase> future = new CompletableFuture<>();
        this.clientProvider.getClient().sendQuery(new PacketClientViewEnter(profile.getId())).addListener(new FutureListener<>() {
            @Override
            public void nullResult() {
                future.complete(null);
            }

            @Override
            public void nonNullResult(@NotNull PacketBase packetBase) {
                PacketServerTextureResponse response = (PacketServerTextureResponse) packetBase;
                future.complete(response);
            }

            @Override
            public void cancelled() {
                future.complete(null);
            }
        });

        try {
            PacketBase result = future.get();
            if (result != null) {
                PacketServerTextureResponse response = (PacketServerTextureResponse) result;

                Texture texture = response.getTextures().get(TextureType.SKIN);
                if (texture == null) {
                    future.complete(null);
                    return emptyPayload;
                }

                SkinTextureMeta meta = (SkinTextureMeta) texture.getTextureMeta();
                boolean slim = meta != null && meta.isSlim();

                textureMap.put(SkinType.SKIN, new MinecraftProfileTexture(texture.getTextureUrl(), Map.of("model", slim ? "slim" : "classic")));
                return new TexturePayload(profile, textureMap);
            }

            return emptyPayload;
        } catch (InterruptedException | ExecutionException e) {
            HDSkins.LOGGER.error("Error while loading skin", e);
        }

        return emptyPayload;
    }

    @Override
    public void uploadSkin(SkinUpload upload) throws IOException, AuthenticationException {
        if (!this.clientProvider.getClient().isConnected()) {
            return;
        }

        CompletableFuture<PacketBase> future = new CompletableFuture<>();
        this.clientProvider.getClient().sendQuery(new PacketClientRequestDashboardUrl()).addListener(new FutureListener<>() {
            @Override
            public void nullResult() {
                future.complete(null);
            }

            @Override
            public void nonNullResult(@NotNull PacketBase packetBase) {
                PacketServerResponseDashboardUrl response = (PacketServerResponseDashboardUrl) packetBase;
                future.complete(response);
            }

            @Override
            public void cancelled() {
                future.complete(null);
            }
        });

        try {
            PacketBase result = future.get();
            if (result != null) {
                PacketServerResponseDashboardUrl response = (PacketServerResponseDashboardUrl) result;
                Util.getOperatingSystem().open(response.getUrl());
            }

        } catch (InterruptedException | ExecutionException e) {
            HDSkins.LOGGER.error("Error while loading skin", e);
        }
    }

    @Override
    public void authorize(SkinUpload.Session session) throws IOException, AuthenticationException {

    }

    @PacketListener
    public void onUpdate(PacketServerUpdateTexture packet, Channel channel) {
        HDSkins.getInstance().getProfileRepository().invalidateByUniqueId(packet.getUniqueId());
    }

    @Override
    public Map<Text, Text> getMetadata() {
        return Map.of(
                Text.literal("Homepage"), Text.literal("https://hdskins.de").formatted(Formatting.UNDERLINE).withColor(Colors.BLUE).styled(style -> {
                    return style.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://hdskins.de")));
                }),
                Text.literal("Discord"), Text.literal("https://discord.hdskins.de").formatted(Formatting.UNDERLINE).withColor(Colors.BLUE).styled(style -> {
                    return style.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://discord.hdskins.de")));
                }),
                Text.translatable("hdskins.label.author"), Text.literal("HDSkins.de")
        );
    }

    @Override
    public String toString() {
        return "HDSkins.de Skin Server";
    }

}
