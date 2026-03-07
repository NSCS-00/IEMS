package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 广播塔 GeoLib 模型
 */
public class EnergyBroadcastTowerModel extends GeoModel<EnergyBroadcastTowerBlockEntity> {

    @Override
    public ResourceLocation getModelResource(EnergyBroadcastTowerBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(IEMSMod.MODID, "geo/electric_pylon.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EnergyBroadcastTowerBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(IEMSMod.MODID, "textures/block/electric_pylon.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EnergyBroadcastTowerBlockEntity animatable) {
        return null; // 暂无动画
    }
}
