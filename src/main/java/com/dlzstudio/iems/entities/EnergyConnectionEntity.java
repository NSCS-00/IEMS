package com.dlzstudio.iems.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * 能源连接实体
 * 用于渲染从一个方块到另一个方块的激光
 */
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
        super(IEMSEntities.ENERGY_CONNECTION.get(), level);
        this.startPos = startPos;
        this.endPos = endPos;
        
        // 设置实体位置为起点
        this.setPos(startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5);
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
        return color;
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
     * 获取激光起点 (世界坐标)
     */
    public Vec3 getStartVector() {
        if (startPos == null) return null;
        return new Vec3(startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5);
    }
    
    /**
     * 获取激光终点 (世界坐标)
     */
    public Vec3 getEndVector() {
        if (endPos == null) return null;
        return new Vec3(endPos.getX() + 0.5, endPos.getY() + 0.5, endPos.getZ() + 0.5);
    }
    
    /**
     * 获取激光长度
     */
    public double getLength() {
        if (startPos == null || endPos == null) return 0;
        return startPos.distSqr(endPos);
    }
    
    @Override
    protected void defineSynchedData() {
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("StartPos")) {
            this.startPos = BlockPos.of(tag.getLong("StartPos"));
        }
        if (tag.contains("EndPos")) {
            this.endPos = BlockPos.of(tag.getLong("EndPos"));
        }
        if (tag.contains("Color")) {
            this.color = tag.getInt("Color");
        }
        if (tag.contains("Depleted")) {
            this.isDepleted = tag.getBoolean("Depleted");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (startPos != null) {
            tag.putLong("StartPos", startPos.asLong());
        }
        if (endPos != null) {
            tag.putLong("EndPos", endPos.asLong());
        }
        tag.putInt("Color", color);
        tag.putBoolean("Depleted", isDepleted);
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
    
    @Override
    public boolean isPickable() {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
}
