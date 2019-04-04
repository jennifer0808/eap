package cn.tzauto.octopus.secsLayer.equipImpl.polish.pg300;


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
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.pg300.PG300RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pg300Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(Pg300Host.class.getName());
    private String portId = "";

    public Pg300Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
    }


    public Object clone() {
        Pg300Host newEquip = new Pg300Host(deviceId,
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
                    this.sendS1F13out();
                }
                if (rptDefineNum < 2) {
//                    sendS1F1out();
//                    //为了能调整为online remote
                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    //获取lot号/
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();//得到监听到的东西
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {

                    //sml中s6f11equipstatuschange data1，data2表示状态前后的两个值，需要测试得到
                    processS6F11EquipStatusChange(msg);

                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    try {
                        long ceid = msg.getSingleNumber("CollEventID");
                        processS6F11EquipStatus(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    ppExecName = (String) ((SecsItem) msg.get("PPExecName")).getData();
                    Map map = new HashMap();
                    map.put("PPExecName", ppExecName);
                    changeEquipPanel(map);
                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.error("Exception:", e);
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
            LastComDate = System.currentTimeMillis();
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            }  else if (tagName.contains("s6f11in")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                //todo replyS6F12WithACK
//                replyS6F12WithACK(data, ack);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
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

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    private void sendS1F3CheckPort() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3portstatecheck", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        String port1Status = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        String port2Status = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(1)), deviceType);
        if ("Ready".equalsIgnoreCase(port1Status)) {
            this.portId = "1";
        } else if ("Ready".equalsIgnoreCase(port2Status)) {
            this.portId = "2";
        } else {
            this.portId = "";
        }
    }

    @SuppressWarnings("unchecked")
    public Map sendS1F3RcpParaCheckout(List svidlist) {
        DataMsgMap s1f3out = new DataMsgMap("s1f3" + deviceType + "RcpPara", activeWrapper.getDeviceId());
        DataMsgMap dataHashtable = null;
        s1f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        for (int i = 0; i < svidlist.size(); i++) {
            long[] svid = new long[1];
            // Long.parseLong(svidlist.get(i));
            svid[0] = Long.parseLong(svidlist.get(i).toString());
            s1f3out.put("Data" + i, svid);
        }
        try {
            dataHashtable = activeWrapper.sendAwaitMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = new ArrayList<>();
        if (dataHashtable != null) {
            list = (ArrayList) ((SecsItem) dataHashtable.get("RESULT")).getData();
        }
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("SVList", listtmp);
        resultMap.put("Description", "Get SVList from equip " + listtmp);
        return resultMap;
    }
    // </editor-fold> 

    // <editor-fold defaultstate="collapsed" desc="S2FX Code">
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        long[] portIdL = new long[1];
        if ("1".equals(this.portId)) {
            portIdL[0] = 1;
        } else if ("2".equals(this.portId)) {
            portIdL[0] = 2;
        } else {
            return null;
        }
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName);
        s2f41out.put("PORTID", portIdL);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    private Map sendS2F41Grant() {
        DataMsgMap s2f41out = new DataMsgMap("s2f41grant", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to Grand");
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd GRANT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd GRANT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
//            UiLogUtil.appendLog2SecsTab(deviceCode, "收到CEID: " + ceid);
            if (ceid == 9) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 10) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 8) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            } else if (ceid == 44) {
                this.processS6F11EquipStatusChange(data);
            } else if (ceid == 45 || ceid == 46) {
                checkPortStatusAndPPSelectToLocal();
            } else if (ceid == 57 || ceid == 59 || ceid == 56 || ceid == 58) {
                sendS2F41Grant();
            }
            super.sendS1F3Check();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //更新主界面
        sendS1F3Check();

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            boolean dataReady = false;
            //判断当前执行程序是否是清模程序 Y代表清模程序
            boolean isCleanRecipe = false;
            List<Recipe> cleanRecipes = recipeService.searchRecipeByRcpType(ppExecName, "Y");
            if (cleanRecipes != null && cleanRecipes.size() > 1) {
                isCleanRecipe = true;
            }
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
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
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
                    holdDeviceAndShowDetailInfo();
                } else {
                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查

                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                        if (!checkResult) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
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
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
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
    // </editor-fold> 

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">  上传下载方法
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName.replace("@", "/"));
        s7f3out.put("Processprogram", secsItem);
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
        String ppid = recipe.getRecipeName();
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        DataMsgMap dataHashtable = null;
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", ppid);
        try {
            dataHashtable = activeWrapper.sendAwaitMessage(s7f5out);  //得到S7f5发出去回复的东西
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        ppid = (String) ((SecsItem) dataHashtable.get("ProcessprogramID")).getData();
        byte[] ppbody = (byte[]) ((SecsItem) dataHashtable.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = PG300RecipeUtil.pg300RecipeTran(recipePath);
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
        resultMap.put("Descrption", " Recive the recipe " + ppid + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

    private void checkPortStatusAndPPSelectToLocal() {
        sendS1F3CheckPort();
        if ("".equals(this.portId)) {
            return;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null) {
            String recipeName = deviceInfoExt.getRecipeName();
            Map reMap = sendS7F19out();
            if (reMap != null && reMap.get("eppd") != null) {
                ArrayList<String> recipeNameList = (ArrayList) reMap.get("eppd");
                if (recipeNameList.contains(recipeName)) {
                    Map resultMap = sendS2F41outPPselect(recipeName);
                    if (resultMap != null && !resultMap.isEmpty()) {
                        if ("0".equals(String.valueOf(resultMap.get("HCACK"))) || "4".equals(String.valueOf(resultMap.get("HCACK")))) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "PPSelect成功，PPID=" + recipeName);
                            //选完程序切local状态
                            localDeviceAndShowDetailInfo();
                        } else {
                            Map eqptStateMap = super.findEqptStatus();//失败上报机台状态
                            UiLogUtil.appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + "；原因：" + String.valueOf(resultMap.get("Description")) + "，机台状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                        }
                    } else {
                        UiLogUtil.appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + ", 设备消息回复错误，请联系CIM人员处理");
                    }
                } else {
                    UiLogUtil.appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + ", 设备Recipe列表中没有该Recipe，请重新下载");
                }
            } else {
                UiLogUtil.appendLog2EventTab(deviceCode, "无法获取设备Recipe列表，请检查设备通信");
            }
        }
    }


//事件报告

    private void initRptPara() {
//sendS6F23clear();
        //重写事件 报告
//            sendS2f33out(50007l, 902l, 906l);
//            sendS2f35out(50007l, 50007l, 50007l);
//            sendS2F37out(50007l);
        sendS2F37outCloseAll();
        sendS2F33clear();
        sendS2F35clear();
        sendS2F33init();
        sendS2F35init();
        sendS2F37outAll();   //开启所有事件报告
    }

    //hold机台，先停再锁
    @SuppressWarnings("unchecked")
    public void sendS2F33clear() {
        DataMsgMap s2f37outAll = new DataMsgMap("s2f33clear", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35clear() {
        DataMsgMap s2f37outAll = new DataMsgMap("s2f35clear", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS6F23clear() {
        DataMsgMap s2f37outAll = new DataMsgMap("s6f23out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F33init() {
        DataMsgMap s2f37outAll = new DataMsgMap("s2f33initout", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35init() {
        DataMsgMap s2f37outAll = new DataMsgMap("s2f35initout", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public Map pauseDevice() {
        return this.sendS2f41Cmd("PAUSE");
    }

    public Map resumeDevice() {
        return this.sendS2f41Cmd("RESUME");
    }

    public Map localDevice() {
        return this.sendS2f41Cmd("LOCAL");
    }

    @Override
    public Map holdDevice() {
        sendS1F3Check();
        if (!this.controlState.equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
            UiLogUtil.appendLog2EventTab(deviceCode, "当前设备不在Remote状态，无法进行锁机");
            return null;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            super.sendS2f41Cmd("STOP");
            Map map = this.sendS2f41Cmd("PAUSE");
            if ((byte) map.get("HCACK") == 0) {
                this.setAlarmState(2);
            }
            return map;
        } else {

            //todo 显示界面锁机日志
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        Map map = this.sendS2f41Cmd("RESUME");
        if ((byte) map.get("HCACK") == 0) {
            this.setAlarmState(0);
        }
        return map;
    }

    public boolean localDeviceAndShowDetailInfo() {
        Map resultMap = new HashMap();
        resultMap = localDevice();
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备已经被切至Local");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备已处于Local状态");
                return true;
            } else {
                UiLogUtil.appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + String.valueOf(resultMap.get("Description")));
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备切Local失败，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
