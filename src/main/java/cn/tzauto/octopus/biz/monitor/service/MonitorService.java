
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.biz.monitor.service;

import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.service.BaseService;
import cn.tzauto.octopus.biz.monitor.dao.DeviceRealtimeParaMapper;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author lsy
 */
public class MonitorService extends BaseService {

    private static Logger logger = Logger.getLogger(MonitorService.class);
    private DeviceRealtimeParaMapper deviceRealtimeParaMapper;

    public MonitorService(SqlSession sqlSession) {
        super(sqlSession);
        deviceRealtimeParaMapper = this.session.getMapper(DeviceRealtimeParaMapper.class);
    }

    /**
     * 获取机台实时参数信息
     *
     * @param deviceCode
     * @param remarks
     * @return
     */
    public List<DeviceRealtimePara> getDeviceRealtimeParaByDeviceCode(String deviceCode, String remarks) {
        Long updateCnt = this.deviceRealtimeParaMapper.getMaxUpdateCnt(deviceCode);
        Map paraMap = new HashMap();
        paraMap.put("deviceCode", deviceCode);
        if (remarks != null && !"StartErro".equals(remarks)) {
            paraMap.put("updateCnt", null);
        } else {
            paraMap.put("updateCnt", updateCnt);
        }
        // paraMap.put("updateCnt", updateCnt);
        paraMap.put("remarks", remarks);
        return this.deviceRealtimeParaMapper.searchByMap(paraMap);
    }

    /**
     * 通过svName和deviceCode查询设备实施参数列表
     *
     * @param deviceCode
     * @param paraName
     * @return
     */
    public List<DeviceRealtimePara> getDeviceRealtimeParaByParas(String deviceCode, String recipeRowId, String paraName, String remarks) {
        Map paMap = new HashMap();
        paMap.put("deviceCode", deviceCode);
        paMap.put("recipeRowId", recipeRowId);
        paMap.put("paraName", paraName);
        paMap.put("remarks", remarks);
        return this.deviceRealtimeParaMapper.searchByParaMap(paMap);
    }

    /**
     * 通过svName和deviceCode查询设备实施参数列表
     *
     * @param deviceCode
     * @param paraName
     * @return
     */
    public List<DeviceRealtimePara> getParasInTime(String deviceCode, String paraName, int minuteValue) {
        Map paMap = new HashMap();
        paMap.put("deviceCode", deviceCode);
        paMap.put("paraName", paraName);
        paMap.put("minuteValue", minuteValue);
        return this.deviceRealtimeParaMapper.getParasInTime(paMap);
    }

    public int deleteRealTimeErro() {
        return deviceRealtimeParaMapper.deleteRealTimeErro();
    }

    public int deleteStartErro() {
        return deviceRealtimeParaMapper.deleteStartErro();
    }

    public int deleteErro(String deviceCode, String remarks) {
        Map paraMap = new HashMap();
        paraMap.put("deviceCode", deviceCode);
        paraMap.put("remarks", remarks);
        return this.deviceRealtimeParaMapper.deleteErro(paraMap);
    }

    /**
     * 根据clientId查询所有设备的实时参数列表
     *
     * @param clientID
     * @return
     */
    public List<DeviceRealtimePara> getAllDeviceRealtimePara(String clientID) {
        List<HashMap> dcUcList = this.deviceRealtimeParaMapper.getDeviceCodeAndMaxUpdateCnt(clientID);
        List<DeviceRealtimePara> deviceRealtimeParaList = new ArrayList<>();
        for (int i = 0; i < dcUcList.size(); i++) {
            deviceRealtimeParaList.addAll(this.deviceRealtimeParaMapper.searchByMap(dcUcList.get(i)));
        }
        return deviceRealtimeParaList;
    }

    public int saveDeviceRealtimePara(List<DeviceRealtimePara> deviceRealtimeParas) {
        return this.deviceRealtimeParaMapper.insertBatch(deviceRealtimeParas);
    }

    /**
     * 保存开机check监控到的错误的参数信息
     *
     * @param recipeParas
     * @param deviceCode
     * @return
     */
    public int saveStartCheckErroPara2DeviceRealtimePara(List<RecipePara> recipeParas, String deviceCode) {
        if (recipeParas != null && !recipeParas.isEmpty()) {
            long updateCnt = -1;
            List<DeviceRealtimePara> realtimeParasOld = this.getDeviceRealtimeParaByParas(deviceCode, recipeParas.get(0).getRecipeRowId(), null, "StartErro");
            if (realtimeParasOld != null && !realtimeParasOld.isEmpty()) {
                updateCnt = realtimeParasOld.get(0).getUpdateCnt();
            }
            List<DeviceRealtimePara> deviceRealtimeParas = new ArrayList<>();
            for (RecipePara recipePara : recipeParas) {
                DeviceRealtimePara deviceRealtimePara = new DeviceRealtimePara();
                deviceRealtimePara.setId(UUID.randomUUID().toString());
                deviceRealtimePara.setDeviceCode(deviceCode);
                deviceRealtimePara.setDeviceId("");
                deviceRealtimePara.setDeviceName("");
                deviceRealtimePara.setMinValue(recipePara.getMinValue());
                deviceRealtimePara.setMaxValue(recipePara.getMaxValue());
                deviceRealtimePara.setParaCode(recipePara.getParaCode());
                deviceRealtimePara.setParaDesc(recipePara.getParaDesc());
                deviceRealtimePara.setParaMeasure(recipePara.getParaMeasure());
                deviceRealtimePara.setParaName(recipePara.getParaName());
                deviceRealtimePara.setParaShotName(recipePara.getParaShotName());
                deviceRealtimePara.setRealtimeValue(recipePara.getSetValue());//这里的setValue是从设备取到的recipe参数实时值
                deviceRealtimePara.setSetValue(recipePara.getDefValue());//默认值
                deviceRealtimePara.setRecipeRowId(recipePara.getRecipeRowId());
                deviceRealtimePara.setRemarks("StartErro");
                deviceRealtimePara.setUpdateCnt(updateCnt + 1);
                deviceRealtimePara.setValueType("");
                deviceRealtimeParas.add(deviceRealtimePara);
            }
            return this.deviceRealtimeParaMapper.insertBatch(deviceRealtimeParas);
        } else {
            return 0;
        }
    }

    public void cleanData() {
        new Thread() {
            @Override
            public void run() {
                logger.info("开始清理 deviceRealtimeParas 数据...");
                List<DeviceRealtimePara> deviceRealtimeParas = deviceRealtimeParaMapper.selectOldData(GlobalConstants.redundancyDataSavedDays);
                logger.debug("过期 deviceRealtimeParas 数据条数：" + deviceRealtimeParas.size());
                if (deviceRealtimeParas.size() > 0) {
                    int count = deviceRealtimeParas.size();
                    List<DeviceRealtimePara> deviceRealtimeParaListTmp = new ArrayList<>();
                    if (count <= 1000) {
                        deviceRealtimeParaListTmp = deviceRealtimeParas;
                    }
                    while (count > 1000) {
                        for (DeviceRealtimePara deviceRealtimePara : deviceRealtimeParas) {
                            deviceRealtimeParaListTmp.add(deviceRealtimePara);
                            if (deviceRealtimeParaListTmp.size() >= 1000) {
                                deviceRealtimeParaMapper.deleteRealtimeParaBatch(deviceRealtimeParaListTmp);
                                logger.info("清理 deviceOplogs 数据条数：" + deviceRealtimeParaListTmp.size());
                                count = count - deviceRealtimeParaListTmp.size();
                                deviceRealtimeParaListTmp.clear();
                            }
                        }
                    }
                    deviceRealtimeParaMapper.deleteRealtimeParaBatch(deviceRealtimeParaListTmp);
                    logger.info("清理 deviceOplogs 数据条数：" + deviceRealtimeParaListTmp.size());
                }
                logger.info("deviceRealtimeParas 数据清理完成...");
            }
        }.start();
    }
}
