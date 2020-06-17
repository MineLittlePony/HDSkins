package com.minelittlepony.hdskins.client.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.hdskins.client.upload.FileDialog;
import com.minelittlepony.hdskins.util.net.FileTypes;

public class FileSaverScreen extends FileSelectorScreen {

    private Button saveBtn;

    private String savingFileName;

    public FileSaverScreen(String title, String filename) {
        super(title);

        savingFileName = filename;

        if (Files.isDirectory(currentDirectory)) {
            currentDirectory = currentDirectory.resolve(savingFileName);
        } else {
            currentDirectory = currentDirectory.getParent().resolve(savingFileName);
        }
    }

    @Override
    protected void init() {
        super.init();

        addButton(saveBtn = new Button(width/2 - 50, height - 25, 100, 20))
            .onClick(p -> {
                currentDirectory = Paths.get(textInput.getText());

                if (Files.exists(currentDirectory)) {
                    minecraft.openScreen(new ConfirmationScreen(this, "Ovewrite file?", () -> {
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

    @Override
    public FileDialog filter(String extension, String description) {
        currentDirectory = FileTypes.changeExtension(currentDirectory, extension);
        return super.filter(extension, description);
    }

    protected void updateButtonStates(String value) {
        Path selection = Paths.get(value.trim());

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

    @Override
    public void navigateTo(Path path) {
        Path userInput = Paths.get(textInput.getText());
        String fileName = "";

        if (userInput != null) {
            fileName = userInput.getFileName().toString().trim();
        }
        if (fileName.isEmpty()) {
            fileName = savingFileName;
        } else {
            savingFileName = fileName;
        }

        if (path != null && Files.isDirectory(path)) {
            if (!Files.isDirectory(currentDirectory)) {
                if (path.equals(currentDirectory.getParent()) && path.getParent() != null) {
                    path = path.getParent();
                }
            }

            path = path.resolve(savingFileName);

            if (!extensionFilter.isEmpty()) {
                path = FileTypes.changeExtension(path, extensionFilter);
            }

            onDirectorySelected(path.toAbsolutePath());
        } else {
            super.navigateTo(path);
        }
    }

}
