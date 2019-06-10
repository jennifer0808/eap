package cn.tzauto.octopus.common.rabbit;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

public class SubscribeMessage {

    /**
     * 订阅模式，启动监听
     */
    public void startlistening() {

        String clientCode = GlobalConstants.clientInfo.getClientCode();

//        GlobalConstants.S2CDataTopic.setMessageHandler(new MessageProcess());
        try {
//            GlobalConstants.S2CDataTopic.subscribeMessage("*.DATA_TRANSFER");
            GlobalConstants.S2CDataTopic.subscribeMessage("S2C.EXCHANGE.RECIPE_C", "RECIPE_C."+clientCode,"RECIPE_C.*");
            GlobalConstants.sysLogger.info("开启MQ RECIPE_C 监听");
        } catch (Exception e) {
            e.printStackTrace();
        }

//        GlobalConstants.S2CEQPT_PARATopic.setMessageHandler(new MessageProcess());
        try {
//            GlobalConstants.S2CEQPT_PARATopic.subscribeMessage("*.EQPT_PARAMETER");
            GlobalConstants.S2CDataTopic.subscribeMessage("S2C.EXCHANGE.EQPT_PARAMETER", "EQPT_PARAMETER."+clientCode,"EQPT_PARAMETER.*");
            GlobalConstants.sysLogger.info("开启MQ EQPT_PARAMETER 监听");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        GlobalConstants.S2CRcpTopic.setMessageHandler(new MessageProcess());
        try {
//            GlobalConstants.S2CRcpTopic.subscribeMessage("*.DATA_TRANSFER");
            GlobalConstants.S2CDataTopic.subscribeMessage("S2C.EXCHANGE.DATA_TRANSFER", "DATA_TRANSFER."+clientCode,"DATA_TRANSFER.*");
            GlobalConstants.sysLogger.info("开启MQ DATA_TRANSFER 监听");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
