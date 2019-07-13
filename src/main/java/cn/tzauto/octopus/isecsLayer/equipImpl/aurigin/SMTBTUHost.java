/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.aurigin;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
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
import cn.tzauto.octopus.common.resolver.aurigin.BTURecipeUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
 * @author luosy
 */
public class SMTBTUHost extends EquipModel {

    private static Logger logger = Logger.getLogger(SMTBTUHost.class.getName());

    public SMTBTUHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("any".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipename").get(0);
                    } else if ("recipedetail1".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipename").get(0);
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    String equipStatusTemp = this.getEquipStatus();
                    if (equipStatusTemp.equals("StartUp-Pause")) {
                        return "设备已经处于Pause状态！";
                    }
                    result = iSecsHost.executeCommand("playback pause.txt");
                    for (String start : result) {
                        if ("done".equals(start)) {
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    this.getEquipStatus();
                    iSecsHost.executeCommand("playback gotoworkscreen.txt");
                    result = iSecsHost.executeCommand("curscreen");
                    if (result != null && !result.isEmpty()) {
                        if ("pause".equals(result.get(0))) {
                            return "0";//"main".equals(result.get(0)) ||
                        } else if ("work".equals(result.get(0))) {
                            result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                            for (String colorstr : result) {
                                if ("0x33cc33".equals(colorstr)) {
                                    equipStatus = "Idle";
                                    return "0";
                                }
                                if ("0xff0000".equals(colorstr)) {
                                    equipStatus = "Run";
                                    result = iSecsHost.executeCommand("playback stop.txt");
                                    for (String start : result) {
                                        if ("done".equals(start)) {
                                            result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                                            for (String colorstr2 : result) {
                                                if ("0x33cc33".equals(colorstr2)) {
                                                    equipStatus = "Idle";
                                                    return "0";
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            stopResult = "锁机失败,当前状态无法执行锁机";
                        }
                    }
                } catch (Exception e) {
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
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
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
                boolean ocrUploadOk = true;
//
                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".rcp\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            logger.info("deviceType ============" + deviceType);
                            logger.info("filePath=========" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".rcp");
                            recipeParaList = BTURecipeUtil.transferBTURcp(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".rcp", deviceType);
                            sleep(1000);
//                            for (int i = 0; i < recipeParaList.size(); i++) {
//                                logger.info("+++++++"+recipeParaList.get(i).getParaName().toString() + "======" + recipeParaList.get(i).getSetValue().toString()+"+++++++");
//                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", ftpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);
                        sleep(2000);
                    }
                    if (uploadstr.contains("Not connected")) {
                        ocrUploadOk = false;
                    }
                }
                if (!ocrUploadOk) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
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
        attach.setAttachName(recipeName + ".rcp_V" + recipe.getVersionNo());
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String recipeNameLast = "";
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            try {
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);
                String rcpNameTemp = recipe.getRecipeName();
                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                    return "下载Recipe:" + rcpNameTemp + "时,FTP连接失败,请检查FTP服务是否开启.";
                }
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp", ftpPathTmp + recipe.getRecipeName() + ".rcp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/" + rcpNameTemp + ".rcp", ftpPathTmp + rcpNameTemp + ".rcp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp", ftpPathTmp + recipe.getRecipeName() + ".rcp", ftpip, ftpPort, ftpUser, ftpPwd);
//                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/" + rcpNameTemp + ".rcp", ftpPathTmp + rcpNameTemp + ".rcp", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.dat");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.rcp", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp");
//                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(rcpNameTemp, null, "GOLD");
//                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
//                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/" + rcpNameTemp + ".rcp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/002.dat");
//                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/002.rcp", deviceType);
//                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/" + rcpNameTemp + ".rcp");
                    }
                }
                if (rcpNameTemp.contains("(") || rcpNameTemp.contains(")")) {
                    rcpNameTemp = rcpNameTemp.replace("(", "-").replace(")", "-");
                    Thread.sleep(500);
                    File file = new File(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp");
                    file.renameTo(new File(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + rcpNameTemp + ".rcp"));
                    recipeNameLast = rcpNameTemp;
                    logger.info("当前下载的程序的名称为" + rcpNameTemp);
                } else {
                    recipeNameLast = recipe.getRecipeName();
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipeNameLast + ".rcp\" ");
//                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
//                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + rcpNameTemp + "temp/" + " \"mget $" + rcpNameTemp + ".rcp$\" ");

                for (String str : result) {
                    if ("done".equals(str)) {
                        return "0";
                    }
                    if (str.contains("Not connected")) {
//                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
//                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
                return "下载失败,出现异常:" + e.getMessage();
            } finally {
                sqlSession.close();
                iSecsHost.executeCommand("dos \"ren " + equipRecipePath + "\\" + recipeNameLast + ".rcp" + " " + recipe.getRecipeName() + ".rcp\"");
//                "dos \"dir " + equipRecipePath + " /a/w\"";
//                this.deleteTempFile(recipe.getRecipeName());
            }
        }
//        return "Download recipe " + recipe.getRecipeName() + " failed";
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeName + ".rcp\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        return "删除成功";
                    }
                }
                return "删除失败";
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage());
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultUser = iSecsHost.executeCommand("read user");
                for (String strTemp : resultUser) {
                    if ("op".equalsIgnoreCase(strTemp) || "Default".equalsIgnoreCase(strTemp)) {
                        List<String> result = iSecsHost.executeCommand("playback login1.txt");
                        sleep(500);
                        List<String> result2 = iSecsHost.executeCommand("replay login.exe");
                        sleep(500);
                        List<String> result5 = iSecsHost.executeCommand("playback selrecipe.txt");
                        for (String str : result5) {
                            if ("done".equals(str)) {
                                ppExecName = recipeName;
                                return "0";
                            }
                            if (str.contains("rror")) {
                                return "选中失败";
                            }
                        }
                    }
                    if ("4271".equalsIgnoreCase(strTemp) || "me".equalsIgnoreCase(strTemp)) {
                        List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                        for (String str : result) {
                            if ("done".equals(str)) {
                                ppExecName = recipeName;
                                return "0";
                            }
                            if (str.contains("rror")) {
                                return "选中失败";
                            }
                        }
                    }
                }

//                sleep(500);
//                List<String> result2 = iSecsHost.executeCommand("dialog \"打开菜单\" write " + recipeName);
//                sleep(500);
//                List<String> result3 = iSecsHost.executeCommand("dialog \"打开菜单\" 1 ");
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
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {

                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;

                String equipRecipePathtmp = equipRecipePath;
                TransferUtil.setPPBody(equipRecipePathtmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "RealTimetemp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "RealTimetemp/" + " \"mput "
                        + ppExecName + ".rcp\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
//                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName + ".rcp");
//                            if (paraMap != null && !paraMap.isEmpty()) {
//                                //jiexi
                            recipeParaList = BTURecipeUtil.transferBTURcp(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "RealTimetemp/" + ppExecName + ".rcp", deviceType);
//                            List<RecipePara> recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
                            map.put("recipeParaList", recipeParaList);
                            for (RecipePara recipePara : recipeParaList) {
                                map.put(recipePara.getParaCode(), recipePara);
                            }
//                            } else {
//                                logger.error("解析recipe时出错,recipe文件不存在");
//                            }
                        } catch (Exception ex) {
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
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = new ArrayList<>();
            try {
                result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + " /a/w\"");
            } catch (Exception e) {
                return eppd;
            }
            if (result != null && result.size() > 1) {
                for (String str : result) {
                    if (str.contains(".rcp")) {
                        String[] recipeNameTmps = str.split(".rcp");
                        for (int i = 0; i < recipeNameTmps.length; i++) {
                            String recipeNameTmp = recipeNameTmps[i].replace(" ", "").replaceAll(".rcp", "").replaceAll("\\[", "").replace("..]", "").replace(".]", "").trim();
                            recipeNameList.add(recipeNameTmp);
                        }
                    }
                }
            }
        }
        eppd.put("eppd", recipeNameList);
        return eppd;
    }

    @Override
    public Object clone() {
        SMTBTUHost newEquip = new SMTBTUHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
//        List<String> alarmStrings = new ArrayList<>();
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                List<String> result = iSecsHost.executeCommand("curscreen");
//                if ("alarm".equals(result.get(0))) {
//                    alarmStrings.add("BTUALARM");
//                } else {
//                    alarmStrings.add("");
//                }
//            } catch (Exception e) {
//                logger.error("Get EquipAlarm error:" + e.getMessage());
//            }
//        }
//        return alarmStrings;
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("any".equals(result.get(0))) {
                        List<String> result2 = iSecsHost.executeCommand("readrectcolor 100 54 103 60");
                        if ("0xffff00".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Idle";
                        } else if ("0xff00".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Run";
                        } else if ("0xff0000".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Alarm";
//                            this.startAlarmListen();
                        }
                    }
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".rcp", remoteRcpPath, recipeName + ".rcp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void startAlarmListen() {
        this.start();
    }

    @Override
    public void deleteAllRcpFromDevice(String currentRecipeName) {
        List<String> recipeNameList = (List<String>) getEquipRecipeList().get("eppd");
        for (String recipeName : recipeNameList) {
            if (recipeName.equals(currentRecipeName)) {
                continue;
            }
            if (recipeName.contains("COOLDOWN")) {
                continue;
            }
            String resultString = deleteRecipe(recipeName);
            if ("0".equals(resultString)) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + recipeName + "]删除成功.");
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + recipeName + "]" + resultString);
            }
        }
    }

    @Override
    protected String checkEquipStatus() {
        return "0";
    }

    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map valueMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String curscreen = iSecsHost.executeCommand("curscreen").get(0);
            if ("any".equals(curscreen)) {
                valueMap = iSecsHost.readAllParaByScreen("any");
            }
        }
        return valueMap;
    }
}
