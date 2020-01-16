package com.minelittlepony.hdskins.client.dummy;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.minelittlepony.common.util.settings.ToStringAdapter;
import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.util.RegistryTypeAdapter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class EquipmentList extends JsonDataLoader implements IdentifiableResourceReloadListener {

    private static final Identifier EQUIPMENT = new Identifier(HDSkins.MOD_ID, "skins/equipment");

    private static final Logger logger = LogManager.getLogger();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new ToStringAdapter<>(Identifier::new))
            .registerTypeAdapter(Item.class, new RegistryTypeAdapter<>(Registry.ITEM))
            .registerTypeAdapter(EquipmentSlot.class, new ToStringAdapter<>(EquipmentSlot::getName, s -> EquipmentSlot.byName(s.toLowerCase())))
            .create();

    private EquipmentSet emptySet = new EquipmentSet();

    private final List<EquipmentSet> equipmentSets = Lists.newArrayList(emptySet);

    public EquipmentList() {
        super(gson, "hd_skins_equipment");
    }

    @Override
    public Identifier getFabricId() {
        return EQUIPMENT;
    }

    @Override
    protected void apply(Map<Identifier, JsonObject> resources, ResourceManager manager, Profiler profiler) {
        emptySet = new EquipmentSet();
        equipmentSets.clear();

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
        logger.info("Loaded {} player equipment sets", equipmentSets.size());

        if (equipmentSets.isEmpty()) {
            equipmentSets.add(emptySet);
        }
    }

    public EquipmentSet getDefault() {
        return emptySet;
    }

    public Iterator<EquipmentSet> getCycler() {
        return Iterators.cycle(equipmentSets);
    }

    public static class EquipmentSet {
        private Map<EquipmentSlot, Item> equipment = new EnumMap<>(EquipmentSlot.class);

        private Item item;

        private transient Identifier id;

        public void apply(LivingEntity entity) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                entity.equipStack(slot, new ItemStack(equipment.getOrDefault(slot, Items.AIR)));
            }
        }

        public ItemStack getStack() {
            return new ItemStack(item);
        }

        public Identifier getId() {
            return id;
        }
    }
}
