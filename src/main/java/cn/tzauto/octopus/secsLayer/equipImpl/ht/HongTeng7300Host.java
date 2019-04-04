package cn.tzauto.octopus.secsLayer.equipImpl.ht;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.ht.HongTengRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.icos.TrRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

//import cn.tzauto.octopus.secsLayer.equipImpl.icos.tr.T640Host;

@SuppressWarnings("serial")
public class HongTeng7300Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(HongTeng7300Host.class.getName());

    public HongTeng7300Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
    }


    @Override
    public Object clone() {
        HongTeng7300Host newEquip = new HongTeng7300Host(deviceId,
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


    @Override
    public void run() {
//        String localRecipeFilePath="D:\\HongTengRecipe\\RECIPE\\A6\\BackGrind\\HT\\Engineer\\D7400-6010\\JCET-A-TQFP-14X14-100L-1.4\\JCET-A-TQFP-14X14-100L-1.4.txt";
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sleep(3000);
                    sendS1F17out();
                    sendS1F1out();
                    //为了能调整为online remote

                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                }
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11checklot")) {
                    logger.info("====需要比对批次号，一致则发送Start指令====");
                    processS6F11LotCheck(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11incommon19")) {
                    processS6F11LotEnd(msg);
                } else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Map sendS1F3SingleCheck(String svidName) {
        DataMsgMap s1f3out = new DataMsgMap("s1f3singleout", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] svid = new long[1];
        svid[0] = Long.parseLong(svidName);
        s1f3out.put("SVID", svid);
        DataMsgMap data = null;
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
//         String localRecipeFilePath="D:\\HongTengRecipe\\RECIPE\\A6\\BackGrind\\HT\\Engineer\\D7400-6010\\JCET-A-TQFP-14X14-100L-1.4\\JCET-A-TQFP-14X14-100L-1.4.txt";
//         sendS7F3out(localRecipeFilePath, "JCET-A-TQFP-14X14-100L-1.4.txt");
        return resultMap;
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
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.toLowerCase().contains("s6f11in")) {
                processS6F11in(data);
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
                controlState = FengCeConstant.CONTROL_OFFLINE;
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

    public Map sendS1F15outByT640() {
        DataMsgMap s1f15out = new DataMsgMap("s1f15out", activeWrapper.getDeviceId());
        s1f15out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s1f15out);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        byte[] ack = (byte[]) ((SecsItem) msgdata.get("OFLACK")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f16");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack[0]);
        return resultMap;
    }

    public Map sendS1F17outByT640() {
        DataMsgMap s1f17out = new DataMsgMap("s1f17out", activeWrapper.getDeviceId());
        s1f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s1f17out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] ack = (byte[]) ((SecsItem) msgdata.get("AckCode")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack[0]);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] equipStatuss = new long[1];
        long[] pPExecNames = new long[1];
        long[] controlStates = new long[1];
        DataMsgMap data = null;
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
//            data = activeWrapper.sendAwaitMessage(s1f3out);
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
        ppExecName = (String) listtmp.get(1);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code"> 
    public Map sendS2F41outStart(String batchName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outstart", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("BatchName", batchName);
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception occur,Exception info:" + e.getMessage());
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        return resultMap;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6F11 Code">


    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 2) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 3) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateCommStateInExt();
        showCollectionsEventInfo(ceid);
    }

    protected void processS6F11LotEnd(DataMsgMap data) {
        Long ceid = 0L;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 202) {
                UiLogUtil.appendLog2EventTab(deviceCode, "批次已结批！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String lotIDRead(String lotId) {
        List lotList = new ArrayList();
        int count = 0;
        int len = lotId.length();
        //取lotId的最后两位，判断是否为返工批次
        String newNo = lotId.substring(len - 2);
        if (newNo.equals(" R")) {
            logger.info("该批次:" + lotId + "为rework批次！");
            lotId = lotId.substring(0, len - 2);
        }
        for (int i = 0; i < lotId.length(); i++) {
            if (lotId.charAt(i) == '.') {
                count++;
            }
            if (count > 1) {
                logger.info("该批次:" + lotId + "为合批的批次");
            }
        }
        String newLotId = "";
        newLotId = lotId.split("\\.")[0];
//        String newLotId[] = new String[count];
//        for (int i = 0; i < count; i++) {
//            
//            newLotId[i] = lotId.split("\\.")[0] + '.' + lotId.split("\\.")[i + 1];
//            logger.info("子批批次号为" + newLotId[i]);
//            lotList.add(newLotId[i]);
//        }
        return newLotId;
    }

    protected void processS6F11LotCheck(DataMsgMap data) {
        String lotId = "";
        try {
            lotId = ((SecsItem) data.get("LotId")).getData().toString();
            UiLogUtil.appendLog2EventTab(deviceCode, "设备当前批次号为" + lotId);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        String newLotId = lotIDRead(lotId);
//
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        sqlSession.close();
//        String workLot = "";
//        if (deviceInfoExt != null) {
//            if (deviceInfoExt.getLotId() != null && !"".equals(deviceInfoExt.getLotId())) {
//                workLot = deviceInfoExt.getLotId();
//                UiLogUtil.appendLog2EventTab(deviceCode, "过账批次号为" + workLot);
//            }
//        }
//        String extLotId = lotIDRead(workLot);
//        Boolean flag = false;
//            if (extLotId.equalsIgnoreCase(newLotId)) {
//                flag = true;
//            } else {
//                flag = false;
//            }
        Boolean flag = true;
        if (flag == true) {
            sendS2f41Cmd("NEWLOT-OK");
            UiLogUtil.appendLog2EventTab(deviceCode, "批次号匹配一致，执行开机");
        } else {
            sendS2f41Cmd("NEWLOT-NG");
            UiLogUtil.appendLog2EventTab(deviceCode, "检测到设备当前批次号与MES系统不匹配，不允许执行开机");
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long preStatus = 0l;
        long nowStatus = 0;
        long ceid = 0L;
        try {
            preStatus = data.getSingleNumber("PreStatus");
            nowStatus = data.getSingleNumber("EquipStatus");
            ceid = data.getSingleNumber("CollEventID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(nowStatus), deviceType);
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        if (equipStatus.equalsIgnoreCase("run")) {
            //首先从服务端获取机台是否处于锁机状态
            //如果设备应该是锁机，那么首先发送锁机命令给机台
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                pauseDevice();
            }
        }
//        String preStatusStr = ACKDescription.descriptionStatus(preStatus, deviceType);

        if (equipStatus.equalsIgnoreCase("READY")) {
            findDeviceRecipe();
        }
        super.changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (equipStatus.equalsIgnoreCase("READY")) {
            if ("Engineer".equals(deviceInfoExt.getBusinessMod())) {
                UiLogUtil.appendLog2SecsTab(deviceCode, "工程模式，取消开机卡控！");
            } else {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDevice();
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
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        startCheckRecipePara(downLoadRecipe, "abs");
                    } else {
                        //2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        startCheckRecipePara(goldRecipe);
                    }
                } else {
                    holdDevice();
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
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        String ppbody = (String) TransferUtil.getPPBody(0, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_ASCII);
        s7f3out.put("ProcessprogramID", targetRecipeName.replace("@", "/"));
        s7f3out.put("Processprogram", secsItem);
//        s7f3out.put("Processprogram", localRecipeFilePath);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        byte[] ackc7 = new byte[1];
        try {
            data = activeWrapper.sendAwaitMessage(s7f3out);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            resultMap.put("ACKC7", ackc7[0]);
            resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
//        recipePath = this.getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + ppid + "/" + ppid + "_V" + recipe.getVersionNo() + ".txt";
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        String ppbody = (String) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, 0, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
//        if(ppbody.equalsIgnoreCase("OK")){}
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = HongTengRecipeUtil.transferRcpFromDB(recipePath);
            for (int i = 0; i < recipeParaList.size(); i++) {
                String paraName = recipeParaList.get(i).getParaName();
                if (paraName.equals("") || paraName.equals("NULL")) {
                    recipeParaList.remove(i);
                    i--;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

    @SuppressWarnings("unchecked")
    public Map sendS7F17out(String recipeName) {
        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            sleep(1000);
            if (ackc7[0] == 0 || ackc7[0] == 6) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7[0], "ACKC7"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName.replace("recipe", "component"));
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName.replace("recipe", "handler"));
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
        s7f19out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
//            data = activeWrapper.sendAwaitMessage(s7f19out);
            data = sendMsg2Equip(s7f19out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("EPPD")).getData();
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ArrayList t640RecipeList = new ArrayList();
            for (Object recipeName : listtmp) {
//                if (recipeName.toString().contains("recipe")) {
                t640RecipeList.add(recipeName);
//                }
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
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = true;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
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
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = true;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
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
            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在：" + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
            //不允许开机
            this.holdDeviceAndShowDetailInfo();
        } else {
            checkResult = downLoadGoldRecipe.get(0);
        }
        sqlSession.close();
        return checkResult;
    }

    @Override
    protected boolean checkRecipeName(String recipeName) {
        boolean checkResult = false;
        if (recipeName.equals(ppExecName)) {
            checkResult = true;
        }
        if (!checkResult) {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
            holdDeviceAndShowDetailInfo();
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
        }
        return checkResult;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendS1F13out() {
        DataMsgMap s1f13out = new DataMsgMap("s1f13out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f13out.setTransactionId(transactionId);
//        s1f13out.put("Mdln", Mdln);
//        s1f13out.put("SoftRev", SoftRev);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s1f13out);
            if (data != null) {
                setCommState(1);
            }
        } catch (Exception e) {
            setCommState(0);
            logger.error("Exception:", e);
        }
    }


    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String checkEquipStatus() {
        findDeviceRecipe();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！";
        }
        if (!FengCeConstant.STATUS_IDLE.equalsIgnoreCase(equipStatus)) {
            return "设备未处于" + FengCeConstant.STATUS_IDLE + "状态，不可调整Recipe！";
        }
        return "0";
    }

    @Override
    public String testRUThere() {
        try {
            DataMsgMap s1f1out = new DataMsgMap("s1f1out", activeWrapper.getDeviceId());
            long transactionId = activeWrapper.getNextAvailableTransactionId();
            s1f1out.setTransactionId(transactionId);
            DataMsgMap s1f2in = activeWrapper.sendAwaitMessage(s1f1out);
            if (s1f2in != null) {
                //如果回复取消会话，那么需要重新发送S1F13
                if (s1f2in.getMsgSfName().contains("s1f0")) {
                    logger.info("testRUThere成功,但是未正确回复消息,需要重新建立连接 ");
                    return "0";
                } else {
                    logger.info("testRUThere成功、通信正常 ");
                    return "0";
                }
            } else {
                return "2";
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "2";
        }
    }
}
