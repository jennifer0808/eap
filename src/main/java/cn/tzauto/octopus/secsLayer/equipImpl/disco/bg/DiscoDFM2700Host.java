/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.bg;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
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
public class DiscoDFM2700Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoDFM2700Host.class.getName());
    private Map<String, Boolean> cassUseMap = new HashMap<>();
    private String portARcpName = "";
    private String portBRcpName = "";
    private volatile boolean isInterrupted = false;

    public DiscoDFM2700Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public DiscoDFM2700Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
    }

    @Override
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    @Override
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
                        } else if (ceid == 65) {
                            processS6F11EquipStatusChange(msg);
                        } else if (ceid == 170) {
                            sendS1F3Check();
                            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                            DeviceService deviceService = new DeviceService(sqlSession);
                            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode.replaceAll("-M", ""));
                            String extRecipeName = deviceInfoExt.getRecipeName();
                            sqlSession.close();
                            String[] discoRecipeNameTmps = extRecipeName.split("-");

                            if (discoRecipeNameTmps.length > 5) {
                                String dfmRecipe = extRecipeName.replaceAll("-" + discoRecipeNameTmps[3] + "-" + discoRecipeNameTmps[4], "");
                                if (!dfmRecipe.contains(ppExecName)) {
                                    UiLogUtil.appendLog2EventTab(deviceCode.replaceAll("-M", ""), "8760与2700使用recipe不一致");
                                }
                                String dfmRecipeNametmp = dfmRecipe;
                                if (dfmRecipe.length() > 16) {
                                    dfmRecipeNametmp = dfmRecipe.substring(0, 16);
                                }
                                if (dfmRecipe.length() < 16) {
                                    for (int i = 0; i < 16 - dfmRecipe.length(); i++) {
                                        dfmRecipeNametmp = dfmRecipeNametmp + " ";
                                    }
                                }
                                sendS2F41outPPselect(dfmRecipeNametmp);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11alarmClear")) {
                    this.processS6F11AlarmClear(msg);
                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
        }
    }

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

            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
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
        MsgDataHashtable PPselectA = new MsgDataHashtable("s2f41outPPSelectDFM", mli.getDeviceId());
        PPselectA.setTransactionId(mli.getNextAvailableTransactionId());
        PPselectA.put("PPID", recipeName);
        try {
            MsgDataHashtable dataA = mli.sendPrimaryWsetMessage(PPselectA);
            byte[] hcacka = (byte[]) ((SecsItem) dataA.get("HCACK")).getData();
            if (hcacka != null && hcacka[0] == 0) {
                description = "0";
            }
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK"));
            description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK");

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
    public Map sendS2F41outPPselectA(String recipeName) {
        MsgDataHashtable PPselectA = new MsgDataHashtable("s2f41outPPSelectA", mli.getDeviceId());
        PPselectA.setTransactionId(mli.getNextAvailableTransactionId());
        PPselectA.put("PPID", recipeName);
        PPselectA.put("LotId", lotId);
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {

                if (cassUseMap.get("A")) {
                    MsgDataHashtable dataA = mli.sendPrimaryWsetMessage(PPselectA);
                    byte[] hcacka = (byte[]) ((SecsItem) dataA.get("HCACK")).getData();
                    if (hcacka != null && hcacka[0] == 0) {
                        portARcpName = recipeName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK");
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
                    if (hcackb != null && hcackb[0] == 0) {
                        portBRcpName = recipeName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK");
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
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        String recipeNameTemp = recipeName;
        if (recipeName.length() < 16) {
            for (int i = 0; i < 16 - recipeName.length(); i++) {
                recipeNameTemp = recipeNameTemp + " ";
            }
        }

        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeNameTemp);
        MsgDataHashtable msgdata = null;
        try {
            msgdata = mli.sendPrimaryWsetMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath + ".DFM");
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            //Map paraMap = DiscoRecipeUtil.transferFromFile(recipePath + ".DFM");
            // recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
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

    public Map sendS7F5out1(String recipePath, Recipe recipe, String DFMrecipe) {
        String recipeNameTemp = DFMrecipe;
        if (DFMrecipe.length() < 16) {
            for (int i = 0; i < 16 - DFMrecipe.length(); i++) {
                recipeNameTemp = recipeNameTemp + " ";
            }
        }
        if (DFMrecipe.length() > 16) {
            recipeNameTemp = DFMrecipe.substring(0, 16);
        }
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeNameTemp);
        MsgDataHashtable msgdata = null;
        try {
            msgdata = mli.sendPrimaryWsetMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath.replaceAll(".txt", ".DFM"));
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            //Map paraMap = DiscoRecipeUtil.transferFromFile(recipePath + ".DFM");
            // recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeNameTemp + " from equip " + deviceCode);
        return resultMap;
    }

    /**
     * @param localFilePath
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        if (targetRecipeName.length() > 16) {
            targetRecipeName = targetRecipeName.substring(0, 16);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        if (length[0] == 0) {
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
            return resultMap;
        }
        MsgDataHashtable s7f1out = new MsgDataHashtable("s7f1out", mli.getDeviceId());
        s7f1out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName);
        s7f1out.put("Length", length);

        MsgDataHashtable data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = mli.sendPrimaryWsetMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
            resultMap.put("ppgnt", ppgnt[0]);
            resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    /**
     *
     * @param localRecipeFilePath
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        if (targetRecipeName.length() > 16) {
            targetRecipeName = targetRecipeName.substring(0, 16);
        }
        MsgDataHashtable data = null;
        MsgDataHashtable s7f3out = new MsgDataHashtable("s7f3out", mli.getDeviceId());
        s7f3out.setTransactionId(mli.getNextAvailableTransactionId());
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
            data = mli.sendPrimaryWsetMessage(s7f3out);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            resultMap.put("ACKC7", ackc7[0]);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
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
        DiscoDFM2700Host newEquip = new DiscoDFM2700Host(deviceId, this.equipId,
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

    public Map sendS2f41Cmd(String Remotecommand) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outDFM", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        s2f41out.put("Remotecommand", Remotecommand);
        MsgDataHashtable msgdata = null;
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("prevCmd", Remotecommand);
        byte[] hcack = new byte[1];
        try {
            msgdata = mli.sendPrimaryWsetMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to " + Remotecommand);
            hcack = (byte[]) ((SecsItem) msgdata.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd " + Remotecommand + " at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd " + Remotecommand + " at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map processS5F1in(MsgDataHashtable data) {
        long ALID = 0l;
        try {
            ALID = data.getSingleNumber("ALID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ALCD = (byte[]) ((SecsItem) data.get("ALCD")).getData();
        String ALTX = (String) ((SecsItem) data.get("ALTX")).getData().toString();
        logger.info("Recived s5f1 ID:" + ALID + " from " + deviceCode + " with the ALCD=" + ALCD[0] + " means " + ACKDescription.description(ALCD, "ALCD") + ", and the ALTX is: " + ALTX);
        UiLogUtil.appendLog2SecsTab(deviceCode, "收到报警信息 " + " 报警ID:" + ALID + " 报警详情: " + ALTX);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s5f1");
        resultMap.put("deviceCode", deviceCode.replaceAll("-M", ""));
        resultMap.put("deviceId", deviceId);
        resultMap.put("ALID", ALID);
        resultMap.put("ALCD", ALCD[0]);
        resultMap.put("ALTX", ALTX);
        resultMap.put("Description", ACKDescription.description(ALCD, "ALCD"));
        resultMap.put("TransactionId", data.getTransactionId());
        reportAlarm(resultMap);
        return resultMap;
    }
}
