package com.minelittlepony.hdskins.client.file;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.minelittlepony.hdskins.client.HDSkins;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

abstract class AbstractFileDialog implements FileDialog {

    private static final Executor executor = Executors.newSingleThreadExecutor();

    protected final String title;
    protected Path currentDirectory;

    @Nullable
    protected List<String> extensionFilters;
    @Nullable
    protected String filterMessage;

    public AbstractFileDialog(String title) {
        this.title = title;
        this.currentDirectory = HDSkins.getInstance().getConfig().lastChosenFile.get();
    }

    @Override
    public FileDialog setDescription(String description) {
        this.filterMessage = description;
        return this;
    }

    @Override
    public FileDialog addExtensionFilter(String extension) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(extension));
        if (extensionFilters == null) {
            extensionFilters = new ArrayList<>();
        }
        extensionFilters.add(extension);
        return this;
    }

    @Nullable
    protected abstract String runFileDialog();

    @Override
    public CompletableFuture<Path> launch() {
        return CompletableFuture.supplyAsync(() -> {
            String file = runFileDialog();
            if (file == null) {
                return null;
            }
            return Paths.get(file);
        }, executor);
    }
}
