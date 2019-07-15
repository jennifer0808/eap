package cn.tzauto.octopus.secsLayer.equipImpl.asm.mold;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.asm.ASMIdeal3GRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.util.*;


/**
 */
@SuppressWarnings("serial")
public class ASMIdeal3GHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(ASMIdeal3GHost.class);

    public ASMIdeal3GHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        RCMD_PPSELECT = "PP_SELECT";
    }

    @Override
    public Object clone() {
        ASMIdeal3GHost newEquip = new ASMIdeal3GHost(deviceId,
                this.iPAddress,
                this.tCPPort, this.connectMode,
                this.deviceType, this.deviceCode);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.clear();
        return newEquip;
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    ASMIdeal3GHost.sleep(200);
                }
                if (this.getCommState() != ASMIdeal3GHost.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();//
                    //获取lot号
                    super.updateLotId();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    try {
                        processS6F11EquipStatus(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    ppExecName = (String) ((MsgSection) msg.get("PPExecName")).getData();
                    Map map = new HashMap();
                    map.put("PPExecName", ppExecName);
                    changeEquipPanel(map);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    @Override
    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = System.currentTimeMillis();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                //回复s6f11消息
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f30in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
//    @Override
//    @SuppressWarnings("unchecked")
//    public Map sendS1F3Check() {
//        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s1f3out.setTransactionId(transactionId);
//        long[] equipStatuss = new long[1];
//        long[] pPExecNames = new long[1];
//        long[] controlStates = new long[1];
//        DataMsgMap data = null;
//        try {
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            RecipeService recipeService = new RecipeService(sqlSession);
//            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
//            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
//            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
//            sqlSession.close();
//            s1f3out.put("EquipStatus", equipStatuss);
//            s1f3out.put("PPExecName", pPExecNames);
//            s1f3out.put("ControlState", controlStates);
//            logger.info("Ready to send Message S1F3==============>" + JSONArray.toJSON(s1f3out));
//            data = activeWrapper.sendAwaitMessage(s1f3out);
//            logger.info("Received Message S1F3===================>" + JSONArray.toJSON(data));
//        } catch (Exception e) {
//            logger.error("Wait for get meessage directly error：" + e);
//        }
//        if (data == null || data.isEmpty()) {
//            int i = 0;
//            logger.info("Can not get value directly,will try to get value from message queue");
//            while (i < 5) {
//                DataMsgMap DataMsgMap = this.waitMsgValueMap.get(transactionId);
//                logger.info("try===>" + 1);
//                if (DataMsgMap != null) {
//                    data = DataMsgMap;
//                    logger.info("Had get value from message queue ===>" + JSONArray.toJSON(data));
//                    break;
//                } else {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException ex) {
//                        logger.error("Exception:", ex);
//                    }
//                }
//                i++;
//            }
//            this.waitMsgValueMap.remove(transactionId);
//            if (i >= 5) {
//               UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取设备状态信息失败，请检查设备通讯状态！");
//                logger.error("获取设备:" + deviceCode + "状态信息失败.");
//                return null;
//            }
//        }
//        ArrayList<MsgSection> list = (ArrayList) ((MsgSection) data.get("RESULT")).getData();
//        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
//        equipStatus = ACKDescription.descriptionStatus((long) listtmp.get(0), deviceType);
//        ppExecName = (String) listtmp.get(1);
//        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
//
//        Map panelMap = new HashMap();
//        panelMap.put("EquipStatus", equipStatus);
//        panelMap.put("PPExecName", ppExecName);
//        panelMap.put("ControlState", controlState);
//        changeEquipPanel(panelMap);
//        return panelMap;
//    }
    public List sendS1F3PressCheckout() {
        DataMsgMap data = null;
        List<Long> list = new ArrayList();
        list.add(104L);
        list.add(204L);
        list.add(304L);
        list.add(404L);
        try {
            data = activeWrapper.sendS1F3out(list, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("SV") == null) {
            return null;
        }
        ArrayList listtmp = (ArrayList) data.get("SV");
        return listtmp;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @Override
    public void processS6F11in(DataMsgMap data) {
        //回复s6f11消息
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");

            if (ceid == 5 || ceid == 6 || ceid == 1) {
                processS6F11EquipStatus(data);
            }
            //开机校验看日志找ceid
//            if (ceid == 32 || ceid == 52 || ceid == 72 || ceid == 92 ) {
            if (ceid == 2 ) {
                processS6F11EquipStatusChange(data);
            }
            if (ceid == 8) {
                findDeviceRecipe();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }


    protected void processS6F11EquipStatus(DataMsgMap data) {
        //回复s6f11消息
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 5) {
                super.setControlState(GlobalConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 6) {
                super.setControlState(GlobalConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) { //待验证
                super.setControlState(GlobalConstant.CONTROL_OFFLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        updateCommStateInExt();
        UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "收到事件报告CEID：" + ceid);

        //更新页面显示内容
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "CEID");
        sqlSession.close();
        if (recipeTemplates != null && recipeTemplates.size() > 0) {
            for (int j = 0; j < recipeTemplates.size(); j++) {
                long ceidtmp = Long.parseLong(recipeTemplates.get(j).getDeviceVariableId());
                if (ceid == ceidtmp) {
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "CEID:" + ceid + " 描述：" + recipeTemplates.get(j).getParaDesc());
                    break;
                }
            }
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            boolean dataReady = false;
            //判断当前执行程序是否是清模程序 Y代表清模程序
            boolean isCleanRecipe = false;
            List<Recipe> cleanRecipes = recipeService.searchRecipeByRcpType(ppExecName, "Y");
            if (cleanRecipes != null && cleanRecipes.size() > 1) {
                isCleanRecipe = true;
            }
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
                dataReady = true;
            }

            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();

            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
                getUsingPress();
                //开机check Press使用情况
                checkPressUseState(deviceService, sqlSession);
//                if (!checkPressUseState(deviceService, sqlSession)) {
//                    return;
//                }
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                }
                //查询trackin时的recipe和GoldRecipe
                Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));

                //查询客户端数据库是否存在GoldRecipe
                if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                    hasGoldRecipe = false;
                }

                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (!this.checkLockFlagFromServerByWS(deviceCode)) {
                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查

                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                        if (!checkResult) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if ("Unique".equals(downloadRcpVersionType)) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                            }

                        }
                    } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ppbody = (byte[]) msgdata.get("PPBODY");
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = ASMIdeal3GRecipeUtil.transRcpParaFromDB(recipePath, deviceType);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    // </editor-fold>


    //获取当前使用的Press
    public void getUsingPress() {
        List pressResults = sendS1F3PressCheckout();
        super.pressUseMap.clear();
        String pressStatusAll = "";
        for (int i = 0; i < pressResults.size(); i++) {
            String pressStatus = String.valueOf(pressResults.get(i));
            if ("1".equals(pressStatus) || "2".equals(pressStatus) || "3".equals(pressStatus)) { // || "2".equals(pressStatus)
                pressStatusAll = pressStatusAll + (i + 1) + ",";
                super.pressUseMap.put(i + 1, true);
            }
        }
        if (pressStatusAll.length() > 0) {
            pressStatusAll = pressStatusAll.substring(0, pressStatusAll.length() - 1);
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备正在使用的Press为[" + pressStatusAll + "]");
    }

    @Override
    public void sendUphData2Server() throws IOException, BrokenProtocolException, T6TimeOutException, T3TimeOutException, InterruptedException, StateException, IntegrityException, InvalidDataException {
        String output = this.getOutputData() == null ? "" : this.getOutputData();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List svidlist = recipeService.searchShotSVByDeviceType(deviceType);
        sqlSession.close();

        Map shotCountMap = null;
        if(svidlist != null && svidlist.size() != 0){
            List<Long> svids = new ArrayList<>();
            for(Object svid:svidlist){
                svids.add(Long.parseLong(svid.toString()));
            }
            shotCountMap = activeWrapper.sendS1F3out(svids, svFormat);
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "UphDataTransfer");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("equipStatus", equipStatus);
        mqMap.put("preEquipStatus", preEquipStatus);
        mqMap.put("currentRecipe", ppExecName);
        mqMap.put("lotId", lotId);
        mqMap.put("shotCount", JsonMapper.toJsonString(shotCountMap));
        mqMap.put("output", output);
        mqMap.put("unit", "");
        mqMap.put("currentTime", GlobalConstants.dateFormat.format(new Date()));
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送设备UPH参数至服务端");
        logger.info("设备 " + deviceCode + " UPH参数为:" + mqMap);
//       UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "UPH参数为:" + mqMap);
    }


    private void initRptPara() {
    }


}
