package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.EquipSvec;
import java.util.List;
import java.util.Map;

public interface EquipSvecMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(EquipSvec record);

    int insertSelective(EquipSvec record);

    EquipSvec selectByPrimaryKey(Integer id);
    List<EquipSvec> selectByEquipType(String equipType);
    int updateByPrimaryKeySelective(EquipSvec record);

    int updateByPrimaryKeyWithBLOBs(EquipSvec record);

    int updateByPrimaryKey(EquipSvec record);
    
    List<EquipSvec> searchByMap(Map record);
}