/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.bg;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.*;
import java.util.*;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
 * @author njtz
 */
@SuppressWarnings("serial")
public class DiscoDFG8540Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoDFG8540Host.class.getName());
    private Map<String, Boolean> cassUseMap = new HashMap<>();
    private volatile boolean isInterrupted = false;

    public DiscoDFG8540Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
        svFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    }

    public DiscoDFG8540Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
        svFormat = FormatCode.SECS_2BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.equipId);
        while (!isInterrupted) {
            try {
                while (!this.isJsipReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
//                    sendS1F3CheckCassUse();
//                    if ((!"".equals(ppExecName) && ppExecName != null) && "setup".equalsIgnoreCase(equipStatus)) {
//                        this.sendS2F41outPPselect(ppExecName);
//                    }
                    sendS2F37out(231L);
                    sendS2F37out(233L);
                    sendS2F37out(239L);

                    updateLotId();
                }
                MsgDataHashtable msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11equipstate")) {
                    long ceid = 0l;
                    try {
                        ceid = msg.getSingleNumber("CollEventID");
                        Map panelMap = new HashMap();
                        if (ceid == 83 || ceid == 84) {
                            if (ceid == 83) {
                                panelMap.put("ControlState", FengCeConstant.CONTROL_LOCAL_ONLINE);       //Online_Local
                            } else {
                                panelMap.put("ControlState", FengCeConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
                            }
                            changeEquipPanel(msg);
                            processS6F11EquipStatus(msg);
                        } else if (ceid == 211 || ceid == 221) {
                            processS6F11PPselect(msg);
                        } else {
                            processS6F11EquipStatus(msg);
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11alarmClear")) {
                    this.processS6F11AlarmClear(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    this.processS6F11PPselect(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11finish")) {
                    this.processS6F11FinishThick(msg);
                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
        }
    }

    @Override
    public void inputMessageArrived(MessageArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = new Date().getTime();
            secsMsgTimeoutTime = 0;
            MsgDataHashtable data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f11finish")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11alarmClear")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstatuschange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                long ceid = 0l;
                try {
                    ceid = data.getSingleNumber("CollEventID");
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
                if ((ceid < 10150 && ceid > 10080) || (ceid == 223 || ceid == 211 || ceid == 221 || ceid == 213) || ceid == 44) {
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
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

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @SuppressWarnings("unchecked")
    private void sendS1F3CheckCassUse() {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3CassUse", mli.getDeviceId());
        s1f3out.setTransactionId(mli.getNextAvailableTransactionId());
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        ArrayList<SecsItem> list = new ArrayList<>();
        if (data != null) {
            list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        }
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        long cassA = (long) listtmp.get(0);
        long cassB = (long) listtmp.get(1);
        if (cassA != 0 && cassA != 5) {
            cassUseMap.put("A", true);
        } else {
            cassUseMap.put("A", false);
        }
        if (cassB != 0 && cassB != 5) {
            cassUseMap.put("B", true);
        } else {
            cassUseMap.put("B", false);
        }
    }

    private String sendS1F3DFMrecipeName() {
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3DFMRecipe", mli.getDeviceId());
        s1f3out.setTransactionId(mli.getNextAvailableTransactionId());
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        ArrayList<SecsItem> list = new ArrayList<>();
        if (data != null) {
            list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        }
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        logger.info("dfm 部分使用的程序名为:" + String.valueOf(listtmp.get(0)));
        return String.valueOf(listtmp.get(0));

    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code"> 

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        MsgDataHashtable PPselectA = new MsgDataHashtable("s2f41outPPSelectA", mli.getDeviceId());
        PPselectA.setTransactionId(mli.getNextAvailableTransactionId());
        PPselectA.put("PPID", recipeName);
        PPselectA.put("LotId", lotId);
        MsgDataHashtable PPselectB = new MsgDataHashtable("s2f41outPPSelectB", mli.getDeviceId());
        PPselectB.setTransactionId(mli.getNextAvailableTransactionId());
        PPselectB.put("PPID", recipeName);
        PPselectB.put("LotId", lotId);
        try {
//            if ("setup".equalsIgnoreCase(equipStatus)) {
//                if (cassUseMap.get("A")) {
            MsgDataHashtable dataA = mli.sendPrimaryWsetMessage(PPselectA);

//            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK"));
//            description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK");
//                }
//                if (cassUseMap.get("B")) {
            MsgDataHashtable dataB = mli.sendPrimaryWsetMessage(PPselectB);

//            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK"));
//            description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK");
//                }
//            }
//            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselectA(String recipeName) {
        MsgDataHashtable PPselectA = new MsgDataHashtable("s2f41outPPSelectA", mli.getDeviceId());
        PPselectA.setTransactionId(mli.getNextAvailableTransactionId());
        PPselectA.put("PPID", recipeName);
        PPselectA.put("LotId", lotId);
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {

                if (cassUseMap.get("A")) {
                    MsgDataHashtable dataA = mli.sendPrimaryWsetMessage(PPselectA);

                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselectB(String recipeName) {
        MsgDataHashtable PPselectB = new MsgDataHashtable("s2f41outPPSelectB", mli.getDeviceId());
        PPselectB.setTransactionId(mli.getNextAvailableTransactionId());
        PPselectB.put("PPID", recipeName);
        PPselectB.put("LotId", lotId);
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {
                if (cassUseMap.get("B")) {
                    MsgDataHashtable dataB = mli.sendPrimaryWsetMessage(PPselectB);
                    byte[] hcackb = (byte[]) ((SecsItem) dataB.get("HCACK")).getData();
                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 

    @Override
    protected void processS6F11EquipStatusChange(MsgDataHashtable data) {
        //回复s6f11消息
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map map = new HashMap();
        //TODO 此设备可以反馈前一状态
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
        if (execRecipe == null) {
            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
        }
        if (deviceInfoExt == null) {
            deviceInfoExt = setDeviceInfoExt();
            deviceService.saveDeviceInfoExt(deviceInfoExt);
        } else {
            deviceInfoExt.setDeviceStatus(equipStatus);
            deviceService.modifyDeviceInfoExt(deviceInfoExt);
        }
        try {
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        }
        //开机check
//        if (equipStatus.equalsIgnoreCase("error")) {
//            this.holdDevice();
//        }
        if (equipStatus.equalsIgnoreCase("run")) {
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                this.holdDevice();
                return;
            }
            List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(deviceInfoExt.getRecipeId(), "1367");
            if (recipeParas != null && !recipeParas.isEmpty()) {
                logger.info("recipePara 中的dfm 名称为:" + recipeParas.get(0).getSetValue());
                if (!recipeParas.get(0).getSetValue().equals(sendS1F3DFMrecipeName())) {
                    holdDeviceAndShowDetailInfo("The DFM2800 recipe is different from DGP8761");
                    UiLogUtil.appendLog2EventTab(deviceCode, "8761部分使用程序与2800部分不一致！");
                }
            }
            if (execRecipe == null) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Unique或Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                //不允许开机
                this.holdDevice();
                return;
            }
            Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
            this.startCheckRecipePara(checkRecipe);
        }
        sqlSession.close();
    }

    private void processS6F11AlarmClear(MsgDataHashtable data) {
        long alid = 0l;
        try {
            alid = data.getSingleNumber("ALID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);//(SecsItem) data.get("EquipStatus")).getData().toString();
            if (!"".equals(equipStatus) && equipStatus != null) {
                Map map = new HashMap();
                map.put("EquipStatus", equipStatus);
                changeEquipPanel(map);
            }
//            if ("setup".equalsIgnoreCase(equipStatus) && alid == 3017) {
//                this.findDeviceRecipe();
//                sendS1F3CheckCassUse();
//                if (!"".equals(ppExecName) && ppExecName != null) {
//                    this.sendS2F41outPPselect(ppExecName);
//                }
//            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    private void processS6F11PPselect(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 77) {
                //ppselect 事件
                ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            }
            if (ceid == 211 || ceid == 221) {
                Map reMap = sendS7F19out();
                if (reMap != null && reMap.get("eppd") != null) {
                    ArrayList<String> recipeNameList = (ArrayList) reMap.get("eppd");
                    if (recipeNameList.size() > 1) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "设备上存在不止一条程序,请清理其他程序");
                        String dateStr = GlobalConstants.dateFormat.format(new Date());
                        this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + "There is more than one recipe on the equipment,equipment will not be ready state.");
                        return;
                    }
                }
                this.findDeviceRecipe();
                if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus) || "ready".equalsIgnoreCase(equipStatus)) {
                    sendS1F3CheckCassUse();
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    DeviceService deviceService = new DeviceService(sqlSession);
                    DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                    sqlSession.close();
                    if (deviceInfoExt == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "未配置设备模型信息,无法自动执行选中...");
                        return;
                    }
                    String recipeName = deviceInfoExt.getRecipeName();
                    if (!ppExecName.equals(recipeName)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                        //不允许开机
                        String dateStr = GlobalConstants.dateFormat.format(new Date());
                        this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + "The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will not be ready state.");
                    }
                    if (!"".equals(recipeName) && recipeName != null) {
                        if (ceid == 211) {
                            this.sendS2F41outPPselectA(recipeName);
                        } else {
                            this.sendS2F41outPPselectB(recipeName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        MsgDataHashtable msgdata = null;
        try {
            msgdata = mli.sendPrimaryWsetMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            Map paraMap = DiscoRecipeUtil.transferFromFile(recipePath);
            recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand">

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            return this.sendS2f41Cmd("PAUSE");
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        return this.sendS2f41Cmd("RESUME");
    }
    // </editor-fold> 

    @Override
    public Object clone() {
        DiscoDFG8540Host newEquip = new DiscoDFG8540Host(deviceId, this.equipId,
                this.smlFilePath, this.localIPAddress,
                this.localTCPPort, this.remoteIPAddress,
                this.remoteTCPPort, this.connectMode,
                this.protocolType, this.deviceType, this.deviceCode, recipeType, this.iconPath);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.mli = this.mli;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.mli.addInputMessageListenerToAll(newEquip);
        this.setIsRestarting(isRestarting);
        this.clear();
        return newEquip;
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void processS6F11FinishThick(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            Map thickNessMap = new HashMap();
            String WAFERID = "";
            String finishThickness = "";
            if (ceid == 231 || ceid == 233 || ceid == 239) {
                WAFERID = ((SecsItem) data.get("WAFERID")).getData().toString();
                finishThickness = String.valueOf(data.getSingleNumber("FINTHICK"));
                if (ceid == 231) {
                    thickNessMap.put("Z", "Z1");
                }
                if (ceid == 233) {
                    thickNessMap.put("Z", "Z2");
                }
                if (ceid == 239) {
                    thickNessMap.put("Z", "Z3");
                }
            }
            thickNessMap.put("msgName", "FinishThick");
            thickNessMap.put("WAFERID", WAFERID);
            thickNessMap.put("FINTHICK", finishThickness);
            thickNessMap.put("deviceCode", deviceCode);
            if (!GlobalConstants.isLocalMode) {
                logger.debug("processS6F11FinishThick：" + thickNessMap);
                GlobalConstants.C2SSpecificDataQueue.sendMessage(thickNessMap);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
}
