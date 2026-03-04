package com.dlzstudio.iems.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 视线检查工具类
 */
public class SightCheckUtil {
    
    public static boolean hasLineOfSight(Level level, BlockPos start, BlockPos end) {
        if (level == null || start == null || end == null) return false;
        
        Vec3 startVec = new Vec3(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5);
        Vec3 endVec = new Vec3(end.getX() + 0.5, end.getY() + 0.5, end.getZ() + 0.5);
        
        return hasLineOfSight(level, startVec, endVec);
    }
    
    public static boolean hasLineOfSight(Level level, Vec3 start, Vec3 end) {
        if (level == null) return false;
        
        double stepSize = 0.1;
        double distance = start.distanceTo(end);
        int steps = (int) (distance / stepSize);
        
        Vec3 direction = end.subtract(start).normalize();
        
        int obstructionCount = 0;
        BlockPos lastBlockPos = null;
        boolean wasObstructed = false;
        
        for (int i = 0; i < steps; i++) {
            double t = i * stepSize;
            Vec3 currentPos = start.add(direction.scale(t));
            BlockPos currentBlockPos = new BlockPos(
                (int) Math.floor(currentPos.x),
                (int) Math.floor(currentPos.y),
                (int) Math.floor(currentPos.z)
            );
            
            if (currentBlockPos.equals(new BlockPos((int) Math.floor(start.x), (int) Math.floor(start.y), (int) Math.floor(start.z)))) {
                continue;
            }
            if (currentBlockPos.equals(new BlockPos((int) Math.floor(end.x), (int) Math.floor(end.y), (int) Math.floor(end.z)))) {
                continue;
            }
            
            BlockState state = level.getBlockState(currentBlockPos);
            
            if (isDeviceBlock(state)) {
                wasObstructed = false;
                continue;
            }
            
            boolean isObstructed = isObstruction(state);
            
            if (!currentBlockPos.equals(lastBlockPos)) {
                if (wasObstructed && !isObstructed) {
                    obstructionCount++;
                }
                lastBlockPos = currentBlockPos;
            }
            
            wasObstructed = isObstructed;
            
            if (obstructionCount >= 3) {
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean isObstruction(BlockState state) {
        if (state.isAir()) return false;
        if (isDeviceBlock(state)) return false;
        if (isStructureVoid(state)) return false;
        if (isLiquid(state)) return false;
        if (isTransparent(state)) return false;
        
        return isFullBlock(state);
    }
    
    public static boolean isDeviceBlock(BlockState state) {
        String blockName = state.getBlock().toString().toLowerCase();
        if (blockName.contains("energy_storage")) return true;
        if (blockName.contains("energy_converter")) return true;
        if (blockName.contains("energy_relay")) return true;
        if (blockName.contains("energy_broadcast")) return true;
        return false;
    }
    
    public static boolean isStructureVoid(BlockState state) {
        return state.getBlock().toString().toLowerCase().contains("structure_void");
    }
    
    public static boolean isTransparent(BlockState state) {
        String blockName = state.getBlock().toString().toLowerCase();
        if (blockName.contains("glass")) return true;
        if (blockName.contains("leaves")) return true;
        if (blockName.contains("iron_bars")) return true;
        if (blockName.contains("fence")) return true;
        if (blockName.contains("pane")) return true;
        return false;
    }
    
    public static boolean isLiquid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }
    
    public static boolean isFullBlock(BlockState state) {
        VoxelShape shape = state.getCollisionShape(null, null);
        if (shape.isEmpty()) return false;
        
        AABB bounds = shape.bounds();
        return bounds.minX <= 0.01 && bounds.maxX >= 0.99 &&
               bounds.minY <= 0.01 && bounds.maxY >= 0.99 &&
               bounds.minZ <= 0.01 && bounds.maxZ >= 0.99;
    }
    
    public static boolean hasTopGap(BlockState state) {
        VoxelShape shape = state.getCollisionShape(null, null);
        if (shape.isEmpty()) return false;
        AABB bounds = shape.bounds();
        return bounds.maxY < 0.5;
    }
    
    public static boolean hasBottomGap(BlockState state) {
        VoxelShape shape = state.getCollisionShape(null, null);
        if (shape.isEmpty()) return false;
        AABB bounds = shape.bounds();
        return bounds.minY > 0.5;
    }
    
    public static VoxelShape emptyShape() {
        return Shapes.empty();
    }
}
