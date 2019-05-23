/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.hontech;

import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.hontech.HT9045HWUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Wang Dafeng 机台特性：status是ASCII,@overwrite sendS1F3Check recipe为ASCII类型
 * template中ParaShotName不要修改
 */
public class HT9045HWHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(HT9045HWHost.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    private String Mdln = "HT9045HWHost";
    private String SoftRev = "-.-";
    private boolean canDownladMap = true;
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;

    public HT9045HWHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
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
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
                    sendS1F17out();
                    sendS2f41Cmd("ONLINE_REMOTE");
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    //获取lot号
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().contains("s6f11in")) {
                    long ceid = (long) msg.get("CEID");
                    if (ceid == 1) {
                        processPressStartButton(msg);
                    } else if (ceid == 27) {
                        setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                        processEquipStatusChange2(msg);
                    } else if (ceid == 15) {
                        //刷新当前机台状态
                        logger.info("[" + deviceCode + "]" + "之前Recipe为：{" + ppExecName + "}");
                        findDeviceRecipe();
                        logger.info("[" + deviceCode + "]" + "切换Recipe为：{" + ppExecName + "}");
                    } else if (ceid == 49) {
                       UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Tray Feed Finish!!!需要进行改机！");
                    }
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
                // TODO Auto-generated catch block
//                logger.info("此时线程中断标识为："+this.isInterrupted);
//                Thread.currentThread().interrupt();
//                logger.info("此时线程中断标识为："+this.isInterrupted);

            } catch (Exception ex) {
                logger.error("Exception:", ex);
            }
        }
    }

    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = new Date().getTime();
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
                System.out.println("接受到s1f4inS============================");
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
            } else if (tagName.contains("s6f11in")) {
                long ceid = (long) data.get("CEID");
                if (ceid == 1 || ceid == 15 || ceid == 27 || ceid == 49) {
                    this.inputMsgQueue.put(data);
                }
                replyS6F12WithACK(data, (byte) 0);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f3in")) {
                this.inputMsgQueue.put(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void initRptPara() {
        System.out.println("initRptPara+++++++++++++++++++");
//        //定义rpt，1011=Machine State,1501=recipe
//        sendS2f33out(27L, 1011L, 1501L);
//        //关联10002->27
//        sendS2f35out(1L, 27L, 10002L);
//        //开启事件报告
//        sendS2F37outAll();

        sendS2F37outCloseAll();
        //Press Start button
        sendS2F37out(1L);
        //recipe change
        sendS2F37out(15L);
        //status change
        sendS2F37out(27L);
        //Tray Feed
        sendS2F37out(49L);
        sendS5F3out(true);
    }

    /**
     * Get SVList from equip
     *
     * @return
     */

    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        if (listtmp != null) {
            equipStatus = String.valueOf(listtmp.get(0));
            ppExecName = String.valueOf(listtmp.get(1));
            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    protected void processEquipStatusChange2(DataMsgMap data) {
        //TODO 开机check;
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            //刷新当前机台状态
            sendS1F3Check();
            logger.info("[" + deviceCode + "]" + "设备进入" + equipStatus + "状态！");
//           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备" + equipStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                //锁机
                holdDevice();
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息,不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceInfoExt.setConnectionStatus(controlState);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
//            if (AxisUtility.isEngineerMode(deviceCode)) {
//               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
//                return;
//            }
//            //获取设备状态为ready时检查领料记录
//            if (equipStatus.equalsIgnoreCase("Running")) {
//                if (this.checkLockFlagFromServerByWS(deviceCode)) {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已被锁");
//                    holdDeviceAndShowDetailInfo("RepeatAlarm LOCK");
//                }
//                //1、获取设备需要校验的信息类型,
//                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
//                    holdDevice();
//                }
//                if (!checkRecipeName(deviceInfoExt.getRecipeName())) {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！");
//                    checkNameFlag = false;
//                } else {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序一致，核对通过！");
//                    checkNameFlag = true;
//                }
//
//                if (checkNameFlag && "A".equals(deviceInfoExt.getStartCheckMod())) {
//                    //查询trackin时的recipe和GoldRecipe
//                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
//                    boolean hasGoldRecipe = true;
//                    //查询客户端数据库是否存在GoldRecipe
//                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
//                        hasGoldRecipe = false;
//                    }
//                    //首先判断下载的Recipe类型
//                    //1、如果下载的是Unique版本，那么执行完全比较
//                    String downloadRcpVersionType = downLoadRecipe.getVersionType();
//                    if (false) {
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Unique Recipe:[" + ppExecName + "]参数绝对值Check");
//                        this.startCheckRecipePara(downLoadRecipe, "abs");
//                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
//                        if (!hasGoldRecipe) {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
//                            //不允许开机
//                            checkParaFlag = false;
//                        } else {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + ppExecName + "]开始WI参数Check");
//                            Map resultMap = this.startCheckRecipeParaReturnMap(downLoadGoldRecipe.get(0));
//                            if (resultMap != null) {
//                                if (resultMap.get("CheckParaFlag") != null) {
//                                    checkParaFlag = (boolean) resultMap.get("CheckParaFlag");
//                                    //显示比对不通过参数
//                                    List<RecipePara> recipeParasdiff = null;
//                                    if (!checkParaFlag && resultMap.get("RecipeParasdiff") != null && ((List<RecipePara>) resultMap.get("RecipeParasdiff")).size() > 0) {
//                                        recipeParasdiff = (List<RecipePara>) resultMap.get("RecipeParasdiff");
//                                        StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass, equipment locked!");
//                                        for (RecipePara recipePara : recipeParasdiff) {
//                                            recipeParasDiffText.append("\r\nError Para Name:" + recipePara.getParaShotName() + "Recipe Set Value:" + recipePara.getSetValue() + ",Gold Recipe Set Value：" + recipePara.getDefValue());
//                                        }
//                                        this.holdDeviceAndShowDetailInfo(recipeParasDiffText.toString());
//                                    }
//                                } else {
//                                    checkParaFlag = false;
//                                }
//                            } else {
//                                checkParaFlag = false;
//                            }
////                            checkParaFlag = this.startCheckRecipeParaReturnFlag(downLoadGoldRecipe.get(0));
////                            //向服务端发送机台被锁.更新服务端lockflag;
////                            if (checkParaFlag) {
////                                //解锁机台
////                                this.releaseDevice();
////                            } else {
////                                //锁机
////                                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
////                                //sendStatus2Server("LOCK");
////                            }
//                        }
//                    }
//                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
//                    //如果未设置参数比对模式，默认参数比对通过
//                    checkParaFlag = true;
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check参数模式！");
//                }
//                //总结是否需要锁机
//
//                if (!checkNameFlag || !checkParaFlag) {
//                    //锁机
//                    holdFlag = true;
//                } else {
//                    holdFlag = false;
//                }
//                //更新界面
//                if (!this.checkLockFlagFromServerByWS(deviceCode) && !holdFlag) {
//                    this.setAlarmState(0);
//                } else {
//                    holdDevice();
//                    this.setAlarmState(2);
//                }
//            }

        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();

        }
    }

    protected void processPressStartButton(DataMsgMap data) {
        //TODO 开机check;
        long ceid = 0l;
        logger.info("[" + deviceCode + "]" + "设备start按钮被按下！");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass, equipment locked!");
        try {
            ceid = (long)data.get("CEID");
            //从数据库中\获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                //锁机
                holdDevice();
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息,不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceInfoExt.setConnectionStatus(controlState);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
//            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            if (AxisUtility.isEngineerMode(deviceCode)) {
                logger.info("[" + deviceCode + "]" + "工程模式，取消开机Check卡控！");
                return;
            }
            //获取设备状态为ready时检查领料记录
            if (true) {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已被锁");
                    holdDeviceAndShowDetailInfo("RepeatAlarm LOCK");
                }
                //1、获取设备需要校验的信息类型,
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
                    holdDevice();
                }
                //先锁机
//                holdDevice();
                if (!checkRecipeName(deviceInfoExt.getRecipeName())) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！");
                    recipeParasDiffText.append("\r\nRecipe Error!MES Download Recipe:[" + deviceInfoExt.getRecipeName() + "]");
                    checkNameFlag = false;
                } else {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序一致，核对通过！");
                    checkNameFlag = true;
                }
                if (checkNameFlag && "A".equals(deviceInfoExt.getStartCheckMod())) {
                    //查询trackin时的recipe和GoldRecipe
//                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
//                    boolean hasGoldRecipe = true;
//                    //查询客户端数据库是否存在GoldRecipe
//                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
//                        hasGoldRecipe = false;
//                    }
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
//                    String downloadRcpVersionType = downLoadRecipe.getVersionType();
                    if (false) {
                        //Unique
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Unique Recipe:[" + ppExecName + "]参数绝对值Check");
//                        this.startCheckRecipePara(downLoadRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
                        //查询客户端数据库是否存在GoldRecipe
                        if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
                            //不允许开机
                            checkParaFlag = false;
                        } else {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + ppExecName + "]开始WI参数Check");
                            String testMode = getEquipTestMode();
                            logger.info("当前测试模式：" + testMode);
                            Map resultMap = this.startCheckRecipeParaReturnMap(downLoadGoldRecipe.get(0), testMode);
                            if (resultMap != null) {
                                if (resultMap.get("CheckParaFlag") != null) {
                                    checkParaFlag = (boolean) resultMap.get("CheckParaFlag");
                                    //显示比对不通过参数
                                    List<RecipePara> recipeParasdiff = null;
                                    if (!checkParaFlag && resultMap.get("RecipeParasdiff") != null && ((List<RecipePara>) resultMap.get("RecipeParasdiff")).size() > 0) {
                                        recipeParasdiff = (List<RecipePara>) resultMap.get("RecipeParasdiff");
//                                        StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass, equipment locked!");
                                        for (RecipePara recipePara : recipeParasdiff) {
                                            recipeParasDiffText.append("\r\nError Para Name:");
                                            recipeParasDiffText.append(recipePara.getParaShotName());
                                            recipeParasDiffText.append(",\r\nRecipe Set Value:");
                                            recipeParasDiffText.append(recipePara.getSetValue());
                                            recipeParasDiffText.append(",\r\nGold Recipe Set Value;");
                                            recipeParasDiffText.append(recipePara.getDefValue());
                                        }
//                                        this.holdDeviceAndShowDetailInfo(recipeParasDiffText.toString());
                                    }
                                } else {
                                    checkParaFlag = false;
                                }
                            } else {
                                checkParaFlag = false;
                            }
//                            checkParaFlag = this.startCheckRecipeParaReturnFlag(downLoadGoldRecipe.get(0));
//                            //向服务端发送机台被锁.更新服务端lockflag;
//                            if (checkParaFlag) {
//                                //解锁机台
//                                this.releaseDevice();
//                            } else {
//                                //锁机
//                                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
//                                //sendStatus2Server("LOCK");
//                            }
                        }
                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    //如果未设置参数比对模式，默认参数比对通过
                    checkParaFlag = true;
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check参数模式！");
                }
                //总结是否需要锁机

                if (!checkNameFlag || !checkParaFlag) {
                    //锁机
                    holdFlag = true;
                } else {
                    holdFlag = false;
                }
                //更新界面
                if (!this.checkLockFlagFromServerByWS(deviceCode) && !holdFlag) {
//                    sendS2f41Cmd("START");
                    this.setAlarmState(0);
                } else {
                    this.holdDeviceAndShowDetailInfo(recipeParasDiffText.toString());
//                    this.setAlarmState(2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    public Map startCheckRecipeParaReturnMap(Recipe checkRecipe) {
        return startCheckRecipeParaReturnMap(checkRecipe, "");
    }

    /**
     * 开机check recipe参数
     *
     * @param checkRecipe
     * @param type
     */
    public Map startCheckRecipeParaReturnMap(Recipe checkRecipe, String type) {
        boolean checkParaFlag = false;
        Map resultMap = new HashMap();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
//        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.eapView.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        logger.info("发送ecid给机台");
        List<RecipePara> equipRecipeParas = transferECSVValue2RecipePara(recipeService.searchRecipeTemplateMonitor(deviceCode), new ArrayList<RecipeTemplate>());

        List<RecipePara> recipeParasdiff = this.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
//                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                checkParaFlag = false;
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                checkParaFlag = true;
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
            resultMap.put("CheckParaFlag", checkParaFlag);
            resultMap.put("RecipeParasdiff", recipeParasdiff);
            return resultMap;
        }
    }

    /**
     * 参数检查
     *
     * @param recipeRowid
     * @param deviceCode
     * @param equipRecipeParas
     * @return
     */
    public List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas, String testMode) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        //获取Gold版本的参数(只有gold才有wi信息)
        List<RecipePara> goldRecipeParas = recipeService.searchRecipeParaByRcpRowId(recipeRowid);
        //确定管控参数
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateMonitor(deviceCode);
        sqlSession.close();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (RecipePara recipePara : goldRecipeParas) {
                if (recipePara.getParaCode() != null && recipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                    recipeTemplate.setMinValue(recipePara.getMinValue());
                    recipeTemplate.setMaxValue(recipePara.getMaxValue());
                    recipeTemplate.setSetValue(recipePara.getSetValue());
                }
            }
        }

        //找出设备当前recipe参数中超出wi范围的参数
        List<RecipePara> wirecipeParaDiff = new ArrayList<>();
        List<String> codeList = new ArrayList<>();
        for (RecipePara equipRecipePara : equipRecipeParas) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                if (recipeTemplate.getParaCode().equals(equipRecipePara.getParaCode()) && (recipeTemplate.getParaDesc() != null && testMode.equals(recipeTemplate.getParaDesc()) || "PR".equals(recipeTemplate.getParaDesc()))) {
                    equipRecipePara.setRecipeRowId(recipeRowid);
                    String currentRecipeValue = equipRecipePara.getSetValue();
                    //将数值从ECID查到的数据中提取出来
                    if (recipeTemplate.getParaShotName().contains("SiteMap")) {
                        String[] currentRecipeValues = currentRecipeValue.split(",");
                        int num = Integer.valueOf(recipeTemplate.getParaShotName().substring(1, recipeTemplate.getParaShotName().indexOf("]")).replace("SiteMap", ""));
                        currentRecipeValue = currentRecipeValues[num];
                    }
                    String setValue = recipeTemplate.getSetValue();
                    String minValue = recipeTemplate.getMinValue();
                    String maxValue = recipeTemplate.getMaxValue();
                    equipRecipePara.setParaShotName(recipeTemplate.getParaShotName());
                    DecimalFormat decimalFormat = new DecimalFormat("##########.##########");
                    String numConverted = "";
                    if (!"".equals(setValue)) {
                        if (setValue.matches("^-?[0-9]+(.[0-9]+)?$")) {
                            if (setValue.contains(".")) {
                                numConverted = decimalFormat.format(Double.parseDouble(setValue));
                                setValue = numConverted;
                            } else {
                                numConverted = decimalFormat.format(Integer.parseInt(setValue));
                                setValue = numConverted;
                            }
                        }
                    }
                    String numConverted1 = "";
                    if (!"".equals(currentRecipeValue)) {
                        if (currentRecipeValue.matches("^-?[0-9]+(.[0-9]+)?$")) {
                            if (currentRecipeValue.contains(".")) {
                                numConverted1 = decimalFormat.format(Double.parseDouble(currentRecipeValue));
                                currentRecipeValue = numConverted1;
                            } else {
                                numConverted1 = decimalFormat.format(Integer.parseInt(currentRecipeValue));
                                currentRecipeValue = numConverted1;
                            }
                        } else if ("false".equals(currentRecipeValue)) {
                            currentRecipeValue = "0";
                        } else if ("true".equals(currentRecipeValue)) {
                            currentRecipeValue = "1";
                        }
                    }
                    equipRecipePara.setDefValue(numConverted);//默认值，recipe参数设定值
                    try {
                        if (recipeTemplate.getParaShotName().contains("-") && codeList.contains(recipeTemplate.getParaShotName().substring(0, recipeTemplate.getParaShotName().lastIndexOf("-")))) {
                            logger.info("此值不需要比对,开关默认关闭:" + recipeTemplate.getParaShotName());
                            break;
                        }
                        if ((recipeTemplate.getParaShotName().contains("-EnableRT") || recipeTemplate.getParaShotName().contains("-Enable")) && "0".equals(setValue)) {
                            codeList.add(recipeTemplate.getParaShotName().substring(0, recipeTemplate.getParaShotName().lastIndexOf("-")));
                            logger.info("此值不需要比对,开关默认关闭:" + recipeTemplate.getParaShotName());
//                            break;
                        }
                        logger.info("开始对比参数Name:" + recipeTemplate.getParaName() + ",RecipeValue：" + currentRecipeValue + ",goldRecipeValue:" + setValue + ",ECID:" + recipeTemplate.getDeviceVariableId() + "************************************");
                        boolean paraIsNumber = false;
                        Double.parseDouble(currentRecipeValue);
                        paraIsNumber = true;
                        if ("1".equals(recipeTemplate.getSpecType())) {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                continue;
                            }
                            if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                equipRecipePara.setMinValue(minValue);
                                equipRecipePara.setMaxValue(maxValue);
                                wirecipeParaDiff.add(equipRecipePara);
                            }
                            //abs
                        } else if ("2".equals(recipeTemplate.getSpecType())) {
                            if ("".equals(setValue) || " ".equals(setValue) || "".equals(currentRecipeValue) || " ".equals(currentRecipeValue)) {
                                continue;
                            }
                            if (!currentRecipeValue.equals(setValue)) {
                                equipRecipePara.setMinValue(setValue);
                                equipRecipePara.setMaxValue(setValue);
                                wirecipeParaDiff.add(equipRecipePara);
                            }
                        } else {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                continue;
                            }
                            if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                equipRecipePara.setMinValue(minValue);
                                equipRecipePara.setMaxValue(maxValue);
                                wirecipeParaDiff.add(equipRecipePara);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                }
            }
        }
        return wirecipeParaDiff;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        String ppbody = (String) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 0, recipePath);
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = HT9045HWUtil.getRecipePara(recipePath, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S14FX Code"> 
    @Override
    public Object clone() {
        HT9045HWHost newEquip = new HT9045HWHost(deviceId,
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


    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        Map resultMap = null;
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            if (!"Running".equalsIgnoreCase(equipStatus)) {
                //不在RUN状态，已被锁
                resultMap = new HashMap();
                resultMap.put("msgType", "s2f42");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("prevCmd", "STOP");
                resultMap.put("HCACK", 0);
                resultMap.put("Description", "设备已被锁,将无法开机");
//                this.setAlarmState(2);
            } else {
                //RUN状态，发送停机指令
                //            super.sendS2f41Cmd("STOP");
                resultMap = this.sendS2f41Cmd("PAUSE");
                if ((byte) resultMap.get("HCACK") == 0 || (byte) resultMap.get("HCACK") == 4) {
//                    this.setAlarmState(2);
                }
            }
            return resultMap;
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    public String getEquipTestMode() {
        String testMode = "";
        try {
            List list = new ArrayList();
            list.add("1517");
            Map<String, String> map = getSpecificSVData(list);
            if (map.get("1517") != null) {
                switch (map.get("1517")) {
                    case "0":
                        testMode = "FT";
                        logger.info("当前机台TESTMODE:Continuous Start");
                        break;
                    case "1":
                        testMode = "FT";
                        logger.info("当前机台TESTMODE:Initial Start");
                        break;
                    case "2":
                        testMode = "RT";
                        logger.info("当前机台TESTMODE:Re-Test Continuous");
                        break;
                    case "3":
                        testMode = "RT";
                        logger.info("当前机台TESTMODE:Re-Test Initial Start");
                        break;
                    case "4":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:Site Mapping Check");
                        break;
                    case "5":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:QA Mode");
                        break;
                    case "6":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:Continuous EQC");
                        break;
                    case "7":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:Initial EQC");
                        break;
                    case "8":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:rsmInitial_ART");
                        break;
                    case "9":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:rsmContinuStart_ART");
                        break;
                    case "10":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:rsmContinuRetest_ART");
                        break;
                    case "11":
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:Auto Retest");
                        break;
                    default:
                        testMode = "NOR";
                        logger.info("当前机台TESTMODE:UnKnown!");
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            return testMode;
        }
    }


    //释放机台
    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        return null;
    }

    /**
     * 初始化设备支持的命令格式
     */
    @Override
    public void initRemoteCommand() {
        String commandKey = "start";
        CommandDomain startCommand = new CommandDomain();
        startCommand.setRcmd("START");
        this.remoteCommandMap.put(commandKey, startCommand);

        commandKey = "stop";
        CommandDomain stopCommand = new CommandDomain();
        stopCommand.setRcmd("STOP");
        this.remoteCommandMap.put(commandKey, stopCommand);

        commandKey = "pause";
        CommandDomain pauseCommand = new CommandDomain();
        pauseCommand.setRcmd("PAUSE");
        this.remoteCommandMap.put(commandKey, pauseCommand);

        commandKey = "resume";
        CommandDomain resumeCommand = new CommandDomain();
        resumeCommand.setRcmd("RESUME");
        this.remoteCommandMap.put(commandKey, resumeCommand);

        commandKey = "local";
        CommandDomain localCommand = new CommandDomain();
        localCommand.setRcmd("LOCAL");
        this.remoteCommandMap.put(commandKey, localCommand);

        commandKey = "remote";
        CommandDomain remoteCommand = new CommandDomain();
        remoteCommand.setRcmd("REMOTE");
        this.remoteCommandMap.put(commandKey, remoteCommand);
        //调用父类的方法，生成公用命令，如果不支持，可以删掉，如果不公用，直接覆盖
        initCommonRemoteCommand();
    }


}
