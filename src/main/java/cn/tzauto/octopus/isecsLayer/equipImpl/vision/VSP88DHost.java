package cn.tzauto.octopus.isecsLayer.equipImpl.vision;

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
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
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

public class VSP88DHost extends EquipModel {

    private Logger logger = Logger.getLogger(VSP88DHost.class);

    private ISecsHost recipeHost;

    private Map<String, String> recipeFileMap = new HashMap<>();

    /**
     * 导出DB文件路径
     */
    private static final String exportPath = "D:\\VSPExport";

    public VSP88DHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        recipeHost = new ISecsHost(remoteIPAddress, "12005", deviceType, deviceCode);
        iSecsHostList.add(iSecsHost);
        iSecsHostList.add(recipeHost);
        if (iSecsHost.isConnect) {
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
            List<String> recipeList = iSecsHost.executeCommand("read recipeName");
            if (recipeList != null && !recipeList.isEmpty()) {
                ppExecName = recipeList.get(0);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("playback start.txt");
                Thread.sleep(1000);
                if ("Run".equals(getEquipStatus())) {
                    return "0";
                }
            } catch (Exception e) {
                logger.info("开机失败，" + e);
            }
        }
        return "start failed";
    }

    @Override
    public String pauseEquip() {
        return null;
    }

    @Override
    public String stopEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("playback stop.txt");
                Thread.sleep(1000);
                if ("Idle".equals(getEquipStatus())) {
                    return "0";
                }
            } catch (Exception e) {
                logger.info("停机失败，" + e);
            }
        }
        return "start failed";
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        if (!exportRecipe2File(recipeName)) {
            logger.info(deviceCode + "上传recipe失败，导出recipe失败，检查通讯是否正常。");
            map.put("uploadResult", "upload failed");
            return map;
        }
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientFtpRecipeRelativePath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        getClientFtpRecipeAbsolutePath(recipeName);
        //服务端调用时初始化
        if(recipeFileMap.isEmpty()) {
            getRecipeList();
        }
        String rcpFiles = recipeFileMap.get(recipeName);
        if (rcpFiles == null) {
            logger.info(deviceCode + "上传recipe失败,获取recipe文件失败");
            map.put("uploadResult", "upload failed");
            return map;
        }
        String[] result = rcpFiles.split(",");
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + getClientFtpRecipeRelativePath(recipeName) + "\" \"mput " + result[0] + " " + result[1] + "\"";
        String cmd2 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + exportPath + "\" \"" + getClientFtpRecipeRelativePath(recipeName) + "\" \"put $" + recipeName + ".rcp$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand(cmd);
            iSecsHost.executeCommand(cmd2);
        }
        //压缩文件
        //ZipUtil.toZip(true, new File(getClientFtpRecipeAbsolutePath(recipeName)).listFiles(), getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".zip");
        try {
            ZipUtil.toZip(true, new File(clientFtpRecipeAbsolutePath).listFiles(), new FileOutputStream(clientFtpRecipeAbsolutePath + recipeName + ".zip"));
        } catch (FileNotFoundException e) {
            logger.info(deviceCode + "上传recipe失败，压缩文件失败" + e);
            map.put("uploadResult", "upload failed");
            return map;
        }
        map.put("recipe", setRecipe(recipeName));
        map.put("deviceCode", deviceCode);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        //读取屏幕参数
        //List<String> params = iSecsHost.executeCommand("readbyscreen params");
        //Map<String,String> paramsMap = (Map<String, String>) JsonMapper.fromJsonString(params.get(0),Map.class);
        map.put("recipeParaList", RecipeUtil.transfer2DB(new RecipeFileHandler() {
            @Override
            public Map<String, String> handler(File file) {
                Map<String, String> resultMap = new HashMap<>();
                String[] vals = null;
                try (
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))
                ) {
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        if (line.contains("Step")) {
                            vals = line.split("=")[1].split(",");
                            break;
                        }
                        line = bufferedReader.readLine();
                    }
                    resultMap.put("RF Power(Watt)", vals[1]);
                    resultMap.put("Cleaning Time", vals[2]);
                    resultMap.put("Ar+H2", vals[6]);
                    resultMap.put("Vacuum", vals[0]);
                    resultMap.put("Step Time", vals[4]);
                } catch (Exception e) {
                    logger.info("设备：" + deviceCode + "解析recipe失败" + e);
                    return null;
                }
                return resultMap;
            }
        }, new File(getClientFtpRecipeAbsolutePath(recipeName) + result[0]), recipeTemplates, false));
        recipeFileMap.clear();
        //delete exported file
        deleteExportRcpFile(recipeName);
        return map;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String downLoadResult = "1";
        String recipeName = recipe.getRecipeName();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
        if (!GlobalConstants.isLocalMode) {
            if (FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        try {
            ZipUtil.unZip(new File(clientFtpRecipeAbsolutePath + recipeName + ".zip"), clientFtpRecipeAbsolutePath);
        } catch (IOException e) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + "时,解压文件失败。");
            return "下载Recipe:" + recipe.getRecipeName() + "时,解压文件失败。";
        }
        //删除zip文件
        File zipFile = new File(clientFtpRecipeAbsolutePath + recipeName + ".zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }
        //下载clean和motion到设备
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + getClientFtpRecipeRelativePath(recipeName) + "\" \"mget *.pls *.svr\"";
        String cmd2 = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + exportPath + "\" \"" + getClientFtpRecipeRelativePath(recipeName) + "\" \"get $" + recipeName + ".rcp$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand(cmd);
            iSecsHost.executeCommand(cmd2);
        }
        if (!improtRecipeFromFile(recipeName)) {
            logger.info(deviceCode + "下载recipe失败，导入recipe失败。");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载recipe失败，导入recipe失败。");
            deleteExportRcpFile(recipeName);
            return "下载失败";
        }
        downLoadResult = "0";
        deleteExportRcpFile(recipeName);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + "成功。");
        return downLoadResult;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        //导入之后只保留一个，这时无法删除文件
        String[] result;
        String rcpFiles = recipeFileMap.get(recipeName);
        if (rcpFiles == null) {
            return "recipe文件不存在，无需删除。";
        }
        result = rcpFiles.split(",");
        String cmd = "dos $del /q \"" + equipRecipePath + result[0] + " " + equipRecipePath + result[1] + "\"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand(cmd);
        }
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {
        String selectResult = "0";
        //synchronize the recipe list
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback refresh.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback openLogin.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("replay login.rec");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback selectRcp.txt");
        }
        return selectResult;
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
        String result;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> errorList = iSecsHost.executeCommand("readrectcolor 111 43 128 49");
                if (errorList != null && !errorList.isEmpty()) {
                    result = errorList.get(0);
                    if ("0xff00".equals(result)) {
                        equipStatus = "Run";
                    } else {
                        List<String> idleList = iSecsHost.executeCommand("readrectcolor 111 27 129 34");
                        if (idleList != null && !idleList.isEmpty()) {
                            result = idleList.get(0);
                            if ("0xffff00".equals(result)) {
                                equipStatus = "Idle";
                            } else {
                                equipStatus = "Error";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("获取当前状态失败，" + e);
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
        VSP88DHost vsp88DHost = new VSP88DHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        vsp88DHost.startUp = startUp;
        clear();
        return vsp88DHost;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
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


    private String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    private String getClientFtpRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File recipeDir = new File(filePath);
        if (!recipeDir.exists()) {
            recipeDir.mkdirs();
        }
        return filePath;
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

    /**
     * 导出recipe（db文件）为普通文件
     *
     * @param recipeName recipe
     * @return
     */
    private boolean exportRecipe2File(String recipeName) {
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = recipeHost.executeCommand("Export \"" + recipeName + "\" \"" + exportPath + File.separator + recipeName + ".rcp\"");
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
                List<String> res = recipeHost.executeCommand("Import \"" + exportPath + File.separator + recipeName + ".rcp\"");
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
     * 删除导出的rcp文件
     *
     * @param recipeName
     * @return
     */
    private boolean deleteExportRcpFile(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand("dos $del /q \"" + exportPath + File.separator + recipeName + ".rcp\"$");
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private List<String> getRecipeList() {
        List<String> list = new ArrayList<>();
        recipeFileMap.clear();
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                String recipeJSON = recipeHost.executeCommand("Getlist").get(0);
                JSONObject jsonObject = JSON.parseObject(recipeJSON);
                JSONArray jsonArray = jsonObject.getJSONArray("recipes");
                for (int i = 0; i < jsonArray.size(); i++) {
                    String recipeName = jsonArray.getJSONObject(i).getString("Recipe");
                    String cleanName = jsonArray.getJSONObject(i).getString("Clean") + ".pls";
                    String motionName = jsonArray.getJSONObject(i).getString("Motion") + ".svr";
                    list.add(recipeName);
                    recipeFileMap.put(recipeName, cleanName + "," + motionName);
                }
                return list;
            } catch (Exception e) {
                logger.info("get recipe list failed" + e);
            }
        }
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
