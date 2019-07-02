package cn.tzauto.octopus.isecsLayer.equipImpl.hta;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.hta.FI7300Resolver;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by wj_co on 2018/8/27.
 */
public class FI7300Host extends EquipModel {
    private Logger logger = Logger.getLogger(FI7300Host.class);

    private String eqpStyle = null;

    public FI7300Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }
    @Override
    public String getCurrentRecipeName() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("read recipeName");
                if (results != null && !results.isEmpty()) {
                    ppExecName = results.get(0).split("\\\\")[3].trim();
                    eqpStyle = results.get(0).split("\\\\")[1].trim();
                }
            } catch (Exception e) {
                logger.info("FI7300Host getCurrentRecipeName()-获取recipeName失败" + e);
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }

    @Override
    public String startEquip() {
        String startResult = "Start failed";
        try {
            iSecsHost.executeCommand("playback start.txt");
            Thread.sleep(1000);
            if ("Run".equals(getEquipStatus())) {
                startResult = "0";
            }
        } catch (Exception e) {
            logger.info("开机失败，" + e);
            startResult = "start failed";
        } finally {
            return startResult;
        }
    }

    @Override
    public String pauseEquip() {
        return null;
    }

    @Override
    public String stopEquip() {
        String stopResult = "停机失败,当前状态无法执行停机";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
                try {
                    iSecsHost.executeCommand("playback stop_eqp.txt");
                    Thread.sleep(1000);
                    if ("Idle".equals(getEquipStatus())) {
                        return "0";
                    }
                } catch (Exception e) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "锁机失败,当前状态无法执行锁机！");
                    stopResult = "锁机失败！";
                }
            } else {
                //打日志
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


    /**
     * 判断所需要添加的前缀
     * @param recipeName
     * @return
     */
    public String PrexRCP(String recipeName){
        String prexRecipeName=null;
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> results = iSecsHost.executeCommand("read recipeName");
                if (results != null && !results.isEmpty()) {
                    String rcpName = results.get(0).split("\\\\")[3].trim();
                    // TODO:   String rcpName="QFN-JCET-A-FCQFN-4X4-25L-T0.76";
                    prexRecipeName=rcpName.substring(0,4)+recipeName;
                }
        }
    return prexRecipeName;
    }

    /**
     * @param recipeName
     * @return
     */
    @Override
    public Map uploadRecipe(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        String localFtpIp = GlobalConstants.clientInfo.getClientIp();
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;

        Recipe recipe = setRecipe(recipeName);

        //int first = recipeName.indexOf("-");
       // String specialRecipeName = recipeName.substring(first + 1, recipeName.length()).trim();

        //TODO：7300添加QFN 7200添加QFP
        String prexRecipeName=PrexRCP(recipeName);

        String clientFtpRecipeRelativePath = getClientFtpRecipeRelativePath(prexRecipeName);
        String clientFtpRecipeAbsolutePath= getClientFtpRecipeAbsolutePath(prexRecipeName);
        String zipClientRecipeAbsolutePath = getZipClientRecipeAbsolutePath(prexRecipeName);

        String hostUploadFtpRecipePath = this.getHostUploadFtpRecipePath(recipe);
        String cmd = "ftp " + localFtpIp + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + File.separator + prexRecipeName + "\" \"" + clientFtpRecipeRelativePath + "\" \"mput *\"";//上传recipe文件夹
        String paraCmd="ftp " + localFtpIp + " " + ftpUser + " " + ftpPwd + " " + "\"" +paraPath() + "\" \"" +  clientFtpRecipeRelativePath  + "\" \"put " + recipeName+".xml \"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> list = iSecsHost.executeCommand(cmd);
                if (!list.isEmpty() && list.get(0).contains("success")) {
                    //解析recipe
                    iSecsHost.executeCommand(paraCmd);
                    List<RecipePara> recipeParaList = new ArrayList<>();
                    try{
                        String filePath=clientFtpRecipeAbsolutePath +recipeName+".xml";
                        Map<String, Object> paraMap = FI7300Resolver.transferFrom2Xml(filePath,recipeName);
                        if(paraMap!=null &&!paraMap.isEmpty()){
                            recipeParaList= FI7300Resolver.transferFromDB(paraMap,deviceType);
                        }else{
                            logger.error("解析recipe时出错,recipe文件不存在!");
                        }
                    }catch (Exception e){
                        logger.error("解析recipe时出错!", e);
                        e.printStackTrace();
                    }
                    map.put("recipe", recipe);
                    map.put("deviceCode", deviceCode);
                    map.put("recipeFTPPath", hostUploadFtpRecipePath);
                    map.put("recipeParaList", recipeParaList);
                    //压缩文件
                    try {
                        ZipUtil.toZip(true, new File(zipClientRecipeAbsolutePath).listFiles(), new FileOutputStream(zipClientRecipeAbsolutePath + prexRecipeName + ".zip"));
                    } catch (FileNotFoundException e) {
                        logger.error("压缩recipe失败:" + e);
                    }
                }
                if (!list.isEmpty() && list.get(0).contains("error")) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                    map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                }
            } catch (Exception e) {
                logger.info("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！" + e);
            }
        }
      //  uploadRcpFile2FTP("","/",setRecipe(recipeName));
        return map;
    }


    protected void deleteTempFile(String recipeName) {
        try {
            logger.info("延迟2秒后删除临时文件...");
            Thread.sleep(1500);
        } catch (Exception e) {
        }
        File file = new File(GlobalConstants.localRecipePath +"/RECIPE"+ GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
        FileUtil.deleteAllFilesOfDir(file);
        logger.info("删除临时文件:" + GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/");
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
        String prexRecipeName=PrexRCP(recipeName);
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        String zipClientRecipeAbsolutePath = getZipClientRecipeAbsolutePath(prexRecipeName);
        if (!FtpUtil.uploadFile(zipClientRecipeAbsolutePath + prexRecipeName + ".zip", remoteRcpPath, prexRecipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + prexRecipeName + ".txt 工控路径:" + zipClientRecipeAbsolutePath);
            return false;
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + GlobalConstants.localRecipePath + remoteRcpPath);
//        this.deleteTempFile(recipeName);
        return true;
    }

    /**
     * 下载recipe
     * 1.ftp服务端下载至工控
     * 2.解压recipe文件
     * 3.删除解压文件
     * 4.工控下载至设备机台
     *
     * @param recipe
     * @return
     */
    @Override
    public String downloadRecipe(Recipe recipe) {
        String downLoadResult = "1";
        String recipeName = recipe.getRecipeName();
        String prexRecipeName=PrexRCP(recipeName);

        String localftpip = GlobalConstants.clientInfo.getClientIp();
        String ftpIp = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;

        String clientFtpRecipeAbsolutePath= getClientFtpRecipeAbsolutePath(prexRecipeName);
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(prexRecipeName);
        String filePath=clientFtpRecipeAbsolutePath +recipeName+".xml";
        String zipClientRecipeAbsolutePath = getZipClientRecipeAbsolutePath(prexRecipeName);
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        //从server下载recipe到工控机//
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpIp, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                FtpUtil.downloadFile(zipClientRecipeAbsolutePath + prexRecipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpIp, ftpPort, ftpUser, ftpPwd);
            }
        }
        //解压文件
        try {
            ZipUtil.unZip(new File(zipClientRecipeAbsolutePath + prexRecipeName + ".zip"), zipClientRecipeAbsolutePath);
        } catch (IOException e) {
            logger.info("解压recipe：" + recipeName + "失败" + e);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "下载Recipe:" + recipeName + " 失败.");
            return "download failed";
        }
        //删除压缩文件和卡控文件
        new File(zipClientRecipeAbsolutePath + prexRecipeName + ".zip").delete();
        File file= new File(filePath);
        if(file.exists()){
            file.delete();
        }else{
            logger.info("下载时，卡控文件不存在！");
        }
        String eqpRecipePath = equipRecipePath + File.separator + prexRecipeName;
        String cmdMkdir = "dos $mkdir \"" + eqpRecipePath + "\"$";
        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + eqpRecipePath + "\" \"" + clientRelativeFtpPath + "\" \"mget *\"";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> strings = iSecsHost.executeCommand(cmdMkdir);
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
                logger.info("下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启." + e);
                downLoadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
        }
        return downLoadResult;
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
        attach.setAttachName(recipeName + "_V" + recipe.getVersionNo());
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
        sqlSession.close();
        return attachs;
    }

    /**
     * 卡控参数路径
     * @return
     * "D:\\FI7300\\FI7300 AutoRecipe"
     */
    public String paraPath(){
        String eqpPath=equipRecipePath;//"D:\\FI7300\\Product"
        int index=eqpPath.lastIndexOf("\\");
        String paraName= eqpPath.substring(index+1,eqpPath.length()).trim();
        String parPath=eqpPath.replace(paraName,"FI7300 AutoRecipe").trim();
        return parPath;
    }

    /**
     * 获取recipe上传相对路径
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeRelativePath(String recipeName) {
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/" + recipeName + "/";
    }

    /**
     * 获取并创建工控机recipe绝对路径
     * "D:\autotz\hta\Recipe\product\QFN-JCET-A-FCQFN-4X4-25L-T0.75" "/RECIPE/FI7300_0001QFN-JCET-A-FCQFN-4X4-25L-T0.75temp/QFN-JCET-A-FCQFN-4X4-25L-T0.75/"
     *
     * @param recipeName
     * @return
     */
    public String getClientFtpRecipeAbsolutePath(String recipeName) {
        //D:/RECIPE/**
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        FileUtil.CreateDirectory(filePath);
        return filePath;
    }

    /**
     * 压缩文件绝对路径
     * 路径：D:\RECIPE\RECIPE\FI7300_0001QFN-JCET-A-FCQFN-4X4-25L-T0.755temp\QFN-JCET-A-FCQFN-4X4-25L-T0.755.zip
     *
     * @param recipeName
     * @return
     */
    public String getZipClientRecipeAbsolutePath(String recipeName) {
        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
        FileUtil.CreateDirectory(filePath);
        return filePath;
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
     * 获取服务端下载FtpRecipe路径
     * hostDownloadFtpRecipeFilePath：
     * /RECIPE/A6/DieAttach/FI7300/Engineer/FI7300_0001/QFN-JCET-A-FCQFN-4X4-25L-T0.755/QFN-JCET-A-FCQFN-4X4-25L-T0.755_V1.txt
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
     * 删除recipe文件
     *
     * @param recipeName
     * @return
     */
    @Override
    public String deleteRecipe(String recipeName) {
        String delResult = "Delete failed";
        String cmd = "dos $del /q \"" + equipRecipePath + File.separator + recipeName + File.separator + "* \"$";
        String cmdMkdir = "dos $rd /q \"" + equipRecipePath + File.separator + recipeName + "\"$";
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> Strs = iSecsHost.executeCommand(cmd);
                if ("done".contains(Strs.get(0))) {
                    List<String> StrMkdir = iSecsHost.executeCommand(cmdMkdir);
                    if ("done".contains(StrMkdir.get(0))) {
                        delResult = "Delete success";
                    }
                }
            } catch (Exception e) {
                logger.error("Delete recipe " + recipeName + " error:" + e.getMessage(), e);
                delResult = "Delete failed";
            }
        }
        return delResult;
    }

    /**
     * 切换recipe:
     * 第一步：判断用户是否登录-密码输入错误打日志
     * 第二步：判断是否结批
     * 第三步：lot_ok后操作recipeList
     * 第四步：判断是FI7300/FI7200
     * 第五步：选择切换recipe
     * 第六步：成功后点击系统重置
     *
     * @param recipeName
     * @return
     */
    public String selectRecipe(String recipeName) {
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> userResult = iSecsHost.executeCommand("read user");
                if (userResult != null && userResult.size() > 1) {
                    if (!userResult.get(0).contains("error") && !"done".equalsIgnoreCase(userResult.get(0))) {
                        if (!"OP".equals(userResult.get(0))) {
                            //进行登录操作
                            iSecsHost.executeCommand("replay login_ok.exe");
                            //判断是否登录成功
                            String pwdResult = iSecsHost.executeCommand("read pwd_error").get(0).trim();
                            if ("FI7300".equals(pwdResult)) {
                                logger.info("密码输入有误！");
                            } else {
                                List<String> lotResult = iSecsHost.executeCommand("read lot");
                                if (lotResult != null && !lotResult.isEmpty()) {
                                    if ("結批".equals(lotResult.get(0))) {
                                        try {
                                            iSecsHost.executeCommand("playback gotoLot_ok.txt");
                                        } catch (Exception e) {
                                            logger.error("结批操作失败" + e);
                                            return "failed";
                                        }
                                    } else {
                                        try {
                                            iSecsHost.executeCommand("playback recipelist.txt");
                                        } catch (Exception e) {
                                            logger.info("打开recipe列表操作失败" + e);
                                            return "failed";
                                        }

                                        try {
                                            if ("FI7200".equals(eqpStyle)) {
                                                iSecsHost.executeCommand("playback sel_QFP.txt");//FI7200-QFP
                                            } else {
                                                iSecsHost.executeCommand("playback sel_QFN.txt");//FI7300-QFN
                                            }

                                        } catch (Exception e) {
                                            logger.info("选择机台类型QFP/QFN失败" + e);
                                            return "failed";
                                        }

                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            String rcp_1 = iSecsHost.executeCommand("read rcp_1").get(0);
                                            String rcp_2 = iSecsHost.executeCommand("read rcp_2").get(0);
                                            String rcp_3 = iSecsHost.executeCommand("read rcp_3").get(0);
                                            int first = recipeName.indexOf("-");
                                            String specialRecipeName = recipeName.substring(first + 1, recipeName.length()).trim();

                                            if (rcp_1.equals(specialRecipeName)) {
                                                iSecsHost.executeCommand("playback r1_sel.txt");
                                            } else if (rcp_2.equals(specialRecipeName)) {
                                                iSecsHost.executeCommand("playback r2_sel.txt");
                                            } else if (rcp_3.equals(specialRecipeName)) {
                                                iSecsHost.executeCommand("playback r3_sel.txt");
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
                                            iSecsHost.executeCommand("playback set_ok.txt");
                                            Thread.sleep(500);
                                            iSecsHost.executeCommand("playback sys_reset.txt");
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        while (true) {
                                            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                                                String result = iSecsHost.executeCommand("read sys_reset").get(0).trim();
                                                if (result.equals("FI7300")) {
                                                    break;
                                                } else {
                                                    logger.info("系统重置弹框读取内容为：" + result);
                                                }

                                                try {
                                                    Thread.sleep(8000);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                        try {
                                            iSecsHost.executeCommand("playback sys_reset_ok.txt");
                                        } catch (Exception e) {
                                            logger.info("系统重置操作失败" + e);
                                            return "failed";
                                        }
                                    }
                                }

                            }
                        } else {
                            logger.info("用户名为OP，权限不足！");
                        }
                    }
                } else {
                    logger.info("适配用户名失败");
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

    /**
     * 获得机台卡控参数
     */
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
            logger.info("FI7300Host getEquipRecipeList()-获取recipe列表失败" + e);
        }
        return eppd;
    }


    public List<String> getRecipeList() {
        ArrayList<String> recipeList = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> results = iSecsHost.executeCommand("dos $dir " + "\"" +
                        this.equipRecipePath + "\" /b$");//sml_path
                for (String recipe : results) {
                    if (!"".equals(recipe) && !"done".equals(recipe)) {
                        int first = recipe.indexOf("-");
                        String specialRecipeName = recipe.substring(first + 1, recipe.length()).trim();
                        recipeList.add(specialRecipeName);
                    }
                }
            } catch (Exception e) {
                logger.info("FI7300Host getRecipeList()-获取recipe列表失败" + e);
            }

        }
        return recipeList;
    }

    @Override
    public String getEquipStatus() {
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> screenResult = this.sendMsg2Equip("curscreen");
                if (screenResult != null && !screenResult.isEmpty()) {
                    String screenStatus = screenResult.get(0);
                    if (screenStatus.equals("idle")) {
                        equipStatus = "Idle";
                    } else if (screenStatus.equals("run")) {
                        equipStatus = "Run";
                    }
                }
            } catch (Exception e) {
                logger.info("FI7300Host getEquipStatus()-获取状态失败" + e);
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
    public Object clone() {
        FI7300Host newEquip = new FI7300Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
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
}
