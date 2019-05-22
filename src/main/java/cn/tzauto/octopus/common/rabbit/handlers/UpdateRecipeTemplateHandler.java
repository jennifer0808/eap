package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateRecipeTemplateHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpdateRecipeTemplateHandler.class.getName());
    private String deviceCode = "";
    private String deviceTypeId = "";
    private List<RecipeTemplate> recipeTemplates;


    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {

        deviceCode = msgMap.get("deviceCode");
        deviceTypeId = msgMap.get("deviceTypeId");
        recipeTemplates = (List<RecipeTemplate>) JsonMapper.String2List(msgMap.get("recipeTemplate"), RecipeTemplate.class);
        logger.info("设备" + deviceCode + "请求更新RecipeTemplate表");
        UiLogUtil.getInstance().appendLog2SeverTab(null, "接收到服务端更新RecipeTemplate配置请求");

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "UpdateRecipeTemplate");
        mqMap.put("eventName", "更新RecipeTemplate");
        try {
            if (recipeTemplates != null && !recipeTemplates.isEmpty()) {
                recipeService.deleteRecipeTemplateByDeviceTypeCodeBatch(recipeTemplates);
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    recipeTemplate.setDeviceTypeId(deviceTypeId);
                }
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
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
            UiLogUtil.getInstance().appendLog2SeverTab(null, "RecipeTemplate配置更新失败");
        } finally {
            sqlSession.close();
        }
    }
}
