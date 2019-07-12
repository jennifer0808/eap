package cn.tzauto.octopus.isecsLayer.equipImpl.taisheng;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ER604Host extends EquipModel {

    private Logger logger = Logger.getLogger(ER604Host.class);

    public ER604Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand("read recipeName");
                if (list != null && !list.isEmpty()) {
                    ppExecName = list.get(0);
                }
            } catch (Exception e) {
                logger.info("设备:" + deviceCode + "读取recipeName失败" + e);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> screen = iSecsHost.executeCommand("curscreen");
                if (screen != null && !screen.isEmpty()) {
                    //检测是否在手动界面
                    if ("auto".equals(screen.get(0))) {
                        iSecsHost.executeCommand("goto manual");
                    }
                }
                iSecsHost.executeCommand("playback start.txt");
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "开机失败" + e);
                return "start equip failed";
            }

        }
        return "0";
    }

    @Override
    public String pauseEquip() {
        return null;
    }

    @Override
    public String stopEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> screen = iSecsHost.executeCommand("curscreen");
                if (screen != null && !screen.isEmpty()) {
                    //检测是否在手动界面
                    if ("auto".equals(screen.get(0))) {
                        iSecsHost.executeCommand("goto manual");
                    }
                }
                iSecsHost.executeCommand("playback stop.txt");
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "停机失败" + e);
                return "stop equip failed";
            }

        }
        return "0";
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map map = new HashMap();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        getClientFtpRecipeAbsolutePath(recipeName);

        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"put $" + recipeName + ".prc$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand(cmd);
                for (String s : result) {
                    if (s != null && s.contains("success")) {
                        map.put("recipe", setRecipe(recipeName));
                        map.put("deviceCode", deviceCode);
                        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                        sqlSession.close();
                        map.put("recipeParaList", RecipeUtil.transfer2DB(new RecipeFileHandler() {
                            @Override
                            public Map<String, String> handler(File file) {
                                Map<String, String> resultMap = new HashMap<>();
                                BufferedReader bufferedReader = null;
                                try {
                                    bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                                    int i = 1;
                                    String line = bufferedReader.readLine();
                                    while (line != null) {
                                        if (i == 15) {
                                            //resultMap.put("",line);
                                        } else if (i == 17) {
                                            resultMap.put("Power attenuation percentage", line);
                                        } else if (i == 19) {
                                            resultMap.put("Vacuum Pressure", line);
                                        } else if (i == 20) {
                                            resultMap.put("Plasma Clean Time", line);
                                        } else if (i == 21) {
                                            resultMap.put("Power", line);
                                        } else if (i == 22) {
                                            resultMap.put("Low Vaccum Pressure", line);
                                        }
                                        if (i == 23) {
                                            break;
                                        }
                                        line = bufferedReader.readLine();
                                        i++;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                        bufferedReader.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                return resultMap;
                            }
                        }, new File(getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".prc"), recipeTemplates, false));
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + "成功。");
                        break;
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                    }
                }
            } catch (Exception e) {
                logger.info("设备:" + deviceCode + "上传recipe失败" + e);
            }
        }
        return map;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String downLoadResult = "1";
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName, hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"get $" + recipeName + ".prc$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand(cmd);
                for (String s : result) {
                    if (s != null && s.contains("success")) {
                        downLoadResult = "0";
                    }
                }
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "下载" + recipeName + "失败" + e);
            }
        }
        return downLoadResult;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //压缩文件后的目录
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        //String clientZipFilePath = GlobalConstants.localRecipePath + GlobalConstants.ftpPath + recipeName ;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientAbsoluteFtpPath + recipeName + ".prc", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            deleteTempFile(recipeName);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        Map map = getEquipRecipeList();
        List<String> recipeList = (List<String>) map.get("eppd");
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //机台本地recipe路径
            try {
                if (!recipeList.contains(recipeName)) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                } else {
                    List<String> result = iSecsHost.executeCommand("dos $del /q \"" + equipRecipePath + "\\" + recipeName + ".prc\"$");
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
        String selectResult = "0";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> screen = iSecsHost.executeCommand("curscreen");
            if (screen != null && !screen.isEmpty()) {
                String curscreen = screen.get(0);
                if (!"auto".equals(curscreen)) {
                    iSecsHost.executeCommand("playback gotoAuto.txt");
                }
            }
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback openSR.txt");
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback clickInput.txt");//点击输入框
            iSecsHost.executeCommand("write recipe " + recipeName);
        }
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            List<String> rcpList1 = iSecsHost.executeCommand("read rcp_1");
//            if(rcpList1 != null && !rcpList1.isEmpty()) {
//                if(recipeName.equals(rcpList1.get(0))) {
//                    iSecsHost.executeCommand("playback rcp_1.txt");
//                }else{
//                    List<String> rcpList2 = iSecsHost.executeCommand("read rcp_2");
//                    if(rcpList2 != null && !rcpList2.isEmpty()) {
//                        if(recipeName.equals(rcpList2.get(0))) {
//                            iSecsHost.executeCommand("playback rcp_2.txt");
//                        }else {
//                            List<String> rcpList3 = iSecsHost.executeCommand("read rcp_3");
//                            if(rcpList3 != null && !rcpList3.isEmpty()) {
//                                if(recipeName.equals(rcpList3.get(0))) {
//                                    iSecsHost.executeCommand("playback rcp_3.txt");
//                                }else {
//                                    selectResult = "1";
//                                    logger.info("选中rcp失败，设备不存在该rcp。");
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if ("1".equals(selectResult)) {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                iSecsHost.executeCommand("playback clickCancel.txt");
            }
        } else {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                iSecsHost.executeCommand("playback clickOK.txt");
            }
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotoManual.txt");
        }
        return selectResult;
        //判断是否选中成功
//        String currentRecipe = getCurrentRecipeNam e();
//        if(recipeName.equals(currentRecipe)) {
//            return "0";
//        }else{
//            return "选中失败";
//        }
    }

    @Override
    public Map getEquipRealTimeState() {
        Map map = new HashMap();
        map.put("PPExecName", getCurrentRecipeName());
        map.put("EquipStatus", getEquipStatus());
        map.put("controlState", controlState);
        return map;
    }

    @Override
    public Map getEquipMonitorPara() {
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        Map<String, Object> eppd = new HashMap<>();
        try {
            List<String> recipeList = getRecipeList();
            eppd.put("eppd", recipeList);
        } catch (Exception e) {
            logger.info("设备:" + deviceCode + "repipe列表获取失败！" + e);
        }
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
//                List<String> screen = iSecsHost.executeCommand("curscreen");
//                if (screen != null && !screen.isEmpty()) {
//                    //检测是否在手动界面
//                    if ("auto".equals(screen.get(0))) {
//                        iSecsHost.executeCommand("playback gotoManual.txt");
//                    }
//                }
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    String curscreen = result.get(0);
                    if ("idle".equals(curscreen)) {
                        equipStatus = "Idle";
                    } else if ("run".equals(curscreen)) {
                        equipStatus = "Run";
                    } else {
                        equipStatus = "Error";
                    }
                }
            } catch (Exception e) {
                logger.info("设备:" + deviceCode + "获取状态失败" + e);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        return equipStatus;
    }

    @Override
    public Object clone() {
        ER604Host er604Host = new ER604Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        er604Host.startUp = startUp;
        clear();
        return er604Host;
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
    public List<String> getEquipAlarm() {
        return null;
    }

    private List<String> getRecipeList() {
        ArrayList<String> recipeList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand("dos $dir " + "\""
                        + this.equipRecipePath + File.separator + "*.prc" + "\" /b$");
                for (String recipe : resultList) {
                    if (!"".equals(recipe) && !"done".equals(recipe)) {
                        recipeList.add(recipe.replace(".prc", ""));
                    }
                }
            } catch (Exception ex) {
                logger.info("获取recipe列表失败" + ex);
            }
        }

        return recipeList;
    }

    /**
     * 创建并获取recipe上传相对路径
     */
    private String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    /**
     * 获取工控机recipe绝对路径
     */
    private String getClientFtpRecipeAbsolutePath(String recipeName) {

        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File recipeDir = new File(filePath);
        if (!recipeDir.exists()) {
            recipeDir.mkdirs();
        }
        return filePath;
    }

    /**
     * 获取recipe在服务端的路径
     *
     * @param recipe
     * @return
     */
    private String getHostDownloadFtpRecipeFilePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String downloadFullFilePath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        sqlSession.close();
        return downloadFullFilePath;
    }

    @Override
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        String recipeName = getCurrentRecipeName();
        if(StringUtils.isBlank(recipeName) || recipeName.contains("error")) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "获取当前recipe参数失败,无法获取当前recipe名称。");
            return null;
        }
        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(recipeName).get("recipeParaList");
        if(recipeParaList != null) {
            return recipeParaList;
        }
        return new ArrayList<>();
    }

}
