package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;
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
 * - energyStored: 存储的能量
 *
 * 渲染规则:
 * - 创造模式下手持木棍才显示贴图
 * - 作为绑定机器的一部分渲染
 *
 * 能源功能:
 * - 可以接收广播塔的能源
 * - 将能源传输给绑定的主机器
 */
public class EnergyMarkerBlockEntity extends BlockEntity {

    @Nullable
    private BlockPos controllerPos;
    private int markerType = 0;
    private String markerId = "";

    // 能源存储
    private EnergyValue energyStored = EnergyValue.zero();
    private EnergyValue capacity = new EnergyValue(1000, EnergyValue.EnergyUnit.FE);
    private EnergyValue maxReceive = new EnergyValue(100, EnergyValue.EnergyUnit.FE);
    private EnergyValue maxExtract = new EnergyValue(100, EnergyValue.EnergyUnit.FE);

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
        // 保存能量数据
        tag.putLong("energyStored", energyStored.getValueInFE().longValue());
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
        // 加载能量数据
        if (tag.contains("energyStored")) {
            this.energyStored = new EnergyValue(tag.getLong("energyStored"), EnergyValue.EnergyUnit.FE);
        }
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

    // ========== 能源功能 ==========

    /**
     * 接收能量 (从广播塔)
     */
    public EnergyValue receiveEnergy(EnergyValue energy, boolean simulate) {
        if (energyStored.isFull(capacity)) return EnergyValue.zero();

        EnergyValue space = capacity.subtract(energyStored);
        EnergyValue canAccept = space.min(energy).min(maxReceive);

        if (!simulate && !canAccept.isZero()) {
            energyStored = energyStored.add(canAccept);
            setChanged();

            // 将能量传输给主机器
            transferEnergyToController(canAccept);
        }
        return canAccept;
    }

    /**
     * 提取能量 (供主机器使用)
     */
    public EnergyValue extractEnergy(EnergyValue energy, boolean simulate) {
        if (energyStored.isZero()) return EnergyValue.zero();

        EnergyValue canExtract = energyStored.min(energy).min(maxExtract);

        if (!simulate && !canExtract.isZero()) {
            energyStored = energyStored.subtract(canExtract);
            setChanged();
        }
        return canExtract;
    }

    /**
     * 将能量传输给主机器
     */
    private void transferEnergyToController(EnergyValue energy) {
        if (controllerPos == null || level == null) return;

        BlockEntity controller = level.getBlockEntity(controllerPos);
        if (controller instanceof EnergyMarkerBlockEntity targetMarker) {
            // 如果目标也是标记方块，继续传输
            targetMarker.receiveEnergy(energy, false);
        } else if (controller != null) {
            // 传输给主机器 (由主机器自己处理接收)
            // 这里可以添加能量传输逻辑
        }
    }

    /**
     * 获取当前存储的能量
     */
    public EnergyValue getEnergyStored() {
        return energyStored;
    }

    /**
     * 获取能量容量
     */
    public EnergyValue getCapacity() {
        return capacity;
    }

    /**
     * 获取能量百分比
     */
    public int getEnergyPercentage() {
        if (capacity.isZero()) return 0;
        return (int) (energyStored.getValueInFE().doubleValue() / 
                      capacity.getValueInFE().doubleValue() * 100);
    }

    /**
     * 检查是否有能量可输出
     */
    public boolean hasEnergyToOutput() {
        return !energyStored.isZero();
    }

    /**
     * 检查是否可以接收能量
     */
    public boolean canReceiveEnergy() {
        return !energyStored.isFull(capacity);
    }

    /**
     * 获取主机方块实体
     */
    @Nullable
    public BlockEntity getControllerEntity() {
        if (controllerPos == null || level == null) return null;
        return level.getBlockEntity(controllerPos);
    }

    /**
     * tick - 处理能量传输
     */
    public void tick() {
        if (level == null || level.isClientSide) return;

        // 每 20 tick 检查一次连接
        if (level.getGameTime() % 20 == 0) {
            if (!isValid()) {
                // 主机器不存在，尝试重新绑定
                unbind();
            }
        }

        // 每 10 tick 传输一次能量
        if (level.getGameTime() % 10 == 0 && hasEnergyToOutput()) {
            transferEnergyToController(energyStored);
        }
    }

    /**
     * 标记方块可以接收广播塔能源
     * 广播塔会自动连接此方块
     */
    public boolean canReceiveFromBroadcastTower() {
        return true; // 所有标记方块都可以接收广播塔能源
    }
}
