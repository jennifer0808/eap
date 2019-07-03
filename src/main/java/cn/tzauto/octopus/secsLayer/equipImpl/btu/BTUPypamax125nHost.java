/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.btu;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import com.alibaba.fastjson.JSONArray;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class BTUPypamax125nHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(BTUPypamax125nHost.class);

    public BTUPypamax125nHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        lengthFormat= FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
    }

    public Object clone() {
        BTUPypamax125nHost newEquip = new BTUPypamax125nHost(deviceId,
                this.iPAddress,
                this.tCPPort, this.connectMode,
                this.deviceType, this.deviceCode);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.clear();
        return newEquip;
    }

    public void run() {
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? FengCeConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(FengCeConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    //重定义 机台的equipstatuschange事件报告
                    sendS2F37outAll();
                    sendS5F3out(true);
                    updateLotId();
//                    upLoadAllRcp();
                }

                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);

                }
            } catch (InterruptedException e) {
                logger.fatal("Caught Interruption", e);
            }
        }
    }
    @Override
    public  void processS6F11in(DataMsgMap msg){
        long ceid = 0l;
        try {
            ceid = (long) msg.get("CEID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 10060 || ceid == 10070 || ceid == 10080 || ceid == 10090 || ceid == 10100) {
            //TODO 获取状态变化的事件报告
            processS6F11EquipStatusChange(msg);
        } else if (ceid == 1010 || ceid == 1020 || ceid == 1030) {
            processS6F11EquipStatus(msg);
        } else if (ceid == 1040) {
            //TODO 切换recipe后获取事件报告
            sendS1F3Check();
        }
    }
    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = new Date().getTime();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                //回复掉消息
                replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                logger.info("Receive a s1f4 value,and will put in waitMsgValueMap===>" + JSONArray.toJSON(data));
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S6FX Code"> 


    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            Map panelMap = new HashMap();
            if (ceid == 1010) {
                panelMap.put("ControlState", FengCeConstant.CONTROL_LOCAL_ONLINE);//Online_Local
            }
            if (ceid == 1020) {
                panelMap.put("ControlState", FengCeConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
            }
            if (ceid == 1030) {
                panelMap.put("ControlState", FengCeConstant.CONTROL_OFFLINE);
            }
            changeEquipPanel(panelMap);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }




    //释放机台
    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        return null;
    }


    private void initRptPara() {
        sendS2F37outAll();
    }


    @Override
    public String checkPPExecName(String recipeName) {
        if (recipeName.equals(ppExecName)) {
            return "下载程序与当前程序一致，不需要下载！";
        }
        return "0";
    }

}
