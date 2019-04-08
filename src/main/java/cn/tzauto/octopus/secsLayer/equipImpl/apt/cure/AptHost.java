package cn.tzauto.octopus.secsLayer.equipImpl.apt.cure;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.apt.APTRecipeUtil;
import cn.tzauto.octopus.common.util.tool.CSVUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

@SuppressWarnings("serial")
public class AptHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AptHost.class.getName());

    public AptHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
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
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();//
                    //获取lot号
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
//                if (!FengCeConstant.CONTROL_REMOTE_ONLINE.equalsIgnoreCase(controlState)) {
//                    if (changeEqptControlStateAndShowDetailInfo("REMOTE")) {
//                        controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
//                    }
//                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    try {
                        processS6F11EquipStatus(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {

                logger.error("Exception:", e);
                // logger.fatal("Caught Interruption", e);
            }
        }
    }

    public Map sendS1F3SingleCheck(String svidName) {
        CSVUtil.setCSVFile(getProfile(), deviceCode, ppExecName);
        return null;
    }

    @Override
    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            secsMsgTimeoutTime = 0;
            LastComDate = new Date().getTime();
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                this.putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
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
    private List sendf3beforeStartCheck() {
        DataMsgMap s1f3beforeStartCheck = new DataMsgMap("s1f3beforeStartCheck", activeWrapper.getDeviceId());
        long transActionId = activeWrapper.getNextAvailableTransactionId();
        s1f3beforeStartCheck.setTransactionId(transActionId);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s1f3beforeStartCheck);
            logger.debug("get data from s1f3beforeStartCheck." + JsonMapper.toJsonString(data));
        } catch (Exception e) {
        }
        if (data == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transActionId);
        }
        if (data == null) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        return listtmp;
    }

    // </editor-fold> 

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 1002) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 1003) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1001) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
            updateCommStateInExt();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //ppselect 事件
        if (ceid == 1030L) {
            this.findDeviceRecipe();
        }
        //更新页面显示内容
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        showCollectionsEventInfo(ceid);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (ceid == 1400L) {
            equipStatus = "Idle";
            changeEqptControlStateAndShowDetailInfo("REMOTE");
        } else if (ceid == 1401L) {
            equipStatus = "Run";
            processS6F11EquipStatusChange(data);
        } else if (ceid == 1402L) {
            equipStatus = "End";
            //get profile from equip
            try {
                findDeviceRecipe();
                String fileName = CSVUtil.setCSVFile(getProfile(), deviceCode, ppExecName);
                if (!"".equals(fileName)) {
                    Map mqMap = new HashMap();
                    mqMap.put("msgName", "eqp.EqpEnd");
                    mqMap.put("fileName", fileName);
                    mqMap.put("recipeName", ppExecName);
                    mqMap.put("lotID", lotId);
                    mqMap.put("eqpCode", deviceCode);
                    GlobalConstants.C2SSpecificDataQueue.sendMessage(mqMap);
                    UiLogUtil.appendLog2SeverTab(deviceCode, "发送设备CSV文件至服务端");
                }
            } catch (Exception e) {
            }
        } else if (ceid == 1403L) {
            equipStatus = "Maintain";
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        deviceInfoExt.setDeviceStatus(equipStatus);
        saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
        sqlSession.commit();
        sqlSession.close();
    }

    public List getProfile() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3singleout", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] svids = new long[1];
        svids[0] = 2107L;
        s1f3out.put("SVID", svids);
        DataMsgMap data = null;
        logger.info("设备" + deviceCode + "开始发送S1F3SingleCheck");
        try {
            logger.info("1data == null:" + data == null);
            logger.info("1data.get(RESULT) == null:" + data.get("SV") == null);
            return (ArrayList) sendS1F3SingleCheck("2107").get("SV");
        } catch (Exception e) {
            logger.error("Exception:getProfile", e);
            return null;
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            findDeviceRecipe();
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

            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
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

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        String ppbody = String.valueOf(TransferUtil.getPPBody(2, localRecipeFilePath).get(0));
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_ASCII);
        s7f3out.put("ProcessprogramID", targetRecipeName);
        s7f3out.put("Processprogram", ppbody);
        try {
            sleep(1000);
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null) {
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

    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        String ppbody = (String) msgdata.get("PPBODY");
        TransferUtil.setPPBody(ppbody, 0, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
//            recipeParaList = TowaRecipeUtil.transferTowaRcp(TowaRecipeUtil.Y1R_RECIPE_CONFIG, ppbody);
            recipeParaList = APTRecipeUtil.transRcpParaFromDB(recipePath, deviceType);
            for (int i = 0; i < recipeParaList.size(); i++) {
                String paraName = recipeParaList.get(i).getParaName();
                if (paraName.equals("") || paraName.equals("NULL")) {
                    recipeParaList.remove(i);
                    i--;
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
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

    public Object clone() {
        AptHost newEquip = new AptHost(deviceId,
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
//        sendS2F33Out(1401L, 2100L, 2302L, 2001L);
//        sendS2f35out(1401L, 1401L, 1401L);
//        sendS2F37out(1401L);
        sendS2F37outAll();
        if (changeEqptControlStateAndShowDetailInfo("REMOTE")) {
            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
            Map map = new HashMap();
            map.put("ControlState", FengCeConstant.CONTROL_REMOTE_ONLINE);
            changeEquipPanel(map);
        }
    }

    //释放机台
    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        return null;
    }

    @Override
    public Map startDevice() {
        String checkResult = beforeStartCheck();
        if (!checkResult.equals("0")) {
            UiLogUtil.appendLog2SecsTab(deviceCode, checkResult);
            Map map = new HashMap();
            map.put("HCACK", 2);
            map.put("CheckResult", checkResult);
            return map;
        }
        Map resultMap = this.sendS2f41Cmd("START");
        if (resultMap != null) {
            String hcack = String.valueOf(resultMap.get("HCACK"));
            if ("0".equals(hcack) || "4".equals(hcack)) {
                int count = 10;
                while (count > 0) {
                    try {
                        sleep(2000);
                        findDeviceRecipe();
                        if (equipStatus.equalsIgnoreCase("run")) {
                            UiLogUtil.appendLog2SecsTab(deviceCode, "自动开机成功！");
                            resultMap.put("HCACK", 0);
                            break;
                        } else {
                            resultMap.put("HCACK", 2);
                        }
                    } catch (InterruptedException ex) {
                        logger.error("进行循环Start命令时，线程中断！");
                        logger.error(ex.getMessage());
                        resultMap.put("HCACK", 2);
                        resultMap.put("CheckResult", "线程中断！");
                        break;
                    }
                    count--;
                }
                //如果循环结束,状态还不是run，则判定开机失败
                if (count < 1) {
                    resultMap.put("CheckResult", "机台未开机成功，请查看机台是否存在报警！");
                }
            }
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        if (!FengCeConstant.CONTROL_REMOTE_ONLINE.equals(controlState)) {
            changeEqptControlStateAndShowDetailInfo("REMOTE");
        }
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName);
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
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

    public List getSvIdList(List<RecipeTemplate> recipeTemplates) {
        List svIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            svIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return svIdList;
    }



    /**
     * 在发送开机命令前检查设备的各项条件是否具备
     *
     * @return
     */
    private String beforeStartCheck() {
        List checkValueList = sendf3beforeStartCheck();
        try {
            UiLogUtil.appendLog2EventTab(deviceCode, "执行设备自动开机前的各项状态检查...");
            if (!equipStatus.equalsIgnoreCase(FengCeConstant.STATUS_IDLE)) {
                return "开机前检查失败,设备状态异常,请将设备调整为Idle状态!";
            }
            UiLogUtil.appendLog2EventTab(deviceCode, "设备运行状态为IDLE，检查通过！");
            if (!controlState.equalsIgnoreCase(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备控制状态为[" + controlState + "]，检查不通过，尝试自动切换至Remote！");
                if (changeEqptControlStateAndShowDetailInfo("REMOTE")) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "设备控制状态自动切换至Remote，检查通过！");
                } else {
                    UiLogUtil.appendLog2EventTab(deviceCode, "设备控制状态自动切换Remote失败，请手动将设备调整为Remote状态!");
                    return "开机前检查失败,设备控制状态异常,请将设备调整为Remote状态!";
                }
            }
            UiLogUtil.appendLog2EventTab(deviceCode, "设备控制状态为Remote，检查通过！");
            if (checkValueList != null && checkValueList.size() > 0) {
                //doorState必须为0，close
                long doorState = Long.parseLong(String.valueOf(checkValueList.get(0)));
                if (doorState != 0) {
                    return "开机前检查失败,门状态异常,请确认门是否关闭!";
                }
                UiLogUtil.appendLog2EventTab(deviceCode, "门状态为关闭，检查通过！");
                //pressure 必须小于0.03
                float pressure = Float.parseFloat(String.valueOf(checkValueList.get(1)));
                if (pressure >= 0.03) {
                    return "开机前检查失败,Pressure参数异常,当前值[" + pressure + "]Kgf/cm2.";
                }
                UiLogUtil.appendLog2EventTab(deviceCode, "压力值为[" + pressure + "]Kgf/cm2，检查通过！");
                //ch2Temperature必须小于机台启动温度的设定值
                float ch2Temperature = Float.parseFloat(String.valueOf(checkValueList.get(2)));
                float canStartTemperature = getCanStartTemperature();
                if (ch2Temperature > canStartTemperature) {
                    return "开机前检查失败,CH2 Temperature参数异常,大于可开机温度,当前值:[" + ch2Temperature + "],最高可开机温度:[" + canStartTemperature + "]";
                }
                UiLogUtil.appendLog2EventTab(deviceCode, "CH2 温度为:[" + ch2Temperature + "],最高可开机温度:[" + canStartTemperature + "]，检查通过！");
                //runStep 必须为1
                long runStep = Long.parseLong(String.valueOf(checkValueList.get(3)));
                if (runStep != 1) {
                    return "开机前检查失败,RunStep参数异常,当前值" + runStep;
                }
                UiLogUtil.appendLog2EventTab(deviceCode, "RunStep状态为[1]，检查通过！");
            }
        } catch (Exception e) {
            logger.error("Exception occur, exception info:" + e.getMessage());
        }
        return "0";
    }

    private float getCanStartTemperature() {
        DataMsgMap s2f13CanStartTemperature = new DataMsgMap("s2f13CanStartTemperature", activeWrapper.getDeviceId());
        s2f13CanStartTemperature.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s2f13CanStartTemperature);
        } catch (Exception e) {
        }
        if (data == null) {
            logger.debug("Get the CanStartTemperature failed");
            return Float.MAX_VALUE;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        float canStartTemperature = Float.valueOf(String.valueOf(listtmp.get(0)));
        logger.debug("Get the CanStartTemperature:[" + canStartTemperature + "]");
        return canStartTemperature;
    }

    /**
     * @param transactionId
     * @return
     */
    @Override
    public DataMsgMap getMsgDataFromWaitMsgValueMapByTransactionId(long transactionId) {
        int i = 0;
        logger.info("Can not get value directly,will try to get value from message queue");
        while (i < 10) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                logger.error("Exception occur，Exception info：从WaitMsgValueMap中获取消息时，线程中断。设备编号：" + deviceCode);
                ex.printStackTrace();
            }
            DataMsgMap DataMsgMap = this.waitMsgValueMap.get(transactionId);
            logger.info("try===>" + i);
            if (DataMsgMap != null) {
                logger.info("Had get value from message queue ===>" + JSONArray.toJSON(DataMsgMap));
                return DataMsgMap;
            }
            i++;
        }
        if (i >= 10) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "从设备获取数据失败，请检查设备通讯状态！");
            logger.error("从设备获取数据失败，设备编号：" + deviceCode);
            return null;
        }
        waitMsgValueMap.remove(transactionId);
        if (waitMsgValueMap.entrySet().size() > 1000) {
            waitMsgValueMap.clear();
        }
        return null;
    }
}
