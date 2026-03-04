package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 广播塔 GeoLib 渲染器
 */
public class EnergyBroadcastTowerRenderer extends GeoBlockRenderer<EnergyBroadcastTowerBlockEntity> {
    
    public EnergyBroadcastTowerRenderer() {
        super(new EnergyBroadcastTowerModel());
    }
}
