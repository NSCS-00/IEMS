package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.entities.EnergyConnectionEntity;
import com.dlzstudio.iems.entities.IEMSEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.renderer.entity.EntityRenderers;

/**
 * 渲染器注�? */
@EventBusSubscriber(modid = IEMSMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class IEMSRenderer {
    
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册实体渲染�?            EntityRenderers.register(IEMSEntities.ENERGY_CONNECTION_ENTITY.get(), 
                EnergyConnectionRenderer::new);
            
            IEMSMod.LOGGER.info("注册 IEMS 渲染�?);
        });
    }
}
