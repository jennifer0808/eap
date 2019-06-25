package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

public class UpdateRecipeTemplateHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpdateRecipeTemplateHandler.class);
    private String deviceCode = "";
    private String deviceTypeId = "";
    private List<RecipeTemplate> recipeTemplates;

    @Override
    public void handle(Message message) {
        try {
            MapMessage mapMessage = (MapMessage) message;
            deviceCode = mapMessage.getString("deviceCode");
            deviceTypeId = mapMessage.getString("deviceTypeId");
            recipeTemplates = (List<RecipeTemplate>) JsonMapper.String2List(mapMessage.getString("recipeTemplate"), RecipeTemplate.class);
            logger.info("设备" + deviceCode + "请求更新RecipeTemplate表");
           UiLogUtil.getInstance().appendLog2SeverTab(null, "接收到服务端更新RecipeTemplate配置请求");
        } catch (JMSException e) {
            e.printStackTrace();
        }
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
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "RecipeTemplate配置更新失败");
        } finally {
            sqlSession.close();
        }
    }
}
