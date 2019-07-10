/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;


import cn.tzauto.generalDriver.exceptions.BrokenProtocolException;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsConnection;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.util.DeviceComm;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author rain
 */
public class CommCheckTask implements Job {

    private static final Logger logger = Logger.getLogger(CommCheckTask.class);
    private static String testType = "";
    static MultipleEquipHostManager hostsManager = GlobalConstants.stage.hostManager;

    @Override
    public void execute(JobExecutionContext jec) {
        logger.info("CommCheckTask=====>CommCheckTask任务执行....");
        EquipHost currentHost = null;
        int checkTimes = 4;
//        int restartCnt = 0;//检测到正在重启的次数，如果超过4次，也直接开始重启通信
        String settingTimes = GlobalConstants.getProperty("RECOMM_BEFORE_CHECK_TIMES") != null ? GlobalConstants.getProperty("RECOMM_BEFORE_CHECK_TIMES") : "";
        if (settingTimes != null && !"".equals(settingTimes) && StringUtils.isNumeric(settingTimes)) {
            checkTimes = Integer.parseInt(settingTimes);
        } else {
            logger.info("未正确配置通信检测次数，使用默认值");
        }
        // Todo 扫描所有Host线程，如果超过1分钟未通信，发送S1F1信号
        String temp = (String) MDC.get(FengCeConstant.WHICH_EQUIPHOST_CONTEXT);
        for (int i = 0; i < GlobalConstants.stage.equipBeans.size(); i++) {
            MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, GlobalConstants.stage.equipBeans.get(i).getDeviceCode());
            boolean isSecsException = false;
            // EAPGuiView.removeWatchDog(Integer.valueOf(list.get(i + 1)));                  
            String deviceId = GlobalConstants.stage.equipBeans.get(i).getDeviceCode();
            //EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceId);
            if ( GlobalConstants.stage.equipHosts.get(deviceId) == null) {
                //  EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceId);
                easyCheck( GlobalConstants.stage.equipModels.get(deviceId));
                continue;
            }
            currentHost = hostsManager.getAllEquipHosts().get(deviceId);
            if (currentHost.getDeviceType().contains("NITTO")) {
                continue;
            }
            if (currentHost.getDeviceType().contains("8800Sigma")) {
                continue;
            }
            EquipNodeBean src = null;
            for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
                if (equipNodeBean.getDeviceIdProperty().equals( GlobalConstants.stage.equipHosts.get(deviceId).getDeviceId())) {
                    src = equipNodeBean;
                    break;
                }
            }
            logger.info("设备重启状态:" + currentHost.isIsRestarting());
            if (currentHost.isIsRestarting()) {
                logger.info("设备:" + currentHost.getDeviceCode() + "正在重启,跳过检查,restartCnt=" + currentHost.restartCnt);
                currentHost.restartCnt++;
                if (currentHost.restartCnt <= checkTimes) {
                    continue;
                }
                currentHost.restartCnt = 0;
                try {
                    checkNetAndDealer(deviceId, src);
                } catch (Exception e) {
                    logger.debug("checkNetAndDealer(" + deviceId + ");" + src);
                }
            }
            //start the Host Thread
            logger.info("CommCheckTask======>DeviceID:" + deviceId + ";线程中断状态:" + currentHost.isInterrupted() + ";通信状态:" + currentHost.getCommState() + ";程序状态:" + currentHost.isSdrReady());
            logger.info("----------------------------" + currentHost.getClass().getName() + ": " + currentHost.hashCode());
            long lastConDate = hostsManager.getAllEquipHosts().get(deviceId).getLastComDate();
            long diff = new Date().getTime() - lastConDate;
            logger.info("获取最后一次通信时间========>" + lastConDate);
            String testResult = "";
            if (currentHost != null) {
//                if (currentHost.isSdrReady()) {
//                    currentHost.checkNotReady = 0;
                if (lastConDate == 0) {
                    logger.info("检测到上一次通信时间为0========" + currentHost.checkNotComm);
                    //判断是否通信状态，如果没有通信超过一定次数，那么重新启动通信
                    if (currentHost.getCommState() != 1) {
                        currentHost.checkNotComm++;
                        logger.info("检测到初始化未通信、====checkNotComm======" + currentHost.checkNotComm);
                        //如果网络连接正常，通信异常情况下，重新启动连接
                        if (currentHost.checkNotComm >= checkTimes) {
                           UiLogUtil.getInstance().appendLog2EventTab(currentHost.getDeviceCode(), " Not comm 次数超过" + checkTimes + "次");
                            setPanelCommFail( GlobalConstants.stage.equipHosts.get(deviceId));
                            currentHost.checkNotComm = 0;
                            checkNetAndDealer(deviceId, src);
                            continue;
                        }
                    } else {
                        currentHost.checkNotComm = 0;
                    }
                    logger.info("当前设备:" + deviceId + ",已经Ready,未建立SECS连接，开始初始化连接");
                    testType = "1";
                    testResult = comTestFunction(testType, currentHost);
                    logger.info("当前设备:" + deviceId + ",通信连接结果==>" + testResult);
                    if (currentHost != null && "0".equals(testResult)) {
                        currentHost.setLastComDate(new Date().getTime());
                        currentHost.secsMsgTimeoutTime = 0;
                    }
                    if ("3".equals(testResult) && !currentHost.isIsRestarting()) {
                        resetFlagAndRestart( GlobalConstants.stage.equipHosts.get(deviceId), src);
                    }
                } else {
                    //如果有通信，那么检测上次通信的时间
                    logger.info("==lastConDate===" + lastConDate + "==diff===" + diff + "==checkNotComm===" + currentHost.checkNotComm + "==checkNotReady======" + currentHost.checkNotReady);
                    //如果超过60秒没有通信，那么发送Are u there命令
                    if (lastConDate != 0 && diff > 60000) {
                        logger.info("CommCheckTask=====>发送Are u there 2 eqp，等待回复，DeviceID:" + deviceId + ";线程中断状态:" + currentHost.isInterrupted() + ";通信状态:" + currentHost.getCommState() + ";程序状态:" + currentHost.isSdrReady());
                        testType = "1";
                        testResult = comTestFunction(testType, currentHost);
                        //保存第一次的are u there 结果
                        String testRUThereResult = testResult;
                        logger.info("DeviceID:" + deviceId + ";testRUThereResult:" + testRUThereResult);

                        //0:回复正常；1:有回复但是不是正确回复;2:未回复
                        if (!"0".equals(testResult)) {
                            if ("3".equals(testResult)) {//secs通信异常
                                logger.info("Secs Exception occur");
                                resetFlagAndRestart( GlobalConstants.stage.equipHosts.get(deviceId), src);
                            } else {
                                testType = "2";
                                //发送S1F13指令，建立连接
                                logger.info("DeviceID:" + deviceId + ";由于设备没有正确回复S1F1，开始InitLink");
                                testResult = comTestFunction(testType, currentHost);
                                if (!"0".equals(testResult)) {
                                    logger.info("DeviceID:" + deviceId + "===========InitLinkResult:" + testResult);
                                    resetFlagAndRestart( GlobalConstants.stage.equipHosts.get(deviceId), src);
//                                    currentHost.secsMsgTimeoutTime++;
//                                    msgTimeoutCountAndRestart(deviceId, src);
                                } else {//设备正常回复S1F14
                                    logger.info("重新建立连接正常，再次发送S1F1，Are u there:");
                                    if ("1".equals(testRUThereResult)) {//如果当时没有正确回复S1F1,再发送一次
                                        testType = "1";
                                        testResult = comTestFunction(testType, currentHost);
                                        logger.info("DeviceID:" + deviceId + ";Recall are u there result:" + testResult);
                                        if (!"0".equals(testResult) && !currentHost.isIsRestarting()) {
                                            logger.info("DeviceID:" + deviceId + "再次Are u there异常，:" + testResult);
                                            resetFlagAndRestart( GlobalConstants.stage.equipHosts.get(deviceId), src);
//                                            currentHost.secsMsgTimeoutTime++;
//                                            msgTimeoutCountAndRestart(deviceId, src);
                                        }
                                    }
                                }
                            }

                        } else {
                            currentHost.secsMsgTimeoutTime = 0;
                            currentHost.setLastComDate(new Date().getTime());
                            currentHost.checkNotComm = 0;
                            currentHost.checkNotReady = 0;
                            logger.info("CommCheckTask=====>发送Are u there正常、连接正常，DeviceID:" + deviceId + ";线程中断状态:" + currentHost.isInterrupted() + ";通信状态:" + currentHost.getCommState() + ";程序状态:" + currentHost.isSdrReady());
                        }
                    }
                }

            }
        }
        if(temp ==null){
            MDC.remove(FengCeConstant.WHICH_EQUIPHOST_CONTEXT);
        }else{
            MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, temp);
        }
    }

    private void checkNetAndDealer(String deviceId, EquipNodeBean src) {
        if (checkNet( GlobalConstants.stage.equipHosts.get(deviceId).iPAddress) < 3) {
            doWhenNetRight(deviceId, src);
        } else {
            netBrokenDealer(deviceId);
        }
    }

    /**
     * 网络正常情况下，通信异常，那么直接重启
     *
     * @param deviceId
     * @param src
     */
    private void doWhenNetRight(String deviceId, EquipNodeBean src) {
        if (! GlobalConstants.stage.equipHosts.get(deviceId).isIsRestarting()) {
            GlobalConstants.stage.equipHosts.get(deviceId).checkNotReady = 0;
           UiLogUtil.getInstance().appendLog2EventTab( GlobalConstants.stage.equipHosts.get(deviceId).getDeviceCode(), "工控机与设备网络连接正常，开始重启SECS连接...");
            //自动重连发送日志给服务端
            GlobalConstants.sendStartLog2Server( GlobalConstants.stage.equipHosts.get(deviceId).getDeviceCode());
            resetFlagAndRestart( GlobalConstants.stage.equipHosts.get(deviceId), src);
        }
        if (GlobalConstants.hadHoldLotFlagMap.containsKey(deviceId)) {
            GlobalConstants.hadHoldLotFlagMap.remove(deviceId);
        }
    }

    /**
     * 断网处理措施
     *
     * @param deviceId
     */
    private void netBrokenDealer(String deviceId) {
        String deviceCode = "";
        if ( GlobalConstants.stage.equipHosts.get(deviceId) != null) {
            deviceCode = GlobalConstants.stage.equipHosts.get(deviceId).getDeviceCode();
        } else {
            deviceCode = deviceId;
        }
       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控机与设备网络连接异常，等待网络恢复后重启通讯连接");
        //锁批次
        if (!GlobalConstants.hadHoldLotFlagMap.containsKey(deviceId)) {
            String holdLotFlag = GlobalConstants.getProperty("NET_BREAK_HOLD_LOT");
            if (holdLotFlag != null && !"".equals(holdLotFlag) && "1".equals(holdLotFlag)) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "DeviceCode:" + deviceCode + "检测到网络中断，需要HoldLot");
                logger.info("DeviceCode:" + deviceCode + "已配置HoldLot标志，开始HoldLot...");
                holdLot(deviceId);
            }
            GlobalConstants.hadHoldLotFlagMap.put(deviceId, Boolean.TRUE);
        }
    }

    /**
     * 更新界面为通信失败
     */
    private void setPanelCommFail(EquipHost equipHost) {
        equipHost.setCommState(0);
        Map map = new HashMap();
        map.put("NetState", 0);//0为断网
        equipHost.changeEquipPanel(map);
    }

    /**
     * 设置界面，并重启
     *
     * @param equipNodeBean
     */
    private void resetFlagAndRestart(EquipHost equipHost, EquipNodeBean equipNodeBean) {
        equipHost.sendStatus2Server("OFFLINE");
        logger.info("DeviceCode:" + equipHost.getDeviceCode() + ";开始修改界面状态");
        setPanelCommFail(equipHost);
        logger.info("DeviceCode:" + equipHost.getDeviceCode() + ";即将开始重启通信线程");
        if (!equipHost.isIsRestarting()) {
            hostsManager.getAllEquipHosts().get(equipHost.getDeviceCode()).setIsRestarting(true);
            equipHost.setIsRestarting(true);
            DeviceComm.restartHost(equipNodeBean);
            logger.info("DeviceCode:" + equipHost.getDeviceCode() + ";置空当前Host");
        } else {
            logger.info("DeviceCode:" + equipHost.getDeviceCode() + ";当前正在重启中，无需重复重启");
        }

    }

    /**
     * 在网络和通信正常情况下，统计消息异常次数，超过次数，开始重启
     *
     * @param deviceId
     * @param equipNodeBean
     */
    private void msgTimeoutCountAndRestart(String deviceId, EquipNodeBean equipNodeBean) {
        EquipHost currentHost= GlobalConstants.stage.equipHosts.get(deviceId);
        logger.info("CommCheckTask=====>currentHost.secsMsgTimeoutTime:" + currentHost.secsMsgTimeoutTime + " DeviceID:" + deviceId);
        if (currentHost.secsMsgTimeoutTime == 3) {
            logger.info("CommCheckTask=====>检测到中断，secsMsgTimeoutTime=" + currentHost.secsMsgTimeoutTime + "修改状态，准备重启，DeviceID:" + deviceId + ";线程中断状态:" + currentHost.isInterrupted() + ";通信状态:" + currentHost.getCommState() + ";程序状态:" + currentHost.isSdrReady());
            logger.info("DeviceID:" + deviceId + ";通信状态:" + currentHost.getCommState() + ";程序状态:" + currentHost.isSdrReady());
            resetFlagAndRestart(currentHost, equipNodeBean);
            logger.info("DeviceID:" + deviceId + ";重置统计超时标记secsMsgTimeoutTime");
            currentHost.secsMsgTimeoutTime = 0;
        }
    }

    /**
     *
     * @param type
     * @return testResult 0:正常;1:收到回复、是取消会话；2：异常；3：SECS通信异常
     */
    private String comTestFunction(String type, EquipHost equipHost) {
        final EquipHost currentHost = equipHost;
        final String testType = type;
        String result = "";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<String> future = new FutureTask<>(
                new Callable<String>() {

            public String call() {
                boolean initLinkResult = false;
                String ruThereResult = "0";
                try {
                    if (currentHost != null) {
                        //测试S1F1， Are you there
                        if ("1".equals(testType)) {
                            ruThereResult = currentHost.testRUThere();
                            currentHost.setLastComDate(new Date().getTime());
                            currentHost.secsMsgTimeoutTime = 0;
                            return ruThereResult;
                        } else {
                            //测试S1F13， 初始化连接
                            initLinkResult = currentHost.testInitLink();
                            logger.info("InitLinkResult:" + initLinkResult);
                            currentHost.setLastComDate(new Date().getTime());
                            currentHost.secsMsgTimeoutTime = 0;
                            if (initLinkResult) {
                                //设置设备通信状态
                                logger.info("设置设备通信状态:" + initLinkResult);
                                currentHost.setCommState(1);
                                return "0";
                            } else {
                                return "1";
                            }
                        }
                    } else {
                        //如果currentHost销毁了就返回错误
                        return "2";
                    }

                } catch ( BrokenProtocolException e) {
                    logger.error("Secs Exception:", e);
                    return "3";
                } catch (Exception e) {
                    logger.error("Exception:", e);
                    return "2";
                }
            }
        });
        try {
            executor.execute(future);
            result = future.get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            future.cancel(true);
            result = "2";
            logger.error("exec cancel InterruptedException:", e);
        } catch (ExecutionException e) {
            future.cancel(true);
            result = "2";
            logger.error("exec cancel ExecutionException:", e);
        } catch (TimeoutException e) {
            future.cancel(true);
            result = "2";
            logger.error("exec cancel TimeoutException:", e);
        } finally {
            executor.shutdown();
            logger.info("CommTestType: " + testType + ", exec shutdown");
            return result;
        }
    }

    //根据Ping结果返回失败次数
    public int checkNet(String remoteIp) {
        try {
            Runtime runtime = Runtime.getRuntime();
            String tracertString = "ping " + remoteIp;
            Process process = runtime.exec(tracertString);
            process.waitFor();
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("GBK"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String len = bufferedReader.readLine();
            int failCnt = 0;
            while (len != null) {
                if (len.equals("")) {
                    len = bufferedReader.readLine();
                } else {
                    if ((len.contains("请求超时")) || (len.contains("无法访问目标主机")) || (len.contains("一般故障"))) {
                        logger.info(len);
                        failCnt++;
                    }
                    len = bufferedReader.readLine();
                }
            }
            process.destroy();
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            return failCnt;
        } catch (Exception e) {
            logger.error("Exception:", e);
            return 4;
        }
    }

    public String holdLot(String deviceId) {
        EquipHost equipHost = GlobalConstants.stage.hostManager.getAllEquipHosts().get(deviceId);
        String userId = "CIM";
        String deviceCode = equipHost.getDeviceCode();
        String holdCode = "CIMOFFLINE";
        String lotId = "";
        String reason = "与设备网络连接中断";
        String holdResult = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null) {
            lotId = deviceInfoExt.getLotId();
        }
        if (lotId != null && !"".equals(lotId)) {
            if (!GlobalConstants.holdLotMap.containsKey(lotId)) {
               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "开始HoldLot...Lot:" + lotId);
                holdResult = AxisUtility.holdLotByMES(userId, deviceCode, lotId, holdCode, reason);
                GlobalConstants.holdLotMap.put(lotId, true);
               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "HoldLot结束...Lot:" + lotId + "，结果为:" + holdResult);
                if ("OK".equalsIgnoreCase(holdResult)) {
                    //邮件通知
                    List<String> toList = new ArrayList();
                    toList.add("");
                    Map paraMap = new HashMap();
                    paraMap.put("lotId", lotId);
                    String subject = "CIM系统通知：批次锁定";
                    String ftl = "holdLot.ftl";
                    AxisUtility.sendFtlMail(toList, subject, ftl, paraMap);
                }
            }
        }
        return holdResult;
    }

//    private void easyCheck(EquipModel equipModel) {
//        Map map = new HashMap();
//        boolean needInit = false;
//        logger.info("Check equipModel:" + equipModel.deviceCode);
//        for (ISecsHost iSecsHost : equipModel.iSecsHostList) {
//            if (iSecsHost.iSecsConnection == null
//                    || iSecsHost.iSecsConnection.getSocketClient() == null
//                    || !iSecsHost.iSecsConnection.getSocketClient().isConnected()
//                    || !iSecsHost.iSecsConnection.checkConenctionStatus()) {
//                if (iSecsHost.iSecsConnection.getSocketClient() == null) {
//                    logger.info(equipModel.deviceCode + "Check equipModel:getSocketClient() == null");
//                    needInit = true;
//                    if (checkNet(iSecsHost.ip) >= 3) {
//                        netBrokenDealer(equipModel.deviceCode);
//                        needInit = false;
//                        map.put("CommState", 0);
//                        equipModel.changeEquipPanel(map);
//                        iSecsHost.isConnect = false;
//                    }
//                    GlobalConstants.stage.hostManager.sendStatus2Server(equipModel.deviceCode, "OFFLINE");
//                } else {
//                    if (!iSecsHost.iSecsConnection.getSocketClient().isConnected()) {
//                        logger.info(equipModel.deviceCode + "Check equipModel: SocketClient.disConnected");
//                        needInit = true;
//                    }
////                    if (equipModel.getPassport(2)) {
////                        if (!iSecsHost.checkConenctionStatus()) {
////                            logger.info(equipModel.deviceCode + "Check equipModel: SocketClient.checkConenctionStatus==false");
////                            needInit = true;
////                        }
////                        equipModel.returnPassport();
////                    }
//                }
//                if (needInit) {
//                    logger.info(equipModel.deviceCode + " connect off");
//                    map.put("CommState", 0);
//                    equipModel.changeEquipPanel(map);
//                    iSecsHost.isConnect = false;
//                    equipModel.equipState.setCommOn(false);
//                    equipModel.initialize();
//                }
//
//                //  GlobalConstants.stage.equipModels.remove(equipModel.deviceCode);
//                // GlobalConstants.stage.equipModels.put(equipModelTmp.deviceCode, equipModelTmp);
//            } else {
//                map.put("CommState", 1);
//                equipModel.changeEquipPanel(map);
//                equipModel.equipState.setCommOn(true);
//                equipModel.iSecsHost.isConnect = true;
//                equipModel.setIsConnect(true);
//            }
//        }
////        if (equipModel.iSecsHost.iSecsConnection.getSocketClient() == null
////                || !equipModel.iSecsHost.iSecsConnection.getSocketClient().isConnected()
////                || !equipModel.iSecsHost.iSecsConnection.checkConenctionStatus()) {
////            if (equipModel.iSecsHost.iSecsConnection.getSocketClient() == null) {
////                logger.info("Check equipModel:getSocketClient() == null");
////                needInit = true;
////                if (checkNet(equipModel.remoteIPAddress) >= 3) {
////                    netBrokenDealer(equipModel.deviceCode);
////                    needInit = false;
////                    map.put("CommState", 0);
////                    equipModel.changeEquipPanel(map);
////                    equipModel.iSecsHost.isConnect = false;
////                }
////                GlobalConstants.stage.hostManager.sendStatus2Server(equipModel.deviceCode, "OFFLINE");
////            } else {
////                if (!equipModel.iSecsHost.iSecsConnection.getSocketClient().isConnected()) {
////                    logger.info("Check equipModel: SocketClient.disConnected");
////                    needInit = true;
////                }
////                if (!equipModel.iSecsHost.iSecsConnection.checkConenctionStatus()) {
////                    logger.info("Check equipModel: SocketClient.checkConenctionStatus==false");
////                    needInit = true;
////                }
////            }
////            if (needInit) {
////                logger.info("connect off");
////                // map.put("CommState", 0);
////                // equipModel.changeEquipPanel(map);
////                equipModel.iSecsHost.isConnect = false;
////                equipModel.equipState.setCommOn(false);
////                equipModel.initialize();
////            }
////
////            //  GlobalConstants.stage.equipModels.remove(equipModel.deviceCode);
////            // GlobalConstants.stage.equipModels.put(equipModelTmp.deviceCode, equipModelTmp);
////        } else {
////            map.put("CommState", 1);
////            equipModel.changeEquipPanel(map);
////            equipModel.equipState.setCommOn(true);
////        }
//
//    }

    private void easyCheck(EquipModel equipModel) {
        Map map = new HashMap();
//        boolean needInit = false;
        logger.info("Check equipModel:" + equipModel.deviceCode);
        for (ISecsHost iSecsHost : equipModel.iSecsHostList) {
            logger.info(equipModel.deviceCode + "TESTLink...");
            long nowDate = new Date().getTime();
            //满足任一条件进行check
            if ((nowDate - iSecsHost.preCommDate) > 30 * 1000 | (iSecsHost.lastCommDate - iSecsHost.preCommDate) > 30 * 1000) {
                logger.info(equipModel.deviceCode + "满足check条件：lastCommDate=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(iSecsHost.lastCommDate)
                        + ",preCommDate=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(iSecsHost.preCommDate)
                        + ",nowDate=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(nowDate)
                        + ",Timeout=" + (iSecsHost.lastCommDate - iSecsHost.preCommDate));

            } else {
                logger.info(equipModel.deviceCode + "不满足check条件：lastCommDate=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(iSecsHost.lastCommDate)
                        + ",preCommDate=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(iSecsHost.preCommDate)
                        + ",nowDate=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(nowDate)
                        + ",Timeout=" + (iSecsHost.lastCommDate - iSecsHost.preCommDate));
                continue;
            }
            if (!testLinkSocket(iSecsHost, equipModel.deviceCode)) {
                logger.info(equipModel.deviceCode + "Check equipModel:getSocketClient() == null");
                logger.info(equipModel.deviceCode + " connect off");
                GlobalConstants.stage.hostManager.sendStatus2Server(equipModel.deviceCode, "OFFLINE");
                map.put("CommState", 0);
                equipModel.changeEquipPanel(map);
                iSecsHost.isConnect = false;
                equipModel.equipState.setCommOn(false);
//                equipModel.initialize();
                //置最后一次通信时间为0，确保下次一定执行testLinkSocket
                iSecsHost.preCommDate = 0;
                if (checkNet(iSecsHost.ip) >= 3) {
                    netBrokenDealer(equipModel.deviceCode);
//                    needInit = false;
//                    map.put("CommState", 0);
//                    equipModel.changeEquipPanel(map);
//                    iSecsHost.isConnect = false;
                }
            } else {
                map.put("CommState", 1);
                equipModel.changeEquipPanel(map);
                equipModel.equipState.setCommOn(true);
                equipModel.iSecsHost.isConnect = true;
                equipModel.setIsConnect(true);
            }
//            if (iSecsHost.iSecsConnection.getSocketClient() == null
//                    || !iSecsHost.iSecsConnection.getSocketClient().isConnected()
//                    || !iSecsHost.iSecsConnection.checkConenctionStatus()) {
//                if (iSecsHost.iSecsConnection.getSocketClient() == null) {
//                    logger.info(equipModel.deviceCode + "Check equipModel:getSocketClient() == null");
//                    needInit = true;
//                    if (checkNet(iSecsHost.ip) >= 3) {
//                        netBrokenDealer(equipModel.deviceCode);
//                        needInit = false;
//                        map.put("CommState", 0);
//                        equipModel.changeEquipPanel(map);
//                        iSecsHost.isConnect = false;
//                    }
//                    GlobalConstants.eapView.hostManager.sendStatus2Server(equipModel.deviceCode, "OFFLINE");
//                } else {
//                    if (!iSecsHost.iSecsConnection.getSocketClient().isConnected() || !equipModel.isConnect()) {
//                        logger.info(equipModel.deviceCode + "Check equipModel: SocketClient.disConnected");
//                        needInit = true;
//                    }
////                    if (equipModel.getPassport(2)) {
////                        if (!iSecsHost.checkConenctionStatus()) {
////                            logger.info(equipModel.deviceCode + "Check equipModel: SocketClient.checkConenctionStatus==false");
////                            needInit = true;
////                        }
////                        equipModel.returnPassport();
////                    }
//                }
//
//                if (needInit) {
//                    logger.info(equipModel.deviceCode + " connect off");
//                    map.put("CommState", 0);
//                    equipModel.changeEquipPanel(map);
//                    iSecsHost.isConnect = false;
//                    equipModel.equipState.setCommOn(false);
//                    equipModel.initialize();
//                }
//
//                //  GlobalConstants.eapView.equipModels.remove(equipModel.deviceCode);
//                //GlobalConstants.eapView.equipModels.put(equipModelTmp.deviceCode, equipModelTmp);
//            } else {
//                map.put("CommState", 1);
//                equipModel.changeEquipPanel(map);
//                equipModel.equipState.setCommOn(true);
//                equipModel.iSecsHost.isConnect = true;
//                equipModel.setIsConnect(true);
//            }
        }
//        if (equipModel.iSecsHost.iSecsConnection.getSocketClient() == null
//                || !equipModel.iSecsHost.iSecsConnection.getSocketClient().isConnected()
//                || !equipModel.iSecsHost.iSecsConnection.checkConenctionStatus()) {
//            if (equipModel.iSecsHost.iSecsConnection.getSocketClient() == null) {
//                logger.info("Check equipModel:getSocketClient() == null");
//                needInit = true;
//                if (checkNet(equipModel.remoteIPAddress) >= 3) {
//                    netBrokenDealer(equipModel.deviceCode);
//                    needInit = false;
//                    map.put("CommState", 0);
//                    equipModel.changeEquipPanel(map);
//                    equipModel.iSecsHost.isConnect = false;
//                }
//                GlobalConstants.eapView.hostManager.sendStatus2Server(equipModel.deviceCode, "OFFLINE");
//            } else {
//                if (!equipModel.iSecsHost.iSecsConnection.getSocketClient().isConnected()) {
//                    logger.info("Check equipModel: SocketClient.disConnected");
//                    needInit = true;
//                }
//                if (!equipModel.iSecsHost.iSecsConnection.checkConenctionStatus()) {
//                    logger.info("Check equipModel: SocketClient.checkConenctionStatus==false");
//                    needInit = true;
//                }
//            }
//            if (needInit) {
//                logger.info("connect off");
//                // map.put("CommState", 0);
//                // equipModel.changeEquipPanel(map);
//                equipModel.iSecsHost.isConnect = false;
//                equipModel.equipState.setCommOn(false);
//                equipModel.initialize();
//            }
//
//            //  GlobalConstants.eapView.equipModels.remove(equipModel.deviceCode);
//            //GlobalConstants.eapView.equipModels.put(equipModelTmp.deviceCode, equipModelTmp);
//        } else {
//            map.put("CommState", 1);
//            equipModel.changeEquipPanel(map);
//            equipModel.equipState.setCommOn(true);
//        }

    }

    public boolean testLinkSocket(ISecsHost iSecsHost, String deviceCode) {
        try {
            logger.info("开始检查Socket连接===>" + deviceCode+",PROT:"+iSecsHost.iSecsConnection.getPort());
            if (iSecsHost.iSecsConnection.getSocketClient() == null) {
                logger.info("连接中断，尝试重连===>" + deviceCode+",PROT:"+iSecsHost.iSecsConnection.getPort());
                iSecsHost.iSecsConnection = new ISecsConnection(iSecsHost.ip, iSecsHost.port);
                if (iSecsHost.iSecsConnection.getSocketClient() != null) {
//                    iSecsHost.iSecsConnection.getSocketClient().sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
                    iSecsHost.checkLinkStatus();
//                    List<String> result = iSecsHost.executeCommand("curscreen");
//                    if (result.get(0).toLowerCase().contains("error")) {
//                        if(result.get(0).toLowerCase().contains("Error 0100")){
//                            logger.info("当前设备处于LOCAL模式===>" + deviceCode);
//                        }else{
//                            throw new SocketException("socket error!");
//                        }
//                    }
                    logger.info("重新建立连接成功===>" + deviceCode+",PROT:"+iSecsHost.iSecsConnection.getPort());
                } else {
                    logger.info("重新建立连接失败===>" + deviceCode+",PROT:"+iSecsHost.iSecsConnection.getPort());
                    return false;
                }
            } else if (iSecsHost.iSecsConnection.getSocketClient() != null) {
                iSecsHost.checkLinkStatus();
//                List<String> result = iSecsHost.executeCommand("curscreen");
//                if (result.get(0).toLowerCase().contains("error")) {
//                    if(result.get(0).toLowerCase().contains("Error 0100")){
//                        logger.info("当前设备处于LOCAL模式===>" + deviceCode);
//                    }else {
//                        throw new SocketException("socket error!");
//                    }
//                }
//                iSecsHost.iSecsConnection.getSocketClient().sendUrgentData(0xFF);
                logger.info("Socket连接一切正常===>" + deviceCode+",PROT:"+iSecsHost.iSecsConnection.getPort());
            }
            return true;
        } catch (Exception e) {
//            e.printStackTrace();
            logger.info("Socket异常，再次确认连接状况！：" + e.getMessage());
            logger.debug(e);
            if (iSecsHost.iSecsConnection.getSocketClient() != null) {
                try {
//                    iSecsHost.iSecsConnection.getSocketClient().connect(new InetSocketAddress(iSecsHost.iSecsConnection.getIp(), Integer.parseInt(iSecsHost.iSecsConnection.getPort())), 5000);
                    iSecsHost.checkLinkStatus();
//                    List<String> result = iSecsHost.executeCommand("curscreen");
//                    if (result.get(0).toLowerCase().contains("error")) {
//                        if(result.get(0).toLowerCase().contains("Error 0100")){
//                            logger.info("当前设备处于LOCAL模式===>" + deviceCode);
//                        }else{
//                            throw new SocketException("socket error!");
//                        }
//                    }
                    //此指令无效
                    // iSecsHost.iSecsConnection.getSocketClient().sendUrgentData(0xFF);
                    logger.info("Socket成功建立连接===>" + deviceCode+",PROT:"+iSecsHost.iSecsConnection.getPort());
                    return true;
                } catch (Exception e1) {
                    logger.error("Socket异常，确认连接失败！关闭原有socket并置空！=》", e1);
                    if (iSecsHost.iSecsConnection.getSocketClient() != null) {
                        try {
                            //断开之前的socket，释放资源，ocr只允许一个连接
                            iSecsHost.iSecsConnection.getSocketClient().close();
                        } catch (IOException e2) {
                            logger.debug(e);
//                            e2.printStackTrace();
                        } finally {
                            iSecsHost.iSecsConnection.setSocketClient(null);
                            iSecsHost.iSecsConnection = new ISecsConnection(iSecsHost.ip, iSecsHost.port);
//                            if("Connection reset by peer: send".equals(e1.getMessage())){
//                                return true;
//                            }
                        }
                    }
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
