/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.biz.alarm.service;

import cn.tzauto.octopus.biz.alarm.dao.AlarmRecordMapper;
import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.sys.domain.SysOffice;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.service.BaseService;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 * @author njtz
 */
public class AlarmService extends BaseService {

    private static Logger logger = Logger.getLogger(AlarmService.class);
    private AlarmRecordMapper alarmRecordMapper;

    public AlarmService(SqlSession sqlSession) {
        super(sqlSession);
        alarmRecordMapper = this.session.getMapper(AlarmRecordMapper.class);
    }

    /**
     * 判断接收的alarm，对应的设备的alarm管控范围是否包含这个alarm，以及对应的处理
     *
     * @param alarmMap
     */
    public void triggerAlarm(Map alarmMap) {
        String deviceCode = alarmMap.get("deviceCode").toString();//获取发送触发指令的机台的DeviceCode
        DeviceInfo deviceInfo = GlobalConstants.stage.hostManager.getDeviceInfo(null, deviceCode);
        //deviceService.searchDeviceInfoByPara(GlobalConstants.getProperty("clientId"), deviceCode).get(0); 
        //存储报警信息
        AlarmRecord alarmRecord = setAlarmRecord(deviceInfo, alarmMap);
        if (!GlobalConstants.isLocalMode) {
            //实时发送alarm记录至服务端
            Map alarmRecordMap = new HashMap();
            alarmRecordMap.put("msgName", "ArAlarmRecord");
            alarmRecordMap.put("deviceCode", deviceCode);
            List<AlarmRecord> alarmRecordList = new ArrayList();
            alarmRecordList.add(alarmRecord);
            alarmRecordMap.put("alarmRecord", JsonMapper.toJsonString(alarmRecordList));
            alarmRecordMap.put("alarmDate", GlobalConstants.dateFormat.format(new Date()));
            GlobalConstants.C2SAlarmQueue.sendMessage(alarmRecordMap);
            logger.info("实时发送alarm记录至服务端");
        }
        logger.info("设备编号: " + deviceInfo.getDeviceCode() + " 收到报警信息。"
                + " 报警ID: " + alarmRecord.getAlarmId()
                + " 报警详情: " + alarmRecord.getAlarmName());
    }

    private AlarmRecord setAlarmRecord(DeviceInfo deviceInfo, Map alarmMap) {
        AlarmRecord alarmRecord = new AlarmRecord();
        String id = UUID.randomUUID().toString();
        alarmRecord.setId(id);
        alarmRecord.setClientCode(GlobalConstants.clientInfo.getClientCode());
        alarmRecord.setClientId(GlobalConstants.clientInfo.getId());
        alarmRecord.setClientName(GlobalConstants.clientInfo.getClientName());
        alarmRecord.setDeviceId(deviceInfo.getId());
        alarmRecord.setDeviceCode(deviceInfo.getDeviceCode());
        alarmRecord.setDevcieName(deviceInfo.getDeviceName());
        alarmRecord.setAlarmId(alarmMap.get("ALID").toString());
        alarmRecord.setAlarmCode(alarmMap.get("ALCD").toString());
        alarmRecord.setAlarmName(alarmMap.get("ALTX").toString());
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
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SysService service = new SysService(sqlSession);
        SysOffice sysOffice = service.selectSysOfficeByPrimaryKey(officeId);
        sqlSession.close();
        if (sysOffice != null) {
            alarmRecord.setStationCode(sysOffice.getName());
            alarmRecord.setStationName(sysOffice.getName());
        } else {
            alarmRecord.setStationCode("");
            alarmRecord.setStationName("");
        }
        return alarmRecord;
    }

    public List<AlarmRecord> searchAlarmRecordInOneHour() {
        return alarmRecordMapper.searchByTime();
    }
}
