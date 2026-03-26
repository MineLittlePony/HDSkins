package com.minelittlepony.hdskins.client.gui;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.joml.Quaternionf;
import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

public class Carousel<T extends PlayerSkins<? extends PlayerSkins.PlayerSkin>, S extends AvatarRenderState> implements Closeable, ITextContext {
    public static final int HOR_MARGIN = 30;
    private static final int TOP = 50;

    private final Component title;

    private final PlayerBodyWidget<S> entity;
    private final T skins;

    public final Bounds bounds = new Bounds(TOP, HOR_MARGIN, 0, 0);

    private final List<Element> elements = new ArrayList<>();

    public Carousel(Component title, T skins, Function<PlayerSkins<?>, PlayerBodyWidget<S>> playerFactory) {
        this.title = title;
        this.skins = skins;
        this.entity = playerFactory.apply(skins);
        addElement(entity);
    }

    public void addElement(Element element) {
        elements.add(element);
    }

    public PlayerBodyWidget<S> getEntity() {
        return entity;
    }

    public T getSkins() {
        return skins;
    }

    public boolean mouseClicked(int width, int height, MouseButtonEvent click) {
        if (bounds.contains(click.x(), click.y())) {
            entity.swingArm(click.button() == InputConstants.MOUSE_BUTTON_LEFT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            return true;
        }
        return false;
    }

    public void update() {
        elements.forEach(Element::tick);
    }

    public void extractRenderState(int mouseX, int mouseY, int rotationAngle, float partialTick, GuiGraphicsExtractor context) {
        context.enableScissor(bounds.left, bounds.top, bounds.right(), bounds.bottom());
        int horizon = bounds.bottom() - 50;
        extractBackground(context, horizon);

        Quaternionf rotation = new Quaternionf().rotationX(Mth.PI).rotateY(rotationAngle / 20F);

        elements.forEach(element -> {
            element.updateState(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height * 0.9F,
                    mouseX,
                    bounds.top + bounds.height / 2 - mouseY,
                    partialTick
            );
            element.extractRenderState(context, bounds, mouseX, mouseY, rotation);
        });
        context.disableScissor();

        context.pose().pushMatrix();
        bounds.translate(context.pose());
        context.text(getFont(), title, 5, 5, CommonColors.WHITE, false);
        context.pose().popMatrix();
    }

    protected void extractBackground(GuiGraphicsExtractor context, int horizon) {
        bounds.draw(context, 0xA0000000);
        context.fillGradient(bounds.left, horizon,  bounds.right(), bounds.bottom(), 0x05FFFFFF, 0x40FFFFFF);
    }

    @Override
    public void close() {
        skins.close();
    }

    public interface Element {
        void tick();

        void updateState(float xPosition, float yPosition, float mouseX, float mouseY, float tickDelta);

        void extractRenderState(GuiGraphicsExtractor context, Bounds bounds, int mouseX, int mouseY, Quaternionf rotation);
    }
}
