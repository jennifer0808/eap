/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.asm.smt.dek;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.generalDriver.entity.msg.SecsItem;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.util.CommonSMLUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("serial")
public class Horizon03ixHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(Horizon03ixHost.class.getName());
    public Map<Integer, Boolean> pressUseMap = new HashMap<>();

    public Horizon03ixHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat=FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    @Override
    public Object clone() {
        Horizon03ixHost newEquip = new Horizon03ixHost(deviceId,
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
                    Horizon03ixHost.sleep(200);
                }
                if (this.getCommState() != Horizon03ixHost.COMMUNICATING) {
                    this.sendS1F13out();
                    this.sendS2F37outAll();
                    //this.sendS14F1out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    super.findDeviceRecipe();
                }

                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in") || msg.getMsgSfName().equalsIgnoreCase("s5f1ypmin")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")){
                    processS6F11inAll(msg);
                }
                else if (msg.getMsgSfName() != null && msg.getMsgSfName().contains("s6f11incommon")) {
                    long ceid = 0L;
                    try {
                        ceid = msg.getSingleNumber("CollEventID");
                        if (ceid == 90001 || ceid == 90002 || ceid == 90003 || ceid == 90004 || ceid == 90005 || ceid == 90006 || ceid == 90007) {
                            processS6F11EquipStatusChange(msg);
                        } else if (ceid == 31450 || ceid == 31451 || ceid == 31452 || ceid == 31265 || ceid == 31456 || ceid == 31267) {
                            super.findDeviceRecipe();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
            } else if (tagName.equalsIgnoreCase("s2f17in")) {
                processS2F17in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                //回复s6f11消息
                replyS6F12WithACK(data, (byte) 0);
                processS6F11inAll(data);
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
                this.inputMsgQueue.put(data);
                replyS5F2Directly(data);
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

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {

        long ceid = 0L;
        try {
            ceid = (long)data.get("CEID");
            findDeviceRecipe();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (controlState.equalsIgnoreCase("OnlineLocal")) {
//            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
//        } else if (controlState.equalsIgnoreCase("OnlineRemote")) {
//            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
//        } else if (controlState.equalsIgnoreCase("EquipmentOffline")) {
//            controlState = FengCeConstant.CONTROL_OFFLINE;
//        }
//        Map map = new HashMap();
//        map.put("PPExecName", ppExecName);
//        map.put("EquipStatus", equipStatus);
//        map.put("ControlState", controlState);
//        changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            if (deviceInfoExt == null) {
                deviceInfoExt = setDeviceInfoExt();
                deviceService.saveDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
                sqlSession.commit();
            }
            //保存设备操作记录至本地数据库并发送给sever
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
            sqlSession.commit();
            if (AxisUtility.isEngineerMode(deviceCode)) {
                UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
                sqlSession.close();
                return;
            }
            //开机check
            //1.查看服务端设备是否被锁
            if (equipStatus.equalsIgnoreCase("RUN")) {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "设备已被锁");
                    //此时所正式锁机
                    holdDeviceAndShowDetailInfo("Host hold the equipment,you can see the detail log from Host");
                }
            }
            //2.比对recipe是否正确
            //根据检查模式执行开机检查逻辑
            //1、A-检查recipe名称和参数
            //2、B-检查SV
            if (equipStatus.equalsIgnoreCase("READY")) {
                String startCheckMod = deviceInfoExt.getStartCheckMod();
                boolean checkRecipeResult = false;
                Recipe goldRecipe = null;
                Recipe currentLocalRecipe = null;//下载切换更新到本地数据库的recipe
                if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
                    holdDeviceAndShowDetailInfo();
                    UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
                } else {
                    currentLocalRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    checkRecipeResult = checkRecipeName(deviceInfoExt.getRecipeName());
                    if (!checkRecipeResult) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
                        //不允许开机
                        holdDeviceAndShowDetailInfo();
                    } else {
                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
                        goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
                        if (goldRecipe == null) {
                            //TODO  这里需要讨论做试产时的情况
                            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
                        }
                    }
                }
                //2.recipe check过了，在比对参数
                if (checkRecipeResult && "A".equals(startCheckMod)) {
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = currentLocalRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        this.startCheckRecipePara(currentLocalRecipe, "abs");
                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
                        if (goldRecipe == null) {
                            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
                            //不允许开机
                            this.holdDeviceAndShowDetailInfo();
                        } else {
                            UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
                            this.startCheckRecipePara(goldRecipe);
                        }

                    }
                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
                    UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
                }

//                boolean startPass = false;
//                if (goldRecipe == null) {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                    //不允许开机
//                    this.holdDevice();
//                } else {
//                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                    if (checkRecipe == null) {
//                        UiLogUtil.appendLog2EventTab(deviceCode, "模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过");
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
    }

//    @Override
//    protected void processS6F11EquipStatusChange(DataMsgMap data) {
//        //回复s6f11消息
//        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
//        byte[] ack = new byte[1];
//        ack[0] = 0;
//        out.put("AckCode", ack);
//        long ceid = 0l;
//        try {
//            out.setTransactionId(data.getTransactionId());
//            activeWrapper.respondMessage(out);
//            ceid = data.getSingleNumber("CollEventID");
//            this.sendS1F3Check();
////            equipStatus = newMap.get("EquipStatus").toString();
////            ppExecName = newMap.get("PPExecName").toString();
////            controlState = newMap.get("ControlState").toString();
////            equipStatus = ACKDescription.descriptionStatus(data.getSingleNumber("EquipStatus"), deviceType);
////            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
////            controlState = ACKDescription.describeControlState(data.getSingleNumber("ControlState"), deviceType);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
////        if (controlState.equalsIgnoreCase("OnlineLocal")) {
////            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
////        } else if (controlState.equalsIgnoreCase("OnlineRemote")) {
////            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
////        } else if (controlState.equalsIgnoreCase("EquipmentOffline")) {
////            controlState = FengCeConstant.CONTROL_OFFLINE;
////        }
////        Map map = new HashMap();
////        map.put("PPExecName", ppExecName);
////        map.put("EquipStatus", equipStatus);
////        map.put("ControlState", controlState);
////        changeEquipPanel(map);
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        RecipeService recipeService = new RecipeService(sqlSession);
//        //保存到设备操作记录数据库
//        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
//        DeviceOplog deviceOplog = new DeviceOplog();
//        //更新设备模型状态
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        try {
//            if (deviceInfoExt == null) {
//                deviceInfoExt = setDeviceInfoExt();
//                deviceService.saveDeviceInfoExt(deviceInfoExt);
//            } else {
//                deviceInfoExt.setDeviceStatus(equipStatus);
//                deviceService.modifyDeviceInfoExt(deviceInfoExt);
//            }
//            if (deviceInfoExt.getRecipeId() == null || "".equals(deviceInfoExt.getRecipeId())) {
//                holdDeviceAndShowDetailInfo();
//                UiLogUtil.appendLog2EventTab(deviceCode, "Trackin数据不完整，未设置当前机台应该执行的Recipe，不能运行，设备已被锁!");
//            }
//
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
//            if (equipStatus.equalsIgnoreCase("READY")) {
//                String startCheckMod = deviceInfoExt.getStartCheckMod();
//                boolean checkResult = false;
//                Recipe goldRecipe = null;
//                Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                //根据检查模式执行开机检查逻辑
//                //1、A1-检查recipe名称是否一致
//                //2、A-检查recipe名称和参数
//                //3、B-检查SV
//                //4、AB都检查
//                if (startCheckMod != null && !"".equals(startCheckMod)) {
//                    checkResult = checkRecipeName(deviceInfoExt.getRecipeName());
//                    if (!checkResult) {
//                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
//                        //不允许开机
//                        holdDeviceAndShowDetailInfo();
//                    } else {
//                        UiLogUtil.appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
//                        goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
//                        if (goldRecipe == null) {
//                            //TODO  这里需要讨论做试产时的情况
//                            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
//                        }
//                    }
//                }
//                if (checkResult && "A".equals(startCheckMod)) {
//                    //首先判断下载的Recipe类型
//                    //1、如果下载的是Unique版本，那么执行完全比较
//                    String downloadRcpVersionType = checkRecipe.getVersionType();
//                    if ("Unique".equals(downloadRcpVersionType)) {
//                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
//                        this.startCheckRecipePara(checkRecipe, "abs");
//                    } else {//2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
//                        UiLogUtil.appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数WICheck");
//                        if (goldRecipe == null) {
//                            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                            //不允许开机
//                            this.holdDeviceAndShowDetailInfo();
//                        } else {
//                            UiLogUtil.appendLog2EventTab(deviceCode, ppExecName + "开始WI参数Check");
//                            this.startCheckRecipePara(goldRecipe);
//                        }
//
//                    }
//                } else if (deviceInfoExt.getStartCheckMod() == null || "".equals(deviceInfoExt.getStartCheckMod())) {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "没有设置开机check");
//                }
//
////                boolean startPass = false;
////                if (goldRecipe == null) {
////                    UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
////                    //不允许开机
////                    this.holdDevice();
////                } else {
////                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
////                    if (checkRecipe == null) {
////                        UiLogUtil.appendLog2EventTab(deviceCode, "模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过");
////                        this.startCheckRecipePara(goldRecipe, "abs");
////                    } else {
////                        this.startCheckRecipePara(checkRecipe, "abs");
////                    }
////                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
//    }

    //    protected void processS6F11EquipStatusChange(DataMsgMap data) throws MalformedURLException {
//        //回复s6f11消息
//        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
//        byte[] ack = new byte[1];
//        ack[0] = 0;
//        out.put("AckCode", ack);
//        long ceid = 0l;
//        try {
//            out.setTransactionId(data.getTransactionId());
//            activeWrapper.respondMessage(out);
//            ceid = data.getSingleNumber("CollEventID");
//            Map newMap = this.sendS1F3Check();
//            equipStatus = newMap.get("EquipStatus").toString();
//            ppExecName = newMap.get("PPExecName").toString();
//            controlState = newMap.get("ControlState").toString();
//
////            equipStatus = ACKDescription.descriptionStatus(data.getSingleNumber("EquipStatus"), deviceType);
////            ppExecName = ((SecsItem) data.get("PPExecName")).getData().toString();
////            controlState = ACKDescription.describeControlState(data.getSingleNumber("ControlState"), deviceType);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
////        if (controlState.equalsIgnoreCase("OnlineLocal")) {
////            controlState = FengCeConstant.CONTROL_LOCAL_ONLINE;
////        } else if (controlState.equalsIgnoreCase("OnlineRemote")) {
////            controlState = FengCeConstant.CONTROL_REMOTE_ONLINE;
////        } else if (controlState.equalsIgnoreCase("EquipmentOffline")) {
////            controlState = FengCeConstant.CONTROL_OFFLINE;
////        }
//
////        Map map = new HashMap();
////        map.put("PPExecName", ppExecName);
////        map.put("EquipStatus", equipStatus);
////        map.put("ControlState", controlState);
////        changeEquipPanel(map);
//
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
//            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，将无法对设备执行开机检查，清模程序例外。请联系PE处理！");
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
//            if (equipStatus.equalsIgnoreCase("Ready")) {
//                boolean startPass = false;
//                if (goldRecipe == null) {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
//                    //不允许开机
//                    this.holdDevice();
//                } else {
//                    Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
//                    if (checkRecipe == null) {
//                        UiLogUtil.appendLog2EventTab(deviceCode, "模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过");
//                        this.startCheckRecipePara(goldRecipe);
//                    } else {
//                        this.startCheckRecipePara(checkRecipe);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            sqlSession.rollback();
//        } finally {
//            sqlSession.close();
//        }
//    }
    private void processS6F11EquipStart(DataMsgMap data) {
        //回复s6f11消息
        DataMsgMap out = new DataMsgMap("s6f12out", activeWrapper.getDeviceId());
        byte[] ack = new byte[1];
        ack[0] = 0;
        out.put("AckCode", ack);
        long ceid = 0L;
        try {
            out.setTransactionId(data.getTransactionId());
            activeWrapper.respondMessage(out);
            ceid = data.getSingleNumber("CollEventID");
            Map newMap = this.sendS1F3Check();
            equipStatus = newMap.get("EquipStatus").toString();
            ppExecName = newMap.get("PPExecName").toString();
            controlState = newMap.get("ControlState").toString();

//            equipStatus = ACKDescription.descriptionStatus(data.getSingleNumber("EquipStatus"), deviceType);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);

        DeviceOplog deviceOplog = new DeviceOplog();
        //更新设备模型状态
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        Recipe goldRecipe = recipeService.getGoldRecipe(ppExecName, deviceCode, deviceType);
        if (goldRecipe == null) {

        }
        try {
            if (deviceInfoExt == null) {
                deviceInfoExt = setDeviceInfoExt();
                deviceService.saveDeviceInfoExt(deviceInfoExt);
            } else {
                deviceInfoExt.setDeviceStatus(equipStatus);
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            //保存到设备操作记录数据库
            List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceCode);
            if (deviceOplogList == null || deviceOplogList.isEmpty()) {
                deviceOplog = setDeviceOplog(ceid, ppExecName, equipStatus, "", lotId);
                deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态变化记录到服务端
//                this.sendEqptStatus2Server(ppExecName, deviceCode, lotId, ceid, equipStatus, deviceOplog.getOpType(), deviceOplog.getOpDesc(), deviceOplog.getOpTime().toString());
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
            if (goldRecipe == null) {
//                GlobalConstants.eapView.getJTX_EventLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 工控上不存在： " + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！\n");
//                DialogUtil.AutoNewLine(GlobalConstants.eapView.getJTX_EventLog());
                UiLogUtil.appendLog2EventTab(deviceCode, "");
                //不允许开机
                this.holdDevice();
            } else {
                Recipe checkRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                if (checkRecipe == null) {
//                    GlobalConstants.eapView.getJTX_EventLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 模型表中没有记录Recipe:" + ppExecName + " 需要TrackIn，服务端审核通过！\n");

                    UiLogUtil.appendLog2EventTab(deviceCode, "");
                    this.startCheckRecipePara(goldRecipe);
                } else {
                    this.startCheckRecipePara(checkRecipe);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }

    private void processS6F11inAll(DataMsgMap msg) throws InterruptedException {
        long ceid = 0L;
        ceid = (long) msg.get("CEID");
        if (ceid == 90001 || ceid == 90002 || ceid == 90003 || ceid == 90004 || ceid == 90005 || ceid == 90006 || ceid == 90007) {
            if (ceid == 90001) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备重置中...");
            } else if (ceid == 90002) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备设置中...");
            } else if (ceid == 90003) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备进入Ready状态.");
            } else if (ceid == 90004) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备维修中...");
            } else if (ceid == 90005) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备暂停...");
            } else if (ceid == 90006) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备开机！");
            } else if (ceid == 90007) {
                UiLogUtil.appendLog2EventTab(deviceCode, "设备进入Down状态.");
            }
            processS6F11EquipStatusChange(msg);
        } else if (ceid == 31450 || ceid == 31451 || ceid == 31452 || ceid == 31265 || ceid == 31456 || ceid == 31267) {
            if (ceid == 31265) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Process program loaded.");
            } else if (ceid == 31267) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Process program change.");
            } else if (ceid == 31450) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Control State – Change to LOCAL.");
            } else if (ceid == 31451) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Control State – Change to OFF-LINE.");
            } else if (ceid == 31452) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Control State – Change to REMOTE.");
            } else if (ceid == 31456) {
                UiLogUtil.appendLog2EventTab(deviceCode, "Operator command issued.");
            }
            //刷新状态
            findDeviceRecipe();
        }

    }


    // <editor-fold defaultstate="collapsed" desc="S7FX Code">
    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        DataMsgMap msgData = null;
        try {
            msgData = activeWrapper.sendS7F5out(recipeName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (msgData == null || msgData.isEmpty()) {
            UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备参数信息失败，请检查设备状态！");
            logger.error("获取设备:" + deviceCode + "参数信息失败.");
            return null;
        }
        byte[] ppbody = (byte[]) msgData.get("PPBODY");
        TransferUtil.setPPBody(ppbody, 1, recipePath);
        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);
        //Recipe解析
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = this.sendS14F1out();
//            recipeParaList = TowaRecipeUtil.transferTowaRcp(TowaRecipeUtil.Y1R_RECIPE_CONFIG, ppbody);
//            recipeParaList = TPRecipeUtil.tPRecipeTran(recipePath);// TowaRecipeUtil.transferTowaRcpFromDB(deviceType, ppbody);
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
            UiLogUtil.appendLog2EventTab(deviceCode, "");

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
    public void processS2F17in(DataMsgMap msg) {
//        throw new UnsupportedOperationException("Not yet implemented");
        try {
            DataMsgMap s2f18out = new DataMsgMap("s2f18out", activeWrapper.getDeviceId());
            String time = "";
            time = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
            s2f18out.put("TIME", time);
            long transactionId = msg.getTransactionId();
            s2f18out.setTransactionId(transactionId);
            activeWrapper.respondMessage(s2f18out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    @SuppressWarnings("unchecked")
//    public void sendS1F13out() {
//        DataMsgMap s1f13out = new DataMsgMap("s1f13out", activeWrapper.getDeviceId());
//        long transactionId = activeWrapper.getNextAvailableTransactionId();
//        s1f13out.setTransactionId(transactionId);
//        s1f13out.put("Mdln", "DR3003");
//        s1f13out.put("SoftRev", "GA2013");
//        try {
//            DataMsgMap data = activeWrapper.sendAwaitMessage(s1f13out);
//            if (data != null) {
//                setCommState(1);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    @SuppressWarnings("unchecked")
    public List<RecipePara> sendS14F1out() {
        List<RecipePara> recipeParas = new ArrayList<>();
        DataMsgMap s14f1out = new DataMsgMap("s14f1out", activeWrapper.getDeviceId());
        s14f1out.setTransactionId(activeWrapper.getNextAvailableTransactionId());
        try {
            DataMsgMap s14f2in = activeWrapper.sendAwaitMessage(s14f1out);
            if (s14f2in == null || s14f2in.isEmpty()) {
                UiLogUtil.appendLog2SecsTab(deviceCode, "获取设备状态信息失败，请检查设备通讯状态！");
                logger.error("获取设备：" + deviceCode + "状态信息失败.");
                return null;
            }
            ArrayList<SecsItem> list = (ArrayList) ((SecsItem) s14f2in.get("RESULT")).getData();
            ArrayList<Object> listtem = TransferUtil.getIDValue(CommonSMLUtil.getECSVData(list));
            int parameterCount = (listtem.size() - 1) / 5;
            for (int n = 0; n < parameterCount; n++) {
                String parameterID = listtem.get(5 * n + 2).toString();
                String parameterData = listtem.get(5 * n + 4).toString();
                System.out.println(parameterID + ":" + parameterData);
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(Integer.toString(n + 1));
                recipePara.setParaName(parameterID);
                recipePara.setSetValue(parameterData);
                recipeParas.add(recipePara);
            }
//            for (int n = 3; n < 11; n++) {
//                String Key = "DATA" + Integer.toString(n + 1);
//                String Value = "VALUE" + Integer.toString(n + 1);
//                String ParameterID = ((SecsItem) s14f2in.get(Key)).getData().toString();
//                String ParameterData = ((SecsItem) s14f2in.get(Value)).getData().toString();
//                System.out.println(ParameterID + ":" + ParameterData);
//                recipePara.setParaCode(Integer.toString(n + 1));
//                recipePara.setParaName(ParameterID);
//                recipePara.setSetValue(ParameterData);
//                recipeParas.add(recipePara);
//            }
            return recipeParas;
//            return null;
//            Mdln = (String) ((SecsItem) s1f2in.get("Mdln")).getData();
//            SoftRev = (String) ((SecsItem) s1f2in.get("SoftRev")).getData();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
