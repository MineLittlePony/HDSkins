package com.minelittlepony.hdskins.client.forward;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ForwardingTextureManager extends TextureManager {
    private final TextureManager textureManager;

    public ForwardingTextureManager(TextureManager textureManager) {
        super(null);
        this.textureManager = textureManager;
    }

    @Override
    public void bindTexture(Identifier id) {
        textureManager.bindTexture(id);
    }

    @Override
    public void registerTexture(Identifier identifier, AbstractTexture abstractTexture) {
        textureManager.registerTexture(identifier, abstractTexture);
    }

    @Override
    @Nullable
    public AbstractTexture getTexture(Identifier id) {
        return textureManager.getTexture(id);
    }

    @Override
    public Identifier registerDynamicTexture(String prefix, NativeImageBackedTexture texture) {
        return textureManager.registerDynamicTexture(prefix, texture);
    }

    @Override
    public CompletableFuture<Void> loadTextureAsync(Identifier id, Executor executor) {
        return textureManager.loadTextureAsync(id, executor);
    }

    @Override
    public void tick() {
        textureManager.tick();
    }

    @Override
    public void destroyTexture(Identifier id) {
        textureManager.destroyTexture(id);
    }

    @Override
    public void close() {
        textureManager.close();
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return textureManager.reload(synchronizer, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor);
    }
}
