package com.minelittlepony.hdskins.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.include.com.google.common.base.Strings;

import com.google.common.base.Suppliers;
import com.google.common.io.CharStreams;
import com.minelittlepony.hdskins.HDSkinsServer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public final class PlatformVersionUtil {
    private static final Supplier<Version> MINECRAFT_VERSION = Suppliers.memoize(() -> FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().getMetadata().getVersion());
    private static final Supplier<String> LOADER_VERSION = Suppliers.memoize(() -> {
        var loader = FabricLoader.getInstance().getModContainer("fabricloader");
        var connector = FabricLoader.getInstance().getModContainer("connector");
        var fabricapi = FabricLoader.getInstance().getModContainer("fabric-api");
        var forge = FabricLoader.getInstance().getModContainer("forge");
        var neoforge = FabricLoader.getInstance().getModContainer("neoforge");
        return Stream.of(loader, connector, fabricapi, forge, neoforge).flatMap(Optional::stream).map(c -> {
            return c.getMetadata().getId() + " " + c.getMetadata().getVersion();
        }).collect(Collectors.joining("; "));
    });
    private static final Supplier<Version> JAVA_VERSION = Suppliers.memoize(() -> {
        return FabricLoader.getInstance().getModContainer("java").orElseThrow().getMetadata().getVersion();
    });
    private static final Supplier<String> USER_AGENT = Suppliers.memoize(() -> {
        String osName = Util.getPlatform().telemetryName();
        String osVersion = System.getProperty("os.version");
        @Nullable String launcherBrand = Minecraft.getLauncherBrand();
        String clientBrand = ClientBrandRetriever.getClientModName();
        String loaderVersion = getLoaderVersion();

        return String.format("%s/%s (%s %s; Java %s) Minecraft/%s (%s)",
                HDSkinsServer.DEFAULT_NAMESPACE, HDSkinsServer.getInstance().getVersion(),
                osName, osVersion, getJavaVersion(),
                getMinecraftVersion(),
                combineParts(launcherBrand, combineParts(clientBrand, loaderVersion))
        );
    });

    public static boolean isDev() {
        return "true".equals(System.getProperty("fabric.development"));
    }

    public static Version getGitVersion() {
        try (var reader = new InputStreamReader(new ProcessBuilder().command("git", "describe", "--tags").start().getInputStream())) {
            return Version.parse(CharStreams.toString(reader).trim() + "-DEV");
        } catch (IOException | VersionParsingException e) {
            HDSkinsServer.LOGGER.error(e);
        }
        try {
            return Version.parse("0.0.0-DEV");
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Version getMinecraftVersion() {
        return MINECRAFT_VERSION.get();
    }

    public static String getLoaderVersion() {
        return LOADER_VERSION.get();
    }

    public static Version getJavaVersion() {
        return JAVA_VERSION.get();
    }

    public static String getUserAgent() {
        return USER_AGENT.get();
    }

    private static String combineParts(String a, String b) {
        return Strings.isNullOrEmpty(a) ? b : Strings.isNullOrEmpty(b) ? a : a + "; " + b;
    }
}
