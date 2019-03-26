package cn.tzauto.octopus.secsLayer.equipImpl.hitachi.da;

import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandParaPair;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.common.resolver.hitachi.DB800Util;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.exception.HsmsProtocolNotSelectedException;
import cn.tzinfo.smartSecsDriver.exception.NoConnectionException;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MessageArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.MsgDataHashtable;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import com.alibaba.fastjson.JSONArray;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 * @author NJTZ
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-3-25
 * @(#)EquipHost.java
 *
 * @Copyright tzinfo, Ltd. 2016. This software and documentation contain
 * confidential and proprietary information owned by tzinfo, Ltd. Unauthorized
 * use and distribution are prohibited. Modification History: Modification Date
 * Author Reason class Description
 */
public class HTDB800Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(HTDB800Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    private boolean canDownladMap = true;
    private String holdDesc = "";

    //private Object synS2F41 = null;
    public HTDB800Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public HTDB800Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
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
                if (this.getCommState() != HTDB800Host.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();//
                    //获取lot号
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                }
                MsgDataHashtable msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgTagName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {

                logger.fatal("Caught Interruption", e);
            }
        }
    }

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
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6f11Common(data);
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11EquipStatusChange")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f3in")) {
                processS14f3in(data);
            } else if (tagName.equalsIgnoreCase("s9f9Timeout")) {
                //接收到超时，直接不能下载
                this.canDownladMap = false;
                //或者重新发送参数
                initRptPara();
            } else if (tagName.equalsIgnoreCase("s9f9DataToLong")) {
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
            sendS2F37outCloseAll();
            logger.info("initRptPara+++++++++++++++++++");
            //重新定义processing INIT·SETUP·READY·EXECUTING·PAUSE·ERROR· status
            sendS2f33out(10002L, 50062L, 50070L);//50062,50070
            long[] ceids2 = new long[7];
            ceids2[0] = 3L;
            ceids2[1] = 4L;
            ceids2[2] = 5L;
            ceids2[3] = 6L;
            ceids2[4] = 7L;
            ceids2[5] = 8L;
            ceids2[6] = 80L;
//            sendS2F35outMuilt(ceids2, 10002L);
            for (int i = 0; i < ceids2.length; i++) {
                sendS2F35out(ceids2[i], 10002L);
            }
            sendS2F37outMuilt(true, ceids2);
            //发送s2f33
            String ack = "";
            long rptid = 10001l;
            long vid = 50200l;
            long ceid = 115l;
            ack = sendS2F33out(rptid, vid);//115

            //SEND S2F35
            if (!"".equals(ack)) {
                ack = "";
                ack = sendS2F35out(ceid, rptid);//115 10001
            }

            //SEND S2F37
            if (!"".equals(ack)) {
                sendS2F37out(ceid);
            }
            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
            logger.error("Exception:", ex);
            return "0";
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    public void processS1F1in(MsgDataHashtable data) {
        try {
            MsgDataHashtable s1f2out = new MsgDataHashtable("s1f2out", mli.getDeviceId());
            //String mdln = "SteveLan";
//            s1f2out.put("Mdln", mdln);
//            String softRev = "01.114/04";
//            s1f2out.put("SoftRev", softRev);
            s1f2out.setTimeStamp(new Date());
            s1f2out.setTransactionId(data.getTransactionId());
//            s1f2out.put("Mdln", Mdln);
//            s1f2out.put("SoftRev", SoftRev);
            mli.sendSecondaryOutputMessage(s1f2out);
            if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                this.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F13out() {
        MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13outListZero", mli.getDeviceId());
        s1f13out.setTransactionId(mli.getNextAvailableTransactionId());
//        s1f13out.put("Mdln", Mdln);
//        s1f13out.put("SoftRev", SoftRev);
        try {
            mli.sendPrimaryWsetMessage(s1f13out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    /**
     * Insert the method's description here. Creation date: (11/17/2001 12:11:06
     * PM)
     */
    public void processS1F14in(MsgDataHashtable s1f14in) {
        if (s1f14in == null) {
            return;
        }
        this.setCommState(this.COMMUNICATING);
        logger.info("-----Received s1f14in----.");
        if (this.getCommState() != this.COMMUNICATING) {
            this.setCommState(this.COMMUNICATING);
        }
    }

    /**
     * Insert the method's description here. Creation date: (11/12/01 3:01:56
     * PM)
     */
    /**
     * Insert the method's description here. Creation date: (11/12/01 3:01:56
     * PM)
     */
    public void processS1F2in(MsgDataHashtable s1f2in) {
        if (s1f2in == null) {
            return;
        }
        Mdln = (String) ((SecsItem) s1f2in.get("Mdln")).getData();
        SoftRev = (String) ((SecsItem) s1f2in.get("SoftRev")).getData();
        long transactionId = s1f2in.getTransactionId();
        logger.info("processS1F2in Mdln = " + Mdln);
        logger.info("processS1F2in SoftRev = " + SoftRev);
        logger.info("processS1F2in transactionId = " + transactionId);
        if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
            this.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
        }
    }

    public void processS1F13in(MsgDataHashtable data) {
        try {
            MsgDataHashtable s1f14out = new MsgDataHashtable("s1f14out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s1f14out.put("AckCode", ack);
            s1f14out.setTimeStamp(new Date());
            s1f14out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(s1f14out);
            logger.info("s1f14out sended.");
            if (this.getCommState() != this.COMMUNICATING) {
                this.setCommState(this.COMMUNICATING);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    @SuppressWarnings("unchecked")
    public String sendS2F33out(long rptid, long vid) {

        //MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out",  mli.getDeviceId());
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33out", mli.getDeviceId());

        s2f33out.setTransactionId(mli.getNextAvailableTransactionId());
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
        MsgDataHashtable s2f35out = new MsgDataHashtable("s2f35out", mli.getDeviceId());

        s2f35out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = 1001l;
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

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    public void processS6f11Common(MsgDataHashtable data) {
        try {

            MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
            setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            if (deviceType.contains("Z1")) {
                ppExecName = "QFN@" + ppExecName;
            }
            if (deviceType.contains("Z2")) {
                ppExecName = "BGA@" + ppExecName;
            }
            if (ceid == 80) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Recipe切换为" + ppExecName);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            String startCheckMod = deviceInfoExt.getStartCheckMod();
            boolean checkResult = false;
            if (equipStatus.equalsIgnoreCase("run")) {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                } else {
                    if (!"".equals(holdDesc) && holdFlag) {
                        holdDeviceAndShowDetailInfo(holdDesc);
                    }
                }
            }

            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (equipStatus.equalsIgnoreCase("READY")) {
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
                if (startCheckMod != null && !"".equals(startCheckMod)) {
                    checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                    if (!checkResult) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
//                            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");
                        if ("Y".equals(deviceInfoExt.getLockSwitch())) {
                            sendTerminalMsg2EqpSingle("The current recipe " + ppExecName + " in equipment is different from CIM system " + deviceInfoExt.getRecipeName() + ",equipment will be locked.");
                            holdFlag = true;
                        }
                    } else {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        this.setAlarmState(0);
                        holdFlag = false;
                    }
                }
                //根据检查模式执行开机检查逻辑
                //1、A1-检查recipe名称是否一致
                //2、A-检查recipe名称和参数
                //3、B-检查SV
                //4、AB都检查

                if ("A".equals(startCheckMod)) {
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
                            holdDesc = "There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.";
                            holdFlag = true;
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                            this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                        }
                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
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
    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param recipe
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long[] length = new long[1];
        //length[0] = TransferUtil.getPPLength(localFilePath);
        length[0] = 1024L;
        MsgDataHashtable s7f1out = new MsgDataHashtable("s7f1out", mli.getDeviceId());
        s7f1out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName);
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

    /**
     * 下载Recipe，将原有的recipe使用指定的PPID下载到机台
     *
     * @param recipe
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error("InterruptedException:", e);
        }
        MsgDataHashtable data = null;
        MsgDataHashtable s7f3out = new MsgDataHashtable("s7f3out", mli.getDeviceId());
        s7f3out.setTransactionId(mli.getNextAvailableTransactionId());
        //byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        byte[] ppbody = {1};
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName);
        s7f3out.put("Processprogram", secsItem);
        try {
            data = mli.sendPrimaryWsetMessage(s7f3out);
        } catch (Exception e) {
            logger.error("Exception", e);
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
        Recipe recipe = setRecipe(recipeName);
        //获取本地FTP地址
        recipePath = GlobalConstants.DB800HSDFTPPath + recipe.getRecipeName() + ".tgz";
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        MsgDataHashtable msgdata = null;
        try {
            msgdata = mli.sendPrimaryWsetMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (msgdata.get("Processprogram") == null) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "获取Recipe信息失败!");
            return null;
        }
        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        String recipeSecsGemPath = super.getRecipePathByConfig(recipe);
        TransferUtil.setPPBody(ppbody, recipeType, recipeSecsGemPath);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error("InterruptedException:", e);
        }
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            Map paraMap = DB800Util.transferFromFile(recipePath);
            recipeParaList = DB800Util.transferFromDB(paraMap, deviceType);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        Map resultMap = new HashMap();
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
        MsgDataHashtable s7f17out = new MsgDataHashtable("s7f17out", mli.getDeviceId());
        s7f17out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s7f17out);
            logger.info("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            if (ackc7[0] == 0) {
                logger.info("The recipe " + recipeName + " has been delete from " + deviceCode);
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
    @SuppressWarnings("unchecked")
    public void processS14f3in(MsgDataHashtable data) {
        try {
            MsgDataHashtable out = new MsgDataHashtable("s14f4out", mli.getDeviceId());
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold> 

    @Override
    public Object clone() {
        HTDB800Host newEquip = new HTDB800Host(deviceId, this.equipId,
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

    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            super.sendS2f41Cmd("STOP");
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

    //释放机台
    @Override
    public Map releaseDevice() {
        holdFlag = false;
        this.setAlarmState(0);
//        Map map = this.sendS2f41Cmd("START");//RELEASE
//        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
//            this.setAlarmState(0);
//        }
        return null;
    }

    @Override
    public void initRemoteCommand() {
        //设置commandParalist
        List<CommandParaPair> paraList = new ArrayList<>();
        CommandParaPair cpp = new CommandParaPair();
        cpp.setCpname("");
        cpp.setCpval("");
        paraList.add(cpp);
        //设置commandKey
        String commandKey = "start";
        CommandDomain startCommand = new CommandDomain();
        startCommand.setRcmd("START");
        startCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, startCommand);

        commandKey = "stop";
        CommandDomain stopCommand = new CommandDomain();
        stopCommand.setRcmd("STOP");
        stopCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, stopCommand);

        commandKey = "pause";
        CommandDomain pauseCommand = new CommandDomain();
        pauseCommand.setRcmd("PAUSE");
        pauseCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, pauseCommand);

        commandKey = "resume";
        CommandDomain resumeCommand = new CommandDomain();
        resumeCommand.setRcmd("RESUME");
        resumeCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, resumeCommand);
        //调用父类的方法，生成公用命令，如果不支持，可以删掉，如果不公用，直接覆盖
        //initCommonRemoteCommand();
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            UiLogUtil.appendLog2EventTab(deviceCode, "正在使用预下载的Recipe,下载取消.");
            return "下载取消";
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

    public boolean testInitLink() throws HsmsProtocolNotSelectedException, NoConnectionException {
        try {
            MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13outListZero", mli.getDeviceId());
            long transactionId = mli.getNextAvailableTransactionId();
            s1f13out.setTransactionId(transactionId);
            MsgDataHashtable s1f14in = mli.sendPrimaryWsetMessage(s1f13out);

//            setCommState(1);
            logger.info("testInitLink成功 建立连接、通信正常 ");
            return true;
        } catch (HsmsProtocolNotSelectedException e) {
            e.printStackTrace();
            logger.error("Exception:", e);
            throw new HsmsProtocolNotSelectedException("HsmsProtocolNotSelectedException");
        } catch (NoConnectionException e) {
            e.printStackTrace();
            logger.error("Exception:", e);
            throw new NoConnectionException("NoConnectionException");
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }

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
            logger.info("Ready to send Message S1F3==============>" + JSONArray.toJSON(s1f3out));
            data = sendMsg2Equip(s1f3out);
        } catch (Exception e) {
            logger.error("Wait for get meessage directly error：" + e);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            return null;
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        ppExecName = String.valueOf(listtmp.get(1));
        if (deviceType.contains("Z1")) {
            ppExecName = "QFN@" + ppExecName;
        }
        if (deviceType.contains("Z2")) {
            ppExecName = "BGA@" + ppExecName;
        }
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        logger.info("START CHECK: BEGIN" + new Date());
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        logger.info("START CHECK: ready to upload recipe:" + new Date());
        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        logger.info("START CHECK: transfer recipe over :" + new Date());
        logger.info("START CHECK: ready to check recipe para:" + new Date());
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        logger.info("START CHECK: check recipe para over :" + new Date());
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            String checkRecultDesc = "";
            String eventDescEng = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为[" + recipePara.getParaCode() + "],参数名:[" + recipePara.getParaName() + "]其异常设定值为[" + recipePara.getSetValue() + "],默认值为[" + recipePara.getDefValue() + "]"
                            + "其最小设定值为[" + recipePara.getMinValue() + "],其最大设定值为[" + recipePara.getMaxValue() + "]";
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                    checkRecultDesc = checkRecultDesc + eventDesc;
                    String eventDescEngTmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "";
                    eventDescEng = eventDescEng + eventDescEngTmp;
                }
//                this.holdDeviceAndShowDetailInfo("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng);
                //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
                holdDesc = "Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng;
                holdFlag = true;
            } else {
                this.releaseDevice();
                holdDesc = "";
                holdFlag = false;
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
                checkRecultDesc = eventDesc;
            }
            mqMap.put("eventDesc", checkRecultDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        FtpUtil.uploadFile(GlobalConstants.DB800HSDFTPPath + recipe.getRecipeName() + ".tgz",
                GlobalConstants.getProperty("ftpPath") + remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt",
                GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd
        );
        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
        return true;
    }
}
