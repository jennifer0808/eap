package cn.tzauto.octopus.biz.alarm.dao;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AlarmRecordMapper {

    int deleteByPrimaryKey(String id);

    int insert(AlarmRecord record);

    int insertSelective(AlarmRecord record);

    AlarmRecord selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(AlarmRecord record);

    int updateByPrimaryKey(AlarmRecord record);

    List searchAlarmRecordInCycTime(Map paraMap);

    List searchByMap(Map paraMap);

    List<HashMap> getDeviceCode(String clientId);

    List queryRecentAlarmRecords(Map paraMap);

    List<AlarmRecord> searchByTime();

    List<AlarmRecord> selectOldData(String savedDays);

    int deleteAlarmRecordBatch(List<AlarmRecord> alarmRecords);
}
