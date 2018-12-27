package com.minelittlepony.hdskins;

import com.minelittlepony.gui.IActionable;
import com.minelittlepony.gui.IconicButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = "hdskins")
public class MainMenuListener {

    @SubscribeEvent
    public static void onShowMainMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu) {
            int width = event.getGui().width;
            int height = event.getGui().height;
            event.getButtonList().add(new IconicButton(width - 50, height - 50, sender -> {
                event.getGui().mc.displayGuiScreen(HDSkins.instance.getSkinManager().createSkinsGui());
            }).setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb));
        }
    }

    @SubscribeEvent
    public static void onButtonClick(GuiScreenEvent.ActionPerformedEvent.Post event) {
        // TODO Replace this with vanilla in 1.13
        if (event.getGui() instanceof GuiMainMenu && event.getButton() instanceof IActionable) {
            ((IActionable) event.getButton()).perform();
        }
    }
}
