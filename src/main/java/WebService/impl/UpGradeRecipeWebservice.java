package WebService.impl;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 12631 on 2019/3/1.
 *
 */
public class UpGradeRecipeWebservice implements BaseWebservice {
    private static final Logger logger = Logger.getLogger(UpGradeRecipeWebservice.class.getName());
    private Recipe recipe;
    private List<RecipePara> recipeParas;
    private List<Attach> attachs;

    @Override
    public String  handle(String message) {
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);

        try {
            recipe = (Recipe) JsonMapper.fromJsonString(JSONObject.toJSON(map.get("recipe")).toString(), Recipe.class);


            recipeParas = (List<RecipePara>) JsonMapper.String2List(JSON.toJSONString(map.get("recipeParaList")), RecipePara.class);
            attachs = (List<Attach>) JsonMapper.String2List(JSON.toJSONString(map.get("attachsList")), Attach.class);

            UiLogUtil.appendLog2SeverTab(recipe.getDeviceCode(), "收到MQ消息，服务端请求将recipe " + recipe.getRecipeName() + " 版本升级为" + recipe.getVersionType() + ",版本号" + recipe.getVersionNo());
        } catch (Exception ex) {
            logger.error("JMSException:", ex);
        }
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        Map webMap = new HashMap();
        webMap.put("msgName", "ReplyMessage");
        webMap.put("eventName", "升级recipe");
        try {
            if ("DB-800HSD".equals(recipe.getDeviceTypeCode())) {
                recipeService.saveUpGradeRcpInfoForDB800(recipe, recipeParas, attachs);
            } else {
                recipeService.saveUpGradeRcpInfo(recipe, recipeParas, attachs);
            }
            sqlSession.commit();
            webMap.put("eventDesc", "将recipe " + recipe.getRecipeName() + " 版本升级为" + recipe.getVersionType() + ",版本号" + recipe.getVersionNo() + " 升级成功");
            UiLogUtil.appendLog2SeverTab(recipe.getDeviceCode(), "recipe升级成功");

        } catch (Exception e) {
            sqlSession.rollback();
            logger.error("Exception:", e);
            webMap.put("eventDesc", "将recipe " + recipe.getRecipeName() + " 版本升级为" + recipe.getVersionType() + ",版本号" + recipe.getVersionNo() + " 升级失败");
            UiLogUtil.appendLog2SeverTab(recipe.getDeviceCode(), "recipe升级失败");

        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
    }
}
