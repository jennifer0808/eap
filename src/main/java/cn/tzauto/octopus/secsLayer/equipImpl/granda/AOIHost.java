/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.granda;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.exception.MliBaseException;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MessageArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.MsgDataHashtable;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
 * @author luosy
 */
public class AOIHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AOIHost.class.getName());

    public AOIHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
        this.svFormat = FormatCode.SECS_1BYTE_UNSIGNED_INTEGER;
    }

    public AOIHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
        this.svFormat = FormatCode.SECS_1BYTE_UNSIGNED_INTEGER;
    }

    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.equipId);
        while (!this.isInterrupted()) {
            try {
                while (!this.isJsipReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    initRptPara();
//                    sendS2F41outPPselect("BDM572HFSM-01");
//                    sendS2f41Cmd("STOP");
                }
                MsgDataHashtable msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s5f1in") || msg.getMsgTagName().equalsIgnoreCase("s5f1ypmin")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    Long ceid = 0L;
                    try {
                        ceid = msg.getSingleNumber("CollEventID");
                    } catch (MliBaseException ex) {
                        ex.printStackTrace();
                    }
                    if (ceid == 137L) {
                        processS6F11inStripMapUpload(msg);
                    } else {
                        byte[] ack = new byte[1];
                        ack[0] = 0;
                        replyS6F12WithACK(msg, ack);
                    }
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s14f1inMapDownLoad")) {
                    processS14F1in(msg);

                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11equipstate")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11in")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11incommon40")) {
                    Long ceid = 0L;
                    try {
                        ceid = msg.getSingleNumber("CollEventID");
                    } catch (MliBaseException ex) {
                        ex.printStackTrace();
                    }
                    //TR7700设备点击测试等待HOST发送START指令
                    if (ceid == 133L) {
                        sendS2f41Cmd("START");
                    }
//                    processS6F11in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    ppExecName = (String) ((SecsItem) msg.get("PPExecName")).getData();
                    Map map = new HashMap();
                    map.put("PPExecName", ppExecName);
                    changeEquipPanel(map);
                } else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgTagName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    @Override
    public void inputMessageArrived(MessageArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = new Date().getTime();
            secsMsgTimeoutTime = 0;
            MsgDataHashtable data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstatuschange") || tagName.equalsIgnoreCase("s6f11equipstate") || tagName.equalsIgnoreCase("s6f11ppselectfinish")
                    || tagName.equalsIgnoreCase("s6f11SPCData1") || tagName.equalsIgnoreCase("s6f11SPCData2")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("s6f11in")) {
                //回复掉消息
                processS6F11in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().equals("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
//                processS6F11inStripMapUpload(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1inMapDownLoad")) {
                processS14F1in(data);
            } else if (tagName.contains("F0") || tagName.contains("f0")) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
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
    @SuppressWarnings("unchecked")
    protected void processS6F11EquipStatus(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //获取当前设备状态
        sendS1F3Check();

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
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，请改机!");
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
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo("Equipment has been set and locked by Server");
                } else {
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
                            holdDeviceAndShowDetailInfo("RecipeName Error! Equipment locked!");
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if ("Unique".equals(downloadRcpVersionType)) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
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
//        long ceid = 0l;
//        try {
//            ceid = data.getSingleNumber("CollEventID");
//            Map panelMap = new HashMap();
//            if (ceid == 1L) {
//                panelMap.put("ControlState", FengCeConstant.CONTROL_OFFLINE);
//            } else if (ceid == 2L) {
//                panelMap.put("ControlState", FengCeConstant.CONTROL_LOCAL_ONLINE); //Online_Local               
//            } else if (ceid == 3L) {
//                panelMap.put("ControlState", FengCeConstant.CONTROL_REMOTE_ONLINE); //Online_Remote            
//            }
//            changeEquipPanel(panelMap);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        showCollectionsEventInfo(ceid);
    }// </editor-fold> 

//    @Override
//    @SuppressWarnings("unchecked")
//    public void processS6F11in(MsgDataHashtable data) {
//        long ceid = 0;
//        try {
//            if (data.get("CollEventId") != null) {
//                ceid = data.getSingleNumber("CollEventId");
//                logger.info("Received a s6f11in with CEID = " + ceid);
//            }
//            MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
//            byte[] ack = new byte[1];
//            ack[0] = 0;
//            out.put("AckCode", ack);
//            out.setTransactionId(data.getTransactionId());
//            mli.sendSecondaryOutputMessage(out);
//            this.setCommState(1);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
//        recipePath = this.getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + ppid + "/" + ppid + "_V" + recipe.getVersionNo() + ".txt";
//        recipePath = super.getRecipePathByConfig(recipe);
//        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
//        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
//        s7f5out.put("ProcessprogramID", recipeName);
//        MsgDataHashtable msgdata = null;
//        try {
//            msgdata = mli.sendPrimaryWsetMessage(s7f5out);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();

        TransferUtil.setPPBody(UUID.randomUUID().toString(), recipeType, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        recipeParaList = getRecipeParasByECSV();
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);

        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

    @Override
    public Object clone() {
        AOIHost newEquip = new AOIHost(deviceId, this.equipId,
                this.smlFilePath, this.localIPAddress,
                this.localTCPPort, this.remoteIPAddress,
                this.remoteTCPPort, this.connectMode,
                this.protocolType, this.deviceType, this.deviceCode, recipeType, this.iconPath);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.mli = this.mli;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.mli.addInputMessageListenerToAll(newEquip);
        this.setIsRestarting(isRestarting);
        this.clear();
        return newEquip;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendS1F13out() {
        MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f13out.setTransactionId(transactionId);
//        s1f13out.put("Mdln", Mdln);
//        s1f13out.put("SoftRev", SoftRev);
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s1f13out);
            if (data != null) {
                setCommState(1);
            }
        } catch (Exception e) {
            setCommState(0);
            logger.error("Exception:", e);
        }
    }

    @Override
    public Map sendS1F3SingleCheck(String svidName) {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3singleout", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] svid = new long[1];
        svid[0] = Long.parseLong(svidName);
        s1f3out.put("SVID", svid);
        MsgDataHashtable data = null;
        logger.info("设备" + deviceCode + "开始发送S1F3SingleCheck");
        try {
            data = sendMsg2Equip(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        if (list == null) {
            return null;
        }
        ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        Map resultMap = new HashMap();
        String svValue = String.valueOf(listtmp.get(0));
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Value", svValue);
        logger.info("resultMap=" + resultMap);
        return resultMap;
    }

    private void initRptPara() {
//        sendS2F41outPPselect("GD06-A20");
        sendS2F37outAll();
        //定义StripMapping上传事件
//        sendS2f33out(15399L, 269352993L);
//        sendS2f35out(15399L, 15399L, 15399L);
//        sendS2F37out(15399L);
    }
//public String selectSpecificRecipeSPI(String recipeName) {
//        Map resultMap = sendS2F41outPPselect(recipeName);
//        if (resultMap != null && !resultMap.isEmpty()) {
//            if ("0".equals(String.valueOf(resultMap.get("HCACK"))) || "4".equals(String.valueOf(resultMap.get("HCACK")))) {
//                UiLogUtil.appendLog2EventTab(equipHost.getDeviceCode(), "PPSelect成功，PPID=" + recipeName);
//                return "0";
//            } else {
//                Map eqptStateMap = equipHost.findEqptStatus();//失败上报机台状态
//                UiLogUtil.appendLog2EventTab(equipHost.getDeviceCode(), "选中Recipe失败，PPID=" + recipeName + "；原因：" + String.valueOf(resultMap.get("Description")) + "，机台状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
//                return "选中Recipe失败，PPID=" + recipeName + "，原因：" + String.valueOf(resultMap.get("Description") + "，设备状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
//                  return "选中失败";
//            }
//        } else {
//            return "选中Recipe失败，PPID=" + recipeName + ", 设备消息回复错误，请联系CIM人员处理";
//        }

//    }
    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map map = this.sendS2f41Cmd("STOP");//Map map = this.sendS2f41Cmd("LOCK");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
            }
            return map;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", 0);
        logger.info("此设备默认不进行下载，直接通过");
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", 0);
        logger.info("此设备默认不进行下载，直接通过");
        return resultMap;
    }

}
