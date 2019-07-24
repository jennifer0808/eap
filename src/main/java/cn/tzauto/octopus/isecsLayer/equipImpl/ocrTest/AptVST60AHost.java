/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.ocrTest;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author luosy
 */
public class AptVST60AHost extends EquipModel {

    private static Logger logger = Logger.getLogger(AptVST60AHost.class.getName());

    public AptVST60AHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        iSecsHostList.clear();
        iSecsHostList.add(iSecsHost);
        if (iSecsHost.isConnect) {
            this.equipState.setCommOn(true);
            commState = 1;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getEquipRealTimeState();
                }
            }).start();
        } else {
            this.equipState.setCommOn(false);
            commState = 0;
        }

    }

    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            iSecsHost.executeCommand("curscreen");
            ppExecName = iSecsHost.executeCommand("read recipename").get(0).split(":")[1].trim();
            Map map = new HashMap();
            map.put("PPExecName", ppExecName);
            changeEquipPanel(map);
            return ppExecName;
        }
    }

    @Override
    public String startEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String pauseEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String stopEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String lockEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map uploadRecipe(String recipeName) {
//        cleanRecipe("");
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //从页面取值
            iSecsHost.executeCommand("playback gotonowrecipe.txt");
            try {
                Thread.sleep(600);
            } catch (Exception e) {
            }
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
            Map<String, String> paraMap = iSecsHost.readAllParaByScreen("nowrecipe");
            List<RecipePara> recipeParas = new ArrayList<>();
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                for (Map.Entry<String, String> entry : paraMap.entrySet()) {
                    String paraName = entry.getKey();
                    String value = entry.getValue();
                    if (paraName.equals(recipeTemplate.getParaDesc())) {
                        recipePara.setSetValue(value);
                        recipeParas.add(recipePara);
                    }
                }
            }
            try {

                iSecsHost.executeCommand("playback exitnowrecipe.txt");
                //导出recipe文件
                iSecsHost.executeCommand("playback gotosetup.txt");
                iSecsHost.executeCommand("playback gotorestore.txt");
                iSecsHost.executeCommand("playback gotobackup.txt");
                iSecsHost.executeCommand("playback outportrecipe.txt");
                Thread.sleep(800);
//                iSecsHost.executeCommand("write outrecipe " + recipeName);
                iSecsHost.executeCommand("playback clickuploadrecipe.txt");
//                iSecsHost.executeCommand("dialog \"Please keyin backup recipe file\" write " + recipeName);
//                iSecsHost.executeCommand("dialog \"Please keyin backup recipe file\" action &Save");
                iSecsHost.executeCommand("playback save.txt");
                iSecsHost.executeCommand("replay enter.exe");
                iSecsHost.executeCommand("replay enter.exe");
                iSecsHost.executeCommand("playback exit.txt");
                Thread.sleep(1800);
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);

                String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
                sqlSession.close();
                String equipRecipePathtmp = equipRecipePath;
                TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");
                boolean ocrUploadOk = true;
                Thread.sleep(1500);
                List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                        + ftpUser + " " + ftpPwd + " " + equipRecipePathtmp + "\\" + "  " + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + " \"mput "
                        + "00000.Recipe\"");
            } catch (Exception e) {
            } finally {
                sqlSession.close();
            }
            Recipe recipe = setRecipe(recipeName);
            resultMap.put("recipe", recipe);
            resultMap.put("deviceCode", deviceCode);
//            resultMap.put("recipeFTPPath", ftpRecipePath);
            resultMap.put("recipeParaList", recipeParas);
            return resultMap;
        }
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
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
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/00000.Recipe", ftpPathTmp + recipe.getRecipeName() + ".Recipe_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
            } else {
                FtpUtil.downloadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/00000.Recipe", ftpPathTmp + recipe.getRecipeName() + ".Recipe", ftpip, ftpPort, ftpUser, ftpPwd);
            }
            List<String> result = iSecsHost.executeCommand("ftp " + localftpip + " "
                    + ftpUser + " " + ftpPwd + " " + equipRecipePath + "\\ " + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + " \"mget 00000.Recipe\"");

            iSecsHost.executeCommand("playback gotosetup.txt");
            iSecsHost.executeCommand("playback gotorestore.txt");
            iSecsHost.executeCommand("playback gotorestoreimport.txt");
            try {
//            iSecsHost.executeCommand("dialog \"Please select restore recipe file\" write " + recipe.getRecipeName());
//            iSecsHost.executeCommand("write inrecipe " + recipe.getRecipeName());
                Thread.sleep(800);
                iSecsHost.executeCommand("playback clickuploadrecipe.txt");
//            iSecsHost.executeCommand("dialog \"Please select restore recipe file\" action &Open");
                iSecsHost.executeCommand("playback open.txt");

                Thread.sleep(1800);
                iSecsHost.executeCommand("playback outportrecipe.txt");
                Thread.sleep(1800);
                iSecsHost.executeCommand("replay enter.exe");
//            iSecsHost.executeCommand("playback outportrecipe.txt");
//            iSecsHost.executeCommand("replay enter.exe");
//            iSecsHost.executeCommand("replay enter.exe");
                iSecsHost.executeCommand("playback exit.txt");
                Thread.sleep(800);
                iSecsHost.executeCommand("replay enter.exe");
                Thread.sleep(800);
                iSecsHost.executeCommand("replay enter.exe");
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(AptVST60AHost.class.getName()).log(Level.SEVERE, null, ex);
            }
            return "0";
        }
    }

    @Override
    public String deleteRecipe(String recipeName) {
        iSecsHost.executeCommand("playback gotorecipesetup.txt");
        iSecsHost.executeCommand("curscreen");
        while (true) {
//            List<String> recipe1s = iSecsHost.executeCommand("read recipe1");
            iSecsHost.executeCommand("playback delrecipe1.txt");
            iSecsHost.executeCommand("replay enter.exe");
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            List<String> recipe2s = iSecsHost.executeCommand("read recipe1");
            if (recipe2s.contains(" ")) {
                iSecsHost.executeCommand("playback exit.txt");
                iSecsHost.executeCommand("replay enter.exe");
                try {
                    Thread.sleep(2500);
                } catch (Exception e) {
                }
                iSecsHost.executeCommand("replay enter.exe");
                return "0";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        List<String> selrecipeList = iSecsHost.executeCommand("playback gotorecipelist.txt");
        for (String string : selrecipeList) {
            if (string.equals("done")) {
                iSecsHost.executeCommand("replay enter.exe");
                iSecsHost.executeCommand("replay tab.exe");
                iSecsHost.executeCommand("replay enter.exe");
                return "0";
            }
        }
        return "选中失败";
    }

    @Override
    public Map getEquipMonitorPara() {
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //从页面取值
            iSecsHost.executeCommand("playback gotonowrecipe.txt");
            try {
                Thread.sleep(600);
            } catch (Exception e) {
            }
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
            Map<String, String> paraMap = iSecsHost.readAllParaByScreen("nowrecipe");
            List<RecipePara> recipeParas = new ArrayList<>();
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                for (Map.Entry<String, String> entry : paraMap.entrySet()) {
                    String paraName = entry.getKey();
                    String value = entry.getValue();
                    if (paraName.equals(recipeTemplate.getParaDesc())) {
                        recipePara.setSetValue(value);
                        resultMap.put(recipeTemplate.getParaCode(), recipePara);
                        recipeParas.add(recipePara);
                    }
                }
                resultMap.put("recipeParaList", recipeParas);
            }
        }
        return resultMap;
    }

    @Override
    public Map getEquipRecipeList() {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        recipeNameList.add(ppExecName);
        eppd.put("eppd", recipeNameList);
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            equipStatus = iSecsHost.executeCommand("read equipstatus").get(0);
            Map map = new HashMap();
            map.put("EquipStatus", equipStatus);
            changeEquipPanel(map);
            return equipStatus;
        }
    }

    @Override
    public Object clone() {
        AptVST60AHost newEquip = new AptVST60AHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/00000.Recipe", remoteRcpPath, recipeName + ".Recipe"
                + "_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
//        this.deleteTempFile(recipeName);
        return true;
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
        attach.setAttachName(recipeName + ".Recipe_V" + recipe.getVersionNo());
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

    private void cleanRecipe(String keepRecipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> result = iSecsHost.executeCommand("dos \"dir " + equipRecipePath + "\\ /a/w\"");

            if (result != null && result.size() > 1) {
                for (String str : result) {
                    if (str.contains(".Recipe")) {
                        String[] recipeNameTmps = str.split(".Recipe");
                        for (int i = 0; i < recipeNameTmps.length; i++) {
                            String recipeNameTmp = recipeNameTmps[i].replaceAll(".Recipe", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
                            if (keepRecipeName.equals(recipeNameTmp)) {
                                continue;
                            }
                            List<String> result0 = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeNameTmp + ".Recipe\"");
                        }
                        String[] recipeNameTmps2 = str.split("\\s");
                        for (int i = 0; i < recipeNameTmps2.length; i++) {
                            String recipeNameTmp = recipeNameTmps2[i].replaceAll(".Recipe", "").replaceAll("\\[", "").replace(".]", "").replace("]", "").trim();
                            if (keepRecipeName.equals(recipeNameTmp)) {
                                continue;
                            }
                            List<String> result0 = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\" + recipeNameTmp + ".Recipe\"");
                        }
                    }
                }
            }
        }
    }

}
