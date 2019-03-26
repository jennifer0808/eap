
package cn.tzauto.octopus.common.mq;


import cn.tzauto.octopus.common.mq.common.MessageHandler;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

public class ReciveMessage {
	MessageUtils messageUtils = new MessageUtils("TestQueue");
	/**
	 * P2Pģʽ ��������
	 */ 
	public void startsListener(){
		//��spring������ʼ����ɺ�ͻ�ִ�и÷�����
		messageUtils.setMessageHandler(new MessageHandler() {
			@Override
			public void handle(Message message) {
				// TODO Auto-generated method stub
				MapMessage mapMessage = (MapMessage)message;
				try {
					System.out.println(mapMessage.getString("hello"));
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		try {
			//messageUtils.receiveMessage();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
