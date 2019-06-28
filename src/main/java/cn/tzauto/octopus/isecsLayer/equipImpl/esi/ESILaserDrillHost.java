package cn.tzauto.octopus.isecsLayer.equipImpl.esi;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.log4j.MDC;

import java.util.List;
import java.util.Map;

/**
 * Created by luosy on 2019/6/27.
 */
public class ESILaserDrillHost extends EquipModel {

    public ESILaserDrillHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        return null;
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
