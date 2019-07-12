package cn.tzauto.octopus.isecsLayer.equipImpl.cohu;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.cohu.NY20POJO;
import cn.tzauto.octopus.isecsLayer.resolver.cohu.NY20Resolver;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.util.*;

/**
 * Created by wj_co
 */
public class NY20Host extends EquipModel {
    private Logger logger = Logger.getLogger(NY20Host.class);

    private long getRecipeAndStatusTime = new Date().getTime();

    private String shareVisionPath = "\\\\127.0.0.1\\Generic\\";//TODO："\\\\192.168.5.2\\ME2100\\Package\\InPocket\\"

    public NY20Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    /**
     * 获取当前recipeName
     * @return
     */
    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand("read recipeName");
                if (resultList != null && !resultList.isEmpty()) {
                    ppExecName = resultList.get(0).trim();
                }
            } catch (Exception e) {
                logger.info("NY20Host getCurrentRecipeName()-获取recipeName失败" + e);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }


    @Override
    public String startEquip() {
        String startResult = "Start failed";
        try {
            iSecsHost.executeCommand("playback start.txt");
            Thread.sleep(1000);
            if ("Run".equals(getEquipStatus())) {
                startResult = "0";
            }
        } catch (Exception e) {
            logger.info("开机失败，" + e);
            startResult = "start failed";
        } finally {
            return startResult;
        }
    }

    @Override
    public String pauseEquip() {
        return null;
    }

    @Override
    public String stopEquip() {
        String stopResult = "停机失败,当前状态无法执行停机";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                try {
                    iSecsHost.executeCommand("playback stop.txt");
                    Thread.sleep(1000);
                    if ("Idle".equals(getEquipStatus())) {
                        return "0";
                    }
                } catch (Exception e) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "锁机失败,当前状态无法执行锁机！");
                    stopResult = "锁机失败！";
                }
            } else {
                //打日志
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
                stopResult = "未设置锁机！";
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
        Map<String, Object> map = new HashMap<>();
        Recipe recipe = setRecipe(recipeName);
        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientFtpRecipeRelativePath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \" put " + recipeName + ".txt\"";
        String cmd_vision = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + shareVisionPath + "\" \"" + clientFtpRecipeRelativePath + "\" \" put " + recipeName + ".xml\"";
        String cmd_del = "dos $del /q \"" + clientFtpRecipeRelativePath + File.separator + recipeName + ".xml \"$";
        List<RecipePara> recipeParaList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(cmd);
                if (!resultList.isEmpty() && resultList.get(0).contains("success")) {
                    //1.共享文件上传至工控机
                    List<String> visionResult= iSecsHost.executeCommand(cmd_vision);
                    if(!visionResult.isEmpty() && visionResult.get(0).contains("success")){
                    //2.解析recipe
                    try{
                        String filePath=clientFtpRecipeAbsolutePath+recipeName+".xml";
                        Map<String, NY20POJO> paraMap = NY20Resolver.getParaFromVision(filePath,recipeName);
                            if(paraMap!=null && !paraMap.isEmpty()){
                               recipeParaList = NY20Resolver.transferFromDB(deviceType);
                            }else{
                                logger.error("解析recipe时出错,recipe文件不存在!");
                            }
                    }catch (Exception e){
                        logger.error("解析recipe时出错!", e);
                        e.printStackTrace();
                    }
                    }
                    //3.删除工控-该路径下.xml
                    iSecsHost.executeCommand(cmd_del);

                    map.put("recipe", setRecipe(recipeName));
                    map.put("deviceCode", deviceCode);
                    map.put("recipeFTPPath", hostUploadFtpRecipePath);
                    map.put("recipeParaList", recipeParaList);
                }
                if (!resultList.isEmpty() && resultList.get(0).contains("error")) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！" + e);
            }
        }

        return map;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String downloadResult = "1";
        String recipeName = recipe.getRecipeName();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpIp = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String clientFtpRecipeRelativePath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        String ftpcmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"get " + recipeName + ".txt \"";

        //从server下载到工控机
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpIp, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".txt", hostDownloadFtpRecipeFilePath, ftpIp, ftpPort, ftpUser, ftpPwd);
            }
        }

        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(ftpcmd);
                for (String s : resultList) {
                    if (s != null && s.contains("success")) {
                        downloadResult = "0";
                        break;
                    }
                    if (s != null && s.contains("error")) {
                        downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                        break;
                    }
                }

            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "下载" + recipeName + ".txt失败！" + e);
                downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
        }
        return downloadResult;
    }


    /**
     * 工控机上传至ftp服务端(ftp over)
     *
     * @param localRcpPath
     * @param remoteRcpPath
     * @param recipe
     * @return
     */
    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        String clientRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientRecipeAbsolutePath + recipeName + ".txt", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }


    public String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    public String getClientFtpRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return filePath;
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
     * 获取服务端下载FtpRecipe路径
     * hostDownloadFtpRecipeFilePath：
     * /RECIPE/A6/DieAttach/FI7300/Engineer/FI7300_0001/QFN-JCET-A-FCQFN-4X4-25L-T0.755/QFN-JCET-A-FCQFN-4X4-25L-T0.755_V1.txt
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


    //delete Eqp recipe
    @Override
    public String deleteRecipe(String recipeName) {
        List<String> recipeList = (List<String>) getEquipRecipeList().get("eppd");
        String cmd = "dos $del /q \"" + equipRecipePath + File.separator + recipeName + ".txt \"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                if (!recipeList.contains(recipeName)) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                } else {
                    List<String> results = iSecsHost.executeCommand(cmd);
                    for (String s : results) {
                        if ("done".equals(s)) {
                            return "删除成功";
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                return "删除失败";
            }
        }

        return "删除失败";
    }


    /**
     * 判断是否要结束结批
     *
     * @return
     */
    public Boolean IsEndLot() {
        Boolean isEndLot = false;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultLot = iSecsHost.executeCommand("read endLot");
                if (resultLot != null && !resultLot.isEmpty()) {
                    String str = resultLot.get(0).trim();
                    if (!"EndLot".equals(str)) {
                        isEndLot = false;
                    } else {
                        isEndLot = true;
                    }
                }
            } catch (Exception e) {
                logger.info("endLot failure" + e);
            }
        }
        return isEndLot;
    }

    /**
     * 判断是否开始结批
     *
     * @return
     */
    public Boolean IsStartLot() {
        Boolean IsStartLot = false;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultLot = iSecsHost.executeCommand("read startLot");
                if (resultLot != null && !resultLot.isEmpty()) {
                    String str = resultLot.get(0).trim();
                    if (!"StartLot".equals(str)) {
                        IsStartLot = false;
                    } else {
                        IsStartLot = true;
                    }
                }
            } catch (Exception e) {
                logger.info("StartLot failure" + e);
            }
        }
        return IsStartLot;
    }

    /**
     * 1.登录并判断
     * 2.登录后选择操作
     * 3.判断是否结束结批，否则结批
     * 4.写入批次
     * 5.点击开始结批
     * 6.写入recipe
     * 7.rcp_ok
     * 8.gotomain
     *
     * @param recipeName
     * @return
     */
    @Override
    public String selectRecipe(String recipeName) {
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> loginList = iSecsHost.executeCommand("playback login.txt");

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.info("Thread sleep 0.5s failed:" + e);
                    return "failed";
                }
                if ("done".equals(loginList.get(0))) {
                    List<String> isLoginList = iSecsHost.executeCommand("read isLogin");
                    //判断用户是否登录成功
                    if ("g".equals(isLoginList.get(0))) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            logger.info("Thread sleep 0.5s failed:" + e);
                            return "failed";
                        }

                        try {
                            iSecsHost.executeCommand("playback selectOP.txt");
                        } catch (Exception e) {
                            logger.info("select lot failure:" + e);
                            return "failed";
                        }

                        if (IsEndLot() || IsStartLot()) {
                            try {
                                iSecsHost.executeCommand("playback endLot.txt");
                            } catch (Exception e) {
                                logger.info("endLot failure:" + e);
                                return "failed";
                            }
                            try {
                                iSecsHost.executeCommand("replay writelot.exe");
                            } catch (Exception e) {
                                logger.info("writelot failure:" + e);
                                return "failed";
                            }

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                logger.info("Thread sleep 0.5s failed:" + e);
                                return "failed";
                            }

                            try {
                                iSecsHost.executeCommand("playback startLot.txt");
                            } catch (Exception e) {
                                logger.info("startLot failure:" + e);
                                return "failed";
                            }

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                logger.info("Thread sleep 0.5s failed:" + e);
                                return "failed";
                            }
                            try {
                                iSecsHost.executeCommand("write writercp " + recipeName);
                            } catch (Exception e) {
                                logger.info("write failure:" + e);
                                return "failed";
                            }

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                logger.info("Thread sleep .0.5s failed:" + e);
                                return "failed";
                            }

                            try {
                                iSecsHost.executeCommand("playback rcp_ok.txt");
                                iSecsHost.executeCommand("playback gotomain.txt");
                            } catch (Exception e) {
                                logger.info("read failure:" + e);
                                return "failed";
                            }

                            //结束和生产批次同时灰掉，则直接写入writercp
                        } else if (!IsEndLot() && !IsStartLot()) {
                            try {
                                iSecsHost.executeCommand("playback selrcp.txt");
                                iSecsHost.executeCommand("write writercp " + recipeName);
                            } catch (Exception e) {
                                logger.info("selectRCP or write failure:" + e);
                                return "failed";
                            }
                            try {
                                iSecsHost.executeCommand("playback rcp_ok.txt");
                                iSecsHost.executeCommand("playback gotomain.txt");
                            } catch (Exception e) {
                                logger.info("read failure:" + e);
                                return "failed";
                            }

                        }
                    } else {
                        logger.info("NY20设备-用户登录失败！");
                    }

                } else {
                    logger.info("NY20Host selectRecipe()-登录操作有误！");
                }

                //查询recipe是否有更改成功
                String currentRecipeName = getCurrentRecipeName();
                if (currentRecipeName.equals(recipeName)) {
                    return "0";
                }
            }
            return "failed";
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return "failed";
        }

    }

    @Override
    public Map getEquipRealTimeState() {
        Map map = new HashMap();
        map.put("ppExecName", getCurrentRecipeName());
        map.put("EquipStatus", getEquipStatus());
        map.put("ControlState", controlState);
        return map;
    }

    @Override
    public Map getEquipMonitorPara() {
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        Map<String, Object> eppd = new HashMap<>();
        try {
            eppd.put("eppd", getRecipeList());
        } catch (Exception e) {
            logger.info("NY20Host getEquipRecipeList()-获取recipe列表失败" + e);
        }
        return eppd;
    }


    public List<String> getRecipeList() {
        ArrayList<String> recipeList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("dos $dir " + "\"" +
                        this.equipRecipePath + File.separator + "*.txt" + "\" /b$");

                for (String recipe : results) {
                    if (!"".equals(recipe) && !"done".equals(recipe)) {
                        recipeList.add(recipe.replace(".txt", ""));
                    }
                }

            } catch (Exception e) {
                logger.info("NY20Host getRecipeList()-获取recipe列表失败" + e);
            }

        }
        return recipeList;
    }


    /**
     * 获得间隔时间 10min=600000ms
     * @return
     */
    private boolean getCheckIntervalTimeFlag() {
        long nowDate = new Date().getTime();
        long intervalTime = new Date().getTime() - getRecipeAndStatusTime;
        if (intervalTime > 2000) {//10 * 60 * 1000
            getRecipeAndStatusTime = nowDate;
            return true;
        } else {
            return false;
        }
    }


    /**
     *获取设备状态
     * 状态不稳定，10min后跳一次。
     * 读取AGUL总数
     * @return
     */
    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
               List<String> colorList = iSecsHost.executeCommand("readrectcolor 326 116 330 130");
                if(!colorList.isEmpty() && !colorList.get(0).contains("error") && !"done".equalsIgnoreCase(colorList.get(0)) ){
                    //TODO:ocr适配
                    if(!"0xffffff".equals(colorList.get(0))){
                        equipStatus = "Ready";
                        if(getCheckIntervalTimeFlag()){
                            iSecsHost.executeCommand("playback test.txt");
                        }
                    }
                    List<String> screenResult = iSecsHost.executeCommand("read AGUL");
                    //TODO:加一个判断条件，判断是否为数字
                    if (screenResult != null && !screenResult.isEmpty() && !screenResult.get(0).contains("error")  ) {
                        if("0".equals(screenResult.get(0))){
                            equipStatus = "Idle";
                        }else if(Integer.valueOf(screenResult.get(0))>0){
                            equipStatus = "Run";
                        }else{
                            equipStatus = "Error";
                        }
                    }else{
                        equipStatus = "Error";
                    }
                }else{
                    logger.info("read status color error!");
                }

            } catch (Exception e) {
                logger.info("NY20Host getEquipStatus()-获取状态失败" + e);
            } finally {
                Map<String, String> map = new HashMap<>();
                map.put("PPExecName", ppExecName);
                map.put("EquipStatus", equipStatus);
                changeEquipPanel(map);
            }
        }
        return equipStatus;
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
        NY20Host newHost = new NY20Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newHost.startUp = startUp;
        this.clear();
        return newHost;
    }


    //Eqp alarm
    @Override
    public List<String> getEquipAlarm() {
        return null;
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
