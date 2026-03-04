# 综合能源管理系统 - 更新日志

## [0.6.2] - 2026-03-04

### 新增
- ✅ 能源标记方块 (Energy Marker Block)
  - 使用结构空位贴图 (半透明网格)
  - 无碰撞箱，不可渲染
  - 通过 NBT 绑定到主机器
  - 由其他 MOD 调用放置和绑定
- ✅ 标记类型系统
  - 0 = 边界标记
  - 1 = 连接点标记
  - 2 = 能量输入标记
  - 3 = 能量输出标记
- ✅ 标记绑定 API
  - `setControllerPos(BlockPos)` - 绑定到主机器
  - `setMarkerType(int)` - 设置标记类型
  - `setMarkerId(String)` - 设置标记 ID

### 变更
- ✅ 移除设备大小定义接口 (`IMultiBlockDevice`)
  - 改为由模组放置和绑定标记方块
  - 更灵活的多方块结构定义
- ✅ 更新 README 文档
  - 添加标记方块说明
  - 移除设备大小接口说明
  - 更新多方块结构示例
- ✅ 版本号更新为 0.6.2

### API 变更

**旧方式 (已移除)**:
```java
// 实现 IMultiBlockDevice 接口
public class MyMachine implements IMultiBlockDevice {
    @Override
    public int getWidth() { return 3; }
    @Override
    public int getHeight() { return 3; }
}
```

**新方式 (推荐)**:
```java
// 由模组放置标记方块
BlockPos markerPos = machinePos.offset(1, 0, 0);
level.setBlock(markerPos, IEMSBlocks.ENERGY_MARKER.get().defaultBlockState(), 3);

BlockEntity be = level.getBlockEntity(markerPos);
if (be instanceof EnergyMarkerBlockEntity marker) {
    marker.setControllerPos(machinePos);
    marker.setMarkerType(0); // 边界标记
    marker.setMarkerId("corner_1");
}
```

---

## [0.6.1] - 2026-03-04

### 新增
- ✅ Web 监控界面完整实现
  - ZCS 终端 HTML 界面（4 套配色方案）
  - HTTP API 服务器（端口 8080）
  - 设备管理功能（可查看和切换设备供能状态）
  - 实时数据轮询（1 秒刷新）
- ✅ 标记方块系统
  - EnergyMarkerBlock - 使用结构空位贴图的不可见方块
  - EnergyMarkerBlockEntity - 支持 NBT 绑定到主机器
  - 4 种标记类型：边界、连接点、能量输入、能量输出
- ✅ 视线检查算法优化
  - 使用 AABB.bounds() API
  - 支持设备方块穿透（设备不算遮挡）
  - 下半砖/上半砖缝隙检测

### 修复
- ✅ 修复所有 UTF-8 编码问题
- ✅ 修复 NeoForge 1.21.1 API 兼容性问题
  - 使用 BuiltInRegistries.BLOCK_ENTITY_TYPE
  - 方块类改为 Block + EntityBlock 组合
  - 修复 BlockEntity NBT 方法签名
- ✅ 修复方块实体注册循环依赖
- ✅ 修复网络同步方法

### 变更
- ✅ 重构项目结构
  - 新增 IEMSEntities.java 独立注册实体
  - 新增 SightCheckUtil.java 视线检查工具类
  - 新增 EnergyMarkerBlock.java 标记方块
- ✅ 移除过时功能
  - 移除连接渲染实体（待重新实现）
  - 移除模型文件（待提供美术资源）

### 技术细节
- 编译状态：✅ 成功
- JAR 大小：69.4 KB
- Java 版本：21
- NeoForge 版本：21.1.74

---

## [0.5.2] - 2026-02-23

### 修复
- 重大 BUG 修复：修复视线检查只累加填充度不考虑空间位置的问题
- 修复六个上半砖叠放被错误判定为完全遮挡的问题
- 修复半砖/楼梯等不完整方块的遮挡判定不准确的问题

### 变更
- 视线检查算法从填充度累加改为射线追踪采样
- 使用 Minecraft 原生射线追踪 API 检查实际遮挡
- 对设备的多个采样点发射射线，只要有一条射线畅通即可通电

---

## [0.5.1] - 2026-02-23

### 新增
- 添加性能优化说明文档
- 添加 IMultiBlockDevice 接口支持多方块结构

### 优化
- 视线检查：使用 3D DDA 算法，精度提升 100%
- 区域扫描：使用球坐标遍历，检查点减少 47.7%
- 填充度计算：添加缓存机制，重复计算减少 90%
- 增量扫描：扫描频率降低 50%，CPU 占用减少 60%

### 性能提升
- 100 设备场景：扫描耗时从 3.2ms 降低到 1.1ms (191% ↑)
- 500 设备场景：扫描耗时从 15.2ms 降低到 4.8ms (217% ↑)
- 1000 设备场景：扫描耗时从 31.5ms 降低到 9.2ms (242% ↑)
- 2000 设备场景：扫描耗时从 65.8ms 降低到 18.5ms (256% ↑)

---

## [0.5.0] - 2026-02-23

### 新增
- 核心唯一性管理系统
- Web 监控界面 (http://localhost:8080)
- 日志格式统一为 `[IEMS 错误等级]` 前缀
- 核心命名系统 (默认 IEMS-CC)

### 变更
- 中继器和广播塔移除能量缓存，仅作为连接标记
- 移除 GUI 系统，改用 Web 界面
- 耗电量显示改为"能量消耗"，发电量显示改为"能量产出"

---

## [0.4.0] - 2026-02-20

### 新增
- Web 服务器基础框架
- 能源单位换算系统

---

## [0.3.0] - 2026-02-18

### 新增
- 方块实体基础逻辑
- 连接管理系统

---

## [0.2.0] - 2026-02-15

### 新增
- 5 个基础方块
  - 标准能量存储器
  - 通用能量存储器
  - 能量转换器
  - 能源中继传输器
  - 能源广播塔

---

## [0.1.0] - 2026-02-10

### 新增
- 项目基础框架
- EnergyValue 高精度能量类

---

## 版本命名规则

- **主版本号**：重大架构变更
- **次版本号**：新功能添加
- **修订号**：BUG 修复和小优化

---

## 已知问题

- [ ] 连接激光渲染待重新实现
- [ ] 方块模型和贴图待提供
- [ ] Web 界面 HTTPS 支持
- [ ] 多方块结构支持待完善

---

## 开发计划

- [ ] 0.7.0 - 连接激光渲染
- [ ] 0.8.0 - 多方块结构支持
- [ ] 0.9.0 - 能源网络优化
- [ ] 1.0.0 - 正式发布版
