package cn.tzauto.octopus.secsLayer.equipImpl.icos.tr;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
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
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.icos.TrRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

@SuppressWarnings("serial")
public class T640Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(T640Host.class.getName());
    private boolean startCheckOver = false;
    private boolean needCheck = false;

    public T640Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;

    }


    @Override
    public Object clone() {
        T640Host newEquip = new T640Host(deviceId,
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
    public void interrupt() {
        isInterrupted = true;
        super.interrupt();
    }

    @Override
    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!isInterrupted) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sleep(3000);
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                }
//                if (!holdSuccessFlag) {
//                    holdDevice();
//                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                }else if(msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")){
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstate")) {
                   // processS6F11EquipStatus(msg);
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
            LastComDate = System.currentTimeMillis();
            secsMsgTimeoutTime = 0;
            lastMsgTagName = tagName;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.toLowerCase().contains("s6f11in")) {
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
                equipStatus = "SECS-OFFLINE";
                Map panelMap = new HashMap();
                panelMap.put("EquipStatus", equipStatus);
                panelMap.put("PPExecName", ppExecName);
                panelMap.put("ControlState", controlState);
                changeEquipPanel(panelMap);
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

    public Map sendS1F15outByT640() {
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS1F15out();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        byte ack = (byte) msgdata.get("OFLACK");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f16");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack);
        return resultMap;
    }

    public Map sendS1F17outByT640() {
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS1F17out();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ack = (byte) msgdata.get("ONLACK");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s1f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ack", ack);
        return resultMap;
    }


    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S2FX Code"> 
    public Map sendS2F41outStart(String batchName) {
        String rcmd = "START" ;

        Map<String,String> cpMap = new HashMap();
        cpMap.put("BATCH-NAME",batchName);
        cpMap.put("ACTION","NEW");
        cpMap.put("BATCH-TO-PROCESS","");
        cpMap.put("CARRIER-COUNT","");
        cpMap.put("INPUT-TRAY-MAP","");
        cpMap.put("TRAY-REPORTING","NO");

        Map cpNameFormatMap = new HashMap();
        cpNameFormatMap.put("BATCH-NAME",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("ACTION",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("BATCH-TO-PROCESS",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("CARRIER-COUNT",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("INPUT-TRAY-MAP",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("TRAY-REPORTING",FormatCode.SECS_ASCII);


        Map cpValueFormatMap = new HashMap();
        cpNameFormatMap.put(batchName,FormatCode.SECS_ASCII);
        cpNameFormatMap.put("NEW",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("ACTION",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("",FormatCode.SECS_ASCII);
        cpNameFormatMap.put("NO",FormatCode.SECS_ASCII);


        byte hcack = -1;
        try {
            DataMsgMap data =  activeWrapper.sendS2F41out(rcmd,cpMap,cpNameFormatMap,cpValueFormatMap);
           // DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            hcack = (byte) data.get("HCACK");
            logger.debug("Recive s2f42in,the equip " + deviceCode + "'s requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception occur,Exception info:" + e.getMessage());
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack);
        resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        return resultMap;
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S6F11 Code">


    @Override
     public void processS6F11in(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 1 || ceid == 2 || ceid == 3) {
                if (ceid == 2) {
                    super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
                } else if (ceid == 3) {
                    super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                } else if (ceid == 1) {
                    super.setControlState(FengCeConstant.CONTROL_OFFLINE);
                }
                findDeviceRecipe();
            } else {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            }
            if (ceid == 14032) {
                needCheck = true;
            }

            if(ceid == 100 || ceid == 110 || ceid ==111 || ceid ==112 ){
                processS6F11EquipStatusChange(data);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        updateCommStateInExt();
        showCollectionsEventInfo(ceid);
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long preStatus = 0L;
        long nowStatus = 0;
        long ceid = 0L;

        try {

            preStatus = data.getSingleNumber("PreStatus");
            nowStatus = data.getSingleNumber("EquipStatus");
            ceid = (long) data.get("CEID");

        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(nowStatus), deviceType);
        if (AxisUtility.isEngineerMode(deviceCode)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
            sqlSession.close();
            return;
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        try {
            if (equipStatus.equalsIgnoreCase("run") && needCheck) {
//            if (!startCheckOver) {
//               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "设备开机检查未完成,请将设备状态调整至Ready完成检查！");
//                sendTerminalMsg2EqpSingle("The check is not complete, please adjust the equipment status to Ready to complete the check");
//                pauseDevice();
//            }
                //首先从服务端获取机台是否处于锁机状态
                //如果设备应该是锁机，那么首先发送锁机命令给机台

                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                   UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    pauseDevice();
                    needCheck = false;
                }
            }
//        String preStatusStr = ACKDescription.descriptionStatus(preStatus, deviceType);

            if (equipStatus.equalsIgnoreCase("READY") && needCheck) {
                startCheckOver = false;
                findDeviceRecipe();
            }
            super.changeEquipPanel(map);
            DeviceService deviceService = new DeviceService(sqlSession);
            RecipeService recipeService = new RecipeService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            if (equipStatus.equalsIgnoreCase("READY") && needCheck) {
                if (AxisUtility.isEngineerMode(deviceCode)) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                    sqlSession.close();
                    return;
                }
//else if (this.checkLockFlagFromServerByWS(deviceCode)) {
//                   UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    pauseDevice();
//                }

//        String preStatusStr = ACKDescription.descriptionStatus(preStatus, deviceType);
                if (equipStatus.equalsIgnoreCase("READY") && needCheck) {
                    startCheckOver = false;
                    findDeviceRecipe();
                }
                super.changeEquipPanel(map);
                if (equipStatus.equalsIgnoreCase("READY") && needCheck) {
                    if (AxisUtility.isEngineerMode(deviceCode)) {
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                        //sqlSession.close();
                        return;
                    } else {
//                        if (this.checkLockFlagFromServerByWS(deviceCode)) {
//                           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                            holdDevice();
//                        }
                        //检查领料程序与设备在用程序是否一致
                        boolean recipeNameOk = checkRecipeName(deviceInfoExt.getRecipeName());
                        //检查程序版本
                        Recipe goldRecipe = checkRecipeHasGoldFlag(deviceInfoExt.getRecipeName());
                        if (recipeNameOk && goldRecipe != null) {
                            Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                            //首先判断下载的Recipe类型
                            //1、如果下载的是Unique版本，那么执行完全比较
                            String downloadRcpVersionType = downLoadRecipe.getVersionType();
                            if (false) {
                               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
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

                //更新模型表设备状态
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceInfoExt.setLockFlag(null);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
                //保存设备操作记录到数据库
                saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
                needCheck = false;
            }
        } catch (Exception e) {
            logger.debug(e);
            needCheck = false;
        } finally {
            sqlSession.close();
//            needCheck = false;
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
        long length0  = TransferUtil.getPPLength(localFilePath);
        long length1  = TransferUtil.getPPLength(String.valueOf(hanAndCompMap.get("hanRcpPath")));
        long length2  = TransferUtil.getPPLength(String.valueOf(hanAndCompMap.get("compRcpPath")));
        DataMsgMap data = null;
        byte ppgnt = -1;

        try {
            data = activeWrapper.sendS7F1out(targetRecipeName,length0,lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid(recipe)= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            data = activeWrapper.sendS7F1out(String.valueOf(hanAndCompMap.get("hanRcpName")),length1,lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid(handler)= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            data = activeWrapper.sendS7F1out(String.valueOf(hanAndCompMap.get("compRcpName")),length2,lengthFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid(component)= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    /**
     * T640每次下载要下载3个recipe，这里每一个都成功，recipe才能使用
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
        //下载han文件
        try {
            sleep(1000);
            data = activeWrapper.sendS7F3out(String.valueOf(hanAndCompMap.get("hanRcpName")),ppbody1,FormatCode.SECS_BINARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ackc7han = (byte) data.get("ACKC7");

        if (ackc7han == 0) {
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载成功.");
            logger.debug("Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载成功.");
        } else {
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载失败.");
            logger.error("Recipe:" + String.valueOf(hanAndCompMap.get("hanRcpName")) + "下载失败.");
        }
        //下载comp文件
        try {
            sleep(1000);
            data = activeWrapper.sendS7F3out(String.valueOf(hanAndCompMap.get("compRcpName")),ppbody2,FormatCode.SECS_BINARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ackc7comp =   (byte) data.get("ACKC7");
        if (ackc7comp == 0) {
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载成功.");
            logger.debug("Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载成功.");
        } else {
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载失败.");
            logger.error("Recipe:" + String.valueOf(hanAndCompMap.get("compRcpName")) + "下载失败.");
        }
        //下载recipe文件
        try {
            sleep(1000);
            data = activeWrapper.sendS7F3out(targetRecipeName,ppbody0,FormatCode.SECS_BINARY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte ackc7 =   (byte) data.get("ACKC7");
        if (ackc7 == 0) {
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + targetRecipeName + "下载成功.");
            logger.debug("Recipe:" + targetRecipeName + "下载成功.");
        } else {
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "Recipe:" + targetRecipeName + "下载失败.");
            logger.error("Recipe:" + targetRecipeName + "下载失败.");
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        if (ackc7han == 0 && ackc7comp == 0 && ackc7 == 0) {
            ackc7 = 0;
        } else {
            ackc7 = 1;
        }
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        if ("Idle".equalsIgnoreCase(equipStatus) || "UNKNOWN".equalsIgnoreCase(equipStatus) || "ready".equalsIgnoreCase(equipStatus)) {
            Recipe recipe = setRecipe(recipeName);
            recipePath = super.getRecipePathByConfig(recipe);
            byte[] ppbody = (byte[]) getPPBODY(recipeName);
            TransferUtil.setPPBody(ppbody, 1, recipePath);
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
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]没有找到关联的handler文件，请检测文件是否存在或文件名是否正确");
                rcpAnalyseSucceed = "N";
            }
            if (!rcpContent.contains("component")) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe[" + recipeName + "]没有找到关联的component文件，请检测文件是否存在或文件名是否正确");
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
           UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "请在设备IDLE时上传Recipe.");
            return null;
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F17out(String recipeName) {
        List ProcessprogramIDList = new ArrayList();
        ProcessprogramIDList.add(recipeName);
        byte ackc7 = -1;
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(ProcessprogramIDList);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);

            ackc7 = (byte) data.get("ACKC7");
            sleep(1000);
            if (ackc7 == 0 || ackc7 == 6) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7 + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        List ProcessprogramIDList1 = new ArrayList();
        ProcessprogramIDList1.add(recipeName.replace("recipe", "component"));
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(ProcessprogramIDList1);

            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List ProcessprogramIDList2 = new ArrayList();
        ProcessprogramIDList2.add(recipeName.replace("recipe", "handler"));
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(ProcessprogramIDList2);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map sendS7F19out() {

        Map resultMap = super.sendS7F19out();
        ArrayList list = (ArrayList) resultMap.get("eppd");

            ArrayList t640RecipeList = new ArrayList();
            for (Object recipeName : list) {
                if (recipeName.toString().contains("recipe")) {
                    t640RecipeList.add(recipeName);
                }
            }
            resultMap.put("eppd", t640RecipeList);

        return resultMap;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand Code">
    /*
     * tr t640支持的命令有:
     * PP-SELECT
     * START
     * STOP
     * PAUSE
     * RESUME
     * ABORT
     */
    public Map setEqptOnline() {
        return this.sendS1F17outByT640();
    }

    public Map setEqptOffline() {
        return this.sendS1F15outByT640();
    }

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
            } else {
                holdSuccessFlag = false;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
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
            } else {
                holdSuccessFlag = false;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
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
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在：" + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
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
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
            holdDeviceAndShowDetailInfo();
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
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
        if (FengCeConstant.STATUS_IDLE.equalsIgnoreCase(equipStatus) || equipStatus.equalsIgnoreCase("UNKNOWN")) {
            return "0";
        } else {
            return "设备未处于" + FengCeConstant.STATUS_IDLE + "状态，不可调整Recipe！";
        }
    }

    @Override
    public String testRUThere() {
       return super.testRUThere();
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
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    @Override
    public boolean holdDeviceAndShowDetailInfo(String type) {
        Map resultMap = new HashMap();

        resultMap = holdDevice1();
        if (resultMap != null) {
            if ("0".equals(String.valueOf(resultMap.get("HCACK")))) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "当前设备已经被锁机");
                String dateStr = GlobalConstants.dateFormat.format(new Date());
                this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + type);
                return true;
            } else if ("4".equals(String.valueOf(resultMap.get("HCACK")))) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将稍后执行锁机");
                String dateStr = GlobalConstants.dateFormat.format(new Date());
                this.sendTerminalMsg2EqpSingle("(" + dateStr + ")" + type);
                return true;
            } else {
                Map eqptStateMap = this.findEqptStatus();
               UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "锁机失败，当前机台状态无法进行锁机，机台状态为：" + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                return false;
            }
        } else {
            return false;
        }
    }



    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // 上传ftp
        FtpUtil.uploadFile(localRcpPath, remoteRcpPath, recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + localRcpPath);
        List<String> rcpContent = TrRecipeUtil.readRCP(localRcpPath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        for (String item : rcpContent) {
            if (item.contains("@component@") || item.contains("@handler@")) {
                String relLocalPath = GlobalConstants.localRecipePath + new RecipeService(sqlSession).organizeUploadRecipePath(recipe) + item + "_V" + recipe.getVersionNo() + ".txt";
                String relRemotePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                FtpUtil.uploadFile(relLocalPath, relRemotePath, item + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "关联文件存储位置：" + relLocalPath);
            }
        }
        sqlSession.close();
        return true;
    }
}
