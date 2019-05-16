/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.beac;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author luosy
 */
public class BeacRecipe {

    private static Logger logger = Logger.getLogger(BeacRecipe.class.getName());

    public static Map loadRecipeInfo() {

        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";

            int i = 1;
            File cfgfile = new File(GlobalConstants.getProperty("BEAC_RECIPE_INFO_PATH"));
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));

            while ((cfgline = br.readLine()) != null) {
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

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Can not find any file at " + GlobalConstants.getProperty("BEAC_RECIPE_INFO_PATH"));
            return map;
        }
        return map;
    }

    static Map loadRecipePara(String path) {

        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";

            int i = 1;

//            File cfgfile = new File("E:\\TZ\\EAP\\beacparapoint.csv");
            File cfgfile = new File(path);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));

            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains(",")) {
                    value = "";
                    String[] cfg = cfgline.split(",");
                    key = cfg[0];
                    value = cfg[1];
//                    System.out.println(key + "||" + value);
//                    System.out.println(key);
                    map.put(key, value);
                    continue;
                }

            }
            br.close();

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Can not find any file at " + path);
            return map;
        }
        return map;
    }
}
