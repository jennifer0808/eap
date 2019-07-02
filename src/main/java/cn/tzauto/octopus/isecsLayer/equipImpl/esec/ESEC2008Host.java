/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.esec;


import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Wang DanFeng
 */
public class ESEC2008Host extends EquipModel {

    private static Logger logger = Logger.getLogger(ESEC2008Host.class.getName());
    private ISecsHost recipeHost;
    public ESEC2008Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        recipeHost = new ISecsHost(remoteIPAddress, String.valueOf(12005), deviceType, deviceCode);
        iSecsHostList.clear();
        iSecsHostList.add(iSecsHost);
        iSecsHostList.add(recipeHost);
        if (iSecsHost.isConnect) {
            this.equipState.setCommOn(true);
            commState = 1;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getEquipRealTimeState();
                }
            }).start();
        } else {
            this.equipState.setCommOn(false);
            commState = 0;
        }
    }
    @Override
    public String getCurrentRecipeName() {
        //TODO 从界面上无法读取到完整recipe
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                String readRecipe = recipeHost.executeCommand("getrecipe").get(0);
                ppExecName = readRecipe.substring(readRecipe.lastIndexOf("\\")+1,readRecipe.lastIndexOf("."));

//                String readRecipe = iSecsHost.executeCommand("read recipeName").get(0);
//                List<String> equipRecipeList = new ArrayList<>();
//                String equipLocalRecipPath = this.getEquipLocalRecipePath();
//                synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                    List<String> result = iSecsHost.executeCommand("dos $dir " + "\"" + equipLocalRecipPath + File.separator + "*.dbrcp" + "\" /o-d/b$");
//                    for (String recipeName : result) {
//                        if (!"done".equals(recipeName) && !"".equals(recipeName)) {
//                            if (recipeName.contains(readRecipe.replace(".", ""))) {
//                                ppExecName = recipeName.replace(".dbrcp", "");
//                            }
//                        }
//                    }
//                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage(), e);
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        List<String> result = null;
        String startResult = "Start failed";
        try {
            // TODO: 2018/3/19 开始机台
            result = iSecsHost.executeCommand("playback start.txt");
            if ("done".equals(result.get(0))) {
                result = iSecsHost.executeCommand("read ok_start");
                if ("OK".equalsIgnoreCase(result.get(0))) {
                    result = iSecsHost.executeCommand("read status");
                    if (result != null && !result.isEmpty()) {
                        if (!result.get(0).contains("error")) {
                            if ("AUTO RUNNING".equals(result.get(0))) {
                                equipStatus = "RUN";
                                startResult = "0";
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            startResult = "Start failed";
        } finally {
            return startResult;
        }

    }

    @Override
    public String pauseEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String stopEquip() {
        // TODO: 2018/3/19 停止机台
        String stopResult = "锁机失败,当前状态无法执行锁机";
//        if (getPassport(1)) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();

            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    result = iSecsHost.executeCommand("playback stop.txt");
                    if ("done".equals(result.get(0))) {
                        equipStatus = result.get(0);
                        stopResult = "0";
                    }
//                    for (String str : result) {
//                        if ("done".equals(result.get(0))) {
//                            result = iSecsHost.executeCommand("read status");
//                            if (result != null && !result.isEmpty()) {
//                                if (!result.get(0).contains("error")) {
//                                    if ("STOP".equals(result.get(0))) {
//                                        equipStatus = result.get(0);
//                                        stopResult = "0";
//                                    }
//                                }
//                            }
//                        }
//                    }
                } catch (Exception e) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "锁机失败,当前状态无法执行锁机！");
                    return stopResult;
                }
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
                stopResult = "未设置锁机！";
            }
        }
//            returnPassport();
//        }
        return stopResult;
    }

    @Override
    public String lockEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        // TODO: 2018/3/19 上传recipe OVER
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //ftp ip user pwd lpath rpath "mput "103.rcp"
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端上传ftp路径
                String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
                FileUtil.CreateDirectory(clientFtpRecipeAbsolutePath);
                //ocr上传至本地
                boolean ocrUploadOk = true;
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mput " + recipeName + ".dbrcp \"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String uploadstr : result) {
                    if (uploadstr.contains("success")) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            // TODO  打印文件的形式
                            recipeParaList = getRecipeParasFromMonitorMap2(recipeName);
                        } catch (Exception ex) {
                            logger.error("解析recipe时出错!", ex);
                            ex.printStackTrace();
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", hostUploadFtpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);

                    }
                    if (uploadstr.contains("Error")) {
                        ocrUploadOk = false;
                    }
                }
                if (!ocrUploadOk) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.error("upload recipe failed" + e.getMessage(), e);
            }
        }
        return resultMap;

    }


    public Map uploadRecipe2(String recipeName) {
        // TODO: 2018/3/19 上传recipe OVER
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //ftp ip user pwd lpath rpath "mput "103.rcp"
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端上传ftp路径
                String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
                FileUtil.CreateDirectory(clientFtpRecipeAbsolutePath);
                //ocr上传至本地
                boolean ocrUploadOk = true;
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mput " + recipeName + ".dbrcp \"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String uploadstr : result) {
                    if (uploadstr.contains("success")) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            // TODO  通过读取界面的数值
                            recipeParaList = getRecipeParasFromMonitorMap();
                        } catch (Exception ex) {
                            logger.error("解析recipe时出错!", ex);
                            ex.printStackTrace();
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", hostUploadFtpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);

                    }
                    if (uploadstr.contains("Error")) {
                        ocrUploadOk = false;
                    }
                }
                if (!ocrUploadOk) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.error("upload recipe failed" + e.getMessage(), e);
            }
        }
        return resultMap;

    }

    /**
     * 获取工控机FtpRecipe绝对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeAbsolutePath(String recipeName) {
        //D:/RECIPE/**********
        return GlobalConstants.localRecipePath + "RECIPE/" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    /**
     * 获取工控机FtpRecipe相对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeRelativePath(String recipeName) {
        return (GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
    }

    /**
     * 获取机台本地Recipe路径
     *
     * @return
     */
    public String getEquipLocalRecipePath() {
        return equipRecipePath;
    }

    /**
     * 获取服务端上传FtpRecipe路径
     *
     * @param recipe
     * @return
     */
    public String getHostUploadFtpRecipePath(Recipe recipe) {
        String ftpRecipePath;
        try (SqlSession sqlSession = MybatisSqlSession.getSqlSession()) {
            ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        }
        return ftpRecipePath;
    }

    /**
     * 获取服务端下载FtpRecipe文件全路径
     *
     * @param recipe
     * @return
     */
    public String getHostDownloadFtpRecipeFilePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String downloadFullFilePath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        sqlSession.close();
        return downloadFullFilePath;
    }


    @Override
    public String downloadRecipe(Recipe recipe) {
        // TODO: 2018/3/19 下载recipe
        //cleanRootDir();
        //ftp ip user pwd lpath rpath \"mget 103.rcp\"
        String downloadResult = "";
        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
        synchronized (socketClient) {
            try {
                String recipeName = recipe.getRecipeName();
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端下载ftp文件全路径
                String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String hostDownloadFtpRecipePath = hostDownloadFtpRecipeFilePath.substring(0, hostDownloadFtpRecipeFilePath.lastIndexOf("/") + 1);
                if (!GlobalConstants.isLocalMode) {
                    if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }

                    if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".dbrcp", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                    } else {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".dbrcp", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                    }
                }

                sqlSession.close();
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mget " + recipeName + ".dbrcp\"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String str : result) {
                    if (str.contains("success")) {
                        downloadResult = "0";
                    }
                    if (str.contains("Error")) {
                        downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage(), e);
                downloadResult = "Download recipe " + recipe.getRecipeName() + " failed";
            }
        }
        //this.deleteTempFile(recipe.getRecipeName());
        return downloadResult;
    }

    @Override
    public Map getEquipRealTimeState() {
        equipStatus = getEquipStatus();
        ppExecName = getCurrentRecipeName();
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        return map;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        Map map = getEquipRecipeList();
        List<String> recipeList = (List<String>) map.get("eppd");
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //机台本地recipe路径
            String equipLocalRecipePath = this.getEquipLocalRecipePath();
            try {
                if (!recipeList.contains(recipeName)) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                } else {
                    List<String> result = iSecsHost.executeCommand("dos $del /q \"" + equipLocalRecipePath + "\\" + recipeName + ".dbrcp\"$");
                    for (String str : result) {
                        if ("done".equals(str)) {
                            return "删除成功";
                        }
                    }
                }
                return "删除失败";
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {

        try {
            //登陆权限
//调转至setup界面，可能会是select界面
//1.setup界面时需要判断recipe前的第一个选项是否被打开read recipeflag1 
// a.读取到“RECIPE”则
// b.未读取到“RECIPE”则点击第一选项，再次读取read recipeflag1 
//继续读取read recipemanager,,recipe选项是否被打开
//到select界面，选择行数，load

            List<String> equipRecipeList = getRecipeList();
            String recipeRow = "";
            //查询recipe是否可以选中，并找出recipe位置信息
            for (int i = 0; i < equipRecipeList.size(); i++) {
                if (recipeName.equals(equipRecipeList.get(i))) {
                    recipeRow = "r" + (i + 1) + "_sel";
                    break;
                }
            }
            if ("".equals(recipeRow)) {
                return "不存在预选中recipe!";
            }
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                List<String> logFlag = iSecsHost.executeCommand("read user");
//                if (logFlag != null && !logFlag.isEmpty()) {
//                    if (!"SA".equalsIgnoreCase(logFlag.get(0))) {
//                        List<String> logResult = iSecsHost.executeCommand("playback login.txt");
//                        if (!"done".equals(logResult.get(0))) {
//                            return "选中失败,登陆失败!";
//                        }
//                    }
//                }
                if (!checkUserAndLogin()) {
                    return "选中失败,登陆失败!";
                }
                List<String> gotosetup = iSecsHost.executeCommand("playback gotosetup.txt");
                Thread.sleep(300);
                gotosetup = iSecsHost.executeCommand("goto setup");
                List<String> screen = iSecsHost.executeCommand("read setupflag");
                if ("assist".equalsIgnoreCase(screen.get(0))) {
                    List<String> recipeflag1 = iSecsHost.executeCommand("read recipeflag1");
                    if (!"Recipe".equalsIgnoreCase(recipeflag1.get(0))) {
                        List<String> clickAssist = iSecsHost.executeCommand("playback clickassist.txt");

                    }
                    List<String> recipeManager = iSecsHost.executeCommand("read recipemanager");
                    if (!"recipemanager".equalsIgnoreCase(recipeManager.get(0).replace(" ",""))) {
                        List<String> clickRecipe = iSecsHost.executeCommand("playback clickrecipe.txt");
                    }
                    List<String> clickRecipe = iSecsHost.executeCommand("playback clickrecipemanager.txt");
                }
                Thread.sleep(300);
                screen = iSecsHost.executeCommand("curscreen");
//                if ("select".equals(screen.get(0))) {
                List<String> selectresult = iSecsHost.executeCommand("playback " + recipeRow + ".txt");
                selectresult = iSecsHost.executeCommand("playback load.txt");
                selectresult = iSecsHost.executeCommand("playback clickyes.txt");
                if (!"done".equals(selectresult.get(0))) {
//                        return "选中失败";
                } else {
                    return "0";
                }
            }
            return "选中失败";
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return "选中失败";
        }
    }

    @Override
    public Map getEquipMonitorPara() {
//        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(getCurrentRecipeName()).get("recipeParaList");
//        Map<String, String> resultMap = new HashMap<>();
//        for (RecipePara recipePara : recipeParaList) {
//            resultMap.put(recipePara.getParaName(), recipePara.getSetValue());
//        }
//        logger.info("monitormap:" + resultMap.toString());
//        return resultMap;

        //获取界面上的参数值

        // TODO: 2018/3/19 获取参数
        Map resultMap = new HashMap();
        if (getPassport()) {
            Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
            synchronized (socketClient) {
                if (!checkUserAndLogin()) {
                    logger.error("未登陆权限，获取数据失败！");
                    return resultMap;
                }
                //获取参数
                iSecsHost.executeCommand("playback gotoparamset.txt");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                iSecsHost.executeCommand("goto paramset");
                //读取界面的数值,看是否其他界面被打开了
                List<String> pickflag1 = new ArrayList();
                List<String> pickflag2 = new ArrayList();
                int i = 0;
                boolean haveWaferMapping = false;
                do {
                    //TODO 此时界面会有两种情况，，有waferMapping和无waferMapping

                    pickflag1 = iSecsHost.executeCommand("read pickflag");
                    //有mapping标识
                    pickflag2 = iSecsHost.executeCommand("read pickflag2");
                    //其他界面被打开需要关闭:将第一个页面打开，再开闭，以使页面全部关闭，之后点击“pick&place”
                    if (!"pick&place".equalsIgnoreCase(pickflag1.get(0).replaceAll(" ", ""))
                            && !"pick&place".equalsIgnoreCase(pickflag2.get(0).replaceAll(" ", ""))) {
                        //看第一个标题界面是否被打开，打开则关闭，未打开则点击两次
                        List<String> waferflag = iSecsHost.executeCommand("read waferflag");
                        try {
                            if ("wafer".equalsIgnoreCase(waferflag.get(0))) {
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                                Thread.sleep(300);
                                waferflag = iSecsHost.executeCommand("read waferflag");
                                if ("wafermapping".equalsIgnoreCase(waferflag.get(0).replaceAll(" ", ""))) {
                                    haveWaferMapping = true;
                                }
                            } else if ("wafermapping".equalsIgnoreCase(waferflag.get(0).replaceAll(" ", ""))) {
                                haveWaferMapping = true;
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                                Thread.sleep(300);
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                            } else {
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                                Thread.sleep(300);
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                            }
                        } catch (InterruptedException e1) {
                            logger.debug(e1);
                        } catch (Exception e2) {
                            logger.debug(e2);
                        }
                    } else {
                        if ("pick&place".equalsIgnoreCase(pickflag1.get(0).replaceAll(" ", ""))) {
                            haveWaferMapping = true;
                        }
                        break;
                    }
                    i++;
                } while (i < 2);
                //此时判断是否有wafermapping标签
                if (haveWaferMapping) {
                    //读取pickupflag，判断“pick&place”是否被打开
                    List<String> pickupflag = iSecsHost.executeCommand("read pickupflag");
                    if (!"pickup".equalsIgnoreCase(pickupflag.get(0))) {
                        iSecsHost.executeCommand("playback clickpickplace.txt");
                    }
                    //点击“Extended Pickup Process”，并读取数值
                    iSecsHost.executeCommand("playback clickpickup.txt");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //2k
                    iSecsHost.executeCommand("goto pickparamset");
                    List<String> pickupresult = iSecsHost.executeCommand("readm dtd pwt pf nth nwt nv");
                    resultMap.put("dtd", pickupresult.get(0));
                    resultMap.put("pwt", pickupresult.get(1));
                    resultMap.put("pf", pickupresult.get(2));
                    resultMap.put("nth", pickupresult.get(3));
                    resultMap.put("nwt", pickupresult.get(4));
                    resultMap.put("nv", pickupresult.get(5));
//                List<String> pickupresult = iSecsHost.executeCommand("readbyscreen pickparamset");
//                Map<String, String> pickupResultMap = (Map<String, String>) JsonMapper.fromJsonString(pickupresult.get(0), Map.class);
//                resultMap.putAll(pickupResultMap);
                    //点击“Bond Process”,并读取数值
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("playback clickbond.txt");
                    try {
                        Thread.sleep(2300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("goto bondparamset");
                    List<String> bondresult = iSecsHost.executeCommand("readm dtp bf prest postst");
                    resultMap.put("dtp", bondresult.get(0));
                    resultMap.put("bf", bondresult.get(1));
                    resultMap.put("prest", bondresult.get(2));
                    resultMap.put("postst", bondresult.get(3));
//                List<String> bondresult = iSecsHost.executeCommand("readbyscreen bondparamset");
//                Map<String, String> bondResultMap = (Map<String, String>) JsonMapper.fromJsonString(bondresult.get(0), Map.class);
//                resultMap.putAll(bondResultMap);
                } else {
                    //读取pickupflag，判断“pick&place”是否被打开
                    List<String> pickupflag = iSecsHost.executeCommand("read pickupflag2");
                    if (!"pickup".equalsIgnoreCase(pickupflag.get(0))) {
                        iSecsHost.executeCommand("playback clickpickplace2.txt");
                    }
                    //点击“Extended Pickup Process”，并读取数值
                    iSecsHost.executeCommand("playback clickpickup2.txt");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //2k
                    iSecsHost.executeCommand("goto pickparamset");
                    List<String> pickupresult = iSecsHost.executeCommand("readm dtd pwt pf nth nwt nv");
                    resultMap.put("dtd", pickupresult.get(0));
                    resultMap.put("pwt", pickupresult.get(1));
                    resultMap.put("pf", pickupresult.get(2));
                    resultMap.put("nth", pickupresult.get(3));
                    resultMap.put("nwt", pickupresult.get(4));
                    resultMap.put("nv", pickupresult.get(5));
//                List<String> pickupresult = iSecsHost.executeCommand("readbyscreen pickparamset");
//                Map<String, String> pickupResultMap = (Map<String, String>) JsonMapper.fromJsonString(pickupresult.get(0), Map.class);
//                resultMap.putAll(pickupResultMap);
                    //点击“Bond Process”,并读取数值
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("playback clickbond2.txt");
                    try {
                        Thread.sleep(2300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("goto bondparamset");
                    List<String> bondresult = iSecsHost.executeCommand("readm dtp bf prest postst");
                    resultMap.put("dtp", bondresult.get(0));
                    resultMap.put("bf", bondresult.get(1));
                    resultMap.put("prest", bondresult.get(2));
                    resultMap.put("postst", bondresult.get(3));
//                List<String> bondresult = iSecsHost.executeCommand("readbyscreen bondparamset");
//                Map<String, String> bondResultMap = (Map<String, String>) JsonMapper.fromJsonString(bondresult.get(0), Map.class);
//                resultMap.putAll(bondResultMap);
                }

            }
        }
        return resultMap;
    }


    public Map<String, String> getEquipMonitorPara2(String recipeName) {

        Map<String, String> resultMap = new HashMap();
        BufferedReader br = null;
        try {
            //登陆权限
//调转至setup界面，可能会是select界面
//1.setup界面时需要判断recipe前的第一个选项是否被打开read recipeflag1
// a.读取到“RECIPE”则
// b.未读取到“RECIPE”则点击第一选项，再次读取read recipeflag1
//继续读取read recipemanager,,recipe选项是否被打开
//到select界面，选择行数，load

            List<String> equipRecipeList = getRecipeList();
            String recipeRow = "";
            //查询recipe是否可以选中，并找出recipe位置信息
            for (int i = 0; i < equipRecipeList.size(); i++) {
                if (recipeName.equals(equipRecipeList.get(i))) {
                    recipeRow = "r" + (i + 1) + "_sel";
                    break;
                }
            }
            if ("".equals(recipeRow)) {
                return resultMap;
            }
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                List<String> logFlag = iSecsHost.executeCommand("read user");
//                if (logFlag != null && !logFlag.isEmpty()) {
//                    if (!"SA".equalsIgnoreCase(logFlag.get(0))) {
//                        List<String> logResult = iSecsHost.executeCommand("playback login.txt");
//                        if (!"done".equals(logResult.get(0))) {
//                            return "选中失败,登陆失败!";
//                        }
//                    }
//                }
                if (!checkUserAndLogin()) {
                    return resultMap;
                }
                List<String> gotosetup = iSecsHost.executeCommand("playback gotosetup.txt");
                Thread.sleep(300);
                gotosetup = iSecsHost.executeCommand("goto setup");
                List<String> screen = iSecsHost.executeCommand("read setupflag");
                if ("assist".equalsIgnoreCase(screen.get(0))) {
                    List<String> recipeflag1 = iSecsHost.executeCommand("read recipeflag1");
                    if (!"Recipe".equalsIgnoreCase(recipeflag1.get(0))) {
                        List<String> clickAssist = iSecsHost.executeCommand("playback clickassist.txt");

                    }
                    List<String> recipeManager = iSecsHost.executeCommand("read recipemanager");
                    if (!"recipe manager".equalsIgnoreCase(recipeManager.get(0))) {
                        List<String> clickRecipe = iSecsHost.executeCommand("playback clickrecipe.txt");
                    }
                    List<String> clickRecipe = iSecsHost.executeCommand("playback clickrecipemanager.txt");
                }
                Thread.sleep(300);
                screen = iSecsHost.executeCommand("curscreen");
//                if ("select".equals(screen.get(0))) {
                List<String> selectresult = iSecsHost.executeCommand("playback " + recipeRow + ".txt");
                selectresult = iSecsHost.executeCommand("playback clickprintrecipe.txt");
//                iSecsHost.executeCommand("dos $del D:\\data\\recipeprintout\\dbrecipe.*$");
                iSecsHost.executeCommand("write printrcp " + recipeName);
                selectresult = iSecsHost.executeCommand("playback clickprintok.txt");
                iSecsHost.executeCommand("playback clickprintyes.txt");
//                Thread.sleep(50 * 1000);
                Date oldDate1 = new Date();
                Date newDate1 = null;
                //region 查询窗口弹出
                while (true) {
                    newDate1 = new Date();
                    logger.info((newDate1.getTime() - oldDate1.getTime()) + "，检测时间为：");
                    if ((newDate1.getTime() - oldDate1.getTime()) < (50 * 1000)) {
                        //间隔0.5s
                        Thread.sleep(500);
                        //读取弹出窗的标识“OK”
                        selectresult = iSecsHost.executeCommand("read printflag");
                        if ("clear".equalsIgnoreCase(selectresult.get(0))) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                //上传打印文件
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //ocr上传至本地
                boolean ocrUploadOk = true;
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "D:\\data\\RecipePrintout" + "\" \"" + clientFtpRecipeRelativePath + "\" \"mput " + recipeName + ".* \"";
                List<String> result = iSecsHost.executeCommand(command);
                iSecsHost.executeCommand("playback gotomain.txt");

                br = new BufferedReader(new FileReader("D:\\RECIPE" + clientFtpRecipeRelativePath+"\\"+recipeName+".txt"));
                String line = "";

                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                sqlSession.close();
                while ((line = br.readLine()) != null) {
                     for(RecipeTemplate r:recipeTemplates){
                         String paraName = r.getParaName();
                         if(line.contains(paraName)){
                         String temp = line.replace(paraName,"");
                         String value = temp.substring(0,temp.indexOf("[")).trim();
                         resultMap.put(paraName,value);
                         }
                     }
                }
            }
            //退出权限
            iSecsHost.executeCommand("replay logout.exe");
            return resultMap;
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return resultMap;
        }
    }

    public Map getEquipMonitorPara3() {
//        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(getCurrentRecipeName()).get("recipeParaList");
//        Map<String, String> resultMap = new HashMap<>();
//        for (RecipePara recipePara : recipeParaList) {
//            resultMap.put(recipePara.getParaName(), recipePara.getSetValue());
//        }
//        logger.info("monitormap:" + resultMap.toString());
//        return resultMap;

        //获取界面上的参数值

        // TODO: 2018/3/19 获取参数
        Map resultMap = new HashMap();
        if (getPassport()) {
            Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
            synchronized (socketClient) {
                if (!checkUserAndLogin()) {
                    logger.error("未登陆权限，获取数据失败！");
                    return resultMap;
                }
                //获取参数
                iSecsHost.executeCommand("playback gotoparamset.txt");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                iSecsHost.executeCommand("goto paramset");
                //读取界面的数值,看是否其他界面被打开了
                List<String> pickflag1 = new ArrayList();
                List<String> pickflag2 = new ArrayList();
                int i = 0;
                boolean haveWaferMapping = false;
                do {
                    //TODO 此时界面会有两种情况，，有waferMapping和无waferMapping

                    pickflag1 = iSecsHost.executeCommand("read pickflag");
                    //有mapping标识
                    pickflag2 = iSecsHost.executeCommand("read pickflag2");
                    //其他界面被打开需要关闭:将第一个页面打开，再开闭，以使页面全部关闭，之后点击“pick&place”
                    if (!"pick&place".equalsIgnoreCase(pickflag1.get(0).replaceAll(" ", ""))
                            && !"pick&place".equalsIgnoreCase(pickflag2.get(0).replaceAll(" ", ""))) {
                        //看第一个标题界面是否被打开，打开则关闭，未打开则点击两次
                        List<String> waferflag = iSecsHost.executeCommand("read waferflag");
                        try {
                            if ("wafer".equalsIgnoreCase(waferflag.get(0))) {
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                                Thread.sleep(300);
                                waferflag = iSecsHost.executeCommand("read waferflag");
                                if ("wafermapping".equalsIgnoreCase(waferflag.get(0).replaceAll(" ", ""))) {
                                    haveWaferMapping = true;
                                }
                            } else if ("wafermapping".equalsIgnoreCase(waferflag.get(0).replaceAll(" ", ""))) {
                                haveWaferMapping = true;
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                                Thread.sleep(300);
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                            } else {
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                                Thread.sleep(300);
                                iSecsHost.executeCommand("playback clickwaferhandler.txt");
                            }
                        } catch (InterruptedException e1) {
                            logger.debug(e1);
                        } catch (Exception e2) {
                            logger.debug(e2);
                        }
                    } else {
                        if ("pick&place".equalsIgnoreCase(pickflag1.get(0).replaceAll(" ", ""))) {
                            haveWaferMapping = true;
                        }
                        break;
                    }
                    i++;
                } while (i < 2);
                //此时判断是否有wafermapping标签
                if (haveWaferMapping) {
                    //读取pickupflag，判断“pick&place”是否被打开
                    List<String> pickupflag = iSecsHost.executeCommand("read pickupflag");
                    if (!"pickup".equalsIgnoreCase(pickupflag.get(0))) {
                        iSecsHost.executeCommand("playback clickpickplace.txt");
                    }
                    //点击“Extended Pickup Process”，并读取数值
                    iSecsHost.executeCommand("playback clickpickup.txt");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //2k
                    iSecsHost.executeCommand("goto pickparamset");
                    List<String> pickupresult = iSecsHost.executeCommand("readm dtd pwt pf nth nwt nv");
                    resultMap.put("dtd", pickupresult.get(0));
                    resultMap.put("pwt", pickupresult.get(1));
                    resultMap.put("pf", pickupresult.get(2));
                    resultMap.put("nth", pickupresult.get(3));
                    resultMap.put("nwt", pickupresult.get(4));
                    resultMap.put("nv", pickupresult.get(5));
//                List<String> pickupresult = iSecsHost.executeCommand("readbyscreen pickparamset");
//                Map<String, String> pickupResultMap = (Map<String, String>) JsonMapper.fromJsonString(pickupresult.get(0), Map.class);
//                resultMap.putAll(pickupResultMap);
                    //点击“Bond Process”,并读取数值
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("playback clickbond.txt");
                    try {
                        Thread.sleep(2300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("goto bondparamset");
                    List<String> bondresult = iSecsHost.executeCommand("readm dtp bf prest postst");
                    resultMap.put("dtp", bondresult.get(0));
                    resultMap.put("bf", bondresult.get(1));
                    resultMap.put("prest", bondresult.get(2));
                    resultMap.put("postst", bondresult.get(3));
//                List<String> bondresult = iSecsHost.executeCommand("readbyscreen bondparamset");
//                Map<String, String> bondResultMap = (Map<String, String>) JsonMapper.fromJsonString(bondresult.get(0), Map.class);
//                resultMap.putAll(bondResultMap);
                } else {
                    //读取pickupflag，判断“pick&place”是否被打开
                    List<String> pickupflag = iSecsHost.executeCommand("read pickupflag2");
                    if (!"pickup".equalsIgnoreCase(pickupflag.get(0))) {
                        iSecsHost.executeCommand("playback clickpickplace2.txt");
                    }
                    //点击“Extended Pickup Process”，并读取数值
                    iSecsHost.executeCommand("playback clickpickup2.txt");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //2k
                    iSecsHost.executeCommand("goto pickparamset");
                    List<String> pickupresult = iSecsHost.executeCommand("readm dtd pwt pf nth nwt nv");
                    resultMap.put("dtd", pickupresult.get(0));
                    resultMap.put("pwt", pickupresult.get(1));
                    resultMap.put("pf", pickupresult.get(2));
                    resultMap.put("nth", pickupresult.get(3));
                    resultMap.put("nwt", pickupresult.get(4));
                    resultMap.put("nv", pickupresult.get(5));
//                List<String> pickupresult = iSecsHost.executeCommand("readbyscreen pickparamset");
//                Map<String, String> pickupResultMap = (Map<String, String>) JsonMapper.fromJsonString(pickupresult.get(0), Map.class);
//                resultMap.putAll(pickupResultMap);
                    //点击“Bond Process”,并读取数值
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("playback clickbond2.txt");
                    try {
                        Thread.sleep(2300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    iSecsHost.executeCommand("goto bondparamset");
                    List<String> bondresult = iSecsHost.executeCommand("readm dtp bf prest postst");
                    resultMap.put("dtp", bondresult.get(0));
                    resultMap.put("bf", bondresult.get(1));
                    resultMap.put("prest", bondresult.get(2));
                    resultMap.put("postst", bondresult.get(3));
//                List<String> bondresult = iSecsHost.executeCommand("readbyscreen bondparamset");
//                Map<String, String> bondResultMap = (Map<String, String>) JsonMapper.fromJsonString(bondresult.get(0), Map.class);
//                resultMap.putAll(bondResultMap);
                }

            }
        }
        return resultMap;
    }

    public List<RecipePara> getRecipeParasFromMonitorMap2(String recipeName) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        Map<String, String> paraChangeMap = getEquipMonitorPara2(recipeName);
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if ((paraChangeMap.get(recipeTemplate.getParaName()) != null)) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setSetValue(paraChangeMap.get(recipeTemplate.getParaName()));
                recipeParas.add(recipePara);
            }
        }
        return recipeParas;
    }


    public List<RecipePara> getRecipeParasFromMonitorMap111() {

        //上传日志文件
        //从日志文件中获取修改的参数
        String logPath = "D:\\data\\log\\OperatorLog\\";
        String localLogPath = GlobalConstants.ftpPath + "/log/" + deviceCode + "/";
        File file = new File(localLogPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        List<String> parachangeResult = null;
        List<RecipePara> recipeParas = null;
        String command = "ftp " + GlobalConstants.clientInfo.getClientIp() + GlobalConstants.ftpUser + " " + GlobalConstants.ftpPwd + " " + "\"" + logPath + "\" \"" + localLogPath + "\" \"mput Operator0001.log\"";
        List<String> result = iSecsHost.executeCommand(command);
        String filePath = GlobalConstants.localRecipePath + "RECIPE/" + localLogPath + "Operator0001.log";
        BufferedReader br = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String s = "";
            String date = new SimpleDateFormat("d.MMM").format(new Date());
            while ((s = br.readLine()) != null) {
                if (s.contains("PARAMETER changed") && s.contains(date)) {
                    String paraline = s;
                    parachangeResult.add(paraline);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //ocr dos 获取
//        List<String> parachangeResult = iSecsHost.executeCommand("dos $findstr \"PARAMETER changed\" D:\\data\\log\\OperatorLog\\Operator0001.log$");
        Map<String, String> paraChangeMap = new HashMap<>();
        for (String s : parachangeResult) {
            String[] ss = s.split(",");
            String[] values = ss[3].split("=");
            String value = values.length < 2 ? "" : values[1];
            DecimalFormat decimalFormat = new DecimalFormat("##########.##########");
            String numConverted = "";
            if (!"".equals(value)) {
                if (value.matches("^-?[0-9]+(.[0-9]+)?$")) {
                    if (value.contains(".")) {
                        numConverted = decimalFormat.format(Double.parseDouble(value));
                    } else {
                        numConverted = decimalFormat.format(Integer.parseInt(value));
                    }
                }
            }
            paraChangeMap.put(ss[1].replace("'", "").trim(), numConverted);
        }
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if ((paraChangeMap.get(recipeTemplate.getParaName()) != null)) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setSetValue(paraChangeMap.get(recipeTemplate.getParaName()));
                recipeParas.add(recipePara);
            }
        }
        return recipeParas;
    }


    @Override
    public Map getEquipRecipeList() {

        List<String> equipRecipeList = new ArrayList<>();
        Map eppd = new HashMap();
        //机台本地recipe路径
        equipRecipeList = getRecipeList();
        eppd.put("eppd", equipRecipeList);
        return eppd;
    }

    public List getRecipeList() {
        List<String> equipRecipeList = new ArrayList<>();
        String equipLocalRecipPath = this.getEquipLocalRecipePath();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = iSecsHost.executeCommand("dos $dir " + "\"" + equipLocalRecipPath + File.separator + "*.dbrcp" + "\" /b$");
            for (String recipeName : result) {
                if (!"done".equals(recipeName) && !"".equals(recipeName)) {
                    equipRecipeList.add(recipeName.replace(".dbrcp", ""));
                }
            }
        }
        return equipRecipeList;
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            if (!isInitState) {
                getEquipRealTimeState();
                isInitState = true;
                break;
            }
        }
    }

    @Override
    public Object clone() {
        ESEC2008Host newEquip = new ESEC2008Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
//        newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        // TODO:查询报警
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> alarmflag = iSecsHost.executeCommand("read alarmflag");
                if ("clear".equalsIgnoreCase(alarmflag.get(0))) {
                    List<String> alarmid = iSecsHost.executeCommand("read alarmid");
                    if (alarmid.size() > 1) {
                        alarmStrings.add(alarmid.get(0));
                        logger.info("Get alarm ALID=[" + alarmid.get(0) + "]");
                    }
                }
//                for (String colorstr : result) {
//                    if ("0xc0c0c0".equals(colorstr)) {
//                        alarmStrings.add("");
//                    }
//                    if ("0xff0000".equals(colorstr)) {
//                        logger.info("The equip state changged to alarm...");
//                        List<String> resultAlarmId = iSecsHost.executeCommand("read alarmid");
//                        if (resultAlarmId.size() > 1) {
//                            alarmStrings.add(resultAlarmId.get(0));
//                            logger.info("Get alarm ALID=[" + resultAlarmId.get(0) + "]");
//                        }
//                    }
//                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage(), e);
            }
        }
        return alarmStrings;
    }

    @Override
    public String getMTBA() {
        List<String> result = new ArrayList<>();
        String mtba = "";
        try {
            result = iSecsHost.executeCommand("read mtba");
            if (result != null && result.size() > 1) {
                logger.info("Get the MTBA from equip:" + deviceCode + " MTBA:[" + result.get(0) + "");
                mtba = result.get(0);
            }
        } catch (Exception e) {
            logger.error("Get Equip MTBA error:" + e.getMessage(), e);
        }
        return mtba;
    }

    @Override
    public String getEquipStatus() {
        // TODO: 2018/3/19 获取机台状态
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> result = this.sendMsg2Equip("read status");
                if (result != null && !result.isEmpty()) {
                    if (!result.get(0).contains("error") && !"done".equalsIgnoreCase(result.get(0))) {
                        equipStatus = result.get(0).toUpperCase();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Get equip status error:" + e.getMessage(), e);
        } finally {
            Map map = new HashMap();
            map.put("EquipStatus", equipStatus);
            map.put("PPExecName", ppExecName);
            changeEquipPanel(map);
        }
        return equipStatus;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + ".dbrcp", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".dbrcp 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        //this.deleteTempFile(recipeName);
        return true;
    }

    private void cleanRootDir() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\*\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        logger.info("root 目录清理完成");
                    }
                }
            } catch (Exception e) {
                logger.error("目录清理时发生异常:" + e.getMessage(), e);
            }
        }
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

    private boolean checkUserAndLogin() {
        List<String> logFlag = iSecsHost.executeCommand("read user");
        if (logFlag != null && !logFlag.isEmpty()) {
            if (!"SA".equalsIgnoreCase(logFlag.get(0))) {
                iSecsHost.executeCommand("playback gotomain.txt");
//                List<String> logResult = iSecsHost.executeCommand("action login");
//                try {
//                    Thread.sleep(1500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                logResult = iSecsHost.executeCommand("write username SA");
//                logResult = iSecsHost.executeCommand("write password lIIlIllIl0OlIlO01OIlllI0OI");
//                try {
//                    Thread.sleep(800);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                logResult = iSecsHost.executeCommand("playback loginok.txt");
                List<String> logResult = iSecsHost.executeCommand("replay login1.exe");
                logResult = iSecsHost.executeCommand("replay login2.exe");
                if ("done".equals(logResult.get(0))) {
                    return true;
                } else {
                    return false;
                }
            } else if ("SA".equalsIgnoreCase(logFlag.get(0)) || "ME".equalsIgnoreCase(logFlag.get(0))) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static void main(String[] args) {

//上传日志文件
        //从日志文件中获取修改的参数
//        String logPath = "D:\\data\\log\\OperatorLog\\";
//        String localLogPath = GlobalConstants.ftpPath + "/log/" + deviceCode + "/";
        List<String> parachangeResult = new ArrayList<>();
//        List<RecipePara> recipeParas = null;
//        String command = "ftp " + GlobalConstants.clientInfo.getClientIp() + GlobalConstants.ftpUser + " " + GlobalConstants.ftpPwd + " " + "\"" + logPath + "\" \"" + localLogPath + "\" \"mput Operator0001.log\"";
        String filePath = "D:\\DB-800HSDRecipe\\2008输入密码replay\\log\\OperatorLog\\test2008\\Operator0001.log";
        String readRecipe = "D:\\Data\\Recipe\\BD-AFJS7431200-DW(U6).dbrcp";
        String recipeName = readRecipe.substring(readRecipe.lastIndexOf("\\")+1,readRecipe.lastIndexOf("."));
        System.out.println(recipeName);
        if(true){
            return;
        }

        BufferedReader br = null;
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
//        sqlSession.close();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String s = "";
            while ((s = br.readLine()) != null) {
                if (s.contains("PARAMETER changed")) {
                    String paraline = s;
                    parachangeResult.add(paraline);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //ocr dos 获取
//        List<String> parachangeResult = iSecsHost.executeCommand("dos $findstr \"PARAMETER changed\" D:\\data\\log\\OperatorLog\\Operator0001.log$");
        Map<String, String> paraChangeMap = new HashMap<>();
        for (String s : parachangeResult) {
            String[] ss = s.split(",");
            String[] values = ss[3].split("=");
            String value = values.length < 2 ? "" : values[1];
            DecimalFormat decimalFormat = new DecimalFormat("##########.##########");
            String numConverted = "";
            if (!"".equals(value)) {
                if (value.matches("^-?[0-9]+(.[0-9]+)?$")) {
                    if (value.contains(".")) {
                        numConverted = decimalFormat.format(Double.parseDouble(value));
                    } else {
                        numConverted = decimalFormat.format(Integer.parseInt(value));
                    }
                }
            }
            paraChangeMap.put(ss[1].replace("'", "").trim(), numConverted);
        }
//        for (RecipeTemplate recipeTemplate : recipeTemplates) {
//            if ((paraChangeMap.get(recipeTemplate.getParaName()) != null)) {
//                RecipePara recipePara = new RecipePara();
//                recipePara.setParaName(recipeTemplate.getParaName());
//                recipePara.setParaCode(recipeTemplate.getParaCode());
//                recipePara.setSetValue(paraChangeMap.get(recipeTemplate.getParaName()));
//                recipeParas.add(recipePara);
//            }
//        }
//        return recipeParas;
        for (Map.Entry<String, String> entry : paraChangeMap.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }

    }

}