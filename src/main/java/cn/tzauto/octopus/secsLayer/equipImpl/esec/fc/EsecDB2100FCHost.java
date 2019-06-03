package cn.tzauto.octopus.secsLayer.equipImpl.esec.fc;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;


public class EsecDB2100FCHost extends EquipHost {


    private static final Logger logger = Logger.getLogger(EsecDB2100FCHost.class);


    public EsecDB2100FCHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        EquipStateChangeCeid = 3255;
        StripMapUpCeid = 15339;
        CPN_PPID = "PPNAME";
    }


    @Override
    public Object clone() {
        EsecDB2100FCHost newEquip = new EsecDB2100FCHost(deviceId,
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
                    EsecDB2100FCHost.sleep(200);
                }
                if (this.getCommState() != EsecDB2100FCHost.COMMUNICATING) {
                    sendS1F13out();
                }
                if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                }
                if (rptDefineNum < 1) {
//                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    initRptPara();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11inStripMapUpload")) {
//                    if (msg.get("CollEventID") != null) {
//                        long ceid = msg.getSingleNumber("CollEventID");
//                        if (ceid == 0) {
                    processS6F11inStripMapUpload(msg);
//                        } else {
//                            processS6F11in(msg);
//                        }
//                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
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
            long transactionId = data.getTransactionId();
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
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
//            sendS2F33clear();
//            sendS2F35clear();
            //发送s2f33
            String ack = "";
            long rptid = 1001l;
            long vid = 269352993l;
            long ceid = 15338l;
            sendS2F33out(1001l, 269352993l);//15339


            sendS2F33out(1002l, 269352993l);//15338


            sendS2F33out(1003l, 269352995l);//15328


            //SEND S2F35

            sendS2F35out(100L, 15339l, 1001l);//15339 1001


            sendS2F35out(100L, 15338l, 1002l);//15339 1001

            sendS2F35out(100L, 15328l, 1003l);//15339 1001

//            sendS2F37out(4l);
            List list = new ArrayList();
            list.add(2031L);
            list.add(2009L);
            list.add(2028L);
//            sendS2F33out(3255L, 2031L, 2009L, 2028L);
            sendS2F33out(3255L, 3255L, list);
            sendS2F35out(3255L, 3255L, 3255L);
//
//            sendS2F33out(8L, 8L);
//            sendS2F35out(3L, 3L, 8L);
            //SEND S2F37

            sendS2F37outAll();

            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(EsecDB2100FCHost.class.getName()).log(Level.SEVERE, null, ex);
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
        //由于TOWA的controlstate暂时无法获取，所以暂不处理
//        if (!deviceType.contains("TOWA")) {
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        panelMap.put("ControlState", controlState);
//        }
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold> 


    @Override
    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect(String recipeName) {
        byte[] hcack = new byte[1];
        try {
            Map cp = new HashMap();
            cp.put(CPN_PPID, recipeName + ".dbrcp");
            Map cpName = new HashMap();
            cpName.put(CPN_PPID, FormatCode.SECS_ASCII);
            Map cpValue = new HashMap();
            cpValue.put(recipeName + ".dbrcp", FormatCode.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add(CPN_PPID);
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cp, cpName, cpValue);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
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

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
//            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            ppExecName = ppExecName.replace(".dbrcp", "");
            preEquipStatus = equipStatus;
            findDeviceRecipe();
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
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                sqlSession.close();
                return;
            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
                //TODO 校验2D的开关是否已经开启，若关闭弹窗显示
//                List<String> svlist = new ArrayList<>();
//                svlist.add("252968976");//2D开关
//                Map svValue = this.getSpecificSVData(svlist);
//                if(!svValue.get("252968976").equals("41")){
//                    String dateStr = GlobalConstants.dateFormat.format(new Date());
//                    this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + "2D Mark has already been closed!!");
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "2D已被关闭！");
//                }
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            setAlarmState(0);
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if (false) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
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
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = super.sendS7F1out(localFilePath, targetRecipeName + ".dbrcp");
        resultMap.put("ppid", targetRecipeName);
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map resultMap = super.sendS7F3out(localRecipeFilePath, targetRecipeName + ".dbrcp");
        resultMap.put("ppid", targetRecipeName);
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F5out(recipeName + ".dbrcp");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = null;
        if (data != null && !data.isEmpty()) {
            byte[] ppbody = (byte[]) data.get("PPBODY");
            TransferUtil.setPPBody(ppbody, 1, recipePath);
            logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
            //Recipe解析      
            recipeParaList = getRecipeParasByECSV();
            //设备发过来的参数部分为科学计数法，这里转为一般的
            recipeParaList = recipeParaBD2Str(recipeParaList);
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

    @Override
    public Map sendS7F19out() {
        Map resultMap = super.sendS7F19out();
        ArrayList recipeList = (ArrayList) resultMap.get("eppd");
        if (recipeList.size() != 0) {
            ArrayList list1 = new ArrayList();
            for (int i = 0; i < recipeList.size(); i++) {
                list1.add(recipeList.get(i).toString().replace(".dbrcp", ""));
            }
            resultMap.put("eppd", list1);
        }
        return resultMap;
    }

    public Map sendS7F25out(String recipeName) {
        DataMsgMap s7f5out = new DataMsgMap("s7f25out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        recipeName = recipeName.replace(".dbrcp", "");
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
            recipeParaList = getRecipeParasByECSV();
            //设备发过来的参数部分为科学计数法，这里转为一般的
            recipeParaList = recipeParaBD2Str(recipeParaList);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        Map resultMap = super.sendS7F17out(recipeName + ".dbrcp");
        resultMap.put("recipeName", recipeName);
        return resultMap;
    }

    // </editor-fold>


    @Override
    public String checkPPExecName(String recipeName) {
        if (ppExecName.equals(recipeName)) {
            return "1";
        }
        return "0";
    }


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
}
