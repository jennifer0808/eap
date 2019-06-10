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
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rain
 */
public class FindEquipStatusHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(FindEquipStatusHandler.class);
    private String deviceCode = "";
    private String equipStatus = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            deviceCode = mapMessage.getString("deviceCode");
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求核对设备的运行状态");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            equipStatus = hostManager.getEquipStatus(deviceInfo.getDeviceCode());
            logger.info("设备:" + deviceCode + "当前运行状态为:" + equipStatus);
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "FindEquipStatus");
        mqMap.put("EquipStatus", equipStatus);
        try {
            Destination destination = message.getJMSReplyTo();
            logger.info("destination:================================ " + destination.toString());
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            logger.info("topicName:==========================================" + topicName);
            GlobalConstants.C2SCheckRcpNameQueue.sendMessage(topicName, mqMap);
            logger.info("向服务端[" + topicName + "]回复校验设备运行状态" + JSONArray.toJSONString(mqMap));
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送设备运行状态" + equipStatus);
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        } finally {
            sqlSession.close();
        }
    }
}
