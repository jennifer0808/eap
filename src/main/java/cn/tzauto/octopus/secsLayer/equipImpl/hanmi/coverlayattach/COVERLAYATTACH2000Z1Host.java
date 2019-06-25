package cn.tzauto.octopus.secsLayer.equipImpl.hanmi.coverlayattach;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class COVERLAYATTACH2000Z1Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(COVERLAYATTACH2000Z1Host.class.getName());

    public COVERLAYATTACH2000Z1Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public Object clone() {
        COVERLAYATTACH2000Z1Host newEquip = new COVERLAYATTACH2000Z1Host(deviceId,
                this.iPAddress,
                this.tCPPort, this.connectMode,
                this.deviceType, this.deviceCode);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.clear();
        return newEquip;
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
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                    initRptPara();
//                    getSvidListFromDevice();
//                    getEcidListFromDevice();
                }
                if (holdFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null) {
                    if (msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                        processS14F1in(msg);
                    } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                        processS6F11in(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                        this.processS5F1in(msg);
                    } else {
                        System.out.println("A message in queue with tag = " + msg.getMsgSfName()
                                + " which I do not want to process! ");
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    /**
     * 打开或者关闭所有报警信息报告
     *
     * @param enable true->open ；false-->close
     */
    @Override
    public void sendS5F3out(boolean enable) {
        byte aled;
        boolean[] flag = new boolean[1];
        flag[0] = enable;
        if (enable) {
            aled = 1;
        } else {
            aled = 0;
        }
        try {
            activeWrapper.sendS5F3out(aled, -1, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 40033L || ceid == 40039L || ceid == 40040L) {
                processS6F11EquipStatusChange(data);
            } else if (ceid == 40101L) {
                processS6F11stripIdRead(data);
            } else if (ceid == 40102L) {
                processS6F11stripIdRead(data);
            }
//            || ceid == 40102L 出料时的CEID
        } catch (Exception e) {
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
            LastComDate = System.currentTimeMillis();
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
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.toLowerCase().contains("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
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


    // <editor-fold defaultstate="collapsed" desc="sendS2FXout Code">

    public void getSvidListFromDevice() {
        try {
            DataMsgMap dataMsgMap = activeWrapper.sendS1F11out(null, svFormat);
            List svIdList = new ArrayList();
            List resultList = (List) dataMsgMap.get("SVNRR");
            for (int i = 0; i < resultList.size(); i++) {
                List svContentList = (List) resultList.get(i);
                svIdList.add(Long.valueOf(((long[]) svContentList.get(0))[0]));
            }
            DataMsgMap svValueResult = activeWrapper.sendS1F3out(svIdList, svFormat);
            Map<String, String> svMap = new HashMap<>();
            List svList = (ArrayList) svValueResult.get("SV");
            for (int i = 0; i < svIdList.size(); i++) {
                svMap.put(svIdList.get(i).toString(), svList.get(i).toString());
            }
            System.out.println(svMap);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    public void getEcidListFromDevice() {
        try {
            DataMsgMap sfNameout = new DataMsgMap("S2F29OUT", Integer.valueOf(this.deviceId));
            long transactionId = activeWrapper.getNextAvailableTransactionId();
            sfNameout.setTransactionId(transactionId);
            SecsItem vRoot = new SecsItem();
            vRoot.setFormatCode(0);
            ArrayList rootData = new ArrayList();
            vRoot.setData(rootData);
            sfNameout.put("S2F29OUT", vRoot);
            DataMsgMap dataMsgMap = activeWrapper.sendAwaitMessage(sfNameout);
            Map<String, String> ecMap = new HashMap<>();
            List resultList = (ArrayList) ((SecsItem) dataMsgMap.get("S2F30IN")).getData();
            List ecidList = new ArrayList();
            for (int i = 0; i < resultList.size(); i++) {
                List ecContentList = (ArrayList) ((SecsItem) resultList.get(i)).getData();
                Long ecid = Long.valueOf(((long[]) ((SecsItem) ecContentList.get(0)).getData())[0]);
                ecidList.add(ecid);
            }

            DataMsgMap ecValueRuselt = activeWrapper.sendS2F13out(ecidList, ecFormat);
            Object obj = ecValueRuselt.get("EC");
            List ecList = new ArrayList();
            if (obj != null) {
                if (obj instanceof ArrayList) {
                    ecList = (ArrayList) obj;
                } else {
                    ecList.add(obj);
                }
            }
            for (int i = 0; i < ecidList.size(); i++) {
                ecMap.put(ecidList.get(i).toString(), ecList.get(i).toString());
            }
            System.out.println(ecMap);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="processS6FXin Code">
//    @Override
//    protected void processS6F11EquipStatusChange(DataMsgMap data) {
//        long ceid = 0L;
//        try {
//            ceid = (long) data.get("CEID");
//            findDeviceRecipe();
////            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
//            if (equipStatus.equalsIgnoreCase("Run")) {
//                sendS2f41Cmd("REMOTE");
//            } else if (equipStatus.equalsIgnoreCase("pause") || equipStatus.equalsIgnoreCase("ldle") || equipStatus.equalsIgnoreCase("end")) {
//                sendS2f41Cmd("LOCAL");
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        //将设备的当前状态显示在界面上
//        Map map = new HashMap();
//        map.put("EquipStatus", equipStatus);
//        changeEquipPanel(map);
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        RecipeService recipeService = new RecipeService(sqlSession);
//        try {
//            //从数据库中获取当前设备模型信息
//            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//            boolean dataReady = false;
//            // 更新设备模型
//            if (deviceInfoExt == null) {
//                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
//                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
//            } else {
//                deviceInfoExt.setDeviceStatus(equipStatus);
//                deviceService.modifyDeviceInfoExt(deviceInfoExt);
//                sqlSession.commit();
//                dataReady = true;
//            }
//
//            //保存到设备操作记录数据库
//            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
//            sqlSession.commit();
//            if (equipStatus.equalsIgnoreCase("idle")) {
//                holdFlag = false;
//            }
//            boolean checkResult = false;
//            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
//            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
//                if (holdFlag) {
//                    holdDevice();
//                }
//                //首先从服务端获取机台是否处于锁机状态
//                //如果设备应该是锁机，那么首先发送锁机命令给机台
//                if (this.checkLockFlagFromServerByWS(deviceCode)) {
//                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    holdDeviceAndShowDetailInfo();
//                } else {
//                    //1、获取设备需要校验的信息类型,
//                    String startCheckMod = deviceInfoExt.getStartCheckMod();
//                    boolean hasGoldRecipe = true;
//                    //查询trackin时的recipe和GoldRecipe
//                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//
//                    if (checkResult && "A".equals(startCheckMod)) {
//                        //首先判断下载的Recipe类型
//                        //1、如果下载的是Unique版本，那么执行完全比较
//                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
//                        if ("Unique".equals(downloadRcpVersionType)) {
//                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
////                            this.startCheckRecipePara(downLoadRecipe, "abs");
//                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
//                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
//                            if (!hasGoldRecipe) {
//                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                                //不允许开机
//                                this.holdDeviceAndShowDetailInfo();
//                                return;
//                            } else {
//                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
////                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
//                            }
//
//                        }
//                    } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
//                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
//    }

    /**
     * hold设备，并且显示具体的hold设备具体信息
     *
     * @return
     */
    @Override
    public boolean holdDeviceAndShowDetailInfo() {
        Map resultMap = new HashMap();
        resultMap = holdDevice();

        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "当前设备已经被锁机");
                this.sendTerminalMsg2EqpSingle("StartCheck not pass, equipment locked!");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行锁机");
                return true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + String.valueOf(resultMap.get("Description")));
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                return false;
            }
        } else {
            return false;
        }
    }

    protected void processS6F11stripIdRead(DataMsgMap data) {
        //webserverice  校验
        String stripId = "";
        long ceid = 0L;
        String funcType = "";
        try {
            ceid = (long) data.get("CEID");
            ArrayList reportList = (ArrayList) data.get("REPORT");
            List idList = (List) reportList.get(1);
            stripId = (String) idList.get(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Map msgMap = new HashMap();

        if (ceid == 40101L) {
            msgMap.put("msgName", "ceStripLoad");
            funcType = "MesAolotLoadCheck";
            logger.info("get stripid:[" + stripId + "]ceid:" + ceid + "[" + funcType + "]");
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "读取到StripId:[" + stripId + "],进行检查...");
//        String result = AxisUtility.findMesAoLotService(deviceCode, stripId, funcType);

            msgMap.put("deviceCode", deviceCode);
            msgMap.put("stripId", stripId);
            String result = "";
//        result = "Y";
            try {
                result = AxisUtility.plasma88DService(deviceCode, stripId, funcType);
            } catch (Exception ex) {
                logger.error("WebService sendMessageWithReplay error!" + ex.getMessage());
            }
//        sendS2f41Cmd("REMOTE");
            if (result.equalsIgnoreCase("Y")) {
                //todo 测试 if(true){
                holdFlag = false;
                this.sends2f41stripReply(true);
            } else {
                holdFlag = true;
                this.sends2f41stripReply(false);
                sendS2f41Cmd("STOP");
                if ("".equals(result)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "等待Server回复超时,请检查网络设置!");
                }
            }
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "StripId:[" + stripId + "]检查结果:[" + result + "]");
//        sendS2f41Cmd("LOCAL");
//        changeEqptControlStateAndShowDetailInfo("LOCAL");
        }
        if (ceid == 40102L) {
            msgMap.put("msgName", "ceStripUnload");
            funcType = "MesAolotUnLoadCheck";
            logger.info("get stripid:[" + stripId + "]ceid:" + ceid + "[" + funcType + "]");
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "读取到StripId:[" + stripId + "],进行检查...");
//            String result = AxisUtility.findMesAoLotService(deviceCode, stripId, funcType);

            msgMap.put("deviceCode", deviceCode);
            msgMap.put("stripId", stripId);
            String result = "";
            try {
                if (!"".equals(stripId)){
                    result = AxisUtility.plasma88DService(deviceCode, stripId, funcType);
                }
            } catch (Exception ex) {
                logger.error("WebService sendMessageWithReplay error!" + ex.getMessage());
            }
        }
    }

    protected void sends2f41stripReply(boolean isOk) {
        try {
            Map cp = new HashMap();
            cp.put("RESULT", isOk);
            Map cpName = new HashMap();
            cpName.put("RESULT", FormatCode.SECS_ASCII);
            Map cpValue = new HashMap();
            cpValue.put(isOk, FormatCode.SECS_BOOLEAN);
            List list = new ArrayList();
            list.add("RESULT");
            activeWrapper.sendS2F41out("STRIP_LOAD_CONFIRM", list, cp, cpName, cpValue);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        }
    }

    // </editor-fold>

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = getRecipeParasByECSV();
        } catch (Exception ex) {
            ex.printStackTrace();
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

//    @SuppressWarnings("unchecked")
//    @Override
//    public Map sendS7F17out(String recipeName) {
//        recipeName = recipeName + ".rcp";
//        return super.sendS7F17out(recipeName);
//    }
// </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="sendS2FXout Code">
    //释放机台

    @Override
    public Map releaseDevice() {
        Map map = new HashMap();
        map.put("HCACK", 0);
        return map;
    }

    // </editor-fold>
    /*
     * (non-Javadoc) It only copies field member values except Mli.
     * @see java.lang.Object#clone()
     */


    private void initRptPara() {
        ArrayList<Long> list = new ArrayList();
        list.add(10156L);
        super.sendS2F33out(40102L, 40102L, list);
        sendS2F35out(40102L, 40102L, 40102L);
        sendS2F37outAll();
//        if (equipStatus.equalsIgnoreCase("Run")) {
//            sendS2f41Cmd("REMOTE");
//        } else if (equipStatus.equalsIgnoreCase("pause") || equipStatus.equalsIgnoreCase("ldle") || equipStatus.equalsIgnoreCase("end")) {
//            sendS2f41Cmd("LOCAL");
//        }
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            sendS2f41Cmd("REMOTE");
            Map map = this.sendS2f41Cmd("STOP");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
            }
//            sendS2f41Cmd("LOCAL");
            return map;
        } else {
//            sendS2f41Cmd("LOCAL");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }
}
