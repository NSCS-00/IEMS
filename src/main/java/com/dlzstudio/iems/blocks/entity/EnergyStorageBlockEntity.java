package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.EnergyStorageBlock;
import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.math.BigInteger;

public class EnergyStorageBlockEntity extends BlockEntity {

    private final EnergyStorageBlock.StorageType storageType;
    private EnergyValue energyStored;
    private EnergyValue capacity;
    private EnergyValue maxReceive;
    private EnergyValue maxExtract;

    private boolean canReceive = true;
    private boolean canExtract = true;

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state, EnergyStorageBlock.StorageType type) {
        super(null, pos, state);
        this.storageType = type;
        initCapacity();
        this.energyStored = EnergyValue.zero();
        this.maxReceive = new EnergyValue(capacity.getValueInFE().divide(BigInteger.valueOf(100)), EnergyValue.EnergyUnit.FE);
        this.maxExtract = new EnergyValue(capacity.getValueInFE().divide(BigInteger.valueOf(100)), EnergyValue.EnergyUnit.FE);
    }

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, EnergyStorageBlock.StorageType.STANDARD);
    }

    @Override
    public BlockEntityType<?> getType() {
        return IEMSBlocks.ENERGY_STORAGE_ENTITY.get();
    }

    private void initCapacity() {
        if (storageType == EnergyStorageBlock.StorageType.STANDARD) {
            this.capacity = new EnergyValue(BigInteger.valueOf(100_000), EnergyValue.EnergyUnit.SE);
        } else {
            this.capacity = new EnergyValue(new BigInteger("100000000000000000000"), EnergyValue.EnergyUnit.GE);
        }
    }

    public EnergyStorageBlock.StorageType getStorageType() {
        return storageType;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().registerStorage(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().unregisterStorage(worldPosition);
        }
        super.setRemoved();
    }

    public void tick() {
        if (level != null && level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    public EnergyValue receiveEnergy(EnergyValue energy, boolean simulate) {
        if (!canReceive) return EnergyValue.zero();
        
        EnergyValue space = capacity.subtract(energyStored);
        EnergyValue canAccept = space.min(energy).min(maxReceive);
        
        if (!simulate && !canAccept.isZero()) {
            energyStored = energyStored.add(canAccept);
            setChanged();
        }
        return canAccept;
    }

    public EnergyValue extractEnergy(EnergyValue energy, boolean simulate) {
        if (!canExtract || energyStored.isZero()) return EnergyValue.zero();
        
        EnergyValue canExtract = energyStored.min(energy).min(maxExtract);
        
        if (!simulate && !canExtract.isZero()) {
            energyStored = energyStored.subtract(canExtract);
            setChanged();
        }
        return canExtract;
    }

    public boolean hasEnergyToOutput() {
        return !energyStored.isZero() && canExtract;
    }

    public boolean canReceiveEnergy() {
        return canReceive && !energyStored.isFull(capacity);
    }

    public EnergyValue getEnergyStored() {
        return energyStored;
    }

    public EnergyValue getCapacity() {
        return capacity;
    }

    public int getEnergyPercentage() {
        if (capacity.isZero()) return 0;
        return energyStored.getValueInFE().multiply(BigInteger.valueOf(100))
            .divide(capacity.getValueInFE()).intValue();
    }

    public Component getDisplayName() {
        return Component.literal(storageType == EnergyStorageBlock.StorageType.STANDARD ? 
            "标准能量存储器" : "通用能量存储器");
    }
}
