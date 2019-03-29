/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.ws;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rain
 */
public class AxisUtility {

    private static final Logger logger = Logger.getLogger(AxisUtility.class);

    public static boolean checkBusinessMode(String eqptId) {
        DeviceInfoExt deviceInfoExt = getEqptStatus("SysAuto", eqptId);
        return "Y".equals(deviceInfoExt.getBusinessMod());
    }

    public static String getReplyMessage() {

        try {
//            String endpoint = "http://172.17.173.11/autoServer/services/idGenService";
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/idGenService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            String sendMessage = "22";
            call.setOperationName("genId");
            String replyMeaage = String.valueOf(call.invoke(new Object[]{sendMessage}));
            return replyMeaage;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
    }

    //获取开机状态是否Lock
    public static String getLockFlag(String userID, String eqptId) {
        DeviceInfoExt deviceInfoExt = getEqptStatus(userID, eqptId);
        if ("Y".equals(deviceInfoExt.getLockFlag())) {
            UiLogUtil.appendLog2SeverTab(deviceInfoExt.getDeviceRowid(), "由于[" + deviceInfoExt.getRemarks() + "],设备将被锁机...");
        }
        return deviceInfoExt.getLockFlag();
    }

    //获取开机状态是否Lock
    public static Map getLockFlagAndRemarks(String userId, String eqptId) {
        DeviceInfoExt deviceInfoExt = getEqptStatus(userId, eqptId);
        Map resultMap = new HashMap();
        resultMap.put("lockFlag", deviceInfoExt.getLockFlag());
        if ("Y".equals(deviceInfoExt.getLockFlag())) {
            resultMap.put("remarks", deviceInfoExt.getRemarks());
        } else {
            resultMap.put("remarks", "");
        }
        return resultMap;
    }

    //获取机台状态
    public static DeviceInfoExt getEqptStatus(String userId, String eqptId) {
        DeviceInfoExt deviceInfoExt = new DeviceInfoExt();
        try {
//            String endpoint = "http://172.17.173.11/autoServer/services/eqptService";
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("findEqptStatus");
            String jsonResult = String.valueOf(call.invoke(new Object[]{userId, eqptId}));
            deviceInfoExt = (DeviceInfoExt) JsonMapper.fromJsonString(jsonResult, DeviceInfoExt.class);
            logger.debug("call web service url:[" + GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService/findEqptStatus]");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return deviceInfoExt;
    }

    //调webservice锁机，并更改服务端标志位
    public static boolean holdEqptByServer(String userID, String eqptId, String type) {
        boolean pass = false;
        String result = "";
        try {
//            String endpoint = "http://172.17.173.11/autoServer/services/eqptService";
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("lockEqptByType");
            result = String.valueOf(call.invoke(new Object[]{userID, eqptId, type}));
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if ("pass".equalsIgnoreCase(result)) {
            pass = true;
        }
        return pass;
    }

    //调webservice解锁，并更改服务端标志位
    public static boolean releaseEqptByServer(String userID, String eqptId, String type) {
        String result = "";
        boolean pass = false;
        try {
//            String endpoint = "http://172.17.173.11/autoServer/services/eqptService";
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("releaseEqptByType");
            result = String.valueOf(call.invoke(new Object[]{userID, eqptId, type}));
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if ("pass".equalsIgnoreCase(result)) {
            pass = true;
        }
        return pass;
    }

    public static void sendCommonMailByWS(List<String> toList, String subject, Map message) {
        try {
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/mailService";
//            String endpoint = "http://172.17.255.56:7011/autoServer/services/mailService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("sendCommonMail");
            call.invoke(new Object[]{JsonMapper.toJsonString(toList), subject, JsonMapper.toJsonString(message)});
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public static String sendFtlMail(List<String> toList, String subject, String templatePath, Map msgMap) {
        String result = "N";
        try {
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/mailService";//http://172.17.173.11/autoServer/services
//            String endpoint = "http://172.17.200.246:8080/services/mailService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("sendFtlMail");
            result = call.invoke(new Object[]{JsonMapper.toJsonString(toList), subject, templatePath, JsonMapper.toJsonString(msgMap)}).toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return result;
    }

    public static Map init(String clientCode) {
        //Map resultMap = new HashMap();
        Map<String, String> resultMap = new HashMap();
        try {
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/initService";//172.17.200.191:8080/autoServer/services/initService?method=init&clientCode=A1-MD-T6
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("init");
            String jsonResult = String.valueOf(call.invoke(new Object[]{clientCode}));
            resultMap = (Map<String, String>) JsonMapper.fromJsonString(jsonResult, HashMap.class);//json-->类
        } catch (Exception ex) {
            logger.error("Exception:", ex);
            logger.debug("获取服务端数据失败，使用本地数据进行启动");
            UiLogUtil.appendLog2SeverTab(null, "获取服务端数据失败，使用本地数据进行启动");
            resultMap = null;
        }
        return resultMap;
    }

    public static Map initRecipe(String recipeRowId) {
        Map paraMap = new HashMap();
        try {
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/recipeService";
//            String endpoint = "http://172.17.200.70:8080/services/recipeService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("findRecipebyId");
            String jsonResult = String.valueOf(call.invoke(new Object[]{recipeRowId}));
            paraMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);//json-->类
        } catch (Exception e) {
            logger.error("Exception:", e);
            logger.debug("获取服务端数据失败，使用本地数据进行启动");
            UiLogUtil.appendLog2SeverTab(null, "获取服务端数据失败，使用本地数据进行启动");
            paraMap = null;
        }
        return paraMap;
    }

    public static List<RecipePara> initRecipePara(String recipeRowId) {
        Map resultMap = new HashMap();
        List<RecipePara> recipeParaList = new ArrayList<>();
        resultMap = initRecipe(recipeRowId);
        if (resultMap.get("recipePara") != null) {
            String ArRecipeParaString = (String) resultMap.get("recipePara");
            recipeParaList = (List<RecipePara>) JsonMapper.String2List(ArRecipeParaString, RecipePara.class);
        }
        return recipeParaList;
    }

    public static String releaseLotByMES(String userId, String eqptId, String lotId, String releaseCode, String reason) {
        String result = "";
        String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/lotService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("releaseLotByMES");
            result = String.valueOf(call.invoke(new Object[]{userId, eqptId, lotId, releaseCode, reason}));
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return result;
    }

    public static String holdLotByMES(String userId, String eqptId, String lotId, String holdCode, String reason) {
        String result = "";
        String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/lotService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("holdLotByMES");
            result = String.valueOf(call.invoke(new Object[]{userId, eqptId, lotId, holdCode, reason}));
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return result;
    }

    public static Map pressCheck(String deviceCode, String pressUse) {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.200.16:8080/services/eqptService";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("findPressStatus");
            String jsonResult = String.valueOf(call.invoke(new Object[]{deviceCode, pressUse}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return resultMap;
    }

    public static String getPressCheckFlag(String deviceCode, String pressUse) {
        String flag = "";
        Map resultMap = pressCheck(deviceCode, pressUse);
        if (resultMap.get("flag") != null) {
            flag = String.valueOf(resultMap.get("flag"));
        }
        return flag;
    }

    public static String findMesAoLotService(String deviceCode, String stripId, String funcType) {
        String result = "";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("findMesAoLot");
            result = String.valueOf(call.invoke(new Object[]{deviceCode, stripId, funcType}));
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return result;
    }

    /**
     * 获取设备领用的刀片编号
     *
     * @param deviceCode
     * @return
     */
    public static Map getBladeIdFromServer(String deviceCode) {
        Map resultMap = new HashMap();
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/edcService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("getBladeIdInUseByDeviceCode");
            String jsonResult = String.valueOf(call.invoke(new Object[]{"SysAuto", deviceCode}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap;
    }

    public static Map getBladeTypeByGroupFromServer(String bladeGroupId) {
        Map resultMap = new HashMap();
        if (!GlobalConstants.isLocalMode) {
            String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/edcService";
            try {
                Service service = new Service();
                Call call = (Call) service.createCall();
                call.setTargetEndpointAddress(new java.net.URL(endPoint));
                call.setOperationName("contrastBlade");
                String jsonResult = String.valueOf(call.invoke(new Object[]{bladeGroupId}));
                resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
            } catch (Exception e) {
                logger.error("Exception", e);
                return resultMap;
            }
        }
        return resultMap;
    }


    /**
     * @param stripid
     * @param deviceCode
     * @return
     */
    public static String downloadStripMap(String stripid, String deviceCode) {

//        String endPoint = "http://192.168.103.143:8080/stripMap/services/stripMapping";
        //  "http://192.168.99.146:8080/stripMap/services/stripMapping";
        String endPoint = GlobalConstants.getProperty("SERVER_URL") + "/services/stripMapping";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("downLoad2DBin");
            String jsonResult = String.valueOf(call.invoke(new Object[]{stripid, deviceCode}));
//            Object[] results = (Object[]) JsonMapper.fromJsonString(jsonResult, Object[].class);
//            if (results != null && results.length > 0) {
//                return results[0].toString();
//            } else {
            logger.info("downloadStripMapResult：" + jsonResult);
            return jsonResult;
//            }
        } catch (Exception e) {
            logger.error("Exception", e);
            return "Error";
        }
    }

    /**
     * @param xmlStr
     * @param deviceCode
     * @return
     */
    public static String uploadStripMap(String xmlStr, String deviceCode) {

//        String endPoint = "http://192.168.103.143:8080/stripMap/services/stripMapping";
        //sever  "http://192.168.99.146:8080/stripMap/services/stripMapping";
        String endPoint = GlobalConstants.getProperty("SERVER_URL") + "/services/stripMapping";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("upload2DBin");
            String jsonResult = String.valueOf(call.invoke(new Object[]{xmlStr, deviceCode}));
            logger.info("uploadStripMapResult：" + jsonResult);
            return jsonResult;
        } catch (Exception e) {
            logger.error("Exception", e);
            return "Error";
        }

    }

    /**
     * 88D清洗机调用接口
     *
     * @param deviceCode
     * @param stripId
     * @param funcName
     * @return
     */
    public static String plasma88DService(String deviceCode, String stripId, String funcName) {
        try {
            Service axisService = new Service();
            Call call = (Call) axisService.createCall();
            call.setTargetEndpointAddress("http://192.168.99.130/cim/services/LotMESService?wsdl");
            call.setOperationName(funcName);
            String result = (String) call.invoke(new Object[]{deviceCode, stripId});
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error";
        }
    }
    //获取机台状态

    public static void setInlineLock(String eqpId, String lockType, String lockReason) {

        try {
//            String endpoint = "http://172.17.173.11/autoServer/services/eqptService";
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("lockMasterDevcie");
            String jsonResult = String.valueOf(call.invoke(new Object[]{eqpId, lockType, lockReason}));
            logger.debug("call web service url:[" + GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService/lockMasterDevcie]" + jsonResult);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    public static boolean isEngineerMode(String eqptId) {
        DeviceInfoExt deviceInfoExt = new DeviceInfoExt();
        try {
//            String endpoint = "http://172.17.173.11/autoServer/services/eqptService";
            String endpoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/eqptService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("findEqptStatus");
            String jsonResult = String.valueOf(call.invoke(new Object[]{eqptId, eqptId}));
            logger.debug("isEngineerMode JSONRESULT：" + jsonResult);
            deviceInfoExt = (DeviceInfoExt) JsonMapper.fromJsonString(jsonResult, DeviceInfoExt.class);
            String equipStatus = deviceInfoExt.getDeviceStatus();
            logger.info(eqptId + "=================向服务端发送findEqptStatus，请求机台MES状态！");
            logger.info(eqptId + "当前MES系统equipStatus====" + equipStatus);
            if (equipStatus.equalsIgnoreCase("run") || equipStatus.equalsIgnoreCase("idle")) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return true;
    }

    /**
     * TOWAY1R设备2D码追溯
     *
     * @param deviceCode,stripID(L&R),pressNo,recipeName,formingNo
     * @return
     */
    public static Map get2DCode(String deviceCode, String pressNo, String recipeName, String formingNo, String leftStripID, String rightStripID) {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.200.251/autoServer/services/fbiLotService";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/fbiLotService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("insertMesMapping2DBIn");
            String jsonResult = String.valueOf(call.invoke(new Object[]{deviceCode, pressNo, recipeName, formingNo, leftStripID, rightStripID}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap;
    }

    /**
     * SPI设备SPC数据上传
     *
     * @param deviceCode,stripID(L&R),pressNo,recipeName,formingNo
     * @return
     */
    public static Map getSPCdata(String stripId, String pSampleValues, String pUserName, String pUserNo, String snt, String deviceCode) {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.173.11/autoServer/services/spcService";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/spcService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("getSPCdata");
            String jsonResult = String.valueOf(call.invoke(new Object[]{stripId, pSampleValues, pUserName, pUserNo, snt, deviceCode}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap;
    }

    /**
     * 所有设备recipe上传之前检查是否存在GOLD版本
     *
     * @param deviceCode，recipeName
     * @return
     */
    public static boolean acceptRecipeGoldService(String deviceCode, String recipeName) {
        String result = "";
        Boolean Flag = false;
//        String endPoint = "http://172.17.200.231:8080/services/eqptService";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/AcceptRecipeGoldService";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("acceptRecipeGold");
            result = String.valueOf(call.invoke(new Object[]{deviceCode, recipeName}));
            logger.info("get acceptRecipeGoldService result :" + result);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (result.equalsIgnoreCase("Y")) {
            Flag = true;
        }
        if (result.equalsIgnoreCase("N")) {
            Flag = false;
        }
        return Flag;
    }

    /**
     * 获取WaferMapping设置参数
     *
     * @param deviceCode,stripID(L&R),pressNo,recipeName,formingNo
     * @return
     */
    public static Map getWaferMappingInfo(String waferId, String deviceCode) {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.162.209:8080/services/GetWaferMappingService?wsdl";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/GetWaferMappingService?wsdl";
        System.out.println(endPoint + "===========================");
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("getMappingInfo");
            String jsonResult = String.valueOf(call.invoke(new Object[]{deviceCode, waferId}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap;
    }

    public static Map downloadWaferMap(String deviceCode, String waferId) throws Exception {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.162.209:8080/services/GetWaferMappingService?wsdl";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/wafermappingservice?wsdl";
        System.out.println(endPoint + "===========================");
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("downloadWaferMap");
            String jsonResult = String.valueOf(call.invoke(new Object[]{deviceCode, waferId}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
            throw e;
        }
        return resultMap;
    }

    /**
     * 获取WaferMapping BinList
     *
     * @param waferId
     * @return
     */
    public static Map getWaferMappingFile(String waferId, String deviceCode) {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.200.205:8080/services/GetWaferMappingService?wsdl";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/GetWaferMappingService?wsdl";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("getMappingFile");
            String jsonResult = String.valueOf(call.invoke(new Object[]{deviceCode, waferId}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap;
    }

    public static Map sendWaferMappingInfo(String waferId, String row, String col, String binList, String deviceCode) {
        Map resultMap = new HashMap();
//        String endPoint = "http://172.17.200.205:8080/services/SaveWaferMappingService?wsdl";
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/SaveWaferMappingService?wsdl";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("saveMappingInfo");
            if (binList.indexOf("#") > 0) {
                binList = binList.replaceAll("#", "353535");
            }
            String jsonResult = String.valueOf(call.invoke(new Object[]{deviceCode, waferId, row, col, binList}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap;
    }

    /**
     * wafer防混
     *
     * @param waferId
     * @return
     */
    public static String checkWaferMix(String waferId, String deviceCode) {
        Map resultMap = new HashMap();
        String endPoint = GlobalConstants.getProperty("ServerRecipeWSUrl") + "/edcService?wsdl";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName("confirmWaferIdInLot");
            String jsonResult = String.valueOf(call.invoke(new Object[]{waferId, deviceCode}));
            resultMap = (Map) JsonMapper.fromJsonString(jsonResult, HashMap.class);
            logger.info("flag : " + resultMap.get("flag") + "" + " message:" + resultMap.get("msg"));
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return resultMap.get("flag").toString();
    }


}
