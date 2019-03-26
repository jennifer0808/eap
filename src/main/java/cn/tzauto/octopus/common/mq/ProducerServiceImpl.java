package cn.tzauto.octopus.common.mq;

import cn.tzauto.octopus.common.mq.common.MQConstants;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

/**
 * Created by Chase on 2016/7/24.
 */
public class ProducerServiceImpl implements ExceptionListener {
    //最大连接数

    public final static int DEFAULT_MAX_CONNECTIONS = 5;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    //每个连接中最大活动会话数    
    public final static int DEFAULT_MAXIMUM_ACTIVE_SESSION_PER_CONNECTION = 10;
    private int maximumActiveSessionPerConnection = DEFAULT_MAXIMUM_ACTIVE_SESSION_PER_CONNECTION;
    //线程池中的线程数    
    public final static int DEFAULT_THREAD_POOL_SIZE = 20;
    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    //是否使用同步返回数据的格式    
    public final static boolean DEFAULT_USE_ASYNC_SEND_FOR_JMS = true;
    private boolean useAsyncSendForJMS = DEFAULT_USE_ASYNC_SEND_FOR_JMS;
    //是否持久化消息    
    public final static boolean DEFAULT_IS_PERSISTENT = true;
    private boolean isPersistent = DEFAULT_IS_PERSISTENT;
    //连接地址
    private String brokerUrl;
    //用户名
    private String userName;
    //密码
    private String password;
    //线程池
    private ExecutorService threadPool;
    //连接池工厂
    private PooledConnectionFactory connectionFactory;
    private DestinationResolver destinationResolver = new DynamicDestinationResolver();
    private boolean pubSubDomain = false;

    public ProducerServiceImpl(String brokerUrl, String userName, String password) {
        this(brokerUrl, userName, password, DEFAULT_MAX_CONNECTIONS, DEFAULT_MAXIMUM_ACTIVE_SESSION_PER_CONNECTION, DEFAULT_THREAD_POOL_SIZE, DEFAULT_USE_ASYNC_SEND_FOR_JMS, DEFAULT_IS_PERSISTENT);
    }

    public ProducerServiceImpl(String brokerUrl, String userName, String password, int maxConnections, int maximumActiveSessionPerConnection, int threadPoolSize, boolean useAsyncSendForJMS, boolean isPersistent) {
        this.useAsyncSendForJMS = useAsyncSendForJMS;
        this.isPersistent = isPersistent;
        this.brokerUrl = brokerUrl;
        this.userName = userName;
        this.password = password;
        this.maxConnections = maxConnections;
        this.maximumActiveSessionPerConnection = maximumActiveSessionPerConnection;
        this.threadPoolSize = threadPoolSize;
        init();
    }

    private void init() {
        //设置线程池
        this.threadPool = Executors.newFixedThreadPool(this.threadPoolSize);
        //ActiveMQ连接工厂
        ActiveMQConnectionFactory actualConnectionFactory = new ActiveMQConnectionFactory(this.userName, this.password, this.brokerUrl);
        actualConnectionFactory.setWatchTopicAdvisories(false);
        actualConnectionFactory.setUseAsyncSend(this.useAsyncSendForJMS);
        //ActiveMQ连接池工厂
        this.connectionFactory = new PooledConnectionFactory(actualConnectionFactory);
        this.connectionFactory.setCreateConnectionOnStartup(true);
        this.connectionFactory.setMaxConnections(this.maxConnections);
        this.connectionFactory.setMaximumActiveSessionPerConnection(this.maximumActiveSessionPerConnection);
    }

    /**
     * 执行发送消息的具体方法
     *
     * @param dest 目的地，即Topic或者Queue的名称
     * @param map Map消息
     * @param way 发送消息的形式，"publish"表示发布(Topic)，其他表示发送(Queue)
     */
    public void produce(final Destination dest, final Map<String, String> map, final String way) {
        ////使用线程池来执行具体的调用
        this.threadPool.execute(new Runnable() {

            public void run() {
                try {
                    produceMsg(dest, map, way);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 执行发送消息的具体方法
     *
     * @param dest 目的地，即Topic或者Queue的名称
     * @param map Map消息
     * @param way 发送消息的形式，"publish"表示发布(Topic)，其他表示发送(Queue)
     */
    public void produce(final String dest, final Map<String, String> map, final String way) {
        ////使用线程池来执行具体的调用
        this.threadPool.execute(new Runnable() {

            public void run() {
                try {
                    produceMsg(dest, map, way);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 真正的执行消息发送的方法
     *
     * @param dest 目的地，即Topic或者Queue的名称
     * @param map Map消息
     * @param way 发送消息的形式，"publish"表示发布(Topic)，其他表示发送(Queue)
     * @throws Exception
     */
    private void produceMsg(String dest, Map<String, String> map, String way) throws Exception {

        Connection connection = null;
        Session session = null;
        try {
            //从连接池工厂中获取一个连接
            connection = MQConstants.defaultPoolConnectionFactory.createConnection();

            //false 参数表示 为非事务型消息，后面的参数表示消息的确认类型
            session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);

            //根据way决定Destination是话题(Topic)还是队列(Queue)
            Destination destination = null;
            if ("publish".equalsIgnoreCase(way)) {
                destination = session.createTopic(dest);
            } else {
                destination = session.createQueue(dest);//默认是队列
            }

            //生产者
            MessageProducer producer = session.createProducer(destination);
            //设置消息持久化
            producer.setDeliveryMode(this.isPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            //解析消息
            Message message = getMessage(session, map);
            //发送消息
            producer.send(message);
        } finally {
            //生产者生产消息后关闭会话和连接
            closeSession(session);
            closeConnection(connection);
        }
    }

    /**
     * 真正的执行消息发送的方法
     *
     * @param dest 目的地，即Topic或者Queue的名称
     * @param map Map消息
     * @param way 发送消息的形式，"publish"表示发布(Topic)，其他表示发送(Queue)
     * @throws Exception
     */
    private void produceMsg(Destination destination, Map<String, String> map, String way) throws Exception {

//        UiLogUtil.appendLog2SeverTab(null, "进入produceMsg");
        Connection connection = null;
        Session session = null;
        try {
            //从连接池工厂中获取一个连接
            connection = MQConstants.defaultPoolConnectionFactory.createConnection();

            //false 参数表示 为非事务型消息，后面的参数表示消息的确认类型
            session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);

            //生产者
            MessageProducer producer = session.createProducer(destination);
            //设置消息持久化
            producer.setDeliveryMode(this.isPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            //解析消息
            Message message = getMessage(session, map);
            //发送消息
//            UiLogUtil.appendLog2SeverTab(null, "开始发送消息");
            producer.send(message);
//            UiLogUtil.appendLog2SeverTab(null, "消息发送完毕");
        } finally {
            //生产者生产消息后关闭会话和连接
            closeSession(session);
            closeConnection(connection);
        }
    }

    private Message getMessage(Session session, Map<String, String> map) throws JMSException {
        MapMessage message = session.createMapMessage();
        if (map != null && !map.isEmpty()) {
            Set<String> keys = map.keySet();
            for (String key : keys) {
                message.setString(key, map.get(key));
            }
        }
        return message;
    }

    private void closeSession(Session session) {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onException(JMSException e) {
        e.printStackTrace();
    }

    public Message produceMsgAndConsume(String destinationName, Map<String, String> map) throws Exception {
        TemporaryQueue responseQueue = null;
        Connection connection = null;
        Session session = null;
        try {
            //从连接池工厂中获取一个连接
            connection = MQConstants.defaultPoolConnectionFactory.createConnection();
            //false 参数表示 为非事务型消息，后面的参数表示消息的确认类型
            session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);
            //生产者
            responseQueue = session.createTemporaryQueue();
            connection.start();
            Destination destination = resolveDestinationName(session, destinationName);
            MessageProducer producer = session.createProducer(destination);
            MessageConsumer consumer = session.createConsumer(responseQueue);
            //设置消息持久化
            producer.setDeliveryMode(this.isPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
            //解析消息
            Message message = getMessage(session, map);
            message.setJMSReplyTo(responseQueue);
            //发送消息
            producer.send(message);
            return doReceive(consumer);//consumer.receive();
        } finally {
            //生产者生产消息后关闭会话和连接
            closeSession(session);
            closeConnection(connection);
        }
    }

    private Message doReceive(MessageConsumer consumer) throws JMSException {
        if (GlobalConstants.MQ_MSG_WAIT_TIME == -1) {
            return consumer.receiveNoWait();
        } else if (GlobalConstants.MQ_MSG_WAIT_TIME > 0) {
            return consumer.receive(GlobalConstants.MQ_MSG_WAIT_TIME);
        } else {
            return consumer.receive();
        }
    }

    protected Destination resolveDestinationName(Session session, String destinationName) throws JMSException {
        return getDestinationResolver().resolveDestinationName(session, destinationName, isPubSubDomain());
    }

    public DestinationResolver getDestinationResolver() {
        return this.destinationResolver;
    }

    public boolean isPubSubDomain() {
        return this.pubSubDomain;
    }
}
