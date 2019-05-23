package cn.tzauto.octopus.common.util.tool;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ZipUtil {

    public static String pathOf7Z = "C:/7-Zip/7z.exe";

    public static String remoteUrl = "//192.168.1.232/recipe/";

    /**
     * 使用7z压缩文件
     * @param originalFile
     * @param targetFile
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean zipFileBy7Z(String originalFile, String targetFile) throws IOException, InterruptedException {
        //先删除目标文件,确定为7z文件,防止删除其他文件
        if (targetFile.endsWith(".7z")) {
            new File(targetFile).delete();
        } else {
            //todo  其他类型文件，或文件夹处理（错误路径处理）
            return false;
        }
        String command = pathOf7Z + " a " + targetFile + " " + originalFile;
        Process exec = Runtime.getRuntime().exec(command);

        return  exec.waitFor(20, TimeUnit.MINUTES);
    }

    /**
     * 解压文件
     * @param zipName
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static boolean unzipBy7Z(String zipName) throws InterruptedException, IOException {

        String command = pathOf7Z+ " x "+remoteUrl+zipName+"  -o"+remoteUrl+" -aoa ";
        Process exec = Runtime.getRuntime().exec(command);
        return  exec.waitFor(20, TimeUnit.MINUTES);
    }
}
