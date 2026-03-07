package com.dlzstudio.iems.blocks.entity;

import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.energy.EnergyGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 广播塔方块实体 (带 GeoLib 动画支持)
 */
public class EnergyBroadcastTowerBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean isInConnectionMode = false;
    private UUID connectionId;
    private BlockPos connectedPos;

    private final Set<BlockPos> autoConnectedDevices = new HashSet<>();
    private BlockPos signalSourcePos;

    public EnergyBroadcastTowerBlockEntity(BlockPos pos, BlockState state) {
        super(null, pos, state);
        updateSignalSourcePos();
    }

    public EnergyBroadcastTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        updateSignalSourcePos();
    }

    @Override
    public BlockEntityType<?> getType() {
        return IEMSBlocks.ENERGY_BROADCAST_TOWER_ENTITY.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 暂无动画
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void updateSignalSourcePos() {
        if (worldPosition != null) {
            this.signalSourcePos = worldPosition.above(3);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateSignalSourcePos();
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().registerBroadcastTower(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            EnergyGrid.getInstance().unregisterBroadcastTower(worldPosition);
            autoConnectedDevices.clear();
        }
        super.setRemoved();
    }

    public BlockPos getSignalSourcePos() {
        if (signalSourcePos == null) {
            updateSignalSourcePos();
        }
        return signalSourcePos;
    }

    public void toggleConnectionMode(Player player) {
        if (isInConnectionMode) {
            isInConnectionMode = false;
            connectionId = null;
            player.displayClientMessage(Component.literal("已取消连接模式").withStyle(ChatFormatting.RED), true);
        } else {
            isInConnectionMode = true;
            connectionId = UUID.randomUUID();
            player.displayClientMessage(Component.literal("连接模式开启，对准目标右键").withStyle(ChatFormatting.GREEN), true);
        }
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (connectedPos != null) {
            BlockEntity targetEntity = level.getBlockEntity(connectedPos);
            if (targetEntity == null || targetEntity.isRemoved()) {
                connectedPos = null;
                setChanged();
            }
        }
    }

    public boolean isInConnectionMode() { return isInConnectionMode; }

    public boolean isConnectedToCore() {
        return EnergyGrid.getInstance().isConnectedToCore(worldPosition);
    }

    public Set<BlockPos> getAutoConnectedDevices() {
        return new HashSet<>(autoConnectedDevices);
    }
}
