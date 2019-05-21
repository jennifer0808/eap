/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.hitachi;


import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.hitachi.LaserDrillUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.socket.RecipeMapping;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author luosy
 */
public class HitachiLaserDrillHost extends EquipModel {

    private static Logger logger = Logger.getLogger(HitachiLaserDrillHost.class.getName());
    private String toolName = "";
    private boolean hasAutoChangeRecipe = false;

    public HitachiLaserDrillHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        org.slf4j.MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        String prerecipeName = ppExecName;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("process".equals(result.get(0))) {
                        String recipeNameTemp = iSecsHost.executeCommand("read recipename").get(0);
                        ppExecName = recipeNameTemp.substring(recipeNameTemp.lastIndexOf("\\") + 1);
//                        String lotIdtemp = iSecsHost.executeCommand("read lotno").get(0);
                        if ("done".equals(ppExecName)) {
                            ppExecName = prerecipeName;
                        }
//                        if (!"done".equals(lotIdtemp)) {
//                            lotId = lotIdtemp;
//                        }
                    } else {
                        ppExecName = prerecipeName;
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
                ppExecName = "--";
                return prerecipeName;
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("WorkLot", lotId);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map valueMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String curscreen = iSecsHost.executeCommand("curscreen").get(0);
            if ("main".equals(curscreen)) {
                iSecsHost.executeCommand("playback gotopara.txt");
                iSecsHost.executeCommand("curscreen");
                List<String> areaValues = iSecsHost.executeCommand("readbyscreen ");

                iSecsHost.executeCommand("playback gotopara.txt");
                logger.debug("valueMap:" + valueMap);
            }
        }
        return valueMap;
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
                    if (equipStatusTemp.equals("Idle")) {
                        return "0";
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
                    String equipStatusTemp = this.getEquipStatus();
                    if (equipStatusTemp.equals("Idle")) {
                        return "0";
                    }
                    result = iSecsHost.executeCommand("playback stop.txt");
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
    public String startEquip() {
        String stopResult = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = null;
            try {
                String equipStatusTemp = this.getEquipStatus();
                if (equipStatusTemp.contains("un")) {
                    return "0";
                }
                result = iSecsHost.executeCommand("playback start.txt");
                for (String start : result) {
                    if ("done".equals(start)) {
                        return "0";
                    }
                }
            } catch (Exception e) {
            }
        }
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
//                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String localftpip = GlobalConstants.ftpIP;
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

                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");
                if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP", GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", "TMP", ftpip, ftpPort, ftpUser, ftpPwd)) {

                }
//                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
//                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
//                        + recipeName + "\"");
                List<String> result = sendCmdMsg2Equip("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + "\"");
                if (result == null) {
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 超时,请检查FTP服务及网络是否正常.");
                } else {
                    for (String uploadstr : result) {
                        if ("done".equals(uploadstr)) {
                            List<RecipePara> recipeParaList = new ArrayList<>();
                            try {
                                if (FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName(), GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName(), ftpip, ftpPort, ftpUser, ftpPwd)) {

                                }
                                Map paraMap = LaserDrillUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName);
                                recipeParaList = LaserDrillUtil.transferFromDB(paraMap, deviceType);
                                toolName = String.valueOf(paraMap.get("HEADER"));
                                iSecsHost.executeCommand("ftp " + localftpip + " "
                                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                                        + toolName + "\"");
                                if (FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + toolName, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + toolName, ftpip, ftpPort, ftpUser, ftpPwd)) {

                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            resultMap.put("recipe", recipe);
                            resultMap.put("deviceCode", deviceCode);
                            resultMap.put("recipeFTPPath", ftpRecipePath);
                            resultMap.put("recipeParaList", recipeParaList);
                            resultMap.put("uploadResult", "0");
                        }
                        if (uploadstr.contains("Not connected")) {
                            ocrUploadOk = false;
                        }
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
        for (int i = 0; i < 2; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            if (i == 0) {
                attach.setAttachName(recipeName + "_V" + recipe.getVersionNo());
            } else if (i == 1) {
                attach.setAttachName(toolName + "_V" + recipe.getVersionNo());
            }

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
        }

        return attachs;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            try {
//                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String localftpip = GlobalConstants.ftpIP;
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
                    if (FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName(), ftpPathTmp + recipe.getRecipeName() + "_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                        Map paraMap = LaserDrillUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName());

                        String tool12NameTemp = String.valueOf(paraMap.get("HEADER"));
                        FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + tool12NameTemp, ftpPathTmp + tool12NameTemp + "_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    }

                } else {

                    if (FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName(), ftpPathTmp + recipe.getRecipeName(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                        Map paraMap = LaserDrillUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName());

                        String tool12NameTemp = String.valueOf(paraMap.get("HEADER"));
                        FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + tool12NameTemp, ftpPathTmp + tool12NameTemp, ftpip, ftpPort, ftpUser, ftpPwd);

                    }

                }

                List<String> result = sendCmdMsg2Equip("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + "\"");

                if (result == null) {
                    return "下载Recipe:" + recipe.getRecipeName() + "超时,请检查FTP服务及网络是否正常.";
                }
                for (String str : result) {
                    if ("done".equals(str)) {
                        hasAutoChangeRecipe = false;
                        return "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
                return "下载失败,出现异常:" + e.getMessage();
            } finally {
                sqlSession.close();
                this.deleteTempFile(recipe.getRecipeName());
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\*.LMU*\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        return "0";
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
                List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                Thread.sleep(900);
                iSecsHost.executeCommand("write lot " + this.lotId);
                iSecsHost.executeCommand("replay enter.exe");
                Thread.sleep(1800);
                iSecsHost.executeCommand("playback clearpartno.txt");
                iSecsHost.executeCommand("dialog \"OPEN FILE\" write " + equipRecipePath + "\\" + recipeName);
                result = iSecsHost.executeCommand("dialog \"OPEN FILE\" action \"&Open\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        ppExecName = recipeName;
                        return "0";
                    }
                    if (str.contains("rror")) {
                        return "选中失败";
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
        Map map = new HashMap();
        if ("1".equals(GlobalConstants.getProperty("HITACHI_LASER_DRILL_START_CHECK_RECIPE_PARA"))) {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                try {

//                String localftpip = GlobalConstants.clientInfo.getClientIp();
                    String localftpip = GlobalConstants.ftpIP;
                    String ftpUser = GlobalConstants.ftpUser;
                    String ftpPwd = GlobalConstants.ftpPwd;

                    String equipRecipePathtmp = equipRecipePath;
                    TransferUtil.setPPBody(equipRecipePathtmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");

                    List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                            + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                            + ppExecName + "\"");
                    for (String uploadstr : result) {
                        if ("done".equals(uploadstr)) {
                            try {
                                Map paraMap = LaserDrillUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName);
                                List<RecipePara> recipeParaList = LaserDrillUtil.transferFromDB(paraMap, deviceType);
                                map.put("recipeParaList", recipeParaList);
                                for (RecipePara recipePara : recipeParaList) {
                                    map.put(recipePara.getParaCode(), recipePara);
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Get equip status error:" + e.getMessage());
                }
            }
            logger.info("monitormap:" + map.toString());
            deleteTempFile(ppExecName);
        }
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
                    if (!str.contains(".LMU")) {
                        continue;
                    }
                    if (str.contains("LMU")) {
                        String[] recipeNameTemps = str.split("\\s");
                        for (String temp : recipeNameTemps) {
                            if (temp.contains(".LMU")) {
                                recipeNameList.add(temp.trim());
                            }
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
        HitachiLaserDrillHost newEquip = new HitachiLaserDrillHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
                List<String> alarmid = iSecsHost.executeCommand("read alarm");
//                if (alarmid.size() < 2) {
//                    alarmStrings.add("");
//                    return alarmStrings;
//                }
                for (String string : alarmid) {
                    string = string.replaceAll("done", "").replaceAll("@", "");
                    if (string.equals("")) {
                        continue;
                    }
                    String alarmKey = "-----";
                    if (string.length() > 3) {
                        alarmKey = string.substring(0, 4);
                    }
                    if (GlobalConstants.getProperty("HITACHI_LASER_DRILL_AUTO_CHANGE_RECIPE_ALARM").contains(string)
                            || alarmKey.equals("0234") || alarmKey.equals("0257")) {
                        String panelcount = iSecsHost.executeCommand("read panelcount").get(0);
                        try {
                            Double.parseDouble(panelcount);
                        } catch (NumberFormatException e) {
                            logger.error("read panelcount error,panelcount=" + panelcount);
                        }
                        if (Double.parseDouble(panelcount) == 0) {
                            getCurrentRecipeName();
                            logger.info("getCurrentRecipeName:" + ppExecName);
                            if (ppExecName.equals("--")) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "无法自动切换反面程式,请先保证EAP能获取到准确的程式名");
                                sendMessage2Eqp("Can not get the Current RecipeName,please check it");
                            } else {
                                if (!hasAutoChangeRecipe) {
                                    iSecsHost.executeCommand("playback resetalarm.txt");
                                    changeRecipe();
                                }
                            }
                        }
                    }
//                    if (string.length() > 200) {
//                        string = string.substring(0, 200);
//                    }
//                    alarmStrings.add(string);
                    logger.info("Get alarm ALID=[" + string + "]");
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
//        return alarmStrings;
        return null;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {

                List<String> hrunColors = this.iSecsHost.executeCommand("readrectcolor 1160 846 1170 848");
                for (String startColorTemp : hrunColors) {
                    if (startColorTemp.equals("0x99ff00")) {
                        equipStatus = "HRUN";
                    }
                }
                List<String> pchkColors = this.iSecsHost.executeCommand("readrectcolor 1160 878 1170 880");
                for (String startColorTemp : pchkColors) {
                    if (startColorTemp.equals("0x99ff00")) {
                        equipStatus = "PCHK";
                    }
                }
                List<String> gchkColors = this.iSecsHost.executeCommand("readrectcolor 1160 983 1170 985");
                for (String startColorTemp : gchkColors) {
                    if (startColorTemp.equals("0x99ff00")) {
                        equipStatus = "GCHK";
                    }
                }
                if (!equipStatus.equals("HRUN") && !equipStatus.equals("PCHK") && !equipStatus.equals("GCHK")) {
                    String shutterColor = "";
                    List<String> shutterColors = this.iSecsHost.executeCommand("readrectcolor 1040 783 1070 785");
                    for (String shutterColorTemp : shutterColors) {
                        if (shutterColorTemp.contains("0x")) {
                            shutterColor = shutterColorTemp;
                        }
                    }

                    if (shutterColor.equals("0xcbcbcb")) {
                        equipStatus = "Idle";
                    }

                    String abcColor = "";
                    List<String> abcColors = this.iSecsHost.executeCommand("readrectcolor 1040 556 1070 559");
                    for (String abcColorTemp : abcColors) {
                        if (abcColorTemp.contains("0x")) {
                            abcColor = abcColorTemp;
                        }
                    }
                    String startColor = "";
                    List<String> startColors = this.iSecsHost.executeCommand("readrectcolor 1040 368 1070 374");
                    for (String startColorTemp : startColors) {
                        if (startColorTemp.contains("0x")) {
                            startColor = startColorTemp;
                        }
                    }
                    if (shutterColor.equals("0x99ff00")) {
                        if (startColor.equals("0x99ff00")) {
                            if (abcColor.equals("0x99ff00")) {
                                equipStatus = "AutoRun";
                            }
                            if (abcColor.equals("0xcbcbcb")) {
                                equipStatus = "ManualRun";
                            }
                        } else {
                            equipStatus = "Idle";
                        }
                    }
                }

//                String startColor = "";
//                List<String> startColors = this.iSecsHost.executeCommand("readrectcolor 1040 368 1070 374");
//                for (String startColorTemp : startColors) {
//                    if (startColorTemp.contains("0x")) {
//                        startColor = startColorTemp;
//                    }
//                }
//                String stopColor = "";
//                List<String> stoptColors = this.iSecsHost.executeCommand("readrectcolor 1040 368 1070 374");
//                for (String stopColorTemp : stoptColors) {
//                    if (stopColorTemp.contains("0x")) {
//                        stopColor = stopColorTemp;
//                    }
//                }
//
//                if (startColor.equals(stopColor)) {
//                    equipStatus = "Idle";
//                }
//                if (startColor.equals("0x99ff00")) {
//
//                }
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
//        if (FtpUtil.copyFile(GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", recipeName, remoteRcpPath, recipeName + "_V" + recipe.getVersionNo())) {
//            UiLogUtil.appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + " ftp路径:" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
//            return false;
//        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName, remoteRcpPath, recipeName + "_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + " 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
//        if (FtpUtil.copyFile(GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", toolName, remoteRcpPath, toolName + "_V" + recipe.getVersionNo())) {
//            UiLogUtil.appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + toolName + " ftp路径:" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
//            return false;
//        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + toolName, remoteRcpPath, toolName + "_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + toolName + " 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    protected boolean specialCheck() {
        //这里增加两个可配置的webservice接口
        if ("1".equals(GlobalConstants.getProperty("START_CHECK_OPNUM"))) {

        }
        if ("1".equals(GlobalConstants.getProperty("START_CHECK_DEVICE_WORKSTATUS"))) {

        }
        return true;
    }

    @Override
    protected List<RecipePara> checkRcpPara(String recipeId, String deviceCode, List<RecipePara> equipRecipeParas, String masterCompareType) {
        if ("1".equals(GlobalConstants.getProperty("HITACHI_LASER_DRILL_START_CHECK_RECIPE_PARA"))) {
            List<RecipePara> diffRecipeParas = checkRcpPara(recipeId, deviceCode, equipRecipeParas);
            return diffRecipeParas;
        }
        return null;
    }

    private List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas) {
        SqlSession sqlsession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlsession);

        //确定管控参数
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateMonitor(deviceCode);

        //找出设备当前recipe参数中超出wi范围的参数
        List<RecipePara> wirecipeParaDiff = new ArrayList<>();
        for (RecipePara equipRecipePara : equipRecipeParas) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                if (recipeTemplate.getParaCode().equals(equipRecipePara.getParaCode())) {
                    equipRecipePara.setRecipeRowId(recipeRowid);
                    String currentRecipeValue = equipRecipePara.getSetValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===currentRecipeValue====>" + currentRecipeValue);
                    String setValue = recipeTemplate.getSetValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===setvalue====>" + setValue);
                    String minValue = recipeTemplate.getMinValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===minValue====>" + minValue);
                    String maxValue = recipeTemplate.getMaxValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===maxValue====>" + maxValue);
                    equipRecipePara.setDefValue(setValue);//默认值，recipe参数设定值
                    boolean paraIsNumber = false;
                    try {
                        Double.parseDouble(currentRecipeValue);
                        paraIsNumber = true;
                    } catch (Exception e) {
                    }
                    boolean maxparaIsNumber = false;
                    try {
                        Double.parseDouble(maxValue);
                        paraIsNumber = true;
                    } catch (Exception e) {
                    }
                    boolean minparaIsNumber = false;
                    try {
                        Double.parseDouble(minValue);
                        paraIsNumber = true;
                    } catch (Exception e) {
                    }
                    try {
                        //spec
                        if ("1".equals(recipeTemplate.getSpecType())) {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                continue;
                            }
                            if (maxparaIsNumber && minparaIsNumber) {
                                if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else {
                                equipRecipePara.setMinValue(minValue);
                                equipRecipePara.setMaxValue(maxValue);
                                wirecipeParaDiff.add(equipRecipePara);
                            }

                            //abs
                        } else if ("2".equals(recipeTemplate.getSpecType())) {
                            String setvalueTmp = setValue;
                            if (!currentRecipeValue.equals(setvalueTmp)) {
                                equipRecipePara.setMinValue(setValue);
                                equipRecipePara.setMaxValue(setValue);
                                wirecipeParaDiff.add(equipRecipePara);
                            }
                        } else if ("3".equals(recipeTemplate.getSpecType())) {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                continue;
                            }
                            if (maxparaIsNumber && minparaIsNumber) {
                                if ((Double.parseDouble(equipRecipePara.getSetValue()) <= Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) >= Double.parseDouble(maxValue))) {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else {
                                equipRecipePara.setMinValue(minValue);
                                equipRecipePara.setMaxValue(maxValue);
                                wirecipeParaDiff.add(equipRecipePara);
                            }
                        } else {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                continue;
                            }
                            if (paraIsNumber) {
                                if (maxparaIsNumber && minparaIsNumber) {
                                    if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                        equipRecipePara.setMinValue(minValue);
                                        equipRecipePara.setMaxValue(maxValue);
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                } else {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else {
                                String setvalueTmp = setValue;
                                if (!currentRecipeValue.equals(setvalueTmp)) {
                                    equipRecipePara.setMinValue(setValue);
                                    equipRecipePara.setMaxValue(setValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            }
                        }
                    } catch (Exception e) {
                        sqlsession.close();
                        logger.error("Exception:", e);
                    }
                }
            }
        }
        sqlsession.close();
        return wirecipeParaDiff;

    }

    private String getLineUseStatus() {
        String lineUseStatus = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.iSecsHost.executeCommand("readrectcolor 1067 193 1078 211");
                if (result != null && !result.isEmpty()) {
                    for (String colorstr2 : result) {
                        if (!"0xa0a0a0".equals(colorstr2)) {
                            lineUseStatus = "1";
                        }
                    }
                }
                List<String> result2 = this.iSecsHost.executeCommand("readrectcolor 1108 193 1118 211");
                if (result2 != null && !result2.isEmpty()) {
                    for (String colorstr2 : result2) {
                        if (!"0xa0a0a0".equals(colorstr2)) {
                            lineUseStatus = lineUseStatus + "2";
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get getLineUseStatus status error:" + e.getMessage());
            }
        }
        return lineUseStatus;
    }

    String Date = "";
    String Axis = "";
    String AP_No = "";
    String Power = "";
    String CrystalNo = "";
    String Crystal_Time = "";

    public void setPCHK(String date, String axis, String aP_No, String power, String CrystalNo, String Crystal_Time) {
        if (date != null) {
            Date = date;
            logger.info("power check data:Date:" + date);
        }
        if (axis != null) {
            Axis = axis;
            logger.info("power check data:Axis:" + date);
        }
        if (aP_No != null) {
            AP_No = aP_No;
            logger.info("power check data:AP_No:" + date);
        }
        if (CrystalNo != null) {
            this.CrystalNo = CrystalNo;
            logger.info("power check data:CrystalNo:" + CrystalNo);
        }
        if (Crystal_Time != null) {
            this.Crystal_Time = Crystal_Time;
            logger.info("power check data:Crystal_Time:" + Crystal_Time);
        }
        if (power != null) {
            Power = power;
            logger.info("power check data:Power:" + power);
        }
    }

    private void changeRecipe() throws FileNotFoundException {
        if ("1".equals(GlobalConstants.getProperty("HITACHI_LASER_DRILL_AUTO_CHANGE_RECIPE"))) {
            String recipeName = String.valueOf(RecipeMapping.loadRecipeLotMapping(GlobalConstants.getProperty("HITACHI_LASER_DRILL_RECIPE_MAPPING_PATH")).get(partNo));
            String[] recipeNames = recipeName.split(",");
            for (String recipeNameTemp : recipeNames) {
                if (!recipeNameTemp.equals(ppExecName)) {
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    RecipeService recipeService = new RecipeService(sqlSession);
                    DeviceService deviceService = new DeviceService(sqlSession);
                    List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipeNameTemp, deviceCode, "Engineer");
                    DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                    DeviceInfo deviceInfo = deviceService.getDeviceInfoByDeviceCode(deviceCode).get(0);
                    String downloadResult = recipeService.downLoadRcp2ISECSDeviceByTypeAutomatic(deviceInfo, recipes.get(0), deviceInfoExt.getRecipeDownloadMod());
                    if ("0".equals(downloadResult)) {
                        hasAutoChangeRecipe = true;
                    }
                    logger.info("Auto change recipe：" + ppExecName + " to " + recipeNameTemp);
                    sqlSession.close();
                    break;
                }
            }
        }
    }

    @Override
    public String organizeRecipe(String partNo) {
        Map recipeNameMap = RecipeMapping.loadRecipeLotMapping(GlobalConstants.getProperty("HITACHI_LASER_DRILL_RECIPE_MAPPING_PATH"));
        String recipeName = "";
        if (recipeNameMap != null) {
            recipeName = String.valueOf(recipeNameMap.get(partNo));
            if (!recipeName.equals("null") && !"".equals(recipeName)) {
                if (recipeName.contains(",")) {
                    recipeName = recipeName.split(",")[0];
                }
            } else {
            }
        } else {
            return "Can not find config file at " + GlobalConstants.getProperty("HITACHI_LASER_DRILL_RECIPE_MAPPING_PATH");
        }

        return recipeName;
    }

    public boolean startCheck() {
        //todo  生產前品質確認
        //todo 掃描線長/代理人/工程師工號條碼登陸
        return true;
    }
}
