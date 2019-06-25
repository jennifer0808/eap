package cn.tzauto.octopus.isecsLayer.equipImpl.hanmi;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampressHost extends EquipModel {

    private Logger logger = Logger.getLogger(CampressHost.class);

    //解析recipe的小程序
    private ISecsHost recipeHost;

    //visoin部分OCR
    private ISecsHost visionHost;

    private String visionRecipePath = null;

    public CampressHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    /**
     * 增加对vision和recipe小程序的host初始化操作
     */
    @Override
    public void initialize() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo visionInfo = deviceService.getDeviceInfoByDeviceCode(deviceCode + "-V").get(0);
        Map<String, DeviceType> deviceTypeDic = deviceService.getDeviceTypeMap();
        sqlSession.close();
        if(visionInfo == null) {
            logger.info("未配置vision信息");
            throw new RuntimeException("未配置vision信息");
        }
        //设置vision的recipe路径
        visionRecipePath = deviceTypeDic.get(visionInfo.getDeviceType()).getSmlPath();
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        visionHost = new ISecsHost(visionInfo.getDeviceIp(),visionInfo.getDevicePort(),visionInfo.getDeviceType(),deviceCode+"-V");
        recipeHost = new ISecsHost(remoteIPAddress, "12005", deviceType, deviceCode);
        iSecsHostList.add(iSecsHost);
        iSecsHostList.add(visionHost);
        if (iSecsHost.isConnect && visionHost.isConnect) {
            this.equipState.setCommOn(true);
            commState = 1;
        } else {
            this.equipState.setCommOn(false);
            commState = 0;
        }
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand("read recipeName");
                if (list != null && !list.isEmpty()) {
                    ppExecName = list.get(0);
                }
            } catch (Exception e) {
                logger.info("get equip current recipe name failed" + e);
            } finally {
                Map<String, String> map = new HashMap<>();
                map.put("PPExecName", ppExecName);
                changeEquipPanel(map);
            }
        }
        return ppExecName;
    }

    @Override
    public String startEquip() {
        return null;
    }

    @Override
    public String pauseEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback pause.txt");
        }
        return "0";
    }

    @Override
    public String stopEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback stop.txt");
        }
        return "0";
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        if (!exportRecipe2File(recipeName)) {
            map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,导出失败.");
            map.put("rcpAnalyseSucceed", "N");
            return map;
        }
        Map<String, String> recipeMap = getRecipeNoMapping();
        String nos = recipeMap.get(recipeName);
        if (nos == null) {
            map.put("uploadResult", "上传recipe失败，recipe不存在！");
            map.put("rcpAnalyseSucceed", "N");
            return map;
        }
        String clientFtpAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        //上传vision部分recipe
        String vsionRelativePath = getVisionRecipeRelativePath(recipeName);
        String visionAbsolutePath = getVisionRecipeAbsolutePath(recipeName);
        //上传handler部分recipe
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"put $" + recipeName + ".rcp$\"";
        //TODO setting vision recipe path
        String visionFileCmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + visionRecipePath + "\" \"" + vsionRelativePath + "\" \"put dbindex.inf\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(cmd);
                if (resultList != null && !resultList.isEmpty()) {
                    for (String str : resultList) {
                        if (str != null && str.contains("success")) {
                            //解析recipe,将参数保存至db
                            map.put("recipe", setRecipe(recipeName));
                            map.put("deviceCode", deviceCode);
                            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                            RecipeService recipeService = new RecipeService(sqlSession);
                            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                            sqlSession.close();
                            //map.put("recipeFTPPath", getHostUploadFtpRecipePath(setRecipe(recipeName)));
                            //map.put("recipeParaList", recipeParaList);
                            break;
                        }
                        if (str != null && str.contains("error")) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                            map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                logger.info("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！" + ex);
                map.put("rcpAnalyseSucceed", "N");
                return map;
            }
        }
        //upload dbindex.inf
        String[] NoArray = nos.split(",");
        String groupNo = NoArray[0];
        String deviceNo = NoArray[1];
        String visoionNo = NoArray[2];
        Map<String, String> visionNameMapping;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            visionHost.executeCommand(visionFileCmd);
            //从dbindex.inf文件读取信息
            visionNameMapping = new RecipeFileHandler() {
                @Override
                public Map<String, String> handler(File file) {
                    Map<String, String> map = new HashMap<>();
                    try (
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
                    ) {
                        String line = bufferedReader.readLine();
                        String deviceNo = "";
                        String pre = "";
                        boolean f = false;
                        while (line != null) {
                            if (line.matches("^\\[\\d{3,}_\\d{3,}\\]$")) {
                                deviceNo = line.substring(1, line.indexOf("_")).replace("0", "");
                                f = false;
                                pre = line;
                            }
                            if ("[TOPGROUPNAME]".equals(pre)) {
                                map.put("topGroupName", line.split("=")[1]);
                            }
                            if ("[GROUPNAME]".equals(pre)) {
                                String[] strs = line.split("=");
                                map.put("groupName" + strs[0].replace("0", ""), strs[1]);
                                f = true;
                            }
                            if (line.contains("CAM") && !line.contains("CAMACTIVE")) {
                                String[] strs = line.split("=");
                                String visionNO = strs[0];
                                String fileName = strs[1];
                                map.put(deviceNo + visionNO, fileName);
                            }
                            if (!f) {
                                pre = line;
                            }
                            line = bufferedReader.readLine();
                        }
                    } catch (Exception e) {
                        logger.info("上传dbindex.inf失败" + e);
                    }
                    return map;
                }
            }.handler(new File(visionAbsolutePath + "dbindex.inf"));

        }
        String groupName = visionNameMapping.get("topGroupName");
        String deviceName = visionNameMapping.get("groupName" + deviceNo);
        String visionName = visionNameMapping.get(deviceNo + "CAM" + visoionNo);
        createDbIndexFile(recipeName, groupName, deviceName, visionName);

        String visionCmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + visionRecipePath + "\" \"" + vsionRelativePath + "\"";
        //分多次上传，单次有问题
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String[] categoris = {".BGA", ".INLET", ".XMARK"};
            for (String category : categoris) {
                iSecsHost.executeCommand(visionCmd + " \"mput $" + visionName + category + "$ $" + visionName + category + ".ldb$ $" + visionName + category + ".roi$\"");
            }
        }
        //压缩上传后的文件
        try {
            ZipUtil.toZip(true, new File(clientFtpAbsolutePath).listFiles(), new FileOutputStream(new File(clientFtpAbsolutePath + recipeName + ".zip")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logger.info(deviceCode + "压缩recipe文件失败");
            map.put("rcpAnalyseSucceed", "N");
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
        String recipeName = recipe.getRecipeName();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        try {
            File zipFile = new File(clientFtpRecipeAbsolutePath + recipeName + ".zip");
            ZipUtil.unZip(zipFile, clientFtpRecipeAbsolutePath);
            zipFile.delete();
        } catch (IOException e) {
            logger.info("解压recipe：" + recipeName + "失败" + e);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipeName + " 失败.");
            return "下载失败";
        }
        //下载handler部分的recipe
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"get $" + recipeName + ".rcp$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand(cmd);
        }
        if (!improtRecipeFromFile(recipeName)) {
            return "下载失败";
        }
        //下载vision部分的recipe
        String visionCmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + visionRecipePath + "\" \"" + getVisionRecipeRelativePath(recipeName) + "\" \"mget *.*\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            visionHost.executeCommand(visionCmd);
        }
        return "0";
    }


    @Override
    public String deleteRecipe(String recipeName) {
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback setup.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback deviceChange.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback selectRcp.txt");
        }
        // wait for dialog
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback clickOK.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback login.txt");
        }
        //change rcp finished
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback importOk.txt");
        }
        //gotoNain
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotoMain.txt");
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
                    equipStatus = results.get(0);
                    if (equipStatus.equals("any")) {
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
        CampressHost newHost = new CampressHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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

    /**
     * 导出recipe（db文件）为普通文件
     *
     * @param recipeName recipe
     * @return
     */
    private boolean exportRecipe2File(String recipeName) {
        getRecipeList();
        int start = recipeName.indexOf('-');
        int end = recipeName.lastIndexOf('-');
        String groupId = recipeName.substring(0, start);
        String deviceId = recipeName.substring(start + 1, end);
        String visionId = recipeName.substring(end + 1);
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                // export group device vission filePath
                List<String> res = recipeHost.executeCommand("Export " + groupId + " " + deviceId + " " + visionId + " " + equipRecipePath + File.separator + recipeName + ".rcp");
                for (String str : res) {
                    if (str != null && str.contains("done")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "导出recipe失败" + e);
                logger.info("export recipe failed" + e);
            }
        }
        return false;
    }


    /**
     * 从文件导入recipe
     *
     * @param recipeName recipe
     * @return
     */
    private boolean improtRecipeFromFile(String recipeName) {
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = recipeHost.executeCommand("Import \"" + equipRecipePath + File.separator + recipeName + ".rcp\"");
                for (String str : res) {
                    if (str != null && str.contains("done")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "导入recipe失败" + e);
                logger.info("import recipe failed" + e);
            }
        }
        return false;
    }

    /**
     * 删除导出的recipe文件
     *
     * @param recipeName
     */
    private boolean delRecipeFile(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("dos $del /q \"" + equipRecipePath + File.separator + recipeName + ".rcp\"$");
                return true;
            } catch (Exception e) {
                logger.info("delete recipe file failed" + e);
                return false;
            }
        }
    }

    public void createDbIndexFile(String recipeName, String groupName, String deviceName, String visionName) {
        List<String> lines = new ArrayList<>();
        lines.add("[TOPGROUPNAME]");
        lines.add("001=" + groupName);
        lines.add("[GROUPNAME]");
        lines.add("001=" + deviceName);
        lines.add("[001_001]");
        lines.add("PKGNAME=" + deviceName);
        lines.add("CAM1=" + visionName);
        lines.add("CAMACTIVE1=1");
        try {
            FileUtil.writeFileWithlines(lines, getVisionRecipeAbsolutePath(recipeName) + "dbindex.inf");
        } catch (IOException e) {
            logger.info("create dbindex file failed" + e);
        }
    }

    private List<String> getRecipeList() {
        List<String> recipes = new ArrayList<>();
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = recipeHost.executeCommand("Getlist");
                if (results != null && !results.isEmpty()) {
                    String recipesJson = results.get(0);
                    JSONObject jsonObject = JSON.parseObject(recipesJson);
                    JSONArray jsonArray = jsonObject.getJSONArray("recipes");
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject recipe = jsonArray.getJSONObject(i);
                        recipes.add(recipe.getString("recipename"));
                    }
                }
            } catch (Exception e) {
                logger.info("get recipe list failed." + e);
            }
        }
        return recipes;
    }

    public Map<String, String> getRecipeNoMapping() {
        Map<String, String> recipeMap = new HashMap<>();
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            List<String> results = recipeHost.executeCommand("Getlist");
            if (results != null && !results.isEmpty()) {
                String recipesJson = results.get(0);
                JSONObject jsonObject = JSON.parseObject(recipesJson);
                JSONArray jsonArray = jsonObject.getJSONArray("recipes");
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject recipe = jsonArray.getJSONObject(i);
                    recipeMap.put(recipe.getString("recipename"), recipe.getString("groupno") + "," + recipe.getString("deviceno") + "," + recipe.getString("visionno"));
                }
            }
        }
        return recipeMap;
    }

    public String getVisionRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/vision/";
    }

    public String getVisionRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getVisionRecipeRelativePath(recipeName);
        File recipeDir = new File(filePath);
        if (!recipeDir.exists()) {
            recipeDir.mkdirs();
        }
        return filePath;
    }

    private String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
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

    //handler跳转主界面
    private void gotoMain_Handler() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotoMain.txt");
        }
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
