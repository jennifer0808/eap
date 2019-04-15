package cn.tzauto.octopus.secsLayer.equipImpl.towa.mold;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandParaPair;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.towa.TowaRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

@SuppressWarnings("serial")
public class TowaHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(TowaHost.class.getName());

    public TowaHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        EquipStateChangeCeid = 50007;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }


    @Override
    public Object clone() {
        TowaHost newEquip = new TowaHost(deviceId,
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
        while (!this.isInterrupted) {
            try {
                while (!this.isSdrReady()) {
                    TowaHost.sleep(200);
                }
                if (this.getCommState() != TowaHost.COMMUNICATING) {
                    this.sendS1F13out();
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
                    initRemoteCommand();
                    rptDefineNum++;
//                    sendS5F3out(true);
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in") || msg.getMsgSfName().equalsIgnoreCase("s5f1ypmin")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f112dcodereview")) {
                    processS6f11StripIDReview(msg);
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
                    handleCleanRecipe(ppExecName);
                } else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                logger.info(getName() + "从阻塞中退出...");
                logger.info("this.isInterrupted()=" + this.isInterrupted() + " is interrupt=" + isInterrupted);
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
            secsMsgTimeoutTime = 0;
            LastComDate = System.currentTimeMillis();
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1ypmin")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
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

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    public List sendS1F3PressCheckout() {
        List svidlist = new ArrayList<>();
        if ("TOWAYPM".equals(deviceType)) {
            svidlist.add(501L);
            svidlist.add(601L);
            svidlist.add(701L);
            svidlist.add(801L);
        } else if ("TOWAY1R".equals(deviceType)) {
            svidlist.add(1211L);
            svidlist.add(2211L);
            svidlist.add(3211L);
            svidlist.add(4211L);
        } else if ("TOWAYPS".equals(deviceType)) {
            svidlist.add(100L);
            svidlist.add(200L);
            svidlist.add(300L);
            svidlist.add(400L);
        }
        DataMsgMap data = null;
        try {
            data = activeWrapper.sendS1F3out(svidlist, svFormat);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (data == null || data.isEmpty()) {
            return null;
        }
        ArrayList<SecsItem> list = (ArrayList) data.get("SV");
        ArrayList<Object> listtmp = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
        return listtmp;
    }

    // </editor-fold> 

    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == EquipStateChangeCeid) {
                processS6F11EquipStatusChange(data);
            } else if (ceid == 50013) {
                findDeviceRecipe();
                handleCleanRecipe(ppExecName);
            } else if (ceid == 19006L) {
                processS6f11StripIDReview(data);
            } else if (ceid == 50006 || ceid == 50005 || ceid == 50004 || ceid == 50003 ||
                    ceid == 53 || ceid == 52 || ceid == 51 || ceid == 50) {
                if (ceid == 50006 || ceid == 53) {
                    super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
                } else if (ceid == 50005 || ceid == 52) {
                    super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                } else if (ceid == 50004 || ceid == 51) { //待验证
                    super.setControlState(FengCeConstant.CONTROL_OFFLINE);
                } else if (ceid == 50003 || ceid == 50) {
                    super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
                }
                updateCommStateInExt();
            }
            if (ceid == 9) {
                equipStatus = "Stopping";
                Map map = new HashMap();
                map.put("EquipStatus", equipStatus);
                changeEquipPanel(map);
                updateCommStateInExt();
            }
            if (ceid == 10 && holdFlag) {
                holdDevice();
                Map map = new HashMap();
                map.put("EquipStatus", "PAUSE");
                changeEquipPanel(map);
                updateCommStateInExt();
            }


            if ("TOWAPMC".equalsIgnoreCase(deviceType) || "TOWAY1E".equalsIgnoreCase(deviceType)) {
                if (ceid == 2080) {
                    super.pressUseMap.clear();
                }
                if (ceid == 2020) {
                    super.pressUseMap.put(1, true);
                }
                if (ceid == 2030) {
                    super.pressUseMap.put(2, true);
                }
                if (ceid == 2040) {
                    super.pressUseMap.put(3, true);
                }
                if (ceid == 2050) {
                    super.pressUseMap.put(4, true);
                }
            }
            if ("TOWAYPS".equalsIgnoreCase(deviceType)) {
                if (ceid == 226L) {
                    super.pressUseMap.clear();
                }
                if (ceid == 204L) {
                    super.pressUseMap.put(1, true);
                }
                if (ceid == 206L) {
                    super.pressUseMap.put(2, true);
                }
                if (ceid == 208L) {
                    super.pressUseMap.put(3, true);
                }
                if (ceid == 210L) {
                    super.pressUseMap.put(4, true);
                }
            }
            if ("TOWAYPM".equalsIgnoreCase(deviceType)) {
                if (ceid == 222) {
                    super.pressUseMap.clear();
                }
                if (ceid == 228) {
                    super.pressUseMap.put(1, true);
                }
                if (ceid == 230) {
                    super.pressUseMap.put(2, true);
                }
                if (ceid == 232) {
                    super.pressUseMap.put(3, true);
                }
                if (ceid == 234) {
                    super.pressUseMap.put(4, true);
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 50006 || ceid == 53) {
                super.setControlState(FengCeConstant.CONTROL_LOCAL_ONLINE);
            } else if (ceid == 50005 || ceid == 52) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 50004 || ceid == 51) { //待验证
                super.setControlState(FengCeConstant.CONTROL_OFFLINE);
            } else if (ceid == 50003 || ceid == 50) {
                super.setControlState(FengCeConstant.CONTROL_REMOTE_ONLINE);
            } else if (ceid == 9) {
                equipStatus = "Stopping";
            } else if (ceid == 10) {
                if (holdFlag) {
                    holdDevice();
                }
            }
            updateCommStateInExt();

            if ("TOWAPMC".equalsIgnoreCase(deviceType) || "TOWAY1E".equalsIgnoreCase(deviceType)) {
                if (ceid == 2080) {
                    super.pressUseMap.clear();
                }
                if (ceid == 2020) {
                    super.pressUseMap.put(1, true);
                }
                if (ceid == 2030) {
                    super.pressUseMap.put(2, true);
                }
                if (ceid == 2040) {
                    super.pressUseMap.put(3, true);
                }
                if (ceid == 2050) {
                    super.pressUseMap.put(4, true);
                }
            }
            if ("TOWAYPS".equalsIgnoreCase(deviceType)) {
                if (ceid == 226L) {
                    super.pressUseMap.clear();
                }
                if (ceid == 204L) {
                    super.pressUseMap.put(1, true);
                }
                if (ceid == 206L) {
                    super.pressUseMap.put(2, true);
                }
                if (ceid == 208L) {
                    super.pressUseMap.put(3, true);
                }
                if (ceid == 210L) {
                    super.pressUseMap.put(4, true);
                }
            }
            if ("TOWAYPM".equalsIgnoreCase(deviceType)) {
                if (ceid == 222) {
                    super.pressUseMap.clear();
                }
                if (ceid == 228) {
                    super.pressUseMap.put(1, true);
                }
                if (ceid == 230) {
                    super.pressUseMap.put(2, true);
                }
                if (ceid == 232) {
                    super.pressUseMap.put(3, true);
                }
                if (ceid == 234) {
                    super.pressUseMap.put(4, true);
                }
                if (ceid == 10) {
                    Map map = new HashMap();
                    map.put("EquipStatus", "PAUSE");
                    changeEquipPanel(map);
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
//            equipStatus = ACKDescription.descriptionStatus(String.valueOf(data.getSingleNumber("EquipStatus")), deviceType);
//            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
            findDeviceRecipe();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        //将设备的当前状态显示在界面上
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        handleCleanRecipe(ppExecName);

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
            //发送设备UPH参数至服务端
            UiLogUtil.appendLog2SeverTab(deviceCode, "设备由于状态变化即将发送UPH参数");
            sendUphData2Server();

            boolean checkResult = false;
            //获取设备当前运行状态，如果是Run，执行开机检查逻辑
            if (!isCleanRecipe && dataReady && equipStatus.equalsIgnoreCase("run")) {
                //开机获取Press使用情况
                getUsingPress();
                //检查press使用情况与MES是否相符，如果不相符，流程继续
                checkPressUseState(deviceService, sqlSession);
//                if (!checkPressUseState(deviceService, sqlSession)) {
//                    return;
//                }
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
//                Map checkServerLockResult = checkLockFlagAndReasonFromServerByWS(deviceCode);
//                String lockFlag = String.valueOf(checkServerLockResult.get("lockFlag"));
//                if ("Y".equals(lockFlag)) {
//                    String lockReason = String.valueOf(checkServerLockResult.get("remarks"));
//                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机, 锁机原因为: " + lockReason);
//                    holdDeviceAndShowDetailInfo("Equipment locked because of " + lockReason);
//                }
                if (!this.checkLockFlagFromServerByWS(deviceCode)) {
//                    UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
//                    holdDeviceAndShowDetailInfo("Equipment has been set and locked by Server");
//                } else {
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
                            holdDeviceAndShowDetailInfo("RecipeName Error! Equipment locked!");
                            holdFlag = true;
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                            holdFlag = false;
                        }
                    }
                    if (checkResult && "A".equals(startCheckMod)) {
                        //首先判断下载的Recipe类型
                        //1、如果下载的是Unique版本，那么执行完全比较
                        String downloadRcpVersionType = downLoadRecipe.getVersionType();
                        if ("Unique".equals(downloadRcpVersionType)) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check(Unique)");
                            this.startCheckRecipePara(downLoadRecipe, "abs");
                        } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                            UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck(Gold)");
                            if (!hasGoldRecipe) {
                                UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                                //不允许开机
                                this.holdDeviceAndShowDetailInfo("Host has no gold recipe, equipment locked!");
                                holdFlag = true;
                            } else {
                                UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                                this.startCheckRecipePara(downLoadGoldRecipe.get(0));
                                holdFlag = false;
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

    protected void processS6f11StripIDReview(DataMsgMap data) {
        long pressNoInt = 0L;
        String pressNo = "";
        long rptId = 0L;
        String recipeName = "";
        String formingNo = "";
        String leftStripID = "";
        String rightStripID = "";

        try {
            List report = (List) data.get("REPORT");
            rptId = (long) report.get(0);
            if (rptId == 19006) {
                List dataList = (List) report.get(1);
                pressNoInt = (long) dataList.get(0);
                pressNo = String.valueOf(pressNoInt);

                recipeName = String.valueOf(dataList.get(1));
                formingNo = String.valueOf(dataList.get(2));
                leftStripID = String.valueOf(dataList.get(3));
                rightStripID = String.valueOf(dataList.get(4));
            }
        }
        catch (Exception e){
            logger.error("Exception : " + e);
        }
        try {
            logger.info("pressNo=" + pressNo + "; recipeName=" + recipeName + "; formingNo=" + formingNo + "; leftStripID=" + leftStripID + "; rightStripID=" + rightStripID);
            Map resultMap = AxisUtility.get2DCode(deviceCode, pressNo, recipeName, formingNo, leftStripID, rightStripID);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
        }

    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
//            recipeParaList = TowaRecipeUtil.transferTowaRcp(TowaRecipeUtil.Y1R_RECIPE_CONFIG, ppbody);
            recipeParaList = TowaRecipeUtil.transferTowaRcpFromDB(deviceType, ppbody);
            for (int i = 0; i < recipeParaList.size(); i++) {
                String paraName = recipeParaList.get(i).getParaName();
                if (paraName.equals("") || paraName.equals("NULL")) {
                    recipeParaList.remove(i);
                    i--;
                }
            }
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
    // </editor-fold>

    //获取press使用情况
    public void getUsingPress() {
        //Y1E设备press抓取不准确，使用原来方式抓取
        if ("TOWAY1E".equals(deviceType)) {
            return;
        }
        List pressResults = sendS1F3PressCheckout();
        super.pressUseMap.clear();
        String pressStatusAll = "";
        for (int i = 0; i < pressResults.size(); i++) {
            String pressStatus = String.valueOf(pressResults.get(i));
            if ("true".equals(pressStatus)) {
                pressStatusAll = pressStatusAll + (i + 1) + ",";
                super.pressUseMap.put(i + 1, true);
            }
        }
        if (pressStatusAll.length() > 0) {
            pressStatusAll = pressStatusAll.substring(0, pressStatusAll.length() - 1);
        }
        UiLogUtil.appendLog2EventTab(deviceCode, "设备正在使用的Press为[" + pressStatusAll + "]");
    }

//    @Override
//    public Map findEqptStatus() {
//        //TOWA设备由于获取不了controlstate，所以设备状态从deviceInfoExt表中获取
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        sqlSession.close();
//        Map resultMap = new HashMap();
//        resultMap.put("EquipStatus", deviceInfoExt.getDeviceStatus());
//        resultMap.put("ControlState", deviceInfoExt.getConnectionStatus());
//        return resultMap;
//    }


    private void sendS2f33outMulti(long reportId, long svid0, long svid1, long svid2, long svid3, long svid4) {
        DataMsgMap s2f33out = new DataMsgMap("s2f33outmulti", activeWrapper.getDeviceId());
        s2f33out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        long[] dataid = new long[1];
        dataid[0] = reportId;
        long[] reportid = new long[1];
        reportid[0] = reportId;
        long[] variableID0 = new long[1];
        variableID0[0] = svid0;
        long[] variableID1 = new long[1];
        variableID1[0] = svid1;
        long[] variableID2 = new long[1];
        variableID2[0] = svid2;
        long[] variableID3 = new long[1];
        variableID3[0] = svid3;
        long[] variableID4 = new long[1];
        variableID4[0] = svid4;
        s2f33out.put("DataID", dataid);
        s2f33out.put("ReportID", reportid);
        s2f33out.put("VariableID0", variableID0);
        s2f33out.put("VariableID1", variableID1);
        s2f33out.put("VariableID2", variableID2);
        s2f33out.put("VariableID3", variableID3);
        s2f33out.put("VariableID4", variableID4);
        try {
            activeWrapper.sendAwaitMessage(s2f33out);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    private void initRptPara() {
        //重定义towa 机台的equipstatuschange事件报告 用于在设备状态改变时获取equipstatus和ppexecname
        if (!deviceType.equals("TOWAYPM")) {
            List list = new ArrayList();
            list.add(902L);
            list.add(906L);
//            sendS2F33Out(50007L, 902L, 906L);
            sendS2F33Out(50007L, 50007L, list);
            sendS2F35out(50007L, 50007L, 50007L);
            sendS2F37out(50007L);
        } else {
            List list = new ArrayList();
            list.add(102L);
            list.add(111L);
//            sendS2F33Out(271L, 102L, 111L);
            sendS2F33Out(271L, 271L, list);
            sendS2F35out(271L, 271L, 271L);
            sendS2F37out(271L);
        }
//        sendS2F37outAll(this.activeWrapper);
        //重定义towa 机台的pp-select事件报告 用于在设备状态改变时获取ppexecname

        if (deviceType.equals("TOWAY1R")) { //deviceType.equals("TOWAY1E") ||
            //重定义 ppselect完成事件报告
            List list = new ArrayList();
            list.add(906L);

            sendS2F33Out(50013L, 50013L, list);
            sendS2F35out(50013L, 50013L, 50013L);
            sendS2F37out(50013L);
            //重定义 2DCode追溯事件报告
            sendS2f33outMulti(19005L, 1052L, 906L, 1053L, 1050L, 1051L);
            sendS2F35out(19005L, 19005L, 19005L);
            sendS2F37out(19005L);

            sendS2f33outMulti(19006L, 2052L, 906L, 2053L, 2050L, 2051L);
            sendS2F35out(19006L, 19006L, 19006L);
            sendS2F37out(19006L);

            sendS2f33outMulti(19007L, 3052L, 906L, 3053L, 3050L, 3051L);
            sendS2F35out(19007L, 19007L, 19007L);
            sendS2F37out(19007L);

            sendS2f33outMulti(19008L, 4052L, 906L, 4053L, 4050L, 4051L);
            sendS2F35out(19008L, 19008L, 19008L);
            sendS2F37out(19008L);
        }
        if (deviceType.equals("TOWAPMC")) {
            List list = new ArrayList();
            list.add(906L);

            //重定义 recipe change事件报告
            sendS2F33Out(50010L, 50010L, list);
            sendS2F35out(50010L, 50010L, 50010L);
            sendS2F37out(50010L);
        }
        if (deviceType.equals("TOWAY1E")) {
            //重定义 recipe change事件报告
            List list = new ArrayList();
            list.add(906L);
            sendS2F33Out(5001400L, 5001400L, list);
            sendS2F35out(5001400L, 50014L, 5001400L);
            sendS2F37out(50014L);
        }
    }

    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map map = this.sendS2f41Cmd("STOP");
            if ("lock".equalsIgnoreCase(deviceInfoExt.getRemarks())) {
                map = this.sendS2f41Cmd("LOCK");
            }
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
            }
            holdFlag = true;
            return map;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            holdFlag = false;
            return null;
        }
    }

    @Override
    public Map lockDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            this.sendS2f41Cmd("STOP");
            Map map = this.sendS2f41Cmd("LOCK");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(2);
            }
            holdFlag = true;
            return map;
        } else {
            UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
            holdFlag = false;
            return null;
        }
    }

    //hold机台，先停再锁
    public Map holdDeviceTest() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        try {
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                Map reMap = this.sendS2f41Cmd("STOP");
                logger.info(String.valueOf(reMap.get("Description")));
                reMap = this.sendS2f41Cmd("LOCK");
                logger.info(String.valueOf(reMap.get("Description")));
                Thread.sleep(1000);
                reMap = this.sendS2f41Cmd("RELEASE");
                Thread.sleep(1000);
                logger.info(String.valueOf(reMap.get("Description")));
                Map map = this.sendS2f41Cmd("STOP");//Map map = this.sendS2f41Cmd("LOCK");
                if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                    this.setAlarmState(2);
                }
                return map;
            } else {
                UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
    }

    //释放机台，默认不发指令
    @Override
    public Map releaseDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        Map map = new HashMap();
        if ("lock".equalsIgnoreCase(deviceInfoExt.getRemarks())) {
            map = this.sendS2f41Cmd("RELEASE");
            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
                this.setAlarmState(0);
            }
        } else {
            map.put("HCACK", 0);
            this.setAlarmState(0);
        }
        holdFlag = false;
        return map;
    }

    @Override
    public Map unlockDevice() {
        Map map = this.sendS2f41Cmd("RELEASE");//RELEASE
        if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
            this.setAlarmState(0);
        }
        holdFlag = false;
        return map;
    }

    //根据recipeName和devide_code查询recipe,判断recipe是"Y"的时候，进行清模，获取SV数据，并发送给服务端
    public void handleCleanRecipe(String recipeName) {
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<Recipe> recipes = recipeService.searchRecipeByPara(recipeName, deviceCode, null, null);
        if (!isCleanMode) {
            //非清模模式，清模开始
            if (recipes != null && !recipes.isEmpty()) {
                if ("Y".equals(recipes.get(0).getRecipeType())) {
                    sendCleaningSV2Server("TransferSVBeforeClean", recipeService);
                    super.isCleanMode = true;
                }
            } else {
                //清模模式，清模结束
                if (recipes != null && !recipes.isEmpty()) {
                    if (!"Y".equals(recipes.get(0).getRecipeType())) {
                        sendCleaningSV2Server("TransferSVAfterClean", recipeService);
                        super.isCleanMode = false;
                    }
                }
            }
            sqlSession.close();
        }
    }

    public List getSvIdList(List<RecipeTemplate> recipeTemplates) {
        List svIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            svIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return svIdList;
    }

    public void sendCleaningSV2Server(String msgName, RecipeService recipeService) {
        Map svMap = new HashMap();
        String pressUse = "";
        List<String> pressList = new ArrayList();
        if (pressUseMap.containsKey(1)) {
            if (pressUseMap.get(1)) {
                pressUse = pressUse + "1,";
                String p1 = "P1RecipeParaCheck";
                pressList.add(p1);
            }
        }
        if (pressUseMap.containsKey(2)) {
            if (pressUseMap.get(2)) {
                pressUse = pressUse + "2,";
                String p2 = "P2RecipeParaCheck";
                pressList.add(p2);
            }
        }
        if (pressUseMap.containsKey(3)) {
            if (pressUseMap.get(3)) {
                pressUse = pressUse + "3,";
                String p3 = "P3RecipeParaCheck";
                pressList.add(p3);
            }
        }
        if (pressUseMap.containsKey(4)) {
            if (pressUseMap.get(4)) {
                pressUse = pressUse + "4,";
                String p4 = "P4RecipeParaCheck";
                pressList.add(p4);
            }
        }
        svMap.put("msgName", msgName);
        svMap.put("deviceCode", deviceCode);
        svMap.put("pressUse", pressUse);
        Map resultMap = new HashMap<>();
        List<RecipeTemplate> recipeTemplatesAll = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipeParaCheck");
        List<RecipeTemplate> recipeTemplates = new ArrayList();
        if (!pressList.isEmpty()) {
            recipeTemplates = recipeService.searchPressRecipeTemplateByDeviceCode(deviceType, pressList);
        }
        if (recipeTemplatesAll != null || !recipeTemplatesAll.isEmpty()) {
            List svIdListAll = getSvIdList(recipeTemplatesAll);
            List svIdList = getSvIdList(recipeTemplates);
            //todo sendS1F3RcpParaCheckout

        }
        GlobalConstants.C2SEqptLogQueue.sendMessage(svMap);
        UiLogUtil.appendLog2SeverTab(deviceCode, "成功发送设备清模状态sv参数信息到服务端");
    }

    @Override
    public void sendUphData2Server() {
        String output = this.getOutputData() == null ? "" : this.getOutputData();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<String> svidlist = recipeService.searchShotSVByDeviceType(deviceType);
        sqlSession.close();
        //获取前一状态与当前状态
        //todo sendS1F3RcpAndStateCheck();
//        sendS1F3RcpAndStateCheck();
        //todo sendS1F3SVShotCountCheckout
//        Map shotCountMap = sendS1F3SVShotCountCheckout(svidlist);
//        //towa设备取产量乘以2，用shotCount表示产量
//        if (shotCountMap != null && !shotCountMap.isEmpty()) {
//            Set<Map.Entry> entry = shotCountMap.entrySet();
//            for (Map.Entry e : entry) {
//                double value = Double.parseDouble(e.getValue().toString());
//                value = value * 2;
//                shotCountMap.put(e.getKey(), value);
//            }
//        }
//        Map mqMap = new HashMap();
//        mqMap.put("msgName", "UphDataTransfer");
//        mqMap.put("deviceCode", deviceCode);
//        mqMap.put("equipStatus", equipStatus);
//        mqMap.put("preEquipStatus", preEquipStatus);
//        mqMap.put("currentRecipe", ppExecName);
//        mqMap.put("lotId", lotId);
//        mqMap.put("shotCount", JsonMapper.toJsonString(shotCountMap));
//        mqMap.put("output", output);
//        mqMap.put("unit", "");
//        mqMap.put("currentTime", GlobalConstants.dateFormat.format(new Date()));
//        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
//        UiLogUtil.appendLog2SeverTab(deviceCode, "发送设备UPH参数至服务端");
//        logger.info("设备 " + deviceCode + " UPH参数为:" + mqMap);
//        UiLogUtil.appendLog2SeverTab(deviceCode, "UPH参数为:" + mqMap);
    }

    @Override
    public void initRemoteCommand() {
        //设置commandParalist
        List<CommandParaPair> paraList = new ArrayList<>();
//        CommandParaPair cpp = new CommandParaPair();
//        cpp.setCpname("");
//        cpp.setCpval("");
//        paraList.add(cpp);
        //设置commandKey
        String commandKey = "";
//        commandKey = "start";
//        CommandDomain startCommand = new CommandDomain();
//        startCommand.setRcmd("START");
//        startCommand.setParaList(paraList);
//        this.remoteCommandMap.put(commandKey, startCommand);

        commandKey = "stop";
        CommandDomain stopCommand = new CommandDomain();
        stopCommand.setRcmd("STOP");
        stopCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, stopCommand);

        commandKey = "lock";
        CommandDomain pauseCommand = new CommandDomain();
        pauseCommand.setRcmd("LOCK");
        pauseCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, pauseCommand);

        commandKey = "release";
        CommandDomain resumeCommand = new CommandDomain();
        resumeCommand.setRcmd("RELEASE");
        resumeCommand.setParaList(paraList);
        this.remoteCommandMap.put(commandKey, resumeCommand);
        //调用父类的方法，生成公用命令，如果不支持，可以删掉，如果不公用，直接覆盖
        //initCommonRemoteCommand();
    }

    public Map getSVShotCountValue() {
        Map resultMap = new HashMap();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<String> svidlist = recipeService.searchShotSVByDeviceType(deviceType);
        sqlSession.close();
        if (svidlist != null && !svidlist.isEmpty()) {
            //todo sendS1F3SVShotCountCheckout
//            resultMap = sendS1F3SVShotCountCheckout(svidlist);
        }
        return resultMap;
    }

    /**
     * 开机check recipe参数 覆写父类方法，增加发送设备消息日志
     *
     * @param checkRecipe
     * @param type
     */
    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
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
                this.holdDeviceAndShowDetailInfo("StartCheck not pass, equipment locked!");
                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查未通过!");
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为：" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
                    String dateStr = GlobalConstants.dateFormat.format(new Date());
                    String eventDescEN = "(" + dateStr + ") Start Check Para Error! RecipePara[" + recipePara.getParaName() + "], realValue: " + recipePara.getSetValue() + ", defaultValue: " + recipePara.getDefValue() + ", out of spec[" + recipePara.getMinValue() + "-" + recipePara.getMaxValue() + "], machine locked.";
                    this.sendTerminalMsg2EqpSingle(eventDescEN);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
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
        }
    }
}
