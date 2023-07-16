package com.minelittlepony.hdskins.client.gui;

import static com.mojang.blaze3d.systems.RenderSystem.*;

import java.io.Closeable;
import java.util.*;
import java.util.function.Consumer;

import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.sprite.ISprite;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.*;
import net.minecraft.text.Text;

/**
 * Player previewer that renders the models to the screen.
 */
public class DualPreview implements Closeable, PlayerSkins.Posture, ITextContext {
    private static final int LABEL_BACKGROUND = 0xB0000000;

    protected final MinecraftClient minecraft = MinecraftClient.getInstance();
    protected final GameProfile profile = minecraft.getSession().getProfile();

    public final Carousel<LocalPlayerSkins> local;
    public final Carousel<ServerPlayerSkins> remote;

    private final SkinListWidget skinList;

    private Pose pose = Pose.STAND;
    private SkinType activeSkinType = SkinType.SKIN;

    private List<SkinVariant> skinVariants = new ArrayList<>();

    private EquipmentSet activeEquipmentSet = HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();

    public DualPreview() {
        addSkinVariant("steve");
        addSkinVariant("alex");

        local = new Carousel<>(Text.translatable("hdskins.local"), new LocalPlayerSkins(this), this::createEntity);
        remote = new Carousel<>(Text.translatable("hdskins.server"), new ServerPlayerSkins(this), this::createEntity);
        skinList = new SkinListWidget(this, remote.bounds);
    }

    protected DummyPlayer createEntity(ClientWorld world, PlayerSkins<?> textures) {
        return new DummyPlayer(world, textures);
    }

    protected void addSkinVariant(String name) {
        skinVariants.add(new SkinVariant(
                Text.translatable("hdskins.arm_style", Text.translatable("hdskins.arm_style." + name)),
                new TextureSprite()
                    .setTexture(GuiSkins.WIDGETS_TEXTURE)
                    .setPosition(2, 2)
                    .setSize(16, 16)
                    .setTextureOffset(32, 16 * skinVariants.size()),
                name
        ));
    }

    public Carousel<ServerPlayerSkins> getRemote() {
        return remote;
    }

    public Carousel<LocalPlayerSkins> getLocal() {
        return local;
    }

    public List<SkinVariant> getSkinVariants() {
        return skinVariants;
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

    public void setModelType(String model) {
        local.getSkins().setPreviewThinArms(VanillaModels.isSlim(model));
    }

    public void setSkinType(SkinType type) {
        activeSkinType = type;
    }

    @Override
    public SkinType getActiveSkinType() {
        return activeSkinType;
    }

    @Override
    public Identifier getDefaultSkin(SkinType type, boolean slim) {
        Identifier skin = getBlankSkin(type, slim);
        return DefaultSkinGenerator.generateGreyScale(type == SkinType.SKIN ? VanillaSkins.getTexture(getProfile().getId(), slim) : skin, skin, getExclusion());
    }

    protected TextureLoader.Exclusion getExclusion() {
        return TextureLoader.Exclusion.NULL;
    }

    public Identifier getBlankSkin(SkinType type, boolean slim) {
        return VanillaSkins.getDefaultTexture(type, slim);
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

    public void init(GuiSkins screen) {
        local.bounds.left = Carousel.HOR_MARGIN;
        local.bounds.height = screen.height - 90;
        local.bounds.width = (screen.width / 2) - 70;

        remote.bounds.copy(local.bounds);
        remote.bounds.left = screen.width - Carousel.HOR_MARGIN - remote.bounds.width;

        local.init(screen);
        remote.init(screen);
        skinList.init(screen);
    }

    public void render(DrawContext context, int mouseX, int mouseY, int ticks, float partialTick, SkinChooser chooser, SkinUploader uploader) {
        MinecraftClient client = MinecraftClient.getInstance();
        DrawContext d = new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers());

        enableDepthTest();

        local.render(mouseX, mouseY, ticks, partialTick, context, null);
        remote.render(mouseX, mouseY, ticks, partialTick, context, remote -> {
            skinList.render(remote, d, mouseX, mouseY);
        });

        disableDepthTest();

        MatrixStack matrices = context.getMatrices();

        if (chooser.hasStatus()) {
            matrices.push();
            local.bounds.translate(matrices);
            matrices.translate(0, 0, 300);
            context.fill(10, local.bounds.height / 2 - 12, local.bounds.width - 10, local.bounds.height / 2 + 12, LABEL_BACKGROUND);
            drawCenteredLabel(context, chooser.getStatus(), local.bounds.width / 2, local.bounds.height / 2 - 4, 0xFFFFFF, 0);
            matrices.pop();
        }

        if (uploader.hasStatus()) {
            matrices.push();
            remote.bounds.translate(matrices);
            matrices.translate(0, 0, 300);

            int lineHeight = uploader.isThrottled() ? 18 : 12;

            context.fill(10, remote.bounds.height / 2 - lineHeight, remote.bounds.width - 10, remote.bounds.height / 2 + lineHeight, LABEL_BACKGROUND);

            Text status = uploader.getStatus();

            if (status == SkinUploader.STATUS_MOJANG) {
                drawCenteredLabel(context, status, remote.bounds.width / 2, remote.bounds.height / 2 - 10, 0xff5555, 0);
                drawCenteredLabel(context, Text.translatable(SkinUploader.ERR_MOJANG_WAIT, uploader.getRetries()), remote.bounds.width / 2, remote.bounds.height / 2 + 2, 0xff5555, 0);
            } else {
                drawCenteredLabel(context, status, remote.bounds.width / 2, remote.bounds.height / 2 - 4, status == SkinUploader.STATUS_OFFLINE ? 0xff5555 : 0xffffff, 0);
            }

            matrices.pop();
        }
    }

    public boolean mouseClicked(SkinUploader uploader, int width, int height, double mouseX, double mouseY, int button) {
        return skinList.mouseClicked(uploader, mouseX, mouseY, button)
                || local.mouseClicked(width, height, mouseX, mouseY, button)
                || remote.mouseClicked(width, height, mouseX, mouseY, button);
    }

    @Override
    public void close() {
        remote.close();
        local.close();
    }

    public record SkinVariant (Text tooltip, ISprite icon, String name) { }
}
