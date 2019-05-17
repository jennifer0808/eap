package cn.tzauto.octopus.secsLayer.equipImpl.asm.da;

import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.asm.ASMAD9212RcpUtil;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;


public class AsmAD9212PlusHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AsmAD8312Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    public String Datelength;
    private String holdDesc = "";

    public AsmAD9212PlusHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        StripMapUpCeid = 237L;
        EquipStateChangeCeid = 8L;
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }


    @Override
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!isInterrupted) {
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
//                    sendS1F11out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS2F37outAll();
                    sendS5F3out(true);
                    sendStatus2Server(equipStatus);
                }

                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);

                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
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
            } else if (tagName.contains("F0") || tagName.contains("f0")) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
                equipStatus = "SECS-OFFLINE";
                Map panelMap = new HashMap();
                panelMap.put("EquipStatus", equipStatus);
                panelMap.put("PPExecName", ppExecName);
                panelMap.put("ControlState", controlState);
                changeEquipPanel(panelMap);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="processS1FXin Code">
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

    // <editor-fold defaultstate="collapsed" desc="processS6FXin Code">

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        findEqptStatus();
        if (equipStatus.equalsIgnoreCase("setup")) {
            releaseDevice();
        }
        logger.info("当前状态：" + equipStatus);
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
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
                if (AxisUtility.checkBusinessMode(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "处于工程模式,取消开机检查");
                    sqlSession.close();
                    return;
                }
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
                            holdDesc = "The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.";
                            holdFlag = true;
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            setAlarmState(0);
                            holdFlag = false;
                            holdDesc = "";
                        }
                    }
                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                        if (!checkResult) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");
                            return;
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            releaseDevice();
                        }
                        logger.info("设备[" + deviceCode + "]的开机检查模式为:" + startCheckMod);
                        if (startCheckMod.contains("B")) {
                            startSVcheckPass = false;
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
                            startSVcheck();
                        }
                        if (startCheckMod.contains("A")) {
                            //首先判断下载的Recipe类型
                            //1、如果下载的是Unique版本，那么执行完全比较
                            String downloadRcpVersionType = downLoadRecipe.getVersionType();
                            if ("Unique".equals(downloadRcpVersionType)) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                                //这里要把设备上recipe的后缀加上，否则获取不到
                                downLoadRecipe.setRecipeName(downLoadRecipe.getRecipeName() + ".rcp");
                                this.startCheckRecipePara(downLoadRecipe, "abs");
                            } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                                if (!hasGoldRecipe) {
                                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在[" + ppExecName + "]的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                    //不允许开机
                                    this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                                } else {
                                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                    //这里要把设备上recipe的后缀加上，否则获取不到
                                    downLoadGoldRecipe.get(0).setRecipeName(downLoadGoldRecipe.get(0).getRecipeName() + ".rcp");
                                    this.startCheckRecipePara(downLoadGoldRecipe.get(0), "");
                                }
                            }
                        } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
                        }
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
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        return super.sendS7F1out(localFilePath, targetRecipeName + ".rcp");

    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, final String targetRecipeName) {
        return super.sendS7F3out(localRecipeFilePath, targetRecipeName + ".rcp");
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Map map = new HashMap();
        Map resultMap = new HashMap();
        map = sendS1F3Check();
        String rcpName = (String) map.get("PPExecName");
        logger.info("rcpName=========" + rcpName);
        logger.info("recipeName=========" + recipeName);
        List<RecipePara> recipeParaList = null;

        byte[] ppbody = new byte[0];
        try {
            ppbody = (byte[]) getPPBODY(recipeName + ".rcp");
        } catch (UploadRecipeErrorException e) {
            e.printStackTrace();
        }
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析
//            recipeParaList = getRecipeParasByECSV();
        logger.info("recipePath======" + recipePath + "======recipeName======" + recipeName);
        recipeParaList = ASMAD9212RcpUtil.transferRcpFromDB1(recipePath, recipeName, deviceType);
        logger.info("List=====" + recipeParaList);

        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeNameMapping", null);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }


    public Map sendS7F17out(String recipeName) {
        return super.sendS7F17out(recipeName + ".rcp");
    }
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="sendS2FXout Code">
    //释放机台
    @Override
    public Map releaseDevice() {
//        this.sendTerminalMsg2EqpSingle("Release");
        Map map = new HashMap();
        map = this.sendS2f41Cmd("RESUME");
        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
            this.setAlarmState(0);
        }
        return map;
    }


    // </editor-fold>

    @Override
    public Object clone() {
        AsmAD9212PlusHost newEquip = new AsmAD9212PlusHost(deviceId,
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
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        ArrayList listtmp = (ArrayList) super.sendS7F19out().get("EPPD");
        if (listtmp == null || listtmp.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {

            logger.info("recipeNameList:" + listtmp);
            ArrayList recipenamelist = new ArrayList();
            for (Object object : listtmp) {
                recipenamelist.add(String.valueOf(object).replace(".rcp", ""));
            }
            resultMap.put("eppd", recipenamelist);
        }
        return resultMap;
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map map = this.sendS2f41Cmd("PAUSE");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
                sendStatus2Server("LOCK");
                holdFlag = true;
            }
            return map;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }
}
