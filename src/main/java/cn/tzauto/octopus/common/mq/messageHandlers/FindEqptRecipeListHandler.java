/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author lsy
 * @ 2017/08/02
 */
public class FindEqptRecipeListHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(FindEqptRecipeListHandler.class);

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            String deviceCode = mapMessage.getString("deviceCode");
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求获取设备上的Recipe列表...");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            String deviceId = deviceInfo.getDeviceCode();
            List equipRecipeList = new ArrayList();
            Map equipRecipeListState = hostManager.getRecipeListFromDevice(deviceId);
            if (equipRecipeListState != null) {
                equipRecipeList = (List) equipRecipeListState.get("eppd");
            }
            Map mqMap = new HashMap();
            mqMap.put("msgName", "FindEqptRecipeList");
            mqMap.put("EqptRecipeList", JSONArray.toJSONString(equipRecipeList));
            Destination destination = message.getJMSReplyTo();
            logger.info("destination:================================ " + destination.toString());
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            logger.info("topicName:==========================================" + topicName);
            GlobalConstants.C2SRcpUpLoadQueue.sendMessage(topicName, mqMap);
            logger.info("向服务端[" + topicName + "]回复设备的当前Recipe列表" + JSONArray.toJSONString(mqMap));
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送设备的当前Recipe列表:" + equipRecipeList);
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        } finally {
            sqlSession.close();
        }

    }
}
