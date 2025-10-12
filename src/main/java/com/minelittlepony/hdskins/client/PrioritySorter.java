package com.minelittlepony.hdskins.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.minelittlepony.hdskins.client.profile.DynamicSkinTextures;
import com.minelittlepony.hdskins.profile.SkinType;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.AssetInfo.TextureAsset;
import net.minecraft.util.Identifier;

public class PrioritySorter {

    private final List<Selector> selectors = new ArrayList<>();

    public void addSelector(Selector selector) {
        this.selectors.add(selector);
    }

    public PlayerSkinLayers.Layer selectBest(SkinType type, PlayerSkinLayers skins) {
        if (selectors.isEmpty()) {
            return skins.combined();
        }
        if (selectors.size() == 1) {
            return selectors.get(0).selectBest(type, skins);
        }
        var best = new Object() {
            PlayerSkinLayers.Layer best = skins.combined();
            int bestCount = 0;
        };
        Object2IntOpenHashMap<PlayerSkinLayers.Layer> picks = new Object2IntOpenHashMap<>();
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
        PlayerSkinLayers.Layer selectBest(SkinType type, PlayerSkinLayers skins);
    }

    public DynamicSkinTextures createDynamicTextures(PlayerSkinLayers playerSkins) {
        return new DynamicSkinTextures() {
            @Override
            public Set<Identifier> getProvidedSkinTypes() {
                return playerSkins.combined().dynamic().getProvidedSkinTypes();
            }

            @Override
            public Optional<TextureAsset> getSkin(SkinType type) {
                if (selectors.isEmpty()) {
                    return playerSkins.combined().dynamic().getSkin(type);
                }
                return selectBest(type, playerSkins).getSkin(type);
            }

            @Override
            public Optional<String> getModel() {
                if (selectors.isEmpty()) {
                    return playerSkins.combined().dynamic().getModel();
                }
                return selectBest(SkinType.SKIN, playerSkins).dynamic().getModel();
            }

            @Override
            public boolean hasChanged() {
                return playerSkins.hasChanged();
            }
        };
    }
}
