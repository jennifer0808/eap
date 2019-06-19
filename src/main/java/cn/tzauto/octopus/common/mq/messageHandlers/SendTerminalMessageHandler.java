/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

/**
 *
 * @author luosy
 */
public class SendTerminalMessageHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(SendTerminalMessageHandler.class.getName());
    private String deviceCode = "";
    private String msg = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            deviceCode = mapMessage.getString("deviceCode");
            msg = mapMessage.getString("msg");

            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfo deviceInfo = deviceService.searchDeviceInfoByPara(GlobalConstants.getProperty("clientId"), deviceCode).get(0);
            hostManager.sendTerminalMsg2Eqpt(deviceInfo.getDeviceId(), msg);
            logger.info("服务端向设备" + deviceCode + "发送message " + msg);
        } catch (JMSException ex) {
            logger.error("JMSException:", ex);
        } finally {
            sqlSession.close();
        }
    }

}
