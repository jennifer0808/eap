/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.htm;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

import java.io.*;
import java.util.*;


public class HtmRecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            FileInputStream in = new FileInputStream(filePath);
            byte[] temp = new byte[150];
            in.read(temp);
            int i = 1;
            for (byte a : temp) {
                map.put(String.valueOf(i++), a);
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List transferFromDB(Map paraMap, String deviceType, String deviceCode) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<String> paraNameList = new ArrayList<>();
        if ("HTM-3661".equals(deviceCode)) {
            for (int i = 70; i < recipeTemplates.size(); i++) {
                paraNameList.add(recipeTemplates.get(i).getParaCode());
            }
        } else {
            for (int i = 0; i < 70; i++) {
                paraNameList.add(recipeTemplates.get(i).getParaCode());
            }
        }
        List<RecipePara> recipeParaList = new ArrayList<>();
        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
        for (Map.Entry<String, String> e : entry) {
            if (paraNameList.contains(e.getKey())) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaCode());
                recipePara.setParaName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaName());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipePara.setSetValue(String.valueOf(e.getValue()));
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
            String deviceTypeId = "4AFD9962300901B4E053AC11AD668792";
            String deviceTypeCode = "HTM";
            String deviceTypeName = "HTM";
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
//                    String value = e.getValue();
//                    if (null != value && !"".equals(value) && value.length() > 99) {
//                        value = value.substring(0, 100);
//                    }
//                    recipeTemplate.setSetValue(value);
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
            return false;
        }
        return true;
    }

    public static List analysisRecipe(String recipePath, String deviceCode, String deviceType) {
        Map paraMap = HtmRecipeUtil.transferFromFile(recipePath);
        List<RecipePara> recipeParaList = HtmRecipeUtil.transferFromDB(paraMap, deviceType, deviceCode);
        return recipeParaList;
    }

    public static void main(String[] args) {
        Map paraMap = HtmRecipeUtil.transferFromFile("D:\\11_V2.txt");

        // 保存recipe参数的方法
//        boolean flag = saveRecipeTemplateList(paraMap);
//        if (!flag) {
//            System.out.println("保存失败");
//        }
//
        List<RecipePara> list = HtmRecipeUtil.transferFromDB(paraMap, "HTM", "HTM-3661");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }

}
