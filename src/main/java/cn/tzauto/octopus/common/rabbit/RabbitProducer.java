package cn.tzauto.octopus.common.rabbit;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by leo on 2018-06-12.
 */
public class RabbitProducer {

    private String queueName;

    public RabbitProducer(String queueName) {
        this.queueName = queueName;
    }

    public void sendMessage(String message) {
        sendQueueMessage(queueName, message);
    }

    public void sendQueueMessage(String queueName, String message) {
        //获取连接
        Connection connection = MessageUtils.getConnection();
        System.out.println(connection);
        //创建通道
        Channel channel = null;
        try {
            channel = connection.createChannel(1);
            /*
         * 声明（创建）队列
         * 参数1：队列名称
         * 参数2：为true时server重启队列不会消失
         * 参数3：队列是否是独占的，如果为true只能被一个connection使用，其他连接建立时会抛出异常
         * 参数4：队列不再使用时是否自动删除（没有连接，并且没有未处理的消息)
         * 参数5：建立队列时的其他参数
         */
            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicPublish("", queueName, null, message.getBytes());
            System.out.println("send queue msg：" + message);
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

    public void sendExchangeMsg(String exchangeName, String message) {
        Connection connection = MessageUtils.getConnection();
        Channel channel = null;
        try {
            channel = connection.createChannel();
        /*
            声明exchange交换机
            参数1：交换机名称
            参数2：交换机类型
            参数3：交换机持久性，如果为true则服务器重启时不会丢失
            参数4：交换机在不被使用时是否删除
            参数5：交换机的其他属性
         */
            channel.exchangeDeclare(exchangeName, "direct", true, false, null);

            channel.basicPublish(exchangeName, "", null, message.getBytes());
            System.out.println("send exchange msg：" + message);
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        String queueName = "queue_demo";
        RabbitProducer producer = new RabbitProducer(queueName);
        String message = "Hello World!";
        String exchangeName = "exchange_demo";
        producer.sendQueueMessage(queueName, message);
//            producer.sendExchangeMsg(exchangeName, message);
    }

}
