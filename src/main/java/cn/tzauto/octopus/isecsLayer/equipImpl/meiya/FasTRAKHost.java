package cn.tzauto.octopus.isecsLayer.equipImpl.meiya;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.meiya.FasTRAKResolver;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.util.*;

/**
 * Created by wj_co
 */
public class FasTRAKHost extends EquipModel {

    private static Logger logger = Logger.getLogger(FasTRAKHost.class.getName());
    private long getRecipeAndStatusTime = new Date().getTime();
    private boolean initFlag = false;

    public FasTRAKHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }


    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> recipeResult = null;
                List<String> resultFT = iSecsHost.executeCommand("read FTRecipeName");
                List<String> resultMH = iSecsHost.executeCommand("read MHRecipeName");
                if(resultFT!=null && !resultFT.get(0).contains("Error")){
                    recipeResult=resultFT;
                }else if(resultMH!=null && !resultMH.get(0).contains("Error")){
                    recipeResult=resultMH;
                }
                    ppExecName = recipeResult.get(0).trim();
            } catch (Exception e) {
                logger.error("Get equip ExecName error:" + e.getMessage(), e);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    /**
     * 跳到FlexTRAK页面
     *时间久了，登录失效，按钮变灰色
     * @return
     */
    public Boolean ToFlexTRAKMain() {
        Boolean mainResult = true;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> strings = iSecsHost.executeCommand("read MHOverview");
            String result = strings.get(0).trim();
            if (!"MH Overview".equals(result)) {//FT界面
                mainResult = true;
            } else {//在 MH界面
                mainResult = false;
            }
        }
        return mainResult;
    }

    /**
     * 跳到MH页面
     *若时间长，FT页面FlexTRAK overView按钮是否显示灰色？
     * @return
     */
    public Boolean ToMHMain() {
        Boolean mainResult = true;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> strings = iSecsHost.executeCommand("read FlexTRAKOverview");
            String result = strings.get(0).trim();
            if (!"FlexTRAK Overview".equals(result)) {
                mainResult = true;
            } else {//在 FlexTRAK界面
                mainResult = false;
            }
        }
        return mainResult;
    }

    @Override
    public String startEquip() {
        String startResult = "Start failed";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand("playback start.txt");
                Thread.sleep(1000);
                if ("Run".equals(getEquipStatus())) {
                    startResult = "0";
                }
            } catch (Exception e) {
                logger.error("start equip error:" + e.getMessage(), e);
                startResult = "Start failed";
            }
        }
        return startResult;
    }

    @Override
    public String pauseEquip() {
        String pauseResult = "Pause failed";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand("playback pause.txt");
                if (resultList != null && !resultList.isEmpty()) {
                    Thread.sleep(1000);
                    pauseResult = "0";
                }
            } catch (Exception e) {
                logger.error("pause equip error:" + e.getMessage(), e);
                pauseResult = "Pause failed";
            }
        }
        return pauseResult;
    }

    @Override
    public String stopEquip() {
        String stopResult = "Stop failed";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand("playback stop.txt");
                Thread.sleep(1000);
                if ("Idle".equals(getEquipStatus())) {
                    stopResult = "0";
                }
            } catch (Exception e) {
                logger.error("stop equip error:" + e.getMessage(), e);
                stopResult = "Stop failed";
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
        Map<String, Object> map = new HashMap<>();
        String localFtpIp = GlobalConstants.clientInfo.getClientIp();
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        Recipe recipe = setRecipe(recipeName);
        String clientFtpRecipeRelativePath = getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String cmd = "ftp " + localFtpIp + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientFtpRecipeRelativePath + "\" \"put $" + recipeName + ".flextrak.recipe.xml $\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand(cmd);
                for (String result : list) {
                    if (result != null && result.contains("success")) {
                        //解析recipe
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            System.out.println(recipeName);
                            String filePath = equipRecipePath + "\\" + recipeName + ".flextrak.recipe.xml";
                            Map<String, Object> paraMap = FasTRAKResolver.transferFromXml(filePath);
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = FasTRAKResolver.transferFromDB(paraMap, deviceType);
                            } else {
                                logger.error("解析recipe时出错,recipe文件不存在!");
                            }

                        } catch (Exception e) {
                            logger.error("解析recipe时出错!", e);
                            e.printStackTrace();
                        }
                        map.put("recipe", setRecipe(recipeName));
                        map.put("deviceCode", deviceCode);
                        map.put("recipeParaList", recipeParaList);
                        map.put("recipeFTPPath", hostUploadFtpRecipePath);
                    }
                    if (result != null && result.contains("error")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("upload recipe failed" + e.getMessage(), e);
            }
        }
        return map;
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + ".flextrak.recipe.xml", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".flextrak.recipe.xml 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        //this.deleteTempFile(recipeName);
        return true;
    }

    /**
     * 下载recipe
     * 1.ftp Server下载至工控
     * 2.工控下载至 机台设备
     * //localFilePath本地文件路径：clientAbsoluteFtpPath +recipeName+".flextrak.recipe.xml"；remoteFilePath：hostDownloadFtpRecipeFilePath
     *
     * @param recipe
     * @return
     */
    @Override
    public String downloadRecipe(Recipe recipe) {
        String downLoadResult = "1";
        String recipeName = recipe.getRecipeName();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpIp = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"get $" + recipeName + ".flextrak.recipe.xml $\"";
        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        //从server下载recipe到工控机
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpIp, ftpPort, ftpUser, ftpPwd)) {
                downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启";
            }
            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                FtpUtil.downloadFile(clientAbsoluteFtpPath + recipeName + ".flextrak.recipe.xml", hostDownloadFtpRecipeFilePath, ftpIp, ftpPort, ftpUser, ftpPwd);
            }
        }
        //从工控机下载至机台
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand(cmd);
                for (String str : result) {
                    if (str.contains("success")) {
                        downLoadResult = "0";
                        break;
                    }
                    if (str.contains("error")) {
                        downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("download recipe failed" + e.getMessage(), e);
                downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
        }
        return downLoadResult;
    }

    public String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    public String getClientFtpRecipeAbsolutePath(String recipeName) {//localRecipePath=>D:/
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File file = new File(filePath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return filePath;
    }

    /**
     * 获取服务端上传FtpRecipe路径
     *
     * @param recipe
     * @return
     */
    private String getHostUploadFtpRecipePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        sqlSession.close();
        return ftpRecipePath;
    }

    /**
     * 获取服务端下载FtpRecipe路径
     * hostDownloadFtpRecipeFilePath：
     * /RECIPE/A6/DieAttach/STMLSSP2000ED/Engineer/STMLSSP2000ED_0001/4 LQFP 80L 12x12/
     *
     * @param recipe
     * @return
     */
    public String getHostDownloadFtpRecipeFilePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String downloadFullFilePath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        sqlSession.close();
        return downloadFullFilePath;
    }

    @Override
    public String deleteRecipe(String recipeName) {
        String delResult = "Delete failed";
        String cmd = "dos $del /q \"" + equipRecipePath + File.separator + recipeName + ".flextrak.recipe.xml \"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand(cmd);
                int i = 0;
                for (String s : list) {
                    String s_V = list.get(i);
                    if (s.contains("done") && s_V.contains("done")) {
                        delResult = "Delete success";
                    }
                    i++;
                }
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                delResult = "Delete failed";
            }
        }
        return delResult;
    }

    public Boolean isLogin() {
        Boolean mainResult = true;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            List<String> strings = iSecsHost.executeCommand("read loggedOut");
            String result = strings.get(0).trim();
            if (!"LoggedOut".equals(result)) {
                mainResult = true;
            } else {//已登陆
                mainResult = false;
            }
        }
        return mainResult;
    }

    /**
     * 第一步：判断是否是在MH Overview界面(read 2个界面的Button)
     * 判断是否登录
     * 第二步：读是否是Recipe Setup；如果是点击Recipe Setup
     * 第三步：goto_recipe
     * 第四步：select rcp1/2/3
     * 第五步：click recipe ok
     * 第六步：click return
     *
     * @param recipeName
     * @return
     */
    @Override
    public String selectRecipe(String recipeName) {
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                if (ToMHMain()) {
                    iSecsHost.executeCommand("playback click_FlexTRAK.txt");
                }
                if (!isLogin()) {
                    iSecsHost.executeCommand("playback click_login.txt");
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    List<String> strings = iSecsHost.executeCommand("read selectRecipe");
                    if ("Recipe Setup".equals(strings.get(0).trim())) {
                        iSecsHost.executeCommand("playback goto_recipe.txt");
                        Thread.sleep(800);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    String rcp_1 = iSecsHost.executeCommand("read r1_sel").get(0);
                    String rcp_2 = iSecsHost.executeCommand("read r2_sel").get(0);
                    String rcp_3 = iSecsHost.executeCommand("read r3_sel").get(0);
                    if (rcp_1.equals(recipeName)) {
                        iSecsHost.executeCommand("playback rcp_1.txt");
                    } else if (rcp_2.equals(recipeName)) {
                        iSecsHost.executeCommand("playback rcp_2.txt");
                    } else if (rcp_3.equals(recipeName)) {
                        iSecsHost.executeCommand("playback rcp_3.txt");
                    } else {
                        logger.info("要选中的recipe不存在");
                        iSecsHost.executeCommand("playback gotomain.txt");
                        return "failed";
                    }
                } catch (Exception e) {
                    logger.info("切换recipe失败" + e);
                    return "failed";
                }

                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    iSecsHost.executeCommand("playback rcp_ok.txt");
                } catch (Exception e) {
                    logger.info("点击recipe失败：" + e);
                    return "failed";
                }
                try {
                    iSecsHost.executeCommand("playback rcp_ok_return.txt");
                } catch (Exception e) {
                    logger.info("点击recipe失败：" + e);
                    return "failed";
                }
                //查询recipe是否有更改成功
                String currentRecipeName = getCurrentRecipeName();
                if (currentRecipeName.equals(recipeName)) {
                    return "0";
                }
            }
            return "failed";
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return "failed";
        }
    }

    @Override
    public Map getEquipRealTimeState() {
        Map map = new HashMap();
        map.put("PPExecName", getCurrentRecipeName());
        map.put("EquipStatus", getEquipStatus());
        map.put("ControlState", controlState);
        return map;
    }

    @Override
    public Map getEquipMonitorPara() {
        return null;
    }

    @Override
    public Map getEquipRecipeList() {
        Map<String, Object> eppd = new HashMap<>();
        try {
            eppd.put("eppd", getRecipeList());
        } catch (Exception e) {
            logger.info("FasTRAK getEquipRecipeList()-获取recipe列表失败" + e);
        }
        return eppd;
    }

    public List<String> getRecipeList() {
        ArrayList<String> recipeList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("dos $dir " + "\"" +
                        this.equipRecipePath + File.separator + "*.flextrak.recipe.xml" + "\" /b$");
                for (String recipeName : results) {
                    if (!"".equals(recipeName) && !"done".equals(recipeName)) {
                        recipeName = recipeName.replace(".flextrak.recipe.xml", "");
                        if (!"".equals(recipeName)) {
                            recipeList.add(recipeName);
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("FasTRAK getRecipeList()-获取recipe列表失败" + e);
            }
        }
        return recipeList;
    }

    /**
     * FT页面：procTime=28 && pressure=21是停止状态；procTime=32 || pressure =9999是准备状态;其余是运行状态
     * MH页面：同上
     * 若期间页面被跳转至MH页面，状态不受影响
     *
     * @return
     */
    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            String procTime=null;
            String pressure=null;

//            String resultMHProcTime = iSecsHost.executeCommand("read MHProcTime").get(0);
//            String resultMHPressure = iSecsHost.executeCommand("read MHPressure").get(0);
//
//            String resultFTPressure = iSecsHost.executeCommand("read FTPressure").get(0);
//            String resultFTProcTime = iSecsHost.executeCommand("read FTProcTime").get(0);
//
//            if(resultMHProcTime!=null && !"FlexTRAK Overview".equals(resultFTProcTime) ){
//                procTime=resultMHProcTime;
//            }else if(resultMHPressure!=null && !"FlexTRAK Overview".equals(resultMHPressure)){
//                pressure=resultMHPressure;
//            }else if(resultFTProcTime!=null && !"FlexTRAK Overview".equals(resultFTProcTime) ){
//                procTime=resultFTProcTime;
//            }else if(resultFTPressure!=null && !"FlexTRAK Overview".equals(resultFTPressure) ){
//                pressure=resultFTPressure;
//            }
//            if("28".equals(procTime) && "21".equals(pressure)){
//                equipStatus = "Idle";
//            }else if("32".equals(procTime) ||"9999".equals(pressure)){
//                equipStatus = "Ready";
//            }else{
//                equipStatus = "Run";
//            }
            String flexStatus = iSecsHost.executeCommand("read flexStatus").get(0);
            if(!flexStatus.toLowerCase().contains("error")){
                equipStatus = flexStatus;
            }
            Map<String, String> map = new HashMap<>();
            map.put("EquipStatus", equipStatus);
            changeEquipPanel(map);
        }
        return equipStatus;
    }

    @Override
    public Object clone() {
        FasTRAKHost newEquip = new FasTRAKHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
    }

    @Override
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        String recipeName = getCurrentRecipeName();
        if(StringUtils.isBlank(recipeName) || recipeName.contains("error")) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "获取当前recipe参数失败,无法获取当前recipe名称。");
            return null;
        }
        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(recipeName).get("recipeParaList");
        if(recipeParaList != null) {
            return recipeParaList;
        }
        return new ArrayList<>();
    }


    /**
     * 初始化状态
     */
//    public void initStatus (){
//            synchronized (iSecsHost.iSecsConnection.getSocketClient()){
//            try{
//                String MHresult= iSecsHost.executeCommand("read MHOverview").get(0);
//                String FTresult= iSecsHost.executeCommand("read FlexTRAKOverview").get(0);
//                if("FlexTRAK Overview".equals(MHresult)){
//                    //FT页面：根据颜色读取状态，
//                    List<String> col1Result= iSecsHost.executeCommand("readrectcolor 293 412 401 511");
//                    List<String> col2Result= iSecsHost.executeCommand("readrectcolor 293 412 401 511");
//                    List<String> col3Result= iSecsHost.executeCommand("readrectcolor 466 353 559 362");
//                    List<String> col4Result= iSecsHost.executeCommand("readrectcolor 620 507 729 510");
//                    if("0xf0f0f0".equals(col1Result.get(0)) ||"0xf0f0f0".equals(col2Result.get(0)) ||"0xf0f0f0".equals(col3Result.get(0))||"0xf0f0f0".equals(col4Result.get(0))){
//                        equipStatus = "Idle";
//                    }else{
//                        equipStatus="Run";
//                    }
//
//                }else if("MH Overview".equals(FTresult)){
//                    //MH页面：先跳转至FT页面，根据读取颜色判断状态，
//                   // iSecsHost.executeCommand("playback click_MH.txt");
//                    iSecsHost.executeCommand("goto FT");
//                    List<String> col1Result= iSecsHost.executeCommand("readrectcolor 293 412 401 511");
//                    List<String> col2Result= iSecsHost.executeCommand("readrectcolor 293 412 401 511");
//                    List<String> col3Result= iSecsHost.executeCommand("readrectcolor 466 353 559 362");
//                    List<String> col4Result= iSecsHost.executeCommand("readrectcolor 620 507 729 510");
//                    if("0xf0f0f0".equals(col1Result.get(0)) ||"0xf0f0f0".equals(col2Result.get(0)) ||"0xf0f0f0".equals(col3Result.get(0))||"0xf0f0f0".equals(col4Result.get(0))){
//                        equipStatus = "Idle";
//                    }else{
//                        equipStatus="Run";
//                    }
//                }
//
//            }catch (Exception e){
//                logger.error("Get equip pageName error:" + e.getMessage(), e);
//            }
//        }
//        }
    //   FT页面：根据颜色读取状态，
    //MH页面：先跳转至FT页面，根据读取颜色判断状态，
    //  若期间页面被跳转至MH页面，则10min后跳转至FT页面；
//
// public String getEquipStatus1() {
//        //初始化proctime=28-stop/32-wating;
//            if (!initFlag) {
//                initStatus();
//                initFlag = true;
//            }
//            try {
//                synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                    if(initFlag){
//                        String MHresult= iSecsHost.executeCommand("read MHOverview").get(0);
//                        String FTresult= iSecsHost.executeCommand("read FlexTRAKOverview").get(0);
//                        if("FlexTRAK Overview".equals(MHresult)){
//                                initStatus();
//                        }else if("MH Overview".equals(FTresult)){
//                            //TODO:假设状态不变;如果界面一直在MH，每十分钟跳到FT页面一次
//                            if (getCheckIntervalTimeFlag()) {
//                                initStatus();
//                        }
//                        }
//                    }
//             }
//            } catch (Exception e) {
//                logger.info("FasTRAK getEquipStatus()-获取状态失败" + e);
//            } finally {
//                Map<String, String> map = new HashMap<>();
//                map.put("PPExecName", ppExecName);
//                map.put("EquipStatus", equipStatus);
//                changeEquipPanel(map);
//            }
//
//        return equipStatus;
//    }

}
