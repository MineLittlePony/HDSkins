package com.minelittlepony.hdskins.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class LabelWidget extends AbstractButtonWidget {
    private final int color;
    private final boolean centered;

    public LabelWidget(int x, int y, Text message, int color, boolean centered) {
        super(x, y, 0, 0, message);
        this.color = color;
        this.centered = centered;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        TextRenderer textRenderer = minecraftClient.textRenderer;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        int color = this.color | MathHelper.ceil(this.alpha * 255.0F) << 24;
        if (centered) {
            this.drawCenteredText(matrices, textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
        } else {
            this.drawTextWithShadow(matrices, textRenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
        }
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return false;
    }
}
