/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.monitor.service.MonitorService;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 *
 * @author luosy
 */
public class DataCleanTask implements Job {

    private static final Logger logger = Logger.getLogger(DataCleanTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        logger.info("CleanDataTask=====>CleanDataTask任务执行....");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        MonitorService monitorService = new MonitorService(sqlSession);
        RecipeService recipeService=new RecipeService(sqlSession);
        try {
            deviceService.cleanData();
            recipeService.cleanData();  
            monitorService.cleanData();
            sqlSession.commit();
        } catch (Exception e) {
            sqlSession.rollback();
            logger.error("Exception occur:", e);
        } finally {
            sqlSession.close();
            logger.info("CleanDataTask任务执行结束...");
        }

    }
}
