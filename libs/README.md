# GeckoLib 安装说明

## 为什么需要手动安装？

由于 GeckoLib 官方 Maven 仓库可能无法访问，本项目使用本地 JAR 文件作为编译依赖。

## 安装步骤

### 1. 下载 GeckoLib

从以下任一网站下载 **GeckoLib 4.8.4 for NeoForge 1.21.1**：

- **Modrinth**: https://modrinth.com/mod/geckolib/version/4.8.4
- **CurseForge**: https://www.curseforge.com/minecraft/mc-mods/geckolib/files
- **MC 百科**: https://www.mcmod.cn/mod/geckolib.html

### 2. 放置 JAR 文件

下载完成后，将 JAR 文件重命名为：
```
geckolib-neoforge-1.21.1-4.8.4.jar
```

然后放到本项目的 `libs/` 文件夹：
```
综合能源管理系统/
├── libs/
│   └── geckolib-neoforge-1.21.1-4.8.4.jar  ← 放这里
├── src/
├── build.gradle
└── ...
```

### 3. 构建项目

```cmd
cd E:\114514\MC\综合能源管理系统
D:\GRADLE\gradle-8.14.3-bin\bin\gradle.bat build --no-daemon -Dorg.gradle.java.home=D:\Java\jdk-21
```

### 4. 运行时安装

玩家需要在游戏中安装 GeckoLib：
1. 下载 GeckoLib 4.8.4 for NeoForge 1.21.1
2. 放入 `.minecraft/mods/` 文件夹
3. 启动游戏

---

## 注意事项

- **开发环境**: 需要 GeckoLib JAR 在 `libs/` 文件夹（编译用）
- **运行环境**: 需要 GeckoLib JAR 在 `mods/` 文件夹（运行时用）
- **玩家**: 只需要在 `mods/` 文件夹安装 GeckoLib

---

## 版本对应

| 模组版本 | GeckoLib 版本 | NeoForge 版本 |
|---------|--------------|--------------|
| 0.6.5   | 4.8.4        | 21.1.74      |
