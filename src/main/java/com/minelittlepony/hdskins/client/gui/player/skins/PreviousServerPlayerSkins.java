package com.minelittlepony.hdskins.client.gui.player.skins;

import java.util.Optional;
import java.util.function.Supplier;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.player.skins.ServerPlayerSkins.RemoteTexture;
import com.minelittlepony.hdskins.client.resources.DynamicTextures;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.client.resources.HDPlayerSkinTextureDownloader;
import com.minelittlepony.hdskins.client.resources.TextureLoader.Exclusion;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.server.SkinServer;
import com.mojang.authlib.GameProfile;

import net.minecraft.resources.Identifier;

public class PreviousServerPlayerSkins extends PlayerSkins<ServerPlayerSkins.RemoteTexture> {

    private final ServerPlayerSkins fallback;
    private final SkinServer.SkinServerProfile.Skin skin;
    private final SkinType type;

    public PreviousServerPlayerSkins(ServerPlayerSkins fallback, SkinServer.SkinServerProfile.Skin skin, SkinType type) {
        super(new PostureOverrides(fallback.getPosture()));
        this.fallback = fallback;
        this.skin = skin;
        this.type = type;
    }

    public SkinServer.SkinServerProfile.Skin getSkin() {
        return skin;
    }

    public SkinType getType() {
        return type;
    }

    @Override
    protected ServerPlayerSkins.RemoteTexture createTexture(SkinType type, Supplier<Identifier> blank) {
        if (type == this.type) {
            String uri = skin.getUri();
            String hash = String.valueOf(uri.hashCode());
            return new RemoteTexture(blank,
                new DynamicTextures.Result(uri, HDPlayerSkinTextureDownloader.downloadAndRegisterTexture(
                        HDSkins.id(String.format("dynamic/%s/%s", type.getId().getPath(), hash)),
                        DynamicTextures.createTempFile(hash),
                        uri, type
                )),
                skin.isActive()
            );
        }
        return fallback.get(type);
    }

    @Override
    protected boolean isProvided(SkinType type) {
        return type == this.type || fallback.isProvided(type);
    }

    @Override
    public String getSkinVariant() {
        return fallback.getSkinVariant();
    }

    record PostureOverrides (Posture posture) implements Posture {
        @Override
        public GameProfile getProfile() {
            return posture.getProfile();
        }

        @Override
        public EquipmentSet getEquipment() {
            return HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
        }

        @Override
        public Pose getPose() {
            return Pose.STAND;
        }

        @Override
        public SkinType getActiveSkinType() {
            return posture.getActiveSkinType();
        }

        @Override
        public Optional<SkinVariant> getSkinVariant() {
            return posture.getSkinVariant();
        }

        @Override
        public Identifier getDefaultSkin(SkinType type, String variant) {
            return posture.getDefaultSkin(type, variant);
        }

        @Override
        public Exclusion getExclusion() {
            return posture.getExclusion();
        }
    }
}
