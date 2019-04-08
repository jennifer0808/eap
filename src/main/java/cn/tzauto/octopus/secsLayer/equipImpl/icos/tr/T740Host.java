package cn.tzauto.octopus.secsLayer.equipImpl.icos.tr;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.icos.TrRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

@SuppressWarnings("serial")
public class T740Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(T740Host.class.getName());
    private boolean needCheck = false;

    public T740Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
    }


    @Override
    public Object clone() {
        T740Host newEquip = new T740Host(deviceId,
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
                    sleep(1500);
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();

                    sendS2F37outClose(14010L);
                    sendS2F35outDelete(14010L, 14010L);
                    sendS2f33outDelete(80006L);

                    sendS2f33outTape();
                    sendS2F35out(14010L, 14010L, 80006L);
                    sendS2F37out(14010L);
                    rptDefineNum++;
                    updateLotId();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                    processS6F11EquipStatus(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11tape")) {
//                    sendS2f41Approve();
                    sendS2f41NotApprove();
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
            LastComDate = System.currentTimeMillis();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.toLowerCase().contains("s6f11in")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("F0") || tagName.contains("f0")) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // <editor-fold defaultstate="collapsed" desc="S1FX Code"> 


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
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        } catch (Exception e) {
            logger.error("Exception occur,Exception info:" + e.getMessage());
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack[0]);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack[0], "HCACK"));
        return resultMap;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6F11 Code">

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = data.getSingleNumber("CollEventID");
            if (ceid == 2) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 3) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 1) {
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            }
            if (ceid == 14032) {
                needCheck = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        findDeviceRecipe();
        updateCommStateInExt();
//        showCollectionsEventInfo(ceid);
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long preStatus = 0l;
        long nowStatus = 0;
        long ceid = 0L;
        try {
            findDeviceRecipe();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        if (AxisUtility.isEngineerMode(deviceCode)) {
            UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            sqlSession.close();
            needCheck = false;
            return;
        }
        if (equipStatus.equalsIgnoreCase("run") && needCheck) {
            //首先从服务端获取机台是否处于锁机状态
            //如果设备应该是锁机，那么首先发送锁机命令给机台
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                pauseDevice();
                needCheck = false;
            }
        }
//        String preStatusStr = ACKDescription.descriptionStatus(preStatus, deviceType);

        if (equipStatus.equalsIgnoreCase("READY") && needCheck) {
            findDeviceRecipe();
        }
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfoExt deviceInfoExt = new DeviceInfoExt();
        deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (equipStatus.equalsIgnoreCase("READY") && needCheck) {
            if (AxisUtility.isEngineerMode(deviceCode)) {

                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                sqlSession.close();
                needCheck = false;
                return;
            } else {
                //检查领料程序与设备在用程序是否一致
                boolean recipeNameOk = checkRecipeName(deviceInfoExt.getRecipeName());
                //检查程序版本
                Recipe goldRecipe = checkRecipeHasGoldFlag(deviceInfoExt.getRecipeName());
                if (recipeNameOk && goldRecipe != null) {
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = downLoadRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        startCheckRecipePara(downLoadRecipe, "abs");
                    } else {
                        //2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        startCheckRecipePara(goldRecipe);
                    }
                } else {
                    holdDevice1();
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
            needCheck = false;
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
        resultMap.put("Description", ACKDescription.description(ppgnt[0], "PPGNT"));
        return resultMap;
    }

    /**
     * T740每次下载要下载3个recipe，这里每一个都成功，recipe才能使用
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
        resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        if ("Idle".equalsIgnoreCase(equipStatus) || "UNKNOWN".equalsIgnoreCase(equipStatus) || "ready".equalsIgnoreCase(equipStatus)) {
            Recipe recipe = setRecipe(recipeName);
            recipePath = super.getRecipePathByConfig(recipe);
            byte[] ppbody = (byte[]) getPPBODY(recipeName);
            TransferUtil.setPPBody(ppbody, recipeType, recipePath);
            List<String> list = TrRecipeUtil.readRCP(recipePath);
            String rcpContent = "";
            for (String str : list) {
                if (str.contains("handler") || str.contains("component")) {
                    String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
                    String ppidTem = str.replace("@", "/");
                    byte[] ppbodyTem = (byte[]) getPPBODY(ppidTem);
                    TransferUtil.setPPBody(ppbodyTem, recipeType, recipePathTem);
                    rcpContent = rcpContent + str;

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
                recipeParaList = TrRecipeUtil.transferParaFromDB(deviceType, recipePath);
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
        } else {
            UiLogUtil.appendLog2SecsTab(deviceCode, "请在设备IDLE时上传Recipe.");
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7[0] + " means " + ACKDescription.description(ackc7[0], "ACKC7"));
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
        resultMap.put("Description", ACKDescription.description(ackc7[0], "ACKC7"));
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f20");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("Description", "Get eppd from equip " + deviceCode);
        DataMsgMap s7f19out = new DataMsgMap("s7f19out", activeWrapper.getDeviceId());
        s7f19out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        DataMsgMap data = null;
        try {
//            data = activeWrapper.sendAwaitMessage(s7f19out);
            data = activeWrapper.sendS7F19out();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) ((SecsItem) data.get("EPPD")).getData();
        if (list == null || list.isEmpty()) {
            resultMap.put("eppd", new ArrayList<>());
        } else {
            ArrayList listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            ArrayList t640RecipeList = new ArrayList();
            for (Object recipeName : listtmp) {
                if (recipeName.toString().contains("recipe")) {
                    t640RecipeList.add(recipeName);
                }
            }
            resultMap.put("eppd", t640RecipeList);
        }
        return resultMap;
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
            Map cmdMap = this.sendS2f41Cmd("PAUSE");
            String holdResult = cmdMap.get("HCACK").toString();
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    public Map holdDevice1() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            String holdResult = cmdMap.get("HCACK").toString();
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
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
            Map cmdMap = this.sendS2f41Cmd("PAUSE");
            String holdResult = cmdMap.get("HCACK").toString();
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            }
            return cmdMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        return null;//this.sendS2f41Cmd("RESUME");
//        return this.setEqptOffline();
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
                if (localFilePath.contains("_")) {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("hanRcpName", hanRcpName);
                map.put("hanRcpPath", hanRcpPath);
            } else if (str.contains("@component@")) {
                compRcpName = str.replace("@", "/");
//                compRcpName = str + targetRecipeName.substring(targetRecipeName.lastIndexOf("_")).replace("@", "/");
                if (localFilePath.contains("_")) {
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

    private Recipe checkRecipeHasGoldFlag(String recipeName) {
        Recipe checkResult = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(recipeName, deviceType, "GOLD", null);
        if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
            //不允许开机
            this.holdDeviceAndShowDetailInfo();
        } else {
            checkResult = downLoadGoldRecipe.get(0);
        }
        return checkResult;
    }

    @Override
    protected boolean checkRecipeName(String recipeName) {
        boolean checkResult = false;
        if (recipeName.equals(ppExecName)) {
            checkResult = true;
        }
        if (!checkResult) {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
            holdDeviceAndShowDetailInfo();
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
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
                attach.setCreateBy(GlobalConstants.sysUser.getId());
                attach.setUpdateBy(GlobalConstants.sysUser.getId());
            } else {
                attach.setCreateBy("System");
                attach.setUpdateBy("System");
            }
            attachs.add(attach);
        }
        return attachs;

    }


    @Override
    public String checkEquipStatus() {
        findDeviceRecipe();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！";
        }
        if (!FengCeConstant.STATUS_IDLE.equalsIgnoreCase(equipStatus)) {
            return "设备未处于" + FengCeConstant.STATUS_IDLE + "状态，不可调整Recipe！";
        }
        return "0";
    }

    @SuppressWarnings("unchecked")
    public void sendS2f33outTape() {
        DataMsgMap s2f33out = new DataMsgMap("s2f33outTape", activeWrapper.getDeviceId());
        s2f33out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = 14010L;
        long[] reportid = new long[1];
        reportid[0] = 80006L;
        long[] CarrierName = new long[1];
        CarrierName[0] = 72008L;
        long[] CarrierLot = new long[1];
        CarrierLot[0] = 72011L;
        long[] CarrierPart = new long[1];
        CarrierPart[0] = 72021L;
        long[] CoverLot = new long[1];
        CoverLot[0] = 72012L;
        long[] CoverPart = new long[1];
        CoverPart[0] = 72022L;

        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("CarrierName", CarrierName);
        s2f33out.put("CarrierLot", CarrierLot);
        s2f33out.put("CarrierPart", CarrierPart);
        s2f33out.put("CoverLot", CoverLot);
        s2f33out.put("CoverPart", CoverPart);

        try {
            activeWrapper.sendAwaitMessage(s2f33out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void sendS2f41Approve() {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outApprove", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            activeWrapper.sendAwaitMessage(s2f41out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    @SuppressWarnings("unchecked")
    public void sendS2f41NotApprove() {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outNotApprove", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            activeWrapper.sendAwaitMessage(s2f41out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

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
