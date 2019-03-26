package cn.tzauto.octopus.secsLayer.equipImpl.esec.db;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;

import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MessageArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.MsgDataHashtable;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;

public class EsecDB2100Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(EsecDB2100Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;

    //private Object synS2F41 = null;
    public EsecDB2100Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public EsecDB2100Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.equipId);
        while (!this.isInterrupted()) {
            try {
                while (!this.isJsipReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    initRptPara();
                }
                //设备在下一个可能停止的点才能停止
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                MsgDataHashtable msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equals("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgTagName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
               
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
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
//                processS6F11inStripMapUpload(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("s6f11inCommon")) {
                processS6F11in(data);
            } else if (tagName.equals("s6f11EquipStatusChange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
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

    public String initRptPara() {
        try {
            logger.debug("initRptPara+++++++++++++++++++");
            //发送s2f33
            String ack = "";
            long rptid = 1001l;
            long vid = 269352993l;
            long ceid = 15338l;
            ack = sendS2F33out(rptid, vid);//15339

            if (!"".equals(ack)) {
                ack = "";
                rptid = 1002l;
                ack = sendS2F33out(rptid, vid);//15338
            }
            if (!"".equals(ack)) {
                ack = "";
                rptid = 1003l;
                vid = 269352995l;
                ack = sendS2F33out(rptid, vid);//15328
            }

            //SEND S2F35
            if (!"".equals(ack)) {
                ack = "";
                ceid = 15339l;
                rptid = 1001l;
                ack = sendS2F35out(ceid, rptid);//15339 1001
            }
            if (!"".equals(ack)) {
                ack = "";
                ceid = 15338l;
                rptid = 1002l;
                ack = sendS2F35out(ceid, rptid);//15339 1001
            }
            if (!"".equals(ack)) {
                ack = "";
                ceid = 15328l;
                rptid = 1003l;
                ack = sendS2F35out(ceid, rptid);//15339 1001
            }
            sendS2f33out(3255L, 2031L, 2009L, 2028L);
            sendS2f35out(3255L, 3255L, 3255L);
            //SEND S2F37
            if (!"".equals(ack)) {
                sendS2F37outAll();
            }
            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
//            java.util.logging.Logger.getLogger(EsecDB2100Host.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception:", ex);
            return "0";
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3statecheck", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] equipStatuss = new long[1];
        long[] pPExecNames = new long[1];
        long[] controlStates = new long[1];
        MsgDataHashtable data = null;
        try {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
            sqlSession.close();
            s1f3out.put("EquipStatus", equipStatuss);
            s1f3out.put("PPExecName", pPExecNames);
            s1f3out.put("ControlState", controlStates);
            data = mli.sendPrimaryWsetMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(listtmp.get(0).toString(), deviceType);
        ppExecName = (String) listtmp.get(1);
        ppExecName = ppExecName.replaceAll(".dbrcp", "");
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">
    @SuppressWarnings("unchecked")
    public String sendS2F33out(long rptid, long vid) {
        //MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out",  mli.getDeviceId());
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f33out.setTransactionId(transactionId);
        long[] dataid = new long[1];
        dataid[0] = 1001l;
        long[] reportid = new long[1];
        reportid[0] = rptid;
        long[] variableid = new long[1];
        variableid[0] = vid;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("VariableID", variableid);
        //s1f13out.put("SoftRev", "9.25.5");
        try {
            MsgDataHashtable s2f34in = mli.sendPrimaryWsetMessage(s2f33out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public String sendS2F35out(long ceid, long rptid) {
        //MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out",  mli.getDeviceId());
        MsgDataHashtable s2f35out = new MsgDataHashtable("s2f35out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f35out.setTransactionId(transactionId);
        long[] dataid = new long[1];
        dataid[0] = 1001;
        long[] eventid = new long[1];
        eventid[0] = ceid;
        long[] reportid = new long[1];
        reportid[0] = rptid;
        s2f35out.put("DataID", dataid);
        s2f35out.put("CollEventID", eventid);
        s2f35out.put("ReportID", reportid);
        //s1f13out.put("SoftRev", "9.25.5");
        try {
            MsgDataHashtable s2f34in = mli.sendPrimaryWsetMessage(s2f35out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37out(long pceid) {
        //MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out",  mli.getDeviceId());
        MsgDataHashtable s2f37out = new MsgDataHashtable("s2f37out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37out.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = true;
        long[] ceid = new long[1];
        ceid[0] = pceid;
        s2f37out.put("Booleanflag", flag);
        s2f37out.put("CollEventId", ceid);;
        try {
            mli.sendPrimaryWsetMessage(s2f37out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F15outLotSizeAssign(String lotId, int lotQuantity) {
        MsgDataHashtable out = new MsgDataHashtable("s2f15out", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] u1 = new long[1];
        u1[0] = 96; //ECID = 96
        out.put("EC096", u1);
        out.put("NextLotId", lotId);

        long[] u2 = new long[1];
        u2[0] = 97;  //ECID  = 97
        out.put("EC097", u2);
        long[] u3 = new long[1];
        u3[0] = lotQuantity;
        out.put("NextLotQuantity", u3);

        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS2f16in(MsgDataHashtable in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f16in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("EAC")).getData();
        System.out.println();
        //System.out.println("CPNAme[0] = " + cpName);
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect(String recipeName) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outPPSelect", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName + ".dbrcp");
        byte[] hcack = new byte[1];
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                this.setAlarmState(2);
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    @Override
    protected void processS6F11EquipStatusChange(MsgDataHashtable data) {
        long ceid = 0L;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            controlState = ACKDescription.describeControlState(data.getSingleNumber("ControlState"), deviceType);
            ppExecName = ppExecName.replace(".dbrcp", "");
            preEquipStatus = equipStatus;
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
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑&&
            if (dataReady && equipStatus.equalsIgnoreCase("run") && preEquipStatus.equalsIgnoreCase("stopped RDY")) {
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
                            holdDeviceAndShowDetailInfo(" There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            this.setAlarmState(0);
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
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
//                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
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

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        MsgDataHashtable s7f1out = new MsgDataHashtable("s7f1out", mli.getDeviceId());
        s7f1out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName + ".dbrcp");
        s7f1out.put("Length", length);
        MsgDataHashtable data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = mli.sendPrimaryWsetMessage(s7f1out);
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
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        MsgDataHashtable data = null;
        MsgDataHashtable s7f3out = new MsgDataHashtable("s7f3out", mli.getDeviceId());
        s7f3out.setTransactionId(mli.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName + ".dbrcp");
        s7f3out.put("Processprogram", secsItem);
        try {
            data = mli.sendPrimaryWsetMessage(s7f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        recipeName = recipeName.replace(".dbrcp", "");
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = null;
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
            TransferUtil.setPPBody(ppbody, recipeType, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析      
            recipeParaList = getRecipeParasByECSV();
            //设备发过来的参数部分为科学计数法，这里转为一般的
            recipeParaList = recipeParaBD2Str(recipeParaList);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeNameMapping", null);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        MsgDataHashtable s7f17out = new MsgDataHashtable("s7f17out", mli.getDeviceId());
        s7f17out.setTransactionId(mli.getNextAvailableTransactionId());
        recipeName = recipeName + ".dbrcp";
        s7f17out.put("ProcessprogramID", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            if (ackc7[0] == 0) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S14FX Code"> 

    // </editor-fold> 
    @Override
    public Object clone() {
        EsecDB2100Host newEquip = new EsecDB2100Host(deviceId, this.equipId,
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
        this.clear();
        return newEquip;
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void sendUphData2Server() {

    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "正在使用预下载的Recipe,下载取消!";
        }
        return "0";
    }

    @Override
    public void sendS5F3out(boolean enable) {
        MsgDataHashtable s5f3out = new MsgDataHashtable("s5f3allout", mli.getDeviceId());
        s5f3out.setTransactionId(mli.getNextAvailableTransactionId());
        byte[] aled = new byte[1];
        boolean[] flag = new boolean[1];
        flag[0] = enable;
        if (enable) {
            aled[0] = -128;
        } else {
            aled[0] = 0;
        }
        s5f3out.put("ALED", aled);
        try {
            mli.sendPrimaryWsetMessage(s5f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
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
}
