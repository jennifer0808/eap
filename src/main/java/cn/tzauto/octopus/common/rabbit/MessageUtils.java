package cn.tzauto.octopus.common.rabbit;

import cn.tzauto.octopus.common.util.tool.JsonMapper;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

/**
 * Created by leo on 2019-05-17.
 */
public class MessageUtils {

    private RabbitProducer producer;//生产者

    private static String ipAddress;
    private static int port;
    private static String userName;
    private static String userPwd;

    private static Address[] addressList;

    private static Connection connection = null;

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
        for (int i = 0; i < ipList.length; i++) {
            Address tmp = new Address(ipList[i], port);
            addressList[i] = tmp;
        }
        userName = prop.getProperty("userName");
        userPwd = prop.getProperty("userPwd");

    }

    public static Connection getConnection() {

        if (connection == null) {
            ConnectionFactory connectionFactory = new ConnectionFactory();


            connectionFactory.setUsername(userName);
            connectionFactory.setPassword(userPwd);
            connectionFactory.setAutomaticRecoveryEnabled(true);
            connectionFactory.setTopologyRecoveryEnabled(true);
            try {
                connection = connectionFactory.newConnection(addressList);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        return connection;
    }

    public MessageUtils(String destination) {
        this.producer = new RabbitProducer(destination);
    }


    /**
     * 发送消息
     *
     * @param map
     * @return
     */
    public boolean sendMessage(Map<String, String> map) {
        String msg = JsonMapper.toJsonString(map);
        producer.sendMessage(msg);
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
        producer.sendQueueMessage(destination, msg);
        return true;
    }

}
