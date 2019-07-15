/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.resolver.nordson;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author SunTao
 * @version V1.0
 * @desc
 */
public class FlexTRAKRecipeUtil {

    private static Logger logger = Logger.getLogger(FlexTRAKRecipeUtil.class);

    public static List transferFromRecipe(String filePath, String deviceType) {
        List<RecipePara> recipeParaList = new ArrayList<>();
        Map paraMap = FlexTRAKRecipeUtil.transferFromFile(filePath);
        if (paraMap != null && !paraMap.isEmpty()) {
            recipeParaList = FlexTRAKRecipeUtil.transferFromDB(paraMap, deviceType);
        } else {
            logger.error("解析recipe时出错,recipe文件不存在!");
        }
        return recipeParaList;
    }

    private static Map transferFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        System.out.println(filePath);
        Map map = new HashMap<String, String>();
        BufferedReader br = null;
        String[] names;
        String[] values;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//            String line = "";
//            while ((line = br.readLine()) != null) {
//                String[] strs = line.split(",");
//                if(line.contains("Power")){
//                    names = strs;
//                }else{
//                    values = strs;
//                }
//                
//            }
            names = br.readLine().split(",");
            values = br.readLine().split(",");
            for (int i = 0; i < names.length; i++) {
                map.put(names[i], values[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return map;
        }
    }

    public static List transferFromDB(Map paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            String key = recipeTemplate.getParaName();
            if (key == null || "".equals(key)) {
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue((String) paraMap.get(key));
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }

}
