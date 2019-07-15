package cn.tzauto.octopus.secsLayer.modules.event;

import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.generalDriver.wrapper.ActiveWrapper;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.WSUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.Judge;
import cn.tzauto.octopus.secsLayer.domain.Process;
import cn.tzauto.octopus.secsLayer.domain.ProcessFunction;
import cn.tzauto.octopus.secsLayer.domain.ProcessFunctionNotSupportException;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.modules.Dealer;
import cn.tzauto.octopus.secsLayer.modules.JudgeResult;
import cn.tzauto.octopus.secsLayer.modules.edc.EdcDealer;
import cn.tzauto.octopus.secsLayer.modules.remotecontrol.RcmdDealer;
import cn.tzauto.octopus.secsLayer.util.XmlUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by luosy
 */
public class EventDealer {
    static Logger logger = Logger.getLogger(EventDealer.class);

    public static JudgeResult deal(DataMsgMap dataMsgMap, String deviceCode, Process process, ActiveWrapper activeWrapper) throws IOException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, InterruptedException, ProcessFunctionNotSupportException, StateException, IntegrityException, InvalidDataException {
        List<ProcessFunction> processFunctions = process.getFunction();
        boolean isJudgePass = true;
        for (ProcessFunction processFunction : processFunctions) {
            String functionCode = processFunction.getFunctionCode();
            switch (functionCode) {
                case ProcessFunction.EDC_SV:
                    if (EdcDealer.deal(processFunction, deviceCode).isJudgePass()) {
                        isJudgePass = false;
                    }
                    break;
                case ProcessFunction.EDC_EC:
                    if (EdcDealer.deal(processFunction, deviceCode).isJudgePass()) {
                        isJudgePass = false;
                    }
                    break;
                case ProcessFunction.STRIP_UPLOAD:
                    stripMapUpload(dataMsgMap, deviceCode);
                    break;
                case ProcessFunction.RCP_NAME_CHECK:
                    checkRecipeName(dataMsgMap, deviceCode, processFunction);
                    break;
                case ProcessFunction.RCP_PARA_CHECK:
                    checkRecipePara(dataMsgMap, deviceCode, processFunction);
                    break;
                case ProcessFunction.EQP_STATE_CHECK:
                    checkEquipState(dataMsgMap, deviceCode, processFunction);
                    break;
                case ProcessFunction.RCMD_STOP:
                    RcmdDealer.deal(processFunction, deviceCode);
                    break;
                case ProcessFunction.RCMD_TRML_MSG:
                    RcmdDealer.deal(processFunction, deviceCode);

            }
        }
        JudgeResult judgeResult = new JudgeResult();
        judgeResult.setJudgePass(isJudgePass);
        return judgeResult;
    }

    public static Map deal(ProcessFunction processFunction, ActiveWrapper activeWrapper) {
        return null;
    }

    private static JudgeResult stripMapUpload(DataMsgMap data, String deviceCode) {
        JudgeResult judgeResult = new JudgeResult();
        try {
            ArrayList reportData = (ArrayList) data.get("REPORT");
            //获取xml字符串
//            String stripMapData = (String) ((MsgSection) data.get("MapData")).getData();
            String stripMapData = (String) ((ArrayList) reportData.get(1)).get(0);
            String stripId = XmlUtil.getStripIdFromXml(stripMapData);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "请求上传Strip Map！StripID:[" + stripId + "]");
            //通过Web Service上传mapping
            byte ack = WSUtility.binSet(stripMapData, deviceCode).getBytes()[0];
//            byte ack = AxisUtility.uploadStripMap(stripMapData, deviceCode).getBytes()[0];
            if (ack == 0) {//上传成功
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上传Strip Map成功！StripID:[" + stripId + "]");
                GlobalConstants.stage.equipHosts.get(deviceCode).getActiveWrapper().sendS6F12out((byte) 0, data.getTransactionId());
                judgeResult.setJudgePass(true);
                judgeResult.setResultDescription(deviceCode + "上传Strip Map成功！StripID:[" + stripId + "]");

            } else {//上传失败
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上传Strip Map失败！StripID:[" + stripId + "]");
                GlobalConstants.stage.equipHosts.get(deviceCode).getActiveWrapper().sendS6F12out((byte) 1, data.getTransactionId());
                judgeResult.setJudgePass(false);
                judgeResult.setResultDescription(deviceCode + "上传Strip Map失败！StripID:[" + stripId + "]");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            judgeResult.setJudgePass(false);
        }
        return judgeResult;
    }


    private static boolean checkRecipeName(DataMsgMap dataMsgMap, String deviceCode, ProcessFunction processFunction) {
        //todo processFunction中应该定义判断标准的来源，目前先从数据库的ext表中获取
        String currentRecipeName = String.valueOf(dataMsgMap.get("PPExecName"));
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (currentRecipeName.equals(deviceInfoExt.getRecipeName())) {
            return true;
        } else {
            return false;
        }
    }

    private static JudgeResult checkRecipePara(DataMsgMap dataMsgMap, String deviceCode, ProcessFunction processFunction) {
        //todo processFunction中应该定义判断标准的来源，目前先从数据库的ext表中获取
        String currentRecipeName = String.valueOf(dataMsgMap.get("PPExecName"));
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        RecipeService recipeService = new RecipeService(sqlSession);
        Recipe recipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
        sqlSession.close();
        return startCheckRecipePara(deviceCode, recipe, processFunction);

    }

    private static JudgeResult startCheckRecipePara(String deviceCode, Recipe checkRecipe, ProcessFunction processFunction) {
        String type = "";
        if (checkRecipe.getVersionType().equals("Unique")) {
            type = "abs";
        }
        logger.info("START CHECK: BEGIN" + new Date());
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        logger.info("START CHECK: ready to upload recipe:" + new Date());
        List<RecipePara> equipRecipeParas = null;
        try {
            equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(deviceCode, checkRecipe.getRecipeName()).get("recipeParaList");
        } catch (UploadRecipeErrorException e) {
            e.printStackTrace();
            return null;
        }
        logger.info("START CHECK: transfer recipe over :" + new Date());
        logger.info("START CHECK: ready to check recipe para:" + new Date());
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        logger.info("START CHECK: check recipe para over :" + new Date());
        try {
            String eventDesc = "";
            String checkRecultDesc = "";
            String eventDescEng = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为[" + recipePara.getParaCode() + "],参数名:[" + recipePara.getParaName() + "]其异常设定值为[" + recipePara.getSetValue() + "],默认值为[" + recipePara.getDefValue() + "]"
                            + "其最小设定值为[" + recipePara.getMinValue() + "],其最大设定值为[" + recipePara.getMaxValue() + "]";
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                    checkRecultDesc = checkRecultDesc + eventDesc;
                    String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "/r/n";
                    eventDescEng = eventDescEng + eventDescEngtmp;
                }
                processFunction.getJudge().getFailedFunction().getJudge().setJudgeStandardStr("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng);
                return Dealer.deal(processFunction.getJudge().getFailedFunction(), deviceCode);
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
                checkRecultDesc = eventDesc;
                return Dealer.deal(processFunction.getJudge().getSuccessFunction(), deviceCode);
            }

        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
        return null;
    }

    private static JudgeResult checkEquipState(DataMsgMap dataMsgMap, String deviceCode, ProcessFunction processFunction) {
        //todo processFunction中应该定义判断标准的来源，目前先从数据库的ext表中获取
        String currentEquipState = String.valueOf(dataMsgMap.get("EquipState"));
        Judge judge = processFunction.getJudge();
        if (judge.getJudgeStandardStr().equals(currentEquipState)) {
            return Dealer.deal(judge.getSuccessFunction(), deviceCode);
        } else {
            return new JudgeResult();
        }

    }
}
