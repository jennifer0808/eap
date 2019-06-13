package cn.tzauto.octopus.common.util.tool;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    private static final Logger logger = Logger.getLogger(ZipUtil.class);

    private static final int BUFFER_SIZE = 1024;

    /**
     * 压缩成ZIP 方法1
     *
     * @param srcDir 压缩文件夹路径
     * @param out 压缩文件输出流
     * @param KeepDirStructure 是否保留原来的目录结构,true:保留目录结构;
     * false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     * @throws RuntimeException 压缩失败会抛出运行时异常
     */
    public static void toZip(String srcDir, OutputStream out, boolean KeepDirStructure)
            throws RuntimeException {

        long start = System.currentTimeMillis();
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(out);
            File sourceFile = new File(srcDir);
            compress(sourceFile, zos, sourceFile.getName(), KeepDirStructure);
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) + " ms");
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * 压缩方法2
     *
     * @param srcFiles 压缩源文件
     * @param out 压缩到
     * @return
     */
    public static boolean toZip(List<File> srcFiles, OutputStream out) {
        long start = System.currentTimeMillis();
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(out);
            for (File srcFile : srcFiles) {
                byte[] buf = new byte[BUFFER_SIZE];
                zos.putNextEntry(new ZipEntry(srcFile.getName()));
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                while ((len = in.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
                in.close();
            }
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) + " ms");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 压缩方法2
     *
     * @param srcFiles 压缩源文件
     * @param fileOutPath 压缩到
     * @return
     */
    public static boolean toZip(List<File> srcFiles, String fileOutPath) {
        long start = System.currentTimeMillis();
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new FileOutputStream(fileOutPath));
            for (File srcFile : srcFiles) {
                byte[] buf = new byte[BUFFER_SIZE];
                zos.putNextEntry(new ZipEntry(srcFile.getName()));
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                while ((len = in.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
                in.close();
            }
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) + " ms");
            return true;
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 压缩方法3
     * @param out 输出流
     * @param KeepDirStructure 是否保持原目录结构
     * @param files 要压缩的文件
     */
    public static void toZip(boolean KeepDirStructure, File[] files, OutputStream out) {
        long start = System.currentTimeMillis();
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(out);
            for(File file : files) {
                compress(file, zos, file.getName(), KeepDirStructure);
            }
            long end = System.currentTimeMillis();
            System.out.println("压缩完成，耗时：" + (end - start) + " ms");
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解压zip文件 仅支持文件不支持文件夹
     *
     * @param srcFile 源文件
     * @param targetPath 解压路径
     * @return
     * @throws IOException
     */
    public static boolean unZip(File srcFile, String targetPath) throws IOException {

        if (!srcFile.exists()) {
            logger.debug("源文件" + srcFile + "不存在");
            return false;
        }
        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        long start = System.currentTimeMillis();
        ZipFile zipFile = new ZipFile(srcFile);
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();
            File file = new File(targetPath + File.separator + zipEntry.getName());
            File parent = file.getParentFile();
            if(!parent.exists()) {
                parent.mkdirs();
            }
            OutputStream os = new FileOutputStream(file);
            InputStream in = zipFile.getInputStream(zipEntry);
            int len = 0;
            byte[] bytes = new byte[BUFFER_SIZE];
            while (-1 != (len = in.read(bytes))) {
                os.write(bytes, 0, len);
            }
            os.flush();
            in.close();
            os.close();
        }
        zipFile.close();
        long end = System.currentTimeMillis();
        logger.debug("解压完成，共用时：" + (end - start) + "ms");
        System.out.println("解压完成，共用时：" + (end - start) + "ms");
        return true;
    }

    /**
     * 解压文件到当前文件夹
     *不好使，不释放资源
     * @param filePath 压缩文件路径
     */
//    public static boolean unZip(String filePath) {
//        ZipInputStream zis = null;
//        BufferedOutputStream bos = null;
//        try {
//            long start = System.currentTimeMillis();
//            File source = new File(filePath);
//            if (source.exists()) {
//                zis = new ZipInputStream(new FileInputStream(source));
//                ZipEntry entry = null;
//                while ((entry = zis.getNextEntry()) != null
//                        && !entry.isDirectory()) {
//                    File target = new File(source.getParent(), entry.getName());
//                    if (!target.getParentFile().exists()) {
//                        // 创建文件父目录
//                        target.getParentFile().mkdirs();
//                    }
//                    // 写入文件
//                    bos = new BufferedOutputStream(new FileOutputStream(target));
//
//                    int read = 0;
//                    byte[] buffer = new byte[1024 * 10];
//                    while ((read = zis.read(buffer, 0, buffer.length)) != -1) {
//                        bos.write(buffer, 0, read);
//                    }
//                    bos.flush();
//                }
//                zis.closeEntry();
//            }
//            long end = System.currentTimeMillis();
//            System.out.println("解压完成，共用时：" + (end - start) + "ms");
//        } catch (IOException e) {
//            return false;
//        } finally {
//            System.out.println("关闭流++++++++++++++++");
//            try {
//               
//                zis.close();
//                bos.close();
//            } catch (IOException ex) {
//                java.util.logging.Logger.getLogger(ZipUtil.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            System.out.println("关闭流++++++++++++++++");
//            return true;
//        }
//    }
  
    /**
     * 递归压缩方法
     *
     * @param sourceFile 源文件
     * @param zos	zip输出流
     * @param name	压缩后的名称
     * @param keepDirStructure 是否保留原来的目录结构,true:保留目录结构;
     * false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
     * @throws Exception
     */
    public static void compress(File sourceFile, ZipOutputStream zos, String name,
                                boolean keepDirStructure) throws Exception {
        byte[] buf = new byte[BUFFER_SIZE];
        if (sourceFile.isFile()) {
            // 向zip输出流中添加一个zip实体，构造器中name为zip实体的文件的名字
            zos.putNextEntry(new ZipEntry(name));
            // copy文件到zip输出流中
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            // Complete the entry
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                // 需要保留原来的文件结构时,需要对空文件夹进行处理
                if (keepDirStructure) {
                    // 空文件夹的处理
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    // 没有文件，不需要文件的copy
                    zos.closeEntry();
                }

            } else {
                for (File file : listFiles) {
                    // 判断是否需要保留原来的文件结构
                    if (keepDirStructure) {
                        // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                        // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                        compress(file, zos, name + "/" + file.getName(), keepDirStructure);
                    } else {
                        compress(file, zos, file.getName(), keepDirStructure);
                    }

                }
            }
        }
    }
    
    
     /**
     * zip压缩文件
     *将文件压缩到指定目录
     * @param dir
     * @param zippath
     */
    public static void zipByApache(String dir, String zippath) {
        List<String> paths = getFiles(dir);
        compressFilesZip(paths.toArray(new String[paths.size()]), zippath, dir);
    }
    
    /**
     * 把zip文件解压到指定的文件夹
     *
     * @param zipFilePath zip文件路径, 如 "D:/test/aa.zip"
     * @param saveFileDir 解压后的文件存放路径, 如"D:/test/" ()
     */
    public static void unzipByApache(String zipFilePath, String saveFileDir) {
        if (!saveFileDir.endsWith("\\") && !saveFileDir.endsWith("/")) {
            saveFileDir += File.separator;
        }
        File dir = new File(saveFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(zipFilePath);
        if (file.exists()) {
            InputStream is = null;
            ZipArchiveInputStream zais = null;
            try {
                is = new FileInputStream(file);
                zais = new ZipArchiveInputStream(is);
                ArchiveEntry archiveEntry = null;
                while ((archiveEntry = zais.getNextEntry()) != null) {
                    // 获取文件名  
                    String entryFileName = archiveEntry.getName();
                    // 构造解压出来的文件存放路径  
                    String entryFilePath = saveFileDir + entryFileName;
                    OutputStream os = null;
                    try {
                        // 把解压出来的文件写到指定路径  
                        File entryFile = new File(entryFilePath);
                        File tempParentFile = entryFile.getParentFile();
                        if(!entryFile.getParentFile().exists()){
                            tempParentFile.mkdirs();
                        }
                        if (entryFileName.endsWith("/")) {
                            entryFile.mkdirs();
                        } else {
                            os = new BufferedOutputStream(new FileOutputStream(
                                    entryFile));
                            byte[] buffer = new byte[1024];
                            int len = -1;
                            while ((len = zais.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                        }
                    } catch (IOException e) {
                        throw new IOException(e);
                    } finally {
                        if (os != null) {
                            os.flush();
                            os.close();
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (zais != null) {
                        zais.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 递归取到当前目录所有文件
     *
     * @param dir
     * @return
     */
    private static List<String> getFiles(String dir) {
        List<String> lstFiles = null;
        if (lstFiles == null) {
            lstFiles = new ArrayList<String>();
        }
        File file = new File(dir);
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                lstFiles.add(f.getAbsolutePath());
                lstFiles.addAll(getFiles(f.getAbsolutePath()));
            } else {
                String str = f.getAbsolutePath();
                lstFiles.add(str);
            }
        }
        return lstFiles;
    }

    /**
     * 文件名处理
     *
     * @param dir
     * @param path
     * @return
     */
    private static String getFilePathName(String dir, String path) {
        String p = path.replace(dir + File.separator, "");
        p = p.replace("\\", "/");
        return p;
    }

    /**
     * 把文件压缩成zip格式
     *
     * @param files 需要压缩的文件
     * @param zipFilePath 压缩后的zip文件路径 ,如"D:/test/aa.zip";
     */
    private static void compressFilesZip(String[] files, String zipFilePath, String dir) {
        if (files == null || files.length <= 0) {
            return;
        }
        ZipArchiveOutputStream zaos = null;
        try {
            File zipFile = new File(zipFilePath);
            zaos = new ZipArchiveOutputStream(zipFile);
            zaos.setUseZip64(Zip64Mode.AsNeeded);
            //将每个文件用ZipArchiveEntry封装  
            //再用ZipArchiveOutputStream写到压缩文件中  
            for (String strfile : files) {
                File file = new File(strfile);
                if (file != null) {
                    String name = getFilePathName(dir, strfile);
                    ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(file, name);
                    zaos.putArchiveEntry(zipArchiveEntry);
                    if (file.isDirectory()) {
                        zaos.closeArchiveEntry();
                        continue;
                    }
                    InputStream is = null;
                    try {
                        is = new BufferedInputStream(new FileInputStream(file));
                        byte[] buffer = new byte[1024];
                        int len = -1;
                        while ((len = is.read(buffer)) != -1) {
                            //把缓冲区的字节写入到ZipArchiveEntry  
                            zaos.write(buffer, 0, len);
                        }
                        zaos.closeArchiveEntry();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            }
            zaos.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (zaos != null) {
                    zaos.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public static void main(String[] args) throws IOException {
        //ZipUtil.toZip( true,new File("D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp").listFiles(),new FileOutputStream("D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp\\1.zip"));
        //ZipUtil.toZip("D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp",new FileOutputStream(new File("D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp\\1.zip")),true);
        //ZipUtil.zipByApache("D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp","D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp\\1.zip");
//        ZipUtil.unZip(new File("D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp\\1.zip"), "D:\\RECIPE\\RECIPE\\T6EVO_0001ABG-T-LGA-4X6.8-3000temp");
        ZipUtil.unzipByApache("D:\\桌面文件\\长电\\HP_EPL2400\\besiRecipeFile", "D:\\桌面文件\\长电\\HP_EPL2400\\besiRecipeFile2");

    }
}
