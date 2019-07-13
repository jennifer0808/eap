package cn.tzauto.octopus.isecsLayer.equipImpl.sinyang;

import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.net.Socket;
import java.util.*;


/**
 * Created by wj_co on 2018/9/26.
 */
public class SYMLSSP2000EDHost extends EquipModel {
    private Logger logger = Logger.getLogger(SYMLSSP2000EDHost.class);
    private Boolean isflag = false;
    private long getRecipeAndStatusTime = new Date().getTime();
    //D:\autotz\sinyang\Recipe副本\UserDat\File.dat
    int last = equipRecipePath.lastIndexOf("\\");
    private String recipePath = equipRecipePath.substring(0, last);//D:\autotz\sinyang\Recipe副本\UserDat
    private String datFile = equipRecipePath.substring(last + 1, equipRecipePath.length()).trim();//"File1.dat";

    public SYMLSSP2000EDHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        return null;
    }

    /**
     * 获得间隔时间 10min=600000ms
     *
     * @return
     */
    private boolean getCheckIntervalTimeFlag() {
        long nowDate = new Date().getTime();
        long intervalTime = new Date().getTime() - getRecipeAndStatusTime;
        if (intervalTime > 2000) {//10 * 60 * 1000
            getRecipeAndStatusTime = nowDate;
            return true;
        } else {
            return false;
        }
    }


    public void getRecipeName(showDataCallBack callBack) {

//        if (iSecsHost == null || iSecsHost.iSecsConnection == null || iSecsHost.iSecsConnection.getSocketClient() == null) {
//            while (true) {
//                iSecsHost = new ISecsHost("192.168.2.76", "12000", "", "STMLSSP2000ED_0001");
//                if (iSecsHost.isConnect) {
//                    break;
//                }
//            }
//        }
        Socket socketClient = iSecsHost.iSecsConnection.getSocketClient();
        synchronized (socketClient) {
            try {
                //TODO: 不同界面同一个位置Error
                List<String> results = null;
                results = iSecsHost.executeCommand("read recipeName");
                if (results != null && !results.isEmpty()) {
                    ppExecName = results.get(0).trim();
                }
                //TODO reicpeName = "STOP"
                else {
                    ppExecName = "STOP";
                }
//                else if(results.get(0).contains("Error")){
//                    ppExecName = "--";
//                    if(getCheckIntervalTimeFlag()){
//                        iSecsHost.executeCommand("playback main.txt");
//                        ppExecName = results.get(0).trim();
//                        if(ppExecName.contains("Error")){
//                            ppExecName = "--";
//                        }
//                    }
//                }
            } catch (Exception e) {
                logger.info("SYMLSSP2000EDHost getCurrentRecipeName()-获取recipeName失败" + e);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
//        changeEquipPanel(map);
        callBack.showData(ppExecName);
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
        return null;
    }

    @Override
    public String lockEquip() {
        return null;
    }

    /**
     * 上传：
     * 1.上传整个文件
     * 2.工控上获取指定数据
     * 3.解析
     *
     * @param recipeName
     * @return
     */
    @Override
    public Map uploadRecipe(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        Recipe recipe = setRecipe(recipeName);
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + recipePath + "\" \"" + clientRelativeFtpPath + "\" \"put " + datFile + "\"";
        //ftp 127.0.0.1 rms xccdkj@123 "D:\autotz\sinyang\Recipe\长电sym0288力控2017-12-05\UserDat" "/RECIPE/STMLSSP2000ED_0001QFN LL 5X5 AKJtemp/" "put File1.dat"


        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(cmd);
                if (!resultList.isEmpty() && resultList.get(0).contains("success")) {
                    //保留recipeName一行
                    SYM2000EDResolver sym2000EDResolver = new SYM2000EDResolver(clientAbsoluteFtpPath + datFile);
                    Boolean isStoreRcp = sym2000EDResolver.deleteLine(recipeName, 1);
                    // 解析
                    if (isStoreRcp) {
                        List<RecipePara> recipeParaList = new ArrayList<>();
                        try {
                            Map<String, String> paraMap = sym2000EDResolver.transferFromFile(recipeName);
                            if (paraMap != null && !paraMap.isEmpty()) {
                                recipeParaList = sym2000EDResolver.transferFromDB(paraMap, deviceType);
                            } else {
                                logger.error("解析recipe时出错,recipe文件不存在!");
                            }
                        } catch (Exception e) {
                            logger.error("解析recipe时出错!", e);
                            e.printStackTrace();
                        }
                        map.put("recipe", recipe);
                        map.put("deviceCode", deviceCode);
                        map.put("recipeFTPPath", hostUploadFtpRecipePath);
                        map.put("recipeParaList", recipeParaList);
                    }
                }

                if (!resultList.isEmpty() && resultList.get(0).contains("error")) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！" + e);
            }
        }
        //  uploadRcpFile2FTP("", "/", setRecipe(recipeName));
        return map;
    }


    /**
     * 工控机上传至ftp服务端(ftp over)
     *
     * @param localRcpPath
     * @param remoteRcpPath
     * @param recipe
     * @return
     */
    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        String clientRecipeAbsolutePath = getClientFtpRecipeAbsolutePath(recipeName);
        //将本地路径：clientRecipeAbsolutePath+datFile，上传到远地服务端：/，取名为：recipeName + "_V" + recipe.getVersionNo() + ".txt"
        if (!FtpUtil.uploadFile(clientRecipeAbsolutePath + datFile, remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + datFile + "工控路径:" + clientRecipeAbsolutePath);
            return false;
        }
       UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
        // this.deleteTempFile(recipeName);
        return true;
    }

    protected void deleteTempFile(String recipeName) {
        try {
            logger.info("延迟2秒后删除临时文件...");
            Thread.sleep(1500);
        } catch (Exception e) {
        }
        File file = new File(GlobalConstants.localRecipePath + "/RECIPE" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
        FileUtil.deleteAllFilesOfDir(file);
        logger.info("删除临时文件:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
    }

    /**
     * 下载：
     * 1。ftp Over下载至工控
     * 2.①文件上传至工控机
     * ②工控上将指定数据更新到文件
     * 3.下载
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
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        //ftp 127.0.0.1 rms xccdkj@123 "D:\autotz\sinyang\Recipe\长电sym0288力控2017-12-05\UserDat" "/RECIPE/STMLSSP2000ED_0001QFN LM 4X4 uptemp/" "put File1.dat"
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + recipePath + "\" \"" + clientRelativeFtpPath + "\" \"put " + datFile + "\"";
        //从server下载recipe到工控机
//        if (!GlobalConstants.isLocalMode) {
//            if (!FtpUtil.connectFtp(ftpIp, ftpPort, ftpUser, ftpPwd)) {
//                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启";
//            }
//            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
//                //localFilePath本地文件路径：clientAbsoluteFtpPath + datFile；remoteFilePath：hostDownloadFtpRecipeFilePath
//                FtpUtil.downloadFile(clientAbsoluteFtpPath + datFile, hostDownloadFtpRecipeFilePath, ftpIp, ftpPort, ftpUser, ftpPwd);
//            }
//        }
        List<String> vals = new ArrayList<>();
        //读取文件
        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(getClientFtpRecipeAbsolutePath(recipeName) + datFile), "GBK"));
        ) {
            String rcpLine = null;
            while ((rcpLine = bufferedReader.readLine()) != null) {
                rcpLine = bufferedReader.readLine();
                vals.add(rcpLine);
            }
        } catch (Exception e) {
            logger.info("downloadRecipe failed, resolve template failed" + e);
            // deleteTempFile(recipeName);
            downLoadResult = "没有发现ftp服务端下载至工控的recipe文件";
        }
        //上传
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(cmd);
                if (!resultList.isEmpty() && resultList.get(0).contains("success")) {
                    //写入新的recipe，原先存在则先删除
                    SYM2000EDResolver sym2000EDResolver = new SYM2000EDResolver(clientAbsoluteFtpPath + datFile);
                    try {
                        Map<String, String> map = sym2000EDResolver.getRcpAndLine();
                        String line = map.get(recipeName);
                        if (line != null) {
                            Boolean isStoreRcp = sym2000EDResolver.deleteLine(recipeName, 0);
                            if (isStoreRcp) {
                                sym2000EDResolver.addLine(vals, recipeName);
                            }
                        } else {
                            sym2000EDResolver.addLine(vals, recipeName);
                        }
                        //ftp 127.0.0.1 rms xccdkj@123 "D:\autotz\sinyang\Recipe\长电sym0288力控2017-12-05\UserDat" "/RECIPE/STMLSSP2000ED_0001QFN LL 5X5-6X6temp/" "get File1.dat
                        String cmdDown = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + recipePath + "\" \"" + clientRelativeFtpPath + "\" \"" + "get " + datFile;
                        iSecsHost.executeCommand(cmdDown);
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载recipe[" + recipeName + "]成功。");
                        downLoadResult = "0";
                    } catch (IOException e) {
                        e.printStackTrace();
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "写入新的recipe，原先存在删除失败！下载recipe[" + recipeName + "]失败。");
                        downLoadResult = "下载recipe[" + recipeName + "]失败。";
                    }
                }

                if (!resultList.isEmpty() && resultList.get(0).contains("error")) {
                   UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    downLoadResult = "uploadResult上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.";
                }

            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！" + e);
                downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
        }
        // deleteTempFile(recipeName);
        //重启
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            iSecsHost.executeCommand("playback restart.txt");
//        }
        return downLoadResult;
    }

    /**
     * 1.设备上传至工控机
     * 2.在工控上对File.dat作删除操作
     * 3.下载到设备
     *
     * @param recipeName
     * @return
     */
    @Override
    public String deleteRecipe(String recipeName) {
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + recipePath + "\" \"" + clientRelativeFtpPath + "\" \"put " + datFile + "\"";
        String loadCmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + recipePath + "\" \"" + clientRelativeFtpPath + "\" \"get " + datFile + "\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(cmd);
                if (!resultList.isEmpty() && resultList.get(0).contains("success")) {
                    //对指定rcp作删除操作
                    SYM2000EDResolver sym2000EDResolver = new SYM2000EDResolver(clientAbsoluteFtpPath + datFile);
                    Boolean isDelLine = sym2000EDResolver.deleteLine(recipeName, 0);
                    //ftp下载
                    if (isDelLine) {
                        try {
                            iSecsHost.executeCommand(loadCmd);
                        } catch (Exception e) {
                            logger.info("删除recipe失败，从工控机下载recipe失败" + e);
                            deleteTempFile(recipeName);
                            return "删除失败";
                        }
                    }
                }
            } catch (Exception e) {
                logger.info("删除recipe失败:" + e);
                return "删除失败";
            }
        }
        deleteTempFile(recipeName);
        return "删除成功";
    }

    /**
     * 切换recipe:
     * 1.判断是否登录
     * 2.goto_list
     * 3.rcp_1/rcp_2/rcp_3
     * 4.rcp_ok
     * 5.return_main
     *
     * @param recipeName
     * @return
     */
    @Override
    public String selectRecipe(String recipeName) {
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                try {
                    iSecsHost.executeCommand("playback goto_list.txt");
                } catch (Exception e) {
                    logger.info("打开recipe列表操作失败" + e);
                    return "failed";
                }
                //读屏幕-值rcplist则需要repaly show_list.exe
                try {
                    List<String> isList = iSecsHost.executeCommand("curscreen");
                    if ("rcplist".equals(isList.get(0))) {
                        iSecsHost.executeCommand("repaly show_list.exe");
                    } else {
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
                            iSecsHost.executeCommand("playback rcp_ok.txt");
                        } catch (Exception e) {
                            logger.info("打开recipe列表操作失败" + e);
                            return "failed";
                        }

                        try {
                            iSecsHost.executeCommand("playback return_main.txt");
                        } catch (Exception e) {
                            logger.info("打开recipe列表操作失败" + e);
                            return "failed";
                        }
                    }
                } catch (Exception e) {
                    logger.info("列表截图操作失败：" + e);
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
//        map.put("PPExecName", getCurrentRecipeName());
//        map.put("EquipStatus", getEquipStatus());
        map.put("PPExecName", "STOP");
        map.put("EquipStatus", "RUN");
        map.put("ControlState", controlState);
        return map;
    }

    @Override
    public Map getEquipMonitorPara() {
        return null;
    }

    /**
     * 1.设备上传至工控
     * 2.获取File.dat中recipe列表
     * 获得recipe列表
     *
     * @return
     */
    @Override
    public Map getEquipRecipeList() {
        //dos $findstr "[a-zA-Z]" D:\autotz\sinyang\Recipe\长电sym0288力控2017-12-05\UserDat\File1.dat$
        Map<String, Object> eppd = new HashMap<>();
        List<String> recipeStr = new ArrayList<>();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath("");
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath("");

        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + recipePath + "\" \"" + clientRelativeFtpPath + "\" \"put " + datFile + "\"";
        //dos $findstr ".*" D:\autotz\sinyang\Recipe\长电sym0288力控2017-12-05\UserDat\File1.dat$
        String cmdStr = "dos $findstr " + "\"" + ".*" + "\" " + equipRecipePath + "$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> resultList = iSecsHost.executeCommand(cmdStr, "GBK");
                System.out.println(resultList);
                if (resultList != null && !resultList.isEmpty()) {
                    for (String str : resultList) {
                        if (str != null && !"".equals(str) && !"done".equals(str) && !"error".equals(str)) {
                            String rcpName = str.split("\t")[1];
                            if (!"型号".equals(rcpName)) {
                                recipeStr.add(rcpName);
                            }
                        } else {
                            logger.info("命令执行有误！");
                        }
                    }
                    if (recipeStr != null) {
                        eppd.put("eppd", recipeStr);
                    }
                }

//                List<String> resultList = iSecsHost.executeCommand(cmd);
//                if (!resultList.isEmpty() && resultList.get(0).contains("success")) {
//                    SYM2000EDResolver sym2000EDResolver = new SYM2000EDResolver(clientAbsoluteFtpPath + datFile);
//                    try {
//                        List list = sym2000EDResolver.getRecipeList();
//                        if (list != null) {
//                            eppd.put("eppd", list);
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
            } catch (Exception e) {
                logger.info("获取reicpe列表失败:" + e);
            }
        }
        return eppd;
    }

    private String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    //D:\autotz\sinyang\Recipe\UserDat\File.dat
//ftp 172.17.32.7 rms xccdkj@123 "E:\力控20181015\长电sym0288力控2017-12-05\UserDat" "/RECIPE/D8200-6001temp/" "put File1.dat"
    private String getClientFtpRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        FileUtil.CreateDirectory(filePath);
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

    /**
     * 初始状态
     *
     * @return
     */
//    public void initStatus(List<String> statusResult) {
//        if (statusResult != null && !statusResult.isEmpty()) {
//            if ("status".equalsIgnoreCase(statusResult.get(0)) || "any".equalsIgnoreCase(statusResult.get(0))) {
//                equipStatus = "Idle";
//            } else {
//                equipStatus = "Run";
//            }
//        }
//    }




    /**
     * 初始状态
     *
     * @return
     */
    public void initStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {

            //指令存放map容器
            Map map = initial();
            //遍历循环执行指令
            for (int i = 0; i < map.size(); i++) {
                List<String> statusResult =  this.sendMsg2Equip("readrectcolor " + map.get(i + 1));

                if (!statusResult.contains("Error")) { //TODO unkown?
                    if ("0xff00".equalsIgnoreCase(statusResult.get(0))) {
                        isflag = true;
                        break;
                    } else if("0x0".equalsIgnoreCase(statusResult.get(0))){
                        continue;
                    }
                }

            }
            //若全是0x0，停止；若其中至少有一个是0xff00,运行
            if (isflag) {
                equipStatus = "Run";
            } else {
                equipStatus = "Idle";
            }
        }
    }


    public Map initial() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "157 440 160 443");
        map.put(2, "203 440 206 443");
        map.put(3, "320 440 323 443");
        map.put(4, "451 440 454 443");
        map.put(5, "514 440 517 443");
        map.put(6, "577 440 580 443");
        map.put(7, "641 440 644 443");
        map.put(8, "704 440 708 443");
        map.put(9, "765 440 768 443");
        map.put(10, "850 440 853 443");
        map.put(11, "908 440 911 443");
        map.put(12, "977 440 980 443");
        map.put(13, "1036 440 1039 443");

        return map;

    }

//    @Override
//    public String getEquipStatus() {
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            try {
//                //TODO:读颜色有误差，读截图。不同界面同一位置：any done ;同界面同位置：status done
//                List<String> statusResult = this.sendMsg2Equip("curscreen");
//                // List<String> nameResult = this.sendMsg2Equip("read nameStatus");
//                // if("type".equalsIgnoreCase(nameResult.get(0))) {
//                initStatus(statusResult);
//                //read xxx同一个页面指定位置被遮挡，报空；不同页面指定位置，报Error;
//                //   }
////                else if (nameResult.get(0).contains("Error") || "".equals(nameResult.get(0))){
////                    if(statusResult != null && !statusResult.isEmpty() ) {
////                        if (getCheckIntervalTimeFlag()) {
////                            iSecsHost.executeCommand("playback main.txt");
////                            initStatus(statusResult);
////                        }else{
////                            equipStatus = "--";
////                        }
////                    }
////                }
//            } catch (Exception e) {
//                logger.info("SYMLSSP2000EDHost getEquipStatus()-获取状态失败" + e);
//            } finally {
//                Map<String, String> map = new HashMap<>();
//                map.put("PPExecName", ppExecName);
//                map.put("EquipStatus", equipStatus);
//
//            }
//        }
//        return equipStatus;
//    }

    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //TODO:读颜色有误差，读截图。不同界面同一位置：any done ;同界面同位置：status done
                initStatus();
            } catch (Exception e) {
                logger.info("SYMLSSP2000EDHost getEquipStatus()-获取状态失败" + e);
            } finally {
                Map<String, String> map = new HashMap<>();
                map.put("PPExecName", ppExecName);
                map.put("EquipStatus", equipStatus);

            }
        }
        return equipStatus;
    }








    @Override
    public void run() {
        while (!this.isInterrupted()) {
            if (!isInitState) {
//                Server.acceptMessage(EapClient.server, new showDataCallBack() {
//                    @Override
//                    public void showData(String str) {
//                Map map = new HashMap();
//                String currentRecipeName = getCurrentRecipeName();
//
//                map.put("PPExecName", currentRecipeName);
////                        map.put("EquipStatus", str);
//                map.put("ControlState", GlobalConstant.CONTROL_OFFLINE);
//                changeEquipPanel(map);

//                new Timer().schedule(new TimerTask() {
//                    public void run() {


                getRecipeName(new showDataCallBack() {
                    @Override
                    public void showData(String str) {
                        showUpdateData(str);
                    }
                });


//            }
//                }, 0, 1000);


            }
//                });


            isInitState = true;
            break;
        }
//    }

    }

    public void showUpdateData(String currentRecipeName) {
        Map map = new HashMap();
        if ("STOP".equalsIgnoreCase(currentRecipeName) || "error".equalsIgnoreCase(currentRecipeName)) {
            map.put("ControlState", GlobalConstant.CONTROL_OFFLINE);
        } else {
            map.put("ControlState", GlobalConstant.CONTROL_REMOTE_ONLINE);
        }
        map.put("PPExecName", currentRecipeName);
//                        map.put("EquipStatus", str);
//                        map.put("ControlState", controlState);
        changeEquipPanel(map);
    }

    @Override
    public Object clone() {
        SYMLSSP2000EDHost newEquip = new SYMLSSP2000EDHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newEquip.startUp = this.startUp;
        this.clear();
        return newEquip;
    }

    @Override
    public List<String> getEquipAlarm() {
        return null;
    }


}
