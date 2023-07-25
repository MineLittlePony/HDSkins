package com.minelittlepony.hdskins.client.gui.filesystem.integrated;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

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
import com.minelittlepony.hdskins.client.gui.filesystem.FileDialog;
import com.minelittlepony.hdskins.client.gui.filesystem.FileSystemUtil;
import com.minelittlepony.hdskins.util.net.FileTypes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class FileSelectorScreen extends GameGui implements FileDialog {

    private static final Identifier ICONS = new Identifier("hdskins", "textures/gui/files.png");

    private static final TextureSprite FOLDER = new TextureSprite();
    private static final TextureSprite FILE = new TextureSprite() .setTextureOffset( 0, 14);
    private static final TextureSprite IMAGE = new TextureSprite().setTextureOffset(14, 0);
    private static final TextureSprite AUDIO = new TextureSprite().setTextureOffset(14, 14);
    private static final TextureSprite VIDEO = new TextureSprite().setTextureOffset(28, 14);

    protected Path currentDirectory;

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
        super(Text.literal(title));

        filesList.margin.top = 60;
        filesList.margin.bottom = 30;

        filesList.getContentPadding().setAll(10);

        currentDirectory = HDSkins.getInstance().getConfig().lastChosenFile.get();
        if (!Files.exists(currentDirectory)) {
            currentDirectory = FileSystemUtil.getUserContentDirectory(FileSystemUtil.CONTENT_TYPE_DOWNLOAD);
        }
    }

    @Override
    protected void init() {
        getChildElements().add(filesList);

        renderDirectory();

        addButton(textInput = new TextFieldWidget(getFont(), 10, 30, width - 50, 18, ScreenTexts.EMPTY));
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
            .onClick(p -> finish())
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
    public void finish() {
        super.finish();
        callback.onDialogClosed(currentDirectory, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, partialTicks);

        filesList.render(matrices, mouseX, mouseY, partialTicks);
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
        if (directory == null) {
            directory = FileSystemUtil.getUserContentDirectory(FileSystemUtil.CONTENT_TYPE_DOWNLOAD);
        }

        try {
            return Files.list(directory).filter(this::filterPath);
        } catch (IOException e) {

        }

        return Stream.empty();
    }

    protected boolean filterPath(@Nullable Path path) {
        try {
            if (path == null || Files.isHidden(path)) {
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

        client.setScreen(parent);
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

            Text name = Text.literal(path.getFileName().toString().replace(Formatting.FORMATTING_CODE_PREFIX, '?'));
            MutableText format = describeFile(path);
            format.setStyle(format.getStyle().withColor(Formatting.GRAY).withItalic(true));

            TextureSprite sprite = getIcon(path)
                    .setPosition(6, 6)
                    .setTexture(ICONS)
                    .setTextureSize(53, 53)
                    .setSize(13, 11);

            onClick(self -> onPathSelected(this));
            setEnabled(Files.isReadable(path));
            getStyle()
                .setText(trimLabel(name.getString()))
                .setIcon(sprite)
                .setTooltip(Lists.newArrayList(name, format));
        }

        private String trimLabel(String name) {

            int maxWidth = width - 35;

            if (getFont().getWidth(name) > maxWidth) {
                name = getFont().trimToWidth(name, maxWidth - getFont().getWidth("...")) + "...";
            }

            return name.replace("%", "%%");
        }

        public void clearFocus() {
            setFocused(false);
        }

        protected MutableText describeFile(Path path) {
            if (Files.isDirectory(path)) {
                return Text.translatable("hdskins.filetype.directory");
            }

            String extension = FileTypes.getExtension(path);

            if (extension.isEmpty()) {
                return Text.translatable("hdskins.filetype.unknown");
            }

            return Text.translatable("hdskins.filetype.file", extension.toUpperCase());
        }
    }

    @Override
    public FileDialog startIn(Path currentDirectory) {
        this.currentDirectory = currentDirectory;
        return this;
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
        MinecraftClient.getInstance().setScreen(this);
        return this;
    }
}
