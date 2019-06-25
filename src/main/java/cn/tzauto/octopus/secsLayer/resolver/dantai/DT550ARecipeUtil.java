package cn.tzauto.octopus.secsLayer.resolver.dantai;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class DT550ARecipeUtil {


    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DT550ARecipeUtil.class);

    public static List<RecipePara> getRecipeParas(String filePath, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String line = "";
            String[] lines;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("[")) {
                    line = line.replace("[", "").replace("]", "");
                    lines = line.split(":");
                } else {
                    lines = line.split("=");
                }
                String paraName = lines[0].trim().replaceAll("\\s+", "_");
                String paraValue = lines[1].trim();
//                System.out.println(paraName);
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    if (recipeTemplate.getParaName().equals(paraName)) {
                        RecipePara recipePara = new RecipePara();
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        recipePara.setParaName(recipeTemplate.getParaName());
                        recipePara.setParaShotName(recipeTemplate.getParaShotName());
                        recipePara.setSetValue(paraValue);
                        recipeParaList.add(recipePara);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
            return recipeParaList;
        }
    }

    public static void main(String[] args) {
        String filePath = "D:\\桌面文件\\长电\\DT550A\\001";
        List<RecipePara> recipeParas = getRecipeParas(filePath, "DANTAIDT550A");
        for(RecipePara recipePara:recipeParas){
            System.out.println(recipePara.getParaName()+":"+recipePara.getSetValue());
        }

    }

}
