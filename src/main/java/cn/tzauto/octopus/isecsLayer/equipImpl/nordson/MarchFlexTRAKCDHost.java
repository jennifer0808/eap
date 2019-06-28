/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.nordson;

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
import cn.tzauto.octopus.isecsLayer.resolver.nordson.FlexTRAKRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.net.Socket;
import java.util.*;

/**
 * @author Wang DanFeng
 */
public class MarchFlexTRAKCDHost extends EquipModel {

    private static Logger logger = Logger.getLogger(MarchFlexTRAKCDHost.class.getName());
    private long getRecipeAndStatusTime = new Date().getTime();
    private boolean initFlag = false;

    public MarchFlexTRAKCDHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    /**
     * 初始化状态及recipe
     */
    private void init() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipeName").get(0);
                        result = iSecsHost.executeCommand("playback gotocontrol.txt");
                        getStatus();
                    } else if ("control".equals(result.get(0))) {
                        getStatus();
                        result = iSecsHost.executeCommand("playback gotomain.txt");
                        ppExecName = iSecsHost.executeCommand("read recipeName").get(0);
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage(), e);
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
    }

    private boolean getCheckIntervalTimeFlag() {
        long nowDate = new Date().getTime();
        long intervalTime = new Date().getTime() - getRecipeAndStatusTime;
        if (intervalTime > 5 * 60 * 1000) {
            getRecipeAndStatusTime = nowDate;
            return true;
        } else {
            return false;
        }
    }
    @Override
    public Map getEquipRealTimeStateNow() {
        // 实时check到当前Recipe
//        equipStatus = getEquipStatus();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        String readResult = iSecsHost.executeCommand("read recipeName").get(0);
                        if (!readResult.contains("Error")) {
                            ppExecName = readResult;
                        }else{
                            ppExecName = "Error";
                        }
                    } else if ("control".equals(result.get(0))) {
                        result = iSecsHost.executeCommand("playback gotomain.txt");
                        String readResult = iSecsHost.executeCommand("read recipeName").get(0);
                        if (!readResult.contains("Error")) {
                            ppExecName = readResult;
                        }else{
                            ppExecName = "Error";
                        }
                    }else if (result.get(0).contains("socket write error")){
                        ppExecName = "Error";
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage(), e);
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        return map;
    }

    @Override
    public String getCurrentRecipeName() {
        if (!initFlag) {
            init();
            initFlag = true;
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        String readResult = iSecsHost.executeCommand("read recipeName").get(0);
                        if (!readResult.contains("Error")) {
                            ppExecName = readResult;
                        }else{
                            ppExecName = "Error";
                        }
                    } else if ("control".equals(result.get(0))) {
                        if (getCheckIntervalTimeFlag()) {
                            result = iSecsHost.executeCommand("playback gotomain.txt");
                            String readResult = iSecsHost.executeCommand("read recipeName").get(0);
                            if (!readResult.contains("Error")) {
                                ppExecName = readResult;
                            }else{
                                ppExecName = "Error";
                            }
                        }
                    }else if (result.get(0).contains("socket write error")){
                        ppExecName = "Error";
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage(), e);
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
                    result = iSecsHost.executeCommand("curscreen");
                    if (!"control".equals(result.get(0))) {
                        result = iSecsHost.executeCommand("playback gotocontrol.txt");
                    }
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
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "锁机失败,当前状态无法执行锁机！");
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
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mput " + recipeName + ".csv \"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String uploadstr : result) {
                    if (uploadstr.contains("success")) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            recipeParaList = FlexTRAKRecipeUtil.transferFromRecipe(clientFtpRecipeAbsolutePath + recipeName + ".csv", deviceType);
                        } catch (Exception ex) {
                            logger.error("解析recipe时出错!", ex);
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
                logger.error("upload recipe failed" + e.getMessage(), e);
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
        if ("D3400-6007".equals(this.deviceCode)) {
            return "C:\\Users\\vs\\AppData\\Local\\VirtualStore\\Program Files\\Visionsemicon\\VSP88AH_V90\\RECIPE";
        }
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
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".csv", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                    } else {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".csv", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                        //gold版本
//                        if (RecipeEdit.hasGoldPara(deviceType)) {
//                            RecipeService recipeService = new RecipeService(sqlSession);
//                            List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
//                            List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
//                            FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD");
//                            List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD", deviceType);
//                            RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD");
//                        }
                    }
                }

                sqlSession.close();
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"mget " + recipeName + ".csv\"";
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
        // TODO: 2018/3/19 删除recipe over
        Map map = getEquipRecipeList();
        List<String> recipeList = (List<String>) map.get("eppd");
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //机台本地recipe路径
            String equipLocalRecipePath = this.getEquipLocalRecipePath();
            try {
                if (!recipeList.contains(recipeName)) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                } else {
                    List<String> result = iSecsHost.executeCommand("dos $del /q \"" + equipLocalRecipePath + "\\" + recipeName + ".del\"$");
                    for (String str : result) {
                        if ("done".equals(str)) {
                            return "删除成功";
                        }
                    }
                }
                return "删除失败";
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        // TODO: 2018/3/19 选中recipe over
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> curscreenResult = iSecsHost.executeCommand("curscreen");
                if ("control".equalsIgnoreCase(curscreenResult.get(0))) {
                    iSecsHost.executeCommand("playback gotomain.txt");
                    curscreenResult = iSecsHost.executeCommand("curscreen");
                    if(!"main".equals(curscreenResult.get(0))){
                        return "选中失败,未跳至main界面!";
                    }
                }
                //登录权限
                if(!loginEngineer()){
                    return "选中失败,未机台未登录至Engineer权限!";
                }
                List<String> result = iSecsHost.executeCommand("playback select.txt");
                if ("done".equals(result.get(0))) {
                    //跳转页面成功后，读取recipelist的值
                    result = iSecsHost.executeCommand("goto select");
                    result = iSecsHost.executeCommand("curscreen");
                    if(!"select".equals(result.get(0))){
                        result = iSecsHost.executeCommand("playback select.txt");
                        result = iSecsHost.executeCommand("curscreen");
                        if(!"select".equals(result.get(0))){
                            return "选中失败,未跳至select界面!";
                        }
                    }
                    result = iSecsHost.executeCommand("write writerecipe " + equipRecipePath + File.separator + recipeName + ".csv");
                    result = iSecsHost.executeCommand("playback  select_ok.txt");
//                    result = iSecsHost.executeCommand("goto select");
//                    result = iSecsHost.executeCommand("readm r1_sel r2_sel r3_sel");
//                    String recipeRow = "";
//                    //查询recipe是否可以选中，并找出recipe位置信息
//                    for (int i = 0; i < result.size(); i++) {
//                        if (recipeName.equals(result.get(i))) {
//                            recipeRow = "r" + (i + 1) + "_sel";
//                            break;
//                        }
//                    }
//                    //如果位置不为空,则执行点击此行选中对应recipe
//                    if (!"".equals(recipeRow)) {
//                        result = iSecsHost.executeCommand("playback " + recipeRow + ".txt");
//                    }
//                    //点击Set Default按钮，选定recipe
//                    if ("done".equals(result.get(0))) {
//                        result = iSecsHost.executeCommand("playback set_sel.txt");
//                        //执行成功后，查看弹出窗口，并点击“OK”按钮
//                        if ("done".equals(result.get(0))) {
//                            Date oldDate = new Date();
//                            Date newDate = new Date();
//                            boolean setFlag = false;
//                            //region 查询窗口弹出
//                            while (true) {
//                                newDate = new Date();
//                                if ((newDate.getTime() - oldDate.getTime()) < (3 * 1000)) {
//                                    //间隔0.5s
//                                    Thread.sleep(500);
//                                    //读取弹出窗的标识“OK”
//                                    result = iSecsHost.executeCommand("read setok_sel");
//                                    if ("OK".equals(result.get(0))) {
//                                        setFlag = true;
//                                        break;
//                                    }
//                                }
//                            }
//                            //endregion
//                            if (setFlag) {
//                                //有弹窗,点击"ok",并close
//                                result = iSecsHost.executeCommand("playback setok_sel.txt");
//                                if ("done".equals(result.get(0))) {
//                                    result = iSecsHost.executeCommand("playback close_sel.txt");
//                                    Thread.sleep(500);
//                                    if (!"done".equals(result.get(0))) {
//                                        return "选中失败";
//                                    }
//                                }
//                            }
//                        }
//                    }
                }
                Thread.sleep(1000);
                //查询recipe是否有更改成功
                String currentRecipeName = getCurrentRecipeName();
                if (currentRecipeName.equals(recipeName)) {
                    return "0";
                }
            }
            return "选中失败";
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return "选中失败";
        }finally {
            Thread thread = new Thread(new MyThread(this));
            thread.start();
        }
    }

    class MyThread implements Runnable {
        MarchFlexTRAKCDHost marchFlexTRAKCDHost;

        public MyThread() {
        }

        public MyThread(MarchFlexTRAKCDHost marchFlexTRAKCDHost) {
            this.marchFlexTRAKCDHost = marchFlexTRAKCDHost;
        }

        @Override
        public void run() {
            marchFlexTRAKCDHost.logoutEngineer();
        }
    }

    private boolean loginEngineer() throws InterruptedException {
        boolean result = true;
        List<String> userResult = iSecsHost.executeCommand("read user");
        if (userResult != null && "Engineer".equalsIgnoreCase(userResult.get(0))) {
            logger.info("当前权限已是Engineer，无需重复登录！");
            return result;
        }
        if (!this.gotoScreenByOCR("login", "click_login", 100)) {
            logger.info("跳转至login界面失败！");
            result = false;
        } else {
            List<String> loginResult = iSecsHost.executeCommand("playback login.txt");
            if(!"done".equalsIgnoreCase(loginResult.get(0))){
                logger.info("执行login失败！");
                result = false;
            }else{
                loginResult = iSecsHost.executeCommand("playback login_ok.txt");
                if(!"done".equalsIgnoreCase(loginResult.get(0))){
                    logger.info("执行login_ok失败！");
                    result = false;
                }
            }
        }
        return result;
    }

    private boolean logoutEngineer() {
        boolean result = true;
        try {
            List<String> userResult = iSecsHost.executeCommand("read user");
            if (!this.gotoScreenByOCR("login", "click_login", 100)) {
                logger.info("跳转至login界面失败！");
                result = false;
            } else {
                List<String> loginResult = iSecsHost.executeCommand("playback login_op.txt");
                if (!"done".equalsIgnoreCase(loginResult.get(0))) {
                    logger.info("执行login_op失败！");
                    result = false;
                } else {
                    loginResult = iSecsHost.executeCommand("playback login_ok.txt");
                    if (!"done".equalsIgnoreCase(loginResult.get(0))) {
                        logger.info("执行login_ok失败！");
                        result = false;
                    }
                }
            }
        } catch (Exception ex) {
            logger.info(ex);
        }
        return result;
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
        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(getCurrentRecipeName()).get("recipeParaList");
        Map<String, String> resultMap = new HashMap<>();
        for (RecipePara recipePara : recipeParaList) {
            resultMap.put(recipePara.getParaName(), recipePara.getSetValue());
        }
        logger.info("Monitormap:" + resultMap.toString());
        return resultMap;
        // TODO: 2018/3/19 获取参数
//        Map map = new HashMap();
////        if (getPassport()) {
//        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
//        synchronized (socketClient) {
//            try {
//                iSecsHost.executeCommand("playback gotoworkscreen.txt");
//                iSecsHost.executeCommand("goto param");
//                List<String> screenList = iSecsHost.executeCommand("curscreen");
//                if (screenList != null && !screenList.isEmpty()) {
//                    if (!"param".equals(screenList.get(0))) {
//                        iSecsHost.executeCommand("playback gotoworkscreen.txt");
//                        iSecsHost.executeCommand("goto param");
//                    }
//                    List<String> result = iSecsHost.executeCommand("readm gzxz gzcc hdjp hdjiaop devid");
//                    if (result.size() == 6) {
//                        map.put("gzxz", result.get(0));
//                        map.put("gzcc", result.get(1));
//                        map.put("hdjp", result.get(2));
//                        map.put("hdjiaop", result.get(3));
//                        map.put("devid", result.get(4));
//
//                        if ("done".equals(result.get(5))) {
//                            iSecsHost.executeCommand("goto scsch1");
//                            List<String> result1 = iSecsHost.executeCommand("readm ch1lp1 ch1lp2 ch1f1 ch1f2 ch1da1 ch1da2 ch1fs1 ch1fs2 ch1i1 ch1i2 ch1dsi1 ch1dsi2");
//                            if (result1.size() == 13) {
//                                map.put("ch1lp1", result1.get(0));
//                                map.put("ch1lp2", result1.get(1));
//                                map.put("ch1f1", result1.get(2));
//                                map.put("ch1f2", result1.get(3));
//                                map.put("ch1da1", result1.get(4));
//                                map.put("ch1da2", result1.get(5));
//                                map.put("ch1fs1", result1.get(6));
//                                map.put("ch1fs2", result1.get(7));
//                                map.put("ch1i1", result1.get(8));
//                                map.put("ch1i2", result1.get(9));
//                                map.put("ch1dsi1", result1.get(10));
//                                map.put("ch1dsi2", result1.get(11));
//                                if ("done".equals(result1.get(12))) {
//                                    iSecsHost.executeCommand("goto scsch2");
//                                    List<String> result2 = iSecsHost.executeCommand("readm ch2lp1 ch2lp2 ch2f1 ch2f2 ch2da1 ch2da2 ch2fs1 ch2fs2 ch2i1 ch2i2 ch2dsi1 ch2dsi2");
//                                    if (result2.size() == 13) {
//                                        map.put("ch2lp1", result2.get(0));
//                                        map.put("ch2lp2", result2.get(1));
//                                        map.put("ch2f1", result2.get(2));
//                                        map.put("ch2f2", result2.get(3));
//                                        map.put("ch2da1", result2.get(4));
//                                        map.put("ch2da2", result2.get(5));
//                                        map.put("ch2fs1", result2.get(6));
//                                        map.put("ch2fs2", result2.get(7));
//                                        map.put("ch2i1", result2.get(8));
//                                        map.put("ch2i2", result2.get(9));
//                                        map.put("ch2dsi1", result2.get(10));
//                                        map.put("ch2dsi2", result2.get(11));
//                                        if ("done".equals(result2.get(12))) {
//                                            iSecsHost.executeCommand("goto scsch3");
//                                            List<String> result3 = iSecsHost.executeCommand("readm ch3lp1 ch3lp2 ch3f1 ch3f2 ch3da1 ch3da2 ch3fs1 ch3fs2 ch3i1 ch3i2 ch3dsi1 ch3dsi2");
//                                            if (result3.size() == 13) {
//                                                map.put("ch3lp1", result3.get(0));
//                                                map.put("ch3lp2", result3.get(1));
//                                                map.put("ch3f1", result3.get(2));
//                                                map.put("ch3f2", result3.get(3));
//                                                map.put("ch3da1", result3.get(4));
//                                                map.put("ch3da2", result3.get(5));
//                                                map.put("ch3fs1", result3.get(6));
//                                                map.put("ch3fs2", result3.get(7));
//                                                map.put("ch3i1", result3.get(8));
//                                                map.put("ch3i2", result3.get(9));
//                                                map.put("ch3dsi1", result3.get(10));
//                                                map.put("ch3dsi2", result3.get(11));
//
//                                                if ("done".equals(result3.get(12))) {
//                                                    iSecsHost.executeCommand("goto scsch4");
//                                                    List<String> result4 = iSecsHost.executeCommand("readm ch4lp1 ch4lp2 ch4f1 ch4f2 ch4da1 ch4da2 ch4fs1 ch4fs2 ch4i1 ch4i2 ch4dsi1 ch4dsi2");
//                                                    if (result4.size() == 13) {
//                                                        map.put("ch4lp1", result4.get(0));
//                                                        map.put("ch4lp2", result4.get(1));
//                                                        map.put("ch4f1", result4.get(2));
//                                                        map.put("ch4f2", result4.get(3));
//                                                        map.put("ch4da1", result4.get(4));
//                                                        map.put("ch4da2", result4.get(5));
//                                                        map.put("ch4fs1", result4.get(6));
//                                                        map.put("ch4fs2", result4.get(7));
//                                                        map.put("ch4i1", result4.get(8));
//                                                        map.put("ch4i2", result4.get(9));
//                                                        map.put("ch4dsi1", result4.get(10));
//                                                        map.put("ch4dsi2", result4.get(11));
//
//                                                        if ("done".equals(result4.get(12))) {
//                                                            iSecsHost.executeCommand("goto sidch1");
//                                                            List<String> result5 = iSecsHost.executeCommand("readm qgsx ch1y1");
//                                                            if (result5.size() == 3) {
//                                                                map.put("qgsx", result5.get(0));
//                                                                map.put("ch1y1", result5.get(1));
//                                                                if ("done".equals(result5.get(2))) {
//                                                                    iSecsHost.executeCommand("goto sidch2");
//                                                                    List<String> result6 = iSecsHost.executeCommand("readm ch2y1");
//                                                                    if (result6.size() == 2) {
//                                                                        map.put("ch2y1", result6.get(0));
//                                                                        if ("done".equals(result6.get(1))) {
//                                                                            iSecsHost.executeCommand("goto sidch3");
//                                                                            List<String> result7 = iSecsHost.executeCommand("readm ch3y1");
//                                                                            if (result7.size() == 2) {
//                                                                                map.put("ch3y1", result7.get(0));
//                                                                                if ("done".equals(result7.get(1))) {
//                                                                                    iSecsHost.executeCommand("goto sidch4");
//                                                                                    List<String> result8 = iSecsHost.executeCommand("readm ch4y1");
//                                                                                    if (result8.size() == 2) {
//                                                                                        map.put("ch4y1", result8.get(0));
//                                                                                    }
//                                                                                }
//                                                                            }
//                                                                        }
//                                                                    }
//                                                                }
//                                                            }
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Get equip status error:" + e.getMessage());
//
//            }
//        }
////            returnPassport();
////        }
//        iSecsHost.executeCommand("playback gotoworkscreen.txt");
//        logger.info("monitormap:" + map.toString());
//        return map;
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
                result = iSecsHost.executeCommand("dos $dir " + "\"" + equipLocalRecipPath + File.separator + "*.csv" + "\" /b$");
                for (String recipeName : result) {
                    if (!"done".equals(recipeName) && !"".equals(recipeName)) {
                        equipRecipeList.add(recipeName.replace(".csv", ""));
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
        while (!this.isInterrupted()) {
            if (!isInitState) {
                getEquipRealTimeState();
                isInitState = true;
                break;
            }
        }
    }

    @Override
    public Object clone() {
        MarchFlexTRAKCDHost newEquip = new MarchFlexTRAKCDHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                // TODO:设置颜色坐标
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        //假设状态不变
                        //如果界面一直在main，每十分钟跳到control一次
                        if (getCheckIntervalTimeFlag()) {
                            result = iSecsHost.executeCommand("playback gotocontrol.txt");
                            getStatus();
                        }
                    } else if ("control".equals(result.get(0))) {
                        getStatus();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Get equip status error:" + e.getMessage(), e);
        } finally {
            Map map = new HashMap();
            map.put("EquipStatus", equipStatus);
            map.put("PPExecName", ppExecName);
            changeEquipPanel(map);
            return equipStatus;
        }
    }

    private void getStatus() {
        if (!"control".equalsIgnoreCase(iSecsHost.executeCommand("curscreen").get(0))) {
            return;
        }
        //读取四个点的颜色
        //灰色的按钮为0x8
        //List<String> ready = this.sendMsg2Equip("readrectcolor 751 115 755 118");
        List<String> run = this.sendMsg2Equip("readrectcolor 813 115 817 118");
        //List<String> stop = this.sendMsg2Equip("readrectcolor 876 115 883 118");
        //List<String> error = this.sendMsg2Equip("readrectcolor 937 115 945 118");
        //判断具体状态
        if (run != null && !run.isEmpty() && !run.get(0).contains("error") && !"done".equalsIgnoreCase(run.get(0))) {
            if (!run.get(0).contains("0x8")) {
                if (run.get(0).contains("ff")) {
                    equipStatus = "RUN";
                } else {
                    equipStatus = "SETUP";
                }
            } else {
                List<String> ready = this.sendMsg2Equip("readrectcolor 751 115 755 118");
                if (ready != null && !ready.isEmpty() && !ready.get(0).contains("error") && !"done".equalsIgnoreCase(ready.get(0))) {
                    if (!ready.get(0).contains("0x8")) {
                        if (ready.get(0).contains("0xff")) {
                            equipStatus = "READY";
                        } else {
                            equipStatus = "SETUP";
                        }
                    } else {
                        List<String> error = this.sendMsg2Equip("readrectcolor 937 115 945 118");
                        if (error != null && !error.isEmpty() && !error.get(0).contains("error") && !"done".equalsIgnoreCase(error.get(0))) {
                            if (!error.get(0).contains("0x8")) {
                                if (error.get(0).contains("0xff")) {
                                    equipStatus = "ERROR";
                                } else {
                                    equipStatus = "SETUP";
                                }
                            } else {
                                List<String> stop = this.sendMsg2Equip("readrectcolor 876 115 883 118");
                                if (stop != null && !stop.isEmpty() && !stop.get(0).contains("error") && !"done".equalsIgnoreCase(stop.get(0))) {
                                    if (!stop.get(0).contains("0x8")) {
                                        if (stop.get(0).contains("0xff")) {
                                            equipStatus = "STOP";
                                        } else {
                                            equipStatus = "SETUP";
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + ".csv", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".rcp 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        //this.deleteTempFile(recipeName);
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
                logger.error("目录清理时发生异常:" + e.getMessage(), e);
            }
        }
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

    public boolean startCheck() {
        if (!specialCheck()) {
            return false;
        }
        boolean pass = true;
        String checkRecultDesc = "";
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
            //刷新当前Recipe
            getEquipRealTimeStateNow();
            String trackInRcpName = deviceInfoExt.getRecipeName();
            if (!ppExecName.equals(trackInRcpName)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "已选程序与领料程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " 已选程序 " + ppExecName);
                pass = false;
                checkRecultDesc = "已选程序与领料程序不一致,设备被锁定！请联系ME处理！领料程序:" + trackInRcpName + " 已选程序:" + ppExecName;
            }
        }
        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
        if (execRecipe == null) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理！");
            checkRecultDesc = "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理!";
            pass = false;
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.StartCheckWI");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("recipeName", ppExecName);
        mqMap.put("EquipStatus", equipStatus);
        mqMap.put("lotId", lotId);
        if (pass) {
            List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
            List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(deviceInfoExt.getRecipeId(), deviceCode, equipRecipeParas, "");
            try {
                String eventDesc = "";
                if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                    this.stopEquip();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机参数检查未通过!");
                    for (RecipePara recipePara : recipeParasdiff) {
                        eventDesc = "开机Check参数异常参数编码为:" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为:" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                        checkRecultDesc = checkRecultDesc + eventDesc;
                    }
                    //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
                    sendMessage2Eqp("Recipe parameter error,start check failed!The equipment has been stopped!");
                    pass = false;
                } else {
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
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查条件不具备,检查未通过!");
        }
        mqMap.put("eventDesc", checkRecultDesc);
        GlobalConstants.C2SLogQueue.sendMessage(mqMap);
        return pass;
    }


}
