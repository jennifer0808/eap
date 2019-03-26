package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoLock;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandParaPair;
import cn.tzauto.octopus.secsLayer.exception.NotInitializedException;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.DeviceComm;
import cn.tzauto.octopus.secsLayer.util.XmlUtil;
import cn.tzauto.octopus.biz.alarm.service.AlarmService;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.exception.DeviceNotRegisteredException;
import cn.tzinfo.smartSecsDriver.exception.HsmsProtocolNotSelectedException;
import cn.tzinfo.smartSecsDriver.exception.NoConnectionException;
import cn.tzinfo.smartSecsDriver.exception.ParameterErrorException;
import cn.tzinfo.smartSecsDriver.network.RegistrationTable;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.CntlMessageArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.ErrorDataMessageDealer;
import cn.tzinfo.smartSecsDriver.userapi.HsmsCommunicationFailureDealer;
import cn.tzinfo.smartSecsDriver.userapi.InputMessageListener;
import cn.tzinfo.smartSecsDriver.userapi.MliFactory;
import cn.tzinfo.smartSecsDriver.userapi.MliHsms;
import cn.tzinfo.smartSecsDriver.userapi.MsgDataHashtable;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import cn.tzinfo.smartSecsDriver.userapi.UpLevelAnouncer;
import cn.tzinfo.smartSecsDriver.util.MliConfig;
import com.alibaba.fastjson.JSONArray;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 * @author dingxiaoguo
 * @Company 南京钛志信息系统有限公司
 * @Create Date 2016-7-14
 * @(#)EquipHost.java
 */
public abstract class EquipHost extends Thread implements InputMessageListener {

    private static final long serialVersionUID = -8008553978164121001L;
    private static Logger logger = Logger.getLogger(EquipHost.class.getName());
    public static final int COMMUNICATING = 1;
    public static final int NOT_COMMUNICATING = 0;
    private int commState = NOT_COMMUNICATING;
    public String controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
    private int alarmState = 0;
    private boolean jsipReady = false;
    protected String equipId;
    protected String smlFilePath;
    protected String localIPAddress;
    protected int localTCPPort;
    protected String remoteIPAddress;
    protected int remoteTCPPort;
    protected String connectMode = "active"; //only "active" or "passive" allowed, default is "active"
    protected String protocolType = "hsms";//only "hsms" or "rs232" allowed, default is "hsms"
    protected String deviceId;
    protected MliHsms mli;
    protected String description;
    protected boolean startUp;
    protected EquipState equipState;
    protected boolean threadUsed; //Java Thread can be only started once
    protected LinkedBlockingQueue<MsgDataHashtable> inputMsgQueue;
    //only stores messages which needs to delegate
    protected String deviceType;//设备类型
    protected String manufacturer;//生产厂商
    protected String deviceCode;//设备代码;
    protected String ProcessProgramID;
    protected int recipeType;
    protected String iconPath;
    protected String Mdln = "";
    protected String SoftRev = "";
    protected String recipePath = "";
    public String ppExecName = "--";
    public String equipStatus = "--";
    public String preEquipStatus = "";
    public String lotId = "--";
    protected long LastComDate = 0;//最后一次通信时间
    public Map<Integer, Boolean> pressUseMap = new HashMap<>();
    public boolean holdSuccessFlag = true;
    public int secsMsgTimeoutTime = 0;//check通信超时次数
    protected int rptDefineNum = 0;
    public int checkNotComm = 0;
    public int checkNotReady = 0;
    public boolean isCleanMode = false;
    public Map<String, CommandDomain> remoteCommandMap = new HashMap<>();
    public Map<Long, MsgDataHashtable> waitMsgValueMap = new ConcurrentHashMap<>();
    protected short svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    protected short ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    protected long lastStartCheckTime;
    protected long startCheckIntervalTime = 60;
    protected boolean holdFlag = false;
    protected boolean startSVcheckPass = false;
    protected boolean isRestarting = true;
    protected String lastMsgTagName = "";
    protected volatile boolean isInterrupted = false;
    protected long startedDate = 0;//Host启动的时间
    public int restartCnt = 0;

    public EquipHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int rcpType, String iconPath) {
        this.deviceId = devId;
        this.equipId = equipmentId;
        this.smlFilePath = smlFileFullPath;
        this.localIPAddress = localIpAddress;
        this.localTCPPort = localTcpPort;
        this.remoteIPAddress = remoteIpAddress;
        this.remoteTCPPort = remoteTcpPort;
        this.inputMsgQueue = new LinkedBlockingQueue<>(5000);
        equipState = new EquipState();
        threadUsed = false;
        this.deviceType = deviceType;
        this.deviceCode = deviceCode;
        this.recipeType = rcpType;
        this.iconPath = iconPath;
    }

    public EquipHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int rcpType, String iconPath) {
        this.deviceId = devId;
        this.equipId = equipmentId;
        this.smlFilePath = smlFileFullPath;
        this.localIPAddress = localIpAddress;
        this.localTCPPort = localTcpPort;
        this.remoteIPAddress = remoteIpAddress;
        this.remoteTCPPort = remoteTcpPort;
        this.connectMode = connectMode; //only "active" or "passive" allowed, default is "active"
        this.protocolType = protocolType;//only "hsms" or "rs232" allowed, default is "hsms"
        this.inputMsgQueue = new LinkedBlockingQueue<>(5000);
        equipState = new EquipState();
        threadUsed = false;
        this.deviceType = deviceType;
        this.deviceCode = deviceCode;
        this.recipeType = rcpType;
        this.iconPath = iconPath;
    }

    public void initialize() throws ParameterErrorException, DeviceNotRegisteredException, NullPointerException {
        logger.info("Initializing SECS Protocol for " + this.getEquipId() + ".");
        RegistrationTable.registerHsmsActive(Integer.valueOf(this.deviceId), this.smlFilePath,
                this.remoteIPAddress, this.remoteTCPPort, this.localIPAddress, this.localTCPPort);
        MliConfig.setDumpAllSettings(true);
        mli = (MliHsms) MliFactory.getMliForDevice(Integer.valueOf(this.deviceId));
        //put all addInputMessageListener before calling connectAnsStart()
        //mli.addInputMessageListener(this, "s1f13in");
        //mli.addInputMessageListener(this, "s6f12in");
        //mli.addInputMessageListener(this, "s1f1in");
        mli.addInputMessageListenerToAll(this);
    }

    public int deselectReqMessageArrived(CntlMessageArrivedEvent evt) {
        logger.info("Deselect is received and is supported.");
        //((MliHsms)testObj.mli).
        return 0;
    }

    public boolean isThreadUsed() {
        return threadUsed;
    }

    @Override
    public abstract Object clone();

    protected void clear() {
        this.equipId = null;
        this.smlFilePath = null;
        this.localIPAddress = null;
        this.remoteIPAddress = null;
        this.connectMode = null;
        this.protocolType = null;
        this.description = null;
        this.mli.removeInputMessageListenerToAll(this);
        this.mli = null;
        this.inputMsgQueue = null;
        commState = NOT_COMMUNICATING;
        controlState = FengCeConstant.CONTROL_OFFLINE;
        jsipReady = false;
    }

    /**
     * @return the jsipReady
     */
    public boolean isJsipReady() {
        return jsipReady;
    }

    /**
     * @param jsipReady the jsipReady to set
     */
    public synchronized void setJsipReady(boolean jsipReady) {
        this.jsipReady = jsipReady;
    }

    // <editor-fold defaultstate="collapsed" desc="getset Code">
    public long getLastComDate() {
        return LastComDate;
    }

    public void setLastComDate(long LastComDate) {
        this.LastComDate = LastComDate;
    }

    /**
     * @return the commState
     */
    public int getCommState() {
        return commState;
    }

    /**
     * @param commState the commState to set
     */
    public synchronized void setCommState(int commState) {

        if (commState == 1) {
            equipState.setCommOn(true);
            this.jsipReady = true;
//            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
        }
        if (commState == 0) {
            equipState.setCommOn(false);
            this.jsipReady = false;
        }
        this.commState = commState;
        Map resultMap = new HashMap();
        resultMap.put("CommState", commState);
        changeEquipPanel(resultMap);
    }

    /**
     * @return the controlState
     */
    public String getControlState() {
        return controlState;
    }

    public synchronized void setAlarmState(int alarmState) {
//        equipState.setAlarmState(alarmState);
        this.alarmState = alarmState;
        Map resultMap = new HashMap();
        resultMap.put("AlarmState", alarmState);
        changeEquipPanel(resultMap);
    }

    /**
     * @return the controlState
     */
    public int getAlarmState() {
        return alarmState;
    }

    /**
     * @param controlState the controlState to set
     */
    public synchronized void setControlState(String controlState) {
        this.controlState = controlState;
        Map resultMap = new HashMap();
        resultMap.put("ControlState", controlState);
        changeEquipPanel(resultMap);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MliHsms getMli() {
        return mli;
    }

    public boolean isStartUp() {
        return startUp;
    }

    public void setStartUp(boolean startUp) {
        this.startUp = startUp;
    }

    public String getSmlFilePath() {
        return smlFilePath;
    }

    public String getLocalIPAddress() {
        return localIPAddress;
    }

    public int getLocalTCPPort() {
        return localTCPPort;
    }

    public String getRemoteIPAddress() {
        return remoteIPAddress;
    }

    public int getRemoteTCPPort() {
        return remoteTCPPort;
    }

    public String getConnectMode() {
        return connectMode;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public EquipState getEquipState() {
        return equipState;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    private String getRecipeName() {
        return "";
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String[] getMdlnSoftRev() {
        String[] MdlnSoftRev = new String[2];
        MdlnSoftRev[0] = Mdln;
        MdlnSoftRev[1] = SoftRev;
        return MdlnSoftRev;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getEquipId() {
        return equipId;
    }

    public String getLotId() {
        return lotId;
    }

    public void setEquipId(String equipId) {
        this.equipId = equipId;
    }

    public int getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(int recipeType) {
        this.recipeType = recipeType;
    }

    public DeviceInfoExt setDeviceInfoExt() {
        DeviceInfoExt deviceInfoExt = new DeviceInfoExt();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        Recipe recipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
        sqlSession.close();
        if (recipe != null) {
            deviceInfoExt.setRecipeId(recipe.getId());
            deviceInfoExt.setVerNo(recipe.getVersionNo());
        }
        deviceInfoExt.setDeviceRowid(deviceCode);
        deviceInfoExt.setDeviceStatus(equipStatus);
        deviceInfoExt.setLockFlag("N");
        deviceInfoExt.setLotId(lotId);
        deviceInfoExt.setRecipeName(ppExecName);
        return deviceInfoExt;
    }
    //设置机台信息记录

    protected DeviceOplog setDeviceOplog(long ceid, String PPExecName, String equipStatus, String formerDeviceStatus, String lotId) {
        DeviceOplog deviceOplog = new DeviceOplog();
        deviceOplog.setId(UUID.randomUUID().toString());
        deviceOplog.setDeviceCode(deviceCode);
        deviceOplog.setCurrRecipeName(PPExecName);
        deviceOplog.setDeviceCeid(String.valueOf(ceid));
        deviceOplog.setCurrLotId(lotId);
        deviceOplog.setOpTime(new Date());
        deviceOplog.setOpDesc("机台状态从" + formerDeviceStatus + "切换为" + equipStatus);
        deviceOplog.setOpType("eqpt.EqptStatusChange");
        deviceOplog.setFormerDeviceStatus(formerDeviceStatus);
        deviceOplog.setCurrDeviceStatus(equipStatus);
        deviceOplog.setCreateBy("System");
        deviceOplog.setCreateDate(new Date());
        deviceOplog.setUpdateBy("System");
        deviceOplog.setUpdateDate(new Date());
        deviceOplog.setDelFlag("0");
        deviceOplog.setVerNo(0);
        return deviceOplog;
    }

    protected Recipe setRecipe(String recipeName) {
        Recipe recipe = new Recipe();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        List<DeviceInfo> deviceInfotmp = deviceService.getDeviceInfoByDeviceCode(deviceCode);
        DeviceInfo deviceInfo = deviceInfotmp.get(0);
//        DeviceType deviceType = deviceService.getDeviceTypeMap().get(deviceInfo.getDeviceTypeId());
        recipe.setClientId(GlobalConstants.getProperty("clientId"));
        if (GlobalConstants.sysUser != null && GlobalConstants.sysUser.getId() != null) {
            recipe.setCreateBy(GlobalConstants.sysUser.getId());
            recipe.setUpdateBy(GlobalConstants.sysUser.getId());
        } else {
            recipe.setCreateBy("System");
            recipe.setUpdateBy("System");
        }
        recipe.setDeviceCode(deviceInfo.getDeviceCode());
        recipe.setDeviceId(deviceInfo.getId());
        recipe.setDeviceName(deviceInfo.getDeviceName());
        recipe.setDeviceTypeCode(deviceInfo.getDeviceType());
        recipe.setDeviceTypeId(deviceInfo.getDeviceTypeId());
        recipe.setDeviceTypeName(deviceInfo.getDeviceType());
        //TODO 绑定recipe与产品的关系
        recipe.setProdCode("");//LGA
        recipe.setProdId("");//LGA001-12138
        recipe.setProdName("");//LGA_0.48*0.68
        recipe.setRecipeCode("");
        recipe.setRecipeDesc("");
        recipe.setRecipeName(recipeName);
        recipe.setRecipeStatus("Create");
        recipe.setRecipeType("N");
        recipe.setSrcDeviceId(GlobalConstants.getProperty("clientId"));
        //如果已存在，版本号加1     
        List<Recipe> recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, "Engineer", null);
        Recipe recipeTmp = null;
        if (recipes != null && recipes.size() > 0) {
            recipeTmp = recipes.get(0);
        }
        if (recipeTmp != null) {
            recipe.setVersionNo(recipeTmp.getVersionNo() + 1);
            recipe.setTotalCnt(recipeTmp.getTotalCnt() + 1);
            recipe.setUpdateCnt(recipeTmp.getUpdateCnt() + 1);
        } else {
            recipe.setVersionNo(0);
            recipe.setTotalCnt(0);
            recipe.setUpdateCnt(0);
        }
        recipe.setVersionType("Engineer");
        recipe.setCreateDate(new Date());
        sqlSession.close();
        return recipe;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Equipment Running Logic">
    // <editor-fold defaultstate="collapsed" desc="Equipment StartCheck Logic">
    // <editor-fold defaultstate="collapsed" desc="StartCheck Code">
    public void startCheckRecipePara(Recipe checkRecipe) {
        startCheckRecipePara(checkRecipe, "");
    }

    /**
     * 开机check recipe参数
     *
     * @param checkRecipe
     * @param type
     */
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        logger.info("START CHECK: BEGIN" + new Date());
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        logger.info("START CHECK: ready to upload recipe:" + new Date());
        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        logger.info("START CHECK: transfer recipe over :" + new Date());
        logger.info("START CHECK: ready to check recipe para:" + new Date());
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        logger.info("START CHECK: check recipe para over :" + new Date());
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            String checkRecultDesc = "";
            String eventDescEng = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为[" + recipePara.getParaCode() + "],参数名:[" + recipePara.getParaName() + "]其异常设定值为[" + recipePara.getSetValue() + "],默认值为[" + recipePara.getDefValue() + "]"
                            + "其最小设定值为[" + recipePara.getMinValue() + "],其最大设定值为[" + recipePara.getMaxValue() + "]";
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                    checkRecultDesc = checkRecultDesc + eventDesc;
                    String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "/r/n";
                    eventDescEng = eventDescEng + eventDescEngtmp;
                }
                this.holdDeviceAndShowDetailInfo("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng);
                //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
                checkRecultDesc = eventDesc;
            }
            mqMap.put("eventDesc", checkRecultDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 下载前检查，‘0’表示ok
     *
     * @param recipeName
     * @return
     */
    protected String checkBeforeDownload(String recipeName) {
        String checkResult = checkCommState();
        if ("0".equals(checkResult)) {
            checkResult = checkEquipStatus();
        }
        if ("0".equals(checkResult)) {
            checkResult = checkPPExecName(recipeName);
        }
        if ("0".equals(checkResult)) {
            checkResult = checkControlState();
        }
        return checkResult;
    }

    public String checkCommState() {
        Map map = findDeviceRecipe();
        if (map == null || map.isEmpty()) {
            return "无法获取设备实时状态,请重试并检查设备通讯状态!下载失败！";
        }
        return "0";
    }

    public String checkEquipStatus() {
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！下载失败！";
        }
        return "0";
    }

    public String checkPPExecName(String recipeName) {
        if (recipeName.equals(ppExecName)) {
        }
        return "0";
    }

    public String checkControlState() {
        if (FengCeConstant.CONTROL_REMOTE_ONLINE.equalsIgnoreCase(controlState)) {
        }
        return "0";
    }

    // </editor-fold> 
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Equipment Communicating Logic">
    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    public void processS1F1in(MsgDataHashtable data) {
        try {
            MsgDataHashtable s1f2out = new MsgDataHashtable("s1f2out", mli.getDeviceId());
            s1f2out.put("Mdln", Mdln);
            s1f2out.put("SoftRev", SoftRev);
            s1f2out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(s1f2out);
            setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F1out() {
        MsgDataHashtable s1f1out = new MsgDataHashtable("s1f1out", mli.getDeviceId());
        s1f1out.setTransactionId(mli.getNextAvailableTransactionId());
        try {
            MsgDataHashtable s1f2in = mli.sendPrimaryWsetMessage(s1f1out);
            if (s1f2in != null && s1f2in.get("Mdln") != null) {
                Mdln = (String) ((SecsItem) s1f2in.get("Mdln")).getData();
                SoftRev = (String) ((SecsItem) s1f2in.get("SoftRev")).getData();
                setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS1F2in(MsgDataHashtable s1f2in) {
        if (s1f2in == null) {
            return;
        }
        Mdln = (String) ((SecsItem) s1f2in.get("Mdln")).getData();
        SoftRev = (String) ((SecsItem) s1f2in.get("SoftRev")).getData();
    }

    /**
     * Get SVList from equip
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map sendS1F3Check() {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3statecheck", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] equipStatuss = new long[1];
        long[] pPExecNames = new long[1];
        long[] controlStates = new long[1];
        MsgDataHashtable data = null;
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
            data = sendMsg2Equip(s1f3out);
        } catch (Exception e) {
            logger.error("Wait for get meessage directly error：" + e);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            return null;
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        ppExecName = String.valueOf(listtmp.get(1));
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    public Map sendS1F3RcpAndStateCheck() {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3rcpandstate", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] equipStatuss = new long[1];
        long[] pPExecNames = new long[1];
        long[] preEquipStatuss = new long[1];
        MsgDataHashtable data = null;
        try {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
            //有些设备无法获取前一个状态，故先判断是否存在
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PreProcessStatus");
            if (recipeTemplates != null && !recipeTemplates.isEmpty()) {
                preEquipStatuss[0] = Long.parseLong(recipeTemplates.get(0).getDeviceVariableId());
            } else {
                sqlSession.close();
                return null;
            }
            sqlSession.close();
            s1f3out.put("EquipStatus", equipStatuss);
            s1f3out.put("PreProcessStatus", preEquipStatuss);
            s1f3out.put("PPExecName", pPExecNames);
            data = mli.sendPrimaryWsetMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        preEquipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        ppExecName = (String) listtmp.get(2);
        Map resultMap = new HashMap();
        resultMap.put("EquipStatus", equipStatus);
        resultMap.put("PreProcessStatus", preEquipStatus);
        resultMap.put("PPExecName", ppExecName);
        return resultMap;
    }

    public Map sendS1F3SingleCheck(String svidName) {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3singleout", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] svid = new long[1];
        svid[0] = Long.parseLong(svidName);
        s1f3out.put("SVID", svid);
        MsgDataHashtable data = null;
        logger.info("设备" + deviceCode + "开始发送S1F3SingleCheck");
        try {
            data = sendMsg2Equip(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        if (list == null) {
            return null;
        }
        ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        Map resultMap = new HashMap();
        String svValue = String.valueOf(listtmp.get(0));
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Value", svValue);
        logger.info("resultMap=" + resultMap);
        return resultMap;
    }

    //只获取设备ShotCount的SV值，
    public Map sendS1F3SVShotCountCheckout(List<String> svidlist) {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3svshot", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        for (int i = 0; i < svidlist.size(); i++) {
            long[] svid = new long[1];
            // Long.parseLong(svidlist.get(i));
            svid[0] = Long.parseLong(svidlist.get(i));
            s1f3out.put("Data" + i, svid);
        }

        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return null;
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        for (int i = 0; i < listtmp.size(); i++) {
            resultMap.put(i + 1, listtmp.get(i));
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS1F3RcpParaCheckout(List svidlist) {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3" + deviceType + "RcpPara", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        for (int i = 0; i < svidlist.size(); i++) {
            long[] svid = new long[1];
            // Long.parseLong(svidlist.get(i));
            svid[0] = Long.parseLong(svidlist.get(i).toString());
            s1f3out.put("Data" + i, svid);
        }
        MsgDataHashtable data = null;
        try {
            data = sendMsg2Equip(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return null;
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("SVList", listtmp);
        resultMap.put("Description", "Get SVList from equip " + listtmp);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS1F11out() {
        MsgDataHashtable s1f11out = new MsgDataHashtable("s1f11out", mli.getDeviceId());
        s1f11out.setTransactionId(mli.getNextAvailableTransactionId());
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f12");
        resultMap.put("deviceCode", deviceCode);
        List<Map> resultList = new ArrayList();
        try {
            MsgDataHashtable msgdata = mli.sendPrimaryWsetMessage(s1f11out);
            ArrayList<SecsItem> list = (ArrayList) ((SecsItem) msgdata.get("RESULT")).getData();
            if (list != null && !list.isEmpty()) {
                ArrayList<Object> listtmp = CommonSMLUtil.getECSVData(list);
                for (int i = 0; i < listtmp.size(); i++) {
                    List dateItemList = (List) listtmp.get(i);
                    logger.info("SVID ==" + ((long[]) dateItemList.get(0))[0] + "; SVName ==" + (String) dateItemList.get(1) + ";UNITS" + (String) dateItemList.get(2));
                    Map map = new HashMap();
                    map.put("SVID", ((long[]) dateItemList.get(0))[0]);
                    map.put("SVName", (String) dateItemList.get(1));
                    map.put("UNITS", (String) dateItemList.get(2));
                    resultList.add(map);
                }
            }
            resultMap.put("Description", "Get SVList from equip " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("Description", "S1F11获取失败，原因: " + e.getMessage());
        }
        resultMap.put("svDataList", resultList);
        return resultMap;
    }

    /**
     * 获取设备当前状态信息：EquipStatus，PPExecname，ControlState
     *
     * @return
     */
    public Map findDeviceRecipe() {
        if (!this.isJsipReady()) {
            logger.error("JSIP Not Ready");
            logger.error("isInterrupted:[" + isInterrupted() + "]isStartUp:[" + isStartUp() + "]isThreadUsed:[" + isThreadUsed() + "]");
            return null;
        }
        if (FengCeConstant.CONTROL_OFFLINE.equalsIgnoreCase(this.getControlState())) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "设备处于Offline状态...");
            return null;
        }
        Map resultMap = sendS1F3Check();
        updateCommStateInExt();
        return resultMap;
    }

    /**
     * 获取机台Blade
     *
     * @return
     */
    public Map getBladeThreshold() {

        return null;
    }

    public Map findEqptStatus() {
        return this.findDeviceRecipe();

    }

    public String testRUThere() throws HsmsProtocolNotSelectedException, NoConnectionException {
        try {
            MsgDataHashtable s1f1out = new MsgDataHashtable("s1f1out", mli.getDeviceId());
            long transactionId = mli.getNextAvailableTransactionId();
            s1f1out.setTransactionId(transactionId);
            logger.info("transactionId = " + transactionId);
            MsgDataHashtable s1f2in = mli.sendPrimaryWsetMessage(s1f1out);
            if (s1f2in != null) {
                //如果回复取消会话，那么需要重新发送S1F13
                if (s1f2in.getMsgTagName().contains("s1f0")) {
                    logger.info("testRUThere成功,但是未正确回复消息,需要重新建立连接 ");
                    return "1";
                } else {
                    logger.info("testRUThere成功、通信正常 ");
                    return "0";
                }
            } else {
                return "2";
            }
        } catch (HsmsProtocolNotSelectedException e) {
            logger.error("Exception:", e);
            throw new HsmsProtocolNotSelectedException("HsmsProtocolNotSelectedException");
        } catch (NoConnectionException e) {
            logger.error("Exception:", e);
            throw new NoConnectionException("NoConnectionException");
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "2";
        }
    }

    public boolean testInitLink() throws HsmsProtocolNotSelectedException, NoConnectionException {
        try {
            MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out", mli.getDeviceId());
            long transactionId = mli.getNextAvailableTransactionId();
            s1f13out.setTransactionId(transactionId);
            s1f13out.put("Mdln", Mdln);
            s1f13out.put("SoftRev", SoftRev);
            MsgDataHashtable s1f14in = mli.sendPrimaryWsetMessage(s1f13out);
            Mdln = (String) ((SecsItem) s1f14in.get("Mdln")).getData();
            SoftRev = (String) ((SecsItem) s1f14in.get("SoftRev")).getData();
//            setCommState(1);
            logger.info("testInitLink成功 建立连接、通信正常 ");
            return true;
        } catch (HsmsProtocolNotSelectedException e) {
            e.printStackTrace();
            logger.error("Exception:", e);
            throw new HsmsProtocolNotSelectedException("HsmsProtocolNotSelectedException");
        } catch (NoConnectionException e) {
            e.printStackTrace();
            logger.error("Exception:", e);
            throw new NoConnectionException("NoConnectionException");
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map processS1F4in(MsgDataHashtable data) {
        logger.debug("get s1f4 msg from device:" + deviceCode + ".MsgInfo:" + JsonMapper.toJsonString(data));
//        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
//        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
//        resultMap.put("SVList", listtmp);
//        resultMap.put("Description", "Get SVList from equip " + listtmp);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public void processS1F13in(MsgDataHashtable data) {
        try {
            MsgDataHashtable s1f14out = new MsgDataHashtable("s1f14out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s1f14out.put("AckCode", ack);
            if (data.get("SoftRev") != null) {
                SoftRev = (String) ((SecsItem) data.get("SoftRev")).getData();
            }
            if (data.get("Mdln") != null) {
                Mdln = (String) ((SecsItem) data.get("Mdln")).getData();
            }
            logger.info("-----Received s1f13in----. And SoftRev = " + SoftRev + " Mdln = " + Mdln);
            s1f14out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(s1f14out);
            setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F13out() {
        MsgDataHashtable s1f13out = new MsgDataHashtable("s1f13out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f13out.setTransactionId(transactionId);
        s1f13out.put("Mdln", Mdln);
        s1f13out.put("SoftRev", SoftRev);
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s1f13out);
            if (data != null) {
                setCommState(1);
            }
        } catch (Exception e) {
            setCommState(0);
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS1F14in(MsgDataHashtable data) {
        if (data == null) {
            return;
        }
        if (data.get("SoftRev") != null) {
            SoftRev = (String) ((SecsItem) data.get("SoftRev")).getData();
        }
        if (data.get("Mdln") != null) {
            Mdln = (String) ((SecsItem) data.get("Mdln")).getData();
        }
        setCommState(1);
    }

    @SuppressWarnings("unchecked")
    public void sendS1F15out() {
        MsgDataHashtable s1f15out = new MsgDataHashtable("s1f15out", mli.getDeviceId());
        s1f15out.setTransactionId(mli.getNextAvailableTransactionId());
        MsgDataHashtable msgdata = null;
        try {
            msgdata = mli.sendPrimaryWsetMessage(s1f15out);
            long onlack = msgdata.getSingleNumber("AckCode");
            if (onlack == 0 || onlack == 2) {
                setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F17out() {
        MsgDataHashtable s1f17out = new MsgDataHashtable("s1f17out", mli.getDeviceId());
        s1f17out.setTransactionId(mli.getNextAvailableTransactionId());
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s1f17out);
            long onlack = data.getSingleNumber("AckCode");
            if (onlack == 0 || onlack == 2) {
                setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            }
            data = null;
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    public Map sendS2F13ECCheckout(List ecidlist) {
        MsgDataHashtable s2f13out = new MsgDataHashtable("s2f13" + deviceType + "ECCheck", mli.getDeviceId());
        s2f13out.setTransactionId(mli.getNextAvailableTransactionId());
        for (int i = 0; i < ecidlist.size(); i++) {
            long[] svid = new long[1];
            // Long.parseLong(svidlist.get(i));
            svid[0] = Long.parseLong(String.valueOf(ecidlist.get(i)));
            s2f13out.put("Data" + i, svid);
        }
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s2f13out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = new ArrayList<>();
        if (data == null || data.get("RESULT") == null) {
            return null;
        }
        list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ECValueList", listtmp);
        return resultMap;
    }

    public Map sendS2F13ECSingleCheckout(String ecidStr) {
        MsgDataHashtable s2f13out = new MsgDataHashtable("s2f13singleout", mli.getDeviceId());
        s2f13out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] ecid = new long[1];
        ecid[0] = Long.parseLong(ecidStr);
        s2f13out.put("ECID", ecid);
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s2f13out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = new ArrayList<>();
        String ecValue = null;
        if (data != null) {
            list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        }
        if (list != null) {
            ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ecValue = String.valueOf(listtmp.get(0));
        }
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Value", ecValue);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public void sendS2F15out(String ecid, String ecv, MliHsms mli) {
        MsgDataHashtable s2f15out = new MsgDataHashtable("s2f15out", mli.getDeviceId());
        s2f15out.setTransactionId(mli.getNextAvailableTransactionId());
        long tmpL = Long.parseLong(ecid);
        long l[] = new long[1];
        l[0] = tmpL;
        float tmpF = Float.parseFloat(ecv);
        float f[] = new float[1];
        f[0] = tmpF;
        s2f15out.put("ECID", l);
        s2f15out.put("ECV", f);
        MsgDataHashtable msgdata = null;
        try {
            msgdata = mli.sendPrimaryWsetMessage(s2f15out);
            byte[] ack = (byte[]) ((SecsItem) msgdata.get("AckCode")).getData();
            if (ack[0] == 0 || ack[0] == 2) {
                setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F17in(MsgDataHashtable data) {
        MsgDataHashtable s2f18out = new MsgDataHashtable("s2f18out", mli.getDeviceId());
        s2f18out.setTransactionId(data.getTransactionId());
        try {
            DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
            s2f18out.put("Time", df.format(new Date()));
            mli.sendSecondaryOutputMessage(s2f18out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2f33out(long reportId, long svId) {
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33out", mli.getDeviceId());
        s2f33out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = reportId;
        long[] reportid = new long[1];
        reportid[0] = reportId;
        long[] variableID = new long[1];
        variableID[0] = svId;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("VariableID", variableID);
        try {
            mli.sendPrimaryWsetMessage(s2f33out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2f33out(long reportId, long equipStatusID, long pPExecNameID) {
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33eqpstate", mli.getDeviceId());
        s2f33out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = reportId;
        long[] reportid = new long[1];
        reportid[0] = reportId;
        long[] EquipStatusID = new long[1];
        EquipStatusID[0] = equipStatusID;
        long[] PPExecNameID = new long[1];
        PPExecNameID[0] = pPExecNameID;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("EquipStatusID", EquipStatusID);
        s2f33out.put("PPExecNameID", PPExecNameID);
        try {
            mli.sendPrimaryWsetMessage(s2f33out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2f33outDelete(long reportId) {
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33zeroout", mli.getDeviceId());
        s2f33out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = reportId;
        long[] reportid = new long[1];
        reportid[0] = reportId;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        try {
            mli.sendPrimaryWsetMessage(s2f33out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2f33out(long reportId, long equipStatusID, long pPExecNameID, long controlStateID) {
        MsgDataHashtable s2f33out = new MsgDataHashtable("s2f33state", mli.getDeviceId());
        s2f33out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = reportId;
        long[] reportid = new long[1];
        reportid[0] = reportId;
        long[] EquipStatusID = new long[1];
        EquipStatusID[0] = equipStatusID;
        long[] PPExecNameID = new long[1];
        PPExecNameID[0] = pPExecNameID;
        long[] ControlStateID = new long[1];
        ControlStateID[0] = controlStateID;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("EquipStatusID", EquipStatusID);
        s2f33out.put("PPExecNameID", PPExecNameID);
        s2f33out.put("ControlStateID", ControlStateID);
        try {
            mli.sendPrimaryWsetMessage(s2f33out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F34in(MsgDataHashtable data) {
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
    }

    /**
     *
     * @param dataid
     * @param ceid
     * @param rptid
     */
    @SuppressWarnings("unchecked")
    public void sendS2f35out(long dataid, long ceid, long rptid) {
        MsgDataHashtable s2f35out = new MsgDataHashtable("s2f35out", mli.getDeviceId());
        s2f35out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataId = new long[1];
        dataId[0] = dataid;
        long[] eventid = new long[1];
        eventid[0] = ceid;
        long[] reportid = new long[1];
        reportid[0] = rptid;
        s2f35out.put("DataID", dataId);
        s2f35out.put("CollEventID", eventid);
        s2f35out.put("ReportID", reportid);
        try {
            mli.sendPrimaryWsetMessage(s2f35out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2f35outDelete(long dataid, long ceid) {
        MsgDataHashtable s2f35out = new MsgDataHashtable("s2f35zeroout", mli.getDeviceId());
        s2f35out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] dataId = new long[1];
        dataId[0] = dataid;
        long[] eventid = new long[1];
        eventid[0] = ceid;
        s2f35out.put("DataID", dataId);
        s2f35out.put("CollEventID", eventid);

        try {
            mli.sendPrimaryWsetMessage(s2f35out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F36in(MsgDataHashtable data) {
        logger.info("----------Received s2f36in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37out(long ceid) {
        MsgDataHashtable s2f37out = new MsgDataHashtable("s2f37out", mli.getDeviceId());
        s2f37out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] CollEventId = new long[1];
        CollEventId[0] = ceid;
        boolean[] flag = new boolean[1];
        flag[0] = true;
        s2f37out.put("Booleanflag", flag);
        s2f37out.put("CollEventId", CollEventId);
        //s1f13out.put("SoftRev", "9.25.5");
        try {
            mli.sendPrimaryWsetMessage(s2f37out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37outClose(long ceid) {
        MsgDataHashtable s2f37out = new MsgDataHashtable("s2f37out", mli.getDeviceId());
        s2f37out.setTransactionId(mli.getNextAvailableTransactionId());
        long[] CollEventId = new long[1];
        CollEventId[0] = ceid;
        boolean[] flag = new boolean[1];
        flag[0] = false;
        s2f37out.put("Booleanflag", flag);
        s2f37out.put("CollEventId", CollEventId);
        //s1f13out.put("SoftRev", "9.25.5");
        try {
            mli.sendPrimaryWsetMessage(s2f37out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    /**
     * 开启机台所有事件报告
     */
    @SuppressWarnings("unchecked")
    public void sendS2F37outAll() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f37outAll", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = true;
        s2f37outAll.put("Booleanflag", flag);
        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37outCloseAll() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f37outAll", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = false;
        s2f37outAll.put("Booleanflag", flag);
        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public String sendS2F37outMuilt(boolean openFlag, long[] ceidList) {
        MsgDataHashtable s2f37outMuilt = new MsgDataHashtable("s2f37outMuilt", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outMuilt.setTransactionId(transactionId);
        boolean[] flag = new boolean[1];
        flag[0] = openFlag;
        s2f37outMuilt.put("Booleanflag", flag);

        SecsItem vRoot = new SecsItem();
        vRoot.setFormatCode(FormatCode.SECS_LIST);
        ArrayList rootData = new ArrayList();
        for (long ceid : ceidList) {
            long[] u1 = new long[1];
            u1[0] = ceid;
            SecsItem sItem1 = new SecsItem(u1, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER);
            rootData.add(sItem1);
        }
        vRoot.setData(rootData);//very important
        s2f37outMuilt.put("CEIDList", vRoot);

        try {
            MsgDataHashtable s2f38in = mli.sendPrimaryWsetMessage(s2f37outMuilt);
            byte[] ack = (byte[]) ((SecsItem) s2f38in.get("AckCode")).getData();
            return String.valueOf(ack[0]);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F38in(MsgDataHashtable data) {
        logger.info("----------Received s2f38in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }

    /**
     * 根据配置动态发送command到设备
     *
     * @param commandKey
     * @return
     */
    public Map sendCommandByDymanic(String commandKey) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outConfig", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        CommandDomain commandDomain = this.remoteCommandMap.get(commandKey);
        if (commandDomain != null) {
            s2f41out.put("RCMD", commandDomain.getRcmd());
            SecsItem vRoot = new SecsItem();
            vRoot.setFormatCode(FormatCode.SECS_LIST);
            ArrayList rootData = new ArrayList();
            if (commandDomain.getParaList().size() > 0) {
                for (CommandParaPair cpPair : commandDomain.getParaList()) {
                    SecsItem cpItem = new SecsItem();
                    cpItem.setFormatCode(FormatCode.SECS_LIST);
                    ArrayList cpPairNode = new ArrayList(2);
                    SecsItem cpname = new SecsItem(cpPair.getCpname(), FormatCode.SECS_ASCII);
                    cpPairNode.add(cpname);
                    short cpvalType = FormatCode.SECS_ASCII;
                    if (cpPair.getCpval() instanceof Boolean) {
                        cpvalType = FormatCode.SECS_BOOLEAN;
                    } else if (cpPair.getCpval() instanceof Byte) {
                        cpvalType = FormatCode.SECS_BINARY;
                    } else if (cpPair.getCpval() instanceof Integer) {
                        cpvalType = FormatCode.SECS_4BYTE_SIGNED_INTEGER;
                    }
                    SecsItem cpvalue = new SecsItem(cpPair.getCpval(), cpvalType);
                    cpPairNode.add(cpvalue);
                    cpItem.setData(cpPairNode);
                    rootData.add(cpItem);
                }
            }
            vRoot.setData(rootData);//very important
            s2f41out.put("CPLIST", vRoot);
            byte[] hcack = new byte[1];
            try {
                MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f41out);
                hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
                logger.info("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s2f42");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd " + commandKey + " at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            return resultMap;
        } else {
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s2f42");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("HCACK", new byte[]{1});
            resultMap.put("Description", "该设备不支持" + commandKey + "命令");
            return resultMap;
        }
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect(String recipeName) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outPPSelect", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    /*
     * towa 支持的命令有:PP-SELECT STOP ABORT RELEASE LOCK
     * fico 支持的命令有:PP-SELECT STOP START PAUSE RESUME
     * yamada 170t支持的命令有:PP-SELECT STOP START LOCK UNLOCK LOCAL REMOTE
     * DISCO DGP8761 :PP_SELECT START PAUSE RESUME UNLOAD GO_LOCAL GO_REMOTE END_ACK UNLOAD_GP ABORT 
     * DISCO WS DFD6361:START_S  PP_SELECT_S STOP PAUSE_H RESUME_H ABORT
     * DISCO LS DFL7160 DFL7161:START_S  PP_SELECT_S STOP PAUSE_H RESUME_H ABORT
     */
    @SuppressWarnings("unchecked")
    public Map sendS2f41Cmd(String Remotecommand) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41zeroout", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        s2f41out.put("Remotecommand", Remotecommand);
        MsgDataHashtable msgdata = null;
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("prevCmd", Remotecommand);
        byte[] hcack = new byte[1];
        try {
            msgdata = mli.sendPrimaryWsetMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to " + Remotecommand);
            hcack = (byte[]) ((SecsItem) msgdata.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd " + Remotecommand + " at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd " + Remotecommand + " at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S5FX Code">

    /**
     * 打开或者关闭所有报警信息报告
     *
     * @param enable true->open ；false-->close
     */
    public void sendS5F3out(boolean enable) {
        MsgDataHashtable s5f3out = new MsgDataHashtable("s5f3allout", mli.getDeviceId());
        s5f3out.setTransactionId(mli.getNextAvailableTransactionId());
        byte[] aled = new byte[1];
        boolean[] flag = new boolean[1];
        flag[0] = enable;
        if (enable) {
            aled[0] = -128;
        } else {
            aled[0] = 0;
        }
        s5f3out.put("ALED", aled);
        try {
            mli.sendPrimaryWsetMessage(s5f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public boolean replyS5F2Directly(MsgDataHashtable data) {
        MsgDataHashtable s5f2out = new MsgDataHashtable("s5f2out", mli.getDeviceId());
        byte b[] = new byte[1];
        b[0] = 0;
        s5f2out.put("AckCode", b);
        try {
            s5f2out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(s5f2out);
            return true;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public Map processS5F1in(MsgDataHashtable data) {
        long ALID = 0l;
        try {
            ALID = data.getSingleNumber("ALID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ALCD = (byte[]) ((SecsItem) data.get("ALCD")).getData();
        String ALTX = (String) ((SecsItem) data.get("ALTX")).getData().toString();
        logger.info("Recived s5f1 ID:" + ALID + " from " + deviceCode + " with the ALCD=" + ALCD[0] + " means " + ACKDescription.description(ALCD, "ALCD") + ", and the ALTX is: " + ALTX);
//        UiLogUtil.appendLog2SecsTab(deviceCode, "收到报警信息 " + " 报警ID:" + ALID + " 报警详情: " + ALTX);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s5f1");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("deviceId", deviceId);
        resultMap.put("ALID", ALID);
        resultMap.put("ALCD", ALCD[0]);
        resultMap.put("ALTX", ALTX);
        resultMap.put("Description", ACKDescription.description(ALCD, "ALCD"));
        resultMap.put("TransactionId", data.getTransactionId());
        reportAlarm(resultMap);
        return resultMap;
    }

    protected void reportAlarm(Map alarmMap) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        AlarmService alarmService = new AlarmService(sqlSession);
        alarmService.triggerAlarm(alarmMap);
        sqlSession.close();
    }
    // </editor-fold>   
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @SuppressWarnings("unchecked")
    public void processS6F11in(MsgDataHashtable data) {
        long ceid = 0;
        try {
            if (data.get("CollEventId") != null) {
                ceid = data.getSingleNumber("CollEventId");
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
            this.setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS6f11StripMapUpload(MsgDataHashtable data, MliHsms mli) {
        logger.info("----Received from Equip Strip Map Upload event - S6F11");
        try {
            MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;  //granted
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
            logger.info(" ----- s6f12 sended - Strip Upload Completed-----.");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void processS6F11inStripMapUpload(MsgDataHashtable data) {
        logger.info("----Received from Equip Strip Map Upload event - S6F11");
        try {
            //获取xml字符串
            String stripMapData = (String) ((SecsItem) data.get("MapData")).getData();
            String stripId = XmlUtil.getStripIdFromXml(stripMapData);
            UiLogUtil.appendLog2SecsTab(deviceCode, "请求上传Strip Map！StripID:[" + stripId + "]");
            //通过Web Service上传mapping
            MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
            byte[] ack = new byte[1];
//            ack[0] = WSUtility.binSet(stripMapData, deviceCode).getBytes()[0];
            ack[0] = AxisUtility.uploadStripMap(stripMapData, deviceCode).getBytes()[0];
            if (ack[0] == '0') {//上传成功
                ack[0] = 0;
                UiLogUtil.appendLog2SeverTab(deviceCode, "上传Strip Map成功！StripID:[" + stripId + "]");
            } else {//上传失败
                ack[0] = 1;
                UiLogUtil.appendLog2SeverTab(deviceCode, "上传Strip Map失败！StripID:[" + stripId + "]");
            }
            out.put("AckCode", ack);
            out.setTimeStamp(new Date());
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
            logger.info(" ----- s6f12 sended - Strip Upload Completed-----.");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatus(MsgDataHashtable data) {
        //回复s6f11消息
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
//        showCollectionsEventInfo(ceid);
    }

    //直接回复S6F12
    protected void replyS6F12WithACK(MsgDataHashtable data, byte[] ackCode) {
        //回复s6f11消息
        MsgDataHashtable out = new MsgDataHashtable("s6f12out", mli.getDeviceId());
        out.put("AckCode", ackCode);
        try {
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    //比对Recipe名称
    protected boolean checkRecipeName(String tragetRecipeName) {
        return tragetRecipeName.equals(ppExecName);
    }

    public void sendTerminalMsg2EqpSingle(String msg) {
        byte[] tid = new byte[1];
        tid[0] = 0;
        sendS10F3(tid, msg);
    }

//    public void sendTerminalMsg2EqpAdditional(String msg) {
//        byte[] tid = new byte[1];
//        tid[0] = 1;
//        sendS10F1(tid, msg);
//    }
    protected void sendS10F1(byte[] tid, String text) {
        MsgDataHashtable out = new MsgDataHashtable("s10f1out", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
        try {
            out.put("TID", tid);
            out.put("TEXT", text);
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS10F1in(MsgDataHashtable data) {
        String text = "";
        try {
            if (data.get("TEXT") != null) {
                text = ((SecsItem) data.get("TEXT")).getData().toString();
                logger.info("Received a s10f1in with text = " + text);
                UiLogUtil.appendLog2SecsTab(deviceCode, "收到设备发送的消息:[" + text + "]");
            }
            MsgDataHashtable out = new MsgDataHashtable("s10f2out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            out.put("AckCode", ack);
            out.setTransactionId(data.getTransactionId());
            mli.sendSecondaryOutputMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void sendS10F3(byte[] tid, String text) {
        MsgDataHashtable out = new MsgDataHashtable("s10f3out", mli.getDeviceId());
        out.setTransactionId(mli.getNextAvailableTransactionId());
        try {
            if (text.length() > 300) {
                logger.info("准备向设备发送的消息为:" + text + ",长度超过限制，将被截取");
                text = text.substring(0, 300) + "...";
                logger.info("实际向设备发送的消息为:" + text);
            }
            out.put("TID", tid);
            out.put("TEXT", text);
            mli.sendPrimaryWsetMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatusChange(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        changeEquipPanel(map);

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
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
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
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，请改机!");
                    return;
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
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序一致，核对通过！");
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
                                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在[" + ppExecName + "]的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, "[" + ppExecName + "]开始WI参数Check");
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

    @SuppressWarnings("unchecked")
    public void processS6F12in(MsgDataHashtable data) {
        logger.info("----------Received s6f12in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    @SuppressWarnings("unchecked")
    public void processS7F1in(MsgDataHashtable data) {
        MsgDataHashtable s7f2out = new MsgDataHashtable("s7f2out", mli.getDeviceId());
        byte[] ack = new byte[1];
        //目前不允许从机台直接上传recipe
        ack[0] = 5;
        s7f2out.put("PPGNT", ack);
        s7f2out.setTransactionId(data.getTransactionId());
        try {
            mli.sendSecondaryOutputMessage(s7f2out);
            logger.error("Received s7f1 from equip " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param targetRecipeName
     * @return
     */
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        if (length[0] == 0) {
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
            return resultMap;
        }
        MsgDataHashtable s7f1out = new MsgDataHashtable("s7f1out", mli.getDeviceId());
        s7f1out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName);
        s7f1out.put("Length", length);

        MsgDataHashtable data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = mli.sendPrimaryWsetMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
            resultMap.put("ppgnt", ppgnt[0]);
            resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    /**
     * 下载Recipe，将原有的recipe使用指定的PPID下载到机台
     *
     * @param localRecipeFilePath
     * @param targetRecipeName
     * @return
     */
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        MsgDataHashtable data = null;
        MsgDataHashtable s7f3out = new MsgDataHashtable("s7f3out", mli.getDeviceId());
        s7f3out.setTransactionId(mli.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName.replace("@", "/"));
        s7f3out.put("Processprogram", secsItem);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        byte[] ackc7 = new byte[1];
        try {
            data = mli.sendPrimaryWsetMessage(s7f3out);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            resultMap.put("ACKC7", ackc7[0]);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public void processS7F3in(MsgDataHashtable data) {
//不允许从机台直接上传Recipe此段已注释，勿删！        
//        String ppid = (String) ((SecsItem) data.get("ProcessprogramID")).getData();
//        Object ppbody = new Object();
//        if (rcptype == 1) {
//            ppbody = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
//        } else {
//            ppbody = (String) ((SecsItem) data.get("Processprogram")).getData();
//        }
//        TransferUtil.setPPBody(ppid, ppbody, rcptype, rcpPath + ppid + ".txt");

        MsgDataHashtable s7f4out = new MsgDataHashtable("s7f4out", mli.getDeviceId());
        s7f4out.setTransactionId(data.getTransactionId());
        byte[] ack = new byte[1];
        //目前不允许从机台直接上传recipe
        ack[0] = 1;
        s7f4out.put("AckCode", ack);
        try {
            mli.sendSecondaryOutputMessage(s7f4out);
        } catch (Exception e) {
        }
    }

    @SuppressWarnings("unchecked")
    public void processS7F5in(MsgDataHashtable data) {
        try {
//            MsgDataHashtable s7f6out = new MsgDataHashtable("s7f6out", mli.getDeviceId());
            String ppid = (String) ((SecsItem) data.get("ProcessprogramID")).getData();
            logger.info("This equipment" + deviceCode + " is requesting to download recipe :" + ppid);
//暂时不用的功能，代码勿删            
//            String ppbody = (String) TransferUtil.getPPBody(0, recipePath + ppid).get(0);
//            if (ppbody != null) {
//                byte[] ack = new byte[1];
//                ack[0] = 0;
//                s7f6out.put("AckCode", ack);
//                s7f6out.setTimeStamp(new Date());
//            s7f6out.setTransactionId(data.getTransactionId());
//            mli.sendSecondaryOutputMessage(s7f6out);
//                sendS7F6out(ppid, recipePath + ppid, mli);
//            } else {
//               logger.error("The recipe named" + ppid + "is not exist");
//            }
            MsgDataHashtable s7f6out = new MsgDataHashtable("s7f6zeroout", mli.getDeviceId());
            s7f6out.setTransactionId(mli.getNextAvailableTransactionId());
            mli.sendPrimaryWsetMessage(s7f6out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public abstract Map sendS7F5out(String recipeName);

    @SuppressWarnings("unchecked")
    public Map sendS7F17out(String recipeName) {
        MsgDataHashtable s7f17out = new MsgDataHashtable("s7f17out", mli.getDeviceId());
        s7f17out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s7f17out);
            logger.info("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            if (ackc7[0] == 0) {
                logger.info("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
            resultMap.put("ACKC7", ackc7[0]);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        MsgDataHashtable s7f19out = new MsgDataHashtable("s7f19out", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s7f19out.setTransactionId(transactionId);
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s7f19out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("EPPD") == null) {
            data = this.getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("EPPD")).getData();
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            logger.info("recipeNameList:" + listtmp);
            resultMap.put("eppd", listtmp);
        }
        return resultMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S14FX Code">
    @SuppressWarnings("unchecked")
    protected void processS14F1in(MsgDataHashtable data) {
        if (data == null) {
            return;
        }
        String objType = null;
        if (data.get("ObjectType") != null) {
            objType = (String) ((SecsItem) data.get("ObjectType")).getData();
        }
        String stripId = "";
        if (data.get("StripId") != null) {
            stripId = (String) ((SecsItem) data.get("StripId")).getData();
        }
        UiLogUtil.appendLog2SecsTab(deviceCode, "设备请求下载Strip Map，StripId：[" + stripId + "]");
        MsgDataHashtable out = null;
        //通过Web Service获得xml字符串
//        String stripMapData = WSUtility.binGet(stripId, deviceCode);
        String stripMapData = AxisUtility.downloadStripMap(stripId, deviceCode);
//      String stripMapData ="<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                "<MapData xmlns=\"urn:semi-org:xsd.E142-1.V1005.SubstrateMap\">\n" +
//                "    <SubstrateMaps>\n" +
//                "        <SubstrateMap LayoutSpecifier=\"StripLayout/Device\"\n" +
//                "            SubstrateId=\"W1234567890-99-999\" SubstrateType=\"Strip\">\n" +
//                "            <Overlay MapName=\"DownloadBinCodeMap\">\n" +
//                "                <BinCodeMap BinType=\"HexaDecimal\" NullBin=\"FF\">\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                    <BinCode>008100000000000000000000000000000081000000000000000000000000000000000000000000</BinCode>\n" +
//                "                </BinCodeMap>\n" +
//                "            </Overlay>\n" +
//                "        </SubstrateMap>\n" +
//                "    </SubstrateMaps>\n" +
//                "</MapData>"; 
        if (stripMapData == null) {//stripId不存在  
            out = new MsgDataHashtable("s14f2outNoExist", mli.getDeviceId());
            long[] u1 = new long[1];
            u1[0] = 0;
            out.put("ObjectAck", u1);
            UiLogUtil.appendLog2SeverTab(deviceCode, "StripId：[" + stripId + "] Strip Map 不存在！");
        } else {//stripId存在
            String downLoadResult = stripMapData.substring(0, 1);
            if ("<".equals(downLoadResult)) {
                out = new MsgDataHashtable("s14f2out", mli.getDeviceId());
                out.put("StripId", stripId);
                out.put("MapData", stripMapData);
                UiLogUtil.appendLog2SeverTab(deviceCode, "从服务器下载Strip Map成功,StripId：[" + stripId + "]");
            } else {
                //是分号
                long[] errorCodes = new long[1];
                try {
                    errorCodes[0] = Long.valueOf(stripMapData.split(";")[0]);
                } catch (Exception e) {
                    errorCodes[0] = 10L;
                }
                out = new MsgDataHashtable("s14f2outException", mli.getDeviceId());
                out.put("StripId", stripId);
                out.put("MapData", stripMapData);
                out.put("ErrCode", errorCodes);
                out.put("ErrText", stripMapData);
                UiLogUtil.appendLog2SeverTab(deviceCode, "从服务器下载Strip Map失败,StripId：[" + stripId + "],失败原因：" + stripMapData);
            }
            out.setTransactionId(data.getTransactionId());
        }
        try {
            mli.sendSecondaryOutputMessage(out);
            UiLogUtil.appendLog2SeverTab(deviceCode, "发送Strip Map到设备,StripId：[" + stripId + "]");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="StartStopCOMM Code">
    //开启连接
    /**
     *
     */
    public void startComm() {
        EquipNodeBean equipNodeBean = null;
        for (EquipNodeBean enb : GlobalConstants.stage.equipBeans) {
            if (enb.getDeviceIdProperty() == this.deviceId) {
                equipNodeBean = enb;
            }
        }
        DeviceComm.startHost(equipNodeBean);
    }

    //关闭连接
    public void stopComm() {
        EquipNodeBean equipNodeBean = null;
        for (EquipNodeBean enb : GlobalConstants.stage.equipBeans) {
            if (enb.getDeviceIdProperty() == this.deviceId) {
                equipNodeBean = enb;
            }
        }
        DeviceComm.restartHost(equipNodeBean);
    }

    // </editor-fold>    
    // <editor-fold defaultstate="collapsed" desc="StartStopSECS Code">
    /**
     * 关闭SECS通信
     */
    protected void terminateSecs() {
        mli.getProtocol().setShutdown(true);
        mli.terminateMli();
    }

    /**
     * 开启SECS通信线程
     *
     * @param commFailAdapter
     * @param errAdapter
     * @param feedUpAdapter
     * @throws NotInitializedException
     */
    public void startSecs(HsmsCommunicationFailureDealer commFailAdapter,
            ErrorDataMessageDealer errAdapter,
            UpLevelAnouncer feedUpAdapter)
            throws NotInitializedException {
        if (this.mli == null) {
            throw new NotInitializedException("Host with device id = " + this.deviceId
                    + " Equip Id = " + this.equipId + " is not initialized yet.");
        }
        logger.info("SECS Protocol for " + this.getEquipId() + " is being started.");
        this.mli.connectAndStartForActiveMode(commFailAdapter, errAdapter, feedUpAdapter);
        //if hsms, then it can be MliHsms instance. //this will make the MSP hsms specific.
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="WaitMsgValueMap Code">
    /**
     *
     * @param transactionId
     * @return
     */
    public MsgDataHashtable getMsgDataFromWaitMsgValueMapByTransactionId(long transactionId) {
        int i = 0;
        logger.info("Can not get value directly,will try to get value from message queue");
        while (i < 5) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                logger.error("Exception occur，Exception info：从WaitMsgValueMap中获取消息时，线程中断。设备编号：" + deviceCode);
                ex.printStackTrace();
            }
            MsgDataHashtable msgDataHashtable = this.waitMsgValueMap.get(transactionId);
            logger.info("try===>" + i);
            if (msgDataHashtable != null) {
                logger.info("Had get value from message queue ===>" + JSONArray.toJSON(msgDataHashtable));
                return msgDataHashtable;
            }
            i++;
        }
        if (i >= 5) {
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

    protected void putDataIntoWaitMsgValueMap(MsgDataHashtable dataHashtable) {
        if (waitMsgValueMap.entrySet().size() > 1000) {
            waitMsgValueMap.clear();
        }
        long transActionId = dataHashtable.getTransactionId();
        waitMsgValueMap.put(transActionId, dataHashtable);
    }

    // </editor-fold> 
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Equipment Data Collection ">
    // <editor-fold defaultstate="collapsed" desc="About MOLD Press Code">
    /**
     * 调用webservice，获取PressCheck锁机标志
     *
     * @param deviceCode
     * @return
     */
    public boolean checkPressFlagFromServerByWS(String deviceCode) {
        boolean pass = false;
        String preseUseState = getPressUseState();
        UiLogUtil.appendLog2EventTab(deviceCode, "Press使用状态为[" + preseUseState + "]");
        String curLockFlag = AxisUtility.getPressCheckFlag(deviceCode, preseUseState);
        if (curLockFlag != null && "Y".equals(curLockFlag)) {
            pass = true;
        }
        return pass;
    }

    /**
     * 获取Press的使用状态
     *
     * @return
     */
    public String getPressUseState() {
        String pressUse = "";
        if (pressUseMap.containsKey(1)) {
            if (pressUseMap.get(1)) {
                pressUse = pressUse + "1,";
            }
        }
        if (pressUseMap.containsKey(2)) {
            if (pressUseMap.get(2)) {
                pressUse = pressUse + "2,";
            }
        }
        if (pressUseMap.containsKey(3)) {
            if (pressUseMap.get(3)) {
                pressUse = pressUse + "3,";
            }
        }
        if (pressUseMap.containsKey(4)) {
            if (pressUseMap.get(4)) {
                pressUse = pressUse + "4,";
            }
        }
        if (pressUse.length() > 0) {
            pressUse = pressUse.substring(0, pressUse.length() - 1);
        }
        return pressUse;
    }

    /**
     * 检查Press的使用状态，与领用不符将锁机
     *
     * @param deviceService
     * @param sqlSession
     * @return
     */
    public boolean checkPressUseState(DeviceService deviceService, SqlSession sqlSession) {
        boolean pass = false;
        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行设备Press使用情况检查");
        if (!this.checkPressFlagFromServerByWS(deviceCode)) {
            String pressState = getPressUseState();
            UiLogUtil.appendLog2EventTab(deviceCode, "检测到设备Press[" + pressState + "]使用错误，设备将被锁!");
            this.holdDeviceByServer("PRESS_ERROR_LOCK");
            String dateStr = GlobalConstants.dateFormat.format(new Date());
            this.sendTerminalMsg2EqpSingle("[" + dateStr + "] PressUse Error, machine locked.");
        } else {
            List<DeviceInfoLock> deviceInfoLocks = deviceService.searchDeviceInfoLockByMap(deviceCode, "PRESS_ERROR_LOCK", "Y");
            if (deviceInfoLocks != null && !deviceInfoLocks.isEmpty()) {
                UiLogUtil.appendLog2EventTab(deviceCode, "检测到设备Press使用正常，设备将解锁!");
                deviceService.updateDeviceInfoLock(deviceCode, "PRESS_ERROR_LOCK", "N");
                sqlSession.commit();
                this.releaseDeviceByServer("PRESS_ERROR_LOCK");
            } else {
                UiLogUtil.appendLog2EventTab(deviceCode, "检测到设备Press使用正常");
            }
            pass = true;
        }
        UiLogUtil.appendLog2EventTab(deviceCode, "设备Press使用情况检查执行结束");
        return pass;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="SpecificData Collection">
    /**
     *
     * @param dataIdMap
     * @return
     */
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map resultMap = new HashMap();
        List svIdList = new ArrayList();
        List ecIdList = new ArrayList();
        List recipeParaCodeList = new ArrayList();
        for (Map.Entry<String, String> entry : dataIdMap.entrySet()) {
            if ("SV".equalsIgnoreCase(entry.getValue())) {
                svIdList.add(entry.getKey());
            }
            if ("EC".equalsIgnoreCase(entry.getValue())) {
                ecIdList.add(entry.getKey());
            }
            if ("RecipePara".equalsIgnoreCase(entry.getValue())) {
                recipeParaCodeList.add(entry.getKey());
            }
        }
        Map svValue = this.getSpecificSVData(svIdList);
        Map ecValue = this.getSpecificECData(ecIdList);
        Map recipeParaValue = this.getSpecificRcpParaData(recipeParaCodeList);
        resultMap = svValue;
        resultMap.putAll(ecValue);
        resultMap.putAll(recipeParaValue);
        return resultMap;
    }

    /**
     * 从设备获取指定SVID的数据
     *
     * @param dataIdList-->svidlist
     * @return
     */
    public Map getSpecificSVData(List dataIdList) {
        Map resultMap = new HashMap();
        List svidList = dataIdList;
        List svValueList = new ArrayList();
        //发送查询SV命令，并取值
        if (svidList.size() > 0) {
            MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3Specific", mli.getDeviceId());
            long transactionId = mli.getNextAvailableTransactionId();
            s1f3out.setTransactionId(transactionId);
            SecsItem vRoot = new SecsItem();
            vRoot.setFormatCode(FormatCode.SECS_LIST);
            ArrayList rootData = new ArrayList();
            for (Object svid : svidList) {
                long[] u1 = new long[1];
                u1[0] = Long.valueOf(String.valueOf(svid));
                SecsItem sItem1 = new SecsItem(u1, svFormat);
                rootData.add(sItem1);
            }
            vRoot.setData(rootData);
            s1f3out.put("DATA", vRoot);
            try {
                MsgDataHashtable data = null;
                data = mli.sendPrimaryWsetMessage(s1f3out);
                if (data == null || data.isEmpty()) {
                    data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
                }
                if (data != null && data.get("RESULT") != null) {
                    ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
                    svValueList = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
                    for (int i = 0; i < svValueList.size(); i++) {
                        resultMap.put(svidList.get(i), String.valueOf(svValueList.get(i)));
                    }
                    logger.info("Get SV value list:[" + JsonMapper.toJsonString(data) + "]");
                }
                if (data == null || data.isEmpty()) {
                    logger.error("Query SV List error[" + JsonMapper.toJsonString(data) + "]");
                    UiLogUtil.appendLog2SecsTab(deviceCode, "Query SV List error！");
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        return resultMap;
    }

    /**
     * 从设备获取指定ECID的数据
     *
     * @param dataIdList-->ecidlist
     * @return
     */
    public Map getSpecificECData(List dataIdList) {
        Map resultMap = new HashMap();
        List ecidList = dataIdList;
        List ecValueList = new ArrayList();
        //发送查询EC命令，并取值
        if (ecidList.size() > 0) {
            MsgDataHashtable s2f13out = new MsgDataHashtable("s2f13Specific", mli.getDeviceId());
            long transactionId = mli.getNextAvailableTransactionId();
            s2f13out.setTransactionId(transactionId);
            SecsItem vRoot = new SecsItem();
            vRoot.setFormatCode(FormatCode.SECS_LIST);
            ArrayList rootData = new ArrayList();
            for (Object ecid : ecidList) {
                long[] u1 = new long[1];
                u1[0] = Long.valueOf(String.valueOf(ecid));
                SecsItem sItem1 = new SecsItem(u1, ecFormat);
                rootData.add(sItem1);
            }
            vRoot.setData(rootData);//very important
            s2f13out.put("DATA", vRoot);
            try {
                MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f13out);
                if (data == null || data.isEmpty()) {
                    data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
                }
                if (data != null && data.get("RESULT") != null) {
                    ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
                    ecValueList = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
                    for (int i = 0; i < ecValueList.size(); i++) {
                        resultMap.put(ecidList.get(i), String.valueOf(ecValueList.get(i)));
                    }
                    logger.info("Get EC value list:[" + JsonMapper.toJsonString(data) + "]");
                }
                if (data == null || data.isEmpty()) {
                    logger.error("Query EC value List error[" + JsonMapper.toJsonString(data) + "]");
                    UiLogUtil.appendLog2SecsTab(deviceCode, "Query EC value List error！");
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        return resultMap;
    }

    /**
     * 从设备获取指定paracode的设定值
     *
     * @param dataIdList
     * @return
     */
    public Map getSpecificRcpParaData(List dataIdList) {
        Map resultMap = new HashMap();
        if (dataIdList == null || dataIdList.size() < 1) {
            return resultMap;
        }
        List<String> paraCodeList = dataIdList;
        Map recipeParaMap = this.sendS7F5out(ppExecName);
        List<RecipePara> recipeParas = new ArrayList<>();
        if (recipeParaMap != null) {
            recipeParas = (List<RecipePara>) recipeParaMap.get("recipeParaList");
            if (recipeParas != null) {
                for (int i = 0; i < paraCodeList.size(); i++) {
                    for (RecipePara recipePara : recipeParas) {
                        if (paraCodeList.get(i).equals(recipePara.getParaCode())) {
                            resultMap.put(paraCodeList.get(i), recipePara.getSetValue());
                        }
                    }
                }
            }
        }
        return resultMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Related Data Reported ">
    /**
     * 发送设备操作日志到服务端
     *
     * @param deviceInfoExt
     * @param deviceOplog
     */
    public void sendDeviceInfoExtAndOplog2Server(DeviceInfoExt deviceInfoExt, DeviceOplog deviceOplog) {
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.EqptStatusChange");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("eventName", "eqpt.EqptStatusChange");
        mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
        mqMap.put("deviceCeid", deviceOplog.getDeviceCeid());
        mqMap.put("eventDesc", deviceOplog.getOpDesc());
        mqMap.put("eventDate", GlobalConstants.dateFormat.format(new Date()));
        mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
    }

    /**
     * 保存操作日志并上报给server
     *
     * @param ceid
     * @param deviceService
     * @param deviceInfoExt
     */
    public void saveOplogAndSend2Server(long ceid, DeviceService deviceService, DeviceInfoExt deviceInfoExt) {
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
        if (deviceOplogList == null || deviceOplogList.isEmpty()) {
            DeviceOplog deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
//            deviceService.saveDeviceOplog(deviceOplog);
            //发送设备状态变化记录到服务端
            if (!GlobalConstants.isLocalMode) {
                this.sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                logger.info("发送设备" + deviceCode + "实时状态至服务端");
            }
//            UiLogUtil.appendLog2SeverTab(deviceCode, "发送实时状态至服务端");
        } else {
            String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
            if (!formerDeviceStatus.equals(equipStatus)) {
                DeviceOplog deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
                // deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态到服务端
                if (!GlobalConstants.isLocalMode) {
                    this.sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                    logger.info("发送设备" + deviceCode + "实时状态至服务端");
                }
//                UiLogUtil.appendLog2SeverTab(deviceCode, "发送实时状态至服务端");
            }
        }
    }

    /**
     * uph数据上报
     */
    public void sendUphData2Server() {
    }

    public String getOutputData() {
        return null;
    }

    // </editor-fold> 
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Equipment Control Logic">
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand Code">
    //hold机台
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map map = this.sendS2f41Cmd("STOP");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
                sendStatus2Server("LOCK");
                holdFlag = true;
            }
            return map;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    //Lock设备，主要针对towa，需覆写
    public Map lockDevice() {
        return null;
    }
    //Release设备，主要针对towa，需覆写

    public Map unlockDevice() {
        return null;
    }

    /**
     * hold设备，并且显示具体的hold设备具体信息
     *
     * @return
     */
    public boolean holdDeviceAndShowDetailInfo() {
        Map resultMap = new HashMap();
        resultMap = holdDevice();

        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "当前设备已经被锁机");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备将稍后执行锁机");
                return true;
            } else {
                UiLogUtil.appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + String.valueOf(resultMap.get("Description")));
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean holdDeviceAndShowDetailInfo(String type) {
        Map resultMap = new HashMap();
        boolean hold;
        if ("QA_LOCK".equals(type) && deviceType.contains("TOWA")) {
            resultMap = lockDevice();
        } else {
            resultMap = holdDevice();
        }
        String holdDesc = "";
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                holdDesc = "当前设备已经被锁机";
                UiLogUtil.appendLog2EventTab(deviceCode, holdDesc);
                String dateStr = GlobalConstants.dateFormat.format(new Date());
                this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + type);
                hold = true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                holdDesc = "设备将稍后执行锁机";
                UiLogUtil.appendLog2EventTab(deviceCode, holdDesc);
                String dateStr = GlobalConstants.dateFormat.format(new Date());
                this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + type);
                hold = true;
            } else {
                Map eqptStateMap = this.findEqptStatus();
                holdDesc = "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState"));
                UiLogUtil.appendLog2SecsTab(deviceCode, "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                hold = false;
            }
        } else {
            hold = false;
            holdDesc = "无法锁机,设备状态异常或未开启锁机";
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "HostHoldResult");
        mqMap.put("holdReason", type);
        mqMap.put("deviceCode", deviceCode);
        if (hold) {
            mqMap.put("holdResult", "锁机成功");
            mqMap.put("holdDesc", holdDesc);
        } else {
            mqMap.put("holdResult", "锁机失败");
            mqMap.put("holdDesc", holdDesc);
        }
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        return hold;
    }

    /**
     * release设备，并且显示具体的release设备具体信息
     *
     * @return
     */
    public boolean releaseDeviceAndShowDetailInfo() {
        Map resultMap = new HashMap();
        resultMap = releaseDevice();

        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "当前设备已经被解锁");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备将稍后执行解锁");
                return true;
            } else {
                UiLogUtil.appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "解锁失败，当前机台状态无法进行解锁，机台状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * hold设备，并且显示具体的hold设备具体信息
     *
     * @return
     */
    public boolean releaseDeviceAndShowDetailInfo(String type) {
        Map resultMap = new HashMap();
        resultMap = releaseDevice();
        if ("QA_LOCK".equals(type) && deviceType.contains("TOWA")) {
            resultMap = unlockDevice();
        } else {
            resultMap = releaseDevice();
        }
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "当前设备已经被解锁");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备将稍后执行解锁");
                return true;
            } else {
                UiLogUtil.appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "解锁失败，当前机台状态无法进行解锁，机台状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
                return false;
            }
        } else {
            return false;
        }
    }

    public String startDeviceAndShowDetailInfo() {
        Map resultMap = new HashMap();
        resultMap = startDevice();
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备开机作业！");
                return "Y";
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备将稍后执行开机任务！");
                return "Y";
            } else {
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "开机作业失败，当前机台状态无法进行开机，机台状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
                String failReason = String.valueOf(resultMap.get("CheckResult"));
                logger.debug("Equip start failed ,the reason of fail start:" + failReason);
                return failReason;
            }
        } else {
            return "N";
        }
    }

    public boolean changeEqptControlStateAndShowDetailInfo(String controlState) {
        Map resultMap = new HashMap();
        resultMap = sendS2f41Cmd(controlState);
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                UiLogUtil.appendLog2EventTab(deviceCode, "设备控制状态切换被HOST命令切换到 " + controlState + "！");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                UiLogUtil.appendLog2EventTab(deviceCode, "设备将稍后执行控制状态切换任务！");
                return true;
            } else {
                UiLogUtil.appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换失败，当前状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
                return false;
            }
        } else {
            return false;
        }
    }

    /*
     * 设备运行时需要先stop 再lock
     */
    public Map stopDevice() {
        return this.sendS2f41Cmd("STOP");
    }

    public Map startDevice() {
        return this.sendS2f41Cmd("START");
    }

    //释放机台
    public Map releaseDevice() {
        Map map = this.sendS2f41Cmd("RELEASE");
        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
            this.setAlarmState(0);
        }
        return map;
    }

    /**
     * 初始化公用命令，子类调用
     *
     * @return
     */
    public boolean initCommonRemoteCommand() {
        String commandKey = "start";
        CommandDomain startCommand = new CommandDomain();
        startCommand.setRcmd("START");
        this.remoteCommandMap.put(commandKey, startCommand);

        commandKey = "stop";
        CommandDomain stopCommand = new CommandDomain();
        stopCommand.setRcmd("STOP");
        this.remoteCommandMap.put(commandKey, stopCommand);

        commandKey = "pasuse";
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
        return true;
    }

    /**
     * 通用的发送remoteCommand的方法
     *
     * @param commandMsg
     * @return
     */
    public String sendRemoteCommand(MsgDataHashtable commandMsg) {
        commandMsg.setTransactionId(mli.getNextAvailableTransactionId());
        byte[] hcack = new byte[1];
        try {
            MsgDataHashtable feedBackData = mli.sendPrimaryWsetMessage(commandMsg);
            hcack = (byte[]) ((SecsItem) feedBackData.get("HCACK")).getData();
            return String.valueOf(hcack);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
    }

    public abstract void initRemoteCommand();

    // </editor-fold> 
    // </editor-fold> 
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="External Controlling Logic">
    // <editor-fold defaultstate="collapsed" desc="Server Check Flag">
    /**
     * 调用webservice，获取锁机标志
     *
     * @param deviceCode
     * @return
     */
    public boolean checkLockFlagFromServerByWS(String deviceCode) {
        boolean lockFlag = false;
//        String curLockFlag = AxisUtility.getLockFlag("SysAuto", deviceCode);
//        if (curLockFlag != null && curLockFlag.equals("Y")) {
//            lockFlag = true;
//        }
        Map checkServerLockResult = AxisUtility.getLockFlagAndRemarks("SysAuto", deviceCode);
        String lockFlagStr = String.valueOf(checkServerLockResult.get("lockFlag"));
        if ("Y".equals(lockFlagStr)) {
            lockFlag = true;
            String lockReason = String.valueOf(checkServerLockResult.get("remarks"));
            UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机, 锁机原因为: " + lockReason);
            holdDeviceAndShowDetailInfo("Equipment locked because of " + lockReason);
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            deviceInfoExt.setLockFlag("Y");
            deviceInfoExt.setRemarks(lockReason);
            deviceService.modifyDeviceInfoExt(deviceInfoExt);
            sqlSession.commit();
            sqlSession.close();
        }
        return lockFlag;
    }

    public boolean holdDeviceByServer(String holdType) {
        boolean holdResult = AxisUtility.holdEqptByServer("System", deviceCode, holdType);
        return holdResult;
    }

    public boolean releaseDeviceByServer(String holdType) {
        boolean releaseResult = AxisUtility.releaseEqptByServer("System", deviceCode, holdType);
        return releaseResult;
    }

    // </editor-fold> 
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Update Equipment UI">
    /**
     * 更新设备作业信息显示
     *
     * @param resultMap
     */
    public void changeEquipPanel(Map resultMap) {
        //ArrayList<EquipNodeBean> equipBeans = GlobalConstants.stage.equipBeans;
        for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
            if (equipNodeBean.getDeviceIdProperty().equals(this.deviceId)) {
                EquipPanel oldPanel = (EquipPanel) equipNodeBean.getEquipPanelProperty();
                EquipPanel newPanel = (EquipPanel) equipNodeBean.getEquipPanelProperty().clone();
                if (resultMap.get("PPExecName") != null) {
                    newPanel.setRunningRcp(resultMap.get("PPExecName").toString());
                }
                if (resultMap.get("EquipStatus") != null) {
                    newPanel.setRunState(resultMap.get("EquipStatus").toString());
                }
                if (resultMap.get("AlarmState") != null) {
                    if (oldPanel.getAlarmState() == 2 && resultMap.get("AlarmState").equals(1)) { //报警变红就不能变黄了，除非已消警
                        newPanel.setAlarmState(oldPanel.getAlarmState());
                    } else {
                        newPanel.setAlarmState(Integer.parseInt(resultMap.get("AlarmState").toString()));
                    }
                }
                if (resultMap.get("ControlState") != null) {
                    newPanel.setControlState(resultMap.get("ControlState").toString());
                }
                if (resultMap.get("WorkLot") != null) {
                    newPanel.setWorkLot(resultMap.get("WorkLot").toString());
                }
                if (resultMap.get("NetState") != null) {
                    newPanel.setNetState(Integer.parseInt(resultMap.get("NetState").toString()));
                }
                if (resultMap.get("CommState") != null) {
                    newPanel.setNetState(Integer.parseInt(resultMap.get("CommState").toString()));
                    EquipState equipState = equipNodeBean.getEquipStateProperty();
                    equipState.setCommOn(true);
                    equipNodeBean.setEquipStateProperty(equipState);
                }
                equipNodeBean.setEquipPanelProperty(newPanel);
                break;
            }
        }
    }

    public void updateLotId() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null) {
            if (deviceInfoExt.getLotId() != null && !"".equals(deviceInfoExt.getLotId())) {
                lotId = deviceInfoExt.getLotId();
                //更新lotId到界面
                Map paraMap = new HashMap();
                paraMap.put("WorkLot", lotId);
                changeEquipPanel(paraMap);
            }
        }
    }

    public void updateCommStateInExt() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            if (deviceInfoExt != null && !controlState.equals(deviceInfoExt.getConnectionStatus())) {
                deviceInfoExt.setConnectionStatus(controlState);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    /**
     * 展示设备CEID的信息
     *
     * @param ceid
     */
    protected void showCollectionsEventInfo(long ceid) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "CEID", String.valueOf(ceid));
        sqlSession.close();
        if (recipeTemplates != null && recipeTemplates.size() > 0) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "CEID[" + ceid + "],事件描述：" + recipeTemplates.get(0).getParaDesc());
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="About Recipe">
    /**
     * 读取路径下的recipe配置文件，获取recipe相关的子文件信息
     *
     * @param localFilePath
     * @param targetRecipeName
     * @return
     */
    public Map getRelativeFileInfo(String localFilePath, String targetRecipeName) {
        return null;
    }

    /**
     * 提供一个自动上传的方法，可在设备类加载中调用，方便一次性上传全部recipe
     */
    public void upLoadAllRcp() {
        ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
        for (int i = 0; i < eppd.size(); i++) {
            String recipeName = eppd.get(i).toString();
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            Map recipeMap = sendS7F5out(recipeName);
            Recipe recipe = (Recipe) recipeMap.get("recipe");
            List<RecipePara> recipeParaList = (List<RecipePara>) recipeMap.get("recipeParaList");
            //保存数据
            try {
                RecipeNameMapping recipeNameMapping = (RecipeNameMapping) recipeMap.get("recipeNameMapping");
                //保存数据
                if (recipeNameMapping != null) {
                    recipeService.saveUpLoadRcpInfo(recipe, recipeParaList, deviceCode, recipeNameMapping);
                } else {
                    recipeService.saveUpLoadRcpInfo(recipe, recipeParaList, deviceCode);
                }
                UiLogUtil.appendLog2EventTab(deviceCode, "上传成功！共 " + eppd.size() + " 第 " + i + " 已完成");
                sqlSession.commit();
            } catch (Exception e) {
                sqlSession.rollback();
                logger.error("Exception:", e);
                UiLogUtil.appendLog2EventTab(deviceCode, "上传失败！请重试！");
            } finally {
                sqlSession.close();
            }
        }
    }

    /**
     * 根据recipe信息拼凑出attach数据
     *
     * @param recipe
     * @return
     */
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        List<Attach> attachs = new ArrayList<>();
        Attach attach = new Attach();
        attach.setId(UUID.randomUUID().toString());
        attach.setRecipeRowId(recipe.getId());
        attach.setAttachName(recipe.getRecipeName().replaceAll("/", "@") + "_V" + recipe.getVersionNo());
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        attach.setAttachPath(recipeService.organizeUploadRecipePath(recipe));
        sqlSession.close();
        attach.setAttachType("txt");
        attach.setSortNo(0);
        if (GlobalConstants.sysUser != null) {
            attach.setCreateBy(GlobalConstants.sysUser.getId());
            attach.setUpdateBy(GlobalConstants.sysUser.getId());
        } else {
            attach.setCreateBy("System");
            attach.setUpdateBy("System");
        }
        attachs.add(attach);
        return attachs;
    }

    /**
     * 根据recipe信息拼凑其存储路径
     *
     * @param recipe
     * @return
     */
    public String getRecipePathByConfig(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        String recipePathByConfig = GlobalConstants.localRecipePath + recipeService.organizeUploadRecipePath(recipe) + recipe.getRecipeName().replace("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt";
        if (recipePathByConfig.contains("*")) {
            recipePathByConfig = recipePathByConfig.replace("*", "X");
        }
        sqlSession.close();
        return recipePathByConfig;
    }

    /**
     * getRecipeParasByECSV 系列代码修改后暂未测试
     *
     * @return
     */
    // <editor-fold defaultstate="collapsed" desc="getRecipeParasByECSV ">
    protected List<RecipePara> getRecipeParasByECSV() {
        List<RecipePara> recipeParas = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> ECtemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "ECRecipePara");
        List<RecipeTemplate> SVtemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "SVRecipePara");

        recipeParas = transferECSVValue2RecipePara(ECtemplates, SVtemplates);
        sqlSession.close();
        return recipeParas;
    }

    protected List getECSVIdList(List<RecipeTemplate> recipeTemplates) {
        List ecsvIdList = new ArrayList();
        if (recipeTemplates == null || recipeTemplates.size() < 1) {
            return null;
        }
        for (int i = 0; i < recipeTemplates.size(); i++) {
            ecsvIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return ecsvIdList;
    }

    private List<RecipePara> transferECSVValue2RecipePara(List<RecipeTemplate> ECtemplates, List<RecipeTemplate> SVtemplates) {
        List<RecipeTemplate> totaltTemplates = null;
        Map totalValueMap = null;
        Map svvalueMap = null;
        if (SVtemplates != null && SVtemplates.size() > 0) {
            totaltTemplates = SVtemplates;
            svvalueMap = this.getSpecificSVData(getECSVIdList(SVtemplates));
        }
        Map ecvalueMap = null;
        if (ECtemplates != null && ECtemplates.size() > 0) {
            ecvalueMap = this.getSpecificECData(getECSVIdList(ECtemplates));
        }
        if (svvalueMap != null && svvalueMap.size() > 0 && ecvalueMap != null && ecvalueMap.size() > 0) {
            totaltTemplates = SVtemplates;
            totaltTemplates.addAll(ECtemplates);
            totalValueMap = svvalueMap;
            totalValueMap.putAll(ecvalueMap);
        } else if (svvalueMap != null && svvalueMap.size() > 0) {
            totaltTemplates = SVtemplates;
            totalValueMap = svvalueMap;
        } else if (ecvalueMap != null && ecvalueMap.size() > 0) {
            totaltTemplates = ECtemplates;
            totalValueMap = ecvalueMap;
        } else {
            return null;
        }
        List<RecipePara> recipeParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : totaltTemplates) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setSetValue(String.valueOf(totalValueMap.get(recipeTemplate.getDeviceVariableId())));
            recipeParas.add(recipePara);
        }
        return recipeParas;
    }
    // </editor-fold> 
    // </editor-fold> 

    /**
     * 检查功能开关
     *
     * @param functonCode
     * @return
     */
    public boolean checkFunction(String functonCode) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        boolean checkResult = deviceService.checkFunctionSwitch(deviceCode, functonCode);
        sqlSession.close();
        return checkResult;
    }

    public MsgDataHashtable sendMsg2Equip(MsgDataHashtable msgDataHashtable) {
        final MsgDataHashtable dataHashtable = msgDataHashtable;
        MsgDataHashtable result = null;
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Callable<MsgDataHashtable> call = new Callable<MsgDataHashtable>() {

            @Override
            public MsgDataHashtable call() throws Exception {
                //开始执行耗时操作  
                return mli.sendPrimaryWsetMessage(dataHashtable);
            }
        };

        Future<MsgDataHashtable> future = exec.submit(call);
        try {
            result = future.get(GlobalConstants.msgWaitTime, TimeUnit.MILLISECONDS);
            logger.info("Execute future task...");
        } catch (Exception e) {
            future.cancel(true);//取消该Future里关联的Callable任务
            logger.error("Exception occur" + e.getMessage());
        } finally {
            // 关闭线程池  
            exec.shutdown();
        }
        return result;
    }

    public void sendStatus2Server(String deviceStatus) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        try {
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            deviceInfoExt.setDeviceStatus(deviceStatus);
            DeviceOplog deviceOplog = setDeviceOplog(0, ppExecName, deviceStatus, preEquipStatus, lotId);
            deviceOplog.setOpDesc("equip status report" + deviceStatus);
            sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
            logger.info("*************equip status report*********");
            sqlSession.commit();
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            sqlSession.close();
        }
    }

    protected void startSVcheck() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "SVRecipePara", "Y");
        if (recipeTemplates == null || recipeTemplates.isEmpty()) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "该设备未设置参数实时监控,开机前实时值检查取消...");
            return;
        }
        List svIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            svIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        Map resultMap = GlobalConstants.stage.hostManager.getMonitorParaBySV(this.getDeviceId(), svIdList);
        try {
            List<DeviceRealtimePara> deviceRealtimeParas = putSV2DeviceRealtimeParas(recipeTemplates, resultMap);
            if (deviceRealtimeParas != null && !deviceRealtimeParas.isEmpty()) {
                monitorService.saveDeviceRealtimePara(deviceRealtimeParas);
                sqlSession.commit();
            }
//            monitorService.saveDeviceRealtimePara(deviceRealtimeParas);
           // sqlSession.commit();
        } catch (Exception e) {
            sqlSession.close();
        } finally {
            sqlSession.close();
        }

    }

    public List<DeviceRealtimePara> putSV2DeviceRealtimeParas(List<RecipeTemplate> recipeTemplates, Map resultMap) {
        List<DeviceRealtimePara> realTimeParas = new ArrayList<>();

        //modify by luosy @2017.6.27 realtimeParas add reciperowid
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        RecipeService recipeService = new RecipeService(sqlSession);
        //根据ext表中的reciperowid获取recipepara
        Map<String, RecipePara> monitorMap = recipeService.getMonitorParas(recipeService.searchRecipeParaByRcpRowId(deviceInfoExt.getRecipeId()), deviceType);
        String eventDesc = "";
        String eventDescEng = "";
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.StartCheckWI");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("recipeName", ppExecName);
        mqMap.put("EquipStatus", equipStatus);
        mqMap.put("lotId", lotId);
        for (int i = 0; i < recipeTemplates.size(); i++) {
            String realTimeValue = resultMap.get(recipeTemplates.get(i).getDeviceVariableId()).toString();
            DeviceRealtimePara realtimePara = new DeviceRealtimePara();
            if (monitorMap != null && monitorMap.size() > 0) {
                RecipePara recipePara = monitorMap.get(recipeTemplates.get(i).getParaCode());
                if (recipePara != null) {
                    String minValue = recipePara.getMinValue();
                    String maxValue = recipePara.getMaxValue();
                    String setValue = recipePara.getSetValue();
                    if ("1".equals(recipeTemplates.get(i).getSpecType())) {
                        if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        if ((Double.parseDouble(realTimeValue) < Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) > Double.parseDouble(maxValue))) {
                            realtimePara.setRemarks("RealTimeErro");
                            holdFlag = true;
                            UiLogUtil.appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure());
                            eventDesc += "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure() + " ";
                            String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue();

                            eventDescEng = eventDescEng + eventDescEngtmp;
                        }
                        //abs
                    } else if ("2".equals(recipeTemplates.get(i).getSpecType())) {
                        if ("".equals(setValue) || " ".equals(setValue) || "".equals(realTimeValue) || " ".equals(realTimeValue)) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        boolean paraIsNumber = false;
                        try {
                            Double.parseDouble(realTimeValue);
                            paraIsNumber = true;
                        } catch (Exception e) {
                        }
                        if (paraIsNumber) {
                            if (Double.parseDouble(realTimeValue) != Double.parseDouble(setValue)) {
                                realtimePara.setRemarks("RealTimeErro");
                                holdFlag = true;
                                UiLogUtil.appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure());
                                eventDesc += "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure() + " ";
                                String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",Set_value:" + recipePara.getSetValue();
                                eventDescEng = eventDescEng + eventDescEngtmp;
                            }
                        } else {
                            if (!realTimeValue.equals(setValue)) {
                                realtimePara.setRemarks("RealTimeErro");
                                holdFlag = true;
                                UiLogUtil.appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure());
                                eventDesc += "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                        + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                        + "设定值:[" + recipePara.getSetValue() + "]" + recipePara.getParaMeasure() + " ";
                                String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue();

                                eventDescEng = eventDescEng + eventDescEngtmp;
                            }
                        }
                    } else if ("3".equals(recipeTemplates.get(i).getSpecType())) {
                        if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        if ((Double.parseDouble(realTimeValue) <= Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) >= Double.parseDouble(maxValue))) {
                            realtimePara.setRemarks("RealTimeErro");
                            holdFlag = true;
                            UiLogUtil.appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure());
                            eventDesc += "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure() + " ";
                            String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue();

                            eventDescEng = eventDescEng + eventDescEngtmp;
                        }                      
                    } else {
                        if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                            logger.info("Para:Name[" + recipeTemplates.get(i).getParaName() + "],Code[" + recipeTemplates.get(i).getParaCode() + "]has not set range! Pass");
                            continue;
                        }
                        if ((Double.parseDouble(realTimeValue) < Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) > Double.parseDouble(maxValue))) {
                            realtimePara.setRemarks("RealTimeErro");
                            holdFlag = true;
                            UiLogUtil.appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值:[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure());
                            eventDesc += "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
                                    + "参数名:[" + recipePara.getParaName() + "]实时值:[" + realTimeValue + "]" + recipePara.getParaMeasure() + ","
                                    + "设定的范围值:[" + minValue + " - " + maxValue + "]" + recipePara.getParaMeasure() + " ";
                            String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",RealTime_value:" + realTimeValue + ",Set_value:" + recipePara.getSetValue();

                            eventDescEng = eventDescEng + eventDescEngtmp;
                        }
                    }
                    realtimePara.setMaxValue(maxValue);
                    realtimePara.setMinValue(minValue);
                    realtimePara.setSetValue(setValue);
                    realtimePara.setRealtimeValue(realTimeValue);
                    realtimePara.setId(UUID.randomUUID().toString());
                    realtimePara.setDeviceName(deviceCode);
                    realtimePara.setDeviceCode(deviceCode);
                    realtimePara.setParaCode(recipeTemplates.get(i).getParaCode());
                    realtimePara.setParaDesc(recipeTemplates.get(i).getParaDesc());
                    realtimePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
                    realtimePara.setParaName(recipeTemplates.get(i).getParaName());
                    realtimePara.setValueType(recipeTemplates.get(i).getParaType());
                    realtimePara.setUpdateCnt(0);
                    realtimePara.setRecipeRowId(deviceInfoExt.getRecipeId());
                    realTimeParas.add(realtimePara);
                }

            }
        }
        if (holdFlag) {
            eventDesc = "开机前实时参数检查不通过，设备将被锁." + eventDesc;
            UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
            this.holdDeviceAndShowDetailInfo("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng);
            startSVcheckPass = false;
        } else {
            releaseDevice();
            eventDesc = "设备：" + deviceCode + " 开机前实时参数检查通过.";
            UiLogUtil.appendLog2EventTab(deviceCode, "开机前实时参数检查通过.");
            startSVcheckPass = true;
            holdFlag = false;
        }
        mqMap.put("eventDesc", eventDesc);
        GlobalConstants.C2SLogQueue.sendMessage(mqMap);
        sqlSession.close();
        return realTimeParas;
    }

    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // 上传ftp
        FtpUtil.uploadFile(localRcpPath, GlobalConstants.getProperty("ftpPath") + remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
        return true;
    }

    public boolean isIsRestarting() {
        return isRestarting;
    }

    public void setIsRestarting(boolean isRestarting) {
        this.isRestarting = isRestarting;
    }

    public long getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(long startedDate) {
        this.startedDate = startedDate;
    }

    //waferMapping upload
    public Map processS12F1in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f2out = new MsgDataHashtable("s12f2out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f2out.put("SDACK", ack);
            s12f2out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f2out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public void processS12F5in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f6out = new MsgDataHashtable("s12f6out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f6out.put("GRANT1", ack);
            s12f6out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f6out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public Map processS12F7in(MsgDataHashtable msgDataHashtable) {
        try {

            String MaterialID = msgDataHashtable.get("MaterialID").toString();

            byte IDTYP = ((byte[]) ((SecsItem) msgDataHashtable.get("IDTYP")).getData())[0];

            String binList = msgDataHashtable.get("RSINFBinList").toString();

            MsgDataHashtable s12f8out = new MsgDataHashtable("s12f8out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f8out.put("MDACK", ack);
            s12f8out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f8out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public Map processS12F9in(MsgDataHashtable msgDataHashtable) {
        try {
            String MaterialID = msgDataHashtable.get("MaterialID").toString();
            msgDataHashtable.getSingleNumber("IDTYP");
            byte IDTYP = ((byte[]) ((SecsItem) msgDataHashtable.get("IDTYP")).getData())[0];
            int[] STRPxSTRPy = (int[]) ((SecsItem) msgDataHashtable.get("STRPxSTRPy")).getData();
            String binList = msgDataHashtable.get("BinList").toString();
            //TODO  这里获取到的mapping 存储在什么位置
            //local&ftp 路径是什么？
            //db 表结构？几个表？

            MsgDataHashtable s12f10out = new MsgDataHashtable("s12f10out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f10out.put("MDACK", ack);
            s12f10out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f10out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public Map processS12F11in(MsgDataHashtable msgDataHashtable) {
        try {
            String MaterialID = msgDataHashtable.get("MaterialID").toString();

            byte IDTYP = ((byte[]) ((SecsItem) msgDataHashtable.get("IDTYP")).getData())[0];

            String binList = msgDataHashtable.get("XYPOSBinList").toString();

            MsgDataHashtable s12f12out = new MsgDataHashtable("s12f12out", mli.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f12out.put("MDACK", ack);
            s12f12out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f12out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }
    //waferMapping download

    public void processS12F3in(MsgDataHashtable msgDataHashtable) {
        //    S12F3
//    <V>=MaterialID
//    <B [1]>=IDTYP
//    <B [1]>=MapDataFormatType
//    <U2 [1]>=FlatNotchLocation
//    <U2 [1]>=FileFrameRotation
//    <B [1]>=OriginLocation
//    <B [1]>=ProcessAxis
//    <V>=BinCodeEquivalents
//    <V>=NullBinCodeValue
        try {
            MsgDataHashtable s12f4out = new MsgDataHashtable("s12f4out2", mli.getDeviceId());
            ArrayList<SecsItem> list = (ArrayList) ((SecsItem) msgDataHashtable.get("RESULT")).getData();
            ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            String MaterialID = String.valueOf(listtmp.get(0));
            String IDTYP = String.valueOf(listtmp.get(1));
            String MapDataFormatType = String.valueOf(listtmp.get(2));
            String FlatNotchLocation = String.valueOf(listtmp.get(3));
            String FileFrameRotation = String.valueOf(listtmp.get(4));
            String OriginLocation = String.valueOf(listtmp.get(5));
            String ProcessAxis = String.valueOf(listtmp.get(6));
            String BinCodeEquivalents = String.valueOf(listtmp.get(7));
            String NullBinCodeValue = String.valueOf(listtmp.get(8));
//s12f4
//    <V>=MaterialID
//    <B [1]>=IDTYP
//    <U2 [1]>=FlatNotchLocation
//    <B [1]>=OriginLocation
//    <U1 [1]>=RrferencePointSelect
//    <V>=REFPxREFPy
//    <A[MAX 100]>=DieUnitsOfMeasure
//    <V>=XAxisDieSize
//    <V>=YAxisDieSize
//    <U2[1]>=RowCountInDieIncrements
//    <U2[1]>=ColumnCountInDieIncrements
//    <U4[1]>=ProcessDieCount
//    <V>=BinCodeEquivalents  
//    <V>=NullBinCodeValue  
//    <U2 [1]>=MessageLength
// < A "G2WU93-25.TXT" >MaterialID
//    < B 0x0 >IDTYP
//    < U2 90 >FlatNotchLocation
//    < B 0x2 >OriginLocation
//    < U1 0 >RrferencePointSelect
//    < L 
//    > REFPxREFPy
//    < A  >DieUnitsOfMeasure
//    < U4 0 >XAxisDieSize
//    < U4 0 >YAxisDieSize
//    < U2 61 >RowCountInDieIncrements
//     < U2 37 >ColumnCountInDieIncrements
//     < U4 1761 >ProcessDieCount
//     < U1  >BinCodeEquivalents
//     < U1 46 >NullBinCodeValue
//     < U4 35 >MessageLength
            s12f4out.put("MaterialID", MaterialID);
            s12f4out.put("IDTYP", IDTYP);
            s12f4out.put("FlatNotchLocation", FlatNotchLocation);
            s12f4out.put("OriginLocation", OriginLocation);
            s12f4out.put("RrferencePointSelect", 0);
            s12f4out.put("REFPxREFPy", null);
            s12f4out.put("DieUnitsOfMeasure", "");
            s12f4out.put("XAxisDieSize", 0);
            s12f4out.put("YAxisDieSize", 0);
            s12f4out.put("RowCountInDieIncrements", 61);
            s12f4out.put("ColumnCountInDieIncrements", 37);
            s12f4out.put("ProcessDieCount", 1761);

            s12f4out.put("BinCodeEquivalents", null);
            s12f4out.put("NullBinCodeValue", 46);
            s12f4out.put("MessageLength", null);

            s12f4out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f4out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    public Map processS12F13in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f14out = new MsgDataHashtable("s12f14out", mli.getDeviceId());

            s12f14out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f14out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public void processS12F15in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f16ut = new MsgDataHashtable("s12f16out", mli.getDeviceId());
            byte[] IDTYP = ((byte[]) ((SecsItem) msgDataHashtable.get("IDTYP")).getData());
            String MaterialID = ((SecsItem) msgDataHashtable.get("MaterialID")).getData().toString();
            s12f16ut.put("MaterialID", MaterialID);
            s12f16ut.put("IDTYP", IDTYP);
            int[] STRPxSTRPy = (int[]) ((SecsItem) msgDataHashtable.get("STRPxSTRPy")).getData();
            STRPxSTRPy[0] = 0;
            STRPxSTRPy[1] = 0;
            s12f16ut.put("STRPxSTRPy", STRPxSTRPy);
            String BinList = "";
            s12f16ut.put("BinList", MaterialID);

            s12f16ut.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f16ut);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public Map processS12F17in(MsgDataHashtable msgDataHashtable) {
        try {
            MsgDataHashtable s12f18out = new MsgDataHashtable("s12f18out", mli.getDeviceId());

            s12f18out.setTransactionId(msgDataHashtable.getTransactionId());
            mli.sendSecondaryOutputMessage(s12f18out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public Map processS12F19in(MsgDataHashtable msgDataHashtable) {
        try {
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }
}
