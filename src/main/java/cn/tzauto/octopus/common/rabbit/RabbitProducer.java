package cn.tzauto.octopus.common.rabbit;

import cn.tzauto.octopus.common.util.tool.JsonMapper;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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
            channel.exchangeDeclare(exchangeName, "fanout", true, false, null);
            BasicProperties properties = new AMQP.BasicProperties();
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

    public HashMap<String, String> sendAwaitMessage(String message) {
        //获取连接
        Connection connection = MessageUtils.getConnection();
        //创建通道
        Channel channel = null;
        String result = "";

        final String corrId = UUID.randomUUID().toString();
        final String replyQueueName;
        try {
            replyQueueName = channel.queueDeclare().getQueue();
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();
            channel.basicPublish("", queueName, props, message.getBytes("UTF-8"));
            final BlockingQueue<String> response = new ArrayBlockingQueue<String>(1);
            channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    if (properties.getCorrelationId().equals(corrId)) {
                        response.offer(new String(body, "UTF-8"));
                    }
                }
            });
            channel.close();
            connection.close();
            //json格式的map
            result = response.take();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        HashMap<String, String> resultMap = new HashMap<>();

        if(!"".equalsIgnoreCase(result)){
            resultMap = (HashMap<String, String>) JsonMapper.fromJsonString(result,HashMap.class);
        }
        return resultMap;
    }

    public void replyMessage(String queueName,String corId, String message) {
//        channel.basicPublish("", properties.getReplyTo(), properties, (msg + ", resp is：" + resp).getBytes());


        //获取连接
        Connection connection = MessageUtils.getConnection();
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
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corId)
                    .build();
            channel.basicPublish("", queueName, props, message.getBytes());
            System.out.println("send queue msg：" + message);
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        String queueName = "S2C.T.RECIPE_C";
        RabbitProducer producer = new RabbitProducer(queueName);
        String message = "Hello World!";
        String exchangeName = "S2C.T.EXCHANGE1";
//        producer.sendQueueMessage(queueName, message);
            producer.sendExchangeMsg(exchangeName, message);
    }

}
