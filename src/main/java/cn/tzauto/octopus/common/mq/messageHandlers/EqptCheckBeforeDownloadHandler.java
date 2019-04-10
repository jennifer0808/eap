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
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Weiqy
 */
public class EqptCheckBeforeDownloadHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(EqptCheckBeforeDownloadHandler.class.getName());
    private String deviceCode = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "EqptCheckBeforeDownload");
        String flag = "N";
        try {
            deviceCode = mapMessage.getString("deviceCode");
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求核对设备的当前通信状态");
            DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            Map resultMap = GlobalConstants.stage.hostManager.getEquipInitState(deviceInfo.getDeviceCode());
            if (resultMap != null && !resultMap.isEmpty()) {
                flag = "Y";
                UiLogUtil.appendLog2SeverTab(deviceCode, "设备通信正常，可以正常改机");
            }
            mqMap.put("flag", flag);
            if ("Y".equals(flag)) {
                mqMap.put("msg", "设备通信正常");
            } else {
                mqMap.put("msg", "设备通信异常，请检查设备通信状态");
            }
            sendMsg2Server(message, mqMap);
        } catch (JMSException ex) {
            logger.error("JMSException", ex);
            mqMap.put("flag", flag);
            mqMap.put("msg", "设备通信异常，请检查设备通信状态");
            sendMsg2Server(message, mqMap);
        }

    }

    public void sendMsg2Server(Message message, Map mqMap) {
        try {
            Destination destination = message.getJMSReplyTo();
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            GlobalConstants.C2SRcpDownLoadQueue.sendMessage(topicName, mqMap);
            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端发送设备当前通信状态:" + mqMap.get("flag"));
        } catch (JMSException ex) {
            logger.error("JMSException", ex);
        }
    }
}
