/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.disco.bg;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
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
import cn.tzauto.octopus.secsLayer.resolver.disco.DiscoRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

/**
 * @author njtz
 */
@SuppressWarnings("serial")
public class DiscoDGP8761Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(DiscoDGP8761Host.class.getName());
    private Map<String, Boolean> cassUseMap = new HashMap<>();
    private String portARcpName = "";
    private String portBRcpName = "";

    public DiscoDGP8761Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        EquipStateChangeCeid=10150;
    }

    public Object clone() {
        DiscoDGP8761Host newEquip = new DiscoDGP8761Host(deviceId,
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
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (!this.getControlState().equals(GlobalConstant.CONTROL_REMOTE_ONLINE)) {
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
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    long ceid = 0l;
                    try {
                        ceid = (long) msg.get("CEID");
                        Map panelMap = new HashMap();
                        if (ceid == 83 || ceid == 84) {
                            if (ceid == 83) {
                                panelMap.put("ControlState", GlobalConstant.CONTROL_LOCAL_ONLINE);       //Online_Local
                            } else {
                                panelMap.put("ControlState", GlobalConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
                            }
                            changeEquipPanel(panelMap);
                        } else if (ceid == 211 || ceid == 221 || ceid ==1000000401) {
                            processS6F11PPselect(msg);
                        } else if(ceid == EquipStateChangeCeid){
                            processS6F11EquipStatusChange(msg);
                        } else if(ceid == 77L){
                            //pp select
                            findDeviceRecipe();
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
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
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack[0]);
                long ceid = 0l;
                try {
                    ceid = (long) data.get("CEID");
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
                if ((ceid < 10150 && ceid > 10080) || (ceid == 223 || ceid == 211 || ceid == 221 || ceid == 213) || ceid == 44) {
                    this.inputMsgQueue.put(data);
                }
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
                    Map dataA =this.sendS2F41outPPselectA();
                    byte hcacka = (byte)  dataA.get("HCACK");
                    if ( hcacka == 0) {
                        portARcpName = ppExecName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka + " means " + ACKDescription.description(hcacka, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka + " means " + ACKDescription.description(hcacka, "HCACK");
                }
                if (cassUseMap.get("B")) {
                    Map dataB =this.sendS2F41outPPselectB();
                    byte hcackb = (byte) ( dataB.get("HCACK"));
                    if ( hcackb == 0) {
                        portBRcpName = ppExecName;
                    }
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb + " means " + ACKDescription.description(hcackb, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb + " means " + ACKDescription.description(hcackb, "HCACK");
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
        Map resultMap = new HashMap();
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {

                if (cassUseMap.get("A")) {
                    Map cpmap = new HashMap();
                    cpmap.put("PPID_A", ppExecName);
                    cpmap.put("LOTID_A", lotId);
                    Map cpNameMap = new HashMap();
                    cpNameMap.put("PPID_A", SecsFormatValue.SECS_ASCII);
                    cpNameMap.put("LOTID_A", SecsFormatValue.SECS_ASCII);
                    Map cpValueMp = new HashMap();
                    cpValueMp.put(ppExecName, SecsFormatValue.SECS_ASCII);
                    cpValueMp.put(lotId, SecsFormatValue.SECS_ASCII);
                    List cplist = new ArrayList();
                    cplist.add("PPID_A");
                    cplist.add("LOTID_A");
                    DataMsgMap dataA = activeWrapper.sendS2F41out("PP_SELECT", cplist, cpmap, cpNameMap, cpValueMp);

                    byte hcacka = (byte)  dataA.get("HCACK");
                    if ( hcacka == 0) {
                        portARcpName = ppExecName;
                    }
                    resultMap.put("HCACK",hcacka);
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port A =" + hcacka + " means " + ACKDescription.description(hcacka, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcacka + " means " + ACKDescription.description(hcacka, "HCACK");
                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + ppExecName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

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
        Map resultMap = new HashMap();
        try {
            if ("setup".equalsIgnoreCase(equipStatus) || "run".equalsIgnoreCase(equipStatus)) {
                if (cassUseMap.get("B")) {
                    Map cpmap = new HashMap();
                    cpmap.put("PPID_B", ppExecName);
                    cpmap.put("LOTID_B", lotId);
                    Map cpNameMap = new HashMap();
                    cpNameMap.put("PPID_B", SecsFormatValue.SECS_ASCII);
                    cpNameMap.put("LOTID_B", SecsFormatValue.SECS_ASCII);
                    Map cpValueMp = new HashMap();
                    cpValueMp.put(ppExecName, SecsFormatValue.SECS_ASCII);
                    cpValueMp.put(lotId, SecsFormatValue.SECS_ASCII);
                    List cplist = new ArrayList();
                    cplist.add("PPID_B");
                    cplist.add("LOTID_B");
                    DataMsgMap dataA = activeWrapper.sendS2F41out("PP_SELECT", cplist, cpmap, cpNameMap, cpValueMp);

                    byte hcackb = (byte)  dataA.get("HCACK");
                    if ( hcackb == 0) {
                        portARcpName = ppExecName;
                    }
                    resultMap.put("HCACK",hcackb);
                    logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK at Port B =" + hcackb + " means " + ACKDescription.description(hcackb, "HCACK"));
                    description = "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcackb + " means " + ACKDescription.description(hcackb, "HCACK");
                }
            }
            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + ppExecName);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", description);
        return resultMap;
    }// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 

    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        //回复s6f11消息
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
            findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
//        Map map = new HashMap();
//        //TODO 此设备可以反馈前一状态
//        map.put("EquipStatus", equipStatus);
//        changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
        if (execRecipe == null) {
            //TODO  这里需要讨论做试产时的情况
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
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
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                this.holdDevice();
                return;
            }
            if (!"".equals(portARcpName) && !"".equals(portBRcpName)) {
                if (!ppExecName.equals(portARcpName) || !ppExecName.equals(portBRcpName)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "已选程序与Port口程序不一致，设备被锁定！请联系ME处理！");
                    this.holdDevice();
                    return;
                }
            }
            if (execRecipe == null) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Unique或Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                //不允许开机
                this.holdDevice();
                return;
            }
            Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
            this.startCheckRecipePara(checkRecipe);
        }
        sqlSession.close();
    }



    private void processS6F11PPselect(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
//            if (ceid == 77) {
//                //ppselect 事件
//                ppExecName = ((MsgSection) data.get("PPExecName")).getData().toString();
//                portARcpName = "";
//                portARcpName = "";
//            }
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
                sendS1F3Check();
                String DFMppExecName = ppExecName;
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "DFM使用的程序为： " + DFMppExecName);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
//        byte[] ppbody = (byte[]) ((MsgSection) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, 1, recipePath);
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
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
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
        if (GlobalConstant.STATUS_RUN.equalsIgnoreCase(equipStatus) || "RUN".equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！下载失败！";
        }
        return "0";
    }


}
