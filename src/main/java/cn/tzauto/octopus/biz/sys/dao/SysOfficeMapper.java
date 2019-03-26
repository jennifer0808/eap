package cn.tzauto.octopus.biz.sys.dao;

import cn.tzauto.octopus.biz.sys.domain.SysOffice;

public interface SysOfficeMapper {
    int deleteByPrimaryKey(String id);

    int insertsaveSysOffice(SysOffice record);

    int insertSelective(SysOffice record);

    SysOffice selectSysOfficeByPrimaryKey(String id);

    int updateByPrimaryKeySelective(SysOffice record);

    int updateByPrimaryKeyWithBLOBs(SysOffice record);

    int updateSysOfficeByPrimaryKey(SysOffice record);
}