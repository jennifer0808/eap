/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.hanmi;

import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 *
 * @author luosy
 */
public class Hanmi20000DAHostUpload extends EquipModel {

    private static Logger logger = Logger.getLogger(Hanmi20000DAHostUpload.class.getName());
    private final Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();
    private ISecsHost sawISecsHost;
    private ISecsHost visionISecsHost;
    private String sawRecipePath;
    private String visionRecipePath;
    private ISecsHost handleRecipeHost;
    private final Map<String, String> handleRecipeMap = new HashMap();
    String handleRecipeName = "";

    public Hanmi20000DAHostUpload(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        iSecsHostList.add(iSecsHost);
        handleRecipeHost = new ISecsHost(remoteIPAddress, String.valueOf(12005), deviceType, deviceCode);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo sawinfo = deviceService.getDeviceInfoByDeviceCode(deviceCode + "-S").get(0);
        Map<String, DeviceType> deviceTypeDic = deviceService.getDeviceTypeMap();
        if (sawinfo == null || sawinfo.getDeviceIp() == null) {
            logger.error(deviceCode + " 未配置saw子机信息或者信息不完整");
            return;
        }
        DeviceType sawdeviceType = deviceTypeDic.get(sawinfo.getDeviceTypeId());
        sawRecipePath = sawdeviceType.getSmlPath();
        sawISecsHost = new ISecsHost(sawinfo.getDeviceIp(), String.valueOf(remoteTCPPort), sawinfo.getDeviceType(), deviceCode + "-S");
        iSecsHostList.add(sawISecsHost);
        logger.info(deviceCode + " saw子机初始化完成");
        DeviceInfo versioninfo = deviceService.getDeviceInfoByDeviceCode(deviceCode + "-V").get(0);
        if (versioninfo == null || versioninfo.getDeviceIp() == null) {
            logger.error(deviceCode + " 未配置version子机信息或者信息不完整");
            return;
        }
        DeviceType deviceType = deviceTypeDic.get(versioninfo.getDeviceTypeId());
        visionRecipePath = deviceType.getSmlPath();
        visionISecsHost = new ISecsHost(versioninfo.getDeviceIp(), String.valueOf(remoteTCPPort), versioninfo.getDeviceType(), deviceCode + "-V");
        iSecsHostList.add(visionISecsHost);
        logger.info(deviceCode + " version子机初始化完成");
        if (this.isConnect()) {
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
        sqlSession.close();
    }

    @Override
    public void initialize(ISecsHost iSecsHosttmp) {
        if (iSecsHosttmp != null) {
            String flag = iSecsHosttmp.deviceCode.substring(iSecsHosttmp.deviceCode.length() - 2, iSecsHosttmp.deviceCode.length());
            if ("-S".equals(flag)) {
                sawISecsHost = new ISecsHost(iSecsHosttmp.ip, String.valueOf(remoteTCPPort), iSecsHosttmp.deviceTypeCode, iSecsHosttmp.deviceCode);
            } else if ("-V".equals(flag)) {
                visionISecsHost = new ISecsHost(iSecsHosttmp.ip, String.valueOf(remoteTCPPort), iSecsHosttmp.deviceTypeCode, iSecsHosttmp.deviceCode);
            } else {
                iSecsHost = new ISecsHost(iSecsHosttmp.ip, String.valueOf(remoteTCPPort), iSecsHosttmp.deviceTypeCode, iSecsHosttmp.deviceCode);
            }
            iSecsHostList.clear();
            iSecsHostList.add(iSecsHost);
            iSecsHostList.add(sawISecsHost);
            iSecsHostList.add(visionISecsHost);
        }
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = sawISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        ppExecName = sawISecsHost.executeCommand("read pdevid").get(0);
                    } else {
                        if ("param".equals(result.get(0))) {
                            ppExecName = sawISecsHost.executeCommand("read devid").get(0);
                        } else if ("work".equals(result.get(0))) {
                            ppExecName = sawISecsHost.executeCommand("read workdevid").get(0);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }

        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        handleRecipeName = iSecsHost.executeCommand("read recipename").get(0);
                    } else {
                        if ("main".equals(result.get(0))) {
                            handleRecipeName = iSecsHost.executeCommand("read mainrecipename").get(0);
                        } else if ("work".equals(result.get(0))) {
                            handleRecipeName = iSecsHost.executeCommand("read recipename").get(0);
                        }
                    }
                }
                String[] handleRecipeNames = handleRecipeName.split(",");
                String handleG = handleRecipeNames[0].replaceAll("[G]", "").replaceAll("\\[]", "").trim();
                String handleD = handleRecipeNames[1].replaceAll("[D]", "").replaceAll("\\[]", "").trim();
                String handleV = handleRecipeNames[2].replaceAll("[V]", "").replaceAll("\\[]", "").trim();
                handleRecipeName = handleG + "-" + handleD + "-" + handleV;

                handleRecipeName = handleV;

            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        if (!ppExecName.contains(handleRecipeName)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Saw程序与Handle程序不符.Saw:" + ppExecName + " Handle:" + handleRecipeName);
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
        return startResult;
    }

    @Override
    public String pauseEquip() {
        String stopResult = "";
//        if (getPassport(1)) {
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    this.getEquipStatus();
                    sawISecsHost.executeCommand("playback gotoworkscreen.txt");
                    result = sawISecsHost.executeCommand("curscreen");
                    if (result != null && !result.isEmpty()) {
                        if ("pause".equals(result.get(0))) {
                            return "0";//"main".equals(result.get(0)) ||
                        } else if ("work".equals(result.get(0))) {
                            result = sawISecsHost.executeCommand("readrectcolor 970 160 990 174");
                            for (String colorstr : result) {
                                if ("0x33cc33".equals(colorstr)) {
                                    equipStatus = "Idle";
                                    return "0";
                                }
                                if ("0xff0000".equals(colorstr)) {
                                    equipStatus = "Run";
                                    result = sawISecsHost.executeCommand("playback startorstop.txt");
                                    for (String start : result) {
                                        if ("done".equals(start)) {
                                            result = sawISecsHost.executeCommand("readrectcolor 970 160 990 174");
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
        return stopResult;
    }

    @Override
    public String stopEquip() {
        String stopResult = "";
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt == null || !"Y".equals(deviceInfoExt.getLockSwitch())) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            stopResult = "未设置锁机！";
            return stopResult;
        }
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = null;
            try {
                sawISecsHost.executeCommand("playback gotoworkscreen.txt");
                result = sawISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        return "0";//"main".equals(result.get(0)) ||
                    } else if ("work".equals(result.get(0))) {
                        result = sawISecsHost.executeCommand("readrectcolor 970 160 990 174");
                        for (String colorstr : result) {
                            if ("0x33cc33".equals(colorstr)) {
                                equipStatus = "Idle";
                                return "0";
                            }
                            if ("0xff0000".equals(colorstr)) {
                                equipStatus = "Run";
                                result = sawISecsHost.executeCommand("playback stop.txt");
                                for (String start : result) {
                                    if ("done".equals(start)) {
                                        result = sawISecsHost.executeCommand("readrectcolor 970 160 990 174");
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
        }
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = null;
            try {
                result = visionISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("run".equals(result.get(0))) {
                        return "0";//"main".equals(result.get(0)) ||
                    } else if ("setup".equals(result.get(0))) {
                        result = visionISecsHost.executeCommand("playback stop.txt");
                        for (String start : result) {
                            if ("done".equals(start)) {
                                result = visionISecsHost.executeCommand("curscreen");
                                for (String colorstr2 : result) {
                                    if ("run".equals(colorstr2)) {
                                        equipStatus = "Idle";
                                        return "0";
                                    }
                                }
                            }
                        }
                        stopResult = "锁机失败,当前状态无法执行锁机";
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
        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        Recipe recipe = setRecipe(recipeName);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        sqlSession.close();
        String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");
        String equipRecipeName = equipRecipeNames[1];
        String equipRecipePathtmp = sawRecipePath;
        boolean ocrUploadOk = true;
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {

                // ftp ip user pwd lpath rpath "mput 001.ALU 001.CLN 001.DFD" 
                String body = equipRecipeName + "," + recipeName + ",";
                TransferUtil.setPPBody(body, 0, GlobalConstants.localRecipePath + ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo());
                //  String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
                FtpUtil.uploadFile(GlobalConstants.localRecipePath + ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", "DEV.LST_V" + recipe.getVersionNo(), localftpip, ftpPort, ftpUser, ftpPwd);
                TransferUtil.setPPBody(body, 0, GlobalConstants.localRecipePath + ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo());
                FtpUtil.uploadFile(GlobalConstants.localRecipePath + ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo(), GlobalConstants.ftpPath + deviceCode + recipeName + "temp/", "DEVID.LST_V" + recipe.getVersionNo(), localftpip, ftpPort, ftpUser, ftpPwd);

                if (!"".equals(equipRecipeNames[0])) {
                    equipRecipePathtmp = sawRecipePath + "\\" + equipRecipeNames[0];
                }

                List<String> result = sawISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, sawISecsHost.deviceTypeCode);
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
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());

            }
        }
        String handleRecipeNameTmp = trimUOID(recipeName);
//        if (deviceType.contains("Z2")) {
//            handleRecipeNameTmp = equipRecipeName + "-" + recipeName;
//        }
        //导出，上传handle部分程序
        if (exportHandleRecipe(handleRecipeNameTmp)) {
            List<String> handleRecipeUploadresult = iSecsHost.executeCommand("ftp " + localftpip + " "
                    + ftpUser + " " + ftpPwd + " " + equipRecipePath + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                    + handleRecipeNameTmp + ".han\"");
            for (String uploadstr : handleRecipeUploadresult) {
                if ("done".equals(uploadstr)) {
                    //List<RecipePara> recipeParaList = new ArrayList<>();
                    try {
                        Map paraMap = null;// DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD");
                        if (paraMap != null && !paraMap.isEmpty()) {
                            // recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, sawISecsHost.deviceTypeCode);
                        } else {
//                            logger.error("HandleRecipe 解析recipe时出错,recipe文件不存在");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (uploadstr.contains("Not connected")) {
                    ocrUploadOk = false;
                }
            }
        }
        //上传vision部分程序                
        String command = "ftp " + localftpip + " "
                + ftpUser + " " + ftpPwd + " " + visionRecipePath + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput ";
        String commandAdd = handleRecipeNameTmp + ".INLET " + handleRecipeNameTmp + ".INLET.ldb " + handleRecipeNameTmp + ".INLET.roi ";
        visionISecsHost.executeCommand(command + commandAdd);
        commandAdd = handleRecipeNameTmp + ".MARK " + handleRecipeNameTmp + ".MARK.ldb " + handleRecipeNameTmp + ".MARK.roi ";
        visionISecsHost.executeCommand(command + commandAdd);
        if (getPackageType(handleRecipeNameTmp).contains("Q") || getPackageType(handleRecipeNameTmp).contains("D") || getPackageType(handleRecipeNameTmp).contains("L")) {
            commandAdd = handleRecipeNameTmp + ".QFN " + handleRecipeNameTmp + ".QFN.ldb " + handleRecipeNameTmp + ".QFN.roi ";
            visionISecsHost.executeCommand(command + commandAdd);
        }
        if (getPackageType(handleRecipeNameTmp).contains("B")) {
            commandAdd = handleRecipeNameTmp + ".BGA " + handleRecipeNameTmp + ".BGA.ldb " + handleRecipeNameTmp + ".BGA.roi ";
        }
        List<String> visionRecipeUploadresult = visionISecsHost.executeCommand(command + commandAdd);
        for (String uploadstr : visionRecipeUploadresult) {
            if ("done".equals(uploadstr)) {
                List<RecipePara> recipeParaList = new ArrayList<>();
                try {
                    Map paraMap = null;//DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD");
                    if (paraMap != null && !paraMap.isEmpty()) {
                        // recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, visionISecsHost.deviceTypeCode);
                    } else {
//                        logger.error("visionRecipe 解析recipe时出错,recipe文件不存在");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (uploadstr.contains("Not connected")) {
                ocrUploadOk = false;
            }
        }
        //创建配套的dbindx.inf文件
        createdbindexFile(recipeName);
        if (!ocrUploadOk) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
            resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
        }

        return resultMap;

    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String handleRecipeName = trimUOID(recipeName);
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            String versionNo = String.valueOf(recipe.getVersionNo());
            if (i == 0) {
                attach.setAttachName("DEV.LST_V" + versionNo);
            } else if (i == 1) {
                attach.setAttachName("DEVID.LST_V" + versionNo);
            } else if (i == 2) {
                attach.setAttachName(recipeName + ".ALU_V" + versionNo);
            } else if (i == 3) {
                attach.setAttachName(recipeName + ".CLN_V" + versionNo);
            } else if (i == 4) {
                attach.setAttachName(recipeName + ".DFD_V" + versionNo);
            } else if (i == 5) {
                attach.setAttachName(recipeName + ".han_V" + versionNo);
            } else if (i == 6) {
                attach.setAttachName("dbindex.inf_V" + versionNo);
            } else if (i == 7) {
                attach.setAttachName(recipeName + ".INLET_V" + versionNo);
            } else if (i == 8) {
                attach.setAttachName(recipeName + ".INLET.ldb_V" + versionNo);
            } else if (i == 9) {
                attach.setAttachName(recipeName + ".INLET.roi_V" + versionNo);
            } else if (i == 10) {
                attach.setAttachName(recipeName + ".MARK_V" + versionNo);
            } else if (i == 11) {
                attach.setAttachName(recipeName + ".MARK.ldb_V" + versionNo);
            } else if (i == 12) {
                attach.setAttachName(recipeName + ".MARK.roi_V" + versionNo);
            } else if (i == 13) {
                if (getPackageType(handleRecipeName).contains("Q") || getPackageType(handleRecipeName).contains("D") || getPackageType(handleRecipeName).contains("L")) {
                    attach.setAttachName(recipeName + ".QFN_V" + versionNo);
                }
                if (getPackageType(handleRecipeName).contains("B")) {
                    attach.setAttachName(recipeName + ".BGA_V" + versionNo);
                }
            } else if (i == 14) {
                if (getPackageType(handleRecipeName).contains("Q") || getPackageType(handleRecipeName).contains("D") || getPackageType(handleRecipeName).contains("L")) {
                    attach.setAttachName(recipeName + ".QFN.ldb_V" + versionNo);
                }
                if (getPackageType(handleRecipeName).contains("B")) {
                    attach.setAttachName(recipeName + ".BGA.ldb_V" + versionNo);
                }
            } else if (i == 15) {
                if (getPackageType(handleRecipeName).contains("Q") || getPackageType(handleRecipeName).contains("D") || getPackageType(handleRecipeName).contains("L")) {
                    attach.setAttachName(recipeName + ".QFN.roi_V" + versionNo);
                }
                if (getPackageType(handleRecipeName).contains("B")) {
                    attach.setAttachName(recipeName + ".BGA.roi_V" + versionNo);
                }
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
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Upload专用，不支持下载");
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
    public String deleteRecipe(String recipeName
    ) {
        getEquipRecipeList();
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                String equipRecipeNameTmp = recipeNameMappingMap.get(recipeName);
                if (equipRecipeNameTmp == null) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                }
                String[] equipRecipeNames = equipRecipeNameTmp.split(":");
                List<String> result = sawISecsHost.executeCommand("dos \"del /q " + sawRecipePath + "\\" + equipRecipeNames[0] + "\\*\"");
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
    public String selectRecipe(String recipeName
    ) {
        String select = "";
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = sawISecsHost.executeCommand("playback selrecipe.txt");
                for (String str : result) {
                    if ("done".equals(str)) {
                        ppExecName = recipeName;
                        select = "0";
                    }
                    if (str.contains("rror")) {
                        select = "选中失败";
                    }
                }
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                select = "Saw部分程序选中失败";
            }
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
//                List<String> curscreens = iSecsHost.executeCommand("curscreen");
//                if ("main".equals(curscreens.get(0))) {
//                    iSecsHost.executeCommand("playback gotodevicesetupscreen.txt");
//                } else {
//                    iSecsHost.executeCommand("playback gotoperviousscreen.txt");
//                }
//                iSecsHost.executeCommand("playback selgroup1.txt");
//                iSecsHost.executeCommand("playback seldevicetype1.txt");
//                iSecsHost.executeCommand("playback selvisiontype1.txt");
//                List<String> devicechange = iSecsHost.executeCommand("playback devicechange");
//                iSecsHost.executeCommand("dos \"dialog \"Device Change\" action Yes\"");

            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                select = select + "handle部分程序选中失败";
            }
            List<String> result = iSecsHost.executeCommand("curscreen");
            if (result != null && !result.isEmpty()) {
                if ("pause".equals(result.get(0))) {
                    handleRecipeName = iSecsHost.executeCommand("read recipename").get(0);
                } else {
                    if ("main".equals(result.get(0))) {
                        handleRecipeName = iSecsHost.executeCommand("read mainrecipename").get(0);
                    } else if ("work".equals(result.get(0))) {
                        handleRecipeName = iSecsHost.executeCommand("read recipename").get(0);
                    }
                }
            }
        }
        String[] handleRecipeNames = handleRecipeName.split(",");
        String handleG = handleRecipeNames[0].split("]")[1].trim();
        String handleD = handleRecipeNames[1].split("]")[1].trim();
        String handleV = handleRecipeNames[2].split("]")[1].trim();
        handleRecipeName = handleG + "-" + handleD + "-" + handleV;
        handleRecipeName = handleV;
        //选中vision程序
        Map<String, String> map = readdbindexFile(recipeName);
        String groupNameV = handleG;// map.get("GroupName");
        String deviceTypeNameV = handleD;// map.get("DeviceTypeName");
        String visiontypeNameV = handleV;// map.get("PKGNAME");
        String CAM1 = map.get("CAM1");
        String CAM2 = map.get("CAM2");
        String CAM3 = map.get("CAM3");
        String CAM4 = map.get("CAM4");
        List<String> curscreenList = visionISecsHost.executeCommand("curscreen");
        for (String string : curscreenList) {
            if ("setup".equalsIgnoreCase(string)) {
                visionISecsHost.executeCommand("playback gotorun.txt");
            }
        }
        try {

            visionISecsHost.executeCommand("playback gotodatabaseindex.txt");
            visionISecsHost.executeCommand("playback editgroupname.txt");
            visionISecsHost.executeCommand("write gname " + groupNameV);
            visionISecsHost.executeCommand("playback editgrpdevok.txt");
            visionISecsHost.executeCommand("playback editdevicetypename.txt");
            visionISecsHost.executeCommand("write dname " + deviceTypeNameV);
            visionISecsHost.executeCommand("playback editgrpdevok.txt");
            visionISecsHost.executeCommand("playback editvisiontypename.txt");
            Thread.sleep(300);
            visionISecsHost.executeCommand("write vname " + visiontypeNameV);
            Thread.sleep(300);
            visionISecsHost.executeCommand("playback editpkgok.txt");
        } catch (InterruptedException e) {
        }
        return "0";
    }

    private boolean needPageTurn(int pageSize, int currentSeat) {
        if (pageSize == currentSeat) {
            return true;
        } else {
            return false;
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
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                String[] equipRecipeNames = recipeNameMappingMap.get(ppExecName).split(":");
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String equipRecipeName = equipRecipeNames[1];
                String equipRecipePathtmp = equipRecipePath;
                if (!"".equals(equipRecipeNames[0])) {
                    equipRecipePathtmp = sawRecipePath + "\\" + equipRecipeNames[0];
                }
                List<String> result = sawISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + equipRecipeName + ".DFD\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + equipRecipeName + ".DFD");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                List<RecipePara> recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, sawISecsHost.deviceTypeCode);
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
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> curscreen = iSecsHost.executeCommand("curscreen");
                if ("run".equals(curscreen.get(0))) {
                    iSecsHost.executeCommand("playback gotosetup");
                }
                iSecsHost.executeCommand("playback gotoqfn");
                //TODO read QNF parameter+
                //List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                iSecsHost.executeCommand("playback gotomark");
                //TODO read Mark parameter
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
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {

            List<String> result = new ArrayList<>();
            try {
                result = sawISecsHost.executeCommand("dos \"dir " + sawRecipePath + " /ad/w\"");
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
                        List<String> singleDirRecipe = sawISecsHost.executeCommand("dos \"type " + sawRecipePath + "\\" + dir + "\\DEV.LST\"");
                        if (singleDirRecipe.size() > 0) {
                            for (String strTmp : singleDirRecipe) {
                                if (strTmp.contains(",")) {
                                    String[] recipeNameMappings = strTmp.split(",");
                                    for (int i = 0; i < recipeNameMappings.length; i++) {
                                        if (i == recipeNameMappings.length - 1) {
                                            break;
                                        }
                                        recipeNameMappingMap.put(recipeNameMappings[i] + "-" + recipeNameMappings[i + 1], dir + ":" + recipeNameMappings[i]);
                                        recipeNameList.add(recipeNameMappings[i] + "-" + recipeNameMappings[i + 1]);
                                        i = i + 1;

                                    }
                                }
                            }
                        }
                    }
                }
                List<String> singleDirRecipe1 = sawISecsHost.executeCommand("dos \"type " + sawRecipePath + "\\DEV.LST\"");
                if (singleDirRecipe1.size() > 0) {
                    for (String strTmp : singleDirRecipe1) {
                        if (strTmp.contains(",")) {
                            String[] recipeNameMappings = strTmp.split(",");
                            for (int i = 0; i < recipeNameMappings.length; i++) {
                                if (i == recipeNameMappings.length - 1) {
                                    break;
                                }
                                recipeNameMappingMap.put(recipeNameMappings[i] + "-" + recipeNameMappings[i + 1], ":" + recipeNameMappings[i]);
                                recipeNameList.add(recipeNameMappings[i] + "-" + recipeNameMappings[i + 1]);
                                i = i + 1;

                            }
                        }
                    }
                }
            }
        }
        //获取handler部分的recipelist
        List<String> handlerRecipeList = handleRecipeHost.executeCommand("Getlist");
        handlerRecipeList = getHandleRecipeList(handlerRecipeList);
        recipeNameList = conformRecipeName(recipeNameList, handlerRecipeList);
        eppd.put("eppd", recipeNameList);
        deleteTempFile(ppExecName);
        return eppd;
    }

    @Override
    public Object clone() {
        Hanmi20000DAHostUpload newEquip = new Hanmi20000DAHostUpload(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = sawISecsHost.executeCommand("readrectcolor 730 50 750 60 ");
                for (String colorstr : result) {
                    if ("0xc0c0c0".equals(colorstr)) {
                        alarmStrings.add("");
                    }
                    if ("0xff0000".equals(colorstr)) {
                        logger.info("The equip state changged to alarm...");
                        List<String> alidresult = sawISecsHost.executeCommand("read alarmid");
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //    List<String> result = iSecsHost.executeCommand("read alarm");
                logger.info("The equip state changged to alarm...");
                List<String> alidresult = iSecsHost.executeCommand("read alarm");
                if (alidresult.size() > 1) {
                    for (String string : alidresult) {
                        if (string.contains("]")) {
                            alarmStrings.add(string.split("]")[0].replaceAll("\\[", ""));
                            logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
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
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.sawISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        equipStatus = "Pause";
                    } else if ("work".equals(result.get(0))) {
                        equipStatus = "Run";
                    } else if ("main".equals(result.get(0)) || "any".equals(result.get(0))) {
                        equipStatus = "Idle";
                    } else if ("ready".equalsIgnoreCase(result.get(0))) {
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
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = sawISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        equipStatus = "Pause";
                    } else if ("main".equals(result.get(0)) || "work".equals(result.get(0))) {
                        List<String> result1 = sawISecsHost.executeCommand("goto dpqb");
                        for (String str : result1) {
                            if ("done".equals(str)) {
                                List<String> result2 = sawISecsHost.executeCommand("readm dppzz1 dppzz2");
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
        bladeGroupCode = ppExecNames[ppExecNames.length - 1];
        return bladeGroupCode;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String hanmiRecipeName = trimUOID(recipeName);
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".han", remoteRcpPath, recipeName + ".han_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".INLET", remoteRcpPath, recipeName + ".INLET_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".INLET.ldb", remoteRcpPath, recipeName + ".INLET.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".INLET.roi", remoteRcpPath, recipeName + ".INLET.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".MARK", remoteRcpPath, recipeName + ".MARK_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".MARK.ldb", remoteRcpPath, recipeName + ".MARK.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".MARK.roi", remoteRcpPath, recipeName + ".MARK.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/dbindex.inf", remoteRcpPath, "dbindex.inf_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件上传FTP失败");
            return false;
        }
        if (getPackageType(hanmiRecipeName).equals("Q") || getPackageType(hanmiRecipeName).equals("D") || getPackageType(hanmiRecipeName).equals("L")) {
            if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".QFN", remoteRcpPath, recipeName + ".QFN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                    || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".QFN.ldb", remoteRcpPath, recipeName + ".QFN.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                    || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".QFN.roi", remoteRcpPath, recipeName + ".QFN.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件上传FTP失败");
                return false;
            }
        }
        if (getPackageType(hanmiRecipeName).equals("B")) {
            if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".BGA", remoteRcpPath, recipeName + ".BGA_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                    || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".BGA.ldb", remoteRcpPath, recipeName + ".BGA.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                    || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + hanmiRecipeName + ".BGA.roi", remoteRcpPath, recipeName + ".BGA.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件上传FTP失败");
                return false;
            }
        }

        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    private void cleanRootDir() {
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {

                List<String> result = iSecsHost.executeCommand("dos \"del /q " + sawRecipePath + "\\*\"");
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
        return true;
    }

    @Override
    public boolean isConnect() {
        if (iSecsHost != null && sawISecsHost != null && visionISecsHost != null) {
            return iSecsHost.isConnect && sawISecsHost.isConnect && visionISecsHost.isConnect;
        } else {
            return false;
        }
    }

    /**
     * 获取到的json转化为string list
     *
     * @param recipeList
     * @return
     */
    private List<String> getHandleRecipeList(List<String> recipeList) {
        List<String> handleRecipeList = new ArrayList<>();
        for (String str : recipeList) {
            if (str.contains("}")) {
                String[] strs = str.replaceAll("]}", "").replaceAll("\\{", "").split("}");
                for (String str1 : strs) {
                    String[] strTmps = str1.replaceAll("\"", "").replaceAll("\\t\\[", "").split(",");
                    Map<String, String> map = new HashMap();
                    for (String strTmp : strTmps) {
                        strTmp = strTmp.replaceAll("\\[", "");
                        if ("".equals(strTmp.trim())) {
                            continue;
                        }
                        strTmp = strTmp.replaceAll("recipes:", "");
                        String[] strmaps = strTmp.split(":");
                        map.put(strmaps[0], strmaps[1]);
                    }
                    handleRecipeMap.put(map.get("recipename"), map.get("groupno") + " " + map.get("deviceno") + " " + map.get("visionno"));
                    logger.debug("handlerecipename:" + map.get("recipename"));
                    handleRecipeList.add(map.get("recipename"));
                }
            }
        }
        return handleRecipeList;
    }

    private List<String> conformRecipeName(List<String> discoRecipeNameList, List<String> handleRecipeNameList) {
        List<String> recipeNameOKList = new ArrayList<>();
        for (String discoRecipeName : discoRecipeNameList) {
            for (String handleRecipeNameTmp : handleRecipeNameList) {
                String[] discoRecipeNameTmps = discoRecipeName.split("-");
                if (discoRecipeNameTmps.length > 1) {
                    if (handleRecipeNameTmp.contains(discoRecipeName.replaceAll(discoRecipeNameTmps[0] + "-", ""))) {
                        recipeNameOKList.add(discoRecipeName);
                    }
                }
            }
        }
        return recipeNameOKList;
    }

    private boolean exportHandleRecipe(String recipeName) {
        List<String> exportResultList = handleRecipeHost.executeCommand("Export " + getMapHandleRecipeName(recipeName) + " " + equipRecipePath + "\\" + recipeName + ".han");
        for (String string : exportResultList) {
            if (string.contains("done")) {
                return true;
            }
        }
        return false;
    }

    private String getMapHandleRecipeName(String recipeName) {
        String mapHandleRecipeName = "";
        for (Map.Entry<String, String> entry : handleRecipeMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.contains(recipeName)) {
                mapHandleRecipeName = handleRecipeMap.get(key);
            }
        }
        return mapHandleRecipeName;
//        return recipeName;
    }

    private boolean importHandleRecipe(String recipeName) {
        List<String> importResultList = handleRecipeHost.executeCommand("Import " + equipRecipePath + "\\" + recipeName + ".han");
        for (String string : importResultList) {
            if (string.contains("done")) {
                return true;
            }
            if (string.contains("rror")) {
                logger.error(string);
            }
        }
        return false;
    }

    private void createdbindexFile(String recipeName) {
        synchronized (handleRecipeHost.iSecsConnection.getSocketClient()) {
//            List<String> handlerRecipeList = handleRecipeHost.executeCommand("Getlist");
//            handlerRecipeList = getHandleRecipeList(handlerRecipeList);
//            String[] mapHandleRecipeNames = new String[1];
//            for (String string : handlerRecipeList) {
//                if (string.contains(trimUOID(recipeName))) {
//                    mapHandleRecipeNames = string.split("--");
//                }
//            }
            String recipeNameTmp = recipeName;
            if (deviceType.contains("Z2")) {
                recipeNameTmp = trimUOID(recipeName);
            }
            List<String> dbindexInfo = new ArrayList<>();
            dbindexInfo.add("[TOPGROUPNAME]");
            dbindexInfo.add("001=" + recipeNameTmp);
            dbindexInfo.add("[GROUPNAME]");
            dbindexInfo.add("001=" + recipeNameTmp);
            dbindexInfo.add("[001_001]");
            dbindexInfo.add("PKGNAME=" + recipeNameTmp);
            dbindexInfo.add("CAM1=" + recipeNameTmp);
            dbindexInfo.add("CAMACTIVE1=1");
            if (getPackageType(recipeNameTmp).contains("Q") || getPackageType(recipeNameTmp).contains("D") || getPackageType(recipeNameTmp).contains("L")) {
                dbindexInfo.add("CAM2=" + recipeNameTmp);
                dbindexInfo.add("CAMACTIVE2=1");
                dbindexInfo.add("CAM3=");
            }
            if (getPackageType(recipeNameTmp).contains("B")) {
                dbindexInfo.add("CAM2=");
                dbindexInfo.add("CAMACTIVE2=1");
                dbindexInfo.add("CAM3=" + recipeNameTmp);
            }
            dbindexInfo.add("CAMACTIVE3=1");
            dbindexInfo.add("CAM4=" + recipeNameTmp);
            dbindexInfo.add("CAMACTIVE4=1");

            FileUtil.writeRecipeFile(dbindexInfo, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/dbindex.inf");
        }
    }

    private Map<String, String> readdbindexFile(String recipeName) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            File cfgfile = new File(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/dbindex.inf");
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
            while ((cfgline = br.readLine()) != null) {
                String[] cfg;
                if (cfgline.contains("=")) {
                    cfg = cfgline.split("=");
                    if (!cfg[0].contains("001")) {
                        key = cfg[0];
                    }
                    if (cfg.length > 1) {
                        value = cfg[1];
                    } else {
                        value = "";
                    }
                }
                if (cfgline.equals("[TOPGROUPNAME]")) {
                    key = "GroupName";
                    continue;
                }
                if (cfgline.equals("[GROUPNAME]")) {
                    key = "DeviceTypeName";
                    continue;
                }

                map.put(key, value);
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private String trimUOID(String discoRecipeName) {
        //这里传过来的recipeName是disco的 最后一个"-"之后是刀片编号，要去除
        String[] recipeNames = discoRecipeName.split("-");
        String discoRecipeNameTmp = discoRecipeName.replaceAll(recipeNames[0] + "-", "");
        return discoRecipeNameTmp;
    }

    private String getPackageType(String recipeName) {
        String packageType = "";
        packageType = recipeName.substring(0, 1);
        if (packageType.equals("L") || packageType.equals("Q") || packageType.equals("B") || packageType.equals("D")) {
            return packageType;
        } else {
            return "";
        }
    }
}
