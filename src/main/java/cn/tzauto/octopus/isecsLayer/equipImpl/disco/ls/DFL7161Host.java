/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.disco.ls;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
public class DFL7161Host extends EquipModel {

    private static Logger logger = Logger.getLogger(DFL7161Host.class.getName());
    private Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();

    public DFL7161Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
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
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map valueMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            if (deviceType.contains("Z1")) {
                String curscreen = iSecsHost.executeCommand("curscreen").get(0);
                if ("work".equals(curscreen)) {
                    //param
                    iSecsHost.executeCommand("playback gotoparafromwork.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    //scsch1
                    iSecsHost.executeCommand("playback gotoscsch12.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map scsch1Map = iSecsHost.readAllParaByScreen("scsch1");
                    logger.debug("scsch1Map:" + scsch1Map);
                    valueMap.putAll(scsch1Map);
                    //scsch2
                    iSecsHost.executeCommand("playback gotoscsch3.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map scsch3Map = iSecsHost.readAllParaByScreen("scsch3");
                    logger.debug("scsch3Map:" + scsch3Map);
                    valueMap.putAll(scsch3Map);
                    //scsch3
                    iSecsHost.executeCommand("playback gotoscsch4.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map scsch4Map = iSecsHost.readAllParaByScreen("scsch4");
                    logger.debug("scsch4Map:" + scsch4Map);
                    valueMap.putAll(scsch4Map);
                    //返回work界面
                    iSecsHost.executeCommand("playback gotoworkscreen.txt");
                }
            } else {
                String curscreen = iSecsHost.executeCommand("curscreen").get(0);
                if ("work".equals(curscreen)) {
                    //param
                    iSecsHost.executeCommand("playback gotoparamfromwork.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map paramMap = iSecsHost.readAllParaByScreen("param");
                    logger.debug("paramMap:" + paramMap);
                    valueMap.putAll(paramMap);
                    //scsch1
                    iSecsHost.executeCommand("playback gotoscsh1.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map scsch1Map = iSecsHost.readAllParaByScreen("scsch1");
                    logger.debug("scsch1Map:" + scsch1Map);
                    valueMap.putAll(scsch1Map);
                    //scsch2
                    iSecsHost.executeCommand("playback gotoscsh2.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map scsch2Map = iSecsHost.readAllParaByScreen("scsch2");
                    logger.debug("scsch2Map:" + scsch2Map);
                    valueMap.putAll(scsch2Map);
                    //scsch3
                    iSecsHost.executeCommand("playback gotoscsh3.txt");
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Map scsch3Map = iSecsHost.readAllParaByScreen("scsch3");
                    logger.debug("scsch3Map:" + scsch3Map);
                    valueMap.putAll(scsch3Map);
                    //返回work界面
                    iSecsHost.executeCommand("playback gotoworkscreen.txt");
                }
            }
        }
        logger.debug("valueMap:" + valueMap);
        return valueMap;
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
            logger.error(e.getMessage());
        }
        return startResult;
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
                List<String> result;
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
                                }
                            }
                            stopResult = "锁机失败,当前状态无法执行锁机";
                        }
                    }
                } catch (Exception e) {
                    logger.error("锁机时异常:" + e.getMessage());
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
                List<String> result;
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
//                                    result = iSecsHost.executeCommand("playback startorstop.txt");
//                                    for (String start : result) {
//                                        if ("done".equals(start)) {
//                                            result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
//                                            for (String colorstr2 : result) {
//                                                if ("0x33cc33".equals(colorstr2)) {
//                                                    equipStatus = "Idle";
//                                                    return "0";
//                                                } else {
//                                                    for (int i = 0; i < 3; i++) {
//                                                        try {
//                                                            Thread.sleep(1000);
//                                                        } catch (InterruptedException e) {
//
//                                                        }
//                                                        result = iSecsHost.executeCommand("readrectcolor 980 150 1000 174");
//                                                        for (String colorstr3 : result) {
//                                                            if ("0x33cc33".equals(colorstr3)) {
//                                                                equipStatus = "Idle";
//                                                                return "0";
//                                                            }
//                                                        }
//                                                    }
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
                    logger.error("锁机时异常:" + e.getMessage());
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
                String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");
                String equipRecipeName = equipRecipeNames[1];
                // ftp ip user pwd lpath rpath "mput 001.ALU 001.CLN 001.DFD"
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                //  String ftpip = GlobalConstants.ftpIP;
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
                        + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD "
                        + equipRecipeName + ".COT\"");
                for (String uploadstr : result) {
                    if (uploadstr.contains("rror") || uploadstr.contains("Not connected")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        return resultMap;
                    }
                    if ("done".equals(uploadstr)) {

//                        FtpUtil.uploadFile(GlobalConstants.localRecipePath + ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo(), ftpRecipePath, "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//
//                        FtpUtil.uploadFile(GlobalConstants.localRecipePath + ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo(), ftpRecipePath, "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//
//                        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".ALU", ftpRecipePath, recipeName + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//                        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".CLN", ftpRecipePath, recipeName + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//                        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD", ftpRecipePath, recipeName + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//                        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".COT", ftpRecipePath, recipeName + ".COT_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                        //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + ftpRecipePath + recipeName + ".DFD_V" + recipe.getVersionNo(),
                        //         ftpRecipePath + recipeName + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD");
                            Map CLNMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".CLN");
                            Map CotMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".COT");
                            paraMap.putAll(CLNMap);
                            paraMap.putAll(CotMap);
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
                logger.error("Get equip status error:" ,e);

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
        for (int i = 0; i < 6; i++) {
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
            } else if (i == 5) {
                attach.setAttachName(recipeName + ".COT_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".COT_V" + recipe.getVersionNo());
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
        cleanRootDir();
        //ftp ip user pwd lpath rpath \"mget 001.ALU 001.CLN 001.DFD DEV.LST DEVID.LST\"
        String downloadResult = "";
        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        synchronized (socketClient) {
            try {
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);
                String equipRecipeName = "001";
                String bodyTmp = "001," + recipe.getRecipeName() + ",";
                TransferUtil.setPPBody(bodyTmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST");
                TransferUtil.setPPBody(bodyTmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST");
                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                    return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                }
                boolean ftpDownloadOK = false;
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                    //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPathTmp + "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    if (FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".ALU", ftpPathTmp + recipe.getRecipeName() + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                            && FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".CLN", ftpPathTmp + recipe.getRecipeName() + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                            && FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", ftpPathTmp + recipe.getRecipeName() + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                            && FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".COT", ftpPathTmp + recipe.getRecipeName() + ".COT_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                        ftpDownloadOK = true;
                    }

                    //FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST", ftpPathTmp + "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPathTmp + "DEV.LST", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".ALU", ftpPathTmp + recipe.getRecipeName() + ".ALU", ftpip, ftpPort, ftpUser, ftpPwd)
                            && FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".CLN", ftpPathTmp + recipe.getRecipeName() + ".CLN", ftpip, ftpPort, ftpUser, ftpPwd)
                            && FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", ftpPathTmp + recipe.getRecipeName() + ".DFD", ftpip, ftpPort, ftpUser, ftpPwd)
                            && FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".COT", ftpPathTmp + recipe.getRecipeName() + ".COT", ftpip, ftpPort, ftpUser, ftpPwd)) {
                        ftpDownloadOK = true;
                    }

                    //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST", ftpPathTmp + "DEVID.LST", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD");
                    }
                }
                if (!ftpDownloadOK) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + " 到工控机时失败,请检查FTP是否存在此程序.");
                    return "下载Recipe:" + recipe.getRecipeName() + " 到工控机时失败,请检查FTP是否存在此程序.";
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + equipRecipeName + ".ALU "
                        + equipRecipeName + ".CLN " + equipRecipeName + ".DFD "
                        + equipRecipeName + ".COT DEV.LST DEVID.LST\"");
                for (String str : result) {
                    if (str.contains("rror")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + " 时失败,请检查FTP服务是否开启.");
                        return "下载Recipe:" + recipe.getRecipeName() + "到设备时失败,请检查FTP服务是否开启.";
                    }
                    if ("done".equals(str)) {
                        downloadResult = "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "到设备时失败,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
                return "Download recipe " + recipe.getRecipeName() + " failed";
            } finally {
                sqlSession.close();
            }
        }
        this.deleteTempFile(recipe.getRecipeName());
        return downloadResult;
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
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + equipRecipeNames[0] + "\\" + equipRecipeNames[1] + ".*\"");
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
                List<String> result = iSecsHost.executeCommand("playback selrecipe1.txt");
                for (String str : result) {

                    if ("done".equals(str)) {
                        ppExecName = recipeName;
                        return "0";
                    }
                    if (("rror").contains(str)) {
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
        getEquipRecipeList();
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
                String body = equipRecipeName + "," + ppExecName + ",";
                TransferUtil.setPPBody(body, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + equipRecipeName + ".DFD\"");
                iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + equipRecipeName + ".CLN\"");
                iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + equipRecipeName + ".COT\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + equipRecipeName + ".DFD");
                            Map CLNMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + equipRecipeName + ".CLN");
                            Map CotMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + equipRecipeName + ".COT");
                            paraMap.putAll(CLNMap);
                            paraMap.putAll(CotMap);
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
        deleteTempFile(ppExecName);
        return map;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result;
            try {
                String cmd ="dos \"dir " + equipRecipePath + " /ad/w\"";
                result = iSecsHost.executeCommand(cmd,"GBK");
            } catch (Exception e) {
                return eppd;
            }

            if (result.size() > 1) {
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
        DFL7161Host newEquip = new DFL7161Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("readrectcolor 740 60 760 65 ");
                if (result == null || result.isEmpty()) {
                    return null;
                }
                for (String colorstr : result) {
                    if ("0xc0c0c0".equals(colorstr)) {
                        alarmStrings.add("");
                    } else if ("0xff0000".equals(colorstr)) {
                        logger.info("The equip state changged to alarm...");
                        List<String> resultAlarmId = iSecsHost.executeCommand("read alarmid");
                        if (resultAlarmId.size() > 1) {
                            alarmStrings.add(resultAlarmId.get(0));
                            logger.info("Get alarm ALID=[" + resultAlarmId.get(0) + "]");
                        }
                    } else {
                        alarmStrings.add("");
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        if (alarmStrings.size() > 0) {
            alarmStrings.remove("0xff0000");
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
                    } else if ("ready".equalsIgnoreCase(result.get(0))) {
                        equipStatus = "Ready";
                    } else if ("param".equals(result.get(0))) {
                        equipStatus = "SetUp";
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return equipStatus;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");
        String equipRecipeName = equipRecipeNames[1];
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + remoteRcpPath + "DEV.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:DEV.LST_V" + recipe.getVersionNo() + " 工控路径:" + GlobalConstants.localRecipePath + remoteRcpPath);
            return false;
        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + remoteRcpPath + "DEVID.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:DEVID.LST_V" + recipe.getVersionNo() + " 工控路径:" + GlobalConstants.localRecipePath + remoteRcpPath);
            return false;
        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".ALU", remoteRcpPath, recipeName + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + equipRecipeName + ".ALU 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".CLN", remoteRcpPath, recipeName + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + equipRecipeName + ".CLN 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD", remoteRcpPath, recipeName + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + equipRecipeName + ".DFD 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".COT", remoteRcpPath, recipeName + ".COT_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + equipRecipeName + ".COT 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    public Map getLaserEnergy() {
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("powercheck".equals(result.get(0))) {

                        List<String> laserEnergyresult = iSecsHost.executeCommand("readm frequency p1uv p1power p2uv p2power p3uv p3power p4uv p4power p5uv p5power");
                        if (laserEnergyresult.size() > 2) {
                            map.put("frequency", laserEnergyresult.get(0));
                            map.put("p1uv", laserEnergyresult.get(1));
                            map.put("p1power", laserEnergyresult.get(2));
                            map.put("p2uv", laserEnergyresult.get(3));
                            map.put("p2power", laserEnergyresult.get(4));
                            map.put("p3uv", laserEnergyresult.get(5));
                            map.put("p3power", laserEnergyresult.get(6));
                            map.put("p4uv", laserEnergyresult.get(7));
                            map.put("p4power", laserEnergyresult.get(8));
                            map.put("p5uv", laserEnergyresult.get(9));
                            map.put("p5power", laserEnergyresult.get(10));
                            logger.info("get LaserEnergy data." + map.toString());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip LaserEnergy error:" + e.getMessage());
            }
        }
        return map;

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
