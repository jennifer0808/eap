/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.eo;


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
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.eo.EORecipeUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author luosy
 */
@SuppressWarnings(value = "all")
public class LMC3200G3Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(LMC3200G3Host.class);

    public LMC3200G3Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        this.svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;

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
                    sendS2f41Cmd("GO-LOCAL");
                    super.findDeviceRecipe();
                    sendStatus2Server(equipStatus);
                    //initRptPara();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in") || msg.getMsgSfName().equalsIgnoreCase("s5f1ypmin")) {
                    processS5F1in(msg);
                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11StripMapUpload")) {
//                    processS6F11inStripMapUpload(msg);
//
//                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
//                    long ceid = 0;
//                    try {
//                        ceid =Long.parseLong(msg.get("CEID").toString());
//                    } catch (Exception ex) {
//                        java.util.logging.Logger.getLogger(LMC3200G3Host.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                    if (ceid == 1003) {
//                        processS6F11EquipStatusChange(msg);
//                    }
//
//                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
//                       processS6F11EquipStatus(msg);
//                }
                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                }
//                else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
//                    ppExecName = (String) msg.get("PPExecName");
//                    Map map = new HashMap();
//                    map.put("PPExecName", ppExecName);
//                    changeEquipPanel(map);
//                }
//                else {
//                    //logger.debug("A message in queue with tag = " + msg.getMsgTagName()
//                    //      + " which I do not want to process! ");
//                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    public void processS6F11in(DataMsgMap data) {
        long ceid = -12345679;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }

            if (ceid == 1003) {
                processS6F11EquipStatusChange(data);
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

//    protected void processS6F11EquipStatus(DataMsgMap data) {
//        //回复s6f11消息
//        long ceid = 0l;
//        try {
//            ceid =(long) data.get("CEID");
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
////        showCollectionsEventInfo(ceid);
//    }

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
            }
//            else if (tagName.equalsIgnoreCase("s6f11equipstatuschange")) {
//                byte[] ack = new byte[1];
//                ack[0] = 0;
//                replyS6F12WithACK(data, ack[0]);
//                long ceid = Long.parseLong(data.get("CEID").toString());
//                if (ceid == 1003) {
//                    this.inputMsgQueue.put(data);
//                }
//            }
            else if (tagName.equalsIgnoreCase("s6f11in")) {
//                byte[] ack = new byte[1];
//                ack[0] = 0;
//                replyS6F12WithACK(data, ack[0]);
                //回复掉消息
//                processS6F11in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
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
            } else if (tagName.equalsIgnoreCase("s6f11StripMapUpload")) {
                processS6F11inStripMapUpload(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                processS14F1in(data);
            } else if (tagName.contains("F0") || tagName.contains("f0")) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
                equipStatus = "SECS-OFFLINE";
                Map panelMap = new HashMap();
                panelMap.put("EquipStatus", equipStatus);
                panelMap.put("PPExecName", ppExecName);
                panelMap.put("ControlState", controlState);
                changeEquipPanel(panelMap);
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
    public void processS6F12in(DataMsgMap data) {
        logger.info("----------Received s6f12in---------");
        byte[] ack = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        logger.info("ackCode = " + ((ack == null) ? "" : ack[0]));
    }


    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName)  throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
//        recipePath = this.getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + ppid + "/" + ppid + "_V" + recipe.getVersionNo() + ".txt";
        recipePath = super.getRecipePathByConfig(recipe);
//        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
//        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f5out.put("ProcessprogramID", "PRODUCTION\\" + recipeName + ".PRJ");
        DataMsgMap msgdata = null;
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
//        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //r
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            Map paraMap = EORecipeUtil.transferFromFile(recipePath);
            recipeParaList = EORecipeUtil.transferFromDB(paraMap, deviceType);
//            recipeParaList = TowaRecipeUtil.transferTowaRcpFromDB(deviceType, ppbody);
        } catch (Exception ex) {
            ex.printStackTrace();
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
    // </editor-fold>

    @Override
    public Object clone() {
        LMC3200G3Host newEquip = new LMC3200G3Host(deviceId,
                this.iPAddress,
                this.tCPPort, this.connectMode,
                this.deviceType, this.deviceCode);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.setIsRestarting(isRestarting);
        this.clear();
        return newEquip;
    }

    private void initRptPara() {
        sendS5F3out(true);
    }

    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            sendS2f41Cmd("GO-REMOTE");
            Map map = this.sendS2f41Cmd("PAUSE");//Map map = this.sendS2f41Cmd("LOCK");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
//                sendStatus2Server("LOCK");
//                holdFlag = true;
            }

            //toDo 逻辑不清楚
            sendS2f41Cmd("GO-LOCAL");
            return map;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = Long.parseLong(data.get("CEID").toString());
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.get(0)), deviceType);
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.get("EquipStatus")), deviceType);
//            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
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
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
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
                if (AxisUtility.checkBusinessMode(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "处于工程模式,取消开机检查");
                    sqlSession.close();
                    return;
                }
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDeviceAndShowDetailInfo("Server locked this equipment");
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
                            holdDeviceAndShowDetailInfo(" There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]，与改机后程序一致，核对通过！");
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
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
                                this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
                            } else {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "[" + ppExecName + "]开始WI参数Check");
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
//        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s7f19out.setTransactionId(transactionId);
        DataMsgMap data = null;
        try {

            data = activeWrapper.sendS7F19out();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
//            data = this.getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        ArrayList list = (ArrayList)  data.get("EPPD");
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ArrayList list1 = new ArrayList();
            for (Object object : listtmp) {
                if (String.valueOf(object).contains("PRODUCTION\\")) {
                    list1.add(String.valueOf(object).replaceAll("PRODUCTION\\\\", "").replaceAll(".PRJ", ""));
                }
            }
            resultMap.put("eppd", list1);
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
//        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
//        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f17out.put("ProcessprogramID", "PRODUCTION\\" + recipeName + ".PRJ");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", "PRODUCTION\\" + recipeName + ".PRJ");

        List recipeIDlist = new ArrayList();
        recipeIDlist.add(recipeName);
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(recipeIDlist);
            logger.info("Request delete recipe " + "PRODUCTION\\" + recipeName + " on " + deviceCode);
     // toDo   data.get("AckCode") or  data.get("ACKC7")?
           byte ackc7 = (byte) data.get("AckCode");
            if (ackc7 == 0) {
                logger.info("The recipe " + "PRODUCTION\\" + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + "PRODUCTION\\" + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7 + " means " + ACKDescription.description(ackc7, "ACKC7"));
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

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param localFilePath
     * @param targetRecipeName
     * @return
     */
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", "PRODUCTION\\" + targetRecipeName + ".PRJ");
        long length = TransferUtil.getPPLength(localFilePath);
        if (length == 0) {
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
            return resultMap;
        }
//        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
//        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s7f1out.put("ProcessprogramID", "PRODUCTION\\" + targetRecipeName + ".PRJ");
//        s7f1out.put("Length", length);

        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F1out(targetRecipeName, length, lengthFormat);
            byte ppgnt  = (byte)  data.get("PPGNT");
            logger.info("Request send ppid= " + "PRODUCTION\\" + targetRecipeName + " to Device " + deviceCode);
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
//        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
//        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
//        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
//        s7f3out.put("ProcessprogramID", "PRODUCTION\\" + targetRecipeName + ".PRJ");
//        s7f3out.put("Processprogram", secsItem);
        targetRecipeName="PRODUCTION\\" + targetRecipeName + ".PRJ";
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, FormatCode.SECS_BINARY);
         //toDo  data.get("AckCode") or data.get("ACKC7")?
            byte ackc7 = (byte)  data.get("AckCode");
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
    public Map sendS1F3Check() {
//        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s1f3out.setTransactionId(transactionId);
//        long[] equipStatuss = new long[1];
//        long[] pPExecNames = new long[1];
//        long[] controlStates = new long[1];
//        DataMsgMap data = null;
//        try {
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            RecipeService recipeService = new RecipeService(sqlSession);
//            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
//            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
//            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
//            sqlSession.close();
//            s1f3out.put("EquipStatus", equipStatuss);
//            s1f3out.put("PPExecName", pPExecNames);
//            s1f3out.put("ControlState", controlStates);
//            logger.info("Ready to send Message S1F3==============>" + JSONArray.toJSON(s1f3out));
//            // DO: 2019/6/10        data = sendMsg2Equip(s1f3out);
//        } catch (Exception e) {
//            logger.error("Wait for get meessage directly error：" + e);
//        }

//        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
//            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
//        }
//        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
//            return null;
//        }
//        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
//        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
//        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        List listtmp = getNcessaryData();
        if (listtmp != null && !listtmp.isEmpty()) {
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
            ppExecName = String.valueOf(listtmp.get(1) == null ? "" : listtmp.get(1)).replaceAll("PRODUCTION\\\\", "").replaceAll(".PRJ", "");
            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    @Override
    public String testRUThere() {
        try {
//            DataMsgMap s1f1out = new DataMsgMap("s1f1out", activeWrapper.getDeviceId());
//            long transactionId = activeWrapper.getNextAvailableTransactionId();
//            s1f1out.setTransactionId(transactionId);
            DataMsgMap s1f2in =  activeWrapper.sendS1F1out();
            // DO: 2019/6/10    s1f2in = activeWrapper.sendPrimaryWsetMessage(s1f1out);
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

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        sendS2f41Cmd("GO-REMOTE");
//        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
//        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s2f41out.put("PPID", "PRODUCTION\\" + recipeName + ".PRJ");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        DataMsgMap data = null;
        try {
            Map cpmap = new HashMap();
            cpmap.put(CPN_PPID, recipeName);
            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put(CPN_PPID, FormatCode.SECS_ASCII);
            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put(recipeName, FormatCode.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add(CPN_PPID);
            // TODO: 2019/6/10   data = activeWrapper.sendPrimaryWsetMessage(s2f41out);
           data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cpmap, cpNameFromatMap, cpValueFromatMap);

            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
          byte  hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9"  + " means " + e.getMessage());
        }
        sendS2f41Cmd("GO-LOCAL");
        return resultMap;
    }
}
