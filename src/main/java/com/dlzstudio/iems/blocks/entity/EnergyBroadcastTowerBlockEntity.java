package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.EnergyBroadcastTowerBlock;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.util.SightCheckUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EnergyBroadcastTowerBlockEntity extends BlockEntity {
    
    private boolean isInConnectionMode = false;
    private UUID connectionId;
    private BlockPos connectedPos;
    
    private static final int MAX_DISTANCE = EnergyBroadcastTowerBlock.MAX_CONNECTION_DISTANCE;
    private static final int AUTO_CONNECT_RADIUS = EnergyBroadcastTowerBlock.AUTO_CONNECT_RADIUS;
    private static final int DISCONNECT_TOLERANCE = 20;
    
    // 自动连接的设备列表
    private final Set<BlockPos> autoConnectedDevices = new HashSet<>();
    
    // 信号源位置（本体向上 3 格）
    private BlockPos signalSourcePos;
    
    public EnergyBroadcastTowerBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
        updateSignalSourcePos();
    }
    
    private void updateSignalSourcePos() {
        if (worldPosition != null) {
            this.signalSourcePos = worldPosition.above(3);
        }
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        updateSignalSourcePos();
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().registerBroadcastTower(worldPosition);
        }
    }
    
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().unregisterBroadcastTower(worldPosition);
            EnergyConnectionManager.getInstance().removeConnection(worldPosition);
        }
        super.setRemoved();
    }
    
    /**
     * 获取信号源位置
     */
    public BlockPos getSignalSourcePos() {
        if (signalSourcePos == null) {
            updateSignalSourcePos();
        }
        return signalSourcePos;
    }
    
    /**
     * 切换连接模式
     */
    public void toggleConnectionMode(Player player) {
        if (isInConnectionMode) {
            isInConnectionMode = false;
            connectionId = null;
            player.displayClientMessage(Component.literal("已取消连接模式").withStyle(ChatFormatting.RED), true);
        } else {
            isInConnectionMode = true;
            connectionId = UUID.randomUUID();
            EnergyConnectionManager.getInstance().startConnection(connectionId, worldPosition);
            player.displayClientMessage(Component.literal("连接模式开启，对准目标右键").withStyle(ChatFormatting.GREEN), true);
        }
        setChanged();
    }
    
    /**
     * 尝试完成连接
     */
    public void tryCompleteConnection(Player player, BlockPos targetPos) {
        if (!isInConnectionMode || connectionId == null) return;
        
        // 检查距离（从信号源开始计算）
        BlockPos sourcePos = getSignalSourcePos();
        int distance = (int) Math.round(sourcePos.distSqr(targetPos));
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
        
        // 广播塔可以连接到存储器、转换器、中继器、其他广播塔
        boolean isValidTarget = targetEntity instanceof EnergyStorageBlockEntity ||
                               targetEntity instanceof EnergyConverterBlockEntity ||
                               targetEntity instanceof EnergyRelayBlockEntity;
        
        if (!isValidTarget) {
            player.displayClientMessage(Component.literal("目标不是有效的连接设备").withStyle(ChatFormatting.RED), true);
            return;
        }
        
        // 检查视线（从信号源到目标）
        if (!hasClearSightFromSource(targetPos)) {
            player.displayClientMessage(Component.literal("目标被完全遮挡！需要≥3 层完整方块").withStyle(ChatFormatting.RED), true);
            return;
        }
        
        // 完成连接
        EnergyConnectionManager.getInstance().completeConnection(connectionId, targetPos);
        EnergyGrid.getInstance().addConnection(worldPosition, targetPos);
        
        this.connectedPos = targetPos;
        autoConnectedDevices.add(targetPos);
        
        player.displayClientMessage(Component.literal("连接成功！距离：" + 
            Math.round(Math.sqrt(distance)) + "/" + MAX_DISTANCE).withStyle(ChatFormatting.GREEN), true);
        
        isInConnectionMode = false;
        connectionId = null;
        setChanged();
    }
    
    /**
     * 查找目标方块
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
     * 从信号源检查视线
     */
    private boolean hasClearSightFromSource(BlockPos target) {
        if (level == null) return false;
        BlockPos sourcePos = getSignalSourcePos();
        return SightCheckUtil.hasLineOfSight(level, sourcePos, target);
    }
    
    /**
     * 自动扫描并连接范围内的设备
     */
    public void scanAndConnect() {
        if (level == null || level.isClientSide) return;
        
        BlockPos sourcePos = getSignalSourcePos();
        int radius = AUTO_CONNECT_RADIUS;
        
        // 扫描半球区域（朝下）
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= 0; y++) { // 只扫描下方
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = sourcePos.offset(x, y, z);
                    
                    // 检查距离
                    if (sourcePos.distSqr(checkPos) > radius * radius) continue;
                    
                    // 跳过自身
                    if (checkPos.equals(worldPosition)) continue;
                    
                    // 检查是否已连接
                    if (autoConnectedDevices.contains(checkPos)) continue;
                    
                    // 检查方块实体
                    BlockEntity targetEntity = level.getBlockEntity(checkPos);
                    
                    if (targetEntity instanceof EnergyStorageBlockEntity ||
                        targetEntity instanceof EnergyConverterBlockEntity) {
                        
                        // 检查视线
                        if (SightCheckUtil.hasLineOfSight(level, sourcePos, checkPos)) {
                            autoConnectedDevices.add(checkPos);
                            EnergyGrid.getInstance().addConnection(worldPosition, checkPos);
                        }
                    }
                }
            }
        }
    }
    
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        // 每 20 tick 扫描一次
        if (level.getGameTime() % 20 == 0) {
            scanAndConnect();
        }
        
        // 检查连接是否仍然有效
        if (connectedPos != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                EnergyGrid.getInstance().removeConnection(worldPosition, connectedPos);
                autoConnectedDevices.remove(connectedPos);
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
    
    public Set<BlockPos> getAutoConnectedDevices() {
        return new HashSet<>(autoConnectedDevices);
    }
}
