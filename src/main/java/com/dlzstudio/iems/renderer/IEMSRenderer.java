package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import com.dlzstudio.iems.blocks.entity.EnergyMarkerBlockEntity;
import com.dlzstudio.iems.blocks.entity.EnergyRelayBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 渲染器注册类
 */
@EventBusSubscriber(modid = IEMSMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class IEMSRenderer {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            IEMSMod.LOGGER.info("注册 IEMS 客户端渲染器");
        });
    }
    
    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册标记方块渲染器
        event.registerBlockEntityRenderer(
            com.dlzstudio.iems.blocks.IEMSBlocks.ENERGY_MARKER_ENTITY.get(),
            EnergyMarkerBlockRenderer::new
        );
        
        IEMSMod.LOGGER.info("注册 IEMS 标记方块渲染器");
    }
}
