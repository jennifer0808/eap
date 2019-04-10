/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author luosy
 */
public class SpecificDataTransferHandler implements MessageHandler {

    private static final Logger logger = Logger.getLogger(SpecificDataTransferHandler.class);
    private String deviceCode = "";
    private Map<String, String> dataIdMap = null;

    @Override
    public void handle(Message message) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            MapMessage mapMessage = (MapMessage) message;
            deviceCode = mapMessage.getString("deviceCode");
            dataIdMap = (HashMap<String, String>) JsonMapper.fromJsonString(mapMessage.getString("dataIdList"), HashMap.class);
            logger.info("服务端请求获取设备[" + deviceCode + "]的指定数据，数据ID:" + JsonMapper.toJsonString(dataIdMap));
//            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求从设备获取数据...");
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            Map resultMap = GlobalConstants.stage.hostManager.getSpecificData(deviceInfo.getDeviceCode(), dataIdMap);
            Map mqMap = new HashMap();
            String resultMapString = "";
            if (resultMap != null) {
                if (dataIdMap.size() > resultMap.size()) {
                    resultMapString = JsonMapper.toJsonString(resultMap);
                    logger.info("设备 " + deviceCode + " 部分数据ID无法取值,需要逐个测试");
                } else {
                    resultMapString = JsonMapper.toJsonString(resultMap);
                    mqMap.put("eventDesc", "获取到设备的数据");
                }
            } else {
                mqMap.put("eventDesc", "从设备数据失败，请重试！");
            }
            mqMap.put("SpecificData", resultMapString);
            Destination destination = message.getJMSReplyTo();
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            GlobalConstants.C2SSpecificDataQueue.sendMessage(topicName, mqMap);
            logger.info("向服务端发送获取到的数据:[" + resultMapString + "]");
//            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端发送获取到的数据");
        } catch (JMSException ex) {
            logger.error("Execption occur:" + ex);
        } finally {
            sqlSession.close();
        }
    }
}
