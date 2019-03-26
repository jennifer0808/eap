package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.EqpFunctionDefine;

public interface EqpFunctionDefineMapper {
    int deleteByPrimaryKey(String id);

    int insert(EqpFunctionDefine record);

    int insertSelective(EqpFunctionDefine record);

    EqpFunctionDefine selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(EqpFunctionDefine record);

    int updateByPrimaryKey(EqpFunctionDefine record);
}