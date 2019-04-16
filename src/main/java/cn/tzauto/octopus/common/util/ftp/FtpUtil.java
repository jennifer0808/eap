package cn.tzauto.octopus.common.util.ftp;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.util.StringTokenizer;

/**
 * @author gavin
 */
public class FtpUtil {
    private static FTPClient ftp;
    private static final Logger logger = Logger.getLogger(FtpUtil.class);

    /**
     * 连接FTP服务器
     *
     * @param serverIp
     * @param serverPort
     * @param userName
     * @param password
     * @return
     */
    public static boolean connectFtp(String serverIp, String serverPort, String userName, String password) {
        try {
            ftp = new FTPClient();
            int reply;
            ftp.connect(InetAddress.getByName(serverIp), Integer.parseInt(serverPort));
            ftp.login(userName, password);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }
//
//    /**
//     * 上传文件到FTP服务器
//     *
//     * @param localFilePath
//     * @param remoteFilePath
//     * @param serverIp
//     * @param serverPort
//     * @param userName
//     * @param password
//     * @return
//     */
//    public static boolean uploadFile(String localFilePath, String remoteFilePath, String fileName, String serverIp, String serverPort, String userName, String password) {
//        if (!connectFtp(serverIp, serverPort, userName, password)) {
//            logger.debug("FTP连接失败!");
//            return false;
//        }
//        File file = new File(localFilePath);
//        if (!file.exists() && !file.isFile()) {
//            logger.debug("本地文件不存在！");
//            return false;
//        }
//        logger.debug("FTP连接成功!");
//        boolean uploadflag = false;
//        try {
//            FileInputStream input = new FileInputStream(file);
//            boolean mkdirFlag = mkDirs(remoteFilePath, ftp);
//            ftp.enterLocalPassiveMode();//切换FTP工作方式为passive，此行代码很重要。
//            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
////            ftp.makeDirectory(remoteFilePath);
//            ftp.changeWorkingDirectory(remoteFilePath);
//            uploadflag = ftp.storeFile(remoteFilePath + fileName, input);
//            int replycode = ftp.getReplyCode();
//            logger.debug("保存文件如Ftp-Reply:" + replycode);
//            input.close();
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        } finally {
//            if (ftp.isConnected()) {
//                try {
//                    ftp.logout();
//                    ftp.disconnect();
//                } catch (Exception e) {
//                    logger.error("Exception:", e);
//                }
//            }
//        }
//        return uploadflag;
//    }

    /**
     * 上传文件到FTP服务器
     *
     * @param localFilePath
     * @param remoteFilePath
     * @param serverIp
     * @param serverPort
     * @param userName
     * @param password
     * @return
     */
    public static boolean uploadFile(String localFilePath, String remoteFilePath, String fileName, String serverIp, String serverPort, String userName, String password) {
        if (GlobalConstants.isLocalMode) {
            return true;
        }
        if (!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败!");
            return false;
        }
        File file = new File(localFilePath);
        if (!file.exists() && !file.isFile()) {
            logger.debug("本地文件不存在！>>" + localFilePath);
            return false;
        }
//        if (file.length() == 0) {
//            logger.debug("本地文件为空！>>" + localFilePath);
//            return false;
//        }
        logger.debug("FTP连接成功!");
        boolean uploadflag = false;
        try {
            FileInputStream input = new FileInputStream(file);
            logger.info("开始上传文件，远程路径为:" + remoteFilePath + "，文件名称为:" + fileName);
            boolean mkdirFlag = mkDirs(remoteFilePath, ftp);
            logger.info("创建路径完毕:" + remoteFilePath + "结果:>>" + mkdirFlag);
            ftp.enterLocalPassiveMode();//切换FTP工作方式为passive，此行代码很重要。
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
//            ftp.makeDirectory(remoteFilePath);
            ftp.changeWorkingDirectory(remoteFilePath);
            String storeFile = new String((remoteFilePath + fileName).getBytes("GBK"), "iso-8859-1");
            uploadflag = ftp.storeFile(storeFile, input);
            int replycode = ftp.getReplyCode();
            logger.debug("保存文件Ftp-Reply:" + replycode);
            input.close();
        } catch (Exception e) {
            logger.debug("上传recipe文件出错", e);
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.logout();
                    ftp.disconnect();
                } catch (Exception e) {
                    logger.debug("关闭FTP出错!", e);
                }
            }
        }
        return uploadflag;
    }

    public static String connectServerAndDownloadFile(String localFilePath, String remoteFilePath, String serverIp, String serverPort, String userName, String password) {
        if (!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP服务器(172.17.173.9)无法连接，请检查网络连接状况!");
            return "FTP服务器(172.17.173.9)无法连接，请检查网络连接状况!";
        }

        //获取本地目录和远程目录
//        String localDirectory = null;
        String remoteDirectory = null;
        String remoteFileName = null;
//        localDirectory = localFilePath.substring(0, localFilePath.lastIndexOf("."));
        remoteDirectory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("."));
        remoteDirectory = remoteDirectory.substring(0, remoteDirectory.lastIndexOf("/") + 1);
        remoteFileName = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);

        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }//判断本地目录是否存在，不存在则创建
            ftp.changeWorkingDirectory(remoteDirectory);
            FTPFile[] ftpfiles = null;
            ftp.setControlEncoding("GBK");
            ftpfiles = ftp.listFiles();
            boolean checkflag = false;
            for (int i = 0; i < ftpfiles.length; i++) {
                if (ftpfiles[i].getName().equals(remoteFileName)) {
                    downloadFTPFile(ftpfiles[i], localFilePath);
                    checkflag = true;
                    break;
                }
            }
            if (!checkflag) {
                logger.debug("需要下载的文件FTP服务器上不存在！");
                return "需要下载的文件FTP服务器上不存在！";
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "从FTP服务器上下载的文件出现异常！";
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.logout();
                    ftp.disconnect();
                    ;
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
            }
        }
        logger.debug("从FTP服务器上下载的文件[" + remoteFileName + "]成功！");
        return "0";
    }

    /**
     * 从ftp服务器获取指定路径的文件,并保存到本次指定路径
     *
     * @param localFilePath
     * @param remoteFilePath
     * @param serverIp
     * @param serverPort
     * @param userName
     * @param password
     * @return
     */
    public static boolean downloadFile(String localFilePath, String remoteFilePath, String serverIp, String serverPort, String userName, String password) {
        if (!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败!");
            return false;
        }

        //获取本地目录和远程目录
//        String localDirectory = null;
        String remoteDirectory = null;
        String remoteFileName = null;
//        localDirectory = localFilePath.substring(0, localFilePath.lastIndexOf("."));
        remoteDirectory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("."));
        remoteDirectory = remoteDirectory.substring(0, remoteDirectory.lastIndexOf("/") + 1);
        remoteFileName = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);

        boolean downloadflag = false;
        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
                logger.debug("mkdir===========================================");
            }//判断本地目录是否存在，不存在则创建
            ftp.changeWorkingDirectory(new String(remoteDirectory.getBytes("GBK"), "iso-8859-1"));
            FTPFile[] ftpfiles = null;
            ftp.setControlEncoding("GBK");
            ftpfiles = ftp.listFiles();
            boolean checkflag = false;
            for (int i = 0; i < ftpfiles.length; i++) {
                if (ftpfiles[i].getName().equals(remoteFileName)) {
                    downloadflag = downloadFTPFile(ftpfiles[i], localFilePath);
                    checkflag = true;
                    break;
                }
            }
            if (!checkflag) {
                logger.debug("需要下载的文件FTP服务器上不存在！");
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.logout();
                    ftp.disconnect();
                    ;
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
            }
        }
        return downloadflag;
    }

    /**
     * 下载FTP文件,并且返回报错信息
     *
     * @param ftpFile
     * @param localPath
     * @return default 0
     */
    private static String downloadFTPFileAndFeedBack(FTPFile ftpFile, String localPath) {
        String returnMsg = "0";
        if (ftpFile.isFile()) {
            if (ftpFile.getName().indexOf("?") == -1) {
                try {
                    File localFile = new File(localPath);
                    //判断本地文件是否存在，存在则删除
                    if (localFile.exists()) {
                        localFile.delete();
                    }
//                    else{
//                        localFile.createNewFile();
//                    }
                    OutputStream outputStream = null;
                    outputStream = new FileOutputStream(localPath);
                    boolean writeFileResult = ftp.retrieveFile(ftpFile.getName(), outputStream);
                    outputStream.flush();
                    outputStream.close();
                    if (!writeFileResult) {
                        returnMsg = "写文件到本地失败";
                    }
                } catch (Exception e) {
                    logger.error("Exception:", e);
                    returnMsg = e.getMessage();
                }
            }
        } else {
            returnMsg = "需要下载的目标不是文件！";
            logger.debug(returnMsg);
        }
        return returnMsg;
    }

    /**
     * 下载FTP文件
     *
     * @param ftpFile
     * @param localPath
     * @return
     */
    private static boolean downloadFTPFile(FTPFile ftpFile, String localPath) {
        boolean flag = false;
        if (ftpFile.isFile()) {
            if (ftpFile.getName().indexOf("?") == -1) {
                try {
                    File localFile = new File(localPath);
                    //判断本地文件是否存在，存在则删除
                    if (localFile.exists()) {
                        localFile.delete();
                    }
//                    else{
//                        localFile.createNewFile();
//                    }
                    OutputStream outputStream = null;
                    outputStream = new FileOutputStream(localPath);
                    flag = ftp.retrieveFile(new String(ftpFile.getName().getBytes("GBK"), "iso-8859-1"), outputStream);
                    outputStream.flush();
                    outputStream.close();
                    flag = true;
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
            }
        } else {
            logger.debug("需要下载的目标不是文件！");
        }
        return flag;
    }

    /**
     * 删除服务器文件
     *
     * @param remoteFilePath
     * @param serverIp
     * @param serverPort
     * @param userName
     * @param password
     * @return
     */
    public static boolean delFile(String remoteFilePath, String serverIp, String serverPort, String userName, String password) {
        if (!connectFtp(serverIp, serverPort, userName, password)) {
            logger.debug("FTP连接失败");
            return false;
        }

        //获取远程目录
        String remoteDirectory = null;
        String remoteFileName = null;
        remoteDirectory = remoteFilePath.substring(0, remoteFilePath.lastIndexOf("."));
        remoteDirectory = remoteDirectory.substring(0, remoteDirectory.lastIndexOf("/"));
        remoteFileName = remoteFilePath.substring(remoteFilePath.lastIndexOf("/") + 1);

        boolean deteteflag = false;

        try {
            FTPFile[] ftpfiles = null;
            ftp.setControlEncoding("GBK");
            ftpfiles = ftp.listFiles();
            boolean deleteflag = false;
            for (int i = 0; i < ftpfiles.length; i++) {
                if (ftpfiles[i].getName().equals(remoteFileName)) {
                    deteteflag = ftp.deleteFile(remoteFilePath);
                    deleteflag = true;
                    break;
                }
            }
            if (deleteflag == false) {
                logger.debug("需要删除的文件FTP服务器上不存在！");
            }
        } catch (IOException e) {
            logger.error("Exception:", e);
        } finally {
            try {
                ftp.logout();
                ftp.disconnect();
                ;
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        return deteteflag;
    }

    /**
     * 检查目录是否存在
     *
     * @param dir
     * @return
     */
    public static boolean isDirExist(String dir, FTPClient client) {
        boolean mkResult = false;
        try {
            mkResult = client.makeDirectory(new String(dir.getBytes("GBK"), "iso-8859-1"));
        } catch (Exception e) {
            return false;
        }
        return mkResult;
    }

    /**
     * 创建层级目录
     *
     * @param path
     * @param client
     * @throws IOException
     * @throws IllegalStateException
     */
    public static boolean mkDirs(String path, FTPClient client) {
        if (null == path) {
            return false;
        }
        try {
            client.changeWorkingDirectory("/");// 切换到根目录
            StringTokenizer dirs = new StringTokenizer(path, "/");
            String temp = null;
            boolean flag = false;
            while (dirs.hasMoreElements()) {
                temp = dirs.nextElement().toString();
                if (!isDirExist(temp, client)) {//判断是否存在目录，不存在则创建
                    flag = true;
                }
                client.changeWorkingDirectory(temp);

            }
            if (flag) {
                logger.debug(">>>>>create directory:[" + path + "]");
            }
            client.changeWorkingDirectory("/");
            return true;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
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

    /**
     * @param args
     */
    public static void main(String[] args) {
        String localFilePath = "F://RECIPE/A9/122/122/121.txt";
        String remoteFilePath = "/RECIPE/A9/122/122/121.txt";
        String serverIp = "172.17.173.9";
        String serverPort = "21";
        String userName = "rms";
        String password = "xccdkj@123";
        downloadFile(localFilePath, remoteFilePath, serverIp, serverPort, userName, password);

    }
}
