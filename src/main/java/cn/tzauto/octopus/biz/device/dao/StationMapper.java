package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.Station;

public interface StationMapper {
    int deleteByPrimaryKey(String id);

    int insert(Station record);

    int insertSelective(Station record);

    Station selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Station record);

    int updateByPrimaryKey(Station record);
}