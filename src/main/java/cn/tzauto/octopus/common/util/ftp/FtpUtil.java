//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
package cn.tzauto.octopus.common.util.ftp;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.util.StringTokenizer;

public class FtpUtil {
    private static FTPClient ftp;
    private static final Logger logger = Logger.getLogger(FtpUtil.class);

    public FtpUtil() {
    }

    public static boolean connectFtp(String serverIp, String serverPort, String userName, String password) {
        try {
            ftp = new FTPClient();
            ftp.connect(InetAddress.getByName(serverIp), Integer.parseInt(serverPort));
            ftp.login(userName, password);
            int e = ftp.getReplyCode();
            if(!FTPReply.isPositiveCompletion(e)) {
                ftp.disconnect();
                return false;
            } else {
                return true;
            }
        } catch (Exception var5) {
            logger.error("Exception:", var5);
            return false;
        }
    }

    public static boolean uploadFile(String localFilePath, String remoteFilePath, String fileName, String serverIp, String serverPort, String userName, String password) {
        if(GlobalConstants.isLocalMode) {
            ;
        }

        if(!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败!");
            return false;
        } else {
            File file = new File(localFilePath);
            if(!file.exists() && !file.isFile()) {
                logger.debug("本地文件不存在！>>" + localFilePath);
                return false;
            } else {
                logger.debug("FTP连接成功!");
                boolean uploadflag = false;

                try {
                    FileInputStream e = new FileInputStream(file);
                    logger.info("开始上传文件，远程路径为:" + remoteFilePath + "，文件名称为:" + fileName);
                    boolean mkdirFlag = mkDirs(remoteFilePath, ftp);
                    logger.info("创建路径完毕:" + remoteFilePath + "结果:>>" + mkdirFlag);
                    ftp.enterLocalPassiveMode();
                    ftp.setFileType(2);
                    ftp.changeWorkingDirectory(remoteFilePath);
                    uploadflag = ftp.storeFile(remoteFilePath + fileName, e);
                    int replycode = ftp.getReplyCode();
                    logger.debug("保存文件Ftp-Reply:" + replycode);
                    e.close();
                } catch (Exception var20) {
                    logger.error(var20.getMessage());
                } finally {
                    if(ftp.isConnected()) {
                        try {
                            ftp.logout();
                            ftp.disconnect();
                        } catch (Exception var19) {
                            logger.error(var19.getMessage());
                        }
                    }

                }

                return uploadflag;
            }
        }
    }

    public static String connectServerAndDownloadFile(String localFilePath, String remoteFilePath, String serverIp, String serverPort, String userName, String password) {
        if(!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP服务器" + serverIp + "无法连接，请检查网络连接状况!");
            return "FTP服务器" + serverIp + "无法连接，请检查网络连接状况!";
        } else {
            String remoteDirectory = null;
            String remoteFileName = null;
            remoteDirectory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("."));
            remoteDirectory = remoteDirectory.substring(0, remoteDirectory.lastIndexOf("/") + 1);
            remoteFileName = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);

            try {
                File e = new File(localFilePath);
                if(!e.exists()) {
                    if(!e.getParentFile().exists()) {
                        e.getParentFile().mkdirs();
                    }

                    e.createNewFile();
                }

                ftp.changeWorkingDirectory(remoteDirectory);
                FTPFile[] ftpfiles = null;
                ftp.setControlEncoding("GBK");
                ftpfiles = ftp.listFiles();
                boolean checkflag = false;

                for(int i = 0; i < ftpfiles.length; ++i) {
                    if(ftpfiles[i].getName().equals(remoteFileName)) {
                        downloadFTPFile(ftpfiles[i], localFilePath);
                        checkflag = true;
                        break;
                    }
                }

                if(!checkflag) {
                    logger.debug("需要下载的文件FTP服务器上不存在！");
                    String var24 = "需要下载的文件FTP服务器上不存在！";
                    return var24;
                }
            } catch (Exception var22) {
                logger.error("Exception:", var22);
            } finally {
                if(ftp.isConnected()) {
                    try {
                        ftp.logout();
                        ftp.disconnect();
                    } catch (Exception var21) {
                        logger.error("Exception:", var21);
                    }
                }

            }

            return "0";
        }
    }

    public static boolean downloadFile(String localFilePath, String remoteFilePath, String serverIp, String serverPort, String userName, String password) {
        if(!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败!");
            return false;
        } else {
            String remoteDirectory = null;
            String remoteFileName = null;
            remoteDirectory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("."));
            remoteDirectory = remoteDirectory.substring(0, remoteDirectory.lastIndexOf("/") + 1);
            remoteFileName = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);
            boolean downloadflag = false;

            boolean var26;
            try {
                File e = new File(localFilePath);
                if(!e.exists()) {
                    if(!e.getParentFile().exists()) {
                        e.getParentFile().mkdirs();
                    }

                    e.createNewFile();
                    logger.debug("mkdir===========================================");
                }

                ftp.changeWorkingDirectory(remoteDirectory);
                FTPFile[] ftpfiles = null;
                String LOCAL_CHARSET = "GBK";
                if(FTPReply.isPositiveCompletion(ftp.sendCommand("OPTS UTF8", "ON"))) {
                    LOCAL_CHARSET = "UTF-8";
                }

                ftp.setControlEncoding(LOCAL_CHARSET);
                ftpfiles = ftp.listFiles();
                boolean checkflag = false;

                for(int i = 0; i < ftpfiles.length; ++i) {
                    if(ftpfiles[i].getName().equals(remoteFileName)) {
                        downloadflag = downloadFTPFile(ftpfiles[i], localFilePath);
                        checkflag = true;
                        break;
                    }
                }

                if(checkflag) {
                    return downloadflag;
                }

                logger.debug("需要下载的文件FTP服务器上不存在！");
                var26 = false;
            } catch (Exception var24) {
                logger.error("Exception:", var24);
                return downloadflag;
            } finally {
                if(ftp.isConnected()) {
                    try {
                        ftp.logout();
                        ftp.disconnect();
                    } catch (Exception var23) {
                        logger.error("Exception:", var23);
                    }
                }

            }

            return var26;
        }
    }

    private static String downloadFTPFileAndFeedBack(FTPFile ftpFile, String localPath) {
        String returnMsg = "0";
        if(ftpFile.isFile()) {
            if(ftpFile.getName().indexOf("?") == -1) {
                try {
                    File e = new File(localPath);
                    if(e.exists()) {
                        e.delete();
                    }

                    FileOutputStream outputStream = null;
                    outputStream = new FileOutputStream(localPath);
                    boolean writeFileResult = ftp.retrieveFile(ftpFile.getName(), outputStream);
                    outputStream.flush();
                    outputStream.close();
                    if(!writeFileResult) {
                        returnMsg = "写文件到本地失败";
                    }
                } catch (Exception var6) {
                    logger.error("Exception:", var6);
                    returnMsg = var6.getMessage();
                }
            }
        } else {
            returnMsg = "需要下载的目标不是文件！";
            logger.debug(returnMsg);
        }

        return returnMsg;
    }

    private static boolean downloadFTPFile(FTPFile ftpFile, String localPath) {
        boolean flag = false;
        if(ftpFile.isFile()) {
            if(ftpFile.getName().indexOf("?") == -1) {
                try {
                    File e = new File(localPath);
                    if(e.exists()) {
                        e.delete();
                    }

                    FileOutputStream outputStream = null;
                    outputStream = new FileOutputStream(localPath);
                    flag = ftp.retrieveFile(ftpFile.getName(), outputStream);
                    outputStream.flush();
                    outputStream.close();
                    flag = true;
                } catch (Exception var5) {
                    logger.error("Exception:", var5);
                }
            }
        } else {
            logger.debug("需要下载的目标不是文件！");
        }

        return flag;
    }

    public static boolean delFile(String remoteFilePath, String serverIp, String serverPort, String userName, String password) {
        if(!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败");
            return false;
        } else {
            String remoteDirectory = null;
            String remoteFileName = null;
            remoteDirectory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("."));
            remoteDirectory = remoteDirectory.substring(0, remoteDirectory.lastIndexOf("/"));
            remoteFileName = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);
            boolean deteteflag = false;

            try {
                FTPFile[] e = null;
                ftp.setControlEncoding("GBK");
                e = ftp.listFiles();
                boolean deleteflag = false;

                for(int i = 0; i < e.length; ++i) {
                    if(e[i].getName().equals(remoteFileName)) {
                        deteteflag = ftp.deleteFile(remoteFilePath);
                        deleteflag = true;
                        break;
                    }
                }

                if(!deleteflag) {
                    logger.debug("需要删除的文件FTP服务器上不存在！");
                }
            } catch (IOException var19) {
                logger.error("Exception:", var19);
            } finally {
                try {
                    ftp.logout();
                    ftp.disconnect();
                } catch (Exception var18) {
                    logger.error("Exception:", var18);
                }

            }

            return deteteflag;
        }
    }

    public static boolean isDirExist(String dir, FTPClient client) {
        boolean mkResult = false;

        try {
            mkResult = client.makeDirectory(dir);
            logger.info("创建目录>>" + dir + ">>" + mkResult);
            return mkResult;
        } catch (Exception var4) {
            var4.printStackTrace();
            logger.error("创建目录失败>>" + dir + var4.getMessage());
            return false;
        }
    }

    public static boolean mkDirs(String path, FTPClient client) {
        if(null == path) {
            return false;
        } else {
            path = path.substring(0, path.lastIndexOf("/"));

            try {
                client.changeWorkingDirectory("/");
                StringTokenizer e = new StringTokenizer(path, "/");
                String temp = null;
                boolean flag = false;

                while(e.hasMoreElements()) {
                    temp = e.nextElement().toString();
                    logger.info("开始校验是否存在>>" + temp);
                    if(!isDirExist(temp, client)) {
                        flag = true;
                    }

                    logger.info("切换到>>" + temp);
                    client.changeWorkingDirectory(temp);
                }

                if(flag) {
                    logger.debug(">>>>>create directory:[" + path + "]成功");
                }

                client.changeWorkingDirectory("/");
                return true;
            } catch (Exception var5) {
                logger.error("Exception:", var5);
                return false;
            }
        }
    }

    public static boolean copyFile(String sourceDir, String sourceFileName, String targetDir, String targetFileName) {
        boolean flag = false;
        if(!connectFtp("")) {
            return false;
        } else {
            try {
                mkDirs(targetDir, ftp);
                ftp.changeWorkingDirectory(sourceDir);
                ByteArrayOutputStream e = new ByteArrayOutputStream();
                ByteArrayInputStream in = null;
                ftp.retrieveFile(sourceFileName, e);
                in = new ByteArrayInputStream(e.toByteArray());
                ftp.changeWorkingDirectory(targetDir);
                ftp.storeFile(targetFileName, in);
                in.close();
                flag = true;
            } catch (Exception var10) {
                var10.printStackTrace();
            } finally {
                close();
            }

            return flag;
        }
    }

    public static boolean connectFtp(String ftpName) {
        return connectFtp(GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
    }

    public static void close() {
        if(ftp.isConnected()) {
            try {
                ftp.logout();
                ftp.disconnect();
            } catch (Exception var1) {
                var1.printStackTrace();
            }
        }

    }


    /**
     * 检验文件是否存在
     *
     * @param dir
     * @param fileName
     * @param serverIp
     * @param serverPort
     * @param userName
     * @param password
     * @return
     */
    public static boolean checkFileExist(String dir, String fileName, String serverIp, String serverPort, String userName, String password) {
        if (!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败!");
            return false;
        }
        try {
            ftp.changeWorkingDirectory(new String(dir.getBytes("GBK"), "iso-8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream is = findInput(fileName);
        if (is == null || ftp.getReplyCode() == FTPReply.FILE_UNAVAILABLE) {
            return false;
        }
        return true;
    }


    /**
     * 获取ftp文件input流
     *
     * @param ftpName ftp服务器名称,后期使用
     * @return 是否连接陈功
     */
    public static InputStream findInput(String ftpName) {

        try {
            return ftp.retrieveFileStream(new String(ftpName.getBytes("GBK"), "iso-8859-1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) {
        String localFilePath = "D://RECIPE/12-210-145-6K-NP-8SR-12.txt";
        String remoteFilePath = "/RECIPE/Z2/BGWS/DISCODGP8761/Unique/BG-008/12-210-145-6K-NP-8SR/12-210-145-6K-NP-8SR.txt";
        String serverIp = "192.168.98.12";
        String serverPort = "21";
        String userName = "rms";
        String password = "rms123!";
        downloadFile(localFilePath, remoteFilePath, serverIp, serverPort, userName, password);
        String localFilePath1 = "D://RECIPE/12-210-145-6K-NP-8SR-137.txt";
        String serverIp1 = "192.168.99.137";
        downloadFile(localFilePath1, remoteFilePath, serverIp1, serverPort, userName, password);
    }
}
