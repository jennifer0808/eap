/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.vision;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author luosy
 */
public class VisionRecipeUtil {
    //filePath 是PPBODY原文件的存储路径(非文件夹)

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

                if (cfgline.contains("[")) { //去除[]
                    groupName = cfgline.replaceAll("\\[", "").replaceAll("]", "");
                    continue;
                }
                String keyTemp = groupName;
                if (cfgline.contains("=")) {
                    String[] tmp = cfgline.split("=");
                    if (tmp[1].contains(",")) {
                        String[] values = tmp[1].split(",");
                        //如果参数值为数组，参数名后面加数字并分别列出
                        for (int j = 0; j < values.length; j++) {
                            keyTemp = groupName + tmp[0] + String.valueOf(j + 1);
                            map.put(keyTemp, values[j]);
                            System.out.println(keyTemp);
                        }
                    } else {
                        keyTemp = keyTemp + tmp[0];
                        value = tmp[1];
                        map.put(keyTemp, value);
                        System.out.println(keyTemp);
                    }
                } else {
                    map.put(keyTemp, value);
                }
            }
            br.close();
            return map;
        } catch (IOException e) {
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
        Map map = transferFromFile("D:\\RECIPE\\Test_V0.txt");
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
                    strTmp = key1 + "= " + value;
                } else {
                    if (key.contains(keytmp)) {
                        //System.out.println("key " + key + "value " + value + " keytmp " + keytmp + " value1" + value1);
                        value = setPara2String(key, value, keytmp, value1);
                        if (!"".equals(value)) {
                            strTmp = key1 + "=" + value;
                        }

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
        String result = "";

        String[] cfg = str.split("=");
        //因为文件的第一行有乱码，如果读取的是第一行，要把乱码去除
        if (cfg.length < 2) {
            return "";
        }
        if (cfg.length > 2) {
            cfg[0] = cfg[1];
            cfg[1] = cfg[2];
        }
        String value = cfg[0];

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
                result = result + " " + values[i] + ",";
            }
        }

        return result;
    }

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static List<String> getUniqueRecipeParaMap(String filePath) {
        List list = new ArrayList();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                list.add(cfgline);
            }
            br.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
