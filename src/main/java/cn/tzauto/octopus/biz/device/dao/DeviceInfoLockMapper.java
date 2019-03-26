package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoLock;
import java.util.List;
import java.util.Map;

public interface DeviceInfoLockMapper {

    int deleteByPrimaryKey(String id);

    int insert(DeviceInfoLock record);

    int insertSelective(DeviceInfoLock record);

    DeviceInfoLock selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(DeviceInfoLock record);

    int updateByPrimaryKey(DeviceInfoLock record);

    List<DeviceInfoLock> selectByDeviceRowId(String id);

    int delete(DeviceInfoLock record);

    List<DeviceInfoLock> selectByType(String type);

    int saveDeviceInfoLockBatch(List<DeviceInfoLock> deviceInfoLocks);

    int deleteDeviceInfoLockBatch(List<DeviceInfoLock> deviceInfoLocks);

    List<DeviceInfoLock> searchByMap(Map map);
}