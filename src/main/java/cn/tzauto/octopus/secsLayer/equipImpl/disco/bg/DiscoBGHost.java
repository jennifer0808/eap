/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.bg;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

/**
 * @author njtz
 */
@SuppressWarnings("serial")
public class DiscoBGHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoBGHost.class.getName());
    private Map<String, Boolean> cassUseMap = new HashMap<>();
    private String portARcpName = "";
    private String portBRcpName = "";

    public DiscoBGHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
    }

    public Object clone() {
        DiscoBGHost newEquip = new DiscoBGHost(deviceId,
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
//                    upLoadAllRcp();
                }
                //针对DISCO开机无法hold，运行时hold的处理
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    long ceid = 0l;
                    try {
                        ceid = msg.getSingleNumber("CollEventID");
                        Map panelMap = new HashMap();
                        if (ceid == 10103 || ceid == 10104) {
                            if (ceid == 10103) {
                                panelMap.put("ControlState", FengCeConstant.CONTROL_LOCAL_ONLINE);       //Online_Local
                            } else {
                                panelMap.put("ControlState", FengCeConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
                            }
                            changeEquipPanel(msg);
//                            processS6F11EquipStatus(msg);
                        } else if (ceid == 211 || ceid == 221) {
                            processS6F11PPselect(msg);
                        } else {
//                            processS6F11EquipStatus(msg);
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
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
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
        List cassIdlist = new ArrayList();
        cassIdlist.add(1004L);
        cassIdlist.add(1005L);
        try {
            data = activeWrapper.sendS1F3out(cassIdlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        ArrayList listtmp = new ArrayList<>();
        if (data != null) {
            listtmp = (ArrayList) data.get("SV");
        }

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

    @SuppressWarnings("unchecked")

    public Map sendS1F3RcpParaCheckout(List svidlist) {
        DataMsgMap s1f3out = new DataMsgMap("s1f3" + deviceType + "RcpPara", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        for (int i = 0; i < svidlist.size(); i++) {
            long[] svid = new long[1];
            // Long.parseLong(svidlist.get(i));
            svid[0] = Long.parseLong(svidlist.get(i).toString());
            s1f3out.put("Data" + i, svid);
        }
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.isEmpty()) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        if (data == null || data.get("RESULT") == null) {
            logger.error("设备回复Data Error;{" + JsonMapper.toJsonString(data) + "}");
            return null;
        }
        Map resultMap = new HashMap();
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        resultMap.put("msgType", "s1f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("SVList", complementSvlist(listtmp));
        resultMap.put("Description", "Get SVList from equip " + listtmp);
        return resultMap;
    }

    /**
     * @param svValueList
     * @return
     */
    private List complementSvlist(List svValueList) {
        List fullList = svValueList;
        Map map = sendS7F5out(ppExecName);
        List<RecipePara> recipeParaList = null;
        if (map != null && map.get("recipeParaList") != null) {
            recipeParaList = (List<RecipePara>) map.get("recipeParaList");
        }

        if (recipeParaList != null) {
            List tempList = new ArrayList();
            for (RecipePara recipePara : recipeParaList) {
                if (recipePara.getParaName().equalsIgnoreCase("VDT_ORG_THICK") || recipePara.getParaName().equalsIgnoreCase("VDT_FIN_THICK")) {
                    tempList.add(recipePara.getSetValue());
                }
                if (tempList.size() > 2) {
                    break;
                }
            }
            fullList.addAll(tempList);
        }
        return fullList;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code"> 

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        this.findDeviceRecipe();
        if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus) || "ready".equalsIgnoreCase(equipStatus)) {
            sendS1F3CheckCassUse();
            ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
            boolean selectOkFlag = false;
            for (int i = 0; i < eppd.size(); i++) {
                if (eppd.get(i).toString().equals(recipeName)) {
                    DataMsgMap PPselectA = new DataMsgMap("s2f41outPPSelectA", activeWrapper.getDeviceId());
                    PPselectA.setTransactionId(activeWrapper.getNextAvailableTransactionId());
                    PPselectA.put("PPID", recipeName);
                    PPselectA.put("LotId", lotId);
                    DataMsgMap PPselectB = new DataMsgMap("s2f41outPPSelectB", activeWrapper.getDeviceId());
                    PPselectB.setTransactionId(activeWrapper.getNextAvailableTransactionId());
                    PPselectB.put("PPID", recipeName);
                    PPselectB.put("LotId", lotId);
                    try {
                        if ("setup".equalsIgnoreCase(equipStatus)) {
                            if (cassUseMap.get("A")) {
                                DataMsgMap dataA = activeWrapper.sendAwaitMessage(PPselectA);
                                byte[] hcacka = (byte[]) ((SecsItem) dataA.get("HCACK")).getData();
                                if (hcacka != null && hcacka[0] == 0) {
                                    portARcpName = recipeName;
                                    selectOkFlag = true;
                                }
                                logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka[0], "HCACK"));
                                description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka[0], "HCACK");
                            }
                            if (cassUseMap.get("B")) {
                                DataMsgMap dataB = activeWrapper.sendAwaitMessage(PPselectB);
                                byte[] hcackb = (byte[]) ((SecsItem) dataB.get("HCACK")).getData();
                                if (hcackb != null && hcackb[0] == 0) {
                                    portBRcpName = recipeName;
                                    selectOkFlag = true;
                                }
                                logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb[0] + " means " + ACKDescription.description(hcackb[0], "HCACK"));
                                description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb[0] + " means " + ACKDescription.description(hcackb[0], "HCACK");
                            }
                        }
                        logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + ppExecName);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                }
            }
            if (!selectOkFlag) {
                UiLogUtil.appendLog2EventTab(deviceCode, "不存在领料程序，确认是否成功SET UP！\n");
            }
        }

        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", 0);
        resultMap.put("Description", description);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselectA(String recipeName) {
        DataMsgMap PPselectA = new DataMsgMap("s2f41outPPSelectA", activeWrapper.getDeviceId());
        PPselectA.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        PPselectA.put("PPID", recipeName);
        PPselectA.put("LotId", lotId);
        try {
            if (cassUseMap.get("A")) {
                if ("".equals(portARcpName) || !portARcpName.equals(recipeName)) {
                    DataMsgMap dataA = activeWrapper.sendAwaitMessage(PPselectA);
                    byte[] hcacka = (byte[]) ((SecsItem) dataA.get("HCACK")).getData();
                    if (hcacka != null && hcacka[0] == 0) {
                        portARcpName = recipeName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka[0] + " means " + ACKDescription.description(hcacka[0], "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka[0] + " means " + ACKDescription.description(hcacka[0], "HCACK");
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
    public Map sendS2F41outPPselectB(String recipeName) {
        DataMsgMap PPselectB = new DataMsgMap("s2f41outPPSelectB", activeWrapper.getDeviceId());
        PPselectB.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        PPselectB.put("PPID", recipeName);
        PPselectB.put("LotId", lotId);
        try {
            if (cassUseMap.get("B")) {
                if ("".equals(portARcpName) || !portARcpName.equals(recipeName)) {
                    DataMsgMap dataB = activeWrapper.sendAwaitMessage(PPselectB);
                    byte[] hcackb = (byte[]) ((SecsItem) dataB.get("HCACK")).getData();
                    if (hcackb != null && hcackb[0] == 0) {
                        portBRcpName = recipeName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb[0] + " means " + ACKDescription.description(hcackb[0], "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb[0] + " means " + ACKDescription.description(hcackb[0], "HCACK");
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

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
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
        try {
            //更新设备模型状态
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
            if (execRecipe == null) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Unique或Gold版本，将无法执行开机检查。请联系PE处理！\n");
            }
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！\n");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();

            if (equipStatus.equalsIgnoreCase("SETUP")) {
                portARcpName = "";
                portBRcpName = "";
            }
            String busniessMod = deviceInfoExt.getBusinessMod();
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            } else //开机check
                if (equipStatus.equalsIgnoreCase("run")) {
                    if (this.checkLockFlagFromServerByWS(deviceCode)) {
                        UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                        this.holdDevice();
                    }
                    String trackInRcpName = deviceInfoExt.getRecipeName();
                    if (!"".equals(portARcpName) && !trackInRcpName.equals(portARcpName)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "领料程序与Port口程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " PortA:" + portARcpName);
                        this.holdDevice();
                    }
                    if (!"".equals(portBRcpName) && !trackInRcpName.equals(portBRcpName)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "领料程序与Port口程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " PortB:" + portBRcpName);
                        this.holdDevice();
                    }
                    if (!ppExecName.equals(trackInRcpName)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "已选程序与领料程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " 已选程序 " + ppExecName + "\n");
                        this.holdDevice();
                    }
                    if (execRecipe == null) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Unique或Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！\n");
                        //不允许开机
                        this.holdDevice();
                    }
                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    this.startCheckRecipePara(checkRecipe);
                }
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
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
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                DeviceService deviceService = new DeviceService(sqlSession);
                DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                if (deviceInfoExt != null) {
                    String trackInRcpName = deviceInfoExt.getRecipeName();
                    if (trackInRcpName != null && !"".equals(trackInRcpName)) {
                        this.sendS2F41outPPselect(trackInRcpName);
                    }
                }
                sqlSession.close();
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
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    DeviceService deviceService = new DeviceService(sqlSession);
                    DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                    sqlSession.close();
                    String trackInRcpName = deviceInfoExt.getRecipeName();
                    if (trackInRcpName == null || "".equals(trackInRcpName)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "领料信息不完整！\n");
                    } else {
                        ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
                        boolean selectOkFlag = false;
                        for (int i = 0; i < eppd.size(); i++) {
                            String rcpNameString = eppd.get(i).toString();
                            if (rcpNameString.equals(trackInRcpName)) {
                                if (ceid == 211) {
                                    this.sendS2F41outPPselectA(trackInRcpName);
                                    selectOkFlag = true;
                                } else {
                                    this.sendS2F41outPPselectB(trackInRcpName);
                                    selectOkFlag = true;
                                }
                            }
                        }
                        if (!selectOkFlag) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "不存在领料程序，确认是否成功提交改机！\n");
                        }
                    }
                }
            }
            if (ceid == 1000000401) {
                String DFMppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
                UiLogUtil.appendLog2SecsTab(deviceCode, "DFM部分使用的程序为： " + DFMppExecName + "\n");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {

        }
    }

    private void checkGrindingWheelLife() {
        List dataIdList = new ArrayList();
        long z1 = 2012L;
        long z2 = 2013L;
        long z3 = 2014L;
        dataIdList.add(z1);
        dataIdList.add(z2);
        dataIdList.add(z3);
        Map dataMap = getSpecificSVData(dataIdList);
        String z1life = dataMap.get(z1).toString();
        String z2life = dataMap.get(z2).toString();
        String z3life = dataMap.get(z3).toString();
        UiLogUtil.appendLog2SecsTab(deviceCode, "当前磨轮可用量: Z1[" + z1life + "],Z2[" + z2life + "],Z3[" + z3life + "]");
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
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
            Map cmdMap = this.sendS2f41Cmd("PAUSE");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                holdSuccessFlag = true;
                this.setAlarmState(2);
            } else {
                holdSuccessFlag = false;
                this.setAlarmState(2);
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        return this.sendS2f41Cmd("RESUME");
    }
    // </editor-fold> 

    @Override
    public String checkEquipStatus() {
        findEqptStatus();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus) || "RUN".equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！下载失败！";
        }
        return "0";
    }


}
