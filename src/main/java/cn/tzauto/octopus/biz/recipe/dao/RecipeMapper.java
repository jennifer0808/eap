package cn.tzauto.octopus.biz.recipe.dao;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;

import java.util.List;
import java.util.Map;

public interface RecipeMapper {

    int deleteByPrimaryKey(String id);

    int insert(Recipe record);

    int insertSelective(Recipe record);

    Recipe selectByPrimaryKey(String id);

    List<Recipe> searchRecipeByRcpType(Map paMap);

    Recipe searchRecipeByPaExtMap(Map extMap);

    int updateByPrimaryKeySelective(Recipe record);

    int updateByPrimaryKeyWithBLOBs(Recipe record);

    int updateByPrimaryKey(Recipe record);

    List<Recipe> searchByMap(Map paraMap);

    List<Recipe> searchByPaMap(Map paraMap);

    List<Recipe> searchByMapVerNo(Map paraMap);

    List<Recipe> searchRecipeRecent(String clientId);

    public List<String> getAllRecipeName(String deviceCode);
    
    int deleteRcp(Recipe record);

    String getUUID();

    int saveRecipeBatch(List<Recipe> recipes);

    int deleteRecipeByIdBatch(List<Recipe> recipes);
    
}