package com.minelittlepony.hdskins.client.resources;

import com.minelittlepony.hdskins.client.HDSkins;
import com.minelittlepony.hdskins.client.gui.PlayerBodyWidget;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

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
            Item item,
            Optional<SoundEvent> sound,
            String tooltip
        ) {
        private static final EquipmentSet EMPTY = new EquipmentSet(Map.of(), Items.PLAYER_HEAD, null, "empty");
        public static final Codec<EquipmentSet> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(EquipmentSlot.CODEC, BuiltInRegistries.ITEM.byNameCodec()).fieldOf("equipment").forGetter(EquipmentSet::equipment),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(EquipmentSet::item),
                BuiltInRegistries.SOUND_EVENT.byNameCodec().optionalFieldOf("sound").forGetter(EquipmentSet::sound),
                Codec.STRING.fieldOf("tooltip").forGetter(EquipmentSet::tooltip)
        ).apply(i, EquipmentSet::new));

        public void apply(PlayerBodyWidget<?> state) {
            state.playerState.headEquipment = getStack(EquipmentSlot.HEAD).create();
            state.playerState.chestEquipment = getStack(EquipmentSlot.CHEST).create();
            state.playerState.legsEquipment = getStack(EquipmentSlot.LEGS).create();
            state.playerState.feetEquipment = getStack(EquipmentSlot.FEET).create();
            state.setHandStack(InteractionHand.MAIN_HAND, getStack(EquipmentSlot.MAINHAND));
            state.setHandStack(InteractionHand.OFF_HAND, getStack(EquipmentSlot.OFFHAND));
        }

        public SoundEvent getSound() {
            return sound.orElse(SoundEvents.ARMOR_EQUIP_GENERIC.value());
        }

        public ItemStackTemplate getStack(EquipmentSlot slot) {
            return new ItemStackTemplate(equipment.getOrDefault(slot, Items.AIR));
        }

        public ItemStackTemplate getStack() {
            return new ItemStackTemplate(item);
        }

        public Component getTooltip() {
            return Component.translatable("hdskins.equipment", Component.translatable(tooltip));
        }
    }
}
