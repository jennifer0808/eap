/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.Job;
import org.quartz.JobExecutionContext;


import java.util.*;

/**
 *
 */
public class MonitorTask implements Job {

    private static final Logger logger = Logger.getLogger(MonitorTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        logger.info("MonitorTask任务执行....");
        monitorReflow();
        //Map<String, EquipHost> equipHosts = GlobalConstants.stage.equipHosts;
        //TODO 根据deviceCode 查询出该机台需要管控参数的对应svid 返回一个list
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        String temp = (String) MDC.get(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
        try {
            for (EquipHost equipHost : GlobalConstants.stage.equipHosts.values()) {
                MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, equipHost.getDeviceCode());
                try {
                    String deviceCode = equipHost.getDeviceCode();
                    DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                    if (deviceInfoExt == null) {
                        logger.info("设备" + deviceCode + " 未配置模型信息，无法执行实时参数监控！");
                        continue;
                    }
                    String busniessMod = deviceInfoExt.getBusinessMod();
                    if ("Engineer".equals(busniessMod)) {
                       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "工程模式，取消实时Check卡控！");
                        continue;
                    }
                    if (!"Y".equals(deviceInfoExt.getRecipeAutoCheckFlag())) {
//                       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "该设备未设置参数实时监控！");
                        logger.info("设备" + deviceCode + "未设置参数实时监控！");
                        continue;
                    }
                    if (!equipHost.getEquipState().isCommOn()) {
                        logger.info("设备" + deviceCode + "设备不在通讯状态！");
                        continue;
                    }
                    if (!equipHost.equipStatus.equalsIgnoreCase("run")) {
                        logger.info("设备" + deviceCode + "设备不在运行状态！");
                        continue;
                    }
                    // MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
                    List<RecipeTemplate> recipeTemplates = new ArrayList<>();//实时参数受到管控信息
                    List<RecipeTemplate> recipeTemplatesAll = new ArrayList<>();//机台的所有参数信息
                    if (distPress(equipHost.getDeviceType())) {
                        recipeTemplates = getPressSv(equipHost);
                    } else {
                        recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipeParaCheck");
                    }
                    if (recipeTemplates == null || recipeTemplates.isEmpty()) {
                       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "该设备没有实时参数受到管控！");
                        continue;
                    }
                    recipeTemplatesAll = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipeParaCheck");
                    List svIdListAll = getSvIdList(recipeTemplatesAll);//获取机台所有参数对应的svIdList
                    List svIdList = getSvIdList(recipeTemplates);//获取机台实时管控参数的svIdList
                    List<DeviceRealtimePara> deviceRealtimeParas = monitorService.getDeviceRealtimeParaByDeviceCode(deviceCode, null);
                    Map resultMap = GlobalConstants.stage.hostManager.getMonitorParaBySV(equipHost.getDeviceId(), svIdListAll);
                    logger.info("MonitorTask获取到设备" + deviceCode + "的管控参数，内容 :" + JsonMapper.toJsonString(resultMap));
                    if (resultMap != null && !resultMap.isEmpty()) {//                   
                        List svListAll = getSvValueListFromSvMap(resultMap);
                        if (svIdListAll.size() != svIdList.size() && !svIdList.isEmpty()) {
                            List svList = new ArrayList();
                            for (int i = 0; i < svIdList.size(); i++) {
                                for (int j = 0; j < svIdListAll.size(); j++) {
                                    if (svIdList.get(i).equals(svIdListAll.get(j))) {//                                      
                                        svList.add(svListAll.get(j));
                                        break;
                                    }
                                }
                            }
                            deviceRealtimeParas = putSV2DeviceRealtimeParas(recipeTemplates, resultMap, deviceRealtimeParas, equipHost.getDeviceCode());
                        } else {
                            deviceRealtimeParas = putSV2DeviceRealtimeParas(recipeTemplatesAll, resultMap, deviceRealtimeParas, equipHost.getDeviceCode());
                        }
                        //TODO发送机台所有的参数给服务端     
                        Map paraMap = new HashMap();
                        paraMap.put("msgName", "TransferArDeviceRealtimePara");
                        paraMap.put("deviceRealtimePara", JsonMapper.toJsonString(deviceRealtimeParas));
                        sendMessage2Server(paraMap);
                       UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "成功发送定时检测该设备的sv参数实时信息到服务端");
                        //保存实时参数信息
                        // monitorService.saveDeviceRealtimePara(deviceRealtimeParas);
                       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取到该设备的SV参数实时信息！");
                        Map mqMap = new HashMap();
                        mqMap.put("msgName", "eqpt.MonitorCheckWI");
                        mqMap.put("deviceCode", deviceCode);
                        mqMap.put("lotId", equipHost.lotId);
                        mqMap.put("eventDesc", "获取到设备:" + deviceCode + " 的实时参数管控信息");
                        sendMessage2Server(mqMap);
                    } else {
                        // SECS LOG 打印显示
                        Map mqMap = new HashMap();
                        mqMap.put("msgName", "eqpt.MonitorCheckWI");
                        mqMap.put("deviceCode", deviceCode);
                        mqMap.put("lotId", equipHost.lotId);
                        mqMap.put("eventDesc", "获取设备:" + deviceCode + " 的管控信息失败。");
                        sendMessage2Server(mqMap);
                        logger.debug("获取设备:" + deviceCode + " 的管控信息失败。");
                       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取该设备的SV参数实时信息，获取失败！");
                    }
                    sqlSession.commit();
                } catch (Exception e) {
                    sqlSession.rollback();
                    logger.error("Exception:", e);
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            if(temp ==null){
                MDC.remove(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
            }else{
                MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, temp);
            }
            sqlSession.close();
        }
    }

    private void sendMessage2Server(Map map) {
        if (!GlobalConstants.isLocalMode) {
            GlobalConstants.C2SLogQueue.sendMessage(map);
        }
    }

    /**
     * 获取机台所有的参数
     *
     * @param recipeTemplates
     * @param deviceRealtimeParas
     * @return
     */
    public static List<DeviceRealtimePara> putSV2DeviceRealtimeParas(List<RecipeTemplate> recipeTemplates, Map resultMap, List<DeviceRealtimePara> deviceRealtimeParas, String deviceCode) {
        EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceCode);
        List<DeviceRealtimePara> realTimeParas = new ArrayList<>();
        boolean holdFlag = false;
        //modify by luosy @2017.6.27 realtimeParas add reciperowid
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        RecipeService recipeService = new RecipeService(sqlSession);
        //根据ext表中的reciperowid获取recipepara
        Map<String, RecipePara> monitorMap = recipeService.getMonitorParas(recipeService.searchRecipeParaByRcpRowId(deviceInfoExt.getRecipeId()), deviceInfo.getDeviceType());
        String eventDescEng = "";
        for (int i = 0; i < recipeTemplates.size(); i++) {
            String realTimeValue = ((RecipePara) resultMap.get(recipeTemplates.get(i).getParaCode())).getSetValue();
            DeviceRealtimePara realtimePara = new DeviceRealtimePara();
            if (monitorMap != null && monitorMap.size() > 0) {
                RecipePara recipePara = monitorMap.get(recipeTemplates.get(i).getParaCode());
                if (recipePara != null && recipePara.getSetValue() != null && !"".equals(recipePara.getSetValue().trim())) {
                    String minValue = recipePara.getMinValue();
                    String maxValue = recipePara.getMaxValue();
                    String setValue = recipePara.getSetValue();
                    if ("1".equals(recipeTemplates.get(i).getSpecType())) {
                        if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        if ((Double.parseDouble(realTimeValue) < Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) > Double.parseDouble(maxValue))) {
                            realtimePara.setRemarks("RealTimeErro");
                            holdFlag = true;
                           UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure());
                            eventDescEng = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "/r/n";
                            if (equipModel != null) {
                                equipModel.sendMessage2Eqp(eventDescEng);
                            }
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
                                eventDescEng = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue();
                                if (equipModel != null) {
                                    equipModel.sendMessage2Eqp(eventDescEng);
                                }
                            }
                        } else {
                            if (!realTimeValue.equals(setValue)) {
                                realtimePara.setRemarks("RealTimeErro");
                                holdFlag = true;
                               UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure());
                                eventDescEng = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue();

                                if (equipModel != null) {
                                    equipModel.sendMessage2Eqp(eventDescEng);
                                }
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
                            eventDescEng = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "/r/n";

                            if (equipModel != null) {
                                equipModel.sendMessage2Eqp(eventDescEng);
                            }
                        }
                    }
                    realtimePara.setMaxValue(maxValue);
                    realtimePara.setMinValue(minValue);
                    realtimePara.setSetValue(setValue);
                    realtimePara.setRealtimeValue(realTimeValue);
                    realtimePara.setId(UUID.randomUUID().toString());
                    realtimePara.setDeviceName(deviceCode);
                    realtimePara.setDeviceCode(deviceCode);
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
        if ("Y".equals(deviceInfoExt.getLockSwitch())) {
            if (holdFlag) {
                AxisUtility.setInlineLock(deviceInfo.getDeviceCode(), "Y", "RealTimeValueErrorLock");
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "实时参数检查不通过，设备将被锁");
                GlobalConstants.stage.hostManager.stopDevice(deviceInfo.getDeviceCode());
//            GlobalConstants.stage.equipModels.get(deviceInfo.getDeviceCode()()).sendMessage2Eqp("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:/r/n" + eventDescEng);

            } else {
                AxisUtility.setInlineLock(deviceInfo.getDeviceCode(), "N", "RealTimeValueErrorLock");
            }
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
    public static List<String> getSvIdList(List<RecipeTemplate> recipeTemplates) {
        List svIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            svIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return svIdList;
    }

    /**
     * 获取TOWAY1R，TOWAYPM类型机台的press，recipeTemplates：实时参数受到管控信息
     *
     * @param
     * @return
     */
    private List<RecipeTemplate> getPressSv(EquipHost equipHost) {
        List<RecipeTemplate> recipeTemplates = new ArrayList<>();
        List<String> pressList = new ArrayList();
        if (equipHost.pressUseMap.containsKey(1)) {
            if (equipHost.pressUseMap.get(1)) {
                String p1 = "P1RecipeParaCheck";
                pressList.add(p1);
            }
        }
        if (equipHost.pressUseMap.containsKey(2)) {
            if (equipHost.pressUseMap.get(2)) {
                String p2 = "P2RecipeParaCheck";
                pressList.add(p2);
            }
        }
        if (equipHost.pressUseMap.containsKey(3)) {
            if (equipHost.pressUseMap.get(3)) {
                String p3 = "P3RecipeParaCheck";
                pressList.add(p3);
            }
        }
        if (equipHost.pressUseMap.containsKey(4)) {
            if (equipHost.pressUseMap.get(4)) {
                String p4 = "P4RecipeParaCheck";
                pressList.add(p4);
            }
        }
//        if (equipHost.getDeviceType().contains("FICO")) {
//            pressList.add("RecipeParaCheck");
//        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            if (!pressList.isEmpty()) {
                recipeTemplates = recipeService.searchPressRecipeTemplateByDeviceCode(equipHost.getDeviceType(), pressList);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
        return recipeTemplates;
    }

    //判断是否根据press进行参数管控
    public boolean distPress(String deviceType) {
        boolean flag = false;
        if (deviceType.contains("TOWA")) {
            flag = true;
        } else if (deviceType.contains("FICO")) {
            flag = true;
        }
        return flag;
    }

    private List getSvValueListFromSvMap(Map svMap) {
        List svValueList = new ArrayList();
        for (Object value : svMap.values()) {
            svValueList.add(value);
        }
        return svValueList;
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

    private void monitorReflow() {

        logger.info("MonitorReflow任务执行....");
        //Map<String, EquipHost> equipHosts = GlobalConstants.stage.equipHosts;
        //TODO 根据deviceCode 查询出该机台需要管控参数的对应svid 返回一个list
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        String temp = (String) MDC.get(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
        try {
            for (EquipModel equipModel : GlobalConstants.stage.equipModels.values()) {
                MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, equipModel.deviceCode);
                if (equipModel.deviceType.contains("BTU") || equipModel.deviceType.contains("HELLER") || equipModel.deviceType.contains("HTM5022")) {
                    try {
                        String deviceCode = equipModel.deviceCode;
                        if (equipModel.checkLockFlagFromServerByWS(deviceCode)) {
                           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                            equipModel.stopEquip();
                            return;
                        }
                        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                        if (deviceInfoExt == null) {
                            logger.info("设备" + deviceCode + " 未配置模型信息，无法执行实时参数监控！");
                            continue;
                        }
                        equipModel.getCurrentRecipeName();
                        if (!equipModel.ppExecName.equals("--") && !equipModel.ppExecName.equals(deviceInfoExt.getRecipeName())) {
                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "已选程序与领料程序不一致,设备被锁定！请联系ME处理！领料程序:" + deviceInfoExt.getRecipeName() + " 已选程序:" + equipModel.ppExecName);
                            equipModel.sendMessage2Eqp("The current recipe <" + equipModel.ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");
                            equipModel.stopEquip();
                            return;
                        }
                        String busniessMod = deviceInfoExt.getBusinessMod();
                        if ("Engineer".equals(busniessMod)) {
                           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "工程模式，取消实时Check卡控！");
                            continue;
                        }
                        if (!"Y".equals(deviceInfoExt.getRecipeAutoCheckFlag())) {
                            logger.info("设备" + deviceCode + "未设置参数实时监控！");
                            continue;
                        }
                        if (!equipModel.getEquipState().isCommOn()) {
                            logger.info("设备" + deviceCode + "设备不在通讯状态！");
                            continue;
                        }
//                        if (!equipModel.equipStatus.equalsIgnoreCase("run")) {
//                            logger.info("设备" + deviceCode + "设备不在运行状态！");
//                            continue;
//                        }
                        // MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
                        List<RecipeTemplate> recipeTemplates = new ArrayList<>();//实时参数受到管控信息

                        recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipePara");
                        if (recipeTemplates == null || recipeTemplates.isEmpty()) {
                           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "该设备没有实时参数受到管控！");
                            continue;
                        }

                        List<DeviceRealtimePara> deviceRealtimeParas = monitorService.getDeviceRealtimeParaByDeviceCode(deviceCode, null);
                        Map resultMap = equipModel.getEquipMonitorPara();
                        logger.info("MonitorTask获取到设备" + deviceCode + "的管控参数，内容 :" + JsonMapper.toJsonString(resultMap));
                        if (resultMap != null && !resultMap.isEmpty()) {

                            deviceRealtimeParas = putSV2DeviceRealtimeParas(recipeTemplates, resultMap, deviceRealtimeParas, deviceCode);

                            //TODO发送机台所有的参数给服务端     
                            Map paraMap = new HashMap();
                            paraMap.put("msgName", "TransferArDeviceRealtimePara");
                            paraMap.put("deviceRealtimePara", JsonMapper.toJsonString(deviceRealtimeParas));
                            sendMessage2Server(paraMap);
                           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "成功发送定时检测该设备的sv参数实时信息到服务端");
                            //保存实时参数信息
                            // monitorService.saveDeviceRealtimePara(deviceRealtimeParas);
                           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取到该设备的SV参数实时信息！");
                            Map mqMap = new HashMap();
                            mqMap.put("msgName", "eqpt.MonitorCheckWI");
                            mqMap.put("deviceCode", deviceCode);
                            mqMap.put("lotId", equipModel.lotId);
                            mqMap.put("eventDesc", "获取到设备:" + deviceCode + " 的实时参数管控信息");
                            sendMessage2Server(mqMap);
                        } else {
                            // SECS LOG 打印显示
                            Map mqMap = new HashMap();
                            mqMap.put("msgName", "eqpt.MonitorCheckWI");
                            mqMap.put("deviceCode", deviceCode);
                            mqMap.put("lotId", equipModel.lotId);
                            mqMap.put("eventDesc", "获取设备:" + deviceCode + " 的管控信息失败。");
                            sendMessage2Server(mqMap);
                            logger.debug("获取设备:" + deviceCode + " 的管控信息失败。");
                           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取该设备的SV参数实时信息，获取失败！");
                        }
                        sqlSession.commit();
                    } catch (Exception e) {
                        sqlSession.rollback();
                        logger.error("Exception:", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            if(temp ==null){
                MDC.remove(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
            }else{
                MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, temp);
            }
            sqlSession.close();
        }
    }
}
