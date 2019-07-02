/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;


import cn.tzauto.octopus.common.util.ftp.HtFtpUtil;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * @author luosy
 */
public class FtpFileTask implements Job {

    private static final Logger logger = Logger.getLogger(FtpFileTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        logger.info("FtpFileTask=====>任务执行....");
        HtFtpUtil htFtpUtil = new HtFtpUtil();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy年MM月");
        LocalDateTime now = LocalDateTime.now();  //这一个月
        LocalDateTime ldt = now.minusMonths(1);   //上一个月

        htFtpUtil.recordFile("/" + ldt.format(dtf) + "/");
        htFtpUtil.recordFile("/" + now.format(dtf) + "/");

        logger.info("FtpFileTask任务执行结束...");


    }
}
