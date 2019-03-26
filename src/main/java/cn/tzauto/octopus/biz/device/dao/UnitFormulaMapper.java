package cn.tzauto.octopus.biz.device.dao;

import cn.tzauto.octopus.biz.device.domain.UnitFormula;

import java.util.List;

public interface UnitFormulaMapper {
    
    int deleteByPrimaryKey(String id);
    
    int insert(UnitFormula record);
    
    int insertSelective(UnitFormula record);
    
    UnitFormula selectByPrimaryKey(String id);
    
    int updateByPrimaryKeySelective(UnitFormula record);
    
    int updateByPrimaryKey(UnitFormula record);
    
    UnitFormula selectByUnitCode(String srcUnitCode, String targetUnit);
    
    List<UnitFormula> getAllUnitFormula();
}
