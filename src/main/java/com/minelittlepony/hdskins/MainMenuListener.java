package com.minelittlepony.hdskins;

import com.minelittlepony.gui.IconicButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmorDyeable;
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

            ItemStack icon = new ItemStack(Items.LEATHER_LEGGINGS);
            ((ItemArmorDyeable) Items.LEATHER_LEGGINGS).setColor(icon, 0x3c5dcb);

            event.getButtonList().add(new IconicButton(width - 50, height - 50, icon) {
                @Override
                public void onClick(double mouseX, double mouseY) {
                    Minecraft.getInstance().displayGuiScreen(HDSkins.instance.getSkinManager().createSkinsGui());
                }
            });
        }
    }
}
