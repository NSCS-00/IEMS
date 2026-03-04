package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 能源标记方块实体
 * 
 * NBT 数据:
 * - controllerPos: 主机器位置
 * - markerType: 标记类型 (0=边界，1=连接点，2=输入，3=输出)
 * - markerId: 标记 ID(用于区分同一机器的多个标记)
 * 
 * 渲染规则:
 * - 创造模式下手持木棍才显示贴图
 * - 作为绑定机器的一部分渲染
 */
public class EnergyMarkerBlockEntity extends BlockEntity {
    
    @Nullable
    private BlockPos controllerPos;
    private int markerType = 0;
    private String markerId = "";
    
    public EnergyMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(IEMSBlocks.ENERGY_MARKER_ENTITY.get(), pos, state);
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
    
    /**
     * 检查玩家是否应该看到标记方块
     * 创造模式下手持木棍才显示
     */
    public boolean shouldRender(Player player) {
        if (level == null || level.isClientSide) {
            return false;
        }
        
        // 检查是否是创造模式
        if (!player.isCreative()) {
            return false;
        }
        
        // 检查是否手持木棍
        var mainHandItem = player.getMainHandItem();
        var offHandItem = player.getOffhandItem();
        
        return mainHandItem.is(Items.STICK) || offHandItem.is(Items.STICK);
    }
    
    /**
     * 同步到客户端
     */
    public void sync() {
        if (level != null && !level.isClientSide) {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            level.getChunkSource().broadcast(this, packet);
        }
    }
    
    /**
     * 检查标记是否有效（主机器是否存在）
     */
    public boolean isValid() {
        if (controllerPos == null || level == null) return false;
        BlockEntity controller = level.getBlockEntity(controllerPos);
        return controller != null && !controller.isRemoved();
    }
    
    /**
     * 移除绑定
     */
    public void unbind() {
        this.controllerPos = null;
        this.markerType = 0;
        this.markerId = "";
        setChanged();
        sync();
    }
}
