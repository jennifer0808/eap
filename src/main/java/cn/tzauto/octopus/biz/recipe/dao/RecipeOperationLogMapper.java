package cn.tzauto.octopus.biz.recipe.dao;

import cn.tzauto.octopus.biz.recipe.domain.RecipeOperationLog;
import java.util.List;
import java.util.Map;

public interface RecipeOperationLogMapper {

    int deleteByPrimaryKey(String id);

    int insert(RecipeOperationLog record);

    int insertSelective(RecipeOperationLog record);

    RecipeOperationLog selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(RecipeOperationLog record);

    int updateByPrimaryKey(RecipeOperationLog record);

    List<RecipeOperationLog> searchByMap(Map paraMap);

    List<RecipeOperationLog> selectOldData(String savedDays);

    int deleteOpLogBatch(List<RecipeOperationLog> recipeOperationLogs);

}
