/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.common.rabbit.MessageUtils;
import cn.tzauto.octopus.common.util.tool.FileUtil;

import java.io.*;
import java.util.*;

public class MainTest {

    public static void main(String[] args) {
        String textPath= "D:\\tzauto\\inkInfo.txt";
        List<String[]> list = new ArrayList<>();
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(textPath), "UTF-8"));
            String tmpString = "";
            while ((tmpString = br.readLine()) != null) {
                String [] arr = tmpString.split(";");
                list.add(arr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(list);
        String recipeName="TTM2FQAPKL1A4A-2PNL";

        File file = new File("D:\\data\\PNLPG012#TZ-TEST\\TTM2FQAPKL1A4A-2PNLtemp\\TZ-TEST\\TZ-TEST\\Img");
        if (file.listFiles().length > 1) {
            File[] files = file.listFiles();
            for (File fileTemp : files) {
                if (!fileTemp.getName().equals(recipeName)) {
                    FileUtil.deleteAllFilesOfDir(fileTemp);
                }
            }
        }
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
