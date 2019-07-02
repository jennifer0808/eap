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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HtFtpUtil {

    private final static Logger logger = Logger.getLogger(HtFtpUtil.class);
    //生成的临时文件存取地址
    public static String tempPath = "E://tempFile/";
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
        ftpClient.setConnectTimeout(1000 * 30);//设置连接超时时间
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
        } catch (IOException e) {
            logger.error("htftp 连接失败", e);
            return null;
        }
        return ftpClient;
    }

    public List<String> getMapping(String waferId) {
        String fileName = waferId.substring(0, waferId.lastIndexOf("-"));
        UUID uuid = UUID.randomUUID();
        String random = uuid.toString();
        FTPClient ftpClient = connectFtpServer();
        InputStream in = null;
        BufferedReader br = null;
        List<String> list = new ArrayList<>();
        try {
            String remotePath = listPre("2019年04月", fileName, ftpClient);
            logger.info(waferId + "的远程文件地址为：" + remotePath);
            if (remotePath == null) {
                return null;
            }

//        System.out.println(random);
//        String remotePath = "/test/A20309.24.rar";

            String localPath = tempPath + random + remotePath.substring(remotePath.lastIndexOf("/"));

            downloadFile(localPath, remotePath);

            boolean unrar = unrar(localPath, tempPath + random);
            if (unrar) {
                File file = getFile(new File(tempPath + random), waferId);
                in = new FileInputStream(file);
                br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String tmpString = null;
                br.readLine();
                br.readLine();
                br.readLine();
                while ((tmpString = br.readLine()) != null) {
                    list.add(tmpString);
                }

            }
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
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileUtils.deleteDirectory(new File(tempPath + random));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static File getFile(File file, String name) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                getFile(files[i], name);
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
    public String listPre(String pathName, String pre, FTPClient ftpClient) throws IOException {
        if (pathName.startsWith("/") && pathName.endsWith("/")) {
            System.out.println(pathName);
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
                        listPre(pathName + file.getName() + "/", pre, ftpClient);
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
        } catch (IOException e) {
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
            if (Instant.now().toEpochMilli() > start) {
                logger.error("ftp 定时任务超时");
                return;
            }
            try {
                ftpClient.changeWorkingDirectory(path + ftpFile.getName() + "/");
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (int i = 0; i < ftpFiles.length; i++) {
                    recodeFile(ftpFiles[i], ftpClient, path + ftpFile.getName() + "/", deviceService, start);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            File out = new File(pathFile + path + ftpFile.getName());   //文件名中包含? 无法识别
            if (out.exists()) {
                return;
            }
            //文件不存在,添加记录进数据库
            String name = ftpFile.getName();
            name = name.substring(0, name.lastIndexOf("."));
            String filePath = path + ftpFile.getName();
            String[] split = path.split("/");
            String month = "";
            logger.info("waferMapping路径加入数据库：" + name + ";" + filePath + ";" + month);
            deviceService.insertWaferMappingPath(name, filePath, month);
            FileOutputStream o = null;
            try {
                o = FileUtils.openOutputStream(out);
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
