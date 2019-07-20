/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.vision;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.vision.VSP88AHRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.disco.RecipeEdit;

import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.net.Socket;
import java.util.*;

/**
 * @author Wang DanFeng
 */
public class VSP88AHHost extends EquipModel {

    private static Logger logger = LoggerFactory.getLogger(VSP88AHHost.class.getName());

    public VSP88AHHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                // TODO: 2018/3/19  读取recipeName over
//                    ppExecName = "102";
                String ppExecNameTemp = iSecsHost.executeCommand("read recipeName").get(0);
                if (ppExecNameTemp.contains("--")) {
                    ppExecName = ppExecNameTemp.split("--")[1].trim();
                }
            } catch (Exception e) {
                logger.error(deviceCode + "=======>Get equip ExecName error:" + e.getMessage(), e);
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
        String startResult = "Start failed";
        try {
            // TODO: 2018/3/19 开始机台
            result = iSecsHost.executeCommand("playback start.txt");
            if ("done".equals(result.get(0))) {
                result = iSecsHost.executeCommand("read ok_start");
                if ("OK".equalsIgnoreCase(result.get(0))) {
                    result = iSecsHost.executeCommand("read status");
                    if (result != null && !result.isEmpty()) {
                        if (!result.get(0).contains("error")) {
                            if ("AUTO RUNNING".equals(result.get(0))) {
                                equipStatus = "RUN";
                                startResult = "0";
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            startResult = "Start failed";
        } finally {
            return startResult;
        }

    }

    @Override
    public String pauseEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String stopEquip() {
        // TODO: 2018/3/19 停止机台
        String stopResult = "锁机失败,当前状态无法执行锁机";
//        if (getPassport(1)) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();

            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    result = iSecsHost.executeCommand("playback stop.txt");
                    if ("done".equals(result.get(0))) {
                        equipStatus = result.get(0);
                        stopResult = "0";
                    }
//                    for (String str : result) {
//                        if ("done".equals(result.get(0))) {
//                            result = iSecsHost.executeCommand("read status");
//                            if (result != null && !result.isEmpty()) {
//                                if (!result.get(0).contains("error")) {
//                                    if ("STOP".equals(result.get(0))) {
//                                        equipStatus = result.get(0);
//                                        stopResult = "0";
//                                    }
//                                }
//                            }
//                        }
//                    }
                } catch (Exception e) {
                    UiLogUtil.getInstance().getInstance().appendLog2EventTab(deviceCode, "锁机失败,当前状态无法执行锁机！");
                    return stopResult;
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
        // TODO: 2018/3/19 上传recipe OVER
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //ftp ip user pwd lpath rpath "mput "103.rcp"
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端上传ftp路径
                String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
                FileUtil.CreateDirectory(clientFtpRecipeAbsolutePath);
                //ocr上传至本地
                boolean ocrUploadOk = true;
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mput " + recipeName + ".rcp \"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String uploadstr : result) {
                    if (uploadstr.contains("success")) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            // TODO: 2018/3/23  可读取修改解析方法 OVER
                            Map paraMap = VSP88AHRecipeUtil.transferFromFile(clientFtpRecipeAbsolutePath + recipeName + ".rcp");
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = VSP88AHRecipeUtil.transferFromDB(paraMap, deviceType);
                            } else {
                                logger.error("解析recipe[{}]时出错,recipe文件不存在!", recipeName);
                            }
                        } catch (Exception ex) {
                            logger.error("解析recipe[{}]时出错!", recipeName, ex);
                            ex.printStackTrace();
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", hostUploadFtpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);

                    }
                    if (uploadstr.contains("Error")) {
                        ocrUploadOk = false;
                    }
                }
                if (!ocrUploadOk) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.error("upload recipe failed", e);
            }
        }
        return resultMap;

    }

    /**
     * 获取工控机FtpRecipe绝对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeAbsolutePath(String recipeName) {
        //D:/RECIPE/**********
        return GlobalConstants.localRecipePath + "RECIPE/" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    /**
     * 获取工控机FtpRecipe相对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeRelativePath(String recipeName) {
        return (GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
    }

    /**
     * 获取机台本地Recipe路径
     *
     * @return
     */
    public String getEquipLocalRecipePath() {
//        if("D3400-6007".equals(this.deviceCode)){
//            return "C:\\Users\\vs\\AppData\\Local\\VirtualStore\\Program Files\\Visionsemicon\\VSP88AH_V90\\RECIPE";
//        }
        return equipRecipePath;
    }

    /**
     * 获取服务端上传FtpRecipe路径
     *
     * @param recipe
     * @return
     */
    public String getHostUploadFtpRecipePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        sqlSession.close();
        return ftpRecipePath;
    }

    /**
     * 获取服务端下载FtpRecipe文件全路径
     *
     * @param recipe
     * @return
     */
    public String getHostDownloadFtpRecipeFilePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String downloadFullFilePath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        sqlSession.close();
        return downloadFullFilePath;
    }


    @Override
    public String downloadRecipe(Recipe recipe) {
        // TODO: 2018/3/19 下载recipe
        //cleanRootDir();
        //ftp ip user pwd lpath rpath \"mget 103.rcp\"
        String downloadResult = "";
        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
        synchronized (socketClient) {
            try {
                String recipeName = recipe.getRecipeName();
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端下载ftp文件全路径
                String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String hostDownloadFtpRecipePath = hostDownloadFtpRecipeFilePath.substring(0, hostDownloadFtpRecipeFilePath.lastIndexOf("/") + 1);
                if (!GlobalConstants.isLocalMode) {
                    if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }

                    if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".rcp", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                    } else {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".rcp", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                        //gold版本
                        if (RecipeEdit.hasGoldPara(deviceType)) {
                            RecipeService recipeService = new RecipeService(sqlSession);
                            List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                            List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                            FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD");
                            List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD", deviceType);
                            RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD");
                        }
                    }
                }

                sqlSession.close();
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mget " + recipeName + ".rcp\"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String str : result) {
                    if (str.contains("success")) {
                        downloadResult = "0";
                    }
                    if (str.contains("Error")) {
                        downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage(), e);
                downloadResult = "Download recipe " + recipe.getRecipeName() + " failed";
            }
        }
        //this.deleteTempFile(recipe.getRecipeName());
        return downloadResult;
    }

    @Override
    public Map getEquipRealTimeState() {
        // TODO: 2018/3/19 获取当前状态等 over
        try {
            equipStatus = getEquipStatus();
            ppExecName = getCurrentRecipeName();
        } catch (NullPointerException e) {
            logger.error("获取设备信息失败,检查通讯状况");
        }
        final Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        map.put("CommState", 1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                changeEquipPanel(map);
            }
        }).start();
        return map;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        // TODO: 2018/3/19 删除recipe over
        Map map = getEquipRecipeList();
        List<String> recipeList = (List<String>) map.get("eppd");
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //机台本地recipe路径
            String equipLocalRecipePath = this.getEquipLocalRecipePath();
            try {
                if (!recipeList.contains(recipeName)) {
                    logger.info("Recipe:[{}]设备上不存在,无需删除", recipeName);
                    return "删除成功";
                } else {
                    List<String> result = iSecsHost.executeCommand("dos $del /q \"" + equipLocalRecipePath + "\\" + recipeName + ".rcp\"$");
                    for (String str : result) {
                        if ("done".equals(str)) {
                            return "删除成功";
                        }
                    }
                }
                return "删除失败";
            } catch (Exception e) {
                logger.error("Delete recipe {} error:" + e.getMessage(), recipeName, e);
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        // TODO: 2018/3/19 选中recipe over
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                String flag = iSecsHost.executeCommand("read completeflag").get(0);
                if (flag.contains("COMPLETEE")) {
                    iSecsHost.executeCommand("playback clickcompleteok.txt");
                }
                List<String> result = iSecsHost.executeCommand("playback select.txt");
                if ("done".equals(result.get(0))) {
                    //跳转页面成功后，读取recipelist的值
                    result = iSecsHost.executeCommand("goto select");
                    result = iSecsHost.executeCommand("readm r1_sel r2_sel r3_sel");
                    String recipeRow = "";
                    //查询recipe是否可以选中，并找出recipe位置信息
                    for (int i = 0; i < result.size(); i++) {
                        if (recipeName.equals(result.get(i))) {
                            recipeRow = "r" + (i + 1) + "_sel";
                            break;
                        }
                    }
                    //如果位置不为空,则执行点击此行选中对应recipe
                    if (!"".equals(recipeRow)) {
                        result = iSecsHost.executeCommand("playback " + recipeRow + ".txt");
                    }
                    //点击Set Default按钮，选定recipe
                    if ("done".equals(result.get(0))) {
                        result = iSecsHost.executeCommand("playback set_sel.txt");
                        //执行成功后，查看弹出窗口，并点击“OK”按钮
                        if ("done".equals(result.get(0))) {
                            Date oldDate = new Date();
                            Date newDate = new Date();
                            boolean setFlag = false;
                            //region 查询窗口弹出
                            while (true) {
                                newDate = new Date();
                                if ((newDate.getTime() - oldDate.getTime()) < (3 * 1000)) {
                                    //间隔0.5s
                                    Thread.sleep(500);
                                    //读取弹出窗的标识“OK”
                                    result = iSecsHost.executeCommand("read setok_sel");
                                    if ("OK".equals(result.get(0))) {
                                        setFlag = true;
                                        break;
                                    }
                                }
                                break;
                            }
                            //endregion
                            if (setFlag) {
                                //有弹窗,点击"ok",并close
                                result = iSecsHost.executeCommand("playback setok_sel.txt");
                                if ("done".equals(result.get(0))) {
                                    result = iSecsHost.executeCommand("playback close_sel.txt");
                                    Thread.sleep(500);
                                    if (!"done".equals(result.get(0))) {
                                        return "选中失败";
                                    } else {
                                        return "0";
                                    }
                                }
                            }
                        }
                    }
                }
                //查询recipe是否有更改成功
//                String currentRecipeName = getCurrentRecipeName();
//                if (currentRecipeName.equals(recipeName)) {
//
//                }
            }
            return "选中失败";
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return "选中失败";
        }
    }

    @Override
    public Map getEquipMonitorPara() {
        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(getCurrentRecipeName()).get("recipeParaList");
        Map<String, String> resultMap = new HashMap<>();
        for (RecipePara recipePara : recipeParaList) {
            resultMap.put(recipePara.getParaName(), recipePara.getSetValue());
        }
        logger.info("monitormap:" + resultMap.toString());
        return resultMap;
        // TODO: 2018/3/19 获取参数
    }

    @Override
    public Map getEquipRecipeList() {
        // TODO: 2018/3/19 获取recipe列表 over
        List<String> result = new ArrayList<>();
        List<String> equipRecipeList = new ArrayList<>();
        Map eppd = new HashMap();
        //机台本地recipe路径
        String equipLocalRecipPath = this.getEquipLocalRecipePath();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                result = iSecsHost.executeCommand("dos $dir " + "\"" + equipLocalRecipPath + File.separator + "*.rcp" + "\" /b$");
                for (String recipeName : result) {
                    if (!"done".equals(recipeName) && !"".equals(recipeName)) {
                        equipRecipeList.add(recipeName.replace(".rcp", ""));
                    }
                }
            } catch (Exception e) {
                return eppd;
            }
        }
        eppd.put("eppd", equipRecipeList);
        return eppd;
    }

    @Override
    public void run() {
//        while (!this.isInterrupted()) {
        if (!isInitState) {
            logger.info(deviceCode + "=======>初始化界面信息！");
            getEquipRealTimeState();
            logger.info(deviceCode + "=======>初始化界面信息完成！");
            isInitState = true;
        }
//        }
    }

    @Override
    public Object clone() {
        VSP88AHHost newEquip = new VSP88AHHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        // TODO: 2018/3/19 查询报警?
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("readrectcolor 740 60 760 65 ");
                for (String colorstr : result) {
                    if ("0xc0c0c0".equals(colorstr)) {
                        alarmStrings.add("");
                    }
                    if ("0xff0000".equals(colorstr)) {
                        logger.info("The equip state changged to alarm...");
                        List<String> resultAlarmId = iSecsHost.executeCommand("read alarmid");
                        if (resultAlarmId.size() > 1) {
                            alarmStrings.add(resultAlarmId.get(0));
                            logger.info("Get alarm ALID=[" + resultAlarmId.get(0) + "]");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage(), e);
            }
        }
        if (alarmStrings.size() > 0) {
            alarmStrings.remove("0xff0000");
        }
        return alarmStrings;
    }

    @Override
    public String getMTBA() {
        List<String> result = new ArrayList<>();
        String mtba = "";
        try {
            result = iSecsHost.executeCommand("read mtba");
            if (result != null && result.size() > 1) {
                logger.info("Get the MTBA from equip:" + deviceCode + " MTBA:[" + result.get(0) + "");
                mtba = result.get(0);
            }
        } catch (Exception e) {
            logger.error("Get Equip MTBA error:" + e.getMessage(), e);
        }
        return mtba;
    }

    @Override
    public String getEquipStatus() {
        // TODO: 2018/3/19 获取机台状态  \
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("read status");
                if (result != null && !result.isEmpty()) {
                    if (!result.get(0).contains("error") && !"done".equalsIgnoreCase(result.get(0))) {
                        if ("MACHINE IDLE".equals(result.get(0))) {
                            equipStatus = "IDLE";
                        } else if ("AUTO RUNNING".equals(result.get(0))) {
                            equipStatus = "RUN";
                        } else if (result.contains("ERROR")) {
                            equipStatus = "ERROR";
                        } else {
                            equipStatus = result.get(0);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(deviceCode + "=======>Get equip status error:", e);
            } finally {
                Map map = new HashMap();
                map.put("EquipStatus", equipStatus);
                map.put("PPExecName", ppExecName);
                changeEquipPanel(map);
                return equipStatus;
            }
        }
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // TODO: 2018/3/19 上传recipe到ftp over
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + ".rcp", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".rcp 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        //this.deleteTempFile(recipeName);
        return true;
    }

    public Map getLaserEnergy() {
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.sendMsg2Equip("curscreen");
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
                logger.error("Get equip LaserEnergy error:" + e.getMessage(), e);
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
                logger.error("目录清理时发生异常:" + e.getMessage(), e);
            }
        }
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            logger.info(deviceCode + "预下载recipe相同，取消下载操作！");
            return "1";
        }
        return "0";
    }

    @Override
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        String recipeName = getCurrentRecipeName();
        if(StringUtils.isBlank(recipeName) || recipeName.contains("error")) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "获取当前recipe参数失败,无法获取当前recipe名称。");
            return null;
        }
        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(recipeName).get("recipeParaList");
        if(recipeParaList != null) {
            return recipeParaList;
        }
        return new ArrayList<>();
    }

}
