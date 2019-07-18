/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.shinkawa;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;


public class UTC5000RecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            String title = "";
            String partTitle = "";
            String key = "";
            String value = "";
            // keyFlag用于标记key值是否读取完毕，valueFlag用于标记value值是否读取完毕
            boolean keyFlag = false;
            boolean valueFlag = false;
            int specialCount = 1;
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.startsWith(",")) {
                    String[] lineStrs = cfgline.split(",");
                    int strCol = 0;
                    for (String str : lineStrs) {
                        if ("".equals(str)) {
                            continue;
                        }
                        if (!keyFlag) {
                            key = key + str.trim() + ":";
                            keyFlag = true;
                        } else {
                            value = value + str.trim() + ",";
                            valueFlag = true;
                        }
                        strCol++;
                    }
                    if (keyFlag && valueFlag) {
                        key = key.substring(0, key.length() - 1);
                        value = value.substring(0, value.length() - 1);
//                        value = StringUtils.substringBeforeLast(value, "(");
//                        if (null != map.get((title.isEmpty() ? title : (title + ":")) + partTitle + ":" + key)) {
//                            System.out.println((title.isEmpty() ? title : (title + ":")) + partTitle + ":" + key);
//                        }
                        map.put(title + partTitle + ":" + key, value);
                        key = "";
                        value = "";
                        keyFlag = false;
                        valueFlag = false;
                    }
                } else {
                    if (cfgline.trim().contains("[") && cfgline.trim().contains("]")) {
                        partTitle = title + cfgline.trim() + ":";
                    } else if (cfgline.trim().contains(",") && !cfgline.trim().contains("Group_LOOP")) {
                        if (cfgline.trim().contains("Device name")) {
                            title = cfgline.trim().split(",")[1] + ":";
                        } else if (cfgline.trim().contains("Chip name")) {
                            title = title + cfgline.trim().split(",")[1] + ":";
                        } else {
                            partTitle = title + cfgline.trim().split(",")[1];
                        }
                    } else if (cfgline.trim().contains("Group_") && cfgline.trim().contains("LOOP")) {
                        title = "";
                        partTitle = cfgline.trim();
                    } else {
                        if (partTitle.contains("[") && partTitle.contains("]")) {
                            partTitle = StringUtils.substringBeforeLast(partTitle, ":").replace(title, "") + ":" + cfgline.trim();
                        } else {
                            partTitle = cfgline.trim();
                        }
                        if (cfgline.trim().contains(".csv")) {
                            // Base Parameter标题与recipe文件地址一起放在第一行
                            partTitle = cfgline.split(".csv")[1].trim();
                        } else if ("Special".equals(cfgline.trim())) {
                            partTitle = partTitle + specialCount++;
                        }
                    }
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
                recipePara.setSetValue(StringUtils.substringBeforeLast(e.getValue(), "("));
                recipePara.setMinValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMinValue());
                recipePara.setMaxValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMaxValue());
                recipePara.setParaMeasure(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaUnit());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
    }

    public static boolean saveRecipeTemplateList(Map paraMap) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            String deviceTypeId = "b42eb65b03c1471baf9289d25cffdcf9";
            String deviceTypeCode = "UTC-5000 NeocuZ1";
            String deviceTypeName = "UTC-5000 NeocuZ1";
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceTypeCode, "RecipePara");
//        sqlSession.close();
            List<String> paraNameList = new ArrayList<>();
            for (int i = 0; i < recipeTemplates.size(); i++) {
                paraNameList.add(recipeTemplates.get(i).getParaName());
            }
            // 需要保存到数据库的recipe参数（数据库中没有的部分）
            List<RecipeTemplate> recipeParaList2Save = new ArrayList<>();
            Set<Map.Entry<String, String>> entry = paraMap.entrySet();
            int k = 1;
            for (Map.Entry<String, String> e : entry) {
                if (!paraNameList.contains(e.getKey())) {
                    RecipeTemplate recipeTemplate = new RecipeTemplate();
                    recipeTemplate.setDeviceTypeId(deviceTypeId);
                    recipeTemplate.setDeviceTypeCode(deviceTypeCode);
                    recipeTemplate.setDeviceTypeName(deviceTypeName);
                    recipeTemplate.setParaCode(String.valueOf(k));
                    recipeTemplate.setParaName(e.getKey());
                    String value = e.getValue();
                    if (value.contains("(") && value.contains(")")) {
                        String unit = StringUtils.substringBeforeLast(StringUtils.substringAfterLast(value, "("), ")");
                        recipeTemplate.setParaUnit(unit);
                    }
                    recipeTemplate.setDeviceVariableType("RecipePara");
                    recipeTemplate.setDelFlag("0");
                    recipeTemplate.setCreateBy("1");
                    recipeTemplate.setCreateDate(new Date());
                    recipeTemplate.setUpdateBy("1");
                    recipeTemplate.setUpdateDate(new Date());
                    recipeParaList2Save.add(recipeTemplate);
                    k++;
                }
            }

            if (recipeParaList2Save != null && !recipeParaList2Save.isEmpty()) {
//                recipeService.deleteRecipeTemplateByDeviceTypeCodeBatch(recipeParaList2Save);
//                for (RecipeTemplate recipeTemplate : recipeParaList2Save) {
//                    recipeTemplate.setDeviceTypeId(deviceTypeId);
//                }
                List<RecipeTemplate> recipeTemplatesTmp = new ArrayList<>();
                for (int i = 0; i < recipeParaList2Save.size(); i++) {
                    recipeTemplatesTmp.add(recipeParaList2Save.get(i));
                    if (recipeTemplatesTmp.size() % 500 == 0) {
                        recipeService.saveRcpTemplateBatch(recipeTemplatesTmp);
                        recipeTemplatesTmp.clear();
                        sqlSession.commit();
                    }
                }
                recipeService.saveRcpTemplateBatch(recipeTemplatesTmp);
                sqlSession.commit();
            }
        } catch (Exception e) {
            System.out.println("something error while save template recipe!!");
            sqlSession.rollback();
        }
        return true;
    }

    public static void main(String[] args) {
        Map paraMap = UTC5000RecipeUtil.transferFromFile("D:\\YAC03DFN008109-AG20-25080303-05-B.csv_V0.txt");
//        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
//        int maxLength = 0;
//        for (Map.Entry<String, String> e : entry) {
//            if (e.getKey().length() > maxLength) {
//                maxLength = e.getKey().length();
//            }
//        }
//        for (Map.Entry<String, String> e : entry) {
//            String key = e.getKey();
//            if (key.length() < maxLength) {
//                int length = maxLength - key.length();
//                for (int i = 0; i < length; i++) {
//                    key = key + " ";
//                }
//            }
//            System.out.println(key + "=" + e.getValue());
//        }
        // 保存recipe参数的方法
        boolean flag = saveRecipeTemplateList(paraMap);
        if (!flag) {
            System.out.println("保存失败");
        }

//        List<RecipePara> list = UTC5000RecipeUtil.transferFromDB(paraMap, "UTC-5000 NeocuZ1");
//        for (int i = 0; i < list.size(); i++) {
//            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue() + "=====" + list.get(i).getParaMeasure());
//        }
    }

}
