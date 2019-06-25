package cn.tzauto.octopus.secsLayer.equipImpl.plasmacleaning;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VSP88DNHTHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(VSP88DNHTHost.class);
    public String Installation_Date;
    public String Lot_Id;
    public String Left_Epoxy_Id;
    public String Lead_Frame_Type_Id;
    public String Datelength;
    boolean holdFlag = false;

    public VSP88DNHTHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }


    @Override
    public Object clone() {
        VSP88DNHTHost newEquip = new VSP88DNHTHost(deviceId,
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
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    initRptPara();
                }
//                if (holdFlag) {
//                    holdDevice();
//                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null) {
                    if (msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                        processS14F1in(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                        processS6F11in(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {// 1
                        long ceid = msg.getSingleNumber("CollEventID");
                        if (ceid == 1010L) {
                            processS6F11EquipStatusChange(msg);
//                        } else if (ceid == 1002L || ceid == 1011L) {
//                            logger.info("将设备控制状态由Local调整为Remote");
//                            sendS2f41Cmd("REMOTE");
                        } else {
                            //todo processS6F11EquipStatus
//                            processS6F11EquipStatus(msg);
                        }
                    } else if (msg.getMsgSfName().equals("s6f11EquipStatusChange")) { // 1
                        processS6F11EquipStatusChange(msg);
                    } else if (msg.getMsgSfName().equals("s6f11stripIdRead")) {//2
                        processS6F11stripIdRead(msg);
                    } else if (msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                        this.processS5F1in(msg);
                    } else {
                        System.out.println("A message in queue with tag = " + msg.getMsgSfName()
                                + " which I do not want to process! ");
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            //1010 ProcessStateChange ; 1101 LotIdRead
            if (ceid == 1010L || ceid == 1101L) {
                processS6F11EquipStatusChange(data);
            }
//            else if (ceid == 1002L || ceid == 1011L) {
//
//                logger.info("将设备控制状态由Local调整为Remote");
//                sendS2f41Cmd("REMOTE");
//            }
            else if (ceid == 1103L || ceid == 1104L) {
                processS6F11stripIdRead(data);
            }

        } catch (Exception e) {
            logger.error("Exception:", e);
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
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
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

    @SuppressWarnings("unchecked")
    public void sendS2f33stripout() {
        ArrayList<Long> list = new ArrayList();
        list.add(2310L);
        list.add(2320L);
        list.add(2331L);
        list.add(2330L);
        super.sendS2F33out(1, 1, list);
    }

    // <editor-fold defaultstate="collapsed" desc="processS6FXin Code">
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            findDeviceRecipe();
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            if (equipStatus.equalsIgnoreCase("Run")) {
                sendS2f41Cmd("REMOTE");
            } else if (equipStatus.equalsIgnoreCase("pause") || equipStatus.equalsIgnoreCase("ldle") || equipStatus.equalsIgnoreCase("end")) {
                sendS2f41Cmd("LOCAL");
            }
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
            if (equipStatus.equalsIgnoreCase("idle")) {
                holdFlag = false;
            }
            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
                if (holdFlag) {
                    holdDevice();
                }
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo();
                } else {
                    //1、获取设备需要校验的信息类型,
                    String startCheckMod = deviceInfoExt.getStartCheckMod();
                    boolean hasGoldRecipe = true;
//                    if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                        holdDeviceAndShowDetailInfo();
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
//                        return;
//                    }
                    //查询trackin时的recipe和GoldRecipe
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));

                    //查询客户端数据库是否存在GoldRecipe
//                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
//                        hasGoldRecipe = false;
//                    }
                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查
//                    if (startCheckMod != null && !"".equals(startCheckMod)) {
//                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
//                        if (!checkResult) {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
//                            //不允许开机
//                            holdDeviceAndShowDetailInfo();
//                        } else {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
//                            setAlarmState(0);
//                        }
//                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if ("Unique".equals(downloadRcpVersionType)) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
//                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                                return;
                            } else {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
//                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
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

    protected void processS6F11stripIdRead(DataMsgMap data) {
        //webserverice  校验
        String stripId = "";
        long ceid = 0L;
        String funcType = "";
        try {
            ceid = (long) data.get("CEID");
            ArrayList reportList = (ArrayList) data.get("REPORT");
            List idList = (List) reportList.get(1);
            stripId = (String) idList.get(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Map msgMap = new HashMap();

        if (ceid == 1103L) {
            msgMap.put("msgName", "MesAolotLoad");
            funcType = "load";
        }
        if (ceid == 1104L) {
            msgMap.put("msgName", "MesAolotUnload");
            funcType = "unload";
        }
        logger.info("get stripid:[" + stripId + "]ceid:" + ceid + "[" + funcType + "]");
        UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "读取到StripId:[" + stripId + "],进行检查...");
//        String result = AxisUtility.findMesAoLotService(deviceCode, stripId, funcType);

        msgMap.put("deviceCode", deviceCode);
        msgMap.put("stripId", stripId);
        String result = "";
        try {
            Message message = GlobalConstants.C2SPlasma2DQueue.sendMessageWithReplay(msgMap);
            if (message != null) {
                MapMessage mapMessage = (MapMessage) message;
                result = mapMessage.getString("message");
            } else {
//                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "等待Server回复超时,请检查网络设置!");
                logger.error("等待MQ回复信息超时!TimeOut= " +GlobalConstants.MQ_MSG_WAIT_TIME);
            }
        } catch (Exception ex) {
            logger.error("MQ sendMessageWithReplay error!" + ex.getMessage());
        }
        sendS2f41Cmd("REMOTE");
        if (result.equalsIgnoreCase("Y")) {
            holdFlag = false;
            this.sends2f41stripReply(true);
        } else {
            holdFlag = true;
            this.sends2f41stripReply(false);
            sendS2f41Cmd("STOP");
            if ("".equals(result)) {
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, msgMap.get("msgName")+"等待MQ回复信息超时!");
            }
        }
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "StripId:[" + stripId + "]检查结果:[" + result + "]");
        sendS2f41Cmd("LOCAL");
//        changeEqptControlStateAndShowDetailInfo("LOCAL");
    }

    protected void sends2f41stripReply(boolean isOk) {
        try {
            Map cp = new HashMap();
            cp.put("RESULT", isOk);
            Map cpName = new HashMap();
            cpName.put("RESULT", FormatCode.SECS_ASCII);
            Map cpValue = new HashMap();
            cpValue.put(isOk, FormatCode.SECS_BOOLEAN);
            List list = new ArrayList();
            list.add("RESULT");
            activeWrapper.sendS2F41out("STRIP_LOAD_CONFIRM", list, cp, cpName, cpValue);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        return super.sendS7F1out(localFilePath, targetRecipeName + ".rcp");
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        return super.sendS7F3out(localRecipeFilePath, targetRecipeName + ".rcp");
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {

        recipeName = recipeName.replace(".rcp", "");
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);

        List<RecipePara> recipeParaList = null;
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析
        recipeParaList = null; //AsmAD8312RecipeUtil.transferRcpFromDB(recipePath, deviceType);
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
        recipeName = recipeName + ".rcp";
        return super.sendS7F17out(recipeName);
    }
// </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="sendS2FXout Code">
    //释放机台

    @Override
    public Map releaseDevice() {
        Map map = new HashMap();
        map.put("HCACK", 0);
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        byte hcack = 0;
        try {
            Map cp = new HashMap();
            cp.put(CPN_PPID, recipeName + ".rcp");
            Map cpName = new HashMap();
            cpName.put(CPN_PPID, FormatCode.SECS_ASCII);
            Map cpValue = new HashMap();
            cpValue.put(recipeName + ".rcp", FormatCode.SECS_ASCII);
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

    // </editor-fold>
    /*
     * (non-Javadoc) It only copies field member values except Mli.
     * @see java.lang.Object#clone()
     */


    private void initRptPara() {
        sendS2f33stripout();
        sendS2F35out(1103L, 1103L, 1L);
        sendS2F35out(1104L, 1104L, 1L);
        sendS2F37outAll();
        if (equipStatus.equalsIgnoreCase("Run")) {
            sendS2f41Cmd("REMOTE");
        } else if (equipStatus.equalsIgnoreCase("pause") || equipStatus.equalsIgnoreCase("ldle") || equipStatus.equalsIgnoreCase("end")) {
            sendS2f41Cmd("LOCAL");
        }
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            sendS2f41Cmd("REMOTE");
            Map map = this.sendS2f41Cmd("STOP");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
            }
            sendS2f41Cmd("LOCAL");
            return map;
        } else {
            sendS2f41Cmd("LOCAL");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }
}
