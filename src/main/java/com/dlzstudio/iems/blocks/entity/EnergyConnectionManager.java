package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.energy.EnergyGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 能源连接管理器
 * 管理所有设备之间的连接关系
 */
public class EnergyConnectionManager {
    
    private static EnergyConnectionManager instance;
    
    // 进行中的连接 (连接 ID -> 连接信息)
    private final Map<UUID, Connection> activeConnections = new ConcurrentHashMap<>();
    
    // 已完成的连接列表
    private final List<Connection> completedConnections = new CopyOnWriteArrayList<>();
    
    // 允许连接的设备类型
    private final Set<ResourceLocation> allowedDevices = ConcurrentHashMap.newKeySet();
    
    // 缓存：位置 -> 是否连接到核心
    private final Map<BlockPos, Boolean> connectionCache = new ConcurrentHashMap<>();
    
    private EnergyConnectionManager() {
        // 注册默认允许连接的设备
        registerAllowedDevice("IEMS", "energy_storage");
        registerAllowedDevice("IEMS", "energy_converter");
        registerAllowedDevice("IEMS", "energy_relay");
        registerAllowedDevice("IEMS", "energy_broadcast_tower");
    }
    
    public static EnergyConnectionManager getInstance() {
        if (instance == null) {
            instance = new EnergyConnectionManager();
        }
        return instance;
    }
    
    /**
     * 开始一个新连接
     */
    public void startConnection(UUID connectionId, BlockPos startPos) {
        activeConnections.put(connectionId, new Connection(connectionId, startPos, null));
        IEMSMod.LOGGER.debug("开始连接：{} 从 {}", connectionId, startPos);
    }
    
    /**
     * 完成连接
     */
    public boolean completeConnection(UUID connectionId, BlockPos endPos) {
        Connection connection = activeConnections.get(connectionId);
        if (connection == null) return false;
        
        connection.endPos = endPos;
        completedConnections.add(connection);
        activeConnections.remove(connectionId);
        
        IEMSMod.LOGGER.debug("完成连接：{} 从 {} 到 {}", connectionId, connection.startPos, endPos);
        return true;
    }
    
    /**
     * 移除连接
     */
    public void removeConnection(UUID connectionId) {
        activeConnections.remove(connectionId);
        IEMSMod.LOGGER.debug("移除连接：{}", connectionId);
    }
    
    /**
     * 移除连接
     */
    public void removeConnection(BlockPos pos) {
        completedConnections.removeIf(c -> 
            (c.startPos != null && c.startPos.equals(pos)) || 
            (c.endPos != null && c.endPos.equals(pos)));
        connectionCache.clear();
    }
    
    /**
     * 注册允许连接的设备类型
     */
    public void registerAllowedDevice(String modId, String deviceName) {
        ResourceLocation id = ResourceLocation.tryBuild(modId, deviceName);
        if (id != null) {
            allowedDevices.add(id);
            IEMSMod.LOGGER.debug("注册允许连接的设备：{}", id);
        }
    }
    
    /**
     * 移除允许连接的设备类型
     */
    public void unregisterAllowedDevice(String modId, String deviceName) {
        ResourceLocation id = ResourceLocation.tryBuild(modId, deviceName);
        if (id != null) {
            allowedDevices.remove(id);
            IEMSMod.LOGGER.debug("移除允许连接的设备：{}", id);
        }
    }
    
    /**
     * 检查设备类型是否允许连接
     */
    public boolean isDeviceAllowed(ResourceLocation deviceId) {
        return allowedDevices.contains(deviceId);
    }
    
    /**
     * 获取所有允许连接的设备
     */
    public Set<ResourceLocation> getAllowedDevices() {
        return Collections.unmodifiableSet(allowedDevices);
    }
    
    /**
     * 检查位置是否已连接到核心
     */
    public boolean isConnectedToCore(Level level, BlockPos pos) {
        if (connectionCache.containsKey(pos)) {
            return connectionCache.get(pos);
        }
        
        // 检查是否是核心位置
        if (EnergyGrid.getInstance().getCorePos() != null && 
            EnergyGrid.getInstance().getCorePos().equals(pos)) {
            connectionCache.put(pos, true);
            return true;
        }
        
        // 检查是否有连接到核心的路径
        boolean connected = hasPathToCore(level, pos, new HashSet<>());
        connectionCache.put(pos, connected);
        return connected;
    }
    
    /**
     * 递归检查是否有路径到核心
     */
    private boolean hasPathToCore(Level level, BlockPos pos, Set<BlockPos> visited) {
        if (visited.contains(pos)) return false;
        visited.add(pos);
        
        // 检查是否是核心
        if (EnergyGrid.getInstance().getCorePos() != null && 
            EnergyGrid.getInstance().getCorePos().equals(pos)) {
            return true;
        }
        
        // 检查所有连接
        for (Connection conn : completedConnections) {
            if (conn.startPos != null && conn.startPos.equals(pos)) {
                if (conn.endPos != null && hasPathToCore(level, conn.endPos, visited)) {
                    return true;
                }
            }
            if (conn.endPos != null && conn.endPos.equals(pos)) {
                if (conn.startPos != null && hasPathToCore(level, conn.startPos, visited)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        connectionCache.clear();
    }
    
    /**
     * 清空所有连接
     */
    public void clear() {
        activeConnections.clear();
        completedConnections.clear();
        connectionCache.clear();
    }
    
    /**
     * 连接信息类
     */
    public static class Connection {
        public final UUID id;
        public BlockPos startPos;
        public BlockPos endPos;
        
        public Connection(UUID id, BlockPos startPos, BlockPos endPos) {
            this.id = id;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }
}
