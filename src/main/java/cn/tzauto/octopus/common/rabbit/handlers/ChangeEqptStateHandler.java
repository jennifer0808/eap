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
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rain
 */
public class ChangeEqptStateHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(ChangeEqptStateHandler.class.getName());
    private String deviceCode = "";
    private String eventId = "";
    private String state = "";
    private String type = "";

    public void sendMsg2Server(HashMap<String, String> msgMap,Map mqMap) {
//        GlobalConstants.C2SEqptRemoteCommand.sendMessage(mqMap);
        if (msgMap.containsKey("replyQ")) {
            GlobalConstants.C2SEqptRemoteCommand.replyMessage(msgMap.get("replyQ"), msgMap.get("correlationId"), mqMap);
        }

        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送改变机台状态的操作结果:" + mqMap.get("eventStatus"));

    }

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        String equipStatus = "";

        eventId = msgMap.get("eventId");
        deviceCode = msgMap.get("deviceCode");
        state = msgMap.get("state");
        type = msgMap.get("type");
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端由于：" + type + "，请求改变设备状态为 " + state);
        logger.info("服务端由于：" + type + "，请求改变设备" + deviceCode + "状态为 " + state);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "ReplyMessage");
        mqMap.put("eventId", eventId);
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("eventName", "改变设备状态");
        try {
            DeviceInfo deviceInfo = deviceService.searchDeviceInfoByPara(GlobalConstants.getProperty("clientId"), deviceCode).get(0);
            String lockStatus = "";
            if ("LOCK".equalsIgnoreCase(state)) {
                lockStatus = "Y";
            } else if ("RELEASE".equalsIgnoreCase(state)) {
                lockStatus = "N";
            }
            //更新deviceInfoLock表
            deviceService.updateDeviceInfoLock(deviceCode, type, lockStatus);
            sqlSession.commit();
            String deviceId = deviceInfo.getDeviceCode();
            Map equipState = hostManager.getEquipInitState(deviceId);
            if (equipState != null && equipState.get("EquipStatus") != null) {
                equipStatus = equipState.get("EquipStatus").toString();
                if (deviceInfo.getDeviceType().contains("APTVTS85A")) {
                    String executResult = hostManager.changeEqptStateAndReturnDetail(deviceId, state);
                    if ("Y".equals(executResult)) {
                        mqMap.put("eventStatus", "Y");
                        mqMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作成功");
                    } else {
                        mqMap.put("eventStatus", "N");
                        mqMap.put("eventDesc", executResult);
                    }
                } else {
                    boolean result = hostManager.changeEqptState(deviceInfo.getDeviceCode(), state, type);
                    if ("ICOST640".equals(deviceInfo.getDeviceType()) && ("READY".equals(equipStatus) || "IDLE".equals(equipStatus))) {
                        mqMap.put("eventStatus", "Y");
                        mqMap.put("eventDesc", state);
                    } else if ("ICOST640".equals(deviceInfo.getDeviceType()) && "PAUSE".equals(equipStatus) && "LOCK".equals(state)) {
                        mqMap.put("eventStatus", "Y");
                        mqMap.put("eventDesc", state);
                    } else if ("ICOST640".equals(deviceInfo.getDeviceType()) && "RELEASE".equals(state)) {
                        mqMap.put("eventStatus", "Y");
                        mqMap.put("eventDesc", state);
                    } else if ("Run".equalsIgnoreCase(equipStatus)) {
                        if (result) {
                            mqMap.put("eventStatus", "Y");
                            mqMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作成功");
                            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作成功");
                        } else {
                            mqMap.put("eventStatus", "N");
                            mqMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作失败");
                            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作失败");
                        }
                    } else {
                        mqMap.put("eventStatus", "Y");
                        mqMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作成功");
                        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作成功");
                    }
                }
            } else {
                mqMap.put("eventStatus", "N");
                mqMap.put("eventDesc", "获取设备当前信息失败!无法执行控制命令!请重试！");
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作失败, 获取设备当前信息失败!");
            }
            sendMsg2Server(msgMap,mqMap);
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
            mqMap.put("eventStatus", "N");
            mqMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作失败");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作失败, 出现异常消息 " + e.getMessage());
            sendMsg2Server(msgMap,mqMap);
        } finally {
            sqlSession.close();
        }
    }
}
