/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.ls;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.equipImpl.disco.bg.DiscoBGHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

/**
 * @author njtz
 */
@SuppressWarnings("serial")
public class DiscoLSHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoBGHost.class.getName());


    public DiscoLSHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        this.ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        this.svFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    }

    public Object clone() {
        DiscoLSHost newEquip = new DiscoLSHost(deviceId,
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
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    updateLotId();
//                    upLoadAllRcp();
                }
                //针对DISCO开机无法hold，运行时hold的处理
//                if (!holdSuccessFlag) {
//                    holdDevice();
//                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().contains("s6f11equipstatuschange")) {
                    try {
                        processS6F11EquipStatusChange(msg);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    long ceid = 0l;
                    try {
                        ceid = (long) msg.get("CEID");
                        Map panelMap = new HashMap();
                        if (ceid == 75 || ceid == 76) {
                            if (ceid == 75) {
                                panelMap.put("ControlState", FengCeConstant.CONTROL_LOCAL_ONLINE);       //Online_Local
                            } else {
                                panelMap.put("ControlState", FengCeConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
                            }
                            changeEquipPanel(panelMap);
//                            processS6F11EquipStatus(msg);
                        }
                        if (ceid == 150) {
                            processS6F11EquipStatusChange(msg);
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                }
            } catch (InterruptedException e) {
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.contains("s6f11equipstatuschange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                long ceid = 0l;
                try {
                    ceid = (long) data.get("CEID");
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
                if (ceid == 75 || ceid == 76) {
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
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
    // <editor-fold defaultstate="collapsed" desc="S1FX Code">


    // </editor-fold> 
    private String getDevIdFromEqp(long DEVID) {
        String devid = "--";
        DataMsgMap s2f13out = new DataMsgMap("s2f13DEVIDout", activeWrapper.getDeviceId());
        long[] devids = new long[1];
        devids[0] = DEVID;
        s2f13out.put("DevID", devids);
        s2f13out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s2f13out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data != null && data.get("RESULT") != null) {
            ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
            ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            devid = String.valueOf(listtmp.get(0));
        }
        return devid;
    }

    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
//            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            super.findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 73) {
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            //检查模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！\n");
                holdDevice();
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            String busniessMod = deviceInfoExt.getBusinessMod();
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            } else //开机check
                if (equipStatus.equalsIgnoreCase("run") && ceid == 150l) {
                    if (this.checkLockFlagFromServerByWS(deviceCode)) {
                        UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                        this.holdDevice();
                        return;
                    }
                    if (!rcpInEqp(deviceInfoExt.getRecipeName())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "设备上不存在改机程序，确认是否成功提交改机！禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    if (!checkRecipe.getId().equals(deviceInfoExt.getRecipeId())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "设备使用程序： " + ppExecName + " ;与领料程序：" + checkRecipe.getRecipeName() + " 不一致，禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    //检查程序是否存在 GOLD
                    Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                    if (goldRecipe == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                        this.holdDevice();
                        return;
                    }
                    if (checkRecipe == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在程序：" + ppExecName + "！请确认是否已审核通过！");
                        this.holdDevice();
                    } else {
                        this.startCheckRecipePara(checkRecipe);
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
    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = (List<RecipePara>) this.sendS7F5out(checkRecipe.getRecipeName()).get("recipeParaList");
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
                this.holdDeviceAndShowDetailInfo();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
                logger.debug("设备：" + deviceCode + " 开机Check失败");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名：" + recipePara.getParaName() + ",其异常设定值为： " + recipePara.getSetValue();
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.debug("设备：" + deviceCode + " 开机Check成功");
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

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
        RecipeNameMapping recipeNameMapping = new RecipeNameMapping();
        String shortNameOK = "Y";
        String realRecipeName = "";
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析      
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            Map paraMap = DiscoRecipeUtil.transferFromFile(recipePath);
            recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
            for (RecipePara recipePara : recipeParaList) {
                if (recipePara.getParaName().equals("DEV_ID")) {
                    realRecipeName = recipePara.getSetValue();
                    recipeNameMapping.setDeviceCode(deviceCode);
                    recipeNameMapping.setRecipeName(realRecipeName);
                    recipeNameMapping.setRecipeShortName(recipeName);
                    List<RecipeNameMapping> recipeNameMappings = recipeService.getRecipeNameByDeviceCodeAndShotName(deviceCode, recipeName, null);
                    if (recipeNameMappings == null || recipeNameMappings.size() < 1) {
                        recipeService.savaRecipeNameMapping(recipeNameMapping);
                    } else {
                        for (RecipeNameMapping recipeNameMappingTmp : recipeNameMappings) {
                            if (!recipeNameMappingTmp.getRecipeName().equals(recipePara.getSetValue())) {
                                shortNameOK = "N";
                            }
                        }
                    }
                }
            }
            sqlSession.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            sqlSession.close();
        }

        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeNameMapping", recipeNameMapping);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("realRecipeName", realRecipeName);
        resultMap.put("shortNameOK", shortNameOK);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    private boolean rcpInEqp(String recipeName) {
        boolean rcpInEqp = false;
        ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
        for (int i = 0; i < eppd.size(); i++) {
            String rcpNameString = eppd.get(i).toString();
            if (recipeName.equals(rcpNameString)) {
                rcpInEqp = true;
            }
        }
        return rcpInEqp;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand">

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            Map cmdMap = this.sendS2f41Cmd("PAUSE_H");
            Map cmdMap = this.sendS2f41Cmd("STOP");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                setAlarmState(2);
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 2);
                changeEquipPanel(panelMap);
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！\n");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        //这里这样写是因为DFD6361 的hold指令使用的是Stop 设备从ready到run过程时间较长，stop后直接结束全自动模式，不用发RESUME
        Map map = new HashMap();
        map.put("HCACK", 0);
        setAlarmState(0);
        return map;//this.sendS2f41Cmd("RESUME_H");
    }

    @Override
    public Map startDevice() {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outCommand", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s2f41out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        return resultMap;
    }
    // </editor-fold>

    @Override
    public String checkEquipStatus() {
        findEqptStatus();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus) || "RUN".equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！下载失败！";
        }
        return "0";
    }


}
