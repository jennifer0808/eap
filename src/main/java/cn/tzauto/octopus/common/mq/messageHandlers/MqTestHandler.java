/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;

import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

/**
 *
 */
public class MqTestHandler implements MessageHandler {

    String deviceCode = "";
    String number = "";
    String getMessage = "";
    String sendMessage = "";

    @Override
    public void handle(Message message) {
        System.out.println("================================");
        MapMessage mapMessage = (MapMessage) message;
        try {
            deviceCode = String.valueOf(mapMessage.getString("deviceCode"));
            number = String.valueOf(mapMessage.getString("number"));

            System.out.println(Thread.currentThread().getName() + "====11111========" + number + "====================");

            Thread.sleep(5000);
            System.out.println(Thread.currentThread().getName() + "====22222========" + number + "====================");
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (JMSException ex) {
            ex.printStackTrace();
        }
        sendMessage = "已收到" + "deviceCode:" + "," + deviceCode + "number:" + number;
        getMessage = "已发送" + "deviceCode:" + "," + deviceCode + "number:" + number;
        Map mqMap = new HashMap<>();
        mqMap.put("msgName", "CheckMq");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("sendMessage", sendMessage);
        mqMap.put("getMessage", getMessage);
        GlobalConstants.C2SRcpQueue.sendMessage(mqMap);
    }
}
