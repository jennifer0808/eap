package cn.tzauto.octopus.secsLayer.equipImpl.icos.tr;

import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.icos.TrRecipeUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;



@SuppressWarnings("serial")
public class T340Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(T340Host.class.getName());
    
    public T340Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        EquipStateChangeCeid=101L;
        this.svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        this.lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        this.ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        this.rptFormat=SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
    }


    @Override
    public Object clone() {
        T340Host newEquip = new T340Host(deviceId,
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
        this.setIsRestarting(isRestarting);
        this.setStartedDate(0);
        return newEquip;
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
                    this.sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sleep(3000);
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                    sendStatus2Server(equipStatus);
                }
//                if (!holdSuccessFlag) {
//                    holdDevice();
//                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
//                    processS6F11EquipStatus(msg);
//                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11equipstatuschange")) {
//                    processS6F11EquipStatusChange(msg);
//                }
                else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void processS6F11in(DataMsgMap data) {
        long ceid=0L;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
                if(ceid==EquipStateChangeCeid){
                    processS6F11EquipStatusChange(data);
                } if (ceid == 1L || ceid == 2L || ceid == 3L) {
                    processS6F11EquipStatus(data);
            }
        }catch (Exception e){
            logger.error("Exception:", e);
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
            lastMsgTagName = tagName;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.equals("s6f11equipstatuschange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            }else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            }
            else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("F0") || tagName.contains("f0")) {
                controlState = GlobalConstant.CONTROL_OFFLINE;
                equipStatus = "SECS-OFFLINE";
                Map panelMap = new HashMap();
                panelMap.put("EquipStatus", equipStatus);
                panelMap.put("PPExecName", ppExecName);
                panelMap.put("ControlState", controlState);
                changeEquipPanel(panelMap);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // <editor-fold defaultstate="collapsed" desc="S1FX Code"> 


    @SuppressWarnings("unchecked")
    public void processS6F12in(DataMsgMap data) {
        logger.info("----------Received s6f12in---------");
        byte[] ack = (byte[]) ((MsgSection) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }


    public Map sendS1F15outByT640() {
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS1F15out();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        byte ack = (byte)  msgdata.get("OFLACK");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f16");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack);
        return resultMap;
    }

    public Map sendS1F17outByT640() {

        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS1F17out();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        byte ack = (byte) msgdata.get("AckCode");
        byte ack = (byte) msgdata.get("ONLACK");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
//    @Override
//    public Map sendS1F3Check() {
//        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s1f3out.setTransactionId(transactionId);
//        long[] equipStatuss = new long[1];
//        long[] pPExecNames = new long[1];
//        long[] controlStates = new long[1];
//        DataMsgMap data = null;
//        try {
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            RecipeService recipeService = new RecipeService(sqlSession);
//            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
//            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
//            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
//            sqlSession.close();
//            s1f3out.put("EquipStatus", equipStatuss);
//            s1f3out.put("PPExecName", pPExecNames);
//            s1f3out.put("ControlState", controlStates);
//            logger.info("Ready to send Message S1F3==============>" + JSONArray.toJSON(s1f3out));
////            data = activeWrapper.sendPrimaryWsetMessage(s1f3out);
//
//
//        } catch (Exception e) {
//            logger.error("Wait for get meessage directly error：" + e);
//        }
//        if (data == null || data.get("RESULT") == null || ((MsgSection) data.get("RESULT")).getData() == null) {
//            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
//        }
//        if (data == null || data.get("RESULT") == null || ((MsgSection) data.get("RESULT")).getData() == null) {
//            if ("SECS-OFFLINE".equalsIgnoreCase(equipStatus)) {
//                Map panelMap = new HashMap();
//                panelMap.put("EquipStatus", equipStatus);
//                panelMap.put("PPExecName", ppExecName);
//                panelMap.put("ControlState", controlState);
//                changeEquipPanel(panelMap);
//            }
//            return null;
//        }
//        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
//        ArrayList<MsgSection> list = (ArrayList) ((MsgSection) data.get("RESULT")).getData();
//        List listtmp = getNcessaryData();
//        if (listtmp != null && !listtmp.isEmpty()) {
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
//            ppExecName = String.valueOf(listtmp.get(1));
//            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
//            logger.info("controlState 为：" + controlState + ":" + deviceType + ";" + listtmp.get(2));
//        }
//        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
//        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
//        ppExecName = (String) listtmp.get(1);
//        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
//        Map panelMap = new HashMap();
//        panelMap.put("EquipStatus", equipStatus);
//        panelMap.put("PPExecName", ppExecName);
//        panelMap.put("ControlState", controlState);
//        changeEquipPanel(panelMap);
//        return panelMap;
//    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code"> 
    public Map sendS2F41outStart(String batchName) {
        String rcmd = "START";

        Map<String, String> cpMap = new HashMap();
        cpMap.put("BATCH-NAME", batchName);
        cpMap.put("ACTION", "NEW");
        cpMap.put("BATCH-TO-PROCESS", "");
        cpMap.put("CARRIER-COUNT", "");
        cpMap.put("INPUT-TRAY-MAP", "");
        cpMap.put("TRAY-REPORTING", "NO");

        Map cpNameFormatMap = new HashMap();
        cpNameFormatMap.put("BATCH-NAME", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("ACTION", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("BATCH-TO-PROCESS", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("CARRIER-COUNT", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("INPUT-TRAY-MAP", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("TRAY-REPORTING", SecsFormatValue.SECS_ASCII);


        Map cpValueFormatMap = new HashMap();
        cpNameFormatMap.put(batchName, SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("NEW", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("ACTION", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("", SecsFormatValue.SECS_ASCII);
        cpNameFormatMap.put("NO", SecsFormatValue.SECS_ASCII);
        byte hcack = -1;
        try {
            DataMsgMap data = activeWrapper.sendS2F41out(rcmd, null, cpMap, cpNameFormatMap, cpValueFormatMap);
            hcack = (byte) data.get("HCACK");
            logger.debug("Recieve s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK = " + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6F11 Code">

 
    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid =Long.parseLong( data.get("CEID").toString());
                if (ceid == 2L) {
                    super.setControlState(GlobalConstant.CONTROL_LOCAL_ONLINE);
                } else if (ceid == 3L) {
                    super.setControlState(GlobalConstant.CONTROL_REMOTE_ONLINE);
                } else if (ceid == 1L) {
                    super.setControlState(GlobalConstant.CONTROL_OFFLINE);
                }
                findDeviceRecipe();

        } catch (Exception e) {
            e.printStackTrace();
        }
        updateCommStateInExt();
//        showCollectionsEventInfo(ceid);
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
//        long preStatus = 0l;
//        long nowStatus = 0L;
        long ceid = 0L;
        try {
//            preStatus = data.get("PreStatus");
//            nowStatus = Long.parseLong(data.get("EquipStatus").toString());
            ceid = Long.parseLong(data.get("CEID").toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        equipStatus = ACKDescription.descriptionStatus(String.valueOf(nowStatus), deviceType);
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        findDeviceRecipe();
        if (equipStatus.equalsIgnoreCase("run")) {
            //首先从服务端获取机台是否处于锁机状态
            //如果设备应该是锁机，那么首先发送锁机命令给机台
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.getInstance().getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                pauseDevice();
            }
        }
        if (equipStatus.equalsIgnoreCase("READY")) {
            findDeviceRecipe();
        }
        super.changeEquipPanel(map);
//        findDeviceRecipe();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (equipStatus.equalsIgnoreCase("READY")) {
            if (AxisUtility.checkBusinessMode(deviceCode)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "处于工程模式,取消开机检查");
                sqlSession.close();
                return;
            }
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                sqlSession.close();
                return;
            }
            //检查领料程序与设备在用程序是否一致
            boolean recipeNameOk = checkRecipeName(deviceInfoExt.getRecipeName());
            //检查程序版本
            Recipe goldRecipe = checkRecipeHasGoldFlag(deviceInfoExt.getRecipeName());
            if (recipeNameOk && goldRecipe != null) {
                Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                //首先判断下载的Recipe类型
                //1、如果下载的是Unique版本，那么执行完全比较
                String downloadRcpVersionType = downLoadRecipe.getVersionType();
                if ("Unique".equals(downloadRcpVersionType)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                    startCheckRecipePara(downLoadRecipe, "abs");
                } else {
                    //2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                    startCheckRecipePara(goldRecipe);
                }
            }

        }
        try {
            //更新模型表设备状态
            deviceInfoExt.setDeviceStatus(equipStatus);
            deviceInfoExt.setLockFlag(null);
            deviceService.modifyDeviceInfoExt(deviceInfoExt);
            sqlSession.commit();
            //保存设备操作记录到数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
        } catch (Exception e) {
        } finally {
            sqlSession.close();
        }
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param localFilePath
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map hanAndCompMap = getRelativeFileInfo(localFilePath, targetRecipeName);
        if (hanAndCompMap == null) {
            return null;
        }
        long length0 = TransferUtil.getPPLength(localFilePath);
        long length1 = TransferUtil.getPPLength(String.valueOf(hanAndCompMap.get("hanRcpPath")));
        long length2 = TransferUtil.getPPLength(String.valueOf(hanAndCompMap.get("compRcpPath")));
//        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
//        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f1out.put("ProcessprogramID", targetRecipeName);
//        s7f1out.put("Length", length0);
        DataMsgMap data = null;
        byte ppgnt = -1;
        try {
            data = activeWrapper.sendS7F1out(targetRecipeName, length0, lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid(recipe)= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
//        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f1out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("hanRcpName")));
//        s7f1out.put("Length", length1);
        try {
            data = activeWrapper.sendS7F1out(String.valueOf(hanAndCompMap.get("hanRcpName")), length1, lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid(handler)= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
//        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f1out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("compRcpName")));
//        s7f1out.put("Length", length2);
        try {
            data = activeWrapper.sendS7F1out(String.valueOf(hanAndCompMap.get("compRcpName")), length2, lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid(component)= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    /**
     * T640每次下载要下载3个recipe，这里每一个都成功，recipe才能使用
     *
     * @param localRecipeFilePath
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map hanAndCompMap = getRelativeFileInfo(localRecipeFilePath, targetRecipeName);
        if (hanAndCompMap == null) {
            return null;
        }
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody0 = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        byte[] ppbody1 = (byte[]) TransferUtil.getPPBody(recipeType, String.valueOf(hanAndCompMap.get("hanRcpPath"))).get(0);
        byte[] ppbody2 = (byte[]) TransferUtil.getPPBody(recipeType, String.valueOf(hanAndCompMap.get("compRcpPath"))).get(0);
//        MsgSection secsItem0 = new MsgSection(ppbody0, SecsFormatValue.SECS_BINARY);
//        MsgSection secsItem1 = new MsgSection(ppbody1, SecsFormatValue.SECS_BINARY);
//        MsgSection secsItem2 = new MsgSection(ppbody2, SecsFormatValue.SECS_BINARY);
        //下载han文件
//        s7f3out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("hanRcpName")));
//        s7f3out.put("Processprogram", secsItem1);
        try {
            sleep(1000);
            data = activeWrapper.sendS7F3out(String.valueOf(hanAndCompMap.get("hanRcpName")), ppbody1, SecsFormatValue.SECS_BINARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ackc7han = (byte) data.get("ACKC7");
        if (ackc7han == 0) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载成功.");
            logger.debug("Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载成功.");
        } else {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载失败.");
            logger.error("Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载失败.");
        }
        //下载comp文件
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f3out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("compRcpName")));
//        s7f3out.put("Processprogram", secsItem2);
        try {
            sleep(1000);
            data = activeWrapper.sendS7F3out(String.valueOf(hanAndCompMap.get("compRcpName")), ppbody2, SecsFormatValue.SECS_BINARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ackc7comp = (byte) data.get("ACKC7");
        if (ackc7comp == 0) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载成功.");
            logger.debug("Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载成功.");
        } else {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载失败.");
            logger.error("Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载失败.");
        }
        //下载recipe文件
//        s7f3out.put("ProcessprogramID", targetRecipeName);
//        s7f3out.put("Processprogram", secsItem0);
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            sleep(1000);
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody0, SecsFormatValue.SECS_BINARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ackc7 = (byte) data.get("AckCode");
        if (ackc7 == 0) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + targetRecipeName + "下载成功.");
            logger.debug("Recipe:" + targetRecipeName + "下载成功.");
        } else {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + targetRecipeName + "下载失败.");
            logger.error("Recipe:" + targetRecipeName + "下载失败.");
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        if (ackc7han == 0 && ackc7comp== 0 && ackc7 == 0) {
            ackc7 = 0;
        } else {
            ackc7 = 1;
        }
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName)  throws UploadRecipeErrorException {
        if ("Idle".equalsIgnoreCase(equipStatus) || "UNKNOWN".equalsIgnoreCase(equipStatus) || "ready".equalsIgnoreCase(equipStatus)) {
            Recipe recipe = setRecipe(recipeName);
            recipePath = super.getRecipePathByConfig(recipe);
//            DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
//            s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//            s7f5out.put("ProcessprogramID", recipeName);
            byte[] ppbody = (byte[]) getPPBODY(recipeName);
            DataMsgMap data = null;
//            byte[] ppbody1 = (byte[])  data.get("Processprogram");
            TransferUtil.setPPBody(ppbody, 1, recipePath);
            List<String> list = TrRecipeUtil.readRCP(recipePath);
            String rcpContent = "";
            for (String str : list) {
                if (str.contains("handler") || str.contains("component")) {
                    String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
                    String ppidTem = str.replace("@", "/");
                    byte[] ppbodyTem = (byte[]) getPPBODY(ppidTem);
                   TransferUtil.setPPBody(ppbodyTem, 1, recipePathTem);
                    rcpContent = rcpContent + str;
//                    DataMsgMap s7f5outTem = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
//                    s7f5outTem.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//                    s7f5outTem.put("ProcessprogramID", ppidTem);
//                    try {
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    if (data.get("ProcessprogramID") != null) {
//                        ppidTem = (String) ((MsgSection) data.get("ProcessprogramID")).getData();
//                        byte[] ppbodyTem = (byte[]) ((MsgSection) data.get("Processprogram")).getData();
//                        TransferUtil.setPPBody(ppbodyTem, recipeType, recipePathTem);
//                        rcpContent = rcpContent + str;
//                    }
                }
            }
            String rcpAnalyseSucceed = "Y";
            if (!rcpContent.contains("handler")) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]没有找到关联的handler文件，请检测文件是否存在或文件名是否正确");
                rcpAnalyseSucceed = "N";
            }
            if (!rcpContent.contains("component")) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]没有找到关联的component文件，请检测文件是否存在或文件名是否正确");
                rcpAnalyseSucceed = "N";
            }
            //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
            //Recipe解析
            List<RecipePara> recipeParaList = new ArrayList<>();
            try {
                recipeParaList = TrRecipeUtil.transferParaFromDB(deviceType, recipePath);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Map resultMap = new HashMap();
            resultMap.put("msgType", "s7f6");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("recipe", recipe);
            resultMap.put("recipeParaList", recipeParaList);
            resultMap.put("rcpAnalyseSucceed", rcpAnalyseSucceed);
            resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
            resultMap.put("Description", " Receive the recipe " + recipeName + " from equip " + deviceCode);
            return resultMap;
        } else {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "请在设备IDLE时上传Recipe.");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Map sendS7F17out(String recipeName) {
//        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
//        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f17out.put("ProcessprogramID", recipeName);
        List ProcessprogramIDList = new ArrayList();
        ProcessprogramIDList.add(recipeName);
        byte ackc7 = -1;
        DataMsgMap data = null;
        try {
            data=activeWrapper.sendS7F17out(ProcessprogramIDList);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte)  data.get("AckCode");
            sleep(1000);
            if (ackc7 == 0 || ackc7 == 6) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7 + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f17out.put("ProcessprogramID", recipeName.replace("recipe", "component"));
        List ProcessprogramIDList1 = new ArrayList();
        ProcessprogramIDList1.add(recipeName.replace("recipe", "component"));
        try {
              data=activeWrapper.sendS7F17out(ProcessprogramIDList1);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f17out.put("ProcessprogramID", recipeName.replace("recipe", "handler"));
        List ProcessprogramIDList2=new ArrayList();
        ProcessprogramIDList2.add(recipeName.replace("recipe", "handler"));
        try {
            data=activeWrapper.sendS7F17out(ProcessprogramIDList2);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = new CaseInsensitiveMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
//        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
//        s7f19out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
//            data = activeWrapper.sendPrimaryWsetMessage(s7f19out);
            data=activeWrapper.sendS7F19out();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList  list = (ArrayList) data.get("EPPD");
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ArrayList t640RecipeList = new ArrayList();
            for (Object recipeName : listtmp) {
                if (recipeName.toString().contains("recipe")) {
                    t640RecipeList.add(recipeName);
                }
            }
            resultMap.put("eppd", t640RecipeList);
        }
        return resultMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand Code">
    /*
     * tr t640支持的命令有:
     * PP-SELECT
     * START
     * STOP
     * PAUSE
     * RESUME
     * ABORT
     */
    public Map setEqptOnline() {
        return this.sendS1F17outByT640();
    }

    public Map setEqptOffline() {
        return this.sendS1F15outByT640();
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            String holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    public Map pauseDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("PAUSE");
            String holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        return null;//this.sendS2f41Cmd("RESUME");
//        return this.setEqptOffline();
    }
    // </editor-fold> 

    @Override
    public Map getRelativeFileInfo(String localFilePath, String targetRecipeName) {
        Map map = new HashMap();
        List<String> hanAndComp = TrRecipeUtil.readRCP(localFilePath);
        String hanRcpName = "";
        String hanRcpPath = "";
        String compRcpName = "";
        String compRcpPath = "";
        for (String str : hanAndComp) {
            if (str.contains("@handler@")) {
                hanRcpName = str.replace("@", "/");
                if (localFilePath.contains("_")) {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("hanRcpName", hanRcpName);
                map.put("hanRcpPath", hanRcpPath);
            } else if (str.contains("@component@")) {
                compRcpName = str.replace("@", "/");
//                compRcpName = str + targetRecipeName.substring(targetRecipeName.lastIndexOf("_")).replace("@", "/");
                if (localFilePath.contains("_")) {
                    compRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    compRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("compRcpName", compRcpName);
                map.put("compRcpPath", compRcpPath);
            } else {
                return null;
            }
        }
        return map;
    }

    private Recipe checkRecipeHasGoldFlag(String recipeName) {
        Recipe checkResult = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(recipeName, deviceType, "GOLD", null);
        if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在：" + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
            this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
        } else {
            checkResult = downLoadGoldRecipe.get(0);
        }
        return checkResult;
    }

    @Override
    protected boolean checkRecipeName(String recipeName) {
        boolean checkResult = false;
        if (recipeName.equals(ppExecName)) {
            checkResult = true;
        }
        if (!checkResult) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + recipeName + ">,equipment will be locked.");
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
        }
        return checkResult;
    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        List<Attach> attachs = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        String attachPath = recipeService.organizeUploadRecipePath(recipe);
        String recipePath = GlobalConstants.localRecipePath + attachPath + recipe.getRecipeName().replace("/", "@") + "_V" + recipe.getVersionNo() + ".txt";
        Map attachPathMap = getRelativeFileInfo(recipePath, "");
        String attachName = recipe.getRecipeName().replaceAll("/", "@") + "_V" + recipe.getVersionNo();
        for (int i = 0; i < 3; i++) {
            if (i == 1) {
                attachName = String.valueOf(attachPathMap.get("compRcpName")).replaceAll("/", "@") + "_V" + recipe.getVersionNo();
            }
            if (i == 2) {
                attachName = String.valueOf(attachPathMap.get("hanRcpName")).replaceAll("/", "@") + "_V" + recipe.getVersionNo();
            }
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachName(attachName);
            attach.setAttachPath(attachPath);
            sqlSession.close();
            attach.setAttachType("txt");
            attach.setSortNo(0);
            if (GlobalConstants.sysUser != null) {
                attach.setCreateBy(GlobalConstants.sysUser.getId());
                attach.setUpdateBy(GlobalConstants.sysUser.getId());
            } else {
                attach.setCreateBy("System");
                attach.setUpdateBy("System");
            }
            attachs.add(attach);
        }
        return attachs;

    }



    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String checkEquipStatus() {
        findDeviceRecipe();
        if (GlobalConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！";
        }
        if (GlobalConstant.STATUS_IDLE.equalsIgnoreCase(equipStatus) || equipStatus.equalsIgnoreCase("UNKNOWN")) {
            return "0";
        } else {
            return "设备未处于" + GlobalConstant.STATUS_IDLE + "状态，不可调整Recipe！";
        }
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // 上传ftp
        FtpUtil.uploadFile(localRcpPath, GlobalConstants.getProperty("ftpPath") + remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
        List<String> rcpContent = TrRecipeUtil.readRCP(localRcpPath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        for (String item : rcpContent) {
            if (item.contains("@component@") || item.contains("@handler@")) {
                String relLocalPath = GlobalConstants.localRecipePath + new RecipeService(sqlSession).organizeUploadRecipePath(recipe) + item + "_V" + recipe.getVersionNo() + ".txt";
                String relRemotePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                FtpUtil.uploadFile(relLocalPath, relRemotePath, item + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "关联文件存储位置：" + relLocalPath);
            }
        }
        sqlSession.close();
        return true;
    }

    @Override
    public String testRUThere() {
//        try {
//            DataMsgMap s1f1out = new DataMsgMap("s1f1out", activeWrapper.getDeviceId());
//            long transactionId = activeWrapper.getNextAvailableTransactionId();
//            s1f1out.setTransactionId(transactionId);
//            DataMsgMap s1f2in = null ;
//
//            s1f2in=activeWrapper.sendS1F1out();
//            if (s1f2in != null) {
//                //如果回复取消会话，那么需要重新发送S1F13
//                if (s1f2in.getMsgSfName().contains("s1f0")) {
//                    logger.info("testRUThere成功,但是未正确回复消息,需要重新建立连接 ");
//                    return "0";
//                } else {
//                    logger.info("testRUThere成功、通信正常 ");
//                    return "0";
//                }
//            } else {
//                return "2";
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//            return "2";
//        }
        return super.testRUThere();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(final String recipeName) {

        final Map resultMap = new HashMap();
        resultMap.put("HCACK", 0);
        new Thread(new Runnable() {

            @Override
            public void run() {
//                DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
//                s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//                s2f41out.put("PPID", recipeName);


                resultMap.put("msgType", "s2f42");
                resultMap.put("deviceCode", deviceCode);
                DataMsgMap data = null;
                try {

                    sendS2f41Cmd("REMOTE");
                    Thread.sleep(4000);
                    Map cpmap = new HashMap();
                    cpmap.put(CPN_PPID, recipeName);
                    Map cpNameFromatMap = new HashMap();
                    cpNameFromatMap.put(CPN_PPID, SecsFormatValue.SECS_ASCII);
                    Map cpValueFromatMap = new HashMap();
                    cpValueFromatMap.put(recipeName, SecsFormatValue.SECS_ASCII);
                    List cplist = new ArrayList();
                    cplist.add(CPN_PPID);
                    data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cpmap, cpNameFromatMap, cpValueFromatMap);

                    logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
                    byte  hcack = (byte) ( data.get("HCACK"));
                    logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
                    resultMap.put("HCACK", hcack);
                    resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
                } catch (Exception e) {
                    logger.error("Exception:", e);
                    resultMap.put("HCACK", 9);
                    resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9"  + " means " + e.getMessage());
                }

            }
        }).start();
        return resultMap;
    }

}
