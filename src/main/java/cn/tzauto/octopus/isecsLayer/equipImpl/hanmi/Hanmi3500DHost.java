package cn.tzauto.octopus.isecsLayer.equipImpl.hanmi;

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
import cn.tzauto.octopus.common.resolver.RecipeFileUtil;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Hanmi3500DHost extends EquipModel {

    private Logger logger = Logger.getLogger(Hanmi3500DHost.class);
    private final String exportPath = "D:\\hanmiRcpExpot";

    private ISecsHost sawHost;
    private ISecsHost rcpHost;//handler部分导入导出rcp
    private ISecsHost visionHost;

    private String sawRecipePath;
    private String visionRecipePath;

    private Map<String, String> recipeNameMappingMap = new HashMap<>();
    private final Map<String, String> handleRecipeMap = new HashMap();

    public Hanmi3500DHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        iSecsHostList.add(iSecsHost);
        rcpHost = new ISecsHost(remoteIPAddress, String.valueOf(12005), deviceType, deviceCode);
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
        sawHost = new ISecsHost(sawinfo.getDeviceIp(), String.valueOf(remoteTCPPort), sawinfo.getDeviceType(), deviceCode + "-S");
        iSecsHostList.add(sawHost);
        logger.info(deviceCode + " saw子机初始化完成");
        DeviceInfo visionInfo = deviceService.getDeviceInfoByDeviceCode(deviceCode + "-V").get(0);
        if (visionInfo == null || visionInfo.getDeviceIp() == null) {
            logger.error(deviceCode + " 未配置version子机信息或者信息不完整");
            return;
        }
        DeviceType deviceType = deviceTypeDic.get(visionInfo.getDeviceTypeId());
        visionRecipePath = deviceType.getSmlPath();
        visionHost = new ISecsHost(visionInfo.getDeviceIp(), String.valueOf(remoteTCPPort), visionInfo.getDeviceType(), deviceCode + "-V");
        iSecsHostList.add(visionHost);
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
    public String getCurrentRecipeName() {
        if (getPassport()) {
            try {
                List<String> result = new ArrayList<>();
                synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                    result = iSecsHost.executeCommand("curscreen");
                }
                if (result != null && !result.isEmpty()) {
                    if ("pause".equals(result.get(0))) {
                        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                            ppExecName = iSecsHost.executeCommand("read pdevid").get(0);
                        }
                    } else {
                        if ("param".equals(result.get(0))) {
                            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                                ppExecName = iSecsHost.executeCommand("read devid").get(0);
                            }
                        } else if ("work".equals(result.get(0))) {
                            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                                ppExecName = iSecsHost.executeCommand("read workdevid").get(0);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
            if (!isGetLegalRecipeName(ppExecName)) {
                ppExecName = "--";
            }
            this.returnPassport();
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        return null;
    }

    @Override
    public String pauseEquip() {
        String stopResult = "";
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    this.getEquipStatus();
                    sawHost.executeCommand("playback gotoworkscreen.txt");
                    result = sawHost.executeCommand("curscreen");
                    if (result != null && !result.isEmpty()) {
                        if ("pause".equals(result.get(0))) {
                            return "0";//"main".equals(result.get(0)) ||
                        } else if ("work".equals(result.get(0))) {
                            result = sawHost.executeCommand("readrectcolor 970 160 990 174");
                            for (String colorstr : result) {
                                if ("0x33cc33".equals(colorstr)) {
                                    equipStatus = "Idle";
                                    return "0";
                                }
                                if ("0xff0000".equals(colorstr)) {
                                    equipStatus = "Run";
                                    result = sawHost.executeCommand("playback startorstop.txt");
                                    for (String start : result) {
                                        if ("done".equals(start)) {
                                            result = sawHost.executeCommand("readrectcolor 970 160 990 174");
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
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            List<String> result = null;
            try {
                sawHost.executeCommand("playback gotoworkscreen.txt");
                if (equipStatus.equalsIgnoreCase("run")) {
                    if (repeatStop()) {
                        return "0";
                    }
                }
                stopResult = "锁机失败,当前状态无法执行锁机";
            } catch (Exception e) {
            }
        }
        synchronized (visionHost.iSecsConnection.getSocketClient()) {
            List<String> result = null;
            try {
                result = visionHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("run".equals(result.get(0))) {
                        return "0";//"main".equals(result.get(0)) ||
                    } else if ("setup".equals(result.get(0))) {
                        result = visionHost.executeCommand("playback stop.txt");
                        for (String start : result) {
                            if ("done".equals(start)) {
                                result = visionHost.executeCommand("curscreen");
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
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        //如果没有初始化映射关系，先初始化
        if (recipeNameMappingMap.isEmpty()) {
            getEquipRecipeList();
        }
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

        String path = recipeNameMappingMap.get(recipeName).split(":")[0];
        String equipRecipeName = recipeNameMappingMap.get(recipeName).split(":")[1];
        String recipePath = equipRecipePath + File.separator + path;

        String relativePath = getClientFtpRecipeRelativePath(recipeName);
        String absolutePath = getClientFtpRecipeAbsolutePath(recipeName);

        //upload saw recipe
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            List<String> result = sawHost.executeCommand("ftp " + localftpip + " "
                    + ftpUser + " " + ftpPwd + " " + recipePath + "  " + relativePath + " \"mput "
                    + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD\"");
            for (String str : result) {
                if ("done".equals(str)) {
                    //TODO 解析recipe
                    resultMap.put("recipe", recipe);
                    resultMap.put("deviceCode", deviceCode);
                    resultMap.put("recipeFTPPath", ftpRecipePath);
                    break;
                } else if (str.contains("rror")) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "upload disco part recipe falied, check the ftp server");
                    return resultMap;
                }
            }
        }

        //create DEV.LST file
        String body = equipRecipeName + "," + recipeName + ",";
        TransferUtil.setPPBody(body, 0, absolutePath + "DEV.LST_V" + recipe.getVersionNo());
        TransferUtil.setPPBody(body, 0, absolutePath + "DEVID.LST_V" + recipe.getVersionNo());

        //upload handler recipe
        String handlerName = recipeName;
        if (exportRcp(handlerName)) {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> result = rcpHost.executeCommand("ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + exportPath + " " + relativePath + " \"put " + recipeName + ".han");
                for (String str : result) {
                    if (str.contains("rror")) {
                        resultMap.put("uploadResult", "failed");
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        return resultMap;
                    }
                }
            }
        } else {
            logger.info(deviceCode + "导出recipe文件失败.");
            resultMap.put("uploadResult", "failed");
            return resultMap;
        }

        //upload vision recipe
        String command = "ftp " + localftpip + " "
                + ftpUser + " " + ftpPwd + " " + visionRecipePath + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput ";
        synchronized (visionHost.iSecsConnection.getSocketClient()) {

            String commandAdd = handlerName + ".INLET " + handlerName + ".INLET.ldb ";
            visionHost.executeCommand(command + commandAdd);

            commandAdd = handlerName + ".INLET.roi " + handlerName + ".MARK.roi ";
            visionHost.executeCommand(command + commandAdd);

            commandAdd = handlerName + ".MARK " + handlerName + ".MARK.ldb ";
            visionHost.executeCommand(command + commandAdd);

            String packageType = recipeName.substring(0, 1);
            if ("Q".equals(packageType) || "D".equals(packageType) || "L".equals(packageType)) {
                commandAdd = handlerName + ".QFN " + handlerName + ".QFN.ldb ";
                visionHost.executeCommand(command + commandAdd);
                commandAdd = handlerName + ".QFN.roi ";
                visionHost.executeCommand(command + commandAdd);
            }
            if ("B".equals(packageType)) {
                commandAdd = handlerName + ".BGA " + handlerName + ".BGA.ldb ";
                visionHost.executeCommand(command + commandAdd);
                commandAdd = handlerName + ".BGA.roi ";
            }
            List<String> result = visionHost.executeCommand(command + commandAdd);
            for (String str : result) {
                if (str.contains("rror")) {
                    resultMap.put("uploadResult", "failed");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    return resultMap;
                }
            }
        }
        //创建vision部分dbindex.inf文件
        createdbindexFile(recipeName);
        return resultMap;
    }

    private void createdbindexFile(String recipeName) {
        String pkgType = recipeName.substring(recipeName.indexOf("-")+1).substring(0,1);
        List<String> dbindexInfo = new ArrayList<>();
        dbindexInfo.add("[TOPGROUPNAME]");
        dbindexInfo.add("001=" + recipeName);
        dbindexInfo.add("[GROUPNAME]");
        dbindexInfo.add("001=" + recipeName);
        dbindexInfo.add("[001_001]");
        dbindexInfo.add("PKGNAME=" + recipeName);
        dbindexInfo.add("CAM1=" + recipeName);
        dbindexInfo.add("CAMACTIVE1=1");
        if (pkgType.equals("Q") || pkgType.equals("D") || pkgType.equals("L")) {
            dbindexInfo.add("CAM2=" + recipeName);
            dbindexInfo.add("CAMACTIVE2=1");
            dbindexInfo.add("CAM3=");
        }
        if (pkgType.equals("B")) {
            dbindexInfo.add("CAM2=");
            dbindexInfo.add("CAMACTIVE2=1");
            dbindexInfo.add("CAM3=" + recipeName);
        }
        dbindexInfo.add("CAMACTIVE3=1");
        dbindexInfo.add("CAM4=" + recipeName);
        dbindexInfo.add("CAMACTIVE4=1");

        FileUtil.writeRecipeFile(dbindexInfo, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/dbindex.inf");
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String pkgType = recipeName.substring(0, 1);
        int version = recipe.getVersionNo();
        String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");
        String equipRecipeName = equipRecipeNames[1];
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        String absolutePath = getClientFtpRecipeAbsolutePath(recipeName);

        //upload disco DEV_LST
        if (!FtpUtil.uploadFile(absolutePath + "DEV.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd) ||
                !FtpUtil.uploadFile(absolutePath + "DEVID.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传" + recipeName + "至服务端失败");
            return false;
        }

        //upload disco DEV file
        if (!FtpUtil.uploadFile(absolutePath + recipeName + ".ALU", remoteRcpPath, recipeName + ".ALU_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                !FtpUtil.uploadFile(absolutePath + recipeName + ".DFD", remoteRcpPath, recipeName + ".DFD_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                !FtpUtil.uploadFile(absolutePath + recipeName + ".CLN", remoteRcpPath, recipeName + ".CLN_V" + version, ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传" + recipeName + "至服务端失败");
            return false;
        }

        //upload handler recipe file
        if (
                !FtpUtil.uploadFile(absolutePath + recipeName + ".han", remoteRcpPath, recipeName + ".han_V" + version, ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传" + recipeName + "至服务端失败");
            return false;
        }

        //upload vision recipe file
        if (
                !FtpUtil.uploadFile(absolutePath + recipeName + ".INLET", remoteRcpPath, recipeName + ".INLET_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                        !FtpUtil.uploadFile(absolutePath + recipeName + ".INLET.ldb", remoteRcpPath, recipeName + ".INLET.ldb_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                        !FtpUtil.uploadFile(absolutePath + recipeName + ".INLET.roi", remoteRcpPath, recipeName + ".INLET.roi_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                        !FtpUtil.uploadFile(absolutePath + recipeName + ".MARK", remoteRcpPath, recipeName + ".MARK_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                        !FtpUtil.uploadFile(absolutePath + recipeName + ".MARK.ldb", remoteRcpPath, recipeName + ".MARK.ldb_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                        !FtpUtil.uploadFile(absolutePath + recipeName + ".MARK.roi", remoteRcpPath, recipeName + ".MARK.roi_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                        !FtpUtil.uploadFile(absolutePath + recipeName + "dbindex.inf", remoteRcpPath, "deindex.inf_V" + version, ftpip, ftpPort, ftpUser, ftpPwd)) {

            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传" + recipeName + "至服务端失败");
            return false;
        }

        if (pkgType.equals("Q") || pkgType.equals("D") || pkgType.equals("L")) {
            if (!FtpUtil.uploadFile(absolutePath + recipeName + ".QFN", remoteRcpPath, recipeName + ".QFN_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                    !FtpUtil.uploadFile(absolutePath + recipeName + ".QFN.ldb", remoteRcpPath, recipeName + ".QFN.ldb_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                    !FtpUtil.uploadFile(absolutePath + recipeName + ".QFN.roi", remoteRcpPath, recipeName + ".QFN.roi_V" + version, ftpip, ftpPort, ftpUser, ftpPwd)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传" + recipeName + "至服务端失败");
                return false;
            }
        } else if (pkgType.equals("B")) {
            if (!FtpUtil.uploadFile(absolutePath + recipeName + ".BGA", remoteRcpPath, recipeName + ".BGA_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                    !FtpUtil.uploadFile(absolutePath + recipeName + ".BGA.ldb", remoteRcpPath, recipeName + ".BGA.ldb_V" + version, ftpip, ftpPort, ftpUser, ftpPwd) ||
                    !FtpUtil.uploadFile(absolutePath + recipeName + ".BGA.roi", remoteRcpPath, recipeName + ".BGA.roi_V" + version, ftpip, ftpPort, ftpUser, ftpPwd)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传" + recipeName + "至服务端失败");
                return false;
            }
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        cleanRootDir();
        String localftpip = GlobalConstants.localRecipePath;
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;

        if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
            return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
        }

        String equipRecipeName = "001";
        String recipeType = recipe.getRecipeType();
        String recipeName = recipe.getRecipeName();
        String pkgType = recipeName.substring(0, 1);
        String relativePath = getClientFtpRecipeRelativePath(recipeName);
        String absolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpPath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);

        if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
            FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPath + "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        } else {
            FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/DEV.LST", ftpPath + "DEV.LST", ftpip, ftpPort, ftpUser, ftpPwd);
        }

        String body = equipRecipeName + "," + recipe.getRecipeName() + ",";
        TransferUtil.setPPBody(body, 0, absolutePath + "DEV.LST");
        TransferUtil.setPPBody(body, 0, absolutePath + "DEVID.LST");

        //download file from server
        if ("Engineer".equals(recipeType)) {
            FtpUtil.downloadFile(absolutePath + equipRecipeName + ".ALU", ftpPath + recipeName + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + equipRecipeName + ".CLN", ftpPath + recipeName + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + equipRecipeName + ".DFD", ftpPath + recipeName + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipe.getRecipeName() + ".han", ftpPath + recipeName + ".han_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

            FtpUtil.downloadFile(absolutePath + recipeName + ".INLET", ftpPath + recipeName + ".INLET_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".INLET.ldb", ftpPath + recipeName + ".INLET.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".INLET.roi", ftpPath + recipeName + ".INLET.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".MARK", ftpPath + recipeName + ".MARK_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".MARK.ldb", ftpPath + recipeName + ".MARK.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".MARK.roi", ftpPath + recipeName + ".MARK.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + "dbindex.inf", ftpPath + "dbindex.inf_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            if (pkgType.equals("Q") || pkgType.equals("D") || pkgType.equals("L")) {
                FtpUtil.downloadFile(absolutePath + recipeName + ".QFN", ftpPath + recipeName + ".QFN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".QFN.ldb", ftpPath + recipeName + ".QFN.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".QFN.roi", ftpPath + recipeName + ".QFN.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            }
            if (pkgType.equals("B")) {
                FtpUtil.downloadFile(absolutePath + recipeName + ".BGA", ftpPath + recipeName + ".BGA_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".BGA.ldb", ftpPath + recipeName + ".BGA.ldb_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".BGA.roi", ftpPath + recipeName + ".BGA.roi_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            }
        } else {
            FtpUtil.downloadFile(absolutePath + equipRecipeName + ".ALU", ftpPath + recipeName + ".ALU", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + equipRecipeName + ".CLN", ftpPath + recipeName + ".CLN", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + equipRecipeName + ".DFD", ftpPath + recipeName + ".DFD", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipe.getRecipeName() + ".han", ftpPath + recipeName + ".han", ftpip, ftpPort, ftpUser, ftpPwd);

            FtpUtil.downloadFile(absolutePath + recipeName + ".INLET", ftpPath + recipeName + ".INLET", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".INLET.ldb", ftpPath + recipeName + ".INLET.ldb", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".INLET.roi", ftpPath + recipeName + ".INLET.roi", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".MARK", ftpPath + recipeName + ".MARK_V", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".MARK.ldb", ftpPath + recipeName + ".MARK.ldb", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + recipeName + ".MARK.roi", ftpPath + recipeName + ".MARK.roi", ftpip, ftpPort, ftpUser, ftpPwd);
            FtpUtil.downloadFile(absolutePath + "dbindex.inf", ftpPath + "dbindex.inf", ftpip, ftpPort, ftpUser, ftpPwd);
            if ("Q".equals(pkgType) || "D".equals(pkgType) || "L".equals(pkgType)) {
                FtpUtil.downloadFile(absolutePath + recipeName + ".QFN", ftpPath + recipeName + ".QFN", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".QFN.ldb", ftpPath + recipeName + ".QFN.ldb", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".QFN.roi", ftpPath + recipeName + ".QFN.roi", ftpip, ftpPort, ftpUser, ftpPwd);
            }
            if ("B".equals(pkgType)) {
                FtpUtil.downloadFile(absolutePath + recipeName + ".BGA", ftpPath + recipeName + ".BGA", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".BGA.ldb", ftpPath + recipeName + ".BGA.ldb", ftpip, ftpPort, ftpUser, ftpPwd);
                FtpUtil.downloadFile(absolutePath + recipeName + ".BGA.roi", ftpPath + recipeName + ".BGA.roi", ftpip, ftpPort, ftpUser, ftpPwd);
            }

            if (RecipeEdit.hasGoldPara(sawHost.deviceTypeCode)) {
                RecipeService recipeService = new RecipeService(sqlSession);
                List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipeName, null, "GOLD");
                List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                try {
                    FileUtil.renameFile(absolutePath + equipRecipeName + ".DFD", absolutePath + "002.DFD");
                } catch (IOException e) {
                    logger.info("rename failed!");
                }
                List list = RecipeEdit.setGoldPara(recipeParas, absolutePath + "002.DFD", sawHost.deviceTypeCode);
                FileUtil.writeRecipeFile(list, absolutePath + equipRecipeName + ".DFD");
            }
        }
        //download disco recipe
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            List<String> result = sawHost.executeCommand("ftp " + localftpip + " "
                    + ftpUser + " " + ftpPwd + " " + sawRecipePath + " " + relativePath + " \"mget " + equipRecipeName + ".ALU " + equipRecipeName + ".CLN " + equipRecipeName + ".DFD DEV.LST DEVID.LST\"");
        }

        //download handler recipe
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> resut = iSecsHost.executeCommand("ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + exportPath + " " + relativePath + " \"get " + recipeName + ".han\"");
            for (String str : resut) {
                if (str.contains("rror")) {
                    return "Handle程序下载失败";
                }
            }
        }
        if (!importRcp(recipeName)) {
            return "Handle程序导入失败.Download recipe " + recipe.getRecipeName() + " failed";
        }

        //download vision recipe
        String visionCmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + visionRecipePath + " " + relativePath + "\"mget ";
        synchronized (visionHost.iSecsConnection.getSocketClient()) {
            visionHost.executeCommand(visionCmd + recipeName + ".INLET " + recipeName + ".INLET.ldb " + recipeName + ".INLET.roi\"");
            visionHost.executeCommand(visionCmd + recipeName + ".INLET " + recipeName + ".INLET.ldb " + recipeName + ".INLET.roi\"");
            visionHost.executeCommand(visionCmd + "dbindex.inf\"");
            if ("Q".equals(pkgType) || "D".equals(pkgType) || "L".equals(pkgType)) {
                visionCmd = visionCmd + recipeName + ".QFN " + recipeName + ".QFN.ldb " + recipeName + ".QFN.roi\"";
            }
            if ("B".equals(pkgType)) {
                visionCmd = visionCmd + recipeName + ".BGA " + recipeName + ".BGA.ldb " + recipeName + ".BGA.roi\"";
            }
            List<String> result = visionHost.executeCommand(visionCmd);
            for (String str : result) {
                if (str.contains("rror")) {
                    return "vision程序下载失败";
                }
            }
        }
        return "0";
    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String pkgType = recipeName.substring(recipeName.indexOf("-")+1).substring(0, 1);
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
                if ("Q".equals(pkgType) || "D".equals(pkgType) || "L".equals(pkgType) || "X".equals(pkgType)) {
                    attach.setAttachName(recipeName + ".QFN_V" + versionNo);
                }
                if ("B".equals(pkgType)) {
                    attach.setAttachName(recipeName + ".BGA_V" + versionNo);
                }
            } else if (i == 14) {
                if ("Q".equals(pkgType) || "D".equals(pkgType) || "L".equals(pkgType) || "X".equals(pkgType)) {
                    attach.setAttachName(recipeName + ".QFN.ldb_V" + versionNo);
                }
                if ("B".equals(pkgType)) {
                    attach.setAttachName(recipeName + ".BGA.ldb_V" + versionNo);
                }
            } else if (i == 15) {
                if ("Q".equals(pkgType) || "D".equals(pkgType) || "L".equals(pkgType) || "X".equals(pkgType)) {
                    attach.setAttachName(recipeName + ".QFN.roi_V" + versionNo);
                }
                if ("B".equals(pkgType)) {
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
    public String deleteRecipe(String recipeName) {
        if (recipeNameMappingMap.isEmpty()) {
            getEquipRecipeList();
        }
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            try {
                String equipRecipeNameTmp = recipeNameMappingMap.get(recipeName);
                if (equipRecipeNameTmp == null) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                }
                String[] equipRecipeNames = equipRecipeNameTmp.split(":");
                List<String> result = sawHost.executeCommand("dos \"del /q " + sawRecipePath + "\\" + equipRecipeNames[0] + "\\*\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        recipeNameMappingMap.remove(recipeName);
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
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = sawHost.executeCommand("playback selrecipe.txt");
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
            //打开选择页面
            iSecsHost.executeCommand("playback openDeviceSetup.txt");
            //登录
            iSecsHost.executeCommand("replay login.rec");
            //选择第一条recipe
            iSecsHost.executeCommand("playback sel_devicetype0.txt");
            iSecsHost.executeCommand("playback sel_visiontype0.txt");
            iSecsHost.executeCommand("playback sel_traytype0.txt");
            //device change
            iSecsHost.executeCommand("playback deviceChange.txt");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            //TODO 确认登录与确认框出现顺序
            iSecsHost.executeCommand("replay login.rec");
            iSecsHost.executeCommand("playback confirmOK.txt");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            iSecsHost.executeCommand("playback completeOK.txt");
        }
        //执行vision的处理
        synchronized (visionHost.iSecsConnection.getSocketClient()) {
            List<String> curscreens = visionHost.executeCommand("curscreen");
            for (String curscreen : curscreens) {
                if ("stop".equals(curscreen)) {
                    visionHost.executeCommand("playback stop.txt");
                }
            }
            List<String> exitvisionResult = visionHost.executeCommand("playback exitvision.txt");
            for (String string : exitvisionResult) {
                if ("done".equals(string)) {
                    String pkgType = trimUOID(recipeName).substring(0, 1);
                    runVisionSoft(pkgType);
                }
            }
        }
        return "0";
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
            Thread.sleep(8000);
        } catch (InterruptedException ex) {

        }
        visionHost.executeCommand("dos $start " + cmd + " $");
    }

    @Override
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        List<RecipePara> recipeParas = (List<RecipePara>) getEquipMonitorPara().get("recipeParaList");
        return recipeParas.size() > 0 ?  recipeParas : new ArrayList<>();
    }

    @Override
    public Map getEquipMonitorPara() {
        getEquipRecipeList();
        Map resultMap = new HashMap();
        String discoName = recipeNameMappingMap.get(ppExecName).split(",")[0];
        String[] equipRecipeNames = recipeNameMappingMap.get(discoName).split(":");
        String equipRecipeName = equipRecipeNames[1];
        String equipRecipePathtmp = sawRecipePath;
        if (!"".equals(equipRecipeNames[0])) {
            equipRecipePathtmp = sawRecipePath + "\\" + equipRecipeNames[0];
        }
        String body = equipRecipeName + "," + discoName + ",";
        String key, value;
        List<String> result = null;
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            result = sawHost.executeCommand("dos type \"" + equipRecipePathtmp + equipRecipeName + ".DFD\"");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String cfgline : result) {
            if (cfgline.contains("$Now") || cfgline.contains("PH_ID") || cfgline.contains("DEV_ID")) {
                String[] cfg = cfgline.split("=");
                //因为文件的第一行有乱码，如果读取的是第一行，要把乱码去除
                key = cfg[0];
                if (cfg.length > 2) {
                    cfg[0] = cfg[1];
                    cfg[1] = cfg[2];
                }
                if (cfg[0].contains("DEV_ID")) {
                    key = "DEV_ID";
                }
                if (cfg[0].contains("PH_ID")) {
                    key = "PH_ID";
                }
                if (key.contains("[")) { //去除[]
                    key = key.substring(0, key.indexOf("["));
                }
                String[] cfg2 = cfg[1].split("\\$");
                value = cfg2[0];
                if (value.contains("{")) {  //去除{}
                    value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
                }
                if (value.contains("\"")) { //去除引号
                    value = value.replaceAll("\"", "");
                }
                key = key.replaceAll(" ", ""); //去除空格
                value = value.replaceAll(" ", "");
                if (value.contains(",")) {
                    String[] values = value.split(",");
                    String keyTemp = "";
                    //如果参数值为数组，参数名后面加数字并分别列出
                    for (int j = 0; j < values.length; j++) {
                        keyTemp = key + String.valueOf(j + 1);
                        map.put(keyTemp, values[j]);
                    }
                } else {
                    map.put(key, value);
                }
            }
        }
        if (map != null && !map.isEmpty()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
            sqlSession.close();
            List<RecipePara> recipeParaList = RecipeFileUtil.transfer2DB(map,recipeTemplates,false);
            resultMap.put("recipeParaList", recipeParaList);
        }
        return resultMap;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {

            List<String> result = new ArrayList<>();
            try {
                result = iSecsHost.executeCommand("dos \"dir " + sawRecipePath + " /ad/w\"");
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
                        List<String> singleDirRecipe = iSecsHost.executeCommand("dos \"type " + sawRecipePath + "\\" + dir + "\\DEV.LST\"");
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
                List<String> singleDirRecipe1 = iSecsHost.executeCommand("dos \"type " + sawRecipePath + "\\DEV.LST\"");
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
        //get handler recipe list
        List<String> handlerRcpList = null;
        synchronized (rcpHost.iSecsConnection.getSocketClient()) {
            handlerRcpList = rcpHost.executeCommand("GetList");
            handlerRcpList = getHandleRecipeList(handlerRcpList);
        }
        recipeNameList = confirmRecipeList(recipeNameList, handlerRcpList);
        eppd.put("eppd", recipeNameList);
        return eppd;
    }

    /**
     * 确认recipe列表
     *
     * @param recipeNameList sawlist
     * @param handlerRcpList hanlderList
     * @return 合法的recipe
     */
    private List<String> confirmRecipeList(List<String> recipeNameList, List<String> handlerRcpList) {
        List<String> recipes = new ArrayList<>();
        for (String sawRecipe : recipeNameList) {
            for (String handlerRecipe : handlerRcpList) {
                if (sawRecipe.equals(handlerRecipe.split("--")[0])) {
                    recipes.add(sawRecipe);
                    break;
                }
            }
        }
        return recipes;
    }

    @Override
    public String getEquipStatus() {
        if (getPassport()) {
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
            this.returnPassport();
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        return equipStatus;
    }

    @Override
    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> alidresult = sawHost.executeCommand("read alarmid");
                if (alidresult.size() > 1) {
                    alarmStrings.add(alidresult.get(0));
                    logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
                } else {
                    alarmStrings.add("");
                }
                if (alidresult == null || alidresult.isEmpty()) {
                    return null;
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        //添加了一个过滤，异常时可能会将
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
    public boolean isConnect() {
        if (iSecsHost != null && sawHost != null && visionHost != null) {
            return iSecsHost.isConnect && sawHost.isConnect && visionHost.isConnect;
        } else {
            return false;
        }
    }

    @Override
    public Map getSpecificData(Map<String, String> map) {
        return null;
    }

    @Override
    public Object clone() {
        Hanmi3500DHost newEquip = new Hanmi3500DHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        this.clear();
        return newEquip;
    }

    private boolean exportRcp(String reciepName) {
        synchronized (rcpHost.iSecsConnection.getSocketClient()) {
            List<String> exportResults = rcpHost.executeCommand("Export " + getHandlerExportInfoByName(reciepName) + " \"" + exportPath + File.separator + reciepName + ".han\"");
        }
        return false;
    }

    private boolean importRcp(String recipeName) {
        List<String> importResultList = rcpHost.executeCommand("Import " + exportPath + "\\" + recipeName + ".han");
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

    /**
     * 获取hanler导出rcp信息
     *
     * @param name
     * @return
     */
    private String getHandlerExportInfoByName(String name) {
        for (Map.Entry<String, String> entry : handleRecipeMap.entrySet()) {
            //TODO 考虑更合理的比对
            if (name != null && name.equals(entry.getKey().split("--")[0])) {
                return entry.getValue();
            }
        }
        return null;
    }

    private List<String> getHandleRecipeList(List<String> recipeList) {
        List<String> handleRecipeList = new ArrayList<>();
        String recipeJson = recipeList.get(0);
        JSONObject recipeJsonObject = JSON.parseObject(recipeJson);
        JSONArray recipes = recipeJsonObject.getJSONArray("recipes");
        for (int i = 0; i < recipes.size(); i++) {
            JSONObject recipe = recipes.getJSONObject(0);
            String recipeName = recipe.getString("recipename");
            String groupNo = recipe.getString("groupno");
            String deviceNo = recipe.getString("deviceno");
            String visionNo = recipe.getString("visionno");
            handleRecipeList.add(recipeName);
            handleRecipeMap.put(recipeName, groupNo + " " + deviceNo + " " + visionNo);
        }
        return handleRecipeList;
    }

    private String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName.hashCode() + "temp/";
    }

    private String getClientFtpRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File recipeDir = new File(filePath);
        if (!recipeDir.exists()) {
            recipeDir.mkdirs();
        }
        return filePath;
    }

    private boolean repeatStop() {
        List<String> result = sawHost.executeCommand("playback stop.txt");
        for (String string : result) {
            if ("done".equals(string)) {
                List<String> curscreens = sawHost.executeCommand("curscreen");
                for (String curscreen : curscreens) {
                    if ("pause".equals(curscreen)) {
                        return true;
                    } else if (curscreen.contains("work")) {

                        for (int i = 0; i < 10; i++) {
                            try {
                                Thread.sleep(500);
                                sawHost.executeCommand("playback stop.txt");
                            } catch (InterruptedException ex) {

                            }
                            List<String> result3 = sawHost.executeCommand("curscreen");
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

    private void cleanRootDir() {
        synchronized (sawHost.iSecsConnection.getSocketClient()) {
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

    private String trimUOID(String discoRecipeName) {
        //这里传过来的recipeName是disco的 最后一个"-"之后是刀片编号，要去除
        String[] recipeNames = discoRecipeName.split("-");
        String discoRecipeNameTmp = discoRecipeName.replaceAll(recipeNames[0] + "-", "");
        return discoRecipeNameTmp;
    }

    protected Recipe setRecipe(String recipeName) {
        Recipe recipe = new Recipe();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        List<DeviceInfo> deviceInfotmp = deviceService.getDeviceInfoByDeviceCode(deviceCode);
        DeviceInfo deviceInfo = deviceInfotmp.get(0);
//        DeviceType deviceType = deviceService.getDeviceTypeMap().get(deviceInfo.getDeviceTypeId());
        recipe.setClientId(GlobalConstants.getProperty("clientId"));
        if (GlobalConstants.sysUser != null && GlobalConstants.sysUser.getId() != null) {
            recipe.setCreateBy(GlobalConstants.sysUser.getId());
            recipe.setUpdateBy(GlobalConstants.sysUser.getId());
        } else {
            recipe.setCreateBy("System");
            recipe.setUpdateBy("System");
        }
        recipe.setDeviceCode(deviceInfo.getDeviceCode());
        recipe.setDeviceId(deviceInfo.getId());
        recipe.setDeviceName(deviceInfo.getDeviceName());
        recipe.setDeviceTypeCode(deviceInfo.getDeviceType());
        recipe.setDeviceTypeId(deviceInfo.getDeviceTypeId());
        recipe.setDeviceTypeName(deviceInfo.getDeviceType());
        recipe.setProdCode("");//LGA
        recipe.setProdId("");//LGA001-12138
        recipe.setProdName("");//LGA_0.48*0.68
        recipe.setRecipeCode("");
        recipe.setRecipeDesc("");
        recipe.setRecipeName(recipeName);
        recipe.setRecipeStatus("Create");
        recipe.setRecipeType("N");
        recipe.setSrcDeviceId(GlobalConstants.getProperty("clientId"));
        //如果已存在，版本号加1
        List<Recipe> recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, "Engineer", null);
        Recipe recipeTmp = null;
        if (recipes != null && recipes.size() > 0) {
            recipeTmp = recipes.get(0);
        }
        if (recipeTmp != null) {
            recipe.setVersionNo(recipeTmp.getVersionNo() + 1);
            recipe.setTotalCnt(recipeTmp.getTotalCnt() + 1);
            recipe.setUpdateCnt(recipeTmp.getUpdateCnt() + 1);
        } else {
            recipe.setVersionNo(0);
            recipe.setTotalCnt(0);
            recipe.setUpdateCnt(0);
        }
        recipe.setVersionType("Engineer");
        recipe.setCreateDate(new Date());
        sqlSession.close();
        return recipe;
    }

}
