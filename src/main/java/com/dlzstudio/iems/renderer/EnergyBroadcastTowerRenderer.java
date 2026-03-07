package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 广播塔 GeoLib 渲染器
 */
public class EnergyBroadcastTowerRenderer extends GeoBlockRenderer<EnergyBroadcastTowerBlockEntity> {

    public EnergyBroadcastTowerRenderer(BlockEntityRendererProvider.Context context) {
        super(new EnergyBroadcastTowerModel());
    }
}
