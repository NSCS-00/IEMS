package com.dlzstudio.iems.energy;

import com.dlzstudio.iems.IEMSMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 电网系统单例
 * 管理所有能源设备、能量分配和连接
 */
public class EnergyGrid {
    
    private static EnergyGrid instance;
    
    // 核心位置
    private BlockPos corePos;
    private ResourceLocation coreId;
    
    // 已连接的设备列表
    private final Set<BlockPos> connectedDevices = ConcurrentHashMap.newKeySet();
    
    // 设备供能开关 (位置字符串 -> 是否启用)
    private final Map<String, Boolean> deviceEnabled = new ConcurrentHashMap<>();
    
    // 存储器列表
    private final List<BlockPos> energyStorages = new CopyOnWriteArrayList<>();
    
    // 中继器列表
    private final List<BlockPos> relays = new CopyOnWriteArrayList<>();
    
    // 广播塔列表
    private final List<BlockPos> broadcastTowers = new CopyOnWriteArrayList<>();
    
    private EnergyGrid() {
    }
    
    public static EnergyGrid getInstance() {
        if (instance == null) {
            instance = new EnergyGrid();
        }
        return instance;
    }
    
    /**
     * 注册核心
     */
    public void registerCore(BlockPos pos, ResourceLocation providerId) {
        this.corePos = pos;
        this.coreId = providerId;
        IEMSMod.LOGGER.info("核心已注册：{} 位于 {}", providerId, pos);
    }
    
    /**
     * 注销核心
     */
    public void unregisterCore() {
        if (corePos != null) {
            GridCoreRegistry.unregisterCore(corePos);
            corePos = null;
            coreId = null;
        }
    }
    
    /**
     * 获取核心位置
     */
    public BlockPos getCorePos() {
        return corePos;
    }
    
    /**
     * 获取核心 ID
     */
    public ResourceLocation getCoreId() {
        return coreId;
    }
    
    /**
     * 检查是否有核心
     */
    public boolean hasCore() {
        return corePos != null && coreId != null;
    }
    
    /**
     * 注册存储器
     */
    public void registerStorage(BlockPos pos) {
        if (!energyStorages.contains(pos)) {
            energyStorages.add(pos);
        }
    }
    
    /**
     * 注销存储器
     */
    public void unregisterStorage(BlockPos pos) {
        energyStorages.remove(pos);
    }
    
    /**
     * 注册中继器
     */
    public void registerRelay(BlockPos pos) {
        if (!relays.contains(pos)) {
            relays.add(pos);
        }
    }
    
    /**
     * 注销中继器
     */
    public void unregisterRelay(BlockPos pos) {
        relays.remove(pos);
    }
    
    /**
     * 注册广播塔
     */
    public void registerBroadcastTower(BlockPos pos) {
        if (!broadcastTowers.contains(pos)) {
            broadcastTowers.add(pos);
        }
    }
    
    /**
     * 注销广播塔
     */
    public void unregisterBroadcastTower(BlockPos pos) {
        broadcastTowers.remove(pos);
    }
    
    /**
     * 添加连接
     */
    public void addConnection(BlockPos from, BlockPos to) {
        connectedDevices.add(from);
        connectedDevices.add(to);
        deviceEnabled.put(posToString(to), true);
    }
    
    /**
     * 移除连接
     */
    public void removeConnection(BlockPos from, BlockPos to) {
        connectedDevices.remove(from);
        connectedDevices.remove(to);
    }
    
    /**
     * 检查设备是否连接到核心
     */
    public boolean isConnectedToCore(BlockPos pos) {
        return connectedDevices.contains(pos) || (corePos != null && corePos.equals(pos));
    }
    
    /**
     * 获取所有连接的设备
     */
    public Set<BlockPos> getConnectedDevices() {
        return Collections.unmodifiableSet(connectedDevices);
    }
    
    /**
     * 切换设备供能状态
     */
    public void toggleDevice(BlockPos pos) {
        String key = posToString(pos);
        boolean current = deviceEnabled.getOrDefault(key, true);
        deviceEnabled.put(key, !current);
        IEMSMod.LOGGER.info("设备 {} 供能状态：{}", pos, !current ? "开启" : "关闭");
    }
    
    /**
     * 设置设备供能状态
     */
    public void setDeviceEnabled(BlockPos pos, boolean enabled) {
        deviceEnabled.put(posToString(pos), enabled);
    }
    
    /**
     * 检查设备是否启用
     */
    public boolean isDeviceEnabled(BlockPos pos) {
        return deviceEnabled.getOrDefault(posToString(pos), true);
    }
    
    /**
     * 获取启用的设备列表
     */
    public Map<String, Boolean> getEnabledDevices() {
        return new HashMap<>(deviceEnabled);
    }
    
    /**
     * 获取核心能量
     */
    public EnergyValue getCoreEnergy(Level level) {
        if (!hasCore()) return EnergyValue.zero();
        
        GridCoreRegistry.CoreProvider provider = GridCoreRegistry.getProvider(coreId);
        if (provider != null) {
            return provider.getEnergy(level, corePos);
        }
        return EnergyValue.zero();
    }
    
    /**
     * 设置核心能量
     */
    public void setCoreEnergy(Level level, EnergyValue energy) {
        if (!hasCore()) return;
        
        GridCoreRegistry.CoreProvider provider = GridCoreRegistry.getProvider(coreId);
        if (provider != null) {
            provider.setEnergy(level, corePos, energy);
        }
    }
    
    /**
     * 获取核心消耗
     */
    public EnergyValue getCoreConsumption(Level level) {
        if (!hasCore()) return EnergyValue.zero();
        
        GridCoreRegistry.CoreProvider provider = GridCoreRegistry.getProvider(coreId);
        if (provider != null) {
            return provider.getConsumption(level, corePos);
        }
        return EnergyValue.zero();
    }
    
    /**
     * 获取核心产出
     */
    public EnergyValue getCoreGeneration(Level level) {
        if (!hasCore()) return EnergyValue.zero();
        
        GridCoreRegistry.CoreProvider provider = GridCoreRegistry.getProvider(coreId);
        if (provider != null) {
            return provider.getGeneration(level, corePos);
        }
        return EnergyValue.zero();
    }
    
    /**
     * 清空电网
     */
    public void clear() {
        connectedDevices.clear();
        deviceEnabled.clear();
        energyStorages.clear();
        relays.clear();
        broadcastTowers.clear();
        corePos = null;
        coreId = null;
    }
    
    private String posToString(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
