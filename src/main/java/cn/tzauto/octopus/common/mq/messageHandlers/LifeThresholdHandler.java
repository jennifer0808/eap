/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import java.util.HashMap;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class LifeThresholdHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(FindRecipeNameHandler.class.getName());
    private String deviceCode = "";
    private String z1LifeThreshold = "";
    private String z2LifeThreshold = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            deviceCode = mapMessage.getString("deviceCode").toString();
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求获取设备的当前刀片阀值");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);           
            EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceInfo.getDeviceCode());
            if (equipHost.getEquipState().isCommOn()) {
                Map equipBladeThresholdMap = hostManager.getEquipBladeLifeThreshold(deviceInfo.getDeviceId());
                if (equipBladeThresholdMap == null) {
                    z1LifeThreshold = "取值失败";
                    z2LifeThreshold = "取值失败";
                    UiLogUtil.appendLog2SeverTab(deviceCode, "获取刀片信息失败！");
                } else {
                    z1LifeThreshold = equipBladeThresholdMap.get("z1LifeThreshold").toString();
                    z2LifeThreshold = equipBladeThresholdMap.get("z2LifeThreshold").toString();
                }
            } else {
                z1LifeThreshold = "设备不在通讯状态，获取刀片信息失败！";
                z2LifeThreshold = "设备不在通讯状态，获取刀片信息失败！";
                UiLogUtil.appendLog2SeverTab(deviceCode, "设备不在通讯状态，获取刀片信息失败！");
            }
        } catch (JMSException ex) {
            ex.printStackTrace();
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "LifeThreshold");
        mqMap.put("z1LifeThreshold", z1LifeThreshold);
        mqMap.put("z2LifeThreshold", z2LifeThreshold);
        try {
            Destination destination = message.getJMSReplyTo();
            logger.info("destination:================================ " + destination.toString());
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            logger.info("topicName:==========================================" + topicName);
            GlobalConstants.C2SRcpQueue.sendMessage(topicName, mqMap);
            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端发送设备的当前刀片阀值：Z1=" + z1LifeThreshold + " Z2=" + z2LifeThreshold);
        } catch (JMSException ex) {
            ex.printStackTrace();
        } finally {
            sqlSession.close();
        }
    }
}
