/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author
 */
public class PPSelectHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(PPSelectHandler.class);
    private String deviceCode = "";
    private String recipeName = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        String result = "";
        try {
            deviceCode = mapMessage.getString("deviceCode"); //获取服务端发送的数据
            recipeName = mapMessage.getString("recipeName");
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求将设备 " + deviceCode + " 的程序更换为 " + recipeName);
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);          
            if (deviceInfo.getDeviceType().contains("MVP")) {
                result = hostManager.selectSpecificRecipe(deviceInfo.getDeviceCode(), recipeName);
            }
        } catch (Exception e) {
            logger.error("Exception", e);
        } finally {
            sqlSession.close();
        }
        Map mqMap = new HashMap(); //消息体
        mqMap.put("msgName", "MVPSelect");
        mqMap.put("deviceCode", deviceCode);
        String flag = "";
        if ("0".equals(result)) {
            flag = "Y";
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "PPSelect成功");
        } else {
            flag = "N";
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "PPSelect失败, 结果为:" + result);
        }
        mqMap.put("flag", flag);
        mqMap.put("reason", result);
        sendMsgToServer(message, mqMap);
    }
    //同步发送
    public void sendMsgToServer(Message message, Map mqMap) {
        try {
            Destination destination = message.getJMSReplyTo();
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            GlobalConstants.C2SRcpSelectQueue.sendMessage(topicName, mqMap); //使用 C2S.Q.PPSELECT
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送MVP PPSelect结果至服务端");
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        }
    }
}
