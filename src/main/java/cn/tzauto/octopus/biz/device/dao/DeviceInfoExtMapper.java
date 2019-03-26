package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import java.util.List;

public interface DeviceInfoExtMapper {

    int deleteByPrimaryKey(String id);

    int insert(DeviceInfoExt record);

    int insertSelective(DeviceInfoExt record);

    DeviceInfoExt selectByPrimaryKey(String id);

    DeviceInfoExt selectByDeviceRowid(String deviceRowid);

    int updateByPrimaryKeySelective(DeviceInfoExt record);

    int updateByPrimaryKey(DeviceInfoExt record);

    int saveDeviceInfoExtBatch(List<DeviceInfoExt> deviceInfoExts);

    int deleteDeviceInfoExtByIdBatch(List<DeviceInfoExt> deviceInfoExts);

    int deleteDeviceInfoExtByDeviceRowIdBatch(List<DeviceInfoExt> deviceInfoExts);

    List<DeviceInfoExt> getAllDeviceInfoExts();
}
