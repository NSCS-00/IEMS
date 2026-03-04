package com.dlzstudio.iems.entities;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.EnergyStorageBlock;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.blocks.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class IEMSEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, IEMSMod.MODID);

    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_ENTITY = BLOCK_ENTITIES.register(
        "energy_storage_entity",
        () -> BlockEntityType.Builder.of((pos, state) -> new EnergyStorageBlockEntity(pos, state, EnergyStorageBlock.StorageType.STANDARD), IEMSBlocks.STANDARD_ENERGY_STORAGE.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyConverterBlockEntity>> ENERGY_CONVERTER_ENTITY = BLOCK_ENTITIES.register(
        "energy_converter_entity",
        () -> BlockEntityType.Builder.of(EnergyConverterBlockEntity::new, IEMSBlocks.ENERGY_CONVERTER.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyRelayBlockEntity>> ENERGY_RELAY_ENTITY = BLOCK_ENTITIES.register(
        "energy_relay_entity",
        () -> BlockEntityType.Builder.of(EnergyRelayBlockEntity::new, IEMSBlocks.ENERGY_RELAY.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyBroadcastTowerBlockEntity>> ENERGY_BROADCAST_TOWER_ENTITY = BLOCK_ENTITIES.register(
        "energy_broadcast_tower_entity",
        () -> BlockEntityType.Builder.of(EnergyBroadcastTowerBlockEntity::new, IEMSBlocks.ENERGY_BROADCAST_TOWER.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyMarkerBlockEntity>> ENERGY_MARKER_ENTITY = BLOCK_ENTITIES.register(
        "energy_marker_entity",
        () -> BlockEntityType.Builder.of(EnergyMarkerBlockEntity::new).build(null)
    );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
