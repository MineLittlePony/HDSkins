package com.minelittlepony.hdskins;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Suppliers;
import com.minelittlepony.hdskins.client.HDConfig;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServerList;
import com.minelittlepony.hdskins.util.PlatformVersionUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

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
        return Identifier.of(DEFAULT_NAMESPACE, name);
    }

    private Supplier<MinecraftSessionService> sessionServiceSupplier = () -> null;

    private final SkinServerList servers = new SkinServerList();

    private final Supplier<Version> version = Suppliers.memoize(() -> {
        if (PlatformVersionUtil.isDev()) {
            return PlatformVersionUtil.getGitVersion();
        }
        return FabricLoader.getInstance().getModContainer(DEFAULT_NAMESPACE).map(c -> c.getMetadata().getVersion()).orElseThrow();
    });

    private final BufferedCache<GameProfile, Map<SkinType, MinecraftProfileTexture>> profileLoader = new BufferedCache<>(
            HDConfig.getInstance().skinBatching.get(),
            servers::fillProfiles
    );

    public HDSkinsServer() {
        instance = this;
    }

    public Version getVersion() {
        return version.get();
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

    public void setSessionService(Supplier<MinecraftSessionService> serviceSupplier) {
        sessionServiceSupplier = serviceSupplier;
    }

    public MinecraftSessionService getSessionService() {
        return Objects.requireNonNull(sessionServiceSupplier.get(), "getSessionService called too early");
    }

    @Override
    public void onInitialize() {
        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(SkinServerList.SKIN_SERVERS, servers);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            ServerLifecycleEvents.SERVER_STARTING.register(server -> {
                setSessionService(() -> server.getApiServices().sessionService());
                PlatformVersionUtil.setClientBrand(server::getServerModName);
            });
            HDConfig.getInstance().onChangedExternally(c -> {
                setBatchingDelay(HDConfig.getInstance().skinBatching.get());
            });
        }
    }
}
