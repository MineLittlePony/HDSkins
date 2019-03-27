package com.minelittlepony.hdskins.upload;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.Nullable;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.Lists;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;

/**
 * Wrapper around GLFW to handle file drop events.
 */
public class FileDrop {
    public static FileDrop newDropEvent(IDropCallback callback) {
        return new FileDrop(callback);
    }

    private boolean cancelled;

    @Nullable
    private GLFWDropCallback hook;

    private final IDropCallback callback;
    private final GLFWDropCallbackI nativ = this::invoke;

    FileDrop(IDropCallback callback) {
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
     * @return
     */
    public FileDrop subscribe() {
        if (!cancelled && hook == null) {
            Minecraft.getInstance().addScheduledTask(() -> {
                if (!cancelled) {
                    MainWindow window = Minecraft.getInstance().mainWindow;
                    hook = GLFW.glfwSetDropCallback(window.getHandle(), nativ);
                }
            });
        }

        return this;
    }

    public void cancel() {
        cancelled = true;

        if (hook != null) {
            Minecraft.getInstance().addScheduledTask(() -> {
                MainWindow window = Minecraft.getInstance().mainWindow;
                hook = GLFW.glfwSetDropCallback(window.getHandle(), null);
            });
        }
    }

    @FunctionalInterface
    public interface IDropCallback {
        void onDrop(List<Path> paths);
    }
}
