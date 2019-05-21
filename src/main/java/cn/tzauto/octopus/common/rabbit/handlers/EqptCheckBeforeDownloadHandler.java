/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Weiqy
 */
public class EqptCheckBeforeDownloadHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(EqptCheckBeforeDownloadHandler.class.getName());
    private String deviceCode = "";


    public void sendMsg2Server(Map mqMap) {
        //TODO 原队列JMSReplyTo获得，待验证
        GlobalConstants.C2SRcpDownLoadQueue.sendMessage(mqMap);
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送设备当前通信状态:" + mqMap.get("flag"));
    }

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "EqptCheckBeforeDownload");
        String flag = "N";
        try {
            deviceCode = msgMap.get("deviceCode");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求核对设备的当前通信状态");
            DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            Map resultMap = GlobalConstants.stage.hostManager.getEquipInitState(deviceInfo.getDeviceCode());
            if (resultMap != null && !resultMap.isEmpty()) {
                flag = "Y";
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "设备通信正常，可以正常改机");
            }
            mqMap.put("flag", flag);
            if ("Y".equals(flag)) {
                mqMap.put("msg", "设备通信正常");
            } else {
                mqMap.put("msg", "设备通信异常，请检查设备通信状态");
            }
            sendMsg2Server(mqMap);
        } catch (Exception ex) {
            logger.error("Exception", ex);
            mqMap.put("flag", flag);
            mqMap.put("msg", "设备通信异常，请检查设备通信状态");
            sendMsg2Server(mqMap);
        }
    }
}
