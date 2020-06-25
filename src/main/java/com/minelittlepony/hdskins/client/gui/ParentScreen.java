package com.minelittlepony.hdskins.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import javax.annotation.Nullable;

public abstract class ParentScreen extends Screen {
    @Nullable
    protected final Screen parent;

    protected ParentScreen(Text title) {
        this(title, MinecraftClient.getInstance().currentScreen);
    }

    protected ParentScreen(Text title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    public void playSound(SoundEvent event) {
        client.getSoundManager().play(PositionedSoundInstance.master(event, 1.0F));
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        this.buttons.forEach((button) -> {
            if (button.isMouseOver(mouseX, mouseY)) {
                button.renderToolTip(matrices, mouseX, mouseY);
            }
        });
    }

    public void onClose() {
        this.client.openScreen(this.parent);
    }
}
