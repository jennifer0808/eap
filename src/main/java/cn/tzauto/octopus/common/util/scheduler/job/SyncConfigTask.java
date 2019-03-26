/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author gavin
 */
public class SyncConfigTask implements Job {

    private static final Logger logger=Logger.getLogger(SyncConfigTask.class);
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        logger.debug("SyncConfigTask任务执行....");
//        String  resultStr=WSUtility.getDeviceList(GlobalConstants.getProperty("SERVER_ID"));
//        jec.getJobDetail().getJobDataMap().put(GlobalConstants.SYNC_JOB_DATA_MAP, resultStr);
    }
}
