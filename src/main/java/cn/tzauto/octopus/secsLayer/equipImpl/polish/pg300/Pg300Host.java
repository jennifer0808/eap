package cn.tzauto.octopus.secsLayer.equipImpl.polish.pg300;


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
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.pg300.PG300RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pg300Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(Pg300Host.class);
    private String portId = "";

    public Pg300Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
    }


    public Object clone() {
        Pg300Host newEquip = new Pg300Host(deviceId,
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
                if (rptDefineNum < 2) {
//                    sendS1F1out();
//                    //为了能调整为online remote
                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    //获取lot号/
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();//得到监听到的东西
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.error("Exception:", e);
            }
        }
    }

    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            secsMsgTimeoutTime = 0;
            LastComDate = System.currentTimeMillis();
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                //回复s6f11消息
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
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
    private void sendS1F3CheckPort() {
        DataMsgMap data = null;
        try {
            List svidlist = new ArrayList();
            svidlist.add(109L);
            svidlist.add(111L);
            data = activeWrapper.sendS1F3out(svidlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("SV") == null) {
            return;
        }
        ArrayList listtmp = (ArrayList) data.get("SV");
        String port1Status = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        String port2Status = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(1)), deviceType);
        if ("Ready".equalsIgnoreCase(port1Status)) {
            this.portId = "1";
        } else if ("Ready".equalsIgnoreCase(port2Status)) {
            this.portId = "2";
        } else {
            this.portId = "";
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S2FX Code">
    @SuppressWarnings("unchecked")
    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        if (!"1".equals(this.portId) && !"2".equals(this.portId)) {
            return null;
        }
        byte hcack = (byte) 0;
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put("LOTID", "testlotid");
            cpmap.put("PPID", recipeName);
            cpmap.put("PORTID", this.portId);
            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put("LOTID", SecsFormatValue.SECS_ASCII);
            cpNameFromatMap.put("PPID", SecsFormatValue.SECS_ASCII);
            cpNameFromatMap.put("PORTID", SecsFormatValue.SECS_ASCII);
            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put("testlotid", SecsFormatValue.SECS_ASCII);
            cpValueFromatMap.put(recipeName, SecsFormatValue.SECS_ASCII);
            cpValueFromatMap.put(this.portId, SecsFormatValue.SECS_1BYTE_UNSIGNED_INTEGER);
            List list = new ArrayList();
            list.add("LOTID");
            list.add("PPID");
            list.add("PORTID");
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, list, cpmap, cpNameFromatMap, cpValueFromatMap);

            hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + e.getMessage());
        }
        return resultMap;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    public void processS6F11in(DataMsgMap data) {
        try {
            long ceid = (long) data.get("CEID");
            if (ceid == 39 || ceid == 44) {
                //sml中s6f11equipstatuschange data1，data2表示状态前后的两个值，需要测试得到
                processS6F11EquipStatusChange(data);
            } else if (ceid == 8 || ceid == 9 || ceid == 10 || ceid == 45 ||
                    ceid == 46 || ceid == 57 || ceid == 59 || ceid == 56 || ceid == 58) {
                processS6F11EquipStatus(data);
            } else if (ceid == 37) {
                findDeviceRecipe();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatus(DataMsgMap data) {
        try {
            long ceid = (long) data.get("CEID");
//           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "收到CEID: " + ceid);
            if (ceid == 9) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 10) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 8) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            } else if (ceid == 45 || ceid == 46) {
                checkPortStatusAndPPSelectToLocal();
            } else if (ceid == 57 || ceid == 59 || ceid == 56 || ceid == 58) {
                sendS2f41Cmd("GRANT");
            }
            super.sendS1F3Check();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        String ppid = recipeName;
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = PG300RecipeUtil.pg300RecipeTran(recipePath);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Descrption", " Recive the recipe " + ppid + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

    private void checkPortStatusAndPPSelectToLocal() {
        sendS1F3CheckPort();
        if ("".equals(this.portId)) {
            return;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null) {
            String recipeName = deviceInfoExt.getRecipeName();
            Map reMap = sendS7F19out();
            if (reMap != null && reMap.get("eppd") != null) {
                ArrayList<String> recipeNameList = (ArrayList) reMap.get("eppd");
                if (recipeNameList.contains(recipeName)) {
                    Map resultMap = sendS2F41outPPselect(recipeName);
                    if (resultMap != null && !resultMap.isEmpty()) {
                        if ("0".equals(String.valueOf(resultMap.get("HCACK"))) || "4".equals(String.valueOf(resultMap.get("HCACK")))) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "PPSelect成功，PPID=" + recipeName);
                            //选完程序切local状态
                            localDeviceAndShowDetailInfo();
                        } else {
                            Map eqptStateMap = super.findEqptStatus();//失败上报机台状态
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + "；原因：" + String.valueOf(resultMap.get("Description")) + "，机台状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                        }
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + ", 设备消息回复错误，请联系CIM人员处理");
                    }
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + ", 设备Recipe列表中没有该Recipe，请重新下载");
                }
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "无法获取设备Recipe列表，请检查设备通信");
            }
        }
    }


//事件报告

    private void initRptPara() {
//sendS6F23clear();
        //重写事件 报告
//            sendS2F33out(50007l, 902l, 906l);
//            sendS2F35out(50007l, 50007l, 50007l);
//            sendS2F37out(50007l);
        sendS2F37outCloseAll();
        sendS2F33clear();
        sendS2F35clear();
        sendS2F33init();
        sendS2F35init();
        sendS2F37outAll();   //开启所有事件报告
    }


    @SuppressWarnings("unchecked")
    public void sendS6F23clear() {
        DataMsgMap s2f37outAll = new DataMsgMap("s6f23out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            activeWrapper.sendAwaitMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F33init() {
        // TODO: 2019/4/10  sml 文件中的结构为多个list,且参数值固定
        try {
            List<Long> list = new ArrayList();
            list.add(27L);
            activeWrapper.sendS2F33out(0, svFormat, 1, rptFormat, list, svFormat);
            list.clear();
            list.add(105L);
            list.add(106L);
            activeWrapper.sendS2F33out(0, svFormat, 2, rptFormat, list, svFormat);
            list.clear();
            list.add(113L);
            activeWrapper.sendS2F33out(0, svFormat, 8, rptFormat, list, svFormat);
            list.clear();
            list.add(114L);
            activeWrapper.sendS2F33out(0, svFormat, 9, rptFormat, list, svFormat);
            list.clear();
            list.add(412L);
            activeWrapper.sendS2F33out(0, svFormat, 10, rptFormat, list, svFormat);
            list.clear();
            list.add(462L);
            activeWrapper.sendS2F33out(0, svFormat, 11, rptFormat, list, svFormat);
            list.clear();
            list.add(512L);
            activeWrapper.sendS2F33out(0, svFormat, 12, rptFormat, list, svFormat);
            list.clear();
            list.add(566L);
            list.add(567L);
            activeWrapper.sendS2F33out(0, svFormat, 14, rptFormat, list, svFormat);
            list.clear();
            list.add(568L);
            list.add(569L);
            activeWrapper.sendS2F33out(0, svFormat, 15, rptFormat, list, svFormat);
            list.clear();
            list.add(108L);
            activeWrapper.sendS2F33out(0, svFormat, 16, rptFormat, list, svFormat);
//            sendS2F33out()
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35init() {
        //// TODO: 2019/4/10 sml 文件中的结构为多个list
        try {
            List<Long> rptlist = new ArrayList();
            rptlist.add(1L);
            activeWrapper.sendS2F35out(1, svFormat, 1, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 2, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 8, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 9, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 10, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 11, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 12, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 14, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 15, ceFormat, rptlist, rptFormat);
            activeWrapper.sendS2F35out(1, svFormat, 16, ceFormat, rptlist, rptFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public Map pauseDevice() {
        return this.sendS2f41Cmd("PAUSE");
    }

    public Map resumeDevice() {
        return this.sendS2f41Cmd("RESUME");
    }

    public Map localDevice() {
        return this.sendS2f41Cmd("LOCAL");
    }

    @Override
    public Map holdDevice() {
        sendS1F3Check();
        if (!this.controlState.equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "当前设备不在Remote状态，无法进行锁机");
            return null;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            super.sendS2f41Cmd("STOP");
            Map map = this.sendS2f41Cmd("PAUSE");
            if ((byte) map.get("HCACK") == 0) {
                this.setAlarmState(2);
            }
            return map;
        } else {

            //todo 显示界面锁机日志
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        Map map = this.sendS2f41Cmd("RESUME");
        if ((byte) map.get("HCACK") == 0) {
            this.setAlarmState(0);
        }
        return map;
    }

    public boolean localDeviceAndShowDetailInfo() {
        Map resultMap = new HashMap();
        resultMap = localDevice();
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已经被切至Local");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已处于Local状态");
                return true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + String.valueOf(resultMap.get("Description")));
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "设备切Local失败，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                return false;
            }
        } else {
            return false;
        }
    }


}
