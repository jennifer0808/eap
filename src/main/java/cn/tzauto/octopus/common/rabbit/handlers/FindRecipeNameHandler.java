/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rain
 */
public class FindRecipeNameHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(FindRecipeNameHandler.class.getName());
    private String deviceCode = "";
    private String recipeName = "";
    private String businessMod = "";


    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            deviceCode = msgMap.get("deviceCode");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            Map equipState = hostManager.getEquipInitState(deviceInfo.getDeviceCode());
            if (equipState == null) {
                recipeName = deviceInfoExt.getRecipeName();
            } else {
                recipeName = equipState.get("PPExecName").toString();
            }
            if (recipeName.equals("--")) {
                recipeName = deviceInfoExt.getRecipeName();
            }
            if (msgMap.get("lotType") != null) {
                businessMod = msgMap.get("lotType");
                if ("E".equals(businessMod)) {
                    deviceInfoExt.setBusinessMod("Engineer");
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "工程批，设备进入工程模式！");
                }
            }

            Recipe recipe = recipeService.getGoldRecipe(recipeName, deviceCode, deviceInfo.getDeviceType());//获取gold类型的版本号最高的recipe
            Map mqMap = new HashMap();
            mqMap.put("msgName", "FindRecipeName");
            mqMap.put("recipeName", recipeName);
            if (recipe != null && recipe.getVersionNo() != null) {
                mqMap.put("verNo", recipe.getVersionNo().toString());
            } else {
                mqMap.put("verNo", "0");
            }
//TODO 原队列JMSReplyTo获得，待验证
            GlobalConstants.C2SCheckRcpNameQueue.sendMessage( mqMap);
            logger.info("向服务端[ C2S.Q.CHECKRCPNAME ]回复校验程序名消息" + JSONArray.toJSONString(mqMap));
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        } finally {
            sqlSession.close();
        }
    }
}
