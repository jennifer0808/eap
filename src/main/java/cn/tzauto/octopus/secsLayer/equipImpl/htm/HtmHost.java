package cn.tzauto.octopus.secsLayer.equipImpl.htm;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.api.SecsDriverFactory;
import cn.tzauto.generalDriver.entity.cnct.ConnRegInfo;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.generalDriver.wrapper.ActiveWrapper;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.EquipmentEventDealer;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.NotInitializedException;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.htm.HtmRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.ibatis.jdbc.Null;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.util.*;

public class HtmHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(HtmHost.class.getName());

    private static Map<Object, String> ceidMap = new HashMap<>();
    private static Map<Object, String> alidMap = new HashMap<>();

    private static Map<String, String> machineMap = new HashMap<>();
    private static Map<String, String> stopCmdMap = new HashMap<>();

    private static Map<String, Map<String, Object>> stateMap = new HashMap<>();

    private static List<Long> popularCeidList = new ArrayList<>();

    public HtmHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER;
        EquipStateChangeCeid = 10;
        RCMD_PPSELECT = "PP_SELECT";
        // 添加所有设备的报警id，用于转发报警事件
        long[] alidsOfHtm4623 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};
        for (long alid : alidsOfHtm4623) {
            alidMap.put(alid, "HTM-4623");
        }
        long[] alidsOfHtm3661 = {51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
                74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
        for (long alid : alidsOfHtm3661) {
            alidMap.put(alid, "HTM-3661");
        }

        // 添加所有设备的状态变化的ceid，用于转发事件
        ceidMap.put(44, "HTM-3661");
        ceidMap.put(81, "HTM-3661");
        ceidMap.put(42, "HTM-4623");
        ceidMap.put(43, "HTM-4623");

        // 添加公共事件(目前仅用于secs连接状态)
        popularCeidList.add(1l);
        popularCeidList.add(2l);
        popularCeidList.add(3l);

        // 添加所有设备的设备切换指令，用于在获取recipe列表，下载recipe文件等事件之前，用于模组确认向哪台设备进行操作
        machineMap.put("HTM-4623", "MACHINE1");
        machineMap.put("HTM-3661", "MACHINE2");

        // 添加所有设备的停机指令
        stopCmdMap.put("HTM-4623", "STOP1");
        stopCmdMap.put("HTM-3661", "STOP2");

        // 添加所有设备的状态查询用的svid

        Map<String, Object> svidMap1 = new HashMap<>();
        svidMap1.put("equipStatussvid", 27l);
        svidMap1.put("pPExecNamesvid", 22l);
        svidMap1.put("controlStatesvid", 26l);
        stateMap.put("HTM-4623", svidMap1);
        Map<String, Object> svidMap2 = new HashMap<>();
        svidMap2.put("equipStatussvid", 25l);
        svidMap2.put("pPExecNamesvid", 41l);
        svidMap2.put("controlStatesvid", 26l);
        stateMap.put("HTM-3661", svidMap2);

    }

    @Override
    public void initialize() {
        logger.info("Initializing SECS Protocol for " + this.deviceId + ".");
        activeWrapper = (ActiveWrapper) SecsDriverFactory.getSecsDriverByReg(new ConnRegInfo(Integer.valueOf(this.deviceId), "active", this.iPAddress, this.tCPPort));
//        ConnRegInfo.register(Integer.valueOf(this.deviceId), "active", this.remoteIPAddress, this.remoteTCPPort);
        synchronized (GlobalConstants.connectHostMap) {
            if (null != GlobalConstants.connectHostMap.get(this.deviceType)) {
                this.activeWrapper = GlobalConstants.connectHostMap.get(this.deviceType).getActiveWrapper();
                GlobalConstants.hostMap.put(this.deviceCode, HtmHost.this);
//                logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            } else {
                activeWrapper = (ActiveWrapper) SecsDriverFactory.getSecsDriverByReg(new ConnRegInfo(Integer.valueOf(this.deviceId), "active", this.iPAddress, this.tCPPort));
                GlobalConstants.connectHostMap.put(this.deviceType, HtmHost.this);
                GlobalConstants.hostMap.put(this.deviceCode, HtmHost.this);
//                logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }
    }

    /**
     * 开启SECS通信线程
     *
     * @throws NotInitializedException
     */
    @Override
    public void startSecs(EquipmentEventDealer eqpEventDealer)
            throws NotInitializedException, InterruptedException, T3TimeOutException, T6TimeOutException {
        if (this.activeWrapper == null) {
            throw new NotInitializedException("Host with device id = " + this.deviceId
                    + " Equip Id = " + this.deviceId + " is not initialized yet.");
        }
        logger.info("SECS Protocol for " + this.deviceId + " is being started.");
        synchronized (this.activeWrapper) {
            if (this.activeWrapper.isInitialized()) {
//            this.activeWrapper = GlobalConstants.activeWrapperMap.get(eqpEventDealer.getDeviceType());
                eqpEventDealer.execute();
//                activeWrapper.addInputMessageListenerToAll(this);
            } else {
                this.activeWrapper.connectByActiveMode(eqpEventDealer);
                eqpEventDealer.execute();
                activeWrapper.addInputMessageListenerToAll(this);
                activeWrapper.startInActiveMode();
//            GlobalConstants.activeWrapperMap.put(eqpEventDealer.getDeviceType(), this.activeWrapper);
//            logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }
    }

    @Override
    public Object clone() {
        HtmHost newEquip = new HtmHost(deviceId,
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
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
//        if (null != GlobalConstants.activeWrapperMap.get(this.deviceType)) {
//            this.activeWrapper = GlobalConstants.activeWrapperMap.get(this.deviceType);
//        } else {
//            GlobalConstants.activeWrapperMap.put(this.deviceType, this.activeWrapper);
//            logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        }
//        if (null == this.activeWrapper && null != GlobalConstants.activeWrapperMap.get(this.deviceType)) {
//            this.activeWrapper = GlobalConstants.activeWrapperMap.get(this.deviceType);
//        }
        while (!this.isInterrupted()) {
            try {
                synchronized (GlobalConstants.connectHostMap) {
                    EquipHost connectHost = GlobalConstants.connectHostMap.get(this.deviceType);
                    while (!this.isSdrReady()) {
                        this.sleep(200);
                        if (null != connectHost && connectHost.isSdrReady()) {
                            setSdrReady(true);
                        }
                    }
                    if (this.getCommState() != this.COMMUNICATING) {
                        if (null != connectHost && !this.deviceCode.equals(connectHost.getDeviceCode()) && connectHost.getCommState() == connectHost.COMMUNICATING) {
                            setCommState(1);
                        } else {
                            sendS1F13out();
                            GlobalConstants.connectHostMap.put(this.deviceType, HtmHost.this);
                        }
                    }
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                    initRptPara();
                }
                //设备在下一个可能停止的点才能停止
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
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
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                super.replyS6F12WithACK(data, (byte) 0);
                long ceid = Long.parseLong(data.get("CEID").toString());
                String reportDeviceCode = "";
                if (null != ceidMap.get(ceid)) {
                    reportDeviceCode = alidMap.get(ceid);
                }
                if (this.deviceCode.equals(reportDeviceCode)) {
                    this.inputMsgQueue.put(data);
                    logger.info(deviceCode + " received a report,ceid:" + ceid);
                } else {
                    if (!"".equals(reportDeviceCode)) {
                        // 将事件转发到对应的host实例上
                        GlobalConstants.hostMap.get(reportDeviceCode).inputMessageArrived(event);
                    } else if (popularCeidList.contains(ceid)) {
                        // 公共事件处理
                        this.inputMsgQueue.put(data);
                    }
                }
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                long alid = Long.parseLong(data.get("ALID").toString());
                String reportDeviceCode = "";
                if (null != alidMap.get(alid)) {
                    reportDeviceCode = alidMap.get(alid);
                }
                if (this.deviceCode.equals(reportDeviceCode)) {
                    this.inputMsgQueue.put(data);
                    logger.info(deviceCode + " occerred a error,alid:" + alid);
                } else {
                    if (!"".equals(reportDeviceCode)) {
                        GlobalConstants.hostMap.get(reportDeviceCode).inputMessageArrived(event);
                    }
                }
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


    public String initRptPara() {
        try {
            logger.debug("initRptPara+++++++++++++++++++");
//            this.sendS2F33clear();
//            this.sendS2F35clear();
//            //重新定义Learn Device事件
//            List<Long> svidlist = new ArrayList<>();
//            svidlist.add(56L);
//            svidlist.add(60L);
//            svidlist.add(11L);
//            sendS2F33out(9L, 9L, svidlist);
//            sendS2F35out(9L, 9L, 9L);
//            sendS2F37out(9L);
//            sendS2F37outAll();

//            sendS2F37outClose(15650L);
//            sendS2F37outClose(15652L);
//            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
//            java.util.logging.Logger.getLogger(EsecDB2100Host.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception:", ex);
            return "0";
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">

    protected List getNcessaryData() {
        DataMsgMap data = null;
        try {
            List<Long> statusList = new ArrayList<>();
            long equipStatussvid = (long) stateMap.get(deviceCode).get("equipStatussvid");
            long pPExecNamesvid = (long) stateMap.get(deviceCode).get("pPExecNamesvid");
            long controlStatesvid = (long) stateMap.get(deviceCode).get("controlStatesvid");
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            RecipeService recipeService = new RecipeService(sqlSession);
//            equipStatussvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
//            pPExecNamesvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
//            controlStatesvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
            statusList.add(equipStatussvid);
            statusList.add(pPExecNamesvid);
            statusList.add(controlStatesvid);
            data = activeWrapper.sendS1F3out(statusList, svFormat);
        } catch (Exception e) {
            logger.error("Wait for get meessage directly error：" + e);
            UiLogUtil.getInstance().appendLog2SecsTab("", "获取设备当前状态信息失败，请检查设备状态.");
        }
        if (data == null || data.get("SV") == null) {
            return null;
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        return (ArrayList) data.get("SV");

    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    //hold机台
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            String cmd = stopCmdMap.get(deviceCode);
            Map map = this.sendS2f41Cmd(cmd);
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

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = -12345679;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else if (ceid == 1 || ceid == 2 || ceid == 3 || ceid == 42 || ceid == 43 || ceid == 44 || ceid == 81) {
                processS6F11EquipStatusChange(data);
                if (ceid == 1 || ceid == 2 || ceid == 3) {
                    for (Map.Entry<String, EquipHost> equipHost : GlobalConstants.hostMap.entrySet()) {
                        if (this.deviceCode.equals(equipHost.getKey()) || !this.deviceType.equals(equipHost.getValue().getDeviceType())) {
                            continue;
                        }
                        GlobalConstants.hostMap.get(equipHost.getKey()).findDeviceRecipe();
                    }
                }
            }
            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    /**
     * Z
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);

        long length = TransferUtil.getPPLength(localFilePath);
        if (length == 0) {
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
            return resultMap;
        }

        DataMsgMap data = null;

        try {
            // 华天锡化设备需要发送远程指令确认给什么设备进行动作
            String cmd = machineMap.get(deviceCode);
            DataMsgMap msgdata = activeWrapper.sendS2F41out(cmd, null, null, null, null);
            byte hcack = (byte) msgdata.get("HCACK");
            if (hcack != 0) {
                resultMap.put("ppgnt", 9);
                resultMap.put("Description", ACKDescription.description(hcack, "HCACK") + "; error occurred while send message '" + cmd + "'");
            } else {
                // 请求下载
                data = activeWrapper.sendS7F1out(targetRecipeName, length, lengthFormat);
                byte ppgnt = (byte) data.get("PPGNT");
                logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
                resultMap.put("ppgnt", ppgnt);
                resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        List<RecipePara> recipeParaList = null;
        // recipe文件解析
//        try {
//            recipeParaList = HtmRecipeUtil.analysisRecipe(recipePath, deviceType, deviceCode);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
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

    public Map sendS7F19out() {
//        Map resultMap = new HashMap();
        Map resultMap = new CaseInsensitiveMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap data = null;
        try {
            String cmd = machineMap.get(deviceCode);
            DataMsgMap msgdata = activeWrapper.sendS2F41out(cmd, null, null, null, null);
            byte hcack = (byte) msgdata.get("HCACK");
            if (hcack != 0) {
                data = null;
                resultMap.put("ppgnt", 9);
                resultMap.put("Description", ACKDescription.description(hcack, "HCACK") + "; error occurred while send message '" + cmd + "'");
            } else {
                data = activeWrapper.sendS7F19out();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList list = (ArrayList) data.get("EPPD");
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
//            resultMap.put("EPPD", new ArrayList<>());
        } else {
            logger.info("recipeNameList:" + list);
            resultMap.put("eppd", list);
//            resultMap.put("EPPD", list);
        }
        return resultMap;
    }

    // </editor-fold>

    @Override
    public void sendTerminalMsg2EqpSingle(String msg) {
        String cmd = machineMap.get(deviceCode);
        try {
            DataMsgMap msgdata = activeWrapper.sendS2F41out(cmd, null, null, null, null);
            byte hcack = (byte) msgdata.get("HCACK");
            if (hcack == 0) {
                sendS10F3((byte) 0, msg);
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }
}
