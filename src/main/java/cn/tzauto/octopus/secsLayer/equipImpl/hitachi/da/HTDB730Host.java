package cn.tzauto.octopus.secsLayer.equipImpl.hitachi.da;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.alarm.service.AutoAlter;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.common.ws.WSUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.hitachi.DB730Util;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;
import java.util.logging.Level;

/**
 * @author NJTZ
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2017-2-6
 * @(#)EquipHost.java
 * @Copyright tzinfo, Ltd. 2016. This software and documentation contain
 * confidential and proprietary information owned by tzinfo, Ltd. Unauthorized
 * use and distribution are prohibited. Modification History: Modification Date
 * Author Reason class Description 机台特性：1.RUN时不可上传recipe
 */
public class HTDB730Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(HTDB730Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    private String Mdln = "DB730";
    private String SoftRev = "02.155";
    private boolean canDownladMap = true;
    private final long StripMapUpCeid = 115L;
    //private boolean startCheckPass = true;
    private boolean recipeParaChange = false;
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;

    public HTDB730Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        CPN_PPID = "PPROGRAM";
    }


    @Override
    public Object clone() {
        HTDB730Host newEquip = new HTDB730Host(deviceId,
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

    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {

            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }
                //nitRptPara();
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    //获取lot号
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                //UiLogUtil.appendLog2EventTab(deviceCode, "接收到"+msg.getMsgSfName()+"消息，正在处理");
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                }
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f3in")) {
                    processS14f3in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    if (msg.get("CollEventID") != null) {
                        long ceid = msg.getSingleNumber("CollEventID");
                        if (ceid == StripMapUpCeid) {
                            processS6F11inStripMapUpload(msg);
                        } else {
                            this.processS6F11StatusChange(msg);
                        }
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11StatusChange(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                logger.fatal("Caught Interruption", e);
            } catch (Exception ex) {
                logger.error("Exception:", ex);
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
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("s6f11in")) {
                long ceid = (long) data.get("CEID");
                if (ceid == 26 || ceid == 27) {
                    processS6F11SpecialEvent(data);
                }
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f3in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s9f9Timeout")) {
                //接收到超时，直接不能下载
                this.canDownladMap = false;
                //或者重新发送参数
                initRptPara();
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
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public String initRptPara() {
        try {
            System.out.println("initRptPara+++++++++++++++++++");
//            //重新定义ppselect事件报告
//            sendS2F33out(10002l, 50070l);//
//            sendS2F35out(80l, 10002l);
//            sendS2F37out(80l);
            //重新定义processing INIT·SETUP·READY·EXECUTING·PAUSE·ERROR·WAIT LOT status
//            sendS2F35outSpecialDelete();
            List list = new ArrayList();
            list.add(50062L);
            list.add(50070L);
            sendS2F33out(10002L, 10002L, list);//50062,50070
            List ceids2list = new ArrayList();
            long[] ceids2 = new long[9];
            ceids2[0] = 3L;
            ceids2[1] = 4L;
            ceids2[2] = 5L;
            ceids2[3] = 6L;
            ceids2[4] = 7L;
            ceids2[5] = 8L;
            ceids2[6] = 38L;
            ceids2[7] = 80L;
            ceids2[8] = 88L;
            for (int i = 0; i < ceids2.length; i++) {
                activeWrapper.sendS2F35out(99999, svFormat, ceids2[i], ceFormat, null, rptFormat);
                sendS2F35out(ceids2[i], 10002L);
                ceids2list.add(ceids2[i]);
            }
            //发送s2f33
            String ack = "";
            long rptid = 10001l;
            long vid = 50200l;
            long ceid = 115l;
            sendS2F33out(rptid, vid);//115

            //SEND S2F35

            sendS2F35out(115l, 10001l);//115 10001


            //SEND S2F37

//            sendS2F37outAll(false);
            activeWrapper.sendS2F37out(false, null, ceFormat);
            activeWrapper.sendS2F37out(true, ceids2list, ceFormat);
//            sendS2F37out(true, ceids2);
            logger.debug("sendS2F37outAll:==" + ack);
            //SEND S2F37 115 发送lf unload事件
//            if (!"".equals(ack)) {
//                ack = "";
//                long[] ceids = new long[26];
//                ceids[0] = 0l;
//                ceids[1] = 1l;
//                ceids[2] = 2l;
//                ceids[3] = 3l;
//                ceids[4] = 4l;
//                ceids[5] = 5l;
//                ceids[6] = 6l;
//                ceids[7] = 7l;
//                ceids[8] = 8l;
//                ceids[9] = 88l;
//                ceids[10] = 12;
//                ceids[11] = 52l;
//                ceids[12] = 80l;
//                ceids[13] = 83l;
//                ceids[14] = 84l;
//                ceids[15] = 86l;
//                ceids[16] = 87l;
//                ceids[17] = 90l;
//                ceids[18] = 93l;
//                ceids[19] = 98l;
//                ceids[20] = 100l;
//                ceids[21] = 103l;
//                ceids[22] = 107l;
//                ceids[23] = 115l;
//                ceids[24] = 11L;
//                ceids[25] = 85L;
//                sendS2F37outMuilt(true, ceids);
////                long[] stripMapCeids = new long[1];
////                stripMapCeids[0] = 115l;
////                sendS2F37outMuilt(true, stripMapCeids);
            sendS2F37out(115L);
//            }
//          开启CEID==27(BONDING INSPECTION事件)
            /*
            <CEID list>
                ・CEID(26)：BONDING REC REG
                ・CEID(27)：BONDING INSPECTION
                ・CEID(28)：WAFER REC REG
                ・CEID(29)：PREFORM REC REG
                ・CEID(30)：PREFORM REC REG FAIL MARK
                ・CEID(31)：Preform Height Offset Teach
                ・CEID(32)：Bonding Stage Height Offset Teach
                ・CEID(33)：Pickup Height Offset Teach
             */
            //解锁绑定
            sendS2f33outDelete(10003L);
            sendS2F35outDelete(26L, 26L);
            sendS2F35outDelete(27L, 27L);
            //开启特殊事件报告

            List list1 = new ArrayList();
            list1.add(50062L);
            list1.add(50070L);
            list1.add(50063L);
//            sendS2F33out(10003L, 50062L, 50070L, 50063L);
            sendS2F33out(10003L, 50062L, list1);
            sendS2F35out(26L, 10003L);
            sendS2F37out(26L);
            sendS2F35out(27L, 10003L);
            sendS2F37out(27L);
            //全部开始时
//            long[] ceidsSpecial = new long[8];
//            ceids2[0] = 26L;
//            ceids2[1] = 27L;
//            ceids2[2] = 28L;
//            ceids2[3] = 29L;
//            ceids2[4] = 30L;
//            ceids2[5] = 31L;
//            ceids2[6] = 32L;
//            ceids2[7] = 33L;
//            for (int i = 0; i < ceids2.length; i++) {
//                sendS2F35out(ceidsSpecial[i], 10003L);
//            }
            //开启报警
            sendS5F3out(false);
            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(HTDB730Host.class.getName()).log(Level.SEVERE, null, ex);
            return "0";
        }
    }


    // <editor-fold defaultstate="collapsed" desc="S2FX Code">


    public Map sendS2F41outPPselect(String recipeName) {
        Map resultMap = new HashMap();
        try {
            logger.info("Wait 3 Seconds");
            sleep(3000);
            return super.sendS2F41outPPselect(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 means " + e.getMessage());
        }
        return resultMap;
    }

    // </editor-fold> 


    private void processS6F11SpecialEvent(DataMsgMap data) {
        long ceid = 0L;
        String event = "";
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 26) {
                event = "BONDING REC REG";
            } else if (ceid == 27) {
                event = "BONDING INSPECTION";
            } else if (ceid == 28) {
                event = "WAFER REC REG";
            } else if (ceid == 29) {
                event = "PREFORM REC REG";
            } else if (ceid == 30) {
                event = "PREFORM REC REG FAIL MARK";
            } else if (ceid == 31) {
                event = "Preform Height Offset Teach";
            } else if (ceid == 32) {
                event = "Bonding Stage Height Offset Teach";
            } else if (ceid == 33) {
                event = "Pickup Height Offset Teach";
            }
            logger.info("检测到设备触发[" + event + "]事件,设备即将被锁!请联系ME进行检查!");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备触发[" + event + "]事件,设备即将被锁!请联系ME进行检查!");
            // TODO 需要检查下MES状态，判断是否需要发送锁机指令
            //sendS2F15outLearnDevice();

            Map resultMap = new HashMap();
            resultMap.put("msgType", "s5f1");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("deviceId", deviceId);
            resultMap.put("ALID", "DB730H000" + ceid);
            resultMap.put("ALCD", 0);
            resultMap.put("ALTX", event);
            resultMap.put("Description", "Other categories");
            resultMap.put("TransactionId", data.getTransactionId());
            AutoAlter.alter(resultMap);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS6F11StatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 0L) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
                logger.info(deviceCode + "=====Equipment Offline");
            } else if (ceid == 1L) {
                controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                logger.info(deviceCode + "=====Equipment control state change to Local");
            } else if (ceid == 2L) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                logger.info(deviceCode + "=====Equipment control state change to Remote");
            } else if (ceid == 3L) {
                equipStatus = "INIT";
                logger.info(deviceCode + "=====设备开始初始化");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 4L) {
                equipStatus = "SETUP";
                logger.info(deviceCode + "=====设备Setup");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 5L) {
                equipStatus = "READY";
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备READY");
                logger.info(deviceCode + "=====设备READY");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 6L) {
                equipStatus = "RUN";
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备开机");
                logger.info(deviceCode + "=====设备开机");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 7L) {
                equipStatus = "PAUSE";
                logger.info(deviceCode + "=====设备Pause");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 8L) {
                equipStatus = "ERROR";
                logger.info(deviceCode + "=====设备Error");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 38L) {
                equipStatus = "WAIT LOT";
                logger.info(deviceCode + "=====设备WAIT LOT");
                processS6F11EquipStatusChange(data);
            } else if (ceid == 88L || ceid == 98L) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到recipe参数被修改,开机时将执行参数检查...");
                //reciep参数修改事件
                recipeParaChange = true;
            } else if (ceid == 80L) {
                findDeviceRecipe();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe切换为[" + ppExecName + "]");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        changeEquipPanel(map);
    }

    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 80) {
                findDeviceRecipe();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe切换为[" + ppExecName + "]");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                holdDevice();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息,不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            if (equipStatus.equalsIgnoreCase("READY")) {
                //TODO 校验2D的开关是否已经开启，若关闭弹窗显示
                List<Long> svlist = new ArrayList<>();
                svlist.add(54121L);//2D开关
                Map svValue = this.getSpecificSVData(svlist);
                if (svValue.get("54121").equals("0")) {
                    String dateStr = GlobalConstants.dateFormat.format(new Date());
                    this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + "2D Mark has already been closed！！");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "2D已被关闭！");
                }
            }
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                sqlSession.close();
                return;
            }
            //设备状态切换到run时检查开机check是否通过的
            //UiLogUtil.appendLog2EventTab(deviceCode, "查询服务端锁机状态：" + this.checkLockFlagFromServerByWS(deviceCode) + "本地锁机状态：" + startCheckPass);
            if (equipStatus.equalsIgnoreCase("RUN")) {
                if (holdFlag || this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已被锁");
                    holdDevice();
                }
            }
            //获取设备状态为ready时检查领料记录
            if (equipStatus.equalsIgnoreCase("READY")) {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    holdDeviceAndShowDetailInfo("RepeatAlarm LOCK");
                }
                //1、获取设备需要校验的信息类型,
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
                    //startCheckPass = false;
                    holdFlag = true;
                }
                if (!checkRecipeName(deviceInfoExt.getRecipeName())) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！");
                    //startCheckPass = false;
                    checkNameFlag = false;
                    //holdFlag= true;
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序一致，核对通过！");
                    checkNameFlag = true;
                }
                if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机检查模式!");
                }

//                if (recipeParaChange) {
                if (checkNameFlag && "A".equals(deviceInfoExt.getStartCheckMod())) {
                    //查询trackin时的recipe和GoldRecipe
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
                    boolean hasGoldRecipe = true;
                    //查询客户端数据库是否存在GoldRecipe
                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                        hasGoldRecipe = false;
                    }
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = downLoadRecipe.getVersionType();
                    if (false) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数绝对值Check");
                        this.startCheckRecipePara(downLoadRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
                        if (!hasGoldRecipe) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
                            //不允许开机
                            checkParaFlag = false;
                            //holdFlag = true;
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + ppExecName + "]开始WI参数Check");
                            checkParaFlag = this.startCheckRecipeParaReturnFlag(downLoadGoldRecipe.get(0));
                            if (!checkParaFlag) {
                                sendStatus2Server("LOCK");
                            }
                        }
                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    //如果未设置参数比对模式，默认参数比对通过
                    checkParaFlag = true;
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
                }
//                } else {
//                    //如果参数未改变，默认参数比对通过
//                    checkParaFlag = true;
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
            //总结是否需要锁机
            if (checkNameFlag && checkParaFlag) {
                //不锁机
                holdFlag = false;
            } else {
                holdFlag = true;
                //锁机
                //sendStatus2Server("LOCK");
            }
            //更新界面
            if (!this.checkLockFlagFromServerByWS(deviceCode) && !holdFlag) {
                this.setAlarmState(0);
            } else {
                this.setAlarmState(2);
            }
        }
    }

    /**
     * 开机check recipe参数 由于只有s7f5(ppid="CURRENT")时，才能获取修改参数后的recipe，所以重写此方法
     *
     * @param checkRecipe
     * @param type
     */
    public boolean startCheckRecipeParaReturnFlag(Recipe checkRecipe, String type) {
        boolean checkParaFlag = false;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        //List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.eapView.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        //此时获取修改参数后的recipe及参数，需要ppid=“CURRENT”
//        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.eapView.hostManager.getRecipeParaFromDevice(this.deviceId, "CURRENT").get("recipeParaList");
        //获取卡控的数值，直接用svid查询需要卡控参数的数值
        List<RecipePara> equipRecipeParas = transferECSVValue2RecipePara(new ArrayList<RecipeTemplate>(), recipeService.searchRecipeTemplateMonitor(deviceCode));
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
                checkParaFlag = false;
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                checkParaFlag = true;
                this.releaseDevice();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
            }
            mqMap.put("eventDesc", eventDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
            return checkParaFlag;
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param localFilePath
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName);
        s7f1out.put("Length", length);
        DataMsgMap data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.debug("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt[0]);
        resultMap.put("Description", ACKDescription.description(ppgnt[0], "PPGNT"));
        return resultMap;
    }

    /**
     * 下载Recipe，将原有的recipe使用指定的PPID下载到机台
     *
     * @param targetRecipeName
     * @return
     * @modified luosy @2017/5/4 加入sleep 3000 ms 防止出现permission not grant 导致下载失败
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName);
        s7f3out.put("Processprogram", secsItem);
        try {
            sleep(3000);
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (msgdata == null || msgdata.isEmpty()) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传请求被设备拒绝,请调整设备状态重试.");
            return null;
        }
        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            Map paraMap = DB730Util.transferFromFile(recipePath);
            recipeParaList = DB730Util.transferFromDB(paraMap, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
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

    /**
     * 在recipe被选中后删除原有recipe需要延迟删除
     *
     * @param recipeName
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        Map resultMap = new HashMap();
        try {
            //先检查是否成功执行切换recipe，
            if (ppselectFlag) {
//                int i = 0;
//                //超过四次直接执行，不管成功与否
//                while (i < 4) {
//                    //切换recipe完成,执行删除命令，，另建线程删除recipe文件
//                    if (ppselectDoneFlag) {
//                        
//                        break;
//                    }
//                    Thread.sleep(1000);
//                    i++;
//                }
                //todo切换之前recipe时
                logger.info(deviceCode + "=====正执行切换recipe动作！现延迟删除[" + recipeName + "]");
                Thread thread = new Thread(new RunnableImpl(recipeName));
                thread.start();
                //造假的回复信息
                resultMap.put("msgType", "s7f18");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("recipeName", recipeName);
                resultMap.put("ACKC7", 0);
                resultMap.put("Description", "Delete Later!");
            } else {
                //没有执行选中程序的删除recipe不需要延迟执行，不行
                resultMap = sendS7F17outReal(recipeName);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            return resultMap;
        }
    }

    class RunnableImpl implements Runnable {

        String recipeNameo;

        public RunnableImpl() {
        }

        public RunnableImpl(String recipeNameOther) {
            this.recipeNameo = recipeNameOther;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(30 * 1000);
                logger.info("延迟删除线程阻塞结束,开始执行删除操作,RECIPE为:[" + recipeNameo + "]");
                Map resultMap = sendS7F17outReal(recipeNameo);
                logger.info("执行删除完毕，RECIPE为：[" + recipeNameo + "]"
                        + "删除结果为：" + resultMap.get("Description"));
            } catch (Exception ex) {
                logger.info(ex);
            }
        }

    }

    @SuppressWarnings("unchecked")
    public Map sendS7F17outReal(String recipeName) {
        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            if (ackc7[0] == 0) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7[0], "ACKC7"));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        return resultMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S14FX Code"> 
    @SuppressWarnings("unchecked")
    @Override
    protected void processS14F1in(DataMsgMap data
    ) {
        if (data == null) {
            return;
        }
        String objType = null;
        if (data.get("ObjectType") != null) {
            objType = (String) ((SecsItem) data.get("ObjectType")).getData();
        }
        String stripId = "";
        if (data.get("StripId") != null) {
            stripId = (String) ((SecsItem) data.get("StripId")).getData();
        }
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "设备请求下载Strip Map，StripId：[" + stripId + "]");
        DataMsgMap out = null;
        //通过Web Service获得xml字符串
        String stripMapData = WSUtility.binGet(stripId, deviceCode);
        if (stripMapData == null) {//stripId不存在  
            out = new DataMsgMap("s14f2outNoExist", activeWrapper.getDeviceId());
            out.setTransactionId(data.getTransactionId());
            long[] u1 = new long[1];
            u1[0] = 0;
            out.put("ObjectAck", u1);
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "StripId：[" + stripId + "] Strip Map 不存在！");
        } else {//stripId存在
            String downLoadResult = stripMapData.substring(0, 1);
            if ("<".equals(downLoadResult)) {
                out = new DataMsgMap("s14f2out", activeWrapper.getDeviceId());
                out.put("StripId", stripId);
                out.put("MapData", stripMapData);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "从服务器下载Strip Map成功,StripId：[" + stripId + "]");
            } else {
                //是分号
                long[] errorCodes = new long[1];
                try {
                    errorCodes[0] = Long.valueOf(stripMapData.split(";")[0]);
                } catch (Exception e) {
                    errorCodes[0] = 10L;
                }
                out = new DataMsgMap("s14f2outException", activeWrapper.getDeviceId());
                out.put("StripId", stripId);
                out.put("MapData", stripMapData);
                out.put("ErrCode", errorCodes);
                out.put("ErrText", stripMapData);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "从服务器下载Strip Map失败,StripId：[" + stripId + "],失败原因：" + stripMapData);
            }
            out.setTransactionId(data.getTransactionId());
        }
        try {
            activeWrapper.respondMessage(out);
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送Strip Map到设备,StripId：[" + stripId + "]");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void processS14f3in(DataMsgMap data) {
        try {
            Map objMap = new HashMap();
            Map attrMap = new HashMap();
            attrMap.put("Orientation", "0");
            attrMap.put("OriginLocation", "UpperRight");
            attrMap.put("SubstrateSide", "TopSide");
            attrMap.put("AxisDirection", "DownLeft");
            objMap.put(new String(), attrMap);
            Map stripIDformatMap = new HashMap();
            stripIDformatMap.put("Orientation", FormatCode.SECS_ASCII);
            stripIDformatMap.put("OriginLocation", FormatCode.SECS_ASCII);
            stripIDformatMap.put("SubstrateSide", FormatCode.SECS_ASCII);
            stripIDformatMap.put("AxisDirection", FormatCode.SECS_ASCII);
            activeWrapper.sendS14F2out(objMap, FormatCode.SECS_ASCII, FormatCode.SECS_ASCII, stripIDformatMap,
                    (byte) 0, new HashMap<>(), FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, data.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold> 


    @Override
    public boolean testInitLink() {
        try {
            DataMsgMap s1f13out = new DataMsgMap("s1f13out", activeWrapper.getDeviceId());
            long transactionId = activeWrapper.getNextAvailableTransactionId();
            s1f13out.setTransactionId(transactionId);
            s1f13out.put("Mdln", Mdln);
            s1f13out.put("SoftRev", SoftRev);
            activeWrapper.sendAwaitMessage(s1f13out);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String testRUThere() {
        try {
            DataMsgMap s1f1out = new DataMsgMap("s1f1out", activeWrapper.getDeviceId());
            long transactionId = activeWrapper.getNextAvailableTransactionId();
            s1f1out.setTransactionId(transactionId);
            logger.info("transactionId = " + transactionId);
            DataMsgMap s1f2in = activeWrapper.sendAwaitMessage(s1f1out);
            if (s1f2in != null) {
                //如果回复取消会话，那么需要重新发送S1F13
                if (s1f2in.getMsgSfName().contains("s1f0")) {
                    logger.info("testRUThere成功,但是未正确回复消息,需要重新建立连接 ");
                    return "1";
                } else {
                    logger.info("testRUThere成功、通信正常 ");
                    return "0";
                }
            } else {
                return "2";
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "2";
        }
    }

    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        Map resultMap = null;
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            if (!"RUN".equalsIgnoreCase(equipStatus)) {
                //不在RUN状态，假装已被锁
                resultMap = new HashMap();
                resultMap.put("msgType", "s2f42");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("prevCmd", "STOP");
                resultMap.put("HCACK", 0);
                resultMap.put("Description", "设备已被锁,将无法开机");
                this.setAlarmState(2);
            } else {
                //RUN状态，发送停机指令
                //            super.sendS2f41Cmd("STOP");
                resultMap = this.sendS2f41Cmd("STOP");
                if ((byte) resultMap.get("HCACK") == 0 || (byte) resultMap.get("HCACK") == 4) {
                    this.setAlarmState(2);
                }
            }
            //锁机状态发送给服务端并修改锁机标识
            //sendStatus2Server("LOCK");
            //重置标识
            holdFlag = true;
            //startCheckPass = false;
            //此时默认未被修改，不再上传recipe，待ceid=88，才上传recipe进行修改
//            recipeParaChange = false;
            return resultMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    //释放机台
    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        //startCheckPass = true;
        holdFlag = false;
//        recipeParaChange = false;
        return null;
    }

    /**
     * 初始化设备支持的命令格式
     */
    @Override
    public void initRemoteCommand() {
        String commandKey = "start";
        CommandDomain startCommand = new CommandDomain();
        startCommand.setRcmd("START");
        this.remoteCommandMap.put(commandKey, startCommand);

        commandKey = "stop";
        CommandDomain stopCommand = new CommandDomain();
        stopCommand.setRcmd("STOP");
        this.remoteCommandMap.put(commandKey, stopCommand);

        commandKey = "pause";
        CommandDomain pauseCommand = new CommandDomain();
        pauseCommand.setRcmd("PAUSE");
        this.remoteCommandMap.put(commandKey, pauseCommand);

        commandKey = "resume";
        CommandDomain resumeCommand = new CommandDomain();
        resumeCommand.setRcmd("RESUME");
        this.remoteCommandMap.put(commandKey, resumeCommand);

        commandKey = "local";
        CommandDomain localCommand = new CommandDomain();
        localCommand.setRcmd("LOCAL");
        this.remoteCommandMap.put(commandKey, localCommand);

        commandKey = "remote";
        CommandDomain remoteCommand = new CommandDomain();
        remoteCommand.setRcmd("REMOTE");
        this.remoteCommandMap.put(commandKey, remoteCommand);
        //调用父类的方法，生成公用命令，如果不支持，可以删掉，如果不公用，直接覆盖
        initCommonRemoteCommand();
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

}
