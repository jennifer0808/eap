/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.tool;

import cn.tzauto.octopus.common.util.scheduler.QuartzManager;
import cn.tzauto.octopus.common.util.scheduler.job.MonitorTask;
import cn.tzauto.octopus.common.util.scheduler.job.ScanHostTask;
import cn.tzauto.octopus.common.util.scheduler.job.SyncConfigTask;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.scheduler.job.DataCleanTask;
import cn.tzauto.octopus.common.util.scheduler.job.CommCheckTask;
import cn.tzauto.octopus.common.util.scheduler.job.LogSaveTask;
import cn.tzauto.octopus.common.util.scheduler.job.MonitorAlarmLockTask;
import cn.tzauto.octopus.common.util.scheduler.job.MonitorAlarmTask;
import cn.tzauto.octopus.common.util.scheduler.job.MonitorECTask;
import cn.tzauto.octopus.common.util.scheduler.job.NetCheckTask;
import cn.tzauto.octopus.common.util.scheduler.job.RefreshEquipStatusTask;
import cn.tzauto.octopus.common.util.scheduler.job.SessionControlTask;
import cn.tzauto.octopus.common.util.scheduler.job.UploadMTBATask;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.quartz.JobKey;
import org.quartz.JobListener;

/**
 *
 * @author gavin
 */
public class CommonUtil {

    private static final Logger logger = Logger.getLogger(CommonUtil.class.getName());

    public static boolean wirteString2File(String fielName, String str) {
        try {
            //将文件缓存到本地
            InputStream in = GlobalConstants.class.getClassLoader().getResourceAsStream(fielName);

            File file = new File(fielName);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(str);
            bw.close();
            fw.close();
            return true;
        } catch (IOException ex) {
            logger.error("IOException:", ex);
            return false;
        }
    }

//    public static String getDeviceListFromServer() {
//        String hostJsonFilePath = GlobalConstants.HOST_JSON_FILE;
//        String resultStr = "";
//        try {
//            logger.info("开始获取服务器配置");
//            resultStr = WSUtility.getDeviceList(GlobalConstants.getProperty("SERVER_ID"));
//
//            if (resultStr != null && !resultStr.equals("") && !resultStr.equals("Error")) {
//                logger.info("服务器配置获取成功...");
//                //wirteString2File(hostJsonFilePath,resultStr);
//            }
//        } catch (Exception e) {
//            logger.info("获取服务器配置失败，开始读取本地文件配置");
//
//            resultStr = loadLocalHostJson();
//            logger.error("Exception", e);
//        }
//        return resultStr;
//    }

    public static String loadLocalHostJson() {
        String hostJsonFilePath = GlobalConstants.HOST_JSON_FILE;
        String resultStr = "";
        File jsonHostFile = new File(hostJsonFilePath);
        if (!jsonHostFile.exists()) {
            logger.info("本地配置文件不存在，无法启动应用");
            System.exit(0);
        } else {
            //读取文件，
            BufferedReader reader = null;
            String laststr = "";
            try {
                reader = new BufferedReader(new FileReader(jsonHostFile));
                String tempString = null;
                while ((tempString = reader.readLine()) != null) {
                    laststr = laststr + tempString;
                }
                reader.close();
                resultStr = laststr;
            } catch (Exception e) {
                logger.error("Exception", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
        return resultStr;
    }

    /**
     * 启动定时同步配置job，每5分钟同步一次
     *
     * @param jobListener
     */
    public static void startSyncConfigJob(JobListener jobListener) {
        SyncConfigTask syncConfigTask = new SyncConfigTask();
        try {
            QuartzManager.addJob(GlobalConstants.SYNC_CONFIG_JOB_NAME, syncConfigTask, "0/30 * * * * ?"); //每5秒钟执行一次  
            JobKey jobKey = new JobKey(GlobalConstants.SYNC_CONFIG_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    public static void startScanHostJob() {
        ScanHostTask scanHostTask = new ScanHostTask();
        try {
            QuartzManager.addJob(GlobalConstants.SCAN_HOST_JOB_NAME, scanHostTask, "0 2/15 * * * ?"); //每15分钟执行一次 
//            JobKey jobKey=new JobKey(GlobalConstants.SCAN_HOST_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
//            QuartzManager.addJobListner(eapWin,jobKey);
            System.out.println("--------------------------------------------open host restart job----------------");
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    /**
     * 启动定时同步配置job，每5分钟同步一次
     *
     * @param jobListener
     */
    public static void startMonitorJob(JobListener jobListener) {
        MonitorTask monitorTask = new MonitorTask();
        String monitorTime = GlobalConstants.monitorTaskCycle;//"//"0 0/" 
        try {
            QuartzManager.addJob(GlobalConstants.MONITOR_CONFIG_JOB_NAME, monitorTask, "0 0/" + monitorTime + " * * * ?"); //第0分钟开始，每2分钟执行一次  
            JobKey jobKey = new JobKey(GlobalConstants.MONITOR_CONFIG_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        MonitorECTask monitorECTask = new MonitorECTask();
        try {
            QuartzManager.addJob(GlobalConstants.MONITOR_EC_JOB_NAME, monitorECTask, "0 0/" + monitorTime + " * * * ?"); //第0分钟开始，每2分钟执行一次  
            JobKey jobKey = new JobKey(GlobalConstants.MONITOR_EC_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    /**
     * 启动定时同步配置job，每5分钟同步一次
     *
     * @param jobListener
     */
    public static void startNetCheckJob(JobListener jobListener) {
        NetCheckTask netCheckTask = new NetCheckTask();
        String netCheckTime = GlobalConstants.netCheckCycle;
        try {
            QuartzManager.addJob(GlobalConstants.NET_CHECK_JOB_NAME, netCheckTask, "0/" + netCheckTime + " * * * * ?"); //每10秒执行一次  
            JobKey jobKey = new JobKey(GlobalConstants.NET_CHECK_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    /**
     * 启动定时连接任务，每10秒执行一次
     *
     * @param jobListener
     */
    public static void startCommuCheckJob(JobListener jobListener) {
        CommCheckTask commCheckTask = new CommCheckTask();
        try {
            QuartzManager.addJob(GlobalConstants.COMM_CHECK_JOB_NAME, commCheckTask, "0 0/10 * * * ?"); //每10秒执行一次
            JobKey jobKey = new JobKey(GlobalConstants.COMM_CHECK_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    /**
     * 启动定时同步配置job，每5分钟同步一次
     *
     * @param jobListener
     */
    public static void startLogSaveJob(JobListener jobListener) {
        LogSaveTask logSaveTask = new LogSaveTask();
        try {
            QuartzManager.addJob(GlobalConstants.LOG_SAVE_JOB_NAME, logSaveTask, "0 0 0 * * ?"); //每天0点触发，获取并发送前一天日志    '?' can only be specfied for Day-of-Month or Day-of-Week.
            JobKey jobKey = new JobKey(GlobalConstants.LOG_SAVE_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    /**
     * 启动定时同步配置job，每5分钟同步一次
     *
     * @param jobListener
     */
    public static void startSessCtrlJob(JobListener jobListener) {
        SessionControlTask sessionControlTask = new SessionControlTask();
        try {
            QuartzManager.addJob(GlobalConstants.SESSION_CONTROL_JOB_NAME, sessionControlTask, "0 * * * * ?"); //每分钟触发，控制Session
            JobKey jobKey = new JobKey(GlobalConstants.SESSION_CONTROL_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

//    public static boolean checkRecipe() {
//        //String hostJsonFilePath=GlobalConstants.HOST_JSON_FILE;
//        String resultStr = "";
//        try {
//            logger.info("开始获取服务器上的本工控机配置");
//            resultStr = WSUtility.getRecipeFromServer(GlobalConstants.getProperty("SERVER_ID"));
//
//            if (resultStr != null && !resultStr.equals("") && !resultStr.equals("Error")) {
//                logger.info("服务器此工控机配置获取成功...");
//                //wirteString2File(hostJsonFilePath,resultStr);
//            }
//        } catch (Exception e) {
//            logger.info("获取服务器此工控机配置失败，开始读取本地文件配置");
//
//            //resultStr=loadLocalHostRcpJson();
//            logger.error("Exception", e);
//        }
//        return true;
//    }

    public static int objectToInteger(Object obj) {
        try {
            if (obj != null && !obj.toString().trim().equals("")) {
                return Integer.parseInt(obj.toString());
            }
        } catch (Exception e) {
            logger.error("Exception", e);
            return 0;
        }
        return 0;
    }

    public static void startCleanDataJob(JobListener jobListener) {
        DataCleanTask cleanDataTask = new DataCleanTask();
        try {
            QuartzManager.addJob(GlobalConstants.DATA_CLEAN_JOB_NAME, cleanDataTask, "0 0 0 * * ?"); //每天0时执行一次
            JobKey jobKey = new JobKey(GlobalConstants.DATA_CLEAN_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }
    public static void startHtFtpJob(JobListener jobListener) {
        DataCleanTask cleanDataTask = new DataCleanTask();
        try {
            QuartzManager.addJob(GlobalConstants.FTP_FILE_JOB, cleanDataTask, "0 0 0/1 * * ?"); //每小时执行一次
            JobKey jobKey = new JobKey(GlobalConstants.FTP_FILE_JOB, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    public static void startMonitorAlarmJob(JobListener jobListener) {
        MonitorAlarmTask monitorAlarmTask = new MonitorAlarmTask();
        try {
            QuartzManager.addJob(GlobalConstants.MONITOR_ALARM_JOB_NAME, monitorAlarmTask, "0/8 * * * * ? "); //每2分钟执行一次
            JobKey jobKey = new JobKey(GlobalConstants.MONITOR_ALARM_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        MonitorAlarmLockTask monitorAlarmLockTask = new MonitorAlarmLockTask();
        try {
            QuartzManager.addJob(GlobalConstants.MONITOR_ALARM_LOCK_JOB_NAME, monitorAlarmLockTask, "0/35 * * * * ? "); //每35S执行一次
            JobKey jobKey = new JobKey(GlobalConstants.MONITOR_ALARM_LOCK_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    public static void startUploadMTBAJob(JobListener jobListener) {
        UploadMTBATask uploadMTBATask = new UploadMTBATask();
        try {
            QuartzManager.addJob(GlobalConstants.UPLOAD_MTBA_JOB_NAME, uploadMTBATask, "0 0/20 * * * ? "); //每2分钟执行一次
            JobKey jobKey = new JobKey(GlobalConstants.UPLOAD_MTBA_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }

    public static void startRefreshEquipStateJob(JobListener jobListener) {
        RefreshEquipStatusTask refreshEquipStatusTask = new RefreshEquipStatusTask();
        try {
            QuartzManager.addImmediateJob(GlobalConstants.REFRESH_EQUIPSTATE_JOB_NAME, refreshEquipStatusTask, "0/6 * * * * ? "); //每2分钟执行一次
            JobKey jobKey = new JobKey(GlobalConstants.REFRESH_EQUIPSTATE_JOB_NAME, QuartzManager.JOB_GROUP_NAME);
            QuartzManager.addJobListner(jobListener, jobKey);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }


}
