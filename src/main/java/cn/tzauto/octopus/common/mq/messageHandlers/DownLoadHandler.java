package cn.tzauto.octopus.common.mq.messageHandlers;

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
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownLoadHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(DownLoadHandler.class);
    private DeviceInfo deviceInfo = new DeviceInfo();
    private Recipe recipe = null;
    private List<RecipePara> recipeParaList = null;
    private List<Attach> attachs = null;
    private Recipe recipeGold = null;
    private List<RecipePara> recipeParaListGold = null;
    private List<Attach> attachGolds = null;
    private String deviceCode = "";

    @Override
    public void handle(Message message) {
        String eventId = "";
        try {
            MapMessage mapMessage = (MapMessage) message;
            eventId = mapMessage.getString("eventId");
            deviceCode = mapMessage.getString("deviceCode");
            recipe = (Recipe) JsonMapper.fromJsonString(mapMessage.getString("recipe"), Recipe.class);
            recipeParaList = (List<RecipePara>) JsonMapper.String2List(mapMessage.getString("recipePara"), RecipePara.class);
            attachs = (List<Attach>) JsonMapper.String2List(mapMessage.getString("arAttach"), Attach.class);
            if (mapMessage.getString("recipeGold") != null) {
                recipeGold = (Recipe) JsonMapper.fromJsonString(mapMessage.getString("recipeGold"), Recipe.class);
                recipeParaListGold = (List<RecipePara>) JsonMapper.String2List(mapMessage.getString("recipeParaGold"), RecipePara.class);
                attachGolds = (List<Attach>) JsonMapper.String2List(mapMessage.getString("arAttachGold"), Attach.class);
            }
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "收到MQ消息，服务端请求下载recipe " + recipe.getRecipeName() + " 到设备");
        } catch (JMSException e) {
            logger.error("Exception:", e);
        }
        if (recipe != null && !"Engineer".equalsIgnoreCase(recipe.getVersionType())) {
            //判断服务端发来的数据是否有GOLD版本
            if (!"GOLD".equalsIgnoreCase(recipe.getVersionType())) {
                if (recipeGold == null || !"GOLD".equalsIgnoreCase(recipeGold.getVersionType())) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端没有下载recipe " + recipe.getRecipeName() + " 的Gold版本到设备，请联系ME处理");
                    return;
                }
            }
        }

        //将服务端发来的数据同步到数据库
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "ReplyMessage");
        mqMap.put("eventId", eventId);
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("eventName", "recipe下载结果");
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

                recipeService.saveRcpParaBatch(recipeParaList);

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
                String deviceId = deviceInfo.getDeviceCode();
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
                //0:表示下载成功，1:表示机台中已是当前程序，其他下载失败
                if (downLoadResultString.equals("0")) {
                    mqMap.put("eventDesc", "下载成功！");
                    recipeOperationLog.setOperationResult("Y");
                    mqMap.put("downloadResult", "Y");
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "下载成功！");
                    //不管成功与否都更新ext表
                    deviceInfoExt.setRecipeId(recipe.getId());
                    deviceInfoExt.setRecipeName(recipe.getRecipeName());
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                } else if (downLoadResultString.equals("1")) {
                    mqMap.put("eventDesc", "下载取消,正在使用预下载的Recipe!");
                    mqMap.put("downloadResult", "N");
                    recipeOperationLog.setOperationResult("N");
                    recipeOperationLog.setOperationResultDesc("正在使用预下载的Recipe!");
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "下载取消,正在使用预下载的Recipe!");
                    //不管成功与否都更新ext表
                    deviceInfoExt.setRecipeId(recipe.getId());
                    deviceInfoExt.setRecipeName(recipe.getRecipeName());
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                } else if (downLoadResultString.equals("2")) {
                    mqMap.put("eventDesc", "下载成功,选中失败!");
                    mqMap.put("downloadResult", "Y");
                    recipeOperationLog.setOperationResult("Y");
                    recipeOperationLog.setOperationResultDesc("下载成功,选中失败!");
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "下载成功,选中失败!");
                    deviceInfoExt.setRecipeId(recipe.getId());
                    deviceInfoExt.setRecipeName(recipe.getRecipeName());
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                } else {
                    mqMap.put("eventDesc", "下载失败," + downLoadResultString);
                    mqMap.put("downloadResult", "N");
                    recipeOperationLog.setOperationResult("N");
                    recipeOperationLog.setOperationResultDesc(downLoadResultString);
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "下载失败," + downLoadResultString);
                }
                //保存下载结果至数据库并发送至服务端
                recipeService.saveRecipeOperationLog(recipeOperationLog);
                GlobalConstants.C2SRcpDownLoadQueue.sendMessage(mqMap);
            } else {
                logger.error("设备模型表中没有配置设备" + deviceCode + "的Recipe下载方式");
                mqMap.put("eventDesc", "下载失败");
                mqMap.put("downloadResult", "N");
                GlobalConstants.C2SRcpDownLoadQueue.sendMessage(mqMap);
                UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "设备模型表中没有配置该设备的Recipe下载方式，请联系ME处理！");
            }
            sqlSession.commit();
        } catch (ParseException e) {
            sqlSession.rollback();
            logger.error("Exception:", e);
            mqMap.put("eventDesc", "下载失败");
            mqMap.put("downloadResult", "N");
            GlobalConstants.C2SRcpDownLoadQueue.sendMessage(mqMap);
        } finally {
            sqlSession.close();
        }
    }
}
