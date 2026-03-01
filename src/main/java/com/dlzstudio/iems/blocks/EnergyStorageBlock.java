package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 能量存储方块基类
 */
public class EnergyStorageBlock extends BaseEntityBlock {
    
    protected static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);
    
    private final StorageType storageType;
    
    public enum StorageType {
        STANDARD,
        GENERAL
    }
    
    public EnergyStorageBlock(Properties properties, StorageType type) {
        super(properties);
        this.storageType = type;
    }
    
    public StorageType getStorageType() {
        return storageType;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state, 
            storageType == StorageType.STANDARD ? 
                EnergyStorageBlockEntity.StorageType.STANDARD : 
                EnergyStorageBlockEntity.StorageType.GENERAL);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, 
                        BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyStorageBlockEntity storage) {
                storage.initialize();
            }
        }
    }
    
    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, 
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof EnergyStorageBlockEntity storage) {
                    storage.setRemoved();
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, 
                                                                   BlockState state, 
                                                                   BlockEntityType<T> type) {
        return createTickerHelper(type, IEMSEntities.ENERGY_STORAGE_ENTITY.get(),
            (level1, pos, state1, blockEntity) -> {});
    }
}
