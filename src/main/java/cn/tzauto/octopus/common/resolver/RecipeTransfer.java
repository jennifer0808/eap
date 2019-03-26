/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author luosy
 */
public class RecipeTransfer implements IRecipeTransfer {

    @Override
    public List<RecipePara> transferRecipeParaFromDB(String filePath, String deviceType) {
        return null;
    }

    @Override
    public void edit(Recipe recipe, String deviceType, String localRecipeFilePath) {
        try {
            //创建类
            Class<?> class1 = Class.forName("cn.tzinfo.htauto.octopus.secsLayer.resolver." + deviceType + "RecipeEdit");

            //创建实例化：相当于 new 了一个对象
            Object object = class1.newInstance();
            //向下转型
            IRecipeTransfer recipeTransfer = (RecipeTransfer) object;
            recipeTransfer.editRecipeFile(recipe, deviceType, localRecipeFilePath);
        } catch (Exception e) {
            System.out.println("cn.tzinfo.htauto.octopus.secsLayer.resolver.RecipeTransfer.edit()");
        }

    }

    public static boolean hasGoldPara(String deviceType) {
        List<RecipeTemplate> recipeTemplates;
        try (SqlSession sqlSession = MybatisSqlSession.getSqlSession()) {
            RecipeService recipeService = new RecipeService(sqlSession);
            recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        }
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if (recipeTemplate.getGoldPara() != null && recipeTemplate.getGoldPara().equals("Y")) {
                return true;
            }
        }
        return false;
    }

    public static void writeRecipeFile(List<String> list, String filepath) {
        File file = new File(filepath);
        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (String string : list) {
                bw.write(string);
                bw.newLine();
            }
            bw.close();
            fw.close();
        } catch (Exception e) {
        }

    }

    @Override
    public void editRecipeFile(Recipe recipe, String deviceType, String localRecipeFilePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
