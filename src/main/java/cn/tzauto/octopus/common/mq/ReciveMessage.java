
package cn.tzauto.octopus.common.mq;


import cn.tzauto.octopus.common.mq.common.MessageHandler;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

public class ReciveMessage {
    MessageUtils messageUtils = new MessageUtils("TestQueue");

    public void startsListener() {

        messageUtils.setMessageHandler(new MessageHandler() {
            @Override
            public void handle(Message message) {

                MapMessage mapMessage = (MapMessage) message;
                try {
                    System.out.println(mapMessage.getString("hello"));
                } catch (JMSException e) {

                    e.printStackTrace();
                }
            }
        });
        try {
            //messageUtils.receiveMessage();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
