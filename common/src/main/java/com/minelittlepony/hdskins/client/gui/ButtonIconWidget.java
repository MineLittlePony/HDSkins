package com.minelittlepony.hdskins.client.gui;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.command.arguments.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;

public class ButtonIconWidget extends ButtonWidget {
    private final ItemStack item;

    public ButtonIconWidget(int x, int y, String item, ButtonWidget.PressAction onPress) {
        this(x, y, stringToItem(item), onPress);
    }

    public ButtonIconWidget(int x, int y, ItemStack item, ButtonWidget.PressAction onPress) {
        super(x, y, 20, 20, "", onPress);
        this.item = item;
    }

    @Override
    protected void renderBg(MinecraftClient client, int mouseX, int mouseY) {
        client.getItemRenderer().renderGuiItem(item, x + 2, y + 2);
    }

    private static ItemStack stringToItem(String item) {
        try {
            return ItemStackArgumentType.itemStack().parse(new StringReader(item)).createStack(1, false);
        } catch (CommandSyntaxException e) {
            LogManager.getLogger().warn("Failed to parse item string: {}", item, e);
            return ItemStack.EMPTY;
        }
    }
}
