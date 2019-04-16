package cn.tzauto.octopus.gui.main;

import java.io.IOException;
import java.util.Date;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import javafx.scene.control.Button;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class loginOutTask implements Job {
    private static final Logger logger = Logger.getLogger(cn.tzauto.octopus.common.util.scheduler.job.SessionControlTask.class);
    private static final long timeDiff = Long.parseLong(GlobalConstants.sessionCtrlCycle);//5分钟

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        logger.debug("SessionControlTask任务执行....");
        if (GlobalConstants.loginTime != null) {
            long past = GlobalConstants.loginTime.getTime();
            long now = new Date().getTime();
            if (now - past >= timeDiff) {
                if (GlobalConstants.sysUser != null) {
                    String userName = GlobalConstants.sysUser.getLoginName();
                    GlobalConstants.sysUser = null;
                   UiLogUtil.getInstance().appendLog2EventTab(null, "用户：" + userName + " 长时间未进行关键操作，登录已自动注销...");
                }
                try {
                    new EapMainController().loginOut();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                GlobalConstants.loginTime = null;
                logger.debug("长时间未进行关键性操作，登录已被注销");
            }
        }
    }
}
