/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.biz.recipe.service;

import cn.tzauto.octopus.biz.device.dao.DeviceInfoMapper;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.dao.*;
import cn.tzauto.octopus.biz.recipe.domain.*;
import cn.tzauto.octopus.biz.sys.dao.SysOfficeMapper;
import cn.tzauto.octopus.biz.sys.domain.SysOffice;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.service.BaseService;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.RoundingOff;
import cn.tzauto.octopus.common.ws.AxisUtility;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSON;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @author njtz
 */
public class RecipeService extends BaseService {

    private RecipeMapper recipeMapper;
    private RecipeParaMapper recipeParaMapper;
    private RecipeTemplateMapper recipeTemplateMapper;
    private RecipeOperationLogMapper recipeOperationLogMapper;
    private AttachMapper attachMapper;
    private DeviceInfoMapper deviceInfoMapper;
    public SysOfficeMapper sysOfficeMapper;
    private RecipeNameMappingMapper recipeNameMappingMapper;
    private static Logger logger = Logger.getLogger(RecipeService.class.getName());

    public RecipeService(SqlSession sqlSession) {
        super(sqlSession);
        recipeMapper = this.session.getMapper(RecipeMapper.class);
        recipeParaMapper = this.session.getMapper(RecipeParaMapper.class);
        recipeTemplateMapper = this.session.getMapper(RecipeTemplateMapper.class);
        recipeOperationLogMapper = this.session.getMapper(RecipeOperationLogMapper.class);
        attachMapper = this.session.getMapper(AttachMapper.class);
        deviceInfoMapper = this.session.getMapper(DeviceInfoMapper.class);
        sysOfficeMapper = this.session.getMapper(SysOfficeMapper.class);
    }

    /**
     * 根据recipeName, deviceCode, versionType, ersionNo查询recipe 根据
     *
     * @param recipeName
     * @param deviceCode
     * @param versionType
     * @param versionNo
     * @return
     */
    public List<Recipe> searchRecipeByPara(String recipeName, String deviceCode, String versionType, String versionNo) {
        Map paraMap = new HashMap();
        paraMap.put("recipeName", recipeName.replace("\\", "\\\\"));
        paraMap.put("deviceCode", deviceCode);
        paraMap.put("versionType", versionType);
        paraMap.put("versionNo", versionNo);
        return this.recipeMapper.searchByMap(paraMap);
    }//searchByPaMap

    /**
     * 根据recipeName, String deviceTypeCode, String versionType, String
     * versionNo查询Gold版本的recipe
     *
     * @param recipeName
     * @param deviceTypeCode
     * @param versionType
     * @param versionNo
     * @return
     */
    public List<Recipe> searchRecipeGoldByPara(String recipeName, String deviceTypeCode, String versionType, String versionNo) {
        Map paraMap = new HashMap();
        paraMap.put("recipeName", recipeName.replace("\\", "\\\\"));
        paraMap.put("deviceTypeCode", deviceTypeCode);
        paraMap.put("versionType", versionType);
        paraMap.put("versionNo", versionNo);
        return this.recipeMapper.searchByPaMap(paraMap);
    }

    /**
     * 根据recipeName, String deviceCode, String versionType查询recipe
     *
     * @param recipeName
     * @param deviceCode
     * @param versionType
     * @return
     */
    public List<Recipe> searchRecipeOrderByVerNo(String recipeName, String deviceCode, String versionType) {
        Map paraMap = new HashMap();
        paraMap.put("recipeName", recipeName.replace("\\", "\\\\"));
        paraMap.put("deviceCode", deviceCode);
        paraMap.put("versionType", versionType);
        return this.recipeMapper.searchByMapVerNo(paraMap);
    }

    /**
     * 根据recipeName, String recipeType查询recipe
     *
     * @param recipeName
     * @param recipeType
     * @return
     */
    public List<Recipe> searchRecipeByRcpType(String recipeName, String recipeType) {
        Map paraMap = new HashMap();
        paraMap.put("recipeName", recipeName);
        paraMap.put("recipeType", recipeType);
        return this.recipeMapper.searchRecipeByRcpType(paraMap);
    }

    /**
     * 根据deviceId, String recipeName, String clientId, String verNo查询recipe
     *
     * @param deviceId
     * @param recipeName
     * @param clientId
     * @param verNo
     * @return
     */
    public Recipe searchRecipeByPaExtMap(String deviceId, String recipeName, String clientId, String verNo) {
        Map extMap = new HashMap();
        extMap.put("deviceId", deviceId);
        extMap.put("recipeName", recipeName);
        extMap.put("clientId", clientId);
        extMap.put("versionNo", verNo);
        return this.recipeMapper.searchRecipeByPaExtMap(extMap);
    }

    /**
     * 根据clientid查询最近一周的recipe
     *
     * @param clientId
     * @return
     */
    public List searchRecipeRecent(String clientId) {
        return this.recipeMapper.searchRecipeRecent(clientId);
    }

    /**
     * 根据recipeName, String deviceCode查询正在执行的recipe
     *
     * @param recipeName
     * @param deviceCode
     * @return
     */
    public Recipe getExecRecipe(String recipeName, String deviceCode) {
        Recipe recipe = null;
        List<Recipe> recipes = this.searchRecipeOrderByVerNo(recipeName, deviceCode, "Unique");
        if (recipes == null || recipes.isEmpty()) {
            recipes = this.searchRecipeOrderByVerNo(recipeName, null, "GOLD");
            if (recipes != null && !recipes.isEmpty()) {
                recipe = recipes.get(0);
            }
        } else {
            recipe = recipes.get(0);
        }
        return recipe;
    }

    /**
     * 根据recipeName, String deviceCode查询goldrecipe
     *
     * @param recipeName
     * @param deviceCode
     * @return
     */
    public Recipe getGoldRecipe(String recipeName, String deviceCode, String deviceTypeCode) {
        Recipe recipe = null;
        List<Recipe> recipes = this.searchRecipeOrderByVerNo(recipeName, deviceCode, "GOLD");
        if (recipes != null && !recipes.isEmpty()) {
            recipe = recipes.get(0);
        } else {
            recipes = this.searchRecipeGoldByPara(recipeName, deviceTypeCode, "GOLD", null);
            if (recipes != null && !recipes.isEmpty()) {
                recipe = recipes.get(0);
            }
        }
        return recipe;
    }

    public Recipe getUniqueRecipe(String recipeName, String deviceCode) {
        Recipe recipe = null;
        List<Recipe> recipes = this.searchRecipeOrderByVerNo(recipeName, deviceCode, "Unique");
        if (recipes != null && !recipes.isEmpty()) {
            recipe = recipes.get(0);
        }
        return recipe;
    }

    public Recipe getRecipe(String id) {
        return this.recipeMapper.selectByPrimaryKey(id);
    }

    public List searchRecipeParaByRcpRowId(String recipeRowId) {
        List<RecipePara> recipeParas = this.recipeParaMapper.searchByRcpRowId(recipeRowId);
        if (recipeParas == null || recipeParas.isEmpty()) {
            if (!GlobalConstants.isLocalMode) {
                recipeParas = AxisUtility.initRecipePara(recipeRowId);
                if (recipeParas != null && !recipeParas.isEmpty()) {
                    try {
                        this.saveRcpParaBatch(recipeParas);
                        session.commit();
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                        session.rollback();
                        return recipeParas;
                    }
                }
            }
        }
        return recipeParas;
    }

    public List<RecipePara> searchRcpParaByRcpRowIdAndParaCode(String recipeRowId, String paraCode) {
        Map paraMap = new HashMap();
        paraMap.put("recipeRowId", recipeRowId);
        paraMap.put("paraCode", paraCode);
        return this.recipeParaMapper.searchByMap(paraMap);
    }

    public List<String> searchByMapWithRcpTemp(String recipeRowId, String paraName, String deviceTypeCode, String deviceVariableType) {
        Map paraMap = new HashMap();
        paraMap.put("recipeRowId", recipeRowId);
        paraMap.put("paraName", paraName);
        paraMap.put("deviceTypeCode", deviceTypeCode);
        paraMap.put("deviceVariableType", deviceVariableType);
        return this.recipeParaMapper.searchByMapWithRcpTemp(paraMap);
    }

    /**
     * 获取TOWAYPM和TOWAY1R之外类型的实时参数受到管控信息
     *
     * @param deviceCode
     * @param deviceVariableType
     * @return
     */
    public List<RecipeTemplate> searchRecipeTemplateByDeviceCode(String deviceCode, String deviceVariableType) {
        DeviceService deviceService = new DeviceService(session);
        DeviceInfo deviceInfo = deviceService.searchDeviceInfoByPara(null, deviceCode).get(0);
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeId", deviceInfo.getDeviceTypeId());
        paraMap.put("deviceVariableType", deviceVariableType);
        return this.recipeTemplateMapper.searchByMap(paraMap);
    }

    /**
     * 查询设备指定类型下对应id的信息
     *
     * @param deviceCode
     * @param deviceVariableType
     * @param deviceVariableId
     * @return
     */
    public List<RecipeTemplate> searchRecipeTemplateByDeviceCode(String deviceCode, String deviceVariableType, String deviceVariableId) {
        DeviceService deviceService = new DeviceService(session);
        DeviceInfo deviceInfo = deviceService.searchDeviceInfoByPara(null, deviceCode).get(0);
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeId", deviceInfo.getDeviceTypeId());
        paraMap.put("deviceVariableType", deviceVariableType);
        paraMap.put("deviceVariableId", deviceVariableId);
        return this.recipeTemplateMapper.searchByMap(paraMap);
    }

    /**
     * 根据deviceCode查询RecipeTemplate
     *
     * @param deviceCode
     * @return
     */
    public List<RecipeTemplate> searchRecipeTemplateMonitor(String deviceCode) {
        DeviceService deviceService = new DeviceService(session);
        DeviceInfo deviceInfo = deviceService.searchDeviceInfoByPara(GlobalConstants.getProperty("clientId"), deviceCode).get(0);
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeId", deviceInfo.getDeviceTypeId());
        return this.recipeTemplateMapper.searchRecipeTemplateMonitor(paraMap);
    }

    /**
     * 根据deviceTypeCode, String deviceVariableType查询RecipeTemplate
     *
     * @param deviceTypeCode
     * @param deviceVariableType
     * @return
     */
    public List<RecipeTemplate> searchRecipeTemplateByDeviceTypeCode(String deviceTypeCode, String deviceVariableType) {
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeCode", deviceTypeCode);
        paraMap.put("deviceVariableType", deviceVariableType);
        return this.recipeTemplateMapper.searchByMapOrderByParaCode(paraMap);
    }

    /**
     * 根据deviceTypeCode, String deviceVariableType查询RecipeTemplate
     *
     * @param deviceTypeCode
     * @param deviceVariableType
     * @return
     */
    public List<RecipeTemplate> searchMonitorByMap(String deviceTypeCode, String deviceVariableType, String monitorFlag) {
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeCode", deviceTypeCode);
        paraMap.put("deviceVariableType", deviceVariableType);
        paraMap.put("monitorFlag", monitorFlag);
        return this.recipeTemplateMapper.searchMonitorByMapOrderByParaCode(paraMap);
    }

    /**
     * 根据deviceTypeCode, deviceVariableType, paraDesc查询RecipeTemplate的shotcount
     * SVID
     *
     * @param deviceTypeCode
     * @return
     */
    public List<String> searchShotSVByDeviceType(String deviceTypeCode) {
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeCode", deviceTypeCode);
        paraMap.put("deviceVariableType", "RecipeParaCheck");
        paraMap.put("paraDesc", "ShotCount");
        return this.recipeTemplateMapper.searchShotSVByMap(paraMap);
    }

    /**
     * 根据deviceTypeCode, List<String> list查询RecipeTemplate
     *
     * @param deviceTypeCode
     * @param list
     * @return
     */
    public List<RecipeTemplate> searchPressRecipeTemplateByDeviceCode(String deviceTypeCode, List<String> list) {
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeCode", deviceTypeCode);
        paraMap.put("list", list);
        return this.recipeTemplateMapper.searchPressRecipeTemplateByDeviceCode(paraMap);

    }

    /**
     * 根据deviceCode, String operatorBy, String operationDateStart, String
     * operationDateEnd查询RecipeOperationLog
     *
     * @param deviceCode
     * @param operatorBy
     * @param operationType
     * @param operationDateStart
     * @param operationDateEnd
     * @return
     */
    public List searchRecipeOperationLogByPara(String deviceCode, String operatorBy, String operationType, String operationDateStart, String operationDateEnd) {
        Map paraMap = new HashMap();
        paraMap.put("deviceCode", deviceCode);
        paraMap.put("operatorBy", operatorBy);
        paraMap.put("operationType", operationType);
        paraMap.put("operationDateStart", operationDateStart);
        paraMap.put("operationDateEnd", operationDateEnd);
        return this.recipeOperationLogMapper.searchByMap(paraMap);
    }

    public int saveRecipeOperationLog(RecipeOperationLog recipeOperationLog) {
        int result = 0;
        if (recipeOperationLog != null) {
            result = this.recipeOperationLogMapper.insert(recipeOperationLog);
        }
        return result;
    }

    /**
     * 根据recipeRowId查询AttachPath
     *
     * @param recipeRowId
     * @return
     */
    public String searchAttachPathByRecipeRowId(String recipeRowId) {
        return this.attachMapper.searchByRecipeRowId(recipeRowId).get(0).getAttachPath();
    }

    /**
     * 下载指定的recipe，同时根据类型执行不同的删除recipe操作，最后选中Recipe
     *
     * @param deviceInfo
     * @param recipe
     * @param eventId
     * @param type
     * @return
     */
    public String downLoadRcp2DeviceByType(DeviceInfo deviceInfo, Recipe recipe, String eventId, String type) {
        String result = "0";//默认返回OK
        //获取机台状态，判断是否可以下载Recipe
        //验证机台状态
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        String deviceId = deviceInfo.getDeviceCode();
        String recipeName = recipe.getRecipeName();
        String checkResult = hostManager.checkBeforeDownload(deviceId, recipeName);
        if (!"0".equals(checkResult)) {
            return checkResult;
        }
        if (type.contains("Delete")) {
            String delResult = hostManager.deleteRcpFromDeviceAndShowLog(deviceId, recipeName);
            //如果删除失败，流程继续
        }
        if (type.contains("Download")) {
            //下载指定Recipe到机台上
            String recipeFilePath = organizeRecipeDownloadFullFilePath(recipe);
            String downLoadResult = null;
            if (deviceInfo.getDeviceType().equalsIgnoreCase("NITTODR3000III")) {
                downLoadResult = hostManager.downLoadRcp2DeviceCompleteForTP(recipeFilePath, deviceInfo, recipe);
            } else {
                downLoadResult = hostManager.downLoadRcp2DeviceComplete(recipeFilePath, deviceInfo, recipe);
            }
            if (!"0".equals(downLoadResult)) {
                result = result + " " + downLoadResult;
                return result;//如果下载失败，直接return
            }
        }
        if (type.contains("Select")) {
            //选中Recipe
            String ppselectResult = hostManager.selectSpecificRecipe(deviceId, recipe.getRecipeName());
            if (deviceInfo.getDeviceType().contains("DISCO") || deviceInfo.getDeviceType().contains("DB-800HSD")) {
                ppselectResult = "0";
            }
            if (!"0".equals(ppselectResult)) {
                result = result + " " + ppselectResult;
                if (type.contains("DeleteAll")) {
                    String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
                    //如果删除失败，流程继续
                }
                return result;
            }
        }
        if (type.contains("DeleteAll")) {
            String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
            //如果删除失败，流程继续
            //这里又删除了一遍，处理部分设备在用recipe无法删除的问题
//             hostManager.deleteRcpFromDeviceAndShowLog(deviceId, recipeName);
        }
        return result;
    }

    /**
     * 下载指定的recipe，同时根据类型执行不同的删除recipe操作，最后选中Recipe
     *
     * @param deviceInfo
     * @param recipe
     * @param type
     * @return
     */
    public String downLoadRcp2DeviceByType(DeviceInfo deviceInfo, Recipe recipe, String type) {
        String result = "0";//默认返回OK
        //获取机台状态，判断是否可以下载Recipe
        //验证机台状态
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        String deviceId = deviceInfo.getDeviceCode();
        String recipeName = recipe.getRecipeName();
        String checkResult = hostManager.checkBeforeDownload(deviceId, recipeName);
        if (!"0".equals(checkResult)) {
            return checkResult;
        }
        if (type.contains("Delete")) {
            hostManager.deleteRecipeFromDevice(deviceId, recipeName);
            //如果删除失败，流程继续
        }
        if (deviceInfo.getDeviceType().contains("DGP8761") && type.contains("DeleteAll")) {
            hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
        }
        if (type.contains("Download")) {
            //下载指定Recipe到机台上
            String recipeFilePath = organizeRecipeDownloadFullFilePath(recipe);
            String downLoadResult = null;
            if (deviceInfo.getDeviceType().equalsIgnoreCase("NITTODR3000III")) {
                downLoadResult = hostManager.downLoadRcp2DeviceCompleteForTP(recipeFilePath, deviceInfo, recipe);
            } else {
                downLoadResult = hostManager.downLoadRcp2DeviceComplete(recipeFilePath, deviceInfo, recipe);
            }
            if (!"0".equals(downLoadResult)) {
                result = result + " " + downLoadResult;
                return result;//如果下载失败，直接return
            }
        }
        if (type.contains("Select")) {
            //选中Recipe
            String ppselectResult = hostManager.selectSpecificRecipe(deviceId, recipe.getRecipeName());
            if (deviceInfo.getDeviceType().contains("DISCO") || deviceInfo.getDeviceType().contains("DB-800HSD")) {
                ppselectResult = "0";
            }
            if (!"0".equals(ppselectResult)) {
                result = result + " " + ppselectResult;
                if (type.contains("DeleteAll")) {
                    String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
                    //如果删除失败，流程继续
                }
                return result;
            }
        }
        if (!deviceInfo.getDeviceType().contains("DGP8761")) {
            if (type.contains("DeleteAll")) {
                String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
                //如果删除失败，流程继续
            }
        }
        return result;
    }

    /**
     * MES调用下载接口时使用的自动下载方法
     *
     * @param deviceInfo
     * @param recipe
     * @param type
     * @return
     */
    public String downLoadRcp2DeviceByTypeAutomatic(DeviceInfo deviceInfo, Recipe recipe, String type) {
        String result = "0";//默认返回OK
        //获取机台状态，判断是否可以下载Recipe
        //验证机台状态
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        String deviceId = deviceInfo.getDeviceCode();
        String recipeName = recipe.getRecipeName();
        String checkResult = hostManager.checkBeforeDownload(deviceId, recipeName);
        /*
            这里checkBeforeDownload有四种情况：
            1.“无法获取设备实时状态,请重试并检查设备通讯状态!下载失败！”
            2.“设备正在运行，不可调整Recipe！下载失败！”
            3.“1”：recipe与预下载recipe相同
            4.“0”：check通过可以正常下载recipe
         */
        if (!"0".equals(checkResult)) {
            //check当前recipe，连接状态等失败
            if ("1".equals(checkResult)) {
                //机台已是当前程序，删除其他recipe
                if (type.contains("DeleteAll")) {
                    String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
                    //如果删除失败，流程继续
                }
            }
            return checkResult;
        }
        if (type.contains("Delete")) {
            String delResult = hostManager.deleteRcpFromDeviceAndShowLog(deviceId, recipeName);
            //如果删除失败，流程继续
        }
        if (type.contains("Download")) {
            //下载指定Recipe到机台上
            String recipeFilePath = organizeRecipeDownloadFullFilePath(recipe);
            String downLoadResult = null;
            if (deviceInfo.getDeviceType().equalsIgnoreCase("NITTODR3000III")) {
                downLoadResult = hostManager.downLoadRcp2DeviceCompleteForTP(recipeFilePath, deviceInfo, recipe);
            } else {
                downLoadResult = hostManager.downLoadRcp2DeviceComplete(recipeFilePath, deviceInfo, recipe);
            }
            if (!"0".equals(downLoadResult)) {
                //将返回Alreay have的修改成Alreay-have，以免改机被通过，保持两端一致
                result = result + " " + downLoadResult;
                return result;//如果下载失败，直接return
            }
        }
        if (type.contains("Select")) {
            //选中Recipe
            String ppselectResult = hostManager.selectSpecificRecipe(deviceId, recipe.getRecipeName());
            if (deviceInfo.getDeviceType().contains("DISCO") || deviceInfo.getDeviceType().contains("DB-800HSD")
                    || deviceInfo.getDeviceType().contains("AD83") || deviceInfo.getDeviceType().contains("ESEC")
                    || deviceInfo.getDeviceType().contains("Twin832")) {
                ppselectResult = "0";
            }
            if (!"0".equals(ppselectResult)) {
                result = "2";
                if (type.contains("DeleteAll")) {
                    String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
                    //如果删除失败，流程继续
                }
                return result;
            }
        }
        if (type.contains("DeleteAll")) {
            String delAllResult = hostManager.deleteAllRcpFromDevice(deviceId, recipeName);
            //如果删除失败，流程继续
        }
        return result;
    }

    /**
     * 保存Recipe上传时的相关信息
     *
     * @param recipe
     * @param recipeParas
     * @param deviceCode
     */
    public boolean saveUpLoadRcpInfo(Recipe recipe, List<RecipePara> recipeParas, String deviceCode) {

//        String rcpNewId = this.getNewId();
        String rcpNewId = UUID.randomUUID().toString();
        recipe.setId(rcpNewId);
        saveRecipe(recipe);
        String recipeName = recipe.getRecipeName();
        List<Recipe> recipes = new ArrayList();
        recipes.add(recipe);

        //路径信息
//        String recipeLocalPath = GlobalConstants.stage.equipHosts.get(deviceId).getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + recipeName.replaceAll("/", "@") + "/" + recipeName.replaceAll("/", "@") + "_V" + recipe.getVersionNo() + ".txt";
//        String remoteRecipePath = GlobalConstants.clientFtpPath + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + recipeName.replaceAll("/", "@") + "/";
        //本地路径需要到文件
        String recipeLocalPath = GlobalConstants.localRecipePath + organizeRecipePath(recipe) + recipeName.replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt";

        //ftp路径需要到目录
        String recipeRemotePath = organizeUploadRecipePath(recipe);

        RecipeOperationLog recipeOperationLog = setRcpOperationLog(recipe, "upload");

        this.saveRecipeOperationLog(recipeOperationLog);
        if (recipeParas != null && !recipeParas.isEmpty()) {
            recipeParas = setParasRCProwId(recipeParas, recipe.getId());
//            this.saveRcpParaBatch(recipeParas);
            //存储之后查询，得到id
            //  recipeParas = recipeParaMapper.searchByRcpRowId(recipe.getId());
        }
        //附件信息       
        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());
        List<Attach> attachs = GlobalConstants.stage.hostManager.getEquipRecipeAttarch(deviceInfo.getDeviceCode(), recipe);
        List<RecipeOperationLog> recipeOperationLogs = new ArrayList<>();
        recipeOperationLogs.add(recipeOperationLog);


        if (!GlobalConstants.isLocalMode) {
//            boolean existFlag = FtpUtil.checkFileExist(recipeRemotePath, recipeName.replaceAll("/", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
//            if (!existFlag) {
//                return false;
//            }
            uploadRcpFile2FTP(recipeLocalPath, recipeRemotePath, recipe);
            sendUploadInfo2Server(deviceCode, recipes, recipeParas, recipeOperationLogs, attachs);
        }
        return true;
    }

    private boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        return GlobalConstants.stage.hostManager.uploadRcpFile2FTP(localRcpPath, remoteRcpPath, recipe);
    }

    private void sendUploadInfo2Server(String deviceCode, List<Recipe> recipes, List<RecipePara> recipeParas, List<RecipeOperationLog> recipeOperationLogs, List<Attach> attachs) {
        Map mqMap = new HashMap();
            mqMap.put("msgName", "Upload");
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipe", JSON.toJSONString(recipes));
            mqMap.put("recipePara", JSON.toJSONString(recipeParas));
            mqMap.put("recipeOperationLog", JSON.toJSONString(recipeOperationLogs));
            mqMap.put("attach", JSON.toJSONString(attachs));
            String eventId = "";
            try {
                eventId = AxisUtility.getReplyMessage();
            }catch (Exception e){
            e.printStackTrace();
        }

        mqMap.put("eventId", eventId==null?"":eventId);
        GlobalConstants.C2SRcpUpLoadQueue.sendMessage(mqMap);
        GlobalConstants.sysLogger.info(" MQ发送记录：Recipe= " + JSON.toJSONString(recipes) + " recipePara= " + JSON.toJSONString(recipeParas) + " recipeOperationLog= " + JSON.toJSONString(recipeOperationLogs));
    }

    public List<RecipePara> saveUpLoadRcpInfo(Recipe recipe, List<RecipePara> recipeParas) {
        String rcpNewId = UUID.randomUUID().toString();
        recipe.setId(rcpNewId);
        saveRecipe(recipe);
        List<Recipe> recipes = new ArrayList();
        recipes.add(recipe);
        if (recipeParas != null && !recipeParas.isEmpty()) {
            recipeParas = setParasRCProwId(recipeParas, recipe.getId());
//            this.saveRcpParaBatch(recipeParas);
            //存储之后查询，得到id
            //recipeParas = recipeParaMapper.searchByRcpRowId(recipe.getId());
        }
        //本地路径需要到文件
        String recipeLocalPath = GlobalConstants.localRecipePath + organizeRecipePath(recipe) + recipe.getRecipeName().replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt";
        //ftp路径需要到目录
        String recipeRemotePath = organizeUploadRecipePath(recipe);
        try {
            if (!uploadRcpFile2FTP(recipeLocalPath, recipeRemotePath, recipe)) {
                logger.info("上传recipe文件到ftp时失败");
            }
        } catch (Exception e) {
            logger.error("上传recipe:" + recipe.getRecipeName() + "]到ftp时异常." + e.getMessage());
            return null;
        }
        return recipeParas;

    }

    /**
     * 保存Recipe上传时的相关信息
     *
     * @param recipe
     * @param recipeParas
     * @param deviceCode
     */
    public void saveUpLoadRcpInfoBatch(Recipe recipe, List<RecipePara> recipeParas, String deviceCode) {

//        String rcpNewId = this.getNewId();
        String rcpNewId = UUID.randomUUID().toString();
        recipe.setId(rcpNewId);
        saveRecipe(recipe);
        String recipeName = recipe.getRecipeName();
        List<Recipe> recipes = new ArrayList();
        recipes.add(recipe);

        //路径信息
        //本地路径需要到文件
        String recipeLocalPath = GlobalConstants.localRecipePath + organizeRecipePath(recipe) + recipeName.replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt";
        //ftp路径需要到目录
        String recipeRemotePath = organizeUploadRecipePath(recipe);
        //DB800从ftp上传recipe
        if (recipe.getDeviceTypeCode().contains("DB810")) {
            recipeLocalPath = GlobalConstants.DB800HSDFTPPath + recipe.getRecipeName() + ".tgz";
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe本地FTP路径：" + recipeLocalPath);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe远程FTP路径：" + recipeRemotePath);
        }
        RecipeOperationLog recipeOperationLog = setRcpOperationLog(recipe, "upload");
        this.saveRecipeOperationLog(recipeOperationLog);
        if (recipeParas != null && !recipeParas.isEmpty()) {
            recipeParas = setParasRCProwId(recipeParas, recipe.getId());
            for (int i = 0; i < recipeParas.size(); i++) {
                saveRecipePara(recipeParas.get(i));
                if (i % 1000 == 0) {
                    session.commit();
                }
            }
            //存储之后查询，得到id
            // recipeParas = recipeParaMapper.searchByRcpRowId(recipe.getId());
        }
        //附件信息  

        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());
        List<Attach> attachs = GlobalConstants.stage.hostManager.getEquipRecipeAttarch(deviceInfo.getDeviceCode(), recipe);
        List<RecipeOperationLog> recipeOperationLogs = new ArrayList<>();
        recipeOperationLogs.add(recipeOperationLog);
        if (!GlobalConstants.isLocalMode) {
            //添加至MQ
            Map mqMap = new HashMap();
            mqMap.put("msgName", "Upload");
            mqMap.put("recipe", JSON.toJSONString(recipes));
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipePara", JSON.toJSONString(recipeParas));
            mqMap.put("recipeOperationLog", JSON.toJSONString(recipeOperationLogs));
            mqMap.put("attach", JSON.toJSONString(attachs));
            String eventId = "";
            try {
                eventId = AxisUtility.getReplyMessage();
            }catch (Exception e){
                e.printStackTrace();
            }
            mqMap.put("eventId", eventId == null?"":eventId);
            GlobalConstants.C2SRcpUpLoadQueue.sendMessage(mqMap);
            GlobalConstants.sysLogger.info(" MQ发送记录：Recipe= " + JSON.toJSONString(recipe) + " recipePara= " + JSON.toJSONString(recipeParas) + " recipeOperationLog= " + JSON.toJSONString(recipeOperationLog));

            // 上传ftp
            FtpUtil.uploadFile(recipeLocalPath, GlobalConstants.getProperty("ftpPath") + recipeRemotePath, recipeName.replaceAll("/", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
        }
        //日志
//      UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "用户 " + GlobalConstants.sysUser.getName() + "上传Recipe： " + recipeName + " 到工控机：" + GlobalConstants.clientId);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + recipeLocalPath);

    }

    /**
     * 保存Recipe上传时的相关信息(针对长短号问题附加的保存方法)
     *
     * @param recipe
     * @param recipeParas
     * @param deviceCode
     */
    public boolean saveUpLoadRcpInfo(Recipe recipe, List<RecipePara> recipeParas, String deviceCode, RecipeNameMapping recipeNameMapping) {

        String rcpNewId = UUID.randomUUID().toString();
        recipe.setId(rcpNewId);
        saveRecipe(recipe);
        String recipeName = recipe.getRecipeName();
        List<Recipe> recipes = new ArrayList();
        recipes.add(recipe);

        //路径信息
//        String recipeLocalPath = GlobalConstants.stage.equipHosts.get(deviceId).getRecipePathPrefix() + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + recipeName.replaceAll("/", "@") + "/" + recipeName.replaceAll("/", "@") + "_V" + recipe.getVersionNo() + ".txt";
//        String remoteRecipePath = GlobalConstants.clientFtpPath + "/" + recipe.getDeviceTypeCode() + "/" + recipe.getDeviceCode() + "/" + recipe.getVersionType() + "/" + recipeName.replaceAll("/", "@") + "/";
        //本地路径需要到文件
        String recipeLocalPath = GlobalConstants.localRecipePath + organizeRecipePath(recipe) + recipeName.replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt";
        //ftp路径需要到目录
        String recipeRemotePath = organizeUploadRecipePath(recipe);

        RecipeOperationLog recipeOperationLog = setRcpOperationLog(recipe, "upload");
        this.saveRecipeOperationLog(recipeOperationLog);
        recipeParas = setParasRCProwId(recipeParas, recipe.getId());
        //  new RecipeParaDao().saveRecipeParaBatch(recipeParaList);
//        this.saveRcpParaBatch(recipeParas);
        //存储之后查询，得到id
        //这里太慢，于是在setParasRCProwId给赋值了
        //recipeParas = recipeParaMapper.searchByRcpRowId(recipe.getId());
        List<RecipeOperationLog> recipeOperationLogs = new ArrayList<>();
        recipeOperationLogs.add(recipeOperationLog);
        //附件信息         
        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());
        List<Attach> attachs = GlobalConstants.stage.hostManager.getEquipRecipeAttarch(deviceInfo.getDeviceCode(), recipe);
        boolean existFlag = false;
        if (!GlobalConstants.isLocalMode) {
            //添加至MQ
            Map mqMap = new HashMap();
            mqMap.put("msgName", "Upload");
//        mqMap.put("recipe", JSON.toJSONString(recipetmps));
            mqMap.put("recipe", JSON.toJSONString(recipes));
            mqMap.put("deviceCode", deviceCode);
            mqMap.put("recipePara", JSON.toJSONString(recipeParas));
            mqMap.put("recipeOperationLog", JSON.toJSONString(recipeOperationLogs));
            mqMap.put("attach", JSON.toJSONString(attachs));
            mqMap.put("eventId", AxisUtility.getReplyMessage());
            mqMap.put("recipeNameMapping", JSON.toJSONString(recipeNameMapping));
            GlobalConstants.C2SRcpUpLoadQueue.sendMessage(mqMap);
            GlobalConstants.sysLogger.debug(" MQ发送记录：Recipe= " + JSON.toJSONString(recipe) + " recipePara= " + JSON.toJSONString(recipeParas) + " recipeOperationLog= " + JSON.toJSONString(recipeOperationLog));

            // 上传ftp
            FtpUtil.uploadFile(recipeLocalPath, recipeRemotePath, recipeName.replaceAll("/", "@").replace("\\", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
            existFlag = FtpUtil.checkFileExist(recipeRemotePath, recipeName.replaceAll("/", "@") + "_V" + recipe.getVersionNo() + ".txt", GlobalConstants.ftpIP, GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);

            //日志
//       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "用户 " + GlobalConstants.sysUser.getName() + "上传Recipe： " + recipeName + " 到工控机：" + GlobalConstants.clientId);
            GlobalConstants.sysLogger.info(" MQ发送记录：Recipe= " + JSON.toJSONString(recipe) + " recipePara= " + JSON.toJSONString(recipeParas) + " recipeOperationLog= " + JSON.toJSONString(recipeOperationLog));
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe文件存储位置：" + recipeLocalPath);
        return existFlag;
    }

    /*
     * 保存recipe升级信息（来自server的信息）
     */
    public void saveUpGradeRcpInfo(Recipe recipe, List<RecipePara> recipeParas, List<Attach> attachs) {
        logger.info("save upgrade info...");
        this.deleteRcpByPrimaryKey(recipe.getId());
        logger.info("delete recipe:rowid=" + recipe.getId());
        this.saveRecipe(recipe);
        logger.info("insert recipe:rowid=" + recipe.getId());
        if (recipeParas != null && !recipeParas.isEmpty()) {
            this.deleteRcpParaByRecipeId(recipe.getId());
            logger.info("delete recipePara:rcprowid=" + recipe.getId());
            this.saveRcpParaBatch(recipeParas);
            logger.info("insert recipePara:rcprowid=" + recipe.getId());
        }
        if (attachs != null && !attachs.isEmpty()) {
            for (Attach attach : attachs) {
                this.deleteAttachByPrimaryKey(attach.getId());
                logger.info("delete attach:rcprowid=" + recipe.getId());
                this.saveAttach(attach);
                logger.info("insert attach:rcprowid=" + recipe.getId());
            }
        }
        logger.info("save upgrade info over");
    }

    /*
     * 保存recipe升级信息（来自server的信息）
     */
    public void saveUpGradeRcpInfoForDB800(Recipe recipe, List<RecipePara> recipeParas, List<Attach> attachs) {

        Recipe recipetmp = this.selectByPrimaryKey(recipe.getId());
        if (recipetmp == null) {
            //recipe.setId(this.getNewId());
            if (this.saveRecipe(recipe) > 0 && recipeParas != null && !recipeParas.isEmpty() && attachs != null && !attachs.isEmpty()) {
                //this.saveRcpParaBatch(recipeParas);
                if (recipeParas != null && !recipeParas.isEmpty()) {
                    recipeParas = setParasRCProwId(recipeParas, recipe.getId());
                    for (int i = 0; i < recipeParas.size(); i++) {
                        saveRecipePara(recipeParas.get(i));
                        if (i % 1000 == 0) {
                            session.commit();
                        }
                    }
                }
                for (Attach attach : attachs) {
                    this.saveAttach(attach);
                }
            }
        } else if (this.modifyRecipe(recipe) > 0 && recipeParas != null && !recipeParas.isEmpty() && attachs != null && !attachs.isEmpty()) {
            this.deleteRcpParaByRecipeId(recipe.getId());
            //this.saveRcpParaBatch(recipeParas);
            if (recipeParas != null && !recipeParas.isEmpty()) {
                recipeParas = setParasRCProwId(recipeParas, recipe.getId());
                for (int i = 0; i < recipeParas.size(); i++) {
                    saveRecipePara(recipeParas.get(i));
                    if (i % 1000 == 0) {
                        session.commit();
                    }
                }
            }
            for (Attach attach : attachs) {
                this.saveAttach(attach);
            }
        }

    }

    public int saveRcpParaBatch(List<RecipePara> recipeParas) {
        int result = 0;
        if (recipeParas != null && !recipeParas.isEmpty()) {
            for (int i = 0; i < recipeParas.size(); i++) {
                result = saveRecipePara(recipeParas.get(i));
                if (i % 1000 == 0) {
                    session.commit();
                }
            }
        }
        return result;
    }

    public int saveRcpParaBatchForDB800(List<RecipePara> recipeParas) {
        int result = 0;
        if (recipeParas != null && !recipeParas.isEmpty()) {
            for (int i = 0; i < recipeParas.size(); i++) {
                result = saveRecipePara(recipeParas.get(i));
                if (i % 1000 == 0) {
                    session.commit();
                }
            }
        }
        return result;
    }

    public int deleteRcpParaBatch(List<RecipePara> recipeParas) {
        int result = 0;
        if (recipeParas != null && !recipeParas.isEmpty()) {
            result = this.recipeParaMapper.deleteRcpParaBatch(recipeParas);
        }
        return result;
    }

    public int saveRcpTemplateBatch(List<RecipeTemplate> recipeTemplates) {
        return recipeTemplateMapper.saveRecipeTemplateBatch(recipeTemplates);
    }

    private int modifyRcpParaBatch(List<RecipePara> recipeParas) {
        return this.recipeParaMapper.modifyRcpParaBatch(recipeParas);
    }

    public int saveRecipe(Recipe recipe) {
        return this.recipeMapper.insert(recipe);
    }

    public int modifyRecipe(Recipe recipe) {
        return this.recipeMapper.updateByPrimaryKey(recipe);
    }

    public int saveAttach(Attach attach) {
        return this.attachMapper.insert(attach);
    }

    public int deleteAttachByPrimaryKey(String attachId) {
        return this.attachMapper.deleteByPrimaryKey(attachId);
    }

    public int deleteAttachByRcpRowId(String rcpRowId) {
        return this.attachMapper.deleteByRcpRowId(rcpRowId);
    }

    public int modifyAttach(Attach attach) {
        return this.attachMapper.updateByPrimaryKey(attach);
    }

    /*
     * 为recipePara附上reciperowid数据
     */
    private List<RecipePara> setParasRCProwId(List<RecipePara> recipeParas, String recipeRowId) {
        for (RecipePara recipePara : recipeParas) {
            recipePara.setRecipeRowId(recipeRowId);
//            recipePara.setCreateBy(GlobalConstants.sysUser.getId());
//            recipePara.setUpdateBy(GlobalConstants.sysUser.getId());
            recipePara.setCreateBy("SYSTEM");
            recipePara.setUpdateBy("SYSTEM");
            recipePara.setId(UUID.randomUUID().toString());
        }
        return recipeParas;
    }

    public int deleteRcp(Recipe recipe) {
//        this.attachMapper.delete(attach);
        return this.recipeMapper.deleteRcp(recipe);
    }

    public int deleteRcpByPrimaryKey(String recipeId) {
        return this.recipeMapper.deleteByPrimaryKey(recipeId);
    }

    public Recipe selectByPrimaryKey(String recipeId) {
        return this.recipeMapper.selectByPrimaryKey(recipeId);
    }

    public String getNewId() {
        return this.recipeMapper.getUUID();
    }

    public Attach searchAttachByRcpRowId(String rcpRowId) {
        return this.attachMapper.searchByRecipeRowId(rcpRowId).get(0);
    }

    public int deleteRcpParaByRecipeId(String recipeRowId) {
        return this.recipeParaMapper.deleteByRcpRowId(recipeRowId);
    }

    /*
     * modify by njtz 
     * @2016/10/31
     */
    public List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas) {
        return checkRcpPara(recipeRowid, deviceCode, equipRecipeParas, "");
    }

    /**
     * 参数检查
     *
     * @param recipeRowid
     * @param deviceCode
     * @param equipRecipeParas
     * @param masterCompareType
     * @return
     */
    public List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas, String masterCompareType) {
        //获取Gold版本的参数(只有gold才有wi信息)
        List<RecipePara> goldRecipeParas = this.searchRecipeParaByRcpRowId(recipeRowid);
        //确定管控参数
        List<RecipeTemplate> recipeTemplates = this.searchRecipeTemplateMonitor(deviceCode);
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (RecipePara recipePara : goldRecipeParas) {
                if (recipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                    recipeTemplate.setMinValue(recipePara.getMinValue());
                    recipeTemplate.setMaxValue(recipePara.getMaxValue());
                    recipeTemplate.setSetValue(recipePara.getSetValue());
                }
            }
        }
        //找出设备当前recipe参数中超出wi范围的参数
        List<RecipePara> wirecipeParaDiff = new ArrayList<>();
        for (RecipePara equipRecipePara : equipRecipeParas) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                if (recipeTemplate.getParaCode().equals(equipRecipePara.getParaCode())) {
                    equipRecipePara.setRecipeRowId(recipeRowid);
                    String currentRecipeValue = equipRecipePara.getSetValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===currentRecipeValue====>" + currentRecipeValue);
                    String setValue = recipeTemplate.getSetValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===setvalue====>" + setValue);
                    String minValue = recipeTemplate.getMinValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===minValue====>" + minValue);
                    String maxValue = recipeTemplate.getMaxValue();
                    logger.info(equipRecipePara.getParaCode() + "===" + equipRecipePara.getParaName() + "===maxValue====>" + maxValue);
                    equipRecipePara.setDefValue(setValue);//默认值，recipe参数设定值
                    boolean paraIsNumber = false;
                    try {
                        Double.parseDouble(currentRecipeValue);
                        paraIsNumber = true;
                    } catch (Exception e) {
                    }
                    try {
                        if ("abs".equals(masterCompareType)) {
                            if ("".equals(setValue) || " ".equals(setValue) || "".equals(currentRecipeValue) || " ".equals(currentRecipeValue)) {
                                continue;
                            }
                            equipRecipePara.setMinValue(setValue);
                            equipRecipePara.setMaxValue(setValue);
                            if (paraIsNumber) {
                                if (Double.parseDouble(currentRecipeValue) != Double.parseDouble(setValue)) {
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else if (!currentRecipeValue.equals(setValue)) {
                                wirecipeParaDiff.add(equipRecipePara);
                            }
                        } else {//spec
                            if ("1".equals(recipeTemplate.getSpecType())) {
                                if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                    logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                    continue;
                                }
                                if ("".equals(masterCompareType)) {
                                    if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                        equipRecipePara.setMinValue(minValue);
                                        equipRecipePara.setMaxValue(maxValue);
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                } else {
                                    if ((Double.parseDouble(equipRecipePara.getSetValue()) <= Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) >= Double.parseDouble(maxValue))) {
                                        equipRecipePara.setMinValue(minValue);
                                        equipRecipePara.setMaxValue(maxValue);
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                }

                                //abs
                            } else if ("2".equals(recipeTemplate.getSpecType())) {
                                if ("".equals(setValue) || " ".equals(setValue) || "".equals(currentRecipeValue) || " ".equals(currentRecipeValue)) {
                                    logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                    continue;
                                }
                                String setvalueTmp = setValue;
                                if (setValue.length() != currentRecipeValue.length()) {
                                    //setValue = String.valueOf(RoundingOff.roundOff(Double.valueOf(setValue), lastNotZero));
                                    try {
                                        setvalueTmp = RoundingOff.roundOff1(String.valueOf(currentRecipeValue), String.valueOf(setValue));

                                    } catch (Exception e) {
                                    }

                                    logger.debug("根据:" + currentRecipeValue + "四舍五入:" + setValue + "的结果是" + setvalueTmp);
                                }
                                if (!currentRecipeValue.equals(setvalueTmp)) {
                                    equipRecipePara.setMinValue(setValue);
                                    equipRecipePara.setMaxValue(setValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else if ("3".equals(recipeTemplate.getSpecType())) {
                                if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                    logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                    continue;
                                }
                                if ((Double.parseDouble(equipRecipePara.getSetValue()) <= Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) >= Double.parseDouble(maxValue))) {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else {
                                if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                    logger.info("Para:Name[" + recipeTemplate.getParaName() + "],Code[" + recipeTemplate.getParaCode() + "]has not set range! Pass");
                                    continue;
                                }
                                if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                    equipRecipePara.setMinValue(minValue);
                                    equipRecipePara.setMaxValue(maxValue);
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                }
            }
        }
        return wirecipeParaDiff;
    }

    /**
     * 保存recipe操作日志
     *
     * @param recipe
     * @param operationType
     * @return
     */
    public RecipeOperationLog setRcpOperationLog(Recipe recipe, String operationType) {
        RecipeOperationLog recipeOperationLog = new RecipeOperationLog();
        if (GlobalConstants.sysUser == null) {
            recipeOperationLog.setOperatorBy("System");
        } else {
            recipeOperationLog.setOperatorBy(GlobalConstants.sysUser.getLoginName());
        }
        recipeOperationLog.setOperationDate(new Date());
        recipeOperationLog.setDeviceCode(recipe.getDeviceCode());
        recipeOperationLog.setDeviceStatus("");
        recipeOperationLog.setRecipeRowId(recipe.getId());
        switch (operationType) {
            case "upload":
                recipeOperationLog.setOperationType("Upload");
                recipeOperationLog.setOperationResultDesc("上传Recipe： " + recipe.getRecipeName() + " 到工控机：" + GlobalConstants.getProperty("clientId"));
                break;
            case "download":
                recipeOperationLog.setOperationType("Download");
                recipeOperationLog.setOperationResultDesc("手动下载Recipe： " + recipe.getRecipeName() + " 到机台：" + recipe.getDeviceCode());
                break;
            case "mesdownload":
                recipeOperationLog.setOperationType("MESDownload");
                recipeOperationLog.setOperationResultDesc("自动下载Recipe： " + recipe.getRecipeName() + " 到机台：" + recipe.getDeviceCode());
                break;
        }
        return recipeOperationLog;
    }

    /**
     * 根据recipe类型组装recipe的下载完整路径
     *
     * @param recipe
     * @return
     */
    public String organizeRecipeDownloadFullFilePath(Recipe recipe) {
        String returnPath = "";
        String recipeNamePath = recipe.getRecipeName().replace("/", "@").replace("\\", "@");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());

        if (deviceInfo == null) {
            returnPath = this.searchAttachPathByRecipeRowId(recipe.getId()) + "/" + recipeNamePath + ".txt";
            logger.debug("Client:" + GlobalConstants.clientId + " 没有配置Deivce：" + recipe.getDeviceCode() + "的设备信息.使用附件信息查找recipe：" + recipe.getRecipeName() + "的路径");
            logger.debug("Recipe:" + recipe.getRecipeName() + "附件中的下载路径：" + returnPath);
        } else {
            SysService sysService = new SysService(sqlSession);
            SysOffice sysOffice = sysService.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
            returnPath = "/RECIPE/" + sysOffice.getPlant() + "/" + sysOffice.getName() + "/" + deviceInfo.getDeviceType() + "/" + recipe.getVersionType();
            if ("GOLD".equalsIgnoreCase(recipe.getVersionType())) {
                returnPath = returnPath + "/" + recipeNamePath + "/" + recipeNamePath + ".txt";
            } else if ("UNIQUE".equalsIgnoreCase(recipe.getVersionType())) {
                returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipeNamePath + "/" + recipeNamePath + ".txt";
            } else if ("ENGINEER".equalsIgnoreCase(recipe.getVersionType())) {
                returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipeNamePath + "/" + recipeNamePath + "_V" + recipe.getVersionNo() + ".txt";
            }
            logger.debug("Recipe:" + recipe.getRecipeName() + "使用自动拼接的下载路径：" + returnPath);
        }
        sqlSession.close();
        return returnPath;
    }

    /**
     * 根据recipe类型和名称，生成recipe的保存路径
     *
     * @return
     */
    public String organizeRecipePath(Recipe recipe) {
        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());
        SysOffice sysOffice = sysOfficeMapper.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
        String returnPath = "";
        returnPath = "/RECIPE/" + sysOffice.getPlant() + "/" + sysOffice.getName() + "/" + deviceInfo.getDeviceType() + "/" + recipe.getVersionType();
        String recipeName = recipe.getRecipeName().replace("\\", "@").replaceAll("/", "@");
//        if (deviceInfo.getDeviceType().contains("DF")) {
//            recipeName = recipe.getRecipeName().replace("\\", "@");
//        } else {
//            recipeName = recipe.getRecipeName().replaceAll("/", "@");
//        }   
        if ("GOLD".equalsIgnoreCase(recipe.getVersionType())) {
            returnPath = returnPath + "/" + recipeName + "/";
            //批量上传recipe时修改
            //returnPath = returnPath + "/";
        } else if ("Unique".equalsIgnoreCase(recipe.getVersionType())) {
            returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipeName + "/";
            //批量上传recipe时修改
            //returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/";
        } else if ("Engineer".equalsIgnoreCase(recipe.getVersionType())) {
            returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipeName + "/";
            //批量上传recipe时修改
            //returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/";
        }
        //        returnPath = returnPath + "/" + deviceInfo.getDeviceCode() + "/" + recipe.getRecipeName().replace("/", "@").replace("\\", "@") + "/";
        return returnPath;
    }

    /**
     * 组织出recipe上传时的存储路径
     *
     * @param recipe
     * @return
     */
    public String organizeUploadRecipePath(Recipe recipe) {
        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());
        SysOffice sysOffice = sysOfficeMapper.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
        String returnPath = "";
        returnPath = "/RECIPE/" + sysOffice.getPlant() + "/" + sysOffice.getName() + "/" + deviceInfo.getDeviceType() + "/" + recipe.getVersionType() + "/" + deviceInfo.getDeviceCode() + "/" + recipe.getRecipeName().replace("/", "@").replace("\\", "@") + "/";
        logger.info("recipe上传时的存储路径:"+returnPath);
        return returnPath;
    }

    public int saveRecipeTemplateBatch(List<RecipeTemplate> recipeTemplates) {
        return this.recipeTemplateMapper.saveRecipeTemplateBatch(recipeTemplates);
    }

    public int deleteRecipeTemplateByDeviceTypeIdBatch(List<RecipeTemplate> recipeTemplates) {
        return this.recipeTemplateMapper.deleteRecipeTemplateByDeviceTypeIdBatch(recipeTemplates);
    }

    public int deleteRecipeTemplateByDeviceTypeCodeBatch(List<RecipeTemplate> recipeTemplates) {
        return this.recipeTemplateMapper.deleteRecipeTemplateByDeviceTypeCodeBatch(recipeTemplates);
    }

    public int deleteRecipeTemplateByIdBatch(List<RecipeTemplate> recipeTemplates) {
        return this.recipeTemplateMapper.deleteRecipeTemplateByIdBatch(recipeTemplates);
    }

    public int saveRecipeBatch(List<Recipe> recipes) {
        return this.recipeMapper.saveRecipeBatch(recipes);
    }

    /**
     * 批量删除Recipe
     *
     * @param recipes
     * @return
     */
    public int deleteRecipeByIdBatch(List<Recipe> recipes) {
        return this.recipeMapper.deleteRecipeByIdBatch(recipes);
    }

    public int saveRecipePara(RecipePara record) {
        return this.recipeParaMapper.insert(record);
    }

    /**
     * 判断recipeParas中是否包含指定paraCode的参数
     *
     * @param recipeParas
     * @param paraCode
     * @return
     */
    private boolean recipeParaContainPara(List<RecipePara> recipeParas, String paraCode) {
        boolean contain = false;
        for (RecipePara recipePara : recipeParas) {
            if (recipePara.getParaCode().equals(paraCode)) {
                return true;
            }
        }
        return contain;
    }

    /**
     * 判断recipepara里是否包含该设备要管控的所有参数
     *
     * @param recipeParas
     * @return
     */
    public List<RecipePara> recipeParaContainAllTemplatePara(List<RecipePara> recipeParas, String deviceCode) {
        List<RecipeTemplate> recipeTemplates = this.searchRecipeTemplateMonitor(deviceCode);
        List<RecipePara> recipeParasDiff = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if (!recipeParaContainPara(recipeParas, recipeTemplate.getParaCode())) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipeParasDiff.add(recipePara);
            }
        }
        return recipeParasDiff;
    }

    public List<String> getRecipeNameByDeviceCode(String deviceCode) {
        return this.recipeMapper.getAllRecipeName(deviceCode);
    }

    public void cleanData() {
        new Thread() {

            @Override
            public void run() {
                logger.info("开始清理 recipeOperationLogs 数据...");
                List<RecipeOperationLog> recipeOperationLogs = recipeOperationLogMapper.selectOldData(GlobalConstants.redundancyDataSavedDays);
                logger.debug("过期 recipeOperationLogs 数据条数：" + recipeOperationLogs.size());
                if (recipeOperationLogs.size() > 0) {
                    int cleanRows = recipeOperationLogMapper.deleteOpLogBatch(recipeOperationLogs);
                    logger.info("清理 recipeOperationLogs 数据条数：" + cleanRows);
                }
                logger.info("recipeOperationLogs 数据清理完成...");

                logger.info("开始清理 recipePara 数据...");
                List<RecipePara> recipeParas = recipeParaMapper.selectOldData(GlobalConstants.redundancyDataSavedDays);
                logger.debug("过期 recipeOperationLogs 数据条数：" + recipeParas.size());
                if (recipeParas.size() > 0) {
                    int cleanRows = cleanOldRecipePara(recipeParas);
                    logger.info("清理 recipeOperationLogs 数据条数：" + cleanRows);
                }
                logger.info("recipeOperationLogs 数据清理完成...");
            }
        }.start();
    }

    public Map<String, RecipePara> getMonitorParas(List<RecipePara> recipeParas, String deviceTypeCode) {
        List<RecipePara> recipeParasMonitor = new ArrayList<>();
        List<RecipeTemplate> recipeTemplates = new ArrayList<>();
        recipeTemplates = this.searchMonitorByMap(deviceTypeCode, "RecipePara", "Y");
        if (recipeTemplates == null || recipeTemplates.isEmpty()) {
            recipeTemplates = this.searchMonitorByMap(deviceTypeCode, "ECRecipePara", "Y");
        }
        if (recipeTemplates == null || recipeTemplates.isEmpty()) {
            recipeTemplates = this.searchMonitorByMap(deviceTypeCode, "SVRecipePara", "Y");
        }
        if (recipeTemplates == null || recipeTemplates.isEmpty()) {
            return null;
        }
        for (int i = 0; i < recipeTemplates.size(); i++) {
            for (RecipePara recipePara : recipeParas) {
                if (recipeTemplates.get(i).getParaCode().equals(recipePara.getParaCode())) {
                    recipeParasMonitor.add(recipePara);
                }
            }
        }
        Map<String, RecipePara> monitorParaMap = new HashMap<>();
        for (RecipePara recipePara : recipeParasMonitor) {
            monitorParaMap.put(recipePara.getParaCode(), recipePara);
        }
        return monitorParaMap;
    }

    public List<RecipeTemplate> searchRecipeTemplateByDeviceVariableType(String deviceCode, String deviceVariableType) {
        DeviceService deviceService = new DeviceService(session);
        DeviceInfo deviceInfo = deviceService.searchDeviceInfoByPara(null, deviceCode).get(0);
        Map paraMap = new HashMap();
        paraMap.put("deviceTypeId", deviceInfo.getDeviceTypeId());
        paraMap.put("deviceVariableType", deviceVariableType);
        return this.recipeTemplateMapper.searchByType(paraMap);
    }

    /**
     * 为保证下载流程能完整走完，isecs设备在执行下载过程中持续独占通道
     *
     * @param deviceInfo
     * @param recipe
     * @param type
     * @return
     */
    public String downLoadRcp2ISECSDeviceByTypeAutomatic(DeviceInfo deviceInfo, Recipe recipe, String type) {
        EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceInfo.getDeviceCode());
        String result = "";
        if (equipModel.getPassport()) {
            logger.info("设备:[" + deviceInfo.getDeviceCode() + "]进入下载流程,通讯通道将被独占");
            logger.debug("下载流程开始时间:" + new Date());
            String recipeName = recipe.getRecipeName();
            if (type.contains("Delete")) {
                String deleteString = equipModel.deleteRecipe(recipeName);
                //如果删除失败，流程继续             
            }
            if (type.contains("Download")) {
                String downLoadResult = equipModel.downloadRecipe(recipe);
                if (!downLoadResult.equals("0")) {
                    result = result + " " + downLoadResult;
                    equipModel.returnPassport();
                    return result;//如果下载失败，直接return
                } else {
                    result = downLoadResult;
                }
            }
            if (type.contains("Select")) {
                //选中Recipe
                String ppselectResult = equipModel.selectRecipe(recipeName);
                if (!ppselectResult.equals("0")) {
                    result = result + " " + ppselectResult;
                    if (type.contains("DeleteAll")) {
                        equipModel.deleteAllRcpFromDevice(recipeName);
                    }
                    equipModel.returnPassport();
                    return result;
                } else {
                    result = ppselectResult;
                }
            }
            if (type.contains("DeleteAll")) {
                equipModel.deleteAllRcpFromDevice(recipeName);
            }
            equipModel.returnPassport();
            logger.info("设备:[" + deviceInfo.getDeviceCode() + "]下载流程结束,通讯通道已被释放");
            logger.debug("下载流程结束时间:" + new Date());
        } else {
            result = "下载Recipe失败,通讯资源正在被占用,请稍后重试";
        }
        return result;
    }

    public int cleanOldRecipePara(List<RecipePara> recipeParas) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        int count = 0;
        try {
            List<DeviceInfoExt> deviceInfoExts = deviceService.getAllDeviceInfoExts();
            for (RecipePara recipePara : recipeParas) {
                boolean inUse = false;
                for (DeviceInfoExt deviceInfoExt : deviceInfoExts) {
                    String recipeId = recipePara.getRecipeRowId();
                    if (recipeId != null) {
                        if (recipeId.equals(deviceInfoExt.getRecipeId())) {
                            inUse = true;
                            break;
                        }
                    }
                }
                if (!inUse) {
                    count = count + deleteRcpParaByRecipeId(recipePara.getRecipeRowId());
                }
            }
            sqlSession.commit();
        } catch (Exception e) {
            logger.error("删除recipepara时出错:" + e.getMessage());
            sqlSession.close();
        } finally {
            sqlSession.close();
        }
        return count;
    }

    public String multiDownLoadRcp2DeviceByTypeAutomatic(DeviceInfo deviceInfo, List<Recipe> recipes) {
        //获取机台状态，判断是否可以下载Recipe
        //验证机台状态
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        String deviceId = deviceInfo.getDeviceCode();
        String checkResult = hostManager.checkBeforeDownload(deviceId, "");
        if (!"0".equals(checkResult)) {
            return checkResult;
        }
        if (recipes != null && !recipes.isEmpty()) {
            for (Recipe recipe : recipes) {
                String recipeFilePath = organizeRecipeDownloadFullFilePath(recipe);
                String downLoadResult = hostManager.downLoadRcp2DeviceComplete(recipeFilePath, deviceInfo, recipe);
                if (downLoadResult.equals("0")) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceId, "下载Reciep[" + recipe.getRecipeName() + "]成功");
                }
            }
        }
        return "0";
    }

    /**
     * 由于ws、ls站程序名问题，做一步转换（设备软件改正后将废弃）
     *
     * @param deviceCode
     * @param shotName
     * @param recipeName
     * @return
     */
    public List<RecipeNameMapping> getRecipeNameByDeviceCodeAndShotName(String deviceCode, String shotName, String recipeName) {
        Map paraMap = new HashMap();
        paraMap.put("deviceCode", deviceCode);
        paraMap.put("recipeShortName", shotName);
        paraMap.put("recipeName", recipeName);
        return this.recipeNameMappingMapper.searchRcpNameByDeviceCodeAndShotName(paraMap);
    }

    public int savaRecipeNameMapping(RecipeNameMapping recipeNameMapping) {
        return this.recipeNameMappingMapper.insert(recipeNameMapping);
    }
}
