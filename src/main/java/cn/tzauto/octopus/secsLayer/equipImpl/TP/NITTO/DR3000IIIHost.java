/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.TP.NITTO;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TPRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("serial")
public class DR3000IIIHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DR3000IIIHost.class.getName());
    public Map<Integer, Boolean> pressUseMap = new HashMap<>();

    public DR3000IIIHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        EquipStateChangeCeid = 9;
//        StripMapUpCeid = 0;
        RCMD_PPSELECT = "PPSELECT";
    }


    @Override
    public Object clone() {
        DR3000IIIHost newEquip = new DR3000IIIHost(deviceId,
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
                    this.sleep(10000);
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    this.sleep(30000);
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    //获取设备开机状态

                    super.findDeviceRecipe();
//                    this.sendS1F3Check();

//                    initRptPara();
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
//                    //为了能调整为online remote
//                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    //获取lot号/
                    super.updateLotId();
//                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in") || msg.getMsgSfName().equalsIgnoreCase("s5f1ypmin")) {
                    replyS5F2Directly(msg);
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    long ceid = 0l;
                    try {
                        ceid = (long) msg.get("CEID");
                        if (ceid == 22) {
//                            this.processS6F11EquipStatusChange(msg);
                            processS6F11in(msg);
                            super.findDeviceRecipe();
                        } else {
                            processS6F11in(msg);
                        }
//                        else if (ceid == EquipStateChangeCeid) {
//                            processS6F11EquipStatusChange(msg);
//                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
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
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s2f17in")) {
                processS2F17in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                System.out.println("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    public Map sendS1F3Check() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
//        DataMsgMap s1f3out = new DataMsgMap("s1f3out", activeWrapper.getDeviceId());
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
            //todo  data = activeWrapper.sendS1F3out(s1f3out,svFormat);
//            data = activeWrapper.sendS1F3out(s1f3out,svFormat);
            logger.info("received Message=S1F3===================>" + JSONArray.toJSON(data));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Wait for get meessage directly error：" + e.getMessage());
        }
        if (data == null || data.isEmpty()) {
            int i = 0;
            int j = 0;
            logger.info("Can not get value directly,will try to get value from message queue");
            while (i < 5) {
                j = i + 1;
                DataMsgMap DataMsgMap = this.waitMsgValueMap.get(transactionId);
                logger.info("try===>" + j);
                if (DataMsgMap != null) {
                    data = DataMsgMap;
                    logger.info("Had get value from message queue ===>" + JSONArray.toJSON(data));
                    break;
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                i++;
            }
            if (i >= 5) {
                UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备状态信息失败，请检查设备通讯状态！");
                logger.error("获取设备:" + deviceCode + "状态信息失败.");
                return null;
            }
        }
        this.waitMsgValueMap.remove(transactionId);
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        ppExecName = (String) listtmp.get(1);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);

//        if (controlState.equalsIgnoreCase("OnlineLocal")) {
//            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
//        } else if (controlState.equalsIgnoreCase("OnlineRemote")) {
//            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
//        } else if (controlState.equalsIgnoreCase("EquipmentOffline")) {
//            controlState = FengCeConstant.CONTROL_OFFLINE;
//        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        data = null;
        return panelMap;
    }
    // </editor-fold>

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        //回复s6f11消息
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        byte[] ack = new byte[1];
        ack[0] = 0;
        out.put("AckCode", ack);
        long ceid = 0l;
        try {
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
            ceid = (long) data.get("CEID");
            this.sendS1F3Check();
            preEquipStatus = ACKDescription.descriptionStatus(String.valueOf(data.get("PreviousEquipStatus")), deviceType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        //保存到设备操作记录数据库
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
        DeviceOplog deviceOplog = new DeviceOplog();
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            if (deviceInfoExt == null) {
                deviceInfoExt = setDeviceInfoExt();
                deviceService.saveDeviceInfoExt(deviceInfoExt);
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                holdDeviceAndShowDetailInfo();
                UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
            }

            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
                deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态到服务端
//                sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
            } else {
                String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
                if (!formerDeviceStatus.equals(equipStatus)) {
                    deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
                    deviceService.saveDeviceOplog(deviceOplog);

                    //发送设备状态到服务端
//                    sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                    // sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpDesc(),deviceOplog.getOpTime());
                }
            }
            sqlSession.commit();
            //开机check
            if (equipStatus.equalsIgnoreCase("READY") && !preEquipStatus.equalsIgnoreCase("RUN")) {
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean checkResult = false;
                Recipe goldRecipe = null;
                Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
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
                        goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                        if (goldRecipe == null) {
                            //TODO  这里需要讨论做试产时的情况
                            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
                        }
                    }
                }
                if (checkResult && "A".equals(startCheckMod)) {
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = checkRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        this.startCheckRecipePara(checkRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                        if (goldRecipe == null) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                            //不允许开机
                            this.holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                            this.startCheckRecipePara(goldRecipe);
                        }

                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    private void processS6F11EquipStart(DataMsgMap data) {
        //回复s6f11消息
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        byte[] ack = new byte[1];
        ack[0] = 0;
        out.put("AckCode", ack);
        long ceid = 0l;
        try {
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
            ceid = (long) data.get("CEID");
//            equipStatus = ACKDescription.descriptionStatus(data.get("EquipStatus"), deviceType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        DeviceOplog deviceOplog = new DeviceOplog();
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
        if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
            holdDeviceAndShowDetailInfo();
            UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
        }
        if (goldRecipe == null) {
            //TODO  这里需要讨论做试产时的情况

            //todo 显示界面日志
        }
        try {
            if (deviceInfoExt == null) {
                deviceInfoExt = setDeviceInfoExt();
                deviceService.saveDeviceInfoExt(deviceInfoExt);
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            //保存到设备操作记录数据库
            List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
                deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态变化记录到服务端
//                this.sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
            } else {
                String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
                if (!formerDeviceStatus.equals(equipStatus)) {
                    deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
                    deviceService.saveDeviceOplog(deviceOplog);
                    //发送设备状态到服务端
//                    sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                    // sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpDesc(),deviceOplog.getOpTime());
                }
            }
            sqlSession.commit();

            //开机check
            String startCheckMod = deviceInfoExt.getStartCheckMod();
            boolean checkResult = false;
            Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
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
                    this.setAlarmState(0);
                    UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                }
            }
            if (checkResult && "A".equals(startCheckMod)) {
                //首先判断下载的Recipe类型
                //1、如果下载的是Unique版本，那么执行完全比较
                String downloadRcpVersionType = checkRecipe.getVersionType();
                if ("Unique".equals(downloadRcpVersionType)) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                    this.startCheckRecipePara(checkRecipe, "abs");
                } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                    UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                    if (goldRecipe == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                        //不允许开机
                        this.holdDeviceAndShowDetailInfo();
                    } else {
                        UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                        this.startCheckRecipePara(goldRecipe);
                    }

                }
            } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        String ppid = recipeName;
        Recipe recipe = setRecipe(recipeName);
//        recipePath = this.getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + ppid + "/" + ppid + "_V" + recipe.getVersionNo() + ".txt";
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
//        recipePath = GlobalConstants.localRecipePath + recipeService.organizeRecipePath(recipe) + "/" + deviceCode + "/" + ppid.replace("/", "@").replace(".", "#") + "/" + ppid.replace("/", "@").replace(".", "#") + "_V" + recipe.getVersionNo() + ".txt";
        recipePath = GlobalConstants.localRecipePath + recipeService.organizeRecipePath(recipe) + "/" + ppid.replace("/", "@") + "_V" + recipe.getVersionNo() + ".txt";
        sqlSession.close();
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", ppid);
        DataMsgMap msgData = null;
        try {
            msgData = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msgData == null || msgData.isEmpty()) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备参数信息失败，可能原因是当前设备为RUN状态，请检查！");
            logger.error("获取设备:" + deviceCode + "参数信息失败，可能原因是当前机台为RUN状态！");
            return null;
        }
        ppid = (String) ((SecsItem) msgData.get("ProcessprogramID")).getData();
//        byte[] ppbody = (byte[]) ((SecsItem) msgData.get("Processprogram")).getData();
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
//            recipeParaList = TowaRecipeUtil.transferTowaRcp(TowaRecipeUtil.Y1R_RECIPE_CONFIG, ppbody);
            recipeParaList = TPRecipeUtil.tPRecipeTran(recipePath);// TowaRecipeUtil.transferTowaRcpFromDB(deviceType, ppbody);
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
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + ppid + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>


    private void initRptPara() {
    }

    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
//            super.sendS2f41Cmd("PAUSE");
//            //????????????????
//            return this.sendS2f41Cmd("PAUSE");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = sendS2f41Cmd("ABORT");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 2);
                changeEquipPanel(panelMap);
            }
            return cmdMap;
        } else {
            //todo 显示界面锁机日志
            return null;
        }
    }

    //    public Map releaseDevice() {
//        Map map = new HashMap();
//        map.put("HCACK", 0);
//        return map;
//    }
    public Map releaseDevice() {
        try {
            sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Map map = this.sendS2f41Cmd("START");
        if ((byte) map.get("HCACK") == 0) {
            this.setAlarmState(0);
        }
        return map;
    }

    public void processS2F17in(DataMsgMap msg) {
//        throw new UnsupportedOperationException("Not yet implemented");
        try {
            DataMsgMap s2f18out = new DataMsgMap("s2f18out", activeWrapper.getDeviceId());
            String time = "";
            time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            s2f18out.put("TIME", time + "00");
            long transactionId = msg.getTransactionId();
            s2f18out.setTransactionId(transactionId);
            activeWrapper.respondMessage(s2f18out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
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
                this.holdDeviceAndShowDetailInfo();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
                logger.debug("设备：" + deviceCode + " 开机Check失败");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + "，其异常设定值为： " + recipePara.getSetValue() + "，其最小设定值为：" + recipePara.getMinValue() + "，其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
//                this.releaseDevice();
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 0);
                changeEquipPanel(panelMap);
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.debug("设备：" + deviceCode + " 开机Check成功");
            }
            mqMap.put("eventDesc", eventDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sqlSession.close();
        }
    }

    public List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas, String masterCompareType) {
        //获取Gold版本的参数(只有gold才有wi信息)
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipePara> goldRecipeParas = recipeService.searchRecipeParaByRcpRowId(recipeRowid);
        //确定管控参数
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateMonitor(deviceCode);
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (RecipePara recipePara : goldRecipeParas) {
                if (recipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                    recipeTemplate.setMinValue(recipePara.getMinValue());
                    recipeTemplate.setMaxValue(recipePara.getMaxValue());
                    recipeTemplate.setSetValue(recipePara.getSetValue());
                }
            }
        }
        //找出设备当前recipe参数中超出wi范围的参数
        List<RecipePara> wirecipeParaDiff = new ArrayList<>();
        for (RecipePara equipRecipePara : equipRecipeParas) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                if (recipeTemplate.getParaCode().equals(equipRecipePara.getParaCode())) {
                    String currentRecipeValue = equipRecipePara.getSetValue();
                    String setValue = recipeTemplate.getSetValue();
                    String minValue = recipeTemplate.getMinValue();
                    String maxValue = recipeTemplate.getMaxValue();
                    boolean paraIsNumber = false;
                    try {
                        Double.parseDouble(currentRecipeValue);
                        paraIsNumber = true;
                    } catch (Exception e) {
                    }
                    try {
                        if ("abs".equals(masterCompareType)) {
                            equipRecipePara.setMinValue(setValue);
                            equipRecipePara.setMaxValue(setValue);
                            if (paraIsNumber) {
                                if (Double.parseDouble(currentRecipeValue) != Double.parseDouble(setValue)) {
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else {
                                if (!currentRecipeValue.equals(setValue)) {
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            }

                        } else {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                continue;
                            }
                            //spec
                            if ("1".equals(recipeTemplate.getSpecType())) {
                                if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                                //abs
                            } else if ("2".equals(recipeTemplate.getSpecType())) {
                                if (!equipRecipePara.getSetValue().toString().equals(setValue)) {
                                    equipRecipePara.setMinValue(setValue);
                                    equipRecipePara.setMaxValue(setValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        sqlSession.close();
        return wirecipeParaDiff;
    }


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

    @Override
    public void initRemoteCommand() {
    }
}
