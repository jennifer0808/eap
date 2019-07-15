/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FindRecipeNameHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(FindRecipeNameHandler.class);
    private String deviceCode = "";
    private String recipeName = "";
    private String businessMod = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            deviceCode = mapMessage.getString("deviceCode");
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求核对设备的当前程序名");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            Map equipState = hostManager.getEquipInitState(deviceInfo.getDeviceCode());
            if (equipState == null) {
                recipeName = "---";
            } else {
                recipeName = equipState.get("PPExecName").toString();
            }
            if (mapMessage.getString("lotType") != null) {
                businessMod = mapMessage.getString("lotType");
                if ("E".equals(businessMod)) {
                    DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                    deviceInfoExt.setBusinessMod("Engineer");
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                   UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "工程批，设备进入工程模式！");
                }
            }
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        }
        Recipe recipe = recipeService.getGoldRecipe(recipeName, deviceCode, deviceInfo.getDeviceType());//获取gold类型的版本号最高的recipe
        Map mqMap = new HashMap();
        mqMap.put("msgName", "FindRecipeName");
        mqMap.put("recipeName", recipeName.trim());
        if (recipe != null && recipe.getVersionNo() != null) {
            mqMap.put("verNo", recipe.getVersionNo().toString());
        } else {
            mqMap.put("verNo", "0");
        }
        try {
            Destination destination = message.getJMSReplyTo();
            logger.info("destination:================================ " + destination.toString());
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            logger.info("topicName:==========================================" + topicName);
            GlobalConstants.C2SCheckRcpNameQueue.sendMessage(topicName, mqMap);
            logger.info("向服务端[" + topicName + "]回复校验程序名消息" + JSONArray.toJSONString(mqMap));
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送设备的当前程序" + recipeName);
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        } finally {
            sqlSession.close();
        }
    }
}
