package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.generalDriver.api.MsgListener;
import cn.tzauto.generalDriver.api.SecsDriverFactory;
import cn.tzauto.generalDriver.entity.cnct.ConnRegInfo;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.generalDriver.wrapper.ActiveWrapper;
import cn.tzauto.octopus.biz.alarm.service.AlarmService;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoLock;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.*;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.EquipmentEventDealer;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandParaPair;
import cn.tzauto.octopus.secsLayer.exception.NotInitializedException;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.*;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static cn.tzauto.octopus.common.resolver.TransferUtil.getIDValue;


public abstract class EquipHost extends Thread implements MsgListener {

    private static final long serialVersionUID = -8008553978164121001L;
    private static Logger logger = Logger.getLogger(EquipHost.class.getName());
    public static final int COMMUNICATING = 1;
    public static final int NOT_COMMUNICATING = 0;
    public int commState = NOT_COMMUNICATING;
    public String controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
    private int alarmState = 0;
    private boolean sdrReady = false;

    public String iPAddress;
    protected int tCPPort;
    protected String connectMode = "active"; //only "active" or "passive" allowed, default is "active"
    protected String deviceId;
    protected ActiveWrapper activeWrapper;
    protected String description;
    protected boolean startUp;
    protected EquipState equipState;
    protected boolean threadUsed; //Java Thread can be only started once
    protected LinkedBlockingQueue<DataMsgMap> inputMsgQueue;
    //only stores messages which needs to delegate
    protected String deviceType;//设备类型
    protected String manufacturer;//生产厂商
    protected String deviceCode;//设备代码;
    protected int recipeType = 1;
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
    public Map<Long, DataMsgMap> waitMsgValueMap = new ConcurrentHashMap<>();
    protected short svFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    protected short ecFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    protected short ceFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    protected short rptFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    protected short lengthFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    protected long lastStartCheckTime;
    protected long startCheckIntervalTime = 60;
    protected boolean holdFlag = false;
    protected boolean startSVcheckPass = false;
    protected boolean isRestarting = true;
    protected String lastMsgTagName = "";
    protected volatile boolean isInterrupted = false;
    protected long startedDate = 0;//Host启动的时间
    public int restartCnt = 0;
    //wafermapping相关属性
    protected long downFlatNotchLocation;
    protected long upFlatNotchLocation;
    // protected String waferMappingrow = ""; //原始数据的行
//    protected String waferMappingcol = "";//原始数据的列
    protected String waferMappingbins = ""; //转换后的binlist
    protected String uploadWaferMappingRow = "";
    protected String uploadWaferMappingCol = "";
    //延迟删除的标识，判断是否是切换recipe之后,用于DB800,DB730
    public boolean ppselectFlag = false;
    //    EquipSecsBean equipSecsBean;
    protected long StripMapUpCeid;
    protected long EquipStateChangeCeid;
    protected String RCMD_PPSELECT = "PP-SELECT";
    protected String CPN_PPID = "PPID";
    protected String iconPath;

    public EquipHost(String devId, String remoteIpAddress, int remoteTcpPort,
                     String connectMode, String deviceType, String deviceCode) {
        this.deviceId = devId;
        this.iPAddress = remoteIpAddress;
        this.tCPPort = remoteTcpPort;
        this.connectMode = connectMode; //only "active" or "passive" allowed, default is "active"
        this.inputMsgQueue = new LinkedBlockingQueue<>(5000);
        equipState = new EquipState();
        threadUsed = false;
        this.deviceType = deviceType;
        this.deviceCode = deviceCode;
//        equipSecsBean = new EquipSecsBean(deviceCode, deviceType);
        EquipStateChangeCeid = -1;
        StripMapUpCeid = -1;
    }

    public void initialize() {
        logger.info("Initializing SECS Protocol for " + this.deviceId + ".");
//        ConnRegInfo.register(Integer.valueOf(this.deviceId), "active", this.remoteIPAddress, this.remoteTCPPort);
        activeWrapper = (ActiveWrapper) SecsDriverFactory.getSecsDriverByReg(new ConnRegInfo(Integer.valueOf(this.deviceId), "active", this.iPAddress, this.tCPPort));
    }


    public boolean isThreadUsed() {
        return threadUsed;
    }

    @Override
    public abstract Object clone();

    protected void clear() {
        this.connectMode = null;
        this.description = null;
        this.activeWrapper.removeInputMessageListenerToAll(this);
        this.activeWrapper = null;
        this.inputMsgQueue = null;
        commState = NOT_COMMUNICATING;
        controlState = FengCeConstant.CONTROL_OFFLINE;
        sdrReady = false;
    }


    public boolean isSdrReady() {
        return sdrReady;
    }


    public synchronized void setSdrReady(boolean sdrReady) {
        this.sdrReady = sdrReady;
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
            equipState.setNetConnect(true);
            this.sdrReady = true;
//            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
        }
        if (commState == 0) {
            equipState.setCommOn(false);
            this.sdrReady = false;
        }
        this.commState = commState;
        Map resultMap = new HashMap();
        resultMap.put("CommState", commState);
        resultMap.put("NetConnect", commState);
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

    public ActiveWrapper getActiveWrapper() {
        return activeWrapper;
    }

    public boolean isStartUp() {
        return startUp;
    }

    public void setStartUp(boolean startUp) {
        this.startUp = startUp;
    }


    public String getConnectMode() {
        return connectMode;
    }


    public EquipState getEquipState() {
        return equipState;
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


    public String getLotId() {
        return lotId;
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
        List<RecipePara> equipRecipeParas = null;
        try {
            equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        } catch (UploadRecipeErrorException upe) {
            logger.error("Get recipe info from device " + deviceCode + " failed,recipeName= " + checkRecipe.getRecipeName());
        }
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为[" + recipePara.getParaCode() + "],参数名:[" + recipePara.getParaName() + "]其异常设定值为[" + recipePara.getSetValue() + "],默认值为[" + recipePara.getDefValue() + "]"
                            + "其最小设定值为[" + recipePara.getMinValue() + "],其最大设定值为[" + recipePara.getMaxValue() + "]";
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                    checkRecultDesc = checkRecultDesc + eventDesc;
                    String eventDescEngtmp = " Para_Code:" + recipePara.getParaCode() + ",Para_name:" + recipePara.getParaName() + ",Set_value:" + recipePara.getSetValue() + ",MIN_value:" + recipePara.getMinValue() + ",MAX_value:" + recipePara.getMaxValue() + "/r/n";
                    eventDescEng = eventDescEng + eventDescEngtmp;
                }
                this.holdDeviceAndShowDetailInfo("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng);
                //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
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
    public void processS1F1in(DataMsgMap data) {

        try {

            activeWrapper.sendS1F2out(data.getTransactionId());
            setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS1F1out() {
        try {
            DataMsgMap s1f2in = activeWrapper.sendS1F1out();
            if (s1f2in != null && s1f2in.get("MDLN") != null) {
                setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS1F2in(DataMsgMap s1f2in) {
        setCommState(1);
    }

    /**
     * Get SVList from equip
     *
     * @return
     */

    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        if (listtmp != null && !listtmp.isEmpty()) {
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
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

    protected List getNcessaryData() {
        DataMsgMap data = null;
        try {
            List<Long> statusList = new ArrayList<>();

            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            long equipStatussvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
            long pPExecNamesvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
            long controlStatesvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
            statusList.add(equipStatussvid);
            statusList.add(pPExecNamesvid);
            statusList.add(controlStatesvid);
            data = activeWrapper.sendS1F3out(statusList, svFormat);
        } catch (Exception e) {
            logger.error("Wait for get meessage directly error：" + e);
        }
        if (data == null || data.get("SV") == null) {
            return null;
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        return (ArrayList) data.get("SV");

    }

    public Map sendS1F3SingleCheck(String svid) {
        List svidlist = new ArrayList();
        svidlist.add(Long.parseLong(svid));
        DataMsgMap data = null;
        logger.info("设备" + deviceCode + "开始发送S1F3SingleCheck");
        try {
            data = activeWrapper.sendS1F3out(svidlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("SV") == null) {
            return null;
        }
        Object obj = data.get("SV");
        Map resultMap = new HashMap();
        ArrayList listsvValue = new ArrayList();
        if(obj != null){
            if(obj instanceof ArrayList){
                ArrayList listtmp = (ArrayList)obj;
                for(int i=0;i<listtmp.size();i++){
                    String svValue = String.valueOf(listtmp.get(i));
                    listsvValue.add(svValue);
                }
                resultMap.put("Value", listsvValue);
                logger.info("SV查询得值svValue:"+resultMap);
            }else{
                    resultMap.put("Value", obj);
            }
        }

        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);

        logger.info("resultMap=" + resultMap);
        return resultMap;
    }

    /**
     * 获取设备当前状态信息：EquipStatus，PPExecname，ControlState
     *
     * @return
     */
    public Map findDeviceRecipe() {
        if (!this.isSdrReady()) {
            logger.error("sdrReady Not Ready");
            logger.error("isInterrupted:[" + isInterrupted() + "]isStartUp:[" + isStartUp() + "]isThreadUsed:[" + isThreadUsed() + "]");
            return null;
        }
        if (FengCeConstant.CONTROL_OFFLINE.equalsIgnoreCase(this.getControlState())) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备处于Offline状态...");
            return null;
        }
        Map resultMap = sendS1F3Check();
        updateCommStateInExt();
        return resultMap;
    }


    public Map findEqptStatus() {
        return this.findDeviceRecipe();

    }

    public String testRUThere() {
        try {
            DataMsgMap s1f2in = activeWrapper.sendS1F1out();
            if (s1f2in != null) {
                //如果回复取消会话，那么需要重新发送S1F13
                if (s1f2in.getMsgSfName().contains("S1F0")) {
                    logger.info("testRUThere成功,但是未正确回复消息,需要重新建立连接 ");
                    return "1";
                } else {
                    logger.info("testRUThere成功、通信正常 ");
                    return "0";
                }
            } else {
                return "2";
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return "2";
        }
    }

    public boolean testInitLink() throws BrokenProtocolException, HsmsProtocolNotSelectedException {
        try {

            DataMsgMap s1f14in = activeWrapper.sendS1F13out();
            setCommState(1);
            logger.info("testInitLink成功 建立连接、通信正常 ");
            return true;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    public void processS1F13in(DataMsgMap data) {
        try {

            activeWrapper.sendS1F14out((byte) 0, data.getTransactionId());
            setCommState(1);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F13out() {
        try {
            DataMsgMap data = activeWrapper.sendS1F13out();
            if (data != null) {
                setCommState(1);
            }
        } catch (Exception e) {
            setCommState(0);
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS1F14in(DataMsgMap data) {
        setCommState(1);
    }

    @SuppressWarnings("unchecked")
    public void sendS1F15out() {

        try {
            DataMsgMap msgdata = activeWrapper.sendS1F15out();
            byte onlack = (byte) msgdata.get("OFLACK");
            if (onlack == 0 || onlack == 2) {
                setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS1F17out() {

        try {
            DataMsgMap data = activeWrapper.sendS1F17out();
            byte onlack = (byte) data.get("ONLACK");
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
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS2F13out(ecidlist, ecFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = new ArrayList<>();
        if (data == null || data.get("EC") == null) {
            return null;
        }
        ArrayList listtmp = (ArrayList) data.get("EC");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ECValueList", listtmp);
        return resultMap;
    }

    public Map sendS2F13ECSingleCheckout(String ecid) {

        List<Long> ecidList = new ArrayList();
        ecidList.add(Long.parseLong(ecid));
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS2F13out(ecidList, ecFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();

        String ecValue = null;

       // ArrayList listtmp = (ArrayList) data.get("EC");
        //  ecValue = String.valueOf(listtmp.get(0));
        //EC
        ArrayList listecValue = new ArrayList();
        Object obj = data.get("EC");
        if(obj != null){
           if(obj instanceof ArrayList){
                ArrayList listtmp = (ArrayList)obj;
                for(int i=0;i<listtmp.size();i++){
                     ecValue = String.valueOf(listtmp.get(i));
                    listecValue.add(ecValue);
                }
                resultMap.put("Value", listecValue);
                logger.info("EC查询得值ecValue:"+listecValue);
            }else{
                resultMap.put("Value", obj);
            }
        }
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
       // resultMap.put("Value", ecValue);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public void sendS2F15out(String ecid, String ecv) {
        DataMsgMap s2f15out = new DataMsgMap("S2F15OUT", activeWrapper.getDeviceId());
        s2f15out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        long tmpL = Long.parseLong(ecid);
        long l[] = new long[1];
        l[0] = tmpL;
        float tmpF = Float.parseFloat(ecv);
        float f[] = new float[1];
        f[0] = tmpF;
        SecsItem rootItem = new SecsItem();
        List rootList = new ArrayList();

        SecsItem secsItemL = new SecsItem(l, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER);
        rootList.add(secsItemL);
        SecsItem secsItemF = new SecsItem(f, FormatCode.SECS_4BYTE_FLOAT_POINT);
        rootList.add(secsItemF);
        SecsItem secsItemRootList = new SecsItem(rootList, FormatCode.SECS_LIST);

        s2f15out.put("S2F15OUT", secsItemRootList);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s2f15out);
            byte[] ack = (byte[]) ((SecsItem) msgdata.get("AckCode")).getData();
            if (ack[0] == 0 || ack[0] == 2) {
                setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F17in(DataMsgMap data) {
        DataMsgMap s2f18out = new DataMsgMap("S2F18OUT", activeWrapper.getDeviceId());
        s2f18out.setTransactionId(data.getTransactionId());
        try {
            DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
//            s2f18out.put("Time", df.format(new Date()));
            SecsItem secsItem = new SecsItem(df.format(new Date()), FormatCode.SECS_ASCII);
            s2f18out.put("S2F18OUT", secsItem);
            activeWrapper.respondMessage(s2f18out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    public void sendS2F33out(long dataid, long reportId, List svidList) {
        try {
            activeWrapper.sendS2F33out(dataid, svFormat, reportId, rptFormat, svidList, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS2F33out(long reportId, long svid) {
        List svidList = new ArrayList();
        svidList.add(svid);
        try {
            activeWrapper.sendS2F33out(reportId, svFormat, reportId, rptFormat, svidList, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS2f33outDelete(long reportId) {
        try {
            activeWrapper.sendS2F33out(reportId, svFormat, reportId, rptFormat, null, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    public void processS2F34in(DataMsgMap data) {
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
    }

    /**
     * @param dataid
     * @param ceid
     * @param rptid
     */
    public void sendS2F35out(long dataid, long ceid, long rptid) {
        List reportidList = new ArrayList();
        reportidList.add(rptid);
        try {
            activeWrapper.sendS2F35out(dataid, svFormat, ceid, ceFormat, reportidList, rptFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS2F35out(long ceid, long rptid) {
        List reportidList = new ArrayList();
        reportidList.add(rptid);
        try {
            activeWrapper.sendS2F35out(rptid, svFormat, ceid, ceFormat, reportidList, rptFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35outDelete(long dataid, long ceid) {
        try {
            activeWrapper.sendS2F35out(dataid, svFormat, ceid, ceFormat, null, rptFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void processS2F36in(DataMsgMap data) {
        logger.info("----------Received s2f36in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37out(long ceid) {
        List ceidList = new ArrayList();
        ceidList.add(ceid);
        try {
            activeWrapper.sendS2F37out(true, ceidList, ceFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F37outClose(long ceid) {
        List<Long> ceids = new ArrayList<>();
        ceids.add(ceid);
        try {
            activeWrapper.sendS2F37out(false, ceids, ceFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    /**
     * 开启机台所有事件报告
     */
    public void sendS2F37outAll() {
        try {
            activeWrapper.sendS2F37out(true, new ArrayList<>(), ceFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void sendS2F37outCloseAll() {
        try {
            activeWrapper.sendS2F37out(false, new ArrayList<>(), ceFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS2F38in(DataMsgMap data) {
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
    //TODO 此方法异常 有时间再修改
    public Map sendCommandByDymanic(String commandKey) {
        DataMsgMap s2f41out = new DataMsgMap("S2F41OUT", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
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
            byte hcack = -1;
            try {
                DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
                hcack = (byte) ((SecsItem) data.get("HCACK")).getData();
                logger.info("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s2f42");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd " + commandKey + " at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
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


    public Map sendS2F41outPPselect(String recipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put(CPN_PPID, recipeName);
            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put(CPN_PPID, FormatCode.SECS_ASCII);
            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put(recipeName, FormatCode.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add(CPN_PPID);
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cpmap, cpNameFromatMap, cpValueFromatMap);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            byte hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 means " + e.getMessage());
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
    public Map sendS2f41Cmd(String rcmd) {
        DataMsgMap msgdata = null;
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("prevCmd", rcmd);

        try {
            msgdata = activeWrapper.sendS2F41out(rcmd, null, null, null, null);
            logger.info("The equip " + deviceCode + " request to " + rcmd);
            byte hcack = (byte) msgdata.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd " + rcmd + " at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd " + rcmd + " at equip " + deviceCode + " get a result with HCACK=" + 9 + " means " + e.getMessage());
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
        byte aled;
        boolean[] flag = new boolean[1];
        flag[0] = enable;
        if (enable) {
            aled = -128;
        } else {
            aled = 0;
        }
        try {
            activeWrapper.sendS5F3out(aled, -1, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public boolean replyS5F2Directly(DataMsgMap data) {
        try {
            activeWrapper.sendS5F2out((byte) 0, data.getTransactionId());
            return true;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return false;
        }
    }

    public Map processS5F1in(DataMsgMap data) {
        try {
            activeWrapper.sendS5F2out((byte) 0, data.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte ALCD = (byte) data.get("ALCD");
        long ALID = (long) data.get("ALID");
        String ALTX = (String) data.get("ALTX");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s5f1");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("deviceId", deviceId);
        resultMap.put("ALID", ALID);
        resultMap.put("ALCD", ALCD);
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

    public void processS6F11in(DataMsgMap data) {
        long ceid = -12345679;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
//            if (equipSecsBean.collectionReports.get(ceid) != null) {
//                Process process = equipSecsBean.collectionReports.get(ceid);
            //todo 这里看是重定义事件报告，还是查一遍sv数据把数据放到data里方便后面使用
            //
//                if (process.getProcessKey().equals("STATE_CHANGE")) {
//
//                }
//                EventDealer.deal(data, deviceCode, process, activeWrapper);

//            }
            //TODO 根据ceid分发处理事件
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else {
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
                if (ceid == EquipStateChangeCeid) {
                    processS6F11EquipStatusChange(data);
                }
            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    protected void processS6F11inStripMapUpload(DataMsgMap data) {
        logger.info("----Received from Equip Strip Map Upload event - S6F11");
        try {
            ArrayList reportData = (ArrayList) data.get("REPORT");
            //获取xml字符串
//            String stripMapData = (String) ((SecsItem) data.get("MapData")).getData();
            String stripMapData = (String) ((ArrayList) reportData.get(1)).get(0);
            String stripId = XmlUtil.getStripIdFromXml(stripMapData);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "请求上传Strip Map！StripID:[" + stripId + "]");
            //通过Web Service上传mapping

        //    byte ack = WSUtility.binSet(stripMapData, deviceCode).getBytes()[0];
            byte ack = AxisUtility.uploadStripMap(stripMapData, deviceCode).getBytes()[0];
            if (ack == '0') {//上传成功
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上传Strip Map成功！StripID:[" + stripId + "]");
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
            } else {//上传失败
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上传Strip Map失败！StripID:[" + stripId + "]");
                activeWrapper.sendS6F12out((byte) 1, data.getTransactionId());
            }
            logger.info(" ----- s6f12 sended - Strip Upload Completed-----.");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    //直接回复S6F12
    protected void replyS6F12WithACK(DataMsgMap data, byte ackCode) {
        //回复s6f11消息
        try {
            activeWrapper.sendS6F12out(ackCode, data.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    //比对Recipe名称
    protected boolean checkRecipeName(String tragetRecipeName) {
        return tragetRecipeName.equals(ppExecName);
    }

    public void sendTerminalMsg2EqpSingle(String msg) {
        sendS10F3((byte) 0, msg);
    }

    protected void sendS10F1(byte[] tid, String text) {

        DataMsgMap out = new DataMsgMap("s10f1out", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            out.put("TID", tid);
            out.put("TEXT", text);
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

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
    }

    protected void sendS10F3(byte tid, String text) {
        try {
            if (text.length() > 300) {
                logger.info("准备向设备发送的消息为:" + text + ",长度超过限制，将被截取");
                text = text.substring(0, 300) + "...";
                logger.info("实际向设备发送的消息为:" + text);
            }
            activeWrapper.sendS10F3out(tid, text);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        findDeviceRecipe();

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
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，请改机!");
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
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序一致，核对通过！");
                        }
                    }
                    if (checkResult) {
                        if ("A".equals(startCheckMod)) {
                            //首先判断下载的Recipe类型
                            //1、如果下载的是Unique版本，那么执行完全比较
                            String downloadRcpVersionType = downLoadRecipe.getVersionType();
                            if ("Unique".equals(downloadRcpVersionType)) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                                this.startCheckRecipePara(downLoadRecipe, "abs");
                            } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                                if (!hasGoldRecipe) {
                                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在[" + ppExecName + "]的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                    //不允许开机
                                    this.holdDeviceAndShowDetailInfo();
                                } else {
                                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "[" + ppExecName + "]开始WI参数Check");
                                    this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                                }
                            }
                        } else if ("B".equals(startCheckMod)) {
                            startSVcheckPass = false;
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
                            startSVcheck();
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

    @SuppressWarnings("unchecked")
    public void processS7F1in(DataMsgMap data) {
        DataMsgMap s7f2out = new DataMsgMap("s7f2out", activeWrapper.getDeviceId());
        byte[] ack = new byte[1];
        //目前不允许从机台直接上传recipe
        ack[0] = 5;
        s7f2out.put("PPGNT", ack);
        s7f2out.setTransactionId(data.getTransactionId());
        try {
            activeWrapper.respondMessage(s7f2out);
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

        long length = TransferUtil.getPPLength(localFilePath);
        if (length == 0) {
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
            return resultMap;
        }

        DataMsgMap data = null;

        try {
            data = activeWrapper.sendS7F1out(targetRecipeName, length, lengthFormat);
            byte ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
            resultMap.put("ppgnt", ppgnt);
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
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        targetRecipeName = targetRecipeName.replace("@", "/");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, FormatCode.SECS_BINARY);
            byte ackc7 = (byte) data.get("ACKC7");
            resultMap.put("ACKC7", ackc7);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }


    protected Object getPPBODY(String recipeName) throws UploadRecipeErrorException {
        try {
            return activeWrapper.sendS7F5out(recipeName).get("PPBODY");
        } catch (Exception e) {
            e.printStackTrace();
            throw new UploadRecipeErrorException("Get recipe body from equip " + deviceCode + " failed.");
        }
    }

    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);

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


    public Map sendS7F17out(String recipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        List recipeIDlist = new ArrayList();
        recipeIDlist.add(recipeName);
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(recipeIDlist);
            logger.info("Request delete recipe " + recipeName + " on " + deviceCode);
            byte ackc7 = (byte) data.get("ACKC7");
            if (ackc7 == 0) {
                logger.info("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7 + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
            resultMap.put("ACKC7", ackc7);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F19out();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList list = (ArrayList) data.get("EPPD");
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
            resultMap.put("EPPD", new ArrayList<>());
        } else {
            logger.info("recipeNameList:" + list);
            resultMap.put("eppd", list);
            resultMap.put("EPPD", list);
        }
        return resultMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S14FX Code">
    @SuppressWarnings("unchecked")
    protected void processS14F1in(DataMsgMap data) {
        if (data == null) {
            return;
        }
        String objType = null;
        if (data.get("OBJTYPE") != null) {
            objType = (String) data.get("OBJTYPE");
        }
        String stripId = "";
        if (data.get("OBJID") != null) {
            stripId = (String) (data.get("OBJID"));
        }
        UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备请求下载Strip Map，StripId：[" + stripId + "]");
        DataMsgMap out = null;
        //通过Web Service获得xml字符串
        Map<Object, Map> objMap = new HashMap<>();
        Map<Long, String> errorMap = new HashMap<>();
        Map stripMap = new HashMap();
        Map stripIDformatMap = new HashMap();
        stripIDformatMap.put("MapData", FormatCode.SECS_ASCII);
        byte objack = 0;
//        String stripMapData = WSUtility.binGet(stripId, deviceCode);
        String stripMapData = AxisUtility.downloadStripMap(stripId, deviceCode);
//        String stripMapData = "<stripmaptest12312313";
        if (stripMapData == null) {//stripId不存在
            out = new DataMsgMap("s14f2outNoExist", activeWrapper.getDeviceId());
            long[] u1 = new long[1];
            u1[0] = 0;
            out.put("OBJACK", u1);
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "StripId：[" + stripId + "] Strip Map 不存在！");

        } else {//stripId存在
            String downLoadResult = stripMapData.substring(0, 1);
            if ("<".equals(downLoadResult)) {
//                out = new DataMsgMap("s14f2out", activeWrapper.getDeviceId());
//                out.put("StripId", stripId);
//                out.put("MapData", stripMapData);
                stripMap.put("MapData", stripMapData);
                objMap.put(stripId, stripMap);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "从服务器下载Strip Map成功,StripId：[" + stripId + "]");
            } else {
                objack = 1;
                //是分号
                long errorCode = 10L;
                try {
                    errorCode = Long.valueOf(stripMapData.split(";")[0]);
                } catch (Exception e) {
                    errorCode = 10L;
                }
                stripMap.put("MapData", stripMapData);
                objMap.put(stripId, stripMap);
                errorMap.put(errorCode, stripMapData);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "从服务器下载Strip Map失败,StripId：[" + stripId + "],失败原因：" + stripMapData);
            }

        }
        try {

//            activeWrapper.sendS14F2out(stripMap, FormatCode.SECS_ASCII, FormatCode.SECS_ASCII, stripIDformatMap, (byte) 2,
//                    errorMap, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, data.getTransactionId());

            activeWrapper.sendS14F2out(objMap, FormatCode.SECS_ASCII, FormatCode.SECS_ASCII, stripIDformatMap,
                    objack, errorMap, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, data.getTransactionId());
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送Strip Map到设备,StripId：[" + stripId + "]");
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
        activeWrapper.terminateSecsDriver();
    }

    /**
     * 开启SECS通信线程
     *
     * @throws NotInitializedException
     */
    public void startSecs(EquipmentEventDealer eqpEventDealer)
            throws NotInitializedException, InterruptedException, InvalidHsmsHeaderDataException, T3TimeOutException, T6TimeOutException, HsmsProtocolNotSelectedException, IllegalStateTransitionException {
        if (this.activeWrapper == null) {
            throw new NotInitializedException("Host with device id = " + this.deviceId
                    + " Equip Id = " + this.deviceId + " is not initialized yet.");
        }
        logger.info("SECS Protocol for " + this.deviceId + " is being started.");
        this.activeWrapper.connectByActiveMode(eqpEventDealer);
        eqpEventDealer.execute();
        activeWrapper.addInputMessageListenerToAll(this);
        activeWrapper.startInActiveMode();
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="WaitMsgValueMap Code">

    /**
     * @param transactionId
     * @return
     */
    public DataMsgMap getMsgDataFromWaitMsgValueMapByTransactionId(long transactionId) {
        int i = 0;
        logger.info("Can not get value directly,will try to get value from message queue");
        while (i < 5) {
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
        if (i >= 5) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "从设备获取数据失败，请检查设备通讯状态！");
            logger.error("从设备获取数据失败，设备编号：" + deviceCode);
            return null;
        }
        waitMsgValueMap.remove(transactionId);
        if (waitMsgValueMap.entrySet().size() > 1000) {
            waitMsgValueMap.clear();
        }
        return null;
    }

    protected void putDataIntoWaitMsgValueMap(DataMsgMap dataHashtable) {
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
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Press使用状态为[" + preseUseState + "]");
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
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行设备Press使用情况检查");
        if (!this.checkPressFlagFromServerByWS(deviceCode)) {
            String pressState = getPressUseState();
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备Press[" + pressState + "]使用错误，设备将被锁!");
            this.holdDeviceByServer("PRESS_ERROR_LOCK");
            String dateStr = GlobalConstants.dateFormat.format(new Date());
            this.sendTerminalMsg2EqpSingle("[" + dateStr + "] PressUse Error, machine locked.");
        } else {
            List<DeviceInfoLock> deviceInfoLocks = deviceService.searchDeviceInfoLockByMap(deviceCode, "PRESS_ERROR_LOCK", "Y");
            if (deviceInfoLocks != null && !deviceInfoLocks.isEmpty()) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备Press使用正常，设备将解锁!");
                deviceService.updateDeviceInfoLock(deviceCode, "PRESS_ERROR_LOCK", "N");
                sqlSession.commit();
                this.releaseDeviceByServer("PRESS_ERROR_LOCK");
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备Press使用正常");
            }
            pass = true;
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备Press使用情况检查执行结束");
        return pass;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="SpecificData Collection">

    /**
     * @param dataIdMap
     * @return
     */
    public Map getSpecificData(Map<String, String> dataIdMap) throws UploadRecipeErrorException {
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

            try {
                DataMsgMap data = null;
                data = activeWrapper.sendS1F3out(dataIdList, svFormat);

                if (data != null && data.get("SV") != null) {
                    //todo 取值的問題，有可能是String
                    svValueList = (ArrayList) (data.get("SV"));

                    for (int i = 0; i < svValueList.size(); i++) {
                        if (svValueList.get(i) instanceof long[]) {
                            long[] longs = ((long[]) svValueList.get(i));
                            if (longs.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(longs[0]));
                            }
                            continue;
                        }

                        if (svValueList.get(i) instanceof int[]) {
                            int[] ints = ((int[]) svValueList.get(i));
                            if (ints.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(ints[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof String) {
                            String s = (String) svValueList.get(i);
                            resultMap.put(svidList.get(i), s);
                            continue;
                        }
                        if (svValueList.get(i) instanceof String[]) {
                            String[] s = (String[]) svValueList.get(i);
                            if (s.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(s[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof  float[]) {
                            float[] floats = ( float[]) svValueList.get(i);
                            if (floats.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(floats[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof  byte[]) {
                            byte[] bytes = ( byte[]) svValueList.get(i);
                            if (bytes.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(bytes[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof  boolean[]) {
                            boolean[] booleans = (boolean[]) svValueList.get(i);
                            if (booleans.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(booleans[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof  double[]) {
                            double[] doubles = (double[]) svValueList.get(i);
                            if (doubles.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(doubles[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof  char[]) {
                            char[] chars = (char[]) svValueList.get(i);
                            if (chars.length == 0) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                resultMap.put(svidList.get(i), String.valueOf(chars[0]));
                            }
                            continue;
                        }
                        if (svValueList.get(i) instanceof  List) {
                            List list = (List) svValueList.get(i);
                            if (((List) list.get(i)).isEmpty()) {
                                resultMap.put(svidList.get(i), "");
                            } else {
                                ArrayList obj = new ArrayList<>();
                                ArrayList tmp = getIDValue((ArrayList) list.get(i));
                                resultMap.put(svidList.get(i), String.valueOf(tmp.get(0)));
                            }
                            continue;

                        }
                        resultMap.put(svidList.get(i), String.valueOf(svValueList.get(i)));
                        logger.info("resultMap:"+resultMap);
                    }
                    logger.info("Get SV value list:[" + JsonMapper.toJsonString(data) + "]");
                }
                if (data == null || data.isEmpty()) {
                    logger.error("Query SV List error[" + JsonMapper.toJsonString(data) + "]");
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Query SV List error！");
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
                return null;
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
            try {
                DataMsgMap data = activeWrapper.sendS2F13out(dataIdList, ecFormat);

                if (data != null && data.get("EC") != null) {
                    //判断返回值String/ArrayList
                        ecValueList = (ArrayList) data.get("EC");
                        for (int i = 0; i < ecValueList.size(); i++) {
                            resultMap.put(ecidList.get(i), String.valueOf(ecValueList.get(i)));
                        }

                    logger.info("Get EC value list:[" + JsonMapper.toJsonString(data) + "]");
                }
                if (data == null || data.isEmpty()) {
                    logger.error("Query EC value List error[" + JsonMapper.toJsonString(data) + "]");
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Query EC value List error！");
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
                return null;
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
    public Map getSpecificRcpParaData(List dataIdList) throws UploadRecipeErrorException {
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
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送实时状态至服务端");
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
//               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送实时状态至服务端");
            }
        }
    }

    /**
     * uph数据上报
     */
    public void sendUphData2Server() throws IOException, BrokenProtocolException, T6TimeOutException, HsmsProtocolNotSelectedException, T3TimeOutException, MessageDataException, StreamFunctionNotSupportException, ItemIntegrityException, InterruptedException {
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
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "当前设备已经被锁机");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行锁机");
                return true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + String.valueOf(resultMap.get("Description")));
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, holdDesc);
                String dateStr = GlobalConstants.dateFormat.format(new Date());
                this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + type);
                hold = true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                holdDesc = "设备将稍后执行锁机";
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, holdDesc);
                String dateStr = GlobalConstants.dateFormat.format(new Date());
                this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + type);
                hold = true;
            } else {
                Map eqptStateMap = this.findEqptStatus();
                holdDesc = "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState"));
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "当前设备已经被解锁");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行解锁");
                return true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "解锁失败，当前机台状态无法进行解锁，机台状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "当前设备已经被解锁");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行解锁");
                return true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "解锁失败，当前机台状态无法进行解锁，机台状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备开机作业！");
                return "Y";
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行开机任务！");
                return "Y";
            } else {
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "开机作业失败，当前机台状态无法进行开机，机台状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备控制状态切换被HOST命令切换到 " + controlState + "！");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行控制状态切换任务！");
                return true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备控制状态切换失败，当前状态为：" + eqptStateMap.get("EquipStatus") + "/" + eqptStateMap.get("ControlState"));
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
    public String sendRemoteCommand(DataMsgMap commandMsg) {
        commandMsg.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] hcack = new byte[1];
        try {
            DataMsgMap feedBackData = activeWrapper.sendAwaitMessage(commandMsg);
            hcack = (byte[]) ((SecsItem) feedBackData.get("HCACK")).getData();
            return String.valueOf(hcack);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
    }

    public void initRemoteCommand() {
    }

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
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机, 锁机原因为: " + lockReason);
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
            if (equipNodeBean.getDeviceCode().equals(this.deviceCode)) {
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
                    if (1 == (int) resultMap.get("CommState")) {
                        equipState.setCommOn(true);
                    } else {
                        equipState.setCommOn(false);
                    }
                    equipNodeBean.setEquipStateProperty(equipState);
                }
                if (resultMap.get("NetConnect") != null) {
                    if (1 == (int) resultMap.get("NetConnect")) {
                        EquipState equipState = equipNodeBean.getEquipStateProperty();
                        equipState.setNetConnect(true);
                        equipNodeBean.setEquipStateProperty(equipState);
                    }
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
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "CEID[" + ceid + "],事件描述：" + recipeTemplates.get(0).getParaDesc());
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
    public void upLoadAllRcp() throws UploadRecipeErrorException {
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
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传成功！共 " + eppd.size() + " 第 " + i + " 已完成");
                sqlSession.commit();
            } catch (Exception e) {
                sqlSession.rollback();
                logger.error("Exception:", e);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传失败！请重试！");
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
            ecsvIdList.add(Long.parseLong(recipeTemplates.get(i).getDeviceVariableId()));
//            ecsvIdList.add(((RecipeTemplate)recipeTemplates.get(i)).getDeviceVariableId());
        }
        return ecsvIdList;
    }



    protected List<RecipePara> transferECSVValue2RecipePara(List<RecipeTemplate> ECtemplates, List<RecipeTemplate> SVtemplates) {
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
            recipePara.setSetValue(String.valueOf(totalValueMap.get(Long.parseLong(recipeTemplate.getDeviceVariableId()))));
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

    public DataMsgMap sendMsg2EquipOld(DataMsgMap dataMsgMap) {
        final DataMsgMap dataMsgMapTemp = dataMsgMap;
        DataMsgMap result = null;
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Callable<DataMsgMap> call = new Callable<DataMsgMap>() {

            @Override
            public DataMsgMap call() throws Exception {
                //开始执行耗时操作  
                return activeWrapper.sendAwaitMessage(dataMsgMapTemp);
            }
        };

        Future<DataMsgMap> future = exec.submit(call);
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
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "该设备未设置参数实时监控,开机前实时值检查取消...");
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
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
                                UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
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
                                UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceInfoExt.getDeviceRowid(), "开机前参数实时检查未通过,参数编号:[" + recipePara.getParaCode() + "],"
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
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
            this.holdDeviceAndShowDetailInfo("Recipe parameter error,start check failed!The equipment has been stopped! Error parameter:" + eventDescEng);
            startSVcheckPass = false;
        } else {
            releaseDevice();
            eventDesc = "设备：" + deviceCode + " 开机前实时参数检查通过.";
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机前实时参数检查通过.");
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
        FtpUtil.uploadFile(localRcpPath, remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
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

    @SuppressWarnings("unchecked")
    public void sendS2F33clear() {

        try {
            activeWrapper.sendS2F33out(0, svFormat, 0, svFormat, null, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35clear() {
        try {
            activeWrapper.sendS2F35out(0, svFormat, 0, ceFormat, null, rptFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    /**
     * WaferMappingInfo Upload (Simple)
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F1inSimple(DataMsgMap DataMsgMap) {
        try {

            DataMsgMap s12f2out = new DataMsgMap("s12f2out", activeWrapper.getDeviceId());
            //TODO 调用webservices回传waferMapping信息
            byte[] ack = new byte[]{0};
            s12f2out.put("SDACK", ack);
            s12f2out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.respondMessage(s12f2out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * Map Transmit Inquire
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F5in(DataMsgMap DataMsgMap) {
        try {
            DataMsgMap s12f6out = new DataMsgMap("s12f6out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f6out.put("GRANT1", ack);
            s12f6out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.sendS12F6out((byte) 0, DataMsgMap.getTransactionId());

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * upload type1 未使用
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F7in(DataMsgMap DataMsgMap) {
        try {

            String MaterialID = DataMsgMap.get("MaterialID").toString();

            byte IDTYP = ((byte[]) ((SecsItem) DataMsgMap.get("IDTYP")).getData())[0];

            String binList = DataMsgMap.get("RSINFBinList").toString();

            DataMsgMap s12f8out = new DataMsgMap("s12f8out", activeWrapper.getDeviceId());
            byte[] ack = new byte[1];
            ack[0] = 0;
            s12f8out.put("MDACK", ack);
            s12f8out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.sendS12F8out((byte) 0, DataMsgMap.getTransactionId());

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * upload type3 未使用
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F11in(DataMsgMap DataMsgMap) {
        try {
            String MaterialID = DataMsgMap.get("MID").toString();

            byte IDTYP = (byte) DataMsgMap.get("IDTYP");

            String binList = DataMsgMap.get("XYPOSBinList").toString();

            activeWrapper.sendS12F2out((byte) 0, DataMsgMap.getTransactionId());

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * download type1 未使用
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F13in(DataMsgMap DataMsgMap) {
        try {
            DataMsgMap s12f14out = new DataMsgMap("s12f14out", activeWrapper.getDeviceId());

            s12f14out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.sendS12F14out("null", FormatCode.SECS_ASCII, (byte) 1, null, FormatCode.SECS_1BYTE_SIGNED_INTEGER, FormatCode.SECS_ASCII, DataMsgMap.getTransactionId());

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * download type3 未使用
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F17in(DataMsgMap DataMsgMap) {
        try {
            DataMsgMap s12f18out = new DataMsgMap("s12f18out", activeWrapper.getDeviceId());

            s12f18out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.respondMessage(s12f18out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public Map processS12F19in(DataMsgMap DataMsgMap) {
        try {

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * Map Restart Request
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F67in(DataMsgMap DataMsgMap) {
        try {
            DataMsgMap s12f68out = new DataMsgMap("s12f68out", activeWrapper.getDeviceId());
            s12f68out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.respondMessage(s12f68out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="S12FX Code"> 

    /**
     * WaferMappingInfo Upload
     *
     * @param dataMsgMap
     * @return
     */
    public Map processS12F1in(DataMsgMap dataMsgMap) {
        try {
            String MaterialID = (String) dataMsgMap.get("MID");
            MaterialID = MaterialID.trim();
            byte IDTYP = ((byte) dataMsgMap.get("IDTYP"));
            upFlatNotchLocation = (long) dataMsgMap.get("FNLOC");
//            long FileFrameRotation = dataMsgMap.getSingleNumber("FileFrameRotation");
            byte OriginLocation = ((byte) dataMsgMap.get("ORLOC"));
            long RowCountInDieIncrements = (long) dataMsgMap.get("ROWCT");
            long ColumnCountInDieIncrements = (long) dataMsgMap.get("COWCT");

            uploadWaferMappingRow = String.valueOf(RowCountInDieIncrements);
            uploadWaferMappingCol = String.valueOf(ColumnCountInDieIncrements);
            //kong
            //String NullBinCodeValue = (String)((SecsItem) dataMsgMap.get("NullBinCodeValue")).getData();
            //byte[] ProcessAxis = ((byte[]) ((SecsItem) dataMsgMap.get("ProcessAxis")).getData());
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "接受到机台上传WaferId：[" + MaterialID + "]设置信息！");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端上传机台WaferId：[" + MaterialID + "]设置信息！");
            DataMsgMap s12f2out = new DataMsgMap("s12f2out", activeWrapper.getDeviceId());
            //TODO 调用webservices回传waferMapping信息
            activeWrapper.sendS12F2out((byte) 0, dataMsgMap.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * WaferMapping Upload (Simple)
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F9in(DataMsgMap DataMsgMap) {
        try {
            String MaterialID = (String) DataMsgMap.get("MID");
            MaterialID = MaterialID.trim();
            byte IDTYP = (byte) DataMsgMap.get("IDTYP");
            int[] STRPxSTRPy = (int[]) DataMsgMap.get("STRP");
            Object BinListItem = DataMsgMap.get("BINLT");
            String binList = "";
            if (BinListItem instanceof Long[] || BinListItem instanceof long[]) {
                long[] binlists = (long[]) BinListItem;
                StringBuffer binBuffer = new StringBuffer();
                for (Long binlistLong : binlists) {
                    int temp = binlistLong.intValue();
                    char c = (char) temp;
                    binBuffer.append(c);
                }
                binList = binBuffer.toString();
            } else {
                binList = (String) DataMsgMap.get("BINLT");
            }
            logger.info("waferid:" + MaterialID + "binlist:" + binList);
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "机台上传WaferMapping成功！WaferId：[" + MaterialID + "]");
            //上传WaferMapping,
            String _uploadWaferMappingRow = uploadWaferMappingRow;
            String _uploadWaferMappingCol = uploadWaferMappingCol;
            if (this.deviceType.contains("ESEC") || this.deviceType.contains("SIGMA") || this.deviceType.contains("8800")) {
                logger.info(this.deviceType + "旋转角度:" + (360L - upFlatNotchLocation));
                binList = WaferTransferUtil.transferAngleAsFlatNotchLocation(binList, 360L - upFlatNotchLocation, uploadWaferMappingRow, uploadWaferMappingCol);
                if (upFlatNotchLocation == 90 || upFlatNotchLocation == 270) {
                    _uploadWaferMappingRow = uploadWaferMappingCol;
                    _uploadWaferMappingCol = uploadWaferMappingRow;
                }
            }
            //上传旋转后的行列数及mapping
            AxisUtility.sendWaferMappingInfo(MaterialID, _uploadWaferMappingRow, _uploadWaferMappingCol, binList, deviceCode);
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送WaferMapping成功！WaferId：[" + MaterialID + "]");
            DataMsgMap s12f10out = new DataMsgMap("s12f10out", activeWrapper.getDeviceId());
            byte[] ack = new byte[]{0};
            s12f10out.put("MDACK", ack);
            s12f10out.setTransactionId(DataMsgMap.getTransactionId());
            activeWrapper.sendS12F10out((byte) 0, DataMsgMap.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    /**
     * WaferMappingInfo Download
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F3in(DataMsgMap DataMsgMap) {
        DataMsgMap s12f4out = null;
        String MaterialID = "";
        try {
            //DataMsgMap s12f4out = new DataMsgMap("s12f4out2", activeWrapper.getDeviceId());
            MaterialID = (String) DataMsgMap.get("MID");
            MaterialID = MaterialID.trim();
            byte IDTYP = ((byte) DataMsgMap.get("IDTYP"));
            byte MapDataFormatType = (byte) DataMsgMap.get("MAPFT");
            downFlatNotchLocation = (long) DataMsgMap.get("FNLOC");
            byte OriginLocation = (byte) DataMsgMap.get("ORLOC");
            byte ProcessAxis = ((byte) DataMsgMap.get("PRAXI"));
//            String BinCodeEquivalents = (String) ((SecsItem) DataMsgMap.get("BinCodeEquivalents")).getData();
//            String NullBinCodeValue = (String) ((SecsItem) DataMsgMap.get("NullBinCodeValue")).getData();
            Object BinCodeEquivalents = DataMsgMap.get("BCEQU");
            Object NullBinCodeValue = DataMsgMap.get("NULBC");
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "机台请求WaferMapping设置信息！WaferId：[" + MaterialID + "]");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端请求WaferMapping设置信息！WaferId：[" + MaterialID + "]");
            Map<String, String> mappingInfo = AxisUtility.downloadWaferMap(deviceCode, MaterialID);
            if ("N".equals(mappingInfo.get("flag"))) {
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "WaferId：[" + MaterialID + "]下载失败," + mappingInfo.get("msg"));
                s12f4out = new DataMsgMap("s12f4Zeroout", activeWrapper.getDeviceId());
                s12f4out.setTransactionId(DataMsgMap.getTransactionId());

                activeWrapper.sendS12F4out(null, FormatCode.SECS_ASCII, IDTYP, downFlatNotchLocation, OriginLocation, 0, null, FormatCode.SECS_LIST, "um", 1231, 1231, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER
                        , 0, 0, -1, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, BinCodeEquivalents, NullBinCodeValue, FormatCode.SECS_ASCII, 0 * 0, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, DataMsgMap.getTransactionId()
                );
                this.sendTerminalMsg2EqpSingle(mappingInfo.get("msg"));
                return null;
            }
            logger.info("mappingInfo:" + mappingInfo);
            String binList = mappingInfo.get("BinList");
            int mapRow = Integer.parseInt(mappingInfo.get("RowCountInDieIncrements")); //原始wafer map行
            int mapCol = Integer.parseInt(mappingInfo.get("ColumnCountInDieIncrements")); //原始wafer map列
            String rote = mappingInfo.get("rote");//mapping原路径

            //esec 单独处理
            if (this.deviceType.contains("ESEC")) {
                StringBuilder newbinList = new StringBuilder("");
                char[][] binArray = WaferTransferUtil.toDoubleArray(binList, mapRow, mapCol);
                Map<String, Integer> map = WaferTransferUtil.blankCheck(binList.charAt(0), binList, mapRow, mapCol);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "UP:" + map.get("UP") + "DOWN:" + map.get("DOWN")
                        + "LEFT:" + map.get("LEFT") + "RIGHT:" + map.get("RIGHT"));

                int mapRowEsecNoNull = map.get("DOWN") - map.get("UP") + 1;
                int mapColEsecNoNull = map.get("RIGHT") - map.get("LEFT") + 1;

                for (int i = 0; i < mapRow; i++) {
                    if (i < map.get("UP") || i > map.get("DOWN")) {
                        continue;
                    }
                    for (int j = 0; j < mapCol; j++) {
                        if (j < map.get("LEFT") || j > map.get("RIGHT")) {
                            continue;
                        }
                        newbinList.append(binArray[i][j]);
                    }
                }
                binList = newbinList.toString();
                mapRow = mapRowEsecNoNull;
                mapCol = mapColEsecNoNull;
            }

//            waferMappingrow = String.valueOf(RowCountInDieIncrementss[0]);
//            waferMappingcol = String.valueOf(ColumnCountInDieIncrementss[0]);
//            String _waferMappingcol = waferMappingcol;
//            String _waferMappingrow = waferMappingrow;
            waferMappingbins = binList;
            logger.info(this.deviceType + "wafer map 旋转角度:" + downFlatNotchLocation + ";waferMappingrow:" + mapRow + ";waferMappingcol" + mapCol);
            if (this.deviceType.contains("ESEC") || this.deviceType.contains("SIGMA") || this.deviceType.contains("8800") || this.deviceType.contains("DB-800")) {
                logger.info(this.deviceType + "程序旋转方向和bin");
                waferMappingbins = WaferTransferUtil.transferAngleAsFlatNotchLocation(waferMappingbins, downFlatNotchLocation, mapRow, mapCol);
                if (downFlatNotchLocation == 90 || downFlatNotchLocation == 270) {
                    int temp = mapRow;
                    mapRow = mapCol;
                    mapCol = temp;
                }
            }

            s12f4out = new DataMsgMap("s12f4out", activeWrapper.getDeviceId());
            s12f4out.put("MaterialID", MaterialID);
            s12f4out.put("IDTYP", IDTYP);
            s12f4out.put("FlatNotchLocation", new long[]{downFlatNotchLocation});
            s12f4out.put("OriginLocation", OriginLocation);
            s12f4out.put("RowCountInDieIncrements", new long[]{mapRow});
            s12f4out.put("ColumnCountInDieIncrements", new long[]{mapCol});
            s12f4out.put("BinCodeEquivalents", BinCodeEquivalents);
            s12f4out.put("NullBinCodeValue", NullBinCodeValue);
            s12f4out.put("MessageLength", new long[]{mapRow * mapCol});
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "从服务端成功获取WaferMapping设置信息！WaferId：[" + MaterialID + "]");

            //针对DB800 mapping展示软件
            if (this.deviceType.contains("DB-800")) {
                String port = "8080";
                ISecsHost iSecsHost = new ISecsHost("127.0.0.1", port, deviceType, deviceCode);
                String commond = rote + "," + mapRow + "," + mapCol + "," + binList + "," + MaterialID;
                logger.info("准备发送服务器端数据至wafer软件" + commond);
                iSecsHost.executeCommand3("START," + commond + ",END;");
            }
            try {
                s12f4out.setTransactionId(DataMsgMap.getTransactionId());

                activeWrapper.sendS12F4out(MaterialID, FormatCode.SECS_ASCII, IDTYP, downFlatNotchLocation, OriginLocation, 0, null, FormatCode.SECS_LIST, "um", 1231, 1231, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER
                        , mapRow, mapCol, -1, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, BinCodeEquivalents, NullBinCodeValue, FormatCode.SECS_ASCII, mapRow * mapCol, FormatCode.SECS_2BYTE_UNSIGNED_INTEGER, DataMsgMap.getTransactionId()
                );
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "发送WaferMapping设置信息至机台！WaferId：[" + MaterialID + "]");
            } catch (Exception ex) {
                logger.error("Exception:", ex);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            try {
                s12f4out = new DataMsgMap("s12f4Zeroout", activeWrapper.getDeviceId());
                s12f4out.setTransactionId(DataMsgMap.getTransactionId());
                activeWrapper.respondMessage(s12f4out);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "获取服务端WaferMappingInfo出现异常！");
            } catch (Exception ex) {
                logger.error("Exception:", e);
            }
        }

        return null;
    }


    /**
     * WaferMapping Download
     *
     * @param DataMsgMap
     * @return
     */
    public Map processS12F15in(DataMsgMap DataMsgMap) {
        DataMsgMap s12f16out = null;
        String MaterialID = "";
        try {
            MaterialID = (String) DataMsgMap.get("MID");
            MaterialID = MaterialID.trim();
            byte IDTYP = (byte) DataMsgMap.get("IDTYP");
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "机台请求WaferMapping！WaferId：[" + MaterialID + "]");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端请求WaferMapping！WaferId：[" + MaterialID + "]");

            s12f16out = new DataMsgMap("s12f16out", activeWrapper.getDeviceId());
            s12f16out.put("MaterialID", MaterialID);
            s12f16out.put("IDTYP", IDTYP);
            logger.info("waferMappingbinList:" + waferMappingbins);
            SecsItem BinList = new SecsItem(waferMappingbins, FormatCode.SECS_ASCII);
            s12f16out.put("BinList", BinList);
            s12f16out.setTransactionId(DataMsgMap.getTransactionId());
            long[] strps = new long[2];
            strps[0] = 0;
            strps[1] = 0;
            activeWrapper.sendS12F16out(MaterialID, FormatCode.SECS_ASCII, IDTYP, strps, FormatCode.SECS_2BYTE_SIGNED_INTEGER,
                    waferMappingbins, FormatCode.SECS_ASCII, DataMsgMap.getTransactionId());
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "发送WaferMapping至机台！WaferId：[" + MaterialID + "]");
        } catch (Exception e) {
            logger.error("Exception:", e);
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "获取服务端WaferMapping出现异常！");
            s12f16out = new DataMsgMap("s12f16outZero", activeWrapper.getDeviceId());
            s12f16out.setTransactionId(DataMsgMap.getTransactionId());
            try {
                activeWrapper.respondMessage(s12f16out);
            } catch (Exception ex) {
                logger.error("Exception:", e);
            }
        }
        return null;
    }

    public Map processS12F81in(DataMsgMap DataMsgMap) {
        try {
            activeWrapper.sendS12F82out((byte) 0, DataMsgMap.getTransactionId());
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    public Map processS6F83in(DataMsgMap DataMsgMap) {
        if (this.deviceType.contains("HITACHI")) {
            try {
                long[] IDTYP = (long[]) DataMsgMap.get("PICKxPICKy");

                String row = "" + IDTYP[0];//X坐标
                String col = "" + IDTYP[1];//Y坐标

                InetAddress address = InetAddress.getLocalHost();
                String HostName = address.getHostName();//获取计算机名
                String ip = address.getHostAddress();//获取IP地址
                InetAddress address3 = InetAddress.getByName("IP地址");
                String port = "8080";
                ISecsHost iSecsHost = new ISecsHost("127.0.0.1", port, deviceType, deviceCode);
                logger.info("准备发送坐标至wafer软件" + row + "," + col);
                iSecsHost.executeCommand3("START," + row + col + ",END;");

            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        return null;
    }

    public String GpMqTest(int deviceId, String lot_id, String lot_count) {
        return null;
    }


    protected String getRecipeRemotePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        return recipeService.organizeUploadRecipePath(recipe);
    }

    public boolean startCheckRecipeParaReturnFlag(Recipe checkRecipe) throws UploadRecipeErrorException {
        return startCheckRecipeParaReturnFlag(checkRecipe, "");
    }

    /**
     * 开机check recipe参数:使用获取recipe文件解析参数
     *
     * @param checkRecipe
     * @param type
     */
    public boolean startCheckRecipeParaReturnFlag(Recipe checkRecipe, String type) throws UploadRecipeErrorException {
        boolean checkParaFlag = false;
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
                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
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
            return checkParaFlag;
        } finally {
            sqlSession.close();
            return checkParaFlag;
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

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

}
