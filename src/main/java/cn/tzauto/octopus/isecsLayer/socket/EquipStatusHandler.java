/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.socket;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.slf4j.MDC;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author luosy
 */
public class EquipStatusHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(EquipStatusHandler.class);
    static String equipstatus = "--";
    static String preEquipstatus = "--";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        String msgCtx = msg.toString();
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];

        buf.readBytes(req);

        String message = new String(req, "UTF-8");
        if(message.startsWith("alarm")) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (message.contains("done")) {
            Map<String, String> map = new HashMap();
            for (Map.Entry<String, EquipModel> equipmodelEntry : GlobalConstants.stage.equipModels.entrySet()) {
                EquipModel equipModel = equipmodelEntry.getValue();
                ConcurrentLinkedQueue<ISecsHost> isecsHosts = equipModel.iSecsHostList;
                for (ISecsHost isecsHost : isecsHosts) {
                    map.put(isecsHost.ip, isecsHost.deviceCode);
                }
            }
            String eqpIp = ctx.channel().remoteAddress().toString().split(":")[0].replaceAll("/", "");
            String deviceCode = map.get(eqpIp);
            MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, deviceCode);
            logger.debug("接收到设备:" + deviceCode + "的alert信息:" + message);
            if (deviceCode.contains("-S")) {
                deviceCode = deviceCode.replace("-S", "");
            }
            String[] messages = message.replaceAll("alert", "").replaceAll("done", "").split(";");
            String status = transferStatus(messages[0]).replace("\r\n", "");
            String prestatus = "";
            if (messages.length > 1) {
                prestatus = transferStatus(messages[1]);
            }
            logger.debug("设备:" + deviceCode + "设备进入" + status + "状态.");
//        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备进入" + status + "状态...");
            Map statusmap = new HashMap();
            statusmap.put("EquipStatus", status);
            EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceCode);
            if (equipModel != null) {
                equipModel.getCurrentRecipeName();
                statusmap.put("PPExecName", equipModel.ppExecName);
                equipModel.changeEquipPanel(statusmap);
//                //由于start按钮没有之前状态，对之前状态做特殊处理,
                if("".equals(prestatus)){
                    if(!"START".equalsIgnoreCase(equipModel.curEquipStatus)){
                        //第一次按start时，将上时刻的当前状态，设成此时的之前状态
                        equipModel.preEquipStatus = equipModel.curEquipStatus;
                    }
                    //第一次按start之后，此时的之前状态不变
                }else{
                    //OCR ALERT正常处理
                    equipModel.preEquipStatus = prestatus.trim();
                }
//                equipModel.preEquipStatus = prestatus.trim();
                equipModel.equipStatus = status.trim();
                equipModel.curEquipStatus = status.trim();
                preEquipstatus = prestatus.trim();
                equipstatus = status.trim();
                logger.debug("设备:" + deviceCode + " PreEquipstatus: " + preEquipstatus + " Equipstatus: " + equipstatus);
                if ("pause".equalsIgnoreCase(preEquipstatus) && "RUN".equalsIgnoreCase(equipstatus)) {
                    if (!AxisUtility.isEngineerMode(deviceCode)) {
                        if (equipModel.checkLockFlagFromServerByWS(deviceCode)) {
                            String stopResult = equipModel.pauseEquip();
                            UiLogUtil.getInstance().getInstance().appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
                        }
                    } else {
                        logger.info("设备:" + deviceCode + "处于工程模式,取消开机检查.");
                    }
                }
//            if("RUN".equalsIgnoreCase(preEquipstatus)){
//                //标记HT7045开机check通过后默认状态已结束，下次开机需要重新check;
//                equipModel.setStartCheckOver1st(false);
//                logger.info("设备:" + deviceCode + "第一次锁机标识："+equipModel.isStartCheckOver1st());
//            }
                if ("START".equalsIgnoreCase(equipstatus)) {
                    logger.info("设备:" + deviceCode + "START开机作业.");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备请求开始作业...");
                    if (!AxisUtility.isEngineerMode(deviceCode) ) {
                        if (!GlobalConstants.stage.equipModels.get(deviceCode).startCheck()) {
//                        String stopResult = GlobalConstants.eapView.equipModels.get(deviceCode).stopEquip();
                            String stopResult = "0";
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将被锁机...");
                            String holdDesc = "";
                            Map mqMap = new HashMap();
                            if ("0".equals(stopResult)) {
                                holdDesc = "当前设备已经被锁机";
                                Map mapTmp = new HashMap();
                                mapTmp.put("EquipStatus", "Idle");
                                equipModel.changeEquipPanel(mapTmp);
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "锁机成功...");
                                mqMap.put("holdResult", "锁机成功");
                            } else {
                                mqMap.put("holdResult", "锁机失败");
                                holdDesc = stopResult;
                            }
                            mqMap.put("holdDesc", holdDesc);
                            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
                        }
                    } else {
                        GlobalConstants.stage.equipModels.get(deviceCode).startEquip();
                        logger.info("设备:" + deviceCode + "处于工程模式,取消开机检查.");
                    }
                } else if ((preEquipstatus.contains("EADY") || preEquipstatus.contains("DLE")) && "RUN".equalsIgnoreCase(equipstatus)) {
                    logger.info("设备:" + deviceCode + "开机作业.");
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备进入运行状态...");
                    if (!AxisUtility.isEngineerMode(deviceCode)) {
                        if (!GlobalConstants.stage.equipModels.get(deviceCode).startCheck()) {
                            String stopResult = GlobalConstants.stage.equipModels.get(deviceCode).stopEquip();
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备将被锁机...");
                            String holdDesc = "";
                            Map mqMap = new HashMap();
                            if ("0".equals(stopResult)) {
                                holdDesc = "当前设备已经被锁机";
                                Map mapTmp = new HashMap();
                                mapTmp.put("EquipStatus", "Idle");
                                equipModel.changeEquipPanel(mapTmp);
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "锁机成功...");
                                mqMap.put("holdResult", "锁机成功");
                            } else {
                                mqMap.put("holdResult", "锁机失败");
                                holdDesc = stopResult;
                            }
                            mqMap.put("holdDesc", holdDesc);
                            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
                        }
                    } else {
                        logger.info("设备:" + deviceCode + "处于工程模式,取消开机检查.");
                    }
                }
                sendDeviceInfoExtAndOplog2Server(equipModel, preEquipstatus);
            }
        }
        //System.out.println("Netty-Server:Receive Message:" + message);
//        System.out.println("cn.tzauto.octopus.isecsLayer.socket.TimeServerHandler.channelRead()" + msgCtx);
    }

    private static String transferStatus(String status) {
//        if (status.contains("START") || status.contains("tart") || status.contains("WORK") || status.contains("ork") || status.contains("un") || status.contains("RUN")) {
//            status = "RUN";
//        }
        if (status.contains("WORK") || status.contains("ork") || status.contains("un") || status.contains("RUN")) {
            status = "RUN";
        }
        if (status.contains("IDLE") || status.contains("dle")) {
            status = "IDLE";
        }
        if (status.contains("READY") || status.contains("eady")) {
            status = "READY";
        }
        if (status.contains("STOP") || status.contains("top")) {
            status = "STOP";
        }
        if (status.contains("ause")) {
            status = "PAUSE";
        }
        return status;
    }

    private static void sendDeviceInfoExtAndOplog2Server(EquipModel equipModel, String preEquipstatus) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(equipModel.deviceCode);
        if (deviceInfoExt == null) {
            logger.error("未配置:" + equipModel.deviceCode + "的模型信息.");
            return;
        }
        deviceInfoExt.setDeviceStatus(equipModel.equipStatus);
        DeviceOplog deviceOplog = new DeviceOplog();
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(equipModel.deviceCode);
        if (deviceOplogList == null || deviceOplogList.isEmpty()) {
            deviceOplog = equipModel.setDeviceOplog(preEquipstatus, deviceInfoExt.getLotId());
            //deviceService.saveDeviceOplog(deviceOplog);
        } else {
            String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
            if (!formerDeviceStatus.equals(equipModel.equipStatus)) {
                deviceOplog = equipModel.setDeviceOplog(preEquipstatus, deviceInfoExt.getLotId());
                //deviceService.saveDeviceOplog(deviceOplog);
            }
        }
        sqlSession.close();
        if (!GlobalConstants.isLocalMode) {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.EqptStatusChange");
            mqMap.put("deviceCode", equipModel.deviceCode);
            mqMap.put("eventName", "eqpt.EqptStatusChange");
            mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
            mqMap.put("deviceCeid", "0");
            mqMap.put("eventDesc", deviceOplog.getOpDesc());
            mqMap.put("eventDate", GlobalConstants.dateFormat.format(new Date()));
            mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        }
    }


    public static void main(String[] args) {
        String s = "statr" +
                "";
        System.out.println(s.replace("\n\r", ""));
    }
}
