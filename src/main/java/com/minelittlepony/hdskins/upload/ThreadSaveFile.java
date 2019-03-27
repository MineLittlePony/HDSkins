package com.minelittlepony.hdskins.upload;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.io.File;

/**
 * Opens an awt "Save File" dialog
 */
@Deprecated
class ThreadSaveFile extends ThreadOpenFile {

    private String filename;

    ThreadSaveFile(String dialogTitle, String initialFilename) throws IllegalStateException {
        super(dialogTitle);
        this.filename = initialFilename;
    }

    @Override
    protected int showDialog(JFileChooser chooser) {
        do {
            chooser.setSelectedFile(new File(filename));

            int result = chooser.showSaveDialog(null);

            File f = chooser.getSelectedFile();
            if (result == 0 && f != null && f.exists()) {

                if (JOptionPane.showConfirmDialog(chooser,
                        "A file named \"" + f.getName() + "\" already exists. Do you want to replace it?", "Confirm",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                    continue;
                }

                f.delete();
            }


            return result;
        } while (true);
    }
}
