# IEMS API 使用示例

## 简介

IEMS (综合能源管理系统) 提供了灵活的 API，允许其他 MOD 注册：
1. 核心方块/多方块结构到电网系统
2. 可连接到能源广播塔的设备

**Mod ID**: `IEMS`  
**Group ID**: `com.dlzstudio.iems`  
**Version**: `0.2.1`

---

## 能源系统架构

```
                    ┌─────────────┐
                    │   [核心]     │ ← 世界电力系统的中心
                    │  (其他 MOD)  │   管理整个世界的能源
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────┴────┐      ┌────┴────┐      ┌────┴────┐
    │ 中继器   │      │ 广播塔   │      │ 存储器   │
    │(标记连接)│      │(标记连接)│      │(储能备用)│
    └────┬────┘      └────┬────┘      └─────────┘
         │                 │
    其他 MOD 设备       其他 MOD 设备
```

**供电逻辑**:
1. **核心**直接供电给所有连接的设备
2. **核心**优先使用自身存储的能量
3. **核心**能量不足时，向存储器请求能源 (以总耗电功率×2)
4. **核心**能量有富余时，向存储器充能
5. **中继器/广播塔**本身没有能量缓存，仅作为连接标记

---

## 快速开始

### 添加依赖

在 `build.gradle` 中添加:

```gradle
dependencies {
    // 编译时依赖 IEMS API
    compileOnly fg.deobf("com.dlzstudio.iems:IEMS:1.0.0")
    // 或者运行时依赖
    runtimeOnly fg.deobf("com.dlzstudio.iems:IEMS:1.0.0")
}
```

---

## 第一部分：注册核心提供者

### 1. 注册核心提供者

```java
package com.example.mymod;

import com.dlzstudio.iems.energy.GridCoreRegistry;
import com.dlzstudio.iems.energy.EnergyValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MyModCore {
    
    public static void init() {
        registerCoreProvider();
    }
    
    private static void registerCoreProvider() {
        ResourceLocation CORE_ID = new ResourceLocation("mymod", "energy_core");
        
        GridCoreRegistry.register(CORE_ID, new GridCoreRegistry.CoreProvider() {
            
            @Override
            public boolean isValidCore(Level level, BlockPos pos) {
                // 检查该位置是否是你的核心方块
                return level.getBlockState(pos).getBlock() instanceof EnergyCoreBlock;
            }
            
            @Override
            public EnergyValue getEnergy(Level level, BlockPos pos) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    return core.getStoredEnergy();
                }
                return EnergyValue.zero();
            }
            
            @Override
            public void setEnergy(Level level, BlockPos pos, EnergyValue energy) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    core.setStoredEnergy(energy);
                }
            }
            
            @Override
            public EnergyValue getConsumption(Level level, BlockPos pos) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    return core.getConsumption();
                }
                return EnergyValue.zero();
            }
            
            @Override
            public void setConsumption(Level level, BlockPos pos, EnergyValue consumption) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    core.setConsumption(consumption);
                }
            }
            
            @Override
            public EnergyValue getGeneration(Level level, BlockPos pos) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    return core.getGeneration();
                }
                return EnergyValue.zero();
            }
            
            @Override
            public void setGeneration(Level level, BlockPos pos, EnergyValue generation) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    core.setGeneration(generation);
                }
            }
            
            @Override
            public void setDepleted(Level level, BlockPos pos, boolean depleted) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    core.setDepleted(depleted);
                }
            }
            
            @Override
            public boolean isDepleted(Level level, BlockPos pos) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof EnergyCoreBlockEntity core) {
                    return core.isDepleted();
                }
                return false;
            }
            
            @Override
            public EnergyValue getCapacity(Level level, BlockPos pos) {
                // 返回你的核心容量
                return new EnergyValue(java.math.BigInteger.TEN.pow(18), 
                                      EnergyValue.EnergyUnit.SE);
            }
        });
        
        IEMSMod.LOGGER.info("Registered energy core provider: {}", CORE_ID);
    }
}
```

### 2. 注册/注销核心到电网

```java
public class EnergyCoreBlock extends BaseEntityBlock {
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, 
                        BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // 注册到电网
            EnergyGrid.getInstance().registerCore(
                pos, 
                new ResourceLocation("mymod", "energy_core")
            );
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, 
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                // 从电网注销
                EnergyGrid.getInstance().unregisterCore(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
```

---

## 第二部分：注册可连接设备

### 注册设备到能源广播塔

能源广播塔会自动连接已注册的设备。在你的 MOD 初始化时注册：

```java
package com.example.mymod;

import com.dlzstudio.iems.blocks.entity.EnergyConnectionManager;
import net.minecraft.resources.ResourceLocation;

public class MyModDevices {
    
    public static void init() {
        // 方式 1: 注册单个设备
        EnergyConnectionManager.getInstance().registerAllowedDevice(
            "mymod",  // 你的 MOD ID
            "energy_storage_mk1"  // 设备名称 (方块 ID 的路径部分)
        );
        
        // 方式 2: 使用 ResourceLocation
        EnergyConnectionManager.getInstance().registerAllowedDevice(
            new ResourceLocation("mymod", "energy_storage_mk2")
        );
        
        // 方式 3: 注册多个设备
        registerAllDevices();
    }
    
    private static void registerAllDevices() {
        EnergyConnectionManager manager = EnergyConnectionManager.getInstance();
        
        // 注册你的 MOD 的所有能源设备
        manager.registerAllowedDevice("mymod", "basic_energy_storage");
        manager.registerAllowedDevice("mymod", "advanced_energy_storage");
        manager.registerAllowedDevice("mymod", "quantum_energy_storage");
        manager.registerAllowedDevice("mymod", "energy_converter");
        manager.registerAllowedDevice("mymod", "power_generator");
    }
}
```

### 在客户端/服务端初始化时调用

```java
@Mod("mymod")
public class MyMod {
    public MyMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        // 注册核心提供者
        MyModCore.init();
        
        // 注册可连接设备
        MyModDevices.init();
    }
}
```

---

## 第三部分：获取电网信息

```java
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.EnergyValue;

// 在任何地方获取电网信息
EnergyGrid grid = EnergyGrid.getInstance();

// 当前存储电量
String energy = grid.getEnergyDisplay();  // 例如："1000SE"

// 耗电功率
EnergyValue consumption = grid.getConsumption();

// 发电功率
EnergyValue generation = grid.getGeneration();

// 充满时间
String timeToFull = grid.getTimeToFull();  // 例如："5:30" 或 "∞"

// 是否耗尽
boolean depleted = grid.isDepleted();

// 核心容量
EnergyValue capacity = grid.getCoreCapacity();

// 活跃核心位置
BlockPos corePos = grid.getActiveCorePos();

// 核心是否正在向存储器充能
boolean isCharging = grid.isChargingStorages();
```

---

## 第四部分：能源系统架构说明

### 核心 ([Core])

- **作用**: 世界电力系统的中心，管理整个世界的能源
- **供电方式**: 直接向所有连接的设备供电
- **能量来源**: 
  - 自身存储的能量
  - 发电设备产生的能量
  - 核心缺电时，从存储器提取能量 (以耗电功率×2)
- **能量去向**:
  - 供给所有连接的设备
  - 能量富余时，向存储器充能

### 中继器/广播塔

- **作用**: 标记设备接入点，本身没有能量缓存
- **功能**:
  - 中继器：长距离有线连接 (500 格)
  - 广播塔：短距离无线连接 (50 格)
- **能量处理**: 不处理能量，仅作为连接标记

### 存储器

- **作用**: 电网的备用能源
- **充能**: 核心能量富余时，核心向存储器充能
- **放电**: 核心能量不足时，以耗电功率×2 向核心反向供电
- **不参与**: 不直接参与电网供电

---

## 注意事项

1. **线程安全**: 所有 API 调用都应在主线程进行
2. **世界保存**: 确保在你的方块实体 NBT 中保存能量数据
3. **注册时机**: 设备注册应在 MOD 初始化阶段完成
4. **容量单位**: 建议使用 SE 作为核心容量单位
5. **能量单位**: 内部计算使用 FE 作为基准单位

---

## 完整示例 MOD

```java
@Mod("examplemod")
public class ExampleMod {
    
    public static final String MODID = "examplemod";
    
    public ExampleMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        // 1. 注册核心提供者
        GridCoreRegistry.register(
            new ResourceLocation(MODID, "energy_core"),
            new GridCoreRegistry.CoreProvider() {
                // ... 实现所有方法
            }
        );
        
        // 2. 注册可连接设备
        EnergyConnectionManager manager = EnergyConnectionManager.getInstance();
        manager.registerAllowedDevice(MODID, "basic_energy_storage");
        manager.registerAllowedDevice(MODID, "advanced_energy_storage");
        manager.registerAllowedDevice(MODID, "energy_converter");
    }
}
```

---

**如有问题，请联系 DLZstudio 或提交 Issue**
