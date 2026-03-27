package com.minelittlepony.hdskins.client.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.minelittlepony.common.client.gui.GameGui;
import com.minelittlepony.common.client.gui.ScrollContainer;
import com.minelittlepony.common.client.gui.Tooltip;
import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.element.EnumSlider;
import com.minelittlepony.common.client.gui.element.Label;
import com.minelittlepony.common.client.gui.element.Toggle;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.HDSkinsServer;
import com.minelittlepony.hdskins.client.HDConfig;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.server.Gateway;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class SettingsScreen extends GameGui {

    private final ScrollContainer content = new ScrollContainer();

    private final Identifier panoramaTexture;

    private final HDConfig config = HDSkins.getInstance().getConfig();

    public SettingsScreen(@Nullable Screen parent, Identifier panoramaTexture) {
        super(Component.translatable("options.title"), parent);
        this.panoramaTexture = panoramaTexture;

        content.margin.setVertical(30);
        content.getContentPadding().setHorizontal(10);
        content.getContentPadding().top = 10;
        content.getContentPadding().bottom = 20;
    }

    @Override
    public void init() {
        content.init(this::rebuildContent);
    }

    private void rebuildContent() {
        int LEFT = content.width / 2 - 105;

        getChildElements().add(content);

        int row = -20;

        addButton(new Label(width / 2, 5).setCentered()).getStyle().setText(getTitle());
        addButton(new Button(width / 2 - 100, height - 25))
            .onClick(_ -> finish())
            .getStyle()
                .setText("gui.done");

        content.addButton(new EnumSlider<>(LEFT, row += 20, config.pantsButtonVisibility))
            .onChange(config.pantsButtonVisibility)
            .setTextFormat(slider -> Component.translatable("hdskins.settings.main_screen_button", slider.getValue().name()))
            .getStyle().setText(Component.translatable("hdskins.settings.main_screen_button", config.pantsButtonVisibility.get().name()));

        content.addButton(new Button(LEFT, row += 25, 200, 20))
            .onClick(_ -> {
                try {
                    Path path = GamePaths.getAssetsDirectory().resolve("hd");
                    Files.createDirectories(path);
                    Util.getPlatform().openPath(path);
                } catch (IOException e) {
                    HDSkins.LOGGER.error("Could not create cache folder", e);
                }
            })
            .getStyle()
                .setText("hdskins.options.open_cache_folder");

        content.addButton(new Label(width / 2, row += 40).setCentered()).getStyle().setText("hdskins.settings.category.compatibility");

        content.addButton(new Toggle(LEFT, row += 20, config.useNativeFileChooser))
            .onChange(config.useNativeFileChooser)
            .getStyle().setText("hdskins.compatibility.native_file_picker");
        content.addButton(new Toggle(LEFT, row += 20, config.enableSandboxingCheck))
        .onChange(config.enableSandboxingCheck)
        .getStyle().setText("hdskins.compatibility.sandboxing");

        content.addButton(new Label(width / 2, row += 20).setCentered()).getStyle().setText("hdskins.settings.category.experiments");

        content.addButton(new Toggle(LEFT, row += 20, config.useBatchLoading))
            .onChange(config.useBatchLoading)
            .getStyle().setText("hdskins.experiments.batches");

        content.addButton(new Label(width / 2, row += 20).setCentered()).getStyle().setText("hdskins.settings.category.servers");
        row += 10;
        int index = 1;
        for (Gateway gateway : HDSkinsServer.getInstance().getServers().getGateways()) {
            content.addButton(new Label(LEFT, row += getFont().lineHeight))
                .getStyle()
                .setText("#" + (index++));
            for (Component line : Tooltip.of(Component.literal(gateway.getServer().toString()), 300).getLines()) {
                content.addButton(new Label(LEFT, row += getFont().lineHeight))
                    .getStyle()
                    .setText(line);
            }
            Set<Map.Entry<Component, Component>> buttons = new HashSet<>();

            for (var metadata : gateway.getServer().getMetadata().entrySet()) {

                if (metadata.getValue().getStyle().getClickEvent() != null) {
                    buttons.add(metadata);
                } else {
                    for (Component line : Tooltip.of(metadata.getKey().copy().withStyle(ChatFormatting.YELLOW).append(": ").append(metadata.getValue()), 300).getLines()) {
                        content.addButton(new Label(LEFT + 7, row += getFont().lineHeight))
                            .getStyle()
                            .setText(line);
                    }
                }
            }
            row += 20;

            int left = LEFT + 7;
            for (var metadata : buttons) {
                int width = getFont().width(metadata.getKey()) + 10;
                content.addButton(new Button(left, row, width, 20))
                    .onClick(_ -> defaultHandleClickEvent(metadata.getValue().getStyle().getClickEvent(), minecraft, this))
                    .getStyle().setText(metadata.getKey());
                left += width + 2;
            }

            row += 20;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(context, mouseX, mouseY, tickDelta);
        content.extractRenderState(context, mouseX, mouseY, tickDelta);
    }

    @Override
    protected void extractPanorama(GuiGraphicsExtractor context, float delta) {
        super.extractPanorama(context, delta);
        context.guiRenderState.panoramaRenderState.setData(RenderStateKeys.PANORAMA_TEXTURE_KEY, panoramaTexture);
    }

    @Override
    public void removed() {
        config.save();
    }
}
