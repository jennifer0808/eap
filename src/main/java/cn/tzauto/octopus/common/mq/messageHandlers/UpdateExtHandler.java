/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import com.alibaba.fastjson.JSONArray;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class UpdateExtHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpdateExtHandler.class);
    private String deviceCode = "";
    private String lockSwitch = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        try {
            deviceCode = mapMessage.getString("deviceCode");
            lockSwitch = mapMessage.getString("lockSwitch");
        } catch (JMSException ex) {
            logger.error("JMSException:", ex);
        }
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            int updateCount = 0;
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置，DEVICE_CODE:" + deviceCode);
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在该设备模型信息，请联系ME处理！");
            } else {
                deviceInfoExt.setLockSwitch(lockSwitch);
                updateCount = deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            sqlSession.commit();

            Map mqMap = new HashMap();
            mqMap.put("msgName", "UpdateExt");
            mqMap.put("updateCount", String.valueOf(updateCount));
            Destination destination = message.getJMSReplyTo();
            logger.info("destination:================================ " + destination.toString());
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            GlobalConstants.C2SSpecificDataQueue.sendMessage(topicName, mqMap);//C2SCheckRcpNameQueue
            logger.info("向服务端[" + topicName + "]回复锁机开关更改结果:" + JSONArray.toJSONString(mqMap));
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "Server修改锁机开关为：[" + lockSwitch + "]");
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }
}
