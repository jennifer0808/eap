/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.equipImpl.disco.ls.DFL7161Host;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author luosy
 */
public class MonitorAlarmTask implements Job {

    private static final Logger logger = Logger.getLogger(MonitorAlarmTask.class);

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        logger.info("MonitorAlarmTask start...");
        // ConcurrentHashMap<String, EquipModel> equipModels = GlobalConstants.stage.equipModels;
        if (GlobalConstants.stage.equipModels != null && GlobalConstants.stage.equipModels.size() > 0) {
            for (EquipModel equipModel : GlobalConstants.stage.equipModels.values()) {
                MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, equipModel.deviceCode);
                if (equipModel.getPassport(30)) {
                    if (!equipModel.iSecsHost.isConnect) {
                        continue;
                    }
                    List<String> alarmStrings = new ArrayList<>();
                    try {
                        alarmStrings = equipModel.getEquipAlarm();
                        if (alarmStrings == null) {
                            continue;
                        }
                    } catch (Exception e) {
                        equipModel.returnPassport();
                        logger.error("设备:" + equipModel.deviceCode + "获取设备报警信息时异常." + e.getMessage());
                        Map map = new HashMap();
                        map.put("CommState", 0);
                        equipModel.changeEquipPanel(map);
                        equipModel.equipState.setCommOn(false);
                        equipModel.iSecsHost.isConnect = false;
                        continue;
                    }
                    //  String deviceCode = equipModel.deviceCode;//获取发送触发指令的机台的DeviceCode
                    DeviceInfo deviceInfo = GlobalConstants.stage.getDeviceInfo(null, equipModel.deviceCode);
                    List<AlarmRecord> alarmRecordList = setAlarmRecord(deviceInfo, alarmStrings);
                    if (equipModel.deviceType.contains("DFL7161")) {
                        for (String alarmString : alarmStrings) {
                            if ("G1121".equals(alarmString)) {
                                Map laserEnergyMap = ((DFL7161Host) equipModel).getLaserEnergy();
                                if (!GlobalConstants.isLocalMode) {
                                    //实时发送alarm记录至服务端
                                    Map laserEnergyRecordMap = new HashMap();
                                    laserEnergyRecordMap.put("msgName", "LaserEnergyParam");
                                    laserEnergyRecordMap.put("deviceCode", equipModel.deviceCode);
                                    laserEnergyRecordMap.put("laserEnergy", JsonMapper.toJsonString(laserEnergyMap));
                                    laserEnergyRecordMap.put("eventDesc", "");
                                    GlobalConstants.C2SLogQueue.sendMessage(laserEnergyMap);
                                    logger.info("Send LaserEnergy to server..." + laserEnergyRecordMap.toString());
                                }
                            }
                        }
                    }
                    if (!GlobalConstants.isLocalMode) {
                        //实时发送alarm记录至服务端
                        Map alarmRecordMap = new HashMap();
                        alarmRecordMap.put("msgName", "ArAlarmRecord");
                        alarmRecordMap.put("deviceCode", equipModel.deviceCode);
                        alarmRecordMap.put("alarmRecord", JsonMapper.toJsonString(alarmRecordList));
                        alarmRecordMap.put("alarmDate", GlobalConstants.dateFormat.format(new Date()));
                        GlobalConstants.C2SAlarmQueue.sendMessage(alarmRecordMap);
                        logger.info("Send alarmRecords to server..." + alarmRecordMap.toString());
                    }
                    equipModel.returnPassport();
                }
            }
        }

    }

    private List<AlarmRecord> setAlarmRecord(DeviceInfo deviceInfo, List<String> alarmStrings) {
        List<AlarmRecord> alarmRecords = new ArrayList<>();
        if (alarmStrings != null && alarmStrings.size() > 0) {
            for (String str : alarmStrings) {
                AlarmRecord alarmRecord = new AlarmRecord();
                String id = UUID.randomUUID().toString();
                alarmRecord.setId(id);
                alarmRecord.setClientCode(GlobalConstants.clientInfo.getClientCode());
                alarmRecord.setClientId(GlobalConstants.clientInfo.getId());
                alarmRecord.setClientName(GlobalConstants.clientInfo.getClientName());
                alarmRecord.setDeviceId(deviceInfo.getId());
                alarmRecord.setDeviceCode(deviceInfo.getDeviceCode());
                alarmRecord.setDevcieName(deviceInfo.getDeviceName());
                alarmRecord.setAlarmId(str);
                alarmRecord.setAlarmCode("");
                alarmRecord.setAlarmName("");
                alarmRecord.setAlarmDate(new Date());
                alarmRecord.setDeviceTypeCode(deviceInfo.getDeviceType());
                alarmRecord.setDeviceTypeId(deviceInfo.getDeviceTypeId());
                alarmRecord.setRepeatFlag("N");
                alarmRecord.setStepCode("");
                alarmRecord.setStepId("");
                alarmRecord.setStepName("");
                alarmRecord.setVerNo(0);
                alarmRecord.setRemarks("");
                alarmRecord.setDelFlag("0");
                String officeId = deviceInfo.getOfficeId();
                alarmRecord.setStationId(officeId);
                alarmRecord.setStationCode("");
                alarmRecord.setStationName("");
                logger.info("设备编号: " + deviceInfo.getDeviceCode() + " 收到报警信息."
                        + " 报警ID: " + alarmRecord.getAlarmId()
                        + " 报警详情: " + alarmRecord.getAlarmName());
                alarmRecords.add(alarmRecord);
            }
        }
        return alarmRecords;
    }
}
