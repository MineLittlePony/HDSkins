package com.minelittlepony.hdskins.client.gui.player.skins;

import com.google.common.base.MoreObjects;
import com.minelittlepony.common.client.gui.sprite.ISprite;
import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.VanillaModels;
import com.minelittlepony.hdskins.client.VanillaSkins;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.resources.TextureLoader;
import com.minelittlepony.hdskins.client.resources.TextureLoader.Exclusion;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.client.resources.NativeImageFilters;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

import java.io.Closeable;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public abstract class PlayerSkins<T extends PlayerSkins.PlayerSkin> implements Closeable {
    public static final PlayerSkins<PlayerSkin> EMPTY = new PlayerSkins<>(Posture.NULL) {
        @Override
        public PlayerSkin createTexture(SkinType type, Supplier<Identifier> blank) {
            return new PlayerSkins.PlayerSkin() {
                @Override
                public Identifier getId() {
                    return blank.get();
                }

                @Override
                public void close() { }

                @Override
                public boolean isReady() {
                    return false;
                }
            };
        }

        @Override
        protected boolean isProvided(SkinType type) {
            return false;
        }

        @Override
        public String getSkinVariant() {
            return VanillaModels.DEFAULT;
        }
    };

    protected final Map<SkinType, T> textures = new HashMap<>();

    @Nullable
    private Set<Identifier> providedSkinTypes;
    @Nullable
    private net.minecraft.world.entity.player.PlayerSkin bundle;
    private long setAt;

    private final Posture posture;

    protected PlayerSkins(Posture posture) {
        this.posture = posture;
    }

    protected abstract T createTexture(SkinType type, Supplier<Identifier> blank);

    public Posture getPosture() {
        return posture;
    }

    public abstract String getSkinVariant();

    public T get(SkinType type) {
        return textures.computeIfAbsent(type, t -> createTexture(t, () -> posture.getDefaultSkin(t, getSkinVariant())));
    }

    public boolean hasAny() {
        return textures.values().stream().anyMatch(PlayerSkins.PlayerSkin::isReady);
    }

    public Set<Identifier> getProvidedSkinTypes() {
        long now = System.currentTimeMillis();
        if (providedSkinTypes == null || setAt < now) {
            setAt = now + 500;
            providedSkinTypes = SkinType.REGISTRY.stream().filter(this::isProvided).map(SkinType::getId).collect(Collectors.toSet());
        }
        return providedSkinTypes;
    }

    protected abstract boolean isProvided(SkinType type);

    public net.minecraft.world.entity.player.PlayerSkin getSkinTextureBundle() {
        ClientAsset.Texture skinId = get(SkinType.SKIN).getAsset();
        return net.minecraft.world.entity.player.PlayerSkin.insecure(
                getGreyScaleSkinIfActive(SkinType.SKIN, skinId),
                getGreyScaleSkinIfActive(SkinType.CAPE, get(SkinType.CAPE).getAsset()),
                getPosture().getActiveSkinType() == SkinType.ELYTRA ? MoreObjects.firstNonNull(get(SkinType.CAPE).getAsset(), get(SkinType.ELYTRA).getAsset()) : null,
                VanillaModels.isSlim(getSkinVariant()) ? PlayerModelType.SLIM : PlayerModelType.WIDE
        );
    }

    @Nullable
    private ClientAsset.Texture getGreyScaleSkinIfActive(SkinType type, @Nullable ClientAsset.Texture asset) {
        return getPosture().getActiveSkinType() == type ? asset : getGreyScaleSkin(asset);
    }

    @Nullable
    private ClientAsset.Texture getGreyScaleSkin(@Nullable ClientAsset.Texture asset) {
        if (asset == null) {
            return null;
        }
        Identifier newPath = NativeImageFilters.REDUCE_ALPHA.load(asset.texturePath(), asset.texturePath(), getPosture().getExclusion());
        if (newPath.equals(asset.texturePath())) {
            return asset;
        }
        return new ClientAsset.ResourceTexture(asset.id().withSuffix("_greyscaled"), newPath);
    }

    @Override
    public void close() {
        textures.values().forEach(PlayerSkin::close);
        textures.clear();
        providedSkinTypes = null;
        bundle = null;
    }

    public interface PlayerSkin extends Closeable {
        Identifier getId();

        default ClientAsset.Texture getAsset() {
            Identifier id = getId();
            return new ClientAsset.ResourceTexture(id, id);
        }

        @Override
        void close();

        boolean isReady();
    }

    public interface SkinFactory<T extends PlayerSkins.PlayerSkin, K extends PlayerSkins<? extends T>> {
        T create(SkinType type, Supplier<Identifier> blank, K parent);
    }

    public interface Posture {
        Posture NULL = new Posture() {
            @Override
            public GameProfile getProfile() {
                return Minecraft.getInstance().getGameProfile();
            }

            @Override
            public Pose getPose() {
                return Pose.STAND;
            }

            @Override
            public SkinType getActiveSkinType() {
                return SkinType.SKIN;
            }

            @Override
            public Optional<SkinVariant> getSkinVariant() {
                return Optional.empty();
            }

            @Override
            public Identifier getDefaultSkin(SkinType type, String variant) {
                return VanillaSkins.getDefaultTexture(type, variant);
            }

            @Override
            public EquipmentSet getEquipment() {
                return HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
            }

            @Override
            public Exclusion getExclusion() {
                return TextureLoader.Exclusion.NULL;
            }
        };

        GameProfile getProfile();

        EquipmentSet getEquipment();

        Pose getPose();

        SkinType getActiveSkinType();

        Optional<SkinVariant> getSkinVariant();

        Identifier getDefaultSkin(SkinType type, String variant);

        TextureLoader.Exclusion getExclusion();

        public record SkinVariant (Component tooltip, ISprite icon, String name) {
            public static final Set<SkinVariant> VALUES = new HashSet<>();
            public static final SkinVariant DEFAULT = new SkinVariant(HDSkins.id(VanillaModels.DEFAULT));
            public static final SkinVariant SLIM = new SkinVariant(HDSkins.id(VanillaModels.SLIM));

            public SkinVariant(Identifier id) {
                this(
                        Component.translatable("hdskins.arm_style", Component.translatable(id.getNamespace() + ".arm_style." + id.getPath())),
                        GuiSkins.createIcon(32, 16 * VALUES.size()),
                        id.getPath()
                );
                VALUES.add(this);
            }
        }

        public enum Pose {
            STAND(net.minecraft.world.entity.Pose.STANDING),
            SLEEP(net.minecraft.world.entity.Pose.SLEEPING),
            RIDE(net.minecraft.world.entity.Pose.SITTING),
            SWIM(net.minecraft.world.entity.Pose.SWIMMING),
            RIPTIDE(net.minecraft.world.entity.Pose.SPIN_ATTACK);

            public static final Pose[] VALUES = values();
            public static final Style[] STYLES = Arrays.stream(VALUES).map(Pose::getStyle).toArray(Style[]::new);

            private final Style style = new Style()
                    .setIcon(new TextureSprite()
                            .setTexture(GuiSkins.WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 16 * ordinal()))
                        .setTooltip(Component.translatable("hdskins.mode", Component.translatable("hdskins.mode." + name().toLowerCase(Locale.ROOT))), 0, 10);

            private final net.minecraft.world.entity.Pose pose;

            Pose(net.minecraft.world.entity.Pose pose) {
                this.pose = pose;
            }

            public Style getStyle() {
                return style;
            }

            public net.minecraft.world.entity.Pose getPose() {
                return pose;
            }
        }
    }
}
