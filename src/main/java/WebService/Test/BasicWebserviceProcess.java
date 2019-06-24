package WebService.Test;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import java.util.HashMap;

public class BasicWebserviceProcess implements BaseWebservice {
    private String deviceTypeId;
    private String deviceCode;
    private String msgName;
    private static Logger logger = Logger.getLogger(BasicWebserviceProcess.class);

    private static Logger mqLogger = Logger.getLogger("mqLog");

    public String handle(String message) {
        try {
            HashMap maps = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);

            //MessageHandler messageHandler = (MessageHandler) SpringBeanFactoryUtil.getBean(Class.forName("cn.tfinfo.jcauto.octopus.messageHandlers." + mapMessage.getString("msgName") + "Handler"));
//            MessageHandler messageHandler;
            deviceTypeId = String.valueOf(maps.get("deviceTypeId"));
            deviceTypeId = deviceTypeId + "";
            deviceCode = String.valueOf(maps.get("deviceCode"));
            deviceCode = deviceCode + "";
            msgName = String.valueOf(maps.get("msgName"));
            msgName = msgName + "";
//            GlobalConstants.stage.getJTX_ServerLog().append("[" + GlobalConstants.dateFormat.format(new Date()) + "] 接收到mq消息\n");
//            DialogUtil.AutoNewLine( GlobalConstants.stage.getJTX_ServerLog());
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "接收到mq============================");
            mqLogger.info("接收到webservice==================================,deviceCode：" + deviceCode + "，msgName:" + msgName + "，deviceTypeId:" + deviceTypeId);
            if (isInvoke(deviceCode, deviceTypeId)) {
                logger.info("接收到webservice==================================" + deviceCode + "==deviceTypeId:" + deviceTypeId);
                logger.info("开始处理webservice请求...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        BaseWebservice baseWebservice;
                        try {
                            baseWebservice = (BaseWebservice) Class.forName("cn.tzinfo.htauto.octopus.common.WebService.impl." + String.valueOf(maps.get("msgName")).replaceAll("\"", "")+"Webservice").newInstance();
                            Object o = JSONObject.toJSON(maps);
                            baseWebservice.handle(o.toString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        }

                    }
                }).start();
                logger.info("MQ请求处理结束...");
            }
        } catch (Exception ex) {
            logger.error("Exception", ex);
            mqLogger.error("接收到mq==================================,Exception:" + deviceCode, ex);
            ex.printStackTrace();
        }
        return  message;
    }



    private boolean isInvoke(String deviceCode, String deviceTypeId) {

        try {
            if ("All".equals(deviceCode)) {
                return true;
            } else {
                if (!"".equals(deviceTypeId)) {
                    for (DeviceInfo deviceInfo : GlobalConstants.stage.deviceInfos) {
                        if (deviceInfo.getDeviceTypeId().equals(deviceTypeId)) {
                            return true;
                        }
                    }
                }
                if (!"".equals(deviceCode)) {
                    for (DeviceInfo deviceInfo : GlobalConstants.stage.deviceInfos) {
                        if (deviceInfo.getDeviceCode().equals(deviceCode)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception", e);
        }
        return false;
    }
}

