package cn.tzauto.octopus.gui;

import cn.tzauto.generalDriver.api.EqpEventDealer;
import cn.tzauto.generalDriver.entity.msg.MessageHeader;
import cn.tzauto.generalDriver.exceptions.InvalidHsmsDataLengthException;
import cn.tzauto.generalDriver.exceptions.InvalidHsmsHeaderDataException;
import cn.tzauto.octopus.gui.equipevent.CommFailureEvent;
import cn.tzauto.octopus.gui.equipevent.CommStatusEvent;
import cn.tzauto.octopus.gui.equipevent.ControlEvent;
import cn.tzauto.octopus.gui.equipevent.ReceivedSeparateEvent;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.gui.main.EapClient;
import cn.tzauto.octopus.secsLayer.domain.EquipNodeBean;
import cn.tzauto.octopus.secsLayer.domain.EquipState;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.util.DeviceComm;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class EquipmentEventDealer extends SwingWorker<Object, EquipState>
        implements EqpEventDealer {

    private EquipNodeBean equipNodeBean;
    private LinkedBlockingQueue<ControlEvent> eventQueue;
    //(1) CommStatusEvent; (2) ReceivedSeparateEvent (3) CommFailureEvent (4) ServiceStatusEvent
    //(5) BehaviorStatusEvent
    private int sync = 0;
    private static final Logger logger = Logger.getLogger(EquipmentEventDealer.class.getName());
    private static boolean hostIsShutDown = false;
    private EapClient stage;

    public static boolean isHostIsShutDown() {
        return hostIsShutDown;
    }

    public static void setHostIsShutDown(boolean hostIsShutDown) {
        EquipmentEventDealer.hostIsShutDown = hostIsShutDown;
    }

    public EquipmentEventDealer(EquipNodeBean eNodeBean, EapClient stage) {
        equipNodeBean = eNodeBean;
        eventQueue = new LinkedBlockingQueue<>();
        this.stage = stage;
        hostIsShutDown = false;
    }

    /*
     * The execute() method must be called before start the lower level protovols,
     * In other words, the doInBackground method must be executed before start SECS and
     * run EquipHost Thread. And this Worker must be cancel(true) after SECS and EquipHost
     * has been stopped.
     */
    @Override
    public Object doInBackground() {
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.equipNodeBean.getDeviceCode());
        try {
            while (!this.isCancelled()) {
                ControlEvent ev = null;
                ev = this.eventQueue.take();
                //释放锁，处理页面显示逻辑，让其他获取状态变化的作业继续，
                while (sync > 0) {
//                    logger.info("Progress Number syc = " + sync);
                    Thread.yield();
                }
                EquipState newState = (EquipState) equipNodeBean.getEquipStateProperty().clone();
                MultipleEquipHostManager hostsManager = stage.hostManager;
                if (ev instanceof CommStatusEvent) {
                    CommStatusEvent cev = (CommStatusEvent) ev;
                    logger.info("Equip State is changed. publish is called==>" + JSON.toJSONString(ev));
                    newState.setCommOn(cev.isComm());
                    //如果是通信异常事件，那么需要重启线程
                    if (!cev.isComm()) {
//                        logger.info("Catch CommStatusEvent false status..and ready to restart secs.... ");
//                        logger.debug("start to shutdown secs;==>" + this.equipNodeBean.getDeviceIdProperty());
                        newState.transitServiceState(EquipState.OUT_OF_SERVICE_STATE);
//                        hostsManager.terminateSECS(this.equipNodeBean.getDeviceIdProperty());
                    }
                    sync++;
                    publish(newState);
                    if (cev.isComm()) {
                        hostsManager.notifyHostOfJsipReady(this.equipNodeBean.getDeviceIdProperty());
                    }
                    logger.info("Equip State is changed. publish is called");
                } else if (ev instanceof ReceivedSeparateEvent) {
                    logger.info("Received Separate event, SECS prototcols has been terminated.");
                    newState.setCommOn(false);
                    newState.transitServiceState(EquipState.OUT_OF_SERVICE_STATE);
                    sync++;
                    publish(newState);
                    //terminate the host server                 
//                    Thread.yield();
                    logger.info("Ready to terminate host comm thread ...");
//                    hostsManager.terminateSECS(this.equipNodeBean.getDeviceIdProperty());
//                    hostsManager.terminateHostThread(this.equipNodeBean.getDeviceIdProperty());
                    DeviceComm.restartHost(equipNodeBean);
                    logger.info("terminate host comm thread done...");
                    continue;
//                    break;
                } else if (ev instanceof CommFailureEvent) {
                    logger.info("Communication Failure occured, SECS prototcols has been terminated.");
                    hostsManager.terminateSECS(this.equipNodeBean.getDeviceIdProperty());
                    newState.setCommOn(false);
                    newState.transitServiceState(EquipState.OUT_OF_SERVICE_STATE);
                    sync++;
                    publish(newState);
                    //terminate the host server
//                    Thread.yield();
                    //this.eventQueue.put(new EquipmentHoldEvent(true, this.equipNodeBean.getDeviceIdProperty())); 
                    hostsManager.terminateHostThread(this.equipNodeBean.getDeviceIdProperty());
//                    break;
                    continue;
                }
            }
        } catch (InterruptedException e) {

            e.printStackTrace();
            logger.info("Caught Interruption", e);
            //Thread.currentThread().interrupt();
            //shall we add GUI house keeping here???????????????????????

        }
        return null;
    }

    @Override
    protected void process(List<EquipState> equipStates) {
        for (EquipState eState : equipStates) {
            equipNodeBean.setEquipStateProperty(eState);
            sync--;
        }
    }

    @Override
    protected void done() {
        eventQueue.clear();
        eventQueue = null;
        stage = null;
        //equipNodeBean = null;
    }

    public void exit() {
        this.cancel(true);
        //it will throw an interrupted exception into the doBackground() method
    }

    /*
     * The following block is from UpLevelAnouncer interface
     **/
    @Override
    public void notificationOfSecsDriverReady(int deviceId) {
        logger.info("notificationOfJsipReady Invoked at device id " + deviceId + " equip name "
                + equipNodeBean.getDeviceCode());
       UiLogUtil.getInstance().appendLog2EventTab(equipNodeBean.getDeviceCode(), "SECS连接正常启动...");
        eventQueue.add(new CommStatusEvent(true, deviceId));
        stage.equipHosts.get(equipNodeBean.getDeviceCode()).setSdrReady(true);
        stage.equipHosts.get(equipNodeBean.getDeviceCode()).setIsRestarting(false);
        stage.equipHosts.get(equipNodeBean.getDeviceCode()).restartCnt = 0;

    }

    @Override
    public void notificationOfHsmsReceivedSeparate(int deviceId) {
        logger.info("notificationOfHsmsReceivedSeparate Invoked at device id " + deviceId + " equip name "
                + equipNodeBean.getDeviceCode());
        eventQueue.add(new ReceivedSeparateEvent(deviceId));
    }


    public void notificationOfSentPrimaryMessage(int deviceId, long transId, String msgTagName) {
        logger.debug("notificationOfSentPrimaryMessage Invoked at device id " + deviceId + " " + msgTagName);
    }


    public void notificationOfSentSecondaryMessage(int deviceId, long transId, String msgTagName) {
        logger.debug("notificationOfSentSecondaryMessage Invoked at device id " + deviceId + " " + msgTagName);
//        eventQueue.add(new ServiceStatusEvent(true, deviceId)); //to be changed
//       eventQueue.add(new BehaviorStatusEvent(EquipState.ACTIVE_ACTIVE_SERVICE_STATE, deviceId));
    }

    @Override
    public void notificationOfT3Timeout(int deviceId, long transId, String msgTagName) {
        logger.debug("notificationOfT3Timeout Invoked at device id " + deviceId + " " + msgTagName
                + " at equip " + equipNodeBean.getDeviceCode());
    }

    @Override
    public void notificationOfSentMessage(int i, long l, String s) {

    }

    @Override
    public void notificationOfRespondMessage(int i, long l, String s) {

    }

    @Override
    public void notificationOfHsmsRequestSendSeparate(int deviceId) {
        logger.info("notificationOfHsmsRequestSendSeparate Invoked at device id " + deviceId + " equip name "
                + equipNodeBean.getDeviceCode());
        eventQueue.add(new CommStatusEvent(false, deviceId));
    }

    @Override
    public void notificationOfCloseNetwork(int deviceId) {
        logger.info("notificationOfJsipCloseNetwork Invoked at device id " + deviceId + " equip name "
                + equipNodeBean.getDeviceCode());
//        eventQueue.add(new CommStatusEvent(false, deviceId));
        eventQueue.add(new CommFailureEvent(null, deviceId));

    }

    @Override
    public void notificationOfSentMessageFailed(int i, long l, String s) {

    }

    @Override
    public void notificationOfSentMessageFailedCommFailure(int i, long l, String s) {

    }

    @Override
    public void notificationOfRespondMessageFailed(int i, long l, String s) {

    }


    public void notificationOfSentPrimaryMessageFailed(int deviceId, long transId, String msgTagName) {
        logger.debug("notificationOfSentPrimaryMessageFailed Invoked at device id " + deviceId + " "
                + msgTagName + " with transId = " + transId);
    }


    public void notificationOfSentPrimaryMessageFailedCommFailure(int deviceId, long transId, String msgTagName) {
        logger.debug("notificationOfSentPrimaryMessageFailedCommFailure Invoked at device id " + deviceId + " "
                + msgTagName + " with transId = " + transId);
    }


    public void notificationOfSentSecondaryMessageFailed(int deviceId, long transId, String msgTagName) {
        logger.debug("notificationOfSentSecondaryMessageFailed Invoked at device id " + deviceId + " "
                + msgTagName + " with transId = " + transId);
    }

    /*
     * The following block is from HsmsCommunicationFailureDealer interface
     **/
    @Override
    public void processDataReadIOException(IOException e, int deviceId) {
        logger.debug("Communication Failure occured: "
                + "DataReadIOException with device id = " + deviceId + ".", e);
        eventQueue.add(new CommFailureEvent(e, deviceId));
    }


    public void processDataSendIOException(IOException e, int deviceId) {
        logger.debug("Communication Failure occured: "
                + "DataSendIOException with device id = " + deviceId + ".", e);
        eventQueue.add(new CommFailureEvent(e, deviceId));
    }

    @Override
    public void processInvalidHeaderDataException(InvalidHsmsHeaderDataException e, int deviceId) {
        logger.debug("Communication Failure occured:InvalidHeaderDataException with device id = " + deviceId + ".", e);
        eventQueue.add(new CommFailureEvent(e, deviceId));
    }

    @Override
    public void processLinktestRequestT6Timeout(int deviceId) {
        logger.debug("Linktest T6 Timeout: Communication Failure occured "
                + " with device id = " + deviceId + ".");
        eventQueue.add(new CommFailureEvent(null, deviceId));
    }

    @Override
    public void processDataSendException(Exception e, int i) {

    }





    @Override
    public void processWrongMessageLengthException(InvalidHsmsDataLengthException e, int deviceId) {
        logger.debug("Communication Failure occured:  "
                + "WrongMessageLengthException with device id = " + deviceId + ".", e);
        eventQueue.add(new CommFailureEvent(e, deviceId));
    }


    public void processHeartBeatingFailuret(int deviceId) {
        logger.debug("Communication Failure occured:  "
                + "Heart Beating Failure with device id = " + deviceId + ".");
        eventQueue.add(new CommFailureEvent(null, deviceId));
    }

    /*
     * The following block is from ErrorDataMessageDealer interface
     **/

    public void processDataMessageOfWrongDevId(MessageHeader header) {
        logger.info("Received a Data Message Of Wrong DevId." + " msg info " + header);
    }

    /*
     * (non-Javadoc)
     * Send S9F7 out.
     */

    public void processMessageOfMistakeDataFormat(byte[] headerBytes) {
        logger.info("Received a Data Message Of Mistake Data Format.");
    }

    /*
     * (non-Javadoc)
     * Send S9F5 out.
     */

    public void processMessageOfWrongFunctionType(byte[] headerBytes) {
        logger.info("Received a Data Message Of  Wrong Function Type.");
    }

    /*
     * (non-Javadoc)
     * Send S9F3 out.
     */

    public void processMessageOfWrongStreamType(byte[] headerBytes) {
        logger.info("Received a Data Message Of  Wrong Stream Type.");
    }

    /*
     * (non-Javadoc)
     * Send S9F9 out.
     */

    public void processT3Timeout(MessageHeader header) {
    }

    /*
     * Send S9F11
     */

    public void processDataMessageOfTooLongData(byte[] headerBytes) {
        logger.info("Received a Data Message Of Too Long Data.");
    }

}
