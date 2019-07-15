/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
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
 */
public class UpGradeRecipeHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpGradeRecipeHandler.class);


    @Override
    public void handle(Message message) {
        Recipe recipe = null;
        List<RecipePara> recipeParas = null;
        List<Attach> attachs = null;
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
            //同步更新ext表里的recipe glod版本
            if ("GOLD".equalsIgnoreCase(recipe.getVersionType()) && ("DB-800HSD".equals(recipe.getDeviceTypeCode()) || "DB730".equals(recipe.getDeviceTypeCode()))) {
                this.updateRecipeGoldVersion(recipe, sqlSession);
            }
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

    public void updateRecipeGoldVersion(Recipe downloadRecipe, SqlSession sqlSession) throws Exception {
        DeviceService deviceService = new DeviceService(sqlSession);
        List<DeviceInfoExt> deviceInfoExts = deviceService.getDeviceInfoExtByRecipeName(downloadRecipe.getRecipeName());
        Recipe goldRecipe = null;
        if (deviceInfoExts.isEmpty()) {
            logger.info("当前没有机台使用升级的GLOD版Recipe作业,无需更新版本号!");
        } else {
            for (DeviceInfoExt deviceInfoExt : deviceInfoExts) {
                deviceInfoExt.setVerNo(goldRecipe.getVersionNo());
                UiLogUtil.getInstance().appendLog2SeverTab(deviceInfoExt.getDeviceRowid(), "Recipe:[" + downloadRecipe.getRecipeName() + "]版本升级，版本号更新为：" + downloadRecipe.getVersionNo());
                deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
        }
        sqlSession.commit();
    }
}
