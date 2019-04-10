package WebService.impl;


import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.messageHandlers.EqptCheckBeforeDownloadHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import java.util.HashMap;
import java.util.Map;

//下载前检查
public class CheckBeforeDownloadWebservice implements BaseWebservice {

    private static Logger logger = Logger.getLogger(EqptCheckBeforeDownloadHandler.class.getName());
    private String deviceCode = "";

    @Override
    public String  handle(String message) {
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n",""),HashMap.class);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Map webMap = new HashMap();
        webMap.put("msgName", "EqptCheckBeforeDownload");
        String flag = "N";
        try {
            deviceCode =  String.valueOf(map.get("deviceCode"));
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求核对设备的当前通信状态");
            DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            Map resultMap = GlobalConstants.stage.hostManager.getEquipInitState(deviceInfo.getDeviceCode());
            if (resultMap != null && !resultMap.isEmpty()) {
                flag = "Y";
                UiLogUtil.appendLog2SeverTab(deviceCode, "设备通信正常，可以正常改机");
            }
            webMap.put("flag", flag);
            if ("Y".equals(flag)) {
                webMap.put("msg", "设备通信正常");
            } else {
                webMap.put("msg", "设备通信异常，请检查设备通信状态");
            }
            return JSONObject.toJSON(webMap).toString();
        } catch (Exception ex) {
            logger.error("JMSException", ex);
            webMap.put("flag", flag);
            webMap.put("msg", "设备通信异常，请检查设备通信状态");
            return JSONObject.toJSON(webMap).toString();
        }

    }

    public void sendMsg2Server(Message message, Map mqMap) {
        try {
            Destination destination = message.getJMSReplyTo();
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            GlobalConstants.C2SRcpDownLoadQueue.sendMessage(topicName, mqMap);
            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端发送设备当前通信状态:" + mqMap.get("flag"));
        } catch (JMSException ex) {
            logger.error("JMSException", ex);
        }
    }
}
