package cn.tzauto.octopus.secsLayer.equipImpl.asm.mold;

import cn.tfinfo.jcauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tfinfo.jcauto.octopus.biz.device.service.DeviceService;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.Recipe;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipePara;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tfinfo.jcauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.secsLayer.resolver.asm.ASMIdeal3GRecipeUtil;
import cn.tfinfo.jcauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tfinfo.jcauto.octopus.common.globalConfig.GlobalConstants;
import cn.tfinfo.jcauto.octopus.common.util.tool.JsonMapper;
import cn.tfinfo.jcauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.userapi.MsgArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.DataMsgMap;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import com.alibaba.fastjson.JSONArray;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;


/**
 *
 * @author njtz
 */
@SuppressWarnings("serial")
public class ASMIdeal3GHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(ASMIdeal3GHost.class.getName());

    public ASMIdeal3GHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int rcpType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, rcpType, iconPtah);
    }

    public ASMIdeal3GHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int rcpType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, rcpType, iconPtah);
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
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();//
                    //获取lot号
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    try {
                        processS6F11EquipStatus(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    ppExecName = (String) ((SecsItem) msg.get("PPExecName")).getData();
                    Map map = new HashMap();
                    map.put("PPExecName", ppExecName);
                    changeEquipPanel(map);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
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
            } else if (tagName.equalsIgnoreCase("s6f11equipstatuschange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                long ceid = 0l;
                try {
                    ceid = data.getSingleNumber("CollEventID");
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
                if (ceid == 8) {  //recipe改变事件
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().contains("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f30in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s7f3in")) {
                processS7F3in(data);
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

//    public void processS1F4statein(DataMsgMap data) {
//        if (data == null) {
//            return;
//        }
//        long ProcessState = 0l;
//        long ControlState = 0l;
//        long AlarmState = 0l;
//        long AlarmID = 0l;
//        try {
//            ProcessState = data.getSingleNumber("ProcessState");
//            ControlState = data.getSingleNumber("ControlState");
//
//            AlarmID = data.getSingleNumber("AlarmID");
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        String CommState = (String) ((SecsItem) data.get("CommState")).getData();
//        System.out.println("CommState = " + CommState);
//        System.out.println("ControlState = " + ControlState);
//        System.out.println("ProcessState = " + ProcessState);
//        System.out.println("AlarmState = " + AlarmState);
//        System.out.println("AlarmID = " + AlarmID);
//    }
//
//    public void processS1F4recipein(DataMsgMap data) {
//        if (data == null) {
//            return;
//        }
//        long PPExecName = 0l;
//        //long   RecipeHanding =0l;
//        try {
//            PPExecName = data.getSingleNumber("PPExecName");
//        } catch (Exception e) {
//        }
//        System.out.println("PPExecName = " + PPExecName);
//    }
//
//    @SuppressWarnings("unchecked")
//    public void sendS2f33out() {
//
//        DataMsgMap s2f33out = new DataMsgMap("s2f33out", activeWrapper.getDeviceId());
//        s2f33out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        long[] dataid = new long[1];
//        dataid[0] = 10012;
//        long[] reportid = new long[1];
//        reportid[0] = 100012;
//        long[] variableid = new long[1];
//        variableid[0] = 2045;
//        s2f33out.put("DataID", dataid);
//        s2f33out.put("ReportID", reportid);
//        s2f33out.put("VariableID", variableid);
//        //s1f13out.put("SoftRev", "9.25.5");
//        try {
//            activeWrapper.sendAwaitMessage(s2f33out);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
//
//
//    public Map sendS2F41outMuiltPPselect(List ppidList) {
//
//        DataMsgMap s2f41out = new DataMsgMap("s2f41muiltout", activeWrapper.getDeviceId());
//        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        s2f41out.put("Remotecommand", "PP-SELECT");
//        for (int i = 0; i < ppidList.size(); i++) {
//            s2f41out.put("Commandparametername" + i, "PPID");
//            s2f41out.put("Commandparametervalue" + i, (String) ppidList.get(i));
//        }
//        try {
//            msgdata = activeWrapper.sendAwaitMessage(s2f41out);
//            logger.debug("The equip " + deviceCode + " request to PP-select the ppid: " + (String) ppidList.get(0));
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        byte[] hcack = (byte[]) ((SecsItem) msgdata.get("HCACK")).getData();
//        logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
//        Map resultMap = new HashMap();
//        resultMap.put("msgType", "s2f42");
//        resultMap.put("deviceCode", deviceCode);
//        resultMap.put("prevCmd", "PP-SELECT");
//        resultMap.put("HCACK", hcack[0]);
//        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
//        return resultMap;
//    }
//
//    public void sendS7F1Multi(List l) {
//        DataMsgMap s7f1multi = new DataMsgMap("s7f1multiout", activeWrapper.getDeviceId());
//        s7f1multi.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        SecsItem secsItemmain = new SecsItem();
//        secsItemmain.setFormatCode(FormatCode.SECS_LIST);
//        ArrayList listmain = new ArrayList();
//        for (int i = 0; i < l.size(); i++) {
//            SecsItem secsItemsub = new SecsItem();
//            ArrayList list = new ArrayList();
//            String ppid = (String) l.get(i);
//            SecsItem secsItemppid = new SecsItem(ppid, FormatCode.SECS_ASCII);
//            list.add(secsItemppid);
//            long[] pplength = new long[1];
//            pplength[0] = TransferUtil.getPPLength(recipePath + ppid);
//            SecsItem secsItemlength = new SecsItem(pplength, FormatCode.SECS_4BYTE_UNSIGNED_INTEGER);
//            list.add(secsItemlength);
//            secsItemsub.setData(list);
//            listmain.add(secsItemsub);
//            //s7f1multi.put("ProcessprogramID"+i, ppid);
//            //s7f1multi.put("Length"+i, l); 
//        }
//        secsItemmain.setData(listmain);
//        s7f1multi.put("RESULT", secsItemmain);
//        try {
//            activeWrapper.sendAwaitMessage(s7f1multi);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
//
//    public void sendS7F3Multi(List l) {
//        DataMsgMap s7f3multi = new DataMsgMap("s7f3multiout", activeWrapper.getDeviceId());
//        s7f3multi.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        SecsItem secsItemmain = new SecsItem();
//        secsItemmain.setFormatCode(FormatCode.SECS_LIST);
//        ArrayList listmain = new ArrayList();
//        for (int i = 0; i < l.size(); i++) {
//            SecsItem secsItemsub = new SecsItem();
//            ArrayList list = new ArrayList();
//            String ppid = (String) l.get(i);
//            SecsItem secsItemppid = new SecsItem(ppid, FormatCode.SECS_ASCII);
//            list.add(secsItemppid);
//            String ppbody = (String) TransferUtil.getPPBody(1, recipePath + ppid).get(0);
//            SecsItem secsItemppbody = new SecsItem(ppbody, FormatCode.SECS_ASCII);
//            list.add(secsItemppbody);
//            secsItemsub.setData(list);
//            listmain.add(secsItemsub);
//            //s7f3multi.put("ProcessprogramID"+i, ppid);
//            //s7f3multi.put("Processprogram"+i, ppbody);            
//        }
//        secsItemmain.setData(listmain);
//        s7f3multi.put("RESULT", secsItemmain);
//        try {
//            activeWrapper.sendAwaitMessage(s7f3multi);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//    }
//    //TODO 组合型recipe（ASM3G）删除指令 要有必要的记录日志，记录操作人，操作时间。
//
//    public Map sendS7F17outMulit(List ppid, EquipHost equipHost) {
//        activeWrapper = equipHost.getMli();
//
//        deviceCode = equipHost.getDeviceCode();
//        DataMsgMap s7f17outmuilt = new DataMsgMap("s7f17outmuilt", activeWrapper.getDeviceId());
//        s7f17outmuilt.setTransactionId(activeWrapper.getNextAvailableTransactionId());
//        for (int i = 0; i < ppid.size(); i++) {
//            s7f17outmuilt.put("ProcessprogramID" + i, ppid.get(i));
//        }
//        try {
//            msgdata = activeWrapper.sendAwaitMessage(s7f17outmuilt);
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        byte[] ackc7 = (byte[]) ((SecsItem) msgdata.get("AckCode")).getData();
//        if (ackc7[0] == 0) {
//            logger.debug("The recipe " + ppid + " has been delete from " + deviceCode);
//        } else {
//            logger.error("Delete recipe " + ppid + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7, "ACKC7"));
//        }
//        Map resultMap = new HashMap();
//        resultMap.put("msgType", "s7f18");
//        resultMap.put("deviceCode", deviceCode);
//        resultMap.put("ppid", ppid);
//        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
//        return resultMap;
//    }
    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
//    @Override
//    @SuppressWarnings("unchecked")
//    public Map sendS1F3Check() {
//        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s1f3out.setTransactionId(transactionId);
//        long[] equipStatuss = new long[1];
//        long[] pPExecNames = new long[1];
//        long[] controlStates = new long[1];
//        DataMsgMap data = null;
//        try {
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            RecipeService recipeService = new RecipeService(sqlSession);
//            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
//            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
//            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
//            sqlSession.close();
//            s1f3out.put("EquipStatus", equipStatuss);
//            s1f3out.put("PPExecName", pPExecNames);
//            s1f3out.put("ControlState", controlStates);
//            logger.info("Ready to send Message S1F3==============>" + JSONArray.toJSON(s1f3out));
//            data = activeWrapper.sendAwaitMessage(s1f3out);
//            logger.info("Received Message S1F3===================>" + JSONArray.toJSON(data));
//        } catch (Exception e) {
//            logger.error("Wait for get meessage directly error：" + e);
//        }
//        if (data == null || data.isEmpty()) {
//            int i = 0;
//            logger.info("Can not get value directly,will try to get value from message queue");
//            while (i < 5) {
//                DataMsgMap DataMsgMap = this.waitMsgValueMap.get(transactionId);
//                logger.info("try===>" + 1);
//                if (DataMsgMap != null) {
//                    data = DataMsgMap;
//                    logger.info("Had get value from message queue ===>" + JSONArray.toJSON(data));
//                    break;
//                } else {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException ex) {
//                        logger.error("Exception:", ex);
//                    }
//                }
//                i++;
//            }
//            this.waitMsgValueMap.remove(transactionId);
//            if (i >= 5) {
//                UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备状态信息失败，请检查设备通讯状态！");
//                logger.error("获取设备:" + deviceCode + "状态信息失败.");
//                return null;
//            }
//        }
//        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
//        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
//        equipStatus = ACKDescription.descriptionStatus((long) listtmp.get(0), deviceType);
//        ppExecName = (String) listtmp.get(1);
//        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
//
//        Map panelMap = new HashMap();
//        panelMap.put("EquipStatus", equipStatus);
//        panelMap.put("PPExecName", ppExecName);
//        panelMap.put("ControlState", controlState);
//        changeEquipPanel(panelMap);
//        return panelMap;
//    }
    public List sendS1F3PressCheckout() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3pressout", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] press1SV = new long[1];
        press1SV[0] = 104l;
        s1f3out.put("Press1", press1SV);
        long[] press2SV = new long[1];
        press2SV[0] = 204l;
        s1f3out.put("Press2", press2SV);
        long[] press3SV = new long[1];
        press3SV[0] = 304l;
        s1f3out.put("Press3", press3SV);
        long[] press4SV = new long[1];
        press4SV[0] = 404l;
        s1f3out.put("Press4", press4SV);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null || ((SecsItem) data.get("RESULT")).getData() == null) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        return listtmp;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    protected void processS6F11EquipStatus(DataMsgMap data) {
        //回复s6f11消息
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 5) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 6) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) { //待验证
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        updateCommStateInExt();
        UiLogUtil.appendLog2SecsTab(deviceCode, "收到事件报告CEID：" + ceid);

        //更新页面显示内容
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "CEID");
        sqlSession.close();
        if (recipeTemplates != null && recipeTemplates.size() > 0) {
            for (int j = 0; j < recipeTemplates.size(); j++) {
                long ceidtmp = Long.parseLong(recipeTemplates.get(j).getDeviceVariableId());
                if (ceid == ceidtmp) {
                    UiLogUtil.appendLog2SecsTab(deviceCode, "CEID:" + ceid + " 描述：" + recipeTemplates.get(j).getParaDesc());
                    break;
                }
            }
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            boolean dataReady = false;
            //判断当前执行程序是否是清模程序 Y代表清模程序
            boolean isCleanRecipe = false;
            List<Recipe> cleanRecipes = recipeService.searchRecipeByRcpType(ppExecName, "Y");
            if (cleanRecipes != null && cleanRecipes.size() > 1) {
                isCleanRecipe = true;
            }
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备模型信息，不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
                dataReady = true;
            }

            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();

            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
                getUsingPress();
                //开机check Press使用情况
                checkPressUseState(deviceService, sqlSession);
//                if (!checkPressUseState(deviceService, sqlSession)) {
//                    return;
//                }
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                }
                //查询trackin时的recipe和GoldRecipe
                Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));

                //查询客户端数据库是否存在GoldRecipe
                if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                    hasGoldRecipe = false;
                }

                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台
                if (!this.checkLockFlagFromServerByWS(deviceCode)) {
                    //根据检查模式执行开机检查逻辑
                    //1、A1-检查recipe名称是否一致
                    //2、A-检查recipe名称和参数
                    //3、B-检查SV
                    //4、AB都检查

                    if (startCheckMod != null && !"".equals(startCheckMod)) {
                        checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                        if (!checkResult) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                            //不允许开机
                            holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if ("Unique".equals(downloadRcpVersionType)) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                            if (!hasGoldRecipe) {
                                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo();
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                            }

                        }
                    } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
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
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = ASMIdeal3GRecipeUtil.transRcpParaFromDB(recipePath, deviceType);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
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

    @SuppressWarnings("unchecked")
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s7f19out.setTransactionId(transactionId);
        DataMsgMap data = null;

        try {
//            data = activeWrapper.sendAwaitMessage(s7f19out);
            data = handleOverTime(s7f19out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
//        if (data == null || data.get("EPPD") == null) {
//            data = this.getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
//        }
        if (data == null || data.get("EPPD") == null) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "从设备获取recipe列表信息失败，请检查设备通讯状态！");
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("EPPD")).getData();
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            resultMap.put("eppd", listtmp);
        }
        return resultMap;
    }
    // </editor-fold>

    public DataMsgMap handleOverTime(DataMsgMap s7f19out) {
        final DataMsgMap s7f19outF = s7f19out;
        DataMsgMap result = null;
//        ExecutorService exec = Executors.newFixedThreadPool(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Callable<DataMsgMap> call = new Callable<DataMsgMap>() {

            public DataMsgMap call() throws Exception {
                //开始执行耗时操作  
                return activeWrapper.sendAwaitMessage(s7f19outF);
            }
        };

        Future<DataMsgMap> future = exec.submit(call);
        try {
            result = future.get(2000, TimeUnit.MILLISECONDS); //任务处理超时时间设为 2 秒  
            logger.info("任务执行成功");
        } catch (TimeoutException e) {
            future.cancel(true);//取消该Future里关联的Callable任务
            logger.error("处理超时....", e);
        } catch (Exception e) {
            future.cancel(true);
            logger.error("处理失败....", e);
        } finally {
            // 关闭线程池  
            exec.shutdown();
            return result;
        }
    }

    //获取当前使用的Press
    public void getUsingPress() {
        List pressResults = sendS1F3PressCheckout();
        super.pressUseMap.clear();
        String pressStatusAll = "";
        for (int i = 0; i < pressResults.size(); i++) {
            String pressStatus = String.valueOf(pressResults.get(i));
            if ("1".equals(pressStatus)||"2".equals(pressStatus)||"3".equals(pressStatus)) { // || "2".equals(pressStatus)
                pressStatusAll = pressStatusAll + (i + 1) + ",";
                super.pressUseMap.put(i + 1, true);
            }
        }
        if (pressStatusAll.length() > 0) {
            pressStatusAll = pressStatusAll.substring(0, pressStatusAll.length() - 1);
        }
        UiLogUtil.appendLog2EventTab(deviceCode, "设备正在使用的Press为[" + pressStatusAll + "]");
    }

    public void sendUphData2Server() {
        String output = this.getOutputData() == null ? "" : this.getOutputData();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<String> svidlist = recipeService.searchShotSVByDeviceType(deviceType);
        sqlSession.close();
        //获取前一状态与当前状态
        sendS1F3RcpAndStateCheck();
        Map shotCountMap = sendS1F3SVShotCountCheckout(svidlist);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "UphDataTransfer");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("equipStatus", equipStatus);
        mqMap.put("preEquipStatus", preEquipStatus);
        mqMap.put("currentRecipe", ppExecName);
        mqMap.put("lotId", lotId);
        mqMap.put("shotCount", JsonMapper.toJsonString(shotCountMap));
        mqMap.put("output", output);
        mqMap.put("unit", "");
        mqMap.put("currentTime", GlobalConstants.dateFormat.format(new Date()));
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        UiLogUtil.appendLog2SeverTab(deviceCode, "发送设备UPH参数至服务端");
        logger.info("设备 " + deviceCode + " UPH参数为:" + mqMap);
//        UiLogUtil.appendLog2SeverTab(deviceCode, "UPH参数为:" + mqMap);
    }

    public Object clone() {
        ASMIdeal3GHost newEquip = new ASMIdeal3GHost(deviceId, this.deviceCode,
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

    private void initRptPara() {
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}