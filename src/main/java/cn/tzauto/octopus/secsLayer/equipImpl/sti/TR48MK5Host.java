package cn.tzauto.octopus.secsLayer.equipImpl.sti;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.sti.TR48MK5RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

@SuppressWarnings("serial")
public class TR48MK5Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(TR48MK5Host.class.getName());
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;

    public TR48MK5Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
    }


    @Override
    public Object clone() {
        TR48MK5Host newEquip = new TR48MK5Host(deviceId,
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
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    updateLotId();
                    sendS2F37outAll();
//                    upLoadAllRcp();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().contains("s6f11incommon")) {
                    long ceid = 0l;
                    try {
                        ceid = msg.getSingleNumber("CollEventID");
                        if (ceid == 11) {
                            processS6F11EquipStatusChange(msg);
                        }else if(ceid ==1){
                            //刷新当前机台状态
                            logger.info("[" + deviceCode + "]" + "之前Recipe为：{" + ppExecName + "}");
                            findDeviceRecipe();
                            logger.info("[" + deviceCode + "]" + "切换Recipe为：{" + ppExecName + "}");
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                long ceid = 0l;
                processS6F11in(data);
                ceid = data.getSingleNumber("CollEventID");
                if (ceid == 11 || ceid == 1) {
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            }  else if (tagName.equalsIgnoreCase("s1f14in")) {
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
    // <editor-fold defaultstate="collapsed" desc="S1FX Code">

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
        s1f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        long[] equipStatuss = new long[1];
        long[] pPExecNames = new long[1];
        long[] controlStates = new long[1];
        DataMsgMap data = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
            s1f3out.put("EquipStatus", equipStatuss);
            s1f3out.put("PPExecName", pPExecNames);
            s1f3out.put("ControlState", controlStates);
            data = activeWrapper.sendAwaitMessage(s1f3out);
            if (data == null || data.get("RESULT") == null) {
                UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备状态信息失败，请检查设备通讯状态！");
                logger.error("获取设备:" + deviceCode + "状态信息失败.");
                return null;
            }
            ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
            ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0).toString()), deviceType);
            ppExecName = listtmp.get(1).toString();
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


    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 73) {
            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            //检查模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！\n");
                holdDevice();
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            String busniessMod = deviceInfoExt.getBusinessMod();
            if ("Engineer".equals(busniessMod) && equipStatus.equalsIgnoreCase("run")) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            } else //开机check
            {
                if (equipStatus.equalsIgnoreCase("run") && ceid == 150l) {
                    if (this.checkLockFlagFromServerByWS(deviceCode)) {
                        UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                        this.holdDevice();
                        return;
                    }
                    if (!rcpInEqp(deviceInfoExt.getRecipeName())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "设备上不存在改机程序，确认是否成功提交改机！禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    if (!checkRecipe.getId().equals(deviceInfoExt.getRecipeId())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "设备使用程序： " + ppExecName + " ;与领料程序：" + checkRecipe.getRecipeName() + " 不一致，禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    //检查程序是否存在 GOLD
                    Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                    if (goldRecipe == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                        this.holdDevice();
                        return;
                    }
                    if (checkRecipe == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在程序：" + ppExecName + "！请确认是否已审核通过！");
                        this.holdDevice();
                    } else {
                        this.startCheckRecipePara(checkRecipe);
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
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
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
            data = activeWrapper.sendAwaitMessage(s7f3out);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            resultMap.put("ACKC7", ackc7[0]);
            resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        //发送PP-Select指令
        sendS2f41CmdPPSelect(targetRecipeName);
        findDeviceRecipe();
        //发送LotConfig指令
        String lPartCounter = getCounterFromPPBody(localRecipeFilePath, targetRecipeName);
        sendS2F41CmdLotConfig("Tape", "Enable", "Enable", "Enable", "Enable", "Enable", "Vision1.0", "Vision1.0", "Vision1.0", "Vision1.0", "Vision1.0", lPartCounter, "500", "0");
        //发送LotStart指令
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        String lotId = deviceInfoExt.getLotId();
        logger.info(lotId);
        //sendS2f41CmdLotStart(lotId, "XXX", "XXX", "XXX");
        sendS2f41CmdLotStart(lotId, "", "", "");
        UiLogUtil.appendLog2EventTab(deviceCode, "发送LotId：[" + lotId + "]至设备！");
        //发送Start指令
        sendS2f41Cmd("START");
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<RecipePara> recipeParaList = null;
        RecipeNameMapping recipeNameMapping = new RecipeNameMapping();
        String shortNameOK = "Y";
        String realRecipeName = "";
        if (data == null || data.isEmpty()) {
            return null;
        }
        byte[] ppbody = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析      
        recipeParaList = TR48MK5RecipeUtil.transferRcpFromDB(recipePath, recipeName, deviceType);
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeNameMapping", null);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("realRecipeName", realRecipeName);
        resultMap.put("shortNameOK", shortNameOK);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    private String getCounterFromPPBody(String recipePath, String recipeName) {
        String counter = "";
        List<RecipePara> recipeParaList = TR48MK5RecipeUtil.transferRcpFromDB(recipePath, recipeName, deviceType);
        for (RecipePara recipePara : recipeParaList) {
            if (recipePara.getParaName().equals("lPartCounter[Tape]")) {
                if (!"".equals(recipePara.getSetValue())) {
                    counter = recipePara.getSetValue();
                }
            }
        }
        return counter;
    }

    private boolean rcpInEqp(String recipeName) {
        boolean rcpInEqp = false;
        ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
        for (int i = 0; i < eppd.size(); i++) {
            String rcpNameString = eppd.get(i).toString();
            if (recipeName.equals(rcpNameString)) {
                rcpInEqp = true;
            }
        }
        return rcpInEqp;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand">

    public Map sendS2f41CmdPPSelect(String PPID) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("PPID", PPID);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + PPID);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    public Map sendS2F41CmdLotConfig(String DATA1, String DATA2, String DATA3, String DATA4, String DATA5, String DATA6, String DATA7,
                                     String DATA8, String DATA9, String DATA10, String DATA11, String DATA12, String DATA13, String DATA14) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41lotConfig", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("DATA1", DATA1);
        s2f41out.put("DATA2", DATA2);
        s2f41out.put("DATA3", DATA3);
        s2f41out.put("DATA4", DATA4);
        s2f41out.put("DATA5", DATA5);
        s2f41out.put("DATA6", DATA6);
        s2f41out.put("DATA7", DATA7);
        s2f41out.put("DATA8", DATA8);
        s2f41out.put("DATA9", DATA9);
        s2f41out.put("DATA10", DATA10);
        s2f41out.put("DATA11", DATA11);
        s2f41out.put("DATA12", DATA12);
        s2f41out.put("DATA13", DATA13);
        s2f41out.put("DATA14", DATA14);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to CHECK the LotConfig");
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd lotconfig at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd lotconfig at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    public Map sendS2f41CmdLotStart(String DATA1, String DATA2, String DATA3, String DATA4) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41lotStart", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("DATA1", DATA1);
        s2f41out.put("DATA2", DATA2);
        s2f41out.put("DATA3", DATA3);
        s2f41out.put("DATA4", DATA4);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to Start the Lot");
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd lotstart at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd lotstart at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
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
//            Map cmdMap = this.sendS2f41Cmd("PAUSE_H");
            Map cmdMap = this.sendS2f41Cmd("STOP");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 2);
                changeEquipPanel(panelMap);
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        //startCheckPass = true;
        holdFlag = false;
        return null;//this.sendS2f41Cmd("RESUME_H");
    }

    @Override
    public Map startDevice() {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outCommand", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s2f41out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        return resultMap;
    }
    // </editor-fold>




//    @Override
//    public Map sendS2f41Cmd(String Remotecommand){
//    DataMsgMap s2f41out = new DataMsgMap("s2f41changeTypeMaterial", activeWrapper.getDeviceId());
//    s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//    DataMsgMap msgdata = null;
//    Map resultMap = new HashMap();
//    resultMap.put("msgType", "s2f42");
//    resultMap.put("deviceCode", deviceCode);
//    byte[] hcack = new byte[1]; 
//        try {
//            msgdata = activeWrapper.sendAwaitMessage(s2f41out);
//            logger.info("The equip " + deviceCode + " request to change typeMaterial!" );
//            hcack = (byte[]) ((SecsItem) msgdata.get("HCACK")).getData();
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        return resultMap;
//    }
}
