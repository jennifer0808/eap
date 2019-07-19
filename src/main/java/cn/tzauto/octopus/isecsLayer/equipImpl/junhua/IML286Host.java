package cn.tzauto.octopus.isecsLayer.equipImpl.junhua;


import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.util.*;

public class IML286Host extends EquipModel {

    private Logger logger = Logger.getLogger(IML286Host.class);


    public IML286Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public String startEquip() {
        return null;
    }

    @Override
    public String pauseEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
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
        Map<String, String> map = new HashMap<>();
        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        Recipe recipe = setRecipe(recipeName);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        sqlSession.close();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return map;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String pkgType = recipeName.substring(0, 1);
        int version = recipe.getVersionNo();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        String absolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        return true;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        sqlSession.close();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public Map getEquipMonitorPara() {
        String recipe = getCurrentRecipeName();
        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String relativePath = getClientFtpRecipeRelativePath(recipe);
        String absolutePath = getClientFtpRecipeAbsolutePath(recipe);
        String ftpCmd = String.format("ftp %s %s %s %s \"put %s\"", localftpip, ftpUser, ftpPwd, equipRecipePath,relativePath,recipe);
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        Map<String, Object> map = new HashMap<>();
        List<String> recipes = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = iSecsHost.executeCommand(String.format("dos type %s /b", equipRecipePath));
            for(String recipeName : result) {
                if(!"done".equals(recipeName) && StringUtils.isNotBlank(recipeName) && recipeName.contains("rror")) {
                    recipes.add(recipeName.substring(0,recipeName.lastIndexOf(".")));
                }
            }
        }
        map.put("eppd",recipes);
        return map;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public Object clone() {
        IML286Host newEquip = new IML286Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public Map getEquipRealTimeState() {
        equipStatus = getEquipStatus();
        ppExecName = getCurrentRecipeName();
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        return map;
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

    public static void main(String[] args) {
        System.out.println("123.".lastIndexOf("."));
    }

}
