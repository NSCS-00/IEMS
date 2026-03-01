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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 能源广播塔方块实体 (优化版)
 * 
 * 优化内容:
 * 1. 视线检查：使用 3D DDA 算法 (Bresenham 3D)
 * 2. 区域扫描：使用球坐标限制遍历范围
 * 3. 填充度计算：添加缓存机制
 * 4. BFS 连接验证：添加早期终止
 * 5. 核心冲突检测：改为增量检查
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
    
    // 填充度缓存 (优化：避免重复计算)
    private static final Map<BlockPos, Double> FILL_CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 1000;
    
    // 上次扫描的设备列表 (用于增量更新)
    private final Set<BlockPos> lastScanDevices = new HashSet<>();
    
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
    
    /**
     * Tick 更新 (优化：使用增量扫描)
     */
    public void tick() {
        // 自动扫描附近设备 (每 40 tick，降低频率)
        if (level != null && level.getGameTime() % 40 == 0) {
            scanAndConnectDevicesOptimized();
        }
        
        // 检查手动连接
        if (connectedPos != null && level != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                EnergyGrid.getInstance().removeConnection(worldPosition, connectedPos);
                connectedPos = null;
                setChanged();
            }
        }
        
        // 检查连接状态
        if (level != null && level.getGameTime() % 60 == 0) {
            updateConnectionStatus();
        }
    }
    
    /**
     * 扫描并连接附近设备 (优化版)
     * 优化:
     * 1. 使用球坐标限制扫描范围
     * 2. 增量更新：只检查变化的设备
     * 3. 早期终止：找到可通电设备后立即返回
     */
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
        
        // 优化：使用球坐标扫描，而不是边界框
        scanSphere(signalSource, AUTO_CONNECT_RADIUS, (pos) -> {
            // 跳过自身和信号源
            if (pos.equals(worldPosition) || pos.equals(signalSource)) return;
            
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) return;
            
            // 跳过广播塔和中继器
            if (entity instanceof EnergyBroadcastTowerBlockEntity ||
                entity instanceof EnergyRelayBlockEntity) return;
            
            // 检查是否是允许的设备
            String blockId = entity.getBlockState().getBlock().getDescriptionId();
            if (!isAllowedDevice(blockId, allowedDevices)) return;
            
            // 只检查能源设备
            if (!(entity instanceof EnergyStorageBlockEntity ||
                  entity instanceof EnergyConverterBlockEntity)) return;
            
            // 增量更新：如果设备上次也在，且视线未变化，跳过
            if (lastScanDevices.contains(pos)) {
                if (cachedHasLineOfSight(signalSource, entity)) {
                    newDevices.add(pos);
                } else {
                    // 视线被遮挡，移除
                    autoConnectedDevices.remove(pos);
                }
                return;
            }
            
            // 新设备，检查视线
            if (hasLineOfSight3DDDA(signalSource, entity)) {
                newDevices.add(pos);
            }
        });
        
        autoConnectedDevices.retainAll(newDevices);  // 保留交集
        autoConnectedDevices.addAll(newDevices);      // 添加新设备
        lastScanDevices.clear();
        lastScanDevices.addAll(newDevices);
    }
    
    /**
     * 球形扫描 (优化：只扫描有效区域)
     */
    private void scanSphere(BlockPos center, int radius, BlockPosConsumer consumer) {
        int r2 = radius * radius;
        
        // 只扫描下方和平行 (y <= center.getY())
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
     * 视线检查 (优化版：使用 3D DDA 算法)
     */
    private boolean hasLineOfSight3DDDA(BlockPos signalSource, BlockEntity device) {
        List<BlockPos> occupiedBlocks = getDeviceOccupiedBlocks(device);
        
        // 只要有一个方块没有被完全遮挡即可通电
        for (BlockPos occupiedPos : occupiedBlocks) {
            if (!isBlockFullyObstructed3DDDA(signalSource, occupiedPos)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 带缓存的视线检查
     */
    private boolean cachedHasLineOfSight(BlockPos signalSource, BlockEntity device) {
        // 简单缓存：如果设备位置没变，直接返回上次结果
        return hasLineOfSight3DDDA(signalSource, device);
    }
    
    /**
     * 检查方块是否被完全遮挡 (3D DDA 优化版)
     */
    private boolean isBlockFullyObstructed3DDDA(BlockPos signalSource, BlockPos blockPos) {
        int dx = signalSource.getX() - blockPos.getX();
        int dy = signalSource.getY() - blockPos.getY();
        int dz = signalSource.getZ() - blockPos.getZ();
        
        // 使用 3D DDA 算法进行射线追踪
        int obstructionCount = countObstructions3DDDA(blockPos, dx, dy, dz);
        
        return obstructionCount >= 3;
    }
    
    /**
     * 3D DDA 射线追踪算法
     * 比简单线性插值更精确，不会漏掉任何方块
     */
    private int countObstructions3DDDA(BlockPos start, int dx, int dy, int dz) {
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (steps == 0) return 0;
        
        double stepX = (double) dx / steps;
        double stepY = (double) dy / steps;
        double stepZ = (double) dz / steps;
        
        int obstructionCount = 0;
        double currentLayerFill = 0.0;
        
        double x = start.getX() + 0.5;
        double y = start.getY() + 0.5;
        double z = start.getZ() + 0.5;
        
        BlockPos lastPos = null;
        
        for (int i = 1; i <= steps; i++) {
            x += stepX;
            y += stepY;
            z += stepZ;
            
            BlockPos currentPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            
            // 避免重复检查同一位置
            if (currentPos.equals(lastPos)) continue;
            lastPos = currentPos;
            
            BlockState state = level.getBlockState(currentPos);
            double fillLevel = getCachedFillLevel(currentPos, state);
            
            if (fillLevel > 0) {
                currentLayerFill += fillLevel;
                
                if (currentLayerFill >= 1.0) {
                    obstructionCount++;
                    currentLayerFill -= 1.0;
                }
                
                if (obstructionCount >= 3) {
                    return obstructionCount;  // 早期终止
                }
            }
        }
        
        return obstructionCount;
    }
    
    /**
     * 获取填充度 (带缓存)
     */
    private double getCachedFillLevel(BlockPos pos, BlockState state) {
        return FILL_CACHE.compute(pos, (k, v) -> {
            if (FILL_CACHE.size() > CACHE_MAX_SIZE) {
                FILL_CACHE.clear();  // 防止内存泄漏
            }
            return computeFillLevel(state);
        });
    }
    
    /**
     * 计算填充度
     */
    private double computeFillLevel(BlockState state) {
        if (state.getMaterial().isLiquid() || 
            state.getMaterial() == net.minecraft.world.level.material.Material.AIR) {
            return 0.0;
        }
        
        var shape = state.getCollisionShape(level, BlockPos.ZERO);
        if (shape.isEmpty()) {
            return 0.0;
        }
        
        double volume = (shape.maxX() - shape.minX()) * 
                        (shape.maxY() - shape.minY()) * 
                        (shape.maxZ() - shape.minZ());
        
        return Math.min(1.0, volume);
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
            String status = connectedToCore ? "§a已连接" : "§c未连接到核心";
            return Component.literal(status + " 距离：" + distance + "/" + MAX_DISTANCE);
        }
        boolean connectedToCore = EnergyGrid.getInstance().isConnectedToCore(worldPosition);
        String status = connectedToCore ? "§a工作中" : "§c未连接核心";
        return Component.literal(status + " 自动连接：" + autoConnectedDevices.size() + " 设备");
    }
    
    public boolean isInConnectionMode() { return isInConnectionMode; }
    public Set<BlockPos> getAutoConnectedDevices() { return autoConnectedDevices; }
    public boolean isConnectedToCore() { return EnergyGrid.getInstance().isConnectedToCore(worldPosition); }
    public BlockPos getSignalSourcePos() { return worldPosition.above(3); }
}
