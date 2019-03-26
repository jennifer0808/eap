package cn.tzauto.octopus.biz.recipe.dao;

import cn.tzauto.octopus.biz.recipe.domain.Attach;

import java.util.List;

public interface AttachMapper {

    int deleteByPrimaryKey(String id);
    
    int deleteByRcpRowId(String recipeRowId);

    int delete(Attach record);

    int insert(Attach record);

    int insertSelective(Attach record);

    Attach selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(Attach record);

    int updateByPrimaryKey(Attach record);

    List<Attach> searchByRecipeRowId(String record);
}