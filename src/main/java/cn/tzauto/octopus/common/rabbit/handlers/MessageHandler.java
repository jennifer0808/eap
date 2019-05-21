package cn.tzauto.octopus.common.rabbit.handlers;


import cn.tzauto.generalDriver.exceptions.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Chase on 2016/7/24.
 */
public interface MessageHandler {

    /**
     * 接收消息后的处理事件
     *
     * @param msgMap
     */
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException;
}
