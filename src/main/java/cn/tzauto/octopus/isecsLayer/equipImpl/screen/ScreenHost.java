package cn.tzauto.octopus.isecsLayer.equipImpl.screen;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.common.ws.AvaryAxisUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.screen.ScreenRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import javax.xml.rpc.ServiceException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luosy on 2019/5/16.
 */
public class ScreenHost extends EquipModel {

    private static Logger logger = Logger.getLogger(ScreenHost.class.getName());

    private String tableNum = "SFCZ4_ZD_DIExposure";
    private String power = ""; //曝光能量

    private String recipeServerPath = GlobalConstants.stage.hostManager.getDeviceInfo(deviceCode, deviceCode).getRemarks();
    private String lotCount = "";
    private String scsl = "";

    public ScreenHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            if (lotCount.equals(scsl)) {
                try {
                    this.uploadData();
                } catch (RemoteException e) {
                    logger.error("上传表单发生异常", e);

                } catch (ServiceException e) {
                    logger.error("上传表单发生异常", e);

                } catch (MalformedURLException e) {

                    logger.error("上传表单发生异常", e);
                }

            }
        }
    }

    public boolean uploadData() throws RemoteException, ServiceException, MalformedURLException {


        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime now = LocalDateTime.now();
        String result1 = AvaryAxisUtil.tableQuery(tableNum, deviceCode, "0"); //夜班，白班，待确认
        if (result1 == null) {
            String result2 = AvaryAxisUtil.getOrderNum("0");
            if (result2 == null) {
                logger.error("报表数据上传中，无法获取到生產單號");
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，无法获取到生產單號");
                return false;
            }
            result1 = result2;
            String result3 = AvaryAxisUtil.insertMasterTable(result2, "status", deviceCode, tableNum, "0", "001", now.format(dtf2), "system");  //system临时代替，  創建工號
            if (!"".equals(result3)) {
                logger.error("报表数据上传中，插入主表數據失败" + result3);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，插入主表數據失败");
                return false;
            }

        }
        Map<String, String> map4 = AvaryAxisUtil.getParmByLotNum(lotId);
        if (map4.size() == 0) {
            logger.error("报表数据上传中，批號獲料號,層別,數量 为空");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，批號獲料號,層別,數量 为空");
            return false;
        }
        Map<String, String> map5 = AvaryAxisUtil.getParmByLotNumAndLayer(lotId, tableNum, map4.get("Layer"));
        if (map5.size() == 0) {
            logger.error("报表数据上传中，根據 批號,層別 帶出 料號,在製層,途程序,主途程序,制程,主配件,層別名稱,第幾次過站,工令,BOM資料 失败");
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，根據 批號,層別 帶出 料號,在製層,途程序,主途程序,制程,主配件,層別名稱,第幾次過站,工令,BOM資料 失败");
            //報錯:獲取途程信息失敗
            return false;
        }

        /**
         * PaperNo	表單號
         MacState	設備狀態   ----   未传入参数以----结尾
         StartTime	開始時間   -----
         EndTime	完成時間
         Lotnum	批號
         Layer	層別
         MainSerial	主途程序
         Partnum	料號
         WorkNo	工令
         SfcLayer	SFC層別    -----
         LayerName	層別名稱
         Serial	途程序
         IsMain	是否主件     ----
         OrderId	第幾次過站
         Qty	生產數量
         Item1	曝光能量
         Item2	PE
         Item3	漲縮值X（左）  ----
         Item4	漲縮值Y（左）   -----
         Item5	漲縮值X（右）----
         Item6	漲縮值Y（右）----
         Isfirst	是否初件----
         FirstCheck	初件判定---
         FirstCheckStatus	初件審核-----
         FirstCheckEmpid	初件審核人---
         FirstCheckTime	初件審核時間----
         QCCheck	品保審核-----
         qcchecktime	品保審核時間----
         CreateEmpid	創建人---
         CreateTime	創建時間----
         ModifyEmpid	最後修改人員---
         ModifyTime	最後修改時間----
         userid	作業員------
         */
        String result = AvaryAxisUtil.insertTable(result1, "开始时间", now.format(AvaryAxisUtil.dtf), lotId, map4.get("Layer"), map5.get("MainSerial"),
                map5.get("PartNum"), map5.get("WorkNo"), map5.get("LayerName"), map5.get("Serial"), map5.get("OrderId"), scsl, power, map5.get("PE")
        );
        if ("".equals(result)) {
            return true;
        }
        logger.error("报表数据上传中，明細表數據插入失败：" + result);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，明細表數據插入失败：" + result);
        return false;
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
//                            ppExecName = recipeNameTemp.substring(recipeNameTemp.lastIndexOf("/") + 1);
                            ppExecName = recipeNameTemp;
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
        String[] recipeNames = recipeName.split("/");
        List<RecipePara> recipeParas = ScreenRecipeUtil.transferFromDB(ScreenRecipeUtil.transferFromFile("//" + recipeServerPath + "//"
                + recipeNames[0] + "//Img//" + recipeNames[1] + "//EXPOSE.JOB"), deviceType);
        //todo 将文件从共享盘压缩，转到ftp
//        TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/TMP");
        try {
            ZipUtil.zipFileBy7Z("//" + recipeServerPath + "//" + recipeNames[0], GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName.replaceAll("/", "@") + "temp/" + recipeName.replaceAll("/", "@") + ".7z");
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
        String partNo = recipe.getRecipeName().split("/")[0];
        String pnlName = recipe.getRecipeName().split("/")[1];
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);

        if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
            if (FtpUtil.downloadFile("//" + recipeServerPath + "//" + recipe.getRecipeName() + ".7z", ftpPathTmp + recipe.getRecipeName().replaceAll("/", "@") + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
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
        FileUtil.delAllFile("//" + recipeServerPath + "//" + recipeName.split("/")[0]);
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {
        String[] recipeNames = recipeName.split("/");
        File file = new File("//" + this.recipeServerPath + "//" + recipeNames[0] + "//Img//");
        if (file.listFiles().length > 1) {
            File[] files = file.listFiles();
            for (File fileTemp : files) {
                if (fileTemp.getName().equals(recipeNames[1])) {

                }
            }
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                String screen = iSecsHost.executeCommand("curscreen").get(0);
                if (!screen.equals("recipe")) {
                    iSecsHost.executeCommand("playback gotoRcp.txt");
                }
                Thread.sleep(500);
                screen = iSecsHost.executeCommand("curscreen").get(0);
                if (!screen.equals("recipe")) {
                    return "选中失败";
                }
                iSecsHost.executeCommand("playback add.txt");
                List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                iSecsHost.executeCommand("playback writenumber.txt");
                Thread.sleep(500);
                //todo 调用接口获取到批次数量
                lotCount = AvaryAxisUtil.getLotQty(lotId);
                for (int i = 0; i < lotCount.length(); i++) {
                    iSecsHost.executeCommand("playback sel" + lotCount.charAt(i) + ".txt");
                }
                iSecsHost.executeCommand("playback selok.txt");
                iSecsHost.executeCommand("playback addjob.txt");
                iSecsHost.executeCommand("playback gotoMain.txt");
                return "0";
            } catch (Exception e) {
                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
                return "选中失败";
            }
        }
    }

    @Override
    public Map getEquipMonitorPara() {
        return this.iSecsHost.readAllParaByScreen("main");
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
                    recipeName.add(files[i].getName() + "/" + filenamesTemp[j]);
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
                    if (startColorTemp.contains("idle") || startColorTemp.contains("Error")) {
                        equipStatus = "Idle";
                    }
                    if (startColorTemp.contains("run")) {
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
//        iSecsHost.readAllParaByScreen("main");
        return null;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName.replaceAll("/", "@") + "temp/" + recipeName.replaceAll("/", "@") + ".7z", remoteRcpPath, recipeName.replaceAll("/", "@") + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + " 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    protected boolean specialCheck() {


        return true;
    }

    public Map getSpecificData(Map<String, String> dataIdMap) {


        //todo 这里需要获取xy的涨缩值 zx zy  yx yy  单片是 x y
        Map<String, String> resultMap = iSecsHost.readAllParaByScreen("main");
        String temp = resultMap.get("scsl");
        if (temp.contains("/")) {
            scsl = temp.split("/")[0];
        }
        power = resultMap.get("bgl");
        Map exposure = new HashMap();
        exposure.put("zx", resultMap.get("zx"));
        exposure.put("zy", resultMap.get("zy"));
        exposure.put("yx", resultMap.get("yx"));
        exposure.put("yy", resultMap.get("yx"));
        exposure.put("x", resultMap.get("x"));
        exposure.put("y", resultMap.get("y"));

        List<String> parms1 = new ArrayList<>();
        parms1.add("Lotnum");
        parms1.add("Partnum");
        parms1.add("Item3");
        parms1.add("Item4");
        parms1.add("Item5");
        parms1.add("Item6");

        List<String> parms2 = new ArrayList<>();
        parms2.add(lotId);
        parms2.add(partNo);
        parms2.add(resultMap.get("zx").equals("") ? resultMap.get("x") : resultMap.get("zx"));
        parms2.add(resultMap.get("zy").equals("") ? resultMap.get("y") : resultMap.get("zy"));
        parms2.add(resultMap.get("yx"));
        parms2.add(resultMap.get("yx"));

        AvaryAxisUtil.uploadMessageEveryPNL(deviceName, parms1, parms2);
        logger.info("发送涨缩值数据到MES");
        return exposure;
    }

    @Override
    public String organizeRecipe(String partNo) {

        String bom = "";
        String recipeName = partNo.substring(0, 7) + "/" + bom + partNo + "-2PNL";


        return recipeName;
    }
}
