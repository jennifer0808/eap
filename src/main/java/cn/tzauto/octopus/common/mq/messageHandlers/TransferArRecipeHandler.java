/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;

import java.util.List;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class TransferArRecipeHandler implements MessageHandler {

    private static final Logger logger = Logger.getLogger(TransferArRecipeHandler.class.getName());
    private List<Recipe> recipes;
    private String deviceCode;
//    private List<RecipePara> recipeParas;

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        try {
            recipes = (List<Recipe>) JsonMapper.String2List(mapMessage.getString("arRecipe"), Recipe.class);
            deviceCode = mapMessage.getString("deviceCode");
//            recipeParas = (List<RecipePara>) JsonMapper.String2List(mapMessage.getString("arRecipePara"), RecipePara.class);
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求同步该设备的Recipe信息，RecipeName为" + recipes.get(0).getRecipeName());
//            UiLogUtil.appendLog2SeverTab(deviceCode, "Recipes大小为:" + recipes.size() + "  RecipeParas大小为:" + recipeParas.size());
//            GlobalConstants.rcpParaNumber = GlobalConstants.rcpParaNumber + recipeParas.size();
//            UiLogUtil.appendLog2SeverTab(deviceCode, "RecipePara总数：" + GlobalConstants.rcpParaNumber);
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
//        SqlSession batchSqlSession = MybatisSqlSession.getBatchSqlSession();
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
//            if (recipeParas != null && !recipeParas.isEmpty()) {
//                recipeService.deleteRcpParaBatch(recipeParas);
//                for (int i = 0; i < recipeParas.size(); i++) {
//                    recipeService.saveRecipePara(recipeParas.get(i));
//                    if (i % 500 == 0) {
//                        batchSqlSession.commit();
//                    }
//                }
//            }
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }
}
