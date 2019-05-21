/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 */
public class MqTestHandler implements MessageHandler {

    String deviceCode = "";
    String number = "";
    String getMessage = "";
    String sendMessage = "";

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {

        deviceCode = msgMap.get("deviceCode");
        number = msgMap.get("number");

        System.out.println(Thread.currentThread().getName() + "====11111========" + deviceCode + "====================");
//            Thread.sleep(5000);
        System.out.println(Thread.currentThread().getName() + "====22222========" + number + "====================");

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
