package cn.tzauto.octopus.secsLayer.equipImpl.icos.tr;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

@SuppressWarnings("serial")
public class T830Host extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(T830Host.class.getName());

    public T830Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
    }

    public T830Host(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
            int localTcpPort, String remoteIpAddress, int remoteTcpPort,
            String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
    }

    @Override
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
                if (rptDefineNum < 1) {
                    sleep(300);
                    super.findDeviceRecipe();
                    rptDefineNum++;
                }
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
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
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstatuschange")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.toLowerCase().equalsIgnoreCase("s6f11incommon")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f11equipstate")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f11ppselectfinish")) {
                byte[] ack = new byte[1];
                ack[0] = 0;
                replyS6F12WithACK(data, ack);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS14F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + "which I don't want to process!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code"> 
    public Map setEqptOnline() {
        return this.sendS1F17outByT830();
    }

    public Map setEqptOffline() {
        return this.sendS1F15outByT830();
    }

    public Map sendS1F15outByT830() {
        DataMsgMap s1f15out = new DataMsgMap("s1f15out", activeWrapper.getDeviceId());
        s1f15out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s1f15out);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        byte[] ack = (byte[]) ((SecsItem) msgdata.get("OFLACK")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f16");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack[0]);
        return resultMap;
    }

    public Map sendS1F17outByT830() {
        DataMsgMap s1f17out = new DataMsgMap("s1f17out", activeWrapper.getDeviceId());
        s1f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s1f17out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] ack = (byte[]) ((SecsItem) msgdata.get("AckCode")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack[0]);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS1F3Check() {
        DataMsgMap s1f3out = new DataMsgMap("s1f3statecheck", activeWrapper.getDeviceId());
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s1f3out.setTransactionId(transactionId);
        long[] equipStatuss = new long[1];
        long[] pPExecNames = new long[1];
        long[] controlStates = new long[1];
        DataMsgMap data = null;
        try {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            equipStatuss[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId());
            pPExecNames[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId());
            controlStates[0] = Long.parseLong(recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId());
            sqlSession.close();
            s1f3out.put("EquipStatus", equipStatuss);
            s1f3out.put("PPExecName", pPExecNames);
            s1f3out.put("ControlState", controlStates);
            data = activeWrapper.sendAwaitMessage(s1f3out);

        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        if (data == null || data.get("RESULT") == null) {
            data = getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("RESULT") == null) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("RESULT")).getData();
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0).toString()), deviceType);
        ppExecName = (String) listtmp.get(1);
        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">
    public Map sendS2F41outStart(String batchName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outstart", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s2f41out.put("BatchName", batchName);
        byte[] hcack = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.debug("Recieve s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK = " + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6F11 Code">

    @Override
    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 01;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 2) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 3) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateCommStateInExt();
        showCollectionsEventInfo(ceid);
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long preStatus = 01;
        long nowStatus = 0;
        long ceid = 0L;
        try {
            preStatus = data.getSingleNumber("PreStatus");
            nowStatus = data.getSingleNumber("EquipStatus");
            ceid = data.getSingleNumber("CollEventID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(nowStatus), deviceType);
        if (equipStatus.equalsIgnoreCase("Run")) {
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "检测到设备设置为锁机，设备将被锁！");
                pauseDevice();
            }
        }
        if (equipStatus.equalsIgnoreCase("READY")) {
            findDeviceRecipe();
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        super.changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfoExt deviceInfoExt = new DeviceInfoExt();
        deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (equipStatus.equalsIgnoreCase("READY")) {
            if ("Engineer".equals(deviceInfoExt.getBusinessMod())) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机卡控！");
            } else {
                //检查领料程序与设备在用程序是否一致
                boolean recipeNameOk = checkRecipeName(deviceInfoExt.getRecipeName());
                //检查程序版本
                Recipe goldRecipe = checkRecipeHasGoldFlag(deviceInfoExt.getRecipeName(), String.valueOf(deviceInfoExt.getVerNo()));
                if (recipeNameOk && goldRecipe != null) {
                    Recipe downloadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = downloadRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                    }
                }
            }
        }
        try {
            //更新模型表设备状态
            deviceInfoExt.setDeviceStatus(equipStatus);
            deviceInfoExt.setLockFlag(null);
            deviceService.modifyDeviceInfoExt(deviceInfoExt);
            sqlSession.commit();
            //保存设备操作记录到数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
        } catch (Exception e) {
        } finally {
            sqlSession.close();
        }
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param localFilePath
     * @return
     */
    @Override
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map hanAndCompMap = getRelativeFileInfo(localFilePath, targetRecipeName);
        if (hanAndCompMap == null) {
            return null;
        }
        long[] length0 = new long[1];
        length0[0] = TransferUtil.getPPLength(localFilePath);
        long[] length1 = new long[1];
        length1[0] = TransferUtil.getPPLength(String.valueOf(hanAndCompMap.get("hanRcpPath")));
        long[] length2 = new long[1];
        length2[0] = TransferUtil.getPPLength(String.valueOf(hanAndCompMap.get("compRcpPath")));
        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", targetRecipeName);
        s7f1out.put("Length", length0);
        DataMsgMap data = null;
        byte[] ppgnt = new byte[1];
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("hanRcpName")));
        s7f1out.put("Length", length1);
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("compRcpName")));
        s7f1out.put("Length", length2);
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt[0]);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    /**
     * T830每次下载要下载3个recipe，这里每一个都成功，recipe才能使用
     *
     * @param localRecipeFilePath
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map hanAndCompMap = getRelativeFileInfo(localRecipeFilePath, targetRecipeName);
        if (hanAndCompMap == null) {
            return null;
        }
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody0 = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        byte[] ppbody1 = (byte[]) TransferUtil.getPPBody(recipeType, String.valueOf(hanAndCompMap.get("hanRcpPath"))).get(0);
        byte[] ppbody2 = (byte[]) TransferUtil.getPPBody(recipeType, String.valueOf(hanAndCompMap.get("compRcpPath"))).get(0);
        SecsItem secsItem0 = new SecsItem(ppbody0, FormatCode.SECS_BINARY);
        SecsItem secsItem1 = new SecsItem(ppbody1, FormatCode.SECS_BINARY);
        SecsItem secsItem2 = new SecsItem(ppbody2, FormatCode.SECS_BINARY);
        //下载han文件
        s7f3out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("hanRcpName")));
        s7f3out.put("Processprogram", secsItem1);
        try {
            sleep(1000);
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] ackc7han = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        if (ackc7han[0] == 0) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载成功.");
            logger.debug("Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载成功.");
        } else {
            UiLogUtil.appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载失败.");
            logger.error("Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载失败.");
        }
        //下载comp文件
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f3out.put("ProcessprogramID", String.valueOf(hanAndCompMap.get("compRcpName")));
        s7f3out.put("Processprogram", secsItem2);
        try {
            sleep(1000);
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] ackc7comp = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        if (ackc7comp[0] == 0) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载成功.");
            logger.debug("Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载成功.");
        } else {
            UiLogUtil.appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载失败.");
            logger.error("Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载失败.");
        }
        //下载recipe文件
        s7f3out.put("ProcessprogramID", targetRecipeName);
        s7f3out.put("Processprogram", secsItem0);
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            sleep(1000);
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        if (ackc7[0] == 0) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "Recipe:" + targetRecipeName + "下载成功.");
            logger.debug("Recipe:" + targetRecipeName + "下载成功.");
        } else {
            UiLogUtil.appendLog2SecsTab(deviceCode, "Recipe:" + targetRecipeName + "下载失败.");
            logger.error("Recipe:" + targetRecipeName + "下载失败.");
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        if (ackc7han[0] == 0 && ackc7comp[0] == 0 && ackc7[0] == 0) {
            ackc7[0] = 0;
        } else {
            ackc7[0] = 1;
        }
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        if ("Run".equalsIgnoreCase(equipStatus)) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "设备正在运行，无法上传Recipe.");
            return null;
        }
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        long transActionId = activeWrapper.getNextAvailableTransactionId();
        s7f5out.setTransactionId(transActionId);
        s7f5out.put("ProcessprogramID", recipeName);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (data == null || data.isEmpty()) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "上传请求被设备拒绝，请查看设备状态。");
            return null;
        }

        byte[] ppbody = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        List<String> list = TrRecipeUtil.readRCP(recipePath);
        String rcpContent = "";
        for (String str : list) {
            if (str.contains("handler") || str.contains("component")) {
                String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
                String ppidTem = str.replace("@", "/");
                DataMsgMap s7f5outTem = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
                s7f5outTem.setTransactionId(activeWrapper.getNextAvailableTransactionId());
                s7f5outTem.put("ProcessprogramID", ppidTem);
                try {
                    data = activeWrapper.sendAwaitMessage(s7f5outTem);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (data.get("ProcessprogramID") != null) {
                    ppidTem = (String) ((SecsItem) data.get("ProcessprogramID")).getData();
                    byte[] ppbodyTem = (byte[]) ((SecsItem) data.get("Processprogram")).getData();
                    TransferUtil.setPPBody(ppbodyTem, recipeType, recipePathTem);
                    rcpContent = rcpContent + str;
                }
            }
        }
        String rcpAnalyseSucceed = "Y";
        if (!rcpContent.contains("handler")) {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]没有找到关联的handler文件，请检测文件是否存在或文件名是否正确");
            rcpAnalyseSucceed = "N";
        }
        if (!rcpContent.contains("component")) {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]没有找到关联的component文件，请检测文件是否存在或文件名是否正确");
            rcpAnalyseSucceed = "N";
        }
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = TrT830RecipeUtil.transferParaFromDB(deviceType, recipePath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("rcpAnalyseSucceed", rcpAnalyseSucceed);
        resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
        resultMap.put("Description", " Receive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        DataMsgMap s7f17out = new DataMsgMap("s7f17out", activeWrapper.getDeviceId());
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName);
        byte[] ackc7 = new byte[1];
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
            sleep(1000);
            if (ackc7[0] == 0 || ackc7[0] == 6) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName.replace("recipe", "component"));
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        s7f17out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f17out.put("ProcessprogramID", recipeName.replace("recipe", "handler"));
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s7f17out);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        if ("Run".equalsIgnoreCase(equipStatus) || "Pause".equalsIgnoreCase(equipStatus)) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "设备正在运行，无法上传Recipe.");
            return null;
        }
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
            e.printStackTrace();
        }
        if (data == null || data.get("EPPD") == null) {
            data = this.getMsgDataFromWaitMsgValueMapByTransactionId(transactionId);
        }
        if (data == null || data.get("EPPD") == null) {
            logger.error("获取设备[" + deviceCode + "]的recipe列表信息失败！");
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("EPPD")).getData();
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ArrayList t830RecipeList = new ArrayList();
            for (Object recipeName : listtmp) {
                if (recipeName.toString().contains("recipe")) {
                    t830RecipeList.add(recipeName);
                }
            }
            resultMap.put("eppd", t830RecipeList);
        }
        return resultMap;
    }

    private DataMsgMap handleOverTime(DataMsgMap s7f19out) {
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

        try {
            Future<DataMsgMap> future = exec.submit(call);
            result = future.get(1000, TimeUnit.MILLISECONDS); //任务处理超时时间设为 1 秒  
            logger.info("任务执行成功");
        } catch (TimeoutException e) {
            logger.error("处理超时....", e);
        } catch (Exception e) {
            logger.error("处理失败....", e);
        } finally {
            // 关闭线程池  
            exec.shutdown();
            return result;
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand Code">
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            String holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = true;
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    public Map pauseDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("Pause");
            String holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = true;
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中为开启锁机功能！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        return null;
    }

    // </editor-fold> 
    @Override
    public Map getRelativeFileInfo(String localFilePath, String targetRecipeName) {
        Map map = new HashMap();
        List<String> hanAndComp = TrRecipeUtil.readRCP(localFilePath);
        String hanRcpName = "";
        String hanRcpPath = "";
        String compRcpName = "";
        String compRcpPath = "";
        for (String str : hanAndComp) {
            if (str.contains("@handler@")) {
                hanRcpName = str.replace("@", "/");
                if (localFilePath.contains("_V")) {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("hanRcpName", hanRcpName);
                map.put("hanRcpPath", hanRcpPath);
            } else if (str.contains("@component@")) {
                compRcpName = str.replace("@", "/");
//                compRcpName = str + targetRecipeName.substring(targetRecipeName.lastIndexOf("_")).replace("@", "/");
                if (localFilePath.contains("_V")) {
                    compRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    compRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("compRcpName", compRcpName);
                map.put("compRcpPath", compRcpPath);
            } else {
                return null;
            }
        }
        return map;
    }

    private Recipe checkRecipeHasGoldFlag(String recipeName, String verNo) {
        Recipe checkResult = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<Recipe> downloadGoldRecipe = recipeService.searchRecipeGoldByPara(recipeName, deviceType, "GOLD", verNo);
        if (downloadGoldRecipe == null || downloadGoldRecipe.isEmpty()) {
            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在[" + ppExecName + "]的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
            //不允许开机
            this.holdDeviceAndShowDetailInfo();
        } else {
            checkResult = downloadGoldRecipe.get(0);
        }
        sqlSession.close();
        return checkResult;
    }

    @Override
    protected boolean checkRecipeName(String recipeName) {
        boolean checkResult = false;
        if (recipeName.equals(ppExecName)) {
            checkResult = true;
        }
        if (!checkResult) {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
            holdDeviceAndShowDetailInfo();
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为[" + ppExecName + "]与改机后程序一致，核对通过！");
        }
        return checkResult;
    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        List<Attach> attachs = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        String attachPath = recipeService.organizeUploadRecipePath(recipe);
        String recipePath = GlobalConstants.localRecipePath + attachPath + recipe.getRecipeName().replace("/", "@") + "_V" + recipe.getVersionNo() + ".txt";
        Map attachPathMap = getRelativeFileInfo(recipePath, "");
        String attachName = recipe.getRecipeName().replaceAll("/", "@") + "_V" + recipe.getVersionNo();
        for (int i = 0; i < 3; i++) {
            if (i == 1) {
                attachName = String.valueOf(attachPathMap.get("compRcpName")).replaceAll("/", "@") + "_V" + recipe.getVersionNo();
            }
            if (i == 2) {
                attachName = String.valueOf(attachPathMap.get("hanRcpName")).replaceAll("/", "@") + "_V" + recipe.getVersionNo();
            }
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachName(attachName);
            attach.setAttachPath(attachPath);
            sqlSession.close();
            attach.setAttachType("txt");
            attach.setSortNo(0);
            if (GlobalConstants.sysUser != null) {
                attach.setCreateBy(GlobalConstants.sysUser.getId().toString());
                attach.setUpdateBy(GlobalConstants.sysUser.getId().toString());
            } else {
                attach.setCreateBy("System");
                attach.setUpdateBy("System");
            }
            attachs.add(attach);
        }
        return attachs;

    }

    @Override
    public Object clone() {
        T830Host newEquip = new T830Host(deviceId, this.deviceCode,
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

    @Override
    public String checkEquipStatus() {
        if (FengCeConstant.STATUS_RUN.equals(equipStatus)) {
            return "设备正在运行，不可调整Recipe！";
        }
        if (!FengCeConstant.STATUS_IDLE.equalsIgnoreCase(equipStatus)) {
            return "设备未处于" + FengCeConstant.STATUS_IDLE + "状态，不可调整Recipe！";
        }
        return "0";
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // 上传ftp
        FtpUtil.uploadFile(localRcpPath, remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
        List<String> rcpContent = TrRecipeUtil.readRCP(localRcpPath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        for (String item : rcpContent) {
            if (item.contains("@component@") || item.contains("@handler@")) {
                String relLocalPath = GlobalConstants.localRecipePath + new RecipeService(sqlSession).organizeUploadRecipePath(recipe) + item + "_V" + recipe.getVersionNo() + ".txt";
                String relRemotePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                FtpUtil.uploadFile(relLocalPath, relRemotePath, item + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                UiLogUtil.appendLog2EventTab(deviceCode, "关联文件存储位置：" + relLocalPath);
            }
        }
        sqlSession.close();
        return true;
    }
}