package com.minelittlepony.hdskins.fabric.core;

import com.minelittlepony.hdskins.core.PrivateFields;

public class YarnFields extends PrivateFields {
    @Override
    protected PrivateFields.ObfHelperFactory factory() {
        return YarnObfHelper::new;
    }

    @Override
    protected String ClientPlayNetworkHandler_playerListEntries() {
        return "field_3693";
    }

    @Override
    protected String PlayerSkinProvider_skinCache() {
        return "field_5306";
    }

    @Override
    protected String PlayerSkinProvider_textureManager() {
        return "field_5304";
    }
}
