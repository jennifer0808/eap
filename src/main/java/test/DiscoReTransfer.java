/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;

/**
 *
 */
public class DiscoReTransfer {
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
                if (cfgline.contains("$Now") || cfgline.contains("DEV_ID") || cfgline.contains("PH_ID")) {
                    String[] cfg = cfgline.split("=");
                    if (i == 0) {
                        if (cfg.length == 3) {
                            cfg[0] = cfg[1];
                            cfg[1] = cfg[2];
                        }
                    }
                    //因为文件的第一行有乱码，如果读取的是第一行，要把乱码去除
                    key = cfg[0];
                    if (i == 0) {
                        if (cfg[0].contains("DEV_ID")) {
                            key = "DEV_ID";
                        }
                        if (cfg[0].contains("PH_ID")) {
                            key = "PH_ID";
                        }
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

    public static List transferFromDB(Map paraMap, String deviceType, String RecipeRowId) {
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
                recipePara.setRecipeRowId(RecipeRowId);
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
    }

    public static void main(String[] args) {
        //8-260PG-200-180-D175-OS10  8-260PG-200-180-D175-OS10TEST   8-575B-200-90-F125E   8-E8180-75P-0-9011-20   DISCO-AP-8-thin   IC-8-575B-90P-0-DU-2385KS

//        Map map = transferFromFile("D:\\TestRecipe\\AEL@12009_V0.txt");//MCXT-010.txt   AMIT-552
//        Set<Map.Entry<String, String>> entry = map.entrySet();
//        for (Map.Entry<String, String> e : entry) {
//            System.out.println(e.getKey() + "——" + e.getValue());
//        }
//        System.out.println(entry.size());
//        List<RecipePara> list = transferFromDB(map, "DISCODFL7161");
//
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        recipeService.saveRcpParaBatch(list);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        int count = 0;

        List<Recipe> TotalList = new ArrayList();
        try {

            File file = new File("D:\\RECIPE\\A@ADA12001_V0.txt");
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (int i = 0; i < files.length; i++) {
//                        if (!files[i].getName().contains("@")) {
//                            if (!files[i].getName().contains("AKJ")) {
//                                continue;
//                            }
//                        }
                        if (files[i].isDirectory()) {
                            File[] RecipeFiles = files[i].listFiles();
                            String fileName = RecipeFiles[0].getName();
                            String rcpname = "";
                            String[] rcpnameTemp = fileName.split("_");
                            if (rcpnameTemp.length < 2) {
                                rcpname = fileName.replace(".txt", "");
                            } else {
                                rcpname = rcpnameTemp[0];
                            }
                            Recipe recipe = null;
                            String RecipeRowId = null;

//                            List<Recipe> recipes = recipeService.searchRecipeByPara(rcpname.replace("@", "\\"), "A1500-0004", "Engineer", "0");
                            List<Recipe> recipes = recipeService.searchRecipeByPara(rcpname.replace("@", "\\"), "A2500-0034", "Unique", null);
                            for (int k = 0; k < recipes.size(); k++) {
                                TotalList.add(recipes.get(k));
                            }

//                            if (recipes == null) {
//                                System.out.println("异常Recipe名字：" + rcpname);
//                            }
                            Map map = transferFromFile(files[i] + "\\" + fileName);//MCXT-010.txt   AMIT-552
                            Set<Map.Entry<String, String>> entry = map.entrySet();
//                            for (Map.Entry<String, String> e : entry) {
//                                System.out.println(e.getKey() + "——" + e.getValue());
//                            }
                            System.out.println(entry.size());

                            if (recipes != null && recipes.size() > 0) {
                                for (int j = 0; j < recipes.size(); j++) {
                                    recipe = recipes.get(j);
                                    RecipeRowId = recipe.getId();
//                                    System.out.println("Recipe的ID：" + RecipeRowId);
                                    List<RecipePara> list = transferFromDB(map, "DISCODFD6361PLUS", RecipeRowId);
                                    recipeService.saveRcpParaBatch(list);
                                    sqlSession.commit();
                                    System.out.println("RecipePara数目：" + list.size());
                                    count++;
                                }

                            } else {
                                System.out.println("异常Recipe名字：" + rcpname);
                            }

                        }
                    }
                }
            }
            System.out.println("ar_recipe所使用的条数：" + count);
            System.out.println("TotalList所有的条数：" + TotalList.size());
//            List<Recipe> arRecipes = recipeService.searchRecipeByPara(null, "A1500-0004", null, null);
//            System.out.println("ar_recipe所有的条数：" + arRecipes.size());
//
//            int conntI = 0;
//            for (int u = 0; u < arRecipes.size(); u++) {
////                if (!TotalList.contains(arRecipes.get(v))) {
////                    System.out.println("异常Recipe：" + arRecipes.get(v).getRecipeName());
////                    conntI++;
////                }
//
//                boolean flag = true;
//                for (int v = 0; v < TotalList.size(); v++) {
//                    if (arRecipes.get(u).getRecipeName().equalsIgnoreCase(TotalList.get(v).getRecipeName())) {
//                        flag = false;
//                        break;
//                    }
//                }
//                if (flag) {
//                    System.out.println("异常Recipe：" + arRecipes.get(u).getRecipeName());
//                    conntI++;
//                }
//            }
//            System.out.println("异常Recipe的条数：" + conntI);
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
//            sqlSession.rollback();
        }
    }
}
