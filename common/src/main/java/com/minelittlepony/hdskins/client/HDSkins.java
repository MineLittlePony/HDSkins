package com.minelittlepony.hdskins.client;

import com.minelittlepony.common.client.gui.element.Button;
import com.minelittlepony.common.util.GamePaths;
import com.minelittlepony.hdskins.client.dummy.EquipmentList;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.profile.ProfileRepository;
import com.minelittlepony.hdskins.client.resources.SkinResourceManager;
import com.minelittlepony.hdskins.skins.SkinServerList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public abstract class HDSkins {
    public static final String MOD_ID = "hdskins";

    public static final Logger logger = LogManager.getLogger();

    private static HDSkins instance;

    public static HDSkins getInstance() {
        return instance;
    }

    protected final HDConfig config = new HDConfig(GamePaths.getConfigDirectory().resolve("hdskins.json"));

    protected final SkinServerList skinServerList = new SkinServerList();
    protected final EquipmentList equipmentList = new EquipmentList();
    protected final SkinResourceManager resources = new SkinResourceManager();
    protected final ProfileRepository repository = new ProfileRepository(this);

    public HDSkins() {
        instance = this;
    }

    public HDConfig getConfig() {
        return config;
    }

    protected void onScreenInit(Screen screen, Consumer<Button> buttons) {
        if (screen instanceof TitleScreen) {
            Button button = new Button(screen.width - 50, screen.height - 50, 20, 20)
                .onClick(sender -> MinecraftClient.getInstance().openScreen(GuiSkins.create(screen, skinServerList)));

            button.getStyle()
                    .setIcon(new ItemStack(Items.LEATHER_LEGGINGS), 0x3c5dcb);
            button.y = screen.height - 50; // ModMenu;
            buttons.accept(button);
        }
    }

    public SkinResourceManager getResourceManager() {
        return resources;
    }

    public ProfileRepository getProfileRepository() {
        return repository;
    }

    public SkinServerList getSkinServerList() {
        return skinServerList;
    }

    public EquipmentList getDummyPlayerEquipmentList() {
        return equipmentList;
    }
}
