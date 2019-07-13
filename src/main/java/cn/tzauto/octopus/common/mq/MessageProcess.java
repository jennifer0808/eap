package cn.tzauto.octopus.common.mq;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import java.io.IOException;


public class MessageProcess implements MessageHandler {

    private String deviceTypeId;
    private String deviceCode;
    private String msgName;
    private static Logger logger = Logger.getLogger(MessageProcess.class.getName());

    private static Logger mqLogger = Logger.getLogger("mqLog");

    @Override
    public void handle(final Message message) {
        try {
            final MapMessage mapMessage = (MapMessage) message;
            //MessageHandler messageHandler = (MessageHandler) SpringBeanFactoryUtil.getBean(Class.forName("cn.tfinfo.jcauto.octopus.messageHandlers." + mapMessage.getString("msgName") + "Handler"));
//            MessageHandler messageHandler;
            deviceTypeId = mapMessage.getString("deviceTypeId") + "";
            deviceCode = mapMessage.getString("deviceCode") + "";
            msgName = mapMessage.getString("msgName") + "";
//           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "接收到mq============================");
            mqLogger.info("接收到mq==================================,deviceCode：" + deviceCode + "，msgName:" + msgName + "，deviceTypeId:" + deviceTypeId);
            if (isInvoke(deviceCode, deviceTypeId)) {
                logger.info("接收到mq==================================" + deviceCode + "==deviceTypeId:" + deviceTypeId);
                logger.info("开始处理MQ请求...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MessageHandler messageHandler;
                        try {
                            messageHandler = (MessageHandler) Class.forName("cn.tzauto.octopus.common.mq.messageHandlers." + mapMessage.getString("msgName").replaceAll("\"", "") + "Handler").newInstance();
                            messageHandler.handle(message);
                        } catch (JMSException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (InstantiationException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (IllegalAccessException ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        } catch (T3TimeOutException e) {
                            e.printStackTrace();
                        }  catch (T6TimeOutException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (BrokenProtocolException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IntegrityException e) {
                            e.printStackTrace();
                        } catch (StateException e) {
                            e.printStackTrace();
                        } catch (InvalidDataException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
                logger.info("MQ请求处理结束...");
            }
        } catch (JMSException ex) {
            logger.error("Exception", ex);
            mqLogger.error("接收到mq==================================,Exception:" + deviceCode, ex);
            ex.printStackTrace();
        }
    }

    private boolean isInvoke(String deviceCode, String deviceTypeId) {

        try {
            if ("All".equals(deviceCode)) {
                return true;
            } else {
                if (!"".equals(deviceTypeId)) {
                    for (DeviceInfo deviceInfo : GlobalConstants.stage.hostManager.deviceInfos) {
                        if (deviceInfo.getDeviceTypeId().equals(deviceTypeId)) {
                            return true;
                        }
                    }
                }
                if (!"".equals(deviceCode)) {
                    for (DeviceInfo deviceInfo : GlobalConstants.stage.hostManager.deviceInfos) {
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
