package com.minelittlepony.hdskins;

import javax.annotation.Nullable;

import com.minelittlepony.common.client.IModUtilities;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.common.event.ScreenInitCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class FabMod implements ClientModInitializer, IModUtilities {

    @Nullable
    private HDSkins hd;

    private boolean firstTick = true;

    @Override
    public void onInitializeClient() {
        ClientTickCallback.EVENT.register(this::onClientTick);
        ScreenInitCallback.EVENT.register(this::onScreenInit);

        hd = new HDSkins(this);
    }

    private void onClientTick(MinecraftClient client) {
        if (hd == null) {
            return;
        }

        if (firstTick) {
            firstTick = false;

            hd.postInit(client);
        }
    }

    private void onScreenInit(Screen screen, ScreenInitCallback.ButtonList buttons) {
        if (screen instanceof TitleScreen) {
            buttons.add(new Button(screen.width - 50, screen.height - 50, 20, 20).onClick(sender -> {
                MinecraftClient.getInstance().openScreen(HDSkins.getInstance().createSkinsGui());
            }).setStyle(new Style().setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)));
        }
    }
}
