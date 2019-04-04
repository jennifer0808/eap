/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.bg;

import cn.tfinfo.jcauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tfinfo.jcauto.octopus.biz.device.service.DeviceService;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.Recipe;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipePara;
import cn.tfinfo.jcauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.secsLayer.resolver.disco.DiscoRecipeUtil;
import cn.tfinfo.jcauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tfinfo.jcauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzinfo.smartSecsDriver.userapi.MsgArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.DataMsgMap;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
 * @author njtz
 */
@SuppressWarnings("serial")
public class DiscoDGP8761Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoDGP8761Host.class.getName());
    private Map<String, Boolean> cassUseMap = new HashMap<>();
    private String portARcpName = "";
    private String portBRcpName = "";

    public DiscoDGP8761Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public DiscoDGP8761Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
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
                if (!this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    sendS1F3CheckCassUse();
                    if ((!"".equals(ppExecName) && ppExecName != null) && "setup".equalsIgnoreCase(equipStatus)) {
                        this.sendS2F41outPPselect(ppExecName);
                    }
                    updateLotId();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
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
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11alarmClear")) {
                    this.processS6F11AlarmClear(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    this.processS6F11PPselect(msg);
                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
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
        DataMsgMap s1f3out = new DataMsgMap("s1f3CassUse", activeWrapper.getDeviceId());
        s1f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s1f3out);
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
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code"> 

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap PPselectA = new DataMsgMap("s2f41outPPSelectA", activeWrapper.getDeviceId());
        PPselectA.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        PPselectA.put("PPID", ppExecName);
        PPselectA.put("LotId", lotId);
        DataMsgMap PPselectB = new DataMsgMap("s2f41outPPSelectB", activeWrapper.getDeviceId());
        PPselectB.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        PPselectB.put("PPID", ppExecName);
        PPselectB.put("LotId", lotId);
        try {
            if ("setup".equalsIgnoreCase(equipStatus)) {
                if (cassUseMap.get("A")) {
                    DataMsgMap dataA = activeWrapper.sendAwaitMessage(PPselectA);
                    byte[] hcacka = (byte[]) ((SecsItem) dataA.get("HCACK")).getData();
                    if (hcacka != null && hcacka[0] == 0) {
                        portARcpName = ppExecName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK");
                }
                if (cassUseMap.get("B")) {
                    DataMsgMap dataB = activeWrapper.sendAwaitMessage(PPselectB);
                    byte[] hcackb = (byte[]) ((SecsItem) dataB.get("HCACK")).getData();
                    if (hcackb != null && hcackb[0] == 0) {
                        portBRcpName = ppExecName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK");
                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + ppExecName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselectA() {
        DataMsgMap PPselectA = new DataMsgMap("s2f41outPPSelectA", activeWrapper.getDeviceId());
        PPselectA.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        PPselectA.put("PPID", ppExecName);
        PPselectA.put("LotId", lotId);
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {

                if (cassUseMap.get("A")) {
                    DataMsgMap dataA = activeWrapper.sendAwaitMessage(PPselectA);
                    byte[] hcacka = (byte[]) ((SecsItem) dataA.get("HCACK")).getData();
                    if (hcacka != null && hcacka[0] == 0) {
                        portARcpName = ppExecName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka, "HCACK");
                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + ppExecName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselectB() {
        DataMsgMap PPselectB = new DataMsgMap("s2f41outPPSelectB", activeWrapper.getDeviceId());
        PPselectB.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        PPselectB.put("PPID", ppExecName);
        PPselectB.put("LotId", lotId);
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {
                if (cassUseMap.get("B")) {
                    DataMsgMap dataB = activeWrapper.sendAwaitMessage(PPselectB);
                    byte[] hcackb = (byte[]) ((SecsItem) dataB.get("HCACK")).getData();
                    if (hcackb != null && hcackb[0] == 0) {
                        portBRcpName = ppExecName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb[0] + " means " + ACKDescription.description(hcackb, "HCACK");
                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + ppExecName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 

    protected void processS6F11EquipStatusChange(DataMsgMap data) {
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
            //TODO  这里需要讨论做试产时的情况
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
            if (!"".equals(portARcpName) && !"".equals(portBRcpName)) {
                if (!ppExecName.equals(portARcpName) || !ppExecName.equals(portBRcpName)) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "已选程序与Port口程序不一致，设备被锁定！请联系ME处理！");
                    this.holdDevice();
                    return;
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

    private void processS6F11AlarmClear(DataMsgMap data) {
        long alid = 0l;
        try {
            alid = data.getSingleNumber("ALID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);//(SecsItem) data.get("EquipStatus")).getData().toString();
            if (!"".equals(equipStatus) && equipStatus != null) {
                Map map = new HashMap();
                map.put("EquipStatus", equipStatus);
                changeEquipPanel(map);
            }
            if ("setup".equalsIgnoreCase(equipStatus) && alid == 3017) {
                this.findDeviceRecipe();
                sendS1F3CheckCassUse();
                if (!"".equals(ppExecName) && ppExecName != null) {
                    this.sendS2F41outPPselect(ppExecName);
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    private void processS6F11PPselect(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 77) {
                //ppselect 事件
                ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
                portARcpName = "";
                portARcpName = "";
            }
//            if (ceid == 17) {
//                //211 Cass A move in complete 
//                //221 Cass B move in complete
//                //17 init over
//                this.findDeviceRecipe();
//                if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {
//                    sendS1F3CheckCassUse();
//                    if (!"".equals(ppExecName) && ppExecName != null) {
//                        this.sendS2F41outPPselect(ppExecName);
//                    }
//                }
//            }
            if (ceid == 211 || ceid == 221) {
                this.findDeviceRecipe();
                if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus) || "ready".equalsIgnoreCase(equipStatus)) {
                    sendS1F3CheckCassUse();
                    if (!"".equals(ppExecName) && ppExecName != null) {
                        if (ceid == 211) {
                            this.sendS2F41outPPselectA();
                        } else {
                            this.sendS2F41outPPselectB();
                        }
                    }
                }
            }
            if (ceid == 1000000401) {
                String DFMppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
                UiLogUtil.appendLog2EventTab(deviceCode, "DFM使用的程序为： " + DFMppExecName);
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
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", recipeName);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s7f5out);
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
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
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
    public String checkEquipStatus() {
        findEqptStatus();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)||"RUN".equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！下载失败！";
        }
        return "0";
    }
    
    @Override
    public Object clone() {
        DiscoDGP8761Host newEquip = new DiscoDGP8761Host(deviceId, this.deviceCode,
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
        return newEquip;
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}