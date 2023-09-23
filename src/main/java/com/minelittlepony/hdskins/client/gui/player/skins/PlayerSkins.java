package com.minelittlepony.hdskins.client.gui.player.skins;

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
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.SkinTextures.Model;
import net.minecraft.entity.EntityPose;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
    private SkinTextures bundle;
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

    public SkinTextures getSkinTextureBundle() {
        if (bundle == null) {
            bundle = new SkinTextures(
                    get(SkinType.SKIN).getId(),
                    null,
                    getPosture().getActiveSkinType() == SkinType.CAPE ? get(SkinType.CAPE).getId() : null,
                    getPosture().getActiveSkinType() == SkinType.ELYTRA ? get(SkinType.ELYTRA).getId() : null,
                    VanillaModels.isSlim(getSkinVariant()) ? Model.SLIM : Model.WIDE,
                    false
            );
        }
        return bundle;
    }

    @Override
    public void close() {
        textures.values().forEach(PlayerSkin::close);
        textures.clear();
        providedSkinTypes = null;
    }

    public interface PlayerSkin extends Closeable {
        Identifier getId();

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
                return MinecraftClient.getInstance().getGameProfile();
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

        public record SkinVariant (Text tooltip, ISprite icon, String name) {
            public static final Set<SkinVariant> VALUES = new HashSet<>();
            public static final SkinVariant DEFAULT = new SkinVariant(new Identifier("hdskins", VanillaModels.DEFAULT));
            public static final SkinVariant SLIM = new SkinVariant(new Identifier("hdskins", VanillaModels.SLIM));

            public SkinVariant(Identifier id) {
                this(
                        Text.translatable("hdskins.arm_style", Text.translatable(id.getNamespace() + ".arm_style." + id.getPath())),
                        GuiSkins.createIcon(32, 16 * VALUES.size()),
                        id.getPath()
                );
                VALUES.add(this);
            }
        }

        public enum Pose {
            STAND(EntityPose.STANDING),
            SLEEP(EntityPose.SLEEPING),
            RIDE(EntityPose.SITTING),
            SWIM(EntityPose.SWIMMING),
            RIPTIDE(EntityPose.SPIN_ATTACK);

            public static final Pose[] VALUES = values();
            public static final Style[] STYLES = Arrays.stream(VALUES).map(Pose::getStyle).toArray(Style[]::new);

            private final Style style = new Style()
                    .setIcon(new TextureSprite()
                            .setTexture(GuiSkins.WIDGETS_TEXTURE)
                            .setPosition(2, 2)
                            .setSize(16, 16)
                            .setTextureOffset(96, 16 * ordinal()))
                        .setTooltip(Text.translatable("hdskins.mode", Text.translatable("hdskins.mode." + name().toLowerCase(Locale.ROOT))), 0, 10);

            private final EntityPose pose;

            Pose(EntityPose pose) {
                this.pose = pose;
            }

            public Style getStyle() {
                return style;
            }

            public EntityPose getPose() {
                return pose;
            }
        }
    }
}
