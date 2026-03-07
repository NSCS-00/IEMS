import java.nio.file.*;
import java.io.*;

public class CopyMod {
    public static void main(String[] args) {
        try {
            String srcPath = "E:\\114514\\MC\\综合能源管理系统\\build\\libs\\综合能源管理系统 -0.6.5.jar";
            String dstPath = "E:\\我的世界\\PCL-CE\\.minecraft\\mods\\综合能源管理系统 -0.6.5.jar";
            
            File src = new File(srcPath);
            File dst = new File(dstPath);
            
            System.out.println("源文件存在 (File.exists): " + src.exists());
            System.out.println("源文件可读 (File.canRead): " + src.canRead());
            System.out.println("源文件大小 (File.length): " + src.length());
            System.out.println("目标文件夹存在：" + dst.getParentFile().exists());
            
            if (src.exists() && src.canRead()) {
                // 使用 FileInputStream/OutputStream 复制
                try (FileInputStream in = new FileInputStream(src);
                     FileOutputStream out = new FileOutputStream(dst)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
                System.out.println("复制成功！");
                System.out.println("目标文件大小：" + dst.length() + " 字节");
            } else {
                System.out.println("错误：源文件无法读取");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
