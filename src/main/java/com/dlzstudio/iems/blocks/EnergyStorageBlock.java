package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EnergyStorageBlock extends Block implements EntityBlock {
    protected static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);
    private final StorageType storageType;

    public enum StorageType { STANDARD, GENERAL }

    public EnergyStorageBlock(Properties properties, StorageType type) {
        super(properties);
        this.storageType = type;
    }

    public StorageType getStorageType() { return storageType; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorageBlockEntity(pos, state, storageType);
    }
}
