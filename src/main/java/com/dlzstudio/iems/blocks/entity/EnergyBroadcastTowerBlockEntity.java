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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 能源广播塔方块实体 (v0.5.2 修复版)
 * 
 * 修复内容:
 * - 修复视线检查只累加填充度不考虑空间位置的 BUG
 * - 使用射线追踪采样检查实际遮挡位置
 * - 正确识别上半砖/下半砖的遮挡情况
 */
public class EnergyBroadcastTowerBlockEntity extends BlockEntity {
    
    private boolean isInConnectionMode = false;
    private UUID connectionId;
    private BlockPos connectedPos;
    
    private static final int MAX_DISTANCE = EnergyBroadcastTowerBlock.MAX_CONNECTION_DISTANCE;
    private static final int AUTO_CONNECT_RADIUS = EnergyBroadcastTowerBlock.AUTO_CONNECT_RADIUS;
    private static final int DISCONNECT_TOLERANCE = 20;
    
    // 自动连接的设备列表
    private final Set<BlockPos> autoConnectedDevices = new HashSet<>();
    
    // 填充度缓存
    private static final Map<BlockPos, Double> FILL_CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 1000;
    
    // 上次扫描的设备列表
    private final Set<BlockPos> lastScanDevices = new HashSet<>();
    
    // 射线采样点 (相对于方块中心的偏移，0.0-1.0)
    private static final Vec3[] RAY_SAMPLES = {
        new Vec3(0.5, 0.5, 0.5),      // 中心
        new Vec3(0.25, 0.25, 0.25),   // 角落
        new Vec3(0.75, 0.25, 0.25),
        new Vec3(0.25, 0.25, 0.75),
        new Vec3(0.75, 0.25, 0.75),
        new Vec3(0.25, 0.75, 0.25),
        new Vec3(0.75, 0.75, 0.25),
        new Vec3(0.25, 0.75, 0.75),
        new Vec3(0.75, 0.75, 0.75),
    };
    
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
    
    public void initialize() {
        EnergyGrid.getInstance().registerBroadcastTower(worldPosition);
    }
    
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
    
    public void tryCompleteConnection(Player player) {
        if (!isInConnectionMode || connectionId == null) return;
        
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
        
        return hitResult != null ? hitResult.getBlockPos() : null;
    }
    
    private int calculateDistance(BlockPos pos1, BlockPos pos2) {
        return (int) Math.round(pos1.distSqr(pos2));
    }
    
    public void tick() {
        if (level != null && level.getGameTime() % 40 == 0) {
            scanAndConnectDevicesOptimized();
        }
        
        if (connectedPos != null && level != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                EnergyGrid.getInstance().removeConnection(worldPosition, connectedPos);
                connectedPos = null;
                setChanged();
            }
        }
        
        if (level != null && level.getGameTime() % 60 == 0) {
            updateConnectionStatus();
        }
    }
    
    private void scanAndConnectDevicesOptimized() {
        boolean towerConnectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        
        if (!towerConnectedToCore) {
            autoConnectedDevices.clear();
            lastScanDevices.clear();
            return;
        }
        
        BlockPos signalSource = getSignalSourcePos();
        Set<BlockPos> newDevices = new HashSet<>();
        Set<String> allowedDevices = EnergyConnectionManager.getInstance().getAllowedDevices();
        
        scanSphere(signalSource, AUTO_CONNECT_RADIUS, (pos) -> {
            if (pos.equals(worldPosition) || pos.equals(signalSource)) return;
            
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) return;
            
            if (entity instanceof EnergyBroadcastTowerBlockEntity ||
                entity instanceof EnergyRelayBlockEntity) return;
            
            String blockId = entity.getBlockState().getBlock().getDescriptionId();
            if (!isAllowedDevice(blockId, allowedDevices)) return;
            
            if (!(entity instanceof EnergyStorageBlockEntity ||
                  entity instanceof EnergyConverterBlockEntity)) return;
            
            if (lastScanDevices.contains(pos)) {
                if (cachedHasLineOfSight(signalSource, entity)) {
                    newDevices.add(pos);
                } else {
                    autoConnectedDevices.remove(pos);
                }
                return;
            }
            
            if (hasLineOfSightRayTrace(signalSource, entity)) {
                newDevices.add(pos);
            }
        });
        
        autoConnectedDevices.retainAll(newDevices);
        autoConnectedDevices.addAll(newDevices);
        lastScanDevices.clear();
        lastScanDevices.addAll(newDevices);
    }
    
    private void scanSphere(BlockPos center, int radius, BlockPosConsumer consumer) {
        int r2 = radius * radius;
        
        for (int y = center.getY() - radius; y <= center.getY(); y++) {
            int yDist = center.getY() - y;
            int maxRadius2D = (int) Math.sqrt(r2 - yDist * yDist);
            
            for (int x = center.getX() - maxRadius2D; x <= center.getX() + maxRadius2D; x++) {
                int xDist = x - center.getX();
                int maxZDist = (int) Math.sqrt(maxRadius2D * maxRadius2D - xDist * xDist);
                
                for (int z = center.getZ() - maxZDist; z <= center.getZ() + maxZDist; z++) {
                    consumer.accept(new BlockPos(x, y, z));
                }
            }
        }
    }
    
    @FunctionalInterface
    private interface BlockPosConsumer {
        void accept(BlockPos pos);
    }
    
    private boolean isAllowedDevice(String blockId, Set<String> allowedDevices) {
        return EnergyConnectionManager.getInstance().isDeviceAllowedByDescriptionId(blockId);
    }
    
    /**
     * 视线检查 (射线追踪版 - v0.5.2 修复)
     * 使用多条射线采样，只要有一条射线能到达设备的任意一个采样点即可通电
     */
    private boolean hasLineOfSightRayTrace(BlockPos signalSource, BlockEntity device) {
        List<BlockPos> occupiedBlocks = getDeviceOccupiedBlocks(device);
        
        // 获取设备所有占用方块的采样点 (世界坐标)
        List<Vec3> targetPoints = new ArrayList<>();
        for (BlockPos occupiedPos : occupiedBlocks) {
            for (Vec3 sample : RAY_SAMPLES) {
                targetPoints.add(new Vec3(
                    occupiedPos.getX() + sample.x,
                    occupiedPos.getY() + sample.y,
                    occupiedPos.getZ() + sample.z
                ));
            }
        }
        
        // 从信号源向设备的每个采样点发射射线
        Vec3 sourcePos = new Vec3(
            signalSource.getX() + 0.5,
            signalSource.getY() + 0.5,
            signalSource.getZ() + 0.5
        );
        
        for (Vec3 target : targetPoints) {
            if (checkRayTrace(sourcePos, target)) {
                return true;  // 找到一条通路
            }
        }
        
        return false;  // 所有射线都被遮挡
    }
    
    /**
     * 检查单条射线是否畅通
     * @param from 起点
     * @param to 终点
     * @return true 如果射线畅通
     */
    private boolean checkRayTrace(Vec3 from, Vec3 to) {
        // 使用 Minecraft 原生的射线追踪
        var hitResult = level.clip(new net.minecraft.world.level.ClipContext(
            from, to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            null
        ));
        
        if (hitResult == null) {
            return true;  // 没有命中，畅通
        }
        
        // 检查命中点是否在目标附近 (允许小误差)
        double distToTarget = hitResult.getLocation().distanceTo(to);
        if (distToTarget < 0.5) {
            return true;  // 命中点接近目标，视为畅通
        }
        
        return false;  // 被其他方块遮挡
    }
    
    private boolean cachedHasLineOfSight(BlockPos signalSource, BlockEntity device) {
        return hasLineOfSightRayTrace(signalSource, device);
    }
    
    private List<BlockPos> getDeviceOccupiedBlocks(BlockEntity device) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(device.getBlockPos());
        
        if (device instanceof IMultiBlockDevice multiBlock) {
            positions.clear();
            for (BlockPos offset : multiBlock.getOccupiedOffsets()) {
                positions.add(device.getBlockPos().offset(offset));
            }
        }
        
        return positions;
    }
    
    private void updateConnectionStatus() {}
    
    public Component getConnectionStatus() {
        if (isInConnectionMode) {
            return Component.literal("连接模式中...").withStyle(ChatFormatting.YELLOW);
        }
        if (connectedPos != null) {
            int distance = calculateDistance(worldPosition, connectedPos);
            boolean connectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
            String status = connectedToCore ? "§a 已连接" : "§c 未连接到核心";
            return Component.literal(status + " 距离：" + distance + "/" + MAX_DISTANCE);
        }
        boolean connectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        String status = connectedToCore ? "§a 工作中" : "§c 未连接核心";
        return Component.literal(status + " 自动连接：" + autoConnectedDevices.size() + " 设备");
    }
    
    public boolean isInConnectionMode() { return isInConnectionMode; }
    public Set<BlockPos> getAutoConnectedDevices() { return autoConnectedDevices; }
    public boolean isConnectedToCore() { return EnergyGrid.getInstance().isConnectedToCore(worldPosition); }
    public BlockPos getSignalSourcePos() { return worldPosition.above(3); }
}
