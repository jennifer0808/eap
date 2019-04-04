package cn.tzauto.octopus.secsLayer.equipImpl.hitachi.da;

import cn.tfinfo.jcauto.octopus.biz.alarm.service.AutoAlter;
import cn.tfinfo.jcauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tfinfo.jcauto.octopus.biz.device.service.DeviceService;
import cn.tfinfo.jcauto.octopus.biz.monitor.service.MonitorService;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.Recipe;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipePara;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tfinfo.jcauto.octopus.biz.recipe.service.RecipeService;
import cn.tfinfo.jcauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tfinfo.jcauto.octopus.common.globalConfig.GlobalConstants;
import cn.tfinfo.jcauto.octopus.common.util.ftp.FtpUtil;
import cn.tfinfo.jcauto.octopus.common.ws.AxisUtility;
import cn.tfinfo.jcauto.octopus.common.ws.WSUtility;
import cn.tfinfo.jcauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandParaPair;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.hitachi.DB800Util;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.exception.HsmsProtocolNotSelectedException;
import cn.tzinfo.smartSecsDriver.exception.NoConnectionException;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MsgArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.DataMsgMap;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

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
    private boolean startCheckPass = true;
    private boolean recipeParaChange = false;
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;
    private boolean ppselectDoneFlag = false;

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
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {

            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                while (this.getCommState() != HTDB800Host.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
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
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
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
                long ceid = data.getSingleNumber("CollEventID");
                if (ceid == 26) {
                    processS6F11SpecialEvent(data);
                }
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
                //接受到切换recipe成功，置ppselectDoneFlag为true
                long ceid = data.getSingleNumber("CollEventID");
                if (ceid == 80) {
                    ppselectDoneFlag = true;
                }
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s9f9Timeout")) {
                //接收到超时，直接不能下载
                this.canDownladMap = false;
                //或者重新发送参数
                initRptPara();
            } else if (tagName.equalsIgnoreCase("s9f9DataToLong")) {
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
            }else if (tagName.equalsIgnoreCase("s6f83in")){
                processS6F83in(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f3in")) {
                processS14f3in(data);
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
            sendS2f33out(10002L, 50062L, 50070L);//50062:当前状态,50070：当前recipe
            long[] ceids2 = new long[7];
            ceids2[0] = 3L;
            ceids2[1] = 4L;
            ceids2[2] = 5L;
            ceids2[3] = 6L;
            ceids2[4] = 7L;
            ceids2[5] = 8L;
            ceids2[6] = 80L;
            //ceids2[7] = 88L;
            for (int i = 0; i < ceids2.length; i++) {
                sendS2F35out(ceids2[i], 10002L);
            }
            sendS2F37outMuilt(true, ceids2);
            //单独开启CEID=88(参数改变事件)，以防部分机台未升级
            sendS2F35out(88L, 10002L);
            sendS2F37out(88L);
            //开启CEID==27(BONDING INSPECTION事件)
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
            sendS2F37out(26L);
            sendS2F37out(27L);
            //开启Mapping事件报告
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
    public void processS1F1in(DataMsgMap data) {
        try {
            DataMsgMap s1f2out = new DataMsgMap("s1f2out", activeWrapper.getDeviceId());
            //String mdln = "SteveLan";
//            s1f2out.put("Mdln", mdln);
//            String softRev = "01.114/04";
//            s1f2out.put("SoftRev", softRev);
            s1f2out.setTimeStamp(new Date());
            s1f2out.setTransactionId(data.getTransactionId());
//            s1f2out.put("Mdln", Mdln);
//            s1f2out.put("SoftRev", SoftRev);
            activeWrapper.respondMessage(s1f2out);
            if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                this.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
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

    /**
     * Insert the method's description here. Creation date: (11/17/2001 12:11:06
     * PM)
     */
    public void processS1F14in(DataMsgMap s1f14in) {
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
    public void processS1F2in(DataMsgMap s1f2in) {
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

    public void processS1F13in(DataMsgMap data) {
        try {
            DataMsgMap s1f14out = new DataMsgMap("s1f14out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s1f14out.put("AckCode", ack);
            s1f14out.setTimeStamp(new Date());
            s1f14out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(s1f14out);
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

        //DataMsgMap s1f13out = new DataMsgMap("s1f13out",  activeWrapper.getDeviceId());
        DataMsgMap s2f33out = new DataMsgMap("s2f33out", activeWrapper.getDeviceId());

        s2f33out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
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
            DataMsgMap s2f34in = activeWrapper.sendAwaitMessage(s2f33out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public String sendS2F35out(long ceid, long rptid) {
        DataMsgMap s2f35out = new DataMsgMap("s2f35out", activeWrapper.getDeviceId());

        s2f35out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
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
            DataMsgMap s2f34in = activeWrapper.sendAwaitMessage(s2f35out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "";
        }
    }

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            //选中成功标识
            if (data != null) {
                ppselectFlag = true;
            }
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    public void processS6f11Common(DataMsgMap data) {
        try {

            DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    private void processS6F11SpecialEvent(DataMsgMap data) {
        long ceid = 0L;
        String event = "";
        try {
            ceid = data.getSingleNumber("CollEventID");
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
            UiLogUtil.appendLog2EventTab(deviceCode, "检测到设备触发[" + event + "]事件,设备即将被锁!请联系ME进行检查!");
            // TODO 需要检查下MES状态，判断是否需要发送锁机指令
            //sendS2F15outLearnDevice();

            Map resultMap = new HashMap();
            resultMap.put("msgType", "s5f1");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("deviceId", deviceId);
            resultMap.put("ALID", "DB800HSD00" + ceid);
            resultMap.put("ALCD", 0);
            resultMap.put("ALTX", event);
            resultMap.put("Description", "Other categories");
            resultMap.put("TransactionId", data.getTransactionId());
            AutoAlter.alter(resultMap);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            if (ceid == 80) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Recipe切换为" + ppExecName);
            }
            if (ceid == 88L) {
                UiLogUtil.appendLog2EventTab(deviceCode, "检测到recipe参数被修改,开机时将执行参数检查...");
                //reciep参数修改事件
                recipeParaChange = true;
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
            if (equipStatus.equalsIgnoreCase("run")) {
                //TODO 校验2D的开关是否已经开启，若关闭弹窗显示
                List<String> svlist = new ArrayList<>();
                svlist.add("54121");//2D开关
                Map svValue = this.getSpecificSVData(svlist);
                if (svValue.get("54121").equals("0")) {
                    String dateStr = GlobalConstants.dateFormat.format(new Date());
                    this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + "2D Mark has already been closed!!");
                    UiLogUtil.appendLog2EventTab(deviceCode, "2D已被关闭！");
                }
            }
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                sqlSession.close();
                return;
            }
            if (equipStatus.equalsIgnoreCase("run")) {
                if (holdFlag || this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "设备已被锁");
                    //此时所正式锁机
                    holdDeviceAndShowDetailInfo("Host hold the equipment,you can see the detail log from Host");
                }
            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("READY")) {
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

                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdFlag = true;
                } else {
                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查
                    //不管怎样都比对recipe
                    checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                    if (!checkResult) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                        checkNameFlag = false;
                    } else {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        checkNameFlag = true;
                    }
                    //参数比对
                    if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机检查模式!");
                    }
                    if (true) {
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
                                    checkParaFlag = false;
                                } else {
                                    UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                    checkParaFlag = this.startCheckRecipeParaReturnFlag(downLoadGoldRecipe.get(0));
                                    if (!checkParaFlag) {
                                        sendStatus2Server("LOCK");
                                    }
                                }
                            }
                        } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                            //如果未设置参数比对模式，默认参数比对通过
                            checkParaFlag = true;
                            UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
                        }
                    } else {
                        //如果参数未改变，默认参数比对通过
                        checkParaFlag = true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
            //总结是否需要锁机
            if (checkNameFlag && checkParaFlag) {
                holdFlag = false;
            } else {
                holdFlag = true;
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
     * 开机check recipe参数
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
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
                checkParaFlag = false;
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                checkParaFlag = true;
                this.releaseDevice();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
            }
            mqMap.put("eventDesc", eventDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
            return checkParaFlag;
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
        //length[0] = TransferUtil.getPPLength(localFilePath);
        length[0] = 1024L;
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
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    /**
     * 下载Recipe，将原有的recipe使用指定的PPID下载到机台
     *
     * @param localRecipeFilePath
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
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        //byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        byte[] ppbody = {1};
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName);
        s7f3out.put("Processprogram", secsItem);
        try {
            data = activeWrapper.sendAwaitMessage(s7f3out);
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
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
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
    @Override
    protected void processS14F1in(DataMsgMap data) {
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
        UiLogUtil.appendLog2SeverTab(deviceCode, "设备请求下载Strip Map，StripId：[" + stripId + "]");
        DataMsgMap out = null;
        //通过Web Service获得xml字符串
        String stripMapData = WSUtility.binGet(stripId, deviceCode);
        if (stripMapData == null) {//stripId不存在  
            out = new DataMsgMap("s14f2outNoExist", activeWrapper.getDeviceId());
            out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
            long[] u1 = new long[1];
            u1[0] = 0;
            out.put("ObjectAck", u1);
            UiLogUtil.appendLog2SeverTab(deviceCode, "StripId：[" + stripId + "] Strip Map 不存在！");
        } else {//stripId存在
            String downLoadResult = stripMapData.substring(0, 1);
            if ("<".equals(downLoadResult)) {
                out = new DataMsgMap("s14f2out", activeWrapper.getDeviceId());
                out.put("StripId", stripId);
                out.put("MapData", stripMapData);
                UiLogUtil.appendLog2SeverTab(deviceCode, "从服务器下载Strip Map成功,StripId：[" + stripId + "]");
            } else {
                //是分号
                long errorCode = Long.valueOf(stripMapData.split(";")[0]);
                out = new DataMsgMap("s14f2outException", activeWrapper.getDeviceId());
                out.put("StripId", stripId);
                out.put("MapData", stripMapData);
                out.put("ErrCode", errorCode);
                out.put("ErrText", stripMapData);
                UiLogUtil.appendLog2SeverTab(deviceCode, "从服务器下载Strip Map失败,StripId：[" + stripId + "],失败原因：" + stripMapData);
            }
            out.setTransactionId(data.getTransactionId());
        }
        try {
            activeWrapper.respondMessage(out);
            UiLogUtil.appendLog2SeverTab(deviceCode, "发送Strip Map到设备,StripId：[" + stripId + "]");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS14f3in(DataMsgMap data) {
        try {
            DataMsgMap out = new DataMsgMap("s14f4out", activeWrapper.getDeviceId());
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold> 

    @Override
    public Object clone() {
        HTDB800Host newEquip = new HTDB800Host(deviceId, this.deviceCode,
                this.smlFilePath, this.localIPAddress,
                this.localTCPPort, this.remoteIPAddress,
                this.remoteTCPPort, this.connectMode,
                this.protocolType, this.deviceType, this.deviceCode, recipeType, this.iconPath);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
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
//        //不在RUN状态，假装已被锁
//        if (!"RUN".equalsIgnoreCase(equipStatus)) {
//            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//                Map resultMap = new HashMap();
//                resultMap.put("msgType", "s2f42");
//                resultMap.put("deviceCode", deviceCode);
//                resultMap.put("prevCmd", "STOP");
//                resultMap.put("HCACK", 0);
//                resultMap.put("Description", "设备已被锁,将无法开机");
//                holdFlag = true;
//                startCheckPass = false;
//                recipeParaChange = false;
//                return resultMap;
//            }
//        }
        Map resultMap = null;
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            if (!"RUN".equalsIgnoreCase(equipStatus)) {
                //不在RUN状态，假装已被锁,修改标识
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
            //修改锁机标识
            holdFlag = true;
            //重置标识
            startCheckPass = false;
            //hold机后，参数设为未改变，强制现场再次修改参数
            recipeParaChange = false;
            return resultMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            return resultMap;
        }
    }

    //释放机台
    @Override
    public Map releaseDevice() {
        holdFlag = false;
        startCheckPass = true;
        recipeParaChange = false;
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
            return "1";
        }
        return "0";
    }

    @Override
    public void sendS5F3out(boolean enable) {
        DataMsgMap s5f3out = new DataMsgMap("s5f3allout", activeWrapper.getDeviceId());
        s5f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
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
            activeWrapper.sendAwaitMessage(s5f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public boolean testInitLink() throws HsmsProtocolNotSelectedException, NoConnectionException {
        try {
            DataMsgMap s1f13out = new DataMsgMap("s1f13outListZero", activeWrapper.getDeviceId());
            s1f13out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
            DataMsgMap s1f14in = activeWrapper.sendAwaitMessage(s1f13out);
            logger.info("testInitLink成功 建立连接、通信正常 ");
            setCommState(1);
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

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // 上传ftpGlobalConstants.getProperty("ftpPath") +
        localRcpPath = GlobalConstants.DB800HSDFTPPath + recipe.getRecipeName() + ".tgz";
        FtpUtil.uploadFile(localRcpPath, remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
        return true;
    }
}