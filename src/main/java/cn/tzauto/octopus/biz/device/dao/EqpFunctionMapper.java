package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.EqpFunction;
import java.util.Map;

public interface EqpFunctionMapper {
    int deleteByPrimaryKey(String id);

    int insert(EqpFunction record);

    int insertSelective(EqpFunction record);

    EqpFunction selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(EqpFunction record);

    int updateByPrimaryKey(EqpFunction record);
    
    String checkFunctionSwitch(Map record);
}