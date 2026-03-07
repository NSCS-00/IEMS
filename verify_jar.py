import zipfile
import os

jar_path = r"E:\114514\MC\综合能源管理系统\build\libs\综合能源管理系统 -0.6.5.jar"

print(f"检查文件：{jar_path}")
print(f"文件存在：{os.path.exists(jar_path)}")

if os.path.exists(jar_path):
    with zipfile.ZipFile(jar_path, 'r') as z:
        print("\n=== assets/IEMS 内容 ===")
        for name in z.namelist():
            if 'assets/IEMS' in name:
                print(f"  {name}")
