/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.resolver.screen;


import cn.tzauto.octopus.common.resolver.TransferUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;


public class ScreenRecipeUtil extends TransferUtil {

    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            String groupName = "";
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("[") || cfgline.contains("=")) {
                    if (cfgline.contains("[")) {
                        groupName = cfgline.replaceAll("\\[", "").replaceAll("]", "");
                        continue;
                    }
                    String[] cfg = cfgline.split("=");
                    key = cfg[0];
                    if (cfg.length > 1) {
                        value = cfg[1];
                    } else {
                        value = "";
                    }
                    map.put(groupName + "-" + key, value);
                }
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) {
        //8-260PG-200-180-D175-OS10  8-260PG-200-180-D175-OS10TEST   8-575B-200-90-F125E   8-E8180-75P-0-9011-20   DISCO-AP-8-thin   IC-8-575B-90P-0-DU-2385KS
//        Map map = transferFromFile("D:\\MT6580DOE.txt");//MCXT-010.txt   AMIT-552
//        Set<Map.Entry<String, String>> entry = map.entrySet();
//        for (Map.Entry<String, String> e : entry) {
//            System.out.println(e.getKey() + "——" + e.getValue());
//        }
//        System.out.println(entry.size());
//        List<RecipePara> list = transferFromDB(map, "DISCODFL7161");
//        System.out.println(list.size());
//        Map paraMap = DiscoRecipeUtil.transferFromFile("D:\\RECIPE\\AKJ@12009.txt");
        Map paraMap = ScreenRecipeUtil.transferFromFile("F:\\FFOutput\\FSAPBH1\\Img\\TTM2FSAPBH1E4A-2PNL\\EXPOSE.JOB");

//        List<RecipePara> list = ScreenRecipeUtil.transferFromDB(paraMap, "DISCODFD6361PLUS");
//        for (int i = 0; i < list.size(); i++) {
//            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
//        }
    }

}
