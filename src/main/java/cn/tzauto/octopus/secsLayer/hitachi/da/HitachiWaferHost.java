/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.hitachi.da;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.hitachi.HitachiWaferUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @author luosy
 */
@SuppressWarnings(value = "all")
public class HitachiWaferHost extends EquipHost {

    private static final Logger logger = Logger.getLogger(HitachiWaferHost.class);
    private volatile boolean isInterrupted = false;
    String FlatNotchLocation;

    public HitachiWaferHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        StripMapUpCeid = -1;
        EquipStateChangeCeid = -1;
    }


    @Override
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!isInterrupted) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }
                if (this.getControlState() == null ? GlobalConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(GlobalConstant.CONTROL_REMOTE_ONLINE)) {
//                    sendS1F1out();
//                    //为了能调整为online remote
//                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
//                    initRptPara();
//                    sendS2F41outPPselect("BDM572HFSM-01");
//                    sendS2f41Cmd("STOP");
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    logger.info("HitachiWafterHost:s6f11in-->"+msg+";"+msg.get("CEID"));
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s12f3in")) {
                    processS12F3in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s12f15in")) {
                    processS12F15in(msg);
                }
            } catch (InterruptedException e) {
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.toLowerCase().contains("s6f11in")) {
                processS6F11in(data);
//                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else if (tagName.equalsIgnoreCase("S12F3in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("S12F15in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("S6F81in")) {
//                 processS6F81in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    @SuppressWarnings("unchecked")
    public void processS6F12in(DataMsgMap data) {
        logger.info("----------Received s6f12in---------");
        byte[] ack = (byte[]) ((MsgSection) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }


    @Override
    public Object clone() {
        HitachiWaferHost newEquip = new HitachiWaferHost(deviceId,
                this.iPAddress,
                this.tCPPort, this.connectMode,
                this.deviceType, this.deviceCode);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.setIsRestarting(isRestarting);
        this.clear();
        return newEquip;
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Map waferInfoMap = new HashMap();

    @Override
    public Map processS12F3in(DataMsgMap DataMsgMap) {
//        DataMsgMap s12f4out = null;
        try {
//            s12f4out = new DataMsgMap("s12f4out", activeWrapper.getDeviceId());
//            ArrayList<MsgSection> list = (ArrayList) ((MsgSection) DataMsgMap.get("RESULT")).getData();
//            ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
//
            String MaterialID = String.valueOf(DataMsgMap.get("MID")).trim();
            logger.info("waferid：" + MaterialID);
            String temp = String.valueOf(DataMsgMap.get("IDTYP"));
            byte IDTYP = Byte.valueOf(temp);
//            byte[] IDTYPs = new byte[1];
////            IDTYPs[0] = Byte.valueOf(IDTYP);

            String MapDataFormatType = String.valueOf(DataMsgMap.get("MAPFT"));
            FlatNotchLocation = String.valueOf(DataMsgMap.get("FNLOC"));
            long flatNotchLocation = Long.valueOf(FlatNotchLocation);
//            long[] FlatNotchLocations = new long[1];
//            FlatNotchLocations[0] = Long.valueOf(FlatNotchLocation);
            String FileFrameRotation = String.valueOf(DataMsgMap.get("FFROT"));
            temp = String.valueOf(DataMsgMap.get("ORLOC"));
            byte OriginLocation = Byte.valueOf(temp);
//            byte[] OriginLocations = new byte[1];
//            OriginLocations[0] = Byte.valueOf(OriginLocation);
            String ProcessAxis = String.valueOf(DataMsgMap.get("PRAXI"));
            String BinCodeEquivalents = String.valueOf(DataMsgMap.get("BCEQU"));
            String NullBinCodeValue = String.valueOf(DataMsgMap.get("NULBC"));
//            long[] NullBinCodeValues = new long[1];
//            NullBinCodeValues[0] = Long.valueOf(NullBinCodeValue);
            waferInfoMap = HitachiWaferUtil.getWaferFileInfo(MaterialID, FlatNotchLocation, deviceCode.replace("-M", ""));
//            s12f4out.put("MaterialID", MaterialID);
//            s12f4out.put("IDTYP", IDTYPs);
//            s12f4out.put("FlatNotchLocation", FlatNotchLocations);
//            s12f4out.put("OriginLocation", OriginLocations);
//            long[] RrferencePointSelects = new long[1];
//            RrferencePointSelects[0] = 0L;
//            s12f4out.put("RrferencePointSelect", RrferencePointSelects);
//            MsgSection vRoot = new MsgSection();
//            vRoot.setFormatCode(SecsFormatValue.SECS_LIST);
//            s12f4out.put("REFPxREFPy", vRoot);
//            s12f4out.put("DieUnitsOfMeasure", "");
//            s12f4out.put("XAxisDieSize", RrferencePointSelects);
//            s12f4out.put("YAxisDieSize", RrferencePointSelects);
//            long[] RowCountInDieIncrementss = new long[1];
            long rowCount = Long.parseLong(String.valueOf(waferInfoMap.get("RowCountInDieIncrements")));
//            s12f4out.put("RowCountInDieIncrements", RowCountInDieIncrementss);

//            long[] ColumnCountInDieIncrementss = new long[1];
            long columnCount = Long.parseLong(String.valueOf(waferInfoMap.get("ColumnCountInDieIncrements")));
//            s12f4out.put("ColumnCountInDieIncrements", ColumnCountInDieIncrementss);

//            long[] ProcessDieCounts = new long[1];

            long dieCount = Long.parseLong(String.valueOf(waferInfoMap.get("ProcessDieCount")));
//            s12f4out.put("ProcessDieCount", ProcessDieCounts);
//            s12f4out.put("BinCodeEquivalents", BinCodeEquivalents);
//            s12f4out.put("NullBinCodeValue", NullBinCodeValues);
//            s12f4out.put("NullBinCodeValue", " ");
//            long[] MessageLengths = new long[1];
//            MessageLengths[0] = 38L;
//            s12f4out.put("MessageLength", MessageLengths);
//            s12f4out.setTransactionId(DataMsgMap.getTransactionId());


//            Object mid, short midFormat, byte idtype, long flatNotchLocation, byte originLocation,
//            long referencePointSelect, List referencePoint,short referencePointFormat, String dieUnit,
//            long xdies, long ydies, short xydiesFormat, long rowCount, long columnCount, long dieCount,
//            short countFormat, Object binCode, Object nullBinCode,short binCodeFormat, long messageLength,
//            short messageLengthFromat, long transactionId

            activeWrapper.sendS12F4out(MaterialID, SecsFormatValue.SECS_ASCII, IDTYP, flatNotchLocation, OriginLocation, 0, null, SecsFormatValue.SECS_LIST, "", 0, 0, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER
                    , rowCount, columnCount, dieCount, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER, BinCodeEquivalents, NullBinCodeValue, SecsFormatValue.SECS_ASCII, 38L, SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER, DataMsgMap.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        return null;

    }

    @Override
    public Map processS12F15in(DataMsgMap msgDataHashtable) {
//        DataMsgMap s12f16ut = null;
        try {
            String MaterialID = (String) msgDataHashtable.get("MID");
            String idtyp = (String) msgDataHashtable.get("IDTYP");
            byte IDTYP = Byte.valueOf(idtyp);

//            s12f16ut = new DataMsgMap("s12f16out", activeWrapper.getDeviceId());
//            byte[] IDTYP = ((byte[]) ((MsgSection) msgDataHashtable.get("IDTYP")).getData());
//            String MaterialID = ((MsgSection) msgDataHashtable.get("MaterialID")).getData().toString();
//            s12f16ut.put("MaterialID", MaterialID);
//            s12f16ut.put("IDTYP", IDTYP);
            long[] STRPxSTRPy = new long[2];
            STRPxSTRPy[0] = 0;
            STRPxSTRPy[1] = 0;
//            s12f16ut.put("STRPxSTRPy", STRPxSTRPy);
            String[] BinListTmp = (String[]) waferInfoMap.get("BinList");
            String BinList = "";
            for (int i = 0; i < BinListTmp.length; i++) {
                BinList = BinList + BinListTmp[i];
            }
//            s12f16ut.put("BinList", new MsgSection(BinList, SecsFormatValue.SECS_ASCII));
//
//
//            map.put("MID", resultList.get(0) != null ? resultList.get(0) : "");
//            map.put("IDTYP", resultList.get(1) != null ? resultList.get(1) : "");
//
//
//            s12f16ut.setTransactionId(msgDataHashtable.getTransactionId());
//
//            Object mid, short midFormat, byte idtype, long[] strp, short strpFormat,
//            Object binlist, short binlistFormat, long transactionId

            activeWrapper.sendS12F16out(MaterialID, SecsFormatValue.SECS_ASCII, IDTYP, STRPxSTRPy, SecsFormatValue.SECS_2BYTE_SIGNED_INTEGER,
                    BinList, SecsFormatValue.SECS_ASCII, msgDataHashtable.getTransactionId());
            waferInfoMap = new HashMap();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

//    @SuppressWarnings("unchecked")
//    @Override
//    public void sendS1F13out() {
//        DataMsgMap s1f13out = new DataMsgMap("s1f13outListZero", activeWrapper.getDeviceId());
//        s1f13out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        try {
//            DataMsgMap data = activeWrapper.sendS1F13out();
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
}
