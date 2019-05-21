package cn.tzauto.octopus.isecsLayer.equipImpl.screen;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luosy on 2019/5/16.
 */
public class ScreenHost extends EquipModel {

    private static Logger logger = Logger.getLogger(ScreenHost.class.getName());


    public ScreenHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        String prerecipeName = ppExecName;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("process".equals(result.get(0))) {
                        String recipeNameTemp = iSecsHost.executeCommand("read recipename").get(0);
                        ppExecName = recipeNameTemp.substring(recipeNameTemp.lastIndexOf("/") + 1);
//                        String lotIdtemp = iSecsHost.executeCommand("read lotno").get(0);
                        if ("done".equals(ppExecName)) {
                            ppExecName = prerecipeName;
                        }
//                        if (!"done".equals(lotIdtemp)) {
//                            lotId = lotIdtemp;
//                        }
                    } else {
                        ppExecName = prerecipeName;
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
                ppExecName = "--";
                return prerecipeName;
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("WorkLot", lotId);
        changeEquipPanel(map);
        selectRecipe("123");
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");

                return "选中失败";
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                return "选中失败";
            }
        }
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
        return null;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
    }
}
