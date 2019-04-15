package cn.tzauto.octopus.biz.sys.dao;

import cn.tzauto.octopus.biz.sys.domain.SysUser;

import java.util.List;
import java.util.Map;

public interface SysUserMapper {

    int deleteByPrimaryKey(String id);

    int insert(SysUser record);

    int insertSelective(SysUser record);

    SysUser selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(SysUser record);

    int updateByPrimaryKeyWithBLOBs(SysUser record);

    int updateByPrimaryKey(SysUser record);

    int deleteByPrimaryKey(SysUser record);

    List<SysUser> searchByMap(Map paraMap);
    List<SysUser> searchUserByMap(Map paraMap);

    int updateSysUser(SysUser record);

    int deleteBatch(List<SysUser> sysUserList);

    int insertSysUserBatch(List<SysUser> sysUserList);
}