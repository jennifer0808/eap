package cn.tzauto.octopus.secsLayer.equipImpl.asm.da;

import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

public class AsmAD832PHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AsmAD832PHost.class.getName());
    private final long StripMapUpCeid = 237L;


    public AsmAD832PHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        EquipStateChangeCeid = 4;
    }

    @Override
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
                if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                }
                sendS5F3out(true);
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS2F37outAll();
                }

                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    if (msg.get("CollEventID") != null) {
                        long ceid = msg.getSingleNumber("CollEventID");
                        if (ceid == StripMapUpCeid) {
                            processS6F11inStripMapUpload(msg);
                        } else {
                            processS6F11in(msg);
                        }
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().toLowerCase().contains("s6f11intodo")) {
                    processS6F11Filter(msg);
                }  else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    this.processS6F11in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
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
            LastComDate = new Date().getTime();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().contains("s6f11intodo")) {
                processS6F11in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
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

    // <editor-fold defaultstate="collapsed" desc="S1FXin Code">
//    @SuppressWarnings("unchecked")
//    public void processS1F1in(DataMsgMap data) {
//        try {
//            DataMsgMap s1f2out = new DataMsgMap("s1f2zeroout", activeWrapper.getDeviceId());
////            s1f2out.put("Mdln", Mdln);
////            s1f2out.put("SoftRev", SoftRev);
//            s1f2out.setTimeStamp(new Date());
//            s1f2out.setTransactionId(data.getTransactionId());
//            activeWrapper.respondMessage(s1f2out);
//            logger.info("s1f2out sended.");
//            if (this.getCommState() != this.COMMUNICATING) {
//                this.setCommState(this.COMMUNICATING);
//            }
//            if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
//                this.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
//    @SuppressWarnings("unchecked")
//    public void processS1F2in(DataMsgMap s1f2in) {
//        if (s1f2in == null) {
//            return;
//        }
//        Mdln = (String) ((SecsItem) s1f2in.get("Mdln")).getData();
//        SoftRev = (String) ((SecsItem) s1f2in.get("SoftRev")).getData();
//        long transactionId = s1f2in.getTransactionId();
//        logger.info("processS1F2in Mdln = " + Mdln);
//        logger.info("processS1F2in SoftRev = " + SoftRev);
//        logger.info("processS1F2in transactionId = " + transactionId);
//        logger.info("processS1F2in" + new Date());
//        if (this.getCommState() != this.COMMUNICATING) {
//            this.setCommState(this.COMMUNICATING);
//        }
//        if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
//            this.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public void processS1F13in(DataMsgMap data) {
//        try {
//            Mdln = (String) ((SecsItem) data.get("Mdln")).getData();
//            SoftRev = (String) ((SecsItem) data.get("SoftRev")).getData();
//            DataMsgMap s1f14out = new DataMsgMap("s1f14outZero", activeWrapper.getDeviceId());
//            byte[] ack = new byte[1];
//            ack[0] = 0;
//            s1f14out.put("AckCode", ack);
//            s1f14out.setTimeStamp(new Date());
//            s1f14out.setTransactionId(data.getTransactionId());
//            activeWrapper.respondMessage(s1f14out);
//            logger.info("s1f14out sended.");
//            if (this.getCommState() != this.COMMUNICATING) {
//                this.setCommState(this.COMMUNICATING);
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public void processS1F14in(DataMsgMap s1f14in) {
//        if (s1f14in == null) {
//            return;
//        }
//        logger.info("-----Received s1f14in----.");
//        if (this.getCommState() != this.COMMUNICATING) {
//            this.setCommState(this.COMMUNICATING);
//        }
//    }
    @SuppressWarnings("unchecked")
    @Override
    public void sendS1F13out() {
        DataMsgMap s1f13out = new DataMsgMap("s1f13outListZero", activeWrapper.getDeviceId());
        s1f13out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s1f13out.put("Mdln", Mdln);
//        s1f13out.put("SoftRev", SoftRev);
        try {
            activeWrapper.sendAwaitMessage(s1f13out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FXin Code">
    //根据ReportId来处理不同事件报告
    protected void processS6F11Filter(DataMsgMap data) {
        long ceid = 0l;
        long reportId = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            reportId = data.getSingleNumber("ReportId");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //TODO ceid 可配置在数据库显示具体描述
//TODO z这里为什么用的rptid判定

        if (reportId == 4) {
            processS6F11EquipStatusChange(data);
        } else if (reportId == 1) {
            processS6F11ControlStateChange(data);
        } else if (reportId == 175) {
            processS6F11PPExecNameChange(data);
        }

    }

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");

        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = data.getSingleNumber("CollEventID");
            preEquipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("PreEquipStatus")), deviceType);
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return;
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
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
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
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
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo();
                } else {
                    //1、获取设备需要校验的信息类型,
                    String startCheckMod = deviceInfoExt.getStartCheckMod();
                    boolean hasGoldRecipe = true;
                    if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                        holdDeviceAndShowDetailInfo();
                        UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                    }
                    //查询trackin时的recipe和GoldRecipe
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));

                    //查询客户端数据库是否存在GoldRecipe
                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                        hasGoldRecipe = false;
                    }

                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查
                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                        if (!checkResult) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        }
                    }
                    logger.info("设备[" + deviceCode + "]的开机检查模式为:" + startCheckMod);
                    if (startCheckMod.contains("B")) {
                        startSVcheckPass = false;
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
                        startSVcheck();
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if (false) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                            }

                        }
                    } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
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

    protected void processS6F11ControlStateChange(DataMsgMap data) {
        //回复s6f11消息
        long ceid = 0l;
        long reportID = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            reportID = data.getSingleNumber("ReportId");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
//        if (ceid == 1 && reportID == 1) {
        Map panelMap = new HashMap();
        if (ceid == 4) {
            controlState = FengCeConstant.CONTROL_OFFLINE;
            UiLogUtil.appendLog2SecsTab(deviceCode, "设备状态切换到OFF-LINE");
        }
        if (ceid == 2) {
            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
            UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换到Local");
        }
        if (ceid == 3) {
            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
            UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换到Remote");
        }
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
//        }
    }

    private void processS6F11PPExecNameChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        UiLogUtil.appendLog2SecsTab(deviceCode, "收到事件报告CEID: " + ceid + ", 设备正在使用的程序为: " + ppExecName);
        Map panelMap = new HashMap();
        panelMap.put("PPExecName", ppExecName);
        changeEquipPanel(panelMap);
    }

    protected void processS6F11LoginUserChange(DataMsgMap data) {
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        long ceid = 0l;
        long reportID = 0l;
        String loginUserName = "";
        try {
            out.setTransactionId(data.getTransactionId());
            ceid = data.getSingleNumber("CollEventID");
            reportID = data.getSingleNumber("ReportId");
            loginUserName = ((SecsItem) data.get("UserLoginName")).getData().toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 120 && reportID == 120) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "登陆用户变更，当前登陆用户：" + loginUserName);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = new ArrayList();
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) data.get("PPBODY");
            TransferUtil.setPPBody(ppbody, 1, recipePath);
            logger.debug("Receive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析   
            recipeParaList = getRecipeParasByECSV();
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeNameMapping", null);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

// </editor-fold> 

    //释放机台
    @Override
    public Map releaseDevice() {
        Map map = new HashMap();// this.sendS2f41Cmd("START");
//        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
//            this.setAlarmState(0);
//        }
        map.put("HCACK", 0);
        return map;
    }


    /*
     * (non-Javadoc) It only copies field member values except Mli.
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() {
        AsmAD832PHost newEquip = new AsmAD832PHost(deviceId,
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
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }
}
