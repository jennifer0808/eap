/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.resolver.cctech;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.MdbUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author SunTao
 * @date 2018-8-11 15:11:23
 * @version V1.0
 * @desc
 */
public class C6430RecipeUtil {
    private static Logger logger = Logger.getLogger(C6430RecipeUtil.class);

    public static List<RecipePara> transferFromRecipe(String filePath, String deviceType) {
        List<RecipePara> recipeParaList = new ArrayList<>();
        Map<String, Map<String, Map<String, String>>> recipeItemMap = MdbUtil.getRecipeItem(filePath, "", "cc");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        String groupName = "";
        Map<String, Map<String, String>> groupItem = null;
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if (!groupName.equals(recipeTemplate.getGroupName())) {
                groupName = recipeTemplate.getGroupName();
                groupItem = recipeItemMap.get(groupName);
            }
            Map<String, String> itemMap = groupItem.get(recipeTemplate.getDeviceVariableId());
            if(itemMap ==null){
                logger.debug("请注意!"+filePath+"中不存在"+recipeTemplate.getParaName()+";"+recipeTemplate.getParaShotName());
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue(itemMap.get("ParamValue"));
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }


    public static void main(String[] args) {
//        String filePath ="D:\\桌面文件\\长电\\CCTECH\\C6430\\changchuan\\Handler\\C6Q430\\ProductConfig\\ABG-LGA-5X5-14X35.mdb";
//        List<RecipePara> recipeParaList = C6430RecipeUtil.transferFromRecipe(filePath,"CCTECHC6430");
//
//        for (int i = 0; i < recipeParaList.size(); i++) {
//            System.out.println(recipeParaList.get(i).getParaName()+":"+recipeParaList.get(i).getSetValue());
//        }


        String file = "2017/10/24  16:20                94 te st.txt";
        String[] names = file.replace("te st.txt","").split("\\s+");
        for(String  s :names){
            System.out.println(s);
        }

    }

}
