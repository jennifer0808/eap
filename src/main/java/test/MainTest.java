/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.common.rabbit.MessageUtils;

import java.util.*;

public class MainTest {

    public static void main(String[] args) {

        MessageUtils messageUtils = new MessageUtils("C2S.Q.RECIPE_C","","1");
        HashMap<String,String> map = new HashMap<>();
        map.put("eventId","");
        map.put("recipePara","[]");
        map.put("msgName","Upload");
        map.put("recipe","[{\"clientId\":\"TESTCLIENT\",\"createBy\":\"1\",\"createDate\":1559113294396,\"delFlag\":\"0\",\"deviceCode\":\"TEST\",\"deviceId\":\"testing\",\"deviceName\":\"TEST\",\"deviceTypeCode\":\"ASM120T\",\"deviceTypeId\":\"d0bf82bc-f74d-11e6-8f7a-7c5cf8406312312\",\"deviceTypeName\":\"ASM120T\",\"id\":\"bc47eb70-35dd-406f-b8c7-7f0b65dadbbb\",\"isNewRecord\":false,\"prodCode\":\"\",\"prodId\":\"\",\"prodName\":\"\",\"recipeCode\":\"\",\"recipeDesc\":\"\",\"recipeName\":\"recipename2\",\"recipeStatus\":\"Create\",\"recipeType\":\"N\",\"remarks\":\"\",\"srcDeviceId\":\"TESTCLIENT\",\"totalCnt\":0,\"updateBy\":\"1\",\"updateCnt\":0,\"versionNo\":0,\"versionType\":\"Engineer\"}]");
        map.put("deviceCode","TEST");
        map.put("recipeOperationLog","[{\"deviceCode\":\"TEST\",\"deviceStatus\":\"\",\"operationDate\":1559113294837,\"operationResultDesc\":\"上传Recipe： recipename2 到工控机：TESTCLIENT\",\"operationType\":\"Upload\",\"operatorBy\":\"admin\",\"recipeRowId\":\"bc47eb70-35dd-406f-b8c7-7f0b65dadbbb\"}]");
        map.put("attach","[{\"attachName\":\"recipename2_V0\",\"attachPath\":\"/RECIPE/A3/DieAttach/ASM120T/Engineer/TEST/recipename2/\",\"attachType\":\"txt\",\"createBy\":\"1\",\"id\":\"f3208eb3-104e-4252-9dc5-796c3383356a\",\"recipeRowId\":\"bc47eb70-35dd-406f-b8c7-7f0b65dadbbb\",\"sortNo\":0,\"updateBy\":\"1\"}]");

        messageUtils.sendMessage(map);
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
