# 综合能源管理系统

**Integrated Energy Management System (IEMS)**

---

## 基本信息

| 项目 | 内容 |
|------|------|
| **模组名称** | 综合能源管理系统 |
| **英文名称** | Integrated Energy Management System |
| **缩写** | IEMS |
| **版本** | 0.6.0 |
| **开发团队** | 等离子工作室 (DLZstudio) |
| **适用版本** | Minecraft 1.21.1 NeoForge |
| **Mod ID** | `IEMS` |
| **Group ID** | `com.dlzstudio.iems` |

---

## 模组简介

综合能源管理系统是一个为大型整合包设计的高精度能源管理模组，提供：

- **高精度能源单位**：SE (标准能量) 和 GE (通用能量)
- **能源管理方块**：存储器、转换器、中继器、广播塔
- **Web 监控界面**：浏览器实时查看电网状态
- **核心唯一性系统**：确保世界中只有一个能源核心

> **注意**：本模组不提供核心方块，核心需由其他 MOD 通过 API 注册。

---

## 核心唯一性原则

### 基本原则

**世界中只能有一个核心！**

核心是整个世界电力系统的中心，所有能源设备都必须连接到核心才能工作。

### 多 MOD 冲突处理

当多个 MOD 同时注册核心时，按以下规则处理：

```
优先级规则:
┌─────────────────────────────────────┐
│ 1. 等离子工作室提供的 MOD 优先       │
│    - 仅有一个：直接使用该核心       │
│    - 有多个：选择第一个注册的       │
│                                     │
│ 2. 没有等离子工作室 MOD             │
│    - 按加载顺序选择第一个           │
└─────────────────────────────────────┘
```

### 错误处理机制

| 情况 | 处理方式 | 日志等级 |
|------|---------|---------|
| 尝试放置第二个核心 | 阻止操作 | `[IEMS 错误]` |
| 多个 MOD 同时注册 | 选择一个，其他忽略 | `[IEMS 警告]` |
| 世界中出现多个核心 | 游戏崩溃 | `[IEMS 致命错误]` |

### 日志格式

所有日志使用统一格式：`[IEMS 错误等级] 消息内容`

| 等级 | 前缀 | 说明 |
|------|------|------|
| 信息 | `[IEMS 信息]` | 正常运行信息 |
| 警告 | `[IEMS 警告]` | 需要注意的问题 |
| 错误 | `[IEMS 错误]` | 操作失败或冲突 |
| 致命错误 | `[IEMS 致命错误]` | 导致游戏崩溃的严重错误 |

### 日志示例

**核心冲突处理:**
```
[IEMS 警告] 多核心冲突
[IEMS 警告] 核心 mymod:core 被选中
[IEMS 警告] 核心 othermodA:core 被忽略
[IEMS 警告] 核心 othermodB:core 被忽略
```

**多次放置核心:**
```
[IEMS 错误] 检测到多个 IEMS-CC 核心！
[IEMS 错误] 当前活跃 IEMS-CC 核心位置：[100, 64, 100]
[IEMS 错误] 新 IEMS-CC 核心位置：[200, 64, 200]
[IEMS 错误] 新核心激活被拒绝
```

**多个核心导致崩溃:**
```
[IEMS 致命错误] 检测到多个活跃的 IEMS-CC 核心！
[IEMS 致命错误] 位置：[100, 64, 100] [200, 64, 200] [300, 64, 300]
[IEMS 致命错误] 这违反了核心唯一性原则，已强制终止加载
```

---

## 能源单位

### 单位换算

```
能源单位层级 (从高到低):

  SE (标准能量单位)
   │
   ├─ 1 SE = 100,000,000 GE
   │
   ▼
  GE (通用能量单位)
   │
   ├─ 1 GE = 9,000,000,000,000,000,000 FE (9×10^18)
   │
   ▼
  FE (Forge Energy)
```

### 换算公式

| 转换 | 公式 |
|------|------|
| SE → GE | 1 SE = 10^8 GE |
| GE → FE | 1 GE = 9×10^18 FE |
| SE → FE | 1 SE = 9×10^26 FE |

### 精度说明

- SE 和 GE 使用 **BigInteger** 存储
- 显示和存储时保留整数
- 小数部分 **向下取整**

---

## 能源系统架构

### 系统结构

```
                        ┌─────────────────┐
                        │     [核心]       │
                        │   (其他 MOD)     │
                        │  世界能源中心    │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
       ┌──────┴──────┐    ┌──────┴──────┐   ┌──────┴──────┐
       │   中继器     │    │   广播塔     │   │   存储器     │
       │  (标记连接)  │    │  (标记连接)  │   │  (储能备用)  │
       └──────┬──────┘    └──────┬──────┘   └─────────────┘
              │                  │
       其他 MOD 设备        其他 MOD 设备
```

### 供电逻辑

```
┌─────────────────────────────────────────────────────────┐
│ 1. 核心直接供电给所有连接的设备                           │
│ 2. 核心优先使用自身存储的能量                             │
│ 3. 核心能量不足时，向存储器请求能源 (耗电功率×2)           │
│ 4. 核心能量富余时，向存储器充能                           │
│ 5. 中继器/广播塔无能量缓存，仅标记连接                    │
└─────────────────────────────────────────────────────────┘
```

### 连接规则

**重要**: 中继器和广播塔必须与核心有直接或间接连接才能工作！

```
有效连接:                          无效连接:

核心 ──────→ 广播塔 ✓              广播塔 (未连接) ✗
                                   │
核心 ──────→ 中继器 ──────→ 广播塔 ✓  广播塔 ──→ 中继器 (未连核心) ✗
                                   │
核心 ──────→ 中继器 ──→ 中继器 ──→ 广播塔 ✓  中继器 ──→ 中继器 (未连核心) ✗
```

**未连接核心的后果:**
- 广播塔不会工作
- 扫描的设备不会接入电网
- 核心不知道这些设备存在

---

## 新增方块

### 1. 标准能量存储器

| 属性 | 内容 |
|------|------|
| **英文名称** | Standard Energy Storage |
| **容量** | 10^5 SE |
| **渲染** | 1x1x1 方块贴图 |
| **获取** | 创造模式 |
| **功能** | 电网备用能源 |

**工作原理:**
- 不参与直接供电
- 核心富余时充能
- 核心缺电时反向供电 (耗电×2 功率)

### 2. 通用能量存储器

| 属性 | 内容 |
|------|------|
| **英文名称** | General Energy Storage |
| **容量** | 10^20 GE |
| **渲染** | 1x1x1 方块贴图 |
| **获取** | 创造模式 |
| **功能** | 电网备用能源 |

### 3. 能量转换器

| 属性 | 内容 |
|------|------|
| **英文名称** | Energy Converter |
| **渲染** | 1x1x1 方块贴图 |
| **缓存** | 1 SE |
| **功能** | 能源单位转换 |

**转换模式:**
| 模式 | 转换 | 速率 |
|------|------|------|
| SE→GE | 1 SE = 10^8 GE | 1 SE/s |
| GE→FE | 1 GE = 9×10^18 FE | 10,000 GE/s |
| GE→AE | 1 GE = 9×10^18 AE | 10,000 GE/s |

> **注意**: 不能直接 SE→FE/AE，会整型溢出！

### 4. 能源中继传输器

| 属性 | 内容 |
|------|------|
| **英文名称** | Energy Relay Transmitter |
| **最大距离** | 500 格 |
| **渲染** | MMD 模型 |
| **功能** | 标记广播塔接入点 |

**使用方法:**
1. 按住 Shift+ 右键 开始连接模式
2. 对准目标右键完成连接
3. 黄色激光显示连接

### 5. 能源广播塔

| 属性 | 内容 |
|------|------|
| **英文名称** | Energy Broadcast Tower |
| **连接距离** | 50 格 |
| **自动半径** | 50 格 |
| **渲染** | MMD 模型 |
| **功能** | 无线能源传输标记 |
| **信号源位置** | 本体向上 3 格 |

**特性:**
- 自动连接 50 格内设备
- 可手动连接其他设备
- **只能向平行/下方的设备传输**
- **视线检查：设备上方/侧面被完整方块阻挡 3 层则无法连接**
- **必须连接核心才能工作**
- **无法自动连接其它广播塔和中继器**

**传输规则:**
```
信号源位置：广播塔本体向上 3 格

扫描区域：以信号源为中心，50 格为半径的朝下半圆

视线检查:
- 设备任意一个方块没有被完全遮挡即可通电

### 6. 能源标记方块 (v0.6.2 新增)

| 属性 | 内容 |
|------|------|
| **英文名称** | Energy Marker Block |
| **渲染** | 结构空位贴图 (半透明网格) |
| **碰撞** | 无碰撞箱 |
| **获取** | 仅代码放置 |
| **功能** | 多方块结构标记 |

**特性:**
- 使用结构空位贴图（半透明网格）
- 无碰撞箱，不阻挡玩家移动
- 不参与视线遮挡计算
- 可通过 NBT 绑定到主机器
- 由其他 MOD 调用放置和绑定
- 不在创造模式标签页显示

**标记类型:**
| ID | 类型 | 说明 |
|----|------|------|
| 0 | 边界标记 | 定义机器边界 |
| 1 | 连接点标记 | 定义连接点位置 |
| 2 | 能量输入标记 | 定义能量输入点 |
| 3 | 能量输出标记 | 定义能量输出点 |

**使用示例:**
```java
// 放置标记方块
BlockPos markerPos = machinePos.offset(1, 0, 0);
level.setBlock(markerPos, 
    IEMSBlocks.ENERGY_MARKER.get().defaultBlockState(), 
    3);

// 绑定到主机器
BlockEntity be = level.getBlockEntity(markerPos);
if (be instanceof EnergyMarkerBlockEntity marker) {
    marker.setControllerPos(machinePos);  // 绑定位置
    marker.setMarkerType(0);              // 边界标记
    marker.setMarkerId("corner_1");       // 标记 ID
}
```
- 不完整/透明方块不算遮挡
- 完整方块阻挡≥3 层才算完全遮挡
- 如果 MOD 没有提供设备大小，默认检查本体方块
- 手动拉电线不受视线限制

示例 (3x3x2 多方块结构):
    ┌───────────────┐
    │ 占位 占位 占位 │  ← 这些被遮挡没关系
    │ 占位 本体 占位 │  ← 只要本体没被遮挡即可通电
    └───────────────┘
```

**多方块结构支持 (v0.6.2+):**

其他 MOD 可以使用能源标记方块来定义多方块结构的占用空间：

```java
import com.dlzstudio.iems.blocks.IEMSBlocks;
import com.dlzstudio.iems.blocks.entity.EnergyMarkerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

// 在你的多方块机器放置时调用
public void placeMarkers(Level level, BlockPos machinePos) {
    // 3x3x2 结构示例
    for (int x = -1; x <= 1; x++) {
        for (int z = -1; z <= 1; z++) {
            for (int y = 0; y <= 1; y++) {
                if (x == 0 && z == 0 && y == 0) continue; // 跳过机器本体
                
                BlockPos markerPos = machinePos.offset(x, y, z);
                
                // 放置标记方块
                level.setBlock(markerPos, 
                    IEMSBlocks.ENERGY_MARKER.get().defaultBlockState(), 
                    3);
                
                // 绑定到主机器
                BlockEntity be = level.getBlockEntity(markerPos);
                if (be instanceof EnergyMarkerBlockEntity marker) {
                    marker.setControllerPos(machinePos);
                    marker.setMarkerType(0); // 0=边界标记
                    marker.setMarkerId("multiblock_" + machinePos);
                }
            }
        }
    }
}
```

**标记类型**:
| ID | 类型 | 说明 |
|----|------|------|
| 0 | 边界标记 | 定义机器边界 |
| 1 | 连接点标记 | 定义连接点位置 |
| 2 | 能量输入标记 | 定义能量输入点 |
| 3 | 能量输出标记 | 定义能量输出点 |

**标记方块特性**:
- 使用结构空位贴图（半透明网格，仅在调试模式可见）
- 无碰撞箱，不阻挡玩家移动
- 不参与视线遮挡计算
- 可通过 NBT 绑定到主机器
- 由其他 MOD 调用放置和绑定

---

## Web 监控界面

### 访问方式

启动游戏并加载世界后，打开浏览器访问：

```
http://localhost:8080
```

### 界面功能

| 功能 | 说明 |
|------|------|
| 电网状态 | 显示正常运行/已耗尽 |
| 当前电量 | 核心存储的总能量 |
| 能量消耗 | 整个电网的总耗电功率 |
| 能量产出 | 整个电网的总发电功率 |
| 充满时间 | 预计核心充满所需时间 |
| 网络连接 | 核心位置和连接设备数量 |
| **设备管理** | **查看所有已连接设备，可开关供能** |

### 设备管理 (v0.6.0 新增)

在 Web 界面底部可以查看所有已连接设备列表，每个设备显示：
- 设备名称
- 设备位置坐标
- 供能状态 (供电中/已禁用)
- 供能开关 (可点击切换)

**使用方式:**
1. 滚动到页面底部的"已连接设备"区域
2. 查看设备列表
3. 点击设备右侧的开关按钮切换供能状态
4. 关闭供能的设备将不再消耗电网能源

### 自动刷新

界面每 **5 秒** 自动刷新一次。

### JSON API

**端点:** `GET http://localhost:8080/api/status`

**返回示例:**
```json
{
  "energy": "1000SE",
  "consumption": "100SE/tick",
  "generation": "150SE/tick",
  "timeToFull": "5:30",
  "depleted": false
}
```

---

## 核心命名

### 默认名称

**IEMS-CC** (Integrated Energy Management System - Central Core)

中文：综合能源管理系统 - 中枢核心

### 自定义名称

其他 MOD 可以为核心命名：

```java
// 在 MOD 初始化时调用
GridCoreRegistry.setCoreName("YourMod-Core");
```

### 名称使用

核心名称用于：
- 所有日志信息
- 错误提示
- Web 界面显示

---

## API 使用指南

### 注册核心提供者

```java
package com.example.mymod;

import com.dlzstudio.iems.energy.GridCoreRegistry;
import com.dlzstudio.iems.energy.EnergyValue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MyModCore {
    public static void init() {
        // 设置核心名称 (可选)
        GridCoreRegistry.setCoreName("MyMod-CentralCore");
        
        // 注册核心提供者
        GridCoreRegistry.register(
            new ResourceLocation("mymod", "core"),
            new GridCoreRegistry.CoreProvider() {
                @Override
                public boolean isValidCore(Level level, BlockPos pos) {
                    return level.getBlockState(pos).getBlock() == MyModBlocks.CORE.get();
                }
                
                @Override
                public EnergyValue getEnergy(Level level, BlockPos pos) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof MyCoreBlockEntity core) {
                        return core.getEnergy();
                    }
                    return EnergyValue.zero();
                }
                
                @Override
                public void setEnergy(Level level, BlockPos pos, EnergyValue energy) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof MyCoreBlockEntity core) {
                        core.setEnergy(energy);
                    }
                }
                
                // 实现其他必需方法...
            }
        );
    }
}
```

### 注册核心到电网

```java
import com.dlzstudio.iems.energy.EnergyGrid;
import com.dlzstudio.iems.energy.GridCoreRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

// 核心方块放置时
public void onCorePlace(BlockPos pos) {
    // 检查是否可以放置
    if (!GridCoreRegistry.canPlaceNewCore(pos)) {
        return; // 世界中已有核心
    }
    
    // 尝试激活核心
    ResourceLocation providerId = new ResourceLocation("mymod", "core");
    if (GridCoreRegistry.tryActivateCore(level, pos, providerId)) {
        EnergyGrid.getInstance().registerCore(pos, providerId);
    }
}

// 核心方块破坏时
public void onCoreBreak(BlockPos pos) {
    GridCoreRegistry.unregisterCore(pos);
    EnergyGrid.getInstance().unregisterCore(pos);
}
```

### 注册可连接设备

```java
import com.dlzstudio.iems.blocks.entity.EnergyConnectionManager;

public class MyModDevices {
    public static void init() {
        EnergyConnectionManager manager = EnergyConnectionManager.getInstance();
        
        // 注册设备
        manager.registerAllowedDevice("mymod", "energy_storage_mk1");
        manager.registerAllowedDevice("mymod", "energy_storage_mk2");
        manager.registerAllowedDevice("mymod", "energy_converter");
    }
}
```

### 获取电网信息

```java
import com.dlzstudio.iems.energy.EnergyGrid;

EnergyGrid grid = EnergyGrid.getInstance();

// 获取信息
String energy = grid.getEnergyDisplay();           // 当前电量
String consumption = grid.getConsumption().toString();  // 能量消耗
String generation = grid.getGeneration().toString();    // 能量产出
String timeToFull = grid.getTimeToFull();          // 充满时间
boolean depleted = grid.isDepleted();              // 是否耗尽
```

---

## 安装说明

### 依赖要求

| 依赖 | 版本 | 必需 |
|------|------|------|
| Minecraft | 1.21.1 | ✓ |
| NeoForge | 21.1.74+ | ✓ |
| Applied Energistics 2 | 19.0.0+ | 可选 (AE 转换) |

### 安装步骤

1. 安装 Minecraft 1.21.1
2. 安装 NeoForge 21.1.74 或更高版本
3. 将 `IEMS-0.5.0.jar` 放入 `mods` 文件夹
4. 启动游戏并加载世界
5. 打开浏览器访问 `http://localhost:8080`

---

## 常见问题

### Q: 为什么不能直接将 SE 转换为 FE？

**A:** 因为 1 SE = 9×10^26 FE，超过了 long 类型的最大值 (2^63-1 ≈ 9.22×10^18)，会导致整型溢出。必须通过 GE 中转。

### Q: 电网耗尽时会发生什么？

**A:** 电网会以耗电功率×2 的功率向所有存储器请求能源，同时 Web 界面显示红色警告。

### Q: 如何连接设备？

**A:** 对着中继器/广播塔按 Shift+ 右键开始连接模式，然后对准目标右键完成连接。

### Q: 为什么我的广播塔不工作？

**A:** 检查广播塔是否通过中继器或直接与核心连接。未连接核心的广播塔不会工作。

### Q: Web 界面无法访问？

**A:** 
1. 确保服务器已启动
2. 检查端口 8080 是否被占用
3. 检查防火墙设置

### Q: 多个 MOD 都有核心怎么办？

**A:** 系统会自动选择第一个注册的核心。建议整合包作者只启用一个核心 MOD。

### Q: 如何更改核心名称？

**A:** 在 MOD 初始化时调用 `GridCoreRegistry.setCoreName("YourName")`。

---

## 技术说明

### 渲染方式

| 方块 | 渲染类型 | 文件位置 |
|------|---------|---------|
| 标准能量存储器 | 1x1x1 贴图 | `textures/block/*.png` |
| 通用能量存储器 | 1x1x1 贴图 | `textures/block/*.png` |
| 能量转换器 | 1x1x1 贴图 | `textures/block/*.png` |
| 能源中继传输器 | MMD 模型 | `models/entity/*.pmx` |
| 能源广播塔 | MMD 模型 | `models/entity/*.pmx` |

### MMD 模型要求

```
格式：PMX 或 PMD
坐标：以方块中心为原点
大小：建议高度 1-2 个方块
贴图：PNG 格式，支持透明度
```

### 项目结构

```
IEMS/
├── src/main/java/com/dlzstudio/iems/
│   ├── blocks/          # 方块定义
│   ├── blocks/entity/   # 方块实体
│   ├── energy/          # 能源系统
│   ├── entities/        # 实体
│   ├── web/             # Web 服务器
│   ├── renderer/        # 渲染器
│   └── tabs/            # 创造模式标签
├── src/main/resources/
│   ├── assets/IEMS/     # 资源文件
│   └── META-INF/        # Mod 配置
└── build.gradle         # 构建配置
```

---

## 许可证

**MIT License**

---

## 致谢

- **开发团队**: 等离子工作室 (DLZstudio)
- **感谢**: 所有贡献者

---

## 联系方式

- **问题反馈**: 提交 Issue
- **团队网站**: (待添加)

---

**祝您游戏愉快！**

*综合能源管理系统 v0.6.2*
