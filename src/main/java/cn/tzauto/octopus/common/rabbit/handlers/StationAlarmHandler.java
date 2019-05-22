/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

public class StationAlarmHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(StationAlarmHandler.class);


    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        logger.info("接收到服务端更新StationAlarm配置请求...");
        throw new UnsupportedOperationException("接收到服务端更新StationAlarm配置请求...");
    }
}
