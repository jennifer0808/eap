/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.granda;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.resolver.granda.OPTIRcpTransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

/**
 * @author
 */
public class OPTI730Host extends EquipModel {

    private static Logger logger = Logger.getLogger(OPTI730Host.class.getName());
    private String visionRecipePath;
    private ISecsHost visionISecsHost;

    public OPTI730Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        iSecsHostList.add(iSecsHost);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Map<String, DeviceType> deviceTypeDic = deviceService.getDeviceTypeMap();
        DeviceInfo versioninfo = deviceService.getDeviceInfoByDeviceCode(deviceCode + "-V").get(0);
        if (versioninfo == null || versioninfo.getDeviceIp() == null) {
            logger.error(deviceCode + " 未配置vision子机信息或者信息不完整");
            return;
        }
        DeviceType deviceType = deviceTypeDic.get(versioninfo.getDeviceTypeId());
        visionRecipePath = deviceType.getSmlPath();
        visionISecsHost = new ISecsHost(versioninfo.getDeviceIp(), String.valueOf(remoteTCPPort), versioninfo.getDeviceType(), deviceCode + "-V");
        iSecsHostList.add(visionISecsHost);
        logger.info(deviceCode + " vision子机初始化完成");
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
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String rcpNameTemp = "";
            try {
                List<String> result = iSecsHost.executeCommand("dos $dir \"" + equipRecipePath + "\\JobInformation\" /a/w$");
                outer:
                if (result != null && result.size() > 1) {
                    for (String str : result) {
                        if (!str.contains(".jif")) {
                            continue;
                        }
                        if (str.contains("[") && str.contains(".jif")) {
                            str = str.replace("[", "").replace(" ", "");
                            String[] dirs = str.split("]");
                            for (String dir : dirs) {
                                if (dir.equals(".") || dir.equals("..")) {
                                    continue;
                                }
                                if (dir.contains(".jif")) {
                                    String[] dirsTemp = dir.split("\\.");
                                    logger.info("recipeName==" + dirsTemp[0]);
                                    rcpNameTemp = dirsTemp[0];
                                    break outer;

                                }
                            }
                        } else {
                            rcpNameTemp = str.split(".jif")[0];
                            break outer;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
            ppExecName = rcpNameTemp;
        }
        if (!isGetLegalRecipeName(ppExecName)) {
            ppExecName = "--";
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
                    if (equipStatusTemp.equals("STOP")) {
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
                    if (equipStatusTemp.equals("STOP")) {
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
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "\\JobInformation  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".jif\"");
                for (String uploadstr : result) {
                    if (uploadstr.contains("rror") || uploadstr.contains("Not connected")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        return resultMap;
                    }
                }
                List<String> stripRresult = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "\\StripInformation  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".sin\"");
                for (String uploadstr : stripRresult) {
                    if (uploadstr.contains("rror") || uploadstr.contains("Not connected")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        return resultMap;
                    }
                }
//                Map map = new HashMap();
//                for (String uploadstr : result) {
//                    if ("done".equals(uploadstr)) {
//                        List<RecipePara> recipeParaList = new ArrayList<>();
//                        try {
//                            recipeParaList = OPTIRcpTransferUtil.transferOptiRcp(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".jif");
//                        } catch (Exception e) {
//                        }
//                        resultMap.put("recipe", recipe);
//                        resultMap.put("deviceCode", deviceCode);
//                        resultMap.put("recipeFTPPath", ftpRecipePath);
//                        resultMap.put("recipeParaList", recipeParaList);
//                    }
//                    if (uploadstr.contains("Not connected")) {
//                        ocrUploadOk = false;
//                    }
//                }
                List<String> handleRecipeUploadresult = visionISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + visionRecipePath + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + "-Criteria.csv "
                        + recipeName + ".o2d\"");
                for (String uploadstr : handleRecipeUploadresult) {
                    if (uploadstr.contains("rror") || uploadstr.contains("Not connected")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        return resultMap;
                    }
                    if (uploadstr.contains("done")) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        Map paraMap = OPTIRcpTransferUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + "-Criteria.csv");
                        paraMap.putAll(OPTIRcpTransferUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".sin"));
                        paraMap.putAll(OPTIRcpTransferUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".jif"));
                        recipeParaList = OPTIRcpTransferUtil.transferFromDB(paraMap, deviceType);
//
//                        try {
//                            recipeParaList = OPTIRcpTransferUtil.transferOptiRcp(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP" + recipeName + ".jif");
//                        } catch (Exception e) {
//                            logger.error("transferOptiRcp 出错", e);
//                        }
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
        for (int i = 0; i < 3; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            if (i == 0) {
                attach.setAttachName(recipeName + ".jif_V" + recipe.getVersionNo());
            } else if (i == 1) {
                attach.setAttachName(recipeName + ".sin_V" + recipe.getVersionNo());
            } else if (i == 2) {
                attach.setAttachName(recipeName + ".o2d_V" + recipe.getVersionNo());
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
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".jif", ftpPathTmp + recipe.getRecipeName() + ".jif_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".sin", ftpPathTmp + recipe.getRecipeName() + ".sin_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".o2d", ftpPathTmp + recipe.getRecipeName() + ".o2d_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

            } else {
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".jif", ftpPathTmp + recipe.getRecipeName() + ".jif", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".sin", ftpPathTmp + recipe.getRecipeName() + ".sin", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".o2d", ftpPathTmp + recipe.getRecipeName() + ".o2d", ftpip, ftpPort, ftpUser, ftpPwd);

            }
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + "\\JobInformation " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".jif\"");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + "\\StripInformation " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".sin\"");

                List<String> resultv = visionISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + visionRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".o2d\"");

                for (String str : resultv) {
                    if (str.contains("rror")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + " 时失败,请检查FTP服务是否开启.");
                        return "下载Recipe:" + recipe.getRecipeName() + "时失败,请检查FTP服务是否开启.";
                    }
                    if (str.contains("done")) {
                        return "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
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
//                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeName + ".jif\"");
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\JobInformation\\*.jif\"");
                List<String> stripresult = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\StripInformation\\*.sin\"");
                List<String> resultv = visionISecsHost.executeCommand("dos \"del /q " + visionRecipePath + "\\*.o2d\"");
                List<String> resultv1 = visionISecsHost.executeCommand("dos \"del /q " + visionRecipePath + "\\*.csv\"");
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
                List<String> resultTemp = iSecsHost.executeCommand("curscreen");
                for (String string : resultTemp) {
                    if (string.contains("main")) {
//                    List<String> result = iSecsHost.executeCommand("playback gotoproduct.txt");
//                    for (String str : result) {
//                        if ("done".equals(str)) {
                        List<String> result1 = iSecsHost.executeCommand("playback selrecipe.txt");
                        if (result1.contains("done")) {
                            ppExecName = recipeName;
                            return "0";
                        }
//                        }
//                        if (str.contains("rror")) {
//                            return "选中失败";
//                        }
//                    }
                    }
                }
//                if (resultTemp.contains("product")) {
//                    List<String> result1 = iSecsHost.executeCommand("playback selrecipe.txt");
//                    if (result1.contains("done")) {
//                        ppExecName = recipeName;
//                        return "0";
//                    }
//                }
//                else {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "请返回主页面后下载程序!");
//                }
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
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            try {

                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                TransferUtil.setPPBody(ppExecName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/TMP");
                List<String> result = visionISecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + visionRecipePath + "  " + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + " \"mput "
                        + ppExecName + "-Criteria.csv\"");

                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList;
                        Map paraMap = OPTIRcpTransferUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName + "-Criteria.csv");
                        recipeParaList = OPTIRcpTransferUtil.transferFromDB(paraMap, deviceType);
                        map.put("deviceCode", deviceCode);
                        map.put("recipeParaList", recipeParaList);
                    }

                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        logger.info("monitormap:" + map.toString());
//        deleteTempFile(ppExecName);
        return map;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (visionISecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = new ArrayList<>();
            try {
//                result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + " /ad/w\"");
                result = visionISecsHost.executeCommand("dos $dir \"" + visionRecipePath + "\" /a/w$");
            } catch (Exception e) {
                return eppd;
            }
            if (result != null && result.size() > 1) {
                for (String str : result) {
                    if (!str.contains("-Criteria.csv")) {
                        continue;
                    }

                    str = str.replace(" ", "").replace("txt", "").replace("log", "").replace("csv", "").replace("o2d", "");
                    String[] dirs = str.split("\\.");
                    for (String dir : dirs) {
                        if (dir.contains("-Criteria")) {
                            recipeNameList.add(dir);
                        }
                    }
                }
            }
        }

        List<String> recipeNameListTmp1 = new ArrayList<>();
        for (String string : recipeNameList) {
            recipeNameListTmp1.add(string.replace("-Criteria", ""));
        }
        eppd.put("eppd", recipeNameListTmp1);
        return eppd;
    }

    @Override
    public Object clone() {
        OPTI730Host newEquip = new OPTI730Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if ("alarm".equals(result)) {
                    logger.info("The equip state changged to alarm...");
                    List<String> alidresult = iSecsHost.executeCommand("read alarmid");
                    if (alidresult.size() > 1) {
                        alarmStrings.add(alidresult.get(0));
                        logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        return alarmStrings;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.iSecsHost.executeCommand("curscreen");//0x8000
                if (result.contains("run")) {
                    //状态尚未补全
                    equipStatus = "Run";
                } else {
                    equipStatus = "Idle";
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".jif", remoteRcpPath, recipeName + ".jif_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".o2d", remoteRcpPath, recipeName + ".o2d_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".sin", remoteRcpPath, recipeName + ".sin_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
//        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isConnect() {
        if (iSecsHost != null && visionISecsHost != null) {
            return iSecsHost.isConnect && visionISecsHost.isConnect;
        } else {
            return false;
        }
    }
}
