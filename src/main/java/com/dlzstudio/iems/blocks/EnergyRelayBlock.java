package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyRelayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
 * 能源中继传输器方�? * 用于传输能源，最长可以拉�?500 �? */
public class EnergyRelayBlock extends BaseEntityBlock {
    
    protected static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);
    
    // 最大连接距�?    public static final int MAX_CONNECTION_DISTANCE = 500;
    
    public EnergyRelayBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyRelayBlockEntity(pos, state);
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
            if (blockEntity instanceof EnergyRelayBlockEntity relay) {
                // 检查是否按�?Shift
                if (player.isShiftKeyDown()) {
                    // 开�?取消 连接模式
                    relay.toggleConnectionMode(player);
                } else {
                    // 完成连接
                    relay.tryCompleteConnection(player);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return createTickerHelper(type, IEMSEntities.ENERGY_RELAY_ENTITY.get(),
            (level1, pos, state1, blockEntity) -> blockEntity.tick());
    }
    
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        // 可以添加实体碰撞逻辑
    }
}
