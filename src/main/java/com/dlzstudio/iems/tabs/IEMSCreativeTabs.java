package com.dlzstudio.iems.tabs;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 创造模式标签页注册
 */
public class IEMSCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IEMSMod.MODID);
    
    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
        "main",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(IEMSBlocks.CORE_BLOCK.get()))
            .title(Component.literal("综合能源管理系统"))
            .displayItems((params, output) -> {
                // 添加所有方�?                output.accept(IEMSBlocks.CORE_BLOCK.get());
                output.accept(IEMSBlocks.STANDARD_ENERGY_STORAGE.get());
                output.accept(IEMSBlocks.GENERAL_ENERGY_STORAGE.get());
                output.accept(IEMSBlocks.ENERGY_CONVERTER.get());
                output.accept(IEMSBlocks.ENERGY_RELAY.get());
                output.accept(IEMSBlocks.ENERGY_BROADCAST_TOWER.get());
            })
            .build()
    );
    
    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
