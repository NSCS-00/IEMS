# 读取或创建版本号
$versionFile = "version.txt"
if (Test-Path $versionFile) {
    $iteration = Get-Content $versionFile
} else {
    $iteration = "00000000"
}

# 计算下一个迭代号
$iterationNum = [Convert]::ToUInt32($iteration, 16) + 1
$iterationNum = $iterationNum -band 0xFFFFFFFF

# 格式化为 8 位 16 进制大写
$newIteration = "{0:X8}" -f $iterationNum

# 保存
Set-Content -Path $versionFile -Value $newIteration -NoNewline

# 输出
Write-Host "构建版本：0.6.5-BUILD.$newIteration"
Write-Host "迭代号：$newIteration"
