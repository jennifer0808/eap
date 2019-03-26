/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author luosy
 */
public class RefreshEquipStatusTask implements Job {

    private static final Logger logger = Logger.getLogger(RefreshEquipStatusTask.class);

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        logger.info("RefreshEquipStatusTask start...");
        String equipstatus = "--";
        String preEquipstatus = "--";
        //ConcurrentHashMap<String, EquipModel> equipModels = GlobalConstants.stage.equipModels;
        if (GlobalConstants.stage.equipModels != null && GlobalConstants.stage.equipModels.size() > 0) {
            for (EquipModel equipModel : GlobalConstants.stage.equipModels.values()) {
                if (equipModel.getPassport(20)) {
                    if (!equipModel.isConnect()) {
                        continue;
                    }
                    preEquipstatus = equipModel.equipStatus;
                    try {
                        equipstatus = equipModel.getEquipRealTimeState().get("EquipStatus").toString();
                    } catch (Exception e) {
                        logger.error("设备:" + equipModel.deviceCode + "获取设备状态时异常." + e.getMessage());
                        Map map = new HashMap();
                        map.put("CommState", 0);
                        equipModel.changeEquipPanel(map);
                        equipModel.equipState.setCommOn(false);
                        equipModel.returnPassport();
                        equipModel.iSecsHost.isConnect = false;
                        continue;
                    }
                    if ("pause".equalsIgnoreCase(preEquipstatus) && "RUN".equalsIgnoreCase(equipstatus)) {
                        if (equipModel.checkLockFlagFromServerByWS(equipModel.deviceCode)) {
                            String stopResult = equipModel.pauseEquip();
                            UiLogUtil.appendLog2SeverTab(equipModel.deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
                        }
                    }
                    if (("Ready".equalsIgnoreCase(preEquipstatus) && "RUN".equalsIgnoreCase(equipstatus))) {
                        logger.info("设备:" + equipModel.deviceCode + "开机作业.");
                        UiLogUtil.appendLog2EventTab(equipModel.deviceCode, "设备进入运行状态...");
                        if (!equipModel.startCheck()) {
                            String stopResult = equipModel.stopEquip();
                            UiLogUtil.appendLog2EventTab(equipModel.deviceCode, "设备将被锁机...");
                            String holdDesc = "";
                            Map mqMap = new HashMap();
                            if ("0".equals(stopResult)) {
                                holdDesc = "当前设备已经被锁机";
                                Map map = new HashMap();
                                map.put("EquipStatus", "Idle");
                                equipModel.changeEquipPanel(map);
                                UiLogUtil.appendLog2EventTab(equipModel.deviceCode, "锁机成功...");
                                mqMap.put("holdResult", "锁机成功");
                            } else {
                                mqMap.put("holdResult", "锁机失败");
                                holdDesc = stopResult;
                            }
                            mqMap.put("holdDesc", holdDesc);
                            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
                        }
                    }
                    sendDeviceInfoExtAndOplog2Server(equipModel, preEquipstatus);
                    equipModel.returnPassport();
                }
            }
        }
    }

    private void sendDeviceInfoExtAndOplog2Server(EquipModel equipModel, String preEquipstatus) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(equipModel.deviceCode);
        if (deviceInfoExt == null) {
            logger.error("未配置:" + equipModel.deviceCode + "的模型信息.");
            return;
        }
        deviceInfoExt.setDeviceStatus(equipModel.equipStatus);
        DeviceOplog deviceOplog = new DeviceOplog();
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(equipModel.deviceCode);
        if (deviceOplogList == null || deviceOplogList.isEmpty()) {
            deviceOplog = equipModel.setDeviceOplog(preEquipstatus, deviceInfoExt.getLotId());
           // deviceService.saveDeviceOplog(deviceOplog);
        } else {
            String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
            if (!formerDeviceStatus.equals(equipModel.equipStatus)) {
                deviceOplog = equipModel.setDeviceOplog(preEquipstatus, deviceInfoExt.getLotId());
               // deviceService.saveDeviceOplog(deviceOplog);
            }
        }
        sqlSession.close();
        if (!GlobalConstants.isLocalMode) {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.EqptStatusChange");
            mqMap.put("deviceCode", equipModel.deviceCode);
            mqMap.put("eventName", "eqpt.EqptStatusChange");
            mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
            mqMap.put("deviceCeid", "0");
            mqMap.put("eventDesc", deviceOplog.getOpDesc());
            mqMap.put("eventDate", GlobalConstants.dateFormat.format(new Date()));
            mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        }
    }

}
