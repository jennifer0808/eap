package cn.tzauto.octopus.secsLayer.equipImpl.asm.da;


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
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.asm.AsmAD8312RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author leo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2019-06-28
 * @(#)EquipHost.java
 * @Copyright tzinfo, Ltd. 2016. This software and documentation contain
 * confidential and proprietary information owned by tzinfo, Ltd. Unauthorized
 * use and distribution are prohibited. Modification History: Modification Date
 * Author Reason class Description
 */
public class AsmAD8312PlusHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AsmAD8312PlusHost.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    public String Datelength;

    public AsmAD8312PlusHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        StripMapUpCeid = 237L;
        EquipStateChangeCeid = 5L;
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public Object clone() {
        AsmAD8312PlusHost newEquip = new AsmAD8312PlusHost(deviceId,
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
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    AsmAD8312PlusHost.sleep(200);
                }
                if (this.getCommState() != AsmAD8312PlusHost.COMMUNICATING) {
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
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) { //237
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11EquipStatusChange")) { // 8
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11ControlStateChange")) { //5 6 7 47 10
                    processS6F11ControlStateChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11LoginUserChange")) { //ignore
                    processS6F11LoginUserChange(msg);
                } else if (msg.getMsgSfName().toLowerCase().contains("s6f11incommon")) {
                    if (msg.getSingleNumber("CollEventID") == 8L) {
                        logger.info("当前机台状态发生改变！2");
                        processS6F11EquipStatusChange(msg);
                    }
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
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
                if ((long) data.get("CEID") == 8L) {
                    logger.info("当前机台状态发生改变！");
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
//                processS6F11inStripMapUpload(data);
                long ceid = (long) data.get("CEID");
                if (ceid == 237L) {
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equals("s6f11EquipStatusChange")) {

                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equals("s6f11ControlStateChange")) {

                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equals("s6f11LoginUserChange")) {

                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equals("s6f11PPExecName")) {

                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {

                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {

                replyS6F12WithACK(data, (byte) 0);
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

    //TODO  寻找ceid
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
                replyS6F12WithACK(data,(byte)0);
                if (ceid == EquipStateChangeCeid || ceid == 8 || ceid == 47  ) {
                    processS6F11EquipStatusChange(data);
                } else if ( ceid == 6 || ceid == 7 || ceid == 9 || ceid == 10 ) {
                    findDeviceRecipe();
                } else {
                    processS6F11EquipStatus(data);
                }
            }
            this.setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatus(DataMsgMap data) {
        //回复s6f11消息
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        showCollectionsEventInfo(ceid);
    }

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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
                dataReady = true;
            }

            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            if (AxisUtility.isEngineerMode(deviceCode)) {
                logger.info("[" + deviceCode + "]" + "工程模式，取消开机Check卡控！");
                return;
            }
            if (equipStatus.equalsIgnoreCase("Execute Remote") && holdFlag) {
                holdDevice();
            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("Execute Remote")) {
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                } else {
                    //1、获取设备需要校验的信息类型,
                    String startCheckMod = deviceInfoExt.getStartCheckMod();
                    boolean hasGoldRecipe = true;
                    if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                        holdDeviceAndShowDetailInfo();
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
//                            holdDeviceAndShowDetailInfo();
                            holdDeviceAndShowDetailInfo(" There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                            return;
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            setAlarmState(0);
                        }
                    }
                    logger.info("设备[" + deviceCode + "]的开机检查模式为:" + startCheckMod);
                    if (startCheckMod.contains("B")) {
                        startSVcheckPass = false;
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
                        startSVcheck();
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if (false) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            //这里要把设备上recipe的后缀加上，否则获取不到
                            downLoadRecipe.setRecipeName(downLoadRecipe.getRecipeName() + ".rcp");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                //这里要把设备上recipe的后缀加上，否则获取不到
                                downLoadGoldRecipe.get(0).setRecipeName(downLoadGoldRecipe.get(0).getRecipeName() + ".rcp");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                            }

                        }
                    } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
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

        long ceid = 0l;
        long reportID = 0l;
        long controlStateTmp = 0l;
        try {
            ceid = (long) data.get("CEID");
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
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备状态切换到OFF-LINE");
            }
            if (controlStateTmp == 1) {
                controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备控制状态切换到Local");
            }
            if (controlStateTmp == 2) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备控制状态切换到Remote");
            }
            equipState.setControlState(controlState);
            changeEquipPanel(panelMap);
        }
    }

    protected void processS6F11LoginUserChange(DataMsgMap data) {
        long ceid = 0l;
        long reportID = 0l;
        String loginUserName = "";
        try {
            ceid = (long) data.get("CEID");
            reportID = data.getSingleNumber("ReportId");
            loginUserName = ((SecsItem) data.get("UserLoginName")).getData().toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 120 && reportID == 120) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "登陆用户变更，当前登陆用户：" + loginUserName);
        }

    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long length = TransferUtil.getPPLength(localFilePath);
        if (!"ASMAD8312PLUS".equals(deviceType)) {
            targetRecipeName = targetRecipeName + ".rcp";
        }
        DataMsgMap data = null;
        byte ppgnt = -1;
        try {
            data = activeWrapper.sendS7F1out(targetRecipeName, length, svFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.debug("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        if (!"ASMAD8312PLUS".equals(deviceType)) {
            targetRecipeName = targetRecipeName + ".rcp";
        }
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, FormatCode.SECS_BINARY);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
        byte ackc7 = (byte) data.get("ACKC7");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        recipeName = recipeName.replace(".rcp", "");
        if (!"ASMAD8312PLUS".equals(deviceType)) {
            recipeName = recipeName + ".rcp";
        }
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
        List<RecipePara> recipeParaListExtra = null;
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
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

    @Override
    public Map sendS7F17out(String recipeName) {
        if (!"ASMAD8312PLUS".equals(deviceType)) {
            recipeName = recipeName + ".rcp";
        }
        List list = new ArrayList();
        list.add(recipeName);
        byte ackc7 = -1;
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(list);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte) data.get("ACKC7");
            if (ackc7 == 0) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7 + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F19out() {
        Map resultMap = super.sendS7F19out();
        List eppd = (ArrayList) resultMap.get("eppd");
        ArrayList recipeNames = new ArrayList();
        for (int i = 0; i < eppd.size(); i++) {
            recipeNames.add(eppd.get(i).toString().replace(".rcp", ""));
        }
        resultMap.put("eppd", recipeNames);
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

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        if (!"ASMAD8312PLUS".equals(deviceType)) {
            recipeName = recipeName + ".rcp";
        }
        byte hcack = (byte) 9;
        try {

            Map data = super.sendS2F41outPPselect(recipeName);
            hcack = (byte) data.get("HCACK");
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }


    // </editor-fold>


    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

    // <editor-fold defaultstate="collapsed" desc="S12FX Code"> 


}
