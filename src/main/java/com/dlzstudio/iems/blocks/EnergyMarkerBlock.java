package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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

/**
 * 能源标记方块
 * 
 * 特性:
 * - 创造模式下手持木棍才显示贴图
 * - 有碰撞箱 (完整方块)
 * - 通过 NBT 绑定到主机器
 * - 作为绑定机器的一部分
 */
public class EnergyMarkerBlock extends Block implements EntityBlock {
    
    // 完整方块碰撞箱
    protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);
    
    public EnergyMarkerBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE; // 有碰撞箱
    }
    
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, net.minecraft.core.Direction side) {
        return true;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof EnergyMarkerBlockEntity marker) {
                // 显示标记信息
                if (marker.getControllerPos() != null) {
                    BlockPos controllerPos = marker.getControllerPos();
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                            "标记方块 - 绑定到: [" + controllerPos.getX() + ", " + 
                            controllerPos.getY() + ", " + controllerPos.getZ() + "]"
                        ), true);
                } else {
                    player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("标记方块 - 未绑定"), true);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyMarkerBlockEntity(pos, state);
    }
}
