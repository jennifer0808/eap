package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.DeviceType;

import java.util.List;
import java.util.Map;

public interface DeviceTypeMapper {

    int deleteByPrimaryKey(String id);

    int insert(DeviceType record);

    int insertSelective(DeviceType record);

    DeviceType selectByPrimaryKey(String id);

    DeviceType searchByPaMap(Map paMap);

    int updateByPrimaryKeySelective(DeviceType record);

    int updateByPrimaryKey(DeviceType record);

    public List searchByMap(Map paraMap);

    DeviceType searchBymaf(Map paMap);
}