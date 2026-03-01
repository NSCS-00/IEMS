package com.dlzstudio.iems.entities;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 实体类型注册
 */
public class IEMSEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.createBlockEntityTypes(IEMSMod.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.createEntityTypes(IEMSMod.MODID);
    
    // ============ 方块实体 ============
    
    // 能量存储器实�?    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_ENTITY = 
        BLOCK_ENTITIES.register("energy_storage_entity", () -> 
            BlockEntityType.Builder.of(EnergyStorageBlockEntity::new, 
                com.dlzstudio.iems.blocks.IEMSBlocks.STANDARD_ENERGY_STORAGE.get(),
                com.dlzstudio.iems.blocks.IEMSBlocks.GENERAL_ENERGY_STORAGE.get()).build(null));
    
    // 能量转换器实�?    public static final Supplier<BlockEntityType<EnergyConverterBlockEntity>> ENERGY_CONVERTER_ENTITY = 
        BLOCK_ENTITIES.register("energy_converter_entity", () -> 
            BlockEntityType.Builder.of(EnergyConverterBlockEntity::new, 
                com.dlzstudio.iems.blocks.IEMSBlocks.ENERGY_CONVERTER.get()).build(null));
    
    // 能源中继传输器实�?    public static final Supplier<BlockEntityType<EnergyRelayBlockEntity>> ENERGY_RELAY_ENTITY = 
        BLOCK_ENTITIES.register("energy_relay_entity", () -> 
            BlockEntityType.Builder.of(EnergyRelayBlockEntity::new, 
                com.dlzstudio.iems.blocks.IEMSBlocks.ENERGY_RELAY.get()).build(null));
    
    // 能源广播塔实�?    public static final Supplier<BlockEntityType<EnergyBroadcastTowerBlockEntity>> ENERGY_BROADCAST_TOWER_ENTITY = 
        BLOCK_ENTITIES.register("energy_broadcast_tower_entity", () -> 
            BlockEntityType.Builder.of(EnergyBroadcastTowerBlockEntity::new, 
                com.dlzstudio.iems.blocks.IEMSBlocks.ENERGY_BROADCAST_TOWER.get()).build(null));
    
    // ============ 实体 ============
    
    // 能量连接实体 (用于渲染激�?
    public static final Supplier<EntityType<EnergyConnectionEntity>> ENERGY_CONNECTION_ENTITY = 
        ENTITIES.register("energy_connection", () -> 
            EntityType.Builder.<EnergyConnectionEntity>of(EnergyConnectionEntity::new, MobCategory.MISC)
                .sized(0.1f, 0.1f)
                .clientTrackingRange(256)
                .updateInterval(1)
                .setShouldReceiveVelocityUpdates(false)
                .build("energy_connection"));
    
    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
        ENTITIES.register(modEventBus);
        IEMSMod.LOGGER.info("注册 IEMS 实体");
    }
}
