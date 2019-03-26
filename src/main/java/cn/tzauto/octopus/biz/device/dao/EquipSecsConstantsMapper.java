package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.EquipSecsConstants;

import java.util.List;
import java.util.Map;

public interface EquipSecsConstantsMapper {

    int deleteByPrimaryKey(String id);

    int insert(EquipSecsConstants record);

    int insertSelective(EquipSecsConstants record);

    EquipSecsConstants selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(EquipSecsConstants record);

    int updateByPrimaryKey(EquipSecsConstants record);

    List<EquipSecsConstants> selectByParaMap(Map record);
}