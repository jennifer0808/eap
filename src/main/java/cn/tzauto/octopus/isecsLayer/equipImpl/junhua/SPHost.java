package cn.tzauto.octopus.isecsLayer.equipImpl.junhua;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPHost extends EquipModel {

    private Logger logger = Logger.getLogger(SPHost.class);

    public SPHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
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
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public String stopEquip() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        return true;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
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
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return null;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("");
        }
        return equipStatus;
    }

    @Override
    public Object clone() {
        SPHost newEquip = new SPHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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

}
