/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.underfill;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
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
 * @author
 */
public class InnovationHost extends EquipModel {

    private static Logger logger = Logger.getLogger(InnovationHost.class);
    private boolean autoStart = false;

    public InnovationHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipename").get(0);
                    } else if ("recipedetail1".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipedetail1recipename").get(0);
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
                    if (!equipStatus.equalsIgnoreCase("RUN")) {
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
                    if (!equipStatus.equalsIgnoreCase("RUN")) {
                        return "0";
                    }
                    result = iSecsHost.executeCommand("playback stop.txt");
                    if (result.contains("done")) {
                        return "0";
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
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                sqlSession.close();
                String equipRecipePathtmp = equipRecipePath;
                boolean ocrUploadOk = true;

                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".Prg\"");
                Map map = new HashMap();
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        List<String> result1 = iSecsHost.executeCommand("curscreen");
                        if (result1.contains("main")) {
                            List<String> resultPara0 = iSecsHost.executeCommand("readm weight fr1");
                            map.put("weight", resultPara0.get(0));
                            map.put("fr1", resultPara0.get(1));
                            List<String> result2 = iSecsHost.executeCommand("playback gotorecipepara1.txt");
                            List<String> result222 = iSecsHost.executeCommand("curscreen");
                            if (result222.contains("recipepara1")) {
                                for (String str : result2) {
                                    if ("done".equals(str)) {
                                        List<String> resultPara1 = iSecsHost.executeCommand("read needle2 ");
                                        map.put("needle2", resultPara1.get(0));
                                        List<String> resultPara2 = iSecsHost.executeCommand("read needle3 ");
                                        map.put("needle3", resultPara2.get(0));
                                    }
                                }
                            }
                            List<String> result3 = iSecsHost.executeCommand("playback gotorecipepara2.txt");
                            List<String> result333 = iSecsHost.executeCommand("curscreen");
                            if (result333.contains("recipepara2")) {
                                for (String str : result3) {
                                    if ("done".equals(str)) {
                                        List<String> resultPara1 = iSecsHost.executeCommand("read presetting ");
                                        map.put("presetting", resultPara1.get(0));
                                        List<String> resultPara2 = iSecsHost.executeCommand("read dispheater ");
                                        map.put("dispheater", resultPara2.get(0));
                                        List<String> resultPara3 = iSecsHost.executeCommand("read postheater ");
                                        map.put("postheater", resultPara2.get(0));
                                        List<String> resultPara4 = iSecsHost.executeCommand("read nozzle ");
                                        map.put("nozzle", resultPara2.get(0));
                                    }
                                }
                            }
                            List<String> result4 = iSecsHost.executeCommand("playback gotorecipepara3.txt");
                            List<String> result444 = iSecsHost.executeCommand("curscreen");
                            if (result444.contains("recipepara3")) {
                                for (String str : result4) {
                                    if ("done".equals(str)) {
                                        List<String> resultPara1 = iSecsHost.executeCommand("read stroke ");
                                        map.put("stroke", resultPara1.get(0));
                                        List<String> resultPara2 = iSecsHost.executeCommand("read delay ");
                                        map.put("delay", resultPara2.get(0));
                                        List<String> resultPara3 = iSecsHost.executeCommand("read numberofpulse ");
                                        map.put("numberofpulse", resultPara3.get(0));
                                        List<String> resultPara4 = iSecsHost.executeCommand("read rising ");
                                        map.put("rising", resultPara4.get(0));
                                        List<String> resultPara5 = iSecsHost.executeCommand("read falling ");
                                        map.put("falling", resultPara5.get(0));
                                        List<String> resultPara6 = iSecsHost.executeCommand("read open ");
                                        map.put("open", resultPara6.get(0));
                                    }
                                }
                            }
                            List<String> result5 = iSecsHost.executeCommand("playback closerecipepara.txt");
                            logger.debug("paraMap======" + map);
                            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                                RecipePara recipePara = new RecipePara();
                                String[] paraNameAtOCRs = new String[1];
                                paraNameAtOCRs[0] = recipeTemplate.getParaDesc();
                                String setValue = "";
                                recipePara.setParaName(recipeTemplate.getParaName());
                                recipePara.setParaCode(recipeTemplate.getParaCode());
                                setValue = String.valueOf(map.get(paraNameAtOCRs[0]));
                                recipePara.setSetValue(setValue);
                                recipeParaList.add(recipePara);
                            }
                        } else {
                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,需保持在主页面.");
                            return null;
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
        attach.setAttachName(recipeName + ".Prg_V" + recipe.getVersionNo());
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
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".Prg", ftpPathTmp + recipe.getRecipeName() + ".Prg_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".Prg", ftpPathTmp + recipe.getRecipeName() + ".Prg", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".Prg", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.Prg");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.Prg", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".Prg");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".Prg\"");
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeName + ".Prg\"");
//                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\*.Prg\"");
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
                List<String> resultTemp = iSecsHost.executeCommand("curscreen");
                if (resultTemp.contains("main")) {
                    List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                    for (String str : result) {
                        if ("done".equals(str)) {
                            sleep(500);
                            List<String> result2 = iSecsHost.executeCommand("dialog \"Open\" write " + recipeName);
                            sleep(500);
                            List<String> result3 = iSecsHost.executeCommand("dialog \"Open\" 1 ");
                            List<String> result4 = iSecsHost.executeCommand("playback selrecipe1.txt");
                            if (result4.contains("done")) {
                                ppExecName = recipeName;
                                return "0";
                            }
                        }
                        if (str.contains("rror")) {
                            return "选中失败";
                        }
                    }
                } else {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "请返回主页面后下载程序!");
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
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {

                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
                sqlSession.close();
                Map map = new HashMap();

                List<RecipePara> recipeParaList = new ArrayList<>();
                List<String> result1 = iSecsHost.executeCommand("curscreen");
                if (result1.contains("main")) {
                    List<String> resultPara0 = iSecsHost.executeCommand("readm weight fr1");
                    map.put("weight", resultPara0.get(0));
                    map.put("fr1", resultPara0.get(1));
                    iSecsHost.executeCommand("playback pause.txt");

                    List<String> result2 = iSecsHost.executeCommand("playback gotorecipepara1.txt");
                    List<String> result222 = iSecsHost.executeCommand("curscreen");
                    if (result222.contains("recipepara1")) {
                        for (String str : result2) {
                            if ("done".equals(str)) {
                                List<String> resultPara1 = iSecsHost.executeCommand("read needle2 ");
                                map.put("needle2", resultPara1.get(0));
                                List<String> resultPara2 = iSecsHost.executeCommand("read needle3 ");
                                map.put("needle3", resultPara2.get(0));
                            }
                        }
                    }
                    List<String> result3 = iSecsHost.executeCommand("playback gotorecipepara2.txt");
                    List<String> result333 = iSecsHost.executeCommand("curscreen");
                    if (result333.contains("recipepara2")) {
                        for (String str : result3) {
                            if ("done".equals(str)) {
                                List<String> resultPara1 = iSecsHost.executeCommand("read presetting ");
                                map.put("presetting", resultPara1.get(0));
                                List<String> resultPara2 = iSecsHost.executeCommand("read dispheater ");
                                map.put("dispheater", resultPara2.get(0));
                                List<String> resultPara3 = iSecsHost.executeCommand("read postheater ");
                                map.put("postheater", resultPara3.get(0));
                                List<String> resultPara4 = iSecsHost.executeCommand("read nozzle ");
                                map.put("nozzle", resultPara4.get(0));
                            }
                        }
                    }
                    List<String> result4 = iSecsHost.executeCommand("playback gotorecipepara3.txt");
                    List<String> result444 = iSecsHost.executeCommand("curscreen");
                    if (result444.contains("recipepara3")) {
                        for (String str : result4) {
                            if ("done".equals(str)) {
                                List<String> resultPara1 = iSecsHost.executeCommand("read stroke ");
                                map.put("stroke", resultPara1.get(0));
                                List<String> resultPara2 = iSecsHost.executeCommand("read delay ");
                                map.put("delay", resultPara2.get(0));
                                List<String> resultPara3 = iSecsHost.executeCommand("read numberofpulse ");
                                map.put("numberofpulse", resultPara3.get(0));
                                List<String> resultPara4 = iSecsHost.executeCommand("read rising ");
                                map.put("rising", resultPara4.get(0));
                                List<String> resultPara5 = iSecsHost.executeCommand("read falling ");
                                map.put("falling", resultPara5.get(0));
                                List<String> resultPara6 = iSecsHost.executeCommand("read open ");
                                map.put("open", resultPara6.get(0));
                            }
                        }
                    }
                    iSecsHost.executeCommand("playback closerecipepara.txt");
                    for (RecipeTemplate recipeTemplate : recipeTemplates) {
                        RecipePara recipePara = new RecipePara();
                        String[] paraNameAtOCRs = new String[1];
                        paraNameAtOCRs[0] = recipeTemplate.getParaDesc();
                        String setValue = "";
                        recipePara.setParaName(recipeTemplate.getParaName());
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        setValue = String.valueOf(map.get(paraNameAtOCRs[0]));
                        recipePara.setSetValue(setValue);
                        recipeParaList.add(recipePara);
                    }
                }
                resultMap.put("recipeParaList", recipeParaList);

            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        logger.info("monitormap:" + resultMap.toString());
        return resultMap;
    }

    public List<String> getEquipEvent() {
        List<String> result = new ArrayList<>();
        try {
            result = iSecsHost.executeCommand("dos \"type D:\\DoDiscoLog.txt\"");
            for (String str : result) {
                if ("done".equals(str)) {
                    result.remove(str);
                }
            }
        } catch (Exception e) {
            logger.error("Get EquipEvent error:" + e.getMessage());
        }
        return result;
    }

    public String clearEquipEvent() {
        try {
            List<String> result = iSecsHost.executeCommand("dos \"del D:\\DoDiscoLog.txt\"");
            for (String str : result) {
                if ("done".equals(str)) {
                    return "0";
                }
            }
            return "Clear EquipEvent failed";
        } catch (Exception e) {
            logger.error("Clear EquipEvent error:" + e.getMessage());
            return "Clear EquipEvent failed";
        }
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
                    if (!str.contains(".Prg")) {
                        continue;
                    }
                    str = str.replace("[", "").replace(" ", "");
                    String[] dirs = str.split("]");
                    for (String dir : dirs) {
                        if (dir.equals(".") || dir.equals("..")) {
                            continue;
                        }
                        if (dir.contains(".Prg")) {
                            String[] dirsTemp = dir.split("\\.");
                            recipeNameList.add(dirsTemp[0]);
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
        InnovationHost newEquip = new InnovationHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
                for (int i = 0; i < 5; i++) {
                    List<String> alidresult = iSecsHost.executeCommand("read alarm" + i);
                    if (alidresult.size() > 1) {
                        if (alidresult.get(0).contains(">")) {
                            String alarmTmp = alidresult.get(0).split(">")[0].replaceAll("<", "");
                            alarmStrings.add(alarmTmp);
                            logger.info("Get alarm ALID=[" + alarmTmp + "]");
                            return alarmStrings;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    //状态尚未补全
                    if ("main".equals(result.get(0))) {
                        List<String> result2 = iSecsHost.executeCommand("read status");
                        if ("STOP".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Stop";
                        } else if ("RUN..".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Run";
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

    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".Prg", remoteRcpPath, recipeName + ".Prg_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        String stopResult = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = null;
            try {
                if (equipStatus.equalsIgnoreCase("RUN")) {
                    autoStart = true;
                    return "0";
                }
                result = iSecsHost.executeCommand("playback start.txt");
                for (String start : result) {
                    if ("done".equals(start)) {
                        autoStart = true;
                        return "0";
                    }
                }
            } catch (Exception e) {
            }

        }
        return stopResult;
    }

    @Override
    public boolean startCheck() {
        if (autoStart) {
            autoStart = false;
            return true;
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
                    startEquip();
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

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map map = new HashMap();
        List<String> result1 = iSecsHost.executeCommand("curscreen");
        if (result1.contains("main")) {
            List<String> resultPara0 = iSecsHost.executeCommand("read weight ");
            map.put("weight", resultPara0.get(0));
            iSecsHost.executeCommand("playback gotorecipepara1.txt");
            Map recipepara1map = iSecsHost.readAllParaByScreen("recipepara1");
            map.putAll(recipepara1map);
            iSecsHost.executeCommand("playback gotorecipepara2.txt");
            Map recipepara2map = iSecsHost.readAllParaByScreen("recipepara2");
            map.putAll(recipepara2map);
        }
        return map;
    }
}
