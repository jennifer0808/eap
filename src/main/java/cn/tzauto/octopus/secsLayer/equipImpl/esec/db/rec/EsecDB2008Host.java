/**
 *
 *
 */
package cn.tzauto.octopus.secsLayer.equipImpl.esec.db.rec;

import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.common.ws.WSUtility;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author 贺从愿
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-3-25
 * @(#)EquipHost.java
 *
 * @Copyright tzinfo, Ltd. 2016. This software and documentation contain
 * confidential and proprietary information owned by tzinfo, Ltd. Unauthorized
 * use and distribution are prohibited. Modification History: Modification Date
 * Author Reason class Description
 */
public class EsecDB2008Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(EsecDB2008Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;

    public EsecDB2008Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
    }


    @Override
    public Object clone() {
        EsecDB2008Host newEquip = new EsecDB2008Host(deviceId,
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

                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    initRptPara();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
            LastComDate = new Date().getTime();
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
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else {
                logger.debug("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            sendS2f33out(4l, 2031L, 2009L);
            sendS2f35out(4L, 4L, 4L);
            //SEND S2F37
            if (!"".equals(ack)) {
                sendS2F37outAll();
            }
            return "1";

        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(EsecDB2008Host.class.getName()).log(Level.SEVERE, null, ex);
            return "0";
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    public void processS1F1in(DataMsgMap data) {
        try {
            DataMsgMap s1f2out = new DataMsgMap("s1f2outListZero", activeWrapper.getDeviceId());
            //String mdln = "SteveLan";
//            s1f2out.put("Mdln", Mdln);
//            s1f2out.put("SoftRev", SoftRev);
            s1f2out.setTimeStamp(new Date());
            s1f2out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(s1f2out);
            logger.debug("s1f2out sended.");
            if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                this.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F13out() {
        DataMsgMap s1f13out = new DataMsgMap("s1f13out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f13out.setTransactionId(transactionId);
        s1f13out.put("Mdln", Mdln);
        s1f13out.put("SoftRev", SoftRev);
        try {
            activeWrapper.sendAwaitMessage(s1f13out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Insert the method's description here. Creation date: (11/17/2001 12:11:06
     * PM)
     */
    public void processS1F14in(DataMsgMap s1f14in) {
        Map a = new HashMap();
        if (s1f14in == null) {
            return;
        }
        System.out.println("-----Received s1f14in----.");
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
        logger.debug("processS1F2in Mdln = " + Mdln);
        logger.debug("processS1F2in SoftRev = " + SoftRev);
        logger.debug("processS1F2in transactionId = " + transactionId);
        logger.debug("processS1F2in" + new Date());
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
            System.out.println("s1f14out sended.");
            if (this.getCommState() != this.COMMUNICATING) {
                this.setCommState(this.COMMUNICATING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    @SuppressWarnings("unchecked")
    public String sendS2F33out(long rptid, long vid) {
        //DataMsgMap s1f13out = new DataMsgMap("s1f13out",  activeWrapper.getDeviceId());
        DataMsgMap s2f33out = new DataMsgMap("s2f33out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
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
            DataMsgMap s2f34in = activeWrapper.sendAwaitMessage(s2f33out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F34in(DataMsgMap data) {
        System.out.println("----------Received s2f34in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        System.out.println();
        System.out.println("ackCode = " + ((ack == null) ? "" : ack[0]));

    }

    @SuppressWarnings("unchecked")
    public String sendS2F35out(long ceid, long rptid) {
        //DataMsgMap s1f13out = new DataMsgMap("s1f13out",  activeWrapper.getDeviceId());
        DataMsgMap s2f35out = new DataMsgMap("s2f35out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
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
            DataMsgMap s2f34in = activeWrapper.sendAwaitMessage(s2f35out);
            byte[] ack = (byte[]) ((SecsItem) s2f34in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F36in(DataMsgMap data) {
        System.out.println("----------Received s2f36in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        System.out.println();
        System.out.println("ackCode = " + ((ack == null) ? "" : ack[0]));
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37out(long pceid) {
        //DataMsgMap s1f13out = new DataMsgMap("s1f13out",  activeWrapper.getDeviceId());
        DataMsgMap s2f37out = new DataMsgMap("s2f37out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37out.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = true;
        long[] ceid = new long[1];
        ceid[0] = pceid;
        s2f37out.put("Booleanflag", flag);
        s2f37out.put("CollEventId", ceid);;
        try {
            activeWrapper.sendAwaitMessage(s2f37out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37outAll() {
        //DataMsgMap s1f13out = new DataMsgMap("s1f13out",  activeWrapper.getDeviceId());
        DataMsgMap s2f37outAll = new DataMsgMap("s2f37outAll", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = true;
        s2f37outAll.put("Booleanflag", flag);
        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F38in(DataMsgMap data) {
        System.out.println("----------Received s2f38in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        System.out.println();
        System.out.println("ackCode = " + ((ack == null) ? "" : ack[0]));
    }

    @SuppressWarnings("unchecked")
    /*
     * whichOne = 1 => ascii; 2 => 1 byte unsigned; 3 => 2 byte unsigned; 4 => 4 byte unsigned
     */
    public void sendS2f41EpoxyVerificationAD838(int whichOne) {
        DataMsgMap out = new DataMsgMap("s2f41outAD838", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());

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
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*
        SecsItem vRoot = new SecsItem();
        vRoot.setFormatCode(FormatCode.SECS_LIST);
        ArrayList rootData = new ArrayList();
        long[] u1 = new long[1];
        u1[0] = 100;
        SecsItem sItem1 = new SecsItem(u1, FormatCode.SECS_1BYTE_SIGNED_INTEGER);
        rootData.add(sItem1);
        
        SecsItem vnlRoot = new SecsItem();
        vnlRoot.setFormatCode(FormatCode.SECS_LIST);
        ArrayList rootnlData = new ArrayList();
        String u1nl = "Test Recursive Variable item.";
        SecsItem sItem1nl = new SecsItem(u1nl, FormatCode.SECS_ASCII);
        rootnlData.add(sItem1nl);
        long[] u100NL = new long[1];
        u100NL[0] = 22222;
        SecsItem sItem21NL = new SecsItem(u100NL, FormatCode.SECS_4BYTE_SIGNED_INTEGER);
        rootnlData.add(sItem21NL);
        vnlRoot.setData(rootnlData);
        rootData.add(vnlRoot);
        
        long[] u100 = new long[1];
        u100[0] = 555;
        SecsItem sItem21 = new SecsItem(u100, FormatCode.SECS_4BYTE_SIGNED_INTEGER);
        rootData.add(sItem21);
        vRoot.setData(rootData);//very important
        out.put("CPval[0]", vRoot);
        long[] u2 = new long[1];
        u2[0] = 99;
        SecsItem sItem2 = new SecsItem(u2, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER);
        out.put("CPval[1]", sItem2);
        long[] u3 = new long[1];
        u3[0] = 98;
        SecsItem sItem3 = new SecsItem(u3, FormatCode.SECS_8BYTE_SIGNED_INTEGER);
        out.put("CPval[2]", sItem3);
        out.put("CPname[0]", "something[0]");
        out.put("CPname[1]", "something[1]");
        out.put("CPname[2]", "something[2]");
        out.put("RemComCode", "1.1.1.1");
        try{
        activeWrapper.sendAwaitMessage(out);
        }catch(Exception e){
        e.printStackTrace();
        }
         */
    }

    /*
     * whichOne = 1 => ascii; 2 => 1 byte unsigned; 3 => 2 byte unsigned; 4 => 4 byte unsigned
     */
    @SuppressWarnings("unchecked")
    public void sendS2f41EpoxyVerificationAD830(int whichOne, String stripCount, String lastFlag) {
        DataMsgMap out = new DataMsgMap("s2f41outAD830", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());

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
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processS2f42in(DataMsgMap in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f42in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("HCACK")).getData();
        System.out.println();
        //System.out.println("CPNAme[0] = " + cpName);
        System.out.println("HCACK = " + ((value == null) ? "" : value[0]));
    }

    public void sendS2f41Stop() {
        DataMsgMap out = new DataMsgMap("s2f41outSTOP", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendS2f41Start() {
        DataMsgMap out = new DataMsgMap("s2f41outSTART", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F15outLotSizeAssign(String lotId, int lotQuantity) {
        DataMsgMap out = new DataMsgMap("s2f15out", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
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
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processS2f16in(DataMsgMap in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f16in---------");
        byte[] value = (byte[]) ((SecsItem) in.get("EAC")).getData();
        System.out.println();
        //System.out.println("CPNAme[0] = " + cpName);
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S3FX Code">
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S4FX Code">
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S5FX Code">
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
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked", "hiding"})
    public void processS6F11InInit(DataMsgMap data) {
        System.out.println("Received s6f11InInit data.getTransactionId() = " + data.getTransactionId());
        long ceid = 0;
        try {
            if (data.get("CollEventId") != null) {
                ceid = data.getSingleNumber("CollEventId");
            }
            if (ceid != 106) {
                System.out.println("Received a s6f11InInit with CEID = " + ceid + ". Ignored");
                return;
            }
            String equipId = null;
            if (data.get("EquipmentId") != null) {
                equipId = (String) ((SecsItem) data.get("EquipmentId")).getData();
            }
            String installationDate = null;
            if (data.get("InstallationDate") != null) {
                installationDate = (String) ((SecsItem) data.get("InstallationDate")).getData();
            }
            String lotId = null;
            String leftEpoxyId = null;
            String leadFrameTypeID = null;
            long rptId = 0;
            if (data.get("ReportId033") != null) {
                rptId = data.getSingleNumber("ReportId033");
            }
            if (rptId == 33) {
                if (data.get("LotID") != null) {
                    lotId = (String) ((SecsItem) data.get("LotID")).getData();
                }
            } else if (rptId == 108) {
                if (data.get("LotID") != null) {
                    leftEpoxyId = (String) ((SecsItem) data.get("LotID")).getData();
                }
            } else if (rptId == 109) {
                if (data.get("LotID") != null) {
                    leadFrameTypeID = (String) ((SecsItem) data.get("LotID")).getData();
                }
            }
            rptId = 0;
            if (data.get("ReportId108") != null) {
                rptId = data.getSingleNumber("ReportId108");
            }
            if (rptId == 33) {
                if (data.get("LeftEpoxyId") != null) {
                    lotId = (String) ((SecsItem) data.get("LeftEpoxyId")).getData();
                }
            } else if (rptId == 108) {
                if (data.get("LeftEpoxyId") != null) {
                    leftEpoxyId = (String) ((SecsItem) data.get("LeftEpoxyId")).getData();
                }
            } else if (rptId == 109) {
                if (data.get("LeftEpoxyId") != null) {
                    leadFrameTypeID = (String) ((SecsItem) data.get("LeftEpoxyId")).getData();
                }
            }
            rptId = 0;
            if (data.get("ReportId109") != null) {
                rptId = data.getSingleNumber("ReportId109");
            }
            if (rptId == 33) {
                if (data.get("LeadFrameTypeID") != null) {
                    lotId = (String) ((SecsItem) data.get("LeadFrameTypeID")).getData();
                }
            } else if (rptId == 108) {
                if (data.get("LeadFrameTypeID") != null) {
                    leftEpoxyId = (String) ((SecsItem) data.get("LeadFrameTypeID")).getData();
                }
            } else if (rptId == 109) {
                if (data.get("LeadFrameTypeID") != null) {
                    leadFrameTypeID = (String) ((SecsItem) data.get("LeadFrameTypeID")).getData();
                }
            }

            System.out.println("Data got from received SECS message s6f11 is: ");
            System.out.println("Equipment Id = " + equipId);
            System.out.println("Installation Date = " + installationDate);
            System.out.println("Lot Id = " + lotId);
            System.out.println("Left Epoxy Id = " + leftEpoxyId);
            System.out.println("Lead Frame Type ID = " + leadFrameTypeID);

            //Equip_Id = equipId;
            Installation_Date = installationDate;
            Lot_Id = lotId;
            Left_Epoxy_Id = leftEpoxyId;
            Lead_Frame_Type_Id = leadFrameTypeID;

            String stripMapXml = null;
            if (data.get("StripMapData") != null) {
                if (((SecsItem) data.get("StripMapData")).getData() instanceof String) {
                    stripMapXml = (String) ((SecsItem) data.get("StripMapData")).getData();
                }
            }
            System.out.print(WSUtility.binSet(stripMapXml, this.deviceCode));

            DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
            System.out.println("Host begin Epoxy Verification ................................");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processS6F11inStripMapUpload(DataMsgMap data) {
        System.out.println("----Received from Equip Strip Map Upload event - S6F11");
        try {
            long ceid = 0;

//            String ceId=(String) ((SecsItem) data.get("CollEventId")).getData();
//            //获取xml字符串
//            String stripMapData = (String) ((SecsItem) data.get("MapData")).getData();
            if (data.get("CollEventId") != null) {
                ceid = data.getSingleNumber("CollEventId");
            }
            System.out.println("Received a s6f11InInit with CEID = " + ceid + ".");

            long dataId = 0;
            if (data.get("DataId") != null) {
                dataId = data.getSingleNumber("DataId");
            }
            System.out.println("Received a s6f11InInit with Data Id = " + dataId + ".");
            long rptId = 0;
            if (data.get("ReportId") != null) {
                rptId = data.getSingleNumber("ReportId");
            }
            System.out.println("Received a s6f11InInit with ReportId = " + rptId + ".");
            String stripMapXml = null;
            if (data.get("StripMapData") != null) {
                if (((SecsItem) data.get("StripMapData")).getData() instanceof String) {
                    stripMapXml = (String) ((SecsItem) data.get("StripMapData")).getData();
                }
            }

            System.out.println("Strip Map Data got from received SECS message s6f11 is: ");
            System.out.println(stripMapXml);

//            String finishFlag="1";
//            if(Integer.parseInt("15338")==ceid){
//                finishFlag="0";
//            }
            //通过Web Service上传mapping
            DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = WSUtility.binSet(stripMapXml, deviceCode).getBytes()[0];
            if (ack[0] == '0') {//上传成功
                ack[0] = 0;
            } else {//上传失败
                ack[0] = 1;
            }
            out.put("AckCode", ack);
            out.setTimeStamp(new Date());
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
            System.out.println(" ----- s6f12 sended - Strip Upload Completed-----.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void processS6f5in(DataMsgMap data) {
        try {
            DataMsgMap out = new DataMsgMap("s6f6out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;  //granted
            out.put("GrantCode", ack);
            out.setTimeStamp(new Date());
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
            System.out.println(" ----- s6f6 sended - Multi Block Request Granted-----.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S14FX Code"> 

    @SuppressWarnings("unchecked")
    @Override
    public void processS14F1in(DataMsgMap data) {
        if (data == null) {
            return;
        }
        System.out.println("--------Received s14f1in---------");
        String objType = null;
        if (data.get("ObjectType") != null) {
            objType = (String) ((SecsItem) data.get("ObjectType")).getData();
        }
        System.out.println("Equipmen is asking = " + ((objType == null) ? "" : objType));
        String stripId = "";
        if (data.get("StripId") != null) {
            stripId = (String) ((SecsItem) data.get("StripId")).getData();
        }
        System.out.println("Get Strip Id from equip request = " + stripId);

        //DataMsgMap out = new DataMsgMap("s14f2out", activeWrapper.getDeviceId());
        DataMsgMap out = null;
        //out.setTransactionId(activeWrapper.getNextAvailableTransactionId());

        //通过Web Service获得xml字符串
        String stripMapData = WSUtility.binGet(stripId, deviceCode);
        if (stripMapData == null) {//stripId不存在
            System.out.println("Sending Strip Map does not exist.  " + stripId);
            out = new DataMsgMap("s14f2outNoExist", activeWrapper.getDeviceId());
            out.setTransactionId(data.getTransactionId());
            long[] u1 = new long[1];
            u1[0] = 0;
            out.put("ObjectAck", u1);
        } else {//stripId存在
            System.out.println("Sending Strip Map exist.  " + stripId);
            out = new DataMsgMap("s14f2out", activeWrapper.getDeviceId());
            out.setTransactionId(data.getTransactionId());
            out.put("StripId", stripId);
            out.put("MapData", stripMapData);
        }
        try {
            activeWrapper.respondMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // </editor-fold> 



    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
