package cn.tzauto.octopus.isecsLayer.resolver.dd;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GinRecipeUtil {
    private static final Logger logger = LoggerFactory.getLogger(GinRecipeUtil.class);

    public static List<RecipePara> getRecipePara(String filePath, String deviceType) {
        BufferedReader br = null;
        List<RecipePara> list = new ArrayList<>();
        SqlSession sqlSession = null;
        try {
            sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
            br = new BufferedReader(new FileReader(filePath));
            String line = "";
            while ((line = br.readLine()) != null) {
//                System.out.println(line);
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    if (line.contains(recipeTemplate.getParaName())) {
                        line = line.replace(recipeTemplate.getParaName(), "");
                        String[] values = line.trim().split("\\s+");
                        System.out.println(values[0]+":"+values[1]);
                        RecipePara recipePara = new RecipePara();
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        recipePara.setParaName(recipeTemplate.getParaName());
                        recipePara.setSetValue(values[0]);
                        list.add(recipePara);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("解析recipe文件出错!文件路径：{}", filePath);
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return list;
        }


    }

    public static void main(String[] args) {
        String filePath = "D:\\桌面文件\\长电\\DD\\GIN\\DEVICE\\SSOP 16LD (8ROWS)\\DeviceData.grd";
        List<RecipePara> recipeParas= GinRecipeUtil.getRecipePara(filePath,"GINA2255");
        for(RecipePara recipePara:recipeParas){
            System.out.println(recipePara.getParaName()+":"+recipePara.getSetValue());
        }
    }

}
