package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.function.Consumer;

public class HDSkinsEvents {

    public void onScreenInit(Screen screen, Consumer<Button> buttons) {
        if (screen instanceof TitleScreen) {
            MinecraftClient mc = MinecraftClient.getInstance();
            HDSkins hd = HDSkins.getInstance();
            Button button = new Button(screen.width - 50, screen.height - 50, 20, 20)
                    .onClick(sender -> mc.openScreen(GuiSkins.create(screen, hd.getSkinServerList())));

            button.getStyle()
                    .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb);
            button.y = screen.height - 50; // ModMenu;
            buttons.accept(button);
        }
    }

}
