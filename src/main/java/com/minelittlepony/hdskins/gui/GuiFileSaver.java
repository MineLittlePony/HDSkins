package com.minelittlepony.hdskins.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.minelittlepony.common.client.gui.element.Button;

public class GuiFileSaver extends GuiFileSelector {

    private Button saveBtn;

    public GuiFileSaver(String title, String filename) {
        super(title);

        if (Files.exists(currentDirectory)) {
            if (Files.isDirectory(currentDirectory)) {
                currentDirectory = currentDirectory.resolve(filename);
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        addButton(saveBtn = new Button(width/2 - 50, height - 25, 100, 20))
            .onClick(p -> {
                currentDirectory = Paths.get(textInput.getText());

                if (Files.exists(currentDirectory)) {
                    minecraft.openScreen(new GuiConfirmation(this, "Ovewrite file?", () -> {
                        navigateTo(currentDirectory);
                    }));
                } else {
                    onFileSelected(currentDirectory);
                }
            })
            .getStyle()
                .setText("hdskins.directory.save");

        textInput.setChangedListener(this::updateButtonStates);
        updateButtonStates(textInput.getText());
    }

    protected void updateButtonStates(String value) {
        Path selection = Paths.get(value);

        saveBtn.setEnabled(selection != null && !Files.isDirectory(selection));
    }

    @Override
    protected void onPathSelected(PathButton sender) {

        if (Files.isDirectory(sender.path)) {
            super.onPathSelected(sender);
        } else {
            filesList.buttons().forEach(p -> ((PathButton)p).clearFocus());

            sender.changeFocus(true);
            saveBtn.setEnabled(true);
            textInput.setText(sender.path.toString());
        }
    }

}
