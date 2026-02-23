package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.*;
import com.dlzstudio.iems.tabs.IEMSCreativeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 方块注册�? * 
 * 注意：本模组不提供核心方块，核心由其�?MOD 通过 GridCoreRegistry 注册
 */
public class IEMSBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IEMSMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IEMSMod.MODID);
    
    // ============ 方块定义 ============
    
    /**
     * 标准能量存储�?- 存储 10^5 SE
     */
    public static final DeferredBlock<Block> STANDARD_ENERGY_STORAGE = registerBlock(
        "standard_energy_storage",
        () -> new EnergyStorageBlock(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion(), 
            EnergyStorageBlock.StorageType.STANDARD),
        "标准能量存储�?
    );
    
    /**
     * 通用能量存储�?- 存储 10^20 GE
     */
    public static final DeferredBlock<Block> GENERAL_ENERGY_STORAGE = registerBlock(
        "general_energy_storage",
        () -> new EnergyStorageBlock(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion(),
            EnergyStorageBlock.StorageType.GENERAL),
        "通用能量存储�?
    );
    
    /**
     * 能量转换�?- 能源单位转换
     */
    public static final DeferredBlock<Block> ENERGY_CONVERTER = registerBlock(
        "energy_converter",
        () -> new EnergyConverterBlock(Block.Properties.of().strength(3.0f, 10.0f).noOcclusion()),
        "能量转换�?
    );
    
    /**
     * 能源中继传输�?- 用于传输能源
     */
    public static final DeferredBlock<Block> ENERGY_RELAY = registerBlock(
        "energy_relay",
        () -> new EnergyRelayBlock(Block.Properties.of().strength(2.0f, 5.0f).noOcclusion()),
        "能源中继传输�?
    );
    
    /**
     * 能源广播�?- 短距离无线传�?     */
    public static final DeferredBlock<Block> ENERGY_BROADCAST_TOWER = registerBlock(
        "energy_broadcast_tower",
        () -> new EnergyBroadcastTowerBlock(Block.Properties.of().strength(2.0f, 5.0f).noOcclusion()),
        "能源广播�?
    );
    
    // ============ 注册方法 ============
    
    private static <T extends Block> DeferredBlock<T> registerBlock(
        String name, 
        Supplier<T> blockSupplier,
        String chineseName
    ) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        registerBlockItem(name, block, chineseName);
        return block;
    }
    
    private static <T extends Block> void registerBlockItem(
        String name, 
        DeferredBlock<T> block,
        String chineseName
    ) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override
            public String getDescriptionKey() {
                return "block." + IEMSMod.MODID + "." + name;
            }
        });
    }
    
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        IEMSCreativeTabs.register(modEventBus);
        IEMSMod.LOGGER.info("注册 IEMS 方块");
    }
}
