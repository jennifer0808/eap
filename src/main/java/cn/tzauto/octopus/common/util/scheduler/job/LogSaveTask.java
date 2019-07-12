/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 */
public class LogSaveTask implements Job{

    private static final Logger logger = Logger.getLogger(LogSaveTask.class);
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        logger.debug("LogSaveTask任务执行....");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(calendar.DATE, -1);//获取前一天操作的操作日志
        date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<DeviceOplog> deviceOplogs = deviceService.queryDeviceOplogByDate(dateFormat.format(date));
        sqlSession.close();
        Map mqMap = new HashMap();
        mqMap.put("msgName", "TransferMdDeviceOplog");
        mqMap.put("mdDeviceOplog", JsonMapper.toJsonString(deviceOplogs));
        GlobalConstants.C2SLogQueue.sendMessage(mqMap);
       UiLogUtil.getInstance().appendLog2SeverTab(null, "定时向服务端发送设备操作历史记录");
    }
    
}
