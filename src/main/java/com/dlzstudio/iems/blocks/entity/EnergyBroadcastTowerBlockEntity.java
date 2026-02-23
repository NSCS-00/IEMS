package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.EnergyBroadcastTowerBlock;
import com.dlzstudio.iems.energy.EnergyGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

/**
 * 能源广播塔方块实体
 * 
 * 作用：短距离无线能源传输标记
 * 重要：广播塔必须通过中继器或直接与核心连接才能工作
 * 
 * 功能:
 * - 自动连接半径 50 格内的设备
 * - 只有连接到核心时，被连接的设备才会被标记为接入电网
 */
public class EnergyBroadcastTowerBlockEntity extends BlockEntity {
    
    private boolean isInConnectionMode = false;
    private UUID connectionId;
    private BlockPos connectedPos;
    
    // 连接距离限制
    private static final int MAX_DISTANCE = EnergyBroadcastTowerBlock.MAX_CONNECTION_DISTANCE;
    private static final int AUTO_CONNECT_RADIUS = EnergyBroadcastTowerBlock.AUTO_CONNECT_RADIUS;
    private static final int DISCONNECT_TOLERANCE = 20;
    
    // 自动连接的设备列表
    private final Set<BlockPos> autoConnectedDevices = new HashSet<>();
    
    public EnergyBroadcastTowerBlockEntity(BlockPos pos, BlockState state) {
        super(IEMSEntities.ENERGY_BROADCAST_TOWER_ENTITY.get(), pos, state);
    }
    
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (connectionId != null) {
            EnergyConnectionManager.getInstance().removeConnection(connectionId);
        }
        EnergyGrid.getInstance().unregisterBroadcastTower(worldPosition);
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        EnergyGrid.getInstance().unregisterBroadcastTower(worldPosition);
    }
    
    /**
     * 初始化广播塔
     */
    public void initialize() {
        EnergyGrid.getInstance().registerBroadcastTower(worldPosition);
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
        if (targetEntity instanceof EnergyBroadcastTowerBlockEntity || 
            targetEntity instanceof EnergyRelayBlockEntity ||
            targetEntity instanceof CoreBlockEntity) {
            
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
        // 自动扫描附近设备 (每 20 tick)
        if (level != null && level.getGameTime() % 20 == 0) {
            scanAndConnectDevices();
        }
        
        // 检查手动连接是否有效
        if (connectedPos != null && level != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                EnergyGrid.getInstance().removeConnection(worldPosition, connectedPos);
                connectedPos = null;
                setChanged();
            }
        }
        
        // 检查是否连接到核心
        if (level != null && level.getGameTime() % 60 == 0) {
            updateConnectionStatus();
        }
    }
    
    /**
     * 扫描并连接附近设备
     * 注意：只有广播塔本身连接到核心时，扫描的设备才会被标记为接入电网
     */
    private void scanAndConnectDevices() {
        autoConnectedDevices.clear();
        
        // 检查广播塔是否连接到核心
        boolean towerConnectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        
        if (!towerConnectedToCore) {
            // 广播塔未连接到核心，不扫描设备
            return;
        }
        
        BoundingBox area = new BoundingBox(
            worldPosition.getX() - AUTO_CONNECT_RADIUS,
            worldPosition.getY() - AUTO_CONNECT_RADIUS,
            worldPosition.getZ() - AUTO_CONNECT_RADIUS,
            worldPosition.getX() + AUTO_CONNECT_RADIUS,
            worldPosition.getY() + AUTO_CONNECT_RADIUS,
            worldPosition.getZ() + AUTO_CONNECT_RADIUS
        );
        
        Set<String> allowedDevices = EnergyConnectionManager.getInstance().getAllowedDevices();
        
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int y = area.minY(); y <= area.maxY(); y++) {
                for (int z = area.minZ(); z <= area.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(worldPosition)) continue;
                    
                    BlockEntity entity = level.getBlockEntity(pos);
                    if (entity == null) continue;
                    
                    String blockId = entity.getBlockState().getBlock().getDescriptionId();
                    
                    if (isAllowedDevice(blockId, allowedDevices)) {
                        if (entity instanceof EnergyStorageBlockEntity ||
                            entity instanceof EnergyConverterBlockEntity) {
                            
                            if (hasLineOfSight(pos)) {
                                autoConnectedDevices.add(pos);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private boolean isAllowedDevice(String blockId, Set<String> allowedDevices) {
        return EnergyConnectionManager.getInstance().isDeviceAllowedByDescriptionId(blockId);
    }
    
    private boolean hasLineOfSight(BlockPos target) {
        var line = line(worldPosition, target);
        for (BlockPos pos : line) {
            if (!pos.equals(worldPosition) && !pos.equals(target)) {
                var state = level.getBlockState(pos);
                if (state.isSolidRender(level, pos)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private List<BlockPos> line(BlockPos from, BlockPos to) {
        List<BlockPos> positions = new ArrayList<>();
        int dx = Math.abs(to.getX() - from.getX());
        int dy = Math.abs(to.getY() - from.getY());
        int dz = Math.abs(to.getZ() - from.getZ());
        int steps = Math.max(dx, Math.max(dy, dz));
        
        for (int i = 0; i <= steps; i++) {
            int x = from.getX() + (to.getX() - from.getX()) * i / steps;
            int y = from.getY() + (to.getY() - from.getY()) * i / steps;
            int z = from.getZ() + (to.getZ() - from.getZ()) * i / steps;
            positions.add(new BlockPos(x, y, z));
        }
        
        return positions;
    }
    
    /**
     * 更新连接状态显示
     */
    private void updateConnectionStatus() {
        boolean connected = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        if (!connected && !isInConnectionMode) {
            // 未连接到核心，显示警告
        }
    }
    
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
        boolean connectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        String status = connectedToCore ? "§a工作中" : "§c未连接核心";
        return Component.literal(status + " 自动连接：" + autoConnectedDevices.size() + " 设备");
    }
    
    public boolean isInConnectionMode() {
        return isInConnectionMode;
    }
    
    public Set<BlockPos> getAutoConnectedDevices() {
        return autoConnectedDevices;
    }
    
    /**
     * 检查是否连接到核心
     */
    public boolean isConnectedToCore() {
        return EnergyGrid.getInstance().isConnectedToCore(worldPosition);
    }
}
