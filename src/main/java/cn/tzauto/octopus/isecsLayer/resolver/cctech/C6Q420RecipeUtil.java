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
import cn.tzauto.octopus.isecsLayer.equipImpl.vision.VSP88AHHost;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author SunTao
 * @version V1.0
 * @desc
 */
public class C6Q420RecipeUtil {
    private static Logger logger = Logger.getLogger(VSP88AHHost.class);

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

}
