package cn.tzauto.octopus.common.mq;


import cn.tzauto.octopus.common.mq.common.MessageHandler;
import java.util.concurrent.ExecutorService;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Created by Chase on 2016/7/24.
 */
public class MultiThreadMessageListener implements MessageListener {

    //默认线程池数量
    public final static int DEFAULT_HANDLE_THREAD_POOL=30;
    //最大的处理线程数
    private int maxHandleThreads;
    //消息处理策略
    private MessageHandler messageHandler;

    //线程池
    private ExecutorService handleThreadPool;


    public MultiThreadMessageListener(MessageHandler messageHandler){
        this(DEFAULT_HANDLE_THREAD_POOL, messageHandler);
    }

    public MultiThreadMessageListener(int maxHandleThreads,MessageHandler messageHandler){
        this.maxHandleThreads=maxHandleThreads;
        this.messageHandler=messageHandler;
        //支持阻塞的固定大小的线程池，如果消息没有前后顺序则可以注释此句，使用JDK提供的线程池处理消息
        this.handleThreadPool = new FixedAndBlockedThreadPoolExecutor(this.maxHandleThreads);
//        this.handleThreadPool = Executors.newFixedThreadPool(this.maxHandleThreads);
    }


    /**
     * 监听程序中自动调用的方法
     */
    public void onMessage(final Message message) {
        //使用支持阻塞的固定大小的线程池来执行操作
        this.handleThreadPool.execute(new Runnable() {
            public void run() {
                try {
                    MultiThreadMessageListener.this.messageHandler.handle(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
