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
//TODO: No more swing
@Deprecated // no more swing
public abstract class ThreadOpenFile extends Thread implements IFileDialog {

    protected String dialogTitle;

    /**
     * Delegate to call back when the dialog box is closed
     */
    protected final IFileCallback parentScreen;

    protected ThreadOpenFile(Minecraft minecraft, String dialogTitle, IFileCallback callback) throws IllegalStateException {
        if (minecraft.mainWindow.isFullscreen()) {
            throw new IllegalStateException("Cannot open an awt window whilst minecraft is in full screen mode!");
        }

        this.parentScreen = callback;
        this.dialogTitle = dialogTitle;
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
        fileDialog.setFileFilter(getFileFilter());

        int dialogResult = showDialog(fileDialog);

        File f = fileDialog.getSelectedFile();

        if (f != null) {
            config.lastChosenFile = f.getAbsolutePath();
            config.save();

            if (!f.exists() && f.getName().indexOf('.') == -1) {
                f = appendMissingExtension(f);
            }
        }

        parentScreen.onDialogClosed(f, dialogResult);
    }

    protected int showDialog(JFileChooser chooser) {
        return chooser.showOpenDialog(null);
    }

    /**
     * Subclasses should override this to return a file filter
     */
    protected abstract FileFilter getFileFilter();

    protected File appendMissingExtension(File file) {
        return file;
    }
}
