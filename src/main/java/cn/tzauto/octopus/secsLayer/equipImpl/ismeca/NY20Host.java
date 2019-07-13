package cn.tzauto.octopus.secsLayer.equipImpl.ismeca;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.ismeca.NY20RecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;


public class NY20Host extends EquipHost {


    private static final Logger logger = Logger.getLogger(NY20Host.class.getName());
    private boolean checkNameFlag = true;
    private boolean checkParaFlag = true;
    private long runCount = 0;

    public NY20Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
    }


    @Override
    public Object clone() {
        NY20Host newEquip = new NY20Host(deviceId,
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
        logger.info(deviceCode + "调用了run方法" + runCount);
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
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    updateLotId();
                    sendS2F37outAll();
//                    upLoadAllRcp();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    long ceid = 0;
                    ceid = (long) msg.get("CEID");
                    if (ceid == 11) {
                        processEquipStatusChange(msg);
                    } else {
                        if (ceid == 7) {
                            //刷新当前机台状态
                            logger.info("[" + deviceCode + "]" + "之前Recipe为：{" + ppExecName + "}");
                            findDeviceRecipe();
                            logger.info("[" + deviceCode + "]" + "切换Recipe为：{" + ppExecName + "}");
                        }
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
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    protected void processEquipStatusChange(DataMsgMap data) {
        //TODO 开机check;
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            //刷新当前机台状态
            findDeviceRecipe();
            logger.info("[" + deviceCode + "]" + "设备进入" + equipStatus + "状态！");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            //从数据库中获取当前设备模型信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            // 更新设备模型
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
                //锁机
                holdDevice();
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备模型信息,不允许开机！请联系ME处理！");
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceInfoExt.setConnectionStatus(controlState);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存到设备操作记录数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
//            if (AxisUtility.isEngineerMode(deviceCode)) {
//               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
//                return;
//            }
            //获取设备状态为ready时检查领料记录
            if (equipStatus.equalsIgnoreCase("Run")) {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备已被锁");
                    holdDeviceAndShowDetailInfo("RepeatAlarm LOCK");
                }
                //1、获取设备需要校验的信息类型,
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
//                    logger.info(deviceCode + "Trackin数据不完整，未设置当前机台应该执行的Recipe,设备被锁定!");
                    holdDevice();
                }
                if (!checkRecipeName(deviceInfoExt.getRecipeName())) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！");
//                    logger.info(deviceCode + "Recipe名称为:[" + ppExecName + "]，与改机后程序不一致，核对不通过，设备被锁定！");
                    checkNameFlag = false;
                } else {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为:[" + ppExecName + "]，与改机后程序一致，核对通过！");
//                    logger.info(deviceCode + " Recipe名称为:[" + ppExecName + "]，与改机后程序一致，核对通过！");
                    checkNameFlag = true;
                }
                StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass, equipment locked!");
                if (checkNameFlag && "A".equals(deviceInfoExt.getStartCheckMod())) {
                    //查询trackin时的recipe和GoldRecipe
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(deviceInfoExt.getRecipeName(), deviceType, "GOLD", String.valueOf(deviceInfoExt.getVerNo()));
                    boolean hasGoldRecipe = true;
                    //查询客户端数据库是否存在GoldRecipe
                    if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
                        hasGoldRecipe = false;
                    }
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = downLoadRecipe.getVersionType();
                    if (false) {
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数绝对值Check");
//                        logger.info(deviceCode + "开始执行Recipe:[" + ppExecName + "]参数绝对值Check");
                        this.startCheckRecipePara(downLoadRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe:[" + ppExecName + "]参数WICheck");
//                        logger.info(deviceCode + "开始执行Recipe:[" + ppExecName + "]参数WICheck");
                        if (!hasGoldRecipe) {
                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
//                            logger.info(deviceCode + "工控上不存在: [" + ppExecName + "]的Gold版本,无法执行开机检查,设备被锁定!");
                            //不允许开机
                            checkParaFlag = false;
                        } else {
                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe:[" + ppExecName + "]开始WI参数Check");
//                            logger.info(deviceCode + "Recipe:[" + ppExecName + "]开始WI参数Check");
//                            checkParaFlag = this.startCheckRecipeParaReturnFlag(downLoadGoldRecipe.get(0));
                            //向服务端发送机台被锁.更新服务端lockflag;
//                            if (checkParaFlag) {
//                                //解锁机台
//                                this.releaseDevice();
//                            } else {
//                                //锁机
//                                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
//                                //sendStatus2Server("LOCK");
//                            }
                            Map resultMap = this.startCheckRecipeParaReturnMap(downLoadGoldRecipe.get(0));
                            if (resultMap != null) {
                                if (resultMap.get("CheckParaFlag") != null) {
                                    checkParaFlag = (boolean) resultMap.get("CheckParaFlag");
                                    //显示比对不通过参数
                                    List<RecipePara> recipeParasdiff = null;
                                    if (!checkParaFlag && resultMap.get("RecipeParasdiff") != null && ((List<RecipePara>) resultMap.get("RecipeParasdiff")).size() > 0) {
                                        recipeParasdiff = (List<RecipePara>) resultMap.get("RecipeParasdiff");
//                                        recipeParasDiffText = new StringBuilder("StartCheck not pass, equipment locked!");
                                        for (RecipePara recipePara : recipeParasdiff) {
                                            recipeParasDiffText.append("\r\nError Para Name:");
                                            recipeParasDiffText.append(recipePara.getParaName());
                                            recipeParasDiffText.append(",\r\nRecipe Set Value:");
                                            recipeParasDiffText.append(recipePara.getSetValue());
                                            recipeParasDiffText.append(",\r\nGold Recipe Set Value;");
                                            recipeParasDiffText.append(recipePara.getDefValue());
                                        }
//                                        this.holdDeviceAndShowDetailInfo(recipeParasDiffText.toString());
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
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check参数模式！");
//                    logger.info(deviceCode + "没有设置开机check参数模式！");
                }
                //总结是否需要锁机

                if (!checkNameFlag || !checkParaFlag) {
                    //锁机
                    holdFlag = true;
                } else {
                    holdFlag = false;
                }
                //更新界面
                if (!this.checkLockFlagFromServerByWS(deviceCode) && !holdFlag) {
                    this.setAlarmState(0);
                } else {
                    if (checkNameFlag) {
                        this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!\r\n"
                                + "Recipe Setup Error！！！");
                    } else {
                        this.holdDeviceAndShowDetailInfo(recipeParasDiffText.toString());
                    }
                    this.setAlarmState(2);
//                    sendS2f41Cmd("RESUME");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }


    public Map startCheckRecipeParaReturnMap(Recipe checkRecipe) throws UploadRecipeErrorException {
        return startCheckRecipeParaReturnMap(checkRecipe, "");
    }

    /**
     * 开机check recipe参数
     *
     * @param checkRecipe
     * @param type
     */
    public Map startCheckRecipeParaReturnMap(Recipe checkRecipe, String type) throws UploadRecipeErrorException {
        boolean checkParaFlag = false;
        Map resultMap = new HashMap();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
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
//                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                logger.info(deviceCode + "开机检查未通过!");
                checkParaFlag = false;
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
//                    logger.info(deviceCode + eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                checkParaFlag = true;
                this.releaseDevice();
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
                logger.info(deviceCode + "开机Check通过！");
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

    // </editor-fold>

    @Override
    public String getRecipePathByConfig(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        String recipePathByConfig = GlobalConstants.localRecipePath + recipeService.organizeUploadRecipePath(recipe) + recipe.getRecipeName().replace("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo();
        if (recipePathByConfig.contains("*")) {
            recipePathByConfig = recipePathByConfig.replace("*", "X");
        }
        sqlSession.close();
        return recipePathByConfig;
    }

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param localFilePath
     * @param targetRecipeName
     * @return
     */
    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f2");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        long length = -1;
        length = TransferUtil.getPPLength(localFilePath);
        if (length == 0) {
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
            return resultMap;
        }
        DataMsgMap data = null;
        byte ppgnt = -1;
        try {
            data = activeWrapper.sendS7F1out(targetRecipeName, length, svFormat);
            ppgnt = (byte) data.get("PPGNT");
            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
            resultMap.put("ppgnt", ppgnt);
            resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ppgnt", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    /**
     * 下载Recipe，将原有的recipe使用指定的PPID下载到机台
     *
     * @param localRecipeFilePath
     * @param targetRecipeName
     * @return
     */
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        byte ackc7 = -1;
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName.replace("@", "/") + "", ppbody, SecsFormatValue.SECS_BINARY);
            ackc7 = (byte) data.get("ACKC7");

            resultMap.put("ACKC7", ackc7);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }


    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        List<RecipePara> recipeParaList = null;
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        try {
            recipeParaList = NY20RecipeUtil.transferRcpFromDB2(recipePath, recipeName, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("解析文件出错！", ex);
        } finally {
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s7f6");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("recipe", recipe);
            resultMap.put("recipeNameMapping", null);
            resultMap.put("recipeParaList", recipeParaList);
            resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
            resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
            return resultMap;
        }
    }


    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        Map resultMap = null;
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            if (!"Run".equalsIgnoreCase(equipStatus)) {
                //不在RUN状态，返回已被锁
                resultMap = new HashMap();
                resultMap.put("msgType", "s2f42");
                resultMap.put("deviceCode", deviceCode);
                resultMap.put("prevCmd", "STOP");
                resultMap.put("HCACK", 0);
                resultMap.put("Description", "设备已被锁,将无法开机");
                this.setAlarmState(2);
            } else {
                //RUN状态，发送停机指令
                resultMap = this.sendS2f41Cmd("PAUSE");
                if ((byte) resultMap.get("HCACK") == 0 || (byte) resultMap.get("HCACK") == 4) {
                    this.setAlarmState(2);
                }
            }
            return resultMap;
        } else {
//           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
            logger.info(deviceCode+"未设置锁机！");
            return null;
        }
    }

    private boolean rcpInEqp(String recipeName) {
        boolean rcpInEqp = false;
        ArrayList eppd = (ArrayList) sendS7F19out().get("eppd");
        for (int i = 0; i < eppd.size(); i++) {
            String rcpNameString = eppd.get(i).toString();
            if (recipeName.equals(rcpNameString)) {
                rcpInEqp = true;
            }
        }
        return rcpInEqp;
    }

    //释放机台
    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        return null;
    }

    @Override
    public Map startDevice() {
        Map data = null;
        try {
            this.sendS2f41Cmd("START");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        byte hcack = (byte) data.get("HCACK");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("HCACK", hcack);
        return resultMap;
    }
    // </editor-fold>


}
