/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.towa;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.towa.TowaISECSRecipeEdit;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.resolver.towa.TowaRecipeUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
 * @author luosy
 */
public class Y1R1060Host extends EquipModel {

    private static Logger logger = Logger.getLogger(Y1R1060Host.class.getName());
    private ISecsHost towaCtrlHost;

    public Y1R1060Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        towaCtrlHost = new ISecsHost(remoteIPAddress, String.valueOf(12005), deviceType, deviceCode);
        iSecsHostList.clear();
        iSecsHostList.add(iSecsHost);
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read mainrecipename").get(0);
                    } else if ("any".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read anyrecipename").get(0);
                    } else if ("recipeoption1".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipeoption1recipename").get(0);
                    } else if ("recipeoption2".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipeoption2recipename").get(0);
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        if ("".equals(ppExecName) || ppExecName.contains("rror")) {
            ppExecName = "--";
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        return ppExecName;
    }

    @Override
    public String pauseEquip() {
        String stopResult = "";
        synchronized (towaCtrlHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    result = towaCtrlHost.executeCommand("stop");
                    for (String string : result) {
                        if ("Y".equals(string)) {
                            return "0";
                        }
                    }
                } catch (Exception e) {
                }
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
                stopResult = "未设置锁机！";
            }
        }
        return stopResult;
    }

    @Override
    public String stopEquip() {
        String stopResult = "";
        synchronized (towaCtrlHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    result = towaCtrlHost.executeCommand("stop");
                    for (String string : result) {
                        if ("Y".equals(string)) {
                            return "0";
                        }
                    }
                } catch (Exception e) {
                }
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
                stopResult = "未设置锁机！";
            }
        }
        return stopResult;
    }

    @Override
    public String lockEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean findRecipe(String recipeName) {
        List<String> resulttmp = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + "\\ /a/w\"");
        if (resulttmp != null && resulttmp.size() > 1) {
            for (String str : resulttmp) {
                if (str.contains(".rsp")) {
                    String[] recipeNameTmps = str.split("\\s");
                    for (String recipeNameTmp1 : recipeNameTmps) {
                        if (recipeNameTmp1.contains(".rsp")) {
                            String recipeNameTmp = recipeNameTmp1.replaceAll(".rsp", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
                            if (recipeName.equals(recipeNameTmp)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                // List<String> result0 = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeName + ".rsp\"");
                // List<String> result1 = iSecsHost.executeCommand("playback uploadrecipe.txt");
                if (findRecipe(recipeName)) {
                    String localftpip = GlobalConstants.clientInfo.getClientIp();
                    String ftpip = GlobalConstants.ftpIP;
                    String ftpPort = GlobalConstants.ftpPort;
                    String ftpUser = GlobalConstants.ftpUser;
                    String ftpPwd = GlobalConstants.ftpPwd;
                    Recipe recipe = setRecipe(recipeName);
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                    sqlSession.close();
                    String equipRecipePathtmp = equipRecipePath;
                    TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");
                    boolean ocrUploadOk = true;
                    List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                            + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "\\" + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                            + recipeName + ".rsp\"");
                    for (String uploadstr : result) {
                        if (uploadstr.contains("rror") || uploadstr.contains("Not connected")) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                            resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                            return resultMap;
                        }
                        if ("done".equals(uploadstr)) {
                            List<RecipePara> recipeParaList = new ArrayList<>();
                            try {
                                Map paraMap = TowaRecipeUtil.transferFromFile4Isecs(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".rsp");
                                if (paraMap != null && !paraMap.isEmpty()) {
                                    recipeParaList = TowaRecipeUtil.transferFromDB4Isecs(paraMap, deviceType);
                                } else {
                                    logger.error("解析recipe时出错,recipe文件不存在");
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            resultMap.put("recipe", recipe);
                            resultMap.put("deviceCode", deviceCode);
                            resultMap.put("recipeFTPPath", ftpRecipePath);
                            resultMap.put("recipeParaList", recipeParaList);
                        }
                        if (uploadstr.contains("Not connected")) {
                            ocrUploadOk = false;
                        }
                    }
                    if (!ocrUploadOk) {
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        cleanRecipe("");
        return resultMap;
    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        Attach attach = new Attach();
        attach.setId(UUID.randomUUID().toString());
        attach.setRecipeRowId(recipe.getId());
        attach.setAttachPath(ftpRecipePath);
        attach.setAttachName(recipeName + ".rsp_V" + recipe.getVersionNo());
        attach.setAttachType("");
        attach.setSortNo(0);
        if (GlobalConstants.sysUser != null) {
            attach.setCreateBy(GlobalConstants.sysUser.getId());
            attach.setUpdateBy(GlobalConstants.sysUser.getId());
        } else {
            attach.setCreateBy("System");
            attach.setUpdateBy("System");
        }
        attachs.add(attach);

        sqlSession.close();
        return attachs;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        cleanRecipe(recipe.getRecipeName());
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            try {
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);
                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                    return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                }
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", ftpPathTmp + recipe.getRecipeName() + ".rsp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/src.rsp");

                    List<String> edits = TowaISECSRecipeEdit.getUniqueRecipeParaMap(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/src.rsp");
                    Thread.sleep(1000);
                    TowaISECSRecipeEdit.writeRecipeFile(edits, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp");
                    Thread.sleep(1000);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", ftpPathTmp + recipe.getRecipeName() + ".rsp", ftpip, ftpPort, ftpUser, ftpPwd);
                    FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/src.rsp");
                    List<String> edits = TowaISECSRecipeEdit.getUniqueRecipeParaMap(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/src.rsp");

                    Thread.sleep(1000);
                    TowaISECSRecipeEdit.writeRecipeFile(edits, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp");
                    Thread.sleep(1000);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.rsp");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.rsp", deviceType);
                        //  RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + "\\ " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".rsp\"");
                //执行导入
                List<String> result1 = iSecsHost.executeCommand("playback downloadrecipe.txt");
                for (String str : result) {
                    if (str.contains("rror")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + " 时失败,请检查FTP服务是否开启.");
                        return "下载Recipe:" + recipe.getRecipeName() + "时失败,请检查FTP服务是否开启.";
                    }
                    if ("done".equals(str)) {
                        return "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (IOException e) {
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
                return "下载失败,出现异常:" + e.getMessage();
            } catch (InterruptedException ex) {
                logger.error(ex.getMessage());
            } finally {
                sqlSession.close();
                this.deleteTempFile(recipe.getRecipeName());
            }
        }
        return "Download recipe " + recipe.getRecipeName() + " failed";
    }

    @Override
    public String deleteRecipe(String recipeName) {
        deleteAllRcpFromDevice(recipeName);
        return "0";
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                for (int i = 1; i < 9; i++) {
//
//                    List<String> result = iSecsHost.executeCommand("playback gotodelrecipe.txt");
//                    Thread.sleep(500);
//                    iSecsHost.executeCommand("curscreen");
//                    List<String> delrecipeList = iSecsHost.executeCommand("read recipe" + i);
//                    if (recipeName.replaceAll("O", "0").equals(delrecipeList.get(0))) {
//                        List<String> result1 = iSecsHost.executeCommand("playback delrecipe" + i + ".txt");
//                        for (String str : result1) {
//                            if ("done".equals(str)) {
//                                ppExecName = recipeName;
//                                return "0";
//                            }
//                            if (str.contains("rror")) {
//                                return "删除失败";
//                            }
//                        }
//                    }
//                }
//                iSecsHost.executeCommand("playback cancledelrecipe.txt");
//                return "删除失败";
//            } catch (Exception e) {
//                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage());
//                iSecsHost.executeCommand("replay auto.exe");
//                return "删除失败";
//            }
//        }
    }

    @Override
    public void deleteAllRcpFromDevice(String currentRecipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
//                for (int j = 0; j < 2; j++) {
                for (int i = 1; i < 4; i++) {
                    List<String> result = iSecsHost.executeCommand("playback gotodelrecipe.txt");
                    Thread.sleep(500);
                    //iSecsHost.executeCommand("curscreen");
                    //  List<String> delrecipeList = iSecsHost.executeCommand("read recipe" + i);
//                    if (currentRecipeName.replaceAll("O", "0").equals(delrecipeList.get(0))) {
//                        continue;
//                    }
                    List<String> result1 = iSecsHost.executeCommand("playback delrecipe" + i + ".txt");
                }
                iSecsHost.executeCommand("playback cancledelrecipe.txt");
//                }
            } catch (Exception e) {
//                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage());
                iSecsHost.executeCommand("replay auto.exe");

            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                Thread.sleep(500);
                List<String> result = iSecsHost.executeCommand("playback gotoselrecipe.txt");
                Thread.sleep(500);
                // List<String> delrecipeList = iSecsHost.executeCommand("readm  recipe1 recipe2 recipe3 recipe4 recipe5 recipe6 recipe7 recipe8");
                for (String string : result) {
                    if ("done".equals(string)) {
                        for (int i = 1; i < 3; i++) {
                            iSecsHost.executeCommand("curscreen");
                            List<String> delrecipeList = iSecsHost.executeCommand("read selrecipe" + i);
                            for (String string1 : delrecipeList) {
                                //这里是用0（零）替换了o
                                if (recipeName.replaceAll("O", "0").equals(string1.replaceAll("O", "0").trim())) {
                                    List<String> result1 = iSecsHost.executeCommand("playback selrecipe" + i + ".txt");
                                    for (String str : result1) {
                                        if ("done".equals(str)) {
                                            ppExecName = recipeName;
                                            Thread.sleep(20000);
                                            for (int k = 1; k < 4; k++) {
                                                iSecsHost.executeCommand("playback gotodelrecipe.txt");
                                                Thread.sleep(500);
                                                iSecsHost.executeCommand("playback delrecipe" + k + ".txt");
                                                Thread.sleep(200);
                                            }
                                            iSecsHost.executeCommand("playback cancledelrecipe.txt");

                                            return "0";
                                        }
                                        if (str.contains("rror")) {
                                            return "选中失败";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                iSecsHost.executeCommand("playback cancledelrecipe.txt");
                return "选中失败";
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage());
                iSecsHost.executeCommand("replay auto.exe");
                return "选中失败";
            }
        }
    }

    @Override
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        List<RecipePara> recipeParas = (List<RecipePara>) getEquipMonitorPara().get("recipeParaList");
        if (recipeParas == null) {
            return new ArrayList<>();
        }
        return recipeParas;
    }

    @Override
    public Map getEquipMonitorPara() {
        FileUtil.delAllTempFile("D:\\" + deviceCode + ppExecName + "temp/");
        cleanRecipe("");
        iSecsHost.executeCommand("playback uploadrecipe.txt");
//        getEquipRecipeList();
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                Thread.sleep(4444);
                if (findRecipe(ppExecName)) {
                    String localftpip = GlobalConstants.clientInfo.getClientIp();
                    String ftpUser = GlobalConstants.ftpUser;
                    String ftpPwd = GlobalConstants.ftpPwd;
                    String equipRecipePathtmp = equipRecipePath;
                    TransferUtil.setPPBody(ppExecName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");
                    List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                            + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "\\" + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                            + ppExecName + ".rsp\"");
                    for (String uploadstr : result) {
                        if ("done".equals(uploadstr)) {
                            try {
                                Map paraMap = TowaRecipeUtil.transferFromFile4Isecs(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName + ".rsp");
                                if (paraMap != null && !paraMap.isEmpty()) {
                                    List<RecipePara> recipeParaList = TowaRecipeUtil.transferFromDB4Isecs(paraMap, deviceType);
                                    map.put("recipeParaList", recipeParaList);
                                } else {
                                    logger.error("解析recipe时出错,recipe文件不存在");
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        logger.info("monitormap:" + map.toString());
        deleteTempFile(ppExecName);
        return map;
    }

    @Override
    public Map getEquipRecipeList() {
        cleanRecipe("");
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result1 = iSecsHost.executeCommand("playback uploadrecipe.txt");
            for (String string : result1) {
                if ("done".equals(string)) {
                    try {
                        Thread.sleep(1500);
                        List<String> resulttmpList = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + "\\*.rsp /a/w\"");
                        if (resulttmpList != null && resulttmpList.size() > 1) {
                            for (String str : resulttmpList) {
                                if (str.contains(".rsp")) {
                                    String[] recipeNameTmps = str.split("\\s");
                                    for (int i = 0; i < recipeNameTmps.length; i++) {
                                        if (recipeNameTmps[i].contains(".rsp")) {
                                            String recipeNameTmp = recipeNameTmps[i].replaceAll(".rsp", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
                                            recipeNameList.add(recipeNameTmp);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return eppd;
                    }
                }
            }
            eppd.put("eppd", recipeNameList);
            return eppd;
        }
    }

    @Override
    public Object clone() {
        Y1R1060Host newEquip = new Y1R1060Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> params3 = iSecsHost.executeCommand("readm alarm  error ");
                if (params3.size() == 3) {
                    String alarm = params3.get(0);
                    String error = params3.get(1);
                    if (!"alarm".equals(alarm)) {
                        logger.info("The equip state changged to alarm...");
//                        alarmStrings.add(alarm);
                        logger.info("Get alarm ALID=[" + alarm + "]");
                    }
                    if (!"error".equals(error)) {
                        logger.info("The equip state changged to error...");
                        if (error.length() == 5) {
                            alarmStrings.add(error);
                        } else {
                            alarmStrings.add("");
                        }
                        logger.info("Get error alarm ALID=[" + error + "]");
                    }
                } else {
                    alarmStrings.add("");
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        if (alarmStrings.size() < 2 && "".equals(String.valueOf(alarmStrings.get(0)))) {
            if (preAlarm.equals(alarmStrings.get(0))) {
                return null;
            }
        }
        preAlarm = alarmStrings.get(0);
        return alarmStrings;
    }

    @Override
    public String getMTBA() {
        String mtba = "";
        try {
            List<String> result = iSecsHost.executeCommand("read mtba");
            if (result != null && result.size() > 1) {
                logger.info("Get the MTBA from equip:" + deviceCode + " MTBA:[" + result.get(0) + "");
                mtba = result.get(0);
            }
        } catch (Exception e) {
            logger.error("Get Equip MTBA error:" + e.getMessage());
        }
        return mtba;
    }

    @Override
    public String getEquipStatus() {
        if (towaCtrlHost.iSecsConnection.getSocketClient() != null) {
            synchronized (towaCtrlHost.iSecsConnection.getSocketClient()) {
                try {
                    List<String> result = towaCtrlHost.executeCommand("state");
                    if (result != null && !result.isEmpty()) {
                        equipStatus = result.get(0).replaceAll("done", "").replaceAll("STATE:", "");
                    }
                } catch (Exception e) {
                    logger.error("Get equip status error:" + e.getMessage());
                }
            }
        } else {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                try {
                    List<String> result = iSecsHost.executeCommand("read anystatus");
                    if (result != null && !result.isEmpty()) {
                        equipStatus = result.get(0).replaceAll("done", "");
                    }
                } catch (Exception e) {
                    logger.error("Get equip status error:" + e.getMessage());
                }
            }
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".rsp", remoteRcpPath, recipeName + ".rsp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void cleanRecipe(String keepRecipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + "\\ /a/w\"");

            if (result != null && result.size() > 1) {
                iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\*.rsp\"");
                for (String str : result) {
                    if (str.contains(".rsp")) {
                        String[] recipeNameTmps = str.split(".rsp");
                        for (int i = 0; i < recipeNameTmps.length; i++) {
                            String recipeNameTmp = recipeNameTmps[i].replaceAll(".rsp", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
                            if (keepRecipeName.equals(recipeNameTmp)) {
                                continue;
                            }
                            List<String> result0 = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeNameTmp + ".rsp\"");
                        }
                        String[] recipeNameTmps2 = str.split("\\s");
                        for (int i = 0; i < recipeNameTmps2.length; i++) {
                            String recipeNameTmp = recipeNameTmps2[i].replaceAll(".rsp", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
                            if (keepRecipeName.equals(recipeNameTmp)) {
                                continue;
                            }
                            List<String> result0 = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeNameTmp + ".rsp\"");
                        }
                    }
                }
            }
        }
    }

    private Map getShotCount() {
        Map shotCountMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotoshot.txt");
            iSecsHost.executeCommand("playback gotop1.txt");
            List<String> curscreens = iSecsHost.executeCommand("curscreen");
            for (String curscreen : curscreens) {
                if (curscreen.equals("press1")) {
                    List<String> shotcount = iSecsHost.executeCommand("readm p1s1 p1s2 ");
                    shotCountMap.put("p1s1", shotcount.get(0));
                    shotCountMap.put("p1s2", shotcount.get(1));
                }
            }
            iSecsHost.executeCommand("playback gotop2.txt");
            List<String> curscreens2 = iSecsHost.executeCommand("curscreen");
            for (String curscreen : curscreens2) {
                if (curscreen.equals("press2")) {
                    List<String> shotcount = iSecsHost.executeCommand("readm p2s1 p2s2 ");
                    shotCountMap.put("p2s1", shotcount.get(0));
                    shotCountMap.put("p2s2", shotcount.get(1));
                }
            }
            iSecsHost.executeCommand("playback gotop3.txt");
            List<String> curscreens3 = iSecsHost.executeCommand("curscreen");
            for (String curscreen : curscreens3) {
                if (curscreen.equals("press3")) {
                    List<String> shotcount = iSecsHost.executeCommand("readm p3s1 p3s2 ");
                    shotCountMap.put("p3s1", shotcount.get(0));
                    shotCountMap.put("p3s2", shotcount.get(1));
                    iSecsHost.executeCommand("playback gotomain.txt");
                }
            }
        }
        return shotCountMap;
    }

    private Map getPressStatus() {
        Map pressStatusMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotopressuse.txt");
            List<String> curscreens = iSecsHost.executeCommand("curscreen");
            for (String curscreen : curscreens) {
                if (curscreen.equals("p1")) {
                    iSecsHost.executeCommand("playback p12pressuse.txt");
                }
                if (curscreen.equals("p2")) {
                    iSecsHost.executeCommand("playback p22pressuse.txt");
                }
            }
            List<String> curscreens2 = iSecsHost.executeCommand("curscreen");
            for (String curscreen : curscreens2) {
                if (curscreen.equals("pressuse")) {
                    List<String> pressStatus = iSecsHost.executeCommand("readm p1use p2use p3use");
                    pressStatusMap.put("p1use", pressStatus.get(0));
                    pressStatusMap.put("p2use", pressStatus.get(1));
                    pressStatusMap.put("p2use", pressStatus.get(2));
                }
            }
        }
        return pressStatusMap;
    }

    @Override
    protected boolean specialCheck() {
        logger.info("塑封特殊卡控开始");
        if (!AxisUtility.getPressCheckFlag(deviceCode)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "模具使用校验开关未开启,跳过校验");
            return true;
        }
        String curscreen = iSecsHost.executeCommand("curscreen").get(0);
        if ("main".equals(curscreen)) {
            logger.info("main页面，执行特殊卡控取值");
            Map mainPressMap = iSecsHost.readAllParaByScreen("main");
            logger.info("main页面特殊卡控取值结果:" + mainPressMap);

            if (mainPressMap != null && !mainPressMap.isEmpty()) {
                String equipPress = "";
                if (!String.valueOf(mainPressMap.get("lineauto1")).contains("no") && !String.valueOf(mainPressMap.get("lineauto1")).contains("NO") && !String.valueOf(mainPressMap.get("lineauto1")).trim().equals("")) {
                    equipPress = equipPress + "MC1";
                }
                if (!String.valueOf(mainPressMap.get("lineauto2")).contains("no") && !String.valueOf(mainPressMap.get("lineauto2")).contains("NO") && !String.valueOf(mainPressMap.get("lineauto2")).trim().equals("")) {
                    equipPress = equipPress + "MC2";
                }
                if (!String.valueOf(mainPressMap.get("lineauto3")).contains("no") && !String.valueOf(mainPressMap.get("lineauto3")).contains("NO") && !String.valueOf(mainPressMap.get("lineauto3")).trim().equals("")) {
                    equipPress = equipPress + "MC3";
                }
                logger.info("设备press使用状况:" + equipPress);
                String servString = AxisUtility.getPressUseFromServerByWS(deviceCode);
                logger.info("从server取到的数据:" + servString);
//                servString = servString.replace("{", "").replace("}", "").replace(";", "").replace("MC", "");
                servString = servString.replace("{", "").replace("}", "").replace(";", "");
                logger.info("整理后的server数据:" + servString);
                if (servString.contains(equipPress)) {
                    return true;
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "模具使用校验失败...不允许开机");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "MTMS提示:" + servString);
                    return false;
                }
            }
        }
        logger.info("不在main页面，特殊卡控取值失败");
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "不在主页面，特殊卡控取值失败...不允许开机");
        sendMessage2Eqp("Check press failed.");
        return false;
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map resultMap = new HashMap();
        if (dataIdMap.containsKey("ShotCount1")) {
            resultMap.putAll(getShotCount());
            return resultMap;
        }
        if (dataIdMap.get("PressState") != null) {
            resultMap.putAll(getPressStatus());
            return resultMap;
        }
        if (dataIdMap.get("shot1") != null) {
            logger.info("main页面特殊取值开始");
            String curscreen = iSecsHost.executeCommand("curscreen").get(0);
            if ("main".equals(curscreen)) {
                logger.info("main页面，执行特殊取值");
                Map mainPressMap = iSecsHost.readAllParaByScreen("main");
                logger.info("main页面特殊取值结果:" + mainPressMap);
                resultMap.putAll(mainPressMap);
                return resultMap;
            }
            logger.info("不在main页面，特殊取值取消");
            return new HashMap();
        }
        //点检取值
        String curscreen = iSecsHost.executeCommand("curscreen").get(0);
        iSecsHost.executeCommand("playback gotowdjsq.txt");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        curscreen = iSecsHost.executeCommand("curscreen").get(0);
        if ("main".equals(curscreen)) {
            Map mainPressMap = iSecsHost.readAllParaByScreen("main");
            resultMap.putAll(mainPressMap);
        }
        boolean needGetPara = false;
        for (Map.Entry<String, String> entry : dataIdMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("RecipePara".equals(value)) {
                needGetPara = true;
            }
        }
        if (needGetPara) {
            Map recipeMap = getRecipeValueMap();
            resultMap.putAll(recipeMap);
//        Map valueMap = new HashMap();
//        if (deviceType.contains("Z1")) {
//            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                String curscreen = iSecsHost.executeCommand("curscreen").get(0);
//                if ("autorun".equals(curscreen)) {
//                    iSecsHost.executeCommand("playback gototransfer.txt");
//                    try {
//                        Thread.sleep(500);
//                    } catch (Exception e) {
//                    }
//                    valueMap = iSecsHost.readAllParaByScreen("autorun");
//                }
//            }
//            resultMap.putAll(valueMap);
//            Map recipeMap = getRecipeValueMap();
//            resultMap.putAll(recipeMap);
//        } else if (deviceType.contains("Z3")) {
//            Map recipeMap = getRecipeValueMap();
//            resultMap.putAll(recipeMap);
//        }
            logger.debug("resultMap:" + resultMap);
        }
        return resultMap;
    }

    private Map getRecipeValueMap() {
        List<RecipePara> recipeParaList = (List<RecipePara>) getEquipMonitorPara().get("recipeParaList");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        Map templateMap = new HashMap();
        for (RecipeTemplate rt : recipeTemplates) {
            templateMap.put(rt.getParaName(), rt.getDeviceVariableId());
        }
        Map recipeMap = new HashMap();
        for (RecipePara rp : recipeParaList) {
            recipeMap.put(templateMap.get(rp.getParaName()), rp.getSetValue());
        }
        return recipeMap;
    }

    public boolean startCheck() {
        String screen = iSecsHost.executeCommand("curscreen").get(0);
        if (!screen.equals("main")) {
            iSecsHost.executeCommand("playback gotomain");
        }
        getCurrentRecipeName();
        if (!specialCheck()) {
            return false;
        }
        boolean pass = true;
        String checkRecultDesc = "";
        String checkRecultDescEng = "";
        if (this.checkLockFlagFromServerByWS(deviceCode)) {
            checkRecultDesc = "检测到设备被Server要求锁机,设备将被锁!";
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
            pass = false;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (deviceInfoExt == null) {
            logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！");
            checkRecultDesc = "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！";
            pass = false;
        } else {
            String trackInRcpName = deviceInfoExt.getRecipeName();
            if (!ppExecName.equals(trackInRcpName)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "已选程序与领料程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " 已选程序 " + ppExecName);
                pass = false;
                checkRecultDesc = "已选程序与领料程序不一致,设备被锁定！请联系ME处理！领料程序:" + trackInRcpName + " 已选程序:" + ppExecName;
                checkRecultDescEng = "The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + trackInRcpName + ">,equipment will be locked.";
            }
        }
        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
        if (execRecipe == null) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理！");
            checkRecultDesc = "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理!";
            checkRecultDescEng = " There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.";
            pass = false;
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.StartCheckWI");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("recipeName", ppExecName);
        mqMap.put("EquipStatus", equipStatus);
        mqMap.put("lotId", lotId);
        if (pass) {
            //MonitorService monitorService = new MonitorService(sqlSession);
            // List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
            List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
            List<RecipePara> recipeParasdiff = checkRcpPara(deviceInfoExt.getRecipeId(), deviceCode, equipRecipeParas, "");
            try {
                String eventDesc = "";
                String eventDescEng = "";
                if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                    this.stopEquip();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机参数检查未通过!");
                    for (RecipePara recipePara : recipeParasdiff) {
                        eventDesc = "开机Check参数异常参数编码为:" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为:" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                        String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "/r/n";
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                        checkRecultDesc = checkRecultDesc + eventDesc;
                        eventDescEng = eventDescEng + eventDescEngtmp;
                    }
                    //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
                    sendMessage2Eqp("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:/r/n" + eventDescEng);
                    pass = false;
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机参数检查通过！");
                    eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                    logger.info("设备：" + deviceCode + " 开机Check成功");
                    pass = true;
                    checkRecultDesc = eventDesc;
                }
                sqlSession.commit();
            } catch (Exception e) {
                logger.error("Exception:", e);
            } finally {
                sqlSession.close();
            }
        } else {
            if (!"".equalsIgnoreCase(checkRecultDescEng)) {
                sendMessage2Eqp(checkRecultDescEng);
            }
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查条件不具备,检查未通过!");
        }
        mqMap.put("eventDesc", checkRecultDesc);
        GlobalConstants.C2SLogQueue.sendMessage(mqMap);
        return pass;
    }

}
