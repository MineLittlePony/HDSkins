package com.minelittlepony.hdskins.util;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;

@Deprecated
public class PlayerUtil {

    private static final Field playerInfo = FieldUtils.getAllFields(AbstractClientPlayerEntity.class)[0];

    public static PlayerListEntry getInfo(AbstractClientPlayerEntity player) {
        try {
            if (!playerInfo.isAccessible()) {
                playerInfo.setAccessible(true);
            }
            return (PlayerListEntry) FieldUtils.readField(playerInfo, player);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
