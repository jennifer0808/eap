package cn.tzauto.octopus.common.util.ftp;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class HtFtpUtil {

    private final static Logger logger = Logger.getLogger(HtFtpUtil.class);
    //生成的临时文件存取地址
    public static String tempPath = "E://tempFile/";
    //ftp路径记录地址
    public static String pathFile = "E://pathFile";
    public static String controlEncoding = "gbk";

    public static boolean unrar(String localPath, String targetPath) {
        String command = GlobalConstants.winRarPath + " x \"" + localPath + "\" \"" + targetPath + "\"";
        try {
            Process exec = Runtime.getRuntime().exec(command);
            return exec.waitFor(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("本地解压错误", e);
        }
        return false;
    }

    private static FTPClient connectFtpServer() {
        FTPClient ftpClient = new FTPClient();
//        ftpClient.setConnectTimeout(1000 * 30);//设置连接超时时间
        ftpClient.setControlEncoding(controlEncoding);//设置ftp字符集
        ftpClient.enterLocalPassiveMode();//设置被动模式，文件传输端口设置
        try {
            ftpClient.connect(GlobalConstants.htFtpUrl);
            ftpClient.login(GlobalConstants.htFtpUser, GlobalConstants.htFtpPwd);
            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                logger.error("connect ftp " + GlobalConstants.htFtpUrl + " failed");
                ftpClient.disconnect();
                return null;
            }
        } catch (Exception e) {
            logger.error("htftp 连接失败", e);
            return null;
        }
        return ftpClient;
    }

    public static String getMapping(String waferId) {

        FTPClient ftpClient = connectFtpServer();
        try {

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy年MM月");
            LocalDateTime now = LocalDateTime.now();  //这一个月
            LocalDateTime ldt = now.minusMonths(1);   //上一个月
            String temp = null;
            String nowStr = "/" + now.format(dtf) + "/";
            String ldtStr = "/" + ldt.format(dtf) + "/";

            String remotePath = listPre(nowStr, waferId, ftpClient);
            if (remotePath == null) {
                remotePath = listPre(ldtStr, waferId, ftpClient);
            } else {
                temp = now.format(dtf);
            }

            if (remotePath == null) {
                logger.error(now.format(dtf) + "和" + ldt.format(dtf) + "ftp上没有找到该压缩文件");
                return null;
            } else if (temp == null) {
                temp = ldt.format(dtf);
            }

            //入库
            File out = new File(pathFile + remotePath);   //文件名中包含? 无法识别
            if (out.exists()) {
                return remotePath;
            }
            //文件不存在,添加记录进数据库
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            String name = out.getName();
            int index = name.lastIndexOf(".");
            if (index > 0) {
                name = name.substring(0, name.lastIndexOf("."));
            }
            String filePath = remotePath;
            String[] split = remotePath.split("/");
            String month = split[1];
            logger.info("waferMapping路径加入数据库：" + name + ";" + filePath + ";" + month);

            FileOutputStream o = null;
            try {
                o = FileUtils.openOutputStream(out);
                deviceService.insertWaferMappingPath(name, filePath, month);
                sqlSession.commit();
            } catch (IOException e) {
                logger.error("输出文件错误", e);
            } finally {
                sqlSession.close();
                if (o != null) {
                    try {
                        o.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            return remotePath;

        } catch (Exception e) {
            logger.error("getMapping发生错误", e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static File getFile(File file, String name) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File result = getFile(files[i], name);
                if (result != null) {
                    return result;
                }
            }
        } else {
            if (file.getName().endsWith(name)) {
                return file;
            }
        }
        return null;
    }

    /**
     * 递归遍历目录下面指定的文件名
     *
     * @param pathName 需要遍历的目录，必须以"/"开始和结束
     * @param pre      文件名前部分
     * @throws IOException
     */
    public static String listPre(String pathName, String pre, FTPClient ftpClient) throws IOException {
        if (pathName.startsWith("/") && pathName.endsWith("/")) {
            //更换目录到当前目录
            ftpClient.changeWorkingDirectory(pathName);
            FTPFile[] files = ftpClient.listFiles();
            for (FTPFile file : files) {
                if (file.isFile()) {
                    if (file.getName().startsWith(pre) && (file.getName().endsWith(".rar") || file.getName().endsWith(".zip"))) {
                        String s = pathName + file.getName();
                        return s;
                    }
                } else if (file.isDirectory()) {
                    if (!".".equals(file.getName()) && !"..".equals(file.getName())) {
                        String s = listPre(pathName + file.getName() + "/", pre, ftpClient);
                        if (s != null) {
                            return s;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void downloadFile(String localPath, String remotePath) {
        FTPClient ftpClient = connectFtpServer();
        File file = new File(localPath);
        OutputStream os = null;
        try {
            os = FileUtils.openOutputStream(file);
            ftpClient.retrieveFile(remotePath, os);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftpClient != null) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param path ftp目录  "/2019年07月/"
     */
    public void recordFile(String path) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        FTPClient ftpClient = connectFtpServer();
        long now = Instant.now().toEpochMilli();
        long start = now + 1800000L;
        logger.info("开始路径为：" + path + "的ftp定时任务：" + now);
        try {
            ftpClient.changeWorkingDirectory(path);
//            ftpClient.changeWorkingDirectory("/");
            FTPFile[] ftpFiles = ftpClient.listFiles();
            for (int i = 0; i < ftpFiles.length; i++) {
                FTPFile ftpFile = ftpFiles[i];
                recodeFile(ftpFile, ftpClient, path, deviceService, start);
            }
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("htftp文件定时任务失败", e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }


    private void recodeFile(FTPFile ftpFile, FTPClient ftpClient, String path, DeviceService deviceService, long start) {

        if (ftpFile.isDirectory()) {
            String name = ftpFile.getName();
            if (".".equals(name) || "..".equals(name)) {
                return;
            }
            if (Instant.now().toEpochMilli() > start) {
                logger.error("ftp 定时任务超时");
                return;
            }
            try {
                ftpClient.changeWorkingDirectory(path + name + "/");
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (int i = 0; i < ftpFiles.length; i++) {
                    recodeFile(ftpFiles[i], ftpClient, path + name + "/", deviceService, start);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String name = ftpFile.getName();
            if (!(name.endsWith(".rar") || name.endsWith(".zip"))) {
                return;
            }
            File out = new File(pathFile + path + name);   //文件名中包含? 无法识别
            if (out.exists()) {
                return;
            }
            //文件不存在,添加记录进数据库

            int index = name.lastIndexOf(".");
            if (index > 0) {
                name = name.substring(0, name.lastIndexOf("."));
            }
            String filePath = path + ftpFile.getName();
            String[] split = path.split("/");
            String month = split[1];
            logger.info("waferMapping路径加入数据库：" + name + ";" + filePath + ";" + month);

            FileOutputStream o = null;
            try {
                o = FileUtils.openOutputStream(out);
                deviceService.insertWaferMappingPath(name, filePath, month);
            } catch (IOException e) {
                logger.error("输出文件错误", e);
            } finally {
                if (o != null) {
                    try {
                        o.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }


}
