package cn.tzauto.octopus.isecsLayer.equipImpl.ekra;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.meiya.FasTRAKResolver;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wj_co on 2018/9/4.
 */
public class XWSTSHost extends EquipModel {

    private static Logger logger = Logger.getLogger(XWSTSHost.class.getName());
    public XWSTSHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }
    @Override
    public String getCurrentRecipeName() {
        /** 判断recipeName是否可直接读到*/
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try{
                List<String> recipeResult= iSecsHost.executeCommand("read recipeName");
                if(recipeResult!=null &&!recipeResult.isEmpty()){
                    ppExecName=recipeResult.get(0);
                }
            }catch (Exception e){
                logger.error("Get equip ExecName error:" + e.getMessage(), e);
            }
        }
        Map<String,String> map=new HashMap<>();
        map.put("PPExecName",ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        /**
         * 第一步：先读状态-呈现Start
         * 第二步：点击按钮启动
         */
        String startResult = "Start failed";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try{
                List<String> resultList= iSecsHost.executeCommand("playback start.txt");
                Thread.sleep(1000);
                if("Run".equals(getEquipStatus())){
                    startResult = "0";
                }
            }catch (Exception e){
                logger.error("start equip  error:" + e.getMessage(), e);
                startResult = "Start failed";
            }
        }
        return startResult;
    }

    @Override
    public String pauseEquip() {
        return null;
    }

    @Override
    public String stopEquip() {
        /**
         * 第一步：先读状态-呈现Stop
         * 第二步：点击按钮关闭
         */
        String stopResult = "Stop failed";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try{
                List<String> resultList= iSecsHost.executeCommand("playback stop.txt");
                Thread.sleep(1000);
                if("Idle".equals(getEquipStatus())){
                    stopResult = "0";
                }
            }catch (Exception e){
                logger.error("stop equip error:" + e.getMessage(), e);
                stopResult="Stop failed";
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
        Map<String,Object> map=new HashMap<>();
        String localFtpIp = GlobalConstants.clientInfo.getClientIp();
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        Recipe recipe = setRecipe(recipeName);
        String clientFtpRecipeRelativePath=getClientFtpRecipeRelativePath(recipeName);
        String clientFtpRecipeAbsolutePath=getClientFtpRecipeAbsolutePath(recipeName);
        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String cmd="ftp "+localFtpIp+" "+ftpUser+" "+ftpPwd+" \""+equipRecipePath+"\" \""+clientFtpRecipeRelativePath+"\" \"put $"+recipeName+".espdatx $\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try {
                List<String> list =  iSecsHost.executeCommand(cmd);
                for (String s : list) {
                    if(s != null && s.contains("success")){
                    //解析recipe
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try{
                            System.out.println(recipeName);
                            String filePath=equipRecipePath+"\\"+recipeName+".espdatx";
                            Map<String,Object> paraMap = FasTRAKResolver.transferFrom2Xml(filePath);
                            if(paraMap!=null && !paraMap.isEmpty()){
                                recipeParaList=  FasTRAKResolver.transferFromDB(paraMap,deviceType);
                            }else {
                                logger.error("解析recipe时出错,recipe文件不存在!");
                            }
                        }catch (Exception e){
                            logger.error("解析recipe时出错!", e);
                            e.printStackTrace();
                        }
                        map.put("recipe", setRecipe(recipeName));
                        map.put("deviceCode", deviceCode);
                        map.put("recipeParaList", recipeParaList);
                        map.put("recipeFTPPath", hostUploadFtpRecipePath);
                    }
                    if (s != null && s.contains("error")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        break;
                    }
                }
            }catch (Exception e){
                logger.error("upload recipe failed" + e.getMessage(), e);
            }
        }
       // uploadRcpFile2FTP("/","",setRecipe(recipeName));
       // selectRecipe(recipeName);
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
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + ".espdatx", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".flextrak.recipe.xml 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        //this.deleteTempFile(recipeName);
        return true;
    }

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
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        String cmd="ftp "+localftpip+" "+ftpUser+" "+ftpPwd+" \""+equipRecipePath+"\" \""+clientRelativeFtpPath+"\" \"get $"+recipeName+".espdatx $\"";
        //从server下载recipe到工控机
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpIp, ftpPort, ftpUser, ftpPwd)) {
                downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启";
            }
            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                FtpUtil.downloadFile(clientAbsoluteFtpPath + recipeName + ".espdatx", hostDownloadFtpRecipeFilePath, ftpIp, ftpPort, ftpUser, ftpPwd);
            }
        }
        //从工控机下载至机台
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            try{
                List<String> result=iSecsHost.executeCommand(cmd);
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
            }catch (Exception e){
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
        String cmd = "dos $del /q \"" + equipRecipePath + File.separator + recipeName + ".espdatx \"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand(cmd);
                int i = 0;
                for (String s : list) {
                    String s_V = list.get(i);
                    if (s.contains("done") && s_V.contains("done")) {
                        delResult="Delete success";
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


    @Override
    public String selectRecipe(String recipeName) {
        /**
         * //判断是否出现警告-适配颜色 readrectcolor 1322 162 1325 165
         * 第一步：判断是否登录;curscreen -login_everyone表示已登录，进入everyone角色，如果未登录执行login_ok
         * 第二步：goto_recipe
         * 第三步：select 带8的前缀
         * 第四步：select rcp1/2/3
         * 第五步：click recipe ok
         */
        //遇到警告如何判断？
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            //判断警告
            List<String> loginResult = iSecsHost.executeCommand("curscreen");
            if (loginResult != null && !loginResult.isEmpty()) {
                String login = loginResult.get(0);//any
                if (!"login_everyone".equals(login) && !"everyone".equals(login)) {
                    try {
                        Thread.sleep(500);
                        iSecsHost.executeCommand("playback login_ok.txt");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(2000);
                        iSecsHost.executeCommand("playback click_pro.txt");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            iSecsHost.executeCommand("playback goto_recipe.txt");
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            iSecsHost.executeCommand("playback sel_8prefix_rcp.txt");
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            //String rcp_1 = iSecsHost.executeCommand("read rcp_1").get(0);
            //String rcp_2 = iSecsHost.executeCommand("read rcp_2").get(0);
            //String rcp_3 = iSecsHost.executeCommand("read rcp_3").get(0);
            iSecsHost.executeCommand("playback rcp_3.txt");
//            if (rcp_1.equals(recipeName)) {
//                iSecsHost.executeCommand("playback rcp_1.txt");
//            } else if (rcp_2.equals(recipeName)) {
//                iSecsHost.executeCommand("playback rcp_2.txt");
//            } else if (rcp_3.equals(recipeName)) {
//                iSecsHost.executeCommand("playback rcp_3.txt");
//            } else {
//                logger.info("要选中的recipe不存在");
//                iSecsHost.executeCommand("playback gotomain");
//                return "failed";
//            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (iSecsHost.iSecsConnection.getSocketClient()){
            iSecsHost.executeCommand("playback rcp_ok.txt");
        }

        return "0";
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
            logger.info("XWSTS getEquipRecipeList()-获取recipe列表失败" + e);
        }
        return eppd;
    }

    public List<String> getRecipeList() {
        ArrayList<String> recipeList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("dos $dir " + "\"" +
                        this.equipRecipePath + File.separator + "*.espdatx" +"\" /b$");
                for (String recipeName : results) {
                    if (!"".equals(recipeName) && !"done".equals(recipeName)) {
                        recipeName = recipeName.replace(".espdatx", "");
                        if(!"".equals(recipeName)) {
                            recipeList.add(recipeName);
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("XWSTSHost getRecipeList()-获取recipe列表失败" + e);
            }
        }
        return recipeList;
    }

    @Override
    public String getEquipStatus() {
        /**
         * 注：获得状态read * 稳定性比curscreen高
         */
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> screenResult = iSecsHost.executeCommand("read Eqpstatus");
                if (screenResult != null && !screenResult.isEmpty()) {
                    equipStatus = screenResult.get(0);
                    if (equipStatus.equals("Start")) {
                        equipStatus = "Idle";
                    }else if(equipStatus.equals("Stop")){
                        equipStatus = "Run";
                    }
                }
            } catch (Exception e) {
                logger.info("XWSTSHost getEquipStatus()-获取状态失败" + e);
            } finally {
                Map<String, String> map = new HashMap<>();
                map.put("PPExecName", ppExecName);
                map.put("EquipStatus", equipStatus);
                changeEquipPanel(map);
            }
        }
        return equipStatus;
    }

    @Override
    public Object clone() {
        XWSTSHost newHost = new XWSTSHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newHost.startUp = startUp;
        clear();
        return newHost;
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            if (!isInitState) {
                getEquipRealTimeState();
                isInitState = true;
                break;
            }
        }
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
}
