package com.minelittlepony.hdskins.client.gui.player.skins;

import com.minelittlepony.common.client.gui.sprite.TextureSprite;
import com.minelittlepony.common.client.gui.style.Style;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.VanillaSkins;
import com.minelittlepony.hdskins.client.gui.GuiSkins;
import com.minelittlepony.hdskins.client.resources.EquipmentList.EquipmentSet;
import com.minelittlepony.hdskins.profile.SkinType;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
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
    };

    protected final Map<SkinType, T> textures = new HashMap<>();

    @Nullable
    private Set<Identifier> providedSkinTypes;
    private long setAt;

    protected final Posture posture;

    protected PlayerSkins(Posture posture) {
        this.posture = posture;
    }

    protected abstract T createTexture(SkinType type, Supplier<Identifier> blank);

    public Posture getPosture() {
        return posture;
    }

    public boolean usesThinSkin() {
        return false;
    }

    public T get(SkinType type) {
        return textures.computeIfAbsent(type, t -> createTexture(t, () -> posture.getDefaultSkin(t, usesThinSkin())));
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
                return MinecraftClient.getInstance().getSession().getProfile();
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
            public Identifier getDefaultSkin(SkinType type, boolean slim) {
                return VanillaSkins.getDefaultTexture(type, slim);
            }

            @Override
            public EquipmentSet getEquipment() {
                return HDSkins.getInstance().getDummyPlayerEquipmentList().getDefault();
            }
        };

        GameProfile getProfile();

        EquipmentSet getEquipment();

        Pose getPose();

        SkinType getActiveSkinType();

        Identifier getDefaultSkin(SkinType type, boolean slim);

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
