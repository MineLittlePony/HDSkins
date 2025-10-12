package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.PlayerBodyWidget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class EquipmentList extends JsonDataLoader<EquipmentList.EquipmentSet> implements ResourceReloader {
    public static final Identifier EQUIPMENT = HDSkins.id("skins/equipment");
    private static final Identifier EMPTY = HDSkins.id("empty");

    private EquipmentSet emptySet = EquipmentSet.EMPTY;

    private Map<Identifier, EquipmentSet> equipmentSets = Map.of(EMPTY, emptySet);

    public EquipmentList() {
        super(EquipmentSet.CODEC, ResourceFinder.json("hd_skins_equipment"));
    }

    @Override
    protected Map<Identifier, EquipmentSet> prepare(ResourceManager resourceManager, Profiler profiler) {
        return super.prepare(resourceManager, profiler);
    }

    @Override
    protected void apply(Map<Identifier, EquipmentSet> sets, ResourceManager manager, Profiler profiler) {
        HDSkins.LOGGER.info("Found {} potential player equipment sets", sets.size());
        equipmentSets = new HashMap<>(sets);
        emptySet = equipmentSets.computeIfAbsent(EMPTY, k -> EquipmentSet.EMPTY);
    }

    public EquipmentSet getDefault() {
        return emptySet;
    }

    public Stream<EquipmentSet> getValues() {
        return equipmentSets.values().stream();
    }

    public static record EquipmentSet(
            Map<EquipmentSlot, Item> equipment,
            Item item,
            Optional<SoundEvent> sound,
            String tooltip
        ) {
        private static final EquipmentSet EMPTY = new EquipmentSet(Map.of(), Items.PLAYER_HEAD, null, "empty");
        public static final Codec<EquipmentSet> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(EquipmentSlot.CODEC, Registries.ITEM.getCodec()).fieldOf("equipment").forGetter(EquipmentSet::equipment),
                Registries.ITEM.getCodec().fieldOf("item").forGetter(EquipmentSet::item),
                Registries.SOUND_EVENT.getCodec().optionalFieldOf("sound").forGetter(EquipmentSet::sound),
                Codec.STRING.fieldOf("tooltip").forGetter(EquipmentSet::tooltip)
        ).apply(i, EquipmentSet::new));

        public void apply(PlayerBodyWidget<?> state) {
            state.playerState.equippedHeadStack = getStack(EquipmentSlot.HEAD);
            state.playerState.equippedChestStack = getStack(EquipmentSlot.CHEST);
            state.playerState.equippedLegsStack = getStack(EquipmentSlot.LEGS);
            state.playerState.equippedFeetStack = getStack(EquipmentSlot.FEET);
            state.setHandStack(Hand.MAIN_HAND, getStack(EquipmentSlot.MAINHAND));
            state.setHandStack(Hand.OFF_HAND, getStack(EquipmentSlot.OFFHAND));
        }

        public SoundEvent getSound() {
            return sound.orElse(SoundEvents.ITEM_ARMOR_EQUIP_GENERIC.value());
        }

        public ItemStack getStack(EquipmentSlot slot) {
            return equipment.getOrDefault(slot, Items.AIR).getDefaultStack();
        }

        public ItemStack getStack() {
            return new ItemStack(item);
        }

        public Text getTooltip() {
            return Text.translatable("hdskins.equipment", Text.translatable(tooltip));
        }
    }
}
