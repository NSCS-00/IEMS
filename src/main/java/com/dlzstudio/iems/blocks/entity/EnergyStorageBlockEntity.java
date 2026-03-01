package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.math.BigInteger;

/**
 * 能量存储方块实体 - 支持标准能量存储器和通用能量存储�? */
public class EnergyStorageBlockEntity extends BlockEntity {
    
    private final StorageType storageType;
    private EnergyValue energyStored;
    private EnergyValue capacity;
    private EnergyValue maxReceive;
    private EnergyValue maxExtract;
    
    private boolean canReceive = true;
    private boolean canExtract = true;
    
    public enum StorageType {
        // 标准能量存储器：10^5 SE
        STANDARD(BigInteger.valueOf(100_000), EnergyValue.EnergyUnit.SE),
        // 通用能量存储器：10^20 GE
        GENERAL(new BigInteger("100000000000000000000"), EnergyValue.EnergyUnit.GE);
        
        public final BigInteger capacityValue;
        public final EnergyValue.EnergyUnit unit;
        
        StorageType(BigInteger capacityValue, EnergyValue.EnergyUnit unit) {
            this.capacityValue = capacityValue;
            this.unit = unit;
        }
        
        public EnergyValue getCapacity() {
            return new EnergyValue(capacityValue, unit);
        }
    }
    
    public EnergyStorageBlockEntity(BlockPos pos, BlockState state, StorageType type) {
        super(IEMSEntities.ENERGY_STORAGE_ENTITY.get(), pos, state);
        this.storageType = type;
        this.capacity = type.getCapacity();
        this.energyStored = EnergyValue.zero();
        // 最大传输速率：容量的 1%
        this.maxReceive = new EnergyValue(capacity.getValueInFE().divide(BigInteger.valueOf(100)));
        this.maxExtract = new EnergyValue(capacity.getValueInFE().divide(BigInteger.valueOf(100)));
    }
    
    public StorageType getStorageType() {
        return storageType;
    }
    
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        EnergyGrid.getInstance().unregisterStorage(this);
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        EnergyGrid.getInstance().unregisterStorage(this);
    }
    
    /**
     * 初始化存储设�?     */
    public void initialize() {
        EnergyGrid.getInstance().registerStorage(this);
    }
    
    /**
     * 接收能量
     */
    public EnergyValue receiveEnergy(EnergyValue maxReceive, boolean simulate) {
        if (!canReceive()) {
            return EnergyValue.zero();
        }
        
        EnergyValue canAccept = capacity.subtract(energyStored);
        if (canAccept.compareTo(maxReceive) < 0) {
            maxReceive = canAccept;
        }
        
        if (!simulate) {
            energyStored = energyStored.add(maxReceive);
            setChanged();
        }
        
        return maxReceive;
    }
    
    /**
     * 提取能量
     */
    public EnergyValue extractEnergy(EnergyValue maxExtract, boolean simulate) {
        if (!canExtract()) {
            return EnergyValue.zero();
        }
        
        if (energyStored.compareTo(maxExtract) < 0) {
            maxExtract = energyStored;
        }
        
        if (!simulate) {
            energyStored = energyStored.subtract(maxExtract);
            setChanged();
        }
        
        return maxExtract;
    }
    
    /**
     * 获取能量请求 (耗电设备使用)
     */
    public EnergyValue getEnergyRequest() {
        if (canReceiveEnergy()) {
            return maxReceive;
        }
        return EnergyValue.zero();
    }
    
    /**
     * 获取能量输出 (发电设备使用)
     */
    public EnergyValue getEnergyOutput() {
        if (canProvideEnergy()) {
            return maxExtract;
        }
        return EnergyValue.zero();
    }
    
    /**
     * 检查是否有能量可输�?     */
    public boolean hasEnergyToOutput() {
        return !energyStored.isEmpty() && canExtract;
    }
    
    /**
     * 检查是否可以接收能�?     */
    public boolean canReceiveEnergy() {
        return canReceive && capacity.subtract(energyStored).compareTo(EnergyValue.zero()) > 0;
    }
    
    /**
     * 检查是否可以提供能�?     */
    public boolean canProvideEnergy() {
        return canExtract && !energyStored.isEmpty();
    }
    
    public EnergyValue getEnergyStored() {
        return energyStored;
    }
    
    public EnergyValue getCapacity() {
        return capacity;
    }
    
    public boolean canReceive() {
        return canReceive;
    }
    
    public boolean canExtract() {
        return canExtract;
    }
    
    /**
     * 设置能量�?     */
    public void setEnergy(EnergyValue energy) {
        this.energyStored = energy;
        setChanged();
    }
    
    /**
     * 获取能量百分�?     */
    public int getEnergyPercent() {
        if (capacity.getValueInFE().equals(BigInteger.ZERO)) {
            return 0;
        }
        return (int) (energyStored.getValueInFE()
                .multiply(BigInteger.valueOf(100))
                .divide(capacity.getValueInFE())
                .longValue());
    }
}
