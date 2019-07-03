/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;

import java.util.Date;

import cn.tzauto.octopus.gui.main.EapClient;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import cn.tzauto.octopus.gui.main.EapMainController;


/**
 *
 * @author rain
 */
public class SessionControlTask implements Job {

    private static final Logger logger = Logger.getLogger(SessionControlTask.class);
    private static final long timeDiff = Long.parseLong(GlobalConstants.sessionCtrlCycle);//5分钟

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        logger.debug("SessionControlTask任务执行....");
        if (GlobalConstants.loginTime != null) {
            long past = GlobalConstants.loginTime.getTime();
            long now = new Date().getTime();
            if (now - past >= timeDiff) {
                if (GlobalConstants.sysUser != null) {
                    String userName = GlobalConstants.sysUser.getLoginName();
                    GlobalConstants.sysUser = null;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Button JB_MainPage = (Button) EapClient.root.lookup("#JB_MainPage");
                            Button JB_RcpMng = (Button) EapClient.root.lookup("#JB_RcpMng");
                            Button JB_Login = (Button) EapClient.root.lookup("#JB_Login");
                            Button JB_SignOut = (Button) EapClient.root.lookup("#JB_SignOut");
                            Button localMode = (Button) EapClient.root.lookup("#localMode");
                            JB_MainPage.setVisible(false);
                            JB_RcpMng.setVisible(false);
                            JB_Login.setVisible(true);
                            JB_SignOut.setVisible(false);
                            localMode.setVisible(false);
                            GlobalConstants.sysUser = null;
                            GlobalConstants.userFlag = true;
                            GlobalConstants.isUpload = false;
                            GlobalConstants.isDownload = false;
                            GlobalConstants.isSvQuery = false;
                            EapMainController.loginmark=0;
                            TabPane TBP_Main = (TabPane) GlobalConstants.stage.root.lookup("#TBP_Main");
                            TBP_Main.getTabs().remove(EapMainController.rcpMngtTab);
                        }
                    });
                   UiLogUtil.getInstance().appendLog2EventTab(null, "用户：" + userName + " 长时间未进行关键操作，登录已自动注销...");
                }
//                GlobalConstants.stage.setPartsInvisible();//超过设定时间系统自动注销
                GlobalConstants.loginTime = null;
                logger.debug("长时间未进行关键性操作，登录已被注销");
            }
        }

    }
}
