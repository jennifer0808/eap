/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * @author luosy
 * @ 2017/08/02
 */
public class UpLoadHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpLoadHandler.class.getName());

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            String deviceCode = msgMap.get("deviceCode");
            String recipeName = msgMap.get("recipeName");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求上传Recipe:[" + recipeName + "]");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            String deviceId = deviceInfo.getDeviceCode();
            Recipe recipe = new Recipe();
            List<RecipePara> recipeParaList = new ArrayList<>();
            Map recipeMap = hostManager.getRecipeParaFromDevice(deviceId, recipeName);
            if (recipeMap != null) {
                logger.info("成功获取到recipe信息，开始上传");
                recipe = (Recipe) recipeMap.get("recipe");
                recipeParaList = (List<RecipePara>) recipeMap.get("recipeParaList");
            }
            recipeParaList = recipeService.saveUpLoadRcpInfo(recipe, recipeParaList);
            sqlSession.commit();
            Map mqMap = new HashMap();
            mqMap.put("msgName", "UpLoad");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipe", JSONArray.toJSONString(recipe));
            mqMap.put("recipeParaList", JSONArray.toJSONString(recipeParaList));
            logger.info("开始获取Attach信息");
            List<Attach> attList = hostManager.getRecipeAttachInfo(deviceId, recipe);
            if (attList == null) {
                logger.info("获取Attach失败，重新开始获取");
                attList = getRecipeAttachInfo(recipe);
            }
            mqMap.put("attach", JSONArray.toJSONString(attList));

            GlobalConstants.C2SRcpUpLoadQueue.sendMessage(mqMap);
            logger.info("向服务端[C2S.Q.RCPUPLOAD]回复获取到的Recipe信息" + JSONArray.toJSONString(mqMap));
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送获取到的Recipe信息");
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
