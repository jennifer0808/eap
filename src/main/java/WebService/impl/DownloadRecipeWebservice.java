package WebService.impl;


import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeOperationLog;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.messageHandlers.DownLoadHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//下载接口
public class DownloadRecipeWebservice implements BaseWebservice {
    private static Logger logger = Logger.getLogger(DownLoadHandler.class.getName());
    private DeviceInfo deviceInfo = new DeviceInfo();
    private Recipe recipe = null;
    private List<RecipePara> recipeParaList = null;
    private List<Attach> attachs = null;
    private Recipe recipeGold = null;
    private List<RecipePara> recipeParaListGold = null;
    private List<Attach> attachGolds = null;
    private String deviceCode = "";

    @Override
    public String handle(String message) {
        String eventId = "";
        try {
            HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n",""),HashMap.class);

            eventId = String.valueOf(map.get("eventId"));
            deviceCode =  String.valueOf(map.get("deviceCode"));
            recipeParaList = (List<RecipePara>) JsonMapper.String2List(JSON.toJSONString(map.get("recipeParaList")), RecipePara.class);
            attachs = (List<Attach>) JsonMapper.String2List(JSON.toJSONString(map.get("attachsList")), Attach.class);

            recipe = (Recipe) JsonMapper.fromJsonString(JSONObject.toJSON(map.get("recipe")).toString(), Recipe.class);

            if (map.get("recipeGold") != null) {
                recipeGold = (Recipe) JsonMapper.fromJsonString(JSONObject.toJSON(map.get("recipeGold")).toString(), Recipe.class);
                recipeParaListGold = (List<RecipePara>) JsonMapper.String2List(JSON.toJSONString(map.get("recipeParaGold")), RecipePara.class);
                attachGolds = (List<Attach>) JsonMapper.String2List(JSON.toJSONString(map.get("arAttachGold")), Attach.class);
            }
            UiLogUtil.appendLog2SeverTab(deviceCode, "收到MQ消息，服务端请求下载recipe " + recipe.getRecipeName() + " 到设备");
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if (recipe != null && !"Engineer".equalsIgnoreCase(recipe.getVersionType())) {
            //判断服务端发来的数据是否有GOLD版本
            if (!"GOLD".equalsIgnoreCase(recipe.getVersionType())) {
                if (recipeGold == null || !"GOLD".equalsIgnoreCase(recipeGold.getVersionType())) {
                    UiLogUtil.appendLog2SeverTab(deviceCode, "服务端没有下载recipe " + recipe.getRecipeName() + " 的Gold版本到设备，请联系ME处理");
                }
            }
        }

        //将服务端发来的数据同步到数据库
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        Map webMap = new HashMap();
        webMap.put("msgName", "ReplyMessage");
        webMap.put("eventId", eventId);
        webMap.put("deviceCode", deviceCode);
        webMap.put("eventName", "recipe下载结果");
        try {
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(recipe.getCreateDate());
            Date currentTime_2 = formatter.parse(dateString);
            recipe.setCreateDate(currentTime_2);

            if (recipeParaList != null && attachs != null) {
                recipeService.deleteRcpByPrimaryKey(recipe.getId());
                logger.info("recipe删除成功");
                recipeService.saveRecipe(recipe);
                logger.info("recipe保存成功");
                recipeService.deleteRcpParaBatch(recipeParaList);
                recipeService.deleteRcpParaByRecipeId(recipe.getId());
                logger.info("recipePara批量删除成功");
                if (deviceInfo.getDeviceType().contains("HITACHIDB8")) {
                    recipeService.saveRcpParaBatchForDB800(recipeParaList);
                } else {
                    recipeService.saveRcpParaBatch(recipeParaList);
                }
                logger.info("recipePara批量保存成功");
                recipeService.deleteAttachByRcpRowId(recipe.getId());
                for (Attach attach : attachs) {
                    recipeService.deleteAttachByPrimaryKey(attach.getId());
                    recipeService.saveAttach(attach);
                }
                logger.info("attach删除并保存成功");
            }
            if (recipeGold != null && recipeParaListGold != null && attachGolds != null) {
                recipeService.deleteRcpByPrimaryKey(recipeGold.getId());
                logger.info("recipeGold删除成功");
                recipeService.saveRecipe(recipeGold);
                logger.info("recipeGold保存成功");
                recipeService.deleteRcpParaBatch(recipeParaListGold);
                recipeService.deleteRcpParaByRecipeId(recipeGold.getId());
                logger.info("recipeParaListGold批量删除成功");
                if (deviceInfo.getDeviceType().contains("HITACHIDB8")) {
                    recipeService.saveRcpParaBatchForDB800(recipeParaListGold);
                } else {
                    recipeService.saveRcpParaBatch(recipeParaListGold);
                }
                logger.info("recipeParaListGold批量保存成功");
                for (Attach attachGold : attachGolds) {
                    recipeService.deleteAttachByRcpRowId(recipeGold.getId());
                    recipeService.deleteAttachByPrimaryKey(attachGold.getId());
                    recipeService.saveAttach(attachGold);
                }
                logger.info("attachGold删除并保存成功");
            }
            sqlSession.commit();

            //更新并获取模型表信息
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);

            //recipe下载分为4种方式
            //1、Download   只下载
            //2、Select 做PPSelect
            //3、DeleteDownloadSelect 删除当前，并且下载当前的Recipe
            //4、DeleteAllDownloadSelect 删除设备上所有的，并且下载当前的，并且选中
            if (deviceInfoExt != null && deviceInfoExt.getRecipeDownloadMod() != null && !"".equals(deviceInfoExt.getRecipeDownloadMod())) {
                logger.info("设备模型表中配置设备" + deviceCode + "的Recipe下载方式为" + deviceInfoExt.getRecipeDownloadMod());
                DeviceType deviceType = deviceService.queryDeviceTypeById(deviceInfo.getDeviceTypeId());
                String downLoadResultString = "";
                //验证机台状态
                MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
                String deviceId = deviceInfo.getDeviceId();
                String recipeName = recipe.getRecipeName();
                downLoadResultString = hostManager.checkBeforeDownload(deviceId, recipeName);
                if ("0".equals(downLoadResultString)) {
                    if ("2".equals(deviceType.getProtocolType())) {
                        downLoadResultString = recipeService.downLoadRcp2ISECSDeviceByTypeAutomatic(deviceInfo, recipe, deviceInfoExt.getRecipeDownloadMod());
                    } else {
                        downLoadResultString = recipeService.downLoadRcp2DeviceByTypeAutomatic(deviceInfo, recipe, deviceInfoExt.getRecipeDownloadMod());
                    }
                }
                //根据不同的下载模式，下载recipe
                RecipeOperationLog recipeOperationLog = recipeService.setRcpOperationLog(recipe, "autodownload");
                if (downLoadResultString.equals("0")) {
                    deviceInfoExt.setRecipeId(recipe.getId());
                    deviceInfoExt.setRecipeName(recipe.getRecipeName());
                    deviceInfoExt.setVerNo(recipe.getVersionNo());
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                    webMap.put("eventDesc", "下载成功!");
                    recipeOperationLog.setOperationResult("Y");
                    webMap.put("downloadResult", "Y");
                    UiLogUtil.appendLog2SeverTab(deviceCode, "下载成功!");
                } else {
                    webMap.put("eventDesc", "下载失败," + downLoadResultString);
                    webMap.put("downloadResult", "N");
                    recipeOperationLog.setOperationResult("N");
                    recipeOperationLog.setOperationResultDesc(downLoadResultString);
                    UiLogUtil.appendLog2SeverTab(deviceCode, "下载失败," + downLoadResultString);
                }
                //保存下载结果至数据库并发送至服务端
                recipeService.saveRecipeOperationLog(recipeOperationLog);

            } else {
                logger.error("设备模型表中没有配置设备" + deviceCode + "的Recipe下载方式");
                webMap.put("eventDesc", "下载失败");
                webMap.put("downloadResult", "N");
                UiLogUtil.appendLog2SeverTab(deviceCode, "设备模型表中没有配置该设备的Recipe下载方式，请联系ME处理！");
            }
            sqlSession.commit();
            return JSONObject.toJSON(webMap).toString();
        } catch (ParseException e) {
            sqlSession.rollback();
            logger.error("Exception:", e);
            webMap.put("eventDesc", "下载失败");
            webMap.put("downloadResult", "N");
            return JSONObject.toJSON(webMap).toString();
        } finally {

            sqlSession.close();
        }
    }

    }

