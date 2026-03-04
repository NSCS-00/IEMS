package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EnergyMarkerBlockEntity extends BlockEntity {
    
    @Nullable
    private BlockPos controllerPos;
    private int markerType = 0;
    private String markerId = "";
    
    public EnergyMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putInt("controllerX", controllerPos.getX());
            tag.putInt("controllerY", controllerPos.getY());
            tag.putInt("controllerZ", controllerPos.getZ());
        }
        tag.putInt("markerType", markerType);
        tag.putString("markerId", markerId);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("controllerX")) {
            this.controllerPos = new BlockPos(
                tag.getInt("controllerX"),
                tag.getInt("controllerY"),
                tag.getInt("controllerZ")
            );
        }
        this.markerType = tag.getInt("markerType");
        this.markerId = tag.getString("markerId");
    }
    
    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }
    
    public void setControllerPos(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
        sync();
    }
    
    public int getMarkerType() {
        return markerType;
    }
    
    public void setMarkerType(int type) {
        this.markerType = type;
        setChanged();
        sync();
    }
    
    public String getMarkerId() {
        return markerId;
    }
    
    public void setMarkerId(String id) {
        this.markerId = id;
        setChanged();
        sync();
    }
    
    public void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public boolean isValid() {
        if (controllerPos == null || level == null) return false;
        BlockEntity controller = level.getBlockEntity(controllerPos);
        return controller != null && !controller.isRemoved();
    }
    
    public void unbind() {
        this.controllerPos = null;
        this.markerType = 0;
        this.markerId = "";
        setChanged();
        sync();
    }
}
