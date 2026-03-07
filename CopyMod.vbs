Set fso = CreateObject("Scripting.FileSystemObject")
Set shell = CreateObject("WScript.Shell")

src = "E:\114514\MC\综合能源管理系统\build\libs\综合能源管理系统 -0.6.5.jar"
dst = "E:\我的世界\PCL-CE\.minecraft\mods\综合能源管理系统 -0.6.5.jar"

WScript.Echo "源文件：" & src
WScript.Echo "目标文件：" & dst

If fso.FileExists(src) Then
    WScript.Echo "源文件存在"
    fso.CopyFile src, dst, True
    WScript.Echo "复制成功！"
    
    If fso.FileExists(dst) Then
        WScript.Echo "目标文件大小：" & fso.GetFile(dst).Size & " 字节"
    End If
Else
    WScript.Echo "错误：源文件不存在"
    
    ' 列出目录内容
    folder = "E:\114514\MC\综合能源管理系统\build\libs"
    If fso.FolderExists(folder) Then
        WScript.Echo "目录内容："
        For Each file In fso.GetFolder(folder).Files
            WScript.Echo "  " & file.Name
        Next
    End If
End If
