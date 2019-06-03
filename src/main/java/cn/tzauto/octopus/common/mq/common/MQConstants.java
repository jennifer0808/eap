/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.common;


import cn.tzauto.octopus.common.mq.MessageUtils;

import java.io.InputStream;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.log4j.Logger;

/**
 *
 * @author gavin
 */
public class MQConstants {

    private static final Logger logger = Logger.getLogger(MQConstants.class);
    //队列预取策略

    public final static int DEFAULT_QUEUE_PREFETCH = 10;
    private int queuePrefetch = DEFAULT_QUEUE_PREFETCH;
    private static String brokerUrl;
    private static String userName;
    private static String password;
    public static ActiveMQConnectionFactory defaultConnectionFactory;
    public static PooledConnectionFactory defaultPoolConnectionFactory;

    //是否使用同步返回数据的格式    
    public final static boolean DEFAULT_USE_ASYNC_SEND_FOR_JMS = true;
    private static boolean useAsyncSendForJMS = DEFAULT_USE_ASYNC_SEND_FOR_JMS;

    private static Connection defaultConnection = null;

    //生产者参数
    private static int maxConnections;//最大连接数
    private static int maximumActiveSessionPerConnection;//每个连接中最大活动会话数
    private static int threadPoolSize;//线程池中的线程数量
    private static boolean isPersistent;//是否使用持久化消息

    public static void initConenction() {
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

            if (userName == null || userName.equals("")) {
                userName = ActiveMQConnection.DEFAULT_USER;
            }
            if (password == null || password.equals("")) {
                password = ActiveMQConnection.DEFAULT_PASSWORD;
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        //ActiveMQ连接池工厂
        defaultConnectionFactory = new ActiveMQConnectionFactory(userName, password, brokerUrl);
        defaultConnectionFactory.setWatchTopicAdvisories(false);
        defaultConnectionFactory.setUseAsyncSend(useAsyncSendForJMS);
        defaultPoolConnectionFactory = new PooledConnectionFactory(defaultConnectionFactory);
//        try {
//            defaultConnection = defaultConnectionFactory.createConnection();
////            defaultConnection.start();
//        } catch (JMSException ex) {
//            ex.printStackTrace();
//          logger.error(ex.getMessage());
//        }

    }

    public static Connection getConnection() {
        if (defaultConnection != null) {
            return defaultConnection;
        } else {
            try {
                defaultConnection = defaultConnectionFactory.createConnection();
            } catch (JMSException ex) {
                ex.printStackTrace();
                logger.error(ex.getMessage());
                return null;
            }
            return defaultConnection;
        }
    }

}
