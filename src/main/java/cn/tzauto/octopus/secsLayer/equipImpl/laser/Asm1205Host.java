package cn.tzauto.octopus.secsLayer.equipImpl.laser;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.generalDriver.entity.msg.MsgSection;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandParaPair;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.IOException;
import java.util.*;

public class Asm1205Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(Asm1205Host.class.getName());

    private String ppExecNameOfClp1 = "";
    private String ppExecNameOfClp2 = "";
    private String ppExecNameOfPlp = "";
    private int loadPort;
    private int CLP1_PortCount;
    private int CLP2_PortCount;
    private String lotId;
    private List SlotMap;
    private List waferFrameId;
    private List waferId;

    public Asm1205Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        CPN_PPID = "RecipeId";
        loadPort = 0;
        RCMD_PPSELECT = "PP_Select";
    }


    @Override
    public Object clone() {
        Asm1205Host newEquip = new Asm1205Host(deviceId,
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
                    sendS5F3out(true);
                    initRptPara();
                    // 自定义S2F49所用的方法中的内容
                    initCommand();
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
                logger.fatal("Caught Exception:", e);
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
//            this.sendS2F33clear();
//            this.sendS2F35clear();
            //重新定义Learn Device事件
//            List<Long> svidlist = new ArrayList<>();
//            svidlist.add(56L);
//            svidlist.add(60L);
//            svidlist.add(11L);
//            sendS2F33out(9L, 9L, svidlist);
//            sendS2F35out(9L, 9L, 9L);
//            sendS2F37out(9L);
//            sendS2F37outAll();

//            sendS2F37outClose(15650L);
//            sendS2F37outClose(15652L);
//            sendS5F3out(true);
            return "1";

        } catch (Exception ex) {
//            java.util.logging.Logger.getLogger(EsecDB2100Host.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception:", ex);
            return "0";
        }
    }

    public void initCommand() {
        String commandKey = "ProceedWithMaterial";
        CommandDomain proceedWithMaterialCommand = new CommandDomain();
        proceedWithMaterialCommand.setRcmd("ProceedWithMaterial");
        List<CommandParaPair> proceedWithMaterialParaList = new ArrayList<>();
        CommandParaPair paraPair1 = new CommandParaPair();
        paraPair1.setCpname("LoadPort");
        paraPair1.setCpval(loadPort);
        proceedWithMaterialParaList.add(paraPair1);
        proceedWithMaterialCommand.setParaList(proceedWithMaterialParaList);
        this.remoteCommandMap.put(commandKey, proceedWithMaterialCommand);

        commandKey = "ProceedWithSlotMap";
        proceedWithMaterialCommand = new CommandDomain();
        proceedWithMaterialCommand.setRcmd("ProceedWithSlotMap");
        proceedWithMaterialParaList = new ArrayList<>();
        paraPair1 = new CommandParaPair();
        paraPair1.setCpname("LoadPort");
        paraPair1.setCpval(loadPort);
        proceedWithMaterialParaList.add(paraPair1);
        CommandParaPair paraPair2 = new CommandParaPair();
        paraPair2.setCpname("LotId");
        paraPair2.setCpval(lotId);
        proceedWithMaterialParaList.add(paraPair2);
        CommandParaPair paraPair3 = new CommandParaPair();
        paraPair3.setCpname("WaferFrameId");
        paraPair3.setCpval(waferFrameId);
        proceedWithMaterialParaList.add(paraPair3);
        CommandParaPair paraPair4 = new CommandParaPair();
        paraPair4.setCpname("WaferId");
        paraPair4.setCpval(waferId);
        proceedWithMaterialParaList.add(paraPair4);
        proceedWithMaterialCommand.setParaList(proceedWithMaterialParaList);
        this.remoteCommandMap.put(commandKey, proceedWithMaterialCommand);


    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @Override
    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        if (listtmp != null && !listtmp.isEmpty()) {
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
            ppExecNameOfPlp = String.valueOf(listtmp.get(1));
            List ppExecNameListOfClp1 = new ArrayList();
            if (null != listtmp.get(2) && !"".equals(listtmp.get(2))) {
                ppExecNameListOfClp1 = (ArrayList) listtmp.get(2);
            }
//            String ppExecNameValueOfClp1 = "";
            CLP1_PortCount = 0;
            for (Object obj : ppExecNameListOfClp1) {
                if (null != obj && !"".equals(obj.toString())) {
                    ppExecNameOfClp1 = obj.toString();
                }
                CLP1_PortCount++;
            }
//            ppExecNameOfClp1 = String.valueOf(listtmp.get(1));
            List ppExecNameListOfClp2 = new ArrayList();
            if (null != listtmp.get(3) && !"".equals(listtmp.get(3))) {
                ppExecNameListOfClp2 = (ArrayList) listtmp.get(3);
            }
//            String ppExecNameValueOfClp2 = "";
            CLP2_PortCount = 0;
            for (Object obj : ppExecNameListOfClp2) {
                if (null != obj && !"".equals(obj.toString())) {
                    ppExecNameOfClp2 = obj.toString();
                }
                CLP2_PortCount++;
            }
//            ppExecNameOfClp2 = String.valueOf(listtmp.get(1));
            if (!"".equals(ppExecNameOfPlp)) {
                ppExecName = ppExecNameOfPlp;
                loadPort = 0;
            } else if (!"".equals(ppExecNameOfClp1)) {
                ppExecName = ppExecNameOfClp1;
                loadPort = 1;
            } else if (!"".equals(ppExecNameOfClp2)) {
                ppExecName = ppExecNameOfClp2;
                loadPort = 2;
            } else {
                ppExecName = "";
                loadPort = 0;
            }

            controlState = ACKDescription.describeControlState(listtmp.get(4), deviceType);
        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    @Override
    protected List getNcessaryData() {
        DataMsgMap data = null;
        try {
            List<Long> statusList = new ArrayList<>();

            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            long equipStatussvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
            long ppExecNameOfPlpSvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ppExecNameOfPlp").get(0).getDeviceVariableId());
            long ppExecNameOfClp1Svid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ppExecNameOfClp1").get(0).getDeviceVariableId());
            long ppExecNameOfClp2Svid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ppExecNameOfClp2").get(0).getDeviceVariableId());
            long controlStatesvid = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
            statusList.add(equipStatussvid);
            statusList.add(ppExecNameOfPlpSvid);
            statusList.add(ppExecNameOfClp1Svid);
            statusList.add(ppExecNameOfClp2Svid);
            statusList.add(controlStatesvid);
            data = activeWrapper.sendS1F3out(statusList, svFormat);
        } catch (Exception e) {
            logger.error("Wait for get meessage directly error：" + e);
        }
        if (data == null || data.get("SV") == null) {
            return null;
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        return (ArrayList) data.get("SV");

    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    //hold机台
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map map = this.sendS2f41Cmd("Stop");
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

    public DataMsgMap sendS2F49out(String rcmd, List cpNameList, Map cpMap, Map<Object, Short> cpNameFormatMap, Map<Object, Short> cpValueFormatMap, int portCount)
            throws StateException, InterruptedException, InvalidDataException, IOException, IntegrityException, InvalidDataException, BrokenProtocolException, T3TimeOutException, T6TimeOutException {
        DataMsgMap s2f49out = new DataMsgMap("S2F49OUT", activeWrapper.getDeviceId());
        s2f49out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        MsgSection vRoot = new MsgSection();
        vRoot.setFormatCode(0);
        ArrayList itemList = new ArrayList();
        MsgSection dataIdItem = new MsgSection(0, SecsFormatValue.SECS_1BYTE_UNSIGNED_INTEGER);
        itemList.add(dataIdItem);
        MsgSection objectItem = new MsgSection("OBJECT", 16);
        itemList.add(objectItem);
        MsgSection rcmdItem = new MsgSection(rcmd, 16);
        itemList.add(rcmdItem);
        ArrayList rcmdCompanionItemList = new ArrayList();
        if (cpMap != null && !cpMap.isEmpty() && cpNameList != null && !cpNameList.isEmpty()) {
            for (Iterator rcmdCompanionItemListSecsItem = cpNameList.iterator(); rcmdCompanionItemListSecsItem.hasNext(); ) {
                Object cpname = rcmdCompanionItemListSecsItem.next();
                Object cpv = cpMap.get(cpname);
                if ("RecipeId".equals(cpname.toString())) {
                    ArrayList cpItemList = new ArrayList();
                    MsgSection cpnameItem = new MsgSection(cpname, ((Short) cpNameFormatMap.get(cpname)).shortValue());
                    cpItemList.add(cpnameItem);
                    if (portCount > 0) {
                        ArrayList valueItemList = new ArrayList();
                        for (int i = 0; i < portCount; i++) {
                            MsgSection cpvItem = new MsgSection(cpv, ((Short) cpValueFormatMap.get(cpv)).shortValue());
                            valueItemList.add(cpvItem);
//                            if (i == 0) {
//                                String cpValue = cpv + " all";
//                                SecsItem cpvItem = new SecsItem(cpValue, ((Short) cpValueFormatMap.get(cpv)).shortValue());
//                                valueItemList.add(cpvItem);
//                            } else {
//                                SecsItem cpvItem = new SecsItem(cpv, ((Short) cpValueFormatMap.get(cpv)).shortValue());
//                                valueItemList.add(cpvItem);
//                            }
                        }
                        MsgSection valueItem = new MsgSection(valueItemList, 0);
                        cpItemList.add(valueItem);
                    } else {
                        MsgSection cpvItem = new MsgSection(cpv, ((Short) cpValueFormatMap.get(cpv)).shortValue());
                        cpItemList.add(cpvItem);
                    }
                    MsgSection cpItemListSecsItem = new MsgSection(cpItemList, 0);
                    rcmdCompanionItemList.add(cpItemListSecsItem);
                } else {
                    ArrayList cpItemList = new ArrayList();
                    MsgSection cpnameItem = new MsgSection(cpname, ((Short) cpNameFormatMap.get(cpname)).shortValue());
                    cpItemList.add(cpnameItem);
                    MsgSection cpvItem = new MsgSection(cpv, ((Short) cpValueFormatMap.get(cpv)).shortValue());
                    cpItemList.add(cpvItem);
                    MsgSection cpItemListSecsItem = new MsgSection(cpItemList, 0);
                    rcmdCompanionItemList.add(cpItemListSecsItem);
                }
            }
//            Iterator rcmdCompanionItemListSecsItem = cpNameList.iterator();
//            while (rcmdCompanionItemListSecsItem.hasNext()) {
//
//            }
        }
        MsgSection rcmdCompanionItemListSecsItem1 = new MsgSection(rcmdCompanionItemList, 0);
        itemList.add(rcmdCompanionItemListSecsItem1);
        vRoot.setData(itemList);
        s2f49out.put("S2F49OUT", vRoot);
        return activeWrapper.sendAwaitMessage(s2f49out);
    }

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        return this.sendS2F49outPPselect(recipeName, 0);
    }

    /**
     * 根据S2F49选中recipe
     *
     * @param recipeName
     * @return
     */
    public Map sendS2F49outPPselect(String recipeName, int portCount) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f50");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put("LoadPort", loadPort);
            cpmap.put(CPN_PPID, recipeName);

            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put("LoadPort", SecsFormatValue.SECS_ASCII);
            cpNameFromatMap.put(CPN_PPID, SecsFormatValue.SECS_ASCII);

            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put(loadPort, SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER);
            cpValueFromatMap.put(recipeName, SecsFormatValue.SECS_ASCII);

            List cplist = new ArrayList();
            cplist.add("LoadPort");
            cplist.add(CPN_PPID);
            DataMsgMap data = this.sendS2F49out(RCMD_PPSELECT, cplist, cpmap, cpNameFromatMap, cpValueFromatMap, portCount);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            byte hcack = ((byte[]) ((MsgSection) (((ArrayList) ((MsgSection) data.get("S2F50IN")).getData()).get(0))).getData())[0];
            logger.info("Receive s2f50in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 means " + e.getMessage());
        }
        return resultMap;
    }

//    public Map sendS2F49Cmd(String cmd) {
//        Map resultMap = new HashMap();
//        resultMap.put("msgType", "s2f50");
//        resultMap.put("deviceCode", deviceCode);
//        try {
//            Map cpmap = new HashMap();
//            cpmap.put("LoadPort", loadPort);
//
//            Map cpNameFromatMap = new HashMap();
//            cpNameFromatMap.put("LoadPort", FormatCode.SECS_ASCII);
//
//            Map cpValueFromatMap = new HashMap();
//            cpValueFromatMap.put(loadPort, FormatCode.SECS_4BYTE_UNSIGNED_INTEGER);
//
//            List cplist = new ArrayList();
//            cplist.add("LoadPort");
//
//            if ("ProceedWithSlotMap".equals(cmd)) {
//                cpmap.put("LotId", lotId);
//                cpNameFromatMap.put("LotId", FormatCode.SECS_ASCII);
//                cpValueFromatMap.put(lotId, FormatCode.SECS_ASCII);
//                cpmap.put("WaferFrameId", waferFrameId);
//                cpNameFromatMap.put("WaferFrameId", FormatCode.SECS_ASCII);
//                cpValueFromatMap.put(waferFrameId, 0);
//                cpmap.put("WaferId", waferId);
//                cpNameFromatMap.put("WaferId", FormatCode.SECS_ASCII);
//                cpValueFromatMap.put(waferId, 0);
//                cplist.add("LotId");
//                cplist.add("WaferFrameId");
//                cplist.add("WaferId");
////                cpmap.put("SlotMap", SlotMap);
//            }
//            DataMsgMap data = this.sendS2F49out(cmd, cplist, cpmap, cpNameFromatMap, cpValueFromatMap);
//            logger.info("The equip " + deviceCode + " send command: " + cmd);
////            byte hcack = (byte) data.get("HCACK");
////            logger.info("Receive s2f50in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
////            resultMap.put("HCACK", hcack);
////            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
//        } catch (Exception e) {
////            logger.error("Exception:", e);
////            resultMap.put("HCACK", 9);
////            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 means " + e.getMessage());
//        }
//        return new HashMap();
//    }

    public Map sendS2F49Cmd(String commandKey) {
        DataMsgMap s2f49out = new DataMsgMap("S2F49OUT", activeWrapper.getDeviceId());
        s2f49out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        CommandDomain commandDomain = this.remoteCommandMap.get(commandKey);
        if (commandDomain != null) {
//            s2f49out.put("RCMD", commandDomain.getRcmd());
            for (CommandParaPair commandParaPair : commandDomain.getParaList()) {
                if ("LoadPort".equals(commandParaPair.getCpname()))
                    commandParaPair.setCpval(loadPort);
            }
            MsgSection vRoot = new MsgSection();
            vRoot.setFormatCode(SecsFormatValue.SECS_LIST);
            ArrayList rootData = new ArrayList();
            MsgSection dataIdItem = new MsgSection(0, SecsFormatValue.SECS_1BYTE_UNSIGNED_INTEGER);
            rootData.add(dataIdItem);
            MsgSection objectItem = new MsgSection("OBJECT", 16);
            rootData.add(objectItem);
            MsgSection rcmdItem = new MsgSection(commandDomain.getRcmd(), 16);
            rootData.add(rcmdItem);
            MsgSection totalItem = new MsgSection();
            totalItem.setFormatCode(SecsFormatValue.SECS_LIST);
            ArrayList totalNode = new ArrayList();
            if (commandDomain.getParaList().size() > 0) {
                for (CommandParaPair cpPair : commandDomain.getParaList()) {
                    MsgSection cpItem = new MsgSection();
                    cpItem.setFormatCode(SecsFormatValue.SECS_LIST);
                    ArrayList cpPairNode = new ArrayList(2);
                    MsgSection cpname = new MsgSection(cpPair.getCpname(), SecsFormatValue.SECS_ASCII);
                    cpPairNode.add(cpname);
                    short cpvalType = SecsFormatValue.SECS_ASCII;
                    if (cpPair.getCpval() instanceof Boolean) {
                        cpvalType = SecsFormatValue.SECS_BOOLEAN;
                    } else if (cpPair.getCpval() instanceof Byte) {
                        cpvalType = SecsFormatValue.SECS_BINARY;
                    } else if (cpPair.getCpval() instanceof Integer) {
                        cpvalType = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
                    } else if (cpPair.getCpval() instanceof List) {
                        cpvalType = SecsFormatValue.SECS_LIST;
                    }
                    MsgSection cpvalue = new MsgSection(cpPair.getCpval(), cpvalType);
                    cpPairNode.add(cpvalue);
                    cpItem.setData(cpPairNode);
                    totalNode.add(cpItem);
                }
            }
            totalItem.setData(totalNode);
            rootData.add(totalItem);
            vRoot.setData(rootData);//very important
            s2f49out.put("S2F49OUT", vRoot);
            byte hcack = -1;
            try {
                DataMsgMap data = activeWrapper.sendAwaitMessage(s2f49out);
//                hcack = (byte) ((SecsItem) data.get("HCACK")).getData();
                logger.info("Recive s2f50in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s2f50");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd " + commandKey + " at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            return resultMap;
        } else {
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s2f50");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("HCACK", new byte[]{1});
            resultMap.put("Description", "该设备不支持" + commandKey + "命令");
            return resultMap;
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
            // TODO: 2019/5/30 与机台的事件报告交互还需进一步确认
            if (ceid == 10011 || ceid == 10012 || ceid == 10014 ||
                    ceid == 10021 || ceid == 10200 || ceid == 10201 || ceid == 10102) {
                processS6F11EquipStatusChange(data);
            } else if (ceid == 10306) {
                //CLP1_MaterialIdRead
                loadPort = 1;
                sendS2F49Cmd("ProceedWithMaterial");
            } else if (ceid == 10406) {
                loadPort = 2;
                sendS2F49Cmd("ProceedWithMaterial");
            } else if (ceid == 10506) {
                loadPort = 0;
                sendS2F49Cmd("ProceedWithMaterial");
            } else if (ceid == 10309) {
                //CLP1_SlotMapScanned
                sendS2F49Cmd("ProceedWithSlotMap");
                loadPort = 1;
            } else if (ceid == 10310) {
                //CLP1_ProceedWithSlotMapOk
                loadPort = 1;
//                RCMD_PPSELECT = "PP_SelectPerSlot";
//                sendS2F49outPPselect("Production\\Main\\15120508-151205AD-HK010-12", CLP1_PortCount);
//                RCMD_PPSELECT = "PP_Select";
//                int portCount = 0;
//                if (loadPort == 0) {
//                    RCMD_PPSELECT = "PP_Select";
//                    portCount = 0;
//                } else {
//                    RCMD_PPSELECT = "PP_SelectPerSlot";
//                    portCount = CLP1_PortCount;
//                }
//                sendS2F49outPPselect("Production\\Main\\15120508-151205AD-HK010-12", portCount);
            } else if (ceid == 10409||ceid == 10410) {
                //CLP2_ProceedWithSlotMapOk
                loadPort = 2;
//                RCMD_PPSELECT = "PP_Select";
//                sendS2F49outPPselect("Production\\Main\\15120508-151205AD-HK010-12", 0);
//                RCMD_PPSELECT = "PP_Select";
//                int portCount = 0;
//                if (loadPort == 0) {
//                    RCMD_PPSELECT = "PP_Select";
//                    portCount = 0;
//                } else {
//                    RCMD_PPSELECT = "PP_SelectPerSlot";
//                    portCount = CLP1_PortCount;
//                }
//                sendS2F49outPPselect("Production\\Main\\15120508-151205AD-HK010-12", portCount);
            } else if (ceid == 10507) {
                loadPort = 0;
                sendS2F49outPPselect("Production\\Main\\15120508-151205AD-HK010-12", 0);
            }
//            else if (ceid == 10513) {
//                //PLP_ProceedWithSlotMapOk
//                loadPort = 0;
//                RCMD_PPSELECT = "PP_Select";
//                sendS2F49outPPselect("Production\\Main\\15120508-151205AD-HK010-12", 0);
//            } else if (ceid == 10200) {
//                //PPSelected
//                sendS2F49out("START", null, null, null, null, 0);
//            } else if (ceid == 10201) {
//                // PPSelectedPerSlot
//                sendS2F49out("START", null, null, null, null, 0);
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

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName.replace("Production\\Main\\", ""));

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
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        targetRecipeName = targetRecipeName.replace("@", "/");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName.replace("Production\\Main\\", ""));
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, SecsFormatValue.SECS_BINARY);
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
//        recipeName = recipeName.replace("Production\\Main\\","Production\\");
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
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
    public Map sendS7F19out() {
//        Map resultMap = new HashMap();
        Map resultMap = new CaseInsensitiveMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS7F19out();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList list = (ArrayList) data.get("EPPD");
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
//            resultMap.put("EPPD", new ArrayList<>());
        } else {
            logger.info("recipeNameList:" + list);
            ArrayList recipeList = new ArrayList<>();
            for (Object recipeName : list) {
                if (recipeName.toString().contains("Production")) {
                    recipeList.add(recipeName);
                }
            }
            resultMap.put("eppd", list);
//            resultMap.put("EPPD", list);
        }
        return resultMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S12FX Code"> 

    // </editor-fold>


}
