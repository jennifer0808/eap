/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.esec.fc;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import cn.tzauto.octopus.secsLayer.util.WaferTransferUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author luosy
 */
public class SigmaPlusHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(SigmaPlusHost.class);
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    String FlatNotchLocation;
    private volatile boolean isInterrupted = false;

    //private Object synS2F41 = null;
    public SigmaPlusHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        RCMD_PPSELECT = "PP_SELECT";
        CPN_PPID = "PROGRAM";
        EquipStateChangeCeid = 49L;
        StripMapUpCeid = 403L;
    }


    @Override
    public Object clone() {
        SigmaPlusHost newEquip = new SigmaPlusHost(deviceId,
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
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!isInterrupted) {

            try {
                while (!this.isSdrReady()) {
                    SigmaPlusHost.sleep(200);
                }
                if (this.getCommState() != SigmaPlusHost.COMMUNICATING) {
                    sendS1F13out();
                    sendS1F1out();
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    initRptPara();

//                    sendS2F15outParameter();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s12f3in")) {
                    processS12F3in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s12f15in")) {
                    processS12F15in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11IN")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.info(getName() + "从阻塞中退出...");
                logger.info("this.isInterrupted()=" + this.isInterrupted() + " is interrupt=" + isInterrupted);
                logger.fatal("Caught Interruption", e);
            } catch (Exception e) {

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
            long transactionId = data.getTransactionId();
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
//                processS6F11inStripMapUpload(data);

            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else if (tagName.equalsIgnoreCase("s12f1in")) {
                processS12F1in(data);
            } else if (tagName.equalsIgnoreCase("s12f3in")) {
                processS12F3in(data);
            } else if (tagName.equalsIgnoreCase("s12f5in")) {
                processS12F5in(data);
            } else if (tagName.equalsIgnoreCase("s12f7in")) {
                processS12F7in(data);
            } else if (tagName.equalsIgnoreCase("s12f9in")) {
                processS12F9in(data);
            } else if (tagName.equalsIgnoreCase("s12f11in")) {
                processS12F11in(data);
            } else if (tagName.equalsIgnoreCase("s12f13in")) {
                processS12F13in(data);
            } else if (tagName.equalsIgnoreCase("s12f15in")) {
                processS12F15in(data);
            } else if (tagName.equalsIgnoreCase("s12f17in")) {
                processS12F17in(data);
            } else if (tagName.equalsIgnoreCase("s12f19in")) {
                processS12F19in(data);
            } else if (tagName.equalsIgnoreCase("s12f67in")) {
                processS12F67in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else {
                replyS6F12WithACK(data, (byte) 0);
                if (ceid == 2L) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendS2f41Cmd("START");
                        }

                    });
                    thread.start();
                    logger.info("Received event ceid = 2 need to send command START.");
                } else if (ceid == EquipStateChangeCeid) {
                    processS6F11EquipStatusChange(data);
                }
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public String initRptPara() {
        try {
//            sendS2f33outDelete(4905L);
//            sendS2F35outDelete(4905L, 4905L);
            sendS2F33clear();
            sendS2F35clear();
//            sendS2F37outAll();
            sendS2F37outCloseAll();
            List list = new ArrayList();
            list.add(10007L);
            list.add(10000L);

            logger.debug("initRptPara+++++++++++++++++++");
            sendS2F33out(10004L, 10004L, list);
            sendS2F35out(49L, 49L, 10004L);

            sendS2F37out(49L);
            sendS2F37out(2L);

//            sendS2F33out(3255L, 2031L, 2009L, 2028L);
//            sendS2F35out(3255L, 3255L, 3255L);
            //SEND S2F37
            //StripMapping事件定义
//            sendS2F33out(403L, 290L, 738L);
            List list1 = new ArrayList();
            list1.add(290L);

            sendS2F33out(403L, 403L, list1);
            sendS2F35out(403L, 403L, 403L);
            sendS2F37out(403L);
            //Parameter参数获取事件定义
            List list2 = new ArrayList();
            list2.add(4905L);
            sendS2F33out(50L, 50L, list2);
            sendS2F35out(50L, 50L, 50L);
            sendS2F37out(50L);
            //Parameter provider Event

            List list3 = new ArrayList();
            list3.add(4905L);
            sendS2F33out(1001L, 4905L, list3);
            sendS2F35out(4905L, 4905L, 4905L);
            sendS2F37out(4905L);
            //
            sendS5F3out(true);
            sendStatus2Server(equipStatus);
            return "1";

        } catch (Exception ex) {
            logger.error(ex.getCause());
            return "0";
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @Override
    public void processS1F13in(DataMsgMap data) {
        super.processS1F13in(data);
        if(rptDefineNum>0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    initRptPara();
                }
            }).start();
        }
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">



    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        changeEquipPanel(map);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            boolean dataReady = false;
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
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，请改机!");
                    return;
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
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    holdDeviceAndShowDetailInfo();
                } else {
                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查

                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        String ppexecnametemp = ppExecName.split("/")[1];
                        if (!ppexecnametemp.equals(deviceInfoExt.getRecipeName())) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");

                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            releaseDevice();
                            checkResult = true;
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
                                this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");

                            } else {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0), "");
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

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map map = s2f41stop();
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
                sendStatus2Server("LOCK");
                holdFlag = true;
            }
            return map;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    private Map s2f41stop() {
        return super.sendS2f41Cmd("STOP");
    }
//    public void startCheckRecipePara(Recipe checkRecipe, String type) {
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        MonitorService monitorService = new MonitorService(sqlSession);
//        List<RecipePara> equipRecipeParas = getRecipeParaFromEqpt();
//        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
//        try {
//            Map mqMap = new HashMap();
//            mqMap.put("msgName", "eqpt.StartCheckWI");
//            mqMap.put("deviceCode", deviceCode);
//            mqMap.put("recipeName", ppExecName);
//            mqMap.put("EquipStatus", equipStatus);
//            mqMap.put("lotId", lotId);
//            String eventDesc = "";
//            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
//                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
//               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
//                for (RecipePara recipePara : recipeParasdiff) {
//                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
//                }
////                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
//            } else {
//                this.releaseDevice();
//               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
//                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
//                logger.info("设备：" + deviceCode + " 开机Check成功");
//            }
//            mqMap.put("eventDesc", eventDesc);
//            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
//            sqlSession.commit();
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        } finally {
//            sqlSession.close();
//        }
//    }

    private List<RecipePara> getRecipeParaFromEqpt() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "SVRecipePara");
        logger.info("recipeTemplates size:" + recipeTemplates.size());
        List svidList = transferRecipeTemplate2SVList(recipeTemplates);
        logger.info("svidList size:" + svidList.size());
        Map svValueMap = getSpecificSVData(svidList);
        logger.info("svValueMap size:" + svValueMap.size());
        List<RecipePara> recipeParas = new ArrayList<>();
        recipeParas = transfersvValueMap2RecipeParaList(recipeTemplates, svValueMap);
        logger.info("recipepara size:" + recipeParas.size());
        return recipeParas;
    }

    private List transferRecipeTemplate2SVList(List<RecipeTemplate> recipeTemplates) {
        List svidList = new ArrayList();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            svidList.add(recipeTemplate.getDeviceVariableId());
        }
        return svidList;
    }

    private List<RecipePara> transfersvValueMap2RecipeParaList(List<RecipeTemplate> recipeTemplates, Map svValueMap) {
        List<RecipePara> recipeParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setSetValue(svValueMap.get(recipeTemplate.getDeviceVariableId()).toString());
            recipeParas.add(recipePara);
        }
        return recipeParas;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Map mapTemp = sendS1F3Check();
        String rcpName = (String) mapTemp.get("PPExecName");
        logger.info("===================rcpName:" + rcpName);
        logger.info("===================recipeName:" + recipeName);
        Map resultMap = new HashMap();
        if (!rcpName.contains(recipeName)) {
//            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传程序与设备当前程序不一致，请调整后再上传！");
            resultMap.put("checkResult", "Y");
//            return resultMap;
        }
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = null;
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) data.get("PPBODY");
            TransferUtil.setPPBody(ppbody, 1, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析      
//            recipeParaList = getRecipeParasByECSV();
//            int[] gids = {100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 133, 110, 15, 15, 15, 15, 15, 31, 31, 34, 32, 30, 23};
//            int[] pids = {10014, 10040, 10044, 10015, 10022, 10030, 10221, 10222, 10028, 10275, 10025, 10024, 10016, 10031, 26544, 15022, 1503, 1504, 1506, 1507, 1514, 3119, 3120, 3408, 3210, 3008, 2306};
//            String[] ecv = new String[27];
//            Map [] map = new HashMap[27];
//            List [] listTemp = new ArrayList[27];
//            for (int i = 0; i < gids.length; i++) {
//                logger.info("<datacon_parameters><group id='" + gids[i] + "'><parameter id='" + pids[i] + "'/></group></datacon_parameters>");
//                ecv[i] = "<datacon_parameters><group id='" + gids[i] + "'><parameter id='" + pids[i] + "'/></group></datacon_parameters>";
//                map[i] = sendS2F15outParameter(ecv[i]);
//                listTemp [i] = Sigma8800RecipeUtil.transferFromDB(map[i]);
//                recipeParaList.add(listTemp);
//        }

//            recipeParaList = Sigma8800RecipeUtil.transferFromDB(map, deviceType);
            //设备发过来的参数部分为科学计数法，这里转为一般的
//            recipeParaList = recipeParaBD2Str(recipeParaList);
        }
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }


    // </editor-fold>


    @Override
    public void initRemoteCommand() {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }


    private List<RecipePara> recipeParaBD2Str(List<RecipePara> recipeParas) {
        if (recipeParas != null && recipeParas.size() > 0) {
            for (RecipePara recipePara : recipeParas) {
                String value = recipePara.getSetValue();
                if (value.contains("E")) {
                    BigDecimal bd = new BigDecimal(value);
                    value = bd.toPlainString();
                    recipePara.setSetValue(value);
                }
            }
        }
        return recipeParas;
    }
    /**
     * WaferMappingInfo Download
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F3in(DataMsgMap DataMsgMap) {
        DataMsgMap s12f4out = null;
        String MaterialID = "";
        try {
            //DataMsgMap s12f4out = new DataMsgMap("s12f4out2", activeWrapper.getDeviceId());
            MaterialID = (String) DataMsgMap.get("MID");
            MaterialID = MaterialID.trim();
            byte IDTYP = ((byte) DataMsgMap.get("IDTYP"));
            byte MapDataFormatType = (byte) DataMsgMap.get("MAPFT");
            downFlatNotchLocation = (long) DataMsgMap.get("FNLOC");
            byte OriginLocation = (byte) DataMsgMap.get("ORLOC");
            byte ProcessAxis = ((byte) DataMsgMap.get("PRAXI"));
//            String BinCodeEquivalents = (String) ((MsgSection) DataMsgMap.get("BinCodeEquivalents")).getData();
//            String NullBinCodeValue = (String) ((MsgSection) DataMsgMap.get("NullBinCodeValue")).getData();
            Object BinCodeEquivalents = DataMsgMap.get("BCEQU");
            Object NullBinCodeValue = DataMsgMap.get("NULBC");
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "机台请求WaferMapping设置信息！WaferId：[" + MaterialID + "]");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端请求WaferMapping设置信息！WaferId：[" + MaterialID + "]");
            Map<String, String> mappingInfo = AxisUtility.downloadWaferMap(deviceCode, MaterialID);
            if ("N".equals(mappingInfo.get("flag"))) {
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "WaferId：[" + MaterialID + "]下载失败," + mappingInfo.get("msg"));
                s12f4out = new DataMsgMap("s12f4Zeroout", activeWrapper.getDeviceId());
                s12f4out.setTransactionId(DataMsgMap.getTransactionId());

                activeWrapper.sendS12F4out(null, SecsFormatValue.SECS_ASCII, IDTYP, downFlatNotchLocation, OriginLocation, 0, null, SecsFormatValue.SECS_LIST, "um", 1231, 1231, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER
                        , 0, 0, -1, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER, BinCodeEquivalents, NullBinCodeValue, SecsFormatValue.SECS_ASCII, 0 * 0, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER, DataMsgMap.getTransactionId()
                );
                this.sendTerminalMsg2EqpSingle(mappingInfo.get("msg"));
                return null;
            }
            logger.info("mappingInfo:" + mappingInfo);
            String binList = mappingInfo.get("BinList");
            int mapRow = Integer.parseInt(mappingInfo.get("RowCountInDieIncrements")); //原始wafer map行
            int mapCol = Integer.parseInt(mappingInfo.get("ColumnCountInDieIncrements")); //原始wafer map列
            String rote = mappingInfo.get("rote");//mapping原路径

            //esec 单独处理
            if (this.deviceType.contains("ESEC")) {
                StringBuilder newbinList = new StringBuilder("");
                char[][] binArray = WaferTransferUtil.toDoubleArray(binList, mapRow, mapCol);
                Map<String, Integer> map = WaferTransferUtil.blankCheck(binList.charAt(0), binList, mapRow, mapCol);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "UP:" + map.get("UP") + "DOWN:" + map.get("DOWN")
                        + "LEFT:" + map.get("LEFT") + "RIGHT:" + map.get("RIGHT"));

                int mapRowEsecNoNull = map.get("DOWN") - map.get("UP") + 1;
                int mapColEsecNoNull = map.get("RIGHT") - map.get("LEFT") + 1;

                for (int i = 0; i < mapRow; i++) {
                    if (i < map.get("UP") || i > map.get("DOWN")) {
                        continue;
                    }
                    for (int j = 0; j < mapCol; j++) {
                        if (j < map.get("LEFT") || j > map.get("RIGHT")) {
                            continue;
                        }
                        newbinList.append(binArray[i][j]);
                    }
                }
                binList = newbinList.toString();
                mapRow = mapRowEsecNoNull;
                mapCol = mapColEsecNoNull;
            }

//            waferMappingrow = String.valueOf(RowCountInDieIncrementss[0]);
//            waferMappingcol = String.valueOf(ColumnCountInDieIncrementss[0]);
//            String _waferMappingcol = waferMappingcol;
//            String _waferMappingrow = waferMappingrow;
            waferMappingbins = binList;
            logger.info(this.deviceType + "wafer map 旋转角度:" + downFlatNotchLocation + ";waferMappingrow:" + mapRow + ";waferMappingcol" + mapCol);
            if (this.deviceType.contains("ESEC") || this.deviceType.contains("SIGMA") || this.deviceType.contains("8800") || this.deviceType.contains("DB-800")) {
                logger.info(this.deviceType + "程序旋转方向和bin");
                waferMappingbins = WaferTransferUtil.transferAngleAsFlatNotchLocation(waferMappingbins, downFlatNotchLocation, mapRow, mapCol);
                if (downFlatNotchLocation == 90 || downFlatNotchLocation == 270) {
                    int temp = mapRow;
                    mapRow = mapCol;
                    mapCol = temp;
                }
            }

            s12f4out = new DataMsgMap("s12f4out", activeWrapper.getDeviceId());
            s12f4out.put("MaterialID", MaterialID);
            s12f4out.put("IDTYP", IDTYP);
            s12f4out.put("FlatNotchLocation", new long[]{downFlatNotchLocation});
            s12f4out.put("OriginLocation", OriginLocation);
            s12f4out.put("RowCountInDieIncrements", new long[]{mapRow});
            s12f4out.put("ColumnCountInDieIncrements", new long[]{mapCol});
            s12f4out.put("BinCodeEquivalents", BinCodeEquivalents);
            s12f4out.put("NullBinCodeValue", NullBinCodeValue);
            s12f4out.put("MessageLength", new long[]{mapRow * mapCol});
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "从服务端成功获取WaferMapping设置信息！WaferId：[" + MaterialID + "]");

            //针对DB800 mapping展示软件
            if (this.deviceType.contains("DB-800")) {
                String port = "8080";
                ISecsHost iSecsHost = new ISecsHost("127.0.0.1", port, deviceType, deviceCode);
                String commond = rote + "," + mapRow + "," + mapCol + "," + binList + "," + MaterialID;
                logger.info("准备发送服务器端数据至wafer软件" + commond);
                iSecsHost.executeCommand3("START," + commond + ",END;");
            }
            try {
                s12f4out.setTransactionId(DataMsgMap.getTransactionId());

                activeWrapper.sendS12F4out(MaterialID, SecsFormatValue.SECS_ASCII, IDTYP, downFlatNotchLocation, OriginLocation, 0, null, SecsFormatValue.SECS_LIST, "um", 1231, 1231, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER
                        , mapRow, mapCol, -1, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER, BinCodeEquivalents, NullBinCodeValue, SecsFormatValue.SECS_1BYTE_UNSIGNED_INTEGER, mapRow * mapCol, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER, DataMsgMap.getTransactionId()
                );
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "发送WaferMapping设置信息至机台！WaferId：[" + MaterialID + "]");
            } catch (Exception ex) {
                logger.error("Exception:", ex);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            try {
                s12f4out = new DataMsgMap("s12f4Zeroout", activeWrapper.getDeviceId());
                s12f4out.setTransactionId(DataMsgMap.getTransactionId());
                activeWrapper.respondMessage(s12f4out);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "获取服务端WaferMappingInfo出现异常！");
            } catch (Exception ex) {
                logger.error("Exception:", e);
            }
        }

        return null;
    }
}
