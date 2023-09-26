package com.minelittlepony.hdskins.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.minelittlepony.hdskins.client.profile.DynamicSkinTextures;
import com.minelittlepony.hdskins.profile.SkinType;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public class PrioritySorter {

    private final List<Selector> selectors = new ArrayList<>();

    public void addSelector(Selector selector) {
        this.selectors.add(selector);
    }

    public PlayerSkins.Layer selectBest(SkinType type, PlayerSkins skins) {
        if (selectors.isEmpty()) {
            return skins.combined();
        }
        if (selectors.size() == 1) {
            return selectors.get(0).selectBest(type, skins);
        }
        var best = new Object() {
            PlayerSkins.Layer best = skins.combined();
            int bestCount = 0;
        };
        Object2IntOpenHashMap<PlayerSkins.Layer> picks = new Object2IntOpenHashMap<>();
        selectors.forEach(selector -> {
            picks.computeInt(selector.selectBest(type, skins), (pick, count) -> {
                count = count == null ? 1 : count + 1;
                if (count > best.bestCount) {
                    best.best = pick;
                }
                return count;
            });
        });
        return best.best;
    }

    public interface Selector {
        PlayerSkins.Layer selectBest(SkinType type, PlayerSkins skins);
    }

    public DynamicSkinTextures createDynamicTextures(PlayerSkins playerSkins) {
        return new PriorityDynamicSkinTextures(playerSkins);
    }

    class PriorityDynamicSkinTextures implements DynamicSkinTextures {
        private final PlayerSkins playerSkins;
        private final Supplier<SkinTextures> skinTextures;

        PriorityDynamicSkinTextures(PlayerSkins playerSkins) {
            this.playerSkins = playerSkins;
            this.skinTextures = Suppliers.memoizeWithExpiration(() -> DynamicSkinTextures.super.toSkinTextures(), 1, TimeUnit.SECONDS);
        }

        @Override
        public Set<Identifier> getProvidedSkinTypes() {
            return playerSkins.combined().dynamic().get().getProvidedSkinTypes();
        }

        @Override
        public Optional<Identifier> getSkin(SkinType type) {
            return selectBest(type, playerSkins).getSkin(type);
        }

        @Override
        public String getModel(String fallback) {
            return selectBest(SkinType.SKIN, playerSkins).dynamic().get().getModel(fallback);
        }

        @Override
        public SkinTextures toSkinTextures() {
            if (selectors.isEmpty()) {
                return playerSkins.combined().resolved().get();
            }
            return skinTextures.get();
        }
    }
}
