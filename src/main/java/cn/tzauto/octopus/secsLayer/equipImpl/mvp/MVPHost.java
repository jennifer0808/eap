/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.equipImpl.mvp;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.generalDriver.entity.msg.SecsFormatValue;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author njtz
 */
@SuppressWarnings("serial")
public class MVPHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(MVPHost.class.getName());

    public MVPHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        StripMapUpCeid = 3014L;
    }


    @Override
    public Object clone() {
        MVPHost newEquip = new MVPHost(deviceId,
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
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (this.getControlState() == null ? GlobalConstant.CONTROL_REMOTE_ONLINE != null : !this.getControlState().equals(GlobalConstant.CONTROL_REMOTE_ONLINE)) {
                    sendS1F1out();
                    //获取设备开机状态                   
                    super.findDeviceRecipe();
                    //重定义 机台的equipstatuschange事件报告
                    initRptPara();
                    updateLotId();
//                    upLoadAllRcp();
                }
//                sendS2f33outDelete(3014);
//                sendS2F35outDelete(3014,3014);
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

    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = System.currentTimeMillis();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
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

    public void processS6F11in(DataMsgMap data) {
        long ceid = -1L;
        try {
            ceid = (long) data.get("CEID");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (ceid == 3002) {
            //获取状态变化的事件报告
            replyS6F12WithACK(data, (byte) 0);
            processS6F11EquipStatusChange(data);
        } else if (ceid == 3006 || ceid == 3007) {
            replyS6F12WithACK(data, (byte) 0);
            processS6F11EquipStatus(data);
        } else if (ceid == 3008 || ceid == 3009 || ceid == 3010 || ceid == 3011 || ceid == 3012 || ceid == 3013) {
            //切换recipe后获取事件报告
            replyS6F12WithACK(data, (byte) 0);
            findDeviceRecipe();
//                sendS1F3Check();
        } else if (ceid == StripMapUpCeid) {
            logger.info("----Received from Equip Strip Map Upload event - S6F11");
            long result = -1L;
            try {
                result = (long) ((ArrayList) ((ArrayList) data.get("REPORT")).get(1)).get(1);
            } catch (Exception e) {

            }
            // 判断机台检测结果，如果为0则上传，结果为1 || 2则不上传
            if (result == 0 || result == -1L) {
                processS6F11inStripMapUpload(data);
            } else {
                replyS6F12WithACK(data, (byte) 0);
                logger.info("检测结果为:" + result + ",不上传mapping!");
            }
        }


    }

    protected void processS6F11EquipStatus(DataMsgMap data) {
        long ceid = 0l;
        try {
            ceid = (long) data.get("CEID");
            Map panelMap = new HashMap();
            if (ceid == 3006) {
                panelMap.put("ControlState", GlobalConstant.CONTROL_LOCAL_ONLINE);       //Online_Local
            }
            if (ceid == 3007) {
                panelMap.put("ControlState", GlobalConstant.CONTROL_REMOTE_ONLINE);//Online_Remote}
            }
            changeEquipPanel(panelMap);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand">
//    @Override
//    public Map holdDevice() {
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceService deviceService = new DeviceService(sqlSession);
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//        sqlSession.close();
//        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//            Map map = this.sendS2f41Cmd("STOP");//Map map = this.sendS2f41Cmd("LOCK");
//            if ((byte) map.get("HCACK") == 0 || (byte) map.get("HCACK") == 4) {
//                this.setAlarmState(2);
//            }
//            return map;
//        } else {
//           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
//            return null;
//        }
//    }

    //释放机台
    @Override
    public Map releaseDevice() {
        this.setAlarmState(0);
        return null;
    }

//    @Override
//    public Map startDevice() {
//        DataMsgMap data = null;
//        try {
//            this.sendS2f41Cmd("START");
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//        }
//        byte hcack = (byte) data.get("HCACK");
//        Map resultMap = new HashMap();
//        resultMap.put("msgType", "s2f42");
//        resultMap.put("deviceCode", deviceCode);
//        resultMap.put("HCACK", hcack);
//        return resultMap;
//    }
    // </editor-fold>

    private void initRptPara() {
        sendS2F33clear();
        sendS2F35clear();
        //重新定义机台UpLoadStripMapping事件
//        sendS2f33outMulti(3014L, 1037L, 1023L);
        List list1 = new ArrayList();
        list1.add(1037L);
        list1.add(1023L);
        sendS2F33out(3014L, 3014L, list1);
        sendS2F35out(3014L, 3014L, 3014L);
        sendS2F37out(3014L);
        logger.info("If the device software upgrades, you need to redefine the ceid=3043 event!！");
        List list = new ArrayList();
        list.add(1037L);
        sendS2F33out(3043L, 3043L, list);
        sendS2F35out(3043L, 3043L, 3043L);
        sendS2F37out(3043L);
//        sendS2F37outAll();
    }

    public void sendS2F33out(long dataid, long reportId, List svidList) {
        // TODO: 2019/4/13  在s2f33out中svFormat和rptFormat为U2，s2f35中为U4
        try {
            short format = SecsFormatValue.SECS_2BYTE_UNSIGNED_INTEGER;
            activeWrapper.sendS2F33out(dataid, format, reportId, format, svidList, format);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

}
