package cn.tzauto.octopus.secsLayer.equipImpl.asm.fc;

import cn.tfinfo.jcauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tfinfo.jcauto.octopus.biz.device.service.DeviceService;
import cn.tfinfo.jcauto.octopus.biz.monitor.service.MonitorService;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.Recipe;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipePara;
import cn.tfinfo.jcauto.octopus.biz.recipe.service.RecipeService;
import cn.tfinfo.jcauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tfinfo.jcauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tfinfo.jcauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.asm.AsmAD8312RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MsgArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.DataMsgMap;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.ibatis.session.SqlSession;

public class AsmAD8312FCHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(AsmAD8312FCHost.class.getName());
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    public String Datelength;

    public AsmAD8312FCHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPath) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPath);
    }

    public AsmAD8312FCHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPath) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPath);
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted) {
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
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    initRptPara();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null) {
                    if (msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                        processS14F1in(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
                        processS6F11inStripMapUpload(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                        processS6F11EquipStatus(msg);
                    } else if (msg.getMsgSfName().contains("s6f11EquipStatusChange")) {
                        processS6F11EquipStatusChange(msg);
                    } else if (msg.getMsgSfName().equals("s6f11ControlStateChange")) {
                        processS6F11ControlStateChange(msg);
                    } else if (msg.getMsgSfName().equals("s6f11LoginUserChange")) {
                        processS6F11LoginUserChange(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                        this.processS5F1in(msg);
                    } else {
                        System.out.println("A message in queue with tag = " + msg.getMsgSfName()
                                + " which I do not want to process! ");
                    }
                }
            } catch (Exception e) {
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
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f11inStripMapUpload")) {
//                processS6F11inStripMapUpload(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("s6f11EquipStatusChange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equals("s6f11ControlStateChange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equals("s6f11LoginUserChange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equals("s6f11PPExecName")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data); 
                this.inputMsgQueue.put(data);
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

    // <editor-fold defaultstate="collapsed" desc="processS1FXin Code">
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
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
            data = activeWrapper.sendAwaitMessage(s1f3out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);

        }
        if (data == null || ((SecsItem) data.get("RESULT")).getData() == null) {

            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0).toString()), deviceType);
        ppExecName = (String) listtmp.get(1);
        ppExecName = ppExecName.replace(".rcp", "");
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="processS6FXin Code">
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
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
            if (equipStatus.equalsIgnoreCase("run") && holdFlag) {
                holdDevice();
            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("ready")) {
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo();
                } else {
                    //1、获取设备需要校验的信息类型,
                    String startCheckMod = deviceInfoExt.getStartCheckMod();
                    boolean hasGoldRecipe = true;
                    if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                        holdDeviceAndShowDetailInfo();
                        UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                        return;
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
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            releaseDevice();
                        }
                        logger.info("设备[" + deviceCode + "]的开机检查模式为:" + startCheckMod);
                        if (startCheckMod.contains("B")) {
                            startSVcheckPass = false;
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行开机前SVCheck");
                            startSVcheck();
                        }
                        if (startCheckMod.contains("A")) {
                            //首先判断下载的Recipe类型
                            //1、如果下载的是Unique版本，那么执行完全比较
                            String downloadRcpVersionType = downLoadRecipe.getVersionType();
                            if (false) {
                                UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                                //这里要把设备上recipe的后缀加上，否则获取不到
                                downLoadRecipe.setRecipeName(downLoadRecipe.getRecipeName() + ".rcp");
                                this.startCheckRecipePara(downLoadRecipe, "abs");
                            } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                                UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                                if (!hasGoldRecipe) {
                                    UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在[" + ppExecName + "]的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                    //不允许开机
                                    this.holdDeviceAndShowDetailInfo();
                                } else {
                                    UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                    //这里要把设备上recipe的后缀加上，否则获取不到
                                    downLoadGoldRecipe.get(0).setRecipeName(downLoadGoldRecipe.get(0).getRecipeName() + ".rcp");
                                    this.startCheckRecipePara(downLoadGoldRecipe.get(0), startCheckMod);
                                }
                            }
                        } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
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

    protected void processS6F11ControlStateChange(DataMsgMap data) {
        //回复s6f11消息
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        long ceid = 0l;
        long reportID = 0l;
        long controlStateTmp = 0l;
        try {
            out.setTransactionId(data.getTransactionId());
            ceid = data.getSingleNumber("CollEventID");
            reportID = data.getSingleNumber("ReportID");
            controlStateTmp = data.getSingleNumber("ControlState");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 1 && reportID == 1) {
            Map panelMap = new HashMap();
            if (controlStateTmp == 0) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备状态切换到OFF-LINE");
            }
            if (controlStateTmp == 1) {
                controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换到Local");
            }
            if (controlStateTmp == 2) {
                controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
                panelMap.put("ControlState", controlState);
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备控制状态切换到Remote");
            }
            changeEquipPanel(panelMap);
        }
    }

    protected void processS6F11LoginUserChange(DataMsgMap data) {
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        long ceid = 0l;
        long reportID = 0l;
        String loginUserName = "";
        try {
            out.setTransactionId(data.getTransactionId());
            ceid = data.getSingleNumber("CollEventID");
            reportID = data.getSingleNumber("ReportId");
            loginUserName = ((SecsItem) data.get("UserLoginName")).getData().toString();
            if (ceid == 9L) {
                Map map = new HashMap();
                map.put("PPExecName", loginUserName);
                changeEquipPanel(map);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 120 && reportID == 120) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "登陆用户变更，当前登陆用户：" + loginUserName);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName + ".rcp");
        s7f1out.put("Length", length);
        DataMsgMap data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.debug("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt[0]);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", targetRecipeName + ".rcp");
        s7f3out.put("Processprogram", secsItem);
        try {
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName+".rcp");
        recipeName = recipeName.replace(".rcp", "");
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = null;
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
            TransferUtil.setPPBody(ppbody, recipeType, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析      
            recipeParaList = AsmAD8312RecipeUtil.transferRcpFromDB(recipePath, deviceType);
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        if (!recipeName.contains(".rcp")) {
            recipeName = recipeName + ".rcp";
        }
        s7f17out.put("ProcessprogramID", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            if (ackc7[0] == 0) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }
    
    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s7f19out.setTransactionId(transactionId);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f19out);
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
            ArrayList list1 = new ArrayList();
            for (int i = 0; i < listtmp.size(); i++) {
                list1.add(listtmp.get(i).toString().replace(".rcp", ""));
            }
            resultMap.put("eppd", list1);
        }
        return resultMap;
    }
// </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="processS14FXin Code">

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="sendS1FXout Code">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="sendS2FXout Code">
    //释放机台
    @Override
    public Map releaseDevice() {
        Map map = new HashMap();// this.sendS2f41Cmd("START");
//        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
        this.setAlarmState(0);
        holdFlag = false;
//        }
        map.put("HCACK", 0);
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName + ".rcp");
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }

    // </editor-fold>
    /*
     * (non-Javadoc) It only copies field member values except Mli.
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() {
        AsmAD8312FCHost newEquip = new AsmAD8312FCHost(deviceId, this.deviceCode,
                this.smlFilePath, this.localIPAddress,
                this.localTCPPort, this.remoteIPAddress,
                this.remoteTCPPort, this.connectMode,
                this.protocolType, this.deviceType, this.deviceCode, recipeType, this.iconPath);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.clear();
        this.setIsRestarting(isRestarting);
        this.setStartedDate(0);
        return newEquip;
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void initRptPara() {
        sendS2F37outAll();
        sendS2F37outClose(23L);
        sendS2F37outClose(24L);
        sendS2F37outClose(132L);
        sendS5F3out(true);
        sendStatus2Server(equipStatus);
    }

    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        logger.info("START CHECK: BEGIN" + new Date());
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        logger.info("START CHECK: ready to upload recipe:" + new Date());
        List<RecipePara> equipRecipeParas = (List<RecipePara>) this.sendS7F5out(checkRecipe.getRecipeName()).get("recipeParaList");
        if (equipRecipeParas == null || equipRecipeParas.isEmpty()) {
            if (type.equalsIgnoreCase("AB") && startSVcheckPass) {
                UiLogUtil.appendLog2EventTab(deviceCode, "开机SVcheck已通过,RecipePara check获取recipePara失败,但默认通过.");
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                sqlSession.close();
                return;
            }
            UiLogUtil.appendLog2EventTab(deviceCode, "从设备获取Recipe数据失败,请确认设备上是否存在Recipe[" + checkRecipe.getRecipeName() + "]");
            sqlSession.close();
            return;
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
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Recipe参数检查未通过!");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为[" + recipePara.getParaCode() + "],参数名:[" + recipePara.getParaName() + "]其异常设定值为[" + recipePara.getSetValue() + "],默认值为[" + recipePara.getDefValue() + "]"
                            + "其最小设定值为[" + recipePara.getMinValue() + "],其最大设定值为[" + recipePara.getMaxValue() + "]";
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Recipe参数检查通过！");
                eventDesc = "设备：" + deviceCode + " 开机Recipe参数没有异常";
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

    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }
}
