package com.minelittlepony.hdskins.client.gui;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.joml.Quaternionf;
import com.minelittlepony.common.client.gui.ITextContext;
import com.minelittlepony.common.client.gui.dimension.Bounds;
import com.minelittlepony.hdskins.client.gui.player.skins.PlayerSkins;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class Carousel<T extends PlayerSkins<? extends PlayerSkins.PlayerSkin>, S extends PlayerEntityRenderState> implements Closeable, ITextContext {
    public static final int HOR_MARGIN = 30;
    private static final int TOP = 50;

    private final Text title;

    private final PlayerBodyWidget<S> entity;
    private final T skins;

    public final Bounds bounds = new Bounds(TOP, HOR_MARGIN, 0, 0);

    private final List<Element> elements = new ArrayList<>();

    public Carousel(Text title, T skins, Function<PlayerSkins<?>, PlayerBodyWidget<S>> playerFactory) {
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

    public boolean mouseClicked(int width, int height, double mouseX, double mouseY, int button) {
        if (bounds.contains(mouseX, mouseY)) {
            entity.swingArm(button == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND);
            return true;
        }
        return false;
    }

    public void update() {
        elements.forEach(Element::tick);
    }

    public void render(int mouseX, int mouseY, int rotationAngle, float partialTick, DrawContext context) {
        context.enableScissor(bounds.left, bounds.top, bounds.right(), bounds.bottom());
        int horizon = bounds.bottom() - 50;
        drawBackground(context, horizon);

        Quaternionf rotation = new Quaternionf().rotationX(MathHelper.PI).rotateY(rotationAngle / 20F);

        elements.forEach(element -> {
            element.updateState(
                    bounds.left + bounds.width / 2,
                    bounds.top + bounds.height * 0.9F,
                    mouseX,
                    bounds.top + bounds.height / 2 - mouseY,
                    partialTick
            );
            element.render(context, bounds, mouseX, mouseY, rotation);
        });
        context.disableScissor();

        context.getMatrices().pushMatrix();
        bounds.translate(context.getMatrices());
        context.drawText(getFont(), title, 5, 5, Colors.WHITE, false);
        context.getMatrices().popMatrix();
    }

    protected void drawBackground(DrawContext context, int horizon) {
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

        void render(DrawContext context, Bounds bounds, int mouseX, int mouseY, Quaternionf rotation);
    }
}
