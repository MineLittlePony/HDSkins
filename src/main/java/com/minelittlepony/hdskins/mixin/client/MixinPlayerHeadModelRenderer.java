package com.minelittlepony.hdskins.mixin.client;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.profile.SkinLoader.ProvidedSkins;
import com.minelittlepony.hdskins.profile.SkinType;

import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.item.model.special.PlayerHeadModelRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;

@Mixin(PlayerHeadModelRenderer.class)
abstract class MixinPlayerHeadModelRenderer {
    private final Map<ProfileComponent, PlayerHeadModelRenderer.Data> hdskins_profileCache = new HashMap<>();

    @Inject(method = "getData(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/render/item/model/special/PlayerHeadModelRenderer$Data;", at = @At("RETURN"), cancellable = true)
    private void onGetData(ItemStack stack, CallbackInfoReturnable<PlayerHeadModelRenderer.Data> info) {
        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) {
            info.setReturnValue(null);
        } else {
            PlayerHeadModelRenderer.Data data = hdskins_profileCache.get(profile);
            if (data != null) {
                info.setReturnValue(data);
            } else {
                profile = profile.resolve();
                if (profile == null) {
                    return;
                }

                ProvidedSkins skins = HDSkins.getInstance().getProfileRepository().load(profile.gameProfile()).getNow(ProvidedSkins.EMPTY);
                if (skins != ProvidedSkins.EMPTY) {
                    data = skins.getSkin(SkinType.SKIN).map(skin -> new PlayerHeadModelRenderer.Data(SkullBlockEntityRenderer.getTranslucentRenderLayer(skin))).orElse(null);
                    if (data != null) {
                        hdskins_profileCache.put(profile, data);
                        info.setReturnValue(data);
                    }
                }
            }
        }
    }
}
