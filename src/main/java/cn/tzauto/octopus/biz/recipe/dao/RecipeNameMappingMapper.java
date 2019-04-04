package cn.tzauto.octopus.biz.recipe.dao;


import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;

import java.util.List;
import java.util.Map;

public interface RecipeNameMappingMapper {

    int deleteByPrimaryKey(String id);

    int insert(RecipeNameMapping record);

    int insertSelective(RecipeNameMapping record);

    RecipeNameMapping selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(RecipeNameMapping record);

    int updateByPrimaryKey(RecipeNameMapping record);

    List<RecipeNameMapping> searchRcpNameByDeviceCodeAndShotName(Map paraMap);
}