/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.ws;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.domain.DeviceRealtimePara;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

/**
 * @author njtz
 */
@SuppressWarnings("serial")
public class DiscoWSHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoWSHost.class.getName());
    private String shortName = "--";
    private boolean kerfCheck = true;
    private long Z1offset = 0L;
    private long Z2offset = 0L;


    public DiscoWSHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        RCMD_PPSELECT = "PP_SELECT_S";
        CPN_PPID = "DEV_NO";

    }

    public Object clone() {
        DiscoWSHost newEquip = new DiscoWSHost(deviceId,
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
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    //重定义 机台的equipstatuschange事件报告
                    initRptPara();
                    sendS5F3out(true);
                    sendS2F37out(7L);
                    updateLotId();
//                    upLoadAllRcp();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipStatusChange1")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11kerfCheck")) {
                    processS6F11KerfCheck(msg);
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
                        if (ceid == 7) {
                            if (!kerfCheck) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "需要刀痕检测,设备将自动暂停.");
                                pauseDevice();
                                kerfCheck = true;
                            }
                            processS6F11KerfCheck(msg);
                        }
                        if (ceid == 13) {
                            if (equipStatus.equalsIgnoreCase("run")) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "换刀后需要刀痕检测,设备将自动暂停.");
                                pauseDevice();
                                Map map = new HashMap();
                                map.put("msgName", "KerfCheck");
                                map.put("Z1", "");
                                map.put("Z2", "");
                                map.put("deviceCode", deviceCode);
                                map.put("type", "clear");
                                map.put("type", "add");
                                GlobalConstants.C2SSpecificDataQueue.sendMessage(map);
                            }
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
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11kerfCheck")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
//                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                this.putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                this.putDataIntoWaitMsgValueMap(data);
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

    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
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
            //检查模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！\n");
                holdDevice();
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            if ("idle".equalsIgnoreCase(equipStatus)) {
                kerfCheck = false;
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            String busniessMod = deviceInfoExt.getBusinessMod();

            if (AxisUtility.isEngineerMode(deviceCode) && equipStatus.equalsIgnoreCase("run")) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            } else //开机check
                if (equipStatus.equalsIgnoreCase("run")) {
                    if (this.checkLockFlagFromServerByWS(deviceCode)) {
                        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                        this.holdDevice();
                        return;
                    }
                    if (!checkBladeId()) {
                        holdDevice();
                        return;
                    }
                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    if (checkRecipe == null) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: " + shortName + "！ 请确认是否已审核通过！");
                        this.holdDevice();
                        return;
                    }
                    if (!shortName.equals(checkRecipe.getRecipeName())) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备使用程序： " + shortName + " ;与领料程序：" + checkRecipe.getRecipeName() + " 不一致，禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    //检查程序是否存在 GOLD
                    Recipe goldRecipe = recipeService.getGoldRecipe(shortName, deviceCode, deviceType);
                    if (goldRecipe == null) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + shortName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                        this.holdDevice();
                        return;
                    }
//                    startCheckEC();
                    this.startCheckRecipePara(checkRecipe);
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


    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
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
            logger.info("paraMap size:" + paraMap.size() + "----" + paraMap.isEmpty() + "--++" + paraMap == null);
            recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
            logger.info("recipePara size:" + recipeParaList.size());
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

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand">
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                setAlarmState(2);
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    public Map pauseDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt == null) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
        if ("Engineer".equals(deviceInfoExt.getBusinessMod())) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式,取消刀痕检测.");
            return null;
        }
        if ("Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("PAUSE");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                setAlarmState(2);
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
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


    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="StartECCheck">
    /*
     *刀片机开机前检查刀片属性：Clearance between Flange and work surface 
     * ECID:4965
     */
    private void startCheckEC() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "StartECCheck");
        if (recipeTemplates != null && recipeTemplates.size() > 0) {
            List ecIdList = getECSVIdList(recipeTemplates);
            Map ecValueMap = this.sendS2F13ECCheckout(ecIdList);
            ArrayList ecValueList = (ArrayList) ecValueMap.get("ECValueList");
            List<DeviceRealtimePara> ecErroParas = putEC2DeviceRealtimeParas(recipeTemplates, this, ecValueList);
            try {
                monitorService.saveDeviceRealtimePara(ecErroParas);
                sqlSession.commit();
            } catch (Exception e) {
                logger.error("Exception:", e);
            } finally {
                sqlSession.close();
            }
        }
    }

    private List<DeviceRealtimePara> putEC2DeviceRealtimeParas(List<RecipeTemplate> recipeTemplates, EquipHost equipHost, ArrayList svList) {
        List<DeviceRealtimePara> realTimeParas = new ArrayList<>();
        boolean holdFlag = false;
        for (int i = 0; i < recipeTemplates.size(); i++) {
            String minValue = recipeTemplates.get(i).getMinValue();
            String maxValue = recipeTemplates.get(i).getMaxValue();
            String realTimeValue = svList.get(i).toString();
            DeviceRealtimePara realtimePara = new DeviceRealtimePara();
            if (!"".equals(minValue) && !"".equals(maxValue) && minValue != null && maxValue != null) {
                if ((Double.parseDouble(realTimeValue) < Double.parseDouble(minValue)) || (Double.parseDouble(realTimeValue) > Double.parseDouble(maxValue))) {
                    realtimePara.setRemarks("RealTimeErro");
                    equipHost.setAlarmState(2);
                    holdFlag = true;
                }
            }
            realtimePara.setId(UUID.randomUUID().toString());
            realtimePara.setDeviceName(equipHost.getDeviceCode());
            realtimePara.setDeviceCode(equipHost.getDeviceCode());
            realtimePara.setMaxValue(recipeTemplates.get(i).getMaxValue());
            realtimePara.setMinValue(recipeTemplates.get(i).getMinValue());
            realtimePara.setParaCode(recipeTemplates.get(i).getParaCode());
            realtimePara.setParaDesc(recipeTemplates.get(i).getParaDesc());
            realtimePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
            realtimePara.setParaName(recipeTemplates.get(i).getParaName());
            realtimePara.setRealtimeValue(realTimeValue);
            realtimePara.setSetValue(recipeTemplates.get(i).getSetValue());
            realtimePara.setValueType(recipeTemplates.get(i).getParaType());
            realtimePara.setUpdateCnt(0);
            realTimeParas.add(realtimePara);
        }
        if (holdFlag) {
            holdDevice();
        }
        return realTimeParas;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="BladeEdge">
    public long[] getBladeEdge() {
        DataMsgMap bladeEdgeDataHashtable = null;
        List bladeidList = new ArrayList();
        bladeidList.add(1302L);
        bladeidList.add(1303L);
        try {
            bladeEdgeDataHashtable = activeWrapper.sendS1F3out(bladeidList, svFormat);
        } catch (Exception e) {
        }
        List listtmp = (ArrayList) bladeEdgeDataHashtable.get("SV");

        long[] bladeEdges = new long[2];
        bladeEdges[0] = Long.valueOf(String.valueOf(listtmp.get(0)));
        bladeEdges[1] = Long.valueOf(String.valueOf(listtmp.get(1)));
        return bladeEdges;
    }

    private Map getThresholdCalculateElement(List<RecipePara> recipeParas) {
        long[] bladeEdges = getBladeEdge();
        long z1BladeEdge = bladeEdges[0];
        long z2BladeEdge = bladeEdges[1];
        long WORK_THICK = 0l;
        long TAPE_THICK = 0l;
        long CH1_HEI1 = 0l;
        long CH2_HEI1 = 0l;
        long CH1_HEI21 = 0l;
        long CH2_HEI21 = 0l;
        String cut_mode = "";
        String cut_proc = "";
        for (RecipePara recipePara : recipeParas) {
            if (recipePara.getParaName().equals("CUT_MODE1")) {
                cut_mode = recipePara.getSetValue();
            }
            if (recipePara.getParaName().equals("CUT_PROC1")) {
                cut_proc = recipePara.getSetValue();
            }
            if (recipePara.getParaName().equals("WORK_THICK")) {
                WORK_THICK = Long.valueOf(recipePara.getSetValue());
            }
            if (recipePara.getParaName().equals("TAPE_THICK")) {
                TAPE_THICK = Long.valueOf(recipePara.getSetValue());
            }
            if (recipePara.getParaName().equals("CH1_HEI1")) {
                CH1_HEI1 = Long.valueOf(recipePara.getSetValue());
            }
            if (recipePara.getParaName().equals("CH2_HEI1")) {
                CH2_HEI1 = Long.valueOf(recipePara.getSetValue());
            }
            if (recipePara.getParaName().equals("CH1_HEI21")) {
                CH1_HEI21 = Long.valueOf(recipePara.getSetValue());
            }
            if (recipePara.getParaName().equals("CH2_HEI21")) {
                CH2_HEI21 = Long.valueOf(recipePara.getSetValue());
            }
        }
        Map elementsMap = new HashMap();
        elementsMap.put("CUT_MODE", cut_mode);
        elementsMap.put("CUT_PROC", cut_proc);
        elementsMap.put("WORK_THICK", WORK_THICK);
        elementsMap.put("TAPE_THICK", TAPE_THICK);
        elementsMap.put("CH1_HEI1", CH1_HEI1);
        elementsMap.put("CH2_HEI1", CH2_HEI1);
        elementsMap.put("CH1_HEI21", CH1_HEI21);
        elementsMap.put("CH2_HEI21", CH2_HEI21);
        elementsMap.put("z1BladeEdge", z1BladeEdge);
        elementsMap.put("z2BladeEdge", z2BladeEdge);
        return elementsMap;
    }

    private long[] getThresholdCalculateValue(Map elementsMap) {
        long[] bladeEdgeCountResults = new long[2];
        if (elementsMap != null) {
            long z1BladeEdge = Long.valueOf(elementsMap.get("z1BladeEdge").toString());
            long z2BladeEdge = Long.valueOf(elementsMap.get("z2BladeEdge").toString());
            long WORK_THICK = Long.valueOf(elementsMap.get("WORK_THICK").toString());
            long TAPE_THICK = Long.valueOf(elementsMap.get("TAPE_THICK").toString());
            long CH1_HEI1 = Long.valueOf(elementsMap.get("CH1_HEI1").toString());
            long CH2_HEI1 = Long.valueOf(elementsMap.get("CH2_HEI1").toString());
            long CH1_HEI21 = Long.valueOf(elementsMap.get("CH1_HEI21").toString());
            long CH2_HEI21 = Long.valueOf(elementsMap.get("CH2_HEI21").toString());
            String cut_mode = String.valueOf(elementsMap.get("CUT_MODE"));
            String cut_proc = String.valueOf(elementsMap.get("CUT_PROC"));
            long z1BladeEdgeCountResult = 0l;
            long z2BladeEdgeCountResult = 0l;
            //管控阀值(svid: z1 1302 z2 1303 ) = 加工物厚度(WORK_THICK code 48) + 胶膜厚度(TAPE_THICK 49) -刀高(高度1【z1 code 57；z2 code 67】 高度2【z1 code 97； z2 code 107】) + 100000nm
            if ("A".equals(cut_mode)) {
                if ("Z1".equalsIgnoreCase(cut_proc)) {
                    z1BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH1_HEI1 + 100000;
                }
                if ("Z2".equalsIgnoreCase(cut_proc)) {
                    z2BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH1_HEI21 + 100000;
                }
                if ("STEP".equalsIgnoreCase(cut_proc)) {
                    z1BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH1_HEI1 + 100000;
                    z2BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH1_HEI21 + 100000;
                }
                if ("DUAL".equalsIgnoreCase(cut_proc)) {
                    z1BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH1_HEI1 + 100000;
                    z2BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH1_HEI1 + 100000;
                }
            }
            if ("SUB_INDEX".equals(cut_mode)) {
                if ("Z1".equalsIgnoreCase(cut_proc)) {
                    z1BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH2_HEI1 + 100000;
                }
                if ("Z2".equalsIgnoreCase(cut_proc)) {
                    z2BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH2_HEI21 + 100000;
                }
                if ("STEP".equalsIgnoreCase(cut_proc)) {
                    z1BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH2_HEI1 + 100000;
                    z2BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH2_HEI21 + 100000;
                }
                if ("DUAL".equalsIgnoreCase(cut_proc)) {
                    z1BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH2_HEI1 + 100000;
                    z2BladeEdgeCountResult = WORK_THICK + TAPE_THICK - CH2_HEI1 + 100000;
                }
            }
            bladeEdgeCountResults[0] = z1BladeEdgeCountResult;
            bladeEdgeCountResults[1] = z2BladeEdgeCountResult;
        }
        return bladeEdgeCountResults;
    }

    private boolean getBladeEdgeCheckResult(long[] bladeEdgeCountResults, Map elementsMap) {
        boolean checkResult = true;
        long z1BladeEdgeCountResult = bladeEdgeCountResults[0];
        long z2BladeEdgeCountResult = bladeEdgeCountResults[1];
        long z1BladeEdge = Long.valueOf(elementsMap.get("z1BladeEdge").toString());
        long z2BladeEdge = Long.valueOf(elementsMap.get("z2BladeEdge").toString());
        String cut_proc = String.valueOf(elementsMap.get("CUT_PROC"));
        if ("Z1".equalsIgnoreCase(cut_proc)) {
            if (z1BladeEdge > z1BladeEdgeCountResult) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片1暴露量：" + z1BladeEdge + "nm,计算值：" + z1BladeEdgeCountResult + "nm");
                checkResult = true;
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片1暴露量不足，剩余量：" + z1BladeEdge + "nm,计算值：" + z1BladeEdgeCountResult + "nm");
                holdDevice();
                checkResult = false;
            }
        }
        if ("Z2".equalsIgnoreCase(cut_proc)) {
            if (z2BladeEdge > z2BladeEdgeCountResult) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片2暴露量：" + z2BladeEdge + "nm,计算值：" + z2BladeEdgeCountResult + "nm");
                checkResult = true;
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片2暴露量不足，剩余量：" + z2BladeEdge + "nm,计算值：" + z2BladeEdgeCountResult + "nm");
                holdDevice();
                checkResult = false;
            }
        }
        if ("STEP".equalsIgnoreCase(cut_proc) || "DUAL".equalsIgnoreCase(cut_proc)) {
            if (z1BladeEdge > z1BladeEdgeCountResult) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片1暴露量：" + z1BladeEdge + "nm,计算值：" + z1BladeEdgeCountResult + "nm");
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片1暴露量不足，剩余量：" + z1BladeEdge + "nm,计算值：" + z1BladeEdgeCountResult + "nm");
                holdDevice();
                checkResult = false;
            }
            if (z2BladeEdge > z2BladeEdgeCountResult) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片2暴露量：" + z2BladeEdge + "nm,计算值：" + z2BladeEdgeCountResult + "nm");

            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "刀片2暴露量不足，剩余量：" + z2BladeEdge + "nm,计算值：" + z2BladeEdgeCountResult + "nm");
                holdDevice();
                checkResult = false;
            }
            checkResult = true;
        }
        return checkResult;
    }


    public Map getBladeThreshold() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        Map bladeEdgeCountResultsMap = new HashMap();
        Recipe recipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
        List<RecipePara> equipRecipeParas = null;
        if (recipe == null) {
            logger.error("工控上不存在模型表中记录的Recipe，ID:" + deviceInfoExt.getRecipeId() + "Name:" + deviceInfoExt.getRecipeName());
            return null;
        } else {
            try {
                equipRecipeParas = (List<RecipePara>) sendS7F5out(recipe.getRecipeName()).get("recipeParaList");
            } catch (UploadRecipeErrorException upe) {
                equipRecipeParas = null;
            }
        }
        if (equipRecipeParas == null) {
            logger.error("从设备上获取recipe" + ppExecName + "的参数信息失败！");
            equipRecipeParas = recipeService.searchRecipeParaByRcpRowId(deviceInfoExt.getRecipeId());
        }
        Map elementsMap = getThresholdCalculateElement(equipRecipeParas);
        long[] bladeEdgeCountResults = getThresholdCalculateValue(elementsMap);
        bladeEdgeCountResultsMap.put("z1LifeThreshold", bladeEdgeCountResults[0]);
        bladeEdgeCountResultsMap.put("z2LifeThreshold", bladeEdgeCountResults[1]);
        sqlSession.close();
        return bladeEdgeCountResultsMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            List listtmp = getNcessaryData();
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
            shortName = (String) listtmp.get(1);
            ppExecName = getDevIdFromEqp(Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "DEVID").get(0).getDeviceVariableId()));
            Map panelMap = new HashMap();
            panelMap.put("EquipStatus", equipStatus);
            panelMap.put("PPExecName", ppExecName);
            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
            panelMap.put("ControlState", controlState);
            changeEquipPanel(panelMap);
            return panelMap;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        } finally {
            sqlSession.close();
        }
    }

    private String getDevIdFromEqp(long DEVID) {
        String devid = "--";
        DataMsgMap data = null;
        List list = new ArrayList();
        list.add(DEVID);
        try {
            data = activeWrapper.sendS2F13out(list, ecFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data != null && data.get("EC") != null) {

            devid = String.valueOf(data.get("EC"));
        }
        return devid;
    }

    private Map getBladeIdFromEqpt() {
        Map map = new HashMap();
        String z1bladeId = "";
        String z2bladeId = "";
        List list = new ArrayList();
        list.add(4922L);
        list.add(4923L);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS2F13out(list, ecFormat);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        if (data != null && data.get("EC") != null) {
            ArrayList listtmp = (ArrayList) data.get("EC");
            z1bladeId = String.valueOf(listtmp.get(0));
            z2bladeId = String.valueOf(listtmp.get(1));
            map.put("Z1", z1bladeId);
            map.put("Z2", z2bladeId);
            logger.info("get blade lot info z1:[" + z1bladeId + "]z2:[" + z2bladeId + "]");
        }
        return map;
    }

    private Boolean checkBladeId() {
        Map map = AxisUtility.getBladeIdFromServer(deviceCode);
        if (map == null || map.isEmpty()) {
            logger.fatal("get blade Id from server failed , return message=[" + map + "]");
            return false;
        }
        String serverValue1 = String.valueOf(map.get("Z1"));
        String serverValue2 = String.valueOf(map.get("Z2"));
        try {
            serverValue1 = serverValue1.substring(serverValue1.length() - 6, serverValue1.length());
            serverValue2 = serverValue2.substring(serverValue2.length() - 6, serverValue2.length());
        } catch (Exception e) {
            //如果收到的是空字符串这里就会报错，做一步处理
            logger.error("Execption occur:" + e.getMessage());
        }
        Map mapClient = getBladeIdFromEqpt();
        if (mapClient == null || mapClient.isEmpty()) {
            logger.fatal("get blade Id from equipment failed , return message=[" + mapClient + "]");
            return false;
        }
        String clientValue1 = String.valueOf(mapClient.get("Z1"));
        String clientValue2 = String.valueOf(mapClient.get("Z2"));
        if (serverValue1.equals(clientValue1)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Z1刀片信息与领用记录一致!刀片编号[" + clientValue1 + "]");
            if (!serverValue2.equals(clientValue2)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Z2刀片信息与领用记录不符!设备在用[" + clientValue2 + "],领用记录[" + serverValue2 + "]");
                return false;
            }
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Z2刀片信息与领用记录一致!刀片编号[" + clientValue2 + "]");
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Z1刀片信息与领用记录不符!设备在用[" + clientValue1 + "],领用记录[" + serverValue1 + "]");
            return false;
        }
        return true;
    }

    // </editor-fold> 
    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = null;
        try {
            equipRecipeParas = (List<RecipePara>) this.sendS7F5out(checkRecipe.getRecipeName()).get("recipeParaList");
        } catch (UploadRecipeErrorException e) {
            logger.error("");
            return;

        }
//        if (!startBladeCheck(equipRecipeParas)) {
//            return;
//        }
        String eventDesc = "";
        //判断recipepara中是否包含要管控的所有参数
        List<RecipePara> recipeParasNotContain = recipeService.recipeParaContainAllTemplatePara(equipRecipeParas, deviceCode);
        if (recipeParasNotContain != null && recipeParasNotContain.size() > 0) {
            this.holdDeviceAndShowDetailInfo();
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
            logger.debug("设备：" + deviceCode + " 开机Check失败");
            for (RecipePara recipePara : recipeParasNotContain) {
                eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名：" + recipePara.getParaName() + ",解析的Recipe参数中不包含此值，请重新上传审核或联系IT解决！";
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
            }
            return;
        }
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                this.holdDeviceAndShowDetailInfo();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                logger.debug("设备：" + deviceCode + " 开机Check失败");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名：" + recipePara.getParaName() + ",其异常设定值为： " + recipePara.getSetValue();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
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


    private boolean startBladeCheck(List<RecipePara> recipeParas) {
        Map elementsMap = getThresholdCalculateElement(recipeParas);
        long[] bladeEdgeCountResults = getThresholdCalculateValue(elementsMap);
        return getBladeEdgeCheckResult(bladeEdgeCountResults, elementsMap);
    }

    @Override
    public String checkEquipStatus() {
        findEqptStatus();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！下载失败！";
        }
        return "0";
    }


    void initRptPara() {

        sendS2F33clear();
        sendS2F35clear();
//        sendS2F33Out(7, 7613, 7615);
//        sendS2F33Out(7, 7602, 7603);
//        sendS2F33Out(7, 1400, 1401);
//        sendS2F35out(7, 7, 7);
    }

    protected void processS6F11KerfCheck(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            long offset1 = 0;
//            long offset1 = data.getSingleNumber("Z1");
//            long offset2 = data.getSingleNumber("Z2");
            long offset2 = 0;

            Map map = new HashMap();
            map.put("msgName", "KerfCheck");
            map.put("Z1", String.valueOf(offset1));
            map.put("Z2", String.valueOf(offset2));
            map.put("deviceCode", deviceCode);
            map.put("type", "add");
//            logger.info("send kerfcheck data:" + map + " offset1:" + offset1 + " offset2:" + offset2);
            List svidList = new ArrayList();
            svidList.add(1412L);
            svidList.add(1413L);
            Map svValue = this.getSpecificSVData(svidList);
            map.put("Z1WIDTH", String.valueOf(svValue.get("1412")));
            map.put("Z2WIDTH", String.valueOf(svValue.get("1413")));
            logger.info("send kerfcheck width:" + map + " offset1:" + offset1 + " offset2:" + offset2 + " Z1WIDTH:" + svValue.get(1412L) + " Z2WIDTH:" + svValue.get(1413L));
            GlobalConstants.C2SSpecificDataQueue.sendMessage(map);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (!kerfCheck) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "需要刀痕检测,设备将自动暂停.");
            pauseDevice();
            kerfCheck = true;
        }
    }

    public Map sendS2F41outPPselect(String recipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put("Port", (byte) 1);
            cpmap.put(CPN_PPID, recipeName);
            Map cpNameMap = new HashMap();
            cpNameMap.put("Port", FormatCode.SECS_ASCII);
            cpNameMap.put(CPN_PPID, FormatCode.SECS_ASCII);
            Map cpValueMp = new HashMap();
            cpValueMp.put((byte) 1, FormatCode.SECS_BINARY);
            cpValueMp.put(recipeName, FormatCode.SECS_ASCII);
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cpmap, cpNameMap, cpValueMp);
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

}
