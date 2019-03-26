/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.rokko;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
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
 * @author luosy
 */
public class RokkoRecipeUtil {
    //filePath 由于需要对多个文件进行解析，这里传入的是文件夹

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
       
                    String[] cfg = cfgline.split("=");
                
                    key = cfg[0];
                    if (cfg.length > 2) {
                        cfg[0] = cfg[1];
                        cfg[1] = cfg[2];
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

        Map paraMap = RokkoRecipeUtil.transferFromFile("D:\\RECIPE\\06inch-150um-310-NP-DSC.DAT");

        List<RecipePara> list = RokkoRecipeUtil.transferFromDB(paraMap, "ROKKO8000P");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }

}
