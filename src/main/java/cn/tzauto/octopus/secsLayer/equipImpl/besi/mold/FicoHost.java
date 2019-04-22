package cn.tzauto.octopus.secsLayer.equipImpl.besi.mold;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.alarm.service.AutoAlter;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoLock;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.besi.FicoRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@SuppressWarnings("serial")
public class FicoHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(FicoHost.class.getName());
    boolean cancelCheckFlag = false;

    public FicoHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public Object clone() {
        FicoHost newEquip = new FicoHost(deviceId,
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

        while (!isInterrupted) {
            try {
                while (!this.isSdrReady()) {
//                    logger.info("等待网络连接就绪");
                    FicoHost.sleep(200);
                }
                if (this.getCommState() != FicoHost.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    //切换成ONLINE-REMOTE状态
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    super.updateLotId();
                    initRptPara();
//                    switchAlarm(false);
                    rptDefineNum++;
                }
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    //判断是否触发Repeat Alarm
                    processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    try {
                        processS6F11EquipStatusChange(msg);
                    } catch (Exception ex) {
                        logger.error("Exception:", ex);
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    try {
                        processS6F11EquipStatus(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11svgetfinish")) {
                    try {
                        processS6F11SVGetFinish(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    ppExecName = (String) ((SecsItem) msg.get("PPExecName")).getData();
                    Map map = new HashMap();
                    map.put("PPExecName", ppExecName);
                    changeEquipPanel(map);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.info(getName() + "从阻塞中退出...");
                logger.info("this.isInterrupted()=" + this.isInterrupted() + " is interrupt=" + isInterrupted);
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
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
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    public Map sendS1F3SingleCheck(String svid){
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplatesAll = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipeParaCheck");
        List svIdListAll = getSvIdList(recipeTemplatesAll);
        sqlSession.commit();
        List list = new ArrayList();
        for(Object sv : svIdListAll){
            list.add(Long.parseLong(String.valueOf(sv)));
        }
        sendS1F3RcpParaCheckout(list);
        return super.sendS1F3SingleCheck(svid);
    }


    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    public List sendS1F3PressCheckout() {
        DataMsgMap data = null;
        List svlist = new ArrayList();
        svlist.add(3300L);
        svlist.add(3400L);
        svlist.add(3500L);
        try {
            data = activeWrapper.sendS1F3out(svlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList<Object> list = (ArrayList) data.get("SV");
        return list;
    }


    public Map sendS1F3RcpParaCheckout(List svidlist) {
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS1F3out(svidlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList<Object> list = (ArrayList) data.get("SV");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("SVList", list);
        return resultMap;
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S5FX Code">
    @Override
    @SuppressWarnings("unchecked")

    public Map processS5F1in(DataMsgMap data) {
        long ALID = (long) data.get("ALID");
        byte ALCD = (byte) data.get("ALCD");
        String ALTX = data.get("ALTX").toString();
        logger.info("Received s5f1 ID:" + ALID + " from " + deviceCode + " with the ALCD=" + ALCD + " means " + ACKDescription.description(ALCD, "ALCD") + ", and the ALTX is: " + ALTX);
        UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "收到报警信息 " + " 报警ID:" + ALID + " 报警详情: " + ALTX);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s5f1");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("deviceId", deviceId);
        resultMap.put("ALID", ALID);
        resultMap.put("ALCD", ALCD);
        resultMap.put("ALTX", ALTX);
        resultMap.put("Description", ACKDescription.description(ALCD, "ALCD"));
        resultMap.put("TransactionId", data.getTransactionId());
        String[] ALIDs = {"100020361", "100020401", "100020441", "100020637", "100020657", "100020677", "100020695",
                "100020697", "100020699", "100021389", "100021391", "100021393", "100021371", "100021373", "100021375"};
        List<String> ALIDList = Arrays.asList(ALIDs);
        if (ALIDList.contains(String.valueOf(ALID))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "收到设备特殊报警，报警ID: " + ALID + " 报警详情: " + ALTX);
            checkFlagFunction();
        }
        AutoAlter.alter(resultMap);
        return resultMap;
    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 1 || ceid == 2 || ceid == 3) {
                processS6F11EquipStatus(data);
            } else if (ceid == 10) {
                processS6F11EquipStatusChange(data);
            } else if (ceid == 62) {
                findDeviceRecipe();
            } else if (ceid == 1555) {
                processS6F11SVGetFinish(data);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 2) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 3) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
            updateCommStateInExt();
//            //tranferEnd事件
//            if (ceid == 1551 || ceid == 1555 || ceid == 1559) {
//                //判断press是否在production状态
//               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "收到事件报告，CEID为" + ceid);
//                getUsingPress();
//                if (!pressUseMap.isEmpty()) {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "CEID为" + ceid + "，开始获取SV数据并保存");
//                    saveSVDataAndSituation(String.valueOf(ceid));
//                }
//            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    private void processS6F11SVGetFinish(DataMsgMap data) {
        long ceid = 0L;
        long pressStateId = 0L;
        long cavityVacuumValue = 0L;
        long boardVacuumValue = 0L;
        long rptid = 0L;
        try {
            ceid = (long) data.get("CEID");
            List report = (List) data.get("REPORT");
            rptid = (long) report.get(0);
            if (rptid == 1555) {
                List dataList = (List) report.get(1);
                pressStateId = (long) dataList.get(0);
                cavityVacuumValue = (long) dataList.get(1);
                boardVacuumValue = (long) dataList.get(2);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        String pressState = ACKDescription.describeOPMode(pressStateId, deviceType);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "收到事件报告[" + ceid + "], Press状态[" + pressState + "], CavityVacuum[" + cavityVacuumValue + "], BoardVacuum[" + boardVacuumValue + "]");
        if ("Production".equals(pressState)) {
            //使用FutureTask，延迟3s，如果收到特殊报警，则不进行比对
            checkSVSpecTask(String.valueOf(ceid), String.valueOf(cavityVacuumValue), String.valueOf(boardVacuumValue));
            saveSVDataAndSituation(String.valueOf(ceid));
        }
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

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {

            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            boolean dataReady = false;
            //判断当前执行程序是否是清模程序 Y代表清模程序
            boolean isCleanRecipe = false;
            List<Recipe> cleanRecipes = recipeService.searchRecipeByRcpType(ppExecName, "Y");
            if (cleanRecipes != null && cleanRecipes.size() > 1) {
                isCleanRecipe = true;
            }
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

            //保存到设备操作记录数据库并发送至服务端
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            //发送设备UPH参数至服务端
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "设备由于状态变化即将发送UPH参数");
            sendUphData2Server();

            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
                //开机通过OpMode获取Press使用情况
                getUsingPress();
                //检查press使用情况与MES是否相符，如果返回Y，表示没问题，流程继续
                checkPressUseState(deviceService, sqlSession);
//                if (!checkPressUseState(deviceService, sqlSession)) {
//                    return;
//                }
                //获取
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机获取SV数据");
                saveSVDataAndSituation("Start");
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
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
                if (!this.checkLockFlagFromServerByWS(deviceCode)) {
//                   UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    holdDeviceAndShowDetailInfo("Equipment has been set and locked by Server");
//                } else {
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
                            holdDeviceAndShowDetailInfo("RecipeName Error! Equipment locked!");
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        }
                    }

                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if ("Unique".equals(downloadRcpVersionType)) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check(Unique)");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck(Gold)");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo("Host has no gold recipe, equipment locked!");
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
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    public Map sendS7F1out(Recipe recipe) {
        String ppid = recipe.getRecipeName();
        recipePath = super.getRecipePathByConfig(recipe);
        long length = TransferUtil.getPPLength(recipePath) - 2;
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F1out(ppid, length, lengthFormat);
            logger.debug("Request send recipeName= " + ppid + " to Device " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte ppgnt = (byte) data.get("PPGNT");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", ppid);
        resultMap.put("ppgnt", ppgnt);
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        String ppbody = (String) data.get("PPBODY");
        TransferUtil.setPPBody(ppbody, 0, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = FicoRecipeUtil.transferFicoRcp(recipePath);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
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


    @Override
    @SuppressWarnings("unchecked")
    public Map sendS7F19out() {
        Map resultMap = super.sendS7F19out();
        if (resultMap == null || resultMap.get("eppd") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList recipeList = (ArrayList) resultMap.get("eppd");
        if (recipeList.size() != 0) {
            ArrayList rcpNameList = new ArrayList();
            for (Object obj : recipeList) {
                if (String.valueOf(obj).contains("/PR/")) {
                    rcpNameList.add(obj);
                }
            }
            resultMap.put("eppd", rcpNameList);
        }
        return resultMap;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S10FX Code">

    @Override
    protected void processS10F1in(DataMsgMap data) {
        String text = "";
        try {
            if (data.get("TEXT") != null) {
                text = data.get("TEXT").toString();
                logger.info("Received a s10f1in with text = " + text);
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "收到设备发送的消息:[" + text + "]");
            }
            activeWrapper.sendS10F2out((byte) 0, data.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (text != null && "OK".equalsIgnoreCase(text)) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            List<DeviceInfoLock> deviceInfoLocks = deviceService.searchDeviceInfoLockByMap(deviceCode, "SV_REAL_LOCK", "Y");
            sqlSession.close();
            if (deviceInfoLocks != null && !deviceInfoLocks.isEmpty()) {
                //在锁机消息页面，如果设备正常回复消息给工控机，则发送解锁命令
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "收到设备正常回复消息[OK]，发送解锁命令");
                releaseDeviceByServer("SV_REAL_LOCK");
            }
        }
    }
    // </editor-fold>


    public void getUsingPress() {
        List pressResults = sendS1F3PressCheckout();
        super.pressUseMap.clear();
        for (int i = 0; i < pressResults.size(); i++) {
            String svValue = ACKDescription.describeOPMode(pressResults.get(i), deviceType);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Press" + (i + 1) + ", OperationMode:" + svValue);
            if ("Production".equals(svValue)) {
                super.pressUseMap.put(i + 1, true);
            }
        }
    }

    /**
     * 开机check recipe参数
     *
     * @param checkRecipe
     * @param type
     */

    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常，参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                    String dateStr = GlobalConstants.dateFormat.format(new Date());
                    String eventDescEN = "(" + dateStr + ") Start Check Para Error! RecipePara[" + recipePara.getParaName() + "], realValue: " + recipePara.getSetValue() + ", defaultValue: " + recipePara.getDefValue() + ", out of spec[" + recipePara.getMinValue() + "-" + recipePara.getMaxValue() + "], machine locked.";
                    this.sendTerminalMsg2EqpSingle(eventDescEN);
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

    /**
     * 获取定时监控的值，并保存入数据库
     */
    public void saveSVDataAndSituation(String remark) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);

        //获取当前所使用的Recipe的具体参数
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        String curRecipeId = deviceInfoExt.getRecipeId();
        List<RecipePara> recipeParas = recipeService.searchRecipeParaByRcpRowId(curRecipeId);

        //获取当前recipe Gold版本的recipePara，暂不使用
//        List<Recipe> recipes = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
//        List<RecipePara> recipeParas = recipeService.searchRecipeParaByRcpRowId(recipes.get(0).getId());
        List<RecipeTemplate> recipeTemplates = getPressSv();
        List<RecipeTemplate> recipeTemplatesAll = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipeParaCheck");
        List svIdListAll = getSvIdList(recipeTemplatesAll);//获取机台所有参数对应的svIdList
        List svIdList = getSvIdList(recipeTemplates);//获取机台Press管控的实时参数的svIdList
        List<DeviceRealtimePara> deviceRealtimeParas = monitorService.getDeviceRealtimeParaByDeviceCode(deviceCode, null);
        try {
            Map resultMap = sendS1F3RcpParaCheckout(svIdListAll);
            if (resultMap != null && resultMap.size() > 0) {
                ArrayList svListAll = (ArrayList) resultMap.get("SVList");
                if (svIdListAll.size() != svIdList.size() && !svIdList.isEmpty()) {
                    ArrayList svList = new ArrayList();
                    for (int i = 0; i < svIdList.size(); i++) {
                        for (int j = 0; j < svIdListAll.size(); j++) {
                            if (svIdList.get(i).equals(svIdListAll.get(j))) {
                                svList.add(svListAll.get(j));
                                break;
                            }
                        }
                    }
                    deviceRealtimeParas = putSV2DeviceRealtimeParas(recipeTemplates, svList, recipeParas, deviceRealtimeParas, remark);
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "获取Press管控SV数据成功，SV数据量为" + deviceRealtimeParas.size());
                } else {
                    deviceRealtimeParas = putSV2DeviceRealtimeParas(recipeTemplatesAll, svListAll, recipeParas, deviceRealtimeParas, remark);
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "获取全部SV数据成功，SV数据量为" + deviceRealtimeParas.size());
                }
                monitorService.saveDeviceRealtimePara(deviceRealtimeParas);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "保存SV数据成功");
            }
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, e.getMessage());
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "保存SV数据失败");
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }

    }

    private List<RecipeTemplate> getPressSv() {
        List<RecipeTemplate> recipeTemplates = new ArrayList<>();
        List<String> pressList = new ArrayList();
        if (pressUseMap.containsKey(1)) {
            if (pressUseMap.get(1)) {
                String p1 = "P1RecipeParaCheck";
                pressList.add(p1);
            }
        }
        if (pressUseMap.containsKey(2)) {
            if (pressUseMap.get(2)) {
                String p2 = "P2RecipeParaCheck";
                pressList.add(p2);
            }
        }
        if (pressUseMap.containsKey(3)) {
            if (pressUseMap.get(3)) {
                String p3 = "P3RecipeParaCheck";
                pressList.add(p3);
            }
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            if (!pressList.isEmpty()) {
                recipeTemplates = recipeService.searchPressRecipeTemplateByDeviceCode(deviceType, pressList);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
        return recipeTemplates;
    }

    private String getRcpParaSetValue(String paraName) {
        String setValue = "";
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        try {
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            String curRecipeId = deviceInfoExt.getRecipeId();
            List<String> setValues = recipeService.searchByMapWithRcpTemp(curRecipeId, paraName, deviceType, "RecipeParaCheck");
            if (setValues != null && !setValues.isEmpty()) {
                setValue = setValues.get(0);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
        return setValue;
    }

    private void checkSVSpec(String ceid, String cavityValue, String boardValue) {
        //比较参数范围
        String press = "";
        if ("1551".equals(ceid)) {
            press = "PRESS1";
        } else if ("1555".equals(ceid)) {
            press = "PRESS2";
        } else if ("1559".equals(ceid)) {
            press = "PRESS3";
        }
        String paraName = press + "_CAVITYVACUUM";
        String setValue = getRcpParaSetValue(paraName);
        if (setValue == null || "".equals(setValue)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有获取到参数" + paraName + "的设定值，请联系ME处理");
            return;
        }
        double setValueD = Double.parseDouble(setValue) / 100;//单位转换，把帕转换成百帕
        double maxValueD = setValueD + 100;
        double minValueD = setValueD - 100;
        double realTimeValueD = Double.parseDouble(cavityValue);
        if (realTimeValueD > maxValueD || realTimeValueD < minValueD) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "参数" + paraName + "实时值为" + cavityValue + "，超出设定范围[" + minValueD + "-" + maxValueD + "]");
            String dateStr = GlobalConstants.dateFormat.format(new Date());
            this.sendTerminalMsg2EqpSingle("(" + dateStr + ") RecipePara[" + paraName + "] current value is " + cavityValue + ", out of spec[" + minValueD + "-" + maxValueD + "], machine locked.");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "向设备发送PAUSE命令");
            this.holdDeviceByServer("SV_REAL_LOCK");
        }
        double realTimeValueD2 = Double.parseDouble(boardValue);
        String paraName2 = press + "_CAVITYVACUUM";
        if (realTimeValueD2 == 0) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "参数" + paraName2 + "实时值为" + boardValue + "，超出设定范围");
            String dateStr = GlobalConstants.dateFormat.format(new Date());
            this.sendTerminalMsg2EqpSingle("(" + dateStr + ") RecipePara[" + paraName2 + "] realtimevalue is " + boardValue + ", out of spec, machine locked.");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "向设备发送PAUSE命令");
            this.holdDeviceByServer("SV_REAL_LOCK");
        }
    }

    public List<DeviceRealtimePara> putSV2DeviceRealtimeParas(List<RecipeTemplate> recipeTemplates, ArrayList svList, List<RecipePara> recipeParas, List<DeviceRealtimePara> deviceRealtimeParas, String remark) {
        List<DeviceRealtimePara> realTimeParas = new ArrayList<>();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            String minValue = "";
            String maxValue = "";
            String setValue = "";
            //根据ParaCode关联，找出SV参数名在RecipePara中对应的参数名，得到设定值、最大值、最小值
            String paraCode = recipeTemplates.get(i).getParaCode();
//            String paraName = recipeTemplates.get(i).getParaName();
            for (RecipePara recipePara : recipeParas) {
                if (paraCode.equals(recipePara.getParaCode())) {
                    setValue = recipePara.getSetValue();
                    minValue = recipePara.getMinValue();
                    maxValue = recipePara.getMaxValue();
                    break;
                }
            }

            String realTimeValue = String.valueOf(svList.get(i));
            DeviceRealtimePara realtimePara = new DeviceRealtimePara();
            realtimePara.setId(UUID.randomUUID().toString());
            realtimePara.setDeviceCode(deviceCode);
            realtimePara.setMaxValue(maxValue);
            realtimePara.setMinValue(minValue);
            realtimePara.setSetValue(setValue);
            realtimePara.setParaCode(recipeTemplates.get(i).getParaCode());
            realtimePara.setParaDesc(recipeTemplates.get(i).getParaDesc());
            realtimePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
            realtimePara.setParaName(recipeTemplates.get(i).getParaName());
            realtimePara.setRealtimeValue(realTimeValue);
            realtimePara.setValueType(recipeTemplates.get(i).getParaType());
            realtimePara.setRemarks(remark);
            if (deviceRealtimeParas != null && deviceRealtimeParas.size() > 0) {
                realtimePara.setUpdateCnt(deviceRealtimeParas.get(0).getUpdateCnt() + 1);
            } else {
                realtimePara.setUpdateCnt(0);
            }
            realTimeParas.add(realtimePara);
        }
        return realTimeParas;
    }

    public List<String> getSvIdList(List<RecipeTemplate> recipeTemplates) {
        List svIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            svIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return svIdList;
    }


    @Override
    public void sendUphData2Server() throws IOException, BrokenProtocolException, T6TimeOutException, HsmsProtocolNotSelectedException, T3TimeOutException, MessageDataException, StreamFunctionNotSupportException, ItemIntegrityException, InterruptedException {
        String output = "";
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List svidlist = recipeService.searchShotSVByDeviceType(deviceType);
        sqlSession.close();
        //获取前一状态与当前状态
//todo z这里处理的逻辑不正确
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

    /**
     * 关闭alarm信息接收
     *
     * @param enable
     */
    public void switchAlarm(boolean enable) {
        super.sendS5F3out(enable);
    }


    @Override
    public void sendTerminalMsg2EqpSingle(String msg) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            byte[] tid = new byte[1];
            tid[0] = 0;
            sendS10F3(tid[0], msg);
        }
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
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return map;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }


    @Override
    public Map releaseDevice() {
        Map map = this.sendS2f41Cmd("RESUME");
        this.setAlarmState(0);
        return map;
    }


    @Override
    public String getOutputData() {
        String outputSVID = "114";
        Map resultMap = sendS1F3SingleCheck(outputSVID);
        if (resultMap != null && resultMap.get("Value") != null) {
            return String.valueOf(resultMap.get("Value"));
        } else {
            return null;
        }
    }


    //获取特定的sv
    public Map getSpecialSVMap() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        MonitorService monitorService = new MonitorService(sqlSession);
        String[] paraNames = {"PRESS1_CLAMPFORCE", "PRESS2_CLAMPFORCE", "PRESS3_CLAMPFORCE", "PRESS1_CAVITYVACUUM", "PRESS2_CAVITYVACUUM", "PRESS3_CAVITYVACUUM"};
        long[] svids = {3306, 3406, 3506, 3371, 3471, 3571};
        Map svMap = new HashMap();
        for (int i = 0; i < paraNames.length; i++) {
            List<DeviceRealtimePara> deviceRealtimeParas = monitorService.getParasInTime(deviceCode, paraNames[i], 10);//取10分钟以内的最近一次的值
            String realtimeValue = "";
            if (deviceRealtimeParas != null && !deviceRealtimeParas.isEmpty()) {
                realtimeValue = deviceRealtimeParas.get(0).getRealtimeValue();
            }
            svMap.put(svids[i], realtimeValue);
        }
        sqlSession.close();
        return svMap;
    }

    public void checkFlagFunction() {
        cancelCheckFlag = true;
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备的取消校验标记(cancelCheckFlag)为" + cancelCheckFlag);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Boolean> future = new FutureTask<>(
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws InterruptedException {
                        Thread.sleep(5000);
                        cancelCheckFlag = false;
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备的取消校验标记(cancelCheckFlag)为" + cancelCheckFlag);
                        return false;
                    }
                });
        executor.execute(future);
        executor.shutdown();
    }

    public void checkSVSpecTask(String ceid, String cavityVacuumValue, String boardVacuumValue) {
        final String ceidF = ceid;
        final String cavityVacuumValueF = cavityVacuumValue;
        final String boardVacuumValueF = boardVacuumValue;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Boolean> future = new FutureTask<>(
                new Callable<Boolean>() {

                    @Override
                    public Boolean call() throws InterruptedException {
                        Thread.sleep(3000);
                        if (!cancelCheckFlag) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始进行SV参数比对");
                            checkSVSpec(ceidF, cavityVacuumValueF, boardVacuumValueF);
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "取消此次SV参数比对");
                        }
                        return false;
                    }
                });
        executor.execute(future);
        executor.shutdown();
    }

    private void initRptPara() {
        //重定义机台的equipstatuschange事件报告
        List list1 = new ArrayList();
        list1.add(15L);
        list1.add(97L);
        sendS2F33Out(10L, 15L, list1);
        sendS2F35out(10L, 10L, 10L);
//        sendS2F37out(10l);

        //重定义ppchange事件
        List list = new ArrayList();
        list.add(97L);

        sendS2F33Out(4L, 60L, list);
//        sendS2F33Out(60l, 97l);
        sendS2F35out(60L, 60L, 60L);
//        sendS2F37out(60l);

        //重定义1551,1555,1559事件
        List list2 = new ArrayList();
        list2.add(3300L);
        list2.add(3371L);
        list2.add(3370L);
        sendS2F33Out(1551L, 1551L, list2);
//        sendS2f33outMulti(1551L, 3300L, 3371L, 3370L);
        sendS2F35out(1551L, 1551L, 1551L);
        List list3 = new ArrayList();
        list3.add(3400L);
        list3.add(3471L);
        list3.add(3470L);
        sendS2F33Out(1555L, 1555L, list3);
//        sendS2f33outMulti(1555L, 3400L, 3471L, 3470L);
        sendS2F35out(1555L, 1555L, 1555L);
        List list4 = new ArrayList();
        list4.add(3500L);
        list4.add(3571L);
        list4.add(3570L);
        sendS2F33Out(1559L, 1559L, list4);
//        sendS2f33outMulti(1559L, 3500L, 3571L, 3570L);
        sendS2F35out(1559L, 1559L, 1559L);

//        super.sendS2F37outCloseAll(activeWrapper);
        long[] ceids = {70L, 2L, 3L, 60L, 62L, 1L, 22L, 18L, 11L, 10L, 20L, 17L, 1550L, 1551L, 1552L, 1553L, 1554L, 1555L, 1556L, 1557L, 1558L, 1559L, 1560L, 1561L};
        List ceidList = new ArrayList();
        for (int i = 0; i < ceids.length; i++) {
            ceidList.add(ceids[i]);
        }
        try {
            activeWrapper.sendS2F37out(true, ceidList, ceFormat);
        } catch (HsmsProtocolNotSelectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (StreamFunctionNotSupportException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ItemIntegrityException e) {
            e.printStackTrace();
        } catch (MessageDataException e) {
            e.printStackTrace();
        } catch (BrokenProtocolException e) {
            e.printStackTrace();
        } catch (T3TimeOutException e) {
            e.printStackTrace();
        } catch (T6TimeOutException e) {
            e.printStackTrace();
        }
        //todo sendS2F37outMuilt
//        super.sendS2F37outMuilt(true, ceids);
//        super.sendS2F37outAll(activeWrapper);

    }


    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
