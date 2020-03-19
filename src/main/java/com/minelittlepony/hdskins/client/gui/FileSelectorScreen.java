package com.minelittlepony.hdskins.client.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.ScrollContainer;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.packing.GridPacker;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.hdskins.client.HDConfig;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.upload.FileDialog;
import com.minelittlepony.hdskins.util.net.FileTypes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class FileSelectorScreen extends GameGui implements FileDialog {

    private static final Identifier ICONS = new Identifier("hdskins", "textures/gui/files.png");

    private static final TextureSprite FOLDER = new TextureSprite();
    private static final TextureSprite FILE = new TextureSprite() .setTextureOffset( 0, 14);
    private static final TextureSprite IMAGE = new TextureSprite().setTextureOffset(14, 0);
    private static final TextureSprite AUDIO = new TextureSprite().setTextureOffset(14, 14);
    private static final TextureSprite VIDEO = new TextureSprite().setTextureOffset(28, 14);

    protected Path currentDirectory = Paths.get("/");

    private FileDialog.Callback callback = (f, b) -> {};

    private final GridPacker packer = new GridPacker()
            .setItemWidth(150)
            .setItemHeight(20);

    protected Button parentBtn;

    protected TextFieldWidget textInput;

    protected final ScrollContainer filesList = new ScrollContainer();

    protected String extensionFilter = "";
    private String filterMessage = "";

    public FileSelectorScreen(String title) {
        super(new LiteralText(title));

        filesList.margin.top = 60;
        filesList.margin.bottom = 30;

        filesList.padding.setAll(10);

        currentDirectory = HDSkins.getInstance().getConfig().lastChosenFile.get();
    }

    @Override
    protected void init() {
        children.add(filesList);

        renderDirectory();

        addButton(textInput = new TextFieldWidget(getFont(), 10, 30, width - 50, 18, ""));
        textInput.setEditable(true);
        textInput.setMaxLength(Integer.MAX_VALUE);
        textInput.setText(currentDirectory.toAbsolutePath().toString());
        addButton(new Button(width - 30, 29, 20, 20))
            .onClick(p -> navigateTo(Paths.get(textInput.getText())))
            .getStyle()
                .setText("hdskins.directory.go");

        addButton(new Label(width/2, 5).setCentered())
            .getStyle()
            .setText(getTitle().getString());

        addButton(parentBtn = new Button(width/2 - 160, height - 25, 100, 20))
            .onClick(p -> navigateTo(currentDirectory.getParent()))
            .setEnabled(canNavigateUp())
            .getStyle()
                .setText("hdskins.directory.up");

        addButton(new Button(width/2 + 60, height - 25, 100, 20))
            .onClick(p -> {
                finish();
                callback.onDialogClosed(currentDirectory, false);
            })
            .getStyle()
                .setText("hdskins.options.close");

        if (!filterMessage.isEmpty()) {
            filesList.margin.bottom = 60;

            addButton(new Label(10, height - 55))
                .getStyle()
                    .setColor(0x88EEEEEE)
                    .setText("* " + filterMessage);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground(0);
        super.render(mouseX, mouseY, partialTicks);

        filesList.render(mouseX, mouseY, partialTicks);
    }

    protected void renderDirectory() {
        filesList.init(() -> {
            int buttonX = filesList.width / 2 - 110;

            listFiles().forEach(path -> {
                int buttonY = filesList.buttons().size() * 20;

                filesList.addButton(new PathButton(buttonX, buttonY, 150, 20, path));
            });

            packer.setListWidth(width).pack(filesList);
        });
    }

    protected Stream<Path> listFiles() {
        Path directory = currentDirectory;

        if (!Files.isDirectory(directory)) {
            directory = directory.getParent();
        }

        File[] files = null;
        try {
            files = directory.toFile().listFiles();
        } catch (Throwable e) {}

        if (files == null) {
            return Stream.empty();
        }

        return Lists.newArrayList(files).stream()
                .map(File::toPath)
                .filter(this::filterPath);
    }

    protected boolean filterPath(Path path) {
        try {
            if (Files.isHidden(path)) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return extensionFilter.isEmpty()
                || Files.isDirectory(path)
                || path.getFileName().toString().endsWith(extensionFilter);
    }

    public void navigateTo(Path path) {
        if (path == null) {
            return;
        }

        path = path.toAbsolutePath();

        if (Files.isDirectory(path)) {
            onDirectorySelected(path);
        } else {
            onFileSelected(path);
        }
    }

    protected void onDirectorySelected(Path path) {
        textInput.setText(path.toString());
        currentDirectory = path;

        HDConfig config = HDSkins.getInstance().getConfig();
        config.lastChosenFile.set(path);
        config.save();

        parentBtn.setEnabled(canNavigateUp());
        renderDirectory();
    }

    protected boolean canNavigateUp() {
        return currentDirectory.getParent() != null
                && (Files.isDirectory(currentDirectory) || currentDirectory.getParent().getParent() != null);
    }

    protected void onFileSelected(Path fileLocation) {

        HDConfig config = HDSkins.getInstance().getConfig();
        config.lastChosenFile.set(fileLocation);
        config.save();

        client.openScreen(parent);
        callback.onDialogClosed(fileLocation, true);
    }

    protected void onPathSelected(PathButton sender) {
        navigateTo(sender.path);
    }

    protected TextureSprite getIcon(Path path) {

        if (Files.isDirectory(path)) {
            return FOLDER;
        }

        String mime = FileTypes.getMimeType(path);

        if (mime.contains("image")) {
            return IMAGE;
        }
        if (mime.contains("audio")) {
            return AUDIO;
        }
        if (mime.contains("video")) {
            return VIDEO;
        }

        return FILE;
    }

    class PathButton extends Button {

        protected final Path path;

        public PathButton(int x, int y, int width, int height, Path path) {
            super(x, y, width, height);

            this.path = path;

            String name = path.getFileName().toString();

            TextureSprite sprite = getIcon(path)
                    .setPosition(6, 6)
                    .setTexture(ICONS)
                    .setTextureSize(53, 53)
                    .setSize(14, 11);

            onClick(self -> onPathSelected(this));
            setEnabled(Files.isReadable(path));
            getStyle()
                .setText(trimLabel(name))
                .setIcon(sprite)
                .setTooltip(Lists.newArrayList(
                        name,
                        Formatting.GRAY + "" + Formatting.ITALIC + describeFile(path))
                );
        }

        private String trimLabel(String name) {

            int maxWidth = width - 35;

            if (getFont().getStringWidth(name) > maxWidth) {
                name = getFont().trimToWidth(name, maxWidth - getFont().getStringWidth("...")) + "...";
            }

            return name.replace("%", "%%");
        }

        public void clearFocus() {
            setFocused(false);
        }

        protected String describeFile(Path path) {
            if (Files.isDirectory(path)) {
                return I18n.translate("hdskins.filetype.directory");
            }

            String extension = FileTypes.getExtension(path);

            if (extension.isEmpty()) {
                return I18n.translate("hdskins.filetype.unknown");
            }

            return I18n.translate("hdskins.filetype.file", extension.toUpperCase());
        }
    }

    @Override
    public FileDialog filter(String extension, String description) {
        extensionFilter = Strings.nullToEmpty(extension);
        filterMessage = Strings.nullToEmpty(description);

        if (!filterMessage.isEmpty()) {
            filesList.margin.bottom = 60;
        } else {
            filesList.margin.bottom = 30;
        }
        return this;
    }

    @Override
    public FileDialog andThen(Callback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public FileDialog launch() {
        MinecraftClient.getInstance().openScreen(this);
        return this;
    }

}
