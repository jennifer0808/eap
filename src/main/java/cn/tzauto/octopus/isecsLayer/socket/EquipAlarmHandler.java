/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.socket;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 陈佳能
 */
public class EquipAlarmHandler extends ChannelInboundHandlerAdapter {

    //    private static final Logger logger = Logger.getLogger(EquipAlarmHandler.class);
    private static final Logger logger = Logger.getLogger("EquipAlarmHandler");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String message = new String(req, "UTF-8");
        logger.info("alarm message =====> " + message);

        //get device code
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
        DeviceInfo deviceInfo = GlobalConstants.stage.hostManager.getDeviceInfo(null, deviceCode);

        List<String> alarmStringList = new ArrayList<>();
        alarmStringList.add(message);

        List<AlarmRecord> alarmRecordList = setAlarmRecord(deviceInfo, alarmStringList);

        if (!GlobalConstants.isLocalMode && alarmRecordList.size() != 0) {
            //实时发送alarm记录至服务端
            Map alarmRecordMap = new HashMap();
            alarmRecordMap.put("msgName", "ArAlarmRecord");
            alarmRecordMap.put("deviceCode", deviceCode);
            alarmRecordMap.put("alarmRecord", JsonMapper.toJsonString(alarmRecordList));
            alarmRecordMap.put("alarmDate", GlobalConstants.dateFormat.format(new Date()));
            GlobalConstants.C2SAlarmQueue.sendMessage(alarmRecordMap);
            logger.info("Send alarmRecords to server..." + alarmRecordMap.toString());
        }
    }

    private List<AlarmRecord> setAlarmRecord(DeviceInfo deviceInfo, List<String> alarmStringList) {
        List<AlarmRecord> alarmRecords = new ArrayList<>();
        if (alarmStringList != null && alarmStringList.size() > 0) {
            for (String str : alarmStringList) {
                String alarm = "";
                String remark = "";
                switch (deviceInfo.getDeviceType()) { //添加机台类型即添加case处理str
                    case "SCREEN-LEDIA":
                        if (str.contains(",")) {
                            String[] alarmstrs = str.split(",");
                            alarm = alarmstrs[3];
                            remark = alarmstrs[2];
                        } else {
                            alarm = str;
                        }
                        break;
                    case "HITACHI-LaserDrill":
                        if (str.contains("._PC")) {
                            if (str.contains("Alarm =")) {
                                String[] HITACHILaserDrillalarms = str.split(",");
                                alarm = HITACHILaserDrillalarms[1].replace("Alarm =", "").trim();
                                logger.info("收到报警信息:时间:" + HITACHILaserDrillalarms[0] + "报警:" + alarm);
                                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "收到报警信息:时间:" + HITACHILaserDrillalarms[0] + "报警:" + alarm);
                            }
                            if (str.contains("AlarmClear =")) {
                                String[] HITACHILaserDrillalarms = str.split(",");
                                alarm = HITACHILaserDrillalarms[1].replace("AlarmClear =", "").trim();
                                logger.info("收到消警信息:时间:" + HITACHILaserDrillalarms[0] + "报警:" + alarm);
                                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "收到消警信息:时间:" + HITACHILaserDrillalarms[0] + "报警:" + alarm);
                            }
                            String[] keys = String.valueOf(GlobalConstants.getProperty("HITACHI_LASER_DRILL_FILE_KEYS")).split(",");
                            for (String key : keys) {
                                if (str.contains(key)) {
                                    logger.info("HITACHILaserDrill " + str);
                                }
                            }
                        }
                        if (str.contains(".UVP")) {
                            if (str.contains("Date")) {
                                logger.info("power check data:Date:" + str.split(",")[1]);
//                            ((HitachiLaserDrillHost) GlobalConstants.eapView.equipModels.get(deviceInfo.getDeviceCode())).setPCHK(str.split(",")[1], null, null, null);
                            }
                            if (str.contains("Axis")) {
                                logger.info("power check data:Axis:" + str.split(",")[1]);
                            }
                            if (str.contains("AP_No")) {
                                logger.info("power check data:AP_No:" + str.split(",")[1]);
                            }
                            if (str.contains("Power")) {
                                logger.info("power check data:Power:" + str.split(",")[1]);
                            }
                            if (str.contains("Crystal No.")) {
                                logger.info("power check data:CrystalNo:" + str.split(",")[1]);
                            }
                            if (str.contains("Crystal_Time")) {
                                logger.info("power check data:Crystal_Time:" + str.split(",")[1]);
                            }
                        }
                        if (str.contains(".GLV")) {
                            if (str.contains("RMAX_")) {
                                logger.info("power check data:Crystal_Time:" + str);
                                accuracyCheck(deviceInfo.getDeviceCode(), str);
                            }
                        }
                        break;
                    default:
                        alarm = str;
                        break;
                }
                AlarmRecord alarmRecord = new AlarmRecord();
                String id = UUID.randomUUID().toString();
                alarmRecord.setId(id);
                alarmRecord.setClientCode(GlobalConstants.clientInfo.getClientCode());
                alarmRecord.setClientId(GlobalConstants.clientInfo.getId());
                alarmRecord.setClientName(GlobalConstants.clientInfo.getClientName());
                alarmRecord.setDeviceId(deviceInfo.getId());
                alarmRecord.setDeviceCode(deviceInfo.getDeviceCode());
                alarmRecord.setDevcieName(deviceInfo.getDeviceName());
                alarmRecord.setAlarmId(alarm);
                alarmRecord.setAlarmCode("");
                alarmRecord.setAlarmName(remark);
                alarmRecord.setAlarmDate(new Date());
                alarmRecord.setDeviceTypeCode(deviceInfo.getDeviceType());
                alarmRecord.setDeviceTypeId(deviceInfo.getDeviceTypeId());
                alarmRecord.setRepeatFlag("N");
                alarmRecord.setStepCode("");
                alarmRecord.setStepId("");
                alarmRecord.setStepName("");
                alarmRecord.setVerNo(0);
                alarmRecord.setRemarks(remark);//BTU alarm值
                alarmRecord.setDelFlag("0");
                String officeId = deviceInfo.getOfficeId();
                alarmRecord.setStationId(officeId);
                alarmRecord.setStationCode("");
                alarmRecord.setStationName("");
                logger.info("设备编号: " + deviceInfo.getDeviceCode() + " 收到报警信息."
                        + " 报警ID: " + alarmRecord.getAlarmId()
                        + " 报警详情: " + alarmRecord.getAlarmName());
                alarmRecords.add(alarmRecord);
            }
        }
        return alarmRecords;
    }

    private void accuracyCheck(String deviceCode, String str) {
        logger.info("HITACHI_LASER_DRILL_ACCURACY=" + GlobalConstants.getProperty("HITACHI_LASER_DRILL_ACCURACY"));
        String[] accuracys = str.split(",");
        for (String accuracy : accuracys) {
            if (accuracy.contains("=") && accuracy.contains("RMAX_")) {
                String[] accuracysTemp = accuracy.split(",");
                try {
                    Double.parseDouble(accuracysTemp[1]);
                    double accuracyD = Double.parseDouble(GlobalConstants.getProperty("HITACHI_LASER_DRILL_ACCURACY"));
                    logger.info(accuracy);
                    if (Double.parseDouble(accuracysTemp[1]) > accuracyD) {
                        logger.warn(accuracy + "out of rang " + accuracyD);
                        GlobalConstants.stage.equipModels.get(deviceCode).stopEquip();
                    }
                } catch (Exception e) {
                }
            }
        }
    }
}
