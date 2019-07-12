/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;


/**
 *
 * @author luosy
 */
public class UpLoadHandler implements MessageHandler {

//    private static Logger logger = Logger.getLogger(UpLoadHandler.class);
    private static Logger logger = LoggerFactory.getLogger(UpLoadHandler.class);

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            String deviceCode = mapMessage.getString("deviceCode");
            String recipeName = mapMessage.getString("recipeName");
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求上传Recipe:[" + recipeName + "]");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            String deviceId = deviceInfo.getDeviceCode();
            Recipe recipe = new Recipe();
            List<RecipePara> recipeParaList = new ArrayList<>();
            Map recipeMap = hostManager.getRecipeParaFromDevice(deviceId, recipeName);
            RecipeNameMapping recipeNameMapping = new RecipeNameMapping();
            if (recipeMap != null) {
                logger.info("成功获取到recipe信息，开始上传");
                recipe = (Recipe) recipeMap.get("recipe");
                recipeParaList = (List<RecipePara>) recipeMap.get("recipeParaList");
                recipeNameMapping = (RecipeNameMapping) recipeMap.get("recipeNameMapping");
            }
            recipeParaList = recipeService.saveUpLoadRcpInfo(recipe, recipeParaList);
            sqlSession.commit();
            Map mqMap = new HashMap();
            mqMap.put("msgName", "UpLoad");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipe", JSONArray.toJSONString(recipe));
            mqMap.put("recipeParaList", JSONArray.toJSONString(recipeParaList));
            mqMap.put("recipeNameMapping", JSONArray.toJSONString(recipeNameMapping));
            StringBuilder stringBuilder = new StringBuilder();
            if(recipe.getDeviceTypeCode().contains("CCTECH")){
                for(RecipePara recipePara:recipeParaList){
                    if("N".equals(recipePara.getRemarks())){
                        stringBuilder.append(recipePara.getParaName()+",");
                    }
                }
                if(!"".equals(stringBuilder.toString())){
                    logger.info(stringBuilder.toString()+"各个参数值为空");
                }
            }
            mqMap.put("errorRecipePara",stringBuilder.toString());
            logger.info("开始获取Attach信息");
            List<Attach> attList=hostManager.getRecipeAttachInfo(deviceId, recipe);
            if(attList==null){
                logger.info("获取Attach失败，重新开始获取");
                attList=getRecipeAttachInfo(recipe);
            }
            mqMap.put("attach", JSONArray.toJSONString(attList));
            Destination destination = message.getJMSReplyTo();
            logger.info("destination:================================ " + destination.toString());
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            logger.info("topicName:==========================================" + topicName);
            GlobalConstants.C2SRcpUpLoadQueue.sendMessage(topicName, mqMap);
            logger.info("向服务端[" + topicName + "]回复获取到的Recipe信息" + JSONArray.toJSONString(mqMap));
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送获取到的Recipe信息");
        } catch (JMSException ex) {
            logger.error("Exception:", ex);
        } catch (UploadRecipeErrorException e) {
            e.printStackTrace();
        } finally {
            sqlSession.close();
        }

    }

    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        List<Attach> attachs = new ArrayList<>();
        Attach attach = new Attach();
        attach.setId(UUID.randomUUID().toString());
        attach.setRecipeRowId(recipe.getId());
        attach.setAttachName(recipe.getRecipeName().replaceAll("/", "@") + "_V" + recipe.getVersionNo());
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        attach.setAttachPath(recipeService.organizeUploadRecipePath(recipe));
        sqlSession.close();
        attach.setAttachType("txt");
        attach.setSortNo(0);
        if (GlobalConstants.sysUser != null) {
            attach.setCreateBy(GlobalConstants.sysUser.getId());
            attach.setUpdateBy(GlobalConstants.sysUser.getId());
        } else {
            attach.setCreateBy("System");
            attach.setUpdateBy("System");
        }
        attachs.add(attach);
        return attachs;
    }
}
