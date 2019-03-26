package cn.tzauto.octopus.secsLayer.equipImpl.accretech;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.accretech.PG3000RMXRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.*;
import com.alibaba.fastjson.JSONArray;
import java.util.*;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

public class PG3000RMXHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(PG3000RMXHost.class.getName());
    private String portId = "";
    private volatile boolean isInterrupted = false;

    public PG3000RMXHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public PG3000RMXHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
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
                if (rptDefineNum < 2) {
//                    sendS1F1out();
//                    //为了能调整为online 
                    sendS1F17out();
                    //获取设备开机状态
                    super.findDeviceRecipe();
                    //获取lot号/
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                MsgDataHashtable msg = null;
                msg = this.inputMsgQueue.take();//得到监听到的东西
                if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11equipstatuschange")) {

                    //sml中s6f11equipstatuschange data1，data2表示状态前后的两个值，需要测试得到
                    processS6F11EquipStatusChange(msg);

                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11equipstate")) {
                    try {
                        long ceid = msg.getSingleNumber("CollEventID");
                        processS6F11EquipStatus(msg);
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (msg.getMsgTagName() != null && msg.getMsgTagName().equalsIgnoreCase("s6f11ppselectfinish")) {
                    ppExecName = (String) ((SecsItem) msg.get("PPExecName")).getData();
                    Map map = new HashMap();
                    map.put("PPExecName", ppExecName);
                    changeEquipPanel(map);
                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgTagName()
                            + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.error("Exception:", e);
            }
        }
    }

    public void inputMessageArrived(MessageArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            secsMsgTimeoutTime = 0;
            LastComDate = new Date().getTime();
            MsgDataHashtable data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstatuschange")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);  //放到队列中
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("s6f11incomm")) {
                //回复s6f11消息
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
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
        MsgDataHashtable s1f3out = new MsgDataHashtable("s1f3portstatecheck", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        MsgDataHashtable data = null;
        try {
            data = mli.sendPrimaryWsetMessage(s1f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        String port1Status = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
        String port2Status = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(1)), deviceType);
        if ("Setup".equalsIgnoreCase(port1Status)) {
            this.portId = "1";
        } else if ("Setup".equalsIgnoreCase(port2Status)) {
            this.portId = "2";
        } else {
            this.portId = "";
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">
    @SuppressWarnings("unchecked")
    public Map sendS2F41outPPselect1(String recipeName) {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outPPSelect", mli.getDeviceId());
        long[] portIdL = new long[1];
        if ("1".equals(this.portId)) {
            portIdL[0] = 1;
        } else if ("2".equals(this.portId)) {
            portIdL[0] = 2;
        } else {
            return null;
        }
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        s2f41out.put("PPID", recipeName);
        s2f41out.put("PORTID", portIdL);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", " cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", " cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        sendS2F41Grant();
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41outPPSelect", mli.getDeviceId());
        long[] portIdL = new long[1];
        Map resultMap = new HashMap();
        byte[] hcack = new byte[1];
        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                portIdL[0] = 1;
            }
            if (i == 1) {
                portIdL[0] = 2;
            }
            s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
            s2f41out.put("PPID", recipeName);
            s2f41out.put("PORTID", portIdL);
            resultMap.put("msgType", "s2f42");
            resultMap.put("deviceCode", deviceCode);
            try {
                MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f41out);
                hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
                logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
                logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
                resultMap.put("HCACK", hcack[0]);
                resultMap.put("Description", " cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            } catch (Exception e) {
                logger.error("Exception:", e);
                resultMap.put("HCACK", 9);
                resultMap.put("Description", " cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
            }
        }
        return resultMap;
    }

    private Map sendS2F41Grant() {
        MsgDataHashtable s2f41out = new MsgDataHashtable("s2f41grant", mli.getDeviceId());
        s2f41out.setTransactionId(mli.getNextAvailableTransactionId());
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        byte[] hcack = new byte[1];
        try {
            MsgDataHashtable data = mli.sendPrimaryWsetMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to Grand");
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", " cmd GRANT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", " cmd GRANT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
        return resultMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">
    @Override
    protected void processS6F11EquipStatus(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
//            UiLogUtil.appendLog2SecsTab(deviceCode, "收到CEID: " + ceid);
            if (ceid == 9) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
                super.sendS1F3Check();
            } else if (ceid == 10) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                super.sendS1F3Check();
            } else if (ceid == 8) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            } else if (ceid == 44) {
                this.processS6F11EquipStatusChange(data);
            } else if (ceid == 45 || ceid == 46) {
                checkPortStatusAndPPSelectToLocal();
            } else if (ceid == 57 || ceid == 59 || ceid == 56 || ceid == 58 || ceid == 300 || ceid == 301 || ceid == 302) {
                sendS2F41Grant();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(MsgDataHashtable data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //更新主界面
        sendS1F3Check();

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            boolean dataReady = false;
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
            if (dataReady && equipStatus.equalsIgnoreCase("run")) {
                //1、获取设备需要校验的信息类型,
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean hasGoldRecipe = true;
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，请改机!");
                    return;
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
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    holdDeviceAndShowDetailInfo();
                } else {
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
                            holdDeviceAndShowDetailInfo("The current recipe <" + ppExecName + "> in equipment is different from CIM system <" + deviceInfoExt.getRecipeName() + ">,equipment will be locked.");
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
                                this.holdDeviceAndShowDetailInfo("There's no GOLD or Unique version of current recipe <" + ppExecName + "> , equipment will be locked.");
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
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">  上传下载方法
    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param recipe
     * @param targetRecipeName
     * @return
     */
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        try {
            if (targetRecipeName.equals(ppExecName)) {
                sendS7F1out(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + "ACCT_POL-TEST", "ACCT_POL-TEST");
                MsgDataHashtable s7f3out = new MsgDataHashtable("s7f3out", mli.getDeviceId());
                s7f3out.setTransactionId(mli.getNextAvailableTransactionId());
                List bodylist = TransferUtil.getPPBody(recipeType, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + "ACCT_POL-TEST");
                if (bodylist == null || bodylist.isEmpty()) {
                    logger.error("未找到ACCT_POL-TEST文件，请确认是否存在");
                    resultMap.put("ppgnt", 9);
                    resultMap.put("Description", "未找到ACCT_POL-TEST文件，请确认是否存在");
                    return resultMap;
                }
                byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + "ACCT_POL-TEST").get(0);
                SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
                s7f3out.put("ProcessprogramID", "ACCT_POL-TEST");
                s7f3out.put("Processprogram", secsItem);
                mli.sendPrimaryWsetMessage(s7f3out);
                sendS2F41outPPselect("ACCT_POL-TEST");
                sendS7F17out(targetRecipeName);
            }
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

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        try {

            MsgDataHashtable data = null;
            MsgDataHashtable s7f3out = new MsgDataHashtable("s7f3out", mli.getDeviceId());
            s7f3out.setTransactionId(mli.getNextAvailableTransactionId());
            byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
            SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
            s7f3out.put("ProcessprogramID", targetRecipeName.replace("@", "/"));
            s7f3out.put("Processprogram", secsItem);

            resultMap.put("msgType", "s7f4");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("ppid", targetRecipeName);
            byte[] ackc7 = new byte[1];
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

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        String ppid = recipe.getRecipeName();
        recipePath = super.getRecipePathByConfig(recipe);
        MsgDataHashtable s7f5out = new MsgDataHashtable("s7f5out", mli.getDeviceId());
        MsgDataHashtable dataHashtable = null;
        s7f5out.setTransactionId(mli.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", ppid);
        try {
            dataHashtable = mli.sendPrimaryWsetMessage(s7f5out);  //得到S7f5发出去回复的东西
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        ppid = (String) ((SecsItem) dataHashtable.get("ProcessprogramID")).getData();
        byte[] ppbody = (byte[]) ((SecsItem) dataHashtable.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = PG3000RMXRecipeUtil.transferPG3000RCP(recipePath);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + ppid + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

    private void checkPortStatusAndPPSelectToLocal() {
        remoteDevice();
        sendS1F3CheckPort();
        if ("".equals(this.portId)) {
            localDeviceAndShowDetailInfo();
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
                if (recipeNameList.size() > 1) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "设备上存在不止一条程序,请清理其他程序");
                    localDeviceAndShowDetailInfo();
                    return;
                }
                if (recipeNameList.contains(recipeName)) {
                    Map resultMap = sendS2F41outPPselect1(recipeName);
                    if (resultMap != null && !resultMap.isEmpty()) {
                        if ("0".equals(String.valueOf(resultMap.get("HCACK"))) || "4".equals(String.valueOf(resultMap.get("HCACK")))) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "PPSelect成功，PPID=" + recipeName);
                            //选完程序切local状态
                        } else {
                            Map eqptStateMap = super.findEqptStatus();//失败上报机台状态
                            UiLogUtil.appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + "；原因：" + String.valueOf(resultMap.get("Description")) + "，机台状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                        }
                    } else {
                        UiLogUtil.appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + ", 设备消息回复错误，请联系CIM人员处理");
                    }
                } else {
                    UiLogUtil.appendLog2EventTab(deviceCode, "选中Recipe失败，PPID=" + recipeName + ", 设备Recipe列表中没有该Recipe，请重新下载");
                }
            } else {
                UiLogUtil.appendLog2EventTab(deviceCode, "无法获取设备Recipe列表，请检查设备通信");
            }
        }
        localDeviceAndShowDetailInfo();
    }

    @Override
    public Object clone() {
        PG3000RMXHost newEquip = new PG3000RMXHost(deviceId, this.equipId,
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
//事件报告

    private void initRptPara() {
//sendS6F23clear();
        //重写事件 报告
//            sendS2f33out(50007l, 902l, 906l);
//            sendS2f35out(50007l, 50007l, 50007l);
//            sendS2F37out(50007l);
//        sendS2F37outCloseAll();
//        sendS2F33clear();
//        sendS2F35clear();
//        sendS2F33init();
//        sendS2F35init();
        sendS2F37outAll();   //开启所有事件报告
    }

    //hold机台，先停再锁
    @SuppressWarnings("unchecked")
    public void sendS2F33clear() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f33clear", mli.getDeviceId());
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35clear() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f35clear", Integer.valueOf(deviceId));
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS6F23clear() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s6f23out", Integer.valueOf(deviceId));
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F33init() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f33initout", Integer.valueOf(deviceId));
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);

        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2F35init() {
        MsgDataHashtable s2f37outAll = new MsgDataHashtable("s2f35initout", Integer.valueOf(deviceId));
        long transactionId = mli.getNextAvailableTransactionId();
        s2f37outAll.setTransactionId(transactionId);
        try {
            mli.sendPrimaryWsetMessage(s2f37outAll);
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

    public Map remoteDevice() {
        return this.sendS2f41Cmd("REMOTE");
    }

    @Override
    public Map holdDevice() {
        remoteDevice();
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
            GlobalConstants.stage.getTX_EventLog().appendText("[" + GlobalConstants.dateFormat.format(new Date()) + "] 设备：" + deviceCode + " 未设置锁机！\n");
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
                UiLogUtil.appendLog2EventTab(deviceCode, "设备已经被切至Local");
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备已处于Local状态");
                return true;
            } else {
                UiLogUtil.appendLog2SecsTab(deviceCode, "HCACK:" + resultMap.get("HCACK") + " Description:" + String.valueOf(resultMap.get("Description")));
                Map eqptStateMap = this.findEqptStatus();
                UiLogUtil.appendLog2SecsTab(deviceCode, "设备切Local失败，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void initRemoteCommand() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
