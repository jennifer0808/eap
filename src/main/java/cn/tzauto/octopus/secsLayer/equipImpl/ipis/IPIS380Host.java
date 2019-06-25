package cn.tzauto.octopus.secsLayer.equipImpl.ipis;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.alarm.service.AutoAlter;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.ipis.Ipis380RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.secsLayer.util.WaferTransferUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.math.BigDecimal;
import java.util.*;

public class IPIS380Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(IPIS380Host.class.getName());

    public IPIS380Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
//        RCMD_PPSELECT = "START";
//        StripMapUpCeid = 15339L;
//        EquipStateChangeCeid = 1010;
    }


    @Override
    public Object clone() {
        IPIS380Host newEquip = new IPIS380Host(deviceId,
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
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                    initRptPara();
                }
                //设备在下一个可能停止的点才能停止
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
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
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
//                processS6F11inStripMapUpload(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
//                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f11EquipStatusChange")) {
                this.inputMsgQueue.put(data);
//                processS6F11EquipStatusChange(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else if (tagName.equalsIgnoreCase("s12f1in")) {
                processS12F1in(data);
            } else if (tagName.equalsIgnoreCase("s12f3in")) {
                processS12F3in(data);
            } else if (tagName.equalsIgnoreCase("s12f5in")) {
                processS12F5in(data);
            } else if (tagName.equalsIgnoreCase("s12f7in")) {
                processS12F7in(data);
            } else if (tagName.equalsIgnoreCase("s12f9in")) {
                processS12F9in(data);
            } else if (tagName.equalsIgnoreCase("s12f11in")) {
                processS12F11in(data);
            } else if (tagName.equalsIgnoreCase("s12f13in")) {
                processS12F13in(data);
            } else if (tagName.equalsIgnoreCase("s12f15in")) {
                processS12F15in(data);
            } else if (tagName.equalsIgnoreCase("s12f17in")) {
                processS12F17in(data);
            } else if (tagName.equalsIgnoreCase("s12f19in")) {
                processS12F19in(data);
            } else if (tagName.equalsIgnoreCase("s12f67in")) {
                processS12F67in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    public String initRptPara() {
        try {
            logger.debug("initRptPara+++++++++++++++++++");
            this.sendS2F33clear();
            this.sendS2F35clear();
            //重新定义Learn Device事件
            List<Long> stateChangeSvidlist = new ArrayList<>();
            stateChangeSvidlist.add(2015L);
            sendS2F33out(1010L, 1010L, stateChangeSvidlist);
            sendS2F35out(1010L, 1010L, 1010L);
//            sendS2F37out(1010L);

            List<Long> rcpUpdatedSvidlist = new ArrayList<>();
            rcpUpdatedSvidlist.add(2020L);
            sendS2F33out(1008L, 1008L, rcpUpdatedSvidlist);
            sendS2F35out(1008L, 1008L, 1008L);

            List<Long> lotStartedSvidlist = new ArrayList<>();
            lotStartedSvidlist.add(2016L);
            lotStartedSvidlist.add(2020L);
            lotStartedSvidlist.add(2032L);
            lotStartedSvidlist.add(2033L);
            lotStartedSvidlist.add(2034L);
            lotStartedSvidlist.add(2035L);
            sendS2F33out(1011L, 1011L, lotStartedSvidlist);
            sendS2F35out(1011L, 1011L, 1011L);

            List<Long> terMsgSvidlist = new ArrayList<>();
            terMsgSvidlist.add(2012L);
            sendS2F33out(1012L, 1012L, terMsgSvidlist);
            sendS2F35out(1012L, 1012L, 1012L);

            List<Long> rcpSelectedSvidlist = new ArrayList<>();
            rcpUpdatedSvidlist.add(2020L);
            sendS2F33out(1014L, 1014L, rcpUpdatedSvidlist);
            sendS2F35out(1014L, 1014L, 1014L);

            List<Long> svidlist1 = new ArrayList<>();
            svidlist1.add(2026L);
            sendS2F33out(1025L, 1025L, svidlist1);
            sendS2F35out(1025L, 1025L, 1025L);

            List<Long> svidlist2 = new ArrayList<>();
            svidlist2.add(2027L);
            sendS2F33out(1026L, 1026L, svidlist2);
            sendS2F35out(1026L, 1026L, 1026L);

            List<Long> svidlist3 = new ArrayList<>();
            svidlist3.add(2028L);
            sendS2F33out(1027L, 1027L, svidlist3);
            sendS2F35out(1027L, 1027L, 1027L);

            List<Long> svidlist4 = new ArrayList<>();
            svidlist4.add(2029L);
            sendS2F33out(1028L, 1028L, svidlist4);
            sendS2F35out(1028L, 1028L, 1028L);

            List<Long> svidlist5 = new ArrayList<>();
            svidlist5.add(2030L);
            sendS2F33out(1029L, 1029L, svidlist5);
            sendS2F35out(1029L, 1029L, 1029L);

            List<Long> lotEndSvidlist = new ArrayList<>();
            lotEndSvidlist.add(2016L);
            lotEndSvidlist.add(2017L);
            lotEndSvidlist.add(2018L);
            lotEndSvidlist.add(2019L);
            lotEndSvidlist.add(2020L);
            lotEndSvidlist.add(2021L);
            lotEndSvidlist.add(2022L);
            lotEndSvidlist.add(2023L);
            lotEndSvidlist.add(2024L);
            lotEndSvidlist.add(2025L);
            lotEndSvidlist.add(2031L);
            sendS2F33out(1030L, 1030L, lotEndSvidlist);
            sendS2F35out(1030L, 1030L, 1030L);

            sendS2F37outAll();
            return "1";
        } catch (Exception ex) {
//            java.util.logging.Logger.getLogger(EsecDB2100Host.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception:", ex);
            return "0";
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @Override
    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        if (listtmp != null && !listtmp.isEmpty()) {
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
            ppExecName = String.valueOf(listtmp.get(1)).replace(".job", "");
            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        if (recipeName.length() != 40 && recipeName.length() < 40) {
            int spaceLength = 40 - recipeName.length();
            for (int i = 0; i < spaceLength; i++) {
                recipeName += " ";
            }
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put(CPN_PPID, recipeName);
            cpmap.put("LOTID", "");
            cpmap.put("PARTNO", "");

            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put(CPN_PPID, FormatCode.SECS_ASCII);
            cpNameFromatMap.put("LOTID", FormatCode.SECS_ASCII);
            cpNameFromatMap.put("PARTNO", FormatCode.SECS_ASCII);

            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put(recipeName, FormatCode.SECS_ASCII);
            cpValueFromatMap.put("", FormatCode.SECS_ASCII);

            List cplist = new ArrayList();
            cplist.add("LOTID");
            cplist.add("PARTNO");
            cplist.add(CPN_PPID);
            DataMsgMap data = activeWrapper.sendS2F41out("START", cplist, cpmap, cpNameFromatMap, cpValueFromatMap);
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
                sendStatus2Server("LOCK");
                holdFlag = true;
            }
            return map;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = -1;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            //TODO 根据ceid分发处理事件
            if (ceid == 1001 || ceid == 1002 || ceid == 1003 || ceid == 1008
                    || ceid == 1010 || ceid == 1014 || ceid == 1016) {
                processS6F11EquipStatusChange(data);
            }
//            else if (ceid == 1010 || ceid == 1016) {
//                processS6F11EquipRun(data);
//            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

//    @Override
//    protected void processS6F11EquipStatusChange(DataMsgMap data) {
//        long ceid = 0L;
//        try {
//            ceid = (long) data.get("CEID");
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        //将设备的当前状态显示在界面上
//        findDeviceRecipe();
//
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        RecipeService recipeService = new RecipeService(sqlSession);
//
//        try {
//            //从数据库中获取当前设备模型信息
//            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//            boolean dataReady = false;
//            //判断当前执行程序是否是清模程序 Y代表清模程序
//            boolean isCleanRecipe = false;
//            List<Recipe> cleanRecipes = recipeService.searchRecipeByRcpType(ppExecName, "Y");
//            if (cleanRecipes != null && cleanRecipes.size() > 1) {
//                isCleanRecipe = true;
//            }
//            // 更新设备模型
//            if (deviceInfoExt == null) {
//                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
//                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
//            } else {
//                deviceInfoExt.setDeviceStatus(equipStatus);
//                deviceService.modifyDeviceInfoExt(deviceInfoExt);
//                sqlSession.commit();
//                dataReady = true;
//            }
//
//            //保存到设备操作记录数据库
//            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
//            sqlSession.commit();
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
//    }
//
//    protected void processS6F11EquipRun(DataMsgMap data) {
//        long ceid = 0L;
//        try {
//            ceid = (long) data.get("CEID");
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        //将设备的当前状态显示在界面上
//        findDeviceRecipe();
//
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        RecipeService recipeService = new RecipeService(sqlSession);
//
//        try {
//            //从数据库中获取当前设备模型信息
//            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//            boolean dataReady = false;
//            //判断当前执行程序是否是清模程序 Y代表清模程序
//            boolean isCleanRecipe = false;
//            List<Recipe> cleanRecipes = recipeService.searchRecipeByRcpType(ppExecName, "Y");
//            if (cleanRecipes != null && cleanRecipes.size() > 1) {
//                isCleanRecipe = true;
//            }
//            // 更新设备模型
//            if (deviceInfoExt == null) {
//                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
//                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
//            } else {
//                deviceInfoExt.setDeviceStatus(equipStatus);
//                deviceService.modifyDeviceInfoExt(deviceInfoExt);
//                sqlSession.commit();
//                dataReady = true;
//            }
//
//            //保存到设备操作记录数据库
//            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
//            sqlSession.commit();
//
//            boolean checkResult = false;
//            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
//            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
//                //1、获取设备需要校验的信息类型,
//                String startCheckMod = deviceInfoExt.getStartCheckMod();
//                boolean hasGoldRecipe = true;
//                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
////                    holdDeviceAndShowDetailInfo();
//                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，请改机!");
//                    return;
//                }
//                //查询trackin时的recipe和GoldRecipe
//                Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
//
//                //查询客户端数据库是否存在GoldRecipe
//                if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
//                    hasGoldRecipe = false;
//                }
//
//                //首先从服务端获取机台是否处于锁机状态
//                //如果设备应该是锁机，那么首先发送锁机命令给机台
//                if (this.checkLockFlagFromServerByWS(deviceCode)) {
//                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    holdDeviceAndShowDetailInfo();
//                } else {
//                    //根据检查模式执行开机检查逻辑
//                    //1、A1-检查recipe名称是否一致
//                    //2、A-检查recipe名称和参数
//                    //3、B-检查SV
//                    //4、AB都检查
//
//                    if (startCheckMod != null && !"".equals(startCheckMod)) {
//                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
//                        if (!checkResult) {
//                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
//                            //不允许开机
//                            holdDeviceAndShowDetailInfo();
//                        } else {
//                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序一致，核对通过！");
//                        }
//                    }
//                    if (checkResult) {
//                        if ("A".equals(startCheckMod)) {
//                            //首先判断下载的Recipe类型
//                            //1、如果下载的是Unique版本，那么执行完全比较
//                            String downloadRcpVersionType = downLoadRecipe.getVersionType();
//                            if ("Unique".equals(downloadRcpVersionType)) {
//                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
//                                this.startCheckRecipePara(downLoadRecipe, "abs");
//                            } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
//                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
//                                if (!hasGoldRecipe) {
//                                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在[" + ppExecName + "]的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                                    //不允许开机
//                                    this.holdDeviceAndShowDetailInfo();
//                                } else {
//                                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "[" + ppExecName + "]开始WI参数Check");
//                                    this.startCheckRecipePara(downLoadGoldRecipe.get(0));
//                                }
//                            }
//                        } else if ("B".equals(startCheckMod)) {
//                            startSVcheckPass = false;
//                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
//                            startSVcheck();
//                        } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
//                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
//    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        if (targetRecipeName.length() != 40 && targetRecipeName.length() < 40) {
            int spaceLength = 40 - targetRecipeName.length();
            for (int i = 0; i < spaceLength; i++) {
                targetRecipeName += " ";
            }
        }
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
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        if (targetRecipeName.length() != 40 && targetRecipeName.length() < 40) {
            int spaceLength = 40 - targetRecipeName.length();
            for (int i = 0; i < spaceLength; i++) {
                targetRecipeName += " ";
            }
        }
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

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
        try {
            Map paraMap = Ipis380RecipeUtil.transferFromFile(recipePath);
            recipeParaList = Ipis380RecipeUtil.transferFromDB(paraMap, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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

    @Override
    public Map sendS7F17out(String recipeName) {
        if (recipeName.length() != 40 && recipeName.length() < 40) {
            int spaceLength = 40 - recipeName.length();
            for (int i = 0; i < spaceLength; i++) {
                recipeName += " ";
            }
        }
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

    // </editor-fold>
}
