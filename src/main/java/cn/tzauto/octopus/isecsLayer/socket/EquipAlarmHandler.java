
package cn.tzauto.octopus.isecsLayer.socket;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.tooling.LaserCrystal;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.FileUtil;
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
        //记得改回来
//        String deviceCode = map.get(eqpIp);
        String deviceCode = "RTRUV009";
        DeviceInfo deviceInfo = GlobalConstants.stage.hostManager.getDeviceInfo(null, deviceCode);

        List<String> alarmStringList = new ArrayList<>();
        alarmStringList.add(message);

        List<AlarmRecord> alarmRecordList = setAlarmRecord(deviceInfo, alarmStringList);

        if (!GlobalConstants.isLocalMode && alarmRecordList != null && alarmRecordList.size() != 0) {
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
                    case "HITACHI-LASERDRILL":
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
                            str = str.split("\\|\\|\\|\\|")[1];
                            if (str.contains("Date")) {
                                logger.info("power check data:Date:" + str.split(",")[1]);
//                            ((HitachiLaserDrillHost) GlobalConstants.eapView.equipModels.get(deviceInfo.getDeviceCode())).setPCHK(str.split(",")[1], null, null, null);
                            }
                            if (str.contains("Crystal No.")) {
                                logger.info("power check data:CrystalNo:" + str.split(",")[1]);
                            }
                            if (str.contains("Crystal_Time")) {
                                logger.info("power check data:Crystal_Time:" + str.split(",")[1]);
                            }
                            if (str.contains("Axis")) {
                                GlobalConstants.laserCrystalPower.setAxis(str.split(",")[1].substring(0, 2));
                                logger.info("power check data:Axis:" + str.split(",")[1]);
                            }
                            if (str.contains("AP_No")) {
                                GlobalConstants.laserCrystalPower.setAp_no(str.split(",")[1]);
//                                crystalPower = crystalPower + "_" + str + "_POWER";
                                logger.info("power check data:AP_No:" + str.split(",")[1]);
                            }
                            if (str.contains("Power")) {
                                GlobalConstants.laserCrystalPower.setPower(str.split(",")[1]);
//                                crystalPower = crystalPower + "=" + str;
                                logger.info("power check data:Power:" + str.split(",")[1]);

                            }
                            crystalPowerCheck(GlobalConstants.laserCrystalPower);
                            return null;
                        }
                        if (str.contains(".GLV")) {
                            if (str.contains("GCHK,")) {
                                GlobalConstants.laserCrystalAccuracy.setCrystalAccuracy(true);
//                                isCrystalAccuracy = true;
                            }
                            if (str.contains("GCOMP,")) {
                                GlobalConstants.laserCrystalAccuracy.setCrystalAccuracy(false);
//                                isCrystalAccuracy = false;
                                FileUtil.writeStrListFile(new ArrayList<>(), GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_ACCURACY_LOG_FILE_PATH"));
                            }

                            if (str.contains(",XY,")) {
                                String[] stringss = str.split(",");
                                GlobalConstants.laserCrystalAccuracy.setAp_no(stringss[1].trim());

                            }
                            if (str.contains("MAX_1")) {
                                String[] strs = str.split(",");
                                str = strs[strs.length - 1];
                                str =  str.split("=")[1];
                                GlobalConstants.laserCrystalAccuracy.setAxis("Z1");
                                GlobalConstants.laserCrystalAccuracy.setAccuracy(str);
//                                String crystalAccuracyTemp = "Z1_" + GlobalConstants.laserCrystalAccuracy.getAp_no() + "_" + "ACCURACY" + str;
//                                    logger.info("ACCURACY check data:" + crystalAccuracy + " RMAX_1:" + str);
//                                accuracyCheck(deviceInfo.getDeviceCode(), crystalAccuracyTemp);
                            }
                            if (str.contains("MAX_2")) {
                                String[] strs = str.split(",");
                                str = strs[strs.length - 1];
                                str = str.split("=")[1];
//                                String crystalAccuracyTemp = "Z2_" + GlobalConstants.laserCrystalAccuracy.getAp_no() + "_" + "ACCURACY" + str;
//                                    logger.info("ACCURACY check data:" + crystalAccuracy + " RMAX_2:" + str);
//                                accuracyCheck(deviceInfo.getDeviceCode(), crystalAccuracyTemp);
//                                    crystalAccuracy = "";
                                GlobalConstants.laserCrystalAccuracy.setAxis("Z2");
                                GlobalConstants.laserCrystalAccuracy.setAccuracy(str);
                            }
                            accuracyCheck(deviceInfo.getDeviceCode(), GlobalConstants.laserCrystalAccuracy);
                            return null;
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

    /**
     * 日立镭射钻孔机水晶头精度校验
     *
     * @param deviceCode
     * @param laserCrystal
     */
    private void accuracyCheck(String deviceCode, LaserCrystal laserCrystal) {
        logger.info("HITACHI_LASER_DRILL_ACCURACY=" +laserCrystal.toAccuracyString());
        if (!laserCrystal.canAccuracyCheck()) {

            return;
        }

        String str = laserCrystal.toAccuracyString();
        String accuracysTemp = laserCrystal.getAccuracy();
        try {
            Double.parseDouble(accuracysTemp);
            double accuracyD = Double.parseDouble(GlobalConstants.getProperty("HITACHI_LASER_DRILL_ACCURACY"));

            if (Double.parseDouble(accuracysTemp) > accuracyD) {
                logger.warn(str + "out of rang " + accuracyD);
                GlobalConstants.stage.equipModels.get(deviceCode).stopEquip();
            }
        } catch (Exception e) {
        }
        List fileBodyAsStrList = FileUtil.getFileBodyAsStrList(GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_ACCURACY_LOG_FILE_PATH"));
        if (fileBodyAsStrList == null || fileBodyAsStrList.isEmpty()) {
            fileBodyAsStrList = new ArrayList();
            fileBodyAsStrList.add(str);
            FileUtil.writeStrListFile(fileBodyAsStrList, GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_ACCURACY_LOG_FILE_PATH"));
        } else {
            if (strHasBeenWrite(fileBodyAsStrList, str)) {
                fileBodyAsStrList = removeRepetitionStr(fileBodyAsStrList, str);
            } else {
                fileBodyAsStrList.add(str);
            }
            FileUtil.writeStrListFile(fileBodyAsStrList, GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_ACCURACY_LOG_FILE_PATH"));
        }


        GlobalConstants.laserCrystalAccuracy = new LaserCrystal();
    }

    /**
     * 日立镭射钻孔机水晶头能量校验
     *
     * @param laserCrystal
     */
    private void crystalPowerCheck(LaserCrystal laserCrystal) {
        if (!laserCrystal.canPowerCheck()) {
            return;
        }
        String crystalPower = laserCrystal.toString();
        String[] crystalPowers = crystalPower.split("=");
        String standardPower = String.valueOf(GlobalConstants.crystalPowerMap.get(crystalPowers[0]));
        double actualPower = Double.parseDouble(crystalPowers[1]);
        double UPPER_LIMIT = Double.valueOf(String.valueOf(GlobalConstants.crystalPowerMap.get("UPPER_LIMIT"))) / 100;
        double LOWER_LIMIT = Double.valueOf(String.valueOf(GlobalConstants.crystalPowerMap.get("LOWER_LIMIT"))) / 100;
        if (Double.valueOf(standardPower) > actualPower / 1000 * (1 + UPPER_LIMIT) || Double.valueOf(standardPower) < actualPower / 1000 * (1 - LOWER_LIMIT)) {
            logger.error("Crystal power out of range. actual data " + crystalPower);
        }
        List fileBodyAsStrList = FileUtil.getFileBodyAsStrList(GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_POWER_LOG_FILE_PATH"));
        if (fileBodyAsStrList == null || fileBodyAsStrList.isEmpty()) {
            fileBodyAsStrList = new ArrayList();
            fileBodyAsStrList.add(crystalPower);
            FileUtil.writeStrListFile(fileBodyAsStrList, GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_POWER_LOG_FILE_PATH"));
        } else {
            if (strHasBeenWrite(fileBodyAsStrList, crystalPower)) {
                fileBodyAsStrList = removeRepetitionStr(fileBodyAsStrList, crystalPower);
            } else {
                fileBodyAsStrList.add(crystalPower);
            }
            FileUtil.writeStrListFile(fileBodyAsStrList, GlobalConstants.getProperty("HITACHI_LASER_DRILL_CRYSTAL_POWER_LOG_FILE_PATH"));
        }
        GlobalConstants.laserCrystalPower = new LaserCrystal();
    }

    private boolean strHasBeenWrite(List<String> strlist, String str) {
        for (String strTemp : strlist) {
            if (strTemp.contains(str.split("=")[0])) {
                return true;
            }
        }
        return false;
    }

    private List removeRepetitionStr(List<String> strlist, String str) {
        List<String> newStrs = new ArrayList<>();
        for (String strTemp : strlist) {
            if (strTemp.contains(str.split("=")[0])) {
                newStrs.add(str);
            } else {
                newStrs.add(strTemp);
            }
        }
        return newStrs;
    }
}
