package cn.tzauto.octopus.common.mq;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.common.mq.common.MQConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageTest {

    public static void main(String[] args) {
        MQConstants.initConenction();
        MessageUtils C2SSpecificDataQueue = new MessageUtils("C2S.Q.EQPT_LOG_D151");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long count=0;
        while (true) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            for (int j = 0; j < 4; j++) {
                for (int i = 1; i < 6; i++) {
                    DeviceInfoExt deviceInfoExt = new DeviceInfoExt();
                    deviceInfoExt.setDeviceRowid("test" + i);
                    deviceInfoExt.setDeviceStatus("Idle" + j);
                    Map mqMap = new HashMap();
                    mqMap.put("msgName", "eqpt.EqptStatusChange");
                    mqMap.put("deviceCode", "test" + i);
                    mqMap.put("eventName", "eqpt.EqptStatusChange");
                    mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
                    mqMap.put("deviceCeid", "0");
                    mqMap.put("eventDesc", "");
                    mqMap.put("eventDate", dateFormat.format(new Date()));
                    DeviceOplog deviceOplog = new DeviceOplog();
                    deviceOplog.setId(UUID.randomUUID().toString());
                    deviceOplog.setDeviceCode("test" + i);
                    deviceOplog.setDeviceRowid("test" + i);
                    mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
                    C2SSpecificDataQueue.sendMessage(mqMap);
                    count++;
                    System.out.println("发送=== 【"+count+" 】条");
                }
            }
        }
    }
}
