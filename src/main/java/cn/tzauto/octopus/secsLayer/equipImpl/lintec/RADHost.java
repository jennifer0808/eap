package cn.tzauto.octopus.secsLayer.equipImpl.lintec;

import cn.tfinfo.jcauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tfinfo.jcauto.octopus.biz.device.service.DeviceService;
import cn.tfinfo.jcauto.octopus.biz.monitor.service.MonitorService;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.Recipe;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipePara;
import cn.tfinfo.jcauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tfinfo.jcauto.octopus.biz.recipe.service.RecipeService;
import cn.tfinfo.jcauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tfinfo.jcauto.octopus.common.globalConfig.GlobalConstants;
import cn.tfinfo.jcauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tfinfo.jcauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tfinfo.jcauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzinfo.smartSecsDriver.representation.secsii.FormatCode;
import cn.tzinfo.smartSecsDriver.userapi.MsgArrivedEvent;
import cn.tzinfo.smartSecsDriver.userapi.DataMsgMap;
import cn.tzinfo.smartSecsDriver.userapi.SecsItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.slf4j.MDC;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class RADHost extends EquipHost {

    private static Logger logger = Logger.getLogger(RADHost.class);

    public RADHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
                   int localTcpPort, String remoteIpAddress, int remoteTcpPort, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort, deviceType, deviceCode, recipeType, iconPtah);
        initRemoteCommand();
    }

    public RADHost(String devId, String equipmentId, String smlFileFullPath, String localIpAddress,
                   int localTcpPort, String remoteIpAddress, int remoteTcpPort,
                   String connectMode, String protocolType, String deviceType, String deviceCode, int recipeType, String iconPtah) {
        super(devId, equipmentId, smlFileFullPath, localIpAddress,
                localTcpPort, remoteIpAddress, remoteTcpPort,
                connectMode, protocolType, deviceType, deviceCode, recipeType, iconPtah);
        initRemoteCommand();
    }

    @Override
    public Object clone() {
        RADHost newEquip = new RADHost(deviceId, this.deviceCode,
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
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        long[] length = new long[1];
        length[0] = TransferUtil.getPPLength(localFilePath);
        DataMsgMap s7f1out = new DataMsgMap("s7f1out", activeWrapper.getDeviceId());
        s7f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f1out.put("ProcessprogramID", StringFormat.appendStr2Len(targetRecipeName, ' ', 16));
        s7f1out.put("Length", length);
        DataMsgMap data;
        byte[] ppgnt = new byte[1];
        try {
            data = activeWrapper.sendAwaitMessage(s7f1out);
            ppgnt = (byte[]) ((SecsItem) data.get("PPGNT")).getData();
            logger.debug("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ppgnt", ppgnt[0]);
        resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        return resultMap;
    }

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        DataMsgMap s7f3out = new DataMsgMap("s7f3out", activeWrapper.getDeviceId());
        s7f3out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        SecsItem secsItem = new SecsItem(ppbody, FormatCode.SECS_BINARY);
        s7f3out.put("ProcessprogramID", StringFormat.appendStr2Len(targetRecipeName, ' ', 16));
        s7f3out.put("Processprogram", secsItem);
        try {
            data = activeWrapper.sendAwaitMessage(s7f3out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte[] ackc7 = (byte[]) ((SecsItem) data.get("AckCode")).getData();
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        resultMap.put("ACKC7", ackc7[0]);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        //通过S7F19获取的recipe列表中含有空格
        recipeName = StringUtils.trim(recipeName);
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        DataMsgMap s7f5out = new DataMsgMap("s7f5out", activeWrapper.getDeviceId());
        s7f5out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        s7f5out.put("ProcessprogramID", StringFormat.appendStr2Len(recipeName, ' ', 16));
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendAwaitMessage(s7f5out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (msgdata == null || msgdata.isEmpty()) {
            UiLogUtil.appendLog2EventTab(deviceCode, "上传请求被设备拒绝,请调整设备状态重试.");
            return null;
        }

        final byte[] ppbody = (byte[]) ((SecsItem) msgdata.get("Processprogram")).getData();
        TransferUtil.setPPBody(ppbody, recipeType, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        try {
            recipeParaList = RecipeUtil.transfer2DB(new RecipeFileHandler() {
                @Override
                public Map<String, String> handler(File file) {
                    int paramVal = 0;
                    int idx = 0;
                    String[] paramNames = {"Config", "Exp dose", "UV Threshold", "Condition"};
                    Map<String, String> rcpParamMap = new HashMap<>();
                    for (int i = 0; i < ppbody.length; i++) {
                        int val = ppbody[i];
                        if (val < 0) {
                            val += 256;
                        }
                        paramVal |= val << 24 - 8 * (i % 4);
                        if (i % 4 == 3) {
                            if (idx == 1 || idx == 2) {
                                rcpParamMap.put(paramNames[idx++], StringFormat.convertNum2Str(paramVal / 10, 1));
                            } else {
                                rcpParamMap.put(paramNames[idx++], String.valueOf(paramVal));
                            }
                            paramVal = 0;
                        }
                    }
                    return rcpParamMap;
                }
            }, new File(recipePath), recipeTemplates, false);
        } catch (Exception ex) {
            logger.info("resolve recipe failed: " + ex);
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

    @Override
    public void initRemoteCommand() {
        initCommonRemoteCommand();
    }

    @Override
    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = new Date().getTime();
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
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("s6f11incommon")) {
                long ceid = data.getSingleNumber("CollEventID");
                if (ceid == 8 || ceid == 9 || ceid == 10) {
                    processS6F11in(data);
                } else if (ceid == 65 || ceid == 170 || ceid == 172 || ceid == 401) {
                    processS6F11in(data);
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equals("s6f11inStatusChange")) {
                processS6F11in(data);
            } else if (tagName.equalsIgnoreCase("s6f12in")) {
                processS6F12in(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f3in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s9f9Timeout")) {
                initRptPara();
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

    public void initRptPara() {
    }

    @Override
    public void run() {
        threadUsed = true;
//        sendS1F13out();
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, equipId);
        while (!isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    super.findDeviceRecipe();
                    //获取lot号
                    super.updateLotId();
                    initRptPara();
                    rptDefineNum++;
                }
                DataMsgMap msg = this.inputMsgQueue.take();
                String tagName = msg.getMsgSfName();
                if (tagName != null) {
                    if (tagName.equalsIgnoreCase("s5f1in")) {
                        processS5F1in(msg);
                    } else if (tagName.equals("s6f11incommon") || tagName.equals("s6f11incommon1")) {
                        long ceid = msg.getSingleNumber("CollEventID");
                        if (ceid == 65 || ceid == 401) {
                            setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                            processEquipStatusChange(msg);
                        } else if (ceid == 170) {
                            processWaitPPSELECT(msg);
                        } else if (ceid == 172) {
                            processPressStartButton(msg);
                        }
                    } else {
                        logger.info("A message in queue with tag = " + msg.getMsgSfName()
                                + " which I do not want to process! ");
                    }
                }

            } catch (Exception e) {
            }

        }
    }

    private void processWaitPPSELECT(DataMsgMap data) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        //String curRcp = ((SecsItem) data.get("PPName")).getData().toString();
        if (deviceInfoExt != null && deviceInfoExt.getRecipeName().equals(ppExecName.trim())) {
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName.trim() + "]，与改机后程序一致，核对通过！");
            sendS2F41outPPselect(ppExecName);
        } else {
            setAlarmState(2);
            sendTerminalMsg2EqpSingle("TASK-ID IS NOT CORRECT!");
            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName.trim() + "]，与改机后程序不一致，核对不通过，设备被锁定！");
        }
    }

    private void processPressStartButton(DataMsgMap data) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        boolean checkParaFlag = true;
        long ceid;
        logger.info("[" + deviceCode + "]" + "Start按钮被按下，设备开始作业！");
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            ceid = data.getSingleNumber("CollEventID");
            deviceInfoExt.setDeviceStatus(equipStatus);
            deviceInfoExt.setConnectionStatus(controlState);
            deviceService.modifyDeviceInfoExt(deviceInfoExt);
            //sqlSession.commit();
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
//            if (AxisUtility.isEngineerMode(deviceCode)) {
//                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
//                return;
//            }
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备已被锁");
                holdDeviceAndShowDetailInfo("RepeatAlarm LOCK");
            }
            //1、获取设备需要校验的信息类型,
            if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
                //holdDevice();
                setAlarmState(2);
            }
            if ("A".equals(deviceInfoExt.getStartCheckMod())) {
                List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
                if (false) {
                    //Unique
                    UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Unique Recipe:[" + ppExecName + "]参数绝对值Check");
                } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                    UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
                    //查询客户端数据库是否存在GoldRecipe
                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
                        //不允许开机
                        checkParaFlag = false;
                    } else {
                        // UiLogUtil.appendLog2EventTab(deviceCode, "Recipe:[" + ppExecName + "]开始WI参数Check");
                        Map resultMap = this.startCheckRecipeParaReturnMap(downLoadGoldRecipe.get(0));
                        if (resultMap != null) {
                            if (resultMap.get("CheckParaFlag") != null) {
                                checkParaFlag = (boolean) resultMap.get("CheckParaFlag");
                                //显示比对不通过参数
                                List<RecipePara> recipeParasdiff = null;
                                if (!checkParaFlag && resultMap.get("RecipeParasdiff") != null && ((List<RecipePara>) resultMap.get("RecipeParasdiff")).size() > 0) {
                                    recipeParasdiff = (List<RecipePara>) resultMap.get("RecipeParasdiff");
                                    StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass, equipment locked!");
                                    for (RecipePara recipePara : recipeParasdiff) {
                                        recipeParasDiffText.append("\r\nError Para Name:");
                                        recipeParasDiffText.append(recipePara.getParaShotName());
                                        recipeParasDiffText.append(",\r\nRecipe Set Value:");
                                        recipeParasDiffText.append(recipePara.getSetValue());
                                        recipeParasDiffText.append(",\r\nGold Recipe Set Value;");
                                        recipeParasDiffText.append(recipePara.getDefValue());
                                    }
                                    this.holdDeviceAndShowDetailInfo(recipeParasDiffText.toString());
                                }
                            } else {
                                checkParaFlag = false;
                            }
                        } else {
                            checkParaFlag = false;
                        }
                    }
                }
            } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                //如果未设置参数比对模式，默认参数比对通过
                checkParaFlag = true;
                UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check参数模式！");
            }
            if (!checkParaFlag) {
                //锁机
                holdFlag = true;
            } else {
                holdFlag = false;
            }
            //更新界面
            if (!this.checkLockFlagFromServerByWS(deviceCode) && !holdFlag) {
                sendS2f41Cmd("G100");
                this.setAlarmState(0);
            } else {
                this.setAlarmState(2);
            }
        } catch (Exception e) {
            logger.info("device:" + deviceCode + e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    private void processEquipStatusChange(DataMsgMap msg) {
        long ceid = 0l;
        try {
            ceid = msg.getSingleNumber("CollEventID");
            //刷新当前机台状态
            sendS1F3Check();
            logger.info("[" + deviceCode + "]" + "设备进入" + equipStatus + "状态！");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                //锁机
                holdDevice();
                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备模型信息,不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceInfoExt.setConnectionStatus(controlState);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
        } catch (Exception e) {
            sqlSession.rollback();
            logger.info("Exception occure:" + e);
        } finally {
            sqlSession.close();
        }
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        Map resultMap = null;
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            sendS1F3Check();
            if (!"EXECUTING".equalsIgnoreCase(equipStatus)) {
                //不在RUN状态，已被锁
                resultMap = new HashMap();
                resultMap.put("msgType", "s2f42");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("prevCmd", "STOP");
                resultMap.put("HCACK", 0);
                resultMap.put("Description", "设备已被锁,将无法开机");
                this.setAlarmState(2);
            } else {
                resultMap = this.sendS2f41Cmd("PAUSE");
                if ((byte) resultMap.get("HCACK") == 0) {
                    this.setAlarmState(2);
                }
            }
            return resultMap;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            return null;
        }
    }

    public Map startCheckRecipeParaReturnMap(Recipe checkRecipe) {
        return startCheckRecipeParaReturnMap(checkRecipe, "");
    }

    public Map startCheckRecipeParaReturnMap(Recipe checkRecipe, String type) {
        boolean checkParaFlag = false;
        Map resultMap = new HashMap();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.eapView.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
                checkParaFlag = false;
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                checkParaFlag = true;
                this.releaseDevice();
                UiLogUtil.appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.info("设备：" + deviceCode + " 开机Check成功");
            }
            mqMap.put("eventDesc", eventDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
            resultMap.put("CheckParaFlag", checkParaFlag);
            resultMap.put("RecipeParasdiff", recipeParasdiff);
            return resultMap;
        }
    }


    @Override
    public Map sendS2F41outPPselect(String recipeName) {
        DataMsgMap s2f41out = new DataMsgMap("s2f41outPPSelect", activeWrapper.getDeviceId());
        s2f41out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        recipeName = StringFormat.appendStr2Len(recipeName, ' ', 16);
        s2f41out.put("PPID", recipeName);
        byte[] hcack = new byte[1];
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            DataMsgMap data = activeWrapper.sendAwaitMessage(s2f41out);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            hcack = (byte[]) ((SecsItem) data.get("HCACK")).getData();
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack[0]);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack[0] + " means " + e.getMessage());
        }
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
        long transactionId = activeWrapper.getNextAvailableTransactionId();
        s7f19out.setTransactionId(transactionId);
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendAwaitMessage(s7f19out);
        } catch (Exception e) {
            logger.error("Exception:", e);
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
            ArrayList listtmp = new ArrayList();
            for(SecsItem secsItem : list) {
                listtmp.add(secsItem.getData().toString().trim());
            }
            resultMap.put("eppd", listtmp);
        }
        return resultMap;
    }

    static class StringFormat {

        static String appendStr2Len(String org, char append, int len) {
            int l = org.length();
            if (l >= len) {
                return org;
            }
            StringBuilder sb = new StringBuilder(org);
            for (int i = l; i < len; i++) {
                sb.append(append);
            }
            return sb.toString();
        }

        /**
         * @param val
         * @param digit
         * @return
         */
        static String convertNum2Str(double val, int digit) {
            if (digit <= 0) {
                return String.valueOf(val);
            }
            StringBuilder sb = new StringBuilder("##0.");
            for (int i = 0; i < digit; i++) {
                sb.append("0");
            }
            NumberFormat nf = new DecimalFormat(sb.toString());
            return nf.format(val);
        }
    }
}
