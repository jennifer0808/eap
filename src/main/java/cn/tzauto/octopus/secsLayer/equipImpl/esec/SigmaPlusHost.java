/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.esec;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.besi.Sigma8800RecipeUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MessageArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.MsgDataHashtable;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
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
    public SigmaPlusHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public SigmaPlusHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
    }

    @Override
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.equipId);
        while (!isInterrupted) {

            try {
                while (!this.isJsipReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
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
                MsgDataHashtable msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s12f3in")) {
                    processS12F3in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s12f15in")) {
                    processS12F15in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equals("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgTagName()
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

    public void inputMessageArrived(MessageArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = new Date().getTime();
            secsMsgTimeoutTime = 0;
            MsgDataHashtable data = event.removeMessageFromQueue();
            long transactionId = data.getTransactionId();
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("S12F3in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("S12F15in")) {
                this.inputMsgQueue.put(data);
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

    @SuppressWarnings("unchecked")
    public void sendS2F33clear() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f33clear", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35clear() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f35clear", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    public void processS6F11in(MsgDataHashtable data) {
        long ceid = 0;
        try {

            MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
            this.setCommState(1);

            if (data.get("CollEventID") != null) {
                ceid = data.getSingleNumber("CollEventID");
                if (ceid == 2L) {
                    sendS2f41Cmd("START");
                    logger.info("Received event ceid = 2 need to send command START.");
                }
                if (ceid == 49L) {
                    processS6F11EquipStatusChange(data);
                }
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public String initRptPara() {
        try {
//            sendS2f33outDelete(4905L);
//            sendS2f35outDelete(4905L, 4905L);
            sendS2F33clear();
            sendS2F35clear();
//            sendS2F37outAll();
            sendS2F37outCloseAll();

            logger.debug("initRptPara+++++++++++++++++++");
            sendS2f33out(10004L, 10004L, 10007L, 10000L);
            sendS2f35out(49L, 49L, 10004L);

            sendS2F37out(49L);
            sendS2F37out(2L);

//            sendS2f33out(3255L, 2031L, 2009L, 2028L);
//            sendS2f35out(3255L, 3255L, 3255L);
            //SEND S2F37
            //StripMapping事件定义
//            sendS2f33out(403L, 290L, 738L);
            sendS2f33out(403L, 290L);
            sendS2f35out(403L, 403L, 403L);
            sendS2F37out(403L);
            //Parameter参数获取事件定义
            sendS2f33out(50L, 4905L);
            sendS2f35out(50L, 50L, 50L);
            sendS2F37out(50L);
            //Parameter provider Event
//            sendS2F33outMutli(4905L, "4905");
            sendS2F33out(4905L, 4905L);
            sendS2f35out(4905L, 4905L, 4905L);
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
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3statecheck", mli.getDeviceId());
        s1f3out.setTransactionId(mli.getNextAvailableTransactionId());
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
        if (data == null || data.isEmpty()) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备状态信息失败，请检查设备通讯状态！");
            logger.error("获取设备:" + deviceCode + "状态信息失败.");
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(listtmp.get(0).toString(), deviceType);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        ppExecName = (String) listtmp.get(1);
        ppExecName = ppExecName.replaceAll(".dbrcp", "");
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);

        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">
    @SuppressWarnings("unchecked")
    public String sendS2F33out(long rptid, long vid) {
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
        try {
            MsgDataHashtable s2f34in = mli.sendPrimaryWsetMessage(s2f33out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "";
        }
    }

    public String sendS2F33outMutli(long rptid, String vid) {
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33outmutli", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f33out.setTransactionId(transactionId);
        long[] dataid = new long[1];
        dataid[0] = 1001l;
        long[] reportid = new long[1];
        reportid[0] = rptid;
//        long[] variableid = new long[1];
//        variableid[0] = vid;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("VariableID", "4905");
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
    public void sendS2F37outAll() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f37outAll", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = true;
        s2f37outAll.put("Booleanflag", flag);
        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    /*
     * whichOne = 1 => ascii; 2 => 1 byte unsigned; 3 => 2 byte unsigned; 4 => 4 byte unsigned
     */
    public void sendS2f41EpoxyVerificationAD838(int whichOne) {
        MsgDataHashtable out = new MsgDataHashtable("s2f41outAD838", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());

        out.put("RCMD", "LIV");
        out.put("CPnameResult", "Result");
        SecsItem sItem2 = null;
        int formatCode = 0;
        if (whichOne == 1) {
            sItem2 = new SecsItem("0", FormatCode.SECS_ASCII); //Verification Pass
        } else {
            if (whichOne == 2) {
                formatCode = FormatCode.SECS_1BYTE_UNSIGNED_INTEGER;
            } else if (whichOne == 3) {
                formatCode = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
            } else if (whichOne == 4) {
                formatCode = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
            }
            long[] u2 = new long[1];
            u2[0] = 0;  //Verification Pass
            sItem2 = new SecsItem(u2, formatCode);
        }

        out.put("CPvalResult", sItem2);

        out.put("CPnameErrorMessage", "ErrorMessage");
        out.put("CPvalErrorMessage", "");

        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    /*
     * whichOne = 1 => ascii; 2 => 1 byte unsigned; 3 => 2 byte unsigned; 4 => 4 byte unsigned
     */
    @SuppressWarnings("unchecked")
    public void sendS2f41EpoxyVerificationAD830(int whichOne, String stripCount, String lastFlag) {
        MsgDataHashtable out = new MsgDataHashtable("s2f41outAD830", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());

        out.put("RCMD", "LIV");
        out.put("CPnameResult", "Result");
        SecsItem sItem2 = null;
        int formatCode = 0;
        if (whichOne == 1) {
            sItem2 = new SecsItem("0", FormatCode.SECS_ASCII); //Verification Pass
        } else {
            if (whichOne == 2) {
                formatCode = FormatCode.SECS_1BYTE_UNSIGNED_INTEGER;
            } else if (whichOne == 3) {
                formatCode = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
            } else if (whichOne == 4) {
                formatCode = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
            }
            long[] u2 = new long[1];
            u2[0] = 0;  //Verification Pass
            sItem2 = new SecsItem(u2, formatCode);
        }

        out.put("CPvalResult", sItem2);

        out.put("CPnameErrorMessage", "ErrorMessage");
        out.put("CPvalErrorMessage", "");

        out.put("CPnameStripCount", "StripCount");
        out.put("CPvalStripCount", stripCount);

        out.put("CPnameLastFlag", "LastFlag");
        out.put("CPvalLastFlag", lastFlag);

        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS2f41Stop() {
        MsgDataHashtable out = new MsgDataHashtable("s2f41outSTOP", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS2f41Start() {
        MsgDataHashtable out = new MsgDataHashtable("s2f41outSTART", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F15outParameter() {
        MsgDataHashtable out = new MsgDataHashtable("s2f15out", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
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
        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap = sendS1F3SingleCheck("14903");
        logger.info("sendS1F3SingleCheck" + resultMap);
//        resultMap = sendS1F3SingleCheck("14903");
        return resultMap;
    }

    public void processS2f16in(MsgDataHashtable in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f16in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("EAC")).getData();
        System.out.println();
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }

    public void sendS2F29outECID() {
        MsgDataHashtable out = new MsgDataHashtable("s2f29oneout", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] u1 = new long[1];
        u1[0] = 5012;
        out.put("ECID", u1);
        try {
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS2f30in(MsgDataHashtable in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f30in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("EAC")).getData();
        System.out.println();
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect(String recipeName) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outPPSelect", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        s2f41out.put("PPID", "Production/" + recipeName);
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

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    @Override
    protected void processS6F11EquipStatusChange(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
//            ppExecName = ppExecName.replace(".dbrcp", "");
//            preEquipStatus = equipStatus;
//            findDeviceRecipe();
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
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                    holdDeviceAndShowDetailInfo();
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
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");

                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            releaseDevice();
                            checkResult = true;
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
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0), "");
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
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    private Map s2f41stop() {
        MsgDataHashtable s2f41stop = new MsgDataHashtable("s2f41stopout", mli.getDeviceId());
        s2f41stop.setTransactionId(mli.getNextAvailableTransactionId());
        MsgDataHashtable data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = mli.sendPrimaryWsetMessage(s2f41stop);
            ppgnt = (byte[]) ((SecsItem) data.get("HCACK")).getData();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", ppgnt[0]);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
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
//                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
//                for (RecipePara recipePara : recipeParasdiff) {
//                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
//                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
//                }
////                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
//            } else {
//                this.releaseDevice();
//                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
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
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        MsgDataHashtable s7f1out = new MsgDataHashtable("s7f1out", mli.getDeviceId());
        s7f1out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", "Production/" + targetRecipeName);
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
        s7f3out.put("ProcessprogramID", "Production/" + targetRecipeName);
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
        resultMap.put("ppid", "Production/" + targetRecipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Map mapTemp = new HashMap();
        mapTemp = sendS1F3Check();
        Map resultMap = new HashMap();
        String rcpName = (String) mapTemp.get("PPExecName");
        logger.info("===================rcpName:" + rcpName);
        logger.info("===================recipeName:" + recipeName);
        if (!rcpName.contains(recipeName)) {
            UiLogUtil.appendLog2EventTab(deviceCode, "上传程序与设备当前程序不一致，请调整后再上传！");
            UiLogUtil.appendLog2SeverTab(deviceCode, "上传程序与设备当前程序不一致，请调整后再上传！");
            resultMap.put("checkResult", "Y");
            return resultMap;
        }
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", "Production/" + recipeName);
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
            Map map = new HashMap();
            map = sendS2F15outParameter();
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
        MsgDataHashtable s7f17out = new MsgDataHashtable("s7f17out", mli.getDeviceId());
        s7f17out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", "Production/" + recipeName);
        byte[] ackc7 = new byte[1];
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s7f17out);
            logger.debug("Request delete recipe " + "Production/" + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            if (ackc7[0] == 0) {
                logger.debug("The recipe " + "Production/" + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + "Production/" + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7, "ACKC7"));
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
    @Override
    public Object clone() {
        SigmaPlusHost newEquip = new SigmaPlusHost(deviceId, this.equipId,
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
//        throw new UnsupportedOperationException("Not supported yet.");
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

    @Override
    public void processS12F3in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f4out = new MsgDataHashtable("s12f4out", mli.getDeviceId());
            ArrayList<SecsItem> list = (ArrayList) ((SecsItem) msgDataHashtable.get("RESULT")).getData();
            ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            String MaterialID = String.valueOf(listtmp.get(0)).trim();
            logger.info("waferid：" + MaterialID);
            String IDTYP = String.valueOf(listtmp.get(1));
            byte[] IDTYPs = new byte[1];
            IDTYPs[0] = Byte.valueOf(IDTYP);
            String MapDataFormatType = String.valueOf(listtmp.get(2));
            FlatNotchLocation = String.valueOf(listtmp.get(3));
            long[] FlatNotchLocations = new long[1];
            FlatNotchLocations[0] = Long.valueOf(FlatNotchLocation);
            String FileFrameRotation = String.valueOf(listtmp.get(4));
            String OriginLocation = String.valueOf(listtmp.get(5));
            byte[] OriginLocations = new byte[1];
            OriginLocations[0] = Byte.valueOf(OriginLocation);
            String ProcessAxis = String.valueOf(listtmp.get(6));
            String BinCodeEquivalents = String.valueOf(listtmp.get(7));
            String NullBinCodeValue = String.valueOf(listtmp.get(8));
            long[] NullBinCodeValues = new long[1];
            NullBinCodeValues[0] = Long.valueOf(NullBinCodeValue);
            Map waferInfoMap = SigmaPlusWaferTransfer.getWaferFileInfo(MaterialID, FlatNotchLocation);
            s12f4out.put("MaterialID", MaterialID);
            s12f4out.put("IDTYP", IDTYPs);
            s12f4out.put("FlatNotchLocation", FlatNotchLocations);
            s12f4out.put("OriginLocation", OriginLocations);
            long[] RrferencePointSelects = new long[1];
            RrferencePointSelects[0] = 0L;
            s12f4out.put("RrferencePointSelect", RrferencePointSelects);
            SecsItem vRoot = new SecsItem();
            vRoot.setFormatCode(FormatCode.SECS_LIST);
//            s12f4out.put("REFPxREFPy", vRoot);
            s12f4out.put("DieUnitsOfMeasure", "");
            s12f4out.put("XAxisDieSize", RrferencePointSelects);
            s12f4out.put("YAxisDieSize", RrferencePointSelects);
            long[] RowCountInDieIncrementss = new long[1];
            RowCountInDieIncrementss[0] = Long.parseLong(String.valueOf(waferInfoMap.get("RowCountInDieIncrements")));
            s12f4out.put("RowCountInDieIncrements", RowCountInDieIncrementss);

            long[] ColumnCountInDieIncrementss = new long[1];
            ColumnCountInDieIncrementss[0] = Long.parseLong(String.valueOf(waferInfoMap.get("ColumnCountInDieIncrements")));
            s12f4out.put("ColumnCountInDieIncrements", ColumnCountInDieIncrementss);

            long[] ProcessDieCounts = new long[1];
            ProcessDieCounts[0] = Long.parseLong(String.valueOf(waferInfoMap.get("ProcessDieCount")));
            s12f4out.put("ProcessDieCount", ProcessDieCounts);
//            s12f4out.put("BinCodeEquivalents", );
            s12f4out.put("NullBinCodeValue", NullBinCodeValues);
            long[] MessageLengths = new long[1];
            MessageLengths[0] = 38L;
            s12f4out.put("MessageLength", MessageLengths);
            s12f4out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f4out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    @Override
    public void processS12F15in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f16ut = new MsgDataHashtable("s12f16out", mli.getDeviceId());
            byte[] IDTYP = ((byte[]) ((SecsItem) msgDataHashtable.get("IDTYP")).getData());
            String MaterialID = ((SecsItem) msgDataHashtable.get("MaterialID")).getData().toString();
            s12f16ut.put("MaterialID", MaterialID);
            s12f16ut.put("IDTYP", IDTYP);
            int[] STRPxSTRPy = new int[2];
            STRPxSTRPy[0] = 0;
            STRPxSTRPy[1] = 0;
            s12f16ut.put("STRPxSTRPy", STRPxSTRPy);
            long[] BinList = (long[]) SigmaPlusWaferTransfer.getWaferFileInfo(MaterialID, FlatNotchLocation).get("BinList");
            s12f16ut.put("BinList", new SecsItem(BinList, FormatCode.SECS_1BYTE_UNSIGNED_INTEGER));

            s12f16ut.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f16ut);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        MsgDataHashtable s7f19out = new MsgDataHashtable("s7f19out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s7f19out.setTransactionId(transactionId);
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s7f19out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("EPPD") == null) {
            data = this.getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("EPPD")).getData();
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ArrayList list1 = new ArrayList();
            for (Object object : listtmp) {
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

}
