package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.PlayerBodyWidget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class EquipmentList extends SimpleJsonResourceReloadListener<EquipmentList.EquipmentSet> {
    public static final Identifier EQUIPMENT = HDSkins.id("skins/equipment");
    private static final Identifier EMPTY = HDSkins.id("empty");

    private EquipmentSet emptySet = EquipmentSet.EMPTY;

    private Map<Identifier, EquipmentSet> equipmentSets = Map.of(EMPTY, emptySet);

    public EquipmentList() {
        super(EquipmentSet.CODEC, FileToIdConverter.json("hd_skins_equipment"));
    }

    @Override
    protected Map<Identifier, EquipmentSet> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return super.prepare(resourceManager, profiler);
    }

    @Override
    protected void apply(Map<Identifier, EquipmentSet> sets, ResourceManager manager, ProfilerFiller profiler) {
        HDSkins.LOGGER.info("Found {} potential player equipment sets", sets.size());
        equipmentSets = new HashMap<>(sets);
        emptySet = equipmentSets.computeIfAbsent(EMPTY, _ -> EquipmentSet.EMPTY);
    }

    public EquipmentSet getDefault() {
        return emptySet;
    }

    public Stream<EquipmentSet> getValues() {
        return equipmentSets.values().stream();
    }

    public static record EquipmentSet(
            Map<EquipmentSlot, Item> equipment,
            ResourceKey<EquipmentAsset> assetId,
            Item item,
            Optional<SoundEvent> sound,
            String tooltip
        ) {
        private static final EquipmentSet EMPTY = new EquipmentSet(Map.of(), EquipmentAssets.CHAINMAIL, Items.PLAYER_HEAD, null, "empty");
        public static final Codec<EquipmentSet> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(EquipmentSlot.CODEC, BuiltInRegistries.ITEM.byNameCodec()).fieldOf("equipment").forGetter(EquipmentSet::equipment),
                ResourceKey.codec(EquipmentAssets.ROOT_ID).fieldOf("asset_id").forGetter(EquipmentSet::assetId),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(EquipmentSet::item),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(EquipmentSet::sound),
                Codec.STRING.fieldOf("tooltip").forGetter(EquipmentSet::tooltip)
        ).apply(i, EquipmentSet::new));

        public void apply(PlayerBodyWidget<?> state) {
            EquipmentSlot.VALUES.forEach(slot -> {
                if (slot.getType() == Type.HUMANOID_ARMOR) {
                    state.setEquippedStack(slot, getStack(slot));
                }
            });
        }

        public SoundEvent getSound() {
            return sound.orElse(SoundEvents.ARMOR_EQUIP_GENERIC.value());
        }

        public Optional<ItemStackTemplate> getStack(EquipmentSlot slot) {
            Item item = equipment.getOrDefault(slot, Items.AIR);
            if (item == Items.AIR) {
                return Optional.empty();
            }
            return Optional.of(new ItemStackTemplate(item, DataComponentPatch.builder()
                    .set(DataComponents.EQUIPPABLE, Equippable.builder(slot)
                            .setEquipSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(getSound()))
                            .setAsset(assetId)
                            .build())
                    .build()
                ));
        }

        public ItemStackTemplate getStack() {
            return new ItemStackTemplate(item);
        }

        public Component getTooltip() {
            return Component.translatable("hdskins.equipment", Component.translatable(tooltip));
        }
    }
}
