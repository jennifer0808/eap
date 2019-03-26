package cn.tzauto.octopus.common.mq;


import cn.tzauto.octopus.common.mq.common.MQConstants;

import javax.jms.*;

/**
 * Created by Chase on 2016/7/24.
 */
public class ConsumerServiceImpl implements ExceptionListener {

    //队列预取策略
    public final static int DEFAULT_QUEUE_PREFETCH = 10;
    private int queuePrefetch = DEFAULT_QUEUE_PREFETCH;
    private String brokerUrl;
    private String userName;
    private String password;
    //消息监听
    private MessageListener messageListener;
    //和生产者不同，消费者不知道消息何时会到来，所以连接和会话不能在消费完一条消息后就关闭，所以需要设置成成员变量
    private Connection connection;
    private Session session;

    public ConsumerServiceImpl(String brokerUrl, String userName, String password) {
        this(brokerUrl, userName, password, DEFAULT_QUEUE_PREFETCH);
    }

    public ConsumerServiceImpl(String brokerUrl, String userName, String password, int queuePrefetch) {
        this.brokerUrl = brokerUrl;
        this.userName = userName;
        this.password = password;
        this.queuePrefetch = queuePrefetch;
    }

    /**
     * 消费消息
     *
     * @param dest 目的地
     * @param way 消费方式：接收或者订阅
     * @throws Exception
     */
    public void consumeMsg(String dest, String way) throws Exception {
        //ActiveMQ的连接工厂
//         ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(this.userName, this.password, this.brokerUrl);
        connection = MQConstants.getConnection();
//        connection = MQConstants.defaultPoolConnectionFactory.createConnection();

//         connection = connectionFactory.createConnection();
        //ActiveMQ预取策略     
//        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
//        prefetchPolicy.setQueuePrefetch(queuePrefetch);
//        ((ActiveMQConnection) connection).setPrefetchPolicy(prefetchPolicy);
        connection.setExceptionListener(this);
        connection.start();
        //会话采用非事务级别，消息到达机制使用自动通知机制
        session = connection.createSession(Boolean.FALSE, Session.AUTO_ACKNOWLEDGE);

        //根据way确定消息的消费形式：接收或者订阅
        Destination destination = null;
        if ("subscribe".equals(way)) {
            destination = session.createTopic(dest);
        } else {
            destination = session.createQueue(dest);//默认接收
        }
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(this.messageListener);
    }

    /**
     * 关闭
     */
    public void shutdown() {
        try {
            if (session != null) {
                session.close();
                session = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onException(JMSException e) {
        e.printStackTrace();
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public MessageListener getMessageListener() {
        return messageListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public int getQueuePrefetch() {
        return queuePrefetch;
    }

    public void setQueuePrefetch(int queuePrefetch) {
        this.queuePrefetch = queuePrefetch;
    }
}
