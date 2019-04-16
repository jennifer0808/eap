/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.disco.ws;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class DFD6750Host extends EquipModel {

    private static Logger logger = Logger.getLogger(DFD6750Host.class.getName());
    private Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();

    public DFD6750Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
 
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read pdevid").get(0);
                    } else {
                        if ("param".equals(result.get(0))) {
                            ppExecName = iSecsHost.executeCommand("read devid").get(0);
                        } else if ("work".equals(result.get(0))) {
                            ppExecName = iSecsHost.executeCommand("read workdevid").get(0);
                        }
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
    public String startEquip() {
        List<String> result = null;
        String startResult = "";
        try {
            result = iSecsHost.executeCommand("playback gotoworkscreen.txt");
            for (String str : result) {
                if ("done".equals(str)) {
                    result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                    for (String colorstr : result) {
                        if ("0xff0000".equals(colorstr)) {
                            equipStatus = "Run";
                            return "0";
                        }
                        if ("0x33cc33".equals(colorstr)) {
                            equipStatus = "Idle";
                            result = iSecsHost.executeCommand("playback startorstop.txt");
                            for (String start : result) {
                                if ("done".equals(start)) {
                                    result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                                    for (String colorstr2 : result) {
                                        if ("0xff0000".equals(colorstr2)) {
                                            equipStatus = "Run";
                                            return "0";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                startResult = "Start failed";
            }
        } catch (Exception e) {
        }
        return startResult;
    }

    @Override
    public String pauseEquip() {
        String stopResult = "";
//        if (getPassport(1)) {
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
                                    result = iSecsHost.executeCommand("playback startorstop.txt");
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
                                    if (repeatStop()) {
                                        return "0";
                                    }
//                                    result = iSecsHost.executeCommand("playback stop.txt");
//                                    for (String start : result) {
//                                        if ("done".equals(start)) {
//                                            result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
//                                            for (String colorstr2 : result) {
//                                                if ("0x33cc33".equals(colorstr2)) {
//                                                    equipStatus = "Idle";
//                                                    return "0";
//                                                }
//                                            }
//                                        }
//                                    }
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
                String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");

                String equipRecipeName = equipRecipeNames[1];
                // ftp ip user pwd lpath rpath "mput 001.ALU 001.CLN 001.DFD" 
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                sqlSession.close();

                String body = equipRecipeName + "," + recipeName + ",";
                TransferUtil.setPPBody(body, 0, GlobalConstants.localRecipePath + ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo());
                //  String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
                FtpUtil.uploadFile(GlobalConstants.localRecipePath + ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", "DEV.LST_V" + recipe.getVersionNo(), localftpip, ftpPort, ftpUser, ftpPwd);
                TransferUtil.setPPBody(body, 0, GlobalConstants.localRecipePath + ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo());
                FtpUtil.uploadFile(GlobalConstants.localRecipePath + ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", "DEVID.LST_V" + recipe.getVersionNo(), localftpip, ftpPort, ftpUser, ftpPwd);
                String equipRecipePathtmp = equipRecipePath;
                if (!"".equals(equipRecipeNames[0])) {
                    equipRecipePathtmp = equipRecipePath + "\\" + equipRecipeNames[0];
                }
                boolean ocrUploadOk = true;
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
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
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());

            }
        }
        this.deleteTempFile(recipeName);
        return resultMap;

    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            if (i == 0) {
                attach.setAttachName("DEV.LST_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo());
            } else if (i == 1) {
                attach.setAttachName("DEVID.LST_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo());
            } else if (i == 2) {
                attach.setAttachName(recipeName + ".ALU_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".ALU_V" + recipe.getVersionNo());
            } else if (i == 3) {
                attach.setAttachName(recipeName + ".CLN_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".CLN_V" + recipe.getVersionNo());
            } else if (i == 4) {
                attach.setAttachName(recipeName + ".DFD_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".DFD_V" + recipe.getVersionNo());
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
        sqlSession.close();
        return attachs;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        cleanRootDir();
        //ftp ip user pwd lpath rpath \"mget 001.ALU 001.CLN 001.DFD DEV.LST DEVID.LST\"       
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            try {
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;

                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                // String localPath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);

                String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);
//                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
//                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + localPath + "DEV.LST", ftpPathTmp + "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//                } else {
//                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + localPath + "DEV.LST", ftpPathTmp + "DEV.LST", ftpip, ftpPort, ftpUser, ftpPwd);
//                }
//                String body = TransferUtil.getPPBody(0, GlobalConstants.localRecipePath + localPath + "DEV.LST").get(0).toString();
//
//                String[] equipRecipeNames = body.split(",");
                String equipRecipeName = "001";//equipRecipeNames[0];
                String bodyTmp = "001," + recipe.getRecipeName() + ",";
                TransferUtil.setPPBody(bodyTmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST");
                TransferUtil.setPPBody(bodyTmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST");
                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                    return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                }
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                    //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPathTmp + "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".ALU", ftpPathTmp + recipe.getRecipeName() + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".CLN", ftpPathTmp + recipe.getRecipeName() + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", ftpPathTmp + recipe.getRecipeName() + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    // FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST", ftpPathTmp + "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPathTmp + "DEV.LST", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".ALU", ftpPathTmp + recipe.getRecipeName() + ".ALU", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".CLN", ftpPathTmp + recipe.getRecipeName() + ".CLN", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", ftpPathTmp + recipe.getRecipeName() + ".DFD", ftpip, ftpPort, ftpUser, ftpPwd);
                    // FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST", ftpPathTmp + "DEVID.LST", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD");
                    }
                }

                // FtpUtil.copyFile(ftpPathTmp, equipRecipeNames[1] + ".ALU_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "/temp/", equipRecipeName + ".ALU");
                //  FtpUtil.copyFile(ftpPathTmp, equipRecipeNames[1] + ".CLN_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "/temp/", equipRecipeName + ".CLN");
                //  FtpUtil.copyFile(ftpPathTmp, equipRecipeNames[1] + ".DFD_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "/temp/", equipRecipeName + ".DFD");
                //   FtpUtil.copyFile(ftpPathTmp, "DEV.LST_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "/temp/", "DEV.LST");
                //    FtpUtil.copyFile(ftpPathTmp, "DEVID.LST_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "/temp/", "DEVID.LST");
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD DEV.LST DEVID.LST\"");
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
        getEquipRecipeList();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                String equipRecipeNameTmp = recipeNameMappingMap.get(recipeName);
                if (equipRecipeNameTmp == null) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                }
                String[] equipRecipeNames = equipRecipeNameTmp.split(":");
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + equipRecipeNames[0] + "\\*\"");
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
                String[] equipRecipeNames = recipeNameMappingMap.get(ppExecName).split(":");
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String equipRecipeName = equipRecipeNames[1];
                String equipRecipePathtmp = equipRecipePath;
                if (!"".equals(equipRecipeNames[0])) {
                    equipRecipePathtmp = equipRecipePath + "\\" + equipRecipeNames[0];
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + equipRecipeName + ".DFD\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + equipRecipeName + ".DFD");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                List<RecipePara> recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
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
                result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + " /ad/w\"");
            } catch (Exception e) {
                return eppd;
            }
            if (result != null && result.size() > 1) {
                for (String str : result) {
                    if (!str.contains("[")) {
                        continue;
                    }
                    str = str.replace("[", "").replace(" ", "");
                    String[] dirs = str.split("]");
                    for (String dir : dirs) {
                        if (dir.contains(".") || dir.contains("..")) {
                            continue;
                        }
                        List<String> singleDirRecipe = iSecsHost.executeCommand("dos \"type " + equipRecipePath + "\\" + dir + "\\DEV.LST\"");
                        if (singleDirRecipe.size() > 0) {
                            for (String strTmp : singleDirRecipe) {
                                if (strTmp.contains(",")) {
                                    String[] recipeNameMappings = strTmp.split(",");
                                    for (int i = 0; i < recipeNameMappings.length; i++) {
                                        if (i == recipeNameMappings.length - 1) {
                                            break;
                                        }
                                        recipeNameMappingMap.put(recipeNameMappings[i + 1], dir + ":" + recipeNameMappings[i]);
                                        recipeNameList.add(recipeNameMappings[i + 1]);
                                        i = i + 1;

                                    }
                                }
                            }
                        }
                    }
                }
                List<String> singleDirRecipe1 = iSecsHost.executeCommand("dos \"type " + equipRecipePath + "\\DEV.LST\"");
                if (singleDirRecipe1.size() > 0) {
                    for (String strTmp : singleDirRecipe1) {
                        if (strTmp.contains(",")) {
                            String[] recipeNameMappings = strTmp.split(",");
                            for (int i = 0; i < recipeNameMappings.length; i++) {
                                if (i == recipeNameMappings.length - 1) {
                                    break;
                                }
                                recipeNameMappingMap.put(recipeNameMappings[i + 1], ":" + recipeNameMappings[i]);
                                recipeNameList.add(recipeNameMappings[i + 1]);
                                i = i + 1;

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
    public void run() {
        while (!this.isInterrupted()) {
            if (!isInitState) {
                getEquipRealTimeState();
                isInitState = true;
            }
        }
    }

    @Override
    public Object clone() {
        DFD6750Host newEquip = new DFD6750Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("readrectcolor 730 50 750 60 ");
                for (String colorstr : result) {
                    if ("0xc0c0c0".equals(colorstr)) {
                        alarmStrings.add("");
                    }
                    if ("0xff0000".equals(colorstr)) {
                        logger.info("The equip state changged to alarm...");
                        List<String> alidresult = iSecsHost.executeCommand("read alarmid");
                        if (alidresult.size() > 1) {
                            alarmStrings.add(alidresult.get(0));
                            logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        //添加了一个过滤，异常时可能会将
        if (alarmStrings.size() > 0) {
            alarmStrings.remove("0xff0000");
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
                    if ("pause".equals(result.get(0))) {
                        equipStatus = "Pause";
                    } else if ("work".equals(result.get(0))) {
                        List<String> result2 = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                        if ("0xff0000".equals(result2.get(0))) {
                            equipStatus = "Run";
                        } else {
                            equipStatus = "Idle";
                        }
                    } else if ("main".equals(result.get(0)) || "any".equals(result.get(0))) {
                        equipStatus = "Idle";
                    } else if (result.get(0).contains("ready")) {
                        equipStatus = "Ready";
                    } else if ("param".equals(result.get(0))) {
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备处于参数设置页面,暂时无法刷新设备状态.页面改变后将会定时刷新");
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


    public boolean checkBladeCode() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    protected Map getBladeCode() {
        Map bladeCodeMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        equipStatus = "Pause";
                    } else if ("main".equals(result.get(0)) || "work".equals(result.get(0))) {
                        List<String> result1 = iSecsHost.executeCommand("goto dpqb");
                        for (String str : result1) {
                            if ("done".equals(str)) {
                                List<String> result2 = iSecsHost.executeCommand("readm dppzz1 dppzz2");
                                if (result2.size() == 3) {
                                    bladeCodeMap.put("dppzz1", result2.get(0));
                                    bladeCodeMap.put("dppzz2", result2.get(1));
                                }
                            }
                        }
                    } else if ("param".equals(result.get(0))) {
                        equipStatus = "Idle";
                    } else if ("done".equalsIgnoreCase(result.get(0))) {
                        //  equipStatus = "Idle";
                    } else {
                        equipStatus = "Idle";
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        return bladeCodeMap;
    }

    private String getBladeGroupCode(String ppExecName) {
        String bladeGroupCode = "未找到刀片组编号,请核对当前程序名是否正确.当前程序名[" + ppExecName + "]";
        if ("--".equals(ppExecName)) {
            getCurrentRecipeName();
        }
        String[] ppExecNames = ppExecName.split("-");
        if (ppExecNames == null || ppExecNames.length < 3) {
            bladeGroupCode = "未找到刀片组编号,请核对当前程序名是否正确.当前程序名[" + ppExecName + "]";
        } else {
            if (ppExecNames.length == 4) {
                if (isNumber(ppExecNames[2])) {
                    bladeGroupCode = ppExecNames[2];
                }
            }
            if (ppExecNames.length == 5) {
                if (isNumber(ppExecNames[3])) {
                    bladeGroupCode = ppExecNames[3];
                }
            }
            if (ppExecNames.length == 7) {
                if (isNumber(ppExecNames[4])) {
                    bladeGroupCode = ppExecNames[4];
                }
            }
            if (ppExecNames.length == 6) {
                if (isNumber(ppExecNames[3])) {
                    bladeGroupCode = ppExecNames[3];
                }
                if (isNumber(ppExecNames[4])) {
                    bladeGroupCode = ppExecNames[4];
                }
            }

        }
        return bladeGroupCode;
    }

    private boolean isNumber(String bladeGroupCode) {
        try {
            Integer.parseInt(bladeGroupCode);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");
        String equipRecipeName = equipRecipeNames[1];
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + remoteRcpPath + "DEV.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

        FtpUtil.uploadFile(GlobalConstants.localRecipePath + remoteRcpPath + "DEVID.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".ALU", remoteRcpPath, recipeName + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".CLN", remoteRcpPath, recipeName + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD", remoteRcpPath, recipeName + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
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
                logger.error("目录清理时发生异常:" + e.getMessage());
            }
        }
    }

    @Override
    public void deleteAllRcpFromDevice(String currentRecipeName) {
        //这里重写了此方法：由于dfd6560客户要求保留磨刀程序，所以此处不执行全删除
    }

    @Override
    protected boolean specialCheck() {
        getCurrentRecipeName();
        String bladeGroup = getBladeGroupCode(ppExecName);
        logger.info("当前程序名:" + ppExecName + " 获取到的刀片组id:" + bladeGroup);
        Map serverMap = AxisUtility.getBladeTypeByGroupFromServer(bladeGroup);
        if (serverMap != null && !serverMap.isEmpty()) {
            logger.info("server 获取到的刀片信息:" + serverMap.toString());
            String serverZ1 = String.valueOf(serverMap.get("Z1"));
            String serverZ2 = String.valueOf(serverMap.get("Z2"));
            Map equipMap = getBladeCode();
            if (equipMap != null && !equipMap.isEmpty()) {
                String equipZ1 = String.valueOf(equipMap.get("dppzz1"));
                String equipZ2 = String.valueOf(equipMap.get("dppzz2"));
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机刀片检测:刀片组编号[" + bladeGroup + "]");
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机刀片检测:刀片组信息Z1[" + serverZ1 + "]Z2[" + serverZ2 + "]");
                if (serverZ1.equals(equipZ1)) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机刀片检测:设备刀片信息Z1[" + equipZ1 + "],比对一致,检查通过");
                } else {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机刀片检测:设备刀片信息Z1[" + equipZ1 + "],比对不一致,检查不通过");
                    return false;
                }
                if (serverZ2.equals(equipZ2)) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机刀片检测:设备刀片信息Z2[" + equipZ2 + "],比对一致,检查通过");
                } else {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机刀片检测:设备刀片信息Z2[" + equipZ2 + "],比对不一致,检查不通过");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean repeatStop() {
        List<String> result = iSecsHost.executeCommand("playback stop.txt");
        for (String string : result) {
            if ("done".equals(string)) {
                List<String> curscreens = iSecsHost.executeCommand("curscreen");
                for (String curscreen : curscreens) {
                    if ("pause".equals(curscreen)) {
                        return true;
                    } else if ("work".equals(curscreen)) {
                        List<String> result2 = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                        if ("0xff0000".equals(result2.get(0))) {
                            for (int i = 0; i < 10; i++) {
                                try {
                                    Thread.sleep(500);
                                    iSecsHost.executeCommand("playback stop.txt");
                                } catch (InterruptedException ex) {

                                }
                                List<String> result3 = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
                                if (!"0xff0000".equals(result3.get(0))) {
                                    return true;
                                }
                            }
                        } else {
                            return true;
                        }
                    } else if ("main".equals(curscreen) || "any".equals(curscreen)) {
                        return true;
                    } else if ("ready".equalsIgnoreCase(curscreen)) {
                        return true;
                    } else if ("param".equals(curscreen)) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

}
