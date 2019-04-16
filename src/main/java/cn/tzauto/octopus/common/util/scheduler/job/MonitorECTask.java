package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author luosy
 */
public class MonitorECTask implements Job {

    private static final Logger logger = Logger.getLogger(MonitorECTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        logger.info("MonitorTask任务执行....");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        try {
            for (EquipHost equipHost : GlobalConstants.stage.equipHosts.values()) {
                String deviceCode = equipHost.getDeviceCode();
                DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                if (deviceInfoExt == null) {
                    logger.info("设备[" + deviceCode + "]未配置模型信息，无法执行实时参数监控！");
                    continue;
                }
                String busniessMod = deviceInfoExt.getBusinessMod();
                if ("Engineer".equals(busniessMod)) {
                    logger.info("设备[" + deviceCode + "]工程模式，取消实时Check卡控！");
                    continue;
                }
                if (!"Y".equals(deviceInfoExt.getRecipeAutoCheckFlag())) {
                    logger.info("设备[" + deviceCode + "]未设置参数实时监控！");
                    continue;
                }
                if (!equipHost.getEquipState().isCommOn()) {
                    logger.info("设备[" + deviceCode + "]设备不在通讯状态！");
                    continue;
                }
                if (!equipHost.equipStatus.equalsIgnoreCase("run")) {
                    logger.info("设备[" + deviceCode + "]设备不在运行状态！");
                    continue;
                }

                List<RecipeTemplate> recipeTemplatesAll = recipeService.searchRecipeTemplateByDeviceVariableType(deviceCode, "RealtimeECCheck");
                if (recipeTemplatesAll == null || recipeTemplatesAll.isEmpty()) {
                   UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "该设备没有实时EC参数受到管控！");
                    continue;
                }
                List svIdListAll = getEcIdList(recipeTemplatesAll);//获取机台所有参数对应的svIdList                 
                List<DeviceRealtimePara> deviceRealtimeParas = monitorService.getDeviceRealtimeParaByDeviceCode(deviceCode, null);
                Map resultMap = GlobalConstants.stage.hostManager.getMonitorParaByEC(equipHost.getDeviceId(), svIdListAll);
                logger.info("MonitorECTask获取到设备" + deviceCode + "的管控参数，内容 :" + JsonMapper.toJsonString(resultMap));
                if (resultMap != null && !resultMap.isEmpty()) {//                   
                    // List ecListAll = getECValueListFromECMap(resultMap);
                    deviceRealtimeParas = putEC2DeviceRealtimeParas(recipeTemplatesAll, equipHost, resultMap, deviceRealtimeParas);            
                    //TODO发送机台所有的参数给服务端
                    Map paraMap = new HashMap();
                    paraMap.put("msgName", "TransferArDeviceRealtimePara");
                    paraMap.put("deviceRealtimePara", JsonMapper.toJsonString(deviceRealtimeParas));
                    GlobalConstants.C2SEqptLogQueue.sendMessage(paraMap);
                   UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "成功发送定时检测该设备的EC参数实时信息到服务端");
                    //保存实时参数信息
//                    monitorService.saveDeviceRealtimePara(deviceRealtimeParas);
                   UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取到该设备的EC参数实时信息！");
                    Map mqMap = new HashMap();
                    mqMap.put("msgName", "eqpt.MonitorCheckWI");
                    mqMap.put("deviceCode", deviceCode);
                    mqMap.put("lotId", equipHost.lotId);
                    mqMap.put("eventDesc", "获取到设备:" + deviceCode + " 的实时参数管控信息");
                    GlobalConstants.C2SLogQueue.sendMessage(mqMap);
                } else {
                    //TODO SECS LOG 打印显示
                    Map mqMap = new HashMap();
                    mqMap.put("msgName", "eqpt.MonitorCheckWI");
                    mqMap.put("deviceCode", deviceCode);
                    mqMap.put("lotId", equipHost.lotId);
                    mqMap.put("eventDesc", "获取设备:" + deviceCode + " 的管控信息失败。");
                    GlobalConstants.C2SLogQueue.sendMessage(mqMap);
                    logger.debug("获取设备:" + deviceCode + " 的管控信息失败。");
                   UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取该设备的EC参数实时信息，获取失败！");
                }
                sqlSession.commit();
            }
        } catch (Exception e) {
            sqlSession.rollback();
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 把获取的实时参数受到管控信息保存到对应机台实时参数信息中
     *
     * @param recipeTemplates 实时参数受到管控信息
     * @param equipHost
     * @param svList
     * @param deviceRealtimeParas
     * @return
     */
    public static List<DeviceRealtimePara> putEC2DeviceRealtimeParas(List<RecipeTemplate> recipeTemplates, EquipHost equipHost, Map resultMap, List<DeviceRealtimePara> deviceRealtimeParas) {
        List<DeviceRealtimePara> realTimeParas = new ArrayList<>();
        boolean holdFlag = false;
        //modify by luosy @2017.6.27 realtimeParas add reciperowid
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(equipHost.getDeviceCode());
        RecipeService recipeService = new RecipeService(sqlSession);
        //根据ext表中的reciperowid获取recipepara
        Map<String, RecipePara> monitorMap = recipeService.getMonitorParas(recipeService.searchRecipeParaByRcpRowId(deviceInfoExt.getRecipeId()), equipHost.getDeviceType());

        for (int i = 0; i < recipeTemplates.size(); i++) {
            String realTimeValue = resultMap.get(recipeTemplates.get(i).getDeviceVariableId()).toString();
            if (realTimeValue.contains("E")) {
                BigDecimal bd = new BigDecimal(realTimeValue);
                realTimeValue = bd.toPlainString();
            }
            DeviceRealtimePara realtimePara = new DeviceRealtimePara();
            if (monitorMap != null && monitorMap.size() > 0) {
                RecipePara recipePara = monitorMap.get(recipeTemplates.get(i).getParaCode());
                if (recipePara != null) {
                    String minValue = recipePara.getMinValue();
                    String maxValue = recipePara.getMaxValue();
                    String setValue = recipePara.getSetValue();
                    if ("1".equals(recipeTemplates.get(i).getSpecType())) {
                        if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        if ((Double.parseDouble(realTimeValue) <= Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) >= Double.parseDouble(maxValue))) {
                            realtimePara.setRemarks("RealTimeErro");
                            holdFlag = true;
                           UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值:[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure());
                        }
                        //abs
                    } else if ("2".equals(recipeTemplates.get(i).getSpecType())) {
                        if ("".equals(setValue) || " ".equals(setValue) || "".equals(realTimeValue) || " ".equals(realTimeValue)) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        boolean paraIsNumber = false;
                        try {
                            Double.parseDouble(realTimeValue);
                            paraIsNumber = true;
                        } catch (Exception e) {
                        }
                        if (paraIsNumber) {
                            if (Double.parseDouble(realTimeValue) != Double.parseDouble(setValue)) {
                                realtimePara.setRemarks("RealTimeErro");
                                holdFlag = true;
                               UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure());
                            }
                        } else {
                            if (!realTimeValue.equals(setValue)) {
                                realtimePara.setRemarks("RealTimeErro");
                                holdFlag = true;
                               UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure());
                            }
                        }
                    } else {
                        if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        if ((Double.parseDouble(realTimeValue) < Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) > Double.parseDouble(maxValue))) {
                            realtimePara.setRemarks("RealTimeErro");
                            holdFlag = true;
                           UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值:[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure());
                        }
                    }
                    realtimePara.setMaxValue(maxValue);
                    realtimePara.setMinValue(minValue);
                    realtimePara.setSetValue(setValue);
                    realtimePara.setRealtimeValue(realTimeValue);
                    realtimePara.setId(UUID.randomUUID().toString());
                    realtimePara.setDeviceName(equipHost.getDeviceCode());
                    realtimePara.setDeviceCode(equipHost.getDeviceCode());
                    realtimePara.setParaCode(recipeTemplates.get(i).getParaCode());
                    realtimePara.setParaDesc(recipeTemplates.get(i).getParaDesc());
                    realtimePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
                    realtimePara.setParaName(recipeTemplates.get(i).getParaName());
                    realtimePara.setValueType(recipeTemplates.get(i).getParaType());
                    if (deviceRealtimeParas != null && deviceRealtimeParas.size() > 0) {
                        realtimePara.setUpdateCnt(deviceRealtimeParas.get(0).getUpdateCnt() + 1);
                    } else {
                        realtimePara.setUpdateCnt(0);
                    }
                    realtimePara.setRecipeRowId(deviceInfoExt.getRecipeId());
                    realTimeParas.add(realtimePara);
                }
            }
        }
        if (holdFlag) {
           UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), "实时参数检查不通过，设备将被锁");
            equipHost.holdDeviceAndShowDetailInfo();
        }
        sqlSession.close();
        return realTimeParas;
    }

    /**
     * 获取机台所有参数的svIdList
     *
     * @param recipeTemplates
     * @return
     */
    public static List<String> getEcIdList(List<RecipeTemplate> recipeTemplates) {
        List ecIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            ecIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return ecIdList;
    }

    private List getECValueListFromECMap(Map ecMap) {
        List ecValueList = new ArrayList();
        for (Object value : ecMap.values()) {
            ecValueList.add(value);
        }
        return ecValueList;
    }

    private static String formatUnit(String format, String value) {
        String result = "";
        String multiple = "1";
        if (format.contains("*")) {
            multiple = format.split("\\*")[1];
            result = String.valueOf(Double.valueOf(value) * Double.valueOf(multiple));
        }
        if (format.contains("/")) {
            multiple = format.split("/")[1];
            result = String.valueOf(Double.valueOf(value) / Double.valueOf(multiple));
        }
        return result;
    }

}
