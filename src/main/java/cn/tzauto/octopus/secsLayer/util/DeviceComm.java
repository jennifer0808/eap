
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.util;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.EquipmentEventDealer;
import cn.tzauto.octopus.gui.main.EapClient;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.exception.NotInitializedException;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;


/**
 * @author njtz
 */
public class DeviceComm {

    private static final Logger logger = Logger.getLogger(DeviceComm.class);

    public static void startHost(final EquipNodeBean equipNodeBean) {
//        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, equipNodeBean.getDeviceCode());
//        EquipmentEventDealer watchDogOld = EAPGuiView.removeWatchDog(equipNodeBean.getDeviceIdProperty());
//        if (watchDogOld != null && !watchDogOld.isDone()) { // still runnning
//            //watchDog is stopped at last if Host is requested to be stopped.
//            JOptionPane.showMessageDialog(null, "设备通信已经开启！");
//            return;
////        }
//        //Container a = this.getParent();
//        SwingWorker worker = new SwingWorker<Void, Void>() {
//
//            @Override
//            public Void doInBackground() {
        logger.info("DoInBackground,start comm... ");
        String deviceCode = equipNodeBean.getDeviceCode();
        EquipmentEventDealer equipmentEventDealer = new EquipmentEventDealer(equipNodeBean, GlobalConstants.stage);
        //start the watch dog
        equipmentEventDealer.execute();
        //start the Host Thread
        MultipleEquipHostManager hostsManager = EapClient.hostManager;
        hostsManager.startHostThread(deviceCode);

        //start the SECS protocols
        try {
            hostsManager.startSECS(deviceCode, equipmentEventDealer);
        } catch (NotInitializedException e1) {
            e1.printStackTrace();
//                    return null;
        } catch (T6TimeOutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (T3TimeOutException e) {
            e.printStackTrace();
        } catch (InvalidDataException e) {
            e.printStackTrace();
        } catch (StateException e) {
            e.printStackTrace();
        }
        EapClient.removeWatchDog(deviceCode);
        EapClient.addWatchDog(deviceCode, equipmentEventDealer);
//                return null;
//            }
//        };
//        worker.execute();
    }

    public static void restartHost(EquipNodeBean equipNodeBean) {
        boolean needRestart = true;
        String temp = (String) MDC.get(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, equipNodeBean.getDeviceCode());
        EquipmentEventDealer watchDog = EapClient.getWatchDog(equipNodeBean.getDeviceCode());
//        if (watchDog == null || watchDog.isDone()) { // not runnning
//            //watchDog is stopped at last if Host is requested to be stopped.
//            JOptionPane.showMessageDialog(null, "设备通信已经关闭！");
//            return;
//        }
        String deviceId = equipNodeBean.getDeviceIdProperty();
//        MultipleEquipHostManager hostsManager = ((EAPGuiApp) GlobalConstants.stage.getContext().getApplication()).getMultipleEquipHostManager();
//        hostsManager.terminateSECS(deviceId);
        logger.info("deviceId===>" + deviceId + "    watchDog==>" + watchDog);
//        watchDog.notificationOfCloseNetwork(deviceId);
        logger.info("deviceId===>" + deviceId + " ; 停止SECS通信");
        GlobalConstants.stage.hostManager.terminateSECS(equipNodeBean.getDeviceCode());
        logger.info("deviceId===>" + deviceId + " ; 停止Host");
        GlobalConstants.stage.hostManager.terminateHostThread(equipNodeBean.getDeviceCode());
//        Thread.yield();
//        watchDog.exit(); //throw an interruption exception into the watchDog thread
        logger.info("deviceId===>" + deviceId + " ; 开始重启线程");
        int i = 0;
//        while (i < 20) {
//            try {
//                logger.info("deviceId===>"+deviceId+"    第"+i+"次尝试关闭通信");
//                if (watchDog.isHostIsShutDown()) {
//                    startHostSwing(equipNodeBean);
//                    logger.info("deviceId===>"+deviceId+"    第"+i+"次关闭通信成功");
//                }
//                Thread.sleep(1000);
//            } catch (InterruptedException ex) {
//                java.util.logging.Logger.getLogger(DeviceComm.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            i++;
//        }
//        if(i==10){
        String intervalSettingString = GlobalConstants.getProperty("DELAY_BEFORE_RESTART");
        if (intervalSettingString != null && !"".equals(intervalSettingString) && Integer.parseInt(intervalSettingString) > 0) {
            try {
                int delaySecond = Integer.parseInt(intervalSettingString) / 1000;
                for (int second = 0; second < delaySecond; second++) {
                    logger.info("deviceId===>" + deviceId + "   通信将在" + (delaySecond - second) + "秒后重启");
                    if (GlobalConstants.stage.hostManager.hostIsConnected(equipNodeBean.getDeviceCode())) {
                        logger.info("deviceId===>" + deviceId + "  目前连接正常，无需重启");
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        } else {
            logger.info("deviceId===>" + deviceId + "    未设置通信重启延迟");
        }
        if (needRestart) {
            startHost(equipNodeBean);
            logger.info("deviceId===>" + deviceId + "    已经重新开启通信");
        } else {
            logger.info("deviceId===>" + deviceId + "    已不需要重启");
        }

        if(temp ==null){
            MDC.remove(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
        }else{
            MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, temp);
        }

    }
}
