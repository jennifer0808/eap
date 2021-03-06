package cn.tzauto.octopus.secsLayer.equipImpl.asm.da;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsmAD838Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AsmAD838Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    public Long ceid = 0L;

    public Boolean connFlag = false;

    public AsmAD838Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        StripMapUpCeid = 237L;
        EquipStateChangeCeid = 6L;
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        DataMsgMap msg = null;
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    AsmAD838Host.sleep(200);
                }
                if (this.getCommState() != AsmAD838Host.COMMUNICATING  ) {
                    sendS1F13out();
//                    sendS1F1out();
                    Thread.sleep(5000);
                }

                if(!connFlag){
                    Thread.sleep(2000);
                }

                if (rptDefineNum < 1) {

                    //为了能调整为online remote
//                    logger.info("sendS1F17out request online...");
//                    this.sendS1F17out();
//                    logger.info("sendS2f41Cmd online remote...");
//                    sendS2f41Cmd("ONLINE_REMOTE");
                    //获取设备开机状态
                    super.findDeviceRecipe();


                    initRptPara();
                    rptDefineNum++;
                    sendStatus2Server(equipStatus);
                }
                logger.info("inputMsgQueue.size():" + inputMsgQueue.size());
                msg = this.inputMsgQueue.take();

                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("S1F1IN")) {
                    processS1F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("S1F13IN")) {
                    processS1F13in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("S1F14IN")) {
                    processS1F14in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("S14F1IN")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    System.out.println("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
                e.printStackTrace();
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
            LastComDate = System.currentTimeMillis();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("S1F1IN")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("S1F13IN")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("S6F11IN")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s7f1in")) {
                processS7F1in(data);
            } else if (tagName.equalsIgnoreCase("S5F1IN")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("S14F1IN")) {
//                processS14F1in(data);   
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
            } else if (tagName.equalsIgnoreCase("f0")) {
                sendSnF0();
                logger.info("tagName===========>" + tagName);
            } else {
                System.out.println("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processS1F1in(DataMsgMap data) {
        super.processS1F1in(data);
        connFlag = true;
        if(rptDefineNum>0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    initRptPara();
                }
            }).start();
        }
    }

    @Override
    public void processS1F13in(DataMsgMap data) {
        super.processS1F13in(data);
        connFlag =false;

    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);//路径
        List<RecipePara> recipeParaList = null;

        try {
            byte[] ppbody = (byte[]) getPPBODY(recipeName);
            TransferUtil.setPPBody(ppbody, 1, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);

            recipeParaList = getRecipeParasByECSV();
        } catch (UploadRecipeErrorException e) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "上传请求被设备拒绝，请查看设备状态。");
            logger.error("Exception:", e);
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


    // <editor-fold defaultstate="collapsed" desc="processS6FXin Code">
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        try {
            preEquipStatus = equipStatus;
            findDeviceRecipe();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            //发送设备UPH参数至服务端
            // sendUphData2Server();


//            String busniessMod = deviceInfoExt.getBusinessMod();
//            if (AxisUtility.isEngineerMode(deviceCode)) {
//                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
//                sqlSession.close();
//                return;
//            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("run") && preEquipStatus.equalsIgnoreCase("Idle Remote")) {
                lastStartCheckTime = System.currentTimeMillis();
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo();
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
                            holdDeviceAndShowDetailInfo();
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
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                            }

                        }
                    } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }


    /**
     * 上报包含preEquipStatus, 发送设备UPH参数至服务端
     */
    @Override
    public void sendUphData2Server() throws IOException, BrokenProtocolException, T6TimeOutException, T3TimeOutException, InterruptedException, StateException, IntegrityException, InvalidDataException {
        String output = "";
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List svidlist = recipeService.searchShotSVByDeviceType(deviceType);

        sqlSession.close();
        //获取前一状态与当前状态
        Map shotCountMap = activeWrapper.sendS1F3out(svidlist, svFormat);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "UphDataTransfer");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("equipStatus", equipStatus);
        mqMap.put("preEquipStatus", preEquipStatus);
        mqMap.put("currentRecipe", ppExecName);
        mqMap.put("lotId", lotId);
        mqMap.put("shotCount", JsonMapper.toJsonString(shotCountMap));
        mqMap.put("output", output);
        mqMap.put("unit", "");
        mqMap.put("currentTime", GlobalConstants.dateFormat.format(new Date()));
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送设备UPH参数至服务端");
        logger.info("设备" + deviceCode + " UPH参数为:" + mqMap);
//       UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "UPH参数为:" + mqMap);
    }

    private void processS6F11ControlStateChange(DataMsgMap data) {
        Map panelMap = new HashMap();
        if (ceid == 4) {
            controlState = GlobalConstant.CONTROL_OFFLINE;
            panelMap.put("ControlState", controlState);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备状态切换到OFF-LINE");
        }
        if (ceid == 2) {
            controlState = GlobalConstant.CONTROL_LOCAL_ONLINE;
            panelMap.put("ControlState", controlState);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备控制状态切换到Local");
        }
        if (ceid == 3) {
            controlState = GlobalConstant.CONTROL_REMOTE_ONLINE;
            panelMap.put("ControlState", controlState);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备控制状态切换到Remote");
        }
        if (ceid == 7) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            try {
                findDeviceRecipe();
                panelMap.put("EquipStatus", equipStatus);
                //从数据库中获取当前设备模型信息
                DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                // 更新设备模型
                if (deviceInfoExt == null) {
                    logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
                } else {
                    deviceInfoExt.setDeviceStatus(equipStatus);
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                }
                //保存到设备操作记录数据库
                saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
                sqlSession.commit();
            } catch (Exception e) {
            } finally {
                sqlSession.close();
            }
        }
        changeEquipPanel(panelMap);

    }

    // </editor-fold>

    @Override
    public Object clone() {
        AsmAD838Host newEquip = new AsmAD838Host(deviceId,
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
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = getRecipeParasByECSV();
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
            }
            mqMap.put("eventDesc", eventDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
    }

//    private void initRptPara() {
////sendS6F23clear();
//        //重写事件 报告
////            sendS2f33out(50007l, 902l, 906l);
////            sendS2f35out(50007l, 50007l, 50007l);
////            sendS2F37out(50007l);
////        sendS2F37outCloseAll();
////        sendS2F33clear();
////        sendS2F35clear();
////        sendS2F33init();
////        sendS2F35init();
////        sendS2F37outAll();
////        sendS2F33clear();
////        sendS2F35clear();
////
////        sendStatus2Server(equipStatus);
//
//    }

    private void initRptPara() {
        sendS2F37outCloseAll();
        sendS2F37out(2);
        sendS2F37out(3);
        sendS2F37out(4);
        sendS2F37out(6);
        sendS2F37out(7);
        sendS2F37out(237);

        sendS5F3out(true);
    }
    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

    @Override
    public void processS6F11in(DataMsgMap data) {
        if (data.get("CEID") != null) {
            ceid = (long) data.get("CEID");
            logger.info("Received a s6f11in with CEID = " + ceid + ";事务Id:" + data.getTransactionId());
        }

        try {

            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else {
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
                if (ceid == EquipStateChangeCeid) {
                    processS6F11EquipStatusChange(data);
                } else if (ceid == 4 || ceid == 2 || ceid == 3 || ceid == 7) {
                    processS6F11ControlStateChange(data);
                }
            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
}
