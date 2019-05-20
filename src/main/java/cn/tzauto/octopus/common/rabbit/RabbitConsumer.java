package cn.tzauto.octopus.common.rabbit;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by leo on 2018-06-12.
 */
public class RabbitConsumer {

    public void listenQueueMsg(String queueName) throws IOException {
        Connection connection = MessageUtils.getConnection();
        Channel channel = connection.createChannel(1);
        channel.queueDeclare(queueName, true, false, false, null);
//        StringBuffer message = new StringBuffer();
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.handleDelivery(consumerTag, envelope, properties, body);
//                message.append(new String(body, "UTF-8"));
                System.out.println("receive queue msg："+new String(body, "UTF-8"));
            }
        };
        //监听队列，当b为true时，为自动提交（只要消息从队列中获取，无论消费者获取到消息后是否成功消息，都认为是消息已经成功消费），
        // 当b为false时，为手动提交（消费者从队列中获取消息后，服务器会将该消息标记为不可用状态，等待消费者的反馈，
        // 如果消费者一直没有反馈，那么该消息将一直处于不可用状态。
        //如果选用自动确认,在消费者拿走消息执行过程中出现宕机时,消息可能就会丢失！！）
        //使用channel.basicAck(envelope.getDeliveryTag(),false);进行消息确认
        channel.basicConsume(queueName, true, consumer);
    }

    public void listenTopicName(String exchangeName,String queueName,String topicName) throws IOException {
        Connection connection = MessageUtils.getConnection();
        Channel channel = connection.createChannel();

        //声明队列
        channel.queueDeclare(queueName,true,false,false,null);

        /*
            绑定队列到交换机（这个交换机名称一定要和生产者的交换机名相同）
            参数1：队列名
            参数2：交换机名
            参数3：Routing key 路由键
         */
        channel.queueBind(queueName,exchangeName,topicName);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                super.handleDelivery(consumerTag, envelope, properties, body);
                String message = new String(body,"UTF-8");
                System.out.println("receive topic msg："+message);
                channel.basicAck(envelope.getDeliveryTag(),false);
            }
        };
        channel.basicConsume(queueName,false,consumer);
    }

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        String queueName = "queue_demo";
        String exchangeName = "exchange_demo";
        RabbitConsumer consumer = new RabbitConsumer();
//        consumer.listenQueueMsg(queueName);
        consumer.listenTopicName(exchangeName,queueName,"");
    }

}
