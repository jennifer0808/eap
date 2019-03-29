/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.util;

/**
 *
 * @author njtz
 */
public class ACKDescription {
    //ackc5: for s5f2 s5f4
    //ackc7: for s7f4 f12 f14 f16 f18 f24 f32
    //PPGNT: for s7f2 f30
    //HCACK: for s2f42 f50 
    //ALCD:  for s5f1 f6

    public static String description(byte ackcode, String acktype) {
        String description = "";
        if (acktype.equals("ACKC5")) {
            if (ackcode == 0) {
                description = "Accepted";
            } else if (ackcode > 0) {
                description = "Error not accepted";
            }
        } else if (acktype.equals("ACKC7")) {
            if (ackcode == 0) {
                description = "Accepted";
            } else if (ackcode == 1) {
                description = "Permission not granted";//未授予权限
            } else if (ackcode == 2) {
                description = "Length error";
            } else if (ackcode == 3) {
                description = "Matrix overflow";//矩阵溢出
            } else if (ackcode == 4) {
                description = "PPID not found";
            } else if (ackcode == 5) {
                description = "Mode unsupported";//模式不支持
            } else if (ackcode > 5) {
                description = "Other error";
            }
        } else if (acktype.equals("PPGNT")) {
            if (ackcode == 0) {
                description = "OK";
            } else if (ackcode == 1) {
                description = "Already have";
            } else if (ackcode == 2) {
                description = "No space";
            } else if (ackcode == 3) {
                description = "Invalid PPID";//无效的ppid
            } else if (ackcode == 4) {
                description = "Busy , try later";
            } else if (ackcode == 5) {
                description = "Will not accept";
            } else if (ackcode > 5) {
                description = "Other error";
            }
        } else if (acktype.equals("HCACK")) {
            if (ackcode == 0) {
                description = "Command has been performed";//命令已执行
            } else if (ackcode == 1) {
                description = "Command does not exist";
            } else if (ackcode == 2) {
                description = "Cannot perform now";
            } else if (ackcode == 3) {
                description = "At least one parameter is invalid";//至少有一个参数无效
            } else if (ackcode == 4) {
                description = "Command will be performed with completion signaled later by an event";//命令将执行完成后由事件发出的信号
            } else if (ackcode == 5) {
                description = "Rejected, Already in Desired Condtion";//拒绝，已经在预期的条件
            } else if (ackcode > 5) {
                description = "NO such object exists";//没有这样的对象存在
            }
        } else if (acktype.equals("ALCD")) {
            if (ackcode == 0) {
                description = "Not used";
            } else if (ackcode == 1) {
                description = "Personal safety";
            } else if (ackcode == 2) {
                description = "Equipment safety";
            } else if (ackcode == 3) {
                description = "Parameter control warning";//参数控制报警
            } else if (ackcode == 4) {
                description = "Parameter control error";
            } else if (ackcode == 5) {
                description = "Irrecoverable error";//不可恢复的错误
            } else if (ackcode == 6) {
                description = "Equipment status waring";
            } else if (ackcode == 7) {
                description = "Attention flags";//注意标志
            } else if (ackcode == 8) {
                description = "Data integries";//数据的完整性
            } else {
                description = "Other categories";//其他类别
            }
        }
        return description;
    }

    /**
     * 将参数 ack 作为string 类型，减少不必要的类型转换
     *
     * @param ack
     * @param deviceType
     * @return
     */
    public static String descriptionStatus(String ack, String deviceType) {
        String description = "";
        if (deviceType.contains("TOWA")) {
            if (ack.equals("0")) {
                description = "Down";
            } else if (ack.equals("1")) {
                description = "Ready";
            } else if (ack.equals("2")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "Run";
            } else if (ack.equals("4")) {
                description = "Pause";
            } else if (ack.equals("5")) {
                description = "Mainte";
            } else if (ack.equals("6")) {
                description = "Ready Off";
            }
        } else if (deviceType.contains("FICO")) {
            if (ack.equals("1")) {
                description = "Not Ready";
            } else if (ack.equals("2")) {
                description = "RESERVED";
            } else if (ack.equals("3")) {
                description = "Idle";
            } else if (ack.equals("4")) {
                description = "Setup";
            } else if (ack.equals("5")) {
                description = "Ready";
            } else if (ack.equals("6")) {
                description = "Run";
            } else if (ack.equals("7")) {
                description = "RESERVED";
            } else if (ack.equals("8")) {
                description = "Pause";
            } else if (ack.equals("13")) {
                description = "Alarm";
            } else {
                description = "Undefined";
            }
        } else if (deviceType.contains("YAMADA")) {
            if (ack.equals("1")) {
                description = "Idle";
            } else if (ack.equals("2")) {
                description = "Setup";
            } else if (ack.equals("3")) {
                description = "Alarm";
            } else if (ack.equals("4")) {
                description = "Error";
            } else if (ack.equals("5")) {
                description = "Run";
            }
        } else if (deviceType.contains("ICOS")) {
            if (ack.equals("0")) {
                description = "UNKNOWN";
            } else if (ack.equals("2")) {
                description = "INIT";
            } else if (ack.equals("3")) {
                description = "IDLE";
            } else if (ack.equals("4")) {
                description = "SETUP";
            } else if (ack.equals("5")) {
                description = "READY";
            } else if (ack.equals("6")) {
                description = "RUN";
            } else if (ack.equals("9")) {
                description = "PAUSE";
            }
        } else if (deviceType.contains("DISCO")) {
            if (ack.equals("0")) {
                description = "INIT";
            } else if (ack.equals("1")) {
                description = "IDLE";
            } else if (ack.equals("2")) {
                description = "SETUP";
            } else if (ack.equals("3")) {
                description = "READY";
            } else if (ack.equals("4")) {
                description = "RUN";
            } else if (ack.equals("5")) {
                if ("DISCODFD6361".equals(deviceType)) {
                    description = "PAUSE";
                } else {
                    description = "END";
                }
            } else if (ack.equals("6")) {
                if ("DISCODFD6361".equals(deviceType)) {
                    description = "Alarm";
                } else {
                    description = "PAUSE";
                }
            } else if (ack.equals("7")) {
                description = "ERROR";
            } else if (ack.equals("8")) {
                description = "ABORT";
            } else if (ack.equals("9")) {
                description = "End";
            } else if (ack.equals("10")) {
                description = "ADJUSTMENT";
            } else if (ack.equals("50")) {
                description = "IDLE";
            } else if (ack.equals("51")) {
                description = "MANUAL";
            } else if (ack.equals("52")) {
                description = "Run";
            } else if (ack.equals("56")) {
                description = "READY";
            } else if (ack.equals("55")) {
                description = "SETUP";
            } else if (ack.equals("57")) {
                description = "ENDCAS";
            } else if (ack.equals("58")) {
                description = "ERROR";
            } else if (ack.equals("60")) {
                description = "STOPPING";
            } else if (ack.equals("61")) {
                description = "ABORTED";
            } else if (ack.equals("62")) {
                description = "Run";
            }
        } else if (deviceType.contains("Hylax")) {
            if (ack.equals("0")) {
                description = "UNKNOWN";
            } else if (ack.equals("2")) {
                description = "INIT";
            } else if (ack.equals("3")) {
                description = "IDLE";
            } else if (ack.equals("4")) {
                description = "SETUP";
            } else if (ack.equals("5")) {
                description = "READY";
            } else if (ack.equals("6")) {
                description = "RUN";
            } else if (ack.equals("9")) {
                description = "PAUSE";
            }
        } else if (deviceType.contains("AU800")) {
            if (ack.equals("0")) {
                description = "Infant";
            } else if (ack.equals("1")) {
                description = "Init";
            } else if (ack.equals("2")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "PreStart";
            } else if (ack.equals("4")) {
                description = "Run";
            } else if (ack.equals("5")) {
                description = "Error";
            } else if (ack.equals("6")) {
                description = "ShutDown";
            } else if (ack.equals("7")) {
                description = "Abort";
            }
        } else if (deviceType.contains("ASM120T") || deviceType.contains("ASM80T")) {
            if (ack.equals("2")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "PreStart";
            } else if (ack.equals("4")) {
                description = "Run";  //Auto-Cycle
            } else if (ack.equals("5")) {
                description = "Error";
            } else if (ack.equals("6")) {
                description = "Stop";
            } else {
                description = "Reserved";
            }
        } else if (deviceType.contains("ASMAD8312") || deviceType.contains("AD86") || deviceType.contains("9212")) {
            if (ack.equals("1")) {
                description = "Initial";
            } else if (ack.equals("2")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "Setup";
            } else if (ack.equals("4")) {
                description = "Ready";
            } else if (ack.equals("5")) {
                description = "Execute remote off";
            } else if (ack.equals("6")) {
                description = "Pause";
            } else if (ack.equals("7")) {
//                description = "Alarm Pause";
                description = "Run";
            } else if (ack.equals("8")) {
                description = "Ready";
            } else if (ack.equals("9")) {
                description = "Run";
            } else if (ack.equals("10")) {
                description = "Download";
            } else if (ack.equals("11")) {
                description = "Upload";
            }
        } else if (deviceType.contains("IDEAL3G")) {
            if (ack.equals("1")) {
                description = "Init";
            } else if (ack.equals("2")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "Setup";
            } else if (ack.equals("4")) {
                description = "Ready";
            } else if (ack.equals("5")) {
                description = "WAIT USER START";
            } else if (ack.equals("6")) {
                description = "WAIT FOR LOT";
            } else if (ack.equals("7")) {
                description = "Run";  //EXECUTING
            } else if (ack.equals("8")) {
                description = "ALARM PAUSED";
            } else if (ack.equals("9")) {
                description = "HOST PAUSED";
            } else {
                description = "Reserved";
            }
        } else if (deviceType.contains("DB730") || deviceType.contains("DB800") || deviceType.contains("DB810")) {
            if (ack.equals("1")) {
                description = "INIT";
            } else if (ack.equals("2")) {
                description = "SETUP";
            } else if (ack.equals("3")) {
                description = "READY";
            } else if (ack.equals("4")) {
                description = "RUN";
            } else if (ack.equals("5")) {
                description = "PAUSE";
            } else if (ack.equals("6")) {
                description = "ERROR";
            } else if (ack.equals("7")) {
                description = "WAIT LOT";
            }
        } else if (deviceType.contains("OCR")) {
            if (ack.equals("0")) {
                description = "IDLE";
            } else if (ack.equals("1")) {
                description = "SETUP";
            } else if (ack.equals("2")) {
                description = "READY";
            } else if (ack.equals("3")) {
                description = "Run";
            } else if (ack.equals("4")) {
                description = "PAUSE";
            }
        } else if (deviceType.contains("2100")) {
            if (ack.equals("0")) {
                description = "Ready";
            } else if (ack.equals("100")) {
                description = "NRDY";
            } else if (ack.equals("200")) {
                description = "Run";
            } else if (ack.equals("10")) {
                description = "Stopped RDY";
            } else if (ack.equals("110")) {
                description = "not init";
            } else if (ack.equals("120")) {
                description = "production NRDY";
            } else if (ack.equals("140")) {
                description = "equipment NRDY";
            } else if (ack.equals("150")) {
                description = "process NRDY";
            } else if (ack.equals("210")) {
                description = "Run";
            } else if (ack.equals("11")) {
                description = "stopped RDY";
            } else if (ack.equals("111")) {
                description = "not init";
            } else if (ack.equals("112")) {
                description = "startup NRDY";
            } else if (ack.equals("113")) {
                description = "shutdown NRDY";
            } else if (ack.equals("121")) {
                description = "prod. menu NRDY";
            } else if (ack.equals("122")) {
                description = "prod. precheck NRDY";
            } else if (ack.equals("123")) {
                description = "prod. access NRDY";
            } else if (ack.equals("124")) {
                description = "setup key NRDY";
            } else if (ack.equals("131")) {
                description = "safety NRDY";
            } else if (ack.equals("141")) {
                description = "equipment NRDY";
            } else if (ack.equals("151")) {
                description = "material NRDY";
            } else if (ack.equals("152")) {
                description = "process NRDY";
            } else if (ack.equals("153")) {
                description = "operator NRDY";
            } else if (ack.equals("154")) {
                description = "host NRDY";
            } else if (ack.equals("211")) {
                description = "Run";
            }
        } else if (deviceType.contains("APT")) {
            if (ack.equals("1")) {
                description = "Idle";
            } else if (ack.equals("2")) {
                description = "Run";
            } else if (ack.equals("3")) {
                description = "End";
            } else if (ack.equals("4")) {
                description = "Maintain";
            } else if (ack.equals("5")) {
                description = "Alarm End";
            }
        } else if (deviceType.contains("NITTODR3000III")) {
            if (ack.equals("1")) {
                description = "INIT";
            } else if (ack.equals("2")) {
                description = "IDLE";
            } else if (ack.equals("3")) {
                description = "SETUP";
            } else if (ack.equals("4")) {
                description = "READY";
            } else if (ack.equals("5")) {
                description = "RUN";
            } else if (ack.equals("6")) {
                description = "CYCLE_END";
            } else if (ack.equals("7")) {
                description = "PAUSE_END";
            } else if (ack.equals("8")) {
                description = "MANUAL";
            } else if (ack.equals("9")) {
                description = "ABORT";
            }
        } else if (deviceType.contains("DEKHorizon03ix")) {
            if (ack.equals("0")) {
                description = "Ready";
            } else if (ack.equals("1")) {
                description = "Waiting";
            } else if (ack.equals("2")) {
                description = "Run";
            } else if (ack.equals("3")) {
                description = "Setup";
            } else if (ack.equals("4")) {
                description = "Down";
            } else if (ack.equals("5")) {
                description = "Recovery";
            } else if (ack.equals("6")) {
                description = "Maintenance";
            }
        } else if (deviceType.contains("TSKAWD300TX")) {
            if (ack.equals("0")) {
                description = "INIT";
            } else if (ack.equals("1")) {
                description = "IDLE";
            } else if (ack.equals("2")) {
                description = "SETUP";
            } else if (ack.equals("3")) {
                description = "READY";
            } else if (ack.equals("4")) {
                description = "RUN";
            } else if (ack.equals("5")) {
                description = "PAUSE";
            }
        } else if (deviceType.contains("AD832P") || deviceType.contains("832i") || deviceType.contains("AD838") || deviceType.contains("Twin832") || deviceType.contains("8312Plus")) {
            if (ack.equals("1")) {
                description = "Unknown Status ";
            } else if (ack.equals("2")) {
                description = "Idle Local";
            } else if (ack.equals("3")) {
                description = "Idle Remote";
            } else if (ack.equals("4")) {
                description = "Setup";
            } else if (ack.equals("5")) {
                description = "Initializing";
            } else if (ack.equals("6")) {
                description = "Execute Local";
            } else if (ack.equals("7")) {
                description = "Run";
            } else if (ack.equals("8")) {
                description = "Error";
            }
        } else if (deviceType.contains("HANMI")) {
            if (ack.equals("1")) {
                description = "INIT";
            } else if (ack.equals("3")) {
                description = "SETUP";
            } else if (ack.equals("4")) {
                description = "Ready";
            } else if (ack.equals("5")) {
                description = "Run";
            } else if (ack.equals("6")) {
                description = "Pause";
            } else if (ack.equals("7")) {
                description = "ErrorPause";
            }
        } else if (deviceType.contains("MVP")) {
            if (ack.equals("0")) {
                description = "Idle";
            } else if (ack.equals("1")) {
                description = "Editor";
            } else if (ack.equals("2")) {
                description = "DB-Setup";
            } else if (ack.equals("3")) {
                description = "Process";
            } else if (ack.equals("4")) {
                description = "Pause";
            }
        } else if (deviceType.contains("ACCRETECHPG3000RMX")) {
            if ("0".equals(ack)) {
                description = "Init";
            } else if ("1".equals(ack)) {
                description = "Idle";
            } else if ("2".equals(ack)) {
                description = "Setup";
            } else if ("3".equals(ack)) {
                description = "Ready";
            } else if ("4".equals(ack)) {
                description = "Run";
            } else if ("5".equals(ack)) {
                description = "Pause";
            } else if ("6".equals(ack)) {
                description = "Warming UP";
            }
        } else if (deviceType.contains("TSKPG300")) {
            if ("0".equals(ack)) {
                description = "Idle";
            } else if ("1".equals(ack)) {
                description = "Setup";
            } else if ("2".equals(ack)) {
                description = "Ready";
            } else if ("3".equals(ack)) {
                description = "Run";
            } else if ("4".equals(ack)) {
                description = "Stopping";
            } else if ("5".equals(ack)) {
                description = "Abort-1";
            } else if ("6".equals(ack)) {
                description = "Abort-2 ";
            } else if ("7".equals(ack)) {
                description = "E-Stop";
            } else if ("8".equals(ack)) {
                description = "Pausing";
            } else if ("9".equals(ack)) {
                description = "Paused";
            } else if ("10".equals(ack)) {
                description = "Alarm Pausing ";
            } else if ("11".equals(ack)) {
                description = "Alarm Paused";
            } else if ("12".equals(ack)) {
                description = "Warming UP ";
            }
        } else if (deviceType.contains("BTU")) {
            if ("0".equals(ack)) {
                description = "INIT";
            } else if ("1".equals(ack)) {
                description = "IDLE";
            } else if ("2".equals(ack)) {
                description = "HOLD";
            } else if ("3".equals(ack)) {
                description = "SETUP";
            } else if ("4".equals(ack)) {
                description = "READY";
            } else {
                description = "UNKNOWN";
            }
        } else if (deviceType.contains("GIS126")) {
            if ("0".equals(ack)) {
                description = "INIT";
            } else if ("1".equals(ack)) {
                description = "IDLE";
            } else if ("2".equals(ack)) {
                description = "HOLD";
            } else if ("3".equals(ack)) {
                description = "SETUP";
            } else if ("4".equals(ack)) {
                description = "READY";
            } else {
                description = "UNKNOWN";
            }
        } else if (deviceType.contains("PARMI")) {
            if (ack.equals("1")) {
                description = "INIT";
            } else if (ack.equals("5")) {
                description = "SETUP";
            } else if (ack.equals("3")) {
                description = "Ready";
            } else if (ack.equals("4")) {
                description = "Run";
            } else if (ack.equals("6")) {
                description = "Pause";
            }
        } else if (deviceType.contains("AOI")) {
            if (ack.equals("0")) {
                description = "UNKNOWN";
            } else if (ack.equals("1")) {
                description = "INIT";
            } else if (ack.equals("2")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "Setup";
            } else if (ack.equals("4")) {
                description = "Ready";
            } else if (ack.equals("5")) {
                description = "Executing";
            }
        } else if (deviceType.contains("STI")) {
            if (ack.equals("0")) {
                description = "Idle";
            } else if (ack.equals("1")) {
                description = "Setting";
            } else if (ack.equals("2")) {
                description = "Ready";
            } else if (ack.equals("3")) {
                description = "Run";
            } else if (ack.equals("4")) {
                description = "Stop";
            } else if (ack.equals("5")) {
                description = "Abort";
            } else if (ack.equals("6")) {
                description = "E-Stop";
            } else if (ack.equals("7")) {
                description = "Alarm";
            }
        } else if (deviceType.contains("8800")) {
            if (ack.equals("0")) {
                description = "INIT";
            } else if (ack.equals("1")) {
                description = "IDLE";
            } else if (ack.equals("2")) {
                description = "HOME";
            } else if (ack.equals("3")) {
                description = " EMERGENCY ";
            } else if (ack.equals("4")) {
                description = "READY ";
            } else if (ack.equals("5")) {
                description = "Run";
            } else if (ack.equals("6")) {
                description = "OPERATOR ";
            } else if (ack.equals("7")) {
                description = "STEP ";
            } else if (ack.equals("8")) {
                description = "SETUP ";
            } else if (ack.equals("9")) {
                description = "ABORT";
            }
        } else if (deviceType.contains("VSP88")) {
            if (ack.equals("0")) {
                description = "Idle";
            } else if (ack.equals("1")) {
                description = "Run";
            } else if (ack.equals("2")) {
                description = "Pause";
            } else if (ack.equals("3")) {
                description = "Down";
            }
        } //        else if (deviceType.contains("VSP88")) {
        //            if (ack.equals("0")) {
        //                description = FengCeConstant.STATUS_IDLE;
        //            } else if (ack.equals("1")) {
        //                description = FengCeConstant.STATUS_RUN;
        //            } else if (ack.equals("2")) {
        //                description = FengCeConstant.STATUS_PAUSE;
        //            } else if (ack.equals("3")) {
        //                description = FengCeConstant.STATUS_END;
        //            }
        //        } 
        else if (deviceType.contains("EOLMC3200")) {
            if (ack.equals("1")) {
                description = "Init";
            } else if (ack.equals("5")) {
                description = "Idle";
            } else if (ack.equals("3")) {
                description = "Ready";
            } else if (ack.equals("4")) {
                description = "Run";
            } else if (ack.equals("5")) {
                description = "Pause";
            }
        }
        return description;
    }

    public static String describeOPMode(Object obj, String deviceType) {
        String opMode = "";
        if (deviceType.contains("FICO")) {
            switch (String.valueOf(obj)) {
                case "1":
                    opMode = "Production";
                    break;
                case "2":
                    opMode = "Disabled";
                    break;
                case "3":
                    opMode = "Conditioning Cleaning";
                    break;
                case "4":
                    opMode = "Compression Cleaning";
                    break;
                case "5":
                    opMode = "Conversion";
                    break;
                case "6":
                    opMode = "Short Shot";
                    break;
                case "7":
                    opMode = "Transfer Cleaning";
                    break;
                case "8":
                    opMode = "Rework Products";
                    break;
                case "9":
                    opMode = "Post Curing";
                    break;
                case "10":
                    opMode = "Calibration";
                    break;
                case "11":
                    opMode = "Robot Teaching";
                    break;
                case "12":
                    opMode = "Leadframe Loading";
                    break;
                case "255":
                    opMode = "Undefined";
                    break;
            }
        }
        return opMode;
    }

    public static String describeControlState(Object obj, String deviceType) {
        String descriControlState = "";
        if (deviceType.contains("8800")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        }
        if (deviceType.contains("TOWA")) {
//            descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
            switch (String.valueOf(obj)) {
                case "true":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On-Line/Remote
                    break;
                case "false":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On-Line/Local
                    break;
            }
        } else if (deviceType.contains("AOI")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
            }
        } else if (deviceType.contains("832i")) {
            switch (String.valueOf(obj)) {
                case "0":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "1":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
            }
        } else if (deviceType.contains("FICO") || deviceType.contains("NITTODR3000III") || deviceType.contains("DEKHorizon03ix") || deviceType.contains("TSKAWD300TX") || deviceType.contains("PARMI") || deviceType.contains("STI")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Off-Line/Equipment Off-Line
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Off-Line/Attempt On-Line
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Off-Line/Host Off-Line
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On-Line/Local
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On-Line/Remote
                    break;
            }
        } else if (deviceType.contains("YAMADA") || deviceType.contains("VSP88D") || deviceType.contains("EOLMC3200")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        } else if (deviceType.contains("DISCO")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        } else if (deviceType.contains("ICOS") || deviceType.contains("2100")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }

        } else if (deviceType.contains("ASM120T") || deviceType.contains("ASM80T") || deviceType.contains("ASMIDEAL3G")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        } else if (deviceType.contains("AU800")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        } else if (deviceType.contains("8312")) {
            switch (String.valueOf(obj)) {
                case "0":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "1":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
            }
        } else if (deviceType.contains("AD86")) {
            switch (String.valueOf(obj)) {
                case "0":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "1":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;

            }
        } else if (deviceType.contains("DB730") || deviceType.contains("DB-800")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Off-Line/Equipment Off-Line
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Off-Line/Attempt On-Line
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Off-Line/Host Off-Line
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On-Line/Local
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On-Line/Remote
                    break;
            }
        } else if (deviceType.contains("APT")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
            }
        } else if (deviceType.contains("Twin832")) {
            switch (String.valueOf(obj)) {
                case "0":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "1":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
            }
        } else if (deviceType.contains("AD832P") || deviceType.contains("AD838")) {
            switch (String.valueOf(obj)) {
                case "0":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                    break;
                case "1":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                    break;
            }
        } else if (deviceType.contains("HANMI")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }

        } else if (deviceType.contains("MVP")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        } else if (deviceType.contains("BTU")) {
            switch (String.valueOf(obj)) {
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        } else if (deviceType.contains("PG300")) {
            switch (String.valueOf(obj)) {
                case "1":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Equipment-Off Line(Equipment-Off-Line)
                    break;
                case "2":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Attempt-On Line(Trial-On-Line)
                    break;
                case "3":
                    descriControlState = FengCeConstant.CONTROL_OFFLINE;//Host-Off Line(Host-Off-Line)
                    break;
                case "4":
                    descriControlState = FengCeConstant.CONTROL_LOCAL_ONLINE;//On Line-Local(On-Line-Local)
                    break;
                case "5":
                    descriControlState = FengCeConstant.CONTROL_REMOTE_ONLINE;//On Line-Remote(On-Line-Remote)
                    break;
            }
        }
        return descriControlState;
    }
}
