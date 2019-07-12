package cn.tzauto.octopus.common.mq.common;


import cn.tzauto.generalDriver.exceptions.*;

import javax.jms.Message;
import java.io.IOException;

/**
 * Created by Chase on 2016/7/24.
 */
public interface MessageHandler {

    /**
     * 接收消息后的处理事件
     * @param message
     */
    public void handle(Message message) throws IOException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, InterruptedException, StateException, IntegrityException, InvalidDataException;
}
