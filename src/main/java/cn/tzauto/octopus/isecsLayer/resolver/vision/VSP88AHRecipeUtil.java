package cn.tzauto.octopus.isecsLayer.resolver.vision;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

import java.io.*;
import java.util.*;

/**
 * Created by wangdanfeng.
 */
public class VSP88AHRecipeUtil {
    public static Map transferFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        System.out.println(filePath);
        Map map = new HashMap<String, String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = "";
            String stepNo = "";
            while ((line = br.readLine()) != null) {
                if (line.contains("[")) {
                    //当前步数
                    stepNo = line;
                } else {
                    String[] nAv = line.split("=");
                    System.out.println(nAv[0] + stepNo);
                    map.put(nAv[0] + stepNo, nAv[1]);
                }
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
        for(RecipeTemplate recipeTemplate :recipeTemplates){
            String key=recipeTemplate.getParaName();
            if(key==null||"".equals(key)){
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue((String)paraMap.get(key));
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }

    public static void main(String[] args) {
        String filePath = "D:\\104.rcp";
        System.out.println(filePath);
        Map map = VSP88AHRecipeUtil.transferFromFile(filePath);
        for (String s : (Set<String>) map.keySet()) {
            System.out.println(s + "**********************" + map.get(s));
        }
        List<RecipePara> list = VSP88AHRecipeUtil.transferFromDB(map, "VISIONVSP-88AH");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }
}
