package cn.tzauto.octopus.isecsLayer.equipImpl.screen;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.common.ws.AvaryAxisUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.screen.ScreenRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import javax.xml.rpc.ServiceException;
import java.io.*;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.time.LocalDateTime;

import java.util.*;

/**
 * Created by luosy on 2019/5/16.
 */
public class ScreenHost extends EquipModel {

    private static Logger logger = Logger.getLogger(ScreenHost.class.getName());

    private String tableNum = "SFCZ4_ZD_DIExposure";
    private String power = ""; //曝光能量
    private String lotStartTime = ""; //开始时间
    private boolean addLimit = false; //追加限制，只有做完一批才能追加，true 限制开启，正在做，无法追加    false 限制关闭，可以追加
    private Map<String, List<String>> zsz;
    private static List<String[]> inkInfo; //油墨型号，及能量格范围

    static {
        readText();
    }

    {
        createMap();

    }
    //加载油墨型号，及能量格范围
    private static void readText() {
        String textPath = GlobalConstants.getProperty("INK_INFO_FILE_PATH");
        inkInfo = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(textPath), "UTF-8"));
            String tmpString = "";
            while ((tmpString = br.readLine()) != null) {
                String[] arr = tmpString.split(";");
                if (arr.length != 4) {
                    logger.error("油墨型号，及能量格范围配置文件格式错误,文件路径："+textPath);
                    AvaryAxisUtil.main.stop();
                }
                inkInfo.add(arr);
            }
            logger.info("加载的油墨信息为：" + inkInfo);
        } catch (IOException e) {
            logger.error("油墨型号，及能量格范围配置文件格式错误,文件路径："+textPath);
            AvaryAxisUtil.main.stop();
        }
    }

    private void createMap() {
        zsz = new HashMap<>();
        zsz.put("ITEM3", new ArrayList<String>());
        zsz.put("ITEM4", new ArrayList<String>());
        zsz.put("ITEM5", new ArrayList<String>());
        zsz.put("ITEM6", new ArrayList<String>());
    }

    private String recipeServerPath = GlobalConstants.stage.hostManager.getDeviceInfo(deviceCode, deviceCode).getRemarks();
    private String scsl = "";

    public ScreenHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }


    public boolean uploadData() throws RemoteException, ServiceException, MalformedURLException {
        addLimit = false; //解除追加限制
        if ("0".equals(GlobalConstants.getProperty("DATA_UPLOAD"))) {
            createMap();//清空该批次涨缩值
            return true;
        }
        int start = 80000;
        int end = 203000;
        LocalDateTime now = LocalDateTime.now();

        String classInfo = "0";
        int nowTime = Integer.parseInt(now.format(AvaryAxisUtil.dtf3));
        if (start > nowTime || end < nowTime) {
            classInfo = "1";
        }

        String result1 = AvaryAxisUtil.tableQuery(tableNum, deviceCode, classInfo);
        if (result1 == null) {
            String result2 = AvaryAxisUtil.getOrderNum(classInfo);
            if (result2 == null) {
                logger.error("报表数据上传中，无法获取到生產單號");
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，无法获取到生產單號");
                return false;
            }
            result1 = result2;
            String result3 = AvaryAxisUtil.insertMasterTable(result2, "1", deviceCode, tableNum, classInfo, "001", now.format(AvaryAxisUtil.dtf2), "eapsystem");  //system临时代替，  創建工號
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
         MacState	設備狀態          未传入参数以----结尾
         StartTime	開始時間
         EndTime	完成時間
         Lotnum	批號
         Layer	層別
         MainSerial	主途程序
         Partnum	料號
         WorkNo	工令
         SfcLayer	SFC層別          与层别一样
         LayerName	層別名稱
         Serial	途程序
         IsMain	是否主件     ----
         OrderId	第幾次過站
         Qty	生產數量
         Item1	曝光能量
         Item2	PE
         Item3	漲縮值X（左）
         Item4	漲縮值Y（左）
         Item5	漲縮值X（右）
         Item6	漲縮值Y（右）
         Isfirst	是否初件
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
        String item3 = getMun("ITEM3");
        String item4 = getMun("ITEM4");
        String item5 = getMun("ITEM5");
        String item6 = getMun("ITEM6");

        String result = AvaryAxisUtil.insertTable(result1, "正常", lotStartTime, now.format(AvaryAxisUtil.dtf2), lotId, map4.get("Layer"), map5.get("MainSerial"),
                map5.get("PartNum"), map5.get("WorkNo"), map4.get("Layer"), map5.get("LayerName"), map5.get("Serial"), map5.get("OrderId"), scsl, power,
                item3, item4, item5, item6, isFirstPro ? "1" : "0"
        );
        createMap();//清空该批次涨缩值
//        String result = AvaryAxisUtil.insertTable();
        if ("".equals(result)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传成功，明細表數據上传成功");
            return true;
        }
        logger.error("报表数据上传中，明細表數據插入失败：" + result);
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "报表数据上传中，明細表數據插入失败：" + result);
        return false;
    }

    private String getMun(String item) {
        List<String> itemList = zsz.get(item);
        logger.info("涨缩值为：" + itemList);
        if (itemList.size() == 0) {
            return "0";
        }
        double temp = 0;
        for (String s : itemList) {
            temp = Double.parseDouble(s) + temp;
        }
        temp = temp / itemList.size();
        item = new Double(temp).toString();
        if (item.contains(".")) {
            item = item.substring(0, item.indexOf(".") + 4);
        }
        return item;
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

        // 从共享盘取来EXPOSE.JOB用来解析
        String[] recipeNames = recipeName.split("/");
        List<RecipePara> recipeParas = ScreenRecipeUtil.transferFromDB(ScreenRecipeUtil.transferFromFile("//" + recipeServerPath + "//"
                + recipeNames[0] + "//Img//" + recipeNames[1] + "//EXPOSE.JOB"), deviceType);
        // 将文件从共享盘压缩，转到ftp
        TransferUtil.setPPBody(recipeName, 0, GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + "temp/TMP");
//        try {
////            ZipUtil.zipFileBy7Z("//" + recipeServerPath + "//" + recipeNames[0], GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName.replaceAll("/", "@") + "temp/" + recipeName.replaceAll("/", "@") + ".7z");
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        Map resultMap = new HashMap();
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", setRecipe(recipeName));
        resultMap.put("recipeParaList", recipeParas);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        if (addLimit) {
            return "当前批次结束后才可追加新的批次！";
        }
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        // 将文件从ftp转到共享盘
        String partNo = recipe.getRecipeName().split("/")[0];
        String pnlName = recipe.getRecipeName().split("/")[1];
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        String ftpPathTmp = ftpPath.substring(0, ftpPath.lastIndexOf("/") + 1);

        if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
            if (FtpUtil.downloadFile("//" + recipeServerPath + "//" + recipe.getRecipeName() + ".7z", ftpPathTmp + recipe.getRecipeName().replaceAll("/", "@") + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
                // 下载之后再解压
                try {
                    ZipUtil.unzipBy7Z(recipe.getRecipeName() + ".7z", "//" + recipeServerPath + "//", "//" + recipeServerPath + "//");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {

            if (FtpUtil.downloadFile("//" + recipeServerPath + "//" + recipe.getRecipeName() + ".7z", ftpPathTmp + recipe.getRecipeName().replaceAll("/", "@") + ".7z", ftpip, ftpPort, ftpUser, ftpPwd)) {
                // 下载之后再解压
                try {
                    ZipUtil.unzipBy7Z(recipe.getRecipeName() + ".7z", "//" + recipeServerPath + "//", "//" + recipeServerPath + "//");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        addLimit = true;
        return "0";
    }

    @Override
    public String deleteRecipe(String recipeName) {
        // 将文件从共享盘转删除
        if (recipeName.substring(0, 1).equals("F")) {
            FileUtil.deleteAllFilesOfDir(new File("//" + recipeServerPath + "//" + recipeName.split("/")[0]));
        }
        List<String> recipeNameList = (List<String>) getEquipRecipeList().get("eppd");
        for (String recipeNameTemp : recipeNameList) {
            if (recipeNameTemp.equals(recipeName)) {
                continue;
            }
            if (recipeName.substring(0, 1).equals("F")) {
                FileUtil.deleteAllFilesOfDir(new File("//" + recipeServerPath + "//" + recipeNameTemp.split("/")[0]));
            }
        }
        return "0";
    }

    @Override
    public String selectRecipe(String recipeName) {
        if (addLimit) {
            return "当前批次结束后才可追加新的批次！";
        }
        if (!checkRecipeExist(recipeName)) {
            return "Recipe file not exist!";
        }
        String[] recipeNames = recipeName.split("/");
//        File file = new File("//" + this.recipeServerPath + "//" + recipeNames[0] + "//Img//");
//        if (file.listFiles().length > 1) {
//            File[] files = file.listFiles();
//            for (File fileTemp : files) {
//                if (!fileTemp.getName().equals(recipeNames[1])) {
//                    FileUtil.deleteAllFilesOfDir(fileTemp);
//                }
//            }
//        }
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
                //点击选择料号
                iSecsHost.executeCommand("playback openpartno.txt");
                Thread.sleep(500);
                iSecsHost.executeCommand("dialog \"Job Select\" listbox " + recipeNames[0]);
                iSecsHost.executeCommand("playback selpartno.txt");
                //点击选择面
                iSecsHost.executeCommand("playback openpnl.txt");
                Thread.sleep(500);
                iSecsHost.executeCommand("dialog \"Front Data Select\" listbox " + recipeNames[1]);
                iSecsHost.executeCommand("playback selpnl.txt");
//                List<String> result = iSecsHost.executeCommand("playback selrecipe.txt");
                iSecsHost.executeCommand("playback writenumber.txt");
                Thread.sleep(500);
                // 调用接口获取到批次数量
                lotCount = AvaryAxisUtil.getLotQty(lotId);
                for (int i = 0; i < lotCount.length(); i++) {
                    iSecsHost.executeCommand("playback sel" + lotCount.charAt(i) + ".txt");
                }
                iSecsHost.executeCommand("playback selok.txt");
                iSecsHost.executeCommand("playback addjob.txt");
                iSecsHost.executeCommand("playback gotoMain.txt");
                lotStartTime = LocalDateTime.now().format(AvaryAxisUtil.dtf2);
                addLimit = true;
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

    private boolean checkRecipeExist(String recipeName) {
        List<String> eppds = (List<String>) getEquipRecipeList().get("eppd");
        for (String eppd : eppds) {
            if (eppd.equals(recipeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map getEquipRecipeList() {
        File file = new File("//" + recipeServerPath + "//");
        String[] filenames = file.list();
        File[] files = file.listFiles();
        List recipeName = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("F")) {
                File fileTemp = new File(files[i].getPath() + "//Img");
                if (fileTemp.exists() && fileTemp.isDirectory()) {
                    String[] filenamesTemp = fileTemp.list();
                    for (int j = 0; j < filenamesTemp.length; j++) {

                        recipeName.add(files[i].getName() + "/" + filenamesTemp[j]);

                    }

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
                    if (startColorTemp.contains("idle") || startColorTemp.contains("Error") || "".equals(startColorTemp.trim()) || "IDChange".equals(startColorTemp)) {
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
        String path = GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + "temp/TMP";
        TransferUtil.setPPBody(recipe.getRecipeName(), 0, path);
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        if (!FtpUtil.uploadFile(path, remoteRcpPath, recipeName.replaceAll("/", "@") + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
//        if (!FtpUtil.uploadFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName.replaceAll("/", "@") + "temp/" + recipeName.replaceAll("/", "@") + ".7z", remoteRcpPath, recipeName.replaceAll("/", "@") + ".7z_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + " 工控路径:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        this.deleteTempFile(recipeName);
        return true;
    }

    protected boolean specialCheck() {
        Map<String, String> resultMap = iSecsHost.readAllParaByScreen("main");
        String gzxx = resultMap.get("gzxx");
        String power = resultMap.get("bgl");
        if (!AvaryAxisUtil.get21Exposure(deviceCode, gzxx, power)) {
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "防焊曝光21节验证通过!!");
        return true;
    }

    public Map getSpecificData(Map<String, String> dataIdMap) {


        // 这里需要获取xy的涨缩值 zx zy  yx yy  单片是 x y
        Map<String, String> resultMap = iSecsHost.readAllParaByScreen("main");
        String temp = resultMap.get("scsl");
        if (temp.contains("/")) {
            scsl = temp.split("/")[0];
            if (lotCount.equals(scsl)) {
                try {
                    uploadData();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
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
        String item3 = resultMap.get("zx").equals("") ? resultMap.get("x") : resultMap.get("zx");
        parms2.add(item3);

        String item4 = resultMap.get("zy").equals("") ? resultMap.get("y") : resultMap.get("zy");
        parms2.add(item4);
        String item5 = resultMap.get("yx");
        parms2.add(item5);
        String item6 = resultMap.get("yy");
        parms2.add(item6);

        zsz.get("ITEM3").add(item3);
        zsz.get("ITEM4").add(item4);
        zsz.get("ITEM5").add(item5);
        zsz.get("ITEM6").add(item6);
        for (String s : parms2) {
            if (s == null || s.equals("")) {
                return null;
            }
        }

        String result = AvaryAxisUtil.uploadMessageEveryPNL(deviceName, parms1, parms2);
        logger.info("发送涨缩值数据到MES" + parms2 + ",已经生产数量：" + scsl);
        return exposure;
    }

    @Override
    public String organizeRecipe(String partNo, String lotNo) {
        String layer = AvaryAxisUtil.getLayer(lotNo);
        String mainserial = null;
        try {
            mainserial = String.valueOf(AvaryAxisUtil.getParmByLotNumAndLayer(lotNo, "SFCZ4_ZD_DIExposure", layer).get("MainSerial"));
        } catch (Exception e) {
            logger.error("根據 批號,層別 帶出 料號,在製層,途程序,主途程序,制程,主配件,層別名稱,第幾次過站,工令,BOM資料发生异常", e);
        }

        String bom = null;
        try {
            bom = AvaryAxisUtil.getBom(deviceCode, partNo, mainserial);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bom == null) {
            return "Can not find any bom info by partNum:" + partNo;
        }
        String recipeName = partNo.substring(0, 7) + "/" + bom + "-2PNL";
        if (!checkRecipeExist(recipeName)) {
            return "Can not find recipe at device " + deviceName;
        }
        return recipeName;
    }

    public static Map<String, String[]> create21(Map<String, String> itemMap) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < inkInfo.size(); i++) {
            String[] arr = inkInfo.get(i);
            map.put(arr[0], createArray(itemMap.get(arr[1]), itemMap.get(arr[2]), arr[3]));
        }

//        map.put("300-22F", createArray(itemMap.get("ITEM1"), itemMap.get("ITEM2")));
//        map.put("300-22D", createArray(itemMap.get("ITEM3"), itemMap.get("ITEM4")));
//        map.put("9000FLX81", createArray(itemMap.get("ITEM5"), itemMap.get("ITEM6")));
//        map.put("9000FLX501", createArray(itemMap.get("ITEM7"), itemMap.get("ITEM8")));
//        map.put("9000FLX505", createArray(itemMap.get("ITEM9"), itemMap.get("ITEM10")));
//        map.put("9000FLX505WB", createArray(itemMap.get("ITEM11"), itemMap.get("ITEM12")));
//        map.put("LCL1000452", createArray(itemMap.get("ITEM13"), itemMap.get("ITEM14")));
//        map.put("LCL1000423", createArray(itemMap.get("ITEM15"), itemMap.get("ITEM16")));
//        map.put("LCL1000421", createArray(itemMap.get("ITEM17"), itemMap.get("ITEM18")));
//        map.put("LCL1000410", createArray(itemMap.get("ITEM19"), itemMap.get("ITEM20")));
        return map;
    }

    private static String[] createArray(String item0, String item1, String range) {
        String[] arr = new String[3];
        arr[0] = item0;
        arr[1] = item1;
        arr[2] = range;
        return arr;
    }

    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        Attach attach = new Attach();
        attach.setId(UUID.randomUUID().toString());
        attach.setRecipeRowId(recipe.getId());
        attach.setAttachPath(ftpRecipePath);
        attach.setAttachName(recipeName + ".7z_V" + recipe.getVersionNo());
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

    protected List<RecipePara> checkRcpPara(String recipeId, String deviceCode, List<RecipePara> equipRecipeParas, String masterCompareType) {
//        SqlSession sqlsession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlsession);
//        List<RecipePara> diffRecipeParas = recipeService.checkRcpPara(recipeId, deviceCode, equipRecipeParas, "");
//        sqlsession.close();
        String MainSerial = "";
        try {
            MainSerial = String.valueOf(AvaryAxisUtil.getParmByLotNumAndLayer(lotId, tableNum, AvaryAxisUtil.getLayer(lotId)).get("MainSerial"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map<String, String> paraMap = AvaryAxisUtil.getRecipeParaByPartNum(deviceName, partNo + MainSerial);
        Map<String, String> equipParaMap = getEquipMonitorPara();
        if (paraMap == null || paraMap.isEmpty()) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "从MES获取生产条件信息失败!!料号:" + partNo + MainSerial);
            return null;
        }
        if (equipParaMap.get("bgl").equals(paraMap.get("1"))) {
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "曝光能量检查不通过,MES取值:" + paraMap.get("1") + "设备当前:" + equipParaMap.get("bgl"));
        }
        if (equipParaMap.get("jbhd").equals(paraMap.get("2"))) {
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "板厚检查不通过,MES取值:" + paraMap.get("2") + "设备当前:" + equipParaMap.get("jbhd"));
        }
        return null;
    }

    public boolean selectProgram(String recipName) {
        int num = 5;//页面显示最大的个数  以0开始，5就是最大6个
        //获取所有的recipe文件夹
        List<String> list = Arrays.asList(new String[]{"FSWUPTYI", "1SWUPTYI", "7SWUPTYI", "0SWUPTYI", "JSWUPTYI", "jSWUPTYI", "sSWUPTYI", "wSWUPTYI", "ZSWUPTYI"});
        Collections.sort(list);
        int indexMax = list.size() - 1;
        int index = list.indexOf(recipName);
        if (index < 0) {
            logger.info("没有可选择的recipe");
            return false;
        }
        logger.info("找到recipe：" + recipName);
        int selected = 0;//选中位置所在num中的坐标
        String content = "";//选中的内容
        if (num >= index) {
            //1.直接选中该位置
            selected = index;
            iSecsHost.executeCommand("playback recipe" + selected + ".txt");
            selected = index;
            //2.获取改位置内容进行比较
            content = "";
            //3.比较无误，直接确定
            if (recipName.equals(content)) {
                return true;
            } else if (recipName.compareTo(content) > 0) { //比较有误，继续选择
                int length = indexMax - index;
                for (int i = 0; i <= length; i++) {
                    index++;
                    if (index > indexMax) {
                        //没有该recipe
                        return false;
                    }
                    if (index < num) {
                        selected++;
                    }
                    //往下一个,
                    iSecsHost.executeCommand("playback nextRecipe.txt");
                    content = "";//获取下一个的recipe名字
                    if (recipName.equals(content)) {
                        return true;
                    }
                }
            } else {
                int length = index;
                for (int i = 0; i < length; i++) {
                    index--;
                    if (index < 0) {
                        //没有该recipe
                        return false;
                    }
                    selected--;
                    //往上一个
                    iSecsHost.executeCommand("playback preRecipe.txt");
                    content = "";//获取上一个的recipe名字
                    if (recipName.equals(content)) {
                        return true;
                    }
                }
            }
        } else {
            selected = num;
            //先选中最下面一个
            iSecsHost.executeCommand("playback recipe" + selected + ".txt");
            int length = index - num;
            for (int i = 0; i < length; i++) {
                iSecsHost.executeCommand("playback nextRecipe.txt");
            }
            //获取recipe名字
            content = "";
            if (recipName.equals(content)) {
                return true;
            } else if (recipName.compareTo(content) > 0) {
                int len = indexMax - index;
                for (int i = 0; i <= len; i++) {
                    index++;
                    if (index > indexMax) {
                        //没有该recipe
                        return false;
                    }
                    //往下一个,
                    iSecsHost.executeCommand("playback nextRecipe.txt");
                    content = "";//获取下一个的recipe名字
                    if (recipName.equals(content)) {
                        return true;
                    }
                }
            } else {
                int len = index;
                for (int i = 0; i < len; i++) {
                    index--;
                    if (index < 0) {
                        //没有该recipe
                        return false;
                    }
                    if (selected > 0) {
                        selected--;
                    }
                    //往上一个
                    iSecsHost.executeCommand("playback preRecipe.txt");
                    content = "";//获取上一个的recipe名字
                    if (recipName.equals(content)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
