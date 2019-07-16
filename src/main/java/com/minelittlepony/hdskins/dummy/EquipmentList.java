package com.minelittlepony.hdskins.dummy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.minelittlepony.common.util.settings.ToStringAdapter;
import com.minelittlepony.hdskins.HDSkins;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

public class EquipmentList extends JsonDataLoader implements IdentifiableResourceReloadListener {

    private static final Identifier EQUIPMENT = new Identifier(HDSkins.MOD_ID, "skins/equipment");

    private static final Logger logger = LogManager.getLogger();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new ToStringAdapter<>(Identifier::new))
            .registerTypeAdapter(EquipmentSlot.class, new ToStringAdapter<>(s -> EquipmentSlot.byName(s.toLowerCase())))
            .create();

    private EquipmentSet emptySet = new EquipmentSet();

    private final List<EquipmentSet> equipmentSets = new ArrayList<>();


    public EquipmentList() {
        super(gson, "hd_skins_equipment");
    }

    @Override
    public Identifier getFabricId() {
        return EQUIPMENT;
    }

    // $FF: synthetic method
    @Override
    protected Map<Identifier, JsonObject> prepare(ResourceManager manager, Profiler profiler) {
        return super.method_20731(manager, profiler);
    }

    @Override
    protected void apply(Map<Identifier, JsonObject> resources, ResourceManager manager, Profiler profiler) {
        emptySet = new EquipmentSet();
        equipmentSets.clear();
        logger.info("Loading player requipment sets");

        for (Entry<Identifier, JsonObject> entry : resources.entrySet()) {
           try {
               EquipmentSet set = gson.fromJson(entry.getValue(), EquipmentSet.class);

               if (set != null) {
                   set.id = entry.getKey();
                   equipmentSets.add(set);

                   if ("empty".equals(entry.getKey().getPath())) {
                       emptySet = set;
                   }
               }
           } catch (IllegalArgumentException | JsonParseException e) {
               logger.error("Unable to read {} from resource packs", EQUIPMENT, e);
           }
        }
    }

    public EquipmentSet getDefault() {
        return emptySet;
    }

    public List<EquipmentSet> getList() {
        return ImmutableList.copyOf(equipmentSets);
    }

    public Iterator<EquipmentSet> getIterator() {
        return getList().iterator();
    }

    public Iterator<EquipmentSet> getCycler() {
        return Iterators.cycle(equipmentSets);
    }

    protected static ItemStack createStack(Identifier item, int damage) {
        ItemStack stack = Registry.ITEM.getOrEmpty(item).map(ItemStack::new).orElse(ItemStack.EMPTY);
        stack.setDamage(damage);
        return stack;
    }

    public static class EquipmentSet {
        private List<Equipment> equipment = Collections.emptyList();

        private Identifier item;

        private int damage;

        transient Identifier id;

        @Nullable
        private transient ItemStack stack;

        public void apply(LivingEntity entity) {
            equipment.forEach(eq -> eq.apply(entity));
        }

        public ItemStack getStack() {
            if (stack == null) {
                stack = createStack(item, damage);
            }
            return stack;
        }

        public Identifier getId() {
            return id;
        }

        private static class Equipment {
            private EquipmentSlot slot;

            private Identifier item;

            private int damage;

            @Nullable
            private transient ItemStack stack;

            public ItemStack getStack() {
                if (stack == null) {
                    stack = createStack(item, damage);
                }
                return stack;
            }

            public void apply(LivingEntity entity) {
                entity.setEquippedStack(slot, getStack());
            }
        }
    }
}
