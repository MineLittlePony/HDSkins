package com.minelittlepony.hdskins;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.minelittlepony.hdskins.client.HDConfig;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServerList;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.SessionService;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class HDSkinsServer implements ModInitializer {
    public static final String DEFAULT_NAMESPACE = "hdskins";

    public static final Logger LOGGER = LogManager.getLogger();

    private static HDSkinsServer instance;

    public static HDSkinsServer getInstance() {
        if (instance == null) {
            instance = new HDSkinsServer();
        }
        return instance;
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(DEFAULT_NAMESPACE, name);
    }

    private Supplier<SessionService> sessionServiceSupplier = () -> null;

    private final SkinServerList servers = new SkinServerList();

    private final BufferedCache<GameProfile, Map<SkinType, MinecraftProfileTexture>> profileLoader = new BufferedCache<>(
            HDConfig.getInstance().skinBatching.get(),
            servers::fillProfiles
    );

    public HDSkinsServer() {
        instance = this;
    }

    public SkinServerList getServers() {
        return servers;
    }

    public void setBatchingDelay(long ticks) {
        profileLoader.setLoadDelay(ticks);
    }

    public CompletableFuture<Map<SkinType, MinecraftProfileTexture>> fillProfile(GameProfile profile) {
        return profileLoader.apply(profile);
    }

    public void setSessionService(Supplier<SessionService> serviceSupplier) {
        sessionServiceSupplier = serviceSupplier;
    }

    public SessionService getSessionService() {
        return Objects.requireNonNull(sessionServiceSupplier.get(), "getSessionService called too early");
    }

    @Override
    public void onInitialize() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SkinServerList.SKIN_SERVERS, servers);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ServerLifecycleEvents.SERVER_STARTING.register(server -> {
                setSessionService(() -> server.services().sessionService());
            });
            HDConfig.getInstance().onChangedExternally(_ -> {
                setBatchingDelay(HDConfig.getInstance().skinBatching.get());
            });
        }
    }
}
