package WebService.impl;
//控制设备接口


import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.messageHandlers.ChangeEqptStateHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class ChangDeviceStateWebservice implements BaseWebservice {
    private static Logger logger = Logger.getLogger(ChangeEqptStateHandler.class.getName());
    private String deviceCode = "";
    private String eventId = "";
    private String state = "";
    private String type = "";

    @Override
    public String handle(String  message) {
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n",""),HashMap.class);

        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        String equipStatus = "";

            eventId = String.valueOf(map.get("eventId"));
            deviceCode = String.valueOf(map.get("deviceCode"));
            state =String.valueOf(map.get("state"));
            type = String.valueOf(map.get("type"));
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端由于：" + type + "，请求改变设备状态为 " + state);
            logger.info("服务端由于：" + type + "，请求改变设备" + deviceCode + "状态为 " + state);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Map webMap = new HashMap();
        webMap.put("msgName", "ReplyMessage");
        webMap.put("eventId", eventId);
        webMap.put("deviceCode", deviceCode);
        webMap.put("eventName", "改变设备状态");
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
                        webMap.put("eventStatus", "Y");
                        webMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作成功");
                    } else {
                        webMap.put("eventStatus", "N");
                        webMap.put("eventDesc", executResult);
                    }
                } else {
                    boolean result = hostManager.changeEqptState(deviceInfo.getDeviceCode(), state, type);
                    if ("ICOST640".equals(deviceInfo.getDeviceType()) && ("READY".equals(equipStatus) || "IDLE".equals(equipStatus))) {
                        webMap.put("eventStatus", "Y");
                        webMap.put("eventDesc", state);
                    } else if ("ICOST640".equals(deviceInfo.getDeviceType()) && "PAUSE".equals(equipStatus) && "LOCK".equals(state)) {
                        webMap.put("eventStatus", "Y");
                        webMap.put("eventDesc", state);
                    } else if ("ICOST640".equals(deviceInfo.getDeviceType()) && "RELEASE".equals(state)) {
                        webMap.put("eventStatus", "Y");
                        webMap.put("eventDesc", state);
                    } else if ("Run".equalsIgnoreCase(equipStatus)) {
                        if (result) {
                            webMap.put("eventStatus", "Y");
                            webMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作成功");
                            UiLogUtil.appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作成功");
                        } else {
                            webMap.put("eventStatus", "N");
                            webMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作失败");
                            UiLogUtil.appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作失败");
                        }
                    } else {
                        webMap.put("eventStatus", "Y");
                        webMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作成功");
                        UiLogUtil.appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作成功");
                    }
                }
            } else {
                webMap.put("eventStatus", "N");
                webMap.put("eventDesc", "改变设备状态为" + state + "，操作失败, 获取设备当前信息失败!");
                UiLogUtil.appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作失败, 获取设备当前信息失败!");
            }
           return JSONObject.toJSON(webMap).toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
            webMap.put("eventStatus", "N");
            webMap.put("eventDesc", "改变设备" + deviceCode + "状态为" + state + "操作失败");
            UiLogUtil.appendLog2SeverTab(deviceCode, "改变设备状态为" + state + "，操作失败, 出现异常消息 " + e.getMessage());
            return JSONObject.toJSON(webMap).toString();
        } finally {
            sqlSession.close();
        }
    }
}



