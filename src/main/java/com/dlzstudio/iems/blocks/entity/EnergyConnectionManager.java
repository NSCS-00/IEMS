package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.IEMSMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 能量连接管理器
 * 管理所有能量连接
 */
public class EnergyConnectionManager {
    
    private static final EnergyConnectionManager INSTANCE = new EnergyConnectionManager();
    
    // 所有活跃的连接
    private final Map<UUID, EnergyConnection> connections = new ConcurrentHashMap<>();
    
    // 允许连接的设备列表 (由其他 MOD 注册)
    private final Set<ResourceLocation> allowedDeviceTypes = ConcurrentHashMap.newKeySet();
    
    // 设备类型注册表 (Block -> 是否允许连接)
    private final Map<ResourceLocation, Boolean> deviceRegistry = new ConcurrentHashMap<>();
    
    private EnergyConnectionManager() {
        // 注册 IEMS 默认设备
        registerDefaultDevices();
    }
    
    /**
     * 注册默认设备
     */
    private void registerDefaultDevices() {
        // IEMS 模组的所有能源设备默认允许
        registerAllowedDevice(IEMSMod.MODID, "standard_energy_storage");
        registerAllowedDevice(IEMSMod.MODID, "general_energy_storage");
        registerAllowedDevice(IEMSMod.MODID, "energy_converter");
        
        IEMSMod.LOGGER.info("注册 IEMS 默认能源设备");
    }
    
    public static EnergyConnectionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 开始连接
     */
    public void startConnection(UUID connectionId, BlockPos startPos) {
        connections.put(connectionId, new EnergyConnection(connectionId, startPos));
        IEMSMod.LOGGER.debug("开始连接：{} 从 {}", connectionId, startPos);
    }
    
    /**
     * 完成连接
     */
    public void completeConnection(UUID connectionId, BlockPos endPos) {
        EnergyConnection connection = connections.get(connectionId);
        if (connection != null) {
            connection.setEndPos(endPos);
            connection.setActive(true);
            IEMSMod.LOGGER.debug("完成连接：{} 从 {} 到 {}", connectionId, connection.getStartPos(), endPos);
        }
    }
    
    /**
     * 移除连接
     */
    public void removeConnection(UUID connectionId) {
        connections.remove(connectionId);
        IEMSMod.LOGGER.debug("移除连接：{}", connectionId);
    }
    
    /**
     * 获取所有活跃连接
     */
    public Collection<EnergyConnection> getAllConnections() {
        return connections.values();
    }
    
    /**
     * 注册允许连接的设备
     * @param modId 模组 ID
     * @param deviceName 设备名称 (方块 ID 的后缀)
     */
    public void registerAllowedDevice(String modId, String deviceName) {
        ResourceLocation id = new ResourceLocation(modId, deviceName);
        allowedDeviceTypes.add(id);
        deviceRegistry.put(id, true);
        IEMSMod.LOGGER.debug("注册允许连接的设备：{}", id);
    }
    
    /**
     * 注册允许连接的设备 (使用 ResourceLocation)
     */
    public void registerAllowedDevice(ResourceLocation deviceId) {
        allowedDeviceTypes.add(deviceId);
        deviceRegistry.put(deviceId, true);
        IEMSMod.LOGGER.debug("注册允许连接的设备：{}", deviceId);
    }
    
    /**
     * 移除允许连接的设备
     */
    public void removeAllowedDevice(String modId, String deviceName) {
        ResourceLocation id = new ResourceLocation(modId, deviceName);
        allowedDeviceTypes.remove(id);
        deviceRegistry.remove(id);
        IEMSMod.LOGGER.debug("移除允许连接的设备：{}", id);
    }
    
    /**
     * 移除允许连接的设备
     */
    public void removeAllowedDevice(ResourceLocation deviceId) {
        allowedDeviceTypes.remove(deviceId);
        deviceRegistry.remove(deviceId);
        IEMSMod.LOGGER.debug("移除允许连接的设备：{}", deviceId);
    }
    
    /**
     * 检查设备是否允许连接
     * @param modId 模组 ID
     * @param deviceName 设备名称
     * @return 是否允许
     */
    public boolean isDeviceAllowed(String modId, String deviceName) {
        ResourceLocation id = new ResourceLocation(modId, deviceName);
        return isDeviceAllowed(id);
    }
    
    /**
     * 检查设备是否允许连接
     */
    public boolean isDeviceAllowed(ResourceLocation deviceId) {
        // 首先检查显式注册的设备
        Boolean allowed = deviceRegistry.get(deviceId);
        if (allowed != null) {
            return allowed;
        }
        
        // 然后检查是否匹配任何已注册的设备类型 (支持模糊匹配)
        for (ResourceLocation allowedId : allowedDeviceTypes) {
            // 完全匹配
            if (allowedId.equals(deviceId)) {
                return true;
            }
            // 模组 ID 匹配且设备名包含
            if (allowedId.getNamespace().equals(deviceId.getNamespace()) &&
                deviceId.getPath().contains(allowedId.getPath())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查设备是否允许连接 (通过方块描述 ID)
     */
    public boolean isDeviceAllowedByDescriptionId(String descriptionId) {
        // descriptionId 格式如："block.iems.standard_energy_storage"
        if (descriptionId == null || descriptionId.isEmpty()) {
            return false;
        }
        
        // 提取模组 ID 和设备名
        String[] parts = descriptionId.split("\\.");
        if (parts.length >= 3) {
            String modId = parts[1]; // 如 "iemd"
            String deviceName = String.join(".", Arrays.copyOfRange(parts, 2, parts.length));
            return isDeviceAllowed(modId, deviceName);
        }
        
        return false;
    }
    
    /**
     * 获取所有允许连接的设备
     */
    public Set<ResourceLocation> getAllowedDevices() {
        return Collections.unmodifiableSet(allowedDeviceTypes);
    }
    
    /**
     * 清空所有注册 (用于测试或重置)
     */
    public void clearRegistry() {
        allowedDeviceTypes.clear();
        deviceRegistry.clear();
        registerDefaultDevices();
    }
    
    /**
     * 能量连接数据
     */
    public static class EnergyConnection {
        private final UUID id;
        private BlockPos startPos;
        private BlockPos endPos;
        private boolean isActive;
        private boolean isDepleted = false; // 电网耗尽时设为 true
        
        public EnergyConnection(UUID id, BlockPos startPos) {
            this.id = id;
            this.startPos = startPos;
            this.isActive = false;
        }
        
        public UUID getId() {
            return id;
        }
        
        public BlockPos getStartPos() {
            return startPos;
        }
        
        public BlockPos getEndPos() {
            return endPos;
        }
        
        public void setEndPos(BlockPos endPos) {
            this.endPos = endPos;
        }
        
        public boolean isActive() {
            return isActive;
        }
        
        public void setActive(boolean active) {
            isActive = active;
        }
        
        public boolean isDepleted() {
            return isDepleted;
        }
        
        public void setDepleted(boolean depleted) {
            isDepleted = depleted;
        }
        
        /**
         * 获取连接长度
         */
        public int getLength() {
            if (endPos == null) return 0;
            return (int) Math.round(startPos.distSqr(endPos));
        }
    }
}
