package cn.tzauto.octopus.secsLayer.equipImpl.esec.db;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import cn.tzauto.octopus.secsLayer.util.WaferTransferUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.math.BigDecimal;
import java.util.*;

public class EsecDB2100Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(EsecDB2100Host.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    //wafermapping相关属性
    protected long downFlatNotchLocation;
    protected long upFlatNotchLocation;

    public EsecDB2100Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
       lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        StripMapUpCeid = 15339L;
        EquipStateChangeCeid = 40L;
        CPN_PPID = "PPNAME";
    }


    @Override
    public Object clone() {
        EsecDB2100Host newEquip = new EsecDB2100Host(deviceId,
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
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }

                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
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
                   super.processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                    processS6F11inStripMapUpload(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11EquipStatusChange")) {
                    processS6F11EquipStatusChange(msg);
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
            }  else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
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
            }else if (tagName.equalsIgnoreCase("f0")) {
                sendSnF0();
                logger.info("tagName===========>" + tagName);
            }  else {
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
            List<Long> svidlist = new ArrayList<>();
            svidlist.add(6L);
            svidlist.add(8L);
            sendS2F33out(3L, 3L, svidlist);
            sendS2F35out(3L, 3L, 3L);
            sendS2F37out(3L);
            //发送s2f33
            String ack = "";
            long rptid = 1001l;
            long vid = 269352993l;
            long ceid = 15338l;
            sendS2F33out(1001l, vid);//15339


            sendS2F33out(1002l, vid);//15338


            sendS2F33out(1003l, 269352995l);//15328


            //SEND S2F35

            sendS2F35out(15339l, 1001l);//15339 1001

            sendS2F35out(15338l, 1002l);//15339 1001


            sendS2F35out(15328l, 1003l);//15339 1001

            List list = new ArrayList();
            list.add(2031L);
            list.add(2009L);
            list.add(2028L);
            sendS2F33out(3255L, 3255L, list);
            sendS2F35out(3255L, 3255L, 3255L);
            //SEND S2F37

            sendS2F37outAll();

            sendS2F37outClose(15650L);
            sendS2F37outClose(15652L);
            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
//            java.util.logging.Logger.getLogger(EsecDB2100Host.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception:", ex);
            return "0";
        }
    }



    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        equipStatus = ACKDescription.descriptionStatus(listtmp.get(0).toString(), deviceType);
        ppExecName = (String) listtmp.get(1);
        ppExecName = ppExecName.replaceAll(".dbrcp", "");
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        // sendS2F15outLearnDevice(151126402L, "disabled");
        return panelMap;
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect(String recipeName) {
        byte hcack = -1;
        try {
            Map cp = new HashMap();
            cp.put(CPN_PPID, recipeName + ".dbrcp");
            Map cpName = new HashMap();
            cpName.put(CPN_PPID, SecsFormatValue.SECS_ASCII);
            Map cpValue = new HashMap();
            cpValue.put(recipeName + ".dbrcp", SecsFormatValue.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add(CPN_PPID);
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cp, cpName, cpValue);

            hcack = (byte) data.get("HCACK");
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            if (cmdMap.get("HCACK").toString().equals("0") || (byte) cmdMap.get("HCACK") == 4) {
                logger.info("锁机成功！");
                this.setAlarmState(2);
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            ppExecName = ppExecName.replace(".dbrcp", "");
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
            String busniessMod = deviceInfoExt.getBusinessMod();
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑&&
//            if (dataReady && equipStatus.equalsIgnoreCase("run") && preEquipStatus.equalsIgnoreCase("stopped RDY")) {
            if (dataReady && equipStatus.equalsIgnoreCase("run") && preEquipStatus.equalsIgnoreCase("stopped RDY") ) {
//                //TODO 校验2D的开关是否已经开启，若关闭弹窗显示
//                List<String> svlist = new ArrayList<>();
//                svlist.add("252968976");//2D开关
//                Map svValue = this.getSpecificSVData(svlist);
//                if (!svValue.get("252968976").equals("41")) {
//                    String dateStr = GlobalConstants.dateFormat.format(new Date());
//                    this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + "2D Mark has already been closed!!");
//                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "2D已被关闭！");
//                }
                if (AxisUtility.isEngineerMode(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                    sqlSession.close();
                    return;
                }
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo("Equipment has been held,you can see the detail log from Host");
                } else {
                    //1、获取设备需要校验的信息类型,
                    String startCheckMod = deviceInfoExt.getStartCheckMod();
                    boolean hasGoldRecipe = true;
                    if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                        holdDeviceAndShowDetailInfo();
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                    }
                    //查询trackin时的recipe和GoldRecipe
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));

                    //查询客户端数据库是否存在GoldRecipe
                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                        hasGoldRecipe = false;
                    }

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
                            holdDeviceAndShowDetailInfo(" There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            this.setAlarmState(0);
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        //如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                        if (!hasGoldRecipe) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                            //不允许开机
                            this.holdDeviceAndShowDetailInfo("The recipePara error,equipment has been locked!");
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                            this.startCheckRecipePara(downLoadGoldRecipe.get(0));
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
    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = recipeParaBD2Str(getRecipeParasByECSV());
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


    public void sendS2F15outLearnDevice(long ecid, Object ecv, short ecvFormat) {
        DataMsgMap out = new DataMsgMap("S2F15OUT", activeWrapper.getDeviceId());
        out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        MsgSection secsItem = new MsgSection();
        List list = new ArrayList();
        MsgSection secsItemECID = new MsgSection(ecid, SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER);
        MsgSection secsItemEcv = new MsgSection(ecv, ecvFormat);
        list.add(secsItemECID);
        list.add(ecv);
        secsItem.setData(list);
        secsItem.setFormatCode(SecsFormatValue.SECS_LIST);
        out.put("S2F15OUT", secsItem);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备Learn Device，设备进行锁机，请报修MES并进行数据BuyOff！");
        try {
            activeWrapper.sendAwaitMessage(out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public void processS2f16inLearnDevice(DataMsgMap in) {
        if (in == null) {
            return;
        }
        System.out.println("--------Received s2f16in---------");
        byte[] value = (byte[]) ((MsgSection) in.get("EAC")).getData();
        System.out.println();
        System.out.println("EAC = " + ((value == null) ? "" : value[0]));
    }

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        logger.info("Request send " + targetRecipeName + " to Device " + deviceCode);
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
        logger.info("length= " + length );
        try {
            data = activeWrapper.sendS7F1out(targetRecipeName+ ".dbrcp", length, lengthFormat);
            byte ppgnt = (byte) data.get("PPGNT");

            logger.info("Request send ppid= " + targetRecipeName +";data:"+ data +";ppgnt:"+ ppgnt + " to Device " + deviceCode);

            resultMap.put("ppgnt", ppgnt);
            resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", e.getMessage());
        }
        logger.info("result: " + resultMap);
        return resultMap;


    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        targetRecipeName = targetRecipeName.replace("@", "/");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName+ ".dbrcp", ppbody, SecsFormatValue.SECS_BINARY);
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
        recipeName = recipeName.replace(".dbrcp", "");
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);

        List<RecipePara> recipeParaList = null;
        try{
            byte[] ppbody = (byte[]) getPPBODY(recipeName + ".dbrcp");
            TransferUtil.setPPBody(ppbody, recipeType, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析
            recipeParaList = getRecipeParasByECSV();
            //设备发过来的参数部分为科学计数法，这里转为一般的
            recipeParaList = this.recipeParaBD2Str(recipeParaList);

        } catch (UploadRecipeErrorException e) {
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "上传请求被设备拒绝，请查看设备状态。");
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeNameMapping", (Object)null);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    /**
     * 在recipe被选中后删除原有recipe需要延迟删除
     *
     * @param recipeName
     * @return
     */
//    @SuppressWarnings("unchecked")
//    @Override
//    public Map sendS7F17out(String recipeName) {
//        Map resultMap = new HashMap();
//        try {
//            //先检查是否成功执行切换recipe，
//            if (ppselectFlag) {
////                int i = 0;
////                //超过四次直接执行，不管成功与否
////                while (i < 4) {
////                    //切换recipe完成,执行删除命令，，另建线程删除recipe文件
////                    if (ppselectDoneFlag) {
////
////                        break;
////                    }
////                    Thread.sleep(1000);
////                    i++;
////                }
//                //todo切换之前recipe时
//                logger.info(deviceCode + "=====正执行切换recipe动作！现延迟删除[" + recipeName + "]");
//                Thread thread = new Thread(new RunnableImpl(recipeName));
//                thread.start();
//                //造假的回复信息
//                resultMap.put("msgType", "s7f18");
//                resultMap.put("deviceCode", deviceCode);
//                resultMap.put("recipeName", recipeName);
//                resultMap.put("ACKC7", 0);
//                resultMap.put("Description", "Delete Later!");
//            } else {
//                //没有执行选中程序的删除recipe不需要延迟执行，不行
//                resultMap = sendS7F17outReal(recipeName);
//            }
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        } finally {
//            return resultMap;
//        }
//    }

    @Override
    public Map sendS7F17out(String recipeName) {
        return sendS7F17outReal(recipeName);
    }


    class RunnableImpl implements Runnable {

        String recipeNameo;

        public RunnableImpl() {
        }

        public RunnableImpl(String recipeNameOther) {
            this.recipeNameo = recipeNameOther;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(30 * 1000);
                logger.info("延迟删除线程阻塞结束,开始执行删除操作,RECIPE为:[" + recipeNameo + "]");
                Map resultMap = sendS7F17outReal(recipeNameo);
                logger.info("执行删除完毕，RECIPE为：[" + recipeNameo + "]"
                        + "删除结果为：" + resultMap.get("Description"));
            } catch (Exception ex) {
                logger.info(ex);
            }
        }

    }

    @SuppressWarnings("unchecked")
    public Map sendS7F17outReal(String recipeName) {
        return super.sendS7F17out(recipeName + ".dbrcp");
    }

    @Override
    public Map sendS7F19out() {
        logger.info("Request sends7f19 to Device: " + deviceCode);
        Map resultMap = super.sendS7F19out();
        List eppd = (ArrayList) resultMap.get("eppd");
        ArrayList recipeNames = new ArrayList();
        for (int i = 0; i < eppd.size(); i++) {
            recipeNames.add(eppd.get(i).toString().replace(".dbrcp", ""));
        }
        resultMap.put("eppd", recipeNames);
        return resultMap;

    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S12FX Code"> 

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
            //String NullBinCodeValue = (String)((MsgSection) dataMsgMap.get("NullBinCodeValue")).getData();
            //byte[] ProcessAxis = ((byte[]) ((MsgSection) dataMsgMap.get("ProcessAxis")).getData());
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

    public Map processS12F9inold(DataMsgMap DataMsgMap) {
        try {
            String MaterialID = (String) ((MsgSection) DataMsgMap.get("MaterialID")).getData();
            byte[] IDTYP = ((byte[]) ((MsgSection) DataMsgMap.get("IDTYP")).getData());
            int[] STRPxSTRPy = (int[]) ((MsgSection) DataMsgMap.get("STRPxSTRPy")).getData();
            String binList = (String) ((MsgSection) DataMsgMap.get("BinList")).getData();
            UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "机台上传WaferMapping成功！WaferId：[" + MaterialID + "]");
            //上传WaferMapping,
            String _uploadWaferMappingRow = uploadWaferMappingRow;
            String _uploadWaferMappingCol = uploadWaferMappingCol;
            if (this.deviceType.contains("ESEC")) {
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
            activeWrapper.respondMessage(s12f10out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }

    // </editor-fold>


    private List<RecipePara> recipeParaBD2Str(List<RecipePara> recipeParas) {
        if (recipeParas != null && recipeParas.size() > 0) {
            for (RecipePara recipePara : recipeParas) {
                String value = recipePara.getSetValue();
                if (value.contains("E")) {
                    BigDecimal bd = new BigDecimal(value);
                    value = bd.toPlainString();
                    recipePara.setSetValue(value);
                }
            }
        }
        return recipeParas;
    }

    @Override
    public void sendUphData2Server() {
    }

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }

}
