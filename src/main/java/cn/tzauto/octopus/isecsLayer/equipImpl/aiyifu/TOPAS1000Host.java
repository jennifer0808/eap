/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.equipImpl.aiyifu;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.resolver.disco.RecipeEdit;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.net.Socket;
import java.util.*;

/**
 * @author Wang DanFeng
 * 更换recipe只能是机台程序目录下Part2Param.txt中存在的文件
 */
public class TOPAS1000Host extends EquipModel {

    private static Logger logger = Logger.getLogger(TOPAS1000Host.class.getName());
    private TOPASSocketLink deviceChannel;
    private Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();

    public TOPAS1000Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        init(remoteIpAddress);
    }

    private void init(String remoteIpAddress) {
        deviceChannel = new TOPASSocketLink(remoteIpAddress, "8881", deviceCode, deviceType);
    }

    public boolean login() {
        return true;
//        List<String> result = iSecsHost.executeCommand("read lockflag");
//        if (result != null && !result.isEmpty()) {
//            //判断是否是登陆
//            if ("LOCK".equalsIgnoreCase(result.get(0))) {
//                //点击登陆
//                iSecsHost.executeCommand("replay login.exe");
//                result = iSecsHost.executeCommand("read lockflag");
//                if (result != null && !result.isEmpty()) {
//                    if ("unLOCK".equalsIgnoreCase(result.get(0))) {
//                        return true;
//                    } else {
//                        logger.info(deviceCode + "========登陆失败，检查设备连接状态！");
//                        return false;
//                    }
//                }else{
//                    logger.info(deviceCode + "========登陆失败，检查设备连接状态！");
//                    return false;
//                }
//            } else {
//                logger.info(deviceCode + "========用户已登录！");
//                return true;
//            }
//        } else {
//            logger.info(deviceCode + "========登陆失败，检查设备连接状态！");
//            return false;
//        }

    }

    @Override
    public String getCurrentRecipeName() {
        //1.以下方法频繁操作麻烦
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                // TODO: 读取recipeName over
//                if (this.login()) {
//                    //点击储存设定
//                    List<String> result = iSecsHost.executeCommand("replay savereport.exe");
//                    //查询最新一条的数据
//                    result = iSecsHost.executeCommand("dos $dir " + "\"c:\\report\"" + "/o-d /b$");
//                    //读取第一个文件的内容
//                    List<String> reportResult = iSecsHost.executeCommand("dos $type " + "\"c:\\report\\" + result.get(0) + "\"$");
//                    if (reportResult != null && !reportResult.isEmpty()) {
//                        ppExecName = reportResult.get(6).substring(reportResult.get(6).lastIndexOf("\\") + 1, reportResult.get(6).lastIndexOf("."));
//                    } else {
//                        logger.info(deviceCode + "========获取recipe失败，检查设备连接状态！");
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Get equip ExecName error:" + e.getMessage());
//            }
//        }
        //直接调用厂商接口
        try {
            synchronized (deviceChannel.iSecsConnection.getSocketClient()) {
                List<String> result = deviceChannel.executeCommand("ReadRecipe");
                if (result != null && !result.isEmpty()) {
                    if (!"N".equalsIgnoreCase(result.get(0))) {
                        ppExecName = result.get(0);
                    }
                } else {
                    logger.info(deviceCode + "========获取recipe失败，检查设备连接状态！");
                }
            }
        } catch (Exception e) {
            logger.error("Get equip ExecName error:" + e.getMessage(), e);
        } finally {
            Map map = new HashMap();
            map.put("PPExecName", ppExecName);
            changeEquipPanel(map);
            return ppExecName;
        }
    }

    @Override
    public String startEquip() {
//        List<String> result = null;
//        String startResult = "Start failed";
//        try {
//            // TODO: 2018/3/19 开始机台
//            result = iSecsHost.executeCommand("playback start.txt");
//            if ("done".equals(result.get(0))) {
//                result = iSecsHost.executeCommand("read ok_start");
//                if ("OK".equalsIgnoreCase(result.get(0))) {
//                    result = iSecsHost.executeCommand("read status");
//                    if (result != null && !result.isEmpty()) {
//                        if (!result.get(0).contains("error")) {
//                            if ("AUTO RUNNING".equals(result.get(0))) {
//                                equipStatus = "RUN";
//                                startResult = "0";
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            startResult = "Start failed";
//        } finally {
//            return startResult;
//        }
        return "Start failed";
    }

    @Override
    public String pauseEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String stopEquip() {
        // TODO: 暂时不需要卡控机台
//        String stopResult = "锁机失败,当前状态无法执行锁机";
////        if (getPassport(1)) {
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            DeviceService deviceService = new DeviceService(sqlSession);
//            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//            sqlSession.close();
//
//            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
//                List<String> result = null;
//                try {
//                    result = iSecsHost.executeCommand("playback stop.txt");
//                    if ("done".equals(result.get(0))) {
//                        equipStatus = result.get(0);
//                        stopResult = "0";
//                    }
////                    for (String str : result) {
////                        if ("done".equals(result.get(0))) {
////                            result = iSecsHost.executeCommand("read status");
////                            if (result != null && !result.isEmpty()) {
////                                if (!result.get(0).contains("error")) {
////                                    if ("STOP".equals(result.get(0))) {
////                                        equipStatus = result.get(0);
////                                        stopResult = "0";
////                                    }
////                                }
////                            }
////                        }
////                    }
//                } catch (Exception e) {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "锁机失败,当前状态无法执行锁机！");
//                    return stopResult;
//                }
//            } else {
//                UiLogUtil.appendLog2EventTab(deviceCode, "未设置锁机！");
//                stopResult = "未设置锁机！";
//            }
//        }
////            returnPassport();
////        }
//        return stopResult;
        return "Start failed";
    }

    @Override
    public String lockEquip() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        // TODO: 2018/3/19 上传recipe OVER
        Map resultMap = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //ftp ip user pwd lpath rpath "mput "103.rcp"
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpPort = GlobalConstants.ftpPort;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                Recipe recipe = setRecipe(recipeName);
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端上传ftp路径
                String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
                FileUtil.CreateDirectory(clientFtpRecipeAbsolutePath + "temp");
                //ocr上传至本地,上传两个文件需要打包成压缩包
                boolean ocrUploadOk = true;
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "temp" + "\" \"mput " + recipeName + ".*\"";
                List<String> result = iSecsHost.executeCommand(command);

                for (String uploadstr : result) {
                    if (uploadstr.contains("success")) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            //不需要解析，将两个文件打包压缩
                            File[] files = new File(clientFtpRecipeAbsolutePath + "temp").listFiles(new myFileFilter(recipeName));
                            boolean zipFlag = ZipUtil.toZip(Arrays.asList(files), clientFtpRecipeAbsolutePath + recipeName + "_V" + recipe.getVersionNo() + ".txt");
                            if (!zipFlag) {
                                ocrUploadOk = zipFlag;
                                logger.error(deviceCode + "=====压缩recipe文件[" + recipeName + "]出错！");
                            }
//                            // TODO: 2018/3/23  可读取修改解析方法 OVER
//                            Map paraMap = VSP88AHRecipeUtil.transferFromFile(clientFtpRecipeAbsolutePath + recipeName + ".rcp");
//                            if (paraMap != null && !paraMap.isEmpty()) {
//                                recipeParaList = VSP88AHRecipeUtil.transferFromDB(paraMap, deviceType);
//                            } else {
//                                
//                            }
                        } catch (Exception ex) {
                            logger.error("解析recipe时出错!", ex);
                            ex.printStackTrace();
                        }
                        resultMap.put("recipe", recipe);
                        resultMap.put("deviceCode", deviceCode);
                        resultMap.put("recipeFTPPath", hostUploadFtpRecipePath);
                        resultMap.put("recipeParaList", recipeParaList);

                    }
                    if (uploadstr.contains("Error")) {
                        ocrUploadOk = false;
                    }
                }
                if (!ocrUploadOk) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.error("upload recipe failed" + e.getMessage(), e);
            }
        }
        return resultMap;

    }

    public static class myFileFilter implements FilenameFilter {

        String fileName = "";

        public myFileFilter() {
        }

        public myFileFilter(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public boolean accept(File dir, String name) {
            //如果文件名称以.java为结尾，或文件对应一个路径，则返回true
            return name.contains(fileName) && !new File(fileName).isDirectory();
        }
    }

    /**
     * 获取工控机FtpRecipe绝对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeAbsolutePath(String recipeName) {
        //D:/RECIPE/**********
        return GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
    }

    /**
     * 获取工控机FtpRecipe相对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    /**
     * 获取机台本地Recipe路径
     *
     * @return
     */
    public String getEquipLocalRecipePath() {
        return equipRecipePath;
    }

    /**
     * 获取服务端上传FtpRecipe路径
     *
     * @param recipe
     * @return
     */
    public String getHostUploadFtpRecipePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        sqlSession.close();
        return ftpRecipePath;
    }

    /**
     * 获取服务端下载FtpRecipe文件全路径
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
    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = new RecipeService(sqlSession).organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        Attach attach = new Attach();
        attach.setId(UUID.randomUUID().toString());
        attach.setRecipeRowId(recipe.getId());
        attach.setAttachPath(ftpRecipePath);
        attach.setAttachName(recipe.getRecipeName() + "_V" + recipe.getVersionNo());
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

    @Override
    public String downloadRecipe(Recipe recipe) {
        // TODO: 2018/3/19 下载recipe
        //清空机台根目录
        //cleanRootDir();
        //ftp ip user pwd lpath rpath \"mget 001.ALU 001.CLN 001.DFD DEV.LST DEVID.LST\"
        //ftp ip user pwd lpath rpath \"mget 103.rcp\"
        String downloadResult = "";
        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
        synchronized (socketClient) {
            try {
                String recipeName = recipe.getRecipeName();
                String localftpip = GlobalConstants.clientInfo.getClientIp();
                String ftpip = GlobalConstants.ftpIP;
                String ftpUser = GlobalConstants.ftpUser;
                String ftpPwd = GlobalConstants.ftpPwd;
                String ftpPort = GlobalConstants.ftpPort;
                //工控机本地绝对路径
                String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
                //工控机本地ftp相对路径
                String clientFtpRecipeRelativePath = this.getClientFtpRecipeRelativePath(recipeName);
                //机台本地路径
                String equipLocalRecipePath = this.getEquipLocalRecipePath();
                //服务端下载ftp文件全路径
                String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//                String ftpPath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
                String hostDownloadFtpRecipePath = hostDownloadFtpRecipeFilePath.substring(0, hostDownloadFtpRecipeFilePath.lastIndexOf("/") + 1);
                if (!GlobalConstants.isLocalMode) {
                    if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                        return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }

                    if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".txt", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                    } else {
                        FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".txt", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                        //gold版本
                        if (RecipeEdit.hasGoldPara(deviceType)) {
                            RecipeService recipeService = new RecipeService(sqlSession);
                            List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
                            List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
                            FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD");
                            List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD", deviceType);
                            RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD");
                        }
                    }
                }
                sqlSession.close();
                //将从服务端ftp上下载的recipe压缩包解压成到文件下
                //创建一个新的目录存放解压后的recipe文件
                FileUtil.CreateDirectory(clientFtpRecipeAbsolutePath + "temp");
                boolean unZipFlag = ZipUtil.unZip(new File(clientFtpRecipeAbsolutePath + recipeName + ".txt"), clientFtpRecipeAbsolutePath + "temp");
                if (!unZipFlag) {
                    downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,解压缩recipe文件失败.";
                    logger.error(deviceCode + "=====解压缩recipe文件[" + recipeName + "]出错！");
                    return downloadResult;

                }
                
                String command = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipLocalRecipePath + "\" \"" + clientFtpRecipeRelativePath + "temp" + "\" \"mget " + recipeName + ".*\"";
                List<String> result = iSecsHost.executeCommand(command);
                for (String str : result) {
                    if (str.contains("success")) {
                        downloadResult = "0";
                    }
                    if (str.contains("Error")) {
                        downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    }
                }
            } catch (Exception e) {
                logger.error("Download recipe " + recipe.getRecipeName() + " error:" + e.getMessage(), e);
                downloadResult = "Download recipe " + recipe.getRecipeName() + " failed";
            }
        }
        //删除工控端临时文件
        //this.deleteTempFile(recipe.getRecipeName());
        return downloadResult;
    }

    @Override
    public Map getEquipRealTimeState() {
        // TODO: 2018/3/19 获取当前状态等 over
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
        // TODO: 2018/3/19 删除recipe over
        Map map = getEquipRecipeList();
        List<String> recipeList = (List<String>) map.get("eppd");
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //机台本地recipe路径
            String equipLocalRecipePath = this.getEquipLocalRecipePath();
            try {
                if (!recipeList.contains(recipeName)) {
                    logger.info("Recipe:[" + recipeName + "]设备上不存在,无需删除");
                    return "删除成功";
                } else {
                    List<String> result = iSecsHost.executeCommand("dos $del /q \"" + equipLocalRecipePath + "\\" + recipeName + ".*\"$");
                    for (String str : result) {
                        if ("done".equals(str)) {
                            return "删除成功";
                        }
                    }
                }
                return "删除失败";
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                return "删除失败";
            }
        }
    }

    @Override
    public String selectRecipe(String recipeName) {
        // TODO: 2018/3/19 选中recipe over
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                List<String> result = iSecsHost.executeCommand("replay select.exe");
//                if ("done".equals(result.get(0))) {
////                    //写入recipe的数值
////                    result = iSecsHost.executeCommand("write selectrecipe " + recipeName + ".");
//                    //判断界面是否最大化
//                    List<String> windowflag = iSecsHost.executeCommand("readm windowflag");
//                    if (windowflag != null && !windowflag.isEmpty()) {
//                        if (!"".equals(windowflag.get(0))) {
//                            result = iSecsHost.executeCommand("replay windowflag.exe");
//                        }
//                    }
//                    //读取每行的模式
//                    result = iSecsHost.executeCommand("readm r1 r2 r3");
//                    String recipeRow = "";
//                    //查询recipe是否可以选中，并找出recipe位置信息
//                    for (int i = 0; i < result.size(); i++) {
//                        if (recipeName.equals(result.get(i))) {
//                            recipeRow = "r" + (i + 1);
//                            break;
//                        }
//                    }
//                    result = iSecsHost.executeCommand("replay " + recipeRow + ".exe");
//                    result = iSecsHost.executeCommand("replay ok_sel.exe");
//                    Thread.sleep(500);
//                    if ("done".equals(result.get(0))) {
//                        //查询recipe是否有更改成功
//                        String currentRecipeName = getCurrentRecipeName();
//                        if (currentRecipeName.equals(recipeName)) {
//                            return "0";
//                        }
//                    }
//                }
//                return "选中失败";
//            } catch (Exception e) {
//                logger.error("Select recipe " + recipeName + " error:" + e.getMessage());
//                return "选中失败";
//            }
//        }        
        try {
           List<String> recipeList= (List<String>)getEquipRecipeList().get("eppd");
           if(!recipeList.contains(recipeName)){
                return "选中失败";
           }
            synchronized (deviceChannel.iSecsConnection.getSocketClient()) {
                List<String> result = deviceChannel.executeCommand("Select \"" + recipeName + "\"");
                if (result != null && result.isEmpty()) {
                    if (!"Y".equals(result.get(0))) {
                        return "选中失败";
                    }
                }
            }
            //查询recipe是否有更改成功
            String currentRecipeName = getCurrentRecipeName();
            if (currentRecipeName.equals(recipeName)) {
                return "0";
            }
            return "选中失败";
        } catch (Exception e) {
            logger.error("Select recipe " + recipeName + " error:" + e.getMessage(), e);
            return "选中失败";
        }
    }

    @Override
    public Map getEquipMonitorPara() {
        List<RecipePara> recipeParaList = (List<RecipePara>) uploadRecipe(getCurrentRecipeName()).get("recipeParaList");
        Map<String, String> resultMap = new HashMap<>();
        for (RecipePara recipePara : recipeParaList) {
            resultMap.put(recipePara.getParaName(), recipePara.getSetValue());
        }
        logger.info("monitormap:" + resultMap.toString());
        return resultMap;
        // TODO: 2018/3/19 获取参数
//        Map map = new HashMap();
////        if (getPassport()) {
//        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
//        synchronized (socketClient) {
//            try {
//                iSecsHost.executeCommand("playback gotoworkscreen.txt");
//                iSecsHost.executeCommand("goto param");
//                List<String> screenList = iSecsHost.executeCommand("curscreen");
//                if (screenList != null && !screenList.isEmpty()) {
//                    if (!"param".equals(screenList.get(0))) {
//                        iSecsHost.executeCommand("playback gotoworkscreen.txt");
//                        iSecsHost.executeCommand("goto param");
//                    }
//                    List<String> result = iSecsHost.executeCommand("readm gzxz gzcc hdjp hdjiaop devid");
//                    if (result.size() == 6) {
//                        map.put("gzxz", result.get(0));
//                        map.put("gzcc", result.get(1));
//                        map.put("hdjp", result.get(2));
//                        map.put("hdjiaop", result.get(3));
//                        map.put("devid", result.get(4));
//
//                        if ("done".equals(result.get(5))) {
//                            iSecsHost.executeCommand("goto scsch1");
//                            List<String> result1 = iSecsHost.executeCommand("readm ch1lp1 ch1lp2 ch1f1 ch1f2 ch1da1 ch1da2 ch1fs1 ch1fs2 ch1i1 ch1i2 ch1dsi1 ch1dsi2");
//                            if (result1.size() == 13) {
//                                map.put("ch1lp1", result1.get(0));
//                                map.put("ch1lp2", result1.get(1));
//                                map.put("ch1f1", result1.get(2));
//                                map.put("ch1f2", result1.get(3));
//                                map.put("ch1da1", result1.get(4));
//                                map.put("ch1da2", result1.get(5));
//                                map.put("ch1fs1", result1.get(6));
//                                map.put("ch1fs2", result1.get(7));
//                                map.put("ch1i1", result1.get(8));
//                                map.put("ch1i2", result1.get(9));
//                                map.put("ch1dsi1", result1.get(10));
//                                map.put("ch1dsi2", result1.get(11));
//                                if ("done".equals(result1.get(12))) {
//                                    iSecsHost.executeCommand("goto scsch2");
//                                    List<String> result2 = iSecsHost.executeCommand("readm ch2lp1 ch2lp2 ch2f1 ch2f2 ch2da1 ch2da2 ch2fs1 ch2fs2 ch2i1 ch2i2 ch2dsi1 ch2dsi2");
//                                    if (result2.size() == 13) {
//                                        map.put("ch2lp1", result2.get(0));
//                                        map.put("ch2lp2", result2.get(1));
//                                        map.put("ch2f1", result2.get(2));
//                                        map.put("ch2f2", result2.get(3));
//                                        map.put("ch2da1", result2.get(4));
//                                        map.put("ch2da2", result2.get(5));
//                                        map.put("ch2fs1", result2.get(6));
//                                        map.put("ch2fs2", result2.get(7));
//                                        map.put("ch2i1", result2.get(8));
//                                        map.put("ch2i2", result2.get(9));
//                                        map.put("ch2dsi1", result2.get(10));
//                                        map.put("ch2dsi2", result2.get(11));
//                                        if ("done".equals(result2.get(12))) {
//                                            iSecsHost.executeCommand("goto scsch3");
//                                            List<String> result3 = iSecsHost.executeCommand("readm ch3lp1 ch3lp2 ch3f1 ch3f2 ch3da1 ch3da2 ch3fs1 ch3fs2 ch3i1 ch3i2 ch3dsi1 ch3dsi2");
//                                            if (result3.size() == 13) {
//                                                map.put("ch3lp1", result3.get(0));
//                                                map.put("ch3lp2", result3.get(1));
//                                                map.put("ch3f1", result3.get(2));
//                                                map.put("ch3f2", result3.get(3));
//                                                map.put("ch3da1", result3.get(4));
//                                                map.put("ch3da2", result3.get(5));
//                                                map.put("ch3fs1", result3.get(6));
//                                                map.put("ch3fs2", result3.get(7));
//                                                map.put("ch3i1", result3.get(8));
//                                                map.put("ch3i2", result3.get(9));
//                                                map.put("ch3dsi1", result3.get(10));
//                                                map.put("ch3dsi2", result3.get(11));
//
//                                                if ("done".equals(result3.get(12))) {
//                                                    iSecsHost.executeCommand("goto scsch4");
//                                                    List<String> result4 = iSecsHost.executeCommand("readm ch4lp1 ch4lp2 ch4f1 ch4f2 ch4da1 ch4da2 ch4fs1 ch4fs2 ch4i1 ch4i2 ch4dsi1 ch4dsi2");
//                                                    if (result4.size() == 13) {
//                                                        map.put("ch4lp1", result4.get(0));
//                                                        map.put("ch4lp2", result4.get(1));
//                                                        map.put("ch4f1", result4.get(2));
//                                                        map.put("ch4f2", result4.get(3));
//                                                        map.put("ch4da1", result4.get(4));
//                                                        map.put("ch4da2", result4.get(5));
//                                                        map.put("ch4fs1", result4.get(6));
//                                                        map.put("ch4fs2", result4.get(7));
//                                                        map.put("ch4i1", result4.get(8));
//                                                        map.put("ch4i2", result4.get(9));
//                                                        map.put("ch4dsi1", result4.get(10));
//                                                        map.put("ch4dsi2", result4.get(11));
//
//                                                        if ("done".equals(result4.get(12))) {
//                                                            iSecsHost.executeCommand("goto sidch1");
//                                                            List<String> result5 = iSecsHost.executeCommand("readm qgsx ch1y1");
//                                                            if (result5.size() == 3) {
//                                                                map.put("qgsx", result5.get(0));
//                                                                map.put("ch1y1", result5.get(1));
//                                                                if ("done".equals(result5.get(2))) {
//                                                                    iSecsHost.executeCommand("goto sidch2");
//                                                                    List<String> result6 = iSecsHost.executeCommand("readm ch2y1");
//                                                                    if (result6.size() == 2) {
//                                                                        map.put("ch2y1", result6.get(0));
//                                                                        if ("done".equals(result6.get(1))) {
//                                                                            iSecsHost.executeCommand("goto sidch3");
//                                                                            List<String> result7 = iSecsHost.executeCommand("readm ch3y1");
//                                                                            if (result7.size() == 2) {
//                                                                                map.put("ch3y1", result7.get(0));
//                                                                                if ("done".equals(result7.get(1))) {
//                                                                                    iSecsHost.executeCommand("goto sidch4");
//                                                                                    List<String> result8 = iSecsHost.executeCommand("readm ch4y1");
//                                                                                    if (result8.size() == 2) {
//                                                                                        map.put("ch4y1", result8.get(0));
//                                                                                    }
//                                                                                }
//                                                                            }
//                                                                        }
//                                                                    }
//                                                                }
//                                                            }
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                logger.error("Get equip status error:" + e.getMessage());
//
//            }
//        }
////            returnPassport();
////        }
//        iSecsHost.executeCommand("playback gotoworkscreen.txt");
//        logger.info("monitormap:" + map.toString());
//        return map;
    }

    @Override
    public Map getEquipRecipeList() {
        // TODO: 2018/3/19 获取recipe列表 over
        List<String> result = new ArrayList<>();
        List<String> equipRecipeList = new ArrayList<>();
        Map eppd = new HashMap();
        //机台本地recipe路径
        String equipLocalRecipPath = this.getEquipLocalRecipePath();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                result = iSecsHost.executeCommand("dos $dir " + "\"" + equipLocalRecipPath + File.separator + "*.Pam" + "\" /b$");
                for (String recipeName : result) {
                    if (!"done".equals(recipeName) && !"".equals(recipeName)) {
                        equipRecipeList.add(recipeName.replace(".Pam", ""));
                    }
                }
            } catch (Exception e) {
                return eppd;
            }
        }
        eppd.put("eppd", equipRecipeList);
        return eppd;
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            if (!isInitState) {
                //check与机台socket
                deviceChannel.iSecsConnection.checkConenctionStatus();
                getEquipRealTimeState();
                isInitState = true;
                break;
            }
        }
    }

    @Override
    public Object clone() {
        TOPAS1000Host newEquip = new TOPAS1000Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        //newEquip.equipState = this.equipState;
        this.clear();
        return newEquip;
    }

    public List<String> getEquipAlarm() {
        // TODO: 2018/3/19 查询报警?
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("readrectcolor 740 60 760 65 ");
                for (String colorstr : result) {
                    if ("0xc0c0c0".equals(colorstr)) {
                        alarmStrings.add("");
                    }
                    if ("0xff0000".equals(colorstr)) {
                        logger.info("The equip state changged to alarm...");
                        List<String> resultAlarmId = iSecsHost.executeCommand("read alarmid");
                        if (resultAlarmId.size() > 1) {
                            alarmStrings.add(resultAlarmId.get(0));
                            logger.info("Get alarm ALID=[" + resultAlarmId.get(0) + "]");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage(), e);
            }
        }
        if (alarmStrings.size() > 0) {
            alarmStrings.remove("0xff0000");
        }
        return alarmStrings;
    }

    @Override
    public String getMTBA() {
        String mtba = "";
        return mtba;
    }

    @Override
    public String getEquipStatus() {
        // TODO: 2018/3/19 获取机台状态
        try {
            synchronized (deviceChannel.iSecsConnection.getSocketClient()) {
                List<String> result = deviceChannel.executeCommand("ReadStatus");
                if (result != null && !result.isEmpty()) {
                    equipStatus = result.get(0);
                } else {
                    logger.info(deviceCode + "========获取状态失败，检查设备连接状态！");
                }
            }
        } catch (Exception e) {
            logger.error("Get equip status error:" + e.getMessage(), e);
        } finally {
            Map map = new HashMap();
            map.put("EquipStatus", equipStatus);
            map.put("PPExecName", ppExecName);
            changeEquipPanel(map);
            return equipStatus;
        }
    }

    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        // TODO: 2018/3/19 上传recipe到ftp over
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientFtpRecipeAbsolutePath + recipeName + "_V" + recipe.getVersionNo() + ".txt", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        //删除本地临时文件
        //this.deleteTempFile(recipeName);
        return true;
    }

    public Map getLaserEnergy() {
        Map map = new HashMap();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = this.sendMsg2Equip("curscreen");
                if (result != null && !result.isEmpty()) {
                    if ("powercheck".equals(result.get(0))) {

                        List<String> laserEnergyresult = iSecsHost.executeCommand("readm frequency p1uv p1power p2uv p2power p3uv p3power p4uv p4power p5uv p5power");
                        if (laserEnergyresult.size() > 2) {
                            map.put("frequency", laserEnergyresult.get(0));
                            map.put("p1uv", laserEnergyresult.get(1));
                            map.put("p1power", laserEnergyresult.get(2));
                            map.put("p2uv", laserEnergyresult.get(3));
                            map.put("p2power", laserEnergyresult.get(4));
                            map.put("p3uv", laserEnergyresult.get(5));
                            map.put("p3power", laserEnergyresult.get(6));
                            map.put("p4uv", laserEnergyresult.get(7));
                            map.put("p4power", laserEnergyresult.get(8));
                            map.put("p5uv", laserEnergyresult.get(9));
                            map.put("p5power", laserEnergyresult.get(10));
                            logger.info("get LaserEnergy data." + map.toString());
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get equip LaserEnergy error:" + e.getMessage(), e);
            }
        }
        return map;

    }

    private void cleanRootDir() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("dos \"del /q " + equipRecipePath + "\\*\"");
                for (String str : result) {
                    if ("done".equals(str)) {
                        logger.info("root 目录清理完成");
                    }
                }
            } catch (Exception e) {
                logger.error("目录清理时发生异常:" + e.getMessage(), e);
            }
        }
    }

    private String selectRecipeBySocket() {

        return "";
    }

}
