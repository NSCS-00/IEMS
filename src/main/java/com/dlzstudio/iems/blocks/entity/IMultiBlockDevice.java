package com.dlzstudio.iems.blocks.entity;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * 多方块设备接口
 * 
 * 其他 MOD 的设备可以实现此接口来声明多方块结构的占用空间
 * 
 * 示例:
 * ```java
 * public class MyMultiBlockEntity extends BlockEntity implements IMultiBlockDevice {
 *     @Override
 *     public List<BlockPos> getOccupiedOffsets() {
 *         // 3x3x2 结构，以本体为中心
 *         List<BlockPos> offsets = new ArrayList<>();
 *         for (int x = -1; x <= 1; x++) {
 *             for (int z = -1; z <= 1; z++) {
 *                 for (int y = 0; y <= 1; y++) {
 *                     offsets.add(new BlockPos(x, y, z));
 *                 }
 *             }
 *         }
 *         return offsets;
 *     }
 * }
 * ```
 */
public interface IMultiBlockDevice {
    
    /**
     * 获取多方块结构占用的所有方块位置 (相对于设备本体的偏移)
     * 
     * @return 偏移位置列表
     * 
     * 示例 (3x3x2 结构，以本体为中心):
     * ```
     * 返回的偏移:
     * (-1, 0, -1), (0, 0, -1), (1, 0, -1)
     * (-1, 0,  0), (0, 0,  0), (1, 0,  0)  ← 本体在 (0, 0, 0)
     * (-1, 0,  1), (0, 0,  1), (1, 0,  1)
     * 
     * (-1, 1, -1), (0, 1, -1), (1, 1, -1)
     * (-1, 1,  0), (0, 1,  0), (1, 1,  0)
     * (-1, 1,  1), (0, 1,  1), (1, 1,  1)
     * ```
     */
    List<BlockPos> getOccupiedOffsets();
}
