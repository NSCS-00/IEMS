package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyBroadcastTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 能源广播塔方�? * 短距离无线传输，可以自动连接半径 50 格内的设�? */
public class EnergyBroadcastTowerBlock extends BaseEntityBlock {
    
    protected static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 20, 13);
    
    // 最大连接距�?    public static final int MAX_CONNECTION_DISTANCE = 50;
    
    // 自动连接半径
    public static final int AUTO_CONNECT_RADIUS = 50;
    
    public EnergyBroadcastTowerBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyBroadcastTowerBlockEntity(pos, state);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyBroadcastTowerBlockEntity tower) {
                if (player.isShiftKeyDown()) {
                    // 开�?取消 连接模式
                    tower.toggleConnectionMode(player);
                } else {
                    // 完成连接
                    tower.tryCompleteConnection(player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return createTickerHelper(type, IEMSEntities.ENERGY_BROADCAST_TOWER_ENTITY.get(),
            (level1, pos, state1, blockEntity) -> blockEntity.tick());
    }
}
