/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import java.util.List;

/**
 *
 * @author luosy
 */
public interface IRecipeTransfer {

    List<RecipePara> transferRecipeParaFromDB(String filePath, String deviceType);

    void edit(Recipe recipe, String deviceType, String localRecipeFilePath);

    void editRecipeFile(Recipe recipe, String deviceType, String localRecipeFilePath);
}
