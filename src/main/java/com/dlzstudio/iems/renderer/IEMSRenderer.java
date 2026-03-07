package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 渲染器注册类
 */
@EventBusSubscriber(modid = IEMSMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class IEMSRenderer {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册广播塔渲染器
        event.registerBlockEntityRenderer(
            IEMSBlocks.ENERGY_BROADCAST_TOWER_ENTITY.get(),
            EnergyBroadcastTowerRenderer::new
        );

        // 注册中继器渲染器
        event.registerBlockEntityRenderer(
            IEMSBlocks.ENERGY_RELAY_ENTITY.get(),
            EnergyRelayRenderer::new
        );

        IEMSMod.LOGGER.info("注册 IEMS GeoLib 渲染器");
    }
}
