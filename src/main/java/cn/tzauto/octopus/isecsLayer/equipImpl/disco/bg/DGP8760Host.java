package cn.tzauto.octopus.isecsLayer.equipImpl.disco.bg;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

public class DGP8760Host extends EquipModel {

    private static Logger logger = Logger.getLogger(DGP8760Host.class.getName());
    private boolean needLock = false;

    public DGP8760Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read mainrecipename").get(0);
                    } else if ("run".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read runrecipename").get(0);
                    } else if ("pause".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read pauserecipename").get(0);
                    } else if ("running".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read runningrecipename").get(0);
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
                    this.getEquipStatus();
                    iSecsHost.executeCommand("playback gotoworkscreen.txt");
                    result = iSecsHost.executeCommand("curscreen");
                    if (result != null && !result.isEmpty()) {
                        if ("run".equals(result.get(0))) {
                            result = iSecsHost.executeCommand("playback pause.txt");
                            for (String start : result) {
                                if ("done".equals(start)) {
                                    return "0";
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
        needLock = true;
        String stopResult = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {

                    //iSecsHost.executeCommand("playback gotoworkscreen.txt");
                    while (needLock) {
                        result = iSecsHost.executeCommand("curscreen");
                        if (result != null && !result.isEmpty()) {
                            if ("run".equals(result.get(0))) {
                                result = iSecsHost.executeCommand("playback pause.txt");
                                for (String start : result) {
                                    if ("done".equals(start)) {
                                        needLock = false;
                                        return "0";

                                    }
                                }
                                stopResult = "锁机失败,当前状态无法执行锁机";
                            }
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
                sqlSession.close();
                String equipRecipePathtmp = equipRecipePath;
                boolean ocrUploadOk = true;

                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".DAT\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".DAT");
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
        attach.setAttachName(recipeName + ".DAT_V" + recipe.getVersionNo());
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
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".DAT", ftpPathTmp + recipe.getRecipeName() + ".DAT_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".DAT", ftpPathTmp + recipe.getRecipeName() + ".DAT", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".DAT", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DAT");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DAT", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".DAT");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget " + recipe.getRecipeName() + ".DAT\"");
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
    public String deleteRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                if (!recipeName.equals(ppExecName)) {
                    List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeName + ".DAT\"");
                    for (String str : result) {
                        if ("done".equals(str)) {
                            return "删除成功";
                        }
                    }
                    return "删除失败";
                } else {
                    return "删除成功";
                }

            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage());
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        getCurrentRecipeName();
        boolean recipeNameChange = false;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resulttmp = iSecsHost.executeCommand("playback gotoworkscreen.txt");
                for (String strtmp : resulttmp) {
                    if ("done".equals(strtmp)) {
                        // 点击 edit save 然后进入recipelist屏幕

                        if (!ppExecName.equals(recipeName)) {
                            iSecsHost.executeCommand("playback editsaverecipe.txt");
                            recipeNameChange = true;
                        }
                        Thread.sleep(1000);
                        List<String> result = iSecsHost.executeCommand("playback gotorecipelist.txt");
                        for (String str : result) {
                            if ("done".equals(str)) {
                                Thread.sleep(500);
                                String screen = iSecsHost.executeCommand("curscreen").get(0);
                                if (!"recipelist".equals(screen)) {
                                    logger.info("页面不在recipelist");
                                    return "设备屏幕不在recipe列表页面，选中失败";
                                }
                                for (int i = 0; i < 10; i++) {
                                    //点击第一条  读取 curid 与selid
                                    iSecsHost.executeCommand("playback selrcp1.txt");
                                    //  String curid = iSecsHost.executeCommand("read curid").get(0);
                                    String selid = iSecsHost.executeCommand("read selid").get(0).trim();
                                    if (!selid.equals(recipeName)) {
                                        iSecsHost.executeCommand("playback delrcp.txt");
                                    } else {
                                        for (int j = 0; j < 10; j++) {
                                            iSecsHost.executeCommand("playback selrcp2.txt");
                                            //点击第2条  读取 curid 与selid
                                            //   iSecsHost.executeCommand("playback selrcp1.txt");
                                            String curid2 = iSecsHost.executeCommand("read curid").get(0).trim();
                                            String selid2 = iSecsHost.executeCommand("read selid").get(0).trim();
                                            if (!recipeNameChange && curid2.equals(selid2)) {
                                                break;
                                            }
                                            if (!selid2.equals("")) {
                                                iSecsHost.executeCommand("playback delrcp.txt");
                                            } else {
                                                break;
                                            }
                                        }
                                        iSecsHost.executeCommand("playback selrcp1.txt");
                                        iSecsHost.executeCommand("playback selrcp.txt");
                                        return "0";
                                    }
                                }
                            }
                        }
                    }
                }
                return "选中失败";

            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                return "选中失败";
            }
        }
    }

    public String selectRecipe1(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resulttmp = iSecsHost.executeCommand("playback gotoworkscreen.txt");
                for (String strtmp : resulttmp) {
                    if ("done".equals(strtmp)) {
                        List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                        for (String str : result) {
                            if ("done".equals(str)) {
                                ppExecName = iSecsHost.executeCommand("read mainrecipename").get(0);
                                if (ppExecName.equals(recipeName)) {
                                    List<String> result1 = iSecsHost.executeCommand("playback delrecipe.txt");
                                    for (String str1 : result1) {
                                        if ("done".equals(str1)) {
                                            ppExecName = recipeName;
                                            return "0";
                                        }
                                        if (str1.contains("rror")) {
                                            return "选中失败";
                                        }
                                    }
                                } else {
                                    List<String> result2 = iSecsHost.executeCommand("playback selrecipe1.txt");
                                    for (String str2 : result2) {
                                        if ("done".equals(str2)) {
                                            ppExecName = recipeName;
                                            return "0";
                                        }
                                        if (str2.contains("rror")) {
                                            return "选中失败";
                                        }
                                    }
                                }
                            }
                        }
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
                        + ppExecName + ".DAT\"");
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        try {
                            Map paraMap = DiscoRecipeUtil.transferFromFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + ppExecName + "temp/" + ppExecName + ".DAT");
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
                    if (str.contains(".dat") && str.contains(".DAT")) {
                        continue;
                    }
                    if (str.contains(".dat")) {
                        String[] recipeNameTmp = str.split(".dat");
                        for (int i = 0; i < recipeNameTmp.length; i++) {
                            String recipeName = recipeNameTmp[i].replaceAll(".dat", "").trim();
                            if (recipeName.equals("")) {
                                continue;
                            }
                            recipeNameList.add(recipeName);
                        }
                    }
                    if (str.contains(".DAT")) {
                        str = str.trim();
                        String[] recipeNameTmp = str.split(".DAT");
                        for (int i = 0; i < recipeNameTmp.length; i++) {
                            String recipeName = recipeNameTmp[i].replaceAll(".DAT", "").replaceAll("\\[", "").replace("..]", "").replace(".]", "").trim();
                            if (recipeName.equals("")) {
                                continue;
                            }
                            recipeNameList.add(recipeName);
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
        DGP8760Host newEquip = new DGP8760Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
                    } else if ("main".equals(result.get(0))) {
                        equipStatus = "Idle";
                    } else if ("recipedetail1".equals(result.get(0)) || "recipedetail2".equals(result.get(0))
                            || "recipeoption1".equals(result.get(0)) || "recipeoption2".equals(result.get(0))
                            || "statusdetail1".equals(result.get(0)) || "statusdetail2".equals(result.get(0))) {
                        equipStatus = "SetUp";
                    } else if ("run".equalsIgnoreCase(result.get(0))) {
                        equipStatus = "Run";
                    } else if ("running".equalsIgnoreCase(result.get(0))) {
                        equipStatus = "Running";
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".dat", remoteRcpPath, recipeName + ".DAT_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".DAT", remoteRcpPath, recipeName + ".DAT_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
