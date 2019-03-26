/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler;

import java.text.ParseException;
import java.util.Date;
import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.KeyMatcher;

/**
 *
 * @author gavin
 */
public class QuartzManager {

    public static final Logger logger = Logger.getLogger(QuartzManager.class);
    public static SchedulerFactory sf = new StdSchedulerFactory();
    public static String JOB_GROUP_NAME = "DefaultGroup";
    public static String TRIGGER_GROUP_NAME = "triggerGroup1ger1";

    /**
     *
     */
    /**
     * 添加一个定时任务，使用默认的任务组名，触发器名，触发器组名
     *
     * @param jobName 任务名
     * @param job 任务
     * @param time 时间设置，参考quartz说明文档
     * @throws SchedulerException
     * @throws ParseException
     */
    public static void addJob(String jobName, Job job, String cronExpression)
            throws SchedulerException, ParseException {
        addJob(jobName, JOB_GROUP_NAME, jobName, TRIGGER_GROUP_NAME, job, cronExpression);
    }

    /**
     *
     */
    /**
     * 添加一个定时任务
     *
     * @param jobName 任务名
     * @param jobGroupName 任务组名
     * @param triggerName 触发器名
     * @param triggerGroupName 触发器组名
     * @param job 任务
     * @param time 时间设置，参考quartz说明文档
     * @throws SchedulerException
     * @throws ParseException
     */
    public static void addJob(String jobName, String jobGroupName,
            String triggerName, String triggerGroupName,
            Job job, String cronExpression)
            throws SchedulerException, ParseException {
        try {
            Scheduler scheduler = sf.getScheduler();
            JobDetail jobDetail = JobBuilder.newJob(job.getClass()).withIdentity(jobName, jobGroupName).build();
            //①-1：创建CronTrigger，指定组及名称
            Date startTime = new Date(System.currentTimeMillis() + 50000);
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName).startAt(startTime).
                    withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing()).build();
            scheduler.scheduleJob(jobDetail, trigger);
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
            logger.info("新增作业=> [作业名称：" + jobName + " 作业组：" + jobGroupName + "] ");
        } catch (SchedulerException e) {
            e.printStackTrace();
            logger.error("新增作业=> [作业名称：" + jobName + " 作业组：" + jobGroupName + "]=> [失败]");
        }

    }

    public static void addImmediateJob(String jobName,
            Job job, String cronExpression)
            throws SchedulerException, ParseException {
        try {
            Scheduler scheduler = sf.getScheduler();
            JobDetail jobDetail = JobBuilder.newJob(job.getClass()).withIdentity(jobName, JOB_GROUP_NAME).build();
            //①-1：创建CronTrigger，指定组及名称
            Date startTime = new Date(System.currentTimeMillis() + 1000);
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(TRIGGER_GROUP_NAME).startAt(startTime).
                    withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing()).build();
            scheduler.scheduleJob(jobDetail, trigger);
            if (!scheduler.isShutdown()) {
                scheduler.start();
            }
            logger.info("新增作业=> [作业名称：" + jobName + " 作业组：" + JOB_GROUP_NAME + "] ");
        } catch (SchedulerException e) {
            e.printStackTrace();
            logger.error("新增作业=> [作业名称：" + jobName + " 作业组：" + JOB_GROUP_NAME + "]=> [失败]");
        }

    }

    /**
     *
     */
    /**
     * 修改一个任务的触发时间(使用默认的任务组名，触发器名，触发器组名)
     *
     * @param jobName
     * @param time
     * @throws SchedulerException
     * @throws ParseException
     */
    public static void modifyJobTime(String jobName, String cronExpression)
            throws SchedulerException, ParseException {
        modifyJobTime(jobName, JOB_GROUP_NAME, TRIGGER_GROUP_NAME, cronExpression);
    }

    /**
     *
     */
    /**
     * 修改一个任务的触发时间
     *
     * @param triggerName
     * @param triggerGroupName
     * @param time
     * @throws SchedulerException
     * @throws ParseException
     */
    public static void modifyJobTime(String jobName, String jobGroupName, String triggerGroupName,
            String cronExpression)
            throws SchedulerException, ParseException {
        try {
            Scheduler scheduler = sf.getScheduler();
            TriggerKey tk = TriggerKey.triggerKey(jobName, jobGroupName);
            Trigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName, triggerGroupName).withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build();
            if (trigger != null) {
                scheduler.rescheduleJob(tk, trigger);
            }
            logger.info("修改作业=> [作业名称：" + jobName + " 作业组：" + jobGroupName + "] ");
        } catch (SchedulerException e) {
            e.printStackTrace();
            logger.error("修改作业=> [作业名称：" + jobName + " 作业组：" + jobGroupName + "]=> [失败]");
        }

    }

    /**
     *
     */
    /**
     * 移除一个任务(使用默认的任务组名，触发器名，触发器组名)
     *
     * @param jobName
     * @throws SchedulerException
     */
    public static void removeJob(String jobName)
            throws SchedulerException {

        removeJob(jobName, JOB_GROUP_NAME, jobName, TRIGGER_GROUP_NAME);
    }

    /**
     *
     */
    /**
     * 移除一个任务
     *
     * @param jobName
     * @param jobGroupName
     * @param triggerName
     * @param triggerGroupName
     * @throws SchedulerException
     */
    public static void removeJob(String jobName, String jobGroupName,
            String triggerName, String triggerGroupName)
            throws SchedulerException {
        Scheduler scheduler = sf.getScheduler();
        try {
            TriggerKey tk = TriggerKey.triggerKey(jobName, triggerGroupName);
            scheduler.pauseTrigger(tk);//停止触发器  
            scheduler.unscheduleJob(tk);//移除触发器
            JobKey jobKey = JobKey.jobKey(jobName, jobGroupName);
            scheduler.deleteJob(jobKey);//删除作业
            logger.info("删除作业=> [作业名称：" + jobName + " 作业组：" + jobGroupName + "] ");
        } catch (SchedulerException e) {
            e.printStackTrace();
            logger.error("删除作业=> [作业名称：" + jobName + " 作业组：" + jobGroupName + "]=> [失败]");
        }

    }

    public static void pauseJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = sf.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            scheduler.pauseJob(jobKey);
            logger.info("暂停作业=> [作业名称：" + jobName + " 作业组：" + jobGroup + "] ");
        } catch (SchedulerException e) {
            e.printStackTrace();
            logger.error("暂停作业=> [作业名称：" + jobName + " 作业组：" + jobGroup + "]=> [失败]");
        }
    }

    public static void resumeJob(String jobName, String jobGroup) {
        try {
            Scheduler scheduler = sf.getScheduler();
            JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
            scheduler.resumeJob(jobKey);
            logger.info("恢复作业=> [作业名称：" + jobName + " 作业组：" + jobGroup + "] ");
        } catch (SchedulerException e) {
            e.printStackTrace();
            logger.error("恢复作业=> [作业名称：" + jobName + " 作业组：" + jobGroup + "]=> [失败]");
        }
    }

    public static void addJobListner(JobListener jobListener, JobKey jobKey) throws SchedulerException {
        Scheduler scheduler = sf.getScheduler();
        scheduler.getListenerManager().addJobListener(jobListener, KeyMatcher.keyEquals(jobKey));

    }
}
