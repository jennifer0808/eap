package cn.tzauto.octopus.secsLayer.equipImpl.asm.da;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.alarm.service.AutoAlter;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.asm.AsmAD8312RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AsmAD8312Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AsmAD8312Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    public String Datelength;


    public AsmAD8312Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        StripMapUpCeid = 237L;
        EquipStateChangeCeid = 4L;
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    AsmAD8312Host.sleep(200);
                }
                if (this.getCommState() != AsmAD8312Host.COMMUNICATING) {
                    sendS1F13out();
                }
//                if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
//                    sendS1F1out();
//                }
                if (rptDefineNum < 1) {
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS2F37outAll();
                    sendS5F3out(true);
                    Thread.sleep(2000);
                }

                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
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

    @Override
    public void inputMessageArrived(MsgArrivedEvent event) {

        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        logger.info("tagName:" + tagName);
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
            } else if (tagName.equalsIgnoreCase("s6f11in")) {

                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data); 
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
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
            } else if (tagName.equalsIgnoreCase("s12f67in")) {
                processS12F67in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="processS1FXin Code">

    @Override
    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        ppExecName = (String) listtmp.get(1);
        ppExecName = ppExecName.replace(".rcp", "");
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }


    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="processS6FXin Code">

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
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
            if (equipStatus.equalsIgnoreCase("run") && holdFlag) {
                holdDevice();
            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("ready")) {
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                } else {
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
//                            holdDeviceAndShowDetailInfo();
                            holdDeviceAndShowDetailInfo(" There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                            return;
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            setAlarmState(0);
                        }
                    }
                    logger.info("设备[" + deviceCode + "]的开机检查模式为:" + startCheckMod);
                    if (startCheckMod.contains("B")) {
                        startSVcheckPass = false;
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
                        startSVcheck();
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if (false) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            //这里要把设备上recipe的后缀加上，否则获取不到
                            downLoadRecipe.setRecipeName(downLoadRecipe.getRecipeName() + ".rcp");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                //这里要把设备上recipe的后缀加上，否则获取不到
                                downLoadGoldRecipe.get(0).setRecipeName(downLoadGoldRecipe.get(0).getRecipeName() + ".rcp");
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

    protected void processS6F11ControlStateChange(DataMsgMap data) {
        //回复s6f11消息
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        long ceid = 0L;
        long reportID = 0L;
        long controlStateTmp = 0L;
        try {
            out.setTransactionId(data.getTransactionId());
            ceid = data.getSingleNumber("CollEventID");
            reportID = data.getSingleNumber("ReportID");
            controlStateTmp = data.getSingleNumber("ControlState");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 1 && reportID == 1) {
            Map panelMap = new HashMap();
            if (controlStateTmp == 0) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备状态切换到OFF-LINE");
            }
            if (controlStateTmp == 1) {
                controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换到Local");
            }
            if (controlStateTmp == 2) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换到Remote");
            }
            equipState.setControlState(controlState);
            changeEquipPanel(panelMap);
        }
    }

    protected void processS6F11LoginUserChange(DataMsgMap data) {
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        long ceid = 0L;
        long reportID = 0L;
        String loginUserName = "";
        try {
            out.setTransactionId(data.getTransactionId());
            ceid = data.getSingleNumber("CollEventID");
            reportID = data.getSingleNumber("ReportId");
            loginUserName = ((SecsItem) data.get("UserLoginName")).getData().toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 120 && reportID == 120) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "登陆用户变更，当前登陆用户：" + loginUserName);
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
        if ("ASMAD8312PLUS".equals(deviceType)) {
            s7f1out.put("ProcessprogramID", targetRecipeName);
        } else {
            s7f1out.put("ProcessprogramID", targetRecipeName + ".rcp");
        }
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
        if ("ASMAD8312PLUS".equals(deviceType)) {
            s7f3out.put("ProcessprogramID", targetRecipeName);
        } else {
            s7f3out.put("ProcessprogramID", targetRecipeName + ".rcp");
        }
        s7f3out.put("Processprogram", secsItem);
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, FormatCode.SECS_BINARY);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
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

        recipeName = recipeName.replace(".rcp", "");
        if (!"ASMAD8312PLUS".equals(deviceType)) {
            recipeName = recipeName + ".rcp";
        }
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = null;
        List<RecipePara> recipeParaListExtra = null;
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) (data.get("PPBODY"));
            TransferUtil.setPPBody(ppbody, 1, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
//            recipeParaList = AsmAD8312RecipeUtil.transferRcpFromDB(recipePath, deviceType);
            //Recipe解析   
            if (deviceType.equals("ASMAD8312PLUS")) {
                recipeParaList = new ArrayList<>();
                recipeParaList = AsmAD8312RecipeUtil.transferRcpFromDBForPlus(recipePath, deviceType);
            } else {
                recipeParaList = AsmAD8312RecipeUtil.transferRcpFromDB(recipePath, deviceType);
                recipeParaListExtra = getRecipeParasByECSV();
                logger.info("recipeParaListExtra = " + recipeParaListExtra);
                Boolean result = recipeParaList.addAll(recipeParaListExtra);
                logger.info(result);
            }
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
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
        //TODO
        if ("ASMAD8312PLUS".equals(deviceType)) {
            s7f17out.put("ProcessprogramID", recipeName);
        } else {
            s7f17out.put("ProcessprogramID", recipeName + ".rcp");
        }
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
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s7f19out.setTransactionId(transactionId);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f19out);
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
            for (int i = 0; i < listtmp.size(); i++) {
                list1.add(listtmp.get(i).toString().replace(".rcp", ""));
            }
            resultMap.put("eppd", list1);
        }
        return resultMap;
    }
// </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="processS14FXin Code">

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="sendS1FXout Code">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="sendS2FXout Code">
    //释放机台
    @Override
    public Map releaseDevice() {
        Map map = new HashMap();// this.sendS2f41Cmd("START");
//        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
//            this.setAlarmState(0);
//        }
        map.put("HCACK", 0);
        setAlarmState(0);
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        if ("ASMAD8312PLUS".equals(deviceType)) {
            s2f41out.put("PPID", recipeName);
        } else {
            s2f41out.put("PPID", recipeName + ".rcp");
        }
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        return resultMap;
    }

    // </editor-fold>
    /*
     * (non-Javadoc) It only copies field member values except Mli.
     * @see java.lang.Object#clone()
     */

    @Override
    public Object clone() {
        AsmAD8312Host newEquip = new AsmAD8312Host(deviceId,
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
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

    // <editor-fold defaultstate="collapsed" desc="S12FX Code"> 

    /**
     * WaferMappingInfo Upload
     *
     * @param DataMsgMap
     * @return
     */
    @Override
    public Map processS12F1in(DataMsgMap DataMsgMap) {
        try {
            String MaterialID = (String) ((SecsItem) DataMsgMap.get("MaterialID")).getData();
            byte[] IDTYP = ((byte[]) ((SecsItem) DataMsgMap.get("IDTYP")).getData());
            upFlatNotchLocation = DataMsgMap.getSingleNumber("FlatNotchLocation");
//            long FileFrameRotation = DataMsgMap.getSingleNumber("FileFrameRotation");
            byte[] OriginLocation = ((byte[]) ((SecsItem) DataMsgMap.get("OriginLocation")).getData());
            long RowCountInDieIncrements = DataMsgMap.getSingleNumber("RowCountInDieIncrements");
            long ColumnCountInDieIncrements = DataMsgMap.getSingleNumber("ColumnCountInDieIncrements");
            uploadWaferMappingRow = String.valueOf(RowCountInDieIncrements);
            uploadWaferMappingCol = String.valueOf(ColumnCountInDieIncrements);
            //kong
            //String NullBinCodeValue = (String)((SecsItem) DataMsgMap.get("NullBinCodeValue")).getData();
            //byte[] ProcessAxis = ((byte[]) ((SecsItem) DataMsgMap.get("ProcessAxis")).getData());
            UiLogUtil.appendLog2SecsTab(deviceCode, "接受到机台上传WaferId：[" + MaterialID + "]设置信息！");
            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端上传机台WaferId：[" + MaterialID + "]设置信息！");
            DataMsgMap s12f2out = new DataMsgMap("s12f2out", activeWrapper.getDeviceId());
            //TODO 调用webservices回传waferMapping信息
            byte[] ack = new byte[]{0};
            s12f2out.put("SDACK", ack);
            s12f2out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.sendS12F2out((byte) 0, DataMsgMap.getTransactionId());

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0;
        try {
            if (data.get("CEID") != null) {
                ceid = (long) data.get("CEID");
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else {
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());

                if (ceid == EquipStateChangeCeid) {
                    processS6F11EquipStatusChange(data);
                }
            }
            if (ceid == 411) {
                logger.info("检测到设备触发LearnDevice事件");
                Map resultMap = new HashMap();
                resultMap.put("msgType", "s5f1");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("deviceId", deviceId);
                resultMap.put("ALID", "E20190328");
                resultMap.put("ALCD", 0);
                resultMap.put("ALTX", "Learn device");
                resultMap.put("Description", "Other categories");
                resultMap.put("TransactionId", data.getTransactionId());
                AutoAlter.alter(resultMap);
            }
            DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());

            this.setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
}
