package cn.tzauto.octopus.isecsLayer.equipImpl.sunyang;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SytAfssRoHost extends EquipModel {

    private Logger logger = Logger.getLogger(SytAfssRoHost.class);
    private ISecsHost recipeHost;


    private Map<String, Map<String, String>> errDisMap;
    private Map<String, String> rcpNameMapping = new HashMap<>();

    public SytAfssRoHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
        String jsonString;
        InputStream in;
        BufferedReader bufferedReader = null;
        try {
            in = SytAfssRoHost.class.getResourceAsStream("sytAtssRoErr.json");
            bufferedReader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line = bufferedReader.readLine();
            while(line != null) {
                sb.append(line);
                line = bufferedReader.readLine();
            }
            jsonString = sb.toString();
            errDisMap = (Map<String, Map<String, String>>) JsonMapper.fromJsonString(jsonString, Map.class);
        } catch (Exception e) {
            logger.info("resolve json file failed" + e);
        }finally {
            if(bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void initialize() {
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        recipeHost = new ISecsHost(remoteIPAddress, "12005", deviceType, deviceType);
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
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            List<String> result = recipeHost.executeCommand("GetCurRecipe");
            if (result != null && !result.isEmpty()) {
                ppExecName = result.get(0);
                if(ppExecName.matches("^\\d\\..+$")) {
                    ppExecName = ppExecName.split("\\.")[1].trim();
                }
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
        return null;
    }

    @Override
    public String stopEquip() {
        return null;
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        map.put("rcpAnalyseSucceed", "upload recipe failed");
        //服务端直接调用时初始化
        if(rcpNameMapping.isEmpty()) {
            getRecipeList();
        }
        if (!exportRecipe2File(rcpNameMapping.get(recipeName))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + "失败。");
            return map;
        }
        map.put("recipe", setRecipe(recipeName));
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"put $" + recipeName + ".rcp$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = iSecsHost.executeCommand(cmd);
                for (String str : res) {
                    if (str != null && str.contains("success")) {
                        map.put("rcpAnalyseSucceed", "Y");
                        map.put("deviceCode", deviceCode);
                        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                        RecipeService recipeService = new RecipeService(sqlSession);
                        //C0032和C0033的recipe的DEVICE1略有区别
                        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                        sqlSession.close();
                        map.put("recipeParaList", RecipeUtil.transfer2DB(new RecipeFileHandler() {
                            @Override
                            public Map<String, String> handler(File file) {
                                Map<String, String> paramMap = new HashMap<>();
                                try {
                                    String json = FileUtils.readFileToString(file,"UTF-8");
                                    JSONArray jsonArray = JSON.parseObject(json).getJSONObject("SYT-ATSS-RO".equals(deviceType) ? "C0032" :"C0033").getJSONArray("DEVICE1");
                                    int idx = 1;
                                    for(int i=0;i<jsonArray.size();i++) {
                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                        String postion = jsonObject.getString("3");
                                        String mspeed = jsonObject.getString("4");
                                        String distance = jsonObject.getString("5");
                                        String sspeed = jsonObject.getString("6");
                                        String atime = jsonObject.getString("7");
                                        String dtime = jsonObject.getString("8");
                                        String motorTime = jsonObject.getString("9");
                                        if(!"SPARE".equalsIgnoreCase(postion)) {
                                            paramMap.put(postion + "[MSPEED]",mspeed);
                                            paramMap.put(postion + "[DISTANCE]",distance);
                                            paramMap.put(postion + "[SSPEED]",sspeed);
                                            paramMap.put(postion + "[ATIME]",atime);
                                            paramMap.put(postion + "[DTIME]",dtime);
                                            paramMap.put(postion + "[MotorTimes]",motorTime);
                                        }else {
                                            paramMap.put(postion+ idx + "[MSPEED]",mspeed);
                                            paramMap.put(postion + idx +  "[DISTANCE]",distance);
                                            paramMap.put(postion + idx +  "[SSPEED]",sspeed);
                                            paramMap.put(postion + idx +  "[ATIME]",atime);
                                            paramMap.put(postion + idx +  "[DTIME]",dtime);
                                            paramMap.put(postion +  idx + "[MotorTimes]",motorTime);
                                            idx++;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return paramMap;
                            }
                        },new File(clientAbsoluteFtpPath + File.separator + recipeName + ".rcp"),recipeTemplates,false));
                        break;
                    }
                }
            } catch (Exception e) {
                logger.info("upload recipe file failed,please make sure you have opened ftp service");
            }
        }
        delRecipeFile(recipeName);
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
        modifyRcpFile(clientAbsoluteFtpPath + recipeName + ".rcp");
        if (!FtpUtil.uploadFile(clientAbsoluteFtpPath + recipeName + ".rcp", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            deleteTempFile(recipeName);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants. localRecipePath+ remoteRcpPath);
        deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String downLoadResult = "1";
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName, hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"get $" + recipeName + ".rcp$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = iSecsHost.executeCommand(cmd);
                for (String str : res) {
                    if (str != null && str.contains("success")) {
                        break;
                    }
                    if (str.contains("error")) {
                        downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                        return downLoadResult;
                    }
                }
            } catch (Exception ex) {
                logger.info("下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启." + ex);
                return downLoadResult;
            }
        }
        if (improtRecipeFromFile(recipeName)) {
            downLoadResult = "0";
        }else{
            downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
        }
        delRecipeFile(recipeName);
        return downLoadResult;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        return null;
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback manual.txt");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            iSecsHost.executeCommand("playback auto.txt");
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
            e.printStackTrace();
            logger.info("device:" + deviceCode + "get repipe list failed" + e.getMessage());
        }
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("read status");
                if (result != null && !result.isEmpty()) {
                    String ecode = result.get(0);
                    String errDis = errDisMap.get(ecode).get("ename");
                    if (errDis.contains("*")) {
                        equipStatus = errDis.replace("*", "").trim();
                    } else {
                        equipStatus = "alarm";
                    }
                }
            } catch (Exception e) {
                logger.info("get equip status failed" + e);
            } finally {
                Map<String, String> map = new HashMap<>();
                map.put("EquipStatus", equipStatus);
                changeEquipPanel(map);
            }
        }
        return equipStatus;
    }

    @Override
    public Object clone() {
        SytAfssRoHost sytAtssRoHost = new SytAfssRoHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        sytAtssRoHost.startUp = startUp;
        clear();
        return sytAtssRoHost;
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

    private List<String> getRecipeList() {
        rcpNameMapping.clear();
        List<String> recipes = new ArrayList<>();
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = recipeHost.executeCommand("Getlist");
                if (results != null && !results.isEmpty()) {
                    String result = results.get(0);
                    if (StringUtils.isNotBlank(result) && !"done".equals(result)) {
                        JSONObject jsonObject = JSON.parseObject(result);
                        JSONArray jsonArray = jsonObject.getJSONArray("recipes");
                        for (int i = 0; i < jsonArray.size(); i++) {
                            String name = jsonArray.getJSONObject(i).getString("DEVICE_NAME");
                            if(name.matches("^\\d\\..+$")) {
                                String newRcpName = name.split("\\.")[1].trim();
                                recipes.add(newRcpName);
                                rcpNameMapping.put(newRcpName, name);
                            }else {
                                recipes.add(name);
                                rcpNameMapping.put(name, name);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("get recipe list failed" + e);
            }
        }
        return recipes;
    }

    /**
     * 导出recipe（db文件）为普通文件
     *
     * @param recipeName recipe
     * @return
     */
    private boolean exportRecipe2File(String recipeName) {
        String newRecipe;
        if(recipeName.matches("^\\d\\..+$")) {
            newRecipe = recipeName.split("\\.")[1].trim();
        }else {
            newRecipe = recipeName;
        }
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = recipeHost.executeCommand("Export \"" + recipeName + "\" \"" + equipRecipePath + File.separator + newRecipe + ".rcp\"");
                for (String str : res) {
                    if (str != null && str.contains("done")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode,"导出recipe失败" + e);
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode,"导入recipe失败" + e);
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

    private String getClientFtpRecipeRelativePath(String recipeName) {
        if(recipeName.matches("^\\d\\..+$")) {
            recipeName = recipeName.split("\\.")[1].trim();
        }
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

    /**
     * 删除导出rcp中的recipeName的1.
     * @param path
     */
    private void modifyRcpFile(String path) {
        try {
            String jsonStr = FileUtils.readFileToString(new File(path), "UTF-8");
            FileUtils.writeStringToFile(new File(path),jsonStr.replaceFirst("\\d\\. ", ""),"UTF-8",false);
        } catch (IOException e) {
            logger.info("修改导出rcp文件失败" + e);
        }
    }


}
