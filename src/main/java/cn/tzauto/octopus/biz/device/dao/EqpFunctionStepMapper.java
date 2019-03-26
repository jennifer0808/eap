package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.EqpFunctionStep;

public interface EqpFunctionStepMapper {
    int deleteByPrimaryKey(String id);

    int insert(EqpFunctionStep record);

    int insertSelective(EqpFunctionStep record);

    EqpFunctionStep selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(EqpFunctionStep record);

    int updateByPrimaryKey(EqpFunctionStep record);
}