package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.EnergyRelayBlock;
import com.dlzstudio.iems.energy.EnergyGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 能源中继传输器方块实体
 * 
 * 作用：用于标记广播塔接入点，必须与核心连接才能工作
 * 连接规则:
 * - 中继器可以连接到核心或其他中继器
 * - 中继器可以连接到广播塔
 * - 广播塔必须通过中继器或直接与核心连接才能工作
 */
public class EnergyRelayBlockEntity extends BlockEntity {
    
    private boolean isInConnectionMode = false;
    private UUID connectionId;
    private BlockPos connectedPos;
    
    // 连接距离限制
    private static final int MAX_DISTANCE = EnergyRelayBlock.MAX_CONNECTION_DISTANCE;
    private static final int DISCONNECT_TOLERANCE = 20;
    
    public EnergyRelayBlockEntity(BlockPos pos, BlockState state) {
        super(IEMSEntities.ENERGY_RELAY_ENTITY.get(), pos, state);
    }
    
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (connectionId != null) {
            EnergyConnectionManager.getInstance().removeConnection(connectionId);
        }
        EnergyGrid.getInstance().unregisterRelay(worldPosition);
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        EnergyGrid.getInstance().unregisterRelay(worldPosition);
    }
    
    /**
     * 初始化中继器
     */
    public void initialize() {
        EnergyGrid.getInstance().registerRelay(worldPosition);
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
    public void tryCompleteConnection(Player player) {
        if (!isInConnectionMode || connectionId == null) {
            return;
        }
        
        BlockPos targetPos = findTargetBlock(player);
        if (targetPos == null) {
            player.displayClientMessage(Component.literal("未找到有效目标").withStyle(ChatFormatting.RED), true);
            return;
        }
        
        int distance = calculateDistance(worldPosition, targetPos);
        if (distance > MAX_DISTANCE) {
            player.displayClientMessage(Component.literal("距离过远！最大距离：" + MAX_DISTANCE).withStyle(ChatFormatting.RED), true);
            isInConnectionMode = false;
            EnergyConnectionManager.getInstance().removeConnection(connectionId);
            return;
        }
        
        BlockEntity targetEntity = level.getBlockEntity(targetPos);
        if (targetEntity instanceof EnergyRelayBlockEntity || 
            targetEntity instanceof EnergyBroadcastTowerBlockEntity ||
            targetEntity instanceof CoreBlockEntity) {
            
            // 创建连接
            EnergyConnectionManager.getInstance().completeConnection(connectionId, targetPos);
            EnergyGrid.getInstance().addConnection(worldPosition, targetPos);
            
            this.connectedPos = targetPos;
            
            player.displayClientMessage(Component.literal("连接成功！距离：" + distance + "/" + MAX_DISTANCE).withStyle(ChatFormatting.GREEN), true);
            
            isInConnectionMode = false;
            connectionId = null;
            setChanged();
        } else {
            player.displayClientMessage(Component.literal("目标不是有效的连接设备").withStyle(ChatFormatting.RED), true);
        }
    }
    
    /**
     * 查找目标方块
     */
    private BlockPos findTargetBlock(Player player) {
        var start = player.getEyePosition(1.0f);
        var look = player.getLookAngle();
        var end = start.add(look.scale(MAX_DISTANCE + DISCONNECT_TOLERANCE));
        
        var hitResult = level.clip(new net.minecraft.world.level.ClipContext(
            start, end,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            null
        ));
        
        if (hitResult != null) {
            return hitResult.getBlockPos();
        }
        return null;
    }
    
    private int calculateDistance(BlockPos pos1, BlockPos pos2) {
        return (int) Math.round(pos1.distSqr(pos2));
    }
    
    /**
     * Tick 更新
     */
    public void tick() {
        // 检查连接是否有效
        if (connectedPos != null && level != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                // 目标失效，断开连接
                EnergyGrid.getInstance().removeConnection(worldPosition, connectedPos);
                connectedPos = null;
                setChanged();
            }
        }
        
        // 检查是否连接到核心
        if (level != null && level.getGameTime() % 20 == 0) {
            updateConnectionStatus();
        }
    }
    
    /**
     * 更新连接状态显示
     */
    private void updateConnectionStatus() {
        boolean connected = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        if (!connected && !isInConnectionMode) {
            // 未连接到核心，显示警告
            // 可以通过粒子效果或其他方式提醒玩家
        }
    }
    
    /**
     * 获取连接状态显示
     */
    public Component getConnectionStatus() {
        if (isInConnectionMode) {
            return Component.literal("连接模式中...").withStyle(ChatFormatting.YELLOW);
        }
        if (connectedPos != null) {
            int distance = calculateDistance(worldPosition, connectedPos);
            boolean connectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
            String status = connectedToCore ? "§a已连接" : "§c未连接到核心";
            return Component.literal(status + " 距离：" + distance + "/" + MAX_DISTANCE);
        }
        return Component.literal("未连接").withStyle(ChatFormatting.GRAY);
    }
    
    public boolean isInConnectionMode() {
        return isInConnectionMode;
    }
    
    public UUID getConnectionId() {
        return connectionId;
    }
    
    public BlockPos getConnectedPos() {
        return connectedPos;
    }
    
    /**
     * 检查是否连接到核心
     */
    public boolean isConnectedToCore() {
        return EnergyGrid.getInstance().isConnectedToCore(worldPosition);
    }
}
