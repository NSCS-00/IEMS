package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.EnergyRelayBlock;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class EnergyRelayBlockEntity extends BlockEntity {
    
    private boolean isInConnectionMode = false;
    private UUID connectionId;
    private BlockPos connectedPos;
    
    private static final int MAX_DISTANCE = EnergyRelayBlock.MAX_CONNECTION_DISTANCE;
    private static final int DISCONNECT_TOLERANCE = 20; // 断开容差
    
    public EnergyRelayBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().registerRelay(worldPosition);
        }
    }
    
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().unregisterRelay(worldPosition);
            EnergyConnectionManager.getInstance().removeConnection(worldPosition);
        }
        super.setRemoved();
    }
    
    /**
     * 切换连接模式
     */
    public void toggleConnectionMode(Player player) {
        if (isInConnectionMode) {
            // 取消连接模式
            isInConnectionMode = false;
            connectionId = null;
            player.displayClientMessage(Component.literal("已取消连接模式").withStyle(ChatFormatting.RED), true);
        } else {
            // 开始连接模式
            isInConnectionMode = true;
            connectionId = UUID.randomUUID();
            EnergyConnectionManager.getInstance().startConnection(connectionId, worldPosition);
            player.displayClientMessage(Component.literal("连接模式开启，对准目标右键").withStyle(ChatFormatting.GREEN), true);
        }
        setChanged();
    }
    
    /**
     * 尝试完成连接（由方块调用）
     */
    public void tryCompleteConnection(Player player, BlockPos targetPos) {
        if (!isInConnectionMode || connectionId == null) return;
        
        // 检查距离
        int distance = (int) Math.round(worldPosition.distSqr(targetPos));
        int maxDistanceSq = MAX_DISTANCE * MAX_DISTANCE;
        
        if (distance > maxDistanceSq) {
            player.displayClientMessage(Component.literal("距离过远！最大距离：" + MAX_DISTANCE + " 格").withStyle(ChatFormatting.RED), true);
            isInConnectionMode = false;
            connectionId = null;
            return;
        }
        
        // 检查目标方块实体
        if (level == null) return;
        BlockEntity targetEntity = level.getBlockEntity(targetPos);
        
        boolean isValidTarget = targetEntity instanceof EnergyRelayBlockEntity || 
                               targetEntity instanceof EnergyBroadcastTowerBlockEntity;
        
        if (!isValidTarget) {
            player.displayClientMessage(Component.literal("目标不是有效的连接设备").withStyle(ChatFormatting.RED), true);
            return;
        }
        
        // 检查视线（仅中继器之间需要）
        if (targetEntity instanceof EnergyRelayBlockEntity) {
            if (!hasClearSight(targetPos)) {
                player.displayClientMessage(Component.literal("目标被完全遮挡！需要≥3 层完整方块").withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        
        // 完成连接
        EnergyConnectionManager.getInstance().completeConnection(connectionId, targetPos);
        EnergyGrid.getInstance().addConnection(worldPosition, targetPos);
        
        this.connectedPos = targetPos;
        
        player.displayClientMessage(Component.literal("连接成功！距离：" + 
            Math.round(Math.sqrt(distance)) + "/" + MAX_DISTANCE).withStyle(ChatFormatting.GREEN), true);
        
        isInConnectionMode = false;
        connectionId = null;
        setChanged();
    }
    
    /**
     * 查找目标方块（由方块 use 方法调用）
     */
    public BlockPos findTargetBlock(Player player) {
        if (level == null) return null;
        
        Vec3 start = player.getEyePosition(1.0f);
        Vec3 look = player.getLookAngle();
        int range = MAX_DISTANCE + DISCONNECT_TOLERANCE;
        Vec3 end = start.add(look.scale(range));
        
        var hitResult = level.clip(new net.minecraft.world.level.ClipContext(
            start, end,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player
        ));
        
        return hitResult != null ? hitResult.getBlockPos() : null;
    }
    
    /**
     * 检查是否有清晰的视线
     */
    private boolean hasClearSight(BlockPos target) {
        if (level == null) return false;
        return com.dlzstudio.iems.util.SightCheckUtil.hasLineOfSight(level, worldPosition, target);
    }
    
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        // 检查连接是否仍然有效
        if (connectedPos != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                EnergyGrid.getInstance().removeConnection(worldPosition, connectedPos);
                EnergyConnectionManager.getInstance().removeConnection(worldPosition);
                connectedPos = null;
                setChanged();
            }
        }
    }
    
    public boolean isInConnectionMode() { return isInConnectionMode; }
    
    public boolean isConnectedToCore() {
        return EnergyGrid.getInstance().isConnectedToCore(worldPosition) ||
               EnergyConnectionManager.getInstance().isConnectedToCore(level, worldPosition);
    }
    
    public BlockPos getConnectedPos() { return connectedPos; }
}
