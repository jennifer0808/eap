/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.common.mq.common.MessageHandler;
import javax.jms.Message;
import org.apache.log4j.Logger;

public class StationAlarmHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(StationAlarmHandler.class);

    @Override
    public void handle(Message message) {
        logger.info("接收到服务端更新StationAlarm配置请求...");
        throw new UnsupportedOperationException("接收到服务端更新StationAlarm配置请求...");
    }
}
