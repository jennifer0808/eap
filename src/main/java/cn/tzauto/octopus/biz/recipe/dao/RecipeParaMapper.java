package cn.tzauto.octopus.biz.recipe.dao;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import java.util.List;
import java.util.Map;

public interface RecipeParaMapper {
    
    int deleteByRcpRowId(String id);
    
    int insert(RecipePara record);
    
    int insertSelective(RecipePara record);
    
    RecipePara selectByPrimaryKey(String id);
    
    int updateByPrimaryKeySelective(RecipePara record);
    
    int updateByPrimaryKey(RecipePara record);
    
    List<RecipePara> searchByRcpRowId(String recipeRowId);
    
    List<RecipePara> searchByMap(Map paraMap);
    
    List<String> searchByMapWithRcpTemp(Map paraMap);
    
    int saveRcpParaBatch(List<RecipePara> recipeParas);
    
    int modifyRcpParaBatch(List<RecipePara> recipeParas);
    
    int deleteRcpParaBatch(List<RecipePara> recipeParas);

    List<RecipePara> selectOldData(String savedDays);
}
