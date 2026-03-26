package com.minelittlepony.hdskins.client.gui.filesystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;

/**
 * Wrapper around GLFW to handle file drop events.
 */
public class FileDrop {
    public static FileDrop newDropEvent(Callback callback) {
        return new FileDrop(callback);
    }

    private boolean cancelled;

    @Nullable
    private GLFWDropCallback hook;

    private final Callback callback;
    private final GLFWDropCallbackI nativ = this::invoke;

    FileDrop(Callback callback) {
        this.callback = callback;
    }

    void invoke(long window, int count, long names) {
        PointerBuffer charPointers = MemoryUtil.memPointerBuffer(names, count);

        List<Path> paths = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            paths.add(Paths.get(MemoryUtil.memUTF8(charPointers.get(i))));
        }

        callback.onDrop(paths);
    }

    /**
     * Starts listening for drop events.
     */
    public FileDrop subscribe() {
        if (!cancelled && hook == null) {
            Minecraft.getInstance().execute(() -> {
                if (!cancelled) {
                    Window window = Minecraft.getInstance().getWindow();
                    hook = GLFW.glfwSetDropCallback(window.handle(), nativ);
                }
            });
        }

        return this;
    }

    public void cancel() {
        cancelled = true;

        if (hook != null) {
            Minecraft.getInstance().execute(() -> {
                Window window = Minecraft.getInstance().getWindow();
                hook = GLFW.glfwSetDropCallback(window.handle(), null);
            });
        }
    }

    @FunctionalInterface
    public interface Callback {
        void onDrop(List<Path> paths);
    }
}
