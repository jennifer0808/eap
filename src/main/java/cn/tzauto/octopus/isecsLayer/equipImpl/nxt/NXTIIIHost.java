/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.nxt;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.resolver.nxt.NXTIIIRecipeUtil;
import cn.tzauto.octopus.isecsLayer.socket.SocketUtil;
import java.net.Socket;
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
public class NXTIIIHost extends EquipModel {

    private static Logger logger = Logger.getLogger(NXTIIIHost.class.getName());
    private boolean needLock = false;
    List<String> stateList = new ArrayList<>();
    Map<String, String> recipeNameMap = new HashMap<>();

    public NXTIIIHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        
    }

    @Override
    public String getCurrentRecipeName() {
        for (String str : stateList) {
            if (str.contains("Device:" + deviceCode) || str.contains("Device:SMT02")) {
                String[] strs = str.split(";");
                ppExecName = strs[2].split("=")[1].split("\\s")[0].replaceAll("----", "--");
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String pauseEquip() {
        String stopResult = "0";
        return stopResult;
    }

    @Override
    public String stopEquip() {
        String stopResult = "0";
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
                String recipeNameTemp = recipeNameMap.get(recipeName);
                logger.info("recipename=" + recipeName + "recipenametemp=" + recipeNameTemp);
                if (findRecipe(recipeNameTemp)) {
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

                    TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");

                    List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                            + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                            + recipeNameTemp + ".JOB\"");
                    for (String uploadstr : result) {
                        if ("done".equals(uploadstr)) {
                            List<RecipePara> recipeParaList = new ArrayList<>();
                            try {
                                Map paraMap = NXTIIIRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".JOB");//DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".JOB");
                                if (paraMap != null && !paraMap.isEmpty()) {
//                                    recipeParaList = null;
                                    //DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
                                    recipeParaList = NXTIIIRecipeUtil.transferFromDB(paraMap, deviceType);
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
                } else {
                    UiLogUtil.appendLog2EventTab(deviceCode, "未找到Recipe文件，确认是否成功导出");
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
        attach.setAttachName(recipeNameMap.get(recipeName) + ".JOB_V" + recipe.getVersionNo());
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
                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                    return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                }
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB", ftpPathTmp + recipe.getRecipeName() + ".JOB_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB", ftpPathTmp + recipe.getRecipeName() + ".JOB", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.JOB");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.JOB", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".JOB\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        //执行导入
                        if (!checkEquipCodeLocation()) {
                            logger.info("对应位置的设备编号不正确，导入已取消");
                            return "0";
                        }
                        List<String> result3 = iSecsHost.executeCommand("replay import.exe");
//                        Thread.sleep(1000);
//                        String s="打开";
//                        s.getBytes
//                        iSecsHost.executeCommand("dos \"dialog \"打开\" write " + recipe.getRecipeName() + " \"");

//                        List<String> result14 = iSecsHost.executeCommand("write recipename " + recipe.getRecipeName());
//                        Thread.sleep(500);
//                        List<String> result4 = iSecsHost.executeCommand("replay importok.exe");
                        List<String> result21 = iSecsHost.executeCommand("playback selimport.txt");
                        List<String> result2 = iSecsHost.executeCommand("playback importrecipe.txt");
                        List<String> result5 = iSecsHost.executeCommand("replay enter.exe");
                        return "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
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
                while (true) {
                    List<String> needdel = iSecsHost.executeCommand("read jobflag");
                    for (String string : needdel) {
                        if ("done".equalsIgnoreCase(string)) {
                            continue;
                        }
                        if ("job".equalsIgnoreCase(string)) {
                            iSecsHost.executeCommand("replay delrecipe.exe");
                        } else {
                            return "删除成功";
                        }
                    }
                }
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
                for (String str : result) {
                    if ("done".equals(str)) {
                        return "0";
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
                        + ppExecName + ".JOB\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = null;// DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName + ".JOB");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                List<RecipePara> recipeParaList = null;// DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
                                map.put("recipeParaList", recipeParaList);
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
//        cleanRecipe("");
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            if (!checkEquipCodeLocation()) {
//                logger.info("对应位置的设备编号不正确，操作已取消");
//                return eppd;
//            }
//            List<String> exportResult = iSecsHost.executeCommand("playback outportrecipe.txt");
//            for (String string : exportResult) {
//                if ("done".equals(string)) {
//                    for (int i = 0; i < 40; i++) {
//                        List<String> curscreens = iSecsHost.executeCommand("curscreen");
//                        for (String curscreen : curscreens) {
//                            if ("export".equals(curscreen)) {
//                                try {
//                                    Thread.sleep(500);
//                                } catch (InterruptedException ex) {
//                                }
//                            } else {
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
            List<String> result = new ArrayList<>();
            try {
                result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + " /a/w\"");
            } catch (Exception e) {
                return eppd;
            }
            if (result != null && result.size() > 1) {
                for (String str : result) {
                    if (str.contains(".JOB")) {
                        str = str.trim();
                        String[] recipeNameTmp = str.split(".JOB");
                        for (int i = 0; i < recipeNameTmp.length; i++) {
//                            String nameShort = recipeNameTmp[i].replaceAll(".JOB", "").replaceAll("\\[", "").replace("..]", "").replace(".]", "").replace("]", "").trim().split("~")[0];
                            String nameShort = recipeNameTmp[i].replaceAll(".JOB", "").replaceAll("\\[", "").replace("..]", "").replace(".]", "").replace("]", "").trim().split("-Line")[0];
                            recipeNameList.add(nameShort);
                            recipeNameMap.put(nameShort, recipeNameTmp[i]);
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
        NXTIIIHost newEquip = new NXTIIIHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
        }
        return alarmStrings;
    }

    @Override
    public String getEquipStatus() {
        if (!stateList.isEmpty()) {
            stateList.clear();
        }
        Socket socket = SocketUtil.connectToClient(iSecsHost.ip, 12008);
        stateList = SocketUtil.doCommanAndGetMultReply(socket, "State");
        for (String str : stateList) {
            if (str.contains("Device:" + deviceCode) || str.contains("Device:" + deviceCode.replaceAll("-003", "02"))) {
                String[] strs = str.split(";");
                equipStatus = strs[1].split(":")[1];
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeNameMap.get(recipeName) + ".JOB", remoteRcpPath, recipeNameMap.get(recipeName) + ".JOB_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        UiLogUtil.appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean findRecipe(String recipeName) {
        List<String> resulttmp = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + "\\ /a/w\"");
        if (resulttmp != null && resulttmp.size() > 1) {
            for (String str : resulttmp) {
                if (str.contains(".JOB")) {
                    String[] recipeNameTmp = str.split(".JOB");
                    for (int i = 0; i < recipeNameTmp.length; i++) {
                        if (recipeName.equals(recipeNameTmp[i].replaceAll(".JOB", "").trim())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void cleanRecipe(String keepRecipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result01 = iSecsHost.executeCommand("dos \"del /s " + equipRecipePath + "\\*.JOB\"");
        }
    }

    private boolean checkEquipCodeLocation() {
        iSecsHost.executeCommand("curscreen");
        List<String> results = iSecsHost.executeCommand("read equipcode");
        for (String result : results) {
            if ("SMT-002".equals(result)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {

        return new HashMap();
    }
}
