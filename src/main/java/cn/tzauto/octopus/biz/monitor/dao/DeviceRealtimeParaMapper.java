package cn.tzauto.octopus.biz.monitor.dao;

import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DeviceRealtimeParaMapper {

    int deleteByPrimaryKey(String id);

    int insertBatch(List<DeviceRealtimePara> record);

    public List<DeviceRealtimePara> searchByMap(Map paraMap);

    public Long getMaxUpdateCnt(String record);

    public List<HashMap> getDeviceCodeAndMaxUpdateCnt(String record);

    public int deleteRealTimeErro();

    public int deleteStartErro();

    List<DeviceRealtimePara> searchByParaMap(Map paMap);

    public int deleteErro(Map paraMap);

    public List<DeviceRealtimePara> getParasInTime(Map paMap);

    List<DeviceRealtimePara> selectOldData(String savedDays);

    int deleteRealtimeParaBatch(List<DeviceRealtimePara> deviceRealtimeParas);

}
