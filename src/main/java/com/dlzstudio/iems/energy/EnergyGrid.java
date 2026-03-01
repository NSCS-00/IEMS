package com.dlzstudio.iems.energy;

import com.dlzstudio.iems.IEMSMod;
import com.dlzstudio.iems.blocks.entity.EnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电网系统 - 单例模式
 * 管理整个世界的能源网络
 */
public class EnergyGrid {
    private static final Lazy<EnergyGrid> INSTANCE = Lazy.of(EnergyGrid::new);
    
    private final Set<EnergyStorageBlockEntity> storageDevices = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> relayPositions = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> broadcastTowerPositions = ConcurrentHashMap.newKeySet();
    private final Map<BlockPos, Set<BlockPos>> connections = new ConcurrentHashMap<>();
    private final Set<BlockPos> connectedToCore = ConcurrentHashMap.newKeySet();
    
    private static final BigInteger DEFAULT_CORE_CAPACITY_SE = BigInteger.TEN.pow(20);
    private EnergyValue coreEnergy;
    
    private EnergyValue currentConsumption = EnergyValue.zero();
    private EnergyValue currentGeneration = EnergyValue.zero();
    private EnergyValue lastConsumption = EnergyValue.zero();
    private EnergyValue lastGeneration = EnergyValue.zero();
    
    private boolean isDepleted = false;
    private boolean isChargingStorages = false;
    
    private MinecraftServer server;
    private int webServerPort = 8080;
    
    private EnergyGrid() {
        this.coreEnergy = new EnergyValue(DEFAULT_CORE_CAPACITY_SE, EnergyValue.EnergyUnit.SE);
    }
    
    public static EnergyGrid getInstance() {
        return INSTANCE.get();
    }
    
    public void onServerTick(MinecraftServer server) {
        this.server = server;
        
        lastConsumption = currentConsumption;
        lastGeneration = currentGeneration;
        currentConsumption = EnergyValue.zero();
        currentGeneration = EnergyValue.zero();
        
        BlockPos corePos = GridCoreRegistry.getActiveCorePos();
        if (corePos != null) {
            Level level = server.overworld();
            ResourceLocation providerId = GridCoreRegistry.getActiveCoreId();
            GridCoreRegistry.CoreProvider provider = GridCoreRegistry.getProvider(providerId);
            
            if (provider != null && provider.isValidCore(level, corePos)) {
                syncCoreData(level, provider, corePos);
            } else {
                IEMSMod.LOGGER.warn("[{}] 核心失效，位置：{}", GridCoreRegistry.getCoreName(), corePos);
            }
        }
        
        if (server.overworld() != null && server.getTickCount() % 100 == 0) {
            GridCoreRegistry.checkForMultipleCores(server.overworld());
        }
        
        validateConnections();
        calculatePower();
        processGridLogic();
    }
    
    private void syncCoreData(Level level, GridCoreRegistry.CoreProvider provider, BlockPos corePos) {
        coreEnergy = provider.getEnergy(level, corePos);
        lastConsumption = provider.getConsumption(level, corePos);
        lastGeneration = provider.getGeneration(level, corePos);
        isDepleted = provider.isDepleted(level, corePos);
        
        provider.setEnergy(level, corePos, coreEnergy);
        provider.setConsumption(level, corePos, lastConsumption);
        provider.setGeneration(level, corePos, lastGeneration);
        provider.setDepleted(level, corePos, isDepleted);
    }
    
    private void validateConnections() {
        connectedToCore.clear();
        
        BlockPos corePos = GridCoreRegistry.getActiveCorePos();
        if (corePos == null) return;
        
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        queue.offer(corePos);
        visited.add(corePos);
        
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            connectedToCore.add(current);
            
            Set<BlockPos> connected = connections.get(current);
            if (connected != null) {
                for (BlockPos neighbor : connected) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
        }
    }
    
    private void calculatePower() {}
    
    private void processGridLogic() {
        EnergyValue netPower = currentGeneration.subtract(currentConsumption);
        
        if (netPower.compareTo(EnergyValue.zero()) >= 0) {
            isDepleted = false;
            isChargingStorages = true;
            coreEnergy = coreEnergy.add(netPower);
            
            EnergyValue coreCapacity = getCoreCapacity();
            if (coreEnergy.compareTo(coreCapacity) > 0) {
                EnergyValue excessEnergy = coreEnergy.subtract(coreCapacity);
                coreEnergy = coreCapacity;
                chargeStorages(excessEnergy);
            }
        } else {
            EnergyValue deficit = currentConsumption.subtract(currentGeneration);
            isChargingStorages = false;
            
            if (coreEnergy.compareTo(deficit) >= 0) {
                coreEnergy = coreEnergy.subtract(deficit);
                isDepleted = false;
            } else {
                isDepleted = true;
                requestEnergyFromStorages(deficit);
            }
        }
    }
    
    private void chargeStorages(EnergyValue excessEnergy) {
        if (storageDevices.isEmpty()) return;
        EnergyValue perStorage = excessEnergy.divide(storageDevices.size());
        for (EnergyStorageBlockEntity storage : storageDevices) {
            if (!storage.isRemoved() && storage.canReceiveEnergy()) {
                storage.receiveEnergy(perStorage, false);
            }
        }
    }
    
    private void requestEnergyFromStorages(EnergyValue deficit) {
        if (storageDevices.isEmpty()) return;
        EnergyValue requestAmount = currentConsumption.multiply(2);
        if (requestAmount.compareTo(deficit) > 0) requestAmount = deficit;
        EnergyValue perStorageRequest = requestAmount.divide(storageDevices.size());
        for (EnergyStorageBlockEntity storage : storageDevices) {
            if (!storage.isRemoved() && storage.hasEnergyToOutput()) {
                EnergyValue extracted = storage.extractEnergy(perStorageRequest, false);
                coreEnergy = coreEnergy.add(extracted);
            }
        }
    }
    
    public void addConnection(BlockPos from, BlockPos to) {
        connections.computeIfAbsent(from, k -> ConcurrentHashMap.newKeySet()).add(to);
        connections.computeIfAbsent(to, k -> ConcurrentHashMap.newKeySet()).add(from);
        validateConnections();
    }
    
    public void removeConnection(BlockPos from, BlockPos to) {
        Set<BlockPos> fromConnections = connections.get(from);
        if (fromConnections != null) fromConnections.remove(to);
        Set<BlockPos> toConnections = connections.get(to);
        if (toConnections != null) toConnections.remove(from);
        validateConnections();
    }
    
    public boolean isConnectedToCore(BlockPos pos) {
        return connectedToCore.contains(pos);
    }
    
    public Set<BlockPos> getConnectedDevices() {
        return Collections.unmodifiableSet(connectedToCore);
    }
    
    public void registerCore(BlockPos pos, ResourceLocation providerId) {
        if (!GridCoreRegistry.canPlaceNewCore(pos)) {
            return;
        }
        GridCoreRegistry.register(providerId, null); // Provider 由其他 MOD 注册
        GridCoreRegistry.tryActivateCore(server != null ? server.overworld() : null, pos, providerId);
        IEMSMod.LOGGER.info("核心已注册：{} 位于 {}", providerId, pos);
    }
    
    public void unregisterCore(BlockPos pos) {
        GridCoreRegistry.unregisterCore(pos);
    }
    
    public void registerStorage(EnergyStorageBlockEntity storage) {
        storageDevices.add(storage);
    }
    
    public void unregisterStorage(EnergyStorageBlockEntity storage) {
        storageDevices.remove(storage);
    }
    
    public void registerRelay(BlockPos pos) {
        relayPositions.add(pos);
    }
    
    public void unregisterRelay(BlockPos pos) {
        relayPositions.remove(pos);
        connections.remove(pos);
        for (Set<BlockPos> connected : connections.values()) {
            connected.remove(pos);
        }
    }
    
    public void registerBroadcastTower(BlockPos pos) {
        broadcastTowerPositions.add(pos);
    }
    
    public void unregisterBroadcastTower(BlockPos pos) {
        broadcastTowerPositions.remove(pos);
        connections.remove(pos);
        for (Set<BlockPos> connected : connections.values()) {
            connected.remove(pos);
        }
    }
    
    public EnergyValue getCoreEnergy() { return coreEnergy; }
    
    public EnergyValue getCoreCapacity() {
        BlockPos corePos = GridCoreRegistry.getActiveCorePos();
        if (corePos != null && server != null) {
            Level level = server.overworld();
            ResourceLocation providerId = GridCoreRegistry.getActiveCoreId();
            GridCoreRegistry.CoreProvider provider = GridCoreRegistry.getProvider(providerId);
            if (provider != null) {
                EnergyValue capacity = provider.getCapacity(level, corePos);
                if (capacity != null) return capacity;
            }
        }
        return new EnergyValue(DEFAULT_CORE_CAPACITY_SE, EnergyValue.EnergyUnit.SE);
    }
    
    public EnergyValue getConsumption() { return lastConsumption; }
    public EnergyValue getGeneration() { return lastGeneration; }
    public boolean isDepleted() { return isDepleted; }
    public boolean isChargingStorages() { return isChargingStorages; }
    public BlockPos getActiveCorePos() { return GridCoreRegistry.getActiveCorePos(); }
    public ResourceLocation getActiveCoreProviderId() { return GridCoreRegistry.getActiveCoreId(); }
    public int getWebServerPort() { return webServerPort; }
    public void setWebServerPort(int port) { this.webServerPort = port; }
    
    public String getTimeToFull() {
        BlockPos corePos = GridCoreRegistry.getActiveCorePos();
        if (corePos == null) return "--:--";
        
        EnergyValue netPower = lastGeneration.subtract(lastConsumption);
        if (netPower.compareTo(EnergyValue.zero()) <= 0) return "∞";
        
        EnergyValue remainingCapacity = getCoreCapacity().subtract(coreEnergy);
        if (remainingCapacity.isEmpty()) return "0:00";
        
        BigInteger ticksNeeded = remainingCapacity.getValueInFE().divide(netPower.getValueInFE());
        long seconds = ticksNeeded.longValue() / 20;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        
        if (minutes > 999) return "∞";
        return String.format("%d:%02d", minutes, secs);
    }
    
    public String getEnergyDisplay() { return coreEnergy.toString(); }
    
    public String getPowerDisplay() {
        return String.format("能量消耗：%s  能量产出：%s", lastConsumption.toString(), lastGeneration.toString());
    }
    
    public void clear() {
        storageDevices.clear();
        relayPositions.clear();
        broadcastTowerPositions.clear();
        connections.clear();
        connectedToCore.clear();
        coreEnergy = new EnergyValue(DEFAULT_CORE_CAPACITY_SE, EnergyValue.EnergyUnit.SE);
        currentConsumption = EnergyValue.zero();
        currentGeneration = EnergyValue.zero();
        lastConsumption = EnergyValue.zero();
        lastGeneration = EnergyValue.zero();
        isDepleted = false;
        isChargingStorages = false;
        GridCoreRegistry.clear();
    }
}
