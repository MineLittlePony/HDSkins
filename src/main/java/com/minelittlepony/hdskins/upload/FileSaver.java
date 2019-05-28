package com.minelittlepony.hdskins.upload;

import javax.swing.JOptionPane;

import java.awt.FileDialog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens an awt "Save File" dialog
 */
class FileSaver extends FileChooser {

    private String filename;

    FileSaver(String dialogTitle, String initialFilename) throws IllegalStateException {
        super(dialogTitle);
        this.filename = initialFilename;
    }

    @Override
    Path showDialog(FileDialog dialog) throws IOException {

        dialog.setFile(filename);

        do {
            Path f = super.showDialog(dialog);

            if (Files.exists(f)) {

                if (JOptionPane.showConfirmDialog(dialog,
                        "A file named \"" + f.toString() + "\" already exists. Do you want to replace it?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                    continue;
                }

                Files.delete(f);

                return f;
            }
        } while (true);
    }
}
