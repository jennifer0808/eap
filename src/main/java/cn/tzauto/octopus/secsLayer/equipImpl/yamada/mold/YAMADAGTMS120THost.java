package cn.tzauto.octopus.secsLayer.equipImpl.yamada.mold;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.yamada.YamadRecipeUtil;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

/**
 *
 * @author njtz
 */
@SuppressWarnings("serial")
public class YAMADAGTMS120THost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(YAMADAGTMS120THost.class);

    public YAMADAGTMS120THost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
        //toDo StripMapUpCeid没找到
        StripMapUpCeid=-1L;
        EquipStateChangeCeid=101L;
        this.svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        this.ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        this.lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        this.rptFormat=SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
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
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    findDeviceRecipe();
                    super.updateLotId();
                    sendS2f41Cmd("UNLOCK");
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
                }else if(msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")){
                    processS6F11in(msg);
                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
//                    try {
//                        processS6F11EquipStatusChange(msg);
//                    } catch (Exception e) {
//                        logger.error("Exception:", e);
//                    }
//                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate") || (msg.getMsgSfName().equalsIgnoreCase("s6f11lotstartready"))) {
//                    try {
//                        processS6F11EquipStatus(msg);
//                    } catch (Exception e) {
//                        logger.error("Exception:", e);
//                    }
//                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11RunModeChange")) {
//                    processS6F11EquipStatus(msg);
//                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11workloaded")) {
//                    processS6F11EquipStatus(msg);
//                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
//                    ppExecName = (String) msg.get("PPExecName");
//                    Map map = new HashMap();
//                    map.put("PPExecName", ppExecName);
//                    changeEquipPanel(map);
//                }
                else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {

                logger.fatal("Caught Interruption", e);
            }
        }
    }
    public void processS6F11in(DataMsgMap data) {
        long ceid=0L;
        try{
        if(data.get("CEID")!=null){
            ceid=Long.parseLong(data.get("CEID").toString());
            logger.info("Received a s6f11in with CEID = " + ceid);
        }
        if(ceid==StripMapUpCeid){
            processS6F11inStripMapUpload(data);
        }else {
            if(ceid==EquipStateChangeCeid){
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
                processS6F11EquipStatusChange(data);
            }if(ceid==1L ||ceid==2L||ceid==3L||ceid==102L||ceid==105L||ceid==115L||ceid==601L){
                processS6F11EquipStatus(data);
            }if(ceid==-1L){
                //toDO  ppselectid
                ppExecName = (String) data.get("PPExecName");
                Map map = new HashMap();
                map.put("PPExecName", ppExecName);
                changeEquipPanel(map);
            }
        }
    } catch (Exception e) {
        logger.error("Exception:", e);
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
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equals("s6f11equipstatuschange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11lotstartready") || (tagName.equalsIgnoreCase("s6f11equipstate"))) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11RunModeChange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11workloaded")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                this.inputMsgQueue.put(data);
            }else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            }
            else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f17in")) {
                processS2F17in(data);
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
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    @SuppressWarnings("unchecked")
    public void processS6F12in(DataMsgMap data) {
        logger.info("----------Received s6f12in---------");
        byte[] ack = (byte[]) ((MsgSection) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }



    public List sendS1F3PressCheckout() {
        List svidlist = new ArrayList<>();
        svidlist.add(5101L);
        svidlist.add(5201L);
        svidlist.add(5301L);
        svidlist.add(5401L);
        DataMsgMap data = null;
        try {
             data = activeWrapper.sendS1F3out(svidlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList listtmp = (ArrayList) data.get("SV");
        return listtmp;
    }


    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = Long.parseLong(data.get("CEID").toString());
            if (ceid == 2) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 3) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            } else if (ceid == 102 || ceid == 601) {
//                sendS2f41Cmd("UNLOCK");
//                UiLogUtil.appendLog2SecsTab(deviceCode, "收到事件报告[CEID=" + ceid + "]，发送UNLOCK指令");
            } else if (ceid == 115) {
                long runMode = -1L;
                runMode = data.getSingleNumber("RunMode");
                if (runMode == 0 || runMode == 2) {
//                    sendS2f41Cmd("UNLOCK");
//                    UiLogUtil.appendLog2SecsTab(deviceCode, "收到事件报告[CEID=" + ceid + "]，发送UNLOCK指令");
                }
            } else if (ceid == 105) {
//                sendS2f41Cmd("UNLOCK");
            }

            updateCommStateInExt();

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //更新页面显示内容
        findDeviceRecipe();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "CEID");
        sqlSession.close();
        if (recipeTemplates != null && recipeTemplates.size() > 0) {
            for (int j = 0; j < recipeTemplates.size(); j++) {
                long ceidtmp = Long.parseLong(recipeTemplates.get(j).getDeviceVariableId());
                if (ceid == ceidtmp) {
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "CEID:" + ceid + " 描述：" + recipeTemplates.get(j).getParaDesc());
                    break;
                }
            }
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid =Long.parseLong(data.get("CEID").toString()) ;
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            if (equipStatus.equalsIgnoreCase("idle") || equipStatus.equalsIgnoreCase("setup")) {
//                sendS2f41Cmd("UNLOCK");
            }
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
                logger.error("数据库中缺少该设备模型配置；DEVICE_CODE:" + deviceCode);
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
                if (AxisUtility.checkBusinessMode(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "处于工程模式,取消开机检查");
                    sqlSession.close();
                    return;
                }
                //开机通过OpMode获取Press使用情况
                getUsingPress();
                //开机check Press使用情况
                checkPressUseState(deviceService, sqlSession);
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                    return;
                }
                //查询trackin时的recipe和GoldRecipe
                Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
                if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                    hasGoldRecipe = false;
                }
                if (!this.checkLockFlagFromServerByWS(deviceCode)) {
                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                        if (!checkResult) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与设备不一致，核对不通过，设备被锁定！请联系PE处理！");
                            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与设备一致，核对通过！");
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
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数范围Check(Gold)");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
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

    // <editor-fold defaultstate="collapsed" desc="S7FX Code"> 
    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
//        recipePath = this.getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + ppid + "/" + ppid + "_V" + recipe.getVersionNo() + ".txt";
        recipePath = super.getRecipePathByConfig(recipe);
        System.out.println(recipePath);

//        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
//        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f5out.put("ProcessprogramID", recipeName);

//        try {
//
//
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        byte[] ppbodys = (byte[]) (data.get("Processprogram"));
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);

        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
        recipeParaList = YamadRecipeUtil.transferYamadaRcp(recipePath);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap=new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeNameMapping", null);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold> 

    @Override
    public Object clone() {
        YAMADAGTMS120THost newEquip = new YAMADAGTMS120THost(deviceId,
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
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            sendS2f41Cmd("REMOTE");
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            Map map = sendS2f41Cmd("STOP");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
//                sendStatus2Server("LOCK");
//                holdFlag = true;
            }
            sendS2f41Cmd("LOCAL");
            return map;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    public Map releaseDevice() {

        Map map = new HashMap();
        map.put("HCACK", "0");
        map.put("Description", "OK");

//        this.sendS2f41Cmd("START");
//        if ((byte) map.get("HCACK") == 0) {
        this.setAlarmState(0);
//        }
        return map;
    }

    //获取当前使用的Press
    public void getUsingPress() {
        List pressResults = sendS1F3PressCheckout();
        super.pressUseMap.clear();
        String pressStatusAll = "";
        for (int i = 0; i < pressResults.size(); i++) {
            String pressStatus = String.valueOf(pressResults.get(i));
            if ("1".equals(pressStatus)) {
                pressStatusAll = pressStatusAll + (i + 1) + ",";
                super.pressUseMap.put(i + 1, true);
            }
        }
        if (pressStatusAll.length() > 0) {
            pressStatusAll = pressStatusAll.substring(0, pressStatusAll.length() - 1);
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备正在使用的Press为[" + pressStatusAll + "]");
    }

    private void initRptPara() {
        //重定义机台的equipstatuschange事件报告
//
//        sendS2f33out(101l, 5000l, 5002l);
//        sendS2f35out(101l, 101l, 101l);

//        sendS2F37out(101l);
//        //重定义ppchange事件
//        sendS2f33out(601l, 5002l);
//        sendS2f35out(601l, 601l, 601l);
//        sendS2F37out(601l);
        sendS2F37outAll();
        sendStatus2Server(equipStatus);
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
//        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
//        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s2f41out.put("PPID", recipeName);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        DataMsgMap data = null;
        try {
            sendS2f41Cmd("REMOTE");
            Thread.sleep(1000);
            Map cpmap = new HashMap();
            cpmap.put(CPN_PPID, recipeName);
            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put(CPN_PPID, SecsFormatValue.SECS_ASCII);
            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put(recipeName, SecsFormatValue.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add(CPN_PPID);
            data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cpmap, cpNameFromatMap, cpValueFromatMap);

            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
          byte  hcack = (byte)  data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 " + " means " + e.getMessage());
        }
        sendS2f41Cmd("LOCAL");
        return resultMap;
    }

}
