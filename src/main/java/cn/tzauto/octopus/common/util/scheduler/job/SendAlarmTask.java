/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.biz.alarm.service.AlarmService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 *
 * @author Administrator
 */
public class SendAlarmTask implements Job {

    private static final Logger logger = Logger.getLogger(SendAlarmTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        AlarmService alarmService = new AlarmService(sqlSession);
        List<AlarmRecord> alarmRecords = alarmService.searchAlarmRecordInOneHour();
        sqlSession.close();
        Map alarmMap = new HashMap();
        alarmMap.put("msgName", "alarm.TransferAlarmRecord");
        alarmMap.put("alarmRecord",JsonMapper.toJsonString(alarmRecords));
        GlobalConstants.C2SAlarmQueue.sendMessage(alarmMap);
        GlobalConstants.sysLogger.info("向服务端每隔1小时发送Alarm记录。");
    }
}
