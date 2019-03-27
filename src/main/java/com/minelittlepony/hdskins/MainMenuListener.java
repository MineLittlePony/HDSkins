package com.minelittlepony.hdskins;

import com.minelittlepony.common.client.gui.IconicButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "hdskins")
public class MainMenuListener {

    @SubscribeEvent
    public static void onShowMainMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            int width = event.getGui().width;
            int height = event.getGui().height;

            event.getButtonList().add(new IconicButton(width - 50, height - 50, sender-> {
                Minecraft.getInstance().displayGuiScreen(HDSkins.getInstance().createSkinsGui());
            }).setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb));
        }
    }
}
