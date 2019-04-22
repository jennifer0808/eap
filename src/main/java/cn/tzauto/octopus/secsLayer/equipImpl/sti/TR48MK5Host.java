package cn.tzauto.octopus.secsLayer.equipImpl.sti;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.sti.TR48MK5RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class TR48MK5Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(TR48MK5Host.class.getName());
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;

    public TR48MK5Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        RCMD_PPSELECT = "PPSELECT";
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
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    this.processS6F11in(msg);
                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    @Override
    public void processS6F11in(DataMsgMap msg) {
        try {
            long ceid = (long) msg.get("CEID");
            if (ceid == 11) {
                this.processS6F11EquipStatusChange(msg);
            } else if (ceid == 1) {
                //刷新当前机台状态
                logger.info("[" + deviceCode + "]" + "之前Recipe为：{" + ppExecName + "}");
                findDeviceRecipe();
                logger.info("[" + deviceCode + "]" + "切换Recipe为：{" + ppExecName + "}");
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
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
        long ceid = 0L;
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
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            String busniessMod = deviceInfoExt.getBusinessMod();
            if ("Engineer".equals(busniessMod) && equipStatus.equalsIgnoreCase("run")) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            } else //开机check
            {
                if (equipStatus.equalsIgnoreCase("run") && ceid == 150l) {
                    if (this.checkLockFlagFromServerByWS(deviceCode)) {
                        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                        this.holdDevice();
                        return;
                    }
                    if (!rcpInEqp(deviceInfoExt.getRecipeName())) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备上不存在改机程序，确认是否成功提交改机！禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    if (!checkRecipe.getId().equals(deviceInfoExt.getRecipeId())) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备使用程序： " + ppExecName + " ;与领料程序：" + checkRecipe.getRecipeName() + " 不一致，禁止开机，设备被锁定！请联系ME处理！");
                        this.holdDevice();
                        return;
                    }
                    //检查程序是否存在 GOLD
                    Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                    if (goldRecipe == null) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                        this.holdDevice();
                        return;
                    }
                    if (checkRecipe == null) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在程序：" + ppExecName + "！请确认是否已审核通过！");
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
        Map resultMap = super.sendS7F3out(localRecipeFilePath, targetRecipeName);
        //发送PP-Select指令
        sendS2F41outPPselect(targetRecipeName);
        findDeviceRecipe();
        //发送LotConfig指令
        String lPartCounter = "";
        try {
            lPartCounter = getCounterFromPPBody(localRecipeFilePath, targetRecipeName);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "发送LotId：[" + lotId + "]至设备！");
        //发送Start指令
        sendS2f41Cmd("START");
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
        String shortNameOK = "Y";
        String realRecipeName = "";
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析
        try {
            recipeParaList = TR48MK5RecipeUtil.transferRcpFromDB(recipePath, recipeName, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

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
        ArrayList eppd = (ArrayList) sendS7F19out().get("EPPD");
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


    public Map sendS2F41CmdLotConfig(String DATA1, String DATA2, String DATA3, String DATA4, String DATA5, String DATA6, String DATA7,
                                     String DATA8, String DATA9, String DATA10, String DATA11, String DATA12, String DATA13, String DATA14) {
        Map s2f41outMap = new HashMap();
        s2f41outMap.put("DATA1", DATA1);
        s2f41outMap.put("DATA2", DATA2);
        s2f41outMap.put("DATA3", DATA3);
        s2f41outMap.put("DATA4", DATA4);
        s2f41outMap.put("DATA5", DATA5);
        s2f41outMap.put("DATA6", DATA6);
        s2f41outMap.put("DATA7", DATA7);
        s2f41outMap.put("DATA8", DATA8);
        s2f41outMap.put("DATA9", DATA9);
        s2f41outMap.put("DATA10", DATA10);
        s2f41outMap.put("DATA11", DATA11);
        s2f41outMap.put("DATA12", DATA12);
        s2f41outMap.put("DATA13", DATA13);
        s2f41outMap.put("DATA14", DATA14);
        Map s2f41outNameFromatMap = new HashMap();
        s2f41outNameFromatMap.put("DATA1", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA2", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA3", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA4", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA5", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA6", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA7", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA8", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA9", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA10", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA11", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA12", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA13", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA14", FormatCode.SECS_ASCII);
        Map s2f41outVauleFromatMap = new HashMap();
        s2f41outVauleFromatMap.put(DATA1, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA2, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA3, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA4, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA5, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA6, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA7, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA8, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA9, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA10, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA11, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA12, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA13, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA14, FormatCode.SECS_ASCII);
        byte hcack = -1;
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendS2F41out("LOTCONFIG", s2f41outMap, s2f41outNameFromatMap, s2f41outVauleFromatMap);
            logger.info("The equip " + deviceCode + " request to CHECK the LotConfig");
            hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd lotconfig at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd lotconfig at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + e.getMessage());
        }
        return resultMap;
    }

    //TODO
    public Map sendS2f41CmdLotStart(String DATA1, String DATA2, String DATA3, String DATA4) {
        Map s2f41outMap = new HashMap();
        s2f41outMap.put("DATA1", DATA1);
        s2f41outMap.put("DATA2", DATA2);
        s2f41outMap.put("DATA3", DATA3);
        s2f41outMap.put("DATA4", DATA4);

        Map s2f41outNameFromatMap = new HashMap();
        s2f41outNameFromatMap.put("DATA1", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA2", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA3", FormatCode.SECS_ASCII);
        s2f41outNameFromatMap.put("DATA4", FormatCode.SECS_ASCII);

        Map s2f41outVauleFromatMap = new HashMap();
        s2f41outVauleFromatMap.put(DATA1, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA2, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA3, FormatCode.SECS_ASCII);
        s2f41outVauleFromatMap.put(DATA4, FormatCode.SECS_ASCII);

        byte hcack = -1;
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendS2F41out("LOTSTART", s2f41outMap, s2f41outNameFromatMap, s2f41outVauleFromatMap);
            logger.info("The equip " + deviceCode + " request to Start the Lot");
            hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd lotstart at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd lotstart at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + e.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map holdDevice() {
        return super.holdDevice();
    }

    @Override
    public Map releaseDevice() {
        return super.releaseDevice();
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
        byte hcack = (byte) data.get("HCACK");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack);
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
