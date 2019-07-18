package cn.tzauto.octopus.secsLayer.equipImpl.vision.plasmacleaning;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.tepla.GIGA690RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

public class VSP88AHHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(VSP88AHHost.class.getName());

    public VSP88AHHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
//        StripMapUpCeid = 15339L;
    }


    @Override
    public Object clone() {
        VSP88AHHost newEquip = new VSP88AHHost(deviceId,
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
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                super.replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
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
//            //重新定义Learn Device事件
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

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
//    @SuppressWarnings("unchecked")
//    @Override
//    public Map sendS1F3Check() {
//        List listtmp = getNcessaryData();
//        equipStatus = ACKDescription.descriptionStatus(listtmp.get(0).toString(), deviceType);
//        ppExecName = (String) listtmp.get(1);
//        ppExecName = ppExecName.replaceAll(".dbrcp", "");
//        Map panelMap = new HashMap();
//        panelMap.put("EquipStatus", equipStatus);
//        panelMap.put("PPExecName", ppExecName);
//        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
//        panelMap.put("ControlState", controlState);
//        changeEquipPanel(panelMap);
//        // sendS2F15outLearnDevice(151126402L, "disabled");
//        return panelMap;
//    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

//    @Override
//    public Map holdDevice() {
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        sqlSession.close();
//        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            Map cmdMap = this.sendS2f41Cmd("ABORT");
//            if (cmdMap.get("HCACK").toString().equals("0") || (byte) cmdMap.get("HCACK") == 4) {
//                logger.info("锁机成功！");
//                this.setAlarmState(2);
//                holdSuccessFlag = true;
//            } else {
//                holdSuccessFlag = false;
//            }
//            return cmdMap;
//        } else {
//            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
//            return null;
//        }
//    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = -12345679;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            if (ceid == 1 || ceid == 2 || ceid == 3 || ceid == 8
                    || ceid == 9 || ceid == 11 || ceid == 102) {
                processS6F11EquipStatusChange(data);
            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold>S10F3之前发送
//    @Override
//    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
//        Recipe recipe = setRecipe(recipeName);
//        recipePath = getRecipePathByConfig(recipe);
//        byte[] ppbody = (byte[]) getPPBODY(recipeName);
//        cn.tzauto.octopus.common.resolver.TransferUtil.setPPBody(ppbody, 1, recipePath);
//        List<RecipePara> recipeParaList = null;
//        try {
//            Map paraMap = GIGA690RecipeUtil.transferFromFile(recipePath);
//            recipeParaList = GIGA690RecipeUtil.transferFromDB(paraMap, deviceType);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
//        Map resultMap = new HashMap();
//        resultMap.put("msgType", "s7f6");
//        resultMap.put("deviceCode", deviceCode);
//        resultMap.put("recipe", recipe);
//        resultMap.put("recipeNameMapping", null);
//        resultMap.put("recipeParaList", recipeParaList);
//        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
//        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
//        return resultMap;
//    }

    // </editor-fold>
}
