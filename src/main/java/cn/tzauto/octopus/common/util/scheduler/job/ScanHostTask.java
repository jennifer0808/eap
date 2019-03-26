/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.exception.NotInitializedException;

import cn.tzauto.octopus.gui.EquipmentEventDealer;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.main.EapClient;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * 监控Host线程，如果interuptered，那么重新启动host线程
 *
 * @author gavin
 */
public class ScanHostTask implements Job {

    private static final Logger logger = Logger.getLogger(ScanHostTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        logger.debug("ScanHostTask=====>ScanHostTask任务执行....");
//        String  resultStr=WSUtility.getDeviceList(GlobalConstants.getProperty("SERVER_ID"));
//        jec.getJobDetail().getJobDataMap().put(GlobalConstants.SYNC_JOB_DATA_MAP, resultStr);
        MultipleEquipHostManager hostsManager = GlobalConstants.stage.getMultipleEquipHostManager();
//        hostsManager.testComm();
        // Todo 扫描所有Host线程，如果中断，重新启动
        for (int i = 0; i < GlobalConstants.stage.equipBeans.size(); i++) {
            MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, GlobalConstants.stage.equipBeans.get(i).getEquipName());
            // EAPGuiView.removeWatchDog(Integer.valueOf(list.get(i + 1)));                                    
            String deviceId = GlobalConstants.stage.equipBeans.get(i).getDeviceIdProperty();
            //start the Host Thread
            logger.debug("ScanHostTask=====>DeviceID:" + deviceId + "=========>状态:" + hostsManager.getAllEquipHosts().get(deviceId).isInterrupted());
            if (hostsManager.getAllEquipHosts().get(deviceId).isInterrupted() == true) {
                logger.debug("ScanHostTask=====>检测到中断，DeviceID:" + deviceId);
                EquipmentEventDealer watchDog = new EquipmentEventDealer(GlobalConstants.stage.equipBeans.get(i), GlobalConstants.stage);
                //start the watch dog
                watchDog.execute();
                hostsManager.startHostThread(deviceId);
                try {
                    hostsManager.startSECS(deviceId, watchDog, watchDog, watchDog);
                    System.out.println(GlobalConstants.stage.equipBeans.get(i).getEquipName() + " has  been restart!");
                } catch (NotInitializedException e1) {
                    System.out.println(GlobalConstants.stage.equipBeans.get(i).getEquipName() + " has restarted failure");
                }
                EapClient.addWatchDog(deviceId, watchDog);
                logger.debug("ScanHostTask=====>重新启动完成，DeviceID:" + deviceId);
            } else {
                continue;
            }

        }

    }
}
