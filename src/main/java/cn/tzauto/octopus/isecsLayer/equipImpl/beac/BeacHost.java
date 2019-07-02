/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.beac;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.socket.RecipeMapping;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.plcdriver.Profinet.Melsec.MelsecMcNet;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;

/**
 * @author luosy
 */
public class BeacHost extends EquipModel {

    private static Logger logger = Logger.getLogger(BeacHost.class.getName());
    private MelsecMcNet melsec_net = null;
    private Map<String, String> equipStatusPointMap = new HashMap();
    private Map<String, String> equipAlarmPointMap = new HashMap();
    private Map<String, String> equipParaPointMap = new HashMap();
    private Map<String, String> monitorParaPointMap = new HashMap();

    public BeacHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public boolean isConnect() {
        if (commState == 1) {
            return true;
        }
        return false;
    }

    @Override
    public void initialize() {
        melsec_net = new MelsecMcNet(remoteIPAddress, remoteTCPPort);
        if (melsec_net.ConnectServer().IsSuccess) {
            logger.info("connet plc success");
            this.commState = 1;
            this.equipState.setCommOn(true);

        } else {
            logger.info("connet plc failed");
            return;
        }
        InputStream beacin = BeacHost.class.getResourceAsStream(GlobalConstants.getProperty("BEAC_POINT_CONFIG"));
        Properties beacHostProperties = new Properties();
        try {
            beacHostProperties.load(beacin);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(BeacHost.class.getName()).log(Level.SEVERE, null, ex);
        }
        String equipStatusPoint = beacHostProperties.getProperty("EQUIPSTATUS");
        String[] equipStatusPoints = equipStatusPoint.split(";");
        for (String equipStatusPointTemp : equipStatusPoints) {
            if (equipStatusPointTemp.contains(",")) {
                String[] equipStatusPointTemps = equipStatusPointTemp.split(",");
                equipStatusPointMap.put(equipStatusPointTemps[0], equipStatusPointTemps[1]);
            }
        }
//        int alarmCount = 10;
//        try {
//            alarmCount = Integer.parseInt(beacHostProperties.getProperty("EQUIPALARM_COUNT"));
//        } catch (Exception e) {
//            logger.info("EQUIPALARM_COUNT=" + beacHostProperties.getProperty("EQUIPALARM_COUNT") + "error. set EQUIPALARM_COUNT =10");
//        }
//        for (int i = 1; i < alarmCount; i++) {
//            String equipAlarmPoint = beacHostProperties.getProperty("EQUIPALARM" + i);
//            String[] equipAlarmPoints = equipAlarmPoint.split(";");
//            for (String equipAlarmPointTemp : equipAlarmPoints) {
//                if (equipAlarmPointTemp.contains(",")) {
//                    String[] equipAlarmPointTemps = equipAlarmPointTemp.split(",");
//                    equipAlarmPointMap.put(equipAlarmPointTemps[0], equipAlarmPointTemps[1]);
//                }
//            }
//        }
//equipParaPointMap
        equipParaPointMap = BeacRecipe.loadRecipePara(GlobalConstants.getProperty("EQUIP_PARA_POINT_PATH"));
        equipAlarmPointMap = BeacRecipe.loadRecipePara(GlobalConstants.getProperty("EQUIP_ALARM_POINT_PATH"));
//        String equipPara = beacHostProperties.getProperty("EQUIP_PARA_POINT");
//        String[] equipParaPoints = equipPara.split(";");
//        for (String equipParaPointTemp : equipParaPoints) {
//            if (equipParaPointTemp.contains(",")) {
//                String[] equipParaPointTemps = equipParaPointTemp.split(",");
//                equipParaPointMap.put(equipParaPointTemps[0], equipParaPointTemps[1]);
//            }
//        }

//        String monitorPara = beacHostProperties.getProperty("MONITOR_PARA");
//        String[] monitorParaPoints = monitorPara.split(";");
//        for (String monitorParaPointTemp : monitorParaPoints) {
//            if (monitorParaPointTemp.contains(",")) {
//                String[] monitorParaPointTemps = monitorParaPointTemp.split(",");
//                monitorParaPointMap.put(monitorParaPointTemps[0], monitorParaPointTemps[1] + "," + monitorParaPointTemps[2]);
//            }
//        }
        startAlarmMonitor();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (commState == 1) {
                    getEquipRealTimeState();
//                    sendStatus2Server(equipStatus);
                }
            }
        }).start();
    }

    @Override
    public String getCurrentRecipeName() {
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        ppExecName = deviceInfoExt.getRecipeName();
        Map map = new HashMap();
        map.put("PPExecName", deviceInfoExt.getRecipeName());
        changeEquipPanel(map);
        return ppExecName;

    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String pauseEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String stopEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String lockEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map uploadRecipe(String recipeName) {

        return new HashMap();

    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        //todo 参数通过webservice获取写入
        //添加配置 先通过配置文件读取
        Map beacRecipeMap = BeacRecipe.loadRecipeInfo();
        if (beacRecipeMap != null && !beacRecipeMap.isEmpty()) {
            String beacRecipeStr = String.valueOf(beacRecipeMap.get("RECIPENAME"));
            String[] beacRecipes = beacRecipeStr.split(",");
            logger.info("RcipeName:" + recipe.getRecipeName() + " RecipePara:" + beacRecipeMap.get(recipe.getRecipeName()));

            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
            sqlSession.close();
//            for (RecipeTemplate recipeTemplate : recipeTemplates) {

            for (int i = 0; i < beacRecipes.length; i++) {

//                for (Map.Entry<String, String> entry : equipParaPointMap.entrySet()) {
                for (RecipeTemplate recipeTemplate : recipeTemplates) {

//                    String key = entry.getKey();
//                    String value = entry.getValue();
                    String key = recipeTemplate.getDeviceVariableId();
                    String value = recipeTemplate.getParaName();
                    String min = String.valueOf(recipeTemplate.getMinValue());
                    String max = String.valueOf(recipeTemplate.getMaxValue());

                    if (beacRecipes[i].equals(value)) {

                        String nmb = String.valueOf(beacRecipeMap.get(recipe.getRecipeName())).split(",")[i];
                        if ("null".equals(nmb)) {
                            break;
                        }
                        logger.info("RcipeName:" + recipe.getRecipeName() + " RecipeParaName:" + value);
                        if (key.contains("R")) {
                            if (melsec_net.Write(key, (short) Short.valueOf(nmb)).IsSuccess) {
                                logger.info(key + " write " + nmb + " success");
                            } else {
                                logger.info(key + " write " + nmb + " failed");
                                return value + " write " + nmb + " failed";
                            }
                            continue;
                        }
                        if (key.contains("D")) {
                            if (melsec_net.Write(key, Integer.valueOf(nmb)).IsSuccess) {
                                logger.info(key + " write " + nmb + " success");
                            } else {
                                logger.info(key + " write " + nmb + " failed");
                                return value + " write " + nmb + " failed";
                            }
                            continue;
                        }
                        if (key.contains("M") || key.contains("Y") || key.contains("X") || key.contains("B")) {
                            boolean[] bs = new boolean[1];
                            if ("1".equals(nmb)) {
                                bs[0] = true;
                            } else {
                                bs[0] = false;
                            }
                            if (melsec_net.Write(key, bs).IsSuccess) {
                                logger.info(key + " write " + bs[0] + " success");
                            } else {
                                logger.info(key + " write " + bs[0] + " failed");
                                return value + " write " + nmb + " failed";
                            }
                        }
                    }
                }
            }
        } else {
            logger.info("Can not find any recipe info by recipe name " + recipe.getRecipeName());
            return "Can not find any recipe info by recipe name " + recipe.getRecipeName();
        }
        return "0";

    }

    @Override
    public String deleteRecipe(String recipeName) {
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {
        return "0";
    }

    @Override
    public Map getEquipMonitorPara() {
        Map resultMap = new HashMap();

        return resultMap;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        recipeNameList.add(ppExecName);
        eppd.put("eppd", recipeNameList);
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        preEquipStatus = equipStatus;
        Map<Boolean, String> equipStatusMapTemp = new HashMap<>();
        for (Map.Entry<String, String> entry : equipStatusPointMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            boolean[] equipstatusb = melsec_net.ReadBool(key, (short) 1).Content;
            equipStatusMapTemp.put(equipstatusb[0], value);
            if (equipstatusb[0]) {
                equipStatus = value;
            }
        }
        if (equipStatusMapTemp.get(true) != null) {
            equipStatus = equipStatusMapTemp.get(true);
        } else {
            equipStatus = "IDLE";
        }
        if (!equipStatus.equals(preEquipStatus)) {
            Map map = new HashMap();
            map.put("EquipStatus", equipStatus);
            changeEquipPanel(map);
            if (equipStatus.equalsIgnoreCase("RUN")) {
                startCheck();
            }
        }
        return equipStatus;

    }

    @Override
    public Object clone() {
        BeacHost newEquip = new BeacHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        return true;
    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        return new ArrayList<>();
    }

    private void startAlarmMonitor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    int rate = 1000;
                    try {
                        rate = Integer.parseInt(GlobalConstants.getProperty("ALARM_READ_RATE"));
                    } catch (NumberFormatException e) {
                        logger.error("ALARM_READ_RATE set error：" + GlobalConstants.getProperty("ALARM_READ_RATE") + " use default 1000ms");
                    }
                    try {
                        Thread.sleep(rate);
                    } catch (InterruptedException ex) {

                    }
                    getEquipStatus();
                    for (Map.Entry<String, String> entry : equipAlarmPointMap.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        boolean[] alarmstatusb = melsec_net.ReadBool(key, (short) 1).Content;
                        if (alarmstatusb != null && alarmstatusb[0]) {
                            logger.info("ALARM:" + value + " point=" + key);
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public String organizeRecipe(String partNo, String lotNo) {
        Map recipeNameMap = RecipeMapping.loadRecipeLotMapping(GlobalConstants.getProperty("BEAC_LASER_EXPOSURE_RECIPE_MAPPING_PATH"));
        String recipeName = "";
        if (recipeNameMap != null) {
            recipeName = String.valueOf(recipeNameMap.get(partNo));
            if (!recipeName.equals("null")) {
                if (recipeName.contains(",")) {
                    recipeName = recipeName.split(",")[0];
                }
            }
        } else {
            return "Can not find config file at " + GlobalConstants.getProperty("BEAC_LASER_EXPOSURE_RECIPE_MAPPING_PATH");
        }

        return recipeName;
    }

    @Override
    public boolean startCheck() {
        if (!specialCheck()) {
            return false;
        }
        boolean pass = true;
        String checkRecultDesc = "";
        String checkRecultDescEng = "";
        if ("1".equals(GlobalConstants.getProperty("START_CHECK_LOCKFLAG"))) {
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                checkRecultDesc = "检测到设备被Server要求锁机,设备将被锁!";
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
                pass = false;

            }
        }

        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.StartCheckWI");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("recipeName", ppExecName);
        mqMap.put("EquipStatus", equipStatus);
        mqMap.put("lotId", lotId);

        if ("1".equals(GlobalConstants.getProperty("START_CHECK_RECIPE_PARA"))) {

//            Map beacRecipeMap = BeacRecipe.loadRecipeInfo();
//            String beacRecipeStr = String.valueOf(beacRecipeMap.get("RECIPENAME"));
//            String[] beacRecipes = beacRecipeStr.split(",");
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
            sqlSession.close();
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
//            for (int i = 0; i < beacRecipes.length; i++) {
//            for (Map.Entry<String, String> entry : monitorParaPointMap.entrySet()) {
//                String key = entry.getKey();
//                String value = entry.getValue();
//                String min = value.split(",")[0];
//                String max = value.split(",")[1];
                String key = recipeTemplate.getDeviceVariableId();
                String value = recipeTemplate.getParaName();
                String min = String.valueOf(recipeTemplate.getMinValue());
                String max = String.valueOf(recipeTemplate.getMaxValue());
                String standard = "";
                if (min.equals(max)) {
                    standard = min;
                }
                if (key.contains("R")) {
                    short temp = melsec_net.ReadInt16(key).Content;
                    if (Short.valueOf(min) <= temp && temp <= Short.valueOf(max)) {
                        logger.info(equipParaPointMap.get(key) + " check  pass set value=" + temp);
                    } else {
                        logger.info(equipParaPointMap.get(key) + " check  failed set value=" + temp);
                    }
                    continue;
                }
                if (key.contains("D")) {
                    int temp = melsec_net.ReadInt32(key).Content;
                    if (Integer.valueOf(min) <= temp && temp <= Integer.valueOf(max)) {
                        logger.info(equipParaPointMap.get(key) + " check  pass set value=" + temp);
                    } else {
                        logger.info(equipParaPointMap.get(key) + " check  failed set value=" + temp);
                    }
                    continue;
                }
                if (key.contains("M") || key.contains("Y") || key.contains("X") || key.contains("B")) {
                    int temp = 0;
                    if (melsec_net.ReadBool(key).Content) {
                        temp = 1;
                    }
                    if (Integer.valueOf(min) <= temp && temp <= Integer.valueOf(max)) {
                        logger.info(equipParaPointMap.get(key) + " check  pass set value=" + temp);
                    } else {
                        logger.info(equipParaPointMap.get(key) + " check  failed set value=" + temp);
                    }
                }
            }
//            }
        }
        mqMap.put("eventDesc", checkRecultDesc);
        return pass;
    }

    @Override
    public boolean getPassport(int level) {
        if (!this.isConnect()) {
            logger.debug(deviceCode + "连接异常.强制回收");
            returnPassport();
            return true;
        }
        return true;
    }

    public boolean getPassport() {

        return getPassport(1);

    }

}
