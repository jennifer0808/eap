package cn.tzauto.octopus.common.rabbit;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.rabbit.handlers.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import com.rabbitmq.client.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by leo on 2018-06-12.
 */
public class RabbitConsumer {

    private static Logger mqLogger = Logger.getLogger("mqLog");

    private String queueName;
    private String exchangeName;

    public RabbitConsumer(String queueName, String exchangeName) {
        this.queueName = queueName;
        this.exchangeName = exchangeName;
    }

    public void listenQueueMsg(String queueName) {
        Connection connection = MessageUtils.getConnection();
        Channel channel = null;
        try {
            channel = connection.createChannel(1);
            channel.queueDeclare(queueName, true, false, false, null);
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
                    System.out.println("receive queue msg：" + new String(body, "UTF-8"));
                }
            };
            //监听队列，当b为true时，为自动提交（只要消息从队列中获取，无论消费者获取到消息后是否成功消息，都认为是消息已经成功消费），
            // 当b为false时，为手动提交（消费者从队列中获取消息后，服务器会将该消息标记为不可用状态，等待消费者的反馈，
            // 如果消费者一直没有反馈，那么该消息将一直处于不可用状态。
            //如果选用自动确认,在消费者拿走消息执行过程中出现宕机时,消息可能就会丢失！！）
            //使用channel.basicAck(envelope.getDeliveryTag(),false);进行消息确认
            channel.basicConsume(queueName, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribeMessage() {
        listenTopicName(exchangeName, queueName, "");
    }

    public void listenTopicName(String exchangeName, String queueName, String topicName) {
        Connection connection = MessageUtils.getConnection();
        try {
            Channel channel = connection.createChannel();
            //声明队列
            channel.queueDeclare(queueName, true, false, false, null);

        /*
            绑定队列到交换机（这个交换机名称一定要和生产者的交换机名相同）
            参数1：队列名
            参数2：交换机名
            参数3：Routing key 路由键
         */
            channel.queueBind(queueName, exchangeName, "");

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    super.handleDelivery(consumerTag, envelope, properties, body);
                    String message = new String(body, "UTF-8");
                    System.out.println("receive topic msg：" + message);
                    HashMap<String, String> msgMap = (HashMap<String, String>) JsonMapper.fromJsonString(message, HashMap.class);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                    String deviceTypeId = msgMap.get("deviceTypeId");
                    String deviceCode = msgMap.get("deviceCode");
                    String msgName = msgMap.get("msgName");
                    mqLogger.info("接收到mq==================================,deviceCode：" + deviceCode + "，msgName:" + msgName + "，deviceTypeId:" + deviceTypeId);
                    if (isInvoke(deviceCode, deviceTypeId)) {

                        mqLogger.info("开始处理MQ请求...");
                        try {
                            MessageHandler messageHandler = (MessageHandler) Class.forName("cn.tzauto.octopus.common.rabbit.handlers." + msgName.replaceAll("\"", "") + "Handler").newInstance();
                            messageHandler.handle(msgMap);
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (InstantiationException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (T3TimeOutException e) {
                            e.printStackTrace();
                        } catch (StreamFunctionNotSupportException e) {
                            e.printStackTrace();
                        } catch (MessageDataException e) {
                            e.printStackTrace();
                        } catch (HsmsProtocolNotSelectedException e) {
                            e.printStackTrace();
                        } catch (ItemIntegrityException e) {
                            e.printStackTrace();
                        } catch (T6TimeOutException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (BrokenProtocolException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mqLogger.info("MQ请求处理结束...");
                    }


//                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };
            channel.basicConsume(queueName, false, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isInvoke(String deviceCode, String deviceTypeId) {

        try {
            if ("All".equals(deviceCode)) {
                return true;
            } else {
                if (!"".equals(deviceTypeId)) {
                    for (DeviceInfo deviceInfo : GlobalConstants.stage.hostManager.deviceInfos) {
                        if (deviceInfo.getDeviceTypeId().equals(deviceTypeId)) {
                            return true;
                        }
                    }
                }
                if (!"".equals(deviceCode)) {
                    for (DeviceInfo deviceInfo : GlobalConstants.stage.hostManager.deviceInfos) {
                        if (deviceInfo.getDeviceCode().equals(deviceCode)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            mqLogger.error("Exception", e);
        }
        return false;
    }

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        String queueName = "S2C.T.RECIPE_C";
        String exchangeName = "S2C.T.EXCHANGE1";
        RabbitConsumer consumer = new RabbitConsumer( queueName,exchangeName);
//        consumer.listenQueueMsg(queueName);
        consumer.subscribeMessage();
    }

}
