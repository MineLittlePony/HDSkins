package com.minelittlepony.hdskins.client.gui;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.hdskins.client.*;
import com.minelittlepony.hdskins.client.gui.player.DummyPlayer;
import com.minelittlepony.hdskins.client.gui.player.skins.LocalPlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins;
import com.minelittlepony.hdskins.client.resources.*;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.*;
import net.minecraft.text.Text;

/**
 * Handles the display of the dummy players in the GUI.
 */
public class DualCarouselWidget implements Closeable, PlayerSkins.Posture, ITextContext {
    private static final int PASSIVE_ROTATION_SPEED = 1;
    private static final int MAX_MANUAL_ROTATION_SPEED = 20;

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    protected final GameProfile profile = minecraft.getSession().getProfile();

    public final Carousel<LocalPlayerSkins> local;
    public final Carousel<ServerPlayerSkins> remote;

    private final SkinListWidget skinList;

    private Pose pose = Pose.STAND;
    private SkinType activeSkinType = SkinType.SKIN;

    private Optional<SkinVariant> variant = Optional.empty();
    private List<SkinVariant> skinVariants = new ArrayList<>(PlayerSkins.Posture.SkinVariant.VALUES);

    private EquipmentSet activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();

    protected final Controls controls;

    private float updateCounter = 72;
    private float rotationSpeed;
    private int prevRotationDirection;

    private final GuiSkins screen;

    public DualCarouselWidget(GuiSkins screen) {
        this.screen = screen;
        local = new Carousel<>(Text.translatable("hdskins.local"), new LocalPlayerSkins(this), this::createEntity);
        remote = new Carousel<>(Text.translatable("hdskins.server"), new ServerPlayerSkins(this), this::createEntity);
        skinList = new SkinListWidget(this, remote.bounds);
        controls = new Controls(this);
        remote.addElement(skinList);
    }

    protected DummyPlayer createEntity(ClientWorld world, PlayerSkins<?> textures) {
        return new DummyPlayer(world, textures);
    }

    public Carousel<ServerPlayerSkins> getRemote() {
        return remote;
    }

    public Carousel<LocalPlayerSkins> getLocal() {
        return local;
    }

    public void setEquipment(EquipmentSet equipment) {
        activeEquipmentSet = equipment;
        apply(activeEquipmentSet::apply);
    }

    @Override
    public EquipmentSet getEquipment() {
        return activeEquipmentSet;
    }

    @Override
    public GameProfile getProfile() {
        return profile;
    }

    public void setPose(Pose pose) {
        this.pose = pose;
    }

    @Override
    public Pose getPose() {
        return pose;
    }

    public void setSkinVariant(SkinVariant variant) {
        this.variant = Optional.of(variant);
        local.getSkins().close();
    }

    public List<SkinVariant> getSkinVariants() {
        return skinVariants;
    }

    @Override
    public Optional<SkinVariant> getSkinVariant() {
        return variant;
    }

    public void setSkinType(SkinType type) {
        activeSkinType = type;
    }

    @Override
    public SkinType getActiveSkinType() {
        return activeSkinType;
    }

    @Override
    public Identifier getDefaultSkin(SkinType type, String variant) {
        Identifier skin = getBlankSkin(type, variant);
        return NativeImageFilters.GREYSCALE.load(type == SkinType.SKIN ? VanillaSkins.getTexture(getProfile().getId(), variant) : skin, skin, getExclusion());
    }

    @Override
    public TextureLoader.Exclusion getExclusion() {
        return TextureLoader.Exclusion.NULL;
    }

    public Identifier getBlankSkin(SkinType type, String variant) {
        return VanillaSkins.getDefaultTexture(type, variant);
    }

    public void setJumping(boolean jumping) {
        apply(p -> p.setJumping(jumping));
    }

    public void setSneaking(boolean sneaking) {
        apply(p -> p.setSneaking(sneaking));
    }

    public void setSprinting(boolean walking) {
        apply(p -> p.setSprinting(walking));
    }

    public void apply(Consumer<DummyPlayer> action) {
        getLocal().getEntity().ifPresent(action);
        getRemote().getEntity().ifPresent(action);
    }

    public void init() {
        local.bounds.left = Carousel.HOR_MARGIN;
        local.bounds.height = screen.height - 90;
        local.bounds.width = (screen.width / 2) - 70;

        remote.bounds.copy(local.bounds);
        remote.bounds.left = screen.width - Carousel.HOR_MARGIN - remote.bounds.width;

        skinList.init(screen);
    }

    public void update() {
        controls.update();

        MinecraftClient client = MinecraftClient.getInstance();

        boolean left = client.options.leftKey.isPressed();
        boolean right = client.options.rightKey.isPressed();

        int rotationDirection = left ? -1 : right ? 1 : 0;

        if (!(left && right) && !screen.isDragging()) {
            if (rotationDirection == 0) {
                rotationSpeed = (int)Math.max(PASSIVE_ROTATION_SPEED, rotationSpeed * 0.6F);
                updateCounter += rotationSpeed;
            } else {
                if (prevRotationDirection != rotationDirection) {
                    rotationSpeed = PASSIVE_ROTATION_SPEED;
                }
                rotationSpeed = Math.min(MAX_MANUAL_ROTATION_SPEED, rotationSpeed + PASSIVE_ROTATION_SPEED);
                updateCounter -= rotationSpeed * rotationDirection;
            }
        }
        prevRotationDirection = rotationDirection;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float partialTick, SkinChooser chooser, SkinUploader uploader) {
        enableDepthTest();
        local.render(mouseX, mouseY, (int)updateCounter, partialTick, context);
        remote.render(mouseX, mouseY, (int)updateCounter, partialTick, context);
        disableDepthTest();

        chooser.renderStatus(context, local.bounds);
        uploader.renderStatus(context, remote.bounds);
    }

    public boolean mouseClicked(SkinUploader uploader, int width, int height, double mouseX, double mouseY, int button) {
        boolean listHit = skinList.mouseClicked(uploader, mouseX, mouseY, button);
        boolean playerHit =
                   local.mouseClicked(width, height, mouseX, mouseY, button)
                || remote.mouseClicked(width, height, mouseX, mouseY, button);

        if (playerHit && !listHit && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            screen.setDragging(true);
        }

        return listHit || playerHit;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double changeX, double changeY) {
        if (screen.isDragging()) {
            updateCounter += changeX;
        }
        return true;
    }

    @Override
    public void close() {
        remote.close();
        local.close();
    }
}
