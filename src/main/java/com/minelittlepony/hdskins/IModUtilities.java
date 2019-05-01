package com.minelittlepony.hdskins;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;

import java.nio.file.Path;
import java.util.function.Function;

public interface IModUtilities {

    <T extends Entity> void addRenderer(Class<T> type, Function<RenderManager, Render<T>> renderer);

    Path getAssetsDirectory();
}
