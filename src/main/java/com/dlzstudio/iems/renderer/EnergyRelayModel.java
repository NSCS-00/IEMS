package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyRelayBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 中继器 GeoLib 模型
 */
public class EnergyRelayModel extends GeoModel<EnergyRelayBlockEntity> {
    
    @Override
    public ResourceLocation getModelResource(EnergyRelayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(IEMSMod.MODID, "geo/relay_tower.geo.json");
    }
    
    @Override
    public ResourceLocation getTextureResource(EnergyRelayBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(IEMSMod.MODID, "textures/block/relay_tower.png");
    }
    
    @Override
    public ResourceLocation getAnimationResource(EnergyRelayBlockEntity animatable) {
        return null; // 暂无动画
    }
}
