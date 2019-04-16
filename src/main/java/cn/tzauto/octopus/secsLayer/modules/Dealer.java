package cn.tzauto.octopus.secsLayer.modules;

import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.wrapper.ActiveWrapper;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.secsLayer.domain.Process;
import cn.tzauto.octopus.secsLayer.domain.ProcessFunction;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by luosy on 2019/4/3.
 */
public class Dealer {
    static Logger logger = Logger.getLogger(Dealer.class);

    public static Map deal(Process process, DataMsgMap dataMsgMap, ActiveWrapper activeWrapper) {
        List<ProcessFunction> processFunctions = process.getFunction();
        for (ProcessFunction processFunction : processFunctions) {
            String functionCode = processFunction.getFunctionCode();
            switch (functionCode) {
                case ProcessFunction.EDC_EC:
                case ProcessFunction.EDC_SV:

                case ProcessFunction.RCMD_PAUSE:
                case ProcessFunction.RCMD_STOP:
                case ProcessFunction.RCMD_TRML_MSG:
                case ProcessFunction.RCMD_START:
                case ProcessFunction.RCP_UPLOAD:
                case ProcessFunction.RCP_DOWNLOAD:
                case ProcessFunction.RCP_ANLY:

            }
        }
        return null;
    }

    public static JudgeResult deal(ProcessFunction processFunction, String deviceCode) {

        return null;
    }

    //todo 发送mq saveOplogAndSend2Server
    private static void saveOplogAndSend2Server(long ceid, String equipStatus, DeviceService deviceService, DeviceInfoExt deviceInfoExt) {
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(deviceInfoExt.getDeviceRowid());
        if (deviceOplogList == null || deviceOplogList.isEmpty()) {
            DeviceOplog deviceOplog = setDeviceOplog(ceid, deviceInfoExt.getRecipeName(), equipStatus, deviceInfoExt);
//            deviceService.saveDeviceOplog(deviceOplog);
            //发送设备状态变化记录到服务端
            if (!GlobalConstants.isLocalMode) {
                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                logger.info("发送设备" + deviceInfoExt.getDeviceRowid() + "实时状态至服务端");
            }
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送实时状态至服务端");
        } else {
            String formerDeviceStatus = deviceInfoExt.getDeviceStatus();
            if (!formerDeviceStatus.equals(equipStatus)) {
                DeviceOplog deviceOplog = setDeviceOplog(ceid, deviceInfoExt.getRecipeName(), equipStatus, deviceInfoExt);
                // deviceService.saveDeviceOplog(deviceOplog);
                //发送设备状态到服务端
                if (!GlobalConstants.isLocalMode) {
                    sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                    logger.info("发送设备" + deviceInfoExt.getDeviceRowid() + "实时状态至服务端");
                }
//               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送实时状态至服务端");
            }
        }
    }

    /**
     * 发送设备操作日志到服务端
     *
     * @param deviceInfoExt
     * @param deviceOplog
     */
    private static void sendDeviceInfoExtAndOplog2Server(DeviceInfoExt deviceInfoExt, DeviceOplog deviceOplog) {
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.EqptStatusChange");
        mqMap.put("deviceCode", deviceInfoExt.getDeviceRowid());
        mqMap.put("eventName", "eqpt.EqptStatusChange");
        mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
        mqMap.put("deviceCeid", deviceOplog.getDeviceCeid());
        mqMap.put("eventDesc", deviceOplog.getOpDesc());
        mqMap.put("eventDate", GlobalConstants.dateFormat.format(new Date()));
        mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
    }

    private static DeviceOplog setDeviceOplog(long ceid, String equipStatus, String formerDeviceStatus, DeviceInfoExt deviceInfoExt) {
        DeviceOplog deviceOplog = new DeviceOplog();
        deviceOplog.setId(UUID.randomUUID().toString());
        deviceOplog.setDeviceCode(deviceInfoExt.getDeviceRowid());
        deviceOplog.setCurrRecipeName(deviceInfoExt.getRecipeName());
        deviceOplog.setDeviceCeid(String.valueOf(ceid));
        deviceOplog.setCurrLotId(deviceInfoExt.getLotId());
        deviceOplog.setOpTime(new Date());
        deviceOplog.setOpDesc("机台状态从" + formerDeviceStatus + "切换为" + equipStatus);
        deviceOplog.setOpType("eqpt.EqptStatusChange");
        deviceOplog.setFormerDeviceStatus(formerDeviceStatus);
        deviceOplog.setCurrDeviceStatus(equipStatus);
        deviceOplog.setCreateBy("System");
        deviceOplog.setCreateDate(new Date());
        deviceOplog.setUpdateBy("System");
        deviceOplog.setUpdateDate(new Date());
        deviceOplog.setDelFlag("0");
        deviceOplog.setVerNo(0);
        return deviceOplog;
    }
}
