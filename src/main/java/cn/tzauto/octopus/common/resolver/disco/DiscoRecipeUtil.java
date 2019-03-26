/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.disco;

import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;

public class DiscoRecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            int i = 0;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("$Now") || cfgline.contains("PH_ID") || cfgline.contains("DEV_ID")) {
                    String[] cfg = cfgline.split("=");
                    //因为文件的第一行有乱码，如果读取的是第一行，要把乱码去除
                    key = cfg[0];
                    if (cfg.length > 2) {
                        cfg[0] = cfg[1];
                        cfg[1] = cfg[2];
                    }
                    if (cfg[0].contains("DEV_ID")) {
                        key = "DEV_ID";
                    }
                    if (cfg[0].contains("PH_ID")) {
                        key = "PH_ID";
                    }
                    if (key.contains("[")) { //去除[]
                        key = key.substring(0, key.indexOf("["));
                    }
                    String[] cfg2 = cfg[1].split("\\$");
                    value = cfg2[0];
                    if (value.contains("{")) {  //去除{}
                        value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
                    }
                    if (value.contains("\"")) { //去除引号
                        value = value.replaceAll("\"", "");
                    }
                    key = key.replaceAll(" ", ""); //去除空格
                    value = value.replaceAll(" ", "");
                    if (value.contains(",")) {
                        String[] values = value.split(",");
                        String keyTemp = "";
                        //如果参数值为数组，参数名后面加数字并分别列出
                        for (int j = 0; j < values.length; j++) {
//                            if (j == 0) { //数组第一个的参数名不带数字
//                                map.put(key, values[j]);
//                            } else {
                            keyTemp = key + String.valueOf(j + 1);
                            map.put(keyTemp, values[j]);
//                            }

                        }
                    } else {
                        map.put(key, value);
                    }
                    i++;
                }
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List transferFromDB(Map paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<String> paraNameList = new ArrayList<>();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            paraNameList.add(recipeTemplates.get(i).getParaName());
        }
        List<RecipePara> recipeParaList = new ArrayList<>();
        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
        for (Map.Entry<String, String> e : entry) {
            if (paraNameList.contains(e.getKey())) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaCode());
                recipePara.setParaName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaName());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipePara.setSetValue(e.getValue());
                recipePara.setMinValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMinValue());
                recipePara.setMaxValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMaxValue());
                recipePara.setParaMeasure(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaUnit());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
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

        editRecipeName("D:\\RECIPE\\003.DFD", "AZ5B6501FR7G-EH270B1-9-HW02", "123-AZ5B6501FR7G-EH270B1-9-HW02");
        
        
        
        Map paraMap = DiscoRecipeUtil.transferFromFile("D:\\RECIPE\\003.DFD");
        List<RecipePara> list = DiscoRecipeUtil.transferFromDB(paraMap, "DISCODFG8540");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }

    public static String getEquipRecipeName(String filePath) {
        String equipRecipeName = "";
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
            while ((cfgline = br.readLine()) != null) {
                equipRecipeName = cfgline.split(",")[0];
            }
            br.close();
        } catch (Exception e) {
            return null;
        }
        return equipRecipeName;
    }

    public static void editRecipeName(String filePath, String recipeName, String uoidRecipeName) {
        List<String> list = new ArrayList();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "utf-8"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains(recipeName)) {
                    cfgline = cfgline.replaceAll(recipeName, uoidRecipeName);
                }
                list.add(cfgline);
            }
            br.close();

            File file = new File(filePath);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (String string : list) {
                bw.write(string);
                bw.newLine();
            }
            //bw.write((String) ppbody);
            bw.close();
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
