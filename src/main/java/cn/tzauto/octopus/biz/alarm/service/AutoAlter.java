/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.biz.alarm.service;


import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 *
 * @author njtz modify :2016-8-23
 */
public class AutoAlter {

    private static Logger logger = Logger.getLogger(AutoAlter.class);

    public static void alter(Map alarmMap) {
        if (alarmMap == null) {
            return;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        AlarmService alarmService = new AlarmService(sqlSession);
        try {
            alarmService.triggerAlarm(alarmMap);
            sqlSession.commit();
        } catch (Exception e) {
            sqlSession.rollback();
            logger.error("Exception:", e);
        } finally {
            sqlSession.close();
        }
    }
}
