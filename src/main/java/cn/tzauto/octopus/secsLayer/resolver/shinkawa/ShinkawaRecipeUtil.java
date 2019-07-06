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


public class ShinkawaRecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
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
                    for (String str : lineStrs) {
                        if ("".equals(str)) {
                            continue;
                        }
                        if (!keyFlag) {
                            if (!"Loop".equals(str) && !"2nd Bond".equals(str)) {
                                key = key + str.trim() + "_";
                                keyFlag = true;
                                if ("Wire".equals(str) && "Special".equals(partTitle)) {
                                    keyFlag = false;
                                }
                            } else {
                                partTitle = partTitle + "_" + str.trim();
                            }
                        } else {
                            value = value + str.trim() + ",";
                            valueFlag = true;
                        }
                    }
                    if (keyFlag && valueFlag) {
                        key = key.substring(0, key.length() - 1);
                        value = value.substring(0, value.length() - 1);
//                        value = StringUtils.substringBeforeLast(value, "(");
                        map.put(partTitle + "_" + key, value);
                        key = "";
                        value = "";
                        keyFlag = false;
                        valueFlag = false;
                    }
                } else {
                    partTitle = cfgline.trim();
                    if (partTitle.contains(".csv")) {
                        // Base Parameter标题与recipe文件地址一起放在第一行
                        partTitle = cfgline.split(".csv")[1].trim();
                    } else if (partTitle.contains(",") && !partTitle.contains("Group_LOOP")) {
                        String[] lineStrs = cfgline.split(",");
                        String lineKey = "";
                        String lineValue = "";
                        for (String str : lineStrs) {
                            if ("".equals(lineKey)) {
                                lineKey = str.trim();
                            } else {
                                lineValue = lineValue + str.trim() + ",";
                            }
                        }
                        map.put(lineKey, lineValue.substring(0, lineValue.length() - 1));
                    } else if ("Special".equals(partTitle)) {
                        partTitle = partTitle + specialCount++;
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

    public static boolean saveRecipeTemplateList(Map paraMap) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            String deviceTypeId = "4AFD9962300901B4E053AC11AD667892";
            String deviceTypeCode = "SHINKAWA";
            String deviceTypeName = "SHINKAWA";
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
        Map paraMap = ShinkawaRecipeUtil.transferFromFile("D:\\RECIPE\\A3\\DieAttach\\SHINKAWA\\Engineer\\SHINKAWA\\00V00YKR012QFN060004-PC20-28080355-04-C.csv\\00V00YKR012QFN060004-PC20-28080355-04-C.csv_V0.csv");
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
//        // 保存recipe参数的方法
//        boolean flag = saveRecipeTemplateList(paraMap);
//        if (!flag) {
//            System.out.println("保存失败");
//        }
//
        List<RecipePara> list = ShinkawaRecipeUtil.transferFromDB(paraMap, "SHINKAWA");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }

}
