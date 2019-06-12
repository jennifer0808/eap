package cn.tzauto.octopus.isecsLayer.equipImpl.vis;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class T6EVOHost extends EquipModel {

    private Logger logger = Logger.getLogger(T6EVOHost.class);

    public T6EVOHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        List<String> results = null;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                results = iSecsHost.executeCommand("read recipeName");
                if (results != null && !results.isEmpty()) {
                    String res = results.get(0);
                    //排除这个异常情况
                    if (res != null && !"T6 Evo - [VisDynamicsTray1]".equals(res) && !"done".equals(res)) {
                        ppExecName = res;
                    }
                }
            } catch (Exception e) {
                logger.info("获取recipeName失败", e);
            }
        }
        Map<String, String> map = new HashMap<>();
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("playback pauseEquip.txt");
                if (results != null && !results.isEmpty()) {
                    if ("done".equals(results.get(0))) {
                        return "0";
                    }
                }
            } catch (Exception e) {
                logger.info("锁机失败" + e);
            }
        }
        return "锁机失败";
    }

    @Override
    public String stopEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("playback stopEquip.txt");
                if (results != null && !results.isEmpty()) {
                    if ("done".equals(results.get(0))) {
                        return "0";
                    }
                }
            } catch (Exception e) {
                logger.info("停机失败" + e);
            }
        }
        return "停机失败";
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        //questions
        //1.查看新建rcp后文件情况
        //2.当前rcp中有些version没有对应的文件
        //3.version部分rcp的解析
        //version怎么处理
        Map<String, Object> map = new HashMap<>();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String cleintRelativeFtpVisionPath = getClientFtpRecipeRelativePathVersion(recipeName);
        /**
         * 创建目录
         */
        getClientFtpRecipeAbsolutePath(recipeName);
        getClientFtpRecipeAbsolutePathVersionInPocket(recipeName);
        getClientFtpRecipeAbsolutePathVersionGangBot(recipeName);
        getClientFtpRecipeAbsolutePathVersionInTray(recipeName);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"put " + recipeName + ".pkg\"";
        String cmd_version;
        String cmd_version2;
        String cmd_version3;
        String cmd_version4;
        String cmd_version5;
        if ("E4400-1113".equals(deviceCode)) {
            cmd_version = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\InPocket\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InPocket" + "\" \"mput " + recipeName + ".*\"";
            cmd_version2 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\GangBot\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "GangBot" + "\" \"mput " + recipeName + "*.*\"";
            cmd_version3 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\InTray\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InTray" + "\" \"mput " + recipeName + ".*\"";
            cmd_version4 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\InPocket\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InPocket" + "\" \"put " + recipeName + "_T1.dev\"";
            cmd_version5 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\GnagBot\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "GnagBot" + "\" \"mput " + recipeName + "_G1.fbga " + recipeName + "_G2.fbga\"";
        } else {
            cmd_version = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\InPocket\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InPocket" + "\" \"mput " + recipeName + ".*\"";
            cmd_version2 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\GangBot\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "GangBot" + "\" \"mput " + recipeName + "*.*\"";
            cmd_version3 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\InTray\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InTray" + "\" \"mput " + recipeName + ".*\"";
            cmd_version4 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\InPocket\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InPocket" + "\" \"put " + recipeName + "_T1.dev\"";
            cmd_version5 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\GnagBot\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "GnagBot" + "\" \"mput " + recipeName + "_G1.fbga " + recipeName + "_G2.fbga\"";
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand(cmd);
                iSecsHost.executeCommand(cmd_version);
                iSecsHost.executeCommand(cmd_version2);
                iSecsHost.executeCommand(cmd_version3);
                iSecsHost.executeCommand(cmd_version4);
                iSecsHost.executeCommand(cmd_version5);
                for (String s : list) {
                    if (s != null && s.contains("success")) {
                        /** zip version file*/
                        ZipUtil.toZip(true, new File(getClientFtpRecipeAbsolutePath(recipeName)).listFiles(), new FileOutputStream(getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".zip"));
                        map.put("recipe", setRecipe(recipeName));
                        map.put("deviceCode", deviceCode);
                        String recipePath = getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".pkg";
                        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                        sqlSession.close();
                        map.put("recipeParaList", RecipeUtil.transfer2DB(null, new File(recipePath), recipeTemplates, false));
                    }
                    if (s != null && s.contains("error")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！" + e);
            }
        }
        return map;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //压缩文件后的目录
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        //String clientZipFilePath = GlobalConstants.localRecipePath + GlobalConstants.ftpPath + recipeName ;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientAbsoluteFtpPath + recipeName + ".zip", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            deleteTempFile(recipeName);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String downLoadResult = "1";
        String recipeName = recipe.getRecipeName();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
        //从server下载recipe到工控机
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.");
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        try {
            ZipUtil.unzipByApache(getClientFtpRecipeAbsolutePathVersion(recipeName) + recipeName + ".zip", clientFtpRecipeAbsolutePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String cleintRelativeFtpVisionPath = getClientFtpRecipeRelativePathVersion(recipeName);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"get " + recipeName + ".pkg\"";
        String cmd_version;
        String cmd_version2;
        String cmd_version3;
        if ("E4400-1113".equals(deviceCode)) {
            cmd_version = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\InPocket\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InPocket" + "\" \"mget " + recipeName + "*.*\"";
            cmd_version2 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\GangBot\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "GangBot" + "\" \"mget " + recipeName + "*.*\"";
            cmd_version3 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.1.2\\ME2100\\Package\\InTray\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InTray" + "\" \"mget " + recipeName + "*.*\"";
        } else {
            cmd_version = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\InPocket\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InPocket" + "\" \"mget " + recipeName + "*.*\"";
            cmd_version2 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\GangBot\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "GangBot" + "\" \"mget " + recipeName + "*.*\"";
            cmd_version3 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + "\\\\192.168.5.2\\ME2100\\Package\\InTray\\" + "\" \"" + cleintRelativeFtpVisionPath + File.separator + "InTray" + "\" \"mget " + recipeName + "*.*\"";
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand(cmd);
                iSecsHost.executeCommand(cmd_version);
                iSecsHost.executeCommand(cmd_version2);
                iSecsHost.executeCommand(cmd_version3);
                for (String str : result) {
                    if (str.contains("success")) {
                        downLoadResult = "0";
                        break;
                    }
                    if (str.contains("error")) {
                        downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                        break;
                    }
                }
            } catch (Exception ex) {
                logger.info("下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启." + ex);
                downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
        }
        return downLoadResult;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        String cmd = "dos $del /q \"" + equipRecipePath + File.separator + recipeName + ".pkg \"$";
        String visionIP = "192.168.1.2";
        if (!"E4400-1113".equals(deviceCode)) {
            visionIP = "192.168.5.2";
        }
        String delCmd1 = "dos $del /q \"" + "\\\\" + visionIP + "\\ME2100\\Package\\InPocket\\" + recipeName + ".* \"$";
        String delCmd2 = "dos $del /q \"" + "\\\\" + visionIP + "\\ME2100\\Package\\InPocket\\" + recipeName + "_T1.dev \"$";
        String delCmd3 = "dos $del /q \"" + "\\\\" + visionIP + "\\ME2100\\Package\\GangBot\\" + recipeName + ".* \"$";
        String delCmd4 = "dos $del /q \"" + "\\\\" + visionIP + "\\ME2100\\Package\\GangBot\\" + recipeName + "_G1.* \"$";
        String delCmd5 = "dos $del /q \"" + "\\\\" + visionIP + "\\ME2100\\Package\\GangBot\\" + recipeName + "_G2.* \"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand(delCmd1);
                iSecsHost.executeCommand(delCmd2);
                iSecsHost.executeCommand(delCmd3);
                iSecsHost.executeCommand(delCmd4);
                iSecsHost.executeCommand(delCmd5);
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                return "删除失败";
            }
        }
        return "删除失败";
    }

    @Override
    public String selectRecipe(String recipeName) {
        /** close lot */
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("playback close_lot.txt");
            } catch (Exception e) {
                logger.info("结批操作失败" + e);
                return "failed";
            }
        }
        //延迟0.5s，等待确认框
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.info("Thread sleep 0.5s failed");
            return "failed";
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("playback close_lot_OK.txt");
            } catch (Exception e) {
                logger.info("结批确认失败" + e);
                return "failed";
            }
        }
        /** open select recipe page and write recipeName and select*/
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("playback openSR.txt");
                //延时2s输入recipeName
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    logger.info("Thread sleep 2s failed");
                    return "failed";
                }
                iSecsHost.executeCommand("write recipe " + recipeName);
                iSecsHost.executeCommand("playback select_recipe_ok.txt");
            } catch (Exception e) {
                logger.info("选中recipe失败" + e);
                return "failed";
            }
        }
        return "0";
    }

    @Override
    public Map getEquipRealTimeState() {
        Map map = new HashMap();
        map.put("PPExecName", getCurrentRecipeName());
        map.put("EquipStatus", getEquipStatus());
        map.put("controlState", controlState);
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
            List<String> recipeList = getRecipeList();
            eppd.put("eppd", recipeList);
        } catch (Exception e) {
            logger.info("设备:" + deviceCode + "repipe列表获取失败！" + e);
        }
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("curscreen");
                if (results != null && !results.isEmpty()) {
                    String curscreen = results.get(0);
                    if ("run".equals(curscreen) || "run1".equals(curscreen)) {
                        equipStatus = "Run";
                    } else if ("idle".equals(curscreen) || "idle1".equals(curscreen) || "ready".equals(curscreen) || "ready1".equals(curscreen) || "ready2".equals(curscreen)) {
                        equipStatus = "Idle";
                    } else {
                        equipStatus = "Error";
                    }
                }
            } catch (Exception e) {
                logger.info("获取状态失败" + e);
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
    public Object clone() {
        T6EVOHost newHost = new T6EVOHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newHost.startUp = startUp;
        this.clear();
        return newHost;
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
    public List<String> getEquipAlarm() {
        return null;
    }

    private List<String> getRecipeList() {
        ArrayList<String> recipeList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand("dos $dir " + "\""
                        + this.equipRecipePath + File.separator + "*.pkg" + "\" /b$");
                for (String recipe : resultList) {
                    if (!"".equals(recipe) && !"done".equals(recipe) && !".pkg".equals(recipe) && !"DEFAULT.pkg".equals(recipe)) {
                        recipeList.add(recipe.replace(".pkg", ""));
                    }
                }
            } catch (Exception ex) {
                logger.info("获取recipe列表失败" + ex);
            }
        }
        return recipeList;
    }

    private String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    private String getClientFtpRecipeRelativePathVersion(String recipeName) {
        String filePath = getClientFtpRecipeRelativePath(recipeName) + "version/";
        return filePath;
    }

    private String getClientFtpRecipeAbsolutePathVersion(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName) + "version/";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return filePath;
    }

    private String getClientFtpRecipeAbsolutePathVersionInPocket(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName) + "version/InPocket";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return filePath;
    }

    private String getClientFtpRecipeAbsolutePathVersionGangBot(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName) + "version/GangBot";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return filePath;
    }

    private String getClientFtpRecipeAbsolutePathVersionInTray(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName) + "version/InTray";
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return filePath;
    }

    private String getClientFtpRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File recipeDir = new File(filePath);
        if (!recipeDir.exists()) {
            recipeDir.mkdirs();
        }
        return filePath;
    }

    /**
     * 获取recipe在服务端的路径
     *
     * @param recipe
     * @return
     */
    private String getHostDownloadFtpRecipeFilePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String downloadFullFilePath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        sqlSession.close();
        return downloadFullFilePath;
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
