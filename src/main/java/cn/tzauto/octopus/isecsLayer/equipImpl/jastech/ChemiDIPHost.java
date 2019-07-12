package cn.tzauto.octopus.isecsLayer.equipImpl.jastech;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChemiDIPHost extends EquipModel {

    private Logger logger = Logger.getLogger(ChemiDIPHost.class);

    private ISecsHost recipeHost;

    private static final String exportPath = "D:/recipeExport";

    public ChemiDIPHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        recipeHost = new ISecsHost(remoteIPAddress, "12005", deviceType, deviceCode);
        iSecsHostList.add(iSecsHost);
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
//        Map<String, String> recipeNameMapping = new HashMap<>();
//        List<String> result = recipeHost.executeCommand("Getlist");
//        if(result != null && !result.isEmpty()) {
//            String rcpString = result.get(0);
//            JSONObject rcpJSON = JSON.parseObject(rcpString);
//            JSONArray rcps = rcpJSON.getJSONArray("recipes");
//            for(int i=0;i<rcps.size();i++) {
//                recipeNameMapping.put(rcps.getJSONObject(i).getString("IDX"), rcps.getJSONObject(i).getString("DEVICE_NAME"));
//            }
//        }
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
        Map resultMap = new HashMap();
        if (!exportRecipe2File(recipeName)) {
            UiLogUtil.getInstance().getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + "失败，导出recipe失败。");
            resultMap.put("uploadResult", "upload recipe failed");
            return resultMap;
        }
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientAbsoluteFtpPAth = getClientFtpRecipeAbsolutePath(recipeName);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + exportPath + "\" \"" + clientRelativeFtpPath + "\" \"put $" + recipeName + "$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = iSecsHost.executeCommand(cmd);
                for (String str : res) {
                    if (str != null && str.contains("success")) {
                        resultMap.put("recipe", setRecipe(recipeName));
                        resultMap.put("deviceCode", deviceCode);
                        Map<String, String> paramMap = new HashMap<>();
                        String jsonString = FileUtils.readFileToString(new File(clientAbsoluteFtpPAth + recipeName), "UTF-8");
                        JSONObject devs = JSON.parseObject(jsonString).getJSONObject("JETDB");
                        JSONObject dev0 = devs.getJSONArray("DEV_01").getJSONObject(0);
                        JSONObject dev1 = devs.getJSONArray("DEV_02").getJSONObject(0);
                        paramMap.put("PRODUCT LENGTH", NumStrConvertor.convert(dev0.getString("8"), 1));
                        paramMap.put("PRODECT WIDTH", NumStrConvertor.convert(dev0.getString("9"), 1));
                        paramMap.put("DIP TIME", NumStrConvertor.convert(dev0.getString("10"), 1));
                        paramMap.put("BELT SPEED", NumStrConvertor.convert(dev0.getString("11"), 1));
                        paramMap.put("SPRAY R/S CONVEYOR WIDTH", NumStrConvertor.convert(dev0.getString("12"), 1));
                        paramMap.put("W/J BUFFER CONVEYOR WIDTH", NumStrConvertor.convert(dev0.getString("13"), 1));
                        paramMap.put("W/J INLET CONVEYOR WIDTH", NumStrConvertor.convert(dev0.getString("14"), 1));
                        paramMap.put("W/J ROOM CONVEYOR WIDTH", NumStrConvertor.convert(dev0.getString("15"), 1));
                        paramMap.put("OFFLOAD CONVEYOR WIDTH", NumStrConvertor.convert(dev0.getString("16"), 1));
                        paramMap.put("C/D FINGER COUNT", NumStrConvertor.convert(dev0.getString("23"), 1));
                        paramMap.put("LOT CHANGE INTERVAL", NumStrConvertor.convert(dev0.getString("24"), 1));
                        paramMap.put("W/J #1 UPPER LIMIT PRESSURE", NumStrConvertor.convert(dev0.getString("68"), 1));
                        paramMap.put("W/J #1 WORK PRESSURE", NumStrConvertor.convert(dev0.getString("69"), 1));
                        paramMap.put("W/J #1 LOWER LIMIT PRESSURE", NumStrConvertor.convert(dev0.getString("71"), 1));
                        paramMap.put("W/J #2 UPPER LIMIT PRESSURE", NumStrConvertor.convert(dev0.getString("78"), 1));
                        paramMap.put("W/J #2 WORK PRESSURE", NumStrConvertor.convert(dev0.getString("79"), 1));
                        paramMap.put("W/J #2 LOWER LIMIT PRESSURE", NumStrConvertor.convert(dev0.getString("81"), 1));
                        paramMap.put("CHEMICAL TEMP HIGH LIMIT", NumStrConvertor.convert(dev1.getString("84"), 1));
                        paramMap.put("CHEMICAL TEMP WORK LIMIT", NumStrConvertor.convert(dev1.getString("85"), 1));
                        paramMap.put("CHEMICAL TEMP LOW LIMIT", NumStrConvertor.convert(dev1.getString("86"), 1));
                        paramMap.put("CHEMOCAL TEMP OFFSET #1", dev1.getString(""));
                        paramMap.put("CHEMOCAL TEMP OFFSET #2", dev1.getString(""));
                        paramMap.put("HOT DRY TEMP HIGH LIMIT", NumStrConvertor.convert(dev1.getString("94"), 1));
                        paramMap.put("HOT DRY TEMP WORK LIMIT", NumStrConvertor.convert(dev1.getString("95"), 1));
                        paramMap.put("HOT DRY TEMP LOW LIMIT", NumStrConvertor.convert(dev1.getString("96"), 1));
                        paramMap.put("W/J HYD OIL TEMP HIGH LIMIT", NumStrConvertor.convert(dev1.getString("99"), 1));
                        paramMap.put("TOP POSITION ONLD", NumStrConvertor.convert(dev1.getString("24"), 2));
                        paramMap.put("UNFIX POSITION ONLD", NumStrConvertor.convert(dev1.getString("25"), 2));
                        paramMap.put("UNLOAD POSITION ONLD", NumStrConvertor.convert(dev1.getString("26"), 2));
                        paramMap.put("WORK POSITION ONLD", NumStrConvertor.convert(dev1.getString("27"), 2));
                        paramMap.put("FIX POSITION ONLD", NumStrConvertor.convert(dev1.getString("28"), 2));
                        paramMap.put("TOP POSITION OFFLD", NumStrConvertor.convert(dev1.getString("34"), 2));
                        paramMap.put("UNFIX POSITION OFFLD", NumStrConvertor.convert(dev1.getString("35"), 2));
                        paramMap.put("UNLOAD POSITION OFFLD", NumStrConvertor.convert(dev1.getString("36"), 2));
                        paramMap.put("WORK POSITION OFFLD", NumStrConvertor.convert(dev1.getString("37"), 2));
                        paramMap.put("FIX POSITION OFFLD", NumStrConvertor.convert(dev1.getString("38"), 2));
                        paramMap.put("BUF TOP POSITION ONLD", NumStrConvertor.convert(dev1.getString("44"), 2));
                        paramMap.put("BUF UNFIX POSITION ONLD", NumStrConvertor.convert(dev1.getString("45"), 2));
                        paramMap.put("BUF UNLOAD POSITION ONLD", NumStrConvertor.convert(dev1.getString("46"), 2));
                        paramMap.put("BUF WORK POSITION ONLD", NumStrConvertor.convert(dev1.getString("47"), 2));
                        paramMap.put("BUF FIX POSITION ONLD", NumStrConvertor.convert(dev1.getString("48"), 2));
                        paramMap.put("1ST PK FWD POS ONLD", NumStrConvertor.convert(dev1.getString("54"), 2));
                        paramMap.put("1ST PK BWD POS ONLD", NumStrConvertor.convert(dev1.getString("55"), 2));
                        paramMap.put("1ST PK FWD POS OFFLD", NumStrConvertor.convert(dev1.getString("56"), 2));
                        paramMap.put("1ST PK BWD POS OFFLD", NumStrConvertor.convert(dev1.getString("57"), 2));
                        paramMap.put("SLOT PITCH", NumStrConvertor.convert(dev1.getString("31"), 2));
                        paramMap.put("SLOT NUMBER", NumStrConvertor.convert(dev1.getString("32"), 2));
                        paramMap.put("SLOT MGZ EMPTY COUNT", NumStrConvertor.convert(dev1.getString("33"), 2));
                        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                        sqlSession.close();
                        resultMap.put("recipeParaList", RecipeUtil.transfer2DB(paramMap, recipeTemplates, false));
                        break;
                    }
                }
            } catch (Exception e) {
                logger.info("upload recipe file failed,please make sure you have opened ftp service");
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传recipe失败" + e);
                resultMap.put("rcpAnalyseSucceed", "N");
            }
        }
        delRecipeFile(recipeName);
        return resultMap;
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
        if (!FtpUtil.uploadFile(clientAbsoluteFtpPath + recipeName, remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + clientAbsoluteFtpPath);
        deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> modelList = iSecsHost.executeCommand("read model");
            if (modelList != null && !modelList.isEmpty()) {
                if ("AUTO".equals(modelList.get(0))) {
                    logger.info(deviceCode + "在自动模式下不能下载和切换recipe！");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "自动模式下不能下载和切换recipe！");
                    return "download recipe failed.";
                }
            }
        }
        String downLoadResult = "1";
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String recipeName = recipe.getRecipeName();
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.");
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName, hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + exportPath + "\" \"" + clientRelativeFtpPath + "\" \"get $" + recipeName + "$\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = iSecsHost.executeCommand(cmd);
                for (String str : res) {
                    if (str != null && str.contains("success")) {
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
        if ("0".equals(downLoadResult) && improtRecipeFromFile(recipeName)) {
            downLoadResult = "0";
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipeName + "成功。");
        } else {
            downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipeName + "失败。");
        }
        //delRecipeFile(recipeName);
        deleteTempFile(recipeName);
        return downLoadResult;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {

        // 定位到主界面
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotoMain.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> modelList = iSecsHost.executeCommand("read model");
            if (modelList != null && !modelList.isEmpty()) {
                if ("AUTO".equals(modelList.get(0))) {
                    logger.info(deviceCode + "在自动模式下不能切换recipe！");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "自动模式下不能切换recipe！");
                    return "0";
                }
            }
        }
        //打开recipe切换界面
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback openSR.txt");
        }
        //登陆
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback loginAndSelect.txt");
        }
        //选中rcp
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            List<String> rcp_1_list = iSecsHost.executeCommand("read rcp_1");
//            if(rcp_1_list != null && ! rcp_1_list.isEmpty()) {
//                if(recipeName.equals(rcp_1_list.get(0))) {
//                    iSecsHost.executeCommand("playback rcp_1.txt");
//                    return "0";
//                }else{
//                    List<String> rcp_2_list = iSecsHost.executeCommand("read rcp_2");
//                    if(rcp_2_list != null && !rcp_2_list.isEmpty()) {
//                        if(recipeName.equals(rcp_2_list.get(0))) {
//                            iSecsHost.executeCommand("playback rcp_2.txt");
//                        }else {
//                            iSecsHost.executeCommand("playback rcp_3.txt");
//                        }
//                        return "0";
//                    }
//                }
//            }
            iSecsHost.executeCommand("playback rcp_1.txt");
            iSecsHost.executeCommand("playback send.txt");
        }
        return "0";
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
                List<String> list = iSecsHost.executeCommand("curscreen");
                if (list != null && !list.isEmpty()) {
                    String result = list.get(0);
                    if ("Run".equals(result)) {
                        equipStatus = "Run";
                    } else {
                        List<String> list1 = iSecsHost.executeCommand("readrectcolor 118 118 140 142");
                        if (list1 != null && !list1.isEmpty()) {
                            String res = list1.get(0);
                            if ("0xff0000".equals(res)) {
                                equipStatus = "Alarm";
                            } else {
                                equipStatus = "Idle";
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("device：" + deviceCode + "获取状态失败。" + e);
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
    public Map getEquipRealTimeState() {
        Map map = new HashMap();
        map.put("PPExecName", getCurrentRecipeName());
        map.put("EquipStatus", getEquipStatus());
        map.put("controlState", controlState);
        return map;
    }

    @Override
    public Object clone() {
        ChemiDIPHost chemiDIPHost = new ChemiDIPHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        chemiDIPHost.startUp = startUp;
        clear();
        return chemiDIPHost;
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
        List<String> recipes = new ArrayList<>();
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = recipeHost.executeCommand("Getlist");
                if (result != null && !result.isEmpty()) {
                    String rcpString = result.get(0);
                    JSONObject rcpJSON = JSON.parseObject(rcpString);
                    JSONArray rcps = rcpJSON.getJSONArray("recipes");
                    for (int i = 0; i < rcps.size(); i++) {
                        recipes.add(rcps.getJSONObject(i).getString("DEVICE_NAME"));
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
        synchronized (recipeHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> res = recipeHost.executeCommand("Export \"" + recipeName + "\" \"" + exportPath + File.separator + recipeName + "\"");
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
                List<String> res = recipeHost.executeCommand("Import \"" + exportPath + File.separator + recipeName + "\"");
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

    public boolean delRecipeFile(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos $del /q \"" + exportPath + File.separator + recipeName + "\"$");
                if (result != null && !result.isEmpty()) {
                    if ("done".equals(result.get(0))) {
                        return true;
                    }
                }
            } catch (Exception e) {
                logger.info("device: " + deviceCode + "删除导出的recipe失败。" + e);
            }
        }
        return false;
    }

    static class NumStrConvertor {

        public static String convert(String org, int digit) {
            if (digit < 0) {
                throw new IllegalArgumentException();
            }
            if (org == null || org.length() == 1) {
                return org;
            }
            int len = org.length();
            if (digit >= len) {
                return org;
            }
            return org.substring(0, len - digit) + "." + org.substring(len - digit, len);
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
