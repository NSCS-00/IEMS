@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ================================
echo   综合能源管理系统 - 构建脚本
echo ================================
echo.

REM 1. 增加版本号
echo [1/4] 增加版本号...
call increment_version.bat
if errorlevel 1 (
    echo 错误：版本号更新失败
    exit /b 1
)
echo.

REM 2. 清理旧构建
echo [2/4] 清理旧构建...
cd /d "%~dp0"
rmdir /s /q build 2>nul
if exist build (
    echo 警告：无法清理 build 目录
)
echo.

REM 3. 构建
echo [3/4] 开始构建...
call D:\GRADLE\gradle-8.14.3-bin\bin\gradle.bat build --no-daemon -Dorg.gradle.java.home=D:\Java\jdk-21
if errorlevel 1 (
    echo 错误：构建失败
    exit /b 1
)
echo.

REM 4. 复制到 mods 文件夹
echo [4/4] 复制到 mods 文件夹...
for /f "tokens=*" %%i in ('dir /b build\libs\*.jar ^| findstr /v "sources javadoc"') do (
    set JAR_FILE=%%i
)
if defined JAR_FILE (
    copy /y "build\libs\%JAR_FILE%" "E:\我的世界\PCL-CE\.minecraft\mods\" >nul
    echo 已复制：%JAR_FILE%
) else (
    echo 错误：未找到 JAR 文件
    exit /b 1
)
echo.

echo ================================
echo   构建完成！
echo ================================
echo.

REM 读取并显示版本号
set /p ITERATION=<version.txt
echo 版本号：0.6.5-BUILD.%ITERATION%
echo 迭代号：%ITERATION%
echo.
echo 请将此版本号告知 AI 助手
echo.

pause
