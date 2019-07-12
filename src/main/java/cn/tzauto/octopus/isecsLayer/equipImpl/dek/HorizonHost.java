/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.dek;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

/**
 *
 * @author luosy
 */
public class HorizonHost extends EquipModel {

    private static Logger logger = Logger.getLogger(HorizonHost.class);

    public HorizonHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipename").get(0);
                    } else if ("recipedetail1".equals(result.get(0))) {
                        ppExecName = iSecsHost.executeCommand("read recipedetail1recipename").get(0);
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String pauseEquip() {
        String stopResult = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    String equipStatusTemp = this.getEquipStatus();
                    if (equipStatusTemp.equals("StartUp-Pause")) {
                        return "设备已经处于Pause状态！";
                    }
                    result = iSecsHost.executeCommand("playback pause.txt");
                    for (String start : result) {
                        if ("done".equals(start)) {
                            return "0";
                        }
                    }
                } catch (Exception e) {
                }
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
                stopResult = "未设置锁机！";
            }
        }
        return stopResult;
    }

    @Override
    public String stopEquip() {
        String stopResult = "";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                List<String> result = null;
                try {
                    Thread.sleep(5000);
                    result = iSecsHost.executeCommand("playback stop.txt");
                    for (String start : result) {
                        if ("done".equals(start)) {
                            return "0";
                        }
                    }
                } catch (Exception e) {
                }
            } else {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未设置锁机！");
                stopResult = "未设置锁机！";
            }
        }
        return stopResult;
    }

    @Override
    public String lockEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                sqlSession.close();
                String equipRecipePathtmp = equipRecipePath;
                boolean ocrUploadOk = true;

                TransferUtil.setPPBody(recipeName + ".ISD", 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + ".ISD" + "/TMP");
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + ".ISD" + " \"mput "
                        + recipeName + ".ISD\"");

                TransferUtil.setPPBody(recipeName + ".PR1", 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + ".PR1" + "/TMP");
                List<String> resultTemp1 = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + ".PR1" + " \"mput "
                        + recipeName + ".PR1\"");

                TransferUtil.setPPBody(recipeName + ".pxf", 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + ".pxf" + "/TMP");
                List<String> resultTemp2 = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + ".pxf" + " \"mput "
                        + recipeName + ".pxf\"");
                Map map = new HashMap();
                for (String uploadstr : resultTemp2) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        List<String> result1 = iSecsHost.executeCommand("curscreen");
                        if (result1.contains("main")) {
                            List<String> result2 = iSecsHost.executeCommand("playback gotorecipepara.txt");
                            if (result2.contains("done")) {
                                List<String> result3 = iSecsHost.executeCommand("curscreen");
                                if (result3.contains("recipepara")) {
                                    List<String> resultPara = iSecsHost.executeCommand("readm frontpressure rearpressure cleanrate1 cleanrate2 frontprintspeed rearprintspeed printfrontlimit "
                                            + "printrearlimit forwardXoffset reverseXoffset separatiionspeed separationdistance forwardYoffset reverseYoffset printgap forwardthetaoffset reversethetaoffset");
                                    map.put("frontpressure", takeNumber(resultPara.get(0)));
                                    map.put("rearpressure", takeNumber(resultPara.get(1)));
                                    map.put("cleanrate1", takeNumber(resultPara.get(2)));
                                    map.put("cleanrate2", takeNumber(resultPara.get(3)));
                                    map.put("frontprintspeed", takeNumber(resultPara.get(4)));
                                    map.put("rearprintspeed", takeNumber(resultPara.get(5)));
                                    map.put("printfrontlimit", takeNumber(resultPara.get(6)));
                                    map.put("printrearlimit", takeNumber(resultPara.get(7)));
                                    map.put("forwardXoffset", takeNumber(resultPara.get(8)));
                                    map.put("reverseXoffset", takeNumber(resultPara.get(9)));
                                    map.put("separatiionspeed", takeNumber(resultPara.get(10)));
                                    map.put("separationdistance", takeNumber(resultPara.get(11)));
                                    map.put("forwardYoffset", takeNumber(resultPara.get(12)));
                                    map.put("reverseYoffset", takeNumber(resultPara.get(13)));
                                    map.put("printgap", takeNumber(resultPara.get(14)));
                                    map.put("forwardthetaoffset", takeNumber(resultPara.get(15)));
                                    map.put("reversethetaoffset", takeNumber(resultPara.get(16)));
                                }
                            }
                            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                                RecipePara recipePara = new RecipePara();
                                String[] paraNameAtOCRs = new String[1];
                                paraNameAtOCRs[0] = recipeTemplate.getParaDesc();
                                String setValue = "";
                                recipePara.setParaName(recipeTemplate.getParaName());
                                recipePara.setParaCode(recipeTemplate.getParaCode());
                                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                                setValue = String.valueOf(map.get(paraNameAtOCRs[0]));
                                recipePara.setSetValue(setValue);
                                recipeParaList.add(recipePara);
                            }
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", ftpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);
                        for (int i = 0; i < recipeParaList.size(); i++) {
                            logger.info(recipeParaList.get(i).getParaName().toString() + "======" + recipeParaList.get(i).getSetValue().toString());
                        }
                    }
                    if (uploadstr.contains("Not connected")) {
                        ocrUploadOk = false;
                    }
                }
                if (!ocrUploadOk) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        return resultMap;
    }

    public String takeNumber(String str) {
        String result = "";
        if (str != null && !"".equals(str)) {
            char[] strArray = str.toCharArray();
            for (char c : strArray) {
                if (c >= 45 && c <= 57 && c != 47) {
                    result += c;
                }
            }
        }
        return result;
    }

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            if (i == 0) {
                attach.setAttachName(recipeName + ".ISD_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo());
            } else if (i == 1) {
                attach.setAttachName(recipeName + ".PR1_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo());
            } else if (i == 2) {
                attach.setAttachName(recipeName + ".pxf_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".ALU_V" + recipe.getVersionNo());
            }
            attach.setAttachType("");
            attach.setSortNo(0);
            if (GlobalConstants.sysUser != null) {
                attach.setCreateBy(GlobalConstants.sysUser.getId());
                attach.setUpdateBy(GlobalConstants.sysUser.getId());
            } else {
                attach.setCreateBy("System");
                attach.setUpdateBy("System");
            }
            attachs.add(attach);
        }
        sqlSession.close();
        return attachs;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            try {
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);
                if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                    return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                }
                if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/" + recipe.getRecipeName() + ".ISD", ftpPathTmp + recipe.getRecipeName() + ".ISD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/" + recipe.getRecipeName() + ".PR1", ftpPathTmp + recipe.getRecipeName() + ".PR1_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/" + recipe.getRecipeName() + ".pxf", ftpPathTmp + recipe.getRecipeName() + ".pxf_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/" + recipe.getRecipeName() + ".ISD", ftpPathTmp + recipe.getRecipeName() + ".ISD", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/" + recipe.getRecipeName() + ".PR1", ftpPathTmp + recipe.getRecipeName() + ".PR1", ftpip, ftpPort, ftpUser, ftpPwd);
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/" + recipe.getRecipeName() + ".pxf", ftpPathTmp + recipe.getRecipeName() + ".pxf", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);

                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/" + recipe.getRecipeName() + ".ISD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/002.dat");
                        List list1 = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/002.ISD", deviceType);
                        RecipeEdit.writeRecipeFile(list1, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/" + recipe.getRecipeName() + ".ISD");

                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/" + recipe.getRecipeName() + ".PR1", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/002.dat");
                        List list2 = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/002.PR1", deviceType);
                        RecipeEdit.writeRecipeFile(list2, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/" + recipe.getRecipeName() + ".PR1");

                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/" + recipe.getRecipeName() + ".pxf", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/002.dat");
                        List list3 = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/002.pxf", deviceType);
                        RecipeEdit.writeRecipeFile(list3, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/" + recipe.getRecipeName() + ".pxf");

                    }
                }
                List<String> result1 = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".ISD/" + " \"mget " + recipe.getRecipeName() + ".ISD\"");
                List<String> result2 = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".PR1/" + " \"mget " + recipe.getRecipeName() + ".PR1\"");
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePath + " " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + ".pxf/" + " \"mget " + recipe.getRecipeName() + ".pxf\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        return "0";
                    }
                    if (str.contains("Not connected")) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage());
                return "下载失败,出现异常:" + e.getMessage();
            } finally {
                sqlSession.close();
                this.deleteTempFile(recipe.getRecipeName());
            }
        }
        return "Download recipe " + recipe.getRecipeName() + " failed";
    }

    @Override
    public Map getEquipRealTimeState() {
        equipStatus = getEquipStatus();
        ppExecName = getCurrentRecipeName();
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
        map.put("ControlState", controlState);
        return map;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + "*\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        return "删除成功";
                    }
                }
                return "删除失败";
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage());
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                for (String str : result) {
                    if ("done".equals(str)) {
                        ppExecName = recipeName;
                        return "0";
                    }
                    if (str.contains("rror")) {
                        return "选中失败";
                    }
                }
                return "选中失败";
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                return "选中失败";
            }
        }
    }

    @Override
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        List<RecipePara> recipeParas = (List<RecipePara>) getEquipMonitorPara().get("recipeParaList");
        if (recipeParas == null) {
            return new ArrayList<>();
        }
        return recipeParas;
    }

    @Override
    public Map getEquipMonitorPara() {
        Map map = new HashMap();
        Map mapResult = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<RecipePara> recipeParaList = new ArrayList<>();
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");

                logger.info("执行开机参数检查");
                List<String> result1 = iSecsHost.executeCommand("curscreen");
                if (result1.contains("main")) {
                    List<String> result2 = iSecsHost.executeCommand("playback gotorecipepara.txt");

                    List<String> result3 = iSecsHost.executeCommand("curscreen");
                    if (result3.contains("recipepara")) {
                        List<String> resultPara = iSecsHost.executeCommand("readm frontpressure rearpressure cleanrate1 cleanrate2 frontprintspeed rearprintspeed printfrontlimit "
                                + "printrearlimit forwardXoffset reverseXoffset separatiionspeed separationdistance forwardYoffset reverseYoffset printgap forwardthetaoffset reversethetaoffset");
                        map.put("frontpressure", takeNumber(resultPara.get(0)));
                        map.put("rearpressure", takeNumber(resultPara.get(1)));
                        map.put("cleanrate1", takeNumber(resultPara.get(2)));
                        map.put("cleanrate2", takeNumber(resultPara.get(3)));
                        map.put("frontprintspeed", takeNumber(resultPara.get(4)));
                        map.put("rearprintspeed", takeNumber(resultPara.get(5)));
                        map.put("printfrontlimit", takeNumber(resultPara.get(6)));
                        map.put("printrearlimit", takeNumber(resultPara.get(7)));
                        map.put("forwardXoffset", takeNumber(resultPara.get(8)));
                        map.put("reverseXoffset", takeNumber(resultPara.get(9)));
                        map.put("separatiionspeed", takeNumber(resultPara.get(10)));
                        map.put("separationdistance", takeNumber(resultPara.get(11)));
                        map.put("forwardYoffset", takeNumber(resultPara.get(12)));
                        map.put("reverseYoffset", takeNumber(resultPara.get(13)));
                        map.put("printgap", takeNumber(resultPara.get(14)));
                        map.put("forwardthetaoffset", takeNumber(resultPara.get(15)));
                        map.put("reversethetaoffset", takeNumber(resultPara.get(16)));
                        for (RecipeTemplate recipeTemplate : recipeTemplates) {
                            RecipePara recipePara = new RecipePara();
                            String[] paraNameAtOCRs = new String[1];
                            paraNameAtOCRs[0] = recipeTemplate.getParaDesc();
                            if (paraNameAtOCRs[0] == null || "".equals(paraNameAtOCRs[0])) {
                                continue;
                            }
                            String setValue = "";
                            recipePara.setParaName(recipeTemplate.getParaName());
                            recipePara.setParaCode(recipeTemplate.getParaCode());
                            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                            setValue = String.valueOf(map.get(paraNameAtOCRs[0].trim()));
                            recipePara.setSetValue(setValue);
                            recipeParaList.add(recipePara);
                        }
                        mapResult.put("recipeParaList", recipeParaList);
                        for (RecipePara recipePara : recipeParaList) {
                            logger.info(recipePara.getParaCode() + "----" + recipePara.getSetValue());
                        }
                    }

                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        logger.info("monitormap:" + map.toString());
        return mapResult;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = new ArrayList<>();
            try {
                result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + " /a/w\"");
            } catch (Exception e) {
                return eppd;
            }

            if (result != null && result.size() > 1) {
                for (String str : result) {
                    str = str.replace("[", "").replace(" ", "");
                    String[] dirs = str.split("]");
                    for (String dir : dirs) {
                        if (dir.equals(".") || dir.equals("..")) {
                            continue;
                        }
                        if (!str.contains(".ISD")) {
                            continue;
                        }
                        if (str.contains(".ISD")) {
                            if (str.split(".ISD")[0].contains(".pxf") || str.split(".ISD")[0].contains(".PR1")) {
                                String[] recipeNameTmps = str.split(".ISD");
                                if (recipeNameTmps[0].contains(".pxf")) {
                                    String w = recipeNameTmps[0].split(".pxf")[1].replace(" ", "");
                                    System.out.println("====" + w);
                                    recipeNameList.add(w);
                                } else {
                                    String w = recipeNameTmps[0].split(".PR1")[1].replace(" ", "");
                                    System.out.println("====" + w);
                                    recipeNameList.add(w);
                                }

                            } else {
                                String[] recipeNameTmps = str.split(".ISD");
                                System.out.println("====" + recipeNameTmps[0]);
                                recipeNameList.add(recipeNameTmps[0]);
                            }
                        }
                    }
                }
            }
        }
        eppd.put("eppd", recipeNameList);
        return eppd;
    }

    @Override
    public Object clone() {
        HorizonHost newEquip = new HorizonHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if ("alarm".equals(result.get(0))) {
                    List<String> alidresult = iSecsHost.executeCommand("read alarmid");
                    byte[] b = alidresult.get(0).getBytes("GBK");
                    String result3 = new String(b, "UTF-8");
                    if (alidresult.size() > 1) {
                        alarmStrings.add(result3);
                        logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        return alarmStrings;
    }

    @Override
    public String getMTBA() {
        String mtba = "";
        try {
            List<String> result = iSecsHost.executeCommand("read mtba");
            if (result != null && result.size() > 1) {
                logger.info("Get the MTBA from equip:" + deviceCode + " MTBA:[" + result.get(0) + "");
                mtba = result.get(0);
            }
        } catch (Exception e) {
            logger.error("Get Equip MTBA error:" + e.getMessage());
        }
        return mtba;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        List<String> result2 = iSecsHost.executeCommand("read status");
                        if ("运行中".equalsIgnoreCase(result2.get(0)) || "Run".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Run";
                        } else if ("准备好".equalsIgnoreCase(result2.get(0)) || "Ready".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "Ready";
                        } else if ("shutdown".equalsIgnoreCase(result2.get(0))) {
                            equipStatus = "ShutDown";
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
            }
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        changeEquipPanel(map);
        return equipStatus;
    }

    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + ".ISD/" + recipeName + ".ISD", remoteRcpPath, recipeName + ".ISD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + ".PR1/" + recipeName + ".PR1", remoteRcpPath, recipeName + ".PR1_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + ".pxf/" + recipeName + ".pxf", remoteRcpPath, recipeName + ".pxf_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result1 = iSecsHost.executeCommand("curscreen");
            if (result1.contains("main")) {
                iSecsHost.executeCommand("playback gotorecipepara.txt");
            }
            List<String> result3 = iSecsHost.executeCommand("curscreen");
            if (result3.contains("recipepara")) {
                List<String> resultPara = iSecsHost.executeCommand("readm frontpressure rearpressure cleanrate1 cleanrate2 frontprintspeed rearprintspeed printfrontlimit "
                        + "printrearlimit forwardXoffset reverseXoffset separatiionspeed separationdistance forwardYoffset reverseYoffset printgap forwardthetaoffset reversethetaoffset");
                map.put("frontpressure", takeNumber(resultPara.get(0)));
                map.put("rearpressure", takeNumber(resultPara.get(1)));
                map.put("cleanrate1", takeNumber(resultPara.get(2)));
                map.put("cleanrate2", takeNumber(resultPara.get(3)));
                map.put("frontprintspeed", takeNumber(resultPara.get(4)));
                map.put("rearprintspeed", takeNumber(resultPara.get(5)));
                map.put("printfrontlimit", takeNumber(resultPara.get(6)));
                map.put("printrearlimit", takeNumber(resultPara.get(7)));
                map.put("forwardXoffset", takeNumber(resultPara.get(8)));
                map.put("reverseXoffset", takeNumber(resultPara.get(9)));
                map.put("separatiionspeed", takeNumber(resultPara.get(10)));
                map.put("separationdistance", takeNumber(resultPara.get(11)));
                map.put("forwardYoffset", takeNumber(resultPara.get(12)));
                map.put("reverseYoffset", takeNumber(resultPara.get(13)));
                map.put("printgap", takeNumber(resultPara.get(14)));
                map.put("forwardthetaoffset", takeNumber(resultPara.get(15)));
                map.put("reversethetaoffset", takeNumber(resultPara.get(16)));
            }
        }
        logger.info("getSpecificData：" + map);
        return map;
    }
}
