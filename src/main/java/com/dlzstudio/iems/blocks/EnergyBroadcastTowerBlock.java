package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class EnergyBroadcastTowerBlock extends Block implements EntityBlock {
    protected static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 20, 13);
    public static final int MAX_CONNECTION_DISTANCE = 50;
    public static final int AUTO_CONNECT_RADIUS = 50;

    public EnergyBroadcastTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyBroadcastTowerBlockEntity(IEMSBlocks.ENERGY_BROADCAST_TOWER_ENTITY.get(), pos, state);
    }
}
