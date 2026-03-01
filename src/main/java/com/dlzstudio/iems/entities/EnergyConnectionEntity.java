package com.dlzstudio.iems.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * 能量连接实体
 * 用于渲染从一个方块到另一个方块的激�? */
public class EnergyConnectionEntity extends Entity {
    
    private BlockPos startPos;
    private BlockPos endPos;
    private int color = 0xFFFF00; // 默认黄色
    private float alpha = 1.0f;
    private boolean isDepleted = false;
    
    public EnergyConnectionEntity(EntityType<?> type, Level level) {
        super(type, level);
    }
    
    public EnergyConnectionEntity(Level level, BlockPos startPos, BlockPos endPos) {
        super(IEMSEntities.ENERGY_CONNECTION_ENTITY.get(), level);
        this.startPos = startPos;
        this.endPos = endPos;
        this.setColor(0xFFFF00);
        
        // 设置实体位置
        double midX = (startPos.getX() + endPos.getX()) / 2.0;
        double midY = (startPos.getY() + endPos.getY()) / 2.0;
        double midZ = (startPos.getZ() + endPos.getZ()) / 2.0;
        this.setPos(midX + 0.5, midY + 0.5, midZ + 0.5);
    }
    
    public void setStartPos(BlockPos pos) {
        this.startPos = pos;
    }
    
    public void setEndPos(BlockPos pos) {
        this.endPos = pos;
    }
    
    public BlockPos getStartPos() {
        return startPos;
    }
    
    public BlockPos getEndPos() {
        return endPos;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public int getColor() {
        return isDepleted ? 0xFF0000 : color; // 耗尽时为红色
    }
    
    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public void setDepleted(boolean depleted) {
        isDepleted = depleted;
    }
    
    public boolean isDepleted() {
        return isDepleted;
    }
    
    /**
     * 获取激光起�?(世界坐标)
     */
    public Vec3 getStartVector() {
        return new Vec3(startPos.getX() + 0.5, startPos.getY() + 1, startPos.getZ() + 0.5);
    }
    
    /**
     * 获取激光终�?(世界坐标)
     */
    public Vec3 getEndVector() {
        return new Vec3(endPos.getX() + 0.5, endPos.getY() + 1, endPos.getZ() + 0.5);
    }
    
    /**
     * 获取激光长�?     */
    public double getLength() {
        if (startPos == null || endPos == null) return 0;
        return startPos.distSqr(endPos);
    }
    
    @Override
    protected void defineSynchedData() {
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }
}
