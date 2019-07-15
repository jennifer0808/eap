/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.disco;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;


public class RecipeEdit {

    public static void main(String[] args) {

    }

    public static List<String> setGoldPara(List<RecipePara> goldRecipeParas, String uniqueFile, String deviceType) {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        Map<String, String> editMap = new HashMap<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if (recipeTemplate.getGoldPara() != null && recipeTemplate.getGoldPara().equals("Y")) {
                for (RecipePara goldRecipePara : goldRecipeParas) {
                    if (goldRecipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                        editMap.put(recipeTemplate.getParaName(), goldRecipePara.getSetValue());
                    }
                }

            }
        }
        List<String> uniqueRecipePara = getUniqueRecipeParaMap(uniqueFile);
        List<String> resultList = new ArrayList<>();

        for (String str : uniqueRecipePara) {
            if (str == null) {
                str = "";
            }
            String strTmp = "";

            String[] strs = str.split("=");
            if (strs.length < 1) {
                continue;
            }
            String key1 = strs[0];
            String value1 = strs[1];
            String keytmp = key1;
            if (key1.contains("[")) {
                keytmp = key1.split("\\[")[0];
            }
            for (Map.Entry<String, String> entry : editMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    value = "";
                }
                if (key.equals(keytmp.trim().replaceAll(" ", ""))) {
                    if (!value1.contains("\"")) {
                        strTmp = key1 + "= " + value + " $Now";
                    } else {
                        strTmp = key1 + "= " + "\"" + value + "\"" + " $Now";
                    }
                } else {
                    if (key.contains(keytmp)) {
                        //System.out.println("key " + key + "value " + value + " keytmp " + keytmp + " value1" + value1);
                        value = setPara2String(key, value, keytmp, value1);
                        if (!"".equals(value)) {
                            strTmp = key1 + "=" + value;
                        }

                        // resuletMap.put(key1, setPara2String(key, value, key1, value1));
                    }
                }
            }
            if ("".equals(strTmp)) {
                resultList.add(str);
            } else {
                resultList.add(strTmp);
            }

        }
        return resultList;
    }

    private static String setPara2String(String paraName, String para, String groupName, String str) {
        String result = " {";
        if (str.contains("$Now") || str.contains("PH_ID") || str.contains("DEV_ID")) {
            String[] cfg = str.split("\\$");
            //因为文件的第一行有乱码，如果读取的是第一行，要把乱码去除
            if (cfg.length < 2) {
                return "";
            }
            if (cfg.length > 2) {
                cfg[0] = cfg[1];
                cfg[1] = cfg[2];
            }

            String value = cfg[0];
            if (value.contains("{")) {  //去除{}
                value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
            }
            boolean flag = false;
            if (value.contains("\"")) { //去除引号
                value = value.replaceAll("\"", "");
                flag = true;
            }
            value = value.replaceAll(" ", "");
            // System.out.println("---------------------------" + paraName);
            if (value.contains(",")) {
                String[] values = value.split(",");
                try {
                    Integer.valueOf(paraName.replaceAll(groupName, ""));
                } catch (Exception e) {
                    return "";
                }
                int no = Integer.valueOf(paraName.replaceAll(groupName, ""));
                if (no > values.length || values.length < 1) {
                    return "";
                }
                values[no - 1] = para;

                for (int i = 0; i < values.length; i++) {
                    if (flag) {
                        result = result + " \"" + values[i] + "\",";
                    } else {
                        result = result + " " + values[i] + ",";
                    }
                }
                result = result + " } $Now";
                result = result.replaceAll(", }", " }");
            }

        }
        return result;
    }

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static List<String> getUniqueRecipeParaMap(String filePath) {
        List list = new ArrayList();
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
                    list.add(cfgline);
                    i++;
                }
            }
            br.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeRecipeFile(List<String> list, String filepath) {

        File file = new File(filepath);
        try {
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
        }
    }

    public static boolean hasGoldPara(String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if (recipeTemplate.getGoldPara() != null && recipeTemplate.getGoldPara().equals("Y")) {
                return true;
            }
        }
        return false;
    }
}
