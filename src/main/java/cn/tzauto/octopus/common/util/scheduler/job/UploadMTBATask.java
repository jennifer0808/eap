/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.scheduler.job;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author luosy
 */
public class UploadMTBATask implements Job {

    private static final Logger logger = Logger.getLogger(UploadMTBATask.class);

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        ConcurrentHashMap<String, EquipModel> equipModels = GlobalConstants.stage.equipModels;
        if (equipModels != null && equipModels.size() > 0) {
            for (EquipModel equipModel : equipModels.values()) {
                if (equipModel.deviceType.contains("DFD6560")) {
                    if (equipModel.getPassport(40)) {
                        if (!GlobalConstants.isLocalMode) {
                            String mtba = equipModel.getMTBA();
                            Map mtbaMap = new HashMap();
                            mtbaMap.put("msgName", "UploadMTBA");
                            mtbaMap.put("deviceCode", equipModel.deviceCode);
                            mtbaMap.put("MTBA", mtba);
                            GlobalConstants.C2SSpecificDataQueue.sendMessage(mtbaMap);
                            logger.info("Send MTBA to server... deviceCode:" + equipModel.deviceCode + " mtba:" + mtba + "");
                        }
                    }
                    equipModel.returnPassport();
                }
            }
        }
    }
}
