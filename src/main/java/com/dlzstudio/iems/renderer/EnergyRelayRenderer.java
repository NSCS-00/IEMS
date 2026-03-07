package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.blocks.entity.EnergyRelayBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 中继器 GeoLib 渲染器
 */
public class EnergyRelayRenderer extends GeoBlockRenderer<EnergyRelayBlockEntity> {

    public EnergyRelayRenderer(BlockEntityRendererProvider.Context context) {
        super(new EnergyRelayModel());
    }
}
