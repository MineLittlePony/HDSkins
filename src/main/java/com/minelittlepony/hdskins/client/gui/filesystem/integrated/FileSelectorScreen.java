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

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;


public class FileSelectorScreen extends GameGui implements FileDialog {

    private static final Identifier ICONS = HDSkins.id("textures/gui/files.png");

    private static final TextureSprite FOLDER = new TextureSprite();
    private static final TextureSprite FILE = new TextureSprite() .setTextureOffset( 0, 14);
    private static final TextureSprite IMAGE = new TextureSprite().setTextureOffset(14, 0);
    private static final TextureSprite AUDIO = new TextureSprite().setTextureOffset(14, 14);
    private static final TextureSprite VIDEO = new TextureSprite().setTextureOffset(28, 14);

    protected Path currentDirectory;

    private FileDialog.Callback callback = (_, _) -> {};

    private final GridPacker packer = new GridPacker()
            .setItemWidth(150)
            .setItemHeight(20);

    protected Button parentBtn;

    protected EditBox textInput;

    protected final ScrollContainer filesList = new ScrollContainer();

    protected String extensionFilter = "";
    private String filterMessage = "";

    public FileSelectorScreen(String title) {
        super(Component.literal(title));

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

        addButton(textInput = new EditBox(getFont(), 10, 30, width - 50, 18, CommonComponents.EMPTY));
        textInput.setEditable(true);
        textInput.setMaxLength(Integer.MAX_VALUE);
        textInput.setValue(currentDirectory.toAbsolutePath().toString());
        addButton(new Button(width - 30, 29, 20, 20))
            .onClick(_ -> navigateTo(Paths.get(textInput.getValue())))
            .getStyle()
                .setText("hdskins.directory.go");

        addButton(new Label(width/2, 5).setCentered())
            .getStyle()
            .setText(getTitle().getString());

        addButton(parentBtn = new Button(width/2 - 160, height - 25, 100, 20))
            .onClick(_ -> navigateTo(currentDirectory.getParent()))
            .setEnabled(canNavigateUp())
            .getStyle()
                .setText("hdskins.directory.up");

        addButton(new Button(width/2 + 60, height - 25, 100, 20))
            .onClick(_ -> finish())
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
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(context, mouseX, mouseY, tickDelta);
        filesList.extractRenderState(context, mouseX, mouseY, tickDelta);
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
        textInput.setValue(path.toString());
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

        minecraft.gui.setScreen(parent);
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

            Component name = Component.literal(path.getFileName().toString().replace(ChatFormatting.PREFIX_CODE, '?'));
            MutableComponent format = describeFile(path);
            format.setStyle(format.getStyle().withColor(ChatFormatting.GRAY).withItalic(true));

            TextureSprite sprite = getIcon(path)
                    .setPosition(6, 6)
                    .setTexture(ICONS)
                    .setTextureSize(53, 53)
                    .setSize(13, 11);

            onClick(_ -> onPathSelected(this));
            setEnabled(Files.isReadable(path));
            getStyle()
                .setText(trimLabel(name.getString()))
                .setIcon(sprite)
                .setTooltip(Lists.newArrayList(name, format));
        }

        private String trimLabel(String name) {

            int maxWidth = width - 35;

            if (getFont().width(name) > maxWidth) {
                name = getFont().plainSubstrByWidth(name, maxWidth - getFont().width("...")) + "...";
            }

            return name.replace("%", "%%");
        }

        public void clearFocus() {
            setFocused(false);
        }

        protected MutableComponent describeFile(Path path) {
            if (Files.isDirectory(path)) {
                return Component.translatable("hdskins.filetype.directory");
            }

            String extension = FileTypes.getExtension(path);

            if (extension.isEmpty()) {
                return Component.translatable("hdskins.filetype.unknown");
            }

            return Component.translatable("hdskins.filetype.file", extension.toUpperCase());
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
        Minecraft.getInstance().gui.setScreen(this);
        return this;
    }
}
