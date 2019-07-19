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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.*;


public class HtmWashRecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath, String deviceCode) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            FileInputStream in = new FileInputStream(filePath);
            byte[] temp = new byte[100];
            in.read(temp);
            List list = new ArrayList<>();
            for (int i = 0; i < temp.length; i++) {
                list.add(Integer.toHexString(temp[i]));
            }
            int paraCode = 1;
            int startIndex = 0;
            int endIndex = list.size();
            if ("PLA-011".equals(deviceCode)) {
                startIndex = 0;
                endIndex = 10;
            } else if ("PLA-012".equals(deviceCode)) {
                startIndex = 11;
                endIndex = 21;
            } else if ("PLA-013".equals(deviceCode)) {
                startIndex = 22;
                endIndex = 32;
            } else if ("PLA-014".equals(deviceCode)) {
                startIndex = 33;
                endIndex = 43;
            } else if ("PLA-017".equals(deviceCode)) {
                startIndex = 44;
                endIndex = 54;
            } else if ("PLA-018".equals(deviceCode)) {
                startIndex = 55;
                endIndex = 65;
            } else if ("PLA-027".equals(deviceCode)) {
                startIndex = 66;
                endIndex = 76;
            }
            for (int i = startIndex; i < endIndex; i++) {
                int value = Integer.parseInt(list.get(i).toString().replace("f", "") + list.get(++i).toString().replace("f", ""), 16);
                map.put(String.valueOf(paraCode++), value);
            }
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
        Map<String, RecipeTemplate> paraNameMap = new HashMap<>();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            paraNameMap.put(recipeTemplates.get(i).getParaCode(), recipeTemplates.get(i));
        }
        List<RecipePara> recipeParaList = new ArrayList<>();
        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
        for (Map.Entry<String, String> e : entry) {
            if (null != paraNameMap.get(e.getKey())) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(paraNameMap.get(e.getKey()).getParaCode());
                recipePara.setParaName(paraNameMap.get(e.getKey()).getParaName());
                recipePara.setParaShotName(paraNameMap.get(e.getKey()).getParaShotName());
                recipePara.setSetValue(String.valueOf(e.getValue()));
                recipePara.setMinValue(paraNameMap.get(e.getKey()).getMinValue());
                recipePara.setMaxValue(paraNameMap.get(e.getKey()).getMaxValue());
                recipePara.setParaMeasure(paraNameMap.get(e.getKey()).getParaUnit());
                recipePara.setParaShotName(paraNameMap.get(e.getKey()).getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
    }

    public static boolean saveRecipeTemplateList(Map paraMap) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            String deviceTypeId = "4AFD9962300901B4E053AC11AD668700";
            String deviceTypeCode = "HTMWash";
            String deviceTypeName = "HTMWash";
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

    public static List analysisRecipe(String recipePath, String deviceType, String deviceCode) {
        Map paraMap = HtmWashRecipeUtil.transferFromFile(recipePath, deviceCode);
        List<RecipePara> recipeParaList = HtmWashRecipeUtil.transferFromDB(paraMap, deviceType);
        return recipeParaList;
    }

    public static void main(String[] args) {
        Map paraMap = HtmWashRecipeUtil.transferFromFile("D:\\QFNDFN-4B_V10.txt", "PLA-012");

        // 保存recipe参数的方法
//        boolean flag = saveRecipeTemplateList(paraMap);
//        if (!flag) {
//            System.out.println("保存失败");
//        }
//
        List<RecipePara> list = HtmWashRecipeUtil.transferFromDB(paraMap, "HTMWash");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }

}
