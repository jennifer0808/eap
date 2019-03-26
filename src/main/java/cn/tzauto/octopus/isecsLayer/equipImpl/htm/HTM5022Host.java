/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.htm;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.resolver.htm.HtmRecipeUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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
public class HTM5022Host extends EquipModel {

    private static Logger logger = Logger.getLogger(HTM5022Host.class.getName());

    public HTM5022Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);

    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    String ppExecNameTmp = iSecsHost.executeCommand("read recipename").get(0);
                    if (ppExecNameTmp.contains("-A")) {
                        ppExecName = ppExecNameTmp.replaceAll("-A", "");
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

                    result = iSecsHost.executeCommand("playback stop.txt");
                    for (String start : result) {
                        if ("done".equals(start)) {
                            equipStatus = "Idle";
                            return "0";
                        }
                    }
                    stopResult = "锁机失败,当前状态无法执行锁机";
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
                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");

                boolean ocrUploadOk = true;
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".rcp\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            Map paraMap = HtmRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".rcp");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = HtmRecipeUtil.transferFromDB(paraMap, deviceType);
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
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp", ftpPathTmp + recipe.getRecipeName() + ".rcp_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp", ftpPathTmp + recipe.getRecipeName() + ".rcp", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.rcp");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.rcp", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".rcp");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".rcp\"");
                for (String str : result) {
                    if ("done".equals(str)) {
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
                iSecsHost.executeCommand("playback selrecipe1.txt");
                try {
                    Thread.sleep(1000);
                    iSecsHost.executeCommand("dialog \"Open File\" write " + recipeName + ".rcp");
                    Thread.sleep(1000);
                    iSecsHost.executeCommand("dialog \"Open File\" 1");
                    Thread.sleep(1000);
                } catch (Exception e) {
                }

                List<String> result = iSecsHost.executeCommand("playback selrecipe2.txt");
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {

                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;

                String equipRecipePathtmp = equipRecipePath;
                TransferUtil.setPPBody(ppExecName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + ppExecName + ".rcp\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = HtmRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName + ".rcp");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                List<RecipePara> recipeParaList = HtmRecipeUtil.transferFromDB(paraMap, deviceType);
                                map.put("recipeParaList", recipeParaList);
                                for (RecipePara recipePara : recipeParaList) {
                                    map.put(recipePara.getParaCode(), recipePara);
                                }
                            } else {
                                logger.error("解析recipe时出错,recipe文件不存在");
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
                        str = str.trim();
                        String[] recipeNameTmp = str.split(".rcp");
                        for (int i = 0; i < recipeNameTmp.length; i++) {
                            recipeNameList.add(recipeNameTmp[i].replaceAll(".rcp", "").replaceAll("\\[", "").replace("..]", "").replace(".]", "").trim());
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
        HTM5022Host newEquip = new HTM5022Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
////                List<String> result = iSecsHost.executeCommand("readrectcolor 217 510 220 513 ");
////                for (String colorstr : result) {
////                    if ("0xc0c0c0".equals(colorstr)) {
////                        alarmStrings.add("");
////                    }
////                    if ("0xffff00".equals(colorstr) || "0xff0000".equals(colorstr)) {
//
////                    
////                        logger.info("The equip state changged to alarm...");
////                        DateFormat dateFormat = new SimpleDateFormat("yyMMdd");
////                        String date = dateFormat.format(new Date());
////
////                        iSecsHost.executeCommand("dos \"del /q D:\\OCRHTM5022XPZ1\\ALARM\\.*\"");
////                        iSecsHost.executeCommand("dos \"XCOPY /Y C:\\Dynamics\\ALM\\" + date + ".ALM D:\\OCRHTM5022XPZ1\\ALARM\\" + date + ".ALM\"");
////                        TransferUtil.setPPBody("Alarm", 0, "D:\\OCRHTM5022XPZ1\\ALARM\\" + GlobalConstants.ftpPath + deviceCode + "ALARMtemp/TMP");
////                        String localftpip = GlobalConstants.clientInfo.getClientIp();
////                        String ftpUser = GlobalConstants.ftpUser;
////                        String ftpPwd = GlobalConstants.ftpPwd;
////
//////                        List<String> loadFileResult = iSecsHost.executeCommand("ftp " + localftpip + " "
//////                                + ftpUser + " " + ftpPwd + " C:\\Dynamics\\ALM\\ " + GlobalConstants.ftpPath + deviceCode + "ALARMtemp/" + " \"mput "
//////                                + date + ".ALM\"");
////                        try {
////                            FileUtil.copyFile("D:\\OCRHTM5022XPZ1\\ALARM\\"  + deviceCode + "ALARMtemp\\" + date + ".ALM", " D:\\OCRHTM5022XPZ1\\ALARM\\" + deviceCode + "ALARMtemp\\" + date + ".ALMtmp");
////                        } catch (Exception e) {
////                        }
////
////                        List<String> loadFileResult = iSecsHost.executeCommand("ftp " + localftpip + " "
////                                + ftpUser + " " + ftpPwd + " D:\\OCRHTM5022XPZ1\\ALARM\\ " +" D:\\OCRHTM5022XPZ1\\ALARM\\" + deviceCode + "ALARMtemp\\" + " \"mput "
////                                + date + ".ALM\"");
////                        for (String string : loadFileResult) {
////                            if (string.contains("success")) {
////                                alarmStrings = transferAlarmFile(" D:\\OCRHTM5022XPZ1\\ALARM\\" + deviceCode + "ALARMtemp\\" + date + ".ALM");
////                            }
////                        }
//                        List<String> alarmStringstmp = iSecsHost.executeCommand("read alarm");
//                        if (alarmStringstmp.size() > 1) {
//                            String alarmString = alarmStringstmp.get(0).replaceAll("done", "");
//                            if (!alarmString.equals("")) {
//                                alarmStrings.add(alarmString);
//                            }
//                        } else {
//                            alarmStrings.add("");
//                        }
////                    }
////                }
//            } catch (Exception e) {
//                logger.error("Get EquipAlarm error:" + e.getMessage());
//            }
//        }
//   
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
                    if ("pause".equals(result.get(0))) {
                        equipStatus = "Pause";
                    } else if ("main".equals(result.get(0))) {
                        List<String> result2 = iSecsHost.executeCommand("readm rectifiers1");
                        if ("Stby".equals(result2.get(0))) {
                            equipStatus = "Idle";
                        } else {
                            equipStatus = "Run";
                        }
                    } else if ("any".equals(result.get(0))) {
                        List<String> result2 = iSecsHost.executeCommand("readm rectifiers1");
                        if ("Stby".equals(result2.get(0))) {
                            equipStatus = "Idle";
                        } else {
                            equipStatus = "Run";
                        }
                    } else if ("run".equalsIgnoreCase(result.get(0))) {
                        equipStatus = "Run";
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
        UiLogUtil.appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List<String> transferAlarmFile(String filePath) {
        List<String> lastAlarmList = new ArrayList<>();
        lastAlarmList = getLastAlarmFile(filePath + "tmp");
        List<String> alarmList = new ArrayList<>();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("OPEN") || cfgline.contains("CLOSE")) {
                    for (String lastAlarm : lastAlarmList) {
                        if (cfgline.equals(lastAlarm)) {
                            break;
                        }
                    }
                    String alarmString = cfgline.replaceAll("\\[CEM", "").replaceAll("]", "");
                    alarmList.add(alarmString);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return alarmList;
    }

    @Override
    protected String checkEquipStatus() {
        return "0";
    }

    private List<String> getLastAlarmFile(String filePath) {

        List<String> alarmList = new ArrayList<>();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("OPEN") || cfgline.contains("CLOSE")) {
                    alarmList.add(cfgline);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return alarmList;
    }

    @Override
    protected String checkPPExecName(String recipeName) {
        if (recipeName.equals(ppExecName)) {
            return "预下载程序与在用程序相同，下载取消0";
        }
        return "0";
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {

        return new HashMap();
    }
}
