package com.minelittlepony.hdskins.client.gui.filesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WatchedFile {
    private static final Logger logger = LogManager.getLogger();

    private final Object locker = new Object();

    @Nullable
    private Path pending;
    @Nullable
    private Path path;
    @Nullable
    private WatchKey key;

    private Consumer<Path> onChange;
    private Runnable onRemove;

    public WatchedFile(Consumer<Path> change, Runnable remove) {
        onChange = change;
        onRemove = remove;
    }

    public boolean isPending() {
        return pending != null;
    }

    public boolean isSet() {
        return path != null;
    }

    @Nullable
    public URI toUri() {
        return path == null ? null : path.toUri();
    }

    public void set(Path newFile) {
        onRemove.run();
        synchronized (locker) {
            this.pending = newFile;

            try {
                clearKey();
                WatchService service = newFile.getParent().getFileSystem().newWatchService();
                key = newFile.getParent().register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            } catch (IOException e) {
                clearKey();
                logger.error(e);
            }
        }
    }

    private void clearKey() {
        if (key != null) {
            key.cancel();
            key = null;
        }
    }

    public void update() {
        synchronized (locker) {
            if (key != null && key.isValid()) {
                for (WatchEvent<?> ev : key.pollEvents()) {
                    Object context = ev.context();
                    Kind<?> kind = ev.kind();

                    if (context instanceof Path && (pending == null ? path : pending).endsWith((Path)context)) {
                        if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                            pending = null;
                            clearKey();
                            onRemove.run();
                            break;
                        } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
                            if (pending == null) {
                                pending = path;
                            }
                        }
                    }
                }
            }


            if (pending != null && Files.exists(pending)) {
                path = pending;
                pending = null;
                onChange.accept(path);
            }
        }
    }
}
