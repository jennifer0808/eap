package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import java.util.List;
import java.util.Map;

public interface DeviceOplogMapper {

    int deleteByPrimaryKey(String id);

    int insert(DeviceOplog record);

    int insertSelective(DeviceOplog record);

    DeviceOplog selectByPrimaryKey(String id);

    List<DeviceOplog> selectByParaMap(Map paMap);

    int updateByPrimaryKeySelective(DeviceOplog record);

    int updateByPrimaryKey(DeviceOplog record);

    List searchByMap(Map paraMap);

    List<DeviceOplog> selectOldData(String savedDays);

    int deleteOpLogBatch(List<DeviceOplog> deviceOplogs);
}
