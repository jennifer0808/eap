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
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.*;


//上传接口
public class UploadRecipeWebservice implements BaseWebservice {
    private static Logger logger = Logger.getLogger(UploadRecipeWebservice.class.getName());
    @Override
    public String  handle(String message) {
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);
        Map webMap = new HashMap();
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
         String    deviceCode = String.valueOf(map.get("deviceCode"));
            String   recipeName = String.valueOf(map.get("recipeName"));
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求上传Recipe:[" + recipeName + "]");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            String deviceId = deviceInfo.getDeviceId();
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

            webMap.put("msgName", "UpLoad");
            webMap.put("deviceCode", deviceCode);
            webMap.put("recipe", JSONArray.toJSONString(recipe));
            webMap.put("recipeParaList", JSONArray.toJSONString(recipeParaList));
            logger.info("开始获取Attach信息");
            List<Attach> attList=hostManager.getRecipeAttachInfo(deviceId, recipe);
            if(attList==null){
                logger.info("获取Attach失败，重新开始获取");
                attList=getRecipeAttachInfo(recipe);
            }
            webMap.put("attach", JSONArray.toJSONString(attList));


            logger.info("向服务端回复获取到的Recipe信息" + JSONArray.toJSONString(webMap));
            webMap.put("eventDesc", "向服务端发送获取到的Recipe信息");
            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端发送获取到的Recipe信息");
        } catch (Exception ex) {
            logger.error("Exception:", ex);
            webMap.put("eventDesc", "向服务端发送获取到的Recipe信息失败");
        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
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



