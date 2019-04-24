/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.esec.fc;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.besi.Sigma8800RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
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
    private static final Logger logger = Logger.getLogger(SigmaPlusHost.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    String FlatNotchLocation;
    private volatile boolean isInterrupted = false;

    //private Object synS2F41 = null;
    public SigmaPlusHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
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
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!isInterrupted) {

            try {
                while (!this.isSdrReady()) {
                    SigmaPlusHost.sleep(200);
                }
                if (this.getCommState() != SigmaPlusHost.COMMUNICATING) {
                    sendS1F13out();
                }
                if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
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
    @SuppressWarnings("unchecked")
    public void sendS2F33clear() {
        DataMsgMap s2f37outAll = new DataMsgMap("s2f33clear", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sendS2F35clear() {
        DataMsgMap s2f37outAll = new DataMsgMap("s2f35clear", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
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
            sendS2F33out(1001L,4905L, list3);
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
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        if (listtmp != null) {
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
            ppExecName = String.valueOf(listtmp.get(1));
            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName.replaceAll(".dbrcp", ""));
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">


    public Map sendS2F15outParameter() {
        DataMsgMap out = new DataMsgMap("s2f15out", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        long[] u1 = new long[1];
        u1[0] = 9903;
        String ecv = "<datacon_parameters>"
                + "<group id='100'>"
                + "<parameter id='10014'/><parameter id='10040'/>"
                + "<parameter id='10044'/><parameter id='10015'/>"
                + "<parameter id='10022'/><parameter id='10030'/>"
                + "<parameter id='10028'/><parameter id='10029'/>"
                + "<parameter id='10025'/>"
                + "<parameter id='10024'/><parameter id='10016'/>"
                + "<parameter id='10031'/>"
                + "<parameter id='10021'/>"
                + "<parameter id='10043'/><parameter id='10046'/>"
                + "</group>"
                + "<group id='133'>"
                + "<parameter id='26544'/>"
                + "</group>"//133
                + "<group id='110'>"
                + "<parameter id='15022'/><parameter id='15061'/>"
                + "</group>"//110
                + "<group id='15'>"
                + "<parameter id='1503'/><parameter id='1504'/>"
                + "<parameter id='1506'/><parameter id='1507'/>"
                + "</group>"//15
                //                + "<group id='25'>"
                //                + "<parameter id='2516'/>"
                //                + "</group>"//25
                + "<group id='31'>"
                + "<parameter id='3119'/><parameter id='3120'/>"
                + "</group>"//31
                + "<group id='34'>"
                + "<parameter id='3408'/>"
                + "</group>"//34
                + "<group id='32'>"
                + "<parameter id='3210'/>"
                + "</group>"//32
                + "<group id='23'>"
                + "<parameter id='2306'/>"
                + "</group>"//23
                + "<group id='30'>"
                + "<parameter id='3008'/>"
                + "</group>"//30
                + "</datacon_parameters>";
        out.put("ECID", u1);
        out.put("ECV", ecv);
        try {//todo s2f15增加
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap = sendS1F3SingleCheck("14903");
        logger.info("sendS1F3SingleCheck" + resultMap);
//        resultMap = sendS1F3SingleCheck("14903");
        return resultMap;
    }

    public void processS2f16in(DataMsgMap in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f16in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("EAC")).getData();
        System.out.println();
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }

    public void sendS2F29outECID() {
        DataMsgMap out = new DataMsgMap("s2f29oneout", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        long[] u1 = new long[1];
        u1[0] = 5012;
        out.put("ECID", u1);
        try {
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS2f30in(DataMsgMap in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f30in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("EAC")).getData();
        System.out.println();
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }


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
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = super.sendS7F1out(localFilePath, "Production/" + targetRecipeName);
        resultMap.put("ppid", targetRecipeName);
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map resultMap = super.sendS7F3out(localRecipeFilePath, "Production/" + targetRecipeName);
        resultMap.put("ppid", targetRecipeName);
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Map mapTemp = sendS1F3Check();
        String rcpName = (String) mapTemp.get("PPExecName");
        logger.info("===================rcpName:" + rcpName);
        logger.info("===================recipeName:" + recipeName);
        Map resultMap = new HashMap();
        if (!rcpName.contains(recipeName)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传程序与设备当前程序不一致，请调整后再上传！");
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上传程序与设备当前程序不一致，请调整后再上传！");
            resultMap.put("checkResult", "Y");
            return resultMap;
        }
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F5out("Production/" + recipeName);
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
            Map map = sendS2F15outParameter();
            recipeParaList = Sigma8800RecipeUtil.transferFromDB(map, deviceType);
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        Map resultMap = super.sendS7F17out("Production/" + recipeName);
        resultMap.put("recipeName", recipeName);
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = super.sendS7F19out();
        ArrayList recipeList = (ArrayList) resultMap.get("eppd");
        if (recipeList.size() != 0) {
            ArrayList list1 = new ArrayList();
            for (Object object : recipeList) {
                if (String.valueOf(object).contains("Production/")) {
                    list1.add(String.valueOf(object).replaceAll("Production/", "").replaceAll(".dbrcp", ""));
                }
            }
            resultMap.put("eppd", list1);
        }
        return resultMap;
    }

    @Override
    public Map getSpecificSVData(List dataIdList) {
        Map resultMap = new HashMap();
        resultMap = Sigma8800RecipeUtil.transferSV(sendS2F15outParameter());
        logger.info("getSpecificSVData" + resultMap);
        return resultMap;
    }


    public Map processS12F9inold(DataMsgMap DataMsgMap) {
        try {
            String MaterialID = (String) ((SecsItem) DataMsgMap.get("MaterialID")).getData();
            byte[] IDTYP = ((byte[]) ((SecsItem) DataMsgMap.get("IDTYP")).getData());
            int[] STRPxSTRPy = (int[]) ((SecsItem) DataMsgMap.get("STRPxSTRPy")).getData();
            SecsItem BinListItem = (SecsItem) DataMsgMap.get("BinList");
            String binList = "";
            if (BinListItem.getData() instanceof Long[] || BinListItem.getData() instanceof long[]) {
                long[] binlists = (long[]) BinListItem.getData();
                StringBuffer binBuffer = new StringBuffer();
                for (Long binlistLong : binlists) {
                    int temp = binlistLong.intValue();
                    char c = (char) temp;
                    binBuffer.append(c);
                }
                binList = binBuffer.toString();
            } else {
                binList = (String) ((SecsItem) DataMsgMap.get("BinList")).getData();
            }

           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "机台上传WaferMapping成功！WaferId：[" + MaterialID + "]");
            //上传WaferMapping,
            String _uploadWaferMappingRow = uploadWaferMappingRow;
            String _uploadWaferMappingCol = uploadWaferMappingCol;
            if (this.deviceType.contains("ESEC") || this.deviceType.contains("SIGMA") || this.deviceType.contains("8800")) {
                binList = WaferTransferUtil.transferAngleAsFlatNotchLocation(binList, 360L - upFlatNotchLocation, uploadWaferMappingRow, uploadWaferMappingCol);
                if (upFlatNotchLocation == 90 || upFlatNotchLocation == 270) {
                    _uploadWaferMappingRow = uploadWaferMappingCol;
                    _uploadWaferMappingCol = uploadWaferMappingRow;
                }
            }
            //上传旋转后的行列数及mapping
            AxisUtility.sendWaferMappingInfo(MaterialID, _uploadWaferMappingRow, _uploadWaferMappingCol, binList, deviceCode);
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送WaferMapping成功！WaferId：[" + MaterialID + "]");
            DataMsgMap s12f10out = new DataMsgMap("s12f10out", activeWrapper.getDeviceId());
            byte[] ack = new byte[]{0};
            s12f10out.put("MDACK", ack);
            s12f10out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.respondMessage(s12f10out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }


}
