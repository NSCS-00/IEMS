package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
    public BlockEntityType<?> getType() {
        return IEMSBlocks.ENERGY_MARKER_ENTITY.get();
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
    }

    public int getMarkerType() {
        return markerType;
    }

    public void setMarkerType(int type) {
        this.markerType = type;
        setChanged();
    }

    public String getMarkerId() {
        return markerId;
    }

    public void setMarkerId(String id) {
        this.markerId = id;
        setChanged();
    }

    public boolean shouldRender(Player player) {
        if (level == null || level.isClientSide) return false;
        if (!player.isCreative()) return false;
        
        var mainHandItem = player.getMainHandItem();
        var offHandItem = player.getOffhandItem();
        
        return mainHandItem.is(Items.STICK) || offHandItem.is(Items.STICK);
    }

    public boolean isValid() {
        if (controllerPos == null || level == null) return false;
        return level.getBlockEntity(controllerPos) != null;
    }

    public void unbind() {
        this.controllerPos = null;
        this.markerType = 0;
        this.markerId = "";
        setChanged();
    }

    @Nullable
    public BlockEntity getControllerEntity() {
        if (controllerPos == null || level == null) return null;
        return level.getBlockEntity(controllerPos);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            if (!isValid()) {
                unbind();
            }
        }
    }

    public boolean canReceiveFromBroadcastTower() {
        return true;
    }
}
