package com.minelittlepony.hdskins.client.gui;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.minelittlepony.hdskins.client.FileDrop;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.SkinChooser;
import com.minelittlepony.hdskins.client.SkinUploader;
import com.minelittlepony.hdskins.client.SkinUploader.ISkinUploadHandler;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.dummy.PlayerPreview;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.Feature;
import com.minelittlepony.hdskins.server.SkinServerList;
import com.minelittlepony.hdskins.util.Edge;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GuiSkins extends ParentScreen implements ISkinUploadHandler, FileDrop.Callback {

    private static BiFunction<Screen, SkinServerList, GuiSkins> skinsGuiFunc = GuiSkins::new;

    /**
     * TODO make this more configurable
     *
     * @deprecated The screen should be configurable enough that it is not
     * required to extend anything
     */
    @Deprecated
    public static void setSkinsGui(BiFunction<Screen, SkinServerList, GuiSkins> skinsGuiFunc) {
        Preconditions.checkNotNull(skinsGuiFunc, "skinsGuiFunc");
        GuiSkins.skinsGuiFunc = skinsGuiFunc;
    }

    public static GuiSkins create(Screen parent, SkinServerList servers) {
        return skinsGuiFunc.apply(parent, servers);
    }

    private int updateCounter = 0;
    private float lastPartialTick;

    private ButtonWidget btnBrowse;
    private ButtonWidget btnUpload;
    private ButtonWidget btnDownload;
    private ButtonWidget btnClear;

    private ButtonWidget btnModeSteve;
    private ButtonWidget btnModeAlex;

    private ButtonWidget btnModeSkin;
    private ButtonWidget btnModeElytra;

    private float msgFadeOpacity = 0;

    private double lastMouseX = 0;

    private boolean jumpState = false;
    private boolean sneakState = false;

    protected final PlayerPreview previewer;
    protected final SkinUploader uploader;
    protected final SkinChooser chooser;

    private final RotatingCubeMapRenderer panorama = new RotatingCubeMapRenderer(new CubeMapRenderer(getBackground()));

    private final FileDrop dropper = FileDrop.newDropEvent(this);

    private final Edge ctrlKey = new Edge(this::ctrlToggled, Screen::hasControlDown);
    private final Edge jumpKey = new Edge(this::jumpToggled, () -> {
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_SPACE);
    });
    private final Edge sneakKey = new Edge(this::sneakToggled, Screen::hasShiftDown);

    public GuiSkins(Screen parent, SkinServerList servers) {
        super(new TranslatableText("hdskins.gui.title"), parent);

        client = MinecraftClient.getInstance();
        previewer = createPreviewer();
        uploader = new SkinUploader(servers, previewer, this);
        chooser = new SkinChooser(uploader);
    }

    public PlayerPreview createPreviewer() {
        return new PlayerPreview();
    }

    protected Identifier getBackground() {
        return new Identifier(HDSkins.MOD_ID, "textures/cubemaps/panorama");
    }

    @Override
    public void tick() {

        if (!( InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT)
            || InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT))) {
            updateCounter++;
        }

        uploader.update();

        updateButtons();
    }

    @Override
    public void init() {
        dropper.subscribe();

        addButton(new LabelWidget(width / 2, 5, new TranslatableText("hdskins.manager"), 0xffffff, true));
        addButton(new LabelWidget(34, 29, new TranslatableText("hdskins.local"), 0xffffff, false));
        addButton(new LabelWidget(width / 2 + 34, 29, new TranslatableText("hdskins.server"), 0xffffff, false));

        btnBrowse = addButton(new ButtonWidget(width / 2 - 150, height - 27, 90, 20,
                new TranslatableText("hdskins.options.browse"),
                button -> chooser.openBrowsePNG(new TranslatableText("hdskins.open.title"))));
        btnBrowse.active = !client.getWindow().isFullscreen();

        btnUpload = addButton(new ButtonWidget(width / 2 - 24, height / 2 - 40, 48, 20,
                new TranslatableText("hdskins.options.chevy"),
                button -> {
                    if (uploader.canUpload()) {
                        punchServer(new TranslatableText("hdskins.upload"));
                    }
                }, supplyTooltip(new TranslatableText("hdskins.options.chevy.title"))
        ));
        btnUpload.active = uploader.canUpload();

        btnDownload = addButton(new ButtonWidget(width / 2 - 24, height / 2 + 20, 48, 20,
                new TranslatableText("hdskins.options.download"),
                sender -> {
                    if (uploader.canClear()) {
                        chooser.openSavePNG(new TranslatableText("hdskins.save.title"), client.getSession().getUsername());
                    }
                }, supplyTooltip(new TranslatableText("hdskins.options.download.title"))));
        btnDownload.active = uploader.canClear();

        btnClear = addButton(new ButtonWidget(width / 2 + 60, height - 27, 90, 20,
                new TranslatableText("hdskins.options.clear"),
                sender -> {
                    if (uploader.canClear()) {
                        punchServer(new TranslatableText("hdskins.request"));
                    }
                }));
        btnClear.active = uploader.canClear();

        addButton(new ButtonWidget(width / 2 - 50, height - 25, 100, 20,
                new TranslatableText("hdskins.options.close"),
                sender -> onClose()));

        btnModeSteve = addButton(new IconButton(width - 25, 32,
                itemFromString(String.format("minecraft:leather_leggings{display:{color:%d}}", 0x3c5dcb)),
                sender -> switchSkinMode(VanillaModels.DEFAULT),
                supplyTooltip(new TranslatableText("hdskins.mode.steve"))
        ));

        btnModeAlex = addButton(new IconButton(width - 25, 51,
                itemFromString(String.format("minecraft:leather_leggings{display:{color:%d}}", 0xfff500)),
                sender -> switchSkinMode(VanillaModels.SLIM),
                supplyTooltip(new TranslatableText("hdskins.mode.alex"))));
        btnModeAlex.active = VanillaModels.isFat(uploader.getMetadataField("model"));

        btnModeSkin = addButton(new IconButton(width - 25, 75,
                new ItemStack(Items.LEATHER_CHESTPLATE),
                sender -> uploader.setSkinType(SkinType.SKIN),
                supplyTooltip(new TranslatableText("hdskins.mode.skin"))));
        btnModeSkin.active = uploader.getSkinType() != SkinType.SKIN;

        btnModeElytra = addButton(new IconButton(width - 25, 94,
                new ItemStack(Items.ELYTRA),
                sender -> uploader.setSkinType(SkinType.ELYTRA),
                supplyTooltip(new TranslatableText("hdskins.mode.elytra"))));
        btnModeElytra.active = uploader.getSkinType() != SkinType.ELYTRA;

        final Tooltips tooltips = new Tooltips(4)
                .add("stand", Items.IRON_BOOTS)
                .add("sleep", Items.CLOCK)
                .add("ride", Items.OAK_BOAT)
                .add("swim", Items.CAULDRON);

        tooltips.setIndex(previewer.getPose());
        addButton(new IconCyclerButton(width - 25, 118, tooltips.items(), sender -> {
            playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);
            previewer.setPose(tooltips.index);
        }, supplyTooltip(tooltips::getTooltip)));

        addButton(new IconCyclerButton(width - 25, height - 40, uploader.cycleEquipment(),
                button -> {
                },
                supplyTooltip(() -> Collections.singletonList(new TranslatableText("hdskins.equipment", new TranslatableText("hdskins.equipment." + uploader.getEquipment().getId().getPath()))))
        ));
        addButton(new ButtonWidget(width - 25, height - 65, 20, 20, new LiteralText("?"), sender -> {
            uploader.cycleGateway();
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_VILLAGER_YES, 1.0F));
        }, supplyTooltip(() -> uploader.getGatewayText().stream().map(LiteralText::new).collect(Collectors.toList()))));
    }


    private static ItemStack itemFromString(String item) {
        try {
            return ItemStackArgumentType.itemStack().parse(new StringReader(item)).createStack(1, false);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private ButtonWidget.TooltipSupplier supplyTooltip(Text tooltipText) {
        return supplyTooltip(() -> Collections.singletonList(tooltipText));
    }

    private ButtonWidget.TooltipSupplier supplyTooltip(Supplier<List<Text>> tooltipText) {
        return ((button, matrices, mouseX, mouseY) -> renderTooltip(matrices, tooltipText.get(), mouseX, mouseY + 10));
    }

    @Override
    public void removed() {
        try {
            uploader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        HDSkins.getInstance().getProfileRepository().clear();

        dropper.cancel();
    }

    @Override
    public void onDrop(List<Path> paths) {
        paths.stream().findFirst().ifPresent(path -> {
            chooser.selectFile(path);
            updateButtons();
        });
    }

    @Override
    public void onSkinTypeChanged(SkinType newType) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

        btnModeSkin.active = newType == SkinType.ELYTRA;
        btnModeElytra.active = newType == SkinType.SKIN;
    }

    protected void switchSkinMode(String model) {
        playSound(SoundEvents.BLOCK_BREWING_STAND_BREW);

        boolean thinArmType = VanillaModels.isSlim(model);

        btnModeSteve.active = thinArmType;
        btnModeAlex.active = !thinArmType;

        uploader.setMetadataField("model", model);
        previewer.setModelType(model);
    }

    protected boolean canTakeEvents() {
        return !chooser.pickingInProgress() && uploader.tryClearStatus() && msgFadeOpacity == 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;

        if (canTakeEvents() && !super.mouseClicked(mouseX, mouseY, button)) {
            int bottom = height - 40;
            int mid = width / 2;

            if ((mouseX > 30 && mouseX < mid - 30 || mouseX > mid + 30 && mouseX < width - 30) && mouseY > 30 && mouseY < bottom) {
                previewer.swingHand();
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double changeX, double changeY) {

        if (canTakeEvents() && !super.mouseDragged(mouseX, mouseY, button, changeX, changeY)) {
            updateCounter -= (lastMouseX - mouseX);
            lastMouseX = mouseX;

            return true;
        }

        lastMouseX = mouseX;

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyCode != GLFW.GLFW_KEY_SPACE && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char keyChar, int keyCode) {
        if (canTakeEvents()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                updateCounter -= 5;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                updateCounter += 5;
            } else if (keyCode == 0) {
                return false;
            }

            if (!chooser.pickingInProgress() && !uploader.uploadInProgress()) {
                return super.charTyped(keyChar, keyCode);
            }
        }

        return false;
    }

    private void jumpToggled(boolean jumping) {
        if (jumping && ctrlKey.getState()) {
            jumpState = !jumpState;
        }

        jumping |= jumpState;

        previewer.setJumping(jumping);
    }

    private void sneakToggled(boolean sneaking) {
        if (sneaking && ctrlKey.getState()) {
            sneakState = !sneakState;
        }

        sneaking |= sneakState;

        previewer.setSneaking(sneaking);

    }

    private void ctrlToggled(boolean ctrl) {
        if (ctrl) {
            if (sneakKey.getState()) {
                sneakState = !sneakState;
            }

            if (jumpKey.getState()) {
                jumpState = !jumpState;
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTick) {
        ctrlKey.update();
        jumpKey.update();
        sneakKey.update();

        panorama.render(partialTick, 1);

        RenderSystem.disableCull();

        float deltaTime = updateCounter + partialTick - lastPartialTick;
        lastPartialTick = updateCounter + partialTick;

        previewer.render(width, height, mouseX, mouseY, updateCounter, partialTick);

        float xPos1 = width / 4F;
        float xPos2 = width * 0.75F;

        if (chooser.getStatus() != null && !uploader.canUpload()) {
            fill(matrices, 40, height / 2 - 12, width / 2 - 40, height / 2 + 12, 0xB0000000);
            drawCenteredText(matrices, textRenderer, chooser.getStatus(), (int) xPos1, height / 2 - 4, 0xffffff);
        }

        if (uploader.downloadInProgress() || uploader.isThrottled() || uploader.isOffline()) {

            int lineHeight = uploader.isThrottled() ? 18 : 12;

            fill(matrices, (int) (xPos2 - width / 4 + 40), height / 2 - lineHeight, width - 40, height / 2 + lineHeight, 0xB0000000);

            if (uploader.isThrottled()) {
                drawCenteredText(matrices, textRenderer, SkinUploader.ERR_MOJANG, (int) xPos2, height / 2 - 10, 0xff5555);
                drawCenteredText(matrices, textRenderer, SkinUploader.getWaitError(uploader.getRetries()), (int) xPos2, height / 2 + 2, 0xff5555);
            } else if (uploader.isOffline()) {
                drawCenteredText(matrices, textRenderer, SkinUploader.ERR_OFFLINE, (int) xPos2, height / 2 - 4, 0xff5555);
            } else {
                drawCenteredText(matrices, textRenderer, SkinUploader.STATUS_FETCH, (int) xPos2, height / 2 - 4, 0xffffff);
            }
        }

        super.render(matrices, mouseX, mouseY, partialTick);

        boolean uploadInProgress = uploader.uploadInProgress();
        boolean showError = uploader.hasStatus();

        if (uploadInProgress || showError || msgFadeOpacity > 0) {
            if (!uploadInProgress && !showError) {
                msgFadeOpacity -= deltaTime / 10;
            } else if (msgFadeOpacity < 1) {
                msgFadeOpacity += deltaTime / 10;
            }

            msgFadeOpacity = MathHelper.clamp(msgFadeOpacity, 0, 1);
        }

        if (msgFadeOpacity > 0) {
            int opacity = (Math.min(180, (int) (msgFadeOpacity * 180)) & 255) << 24;

            fill(matrices, 0, 0, width, height, opacity);

            Text errorMsg = uploader.getStatusMessage();

            if (uploadInProgress) {
                drawCenteredText(matrices, textRenderer, errorMsg, width / 2, height / 2, 0xffffff);
            } else if (showError) {
                int blockHeight = (height - textRenderer.getStringBoundedHeight(errorMsg.getString(), width - 10)) / 2;

                drawCenteredText(matrices, textRenderer, new TranslatableText("hdskins.failed"), width / 2, blockHeight - textRenderer.fontHeight * 2, 0xffff55);
                textRenderer.drawTrimmed(errorMsg, 5, blockHeight, width - 10, 0xff5555);
            }
        }
    }

    private void punchServer(Text uploadMsg) {
        uploader.uploadSkin(uploadMsg).whenComplete((o, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
            updateButtons();
        });

        updateButtons();
    }

    private void updateButtons() {

        Set<Feature> features = uploader.getFeatures();

        btnClear.active = uploader.canClear();
        btnUpload.active = uploader.canUpload() && features.contains(Feature.UPLOAD_USER_SKIN);
        btnDownload.active = uploader.canClear() && !chooser.pickingInProgress();
        btnBrowse.active = !chooser.pickingInProgress();

        // TODO implement feature locking
//        boolean types = !features.contains(Feature.MODEL_TYPES);
//        boolean variants = !features.contains(Feature.MODEL_VARIANTS);

//        btnModeSkin.setLocked(types);
//        btnModeElytra.setLocked(types);
//
//        btnModeSteve.setLocked(variants);
//        btnModeAlex.setLocked(variants);
//
//        btnClear.setLocked(!features.contains(Feature.DELETE_USER_SKIN));
//        btnUpload.setLocked(!features.contains(Feature.UPLOAD_USER_SKIN));
//        btnDownload.setLocked(!features.contains(Feature.DOWNLOAD_USER_SKIN));
    }

    private static class Tooltips {
        private final List<ItemStack> items;
        private final List<Text> tooltips;

        private int index;

        Tooltips(int size) {
            items = new ArrayList<>(size);
            tooltips = new ArrayList<>(size);
        }

        Tooltips add(String name, Item item) {
            items.add(new ItemStack(item));
            tooltips.add(new TranslatableText("hdskins.mode." + name));
            return this;
        }

        void next() {
            setIndex(index + 1);
        }

        void setIndex(int indx) {
            this.index = indx % items.size();
        }

        Iterator<ItemStack> items() {
            Iterator<ItemStack> itemIterator = Iterators.cycle(items);
            return new Iterator<ItemStack>() {
                @Override
                public boolean hasNext() {
                    return itemIterator.hasNext();
                }

                @Override
                public ItemStack next() {
                    Tooltips.this.next();
                    return itemIterator.next();
                }
            };
        }

        List<Text> getTooltip() {
            return Collections.singletonList(tooltips.get(index));
        }
    }

    protected static class IconButton extends ButtonWidget {
        protected ItemStack itemStack;

        public IconButton(int x, int y, ItemStack itemStack, ButtonWidget.PressAction action, TooltipSupplier tooltipSupplier) {
            super(x, y, 20, 20, LiteralText.EMPTY, action, tooltipSupplier);
            this.itemStack = itemStack;
        }

        @Override
        protected void renderBg(MatrixStack matrices, MinecraftClient client, int mouseX, int mouseY) {
            client.getItemRenderer().renderGuiItemIcon(itemStack, x + 2, y + 2);
        }
    }

    protected static class IconCyclerButton extends IconButton {
        private final Iterator<ItemStack> items;

        public IconCyclerButton(int x, int y, Iterator<ItemStack> items, PressAction action, TooltipSupplier tooltipSupplier) {
            super(x, y, items.next(), action, tooltipSupplier);
            this.items = items;
        }

        @Override
        public void onPress() {
            itemStack = items.next();
            super.onPress();
        }
    }
}
