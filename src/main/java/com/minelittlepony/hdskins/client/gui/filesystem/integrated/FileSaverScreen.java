package com.minelittlepony.hdskins.client.gui.filesystem.integrated;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.gui.ConfirmationScreen;
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialog;
import com.minelittlepony.hdskins.util.net.FileTypes;

import net.minecraft.text.Text;

public class FileSaverScreen extends FileSelectorScreen {
    public static final Text SAVE_OVERWRITE = Text.translatable("hdskins.save.overwrite");
    public static final Text SAVE_READONLY = Text.translatable("hdskins.save.readonly");

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
                try {
                    currentDirectory = Paths.get(textInput.getText());

                    if (Files.exists(currentDirectory)) {
                        client.setScreen(new ConfirmationScreen(this, SAVE_OVERWRITE, () -> {
                            navigateTo(currentDirectory);
                        }));
                    } else {
                        onFileSelected(currentDirectory);
                    }
                } catch (InvalidPathException ignored) {}
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
        try {
            Path selection = Paths.get(value.trim());

            saveBtn.setEnabled(selection != null && !Files.isDirectory(selection));
        } catch (InvalidPathException ignored) {}
    }

    @Override
    protected void onPathSelected(PathButton sender) {

        if (Files.isDirectory(sender.path)) {
            super.onPathSelected(sender);
        } else {
            filesList.buttons().forEach(p -> ((PathButton)p).clearFocus());

            focusOn(sender);
            saveBtn.setEnabled(true);
            textInput.setText(sender.path.toString());
        }
    }

    @Override
    protected void onFileSelected(Path fileLocation) {
        Path parent = fileLocation.getParent();
        Path name = fileLocation.getFileName();

        if (parent != null && name != null && !Files.isWritable(parent)) {
            client.setScreen(new ConfirmationScreen(this, SAVE_READONLY, () -> {
                onDirectorySelected(GamePaths.getGameDirectory().resolve(name));
                /*
                FileDialogs.NATIVE.save("Save File", name.toString()).startIn(parent).andThen((p, success) -> {
                    if (success) {
                        super.onFileSelected(p);
                    } else {
                        finish();
                    }
                }).launch();*/
            }));
            return;
        }

        super.onFileSelected(fileLocation);
    }

    @Override
    public void navigateTo(Path path) {
        Path userInput = Paths.get(textInput.getText());
        String fileName = "";

        if (userInput != null) {
            userInput = userInput.getFileName();
            if (userInput != null) {
                fileName = userInput.getFileName().toString().trim();
            }
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

            path = path.toAbsolutePath();

            onDirectorySelected(path);
        } else {
            super.navigateTo(path);
        }
    }
}
