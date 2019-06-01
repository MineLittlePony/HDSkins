package com.minelittlepony.hdskins.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.HDSkins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.MainMenuScreen;
import net.minecraft.client.gui.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Mixin(MainMenuScreen.class)
public class MixinGuiMainMenu extends Screen {

    protected MixinGuiMainMenu() { super(null); }

    @Inject(method = "init()V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        addButton(new Button(width - 50, height - 50).onClick(sender -> {
            MinecraftClient.getInstance().openScreen(HDSkins.getInstance().createSkinsGui());
        }).setStyle(new Style().setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb)));
    }
}
