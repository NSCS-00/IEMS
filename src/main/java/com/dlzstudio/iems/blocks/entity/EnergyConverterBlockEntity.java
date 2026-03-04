package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyValue;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigInteger;

public class EnergyConverterBlockEntity extends BlockEntity {
    
    public enum ConversionMode {
        SE_TO_GE("SE -> GE", EnergyValue.EnergyUnit.SE, EnergyValue.EnergyUnit.GE, BigInteger.valueOf(100_000_000)),
        GE_TO_FE("GE -> FE", EnergyValue.EnergyUnit.GE, EnergyValue.EnergyUnit.FE, new BigInteger("9000000000000000000")),
        GE_TO_AE("GE -> AE", EnergyValue.EnergyUnit.GE, EnergyValue.EnergyUnit.AE, new BigInteger("9000000000000000000"));
        
        public final String name;
        public final EnergyValue.EnergyUnit inputUnit;
        public final EnergyValue.EnergyUnit outputUnit;
        public final BigInteger conversionRate;
        
        ConversionMode(String name, EnergyValue.EnergyUnit inputUnit, EnergyValue.EnergyUnit outputUnit, BigInteger conversionRate) {
            this.name = name;
            this.inputUnit = inputUnit;
            this.outputUnit = outputUnit;
            this.conversionRate = conversionRate;
        }
        
        public String getName() { return name; }
    }
    
    private static final BigInteger CACHE_CAPACITY_SE = BigInteger.ONE;
    private EnergyValue cacheEnergy;
    
    private ConversionMode currentMode = ConversionMode.SE_TO_GE;
    private int tickCounter = 0;
    private EnergyValue outputRatePerTick;
    
    public EnergyConverterBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
        this.cacheEnergy = EnergyValue.zero();
        updateOutputRate();
    }
    
    private void updateOutputRate() {
        switch (currentMode) {
            case SE_TO_GE -> outputRatePerTick = new EnergyValue(BigInteger.valueOf(500), EnergyValue.EnergyUnit.GE);
            case GE_TO_FE -> outputRatePerTick = new EnergyValue(BigInteger.valueOf(500_000_000), EnergyValue.EnergyUnit.FE);
            case GE_TO_AE -> outputRatePerTick = new EnergyValue(BigInteger.valueOf(500_000_000), EnergyValue.EnergyUnit.AE);
        }
    }
    
    public void tick() {
        if (level == null || level.isClientSide) return;
        
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            convertEnergy();
        }
    }
    
    private void convertEnergy() {
        // 简化实现：仅显示转换状态
    }
    
    public void cycleMode() {
        currentMode = ConversionMode.values()[(currentMode.ordinal() + 1) % ConversionMode.values().length];
        updateOutputRate();
        setChanged();
    }
    
    public Component getModeText() {
        return Component.literal("转换模式：" + currentMode.getName()).withStyle(ChatFormatting.YELLOW);
    }
    
    public ConversionMode getCurrentMode() { return currentMode; }
    public EnergyValue getCacheEnergy() { return cacheEnergy; }
    public EnergyValue getOutputRate() { return outputRatePerTick; }
    
    public Component getOutputRateText() {
        return Component.literal("输出速率：" + outputRatePerTick.toString()).withStyle(ChatFormatting.YELLOW);
    }
}
