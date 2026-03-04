package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import com.dlzstudio.iems.blocks.entity.EnergyRelayBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import software.bernie.geckolib.renderers.GeoBlockRenderer;

/**
 * 渲染器注册类
 */
@EventBusSubscriber(modid = IEMSMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class IEMSRenderer {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册 GeoLib 渲染器
            GeoBlockRenderer.registerBlockEntityRenderer(IEMSMod.MODID, 
                EnergyBroadcastTowerBlockEntity::new, 
                new EnergyBroadcastTowerRenderer());
            
            GeoBlockRenderer.registerBlockEntityRenderer(IEMSMod.MODID, 
                EnergyRelayBlockEntity::new, 
                new EnergyRelayRenderer());
            
            IEMSMod.LOGGER.info("注册 IEMS GeoLib 渲染器");
        });
    }
}
