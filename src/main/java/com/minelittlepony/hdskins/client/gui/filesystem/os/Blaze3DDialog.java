package com.minelittlepony.hdskins.client.gui.filesystem.os;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;

final class Blaze3DDialog {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static CompletableFuture<List<Path>> openFile(@Nullable Path defaultDirectory, @Nullable List<DialogFileFilter> filter, boolean allowMultiple) {
        return openDialogAsync(defaultDirectory, filter, (window, callback, filters, directory) -> {
            SDLDialog.SDL_ShowOpenFileDialog(callback, 0, window, filters, directory, allowMultiple);
        });
    }

    public static CompletableFuture<List<Path>> saveFile(@Nullable Path defaultDirectory, @Nullable List<DialogFileFilter> filter) {
        return openDialogAsync(defaultDirectory, filter, (window, callback, filters, directory) -> {
            SDLDialog.SDL_ShowSaveFileDialog(callback, 0, window, filters, directory);
        });
    }

    private static CompletableFuture<List<Path>> openDialogAsync(@Nullable Path defaultDirectory, @Nullable List<DialogFileFilter> filter, NativeAction invoker) {
        final CompletableFuture<List<Path>> future = new CompletableFuture<>();
        invoker.call(
            Minecraft.getInstance().getWindow().handle(),
            SDL_DialogFileCallback.create((_, filelist, _) -> future.complete(retrieveFileList(filelist))),
            createFilterBuffer(filter),
            defaultDirectory == null ? null : MemoryUtil.memUTF8(defaultDirectory.toString(), true)
        );
        return future;
    }

    @Nullable
    private static SDL_DialogFileFilter.Buffer createFilterBuffer(@Nullable List<DialogFileFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        var sdlFilters = SDL_DialogFileFilter.create(filters.size());
        for (int i = 0; i < filters.size(); i++) {
            var f = filters.get(i);
            sdlFilters = sdlFilters.apply(i, filter -> filter.set(
                    MemoryUtil.memUTF8(f.description(), true),
                    MemoryUtil.memUTF8(f.pattern(), true)
            ));
        }
        return sdlFilters;
    }

    private static List<Path> retrieveFileList(final long memAddress) {
        if (memAddress == MemoryUtil.NULL) {
            LOGGER.error("Failed to open file dialog: " + SDLError.SDL_GetError());
            return List.of();
        }

        List<Path> paths = new ArrayList<>();
        long elementPointer = memAddress;
        long element;
        while (true) {
            element = MemoryUtil.memGetAddress(elementPointer);
            if (element == MemoryUtil.NULL) {
                return paths;
            }
            paths.add(Paths.get(MemoryUtil.memUTF8(element)));
            elementPointer += Pointer.POINTER_SIZE;
        }
    }

    private interface NativeAction {
        void call(long window, SDL_DialogFileCallback callback, SDL_DialogFileFilter.Buffer filters, ByteBuffer defaultDirectory);
    }

    public record DialogFileFilter(String description, String pattern) {}
}
