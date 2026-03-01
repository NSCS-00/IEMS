package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;
import com.dlzstudio.iems.entities.IEMSEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.math.BigInteger;

/**
 * 能量转换器方块实�? *
 * 转换模式:
 * 1. SE -> GE (1SE = 100,000,000 GE)
 * 2. GE -> FE (1GE = 9×10^18 FE)
 * 3. GE -> AE (1GE = 9×10^18 AE, 需�?AE2)
 *
 * 缓存容量�?SE
 * 输出速度:
 * - 1 SE/s
 * - 10000 GE/s
 * - 10000000000 FE/s
 */
public class EnergyConverterBlockEntity extends BlockEntity {
    
    public enum ConversionMode {
        SE_TO_GE("SE �?GE", EnergyValue.EnergyUnit.SE, EnergyValue.EnergyUnit.GE, 
                 BigInteger.valueOf(100_000_000)), // 1SE = 10^8 GE
        GE_TO_FE("GE �?FE", EnergyValue.EnergyUnit.GE, EnergyValue.EnergyUnit.FE,
                 new BigInteger("9000000000000000000")), // 1GE = 9×10^18 FE
        GE_TO_AE("GE �?AE", EnergyValue.EnergyUnit.GE, EnergyValue.EnergyUnit.AE,
                 new BigInteger("9000000000000000000")); // 1GE = 9×10^18 AE
        
        public final String name;
        public final EnergyValue.EnergyUnit inputUnit;
        public final EnergyValue.EnergyUnit outputUnit;
        public final BigInteger conversionRate;
        
        ConversionMode(String name, EnergyValue.EnergyUnit inputUnit, 
                       EnergyValue.EnergyUnit outputUnit, BigInteger conversionRate) {
            this.name = name;
            this.inputUnit = inputUnit;
            this.outputUnit = outputUnit;
            this.conversionRate = conversionRate;
        }
        
        public String getName() {
            return name;
        }
    }
    
    // 缓存容量�?SE
    private static final BigInteger CACHE_CAPACITY_SE = BigInteger.ONE;
    private EnergyValue cacheEnergy;
    
    private ConversionMode currentMode = ConversionMode.SE_TO_GE;
    private int tickCounter = 0;
    
    // 输出速率 (�?tick)
    // 1 SE/s = 1/20 SE/tick
    // 10000 GE/s = 500 GE/tick
    // 10000000000 FE/s = 500000000 FE/tick
    private EnergyValue outputRatePerTick;
    
    public EnergyConverterBlockEntity(BlockPos pos, BlockState state) {
        super(IEMSEntities.ENERGY_CONVERTER_ENTITY.get(), pos, state);
        this.cacheEnergy = EnergyValue.zero();
        updateOutputRate();
    }
    
    private void updateOutputRate() {
        switch (currentMode) {
            case SE_TO_GE:
                // 10000 GE/s = 500 GE/tick
                outputRatePerTick = new EnergyValue(BigInteger.valueOf(500), EnergyValue.EnergyUnit.GE);
                break;
            case GE_TO_FE:
            case GE_TO_AE:
                // 10000000000 FE/s = 500000000 FE/tick
                outputRatePerTick = new EnergyValue(BigInteger.valueOf(500_000_000), EnergyValue.EnergyUnit.FE);
                break;
        }
    }
    
    /**
     * 切换转换模式
     */
    public void toggleMode() {
        ConversionMode[] modes = ConversionMode.values();
        int currentIndex = currentMode.ordinal();
        
        // 检�?AE2 是否安装
        boolean hasAE2 = isAE2Loaded();
        
        if (currentMode == ConversionMode.GE_TO_AE) {
            currentMode = ConversionMode.SE_TO_GE;
        } else if (currentMode == ConversionMode.GE_TO_FE && hasAE2) {
            currentMode = ConversionMode.GE_TO_AE;
        } else if (currentMode == ConversionMode.SE_TO_GE) {
            currentMode = ConversionMode.GE_TO_FE;
        } else {
            currentMode = ConversionMode.SE_TO_GE;
        }
        
        updateOutputRate();
        setChanged();
    }
    
    private boolean isAE2Loaded() {
        // 检�?AE2 是否安装
        return net.neoforged.fml.ModList.get().isLoaded("ae2");
    }
    
    /**
     * Tick 更新
     */
    public void tick() {
        tickCounter++;
        
        // �?tick 尝试输出能量
        if (tickCounter >= 1) {
            outputEnergy();
            tickCounter = 0;
        }
    }
    
    /**
     * 输出能量
     */
    private void outputEnergy() {
        if (cacheEnergy.isEmpty()) {
            return;
        }
        
        // 尝试向相邻方块输出能�?        for (var direction : net.minecraft.core.Direction.values()) {
            var neighborPos = worldPosition.relative(direction);
            var neighborState = level.getBlockState(neighborPos);
            var neighborEntity = level.getBlockEntity(neighborPos);
            
            if (neighborEntity instanceof EnergyStorageBlockEntity storage) {
                // 检查是否可以接�?                if (storage.canReceiveEnergy()) {
                    // 转换能量单位
                    EnergyValue toTransfer = convertEnergy(outputRatePerTick);
                    EnergyValue transferred = storage.receiveEnergy(toTransfer, false);
                    
                    if (!transferred.isEmpty()) {
                        cacheEnergy = cacheEnergy.subtract(convertFromOutput(transferred));
                        setChanged();
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * 将缓存能量转换为目标单位
     */
    private EnergyValue convertEnergy(EnergyValue amount) {
        BigInteger outputValue = amount.getValue(currentMode.outputUnit);
        return new EnergyValue(outputValue, currentMode.outputUnit);
    }
    
    /**
     * 从输出单位转换回缓存单位
     */
    private EnergyValue convertFromOutput(EnergyValue amount) {
        BigInteger inputValue = amount.getValue(currentMode.outputUnit)
                .multiply(currentMode.conversionRate);
        return new EnergyValue(inputValue, currentMode.inputUnit);
    }
    
    /**
     * 接收能量 (从输入侧)
     */
    public EnergyValue receiveEnergy(EnergyValue energy, boolean simulate) {
        // 检查是否是正确的输入单�?        if (energy.getValue(currentMode.inputUnit).equals(BigInteger.ZERO)) {
            return energy; // 单位不匹配，无法接收
        }
        
        EnergyValue canAccept = getCacheCapacity().subtract(cacheEnergy);
        if (canAccept.compareTo(energy) < 0) {
            energy = canAccept;
        }
        
        if (!simulate) {
            cacheEnergy = cacheEnergy.add(energy);
            setChanged();
        }
        
        return energy;
    }

    /**
     * 检查是否可以接收能量
     */
    public boolean canReceiveEnergy() {
        return !cacheEnergy.isFull(getCacheCapacity());
    }

    /**
     * 获取模式显示
     */
    public Component getModeDisplay() {
        return Component.literal("转换模式�? + currentMode.getName())
                .withStyle(ChatFormatting.GREEN);
    }
    
    public ConversionMode getCurrentMode() {
        return currentMode;
    }
    
    public EnergyValue getCacheEnergy() {
        return cacheEnergy;
    }
    
    public EnergyValue getCacheCapacity() {
        return new EnergyValue(CACHE_CAPACITY_SE, EnergyValue.EnergyUnit.SE);
    }
    
    public int getCachePercent() {
        BigInteger capacity = getCacheCapacity().getValueInFE();
        if (capacity.equals(BigInteger.ZERO)) {
            return 0;
        }
        return (int) (cacheEnergy.getValueInFE()
                .multiply(BigInteger.valueOf(100))
                .divide(capacity)
                .longValue());
    }
    
    /**
     * 获取输出速率显示
     */
    public Component getOutputRateDisplay() {
        String rate;
        switch (currentMode) {
            case SE_TO_GE:
                rate = "10000 GE/s";
                break;
            case GE_TO_FE:
            case GE_TO_AE:
                rate = "10000000000 FE/s";
                break;
            default:
                rate = "未知";
        }
        return Component.literal("输出速率�? + rate).withStyle(ChatFormatting.YELLOW);
    }
}
