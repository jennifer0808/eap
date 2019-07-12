/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.biz.sys.service;

import cn.tzauto.octopus.biz.sys.dao.SysDictMapper;
import cn.tzauto.octopus.biz.sys.dao.SysOfficeMapper;
import cn.tzauto.octopus.biz.sys.dao.SysUserMapper;
import cn.tzauto.octopus.biz.sys.domain.SysDict;
import cn.tzauto.octopus.biz.sys.domain.SysOffice;
import cn.tzauto.octopus.biz.sys.domain.SysUser;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.service.BaseService;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SysService extends BaseService {

    private static final Logger logger = Logger.getLogger(SysService.class);
    private SysOfficeMapper sysOfficeMapper;
    private SysUserMapper sysUserMapper;
    private SysDictMapper sysDictMapper;

    public SysService(SqlSession sqlSession) {
        super(sqlSession);
        sysOfficeMapper = this.session.getMapper(SysOfficeMapper.class);
        sysUserMapper = this.session.getMapper(SysUserMapper.class);
        sysDictMapper = this.session.getMapper(SysDictMapper.class);
    }

    /**
     * 插入数据
     *
     * @param record
     * @return
     */
    public int insert(SysUser record) {
        return this.sysUserMapper.insert(record);
    }

    /**
     * 删除
     *
     * @param record
     * @return
     */
    public int deleteByPrimaryKey(SysUser record) {
        return this.sysUserMapper.deleteByPrimaryKey(record);

    }

    /**
     * 根据登录姓名进行查询
     *
     * @param loginName
     * @return
     */
    public List<SysUser> searchRecipeByPara(String loginName) {
        Map paraMap = new HashMap();
        paraMap.put("loginName", loginName);
        return this.sysUserMapper.searchByMap(paraMap);
    }

    /**
     * 修改
     *
     * @param record 登录对象
     * @return
     */
    public int updateSysUser(SysUser record) {
        return this.sysUserMapper.updateSysUser(record);
    }


    //SysOffice
    public SysOffice selectSysOfficeByPrimaryKey(String id) {
        return this.sysOfficeMapper.selectSysOfficeByPrimaryKey(id);
    }

    public int updateSysOfficeByPrimaryKey(SysOffice record) {
        return this.sysOfficeMapper.updateSysOfficeByPrimaryKey(record);
    }

    public int insertsaveSysOffice(SysOffice record) {
        return this.sysOfficeMapper.insertsaveSysOffice(record);
    }

    /**
     * 根据登录用户名查询SysUser
     *
     * @param loginName
     * @return
     */
    public List<SysUser> searchSysUsersByLoginName(String loginName) {
        Map paraMap = new HashMap();
        paraMap.put("loginName", loginName);
        return this.sysUserMapper.searchByMap(paraMap);
    }

    /**
     * 根据登录用户名密码查询SysUser
     *
     * @param loginName
     * @return
     */
    public List<SysUser> searchSysUsersByLoginNamePassword(String loginName, String passWord) {
        Map paraMap = new HashMap();
        paraMap.put("loginName", loginName);
        paraMap.put("passWord", passWord);
        return this.sysUserMapper.searchUserByMap(paraMap);
    }

    public int insertSysUserBatch(List<SysUser> sysUserList) {
        return this.sysUserMapper.insertSysUserBatch(sysUserList);
    }

    /**
     * 根据SysUser主键id查询SysUser
     *
     * @param id
     * @return
     */
    public SysUser getSysUserById(String id) {
        return this.sysUserMapper.selectByPrimaryKey(id);
    }

    /**
     * 删除整张sys_user表里面的数据
     *
     * @return
     */
    public int deleteSysUserBatch(List<SysUser> sysUserList) {
        return this.sysUserMapper.deleteBatch(sysUserList);
    }

    /**
     * 更新SysUSer
     *
     * @param sysUser
     * @return
     */
    public int modifySysUser(SysUser sysUser) {
        return this.sysUserMapper.updateByPrimaryKeyWithBLOBs(sysUser);
    }

    public Map selectAllDictFromDB() {
        Map paraMap = new HashMap();
        List<SysDict> sysDicts = this.sysDictMapper.selectAll();
        for (SysDict sysDict : sysDicts) {
            if (!paraMap.containsKey(sysDict.getType())) {
                List<SysDict> sysDictList = new ArrayList();
                sysDictList.add(sysDict);
                paraMap.put(sysDict.getType(), sysDictList);
            } else {
                List<SysDict> sysDictList = (List<SysDict>) paraMap.get(sysDict.getType());
                sysDictList.add(sysDict);
                paraMap.put(sysDict.getType(), sysDictList);
            }
        }
        return paraMap;
    }

    public Map selectAllDictLabelFromDB() {
        Map paraMap = new HashMap();
        List<SysDict> sysDicts = this.sysDictMapper.selectAll();
        for (SysDict sysDict : sysDicts) {
            if (!paraMap.containsKey(sysDict.getType())) {
                List<String> sysDictLabels = new ArrayList();
                sysDictLabels.add(sysDict.getLabel());
                paraMap.put(sysDict.getType(), sysDictLabels);
            } else {
                List<String> sysDictLabels = (List<String>) paraMap.get(sysDict.getType());
                sysDictLabels.add(sysDict.getLabel());
                paraMap.put(sysDict.getType(), sysDictLabels);
            }
        }
        return paraMap;
    }

    public List<SysDict> searchByType(String type) {
        return this.sysDictMapper.searchByType(type);
    }

    public List<Map> searchAllSysProperty() {
        return this.sysDictMapper.searchSysProperties();
    }

    public int modifyVersionNo(String versionNo) {
        return this.sysDictMapper.updateVersionNo(versionNo);
    }
}
