package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DeviceInfoMapper {

    int deleteByPrimaryKey(String id);

    int deleteByClientId(String clientId);

    int batchDeleteDeviceInfo(List<DeviceInfo> deviceInfoList);

    int insert(DeviceInfo record);

    int insertDeviceInfoBatch(List<DeviceInfo> deviceInfoList);

    int insertSelective(DeviceInfo record);

    DeviceInfo selectByPrimaryKey(String id);

    DeviceInfo selectDeviceInfoByDeviceCode(String deviceCode);

    int updateByPrimaryKeySelective(DeviceInfo record);

    int updateByPrimaryKey(DeviceInfo record);

    List searchByMap(Map paMap);

    List<DeviceInfo> getDeviceInfo(String clientId);

    int delete(String id);

    List<String> queryWaferPath(String waferId);

    void insertWaferMappingPath(@Param("name") String name, @Param("filePath")String filePath, @Param("month")String month);
}