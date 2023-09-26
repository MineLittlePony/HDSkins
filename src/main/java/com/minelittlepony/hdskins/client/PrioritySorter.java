package com.minelittlepony.hdskins.client;

import java.util.ArrayList;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PrioritySorter<A, B> {

    private final List<Selector<A, B>> selectors = new ArrayList<>();

    public void addSelector(Selector<A, B> selector) {
        this.selectors.add(selector);
    }

    public B selectBest(A a, B def) {
        if (selectors.isEmpty()) {
            return def;
        }
        if (selectors.size() == 1) {
            return selectors.get(0).selectBest(a);
        }
        var best = new Object() {
            B best = def;
            int bestCount = 0;
        };
        Object2IntOpenHashMap<B> picks = new Object2IntOpenHashMap<>();
        selectors.forEach(selector -> {
            picks.computeInt(selector.selectBest(a), (pick, count) -> {
                count = count == null ? 1 : count + 1;
                if (count > best.bestCount) {
                    best.best = pick;
                }
                return count;
            });
        });
        return best.best;
    }

    public interface Selector<A, B> {
        B selectBest(A a);
    }
}
