@echo off
chcp 65001 >nul
setlocal

REM 读取当前版本号
set VERSION_FILE=version.txt
if exist %VERSION_FILE% (
    set /p ITERATION=<%VERSION_FILE%
) else (
    set ITERATION=00000000
)

REM 计算下一个迭代号 (16 进制)
set /a ITERATION_NUM=0x%ITERATION% + 1
set /a ITERATION_NUM=%%ITERATION_NUM%% 0xFFFFFFFF

REM 格式化为 8 位 16 进制大写
for /f "tokens=*" %%i in ('powershell -Command "('{0:X8}' -f %ITERATION_NUM%)"') do set NEW_ITERATION=%%i

REM 保存新的迭代号
echo %NEW_ITERATION% > %VERSION_FILE%

REM 输出完整版本号
set FULL_VERSION=0.6.5-%NEW_ITERATION%
echo 构建版本：%FULL_VERSION%
echo 迭代号：%NEW_ITERATION%

endlocal
