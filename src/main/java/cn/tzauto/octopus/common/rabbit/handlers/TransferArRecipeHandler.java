/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author Administrator
 */
public class TransferArRecipeHandler implements MessageHandler {

    private static final Logger logger = Logger.getLogger(TransferArRecipeHandler.class.getName());
    private List<Recipe> recipes;
    private String deviceCode;

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        recipes = (List<Recipe>) JsonMapper.String2List(msgMap.get("arRecipe"), Recipe.class);
        deviceCode = msgMap.get("deviceCode");
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求同步该设备的Recipe信息，RecipeName为" + recipes.get(0).getRecipeName());

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            if (recipes != null && !recipes.isEmpty()) {
                recipeService.deleteRecipeByIdBatch(recipes);
                recipeService.saveRecipeBatch(recipes);
                for (Recipe recipe : recipes) {
                    recipeService.deleteRcpParaByRecipeId(recipe.getId());
                }
            }
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }
}
