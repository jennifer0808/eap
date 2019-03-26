package cn.tzauto.octopus.common.mq.common;


import javax.jms.Message;

/**
 * Created by Chase on 2016/7/24.
 */
public interface MessageHandler {

    /**
     * 接收消息后的处理事件
     * @param message
     */
    public void handle(Message message);
}
