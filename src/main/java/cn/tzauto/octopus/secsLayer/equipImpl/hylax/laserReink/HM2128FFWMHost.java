/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.hylax.laserReink;

/**
 * @author njtz
 */


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.LaserRelinkUtil;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class HM2128FFWMHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(HM2128FFWMHost.class);
    public Map<Integer, Boolean> pressUseMap = new HashMap<>();

    public HM2128FFWMHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
//        EquipStateChangeCeid = 1009;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        EquipStateChangeCeid = 5L;
    }


    @Override
    public Object clone() {
        HM2128FFWMHost newEquip = new HM2128FFWMHost(deviceId,
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
                    HM2128FFWMHost.sleep(200);
                }
                if (this.getCommState() != HM2128FFWMHost.COMMUNICATING) {

                    this.sendS1F13out();
                    sendS1F17out();
                }
//                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
//                    sendS1F1out();
//                    //为了能调整为online remote
////                    sendS1F17out();
//                    //获取设备开机状态
//
//                    //super.findDeviceRecipe();
//                    this.sendS1F3Check();
//
//                    //initRptPara();
//                }

//                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
//                    sendS1F1out();
//                    //获取设备开机状态
//                    super.findDeviceRecipe();
//                    initRptPara();
//                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in") || msg.getMsgSfName().equalsIgnoreCase("s5f1ypmin")) {
                    replyS5F2Directly(msg);
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s1f14in")) {
                    super.findDeviceRecipe();
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11equipstatuschange")) {
                    try {
                        processS6F11EquipStatusChange(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);

                } else {
                    logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
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
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
                if (!this.controlState.equalsIgnoreCase(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    this.inputMsgQueue.put(data);
                }
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                System.out.println("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
    @Override
    @SuppressWarnings("unchecked")
    public Map sendS1F3Check() {
        List listtmp = getNcessaryData();
        if (listtmp != null) {
            equipStatus = ACKDescription.descriptionStatus(String.valueOf(listtmp.get(0)), deviceType);
            ppExecName = String.valueOf(listtmp.get(1));
            controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
        }
        if (controlState.equalsIgnoreCase("OnlineLocal")) {
            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
        } else if (controlState.equalsIgnoreCase("OnlineRemote")) {
            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
        } else if (controlState.equalsIgnoreCase("EquipmentOffline")) {
            controlState = FengCeConstant.CONTROL_OFFLINE;
        }
        Map panelMap = new HashMap();
        panelMap.put("EquipStatus", equipStatus);
        panelMap.put("PPExecName", ppExecName);
        panelMap.put("ControlState", controlState);
        changeEquipPanel(panelMap);
        return panelMap;
    }
    // </editor-fold> 

    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            if (ceid == 1009) {
                processS6F11EquipStart(data);
            } else if (ceid == EquipStateChangeCeid) {
                processS6F11EquipStatusChange(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            findDeviceRecipe();
            ppExecName.replace(".rcp", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (controlState.equalsIgnoreCase("OnlineLocal")) {
            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
        } else if (controlState.equalsIgnoreCase("OnlineRemote")) {
            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
        } else if (controlState.equalsIgnoreCase("EquipmentOffline")) {
            controlState = FengCeConstant.CONTROL_OFFLINE;
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        changeEquipPanel(map);

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        //保存到设备操作记录数据库
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
        DeviceOplog deviceOplog = new DeviceOplog();
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            if (deviceInfoExt == null) {
                deviceInfoExt = setDeviceInfoExt();
                deviceService.saveDeviceInfoExt(deviceInfoExt);
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                holdDeviceAndShowDetailInfo();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
            }

            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
                deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态到服务端
//                sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
            } else {
                String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
                if (!formerDeviceStatus.equals(equipStatus)) {
                    deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
                    deviceService.saveDeviceOplog(deviceOplog);

                    //发送设备状态到服务端
//                    sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                    // sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpDesc(),deviceOplog.getOpTime());
                }
            }
            sqlSession.commit();
            //开机check
            if (equipStatus.equalsIgnoreCase("Equipment Ready")) {
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean checkResult = false;
                Recipe goldRecipe = null;
                Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                //根据检查模式执行开机检查逻辑
                //1、A1-检查recipe名称是否一致
                //2、A-检查recipe名称和参数
                //3、B-检查SV
                //4、AB都检查
                if (startCheckMod != null && !"".equals(startCheckMod)) {
                    checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                    if (!checkResult) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                        //不允许开机
//                        holdDeviceAndShowDetailInfo();
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                        if (goldRecipe == null) {
                            //TODO  这里需要讨论做试产时的情况
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
                        }
                    }
                }
                if (checkResult && "A".equals(startCheckMod)) {
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = checkRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        this.startCheckRecipePara(checkRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                        if (goldRecipe == null) {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                            //不允许开机
//                            this.holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                            this.startCheckRecipePara(goldRecipe);
                        }

                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
                }

//                boolean startPass = false;
//                if (goldRecipe == null) {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                    //不允许开机
//                    this.holdDevice();
//                } else {
//                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                    if (checkRecipe == null) {
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过");
//                        this.startCheckRecipePara(goldRecipe, "abs");
//                    } else {
//                        this.startCheckRecipePara(checkRecipe, "abs");
//                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }

//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        RecipeService recipeService = new RecipeService(sqlSession);
//        //保存到设备操作记录数据库
//        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
//        DeviceOplog deviceOplog = new DeviceOplog();
//        //更新设备模型状态
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
//        if (goldRecipe == null) {
//            //TODO  这里需要讨论做试产时的情况
//           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
//        }
//        try {
//            if (deviceInfoExt == null) {
//                deviceInfoExt = setDeviceInfoExt();
//                deviceService.saveDeviceInfoExt(deviceInfoExt);
//            } else {
//                deviceInfoExt.setDeviceStatus(equipStatus);
//                deviceService.modifyDeviceInfoExt(deviceInfoExt);
//            }
//            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
//                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
//                deviceService.saveDeviceOplog(deviceOplog);
//                //发送设备状态到服务端
////                sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
//                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
//            } else {
//                String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
//                if (!formerDeviceStatus.equals(equipStatus)) {
//                    deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
//                    deviceService.saveDeviceOplog(deviceOplog);
//
//                    //发送设备状态到服务端
////                    sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
//                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
//                    // sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpDesc(),deviceOplog.getOpTime());
//                }
//            }
//            sqlSession.commit();
//            //开机check
//            if (equipStatus.equalsIgnoreCase("Equipment Ready")) {
//                boolean startPass = false;
//            }
//            if (ceid == 1009) {
//                //开机check
//                if (equipStatus.equalsIgnoreCase("Equipment Ready")) {
//                    boolean startPass = false;
//
//                    if (goldRecipe == null) {
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                        //不允许开机
//                        this.holdDevice();
//                    } else {
//                        Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                        if (checkRecipe == null) {
//                           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过");
//                            this.startCheckRecipePara(goldRecipe);
//                        } else {
//                            this.startCheckRecipePara(checkRecipe);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
    }

    private void processS6F11EquipStart(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            findDeviceRecipe();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        //保存到设备操作记录数据库
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
        DeviceOplog deviceOplog = new DeviceOplog();
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            if (deviceInfoExt == null) {
                deviceInfoExt = setDeviceInfoExt();
                deviceService.saveDeviceInfoExt(deviceInfoExt);
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                holdDeviceAndShowDetailInfo();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
            }

            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
                deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态到服务端
//                sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
            } else {
                String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
                if (!formerDeviceStatus.equals(equipStatus)) {
                    deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
                    deviceService.saveDeviceOplog(deviceOplog);

                    //发送设备状态到服务端
//                    sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                    // sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpDesc(),deviceOplog.getOpTime());
                }
            }
            sqlSession.commit();
            //开机check
            String startCheckMod = deviceInfoExt.getStartCheckMod();
            boolean checkResult = false;
            Recipe goldRecipe = null;
            Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
            //根据检查模式执行开机检查逻辑
            //1、A1-检查recipe名称是否一致
            //2、A-检查recipe名称和参数
            //3、B-检查SV
            //4、AB都检查
            if (startCheckMod != null && !"".equals(startCheckMod)) {
                checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
                if (!checkResult) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                    //不允许开机
                    holdDeviceAndShowDetailInfo();
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                    goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                    if (goldRecipe == null) {
                        //TODO  这里需要讨论做试产时的情况
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
                    }
                }
            }
            if (checkResult && "A".equals(startCheckMod)) {
                //首先判断下载的Recipe类型
                //1、如果下载的是Unique版本，那么执行完全比较
                String downloadRcpVersionType = checkRecipe.getVersionType();
                if ("Unique".equals(downloadRcpVersionType)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                    this.startCheckRecipePara(checkRecipe, "abs");
                } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                    if (goldRecipe == null) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                        //不允许开机
                        this.holdDeviceAndShowDetailInfo();
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                        this.startCheckRecipePara(goldRecipe);
                    }

                }
            } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "没有设置开机check");
            }

//                boolean startPass = false;
//                if (goldRecipe == null) {
//                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                    //不允许开机
//                    this.holdDevice();
//                } else {
//                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                    if (checkRecipe == null) {
//                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过");
//                        this.startCheckRecipePara(goldRecipe, "abs");
//                    } else {
//                        this.startCheckRecipePara(checkRecipe, "abs");
//                    }
//                }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }

//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        RecipeService recipeService = new RecipeService(sqlSession);
//
//        DeviceOplog deviceOplog = new DeviceOplog();
//
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
//        if (goldRecipe == null) {
//            //TODO  这里需要讨论做试产时的情况
//            GlobalConstants.eapView.getJTX_EventLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备" + deviceCode + "执行开机检查，清模程序例外。请联系PE处理！\n");
//            DialogUtil.AutoNewLine(GlobalConstants.eapView.getJTX_EventLog());
//        }
//        try {
//            if (deviceInfoExt == null) {
//                deviceInfoExt = setDeviceInfoExt();
//                deviceService.saveDeviceInfoExt(deviceInfoExt);
//            } else {
//                deviceInfoExt.setDeviceStatus(equipStatus);
//                deviceService.modifyDeviceInfoExt(deviceInfoExt);
//            }
//            //保存到设备操作记录数据库
//            List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
//            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
//                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
//                deviceService.saveDeviceOplog(deviceOplog);
//                //发送设备状态变化记录到服务端
////                this.sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
//                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
//            } else {
//                String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
//                if (!formerDeviceStatus.equals(equipStatus)) {
//                    deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, formerDeviceStatus, lotId);
//                    deviceService.saveDeviceOplog(deviceOplog);
//                    //发送设备状态到服务端
////                    sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
//                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
//                    // sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpDesc(),deviceOplog.getOpTime());
//                }
//            }
//            sqlSession.commit();
//            //开机check
//            if (goldRecipe == null) {
//                GlobalConstants.eapView.getJTX_EventLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！\n");
//                DialogUtil.AutoNewLine(GlobalConstants.eapView.getJTX_EventLog());
//                //不允许开机
//                this.holdDevice();
//            } else {
//                Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                if (checkRecipe == null) {
//                    GlobalConstants.eapView.getJTX_EventLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过！\n");
//                    DialogUtil.AutoNewLine(GlobalConstants.eapView.getJTX_EventLog());
//                    this.startCheckRecipePara(goldRecipe);
//                } else {
//                    this.startCheckRecipePara(checkRecipe);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
    }

    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        String ppid = recipeName;
        if (!recipeName.contains(".rcp")) {
            recipeName = recipeName.concat(".rcp");
        }
        if (ppid.contains(".rcp")) {
            ppid = ppid.replace(".rcp","");
        }
        Recipe recipe = setRecipe(ppid);
        recipePath = getRecipePathByConfig(recipe);
        DataMsgMap msgdata = null;
        try {
            msgdata = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] ppbody = (byte[]) msgdata.get("PPBODY");
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
//            recipeParaList = TowaRecipeUtil.transferTowaRcp(TowaRecipeUtil.Y1R_RECIPE_CONFIG, ppbody);
            recipeParaList = LaserRelinkUtil.transferHylaxLaserReinkRcp(recipePath);// TowaRecipeUtil.transferTowaRcpFromDB(deviceType, ppbody);
            for (int i = 0; i < recipeParaList.size(); i++) {
                String paraName = recipeParaList.get(i).getParaName();
                if (paraName.equals("") || paraName.equals("NULL")) {
                    recipeParaList.remove(i);
                    i--;
                }
            }
        } catch (Exception ex) {
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
    // </editor-fold>

    @Override
    public void startCheckRecipePara(Recipe checkRecipe, String type) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        List<RecipePara> equipRecipeParas = null;
        try {
            equipRecipeParas = (List<RecipePara>) GlobalConstants.stage.hostManager.getRecipeParaFromDevice(this.deviceId, checkRecipe.getRecipeName()).get("recipeParaList");
        } catch (UploadRecipeErrorException e) {
            e.printStackTrace();
            return;
        }
        List<RecipePara> recipeParasdiff = this.checkRcpPara(checkRecipe.getId(), deviceCode, equipRecipeParas, type);
        try {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.StartCheckWI");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipeName", ppExecName);
            mqMap.put("EquipStatus", equipStatus);
            mqMap.put("lotId", lotId);
            String eventDesc = "";
            if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
//                this.holdDeviceAndShowDetailInfo();
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机检查未通过!");
                logger.debug("设备：" + deviceCode + " 开机Check失败");
//                RealTimeParaMonitor realTimePara = new RealTimeParaMonitor(null, true, deviceCode, ppExecName, recipeParasdiff, 1);
//                realTimePara.setSize(1000, 650);
//                SwingUtil.setWindowCenter(realTimePara);
//                realTimePara.setVisible(true);
                for (RecipePara recipePara : recipeParasdiff) {
                    eventDesc = "开机Check参数异常参数编码为：" + recipePara.getParaCode() + "，其异常设定值为： " + recipePara.getSetValue() + "，其最小设定值为：" + recipePara.getMinValue() + "，其最大设定值为：" + recipePara.getMaxValue();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                }
                monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
            } else {
                this.releaseDevice();
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 0);
                changeEquipPanel(panelMap);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机Check通过！");
                eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                logger.debug("设备：" + deviceCode + " 开机Check成功");
            }
            mqMap.put("eventDesc", eventDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sqlSession.close();
        }
    }

    public List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas, String masterCompareType) {
        //获取Gold版本的参数(只有gold才有wi信息)
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipePara> goldRecipeParas = recipeService.searchRecipeParaByRcpRowId(recipeRowid);
        //确定管控参数
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateMonitor(deviceCode);
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (RecipePara recipePara : goldRecipeParas) {
                if (recipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                    recipeTemplate.setMinValue(recipePara.getMinValue());
                    recipeTemplate.setMaxValue(recipePara.getMaxValue());
                    recipeTemplate.setSetValue(recipePara.getSetValue());
                }
            }
        }
        //找出设备当前recipe参数中超出wi范围的参数
        List<RecipePara> wirecipeParaDiff = new ArrayList<>();
        for (RecipePara equipRecipePara : equipRecipeParas) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                if (recipeTemplate.getParaCode().equals(equipRecipePara.getParaCode())) {
                    String currentRecipeValue = equipRecipePara.getSetValue();
                    String setValue = recipeTemplate.getSetValue();
                    String minValue = recipeTemplate.getMinValue();
                    String maxValue = recipeTemplate.getMaxValue();
                    boolean paraIsNumber = false;
                    try {
                        Double.parseDouble(currentRecipeValue);
                        paraIsNumber = true;
                    } catch (Exception e) {
                    }
                    try {
                        if ("abs".equals(masterCompareType)) {
                            equipRecipePara.setMinValue(setValue);
                            equipRecipePara.setMaxValue(setValue);
                            if (paraIsNumber) {
                                if (Double.parseDouble(currentRecipeValue) != Double.parseDouble(setValue)) {
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else {
                                if (!currentRecipeValue.equals(setValue)) {
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            }

                        } else {
                            if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                continue;
                            }
                            //spec
                            if ("1".equals(recipeTemplate.getSpecType())) {
                                if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                                //abs
                            } else if ("2".equals(recipeTemplate.getSpecType())) {
                                if (!equipRecipePara.getSetValue().toString().equals(setValue)) {
                                    equipRecipePara.setMinValue(setValue);
                                    equipRecipePara.setMaxValue(setValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        sqlSession.close();
        return wirecipeParaDiff;
    }


    private void initRptPara() {
    }

    //hold机台，先停再锁
    @Override
    public Map holdDevice() {
//            super.sendS2f41Cmd("PAUSE");
//            //????????????????
//            return this.sendS2f41Cmd("PAUSE");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = sendS2f41Cmd("STOP");
            if (cmdMap.get("HCACK").toString().equals("0")) {
                Map panelMap = new HashMap();
                panelMap.put("AlarmState", 2);
                changeEquipPanel(panelMap);
            }
            return cmdMap;
        } else {
            //todo 显示界面锁机日志
//            GlobalConstants.stage.getJTX_EventLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 设备：" + deviceCode + " 未设置锁机！\n");
//            DialogUtil.AutoNewLine(GlobalConstants.eapView.getJTX_EventLog());
            return null;
        }
    }

    //    public Map releaseDevice() {
//        Map map = new HashMap();
//        map.put("HCACK", 0);
//        return map;
//    }
    @Override
    public Map releaseDevice() {
        try {
            sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        Map map = this.sendS2f41Cmd("START");
        if ((byte) map.get("HCACK") == 0) {
            this.setAlarmState(0);
        }
        return map;
    }

    @Override
    public void initRemoteCommand() {
    }
}
