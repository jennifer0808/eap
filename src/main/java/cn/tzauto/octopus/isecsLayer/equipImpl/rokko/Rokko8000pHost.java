/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.rokko;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
public class Rokko8000pHost extends EquipModel {

    private static Logger logger = Logger.getLogger(Rokko8000pHost.class.getName());
    private Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();
    private ISecsHost sawISecsHost;
    private ISecsHost visionISecsHost;
    private String sawRecipePath;
    private String visionRecipePath;
    private ISecsHost handleRecipeHost;
    private final Map<String, String> handleRecipeMap = new HashMap();
    String handleRecipeName = "";

    public Rokko8000pHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
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
        if (null == sawinfo || sawinfo.getDeviceIp() == null) {
            logger.error(deviceCode + " 未配置saw子机信息或者信息不完整");
            return;
        }
        Map<String, DeviceType> deviceTypeDic = deviceService.getDeviceTypeMap();
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
        String handleRecipeName = "";
        String visionRecipeName = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    handleRecipeName = iSecsHost.executeCommand("read recipe1").get(0);
                    visionRecipeName = iSecsHost.executeCommand("read recipe2").get(0);
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        if (deviceType.contains("Z2") && !ppExecName.equals("--")) {
            if (!"".equals(ppExecName) && !trimUOID(ppExecName).equals(handleRecipeName)) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Saw程序与Handle程序不符.Saw:" + ppExecName + " Handle:" + handleRecipeName);
            }
        } else {
            if (!"".equals(ppExecName) && !ppExecName.equals(handleRecipeName)) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Saw程序与Handle程序不符.Saw:" + ppExecName + " Handle:" + handleRecipeName);
            }
        }
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = visionISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    visionRecipeName = visionISecsHost.executeCommand("read recipe1").get(0);
//                    visionRecipeName = visionISecsHost.executeCommand("read recipe2").get(0);
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        if (!handleRecipeName.equals(visionRecipeName)) {
            logger.info("Handle程序与Vision程序不符.Handle:" + handleRecipeName + " Vision:" + visionRecipeName);
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
                                if (repeatStop()) {
                                    return "0";
                                }
                            }
                        }
                        stopResult = "Saw锁机失败,当前状态无法执行锁机";
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
                        stopResult = "Handle锁机失败,当前状态无法执行锁机";
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
        List<RecipePara> recipeParaList = new ArrayList<>();
        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        boolean ocrUploadOk = true;
        try {
            synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
                String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");

                String equipRecipeName = equipRecipeNames[1];
                // ftp ip user pwd lpath rpath "mput 001.ALU 001.CLN 001.DFD" 

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
                String equipRecipePathtmp = sawRecipePath;
                if (!"".equals(equipRecipeNames[0])) {
                    equipRecipePathtmp = sawRecipePath + "\\" + equipRecipeNames[0];
                }

                List<String> result = sawISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + equipRecipeName + ".DFD");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, sawISecsHost.deviceTypeCode);
                            } else {
                                logger.error("解析recipe时出错,recipe文件不存在");
                            }
                        } catch (Exception ex) {
                            logger.error("解析Disco recipe时出错" + ex);
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
            }
            String handleRecipeNameTmp = recipeName;
            if (deviceType.contains("Z2")) {
                handleRecipeNameTmp = trimUOID(recipeName);
            }
            //导出，上传handle部分程序
            if (exportHandleRecipe(handleRecipeNameTmp)) {
                List<String> handleRecipeUploadresult = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + handleRecipeNameTmp + ".han\"");
                for (String uploadstr : handleRecipeUploadresult) {
                    if (uploadstr.contains("Not connected")) {
                        ocrUploadOk = false;
                    }
                }
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传失败,上传Recipe:" + recipeName + " 时,导出handle部分失败.");
                return null;
            }
            String visionRecipeTemp = handleRecipeNameTmp + "_" + handleRecipeNameTmp;
            //上传vision部分程序                
            String command = "ftp " + localftpip + " "
                    + ftpUser + " " + ftpPwd + " " + visionRecipePathAdd(visionRecipeTemp) + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput ";
            String commandAdd = visionRecipeTemp + ".MK1para " + visionRecipeTemp + ".MK1pref \"";
            visionISecsHost.executeCommand(command + commandAdd);
            commandAdd = visionRecipeTemp + ".pedg " + visionRecipeTemp + ".MK1timg \"";
            visionISecsHost.executeCommand(command + commandAdd);
            commandAdd = visionRecipeTemp + ".BQ2para " + visionRecipeTemp + ".BQ2pref \"";
            visionISecsHost.executeCommand(command + commandAdd);
            commandAdd = visionRecipeTemp + ".ini " + visionRecipeTemp + ".IN0para \"";
            visionISecsHost.executeCommand(command + commandAdd);
            commandAdd = "DeviceName.ini\"";
            visionISecsHost.executeCommand(command + commandAdd);
            if ("L".equals(getPackageType(visionRecipeTemp)) || "Q".equals(getPackageType(visionRecipeTemp))) {
                commandAdd = visionRecipeTemp + ".PtrnSip_0 " + visionRecipeTemp + ".PtrnSip_1 \"";
                visionISecsHost.executeCommand(command + commandAdd);
                commandAdd = visionRecipeTemp + ".DibSip \"";
            }
            if ("B".equals(getPackageType(visionRecipeTemp))) {
                commandAdd = visionRecipeTemp + ".dat \"";
            }

            List<String> visionRecipeUploadresult = visionISecsHost.executeCommand(command + commandAdd);

            for (String uploadstr : visionRecipeUploadresult) {
                if ("done".equals(uploadstr)) {
                    List<RecipePara> recipeParaListTmp = new ArrayList<>();
                    try {
                        Map paraMap = null;//RokkoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + handleRecipeNameTmp + ".DFD");
                        if (paraMap != null && !paraMap.isEmpty()) {
                            recipeParaListTmp = null;// RokkoRecipeUtil.transferFromDB(paraMap, visionISecsHost.deviceTypeCode);
                        } else {
//                            logger.error("解析recipe时出错,recipe文件不存在");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    resultMap.put("recipeParaList", recipeParaList);
                }
                if (uploadstr.contains("Not connected")) {
                    ocrUploadOk = false;
                }
            }
            //创建配套的DeviceName.ini文件
//            createDeviceNameFile(recipeName);
            if (!ocrUploadOk) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
            }
        } catch (Exception e) {
            logger.error("Get equip status error:" + e.getMessage());

        }

        return resultMap;

    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        String handleRecipeName = recipeName;
        if (deviceType.contains("Z2")) {
            handleRecipeName = recipeName.replace(recipeName.split("-")[0] + "-", "");
        }
        String VisionRecipeName = handleRecipeName + "_" + handleRecipeName;
        List<Attach> attachs = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            if (i == 0) {
                attach.setAttachName("DEV.LST_V" + recipe.getVersionNo());
            } else if (i == 1) {
                attach.setAttachName("DEVID.LST_V" + recipe.getVersionNo());
            } else if (i == 2) {
                attach.setAttachName(recipeName + ".ALU_V" + recipe.getVersionNo());
            } else if (i == 3) {
                attach.setAttachName(recipeName + ".CLN_V" + recipe.getVersionNo());
            } else if (i == 4) {
                attach.setAttachName(recipeName + ".DFD_V" + recipe.getVersionNo());
            } else if (i == 5) {
                attach.setAttachName(recipeName + ".MK1para_V" + recipe.getVersionNo());
            } else if (i == 6) {
                attach.setAttachName(recipeName + ".MK1pref_V" + recipe.getVersionNo());
            } else if (i == 7) {
                attach.setAttachName(recipeName + ".MK1timg_V" + recipe.getVersionNo());
            } else if (i == 8) {
                attach.setAttachName(recipeName + ".pedg_V" + recipe.getVersionNo());
            } else if (i == 9) {
                attach.setAttachName(recipeName + ".BQ2para_V" + recipe.getVersionNo());
            } else if (i == 10) {
                attach.setAttachName(recipeName + ".BQ2pref_V" + recipe.getVersionNo());
            } else if (i == 11) {
                attach.setAttachName("DeviceName.ini_V" + recipe.getVersionNo());
            } else if (i == 12) {
                attach.setAttachName(recipeName + ".IN0para_V" + recipe.getVersionNo());
            } else if (i == 13) {
                attach.setAttachName(recipeName + ".ini_V" + recipe.getVersionNo());
            } else if (i == 14) {
                attach.setAttachName(recipeName + ".han_V" + recipe.getVersionNo());
            }
            if ("B".equals(getPackageType(handleRecipeName))) {
                if (i == 15) {
                    attach.setAttachName(recipeName + ".dat_V" + recipe.getVersionNo());
                }
                if (i == 16) {
                    attach.setAttachName(recipeName + ".dat_V" + recipe.getVersionNo());
                }
            }
            if ("L".equals(getPackageType(handleRecipeName)) || "Q".equals(getPackageType(handleRecipeName))) {
                if (i == 15) {
                    attach.setAttachName(recipeName + ".PtrnSip_0_V" + recipe.getVersionNo());
                }
                if (i == 16) {
                    attach.setAttachName(recipeName + ".PtrnSip_1_V" + recipe.getVersionNo());
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
        cleanRootDir();
        //ftp ip user pwd lpath rpath \"mget 001.ALU 001.CLN 001.DFD DEV.LST DEVID.LST\"       
//        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            String localftpip = GlobalConstants.clientInfo.getClientIp();
            String ftpip = GlobalConstants.ftpIP;
            String ftpUser = GlobalConstants.ftpUser;
            String ftpPwd = GlobalConstants.ftpPwd;
            String ftpPort = GlobalConstants.ftpPort;

            String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
            String handleRecipeNameTmp = recipe.getRecipeName();
            if (deviceType.contains("Z2")) {
                handleRecipeNameTmp = trimUOID(recipe.getRecipeName());
            }
            String visionRecipeName = handleRecipeNameTmp + "_" + handleRecipeNameTmp;
            String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);
            String equipRecipeName = "001";//equipRecipeNames[0];
            String bodyTmp = "001," + recipe.getRecipeName() + ",";
            TransferUtil.setPPBody(bodyTmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST");
            TransferUtil.setPPBody(bodyTmp, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST");
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            String pkgType = getPackageType(handleRecipeNameTmp);
            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPathTmp + "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".ALU", ftpPathTmp + recipe.getRecipeName() + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".CLN", ftpPathTmp + recipe.getRecipeName() + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", ftpPathTmp + recipe.getRecipeName() + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                // FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEVID.LST", ftpPathTmp + "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".MK1para", ftpPathTmp + recipe.getRecipeName() + ".MK1para_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".MK1pref", ftpPathTmp + recipe.getRecipeName() + ".MK1pref_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".MK1timg", ftpPathTmp + recipe.getRecipeName() + ".MK1timg_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".pedg", ftpPathTmp + recipe.getRecipeName() + ".pedg_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".BQ2para", ftpPathTmp + recipe.getRecipeName() + ".BQ2para_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".BQ2pref", ftpPathTmp + recipe.getRecipeName() + ".BQ2pref_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".ini", ftpPathTmp + recipe.getRecipeName() + ".ini_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".IN0para", ftpPathTmp + recipe.getRecipeName() + ".IN0para_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DeviceName.ini", ftpPathTmp + "DeviceName.ini_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + handleRecipeNameTmp + ".han", ftpPathTmp + recipe.getRecipeName() + ".han_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

                if ("B".equals(pkgType)) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".dat", ftpPathTmp + recipe.getRecipeName() + ".dat_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                }
                if ("L".equals(pkgType) || "Q".equals(pkgType)) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".DibSip", ftpPathTmp + recipe.getRecipeName() + ".DibSip_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".PtrnSip_0", ftpPathTmp + recipe.getRecipeName() + ".PtrnSip_0_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".PtrnSip_1", ftpPathTmp + recipe.getRecipeName() + ".PtrnSip_1_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                }
//                if (deviceType.contains("Z2")) {
//                    editRecipeFileAdduoid(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", handleRecipeNameTmp, recipe.getRecipeName());
//                }
            } else {
                //  FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPathTmp + "DEV.LST", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".ALU", ftpPathTmp + recipe.getRecipeName() + ".ALU", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".CLN", ftpPathTmp + recipe.getRecipeName() + ".CLN", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", ftpPathTmp + recipe.getRecipeName() + ".DFD", ftpip, ftpPort, ftpUser, ftpPwd);

                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".MK1para", ftpPathTmp + recipe.getRecipeName() + ".MK1para", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".MK1pref", ftpPathTmp + recipe.getRecipeName() + ".MK1pref", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".MK1timg", ftpPathTmp + recipe.getRecipeName() + ".MK1timg", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".pedg", ftpPathTmp + recipe.getRecipeName() + ".pedg", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".BQ2para", ftpPathTmp + recipe.getRecipeName() + ".BQ2para", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".BQ2pref", ftpPathTmp + recipe.getRecipeName() + ".BQ2pref", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".ini", ftpPathTmp + recipe.getRecipeName() + ".ini", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".IN0para", ftpPathTmp + recipe.getRecipeName() + ".IN0para", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + "DeviceName.ini", ftpPathTmp + "DeviceName.ini", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + handleRecipeNameTmp + ".han", ftpPathTmp + recipe.getRecipeName() + ".han", ftpip, ftpPort, ftpUser, ftpPwd);

                if ("B".equals(pkgType)) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".dat", ftpPathTmp + recipe.getRecipeName() + ".dat", ftpip, ftpPort, ftpUser, ftpPwd);
                }
                if ("L".equals(pkgType) || "Q".equals(pkgType)) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".DibSip", ftpPathTmp + recipe.getRecipeName() + ".DibSip", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".PtrnSip_0", ftpPathTmp + recipe.getRecipeName() + ".PtrnSip_0", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + visionRecipeName + ".PtrnSip_1", ftpPathTmp + recipe.getRecipeName() + ".PtrnSip_1", ftpip, ftpPort, ftpUser, ftpPwd);
                }
//                if (deviceType.contains("Z2")) {
//                    editRecipeFileAdduoid(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + equipRecipeName + ".DFD", handleRecipeNameTmp, recipe.getRecipeName());
//                }
            }
            synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
                List<String> result = sawISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + sawRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD DEV.LST DEVID.LST\"");
                for (String str : result) {
                    if ("done".equals(str)) {

                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            }    //下载handle程序
            String handleRecipeNameTemp = trimUOID(recipe.getRecipeName());
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> handleDownloadresult = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + handleRecipeNameTemp + ".han\"");
                for (String str : handleDownloadresult) {
                    if (str.contains("done")) {
                        continue;
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            }
            //导入handle程序
            if (!importHandleRecipe(handleRecipeNameTemp)) {
                return "Handle程序导入失败.Download recipe " + recipe.getRecipeName() + " failed";
            }
            //下载vision程序
            String visionRecipeNameTemp = handleRecipeNameTemp + "_" + handleRecipeNameTemp;
            String command = "ftp " + localftpip + " "
                    + ftpUser + " " + ftpPwd + " " + visionRecipePathAdd(visionRecipeNameTemp) + "  " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget ";

            synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
                String commandAdd = visionRecipeNameTemp + ".MK1para " + visionRecipeNameTemp + ".MK1pref \"";
                visionISecsHost.executeCommand(command + commandAdd);
                commandAdd = visionRecipeNameTemp + ".pedg " + visionRecipeNameTemp + ".MK1timg \"";
                visionISecsHost.executeCommand(command + commandAdd);
                commandAdd = visionRecipeNameTemp + ".BQ2para " + visionRecipeNameTemp + ".BQ2pref \"";
                visionISecsHost.executeCommand(command + commandAdd);
                commandAdd = visionRecipeNameTemp + ".ini " + visionRecipeNameTemp + ".IN0para \"";
                visionISecsHost.executeCommand(command + commandAdd);

                if ("L".equals(getPackageType(visionRecipeNameTemp)) || "Q".equals(getPackageType(visionRecipeNameTemp))) {
                    commandAdd = visionRecipeNameTemp + ".PtrnSip_0 " + visionRecipeNameTemp + ".PtrnSip_1 \"";
                    visionISecsHost.executeCommand(command + commandAdd);
                    commandAdd = " DeviceName.ini " + visionRecipeNameTemp + ".DibSip \"";
                }
                if ("B".equals(getPackageType(visionRecipeNameTemp))) {
                    commandAdd = " DeviceName.ini " + visionRecipeNameTemp + ".dat \"";
                }
                List<String> visionDownloadresult = visionISecsHost.executeCommand(command + commandAdd);
                for (String str : visionDownloadresult) {
                    if ("done".equals(str)) {
                        return "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
            return "下载失败,出现异常:" + e.getMessage();
        } finally {
            sqlSession.close();
//            this.deleteTempFile(recipe.getRecipeName());
        }
//        }
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
    public String selectRecipe(String recipeName) {
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
                List<String> curscreens = iSecsHost.executeCommand("curscreen");
                if ("work".equals(curscreens.get(0))) {
                    iSecsHost.executeCommand("playback gotomain.txt");
                    Thread.sleep(100);
                    iSecsHost.executeCommand("playback gotomachine.txt");
                    Thread.sleep(100);
                    iSecsHost.executeCommand("playback gotodevice.txt");
//                    iSecsHost.executeCommand("playback password.txt");
                    iSecsHost.executeCommand("replay password.exe");
                    Thread.sleep(500);
                    iSecsHost.executeCommand("curscreen");
                    for (int k = 0; k < 10; k++) {
                        List<String> numberList = iSecsHost.executeCommand("read number");
                        if (numberList.contains("1")) {
                            break;
                        } else {
                            iSecsHost.executeCommand("playback devicelast.txt");
                        }
                    }
                    String handleRecipeTemp = recipeName;
                    if (deviceType.contains("Z2")) {
                        handleRecipeTemp = trimUOID(recipeName);
                    }
                    boolean selHandleOk = false;
                    for (int i = 0; i < 10; i++) {
                        for (int j = 1; j < 6; j++) {
                            List<String> devices = iSecsHost.executeCommand("read device" + j);
                            if (devices.contains(handleRecipeTemp)) {
                                iSecsHost.executeCommand("playback seldevicetype" + j + ".txt");
                                iSecsHost.executeCommand("playback selvisiontype1.txt");
                                iSecsHost.executeCommand("playback devicechange.txt");
                                iSecsHost.executeCommand("replay password.exe");
                                Thread.sleep(100);
                                iSecsHost.executeCommand("playback changeconfirm.txt");
                                selHandleOk = true;
                                break;
                            }
                        }
                        if (selHandleOk) {
                            break;
                        }
                        iSecsHost.executeCommand("playback devicenext.txt");
                    }

                }
                //return "handle部分程序选中失败";
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
            }
        }
        //执行vision的处理
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            List<String> curscreens = visionISecsHost.executeCommand("curscreen");
            for (String curscreen : curscreens) {
                if ("stop".equals(curscreen)) {
                    visionISecsHost.executeCommand("playback stop.txt");
                }
            }
            List<String> exitvisionResult = visionISecsHost.executeCommand("playback exitvision.txt");
            for (String string : exitvisionResult) {
                if ("done".equals(string)) {
                    String pkgType = getPackageType(trimUOID(recipeName));
                    if (deviceType.contains("Z2")) {
                        pkgType = getPackageType(trimUOID(recipeName));
                    }
                    runVisionSoft(pkgType);
                }
            }
        }
        return "0";
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
        getEquipRecipeList();
        Map map = new HashMap();
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                String[] equipRecipeNames = recipeNameMappingMap.get(ppExecName).split(":");
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String equipRecipeName = equipRecipeNames[1];
                String equipRecipePathtmp = sawRecipePath;
                if (!"".equals(equipRecipeNames[0])) {
                    equipRecipePathtmp = sawRecipePath + "\\" + equipRecipeNames[0];
                }
                String body = equipRecipeName + "," + ppExecName + ",";
                TransferUtil.setPPBody(body, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");

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
                                        recipeNameMappingMap.put(recipeNameMappings[i + 1], dir + ":" + recipeNameMappings[i]);
                                        recipeNameList.add(recipeNameMappings[i + 1]);
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
                                recipeNameMappingMap.put(recipeNameMappings[i + 1], ":" + recipeNameMappings[i]);
                                recipeNameList.add(recipeNameMappings[i + 1]);
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
        return eppd;
    }

    @Override
    public Object clone() {
        Rokko8000pHost newEquip = new Rokko8000pHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
                List<String> alidresult = sawISecsHost.executeCommand("read alarmid");
                if (alidresult.size() > 1) {
                    alarmStrings.add(alidresult.get(0));
                    logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
                } else {
                    alarmStrings.add("");
                }
//                    }
//                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        //添加了一个过滤，异常时可能会将
        if (alarmStrings.size() > 0) {
            alarmStrings.remove("0xff0000");
        }
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                //    List<String> result = iSecsHost.executeCommand("read alarm");
//                logger.info("The equip state changged to alarm...");
//                List<String> alidresult = iSecsHost.executeCommand("read alarm");
//                if (alidresult.size() > 1) {
//                    for (String string : alidresult) {
//                        if (string.contains("]")) {
//                            alarmStrings.add(string.split("]")[0].replaceAll("\\[", ""));
//                            logger.info("Get alarm ALID=[" + alarmStrings.get(0) + "]");
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Get EquipAlarm error:" + e.getMessage());
//            }
//        }
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
        synchronized (sawISecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.sawISecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if (result.get(0).contains("pause")) {
                        equipStatus = "Pause";
                    } else if (result.get(0).contains("work") || result.get(0).contains("run")) {
                        equipStatus = "Run";
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
        bladeGroupCode = ppExecNames[ppExecNames.length];
        return bladeGroupCode;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String handlerRecipeName = recipeName;
        if (deviceType.contains("Z2")) {
            handlerRecipeName = trimUOID(recipeName);
        }// recipeName.replace(recipeName.split("-")[0] + "-", "");
        String visionRecipeName = handlerRecipeName + "_" + handlerRecipeName;
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
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".MK1para", remoteRcpPath, recipeName + ".MK1para_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".MK1pref", remoteRcpPath, recipeName + ".MK1pref_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".MK1timg", remoteRcpPath, recipeName + ".MK1timg_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".pedg", remoteRcpPath, recipeName + ".pedg_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".BQ2para", remoteRcpPath, recipeName + ".BQ2para_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".BQ2pref", remoteRcpPath, recipeName + ".BQ2pref_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + "DeviceName.ini", remoteRcpPath, "DeviceName.ini_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".ini", remoteRcpPath, recipeName + ".ini_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".IN0para", remoteRcpPath, recipeName + ".IN0para_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)
                || !FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + handlerRecipeName + ".han", remoteRcpPath, recipeName + ".han_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件上传FTP失败");
            return false;
        }
        String pkgType = getPackageType(recipeName.replace(recipeName.split("-")[0] + "-", ""));
        if ("B".equals(pkgType)) {
            FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".dat", remoteRcpPath, recipeName + ".dat_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        }
        if ("L".equals(pkgType) || "Q".equals(pkgType)) {
            FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".DibSip", remoteRcpPath, recipeName + ".DibSip_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".PtrnSip_0", remoteRcpPath, recipeName + ".PtrnSip_0_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + visionRecipeName + ".PtrnSip_1", remoteRcpPath, recipeName + ".PtrnSip_1_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

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
                    handleRecipeMap.put(map.get("recipename"), map.get("DEVICE_INDEX") + " " + map.get("VISION_INDEX"));
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
                String[] handleRecipeNameTmps = handleRecipeNameTmp.split("--");
                if (handleRecipeNameTmps.length > 1) {
                    if (discoRecipeName.contains(handleRecipeNameTmps[handleRecipeNameTmps.length - 1])) {
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
            //这里传过来的recipeName是disco的 最后一个"-"之后是刀片编号，要去除
            if (key.contains(recipeName)) {
                mapHandleRecipeName = handleRecipeMap.get(key);
            }
        }
        return mapHandleRecipeName;
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

    private String trimUOID(String discoRecipeName) {
        //这里传过来的recipeName是disco的 最后一个"-"之后是刀片编号，要去除
        String[] recipeNames = discoRecipeName.split("-");
        String discoRecipeNameTmp = discoRecipeName.replaceAll(recipeNames[0] + "-", "");
        return discoRecipeNameTmp;
    }

    private void createDeviceNameFile(String recipeName) {
        String mapHandleRecipeName = recipeName;
        if (deviceType.contains("Z2")) {
            mapHandleRecipeName = trimUOID(recipeName);
        }
        String groupName = mapHandleRecipeName;

        String visionName = mapHandleRecipeName;
        List<String> dbindexInfo = new ArrayList<>();
        dbindexInfo.add("[PACKAGE_TYPE]");
        if (visionName.contains("QFN")) {
            dbindexInfo.add("PackageType=1");
        }
        if (visionName.contains("BGA")) {
            dbindexInfo.add("PackageType=0");
        }
        dbindexInfo.add("//0 : BGA");
        dbindexInfo.add("//1 : QFN");
        dbindexInfo.add("BallQfnCam1 = 1");
        dbindexInfo.add("//0 : 2 Camera");
        dbindexInfo.add("//1 : 1 Camera");
        dbindexInfo.add("NumOfPicker = 10");
        dbindexInfo.add("");
        dbindexInfo.add("[MARKING_TYPE]");
        dbindexInfo.add("FixedMarkUser=111111");
        dbindexInfo.add("");
        dbindexInfo.add("[BALL_XMKOPTION]");
        dbindexInfo.add("UTACxmark = 1");
        dbindexInfo.add("");
        dbindexInfo.add("[MARKING_LIGHTPORT]");
        dbindexInfo.add("SquareSide = 2");
        dbindexInfo.add("HalfMirror = 3");
        dbindexInfo.add("");
        dbindexInfo.add("[BALL_LIGHTPORT]");
        dbindexInfo.add("RingLight = 1");
        dbindexInfo.add("");
        dbindexInfo.add("[PACKAGE_SIZE]");
        dbindexInfo.add("NumberOfPkgSize=10");
        dbindexInfo.add("PKGSIZE_0 =" + groupName);
        dbindexInfo.add("");
        dbindexInfo.add("[LEAD_TYPE_0]");
        dbindexInfo.add("NumberOfLeadType=10");
        dbindexInfo.add("LEAD_TYPE_0 =" + visionName);
        dbindexInfo.add("");

        FileUtil.writeRecipeFile(dbindexInfo, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/DeviceName.ini");
    }

    private void runVisionSoft(String clientType) {

        String cmd = visionRecipePath;
        if (clientType.equals("L")) {
            cmd = visionRecipePath + "LGA\\Master6.exe";
        }
        if (clientType.equals("B")) {
            cmd = visionRecipePath + "BGA\\Master6.exe";
        }
        if (clientType.equals("Q")) {
            cmd = visionRecipePath + "QFN\\Master6.exe";
        }

        try {
            //        visionISecsHost.executeCommand("dos  " + cmd + " ");
            Thread.sleep(8000);
        } catch (InterruptedException ex) {

        }
        visionISecsHost.executeCommand("dos $start " + cmd + " $");
    }

    private String getPackageType(String recipeName) {
        String packageType = "";
        packageType = recipeName.substring(0, 1);
        if (packageType.equals("L") || packageType.equals("Q") || packageType.equals("B")) {
            return packageType;
        } else {
            return "";
        }
    }

    private String visionRecipePathAdd(String recipeName) {
        String fullPath = visionRecipePath;
        String packageType = getPackageType(recipeName);

        if (packageType.equals("L")) {
            fullPath = fullPath + "LGA\\DATA";
        } else if (packageType.equals("Q")) {
            fullPath = fullPath + "QFN\\DATA";
        } else if (packageType.equals("B")) {
            fullPath = fullPath + "BGA\\DATA";
        }
        return fullPath;
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        getCurrentRecipeName();
        String pkgType = "";
        if (ppExecName.equals("--")) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Disco部分程序未获取到,请先调整Disco设备状态以获取程序名.");
            return new HashMap();
        }
        if (deviceType.contains("Z2")) {
            String handleRecipeNameTmp = trimUOID(ppExecName);
            pkgType = getPackageType(handleRecipeNameTmp);
        }
        Map valueMap = new HashMap();
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            String curscreen = visionISecsHost.executeCommand("curscreen").get(0);
            if (curscreen.equals("stop")) {
                visionISecsHost.executeCommand("playback stop.txt");
            }
            visionISecsHost.executeCommand("playback gotosetup.txt");
            visionISecsHost.executeCommand("playback gotomark.txt");
            if (pkgType.equals("B")) {
                visionISecsHost.executeCommand("playback gotomarkins.txt");
                valueMap = visionISecsHost.readAllParaByScreen("amarkins");
                logger.debug("amarkins:" + valueMap);
                visionISecsHost.executeCommand("playback gotomarkset.txt");
                Map amarksetMap = visionISecsHost.readAllParaByScreen("amarkset");
                logger.debug("amarksetMap:" + amarksetMap);
                valueMap.putAll(amarksetMap);
            } else {
                visionISecsHost.executeCommand("playback gotomarksetlga.txt");
                Map amarksetlgaMap = visionISecsHost.readAllParaByScreen("amarksetlga");
                valueMap.putAll(amarksetlgaMap);
                visionISecsHost.executeCommand("playback gotomainlga.txt");
                visionISecsHost.executeCommand("playback gotomarkinslga.txt");
                Map amarkinslgaMap = visionISecsHost.readAllParaByScreen("amarkinslga");
                valueMap.putAll(amarkinslgaMap);
                visionISecsHost.executeCommand("playback gotomainlga.txt");

                visionISecsHost.executeCommand("playback gotomarkxmlga.txt");
                Map amarkxmlgaMap = visionISecsHost.readAllParaByScreen("amarkxmlga");
                valueMap.putAll(amarkxmlgaMap);
                visionISecsHost.executeCommand("playback gotomainlga.txt");

            }

            visionISecsHost.executeCommand("playback gotoball.txt");
            visionISecsHost.executeCommand("playback gotoballins.txt");

            if (pkgType.equals("B")) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                Map aballinsMap = visionISecsHost.readAllParaByScreen("aballins");
                valueMap.putAll(aballinsMap);
                visionISecsHost.executeCommand("playback gotoballset.txt");
                Map aballsetMap = visionISecsHost.readAllParaByScreen("aballset");
                valueMap.putAll(aballsetMap);

            } else {

                visionISecsHost.executeCommand("playback gotosipinslpp.txt");
                Map asipinslppMap = visionISecsHost.readAllParaByScreen("asipinslpp");
                valueMap.putAll(asipinslppMap);
                visionISecsHost.executeCommand("playback gotomainsip.txt");

                visionISecsHost.executeCommand("playback gotosipinsxm.txt");
                Map asipinsxmMap = visionISecsHost.readAllParaByScreen("asipinsxm");
                valueMap.putAll(asipinsxmMap);
                visionISecsHost.executeCommand("playback gotomainsip.txt");
            }
            visionISecsHost.executeCommand("playback stop.txt");
        }
        return valueMap;
    }

    private boolean repeatStop() {
        List<String> result = sawISecsHost.executeCommand("playback stop.txt");
        for (String string : result) {
            if ("done".equals(string)) {
                List<String> curscreens = sawISecsHost.executeCommand("curscreen");
                for (String curscreen : curscreens) {
                    if ("pause".equals(curscreen)) {
                        return true;
                    } else if (curscreen.contains("work")) {

                        for (int i = 0; i < 10; i++) {
                            try {
                                Thread.sleep(500);
                                sawISecsHost.executeCommand("playback stop.txt");
                            } catch (InterruptedException ex) {

                            }
                            List<String> result3 = sawISecsHost.executeCommand("curscreen");
                            for (String string1 : result3) {
                                if (!string1.contains("work")) {
                                    return true;
                                }
                            }

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

    @Override
    public void sendMessage2Eqp(String message) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                try {
                    if (message.length() > 300) {
                        logger.info("准备向设备发送的消息为:" + message + ",长度超过限制，将被截取");
                        message = message.substring(0, 300) + "...";
                        logger.info("实际向设备发送的消息为:" + message);
                    }
                    iSecsHost.executeCommand("message " + message);
                    sawISecsHost.executeCommand("message " + message);
                } catch (Exception e) {
                    logger.error("向设备发送消息时发生异常:" + e.getMessage());
                }
            }
        }
    }

    private void editRecipeFileAdduoid(String recipePath, String recipeName, String uoidRecipeName) {
        DiscoRecipeUtil.editRecipeName(recipePath, recipeName, uoidRecipeName);
    }
}
