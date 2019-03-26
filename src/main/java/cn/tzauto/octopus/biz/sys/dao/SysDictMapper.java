package cn.tzauto.octopus.biz.sys.dao;

import cn.tzauto.octopus.biz.sys.domain.SysDict;
import java.util.List;
import java.util.Map;

public interface SysDictMapper {
    int deleteByPrimaryKey(String id);

    int insert(SysDict record);

    int insertSelective(SysDict record);

    SysDict selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(SysDict record);

    int updateByPrimaryKey(SysDict record);
    
    List<SysDict> selectAll();
    
    List<SysDict> searchByType(String type);
    
    List<Map> searchSysProperties();
}