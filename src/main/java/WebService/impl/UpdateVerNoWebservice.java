package WebService.impl;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 12631 on 2019/3/1.
 */
public class UpdateVerNoWebservice implements BaseWebservice {
    private static final Logger logger = Logger.getLogger(UpdateVerNoWebservice.class.getName());
    String eventId = "";
    String recipeName = "";
    String lotId = "";
    String deviceCode = "";

    @Override
    public String handle(String message) {
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);
        Map webMap = new HashMap();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
            eventId = String.valueOf(map.get("eventId"));
            deviceCode = String.valueOf(map.get("deviceCode"));
            recipeName = String.valueOf(map.get("recipeName"));
            lotId = String.valueOf(map.get("lotId"));
        DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (deviceInfo.getDeviceType().contains("DF")) {
            recipeName = deviceInfoExt.getRecipeName();
        }
        Recipe goldRecipe = recipeService.getGoldRecipe(recipeName, deviceCode, deviceInfo.getDeviceType());
        if (goldRecipe == null) {
            UiLogUtil.appendLog2SeverTab(deviceCode, "工控上不存在： " + recipeName + " 的Gold版本，无法更新版本号！请联系PE处理！");

        }
        EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceInfo.getDeviceCode());
        equipHost.lotId = lotId;

        webMap.put("WorkLot", lotId);
        equipHost.changeEquipPanel(map);
        try {
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置，DEVICE_CODE:" + deviceCode);
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在该设备模型信息，请联系ME处理！");

            } else {
                deviceInfoExt.setLotId(lotId);
//                deviceInfoExt.setVerNo(goldRecipe.getVersionNo());
                deviceService.modifyDeviceInfoExt(deviceInfoExt);

            }
            sqlSession.commit();
            UiLogUtil.appendLog2SeverTab(deviceCode, "最新的版本信息为：" + goldRecipe.getVersionNo());
            webMap.put("eventDesc", deviceCode+"最新的版本信息为：" + goldRecipe.getVersionNo());
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
            webMap.put("eventDesc", deviceCode+"更新版本信息失败");
        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
    }
}
