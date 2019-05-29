package WebService.impl;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;


public class BackUpRecipeWebservice implements BaseWebservice {
    private static Logger logger = Logger.getLogger(BackUpRecipeWebservice.class.getName());
    private DeviceInfo deviceInfo = new DeviceInfo();
    private Recipe recipe = null;
    private List<RecipePara> recipeParaList = null;
    private List<Attach> attachs = null;
    private String deviceCode = "";
    List<Recipe> recipeList;
    public String handle(String message){
       UiLogUtil.getInstance().appendLog2SeverTab("ALL", "收到消息，服务端请求备份recipe");
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n",""),HashMap.class);
        Map webMap = new HashMap();
        String eventId = "";
            eventId= String.valueOf(map.get("eventId"));
             deviceCode= String.valueOf(map.get("deviceCode"));
            recipeList = (List<Recipe>) JsonMapper.String2List(JSON.toJSONString(map.get("recipeList")), Recipe.class);
        recipeParaList = (List<RecipePara>) JsonMapper.String2List(JSON.toJSONString(map.get("recipeParaList")), RecipePara.class);
        attachs = (List<Attach>) JsonMapper.String2List(JSON.toJSONString(map.get("attachsList")), Attach.class);

           UiLogUtil.getInstance().appendLog2SeverTab("ALL", "收到MQ消息，服务端请求备份recipe");
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);

        webMap.put("msgName", "ReplyMessage");
        webMap.put("eventId", eventId);
        webMap.put("deviceCode", deviceCode);
        webMap.put("eventName", "recipeBackup结果");
        webMap.put("eventStatus", "Y");
        new Thread(){
            public void run() {
                try {
                    Map backupreusltMap = new HashMap();
                    backupreusltMap.put("msgName", "BackUpResult");
                    List<Recipe> recipes = new ArrayList<>();
                    for (Recipe recipe1 : recipeList) {
                        recipe = recipe1;
                        recipeService.deleteRcpByPrimaryKey(recipe1.getId());
                        logger.info("recipe删除成功");
                        recipeService.saveRecipe(recipe1);
                        logger.info("recipe保存成功");
                        recipeService.deleteRcpParaByRecipeId(recipe1.getId());
                        logger.info("recipePara批量删除成功");
                        recipeService.deleteAttachByRcpRowId(recipe1.getId());
                        logger.info("attach删除成功");
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String dateString = formatter.format(recipe1.getCreateDate());
                        Date currentTime_2 = formatter.parse(dateString);
                        recipe1.setCreateDate(currentTime_2);

                        String recipeFilePath = recipeService.organizeRecipeDownloadFullFilePath(recipe1);
                        //直接从FTP下载，如果本地有就覆盖，如果FTP不存在，那么下载失败
                        String localRecipeFilePath = GlobalConstants.localRecipePath + recipeFilePath;

                        //从Ftp 下载到本地
                        String downLoadFileResult = FtpUtil.connectServerAndDownloadFile(localRecipeFilePath, recipeFilePath, GlobalConstants.ftpIP,
                                GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                        //从Ftp 下载到本地
                        if (!"0".equals(downLoadFileResult)) {
                            logger.info("RMS服务器不存在该Recipe，无法完成下载.PPID=" + recipe1.getRecipeName() + "path:--" + recipeFilePath);
                        } else {
                            logger.info("从FTP下载Recipe成功.PPID=" + recipe1.getRecipeName());
                        }
                        if (recipe1.getDeviceTypeCode().contains("ICOST")) {
                            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(recipe1.getDeviceCode());
                            Map map = GlobalConstants.stage.equipHosts.get(deviceInfo.getDeviceCode()).getRelativeFileInfo(localRecipeFilePath, recipe1.getRecipeName());
                            String localHanRcpPath = String.valueOf(map.get("hanRcpPath"));
                            String hanRcpPath = localHanRcpPath.substring(localHanRcpPath.indexOf("/"));
                            String localCompRcpPath = String.valueOf(map.get("compRcpPath"));
                            String compRcpPath = localCompRcpPath.substring(localCompRcpPath.indexOf("/"));
                            //从Ftp 下载到本地
                            String downLoadHanResult = FtpUtil.connectServerAndDownloadFile(localHanRcpPath, hanRcpPath, GlobalConstants.ftpIP,
                                    GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                            String downLoadCompResult = FtpUtil.connectServerAndDownloadFile(localCompRcpPath, compRcpPath, GlobalConstants.ftpIP,
                                    GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                            if (!"0".equals(downLoadHanResult) || !"0".equals(downLoadCompResult)) {
                                logger.info(downLoadHanResult + "-->" + downLoadCompResult + ".PPID=" + recipe.getRecipeName());
                            } else {
                                logger.info("从FTP下载Recipe成功.PPID=" + recipe.getRecipeName());
                            }
                        }
                        logger.info("Recipe：[" + recipe1.getRecipeName() + "]备份完成");
                        recipes.add(recipe1);
                    }

                    recipeService.saveRcpParaBatch(recipeParaList);
                    logger.info("recipePara批量保存成功");
                    for (Attach attach : attachs) {
                        recipeService.deleteAttachByPrimaryKey(attach.getId());
                        recipeService.saveAttach(attach);
                    }
                    logger.info("attach保存成功");
                    sqlSession.commit();
                    backupreusltMap.put("arRecipeList", JsonMapper.toJsonString(recipes));
                } catch (Exception e) {
                    sqlSession.rollback();
                    logger.error("Exception:", e);
                } finally {
                    sqlSession.close();
                }
            }
            }.start();

        return JSONObject.toJSON(webMap).toString();
    }

}

