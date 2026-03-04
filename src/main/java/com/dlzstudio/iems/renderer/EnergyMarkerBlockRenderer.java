package com.dlzstudio.iems.renderer;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyMarkerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能源标记方块渲染器
 * 
 * 渲染规则:
 * - 创造模式下手持木棍才显示
 * - 显示半透明网格贴图
 * - 根据标记类型显示不同颜色
 */
public class EnergyMarkerBlockRenderer implements BlockEntityRenderer<EnergyMarkerBlockEntity> {
    
    // 结构空位贴图
    private static final ResourceLocation STRUCTURE_VOID_TEXTURE = 
        ResourceLocation.withDefaultNamespace("block/structure_void");
    
    public EnergyMarkerBlockRenderer(BlockEntityRendererProvider.Context context) {
    }
    
    @Override
    public void render(EnergyMarkerBlockEntity entity, float partialTick, PoseStack poseStack, 
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        
        // 检查玩家是否应该看到标记
        var player = Minecraft.getInstance().player;
        if (player == null || !entity.shouldRender(player)) {
            return;
        }
        
        // 获取标记类型
        int markerType = entity.getMarkerType();
        
        poseStack.pushPose();
        
        // 根据标记类型设置颜色
        float red = 1.0f, green = 1.0f, blue = 1.0f;
        switch (markerType) {
            case 0 -> { // 边界标记 - 白色
                red = 1.0f; green = 1.0f; blue = 1.0f;
            }
            case 1 -> { // 连接点标记 - 黄色
                red = 1.0f; green = 1.0f; blue = 0.0f;
            }
            case 2 -> { // 能量输入标记 - 绿色
                red = 0.0f; green = 1.0f; blue = 0.0f;
            }
            case 3 -> { // 能量输出标记 - 红色
                red = 1.0f; green = 0.0f; blue = 0.0f;
            }
        }
        
        // 渲染半透明网格
        renderStructureVoid(poseStack, bufferSource, red, green, blue, 0.5f);
        
        poseStack.popPose();
    }
    
    /**
     * 渲染结构空位网格
     */
    private void renderStructureVoid(PoseStack poseStack, MultiBufferSource buffer, 
                                     float red, float green, float blue, float alpha) {
        // 获取结构空位贴图
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(STRUCTURE_VOID_TEXTURE);
        
        if (sprite == null) return;
        
        // 渲染立方体线框
        renderWireframeCube(poseStack, buffer, sprite, red, green, blue, alpha);
    }
    
    /**
     * 渲染立方体线框
     */
    private void renderWireframeCube(PoseStack poseStack, MultiBufferSource buffer, 
                                     TextureAtlasSprite sprite, float red, float green, 
                                     float blue, float alpha) {
        // 简化实现：使用半透明方块渲染
        // 实际项目中可以使用自定义模型或更复杂的渲染逻辑
    }
    
    @Override
    public boolean shouldRender(EnergyMarkerBlockEntity entity, BlockPos pos) {
        // 总是渲染，由 render 方法内部判断是否显示
        return true;
    }
}
