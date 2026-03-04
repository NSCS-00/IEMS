package com.dlzstudio.iems.energy;

import com.dlzstudio.iems.IEMSMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 电网核心注册表
 * 允许其他 MOD 注册自己的核心方块/多方块结构
 * 世界中只能有一个核心
 */
public class GridCoreRegistry {
    
    private static final Map<ResourceLocation, CoreProvider> CORE_PROVIDERS = new ConcurrentHashMap<>();
    private static final Set<BlockPos> REGISTERED_CORE_POSITIONS = ConcurrentHashMap.newKeySet();
    
    private static ResourceLocation activeCoreId = null;
    private static BlockPos activeCorePos = null;
    
    private static String coreName = "IEMS-CC";
    private static int coreNameCounter = 0;
    
    private static final Set<String> DLZSTUDIO_MODS = Set.of("IEMS", "dlzstudio", "plasmastudio");
    
    /**
     * 核心提供者接口
     */
    public interface CoreProvider {
        boolean isValidCore(Level level, BlockPos pos);
        EnergyValue getEnergy(Level level, BlockPos pos);
        void setEnergy(Level level, BlockPos pos, EnergyValue energy);
        EnergyValue getConsumption(Level level, BlockPos pos);
        void setConsumption(Level level, BlockPos pos, EnergyValue consumption);
        EnergyValue getGeneration(Level level, BlockPos pos);
        void setGeneration(Level level, BlockPos pos, EnergyValue generation);
        void setDepleted(Level level, BlockPos pos, boolean depleted);
        boolean isDepleted(Level level, BlockPos pos);
        EnergyValue getCapacity(Level level, BlockPos pos);
    }
    
    /**
     * 设置核心名称
     */
    public static void setCoreName(String name) {
        if (name != null && !name.isEmpty()) {
            coreName = name;
            coreNameCounter = 0;
            IEMSMod.LOGGER.info("[IEMS 信息] 核心名称已设置为：{}", coreName);
        }
    }
    
    /**
     * 获取核心名称
     */
    public static String getCoreName() {
        return coreName;
    }
    
    private static String generateUniqueCoreName() {
        if (coreNameCounter == 0) {
            return coreName;
        }
        return String.format("%s-%05d", coreName, coreNameCounter);
    }
    
    /**
     * 注册核心提供者
     */
    public static boolean register(ResourceLocation id, CoreProvider provider) {
        if (CORE_PROVIDERS.containsKey(id)) {
            IEMSMod.LOGGER.warn("[IEMS 警告] 核心提供者已存在：{}，注册被忽略", id);
            return false;
        }
        
        CORE_PROVIDERS.put(id, provider);
        IEMSMod.LOGGER.info("[IEMS 信息] 核心提供者已注册：{}", id);
        
        return true;
    }
    
    public static Map<ResourceLocation, CoreProvider> getAllProviders() {
        return Collections.unmodifiableMap(CORE_PROVIDERS);
    }
    
    public static ResourceLocation getActiveCoreId() {
        return activeCoreId;
    }
    
    public static BlockPos getActiveCorePos() {
        return activeCorePos;
    }
    
    public static boolean isActiveCore(BlockPos pos) {
        return activeCorePos != null && activeCorePos.equals(pos);
    }
    
    /**
     * 检查位置是否为核心
     */
    public static ResourceLocation checkCore(Level level, BlockPos pos) {
        for (Map.Entry<ResourceLocation, CoreProvider> entry : CORE_PROVIDERS.entrySet()) {
            if (entry.getValue().isValidCore(level, pos)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    public static CoreProvider getProvider(ResourceLocation id) {
        return CORE_PROVIDERS.get(id);
    }
    
    /**
     * 尝试激活核心
     */
    public static boolean tryActivateCore(Level level, BlockPos pos, ResourceLocation providerId) {
        if (REGISTERED_CORE_POSITIONS.contains(pos)) {
            IEMSMod.LOGGER.warn("[IEMS 警告] 位置 {} 已注册为核心，重复激活被拒绝", pos);
            return false;
        }
        
        if (activeCoreId != null && activeCorePos != null) {
            List<BlockPos> newCores = new ArrayList<>();
            newCores.add(pos);
            
            for (BlockPos registeredPos : REGISTERED_CORE_POSITIONS) {
                if (!registeredPos.equals(pos) && !registeredPos.equals(activeCorePos)) {
                    newCores.add(registeredPos);
                }
            }
            
            if (newCores.size() == 1) {
                IEMSMod.LOGGER.error("[IEMS 错误] 检测到多个{}核心！", generateUniqueCoreName());
                IEMSMod.LOGGER.error("[IEMS 错误] 当前活跃{}核心位置：{}", activeCorePos);
                IEMSMod.LOGGER.error("[IEMS 错误] 新{}核心位置：{}", pos);
                IEMSMod.LOGGER.error("[IEMS 错误] 新核心激活被拒绝");
            } else {
                IEMSMod.LOGGER.error("[IEMS 错误] 检测到多个{}核心！", generateUniqueCoreName());
                IEMSMod.LOGGER.error("[IEMS 错误] 当前活跃{}核心位置：{}", activeCorePos);
                for (int i = 0; i < newCores.size(); i++) {
                    coreNameCounter++;
                    String uniqueName = generateUniqueCoreName();
                    IEMSMod.LOGGER.error("[IEMS 错误] 新{}核心位置：{}", uniqueName, newCores.get(i));
                }
                IEMSMod.LOGGER.error("[IEMS 错误] 新核心激活被拒绝");
            }
            
            return false;
        }
        
        List<ResourceLocation> allProviders = new ArrayList<>(CORE_PROVIDERS.keySet());
        if (allProviders.size() > 1) {
            List<ResourceLocation> selected = new ArrayList<>();
            List<ResourceLocation> ignored = new ArrayList<>();
            
            ResourceLocation selectedId = selectActiveCore(allProviders, selected, ignored);
            
            if (!selectedId.equals(providerId)) {
                IEMSMod.LOGGER.warn("[IEMS 警告] 多核心冲突");
                IEMSMod.LOGGER.warn("[IEMS 警告] 核心 {} 被选中", selectedId);
                for (ResourceLocation ignoredId : ignored) {
                    IEMSMod.LOGGER.warn("[IEMS 警告] 核心 {} 被忽略", ignoredId);
                }
                return false;
            }
        }
        
        activeCoreId = providerId;
        activeCorePos = pos;
        REGISTERED_CORE_POSITIONS.add(pos);
        
        IEMSMod.LOGGER.info("[IEMS 信息] 核心已激活：{} (位置：{})", providerId, pos);
        
        return true;
    }
    
    private static ResourceLocation selectActiveCore(List<ResourceLocation> providers, List<ResourceLocation> selected, List<ResourceLocation> ignored) {
        if (providers.isEmpty()) {
            return null;
        }
        
        List<ResourceLocation> dlzstudioMods = new ArrayList<>();
        for (ResourceLocation id : providers) {
            if (DLZSTUDIO_MODS.contains(id.getNamespace())) {
                dlzstudioMods.add(id);
            }
        }
        
        if (dlzstudioMods.size() == 1) {
            selected.add(dlzstudioMods.get(0));
            IEMSMod.LOGGER.info("[IEMS 信息] 检测到等离子工作室提供的核心 {}，优先选择", dlzstudioMods.get(0));
            return dlzstudioMods.get(0);
        } else if (dlzstudioMods.size() > 1) {
            ResourceLocation first = dlzstudioMods.get(0);
            selected.add(first);
            for (int i = 1; i < dlzstudioMods.size(); i++) {
                ignored.add(dlzstudioMods.get(i));
            }
            IEMSMod.LOGGER.warn("[IEMS 警告] 检测到多个等离子工作室提供的核心，选择第一个：{}", first);
            return first;
        } else {
            ResourceLocation first = providers.get(0);
            selected.add(first);
            for (int i = 1; i < providers.size(); i++) {
                ignored.add(providers.get(i));
            }
            IEMSMod.LOGGER.warn("[IEMS 警告] 未检测到等离子工作室提供的核心，按加载顺序选择：{}", first);
            return first;
        }
    }
    
    /**
     * 注销核心
     */
    public static void unregisterCore(BlockPos pos) {
        if (REGISTERED_CORE_POSITIONS.remove(pos)) {
            if (activeCorePos != null && activeCorePos.equals(pos)) {
                IEMSMod.LOGGER.info("[IEMS 信息] 活跃核心已注销：{} (位置：{})", activeCoreId, activeCorePos);
                activeCoreId = null;
                activeCorePos = null;
            } else {
                IEMSMod.LOGGER.info("[IEMS 信息] 核心已注销：{}", pos);
            }
        }
    }
    
    /**
     * 检查多个核心
     */
    public static void checkForMultipleCores(Level level) {
        Set<BlockPos> validCores = new HashSet<>();
        
        for (BlockPos pos : REGISTERED_CORE_POSITIONS) {
            ResourceLocation providerId = checkCore(level, pos);
            if (providerId != null) {
                validCores.add(pos);
            }
        }
        
        if (validCores.size() > 1) {
            IEMSMod.LOGGER.error("[IEMS 致命错误] 检测到多个活跃的{}核心！", coreName);
            IEMSMod.LOGGER.error("[IEMS 致命错误] 位置：{}", formatPositions(validCores));
            IEMSMod.LOGGER.error("[IEMS 致命错误] 这违反了核心唯一性原则，已强制终止加载");
            
            throw new MultipleCoresDetectedException("检测到多个活跃核心");
        }
    }
    
    private static String formatPositions(Set<BlockPos> positions) {
        StringBuilder sb = new StringBuilder();
        for (BlockPos pos : positions) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("[").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ()).append("]");
        }
        return sb.toString();
    }
    
    /**
     * 检查是否可以放置新核心
     */
    public static boolean canPlaceNewCore(BlockPos pos) {
        if (activeCoreId != null && activeCorePos != null) {
            IEMSMod.LOGGER.error("[IEMS 错误] 世界中已存在核心 {} (位置：{})。每个世界只能有一个核心。", activeCoreId, activeCorePos);
            return false;
        }
        return true;
    }
    
    /**
     * 清空所有注册信息
     */
    public static void clear() {
        CORE_PROVIDERS.clear();
        REGISTERED_CORE_POSITIONS.clear();
        activeCoreId = null;
        activeCorePos = null;
        coreName = "IEMS-CC";
        coreNameCounter = 0;
    }
    
    public static class MultipleCoresDetectedException extends RuntimeException {
        public MultipleCoresDetectedException(String message) {
            super(message);
        }
    }
}
