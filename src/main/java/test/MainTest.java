/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.common.mq.MessageUtils;
import cn.tzauto.octopus.common.mq.common.MQConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainTest {

    public static void main(String[] args) {

        //{FINTHICK=354661, WAFERID=A-10, deviceCode=BG-002, finishDate=Sat Jul 21 16:03:03 GMT+08:00 2018, msgName=FinishThick, Z=Z2}
        MQConstants.initConenction();

        Map alarmRecordMap = new HashMap();
        alarmRecordMap.put("msgName", "ArAlarmRecord");
        alarmRecordMap.put("deviceCode", "BWM-027");
        alarmRecordMap.put("alarmRecord", JsonMapper.toJsonString(setAlarmRecord()));
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        alarmRecordMap.put("alarmDate", dateFormat.format(new Date()));
        MessageUtils C2SAlarmQueue = new MessageUtils("C2S.Q.ALARM_D_LZX");
        C2SAlarmQueue.sendMessage(alarmRecordMap);
    }

    private static List<AlarmRecord> setAlarmRecord() {
        List<AlarmRecord> alarmRecords = new ArrayList<>();

        AlarmRecord alarmRecord = new AlarmRecord();
        String id = UUID.randomUUID().toString();
        alarmRecord.setId(id);
        alarmRecord.setClientCode("2222222");
        alarmRecord.setClientId("2222222");
        alarmRecord.setClientName("122222");
        alarmRecord.setDeviceId("5171f1be-4207-11e8-b736-00e0ff681faa");
        alarmRecord.setDeviceCode("BWM-027");
        alarmRecord.setDevcieName("BWM-027");
        alarmRecord.setAlarmId("1604");
        alarmRecord.setAlarmCode("-128");
        alarmRecord.setAlarmName("");
        alarmRecord.setAlarmDate(new Date());
        alarmRecord.setDeviceTypeCode("NITTODR3000IIIZ1");
        alarmRecord.setDeviceTypeId("d1e1e0c3-091b-11e7-9975-c85b7611933d");
        alarmRecord.setRepeatFlag("N");
        alarmRecord.setStepCode("");
        alarmRecord.setStepId("");
        alarmRecord.setStepName("");
        alarmRecord.setVerNo(0);
        alarmRecord.setRemarks("");
        alarmRecord.setDelFlag("0");
        alarmRecord.setStationId("010101");
        alarmRecord.setStationCode("");
        alarmRecord.setStationName("");
        alarmRecords.add(alarmRecord);

        return alarmRecords;
    }

    private static List<String> getHandleRecipeList(List<String> recipeList) {
        Map handleRecipeMap = new HashMap();
        List<String> handleRecipeList = new ArrayList<>();
        for (String str : recipeList) {
            if (str.contains("}")) {
                String[] strs = str.replaceAll("]}", "").replaceAll("\\{", "").split("}");
                for (String str1 : strs) {
                    String[] strTmps = str1.replaceAll("\"", "").replaceAll("\\t\\[", "").split(",");
                    Map<String, String> map = new HashMap();
                    for (String strTmp : strTmps) {
                        strTmp = strTmp.replaceAll("\\[", "");
                        if ("".equals(strTmp.trim())) {
                            continue;
                        }
                        strTmp = strTmp.replaceAll("recipes:", "");
                        String[] strmaps = strTmp.split(":");
                        map.put(strmaps[0], strmaps[1]);
                    }
                    handleRecipeMap.put(map.get("recipename"), map.get("DEVICE_INDEX") + " " + map.get("VISION_INDEX"));
                    handleRecipeList.add(map.get("recipename"));
                }
            }
        }
        return handleRecipeList;
    }
}
