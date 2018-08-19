package com.minelittlepony.hdskins.gui;

import com.minelittlepony.MineLittlePony;
import com.minelittlepony.PonyManager;
import com.minelittlepony.avatar.texture.TextureData;
import com.minelittlepony.avatar.texture.TextureType;
import com.minelittlepony.gui.Button;
import com.minelittlepony.gui.IconicButton;
import com.mojang.authlib.GameProfile;
import com.voxelmodpack.hdskins.gui.EntityPlayerModel;
import com.voxelmodpack.hdskins.gui.GuiSkins;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;

/**
 * Skin uploading GUI. Usually displayed over the main menu.
 */
public class GuiSkinsMineLP extends GuiSkins {

    private PonyManager ponyManager = MineLittlePony.getInstance().getManager();

    private Button btnModeWet;
    private Button btnModeDry;

    private boolean isWet = false;


    private static final String[] panoramas = new String[] {
        "minelp:textures/cubemap/sugarcubecorner_%d.png",
        "minelp:textures/cubemap/quillsandsofas_%d.png"
    };

    @Override
    protected EntityPlayerModel getModel(GameProfile profile) {
        return new EntityPonyModel(profile);
    }

    @Override
    public void initGui() {
        super.initGui();

        addButton(btnModeWet = new IconicButton(width - 25, 137, sender -> {
            setWet(true);
        }).setIcon(new ItemStack(Items.WATER_BUCKET))).setEnabled(!isWet).setTooltip("minelp.mode.wet");

        addButton(btnModeDry = new IconicButton(width - 25, 118, sender -> {
            setWet(false);
        }).setIcon(new ItemStack(Items.BUCKET))).setEnabled(isWet).setTooltip("minelp.mode.dry");
    }

    @Override
    protected void initPanorama() {
        int i = (int)Math.floor(Math.random() * panoramas.length);

        panorama.setSource(panoramas[i]);
    }

    protected void setWet(boolean wet) {
        if (wet == isWet) {
            return;
        }

        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.BLOCK_BREWING_STAND_BREW, 1));

        isWet = wet;
        localPlayer.releaseTextures();

        btnModeDry.enabled = isWet;
        btnModeWet.enabled = !isWet;

        ((EntityPonyModel)localPlayer).setWet(isWet);
        ((EntityPonyModel)remotePlayer).setWet(isWet);
    }

    @Override
    protected void onSetLocalSkin(TextureType type) {
        MineLittlePony.logger.debug("Invalidating old local skin, checking updated local skin");
        if (type == TextureType.SKIN) {
            ponyManager.removePony(localPlayer.getLocal(TextureType.SKIN).getTexture());
        }
    }

    @Override
    protected void onSetRemoteSkin(TextureType type, TextureData texture) {
        MineLittlePony.logger.debug("Invalidating old remote skin, checking updated remote skin");
        if (type == TextureType.SKIN) {
            ponyManager.removePony(texture.getLocation());
        }
    }
}
