/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.heller;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;

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
 * @author
 */
public class Heller1913Host extends EquipModel {

    private static Logger logger = Logger.getLogger(Heller1913Host.class.getName());

    public Heller1913Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("any".equals(result.get(0))) {
                        List<String> result1 = iSecsHost.executeCommand("read recipename");
                        String name = result1.get(0);
                        String nameTemp = "";
                        for (int i = 0; i < name.length(); i++) {
                            char a = name.charAt(i);
                            // 当从Unicode编码向某个字符集转换时，如果在该字符集中没有对应的编码，则得到0x3f（即问号字符?）
                            //从其他字符集向Unicode编码转换时，如果这个二进制数在该字符集中没有标识任何的字符，则得到的结果是0xfffd
                            if (Integer.valueOf(a) <= 127 && Integer.valueOf(a) >= 32) {
                                nameTemp = nameTemp + a;
                            }
                        }
                        ppExecName = nameTemp.replace("?", "").replace("<", "").replace(">", "").replace(":", "").trim().split(".JOB")[0];
                        char a = ppExecName.charAt(0);
                        if ("-".equals(String.valueOf(a))) {
                            ppExecName = ppExecName.substring(1);
                        }
                        if (ppExecName.contains("Oven Operating")) {
                            ppExecName = ppExecName.split("Overview-")[1];
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
            }
        }
        if (ppExecName.contains("done") || ppExecName.contains("error")) {
            ppExecName = "--";
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String pauseEquip() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String stopEquip() {
        return "0";
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
                String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                sqlSession.close();
                String equipRecipePathtmp = equipRecipePath;
                boolean ocrUploadOk = true;

                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");

                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " $" + equipRecipePathtmp + "$  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + recipeName + ".JOB\"");
                Map map = new HashMap();
                for (String uploadstr : result) {
                    if ("done".equals(uploadstr)) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        List<String> result1 = iSecsHost.executeCommand("curscreen");
                        if (result1.contains("any")) {
                            for (String str : result1) {
                                if (str.contains("done")) {
                                    List<String> resultPara1 = iSecsHost.executeCommand("readm sptop1 sptop2 sptop3 sptop4 sptop5 sptop6 sptop7 sptop8 sptop9 sptop10 sptop11 sptop12 sptop13"
                                            + " sptopc1 sptopc2 pvtop1 pvtop2 pvtop3 pvtop4 pvtop5 pvtop6 pvtop7 pvtop8 pvtop9 pvtop10 pvtop11 pvtop12 pvtop13 pvtopc1 pvtopc2");
                                    map.put("sptop1", resultPara1.get(0));
                                    map.put("sptop2", resultPara1.get(1));
                                    map.put("sptop3", resultPara1.get(2));
                                    map.put("sptop4", resultPara1.get(3));
                                    map.put("sptop5", resultPara1.get(4));
                                    map.put("sptop6", resultPara1.get(5));
                                    map.put("sptop7", resultPara1.get(6));
                                    map.put("sptop8", resultPara1.get(7));
                                    map.put("sptop9", resultPara1.get(8));
                                    map.put("sptop10", resultPara1.get(9));
                                    map.put("sptop11", resultPara1.get(10));
                                    map.put("sptop12", resultPara1.get(11));
                                    map.put("sptop13", resultPara1.get(12));
                                    map.put("sptopc1", resultPara1.get(13));
                                    map.put("sptopc2", resultPara1.get(14));
                                    map.put("pvtop1", resultPara1.get(15));
                                    map.put("pvtop2", resultPara1.get(16));
                                    map.put("pvtop3", resultPara1.get(17));
                                    map.put("pvtop4", resultPara1.get(18));
                                    map.put("pvtop5", resultPara1.get(19));
                                    map.put("pvtop6", resultPara1.get(20));
                                    map.put("pvtop7", resultPara1.get(21));
                                    map.put("pvtop8", resultPara1.get(22));
                                    map.put("pvtop9", resultPara1.get(23));
                                    map.put("pvtop10", resultPara1.get(24));
                                    map.put("pvtop11", resultPara1.get(25));
                                    map.put("pvtop12", resultPara1.get(26));
                                    map.put("pvtop13", resultPara1.get(27));
                                    map.put("pvtopc1", resultPara1.get(28));
                                    map.put("pvtopc2", resultPara1.get(29));
                                }
                            }
                        }
                        List<String> result2 = iSecsHost.executeCommand("curscreen");
                        if (result2.contains("any")) {
                            for (String str : result2) {
                                if (str.contains("done")) {
                                    List<String> resultPara2 = iSecsHost.executeCommand("readm pvbottom1 pvbottom2 pvbottom3 pvbottom4 pvbottom5 pvbottom6 pvbottom7 pvbottom8 pvbottom9 pvbottom10 pvbottom11 pvbottom12 pvbottom13"
                                            + " spbottom1 spbottom2 spbottom3 spbottom4 spbottom5 spbottom6 spbottom7 spbottom8 spbottom9 spbottom10 spbottom11 spbottom12 spbottom13");
//                                    for (int i = 0; i < resultPara1.size(); i++) {
//                                        map.put(i, resultPara1.get(i));
//                                    }
                                    map.put("pvbottom1", resultPara2.get(0));
                                    map.put("pvbottom2", resultPara2.get(1));
                                    map.put("pvbottom3", resultPara2.get(2));
                                    map.put("pvbottom4", resultPara2.get(3));
                                    map.put("pvbottom5", resultPara2.get(4));
                                    map.put("pvbottom6", resultPara2.get(5));
                                    map.put("pvbottom7", resultPara2.get(6));
                                    map.put("pvbottom8", resultPara2.get(7));
                                    map.put("pvbottom9", resultPara2.get(8));
                                    map.put("pvbottom10", resultPara2.get(9));
                                    map.put("pvbottom11", resultPara2.get(10));
                                    map.put("pvbottom12", resultPara2.get(11));
                                    map.put("pvbottom13", resultPara2.get(12));
                                    map.put("spbottom1", resultPara2.get(13));
                                    map.put("spbottom2", resultPara2.get(14));
                                    map.put("spbottom3", resultPara2.get(15));
                                    map.put("spbottom4", resultPara2.get(16));
                                    map.put("spbottom5", resultPara2.get(17));
                                    map.put("spbottom6", resultPara2.get(18));
                                    map.put("spbottom7", resultPara2.get(19));
                                    map.put("spbottom8", resultPara2.get(20));
                                    map.put("spbottom9", resultPara2.get(21));
                                    map.put("spbottom10", resultPara2.get(22));
                                    map.put("spbottom11", resultPara2.get(23));
                                    map.put("spbottom12", resultPara2.get(24));
                                    map.put("spbottom13", resultPara2.get(25));
                                }
                            }
                        }
                        List<String> result3 = iSecsHost.executeCommand("curscreen");
                        if (result3.contains("any")) {
                            for (String str : result3) {
                                if (str.contains("done")) {
                                    List<String> resultPara2 = iSecsHost.executeCommand("readm conveyor1 conveyor2 channel ppm alarm1");
                                    map.put("conveyor1", resultPara2.get(0));
                                    map.put("conveyor2", resultPara2.get(1));
                                    map.put("channel", resultPara2.get(2));
                                    map.put("ppm", resultPara2.get(3));
                                    map.put("alarm1", resultPara2.get(4));
                                }
                            }
                        }
                        for (RecipeTemplate recipeTemplate : recipeTemplates) {
                            RecipePara recipePara = new RecipePara();
                            String[] paraNameAtOCRs = new String[1];
                            paraNameAtOCRs[0] = recipeTemplate.getParaDesc();
                            String setValue = "";
                            recipePara.setParaName(recipeTemplate.getParaName());
                            recipePara.setParaCode(recipeTemplate.getParaCode());
                            setValue = String.valueOf(map.get(paraNameAtOCRs[0]));
                            recipePara.setSetValue(setValue);
                            recipeParaList.add(recipePara);
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", ftpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);
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

    @Override
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        Attach attach = new Attach();
        attach.setId(UUID.randomUUID().toString());
        attach.setRecipeRowId(recipe.getId());
        attach.setAttachPath(ftpRecipePath);
        attach.setAttachName(recipeName + ".JOB_V" + recipe.getVersionNo());
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
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB", ftpPathTmp + recipe.getRecipeName() + ".JOB_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
                } else {
                    FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB", ftpPathTmp + recipe.getRecipeName() + ".JOB", ftpip, ftpPort, ftpUser, ftpPwd);
                    if (RecipeEdit.hasGoldPara(deviceType)) {
                        RecipeService recipeService = new RecipeService(sqlSession);
                        List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                        FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.JOB");
                        List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.JOB", deviceType);
                        RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipe.getRecipeName() + ".JOB");
                    }
                }
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " $" + equipRecipePath + "$ $" + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/$" + " \"mget " + recipe.getRecipeName() + ".JOB\"");
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
                List<String> result = iSecsHost.executeCommand("dos $del /q " + " \"" + equipRecipePath + "\\*.JOB\"$");
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
                List<String> result = iSecsHost.executeCommand("readrectcolor 102 59 104 61");
                for (String str : result) {
                    if ("0x808000".equals(str)) {
                        List<String> result1 = iSecsHost.executeCommand("playback selrecipe1.txt");
                        sleep(500);
                        List<String> result2 = iSecsHost.executeCommand("dialog \"Open an existing Recipe\" write " + recipeName);
                        sleep(500);
                        List<String> result3 = iSecsHost.executeCommand("dialog \"Open an existing Recipe\" 1 ");
                        sleep(500);
                        List<String> result4 = iSecsHost.executeCommand("playback selrecipe2.txt");
                        if (result4.contains("done")) {
                            ppExecName = recipeName;
                            return "0";
                        }
                        if (result4.contains("rror")) {
                            return "选中失败";
                        }
                    }
                    if ("0xa0a0a0".equals(str) || "0xffffff".equals(str)) {
                        //需要先登录用户权限才能更换recipe
                        List<String> result1 = iSecsHost.executeCommand("playback login1.txt");
                        if (result1.contains("done")) {
                            this.sleep(1000);
                            List<String> result2 = iSecsHost.executeCommand("curscreen");
                            this.sleep(1000);
                            List<String> result3 = iSecsHost.executeCommand("replay login.exe");
                            Thread.sleep(500);
                            List<String> result4 = iSecsHost.executeCommand("playback selrecipe1.txt");
                            sleep(500);
                            List<String> result5 = iSecsHost.executeCommand("dialog \"Open an existing Recipe\" write " + recipeName);
                            sleep(500);
                            List<String> result6 = iSecsHost.executeCommand("dialog \"Open an existing Recipe\" 1 ");
                            sleep(500);
                            List<String> result7 = iSecsHost.executeCommand("playback selrecipe2.txt");
                            if (result7.contains("done")) {
                                ppExecName = recipeName;
                                return "0";
                            }
                            if (result7.contains("rror")) {
                                return "选中失败";
                            }

//                            {
//                                this.sleep(1000);
//                                List<String> result3 = iSecsHost.executeCommand("write username oven");
//                                if (result3.contains("done")) {
//                                    List<String> result4 = iSecsHost.executeCommand("playback login2.txt");
//                                    if (result4.contains("done")) {
//                                        List<String> result5 = iSecsHost.executeCommand("curscreen");
//                                        if (result5.contains("login")) {
//                                            this.sleep(1000);
//                                            List<String> result6 = iSecsHost.executeCommand("write password oven");
//                                            List<String> result7 = iSecsHost.executeCommand("playback login3.txt");
//                                            List<String> result8 = iSecsHost.executeCommand("playback selrecipe1.txt");
//                                            sleep(500);
//                                            List<String> result9 = iSecsHost.executeCommand("dialog \"Open an existing Recipe\" write " + recipeName);
//                                            sleep(500);
//                                            List<String> result10 = iSecsHost.executeCommand("dialog \"Open an existing Recipe\" 1 ");
//                                            sleep(500);
//                                            List<String> result11 = iSecsHost.executeCommand("playback selrecipe2.txt");
//                                            if (result11.contains("done")) {
//                                                ppExecName = recipeName;
//                                                return "0";
//                                            }
//                                            if (result11.contains("rror")) {
//                                                return "选中失败";
//                                            }
//                                        }
//                                    }
//                                }
//                            }
                        }
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
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                RecipeService recipeService = new RecipeService(sqlSession);
                List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
                sqlSession.close();
                List<RecipePara> recipeParaList = new ArrayList<>();
                List<String> result1 = iSecsHost.executeCommand("curscreen");
                if (result1.contains("any")) {

                    List<String> resultPara1 = iSecsHost.executeCommand("readm sptop1 sptop2 sptop3 sptop4 sptop5 sptop6 sptop7 sptop8 sptop9 sptop10 sptop11 sptop12 sptop13"
                            + " sptopc1 sptopc2 ");
                    map.put("sptop1", resultPara1.get(0));
                    map.put("sptop2", resultPara1.get(1));
                    map.put("sptop3", resultPara1.get(2));
                    map.put("sptop4", resultPara1.get(3));
                    map.put("sptop5", resultPara1.get(4));
                    map.put("sptop6", resultPara1.get(5));
                    map.put("sptop7", resultPara1.get(6));
                    map.put("sptop8", resultPara1.get(7));
                    map.put("sptop9", resultPara1.get(8));
                    map.put("sptop10", resultPara1.get(9));
                    map.put("sptop11", resultPara1.get(10));
                    map.put("sptop12", resultPara1.get(11));
                    map.put("sptop13", resultPara1.get(12));
                    map.put("sptopc1", resultPara1.get(13));
                    map.put("sptopc2", resultPara1.get(14));

                }
                List<String> result2 = iSecsHost.executeCommand("curscreen");
                if (result2.contains("any")) {

                    List<String> resultPara2 = iSecsHost.executeCommand("readm spbottom1 spbottom2 spbottom3 spbottom4 spbottom5 spbottom6 spbottom7 spbottom8 spbottom9 spbottom10 spbottom11 spbottom12 spbottom13");
                    map.put("spbottom1", resultPara2.get(0));
                    map.put("spbottom2", resultPara2.get(1));
                    map.put("spbottom3", resultPara2.get(2));
                    map.put("spbottom4", resultPara2.get(3));
                    map.put("spbottom5", resultPara2.get(4));
                    map.put("spbottom6", resultPara2.get(5));
                    map.put("spbottom7", resultPara2.get(6));
                    map.put("spbottom8", resultPara2.get(7));
                    map.put("spbottom9", resultPara2.get(8));
                    map.put("spbottom10", resultPara2.get(9));
                    map.put("spbottom11", resultPara2.get(10));
                    map.put("spbottom12", resultPara2.get(11));
                    map.put("spbottom13", resultPara2.get(12));
                }
                List<String> result3 = iSecsHost.executeCommand("curscreen");
                if (result3.contains("any")) {
                    List<String> resultPara2 = iSecsHost.executeCommand("readm conveyor1 conveyor2 channel ppm alarm1");
                    map.put("conveyor1", resultPara2.get(0));
                    map.put("conveyor2", resultPara2.get(1));
                    map.put("channel", resultPara2.get(2));
                    map.put("ppm", resultPara2.get(3));
                    map.put("alarm1", resultPara2.get(4));
                }
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
                    if ("null".equals(setValue)) {
                        continue;
                    }
                    recipePara.setSetValue(setValue);
                    recipeParaList.add(recipePara);
                    map.put(recipePara.getParaCode(), recipePara);
                }
                map.put("recipeParaList", recipeParaList);
            } catch (Exception e) {
                logger.error("Get equip status error:" + e.getMessage());
                return null;
            }
        }
        logger.info("monitormap:" + map.toString());
        return map;
    }

    @Override
    public Map getSpecificData(Map<String, String> dataIdMap) {
        Map valueMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            valueMap = iSecsHost.readAllParaByScreen("any");
            String curscreen = iSecsHost.executeCommand("curscreen").get(0);
            if ("any".equals(curscreen)) {
                Map paraMap = new HashMap();
                List<String> resultPara1 = iSecsHost.executeCommand("readm sptop1 sptop2 sptop3 sptop4 sptop5 sptop6 sptop7 sptop8 sptop9 sptop10 sptop11 sptop12 sptop13");
                paraMap.put("sptop1", resultPara1.get(0));
                paraMap.put("sptop2", resultPara1.get(1));
                paraMap.put("sptop3", resultPara1.get(2));
                paraMap.put("sptop4", resultPara1.get(3));
                paraMap.put("sptop5", resultPara1.get(4));
                paraMap.put("sptop6", resultPara1.get(5));
                paraMap.put("sptop7", resultPara1.get(6));
                paraMap.put("sptop8", resultPara1.get(7));
                paraMap.put("sptop9", resultPara1.get(8));
                paraMap.put("sptop10", resultPara1.get(9));
                paraMap.put("sptop11", resultPara1.get(10));
                paraMap.put("sptop12", resultPara1.get(11));
                paraMap.put("sptop13", resultPara1.get(12));
                logger.debug("paraMap" + paraMap);
                valueMap.putAll(paraMap);
                Map ppmMap = new HashMap();
                List<String> resultPara2 = iSecsHost.executeCommand("readm sptopc1 sptopc2 conveyor1 ppm recipename");
                ppmMap.put("sptopc1", resultPara2.get(0));
                ppmMap.put("sptopc2", resultPara2.get(1));
                ppmMap.put("conveyor1", resultPara2.get(2));
                ppmMap.put("ppm", resultPara2.get(3));
                String rcpName = resultPara2.get(4);
                String nameTemp = rcpName.replace("?", "").replace("<", "").replace(">", "").replace(":", "").trim().split(".JOB")[0];
                if(nameTemp.charAt(0)=='-'){
                    nameTemp = nameTemp.substring(1);
                }
                if (nameTemp.contains("Oven Operating")) {
                    nameTemp = nameTemp.split("Overview-")[1];
                }
                ppmMap.put("rcpName", nameTemp);
                logger.debug("ppmMap" + ppmMap);
                valueMap.putAll(ppmMap);
            }
        }
        logger.info("valueMap:" + valueMap);
        return valueMap;
    }

    public List<String> getEquipEvent() {
        List<String> result = new ArrayList<>();
        try {
            result = iSecsHost.executeCommand("dos \"type D:\\DoDiscoLog.txt\"");
            for (String str : result) {
                if ("done".equals(str)) {
                    result.remove(str);
                }
            }
        } catch (Exception e) {
            logger.error("Get EquipEvent error:" + e.getMessage());
        }
        return result;
    }

    public String clearEquipEvent() {
        try {
            List<String> result = iSecsHost.executeCommand("dos \"del D:\\DoDiscoLog.txt\"");
            for (String str : result) {
                if ("done".equals(str)) {
                    return "0";
                }
            }
            return "Clear EquipEvent failed";
        } catch (Exception e) {
            logger.error("Clear EquipEvent error:" + e.getMessage());
            return "Clear EquipEvent failed";
        }
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = new ArrayList<>();
            try {
//                result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + " /ad/w\"");
                result = iSecsHost.executeCommand("dos $dir \"" + equipRecipePath + "\" /a/w$");
            } catch (Exception e) {
                return eppd;
            }
            if (result != null && result.size() > 1) {
                for (String str : result) {
                    if (!str.contains(".JOB")) {
                        continue;
                    }
                    if (str.contains("[")) {
                        str = str.replace("[", "").replace(" ", "");
                        String[] dirs = str.split("]");
                        for (String dir : dirs) {
                            if (dir.equals(".") || dir.equals("..")) {
                                continue;
                            }
                            if (dir.contains(".JOB")) {
                                dir = dir.replace(".JOB", ".");
                                String[] dirsTemp = dir.split("\\.");
//                                for (String dir1 : dirsTemp) {
//                                    recipeNameList.add(dir1);
//                                }
                                recipeNameList.add(dirsTemp[0]);
                            }
                        }
                    } else {
                        str = str.replace(".JOB", ".").replace(" ", "");
                        String[] dirs = str.split("\\.");
                        for (String dir : dirs) {
                            recipeNameList.add(dir);
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
        Heller1913Host newEquip = new Heller1913Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        return null;
//        List<String> alarmStrings = new ArrayList<>();
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                List<String> result = iSecsHost.executeCommand("readrectcolor 730 50 750 60 ");
//                for (String colorstr : result) {
//                    if ("0xc0c0c0".equals(colorstr)) {
//                        alarmStrings.add("");
//                    }
//                    if ("0xff0000".equals(colorstr)) {
//                        logger.info("The equip state changged to alarm...");
//                        List<String> alidresult = iSecsHost.executeCommand("read alarmid");
//                        if (alidresult.size() > 1) {
//                            alarmStrings.add(alidresult.get(0));
//                            logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Get EquipAlarm error:" + e.getMessage());
//            }
//        }
//        //添加了一个过滤，异常时可能会将
//        if (alarmStrings.size() > 0) {
//            alarmStrings.remove("0xff0000");
//        }
//        return alarmStrings;
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
                    if (result.contains("Idle")) {
                        equipStatus = "Idle";
                    } else {
                        equipStatus = "Run";
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
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".JOB", remoteRcpPath, recipeName + ".JOB_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected String checkEquipStatus() {
        return "0";
    }
}
