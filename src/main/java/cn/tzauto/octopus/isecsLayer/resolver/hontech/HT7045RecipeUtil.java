package cn.tzauto.octopus.isecsLayer.resolver.hontech;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class HT7045RecipeUtil {
    private static final Logger logger = Logger.getLogger(HT7045RecipeUtil.class.getName());

    public static List  transferRcpFromDB(String recipePath, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        BufferedReader br = null;
        List<RecipePara> recipeParas = new ArrayList<>();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(recipePath)));
            String line = "";
            String groupName = "";
            String paraName = "";
            String paraValue = "";
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                RecipePara recipePara =new RecipePara();
                if (line.startsWith("[") && !line.contains("=")) {
                    groupName = line;
                    continue;
                } else {
                    String[] lines = line.split("=");
                    if (lines.length < 2) {
                        paraName = groupName+lines[0].trim();
                        paraValue = "";
                    } else {
                        paraName = groupName+lines[0].trim();
                        paraValue = lines[1].trim();
                    }
                }
                for(RecipeTemplate recipeTemplate:recipeTemplates){
                    String recipeParaName =recipeTemplate.getParaName().trim();
                    if(paraName.equals(recipeParaName)){
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        break;
                    }
                }
                recipePara .setParaName(paraName);
                recipePara.setSetValue(paraValue);
                recipeParas.add(recipePara);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("解析文件出错",e);
        }finally {
            if(br!=null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return recipeParas;
        }


    }

    public static void main(String[] args) {
        String filePath = "D:\\桌面文件\\长电\\HONTECH\\ht7045\\httestrecie\\99999999.temp";
        List<RecipePara> recipeParas = transferRcpFromDB(filePath, "HONTECHHT7045");
        for(RecipePara recipePara :recipeParas){
            System.out.println(recipePara.getParaName()+":"+recipePara.getSetValue()+":"+recipePara.getParaCode());
        }
    }
}
