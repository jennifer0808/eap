package cn.tzauto.octopus.secsLayer.equipImpl.disco.ls;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;
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


public class Disco7160Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(Disco7160Host.class);
    private boolean isFullAutoState1st = false;


    public Disco7160Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        RCMD_PPSELECT = "PP_SELECT_S";
        CPN_PPID = "DEV_NO";
    }

    public Object clone() {
        Disco7160Host newEquip = new Disco7160Host(deviceId,
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
                    this.sendS1F13out();
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    updateLotId();
//                    upLoadAllRcp();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().contains("s6f11equipstatuschange")) {
                    try {
                        processS6F11EquipStatusChange(msg);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11IN")) {
                    Long ceid = 0L;
                    try {
                        ceid = (Long) msg.get("CEID");
                        Map panelMap = new HashMap();
                        if (ceid == 75 || ceid == 76) {
                            if (ceid == 75) {
                                panelMap.put("ControlState", GlobalConstant.CONTROL_LOCAL_ONLINE);       //Online_Local
                            } else {
                                panelMap.put("ControlState", GlobalConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
                            }
                            changeEquipPanel(panelMap);
                        }
                        if (ceid == 191) {
                            logger.info("接受到CoatingEnd事件，发送锁机指令");
                            processCoatingEndEvent(msg);
                        }
                        if (ceid == 150) {
                            processS6F11EquipStatusChange(msg);
                        }
                        if(ceid ==73 ){
                            findDeviceRecipe();
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                }
            } catch (InterruptedException e) {
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
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data,(byte)0);
                long ceid = 0l;
                try {
                    ceid = (long) data.get("CEID");
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
                if (ceid == 43) {
                    isFullAutoState1st = true;
                    logger.info(deviceCode + ":设备已进入全自动模式！");
                }
                this.inputMsgQueue.put(data);
            }  else if (tagName.equalsIgnoreCase("s1f2in")) {
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

    private String getDevIdFromEqp(long DEVID) {
        String devid = "--";
        DataMsgMap s2f13out = new DataMsgMap("s2f13DEVIDout", activeWrapper.getDeviceId());
        long[] devids = new long[1];
        devids[0] = DEVID;
        s2f13out.put("DevID", devids);
        s2f13out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        List<Long> list = new ArrayList();
        list.add(DEVID);
        try {
            data = activeWrapper.sendS2F13out(list, ecFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data != null && data.get("RESULT") != null) {
            ArrayList listtmp = (ArrayList) (data.get("EC"));
            devid = String.valueOf(listtmp.get(0));
        }
        return devid;
    }

    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 
    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            super.findDeviceRecipe();
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
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            } else //开机check
                if ("run".equalsIgnoreCase(equipStatus) && ceid == 150L) {
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
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }


    public void processCoatingEndEvent(DataMsgMap data) {
        if (isFullAutoState1st) {
            logger.info(deviceCode + ":进入全自动模式，做完第一片，启动停止！发送锁机指令");
            holdDevice2();
            isFullAutoState1st = false;
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long length = TransferUtil.getPPLength(localFilePath);
        //从ppbody中找到长号（devid）
        String recipeName = getDevIdFromPPBody(localFilePath);
        if (!"".equals(recipeName)) {
            targetRecipeName = recipeName;
        }
        DataMsgMap data = null;
        byte ppgnt = 0;
        try {
            data = activeWrapper.sendS7F1out(targetRecipeName, length, lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.debug("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        String recipeName = getDevIdFromPPBody(localRecipeFilePath);
        if (!"".equals(recipeName)) {
            targetRecipeName = recipeName;
        }
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, SecsFormatValue.SECS_BINARY);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte ackc7 = (byte) data.get("ACKC7");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String eqpRecipeName) {
        String recipeName = "";
        if (eqpRecipeName.contains("\\")) {
            String[] recipeNames = eqpRecipeName.split("\\\\");
            if (recipeNames.length < 2) {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "无法获取准确程序名,请检查程序名！");
                return null;
            }
            recipeName = recipeNames[1];
        } else {
            recipeName = eqpRecipeName;
        }
        List<RecipePara> recipeParaList = null;
        //这里先将recipe按照短号存储，读取ppbody后再按长号存储
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        byte[] ppbody = new byte[0];
        try {
            ppbody = (byte[]) getPPBODY(recipeName);
        } catch (UploadRecipeErrorException e) {
            e.printStackTrace();
        }
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipe.getRecipeName() + " has been saved at " + recipePath);
        //Recipe解析     
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
        resultMap.put("Descrption", " Recive the recipe " + eqpRecipeName + " from equip " + deviceCode + " and save as " + recipe.getRecipeName());
        return resultMap;
    }

    /*    
    从ppbody中找到长号（devid）
     */
    private String getDevIdFromPPBody(String recipePath) {
        String devId = "";
        Map paraMap = DiscoRecipeUtil.transferFromFile(recipePath);
        List<RecipePara> recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, deviceType);
        for (RecipePara recipePara : recipeParaList) {
            if (recipePara.getParaName().equals("DEV_ID")) {
                if (!"".equals(recipePara.getSetValue())) {
                    devId = recipePara.getSetValue();
                }
            }
        }
        return devId;
    }

    private boolean rcpInEqp(String recipeName) {
        ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
        if (eppd != null && eppd.size() > 0) {
            for (int i = 0; i < eppd.size(); i++) {
                String rcpNameString = eppd.get(i).toString();
                if (recipeName.equals(rcpNameString)) {
                    return true;
                }
            }
        }
        return false;
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
//            Map cmdMap = this.sendS2f41Cmd("PAUSE_H");
            Map cmdMap = this.sendS2f41Cmd("STOP");
            if (0 == (byte)cmdMap.get("HCACK")) {
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 2);
                changeEquipPanel(panelMap);
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！\n");
            return null;
        }
    }


    public Map holdDevice2() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            Map cmdMap = this.sendS2f41Cmd("PAUSE_H");
            Map cmdMap = this.sendS2f41Cmd2("PAUSE_H");
            if (0 == (byte)cmdMap.get("HCACK")) {
//                Map panelMap = new HashMap();
//                panelMap.put("AlarmState", 2);
//                changeEquipPanel(panelMap);
//                holdSuccessFlag = true;
            } else {
//                holdSuccessFlag = false;
            }
            Thread t1 = new Thread(new test(this));
            t1.run();
            logger.info("开始start线程！！！！！！！！！！！！！！！");
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！\n");
            return null;
        }
    }

    class test implements Runnable {
        public Disco7160Host equipHost;

        public test() {
        }

        public test(Disco7160Host equipHost) {
            this.equipHost = equipHost;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.equipHost.resumeDevice();
        }
    }

    public Map sendS2f41Cmd2(String Remotecommand) {
        return super.sendS2f41Cmd(Remotecommand);
    }


    public Map resumeDevice() {
        return super.sendS2f41Cmd("RESUME_H");
    }


    @Override
    public Map releaseDevice() {
        //这里这样写是因为DFD6361 的hold指令使用的是Stop 设备从ready到run过程时间较长，stop后直接结束全自动模式，不用发RESUME
        Map map = new HashMap();
        map.put("HCACK", 0);
        return map;
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

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put("Port", (byte) 1);
            cpmap.put(CPN_PPID, recipeName);
            Map cpNameMap = new HashMap();
            cpNameMap.put("Port", SecsFormatValue.SECS_ASCII);
            cpNameMap.put(CPN_PPID, SecsFormatValue.SECS_ASCII);
            Map cpValueMp = new HashMap();
            cpValueMp.put((byte) 1, SecsFormatValue.SECS_BINARY);
            cpValueMp.put(recipeName, SecsFormatValue.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add("Port");
            cplist.add(CPN_PPID);
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cpmap, cpNameMap, cpValueMp);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            byte hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 means " + e.getMessage());
        }
        return resultMap;
    }
}
