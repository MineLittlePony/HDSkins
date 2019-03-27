package com.minelittlepony.hdskins.upload;

import net.minecraft.client.Minecraft;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.StringUtils;

import com.minelittlepony.hdskins.AbstractConfig;
import com.minelittlepony.hdskins.HDSkins;

/**
 * Base class for "open file" dialog threads
 *
 * @author Adam Mummery-Smith
 */
@Deprecated
class ThreadOpenFile extends Thread implements IFileDialog {

    private String dialogTitle;

    private String extension;

    private String description;

    /**
     * Delegate to call back when the dialog box is closed
     */
    private Callback callback;

    ThreadOpenFile(String dialogTitle) throws IllegalStateException {
        if (Minecraft.getInstance().mainWindow.isFullscreen()) {
            throw new IllegalStateException("Cannot open an awt window whilst minecraft is in full screen mode!");
        }

        this.dialogTitle = dialogTitle;
    }

    @Override
    public IFileDialog andThen(Callback callback) {
        this.callback = callback;

        start();

        return this;
    }

    public IFileDialog filter(String extension, String description) {
        this.extension = extension;
        this.description = description;

        return this;
    }

    @Override
    public IFileDialog launch() {
        start();
        return this;
    }

    @Override
    public void run() {
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setDialogTitle(dialogTitle);

        AbstractConfig config = HDSkins.getInstance().getConfig();

        String last = config.lastChosenFile;
        if (!StringUtils.isBlank(last)) {
            fileDialog.setSelectedFile(new File(last));
        }
        fileDialog.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
            }

            @Override
            public String getDescription() {
                return description;
            }
        });

        int dialogResult = showDialog(fileDialog);

        File f = fileDialog.getSelectedFile();

        if (f != null) {
            config.lastChosenFile = f.getAbsolutePath();
            config.save();

            if (!f.exists() && f.getName().indexOf('.') == -1) {
                f = appendExtension(f);
            }
        }

        callback.onDialogClosed(f.toPath(), dialogResult == 0);
    }

    protected int showDialog(JFileChooser chooser) {
        return chooser.showOpenDialog(null);
    }

    protected File appendExtension(File file) {
        if (!file.getName().endsWith(extension)) {
            return new File(file.getParentFile(), file.getName() + extension);
        }
        
        return file;
    }
}
