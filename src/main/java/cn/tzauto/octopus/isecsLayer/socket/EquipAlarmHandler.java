/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.socket;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.log4j.Logger;

/**
 *
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
                    //报警	0	大气监控与控制	3	氧气：Sample Group 1 - Zone 1, 628 PPM
                    case "BTUPYRAM100NZ2BM":
                    case "BTUPYRAM125NZ3":
                    case "BTUPYRAM125NZ2BM":
                        remark = str.substring(str.indexOf("PPM") - 4, str.indexOf("PPM") - 1);
                        if ("".equals(remark)) {
                            logger.debug("no PPM in this alarm");
                            continue;
                        } else if (Integer.valueOf(remark.trim()) < 300) {
                            logger.debug("PPM less than 300 in this alarm");
                            continue;
                        } else {
                            alarm = str.substring(29, str.indexOf("Sample Group") - 1);
                        }
                        break;
                    case "BTUPYRAM100NZ3SMT": //TODO 待测试
                        if (str.contains("大气监控与控制")) {
                            remark = str.substring(str.indexOf("PPM") - 4, str.indexOf("PPM") - 1);
                            if ("".equals(remark)) {
                                logger.debug("no PPM in this alarm");
                                continue;
                            } else if (Integer.valueOf(remark.trim()) < 500) {
                                logger.debug("PPM less than 500 in this alarm");
                                continue;
                            } else {
//                                alarm = str.substring(29, str.indexOf("Sample Group") - 1);
                                alarm = "大气监控与控制";
                            }
                        } else if (str.contains("压力控制")) {
                            alarm = "压力控制";
                            remark = str;
                        }
                        break;
                    case "HELLER1913MKZ3FRONT":
                        if (str.contains("Light tower is yellow (from green)")) {
                            alarm = "TurnLightYellow";
                        } else if (str.contains("PPM has exceeded the amount set") || str.contains("PPM")) {
                            alarm = "PPMon";
                            logger.debug("LAST CHAR : ------------------->" + str.charAt(str.indexOf("Oxygen") - 1));
                            switch (str.charAt(str.indexOf("Oxygen") - 1)) {
                                case '1':
                                    alarm = "PPMon1";
                                    break;
                                case '2':
                                    alarm = "PPMon2";
                                    break;
                                case '3':
                                    alarm = "PPMon3";
                                    break;
                                case '4':
                                    alarm = "PPMon4";
                                    break;
                                default:
                                    break;
                            }
                        }
                        remark = str;
                        break;
                    case "HTM5022Z1":
//                        if (str.contains("OPEN") || str.contains("CLOSE")) {
//                            int index = str.contains("OPEN") ? str.indexOf("OPEN") : str.indexOf("CLOSE");
//                            alarm = str.substring(22, index).trim();
//                            remark = str.replaceAll("\\[CEM", "").replaceAll("]", "");
//                        }
                        if (str.contains("CLOSE")) {
                            alarm = str.substring(22, str.indexOf("CLOSE")).trim();
                            remark = str.replaceAll("\\[CEM", "").replaceAll("]", "");
                        }
                        break;
                    case "APTVTS60AZ2":
                        String[] splitStrs = str.split(",");
                        if ("Alarm".equals(splitStrs[1]) || "Warning".equals(splitStrs[1])) {
                            alarm = splitStrs[2];
                            remark = splitStrs[3];
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
                alarmRecord.setAlarmName("");
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
}
