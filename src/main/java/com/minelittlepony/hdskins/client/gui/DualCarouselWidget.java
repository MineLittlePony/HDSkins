package com.minelittlepony.hdskins.client.gui;

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.hdskins.client.*;
import com.minelittlepony.hdskins.client.gui.player.skins.LocalPlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins;
import com.minelittlepony.hdskins.client.resources.*;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Handles the display of the dummy players in the GUI.
 */
public abstract class DualCarouselWidget<S extends AvatarRenderState> implements Closeable, PlayerSkins.Posture, ITextContext {
    private static final int PASSIVE_ROTATION_SPEED = 1;
    private static final int MAX_MANUAL_ROTATION_SPEED = 20;

    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final GameProfile profile = minecraft.getGameProfile();

    public final Carousel<LocalPlayerSkins, S> local;
    public final Carousel<ServerPlayerSkins, S> remote;

    private final SkinListWidget<S> skinList;

    private Pose pose = Pose.STAND;
    private SkinType activeSkinType = SkinType.SKIN;

    private Optional<SkinVariant> variant = Optional.of(PlayerSkins.Posture.SkinVariant.DEFAULT);
    private List<SkinVariant> skinVariants = new ArrayList<>(PlayerSkins.Posture.SkinVariant.VALUES);

    private EquipmentSet activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();

    protected final Controls controls;

    private float rotationAngle = 72;
    private float rotationSpeed;
    private int prevRotationDirection;

    private final GuiSkins screen;

    public DualCarouselWidget(GuiSkins screen) {
        this.screen = screen;
        local = new Carousel<>(Component.translatable("hdskins.local"), new LocalPlayerSkins(this), this::createEntity);
        remote = new Carousel<>(Component.translatable("hdskins.server"), new ServerPlayerSkins(this), this::createEntity);
        skinList = new SkinListWidget<>(this, remote.bounds);
        controls = new Controls(this);
        remote.addElement(skinList);
    }

    protected abstract PlayerBodyWidget<S> createEntity(PlayerSkins<?> skins);

    public Carousel<ServerPlayerSkins, S> getRemote() {
        return remote;
    }

    public Carousel<LocalPlayerSkins, S> getLocal() {
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
        return NativeImageFilters.GREYSCALE.load(type == SkinType.SKIN ? VanillaSkins.getSkinTextures(getProfile().id(), variant) : skin, skin, getExclusion());
    }

    @Override
    public TextureLoader.Exclusion getExclusion() {
        return TextureLoader.Exclusion.NULL;
    }

    public Identifier getBlankSkin(SkinType type, String variant) {
        return VanillaSkins.getDefaultTexture(type, variant);
    }

    public void setJumping(boolean jumping) {
        apply(p -> p.jumping = jumping);
    }

    public void setSneaking(boolean sneaking) {
        apply(p -> p.playerState.isCrouching = sneaking);
    }

    public void setSprinting(boolean sprinting) {
        apply(p -> p.sprinting = sprinting);
    }

    public void apply(Consumer<PlayerBodyWidget<?>> action) {
        action.accept(getLocal().getEntity());
        action.accept(getRemote().getEntity());
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
        local.update();
        remote.update();

        Minecraft client = Minecraft.getInstance();

        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();

        int rotationDirection = left ? 1 : right ? -1 : 0;

        if (!(left && right) && !screen.isDragging()) {
            if (rotationDirection == 0) {
                rotationSpeed = (int)Math.max(PASSIVE_ROTATION_SPEED, rotationSpeed * 0.6F);
                rotationAngle += rotationSpeed;
            } else {
                if (prevRotationDirection != rotationDirection) {
                    rotationSpeed = PASSIVE_ROTATION_SPEED;
                }
                rotationSpeed = Math.min(MAX_MANUAL_ROTATION_SPEED, rotationSpeed + PASSIVE_ROTATION_SPEED);
                rotationAngle -= rotationSpeed * rotationDirection;
            }
        }
        prevRotationDirection = rotationDirection;
    }

    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick, SkinChooser chooser, SkinUploader uploader) {
        local.extractRenderState(mouseX, mouseY, (int)rotationAngle, partialTick, context);
        remote.extractRenderState(mouseX, mouseY, (int)rotationAngle, partialTick, context);

        uploader.extractStatus(context, remote.bounds);
        chooser.extractStatus(context, local.bounds);
    }

    public boolean mouseClicked(SkinUploader uploader, int width, int height, MouseButtonEvent click) {
        boolean listHit = skinList.mouseClicked(uploader, click);
        boolean playerHit =
                   local.mouseClicked(width, height, click)
                || remote.mouseClicked(width, height, click);

        if (playerHit && !listHit && click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            screen.setDragging(true);
        }

        return listHit || playerHit;
    }

    public boolean mouseDragged(MouseButtonEvent click, double changeX, double changeY) {
        if (screen.isDragging()) {
            rotationAngle += changeX * 2;
        }
        return true;
    }

    @Override
    public void close() {
        remote.close();
        local.close();
    }
}
