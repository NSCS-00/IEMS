package com.dlzstudio.iems.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.entities.EnergyConnectionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * 能量连接实体渲染�? * 渲染从一个方块到另一个方块的激�? */
public class EnergyConnectionRenderer extends EntityRenderer<EnergyConnectionEntity> {
    
    private static final ResourceLocation TEXTURE = 
        new ResourceLocation(IEMSMod.MODID, "textures/entity/energy_connection.png");
    
    public EnergyConnectionRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
    
    @Override
    public void render(EnergyConnectionEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        
        // 获取起点和终�?        Vec3 start = entity.getStartVector();
        Vec3 end = entity.getEndVector();
        
        // 获取颜色
        int color = entity.getColor();
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        float alpha = entity.getAlpha();
        
        // 渲染激光束
        renderLaserBeam(poseStack, buffer, start, end, red, green, blue, alpha);
        
        // 渲染虚线�?(如果是连接模�?
        if (entity.isDepleted()) {
            renderDepletedIndicator(poseStack, buffer, start, end);
        }
    }
    
    /**
     * 渲染激光束
     */
    private void renderLaserBeam(PoseStack poseStack, MultiBufferSource buffer,
                                  Vec3 start, Vec3 end,
                                  float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        
        // 转换为世界坐�?        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        
        // 渲染主激光束 (黄色)
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        
        // 绘制多条线模拟激光效�?        float width = 0.05f;
        
        // 核心光束
        drawLine(matrix, consumer, 
            start.x, start.y, start.z,
            end.x, end.y, end.z,
            red, green, blue, alpha, width);
        
        // 外层光晕
        drawLine(matrix, consumer,
            start.x - width, start.y, start.z,
            end.x - width, end.y, end.z,
            red, green, blue, alpha * 0.5f, width * 0.5f);
        
        drawLine(matrix, consumer,
            start.x + width, start.y, start.z,
            end.x + width, end.y, end.z,
            red, green, blue, alpha * 0.5f, width * 0.5f);
        
        drawLine(matrix, consumer,
            start.x, start.y - width, start.z,
            end.x, end.y - width, end.z,
            red, green, blue, alpha * 0.5f, width * 0.5f);
        
        drawLine(matrix, consumer,
            start.x, start.y + width, start.z,
            end.x, end.y + width, end.z,
            red, green, blue, alpha * 0.5f, width * 0.5f);
        
        poseStack.popPose();
    }
    
    /**
     * 绘制单条�?     */
    private void drawLine(Matrix4f matrix, VertexConsumer consumer,
                          double x1, double y1, double z1,
                          double x2, double y2, double z2,
                          float red, float green, float blue, float alpha, float width) {
        // 使用四边形绘制线�?        consumer.vertex(matrix, (float)(x1 - width), (float)y1, (float)(z1 - width))
                .color(red, green, blue, alpha)
                .endVertex();
        consumer.vertex(matrix, (float)(x1 + width), (float)y1, (float)(z1 + width))
                .color(red, green, blue, alpha)
                .endVertex();
        consumer.vertex(matrix, (float)(x2 + width), (float)y2, (float)(z2 + width))
                .color(red, green, blue, alpha)
                .endVertex();
        consumer.vertex(matrix, (float)(x2 - width), (float)y2, (float)(z2 - width))
                .color(red, green, blue, alpha)
                .endVertex();
    }
    
    /**
     * 渲染耗尽指示�?(红色虚线)
     */
    private void renderDepletedIndicator(PoseStack poseStack, MultiBufferSource buffer,
                                          Vec3 start, Vec3 end) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();
        
        // 绘制红色虚线
        float red = 1.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 0.8f;
        
        consumer.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
                .color(red, green, blue, alpha)
                .endVertex();
        consumer.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
    
    @Override
    public ResourceLocation getTextureLocation(EnergyConnectionEntity entity) {
        return TEXTURE;
    }
}
