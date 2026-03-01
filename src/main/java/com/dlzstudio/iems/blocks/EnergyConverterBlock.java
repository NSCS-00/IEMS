package com.dlzstudio.iems.blocks;

import com.dlzstudio.iems.blocks.entity.EnergyConverterBlockEntity;
import com.dlzstudio.iems.energy.EnergyValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 能量转换器方�? * 可以�?SE 转为 GE/GE 转为 FE/GE 转为 AE(如果安装了应用能�?
 * 缓存 1SE，输出速度稳定
 */
public class EnergyConverterBlock extends BaseEntityBlock {
    
    protected static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);
    
    public EnergyConverterBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyConverterBlockEntity(pos, state);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyConverterBlockEntity converter) {
                // 切换转换模式
                converter.toggleMode();
                player.displayClientMessage(converter.getModeDisplay(), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, 
                                                                   BlockEntityType<T> type) {
        return createTickerHelper(type, IEMSEntities.ENERGY_CONVERTER_ENTITY.get(),
            (level1, pos, state1, blockEntity) -> blockEntity.tick());
    }
}
