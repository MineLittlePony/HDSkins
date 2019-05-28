package com.minelittlepony.hdskins.upload;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.minelittlepony.hdskins.AbstractConfig;
import com.minelittlepony.hdskins.HDSkins;

class FileChooser extends Thread implements IFileDialog {

    private String dialogTitle;

    private String extension;

    /**
     * Delegate to call back when the dialog box is closed
     */
    private Callback callback;

    FileChooser(String dialogTitle) throws IllegalStateException {
        this.dialogTitle = dialogTitle;
    }

    @Override
    public IFileDialog andThen(Callback callback) {
        this.callback = callback;

        return this;
    }

    public IFileDialog filter(String extension, String description) {
        this.extension = extension;

        return this;
    }

    @Override
    public IFileDialog launch() {
        start();
        return this;
    }

    @Override
    public void run() {
        try {
            Field h = GraphicsEnvironment.class.getDeclaredField("headless");
            h.setAccessible(true);
            h.set(null, false);

            FileDialog fileDialog = new FileDialog((Frame)null, dialogTitle);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File f, String arg1) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
                }
            });

            AbstractConfig config = HDSkins.getInstance().getConfig();

            String last = config.lastChosenFile;
            if (!StringUtils.isBlank(last)) {
                fileDialog.setFile(last);
            }

            Path f = showDialog(fileDialog);

            config.lastChosenFile = f.toAbsolutePath().toString();
            config.save();

            if (!Files.exists(f) && Files.isDirectory(f)) {
                f = appendExtension(f);
            }

            callback.onDialogClosed(f, true);
        } catch (Throwable t) {
            t.printStackTrace();
            callback.onDialogClosed(Paths.get(""), false);
        }
    }

    Path showDialog(FileDialog dialog) throws IOException {
        dialog.setVisible(true);

        return Paths.get(Strings.nullToEmpty(dialog.getFile()));
    }

    Path appendExtension(Path file) {
        if (!file.getFileName().endsWith(extension)) {
            return file.getParent().resolve(file.getFileName() + "." + extension);
        }

        return file;
    }
}
