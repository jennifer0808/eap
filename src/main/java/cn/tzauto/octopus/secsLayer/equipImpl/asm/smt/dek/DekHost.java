/**
 *
 *
 */
package cn.tzauto.octopus.secsLayer.equipImpl.asm.smt.dek;

import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;
import java.util.logging.Level;

public class DekHost extends EquipHost {


    private static final Logger logger = Logger.getLogger(DekHost.class.getName());


    public DekHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    public Object clone() {
        DekHost newEquip = new DekHost(deviceId,
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
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
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
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            LastComDate =  System.currentTimeMillis();
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
            }  else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
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
                sendS2F35out(ceid, rptid);//15339 1001
            }
            if (!"".equals(ack)) {
                ack = "";
                ceid = 15338l;
                rptid = 1002l;
                sendS2F35out(ceid, rptid);//15339 1001
            }
            if (!"".equals(ack)) {
                ack = "";
                ceid = 15328l;
                rptid = 1003l;
                sendS2F35out(ceid, rptid);//15339 1001
            }
            List list=new ArrayList();
            list.add(2031L);
            list.add(2009L);
            sendS2F33Out(4l, 2031L, list);
            sendS2F35out(4L, 4L, 4L);
            //SEND S2F37
            if (!"".equals(ack)) {
                sendS2F37outAll();
            }
            return "1";

        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(DekHost.class.getName()).log(Level.SEVERE, null, ex);
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

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName + ".dbrcp");
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        return resultMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">


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
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName + ".dbrcp");
        s7f1out.put("Length", length);
        DataMsgMap data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.debug("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt[0]);
        resultMap.put("Description", ACKDescription.description(ppgnt[0], "PPGNT"));
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName + ".dbrcp");
        s7f3out.put("Processprogram", secsItem);
        try {
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            e.printStackTrace();
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
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName + ".dbrcp");
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<RecipePara> recipeParaList = null;
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
            TransferUtil.setPPBody(ppbody, recipeType, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析
            recipeParaList = getRecipeParasByECSV();
        }
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        recipeName = recipeName + ".dbrcp";
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
            e.printStackTrace();
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

    // </editor-fold> 



}
