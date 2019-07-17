package com.minelittlepony.hdskins.dummy;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.minelittlepony.common.util.settings.ToStringAdapter;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.util.RegistryTypeAdapter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
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
            .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
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
        logger.info("Loading player equipment sets");

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

    public Iterator<EquipmentSet> getCycler() {
        return Iterators.cycle(equipmentSets);
    }

    public static class EquipmentSet {
        private Map<EquipmentSlot, Item> equipment = Collections.emptyMap();

        private Item item;

        private transient Identifier id;

        public void apply(LivingEntity entity) {
            Maps.transformValues(equipment, ItemStack::new).forEach(entity::setEquippedStack);
        }

        public ItemStack getStack() {
            return new ItemStack(item);
        }

        public Identifier getId() {
            return id;
        }
    }
}
