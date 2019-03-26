package cn.tzauto.octopus.common.mq;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

public class SubscribeMessage {

    /**
     * 订阅模式，启动监听
     */
    public void startlistening() {

        GlobalConstants.S2CDataTopic.setMessageHandler(new MessageProcess());
        try {
            GlobalConstants.S2CDataTopic.subscribeMessage();
            GlobalConstants.sysLogger.info("开启MQ S2C.T.DATA_TRANSFER Topic监听");
        } catch (Exception e) {
            e.printStackTrace();
        }

        GlobalConstants.S2CEQPT_PARATopic.setMessageHandler(new MessageProcess());
        try {
            GlobalConstants.S2CEQPT_PARATopic.subscribeMessage();
            GlobalConstants.sysLogger.info("开启MQ S2C.T.EQPT_PARAMETER Topic监听");
        } catch (Exception e) {
            e.printStackTrace();
        }
        GlobalConstants.S2CRcpTopic.setMessageHandler(new MessageProcess());
        try {
            GlobalConstants.S2CRcpTopic.subscribeMessage();
            GlobalConstants.sysLogger.info("开启MQ S2C.T.RECIPE_C Topic监听");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        GlobalConstants.S2CRcpTopicTest.setMessageHandler(new MessageProcess());
//        try {
//            GlobalConstants.S2CRcpTopicTest.subscribeMessage();
//            GlobalConstants.sysLogger.info("开启MQ S2CRcpTopicTest监听");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}
