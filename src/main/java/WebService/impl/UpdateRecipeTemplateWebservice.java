package WebService.impl;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.messageHandlers.UpdateRecipeTemplateHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Created by 12631 on 2019/3/1.
 */
public class UpdateRecipeTemplateWebservice implements BaseWebservice {
    private static Logger logger = Logger.getLogger(UpdateRecipeTemplateHandler.class);
    private String deviceCode = "";
    private String deviceTypeId = "";
    private List<RecipeTemplate> recipeTemplates;

    @Override
    public String handle(String message) {
        Map webMap = new HashMap();
        try {
            HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);

            deviceCode = String.valueOf(map.get("deviceCode"));
            deviceTypeId = String.valueOf(map.get("deviceTypeId"));
            recipeTemplates = (List<RecipeTemplate>) JsonMapper.String2List(JSON.toJSONString(map.get("recipeTemplate")), RecipeTemplate.class);
            logger.info("设备" + deviceCode + "请求更新RecipeTemplate表");
           UiLogUtil.getInstance().appendLog2SeverTab(null, "接收到服务端更新RecipeTemplate配置请求");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);

        webMap.put("msgName", "UpdateRecipeTemplate");
        webMap.put("eventName", "更新RecipeTemplate");
        try {
            if (recipeTemplates != null && !recipeTemplates.isEmpty()) {
                recipeService.deleteRecipeTemplateByDeviceTypeCodeBatch(recipeTemplates);
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    recipeTemplate.setDeviceTypeId(deviceTypeId);
                }
//                recipeService.saveRcpTemplateBatch(recipeTemplates);
                List<RecipeTemplate> recipeTemplatesTmp = new ArrayList<>();
                for (int i = 0; i < recipeTemplates.size(); i++) {
                    recipeTemplatesTmp.add(recipeTemplates.get(i));
                    if (recipeTemplatesTmp.size() > 500) {
                        recipeService.saveRcpTemplateBatch(recipeTemplatesTmp);
                        recipeTemplatesTmp.clear();
                        sqlSession.commit();
                    }
                }
                recipeService.saveRcpTemplateBatch(recipeTemplatesTmp);
            }
            sqlSession.commit();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "RecipeTemplate配置更新成功");
            webMap.put("eventDesc", "RecipeTemplate配置更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "RecipeTemplate配置更新失败");
            webMap.put("eventDesc", "RecipeTemplate配置更新失败");
        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
    }
}

