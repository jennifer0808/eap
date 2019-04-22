/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.tsk.ws;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
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
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.AWD300TXRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.text.SimpleDateFormat;
import java.util.*;


@SuppressWarnings("serial")
public class AWD300TXHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AWD300TXHost.class.getName());
    public Map<Integer, Boolean> pressUseMap = new HashMap<>();

    public AWD300TXHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        EquipStateChangeCeid = 301L;
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady() || activeWrapper == null) {
                    AWD300TXHost.sleep(200);
                }
                if (this.getCommState() != AWD300TXHost.COMMUNICATING) {
                    Thread.sleep(2000);
                    sendS1F13out();
                }

                if (rptDefineNum < 1) {
                    initRptPara();
                    rptDefineNum++;
                }

                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in") || msg.getMsgSfName().equalsIgnoreCase("s5f1ypmin")) {
                    processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s2f17in")) {
                processS2F17in(data);
            } else if (tagName.equalsIgnoreCase("s6f5in")) {
                processS6F5in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
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
                replyS5F2Directly(data);
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

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            sendS1F3Check();
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备将被锁定!");
                holdDeviceAndShowDetailInfo();
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
            if (equipStatus.equalsIgnoreCase("RUN")) {
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
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备将被锁定！请联系PE处理！");
                        //不允许开机
                        holdDeviceAndShowDetailInfo();
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                        if (goldRecipe == null) {
                            //TODO  这里需要讨论做试产时的情况
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
                        }
                        this.setAlarmState(0);
                    }
                }
                if (checkResult && "A".equals(startCheckMod)) {
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = checkRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        this.startCheckRecipePara(checkRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                        if (goldRecipe == null) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                            //不允许开机
                            this.holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                            this.startCheckRecipePara(checkRecipe, "abs");
                        }

                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
                }
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
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        DataMsgMap msgData = null;
        try {
            msgData = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msgData == null || msgData.isEmpty()) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "获取设备参数信息失败，可能原因是当前设备为RUN状态，请检查！");
            logger.error("获取设备:" + deviceCode + "参数信息失败，可能原因是当前机台为RUN状态！");
            return null;
        }
        byte[] ppbody = (byte[]) msgData.get("PPBODY");
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
//            recipeParaList = TowaRecipeUtil.transferTowaRcp(TowaRecipeUtil.Y1R_RECIPE_CONFIG, ppbody);
            recipeParaList = AWD300TXRecipeUtil.TSKRecipeTran(recipePath);// TowaRecipeUtil.transferTowaRcpFromDB(deviceType, ppbody);
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
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

    @Override
    public Object clone() {
        AWD300TXHost newEquip = new AWD300TXHost(deviceId,
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

    private void initRptPara() {
        sendS1F17out();
        sendS1F1out();
        super.findDeviceRecipe();
    }

    @Override
    public Map holdDevice() {
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
    @Override
    public Map releaseDevice() {
        try {
            sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Map map = this.sendS2f41Cmd("START");
        if ((byte) map.get("HCACK") == 0) {
            this.setAlarmState(0);
        }
        return map;
    }

    @Override
    public void processS2F17in(DataMsgMap msg) {
//        throw new UnsupportedOperationException("Not yet implemented");
        try {
            DataMsgMap s2f18out = new DataMsgMap("s2f18out", activeWrapper.getDeviceId());
            String time = "";
            time = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
            s2f18out.put("TIME", time);
            long transactionId = msg.getTransactionId();
            s2f18out.setTransactionId(transactionId);
            activeWrapper.respondMessage(s2f18out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = null;
        try {
            equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        } catch (UploadRecipeErrorException e) {
            return;

        }
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过，设备将被锁定！");
                this.holdDeviceAndShowDetailInfo();
                logger.debug("设备：" + deviceCode + " 开机Check失败");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + "，异常参数名称为：" + recipePara.getParaName() + "，其异常值为：" + recipePara.getSetValue() + "，其设定值为：" + recipePara.getMinValue();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
//                this.releaseDevice();
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 0);
                changeEquipPanel(panelMap);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
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

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            ;
            if (ceid == EquipStateChangeCeid) {
                this.processS6F11EquipStatusChange(data);
            } else if (ceid == 272 || ceid == 273) {
                findDeviceRecipe();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (commState != 1) {
            this.setCommState(1);
        }
    }

    public void processS6F5in(DataMsgMap data) {
        try {
            DataMsgMap out = new DataMsgMap("S6F6OUT", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            SecsItem secsItem = new SecsItem((byte) 0, FormatCode.SECS_BINARY);
            out.put("S6F6OUT", secsItem);
            activeWrapper.respondMessage(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initRemoteCommand() {
    }
//    @SuppressWarnings("unchecked")
//    public void sendS1F13out() {
//        DataMsgMap s1f13out = new DataMsgMap("s1f13out", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s1f13out.setTransactionId(transactionId);
//        s1f13out.put("Mdln", "DR3003");
//        s1f13out.put("SoftRev", "GA2013");
//        try {
//            DataMsgMap data = activeWrapper.sendAwaitMessage(s1f13out);
//            if (data != null) {
//                setCommState(1);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
