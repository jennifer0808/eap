package cn.tzauto.octopus.common.mq;

import cn.tzauto.octopus.common.mq.common.MessageHandler;
import org.apache.activemq.ActiveMQConnection;

import javax.jms.Destination;
import javax.jms.Message;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Chase on 2016/7/24.
 */
public class MessageUtils {

    private ProducerServiceImpl producer;//生产者
    private ConsumerServiceImpl consumer;//消费者
    private String destination;//目的地
    private MessageHandler messageHandler;//消息处理器
    //中间件参数
    private static String brokerUrl;//中间件地址
    private static String userName;//用户名
    private static String password;//密码
    //生产者参数
    private static int maxConnections;//最大连接数
    private static int maximumActiveSessionPerConnection;//每个连接中最大活动会话数
    private static int threadPoolSize;//线程池中的线程数量
    private static boolean useAsyncSendForJMS;//是否使用同步消息
    private static boolean isPersistent;//是否使用持久化消息
    //消费者参数
    private static int queuePrefetch;//队列预取策略

    static {
        try {
            InputStream in = MessageUtils.class.getClassLoader().getResourceAsStream("mq.properties");
//            File f = new File(MessageUtils.class.getResource("").getPath()+"/"+"mq.properties");
//            InputStream in =new FileInputStream(f);
            Properties prop = new Properties();
            prop.load(in);
            brokerUrl = prop.getProperty("brokerUrl");
            userName = prop.getProperty("username");
            password = prop.getProperty("password");
            maxConnections = Integer.parseInt(prop.getProperty("maxConnections"));
            maximumActiveSessionPerConnection = Integer.parseInt(prop.getProperty("maximumActiveSessionPerConnection"));
            threadPoolSize = Integer.parseInt(prop.getProperty("threadPoolSize"));
            useAsyncSendForJMS = Boolean.parseBoolean(prop.getProperty("useAsyncSendForJMS"));
            isPersistent = Boolean.parseBoolean(prop.getProperty("isPersistent"));
            queuePrefetch = Integer.parseInt(prop.getProperty("queuePrefetch"));

            if (userName == null || userName.equals("")) {
                userName = ActiveMQConnection.DEFAULT_USER;
            }
            if (password == null || password.equals("")) {
                password = ActiveMQConnection.DEFAULT_PASSWORD;
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * 设置中间件信息，此方法为备用方法，因为类初始化时会加载中间件信息
     *
     * @param brokerUrl
     * @param username
     * @param password
     */
    public void setProperties(String brokerUrl, String username, String password) {
        MessageUtils.brokerUrl = brokerUrl;
        MessageUtils.userName = username;
        MessageUtils.password = password;
    }

    /**
     * 构造器，destination即目的地是必须的参数，而监听器和处理器不是，监听器和处理器用于消费者
     *
     * @param destination
     */
    public MessageUtils(String destination) {
        this(destination, null);
    }

    public MessageUtils(String destination, MessageHandler messageHandler) {
        this.destination = destination;
        this.messageHandler = messageHandler;

        this.producer = new ProducerServiceImpl(brokerUrl, userName, password, maxConnections, maximumActiveSessionPerConnection, threadPoolSize, useAsyncSendForJMS, isPersistent);
        this.consumer = new ConsumerServiceImpl(brokerUrl, userName, password, queuePrefetch);
    }

    /**
     * getter、setter
     *
     * @return
     */
    public String getDestination() {
        return this.destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public MessageHandler getMessageHandler() {
        return this.messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * 发送消息
     *
     * @param map
     * @return
     */
    public boolean sendMessage(Map<String, String> map) {
        producer.produce(destination, map, "send");
        return true;
    }

    /**
     * 发送消息
     *
     * @param map
     * @return
     */
    public boolean sendMessage(String destination, Map<String, String> map) {
        producer.produce(destination, map, "send");
        return true;
    }

    /**
     * 发送消息
     *
     * @param map
     * @return
     */
    public boolean sendMessage(Destination destination, Map<String, String> map) {
        producer.produce(destination, map, "send");
        return true;
    }

    /**
     * 发布消息
     *
     * @param map
     */
    public boolean publishMessage(Map<String, String> map) {
        producer.produce(destination, map, "publish");
        return true;
    }

    /**
     * 接收消息并处理
     *
     * @throws Exception 如果处理器没有初始化，则抛出异常
     */
    public void receiveMessage() throws Exception {
        if (messageHandler == null) {
            throw new Exception("No MessageHandler.");
        }
        consumer.setMessageListener(new MultiThreadMessageListener(messageHandler));
        consumer.consumeMsg(destination, "receive");
    }

    /**
     * 订阅消息
     *
     * @throws Exception 如果处理器没有初始化，则抛出异常
     */
    public void subscribeMessage() throws Exception {
        if (messageHandler == null) {
            throw new Exception("No MessageHandler");
        }
        consumer.setMessageListener(new MultiThreadMessageListener(messageHandler));
        consumer.consumeMsg(destination, "subscribe");
    }

    public Message sendMessageWithReplay(Map<String, String> map) throws Exception {
        return producer.produceMsgAndConsume(destination, map);
    }

}
