/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.dantai;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.dantai.DT550ARecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Wang Dafeng
 */
public class DT550AHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DT550AHost.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    private String Mdln = "DT550A";
    private String SoftRev = "-.-";
    private boolean canDownladMap = true;
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;


    public DT550AHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public Object clone() {
        DT550AHost newEquip = new DT550AHost(deviceId,
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
                    DT550AHost.sleep(200);
                }
                if (this.getCommState() != DT550AHost.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
                    sendS1F17out();
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
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    long ceid = (long) msg.get("CEID");
                    if (ceid == 900010) {
                        processPressStartButton(msg);
                    } else if (ceid == 900002) {
                        processEquipStatusChange(msg);
                    } else if (ceid == 900006) {
                        //刷新当前机台状态
                        logger.info("[" + deviceCode + "]" + "之前Recipe为：{" + ppExecName + "}");
                        findDeviceRecipe();
                        logger.info("[" + deviceCode + "]" + "切换Recipe为：{" + ppExecName + "}");
                    }

                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                logger.fatal("Caught Interruption", e);
            } catch (Exception ex) {
                logger.error("Exception:", ex);
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
            } else if (tagName.equalsIgnoreCase("s2f17in")) {
                processS2F17in(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data,(byte)0);
                    this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s9f9Timeout")) {
                //接收到超时，直接不能下载
                this.canDownladMap = false;
                //或者重新发送参数
                initRptPara();
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

    public void initRptPara() {
        System.out.println("initRptPara+++++++++++++++++++");

    }

    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    protected void processEquipStatusChange(DataMsgMap data) {
        //TODO 开机check;
        long ceid = 0L;
        try {
            ceid = (long)data.get("CEID");
            //刷新当前机台状态
            findDeviceRecipe();
            logger.info("[" + deviceCode + "]" + "设备进入" + equipStatus + "状态！");
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
            //获取设备状态为ready时检查领料记录
//            if (equipStatus.contains("RUN")) {
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
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数绝对值Check");
//                        this.startCheckRecipePara(downLoadRecipe, "abs");
//                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
//                        if (!hasGoldRecipe) {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
//                            //不允许开机
//                            checkParaFlag = false;
//                        } else {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + ppExecName + "]开始WI参数Check");
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
//                    //不需要锁机，，发送指令开机
//                    startDevice();
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
        long ceid = 0L;
        try {
            ceid = (long)data.get("CEID");
            //刷新当前机台状态
            findDeviceRecipe();
            logger.info("[" + deviceCode + "]" + "设备进入" + equipStatus + "状态！");
            logger.info("[" + deviceCode + "]" + "设备进入" + controlState + "控制状态！");
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
            if (AxisUtility.isEngineerMode(deviceCode)) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                return;
            }
            //获取设备状态为ready时检查领料记录
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已被锁");
                holdDeviceAndShowDetailInfo("RepeatAlarm LOCK");
            }
            //1、获取设备需要校验的信息类型,
            if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
                holdDevice();
            }
            if (!checkRecipeName(deviceInfoExt.getRecipeName())) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！");
                checkNameFlag = false;
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序一致，核对通过！");
                checkNameFlag = true;
            }

            if (checkNameFlag && "A".equals(deviceInfoExt.getStartCheckMod())) {
                //查询trackin时的recipe和GoldRecipe
                Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
                boolean hasGoldRecipe = true;
                //查询客户端数据库是否存在GoldRecipe
                if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                    hasGoldRecipe = false;
                }
                //首先判断下载的Recipe类型
                //1、如果下载的是Unique版本，那么执行完全比较
                String downloadRcpVersionType = downLoadRecipe.getVersionType();
                if (false) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数绝对值Check");
                    this.startCheckRecipePara(downLoadRecipe, "abs");
                } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
                    if (!hasGoldRecipe) {
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
                        //不允许开机
                        checkParaFlag = false;
                    } else {
                        checkParaFlag = this.startCheckRecipeParaReturnFlag(downLoadGoldRecipe.get(0));
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
                this.setAlarmState(0);
                //不需要锁机，，发送指令开机
                startDevice();
            } else {
                this.setAlarmState(2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (msgdata == null || msgdata.isEmpty()) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传请求被设备拒绝,请调整设备状态重试.");
            return null;
        }
        //
        String ppbody = (String) msgdata.get("PPBODY");
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = DT550ARecipeUtil.getRecipeParas(recipePath, deviceType);
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

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S14FX Code"> 


    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        Map resultMap = null;
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            if (equipStatus.contains("RUN")) {
                //不在RUN状态，假装已被锁
                resultMap = new HashMap();
                resultMap.put("msgType", "s2f42");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("prevCmd", "STOP");
                resultMap.put("HCACK", 0);
                resultMap.put("Description", "设备已被锁,将无法开机");
                this.setAlarmState(2);
            } else {
                //RUN状态，发送停机指令
                resultMap = this.sendS2f41Cmd("PAUSE");
                if ((byte) resultMap.get("HCACK") == 0 || (byte) resultMap.get("HCACK") == 4) {
                    this.setAlarmState(2);
                }
            }
            return resultMap;
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
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
