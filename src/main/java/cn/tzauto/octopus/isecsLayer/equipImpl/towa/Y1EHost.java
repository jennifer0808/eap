/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.towa;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.resolver.towa.TowaISECSRecipeEdit;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.towa.TowaRecipeUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class Y1EHost extends EquipModel {

    private static Logger logger = Logger.getLogger(Y1EHost.class.getName());
    private ISecsHost towaCtrlHost;

    public Y1EHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);

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
                        ppExecName = iSecsHost.executeCommand("read recipe").get(0);
                    } else {
                        ppExecName = iSecsHost.executeCommand("read recipe").get(0);
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
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
                UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
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
                UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
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
                        UiLogUtil.appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
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
        List<String> curresult = iSecsHost.executeCommand("curscreen");
        if (!curresult.get(0).equalsIgnoreCase("moldSetting")) {
            UiLogUtil.appendLog2EventTab(deviceCode, "请将设备调整到塑封设置页面再进行操作.");
            return "请将设备调整到主页面再进行操作.";
        }
        cleanRecipe("");
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
//                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
//                    return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
//                }
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
//                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", ftpPathTmp + recipe.getRecipeName() + ".rsp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/src.rsp");
                    Thread.sleep(1000);
                    List<String> edits = TowaISECSRecipeEdit.getUniqueRecipeParaMap(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/src.rsp");
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
//                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rsp");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + "\\ " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".rsp\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        logger.info("recipe:" + recipe.getRecipeName() + " 下载文件成功,开始执行导入");
                        //执行导入
                        List<String> result1 = iSecsHost.executeCommand("playback downloadrecipe.txt");
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
//                this.deleteTempFile(recipe.getRecipeName());
            }
        }
        return "Download recipe " + recipe.getRecipeName() + " failed";
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
        List<String> curresult = iSecsHost.executeCommand("curscreen");
        if (!curresult.get(0).equalsIgnoreCase("moldSetting")) {
            UiLogUtil.appendLog2EventTab(deviceCode, "请将设备调整到塑封设置页面再进行操作.");
            return "请将设备调整到主页面再进行操作.";
        }
        deleteAllRcpFromDevice(recipeName);
        return "0";
    }

    @Override
    public void deleteAllRcpFromDevice(String currentRecipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {

                for (int i = 1; i < 4; i++) {
                    List<String> result = iSecsHost.executeCommand("playback gotodelrecipe.txt");
                    Thread.sleep(6000);
                    List<String> result1 = iSecsHost.executeCommand("playback delrecipe" + i + ".txt");
                    Thread.sleep(5000);
                }
                iSecsHost.executeCommand("playback cancledelrecipe.txt");

            } catch (Exception e) {
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
        }
        List<String> result1 = iSecsHost.executeCommand("curscreen");
        if (!result1.get(0).equalsIgnoreCase("moldSetting")) {
            UiLogUtil.appendLog2EventTab(deviceCode, "请将设备调整到塑封设置页面再进行操作.");
            return "请将设备调整到主页面再进行操作.";
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                for (int i = 1; i < 3; i++) {
                    List<String> gotodelrecipe = iSecsHost.executeCommand("playback gotoselrecipe.txt");
                    for (String str : gotodelrecipe) {
                        if ("done".equals(str)) {
                            try {
                                Thread.sleep(7000);
                            } catch (InterruptedException ex) {
                                logger.error(ex.getMessage());
                            }
                            iSecsHost.executeCommand("curscreen");
                            List<String> delrecipeList = iSecsHost.executeCommand("read s" + i + "_sel");
                            for (String string1 : delrecipeList) {
                                if ("".equals(string1.trim()) || "done".equals(string1.trim())) {
                                    continue;
                                }
                                //这里是用0（零）替换了o
                                if (recipeName.replaceAll("O", "0").equals(string1.trim())) {
                                    List<String> result11 = iSecsHost.executeCommand("playback selrecipe" + i + ".txt");
                                    for (String str1 : result11) {
                                        if ("done".equals(str)) {
                                            ppExecName = recipeName;
                                            Thread.sleep(20000);
                                            for (int k = 1; k < 4; k++) {
                                                iSecsHost.executeCommand("playback gotodelrecipe.txt");
                                                Thread.sleep(6000);
                                                iSecsHost.executeCommand("playback delrecipe" + k + ".txt");
                                                Thread.sleep(5000);
                                            }
                                            iSecsHost.executeCommand("playback cancledelrecipe.txt");

                                            return "0";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return "选中失败";
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
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
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> curscreens = iSecsHost.executeCommand("curscreen");
                if (!curscreens.contains("autorun")) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "设备未在AutoRun界面取消本次开机检查...");
                    return new HashMap();
                }
                iSecsHost.executeCommand("playback gotomenu.txt");
                Thread.sleep(4000);
                List<String> gotoexport = iSecsHost.executeCommand("playback gotoexportrecipe.txt");
                for (String str : gotoexport) {
                    if ("done".equals(str)) {
                        for (int i = 1; i < 10; i++) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                logger.error(ex.getMessage());
                            }
                            List<String> curscreenstmp = iSecsHost.executeCommand("curscreen");
                            for (String curscreen : curscreenstmp) {
                                if ("export".equals(curscreen)) {
                                    List<String> export = iSecsHost.executeCommand("playback exportrecipe.txt");
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException ex) {
                                        logger.error(ex.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
                if (findRecipe(ppExecName)) {
                    String localftpip = GlobalConstants.clientInfo.getClientIp();
                    String ftpUser = GlobalConstants.ftpUser;
                    String ftpPwd = GlobalConstants.ftpPwd;
                    String equipRecipePathtmp = equipRecipePath;
                    TransferUtil.setPPBody(ppExecName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");

                    List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                            + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
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
        return map;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> result1 = iSecsHost.executeCommand("curscreen");
        if (!result1.get(0).equalsIgnoreCase("moldSetting")) {
            UiLogUtil.appendLog2EventTab(deviceCode, "请将设备调整到塑封设置页面再进行操作.");
        }

        cleanRecipe("");
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {

            List<String> gotoexport = iSecsHost.executeCommand("playback gotoexportrecipe.txt");
            for (String str : gotoexport) {
                if ("done".equals(str)) {
                    for (int i = 1; i < 10; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            logger.error(ex.getMessage());
                        }
                        List<String> curscreens = iSecsHost.executeCommand("curscreen");
                        for (String curscreen : curscreens) {
                            if ("export".equals(curscreen)) {
                                List<String> export = iSecsHost.executeCommand("playback exportrecipe.txt");
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException ex) {
                                    logger.error(ex.getMessage());
                                }
                                try {
                                    List<String> resulttmpList = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + "\\ /a/w\"");

                                    if (resulttmpList != null && resulttmpList.size() > 1) {
                                        for (String str2 : resulttmpList) {
                                            if (str2.contains(".rsp")) {
                                                String[] recipeNameTmps = str2.split("\\s");
                                                for (String recipeNameTmp1 : recipeNameTmps) {
                                                    if (recipeNameTmp1.contains(".rsp")) {
                                                        String recipeNameTmp = recipeNameTmp1.replaceAll(".rsp", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
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
                    }
                }
            }

            return eppd;
        }
    }

    @Override
    public Object clone() {
        Y1EHost newEquip = new Y1EHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
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
        UiLogUtil.appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
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
                            String recipeNameTmp = recipeNameTmps[i].replaceAll(".rsp", "").replaceAll("\\[", "").replace("]", "").replace(".", "").trim();
                            if (keepRecipeName.equals(recipeNameTmp)) {
                                continue;
                            }
                            List<String> result0 = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeNameTmp + ".rsp\"");
                        }
                        String[] recipeNameTmps2 = str.split("\\s");
                        for (int i = 0; i < recipeNameTmps2.length; i++) {
                            String recipeNameTmp = recipeNameTmps2[i].replaceAll(".rsp", "").replaceAll("\\[", "").replace("]", "").replace(".", "").trim();
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

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map valueMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String curscreen = iSecsHost.executeCommand("curscreen").get(0);
            if ("autorun".equals(curscreen)) {
                iSecsHost.executeCommand("playback gototransfer.txt");
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                valueMap = iSecsHost.readAllParaByScreen("autorun");
            }
        }
        return valueMap;
    }
}
