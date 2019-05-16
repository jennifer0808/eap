/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.socket;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author luosy
 */
public class RecipeMapping {

    public static Map loadRecipeLotMapping(String path) {

        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            String groupName = "";
            int i = 1;
            File cfgfile = new File(path);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));

            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("LOT") && cfgline.contains("RECIPENAME")) {
                    continue;
                }
                if (cfgline.contains(",")) {
                    value = "";
                    String[] cfg = cfgline.split(",");
                    for (int j = 0; j < cfg.length; j++) {
                        if (j == 0) {
                            key = cfg[0];
                        } else {
                            if ("".equals(value)) {
                                value = cfg[j];
                            } else {
                                value = value + "," + cfg[j];
                            }
                        }
                    }

//                    System.out.println(key + "||" + value);
//                    System.out.println(key);
                    map.put(key, value);
                    continue;
                }

            }
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return map;
    }
}
