package cn.tzauto.octopus.biz.recipe.dao;

import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;

import java.util.List;
import java.util.Map;

public interface RecipeTemplateMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(RecipeTemplate record);

    int insertSelective(RecipeTemplate record);

    RecipeTemplate selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(RecipeTemplate record);

    int updateByPrimaryKey(RecipeTemplate record);

    List<String> searchShotSVByMap(Map paraMap);

    List<RecipeTemplate> searchByMap(Map paraMap);

    List<RecipeTemplate> searchByMapOrderByParaCode(Map paraMap);

    List<RecipeTemplate> searchMonitorByMapOrderByParaCode(Map paraMap);

    List<RecipeTemplate> searchRecipeTemplateMonitor(Map paraMap);

    List<RecipeTemplate> searchPressRecipeTemplateByDeviceCode(Map paraMap);

    int saveRecipeTemplateBatch(List<RecipeTemplate> recipeTemplates);

    int deleteRecipeTemplateByDeviceTypeIdBatch(List<RecipeTemplate> recipeTemplates);

    int deleteRecipeTemplateByDeviceTypeCodeBatch(List<RecipeTemplate> recipeTemplates);

    int deleteRecipeTemplateByIdBatch(List<RecipeTemplate> recipeTemplates);

    List<RecipeTemplate> searchByType(Map paraMap);
}
