/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.biz.device.service;

import cn.tzauto.octopus.biz.sys.dao.SysOfficeMapper;
import cn.tzauto.octopus.biz.sys.domain.SysOffice;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.service.BaseService;
import cn.tzauto.octopus.biz.device.dao.ClientInfoMapper;
import cn.tzauto.octopus.biz.device.dao.DeviceInfoExtMapper;
import cn.tzauto.octopus.biz.device.dao.DeviceInfoLockMapper;
import cn.tzauto.octopus.biz.device.dao.DeviceInfoMapper;
import cn.tzauto.octopus.biz.device.dao.DeviceOplogMapper;
import cn.tzauto.octopus.biz.device.dao.DeviceTypeMapper;
import cn.tzauto.octopus.biz.device.dao.EqpFunctionMapper;
import cn.tzauto.octopus.biz.device.dao.EquipSecsConstantsMapper;
import cn.tzauto.octopus.biz.device.dao.EquipSvecMapper;
import cn.tzauto.octopus.biz.device.dao.StationMapper;
import cn.tzauto.octopus.biz.device.dao.UnitFormulaMapper;
import cn.tzauto.octopus.biz.device.domain.ClientInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoLock;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.domain.EquipSecsConstants;
import cn.tzauto.octopus.biz.device.domain.EquipSvec;
import cn.tzauto.octopus.biz.device.domain.UnitFormula;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 * @author gavin
 */
public class DeviceService extends BaseService {

    private static Logger logger = Logger.getLogger(DeviceService.class);
    private DeviceInfoMapper deviceInfoMapper;
    private DeviceTypeMapper deviceTypeMapper;
    private DeviceOplogMapper deviceOplogMapper;
    private ClientInfoMapper clientInfoMapper;
    private EquipSvecMapper equipSvecMapper;
    private StationMapper stationMapper;
    private DeviceInfoExtMapper deviceInfoExtMapper;
    private SysOfficeMapper sysOfficeMapper;
    private DeviceInfoLockMapper deviceInfoLockMapper;
    private EquipSecsConstantsMapper equipSecsConstantsMapper;
    private EqpFunctionMapper eqpFunctionMapper;
    private UnitFormulaMapper unitFormulaMapper;

    public DeviceService(SqlSession sqlSession) {
        super(sqlSession);
        deviceInfoMapper = this.session.getMapper(DeviceInfoMapper.class);
        deviceTypeMapper = this.session.getMapper(DeviceTypeMapper.class);
        equipSvecMapper = this.session.getMapper(EquipSvecMapper.class);
        clientInfoMapper = this.session.getMapper(ClientInfoMapper.class);
        stationMapper = this.session.getMapper(StationMapper.class);
        deviceOplogMapper = this.session.getMapper(DeviceOplogMapper.class);
        deviceInfoExtMapper = this.session.getMapper(DeviceInfoExtMapper.class);
        sysOfficeMapper = this.session.getMapper(SysOfficeMapper.class);
        deviceInfoLockMapper = this.session.getMapper(DeviceInfoLockMapper.class);
        equipSecsConstantsMapper = this.session.getMapper(EquipSecsConstantsMapper.class);
        eqpFunctionMapper = this.session.getMapper(EqpFunctionMapper.class);
        unitFormulaMapper = this.session.getMapper(UnitFormulaMapper.class);
    }

    /**
     * 生成以deviceTypeId为key的map，方便查询设备类型
     *
     * @return
     */
    public Map<String, DeviceType> getDeviceTypeMap() {
        Map<String, DeviceType> resultMap = new HashMap();
        List<DeviceType> deviceTypeList = getAllDeviceType();
        for (DeviceType deviceType : deviceTypeList) {
            resultMap.put(deviceType.getId(), deviceType);
        }
//        for (int i = 0; i < deviceTypeList.size(); i++) {
//            DeviceType deviceType = deviceTypeList.get(i)
//        }
        return resultMap;
    }

    /**
     * 通过typeCode, manufacturerName查询DeviceType
     *
     * @param typeCode
     * @param manufacturerName
     * @return
     */
    public DeviceType getDeviceType(String typeCode, String manufacturerName) {
        Map paMap = new HashMap();
        paMap.put("typeCode", typeCode);
        paMap.put("manufacturerName", manufacturerName);
        return this.deviceTypeMapper.searchByPaMap(paMap);
    }

    public int deleteDeviceType(String id) {
        return this.deviceTypeMapper.deleteByPrimaryKey(id);
    }

    public List getAllDeviceType() {
        return this.deviceTypeMapper.searchByMap(null);
    }

    public List<DeviceInfo> getDeviceByMap(Map paraMap) {
        return (List<DeviceInfo>) this.deviceInfoMapper.searchByMap(paraMap);
    }

    public List<DeviceInfo> getDeviceInfoByMap(Map paraMap) {
        return this.deviceInfoMapper.searchByMap(paraMap);
    }

    public DeviceInfo selectDeviceInfoByDeviceCode(String deviceCode) {
        return this.deviceInfoMapper.selectDeviceInfoByDeviceCode(deviceCode);
    }

    public DeviceInfoExt selectByPrimaryKey(String id) {
        return this.deviceInfoExtMapper.selectByPrimaryKey(id);
    }

    /**
     * 通过deviceRowid（deviceInfo的deviceCode）查询设备模板表信息DeviceInfoExt
     *
     * @return
     */
    public DeviceInfoExt getDeviceInfoExtByDeviceCode(String deviceCode) {
        return this.deviceInfoExtMapper.selectByDeviceRowid(deviceCode);
    }

    public DeviceInfoExt selectDeviceInfoExtByPrimaryKey(String id) {
        return this.deviceInfoExtMapper.selectByPrimaryKey(id);
    }

    /**
     * 更新设备模板表里面的信息
     *
     * @param deviceInfoExt
     * @return
     */
    public int modifyDeviceInfoExt(DeviceInfoExt deviceInfoExt) {
        return this.deviceInfoExtMapper.updateByPrimaryKeySelective(deviceInfoExt);
    }

    /**
     * 保存DeviceInfoExt到设备模板表
     *
     * @param deviceInfoExt
     * @return
     */
    public int saveDeviceInfoExt(DeviceInfoExt deviceInfoExt) {
        return this.deviceInfoExtMapper.insert(deviceInfoExt);
    }

    /**
     * 查询clientId工控下的所管控的机台
     *
     * @param clientId
     * @return
     */
    public String[] queryDeviceCodeByClientId(String clientId) {
        List<DeviceInfo> deviceInfoList = getDeviceInfoByClientId(clientId);
        String[] deviceCodes = new String[deviceInfoList.size()];
        if (deviceInfoList.size() > 0) {
            for (int i = 0; i < deviceInfoList.size(); i++) {
                deviceCodes[i] = deviceInfoList.get(i).getDeviceCode();
            }
            return deviceCodes;
        } else {
            return null;
        }
    }

    /**
     * 接收mq消息，并更新设备信息DeviceInfo
     *
     * @param josnStr
     * @return
     */
    public boolean updateFromMQ(String josnStr) {
        try {
            //转换json为对象列表
            //校验Device对象是否完整
            //更新单条Device表记录
            DeviceInfo deviceInfo = new DeviceInfo();
            int updateResult = updateDeviceInfo(deviceInfo);

            //插入Mq更新记录
            this.session.commit();
        } catch (Exception e) {
            this.session.rollback();
            return false;
        }

        return true;
    }

    /**
     * 根据deviceCode查询设备操作日志
     *
     * @param deviceCode
     * @return
     */
    public List<DeviceOplog> getDeviceOplog(String deviceCode) {
        Map paMap = new HashMap();
        paMap.put("deviceCode", deviceCode);
        return this.deviceOplogMapper.selectByParaMap(paMap);
    }

    /**
     * 更新设备信息
     *
     * @param deviceInfo
     * @return
     */
    public int updateDeviceInfo(DeviceInfo deviceInfo) {
        return this.deviceInfoMapper.updateByPrimaryKeySelective(deviceInfo);
    }

    public int deleteDeviceInfo(String id) {
        return this.deviceInfoMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据clientId删除当前工控下不需要管控的机台
     *
     * @param clientId
     * @return
     */
    public int deleteDeviceInfoByClientId(String clientId) {
        return this.deviceInfoMapper.deleteByClientId(clientId);
    }

    /**
     * 批量删除设备信息表里面的设备记录
     *
     * @param deviceInfoList
     * @return
     */
    public int batchDeleteDeviceInfo(List<DeviceInfo> deviceInfoList) {
        return this.deviceInfoMapper.batchDeleteDeviceInfo(deviceInfoList);
    }

    /**
     * 通过deviceCode查询设备信息表中的设备记录
     *
     * @param deviceCode
     * @return
     */
    public List<DeviceInfo> getDeviceInfoByDeviceCode(String deviceCode) {
        Map paraMap = new HashMap();
        paraMap.put("deviceCode", deviceCode);
        return this.deviceInfoMapper.searchByMap(paraMap);
    }

    /**
     * 通过clientId查询设备表里面的设备记录
     *
     * @param clientId
     * @return
     */
    public List<DeviceInfo> getDeviceInfoByClientId(String clientId) {
        Map paraMap = new HashMap();
        paraMap.put("clientId", clientId);
        return this.deviceInfoMapper.searchByMap(paraMap);
    }

    public List<DeviceInfo> getDeviceInfo(String clientId) {
        return this.deviceInfoMapper.getDeviceInfo(clientId);
    }

    public int saveDeviceInfo(DeviceInfo deviceInfo) {
        return this.deviceInfoMapper.insert(deviceInfo);
    }

    /**
     * 批量保存设备信息
     *
     * @param deviceInfoList
     * @return
     */
    public int saveDeviceInfoBatch(List<DeviceInfo> deviceInfoList) {
        return this.deviceInfoMapper.insertDeviceInfoBatch(deviceInfoList);
    }

    public int modifyDeviceInfo(DeviceInfo deviceInfo) {
        return this.deviceInfoMapper.updateByPrimaryKey(deviceInfo);
    }

    public List<DeviceType> getDeviceTypeByMap(Map paraMap) {
        return this.deviceTypeMapper.searchByMap(paraMap);
    }

    /**
     * 通过typeCode查询DeviceType
     *
     * @param typeCode
     * @return
     */
    public List<DeviceType> queryDeviceTypeBytypeCode(String typeCode) {
        Map paraMap = new HashMap();
        paraMap.put("typeCode", typeCode);
        return getDeviceTypeByMap(paraMap);
    }

    /**
     * 通过DeviceType对象的在数据库中的主键id查询DeviceType对象
     *
     * @param id
     * @return
     */
    public DeviceType queryDeviceTypeById(String id) {
        return this.deviceTypeMapper.selectByPrimaryKey(id);
    }

    public int saveDeviceType(DeviceType deviceType) {
        return this.deviceTypeMapper.insert(deviceType);
    }

    public int saveDeviceOplog(DeviceOplog deviceOplog) {
        return this.deviceOplogMapper.insert(deviceOplog);
    }

    /**
     * 根据dateStr查询设备操作记录表
     *
     * @param dateStr
     * @return
     */
    public List<DeviceOplog> queryDeviceOplogByDate(String dateStr) {
        Map paraMap = new HashMap();
        paraMap.put("opTime", dateStr);
        return this.deviceOplogMapper.searchByMap(paraMap);
    }

    public int updateDeviceType(DeviceType deviceType) {
        return this.deviceTypeMapper.updateByPrimaryKeySelective(deviceType);
    }

    /**
     * 同过clientCode查询工控信息
     *
     * @param paraMap
     * @return
     */
    public List<ClientInfo> getClientInfoByMap(Map paraMap) {
        return this.clientInfoMapper.searchByMap(paraMap);
    }

    public ClientInfo queryClientInfoByClientCode(String clientCode) {
        Map paraMap = new HashMap();
        paraMap.put("clientCode", clientCode);
        List<ClientInfo> currentClientInfos = getClientInfoByMap(paraMap);
        if (currentClientInfos != null && currentClientInfos.size() > 0) {
            return getClientInfoByMap(paraMap).get(0);
        } else {
            return null;
        }

    }

    public ClientInfo searchClientInfoByClientCode(String clientCode) {
        return clientInfoMapper.searchClientByClientCode(clientCode);
    }

    public int saveClientInfo(ClientInfo clientInfo) {
        return this.clientInfoMapper.insert(clientInfo);
    }

    public int upDateClient(ClientInfo clientInfo) {
        return this.clientInfoMapper.updateByPrimaryKey(clientInfo);
    }

    public int deleteClientInfo(String id) {
        return this.clientInfoMapper.deleteByPrimaryKey(id);
    }

    public List<EquipSvec> getEquipSvecByMap(Map paraMap) {
        return this.equipSvecMapper.searchByMap(paraMap);
    }

    /**
     * 通过paraId查询EquipSvec
     *
     * @param paraId
     * @return
     */
    public List<EquipSvec> queryEquipSvecByParaId(String paraId) {
        Map paraMap = new HashMap();
        paraMap.put("paraId", paraId);
        return getEquipSvecByMap(paraMap);
    }

    /**
     * 通过 clientId, deviceCode查询设备信息列表
     *
     * @param clientId
     * @param deviceCode
     * @return
     */
    public List<DeviceInfo> searchDeviceInfoByPara(String clientId, String deviceCode) {
        Map paraMap = new HashMap();
        paraMap.put("clientId", clientId);
        paraMap.put("deviceCode", deviceCode);
        return this.deviceInfoMapper.searchByMap(paraMap);
    }

    /**
     * 根据recipe类型和名称，生成recipe的保存路径
     *
     * @param recipeType
     * @param recipeName
     * @return
     */
    public String organizeRecipePath(String deviceCode, String recipeType, String recipeName) {
        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(deviceCode);
        SysOffice sysOffice = sysOfficeMapper.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
        String returnPath = "";
        returnPath = "/RECIPE/" + sysOffice.getPlant() + "/" + sysOffice.getName() + "/" + deviceInfo.getDeviceType() + "/" + recipeType;
        if ("GOLD".equalsIgnoreCase(recipeType)) {
            returnPath = returnPath + "/" + recipeName.replaceAll("/", "@");
        } else if ("UNIQUE".equalsIgnoreCase(recipeType)) {
            returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipeName.replaceAll("/", "@");
        } else if ("ENGINEER".equalsIgnoreCase(recipeType)) {
            returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipeName.replaceAll("/", "@") + "/" + recipeName.replaceAll("/", "@");
        }
        return returnPath;
    }


    /*
     * 通过deviceRowId查询信息
     */
    public List<DeviceInfoLock> selectDeviceInfoLockByDeviceRowId(String deviceRowId) {
        return deviceInfoLockMapper.selectByDeviceRowId(deviceRowId);
    }

    public int saveDeviceInfoLock(DeviceInfoLock record) {
        return deviceInfoLockMapper.insert(record);
    }

    public int deleteDeviceInfoLock(DeviceInfoLock record) {
        return deviceInfoLockMapper.delete(record);
    }

    public int modifyDeviceInfoLock(DeviceInfoLock record) {
        return deviceInfoLockMapper.updateByPrimaryKey(record);
    }

    /**
     * 通过lock_type查询信息
     */
    public List<DeviceInfoLock> selectDeviceInfoLockByType(String type) {
        return deviceInfoLockMapper.selectByType(type);
    }

    public int saveDeviceInfoLockBatch(List<DeviceInfoLock> deviceInfoLocks) {
        return deviceInfoLockMapper.saveDeviceInfoLockBatch(deviceInfoLocks);
    }

    public int deleteDeviceInfoLockBatch(List<DeviceInfoLock> deviceInfoLocks) {
        return deviceInfoLockMapper.deleteDeviceInfoLockBatch(deviceInfoLocks);
    }

    public List<DeviceInfoLock> searchDeviceInfoLockByMap(String deviceCode, String lockType, String lockStatus) {
        Map map = new HashMap();
        map.put("deviceRowid", deviceCode);
        map.put("lockType", lockType);
        map.put("lockStatus", lockStatus);
        return deviceInfoLockMapper.searchByMap(map);
    }

    public void updateDeviceInfoLock(String deviceCode, String lockType, String lockStatus) {
        List<DeviceInfoLock> deviceInfoLocks = this.searchDeviceInfoLockByMap(deviceCode, lockType, null);
        if (deviceInfoLocks != null && deviceInfoLocks.size() > 0) {
            DeviceInfoLock deviceInfoLock = deviceInfoLocks.get(0);
            deviceInfoLock.setLockStatus(lockStatus);
            this.modifyDeviceInfoLock(deviceInfoLock);
        } else {
            DeviceInfoLock deviceInfoLock = this.setDeviceInfoLock(deviceCode, lockType, lockStatus);
            this.saveDeviceInfoLock(deviceInfoLock);
        }
    }

    public int saveDeviceInfoExtBatch(List<DeviceInfoExt> deviceInfoExts) {
        return deviceInfoExtMapper.saveDeviceInfoExtBatch(deviceInfoExts);
    }

    public int deleteDeviceInfoExtByIdBatch(List<DeviceInfoExt> deviceInfoExts) {
        return deviceInfoExtMapper.deleteDeviceInfoExtByIdBatch(deviceInfoExts);
    }

    public int deleteDeviceInfoExtByDeviceRowIdBatch(List<DeviceInfoExt> deviceInfoExts) {
        return deviceInfoExtMapper.deleteDeviceInfoExtByDeviceRowIdBatch(deviceInfoExts);

    }

    public DeviceInfoLock setDeviceInfoLock(String deviceCode, String lockType, String lockStatus) {
        DeviceInfoLock deviceInfoLock = new DeviceInfoLock();
        String id = UUID.randomUUID().toString();
        deviceInfoLock.setId(id);
        deviceInfoLock.setDeviceRowid(deviceCode);
        deviceInfoLock.setLockType(lockType);
        deviceInfoLock.setLockStatus(lockStatus);
        deviceInfoLock.setVerNo(0);
        deviceInfoLock.setDelFlag("0");
        return deviceInfoLock;
    }

    public List<EquipSecsConstants> selectEquipSecsConstants(String deviceTypeId, String deviceTypeName, String constansType, String constansGroup, String constansCode) {
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeId", deviceTypeId);
        paraMap.put("deviceTypeName", deviceTypeName);
        paraMap.put("constansType", constansType);
        paraMap.put("constansGroup", constansGroup);
        paraMap.put("constansCode", constansCode);
        return this.equipSecsConstantsMapper.selectByParaMap(paraMap);
    }

    public boolean checkFunctionSwitch(String deviceCode, String functionCode) {
        Map paraMap = new HashMap();
        paraMap.put("deviceCode", deviceCode);
        paraMap.put("functionCode", functionCode);
        String result = this.eqpFunctionMapper.checkFunctionSwitch(paraMap);
        boolean pass = false;
        if (result != null && "1".equals(result)) {
            pass = true;
        }
        return pass;
    }

    public void cleanData() {
        new Thread() {
            @Override
            public void run() {
                logger.info("开始清理 deviceOplogs 数据...");
                List<DeviceOplog> deviceOplogs = deviceOplogMapper.selectOldData(GlobalConstants.redundancyDataSavedDays);
                logger.debug("过期 deviceOplogs 数据条数：" + deviceOplogs.size());
                if (deviceOplogs.size() > 0) {
                    int count = deviceOplogs.size();
                    List deviceOplogListTmp = new ArrayList<DeviceOplog>();
                    if (count <= 1000) {
                        deviceOplogListTmp = deviceOplogs;
                    }
                    while (count > 1000) {
                        for (DeviceOplog deviceOplog : deviceOplogs) {
                            deviceOplogListTmp.add(deviceOplog);
                            if (deviceOplogListTmp.size() >= 1000) {
                                deviceOplogMapper.deleteOpLogBatch(deviceOplogListTmp);
                                logger.info("清理 deviceOplogs 数据条数：" + deviceOplogListTmp.size());
                                count = count - deviceOplogListTmp.size();
                                deviceOplogListTmp.clear();
                            }
                        }
                    }
                    deviceOplogMapper.deleteOpLogBatch(deviceOplogListTmp);
                    logger.info("清理 deviceOplogs 数据条数：" + deviceOplogListTmp.size());
                }
                logger.info("deviceOplogs 数据清理完成...");
            }
        }.start();

    }

    public UnitFormula getUnitFormulaByUnitCode(String srcUnitCode, String targetUnitCode) {
        Map paraMap = new HashMap();
        paraMap.put("srcUnitCode", srcUnitCode);
        paraMap.put("tgtUnitCode", targetUnitCode);
        return unitFormulaMapper.selectByUnitCode(srcUnitCode, targetUnitCode);
    }

    public Map<String, Map<String, UnitFormula>> getAllUnitFormula() {
        Map<String, Map<String, UnitFormula>> unitFormulaMap = new HashMap();
        List<UnitFormula> unitFormulas = unitFormulaMapper.getAllUnitFormula();
        for (UnitFormula unitFormula : unitFormulas) {
            String srcUnit = unitFormula.getSrcUnitCode();
            unitFormulaMap.put(srcUnit, null);
        }
        for (Map.Entry<String, Map<String, UnitFormula>> entry : unitFormulaMap.entrySet()) {
            String srcUnit = entry.getKey();
            Map tgtUnitFormulaMap = new HashMap<String, UnitFormula>();
            for (UnitFormula unitFormula : unitFormulas) {
                if (srcUnit.equals(unitFormula.getSrcUnitCode())) {
                    tgtUnitFormulaMap.put(unitFormula.getTgtUnitCode(), unitFormula);
                }
            }
            unitFormulaMap.put(srcUnit, tgtUnitFormulaMap);
        }
        return unitFormulaMap;
    }

    public List<DeviceInfoExt> getAllDeviceInfoExts() {
        return deviceInfoExtMapper.getAllDeviceInfoExts();
    }

    public String queryWaferPath(String waferId) {
        return deviceInfoMapper.queryWaferPath(waferId);
    }

    public void insertWaferMappingPath(String name, String filePath, String month) {
        deviceInfoMapper.insertWaferMappingPath(name, filePath, month);
    }
}
