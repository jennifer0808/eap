package cn.tzauto.octopus.isecsLayer.equipImpl.screen;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.screen.ScreenRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luosy on 2019/5/16.
 */
public class ScreenHost extends EquipModel {

    private static Logger logger = Logger.getLogger(ScreenHost.class.getName());

    private String recipeServerPath = GlobalConstants.stage.hostManager.getDeviceInfo(deviceCode, deviceCode).getRemarks();

    public ScreenHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        String prerecipeName = ppExecName;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("main".equals(result.get(0))) {
                        String recipeNameTemp = iSecsHost.executeCommand("read recipename").get(0);
                        if (recipeNameTemp.contains("/")) {
                            ppExecName = recipeNameTemp.substring(recipeNameTemp.lastIndexOf("/") + 1);
                        }
                        if ("done".equals(ppExecName)) {
                            ppExecName = prerecipeName;
                        }
                    } else {
                        ppExecName = prerecipeName;
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage());
                ppExecName = "--";
                return prerecipeName;
            }
        }
        Map map = new HashMap();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        return null;
    }

    @Override
    public String pauseEquip() {
        return null;
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
                    String equipStatusTemp = this.getEquipStatus();
                    if (equipStatusTemp.equals("Idle")) {
                        return "0";
                    }
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
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {

        //todo 从共享盘取来EXPOSE.JOB用来解析
        String[] recipeNames = recipeName.split("--");
        List<RecipePara> recipeParas = ScreenRecipeUtil.transferFromDB(ScreenRecipeUtil.transferFromFile("//" + recipeServerPath + "//"
                + recipeNames[0] + "//Img//" + recipeNames[1] + "//EXPOSE.JOB"), deviceType);
        //todo 将文件从共享盘压缩，转到ftp
        TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");
        try {
            ZipUtil.zipFileBy7Z("//" + recipeServerPath + "//" + recipeNames[0], GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".7z");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", setRecipe(recipeName));
        resultMap.put("recipeParaList", recipeParas);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {

        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //todo 将文件从ftp转到共享盘
        String partNo = recipe.getRecipeName().split("--")[0];
        String pnlName = recipe.getRecipeName().split("--")[1];
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);

        if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
            if (FtpUtil.downloadFile("//" + recipeServerPath + "//" + recipe.getRecipeName() + ".7z", ftpPathTmp + recipe.getRecipeName() + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                //todo 下载之后再解压
                try {
                    ZipUtil.unzipBy7Z(recipe.getRecipeName() + ".7z", "//" + recipeServerPath + "//", "//" + recipeServerPath + "//");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {

            if (FtpUtil.downloadFile("//" + recipeServerPath + "//" + recipe.getRecipeName() + ".7z", ftpPathTmp + recipe.getRecipeName() + ".7z", ftpip, ftpPort, ftpUser, ftpPwd)) {
                //todo 下载之后再解压
                try {
                    ZipUtil.unzipBy7Z(recipe.getRecipeName() + ".7z", "//" + recipeServerPath + "//", "//" + recipeServerPath + "//");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        return "0";
    }

    @Override
    public String deleteRecipe(String recipeName) {
        //todo 将文件从共享盘转删除
        FileUtil.delAllFile("//" + recipeServerPath + "//" + recipeName.split("--")[0]);
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                String screen = iSecsHost.executeCommand("curscreen").get(0);
                if (!screen.equals("recipe")) {
                    iSecsHost.executeCommand("playback gotoRcp.txt");
                }
                screen = iSecsHost.executeCommand("curscreen").get(0);
                if (!screen.equals("recipe")) {
                    return "选中失败";
                }
                iSecsHost.executeCommand("playback add.txt");
                List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                //todo 调用接口获取到批次数量
                String lotNum = "123";
                for (int i = 0; i < lotNum.length(); i++) {
                    iSecsHost.executeCommand("playback sel" + lotNum.charAt(i) + ".txt");
                }
                iSecsHost.executeCommand("playback selok.txt");
                iSecsHost.executeCommand("playback addjob.txt");
                return "0";
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                return "选中失败";
            }
        }
    }

    @Override
    public Map getEquipMonitorPara() {
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        File file = new File("//" + recipeServerPath + "//");
        String[] filenames = file.list();
        File[] files = file.listFiles();
        List recipeName = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            File fileTemp = new File(files[i].getPath() + "//Img");
            if (fileTemp.exists() && fileTemp.isDirectory()) {
                String[] filenamesTemp = fileTemp.list();
                for (int j = 0; j < filenamesTemp.length; j++) {
                    recipeName.add(files[i].getName() + "--" + filenamesTemp[j]);
                }

            }
        }
        Map map = new HashMap();
        map.put("eppd", recipeName);
        map.put("EPPD", recipeName);
        return map;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String screen = this.iSecsHost.executeCommand("curscreen").get(0);
            if (screen.equals("main")) {
                List<String> hrunColors = this.iSecsHost.executeCommand("read equipstatus");
                for (String startColorTemp : hrunColors) {
                    if (startColorTemp.equals("idle")) {
                        equipStatus = "Idle";
                    }
                    if (startColorTemp.equals("run")) {
                        equipStatus = "Run";
                    }
                }
            } else {
                equipStatus = "Idle";
            }
        }
        return equipStatus;
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + ".7z", remoteRcpPath, recipeName + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + " 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

}
