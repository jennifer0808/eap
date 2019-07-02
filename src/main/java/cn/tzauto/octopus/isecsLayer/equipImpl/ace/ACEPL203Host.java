package cn.tzauto.octopus.isecsLayer.equipImpl.ace;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.ace.ACEResolver;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.util.*;

public class ACEPL203Host extends EquipModel {

    private Logger logger = Logger.getLogger(ACEPL203Host.class);

    private static final String csvFile = "changpinxinghao20150726.csv";

    public ACEPL203Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        String cmd = "dos $type \"" + equipRecipePath + "changpinxinghao20150726.csv.ini\" | findstr \"CurrentRecipeName\"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand(cmd,"GBK");
                if (result != null && !result.isEmpty()) {
                    ppExecName = result.get(0).split("=")[1];
                }
            } catch (Exception e) {
                logger.info("get recipeName failed" + e);
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
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        //创建文件夹
        getClientFtpRecipeAbsolutePath(recipeName);
        map.put("recipe", setRecipe(recipeName));
        map.put("deviceCode", deviceCode);
        List<String> data;
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"" + "put changpinxinghao20150726.csv\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand(cmd);
        }

        ACEResolver aceResolver = new ACEResolver(getClientFtpRecipeAbsolutePath(recipeName) + "changpinxinghao20150726.csv");
        try {
            /**
             * 读取recipe信息，并保存
             */
            data = aceResolver.getDataByName(recipeName);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".txt")));
            writer.write(StringUtils.join(data, ","));
            writer.close();
        } catch (IOException e) {
            map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + e);
            map.put("rcpAnalyseSucceed", "N");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传recipe[" + recipeName + "]失败。" + e);
            return map;
        }
        Map<String, String> recipeParaMap = new HashMap<>();
        for (int i = 0; i < data.size(); i++) {
            if (i >= 2) {
                recipeParaMap.put("CurrentRecipeValue" + String.format("%04d", i - 2), data.get(i));
            }
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        map.put("recipeParaList", RecipeUtil.transfer2DB(recipeParaMap, recipeTemplates, true));
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传recipe[" + recipeName + "]成功。");
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
        //String clientZipFilePath = GlobalConstants.localRecipePath + GlobalConstants.ftpPath + recipeName ;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + ".txt", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            deleteTempFile(recipeName);
            return false;
        }
        deleteTempFile(recipeName);
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
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
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        if (!GlobalConstants.isLocalMode) {
            String ftpip = GlobalConstants.ftpIP;
            String ftpPort = GlobalConstants.ftpPort;
            String hostDownloadFtpRecipeFilePath = getHostDownloadFtpRecipeFilePath(recipe);
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }

            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".txt", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        List<String> vals;
        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".txt")));
        ) {
            vals = Arrays.asList(bufferedReader.readLine().split(",", -1));
        } catch (Exception e) {
            logger.info("downloadRecipe failed, resolve template failed" + e);
            deleteTempFile(recipeName);
            return downLoadResult;
        }
        //保留多个recipe可以修改csv.ini文件，将需要选中的recipe的值写入，然后直接加载changpinxinghao
        //1.上传csv文件
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"" + "put changpinxinghao20150726.csv\"");
        }
        //2.写入新的recipe，原先存在则先删除
        ACEResolver aceResolver = new ACEResolver(clientFtpRecipeAbsolutePath + "changpinxinghao20150726.csv");
        try {
            Map<String, Integer> map = aceResolver.getColumnNameIndex();
            if (map.get(recipeName) != null) {
                aceResolver.delColumn(recipeName, clientFtpRecipeAbsolutePath + "changpinxinghao20150726.csv");
            }
            aceResolver.addColumn(vals, clientFtpRecipeAbsolutePath + "changpinxinghao20150726.csv");
        } catch (IOException e) {
            e.printStackTrace();
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载recipe[" + recipeName + "]失败。");
            return downLoadResult;
        }
        //3.下载recipe
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"" + "get changpinxinghao20150726.csv\"");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载recipe[" + recipeName + "]成功。");
        }
        downLoadResult = "0";
        deleteTempFile(recipeName);
        return downLoadResult;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        //1.将机台的recipe文件上传至工控机
        String uploadCMD = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + getClientFtpRecipeRelativePath(recipeName) + "\" \"put " + csvFile + "\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand(uploadCMD);
            } catch (Exception e) {
                logger.info("删除recipe失败，从机台获取recipe失败" + e);
                deleteTempFile(recipeName);
                return "删除失败";
            }
        }
        //删除csv指定的recipe
        ACEResolver resolver = new ACEResolver(getClientFtpRecipeAbsolutePath(recipeName) + csvFile);
        try {
            resolver.delColumn(recipeName, getClientFtpRecipeAbsolutePath(recipeName) + csvFile);
        } catch (IOException e) {
            deleteTempFile(recipeName);
            logger.info("删除recipe失败,解析处理csv文件失败" + e);
        }
        //2.将工控机recipe文件下载至机台
        String downloadCMD = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + getClientFtpRecipeRelativePath(recipeName) + "\" \"get " + csvFile + "\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                iSecsHost.executeCommand(downloadCMD);
            } catch (Exception e) {
                logger.info("删除recipe失败，从工控机上传recipe失败" + e);
                deleteTempFile(recipeName);
                return "删除失败";
            }
        }
        deleteTempFile(recipeName);
        return "删除成功";
    }

    @Override
    public String selectRecipe(String recipeName) {
        String selectResult = "0";
        Map<String, Object> map = getEquipRecipeList();
        List<String> recipeList = (List<String>) map.get("eppd");
        Collections.sort(recipeList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        Map<String, Integer> rcpMap = new HashMap<>();
        for (int i = 0; i < recipeList.size(); i++) {
            rcpMap.put(recipeList.get(i), i);
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback login.txt");
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            logger.info("线程sleep失败" + e);
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback gotoSR.txt");
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback openSR.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback sort.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("playback rcp_" + rcpMap.get(recipeName) + ".txt");
                for (String str : result) {
                    if (str.contains("Error")) {
                        selectResult = "下载recipce失败。";
                        logger.info("选中recipe失败，没有配置对应的playback。");
                        break;
                    }
                }
            } catch (Exception e) {
                selectResult = "下载recipce失败。";
                logger.info("选中recipe失败," + e);
            }
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback clickConfirm.txt");
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback loadRecipe.txt");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("playback SR_OK.txt");
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
        List<String> list = new ArrayList<>();
        List<String> res;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            res = iSecsHost.executeCommand("dos $type \"" + equipRecipePath + csvFile + "\" | findstr -v \"PC\"$");
        }
        if (res != null && !res.isEmpty()) {
            String str = res.get(0);
            if (!"done".equals(str) && !"error".equals(str)) {
                String[] recipes = str.split(",");
                for (String rcp : recipes) {
                    if (StringUtils.isNotBlank(rcp)) {
                        list.add(rcp);
                    }
                }
                eppd.put("eppd", list);
            }
        }
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    String curscreen = result.get(0);
                    if ("run".equals(curscreen)) {
                        equipStatus = "Run";
                    } else {
                        equipStatus = "Idle";
                    }
                }
            } catch (Exception e) {
                logger.info("get equip status failed" + e);
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
        ACEPL203Host newHost = new ACEPL203Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newHost.startUp = startUp;
        clear();
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
    protected void deleteTempFile(String recipeName) {
        logger.info("device:" + deviceCode + "开始删除临时文件。");
        File file = new File(GlobalConstants.localRecipePath + "RECIPE" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
        FileUtil.deleteAllFilesOfDir(file);
        logger.info("删除临时文件:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
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
