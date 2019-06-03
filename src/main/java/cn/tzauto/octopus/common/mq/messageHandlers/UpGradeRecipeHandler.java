/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author RAIN
 * @
 */
public class UpGradeRecipeHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpGradeRecipeHandler.class);
    private Recipe recipe;
    private List<RecipePara> recipeParas;
    private List<Attach> attachs;

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        try {
            recipe = (Recipe) JsonMapper.fromJsonString(mapMessage.getString("recipe"), Recipe.class);
            recipeParas = (List<RecipePara>) JsonMapper.String2List(mapMessage.getString("recipePara"), RecipePara.class);
            attachs = (List<Attach>) JsonMapper.String2List(mapMessage.getString("arAttach"), Attach.class);
           UiLogUtil.getInstance().appendLog2SeverTab(recipe.getDeviceCode(), "收到MQ消息，服务端请求将recipe " + recipe.getRecipeName() + " 版本升级为" + recipe.getVersionType() + ",版本号" + recipe.getVersionNo());
        } catch (JMSException ex) {
            logger.error("JMSException:", ex);
        }
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "ReplyMessage");
        mqMap.put("eventName", "升级recipe");
        try {
            if ("DB-800HSD".equals(recipe.getDeviceTypeCode())) {
                recipeService.saveUpGradeRcpInfoForDB800(recipe, recipeParas, attachs);
            } else {
                recipeService.saveUpGradeRcpInfo(recipe, recipeParas, attachs);
            }
            sqlSession.commit();
            mqMap.put("eventDesc", "将recipe " + recipe.getRecipeName() + " 版本升级为" + recipe.getVersionType() + ",版本号" + recipe.getVersionNo() + " 升级成功");
           UiLogUtil.getInstance().appendLog2SeverTab(recipe.getDeviceCode(), "recipe升级成功");
        } catch (Exception e) {
            sqlSession.rollback();
            logger.error("Exception:", e);
            mqMap.put("eventDesc", "将recipe " + recipe.getRecipeName() + " 版本升级为" + recipe.getVersionType() + ",版本号" + recipe.getVersionNo() + " 升级失败");
           UiLogUtil.getInstance().appendLog2SeverTab(recipe.getDeviceCode(), "recipe升级失败");
        } finally {
            sqlSession.close();
        }
    }
}
