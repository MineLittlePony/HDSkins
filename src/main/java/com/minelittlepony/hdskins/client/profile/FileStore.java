package com.minelittlepony.hdskins.client.profile;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.hash.Hashing;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTexture;
import com.minelittlepony.hdskins.client.resources.TextureLoader;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

class FileStore {
    private final Map<String, CompletableFuture<Identifier>> cache = new Object2ObjectOpenHashMap<>();

    public CompletableFuture<Identifier> get(SkinType type, MinecraftProfileTexture texture) {
        String key = texture.getHash();
        CompletableFuture<Identifier> entry = cache.get(key);
        if (entry == null) {
            entry = store(type, texture);
            cache.put(key, entry);
        }
        return entry;
    }

    private CompletableFuture<Identifier> store(SkinType type, MinecraftProfileTexture texture) {
        @SuppressWarnings("deprecation")
        String hash = Hashing.sha1().hashUnencodedChars(texture.getHash()).toString();
        Identifier id = new Identifier("hdskins", type.getPathName() + "/" + hash);
        Path path = getHDSkinsCache().resolve(type.getPathName()).resolve(hash.length() > 2 ? hash.substring(0, 2) : "xx").resolve(hash);
        CompletableFuture<Identifier> future = new CompletableFuture<>();
        TextureLoader.loadTexture(id, new HDPlayerSkinTexture(
                path.toFile(),
                texture.getUrl(),
                type,
                DefaultSkinHelper.getTexture(),
                () -> future.complete(id)));
        return future;
    }

    private Path getHDSkinsCache() {
        return GamePaths.getAssetsDirectory().resolve("hd");
    }

    public void clear() {
        try {
            Path cachePath = getHDSkinsCache();
            if (Files.exists(cachePath)) {
                Files.walkFileTree(cachePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            cache.clear();
        } catch (IOException e) {
            HDSkins.LOGGER.warn("Could not remove cache folder", e);
        }
    }
}
