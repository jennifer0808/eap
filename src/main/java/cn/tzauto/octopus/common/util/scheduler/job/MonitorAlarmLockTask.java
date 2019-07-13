/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author luosy
 */
public class MonitorAlarmLockTask implements Job {

    private static final Logger logger = Logger.getLogger(MonitorAlarmLockTask.class);

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        logger.info("MonitorAlarmLockTask start...");
        if ( GlobalConstants.stage.equipModels != null && GlobalConstants.stage.equipModels.size() > 0) {
            String temp = (String) MDC.get(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
            for (EquipModel equipModel : GlobalConstants.stage.equipModels.values()) {
                MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, equipModel.deviceCode);
                if (!equipModel.iSecsHost.isConnect) {
                    continue;
                }
                try {
                    Thread.sleep(300);
                    if (equipModel.checkLockFlagFromServerByWS(equipModel.deviceCode)) {
                        equipModel.stopEquip();
                    }
                } catch (Exception e) {
                    logger.error("设备:" + equipModel.deviceCode + "定时检查AlarmLock时触发异常." + e.getMessage());
                }
            }
            if(temp ==null){
                MDC.remove(GlobalConstant.WHICH_EQUIPHOST_CONTEXT);
            }else{
                MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, temp);
            }
        }
    }
}
