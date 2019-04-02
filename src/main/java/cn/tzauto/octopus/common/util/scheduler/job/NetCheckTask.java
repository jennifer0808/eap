/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.secsLayer.util.DeviceComm;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * @author rain
 */
public class NetCheckTask implements Job {

    private static final Logger logger = Logger.getLogger(NetCheckTask.class);

    @Override
    public void execute(JobExecutionContext jec) {
        logger.info("NetCheckTask任务执行....");
        try {
            for (EquipHost equipHost : GlobalConstants.stage.equipHosts.values()) {
                EquipNodeBean src = null;
                for (EquipNodeBean equipNodeBean : GlobalConstants.stage.equipBeans) {
                    if (equipNodeBean.getDeviceIdProperty() == equipHost.getDeviceId()) {
                        src = equipNodeBean;
                        break;
                    }
                }
                if (src != null) {
                    if (src.getEquipPanelProperty().getNetState() == 1) {
                        logger.info(equipHost.iPAddress + "暂未发现网络异常+++++++++++++++++++++++++++");
                        checkNet(equipHost);
                        if (GlobalConstants.restartMap.containsKey(equipHost.getDeviceId())) {
                            GlobalConstants.restartMap.remove(equipHost.getDeviceId());
                        }
                    } else {
                        logger.info(equipHost.iPAddress + "断网啦，正在尝试重连！！+++++++++++++++++++++++++++");
                        if (!GlobalConstants.restartMap.containsKey(equipHost.getDeviceId())) {
                            DeviceComm.restartHost(src);
                            GlobalConstants.restartMap.put(equipHost.getDeviceId(), Boolean.TRUE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }

    }

    public void checkNet(EquipHost equipHost) {
        try {
            Runtime runtime = Runtime.getRuntime();
            boolean errorFlag = false;
            String tracertString = "ping " + equipHost.iPAddress;
            Process process = runtime.exec(tracertString);
            process.waitFor();
            InputStream inputStream = process.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("GBK"));
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String len = bufferedReader.readLine();
            while (len != null) {
                logger.debug(len);
                if (len.equals("")) {
                    len = bufferedReader.readLine();
                } else {
                    if ((len.contains("请求超时")) || (len.contains("无法访问目标主机")) || (len.contains("一般故障"))) {
                        errorFlag = true;
                    }
                    len = bufferedReader.readLine();
                }
            }
            logger.debug(errorFlag);
            if (errorFlag) {
                logger.debug("NetCheckTask++++++++++++++++++++++++" + equipHost.getDeviceId());
                int times = 0;
                if (GlobalConstants.netCheckTimeMap.containsKey(equipHost.getDeviceId())) {
                    times = GlobalConstants.netCheckTimeMap.get(equipHost.getDeviceId()) + 1;
                    GlobalConstants.netCheckTimeMap.remove(equipHost.getDeviceId());
                } else {
                    times = 1;
                }
                logger.debug("Times++++++++++++++++++++++++" + times);
                GlobalConstants.netCheckTimeMap.put(equipHost.getDeviceId(), times);
                if (times >= 3) {
                    logger.debug("NetCheckTask++++++++++++++++++++++++断网3次，开始hold批次");
                    Map map = new HashMap();
                    map.put("NetState", 0);//0为断网
                    equipHost.changeEquipPanel(map);
                    GlobalConstants.netCheckTimeMap.remove(equipHost.getDeviceId());
                }
            }
            process.destroy();
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }
}
