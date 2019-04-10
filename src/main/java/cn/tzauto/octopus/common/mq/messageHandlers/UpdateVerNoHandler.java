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
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author root
 */
public class UpdateVerNoHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpdateVerNoHandler.class.getName());
    String eventId = "";
    String recipeName = "";
    String lotId = "";
    String deviceCode = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            eventId = mapMessage.getString("eventId").toString();
            deviceCode = mapMessage.getString("deviceCode").toString();
            recipeName = mapMessage.getString("recipeName").toString();
            lotId = mapMessage.getString("lotId").toString();
        } catch (JMSException ex) {
            logger.error("JMSException:", ex);
        }
        DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (deviceInfo.getDeviceType().contains("DF")) {
            recipeName = deviceInfoExt.getRecipeName();
        }
        Recipe goldRecipe = recipeService.getGoldRecipe(recipeName, deviceCode, deviceInfo.getDeviceType());
        if (goldRecipe == null) {
            UiLogUtil.appendLog2SeverTab(deviceCode, "工控上不存在： " + recipeName + " 的Gold版本，无法更新版本号！请联系PE处理！");
            return;
        }
        EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceInfo.getDeviceCode());
        equipHost.lotId = lotId;
        Map map = new HashMap();
        map.put("WorkLot", lotId);
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
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }
}
