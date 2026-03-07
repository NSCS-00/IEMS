package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.*;
import com.dlzstudio.iems.tabs.IEMSCreativeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class IEMSBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IEMSMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IEMSMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE, IEMSMod.MODID);

    // 方块注册
    public static final DeferredBlock<EnergyStorageBlock> STANDARD_ENERGY_STORAGE = registerBlock(
        "standard_energy_storage",
        () -> new EnergyStorageBlock(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion(), EnergyStorageBlock.StorageType.STANDARD),
        "标准能量存储器"
    );

    public static final DeferredBlock<EnergyStorageBlock> GENERAL_ENERGY_STORAGE = registerBlock(
        "general_energy_storage",
        () -> new EnergyStorageBlock(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion(), EnergyStorageBlock.StorageType.GENERAL),
        "通用能量存储器"
    );

    public static final DeferredBlock<EnergyConverterBlock> ENERGY_CONVERTER = registerBlock(
        "energy_converter",
        () -> new EnergyConverterBlock(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion()),
        "能量转换器"
    );

    public static final DeferredBlock<EnergyRelayBlock> ENERGY_RELAY = registerBlock(
        "energy_relay",
        () -> new EnergyRelayBlock(Block.Properties.of().strength(2.0f, 5.0f).noOcclusion()),
        "能源中继传输器"
    );

    public static final DeferredBlock<EnergyBroadcastTowerBlock> ENERGY_BROADCAST_TOWER = registerBlock(
        "energy_broadcast_tower",
        () -> new EnergyBroadcastTowerBlock(Block.Properties.of().strength(2.0f, 5.0f).noOcclusion()),
        "能源广播塔"
    );

    // 标记方块（不可见，用于定义机器边界）
    public static final DeferredBlock<EnergyMarkerBlock> ENERGY_MARKER = BLOCKS.register(
        "energy_marker",
        () -> new EnergyMarkerBlock(Block.Properties.of().noCollission().noOcclusion().instabreak())
    );

    // 方块实体注册
    public static final Supplier<BlockEntityType<EnergyStorageBlockEntity>> ENERGY_STORAGE_ENTITY = BLOCK_ENTITIES.register(
        "energy_storage_entity",
        () -> BlockEntityType.Builder.of((pos, state) -> new EnergyStorageBlockEntity(pos, state), STANDARD_ENERGY_STORAGE.get(), GENERAL_ENERGY_STORAGE.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyConverterBlockEntity>> ENERGY_CONVERTER_ENTITY = BLOCK_ENTITIES.register(
        "energy_converter_entity",
        () -> BlockEntityType.Builder.of((pos, state) -> new EnergyConverterBlockEntity(pos, state), ENERGY_CONVERTER.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyRelayBlockEntity>> ENERGY_RELAY_ENTITY = BLOCK_ENTITIES.register(
        "energy_relay_entity",
        () -> BlockEntityType.Builder.of((pos, state) -> new EnergyRelayBlockEntity(pos, state), ENERGY_RELAY.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyBroadcastTowerBlockEntity>> ENERGY_BROADCAST_TOWER_ENTITY = BLOCK_ENTITIES.register(
        "energy_broadcast_tower_entity",
        () -> BlockEntityType.Builder.of((pos, state) -> new EnergyBroadcastTowerBlockEntity(pos, state), ENERGY_BROADCAST_TOWER.get()).build(null)
    );

    public static final Supplier<BlockEntityType<EnergyMarkerBlockEntity>> ENERGY_MARKER_ENTITY = BLOCK_ENTITIES.register(
        "energy_marker_entity",
        () -> BlockEntityType.Builder.of((pos, state) -> new EnergyMarkerBlockEntity(pos, state)).build(null)
    );

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier, String chineseName) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        if (!name.equals("energy_marker")) {
            ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        }
        return block;
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        IEMSCreativeTabs.register(modEventBus);
    }
}
