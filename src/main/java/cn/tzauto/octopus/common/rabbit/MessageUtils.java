package cn.tzauto.octopus.common.rabbit;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Created by leo on 2019-05-17.
 */
public class MessageUtils {

    //生产者
    private RabbitProducer producer;
    //消费者
    private RabbitConsumer consumer;

    private static String ipAddress;
    private static int port;
    private static String userName;
    private static String userPwd;

    private static Address[] addressList;

    private static ConnectionFactory connectionFactory = null;

    static {
        InputStream in = MessageUtils.class.getClassLoader().getResourceAsStream("rabbit.properties");
        Properties prop = new Properties();
        try {
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ipAddress = prop.getProperty("ipAddress");
        port = Integer.parseInt(prop.getProperty("port"));
        String[] ipList = ipAddress.split(",");
        addressList = new Address[ipList.length];
        for (int i = 0; i < ipList.length; i++) {
            Address tmp = new Address(ipList[i], port);
            addressList[i] = tmp;
        }
        userName = prop.getProperty("userName");
        userPwd = prop.getProperty("userPwd");

        connectionFactory = new ConnectionFactory();

        connectionFactory.setUsername(userName);
        connectionFactory.setPassword(userPwd);
        connectionFactory.setAutomaticRecoveryEnabled(true);
        connectionFactory.setTopologyRecoveryEnabled(true);
    }

    public static Connection getConnection() {
        Connection connection = null;
        try {
            connection = connectionFactory.newConnection(addressList);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public MessageUtils(){

    }

    public MessageUtils(String queueName, String exchangeName, String mode) {
        if ("1".equalsIgnoreCase(mode)) {
            this.producer = new RabbitProducer(queueName);
        } else if ("2".equalsIgnoreCase(mode)) {
            this.consumer = new RabbitConsumer(queueName, exchangeName);
        }
    }


    /**
     * 发送消息
     *
     * @param map
     * @return
     */
    public boolean sendMessage(Map<String, String> map) {
        String msg = JsonMapper.toJsonString(map);
        if ("1".equals(GlobalConstants.getProperty("MQ_LISTEN"))) {
            producer.sendMessage(msg);
        }
        return true;
    }

    /**
     * 发送消息
     *
     * @param map
     * @return
     */
    public boolean sendMessage(String destination, Map<String, String> map) {
        String msg = JsonMapper.toJsonString(map);
        if ("1".equals(GlobalConstants.getProperty("MQ_LISTEN"))) {
            producer.sendQueueMessage(destination, msg);
        }
        return true;
    }


    public HashMap<String, String> sendMessageWithReplay(Map<String, String> map) throws Exception {
        String msg = JsonMapper.toJsonString(map);
        return producer.sendAwaitMessage(msg);
    }


    /**
     * 订阅消息
     *
     * @throws Exception 如果处理器没有初始化，则抛出异常
     */
    public void subscribeMessage(String routingKey) throws Exception {
        consumer.subscribeMessage(routingKey);
    }

    public void subscribeMessage(String exchangeName,String queueName,String routingKey) throws Exception {
        this.consumer = new RabbitConsumer(queueName, exchangeName);
        consumer.subscribeMessage(routingKey);
    }

    public void replyMessage(String queueName, String corId, Map<String, String> map) {
        String msg = JsonMapper.toJsonString(map);
        producer.replyMessage(queueName, corId, msg);
    }

}
