package cn.tzauto.octopus.isecsLayer.equipImpl.junhua;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import org.apache.log4j.Logger;

import java.util.*;

public class ILM286Host extends EquipModel {
    private static Logger logger = Logger.getLogger(ILM286Host.class.getName());
    private Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();

    public ILM286Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try{
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (!result.isEmpty()&&null!=result){
                    if ("main".equals(result.get(0))){
                        ppExecName = iSecsHost.executeCommand("read recipename").get(0);
                    }
                }
            }catch (Exception e){
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        Map map = new HashMap();
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
        return null;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        return null;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        return null;
    }

    @Override
    public String selectRecipe(String recipeName) {
        return null;
    }

    @Override
    public Map getEquipMonitorPara() {
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        return null;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try {
                List<String> result = this.iSecsHost.executeCommand("curscreen");
                if (!result.isEmpty()&&null!=result){
                  if ("main".equals(result.get(0))){
                      List<String> result1 = iSecsHost.executeCommand("read status");
                      if ("運轉".equals(result1.get(0))){
                          equipStatus = "Idle";
                      }else if ("停止".equals(result1.get(0))){
                          equipStatus = "Run";
                      }
                  }
                }
            }catch (Exception e){
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        return equipStatus;
    }

    @Override
    public boolean startCheck() {
        return true;
    }

    @Override
    public Object clone() {
        ILM286Host newHost = new ILM286Host(deviceId, remoteIPAddress,remoteTCPPort,deviceType,iconPath,equipRecipePath);
        newHost.startUp = startUp;
        this.clear();
        return newHost;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
    }
}
