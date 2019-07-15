package cn.tzauto.octopus.isecsLayer.equipImpl.gin;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.ZipUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.resolver.dd.GinRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.disco.RecipeEdit;
import cn.tzauto.octopus.secsLayer.util.GlobalConstant;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.util.*;

public class A2255Host extends EquipModel {

    private static final Logger logger = Logger.getLogger(A2255Host.class.getName());

    public A2255Host(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(GlobalConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public String getCurrentRecipeName() {
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> recipeNameResult = iSecsHost.executeCommand("read recipeName");
                if (recipeNameResult != null && !recipeNameResult.isEmpty()) {
                    ppExecName = recipeNameResult.get(0);
                }
                //读取文件内容获取当前recipe的名称
//                List<String> recipeNameResult = iSecsHost.executeCommand("dos $type \"D:\\newtesthander\\system\\lastdata.dat\"$");
//                if (recipeNameResult != null && !recipeNameResult.isEmpty()) {
//                    ppExecName = recipeNameResult.get(0).split(".BLD")[0];
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("设备：" + deviceCode + "获取recipe失败！" + e.getMessage());
        }
        Map<String, String> map = new HashMap<>();
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
        return null;
    }

    @Override
    public String lockEquip() {
        return null;
    }

    @Override
    public Map uploadRecipe(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        String localftpip = GlobalConstants.clientInfo.getClientIp(); //工控机ftpip
        String ftpIp = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String equipRecipePath = this.equipRecipePath;
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipeName);
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        File tempFile = new File(clientAbsoluteFtpPath + recipeName);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        //上传整个文件夹，并压缩

        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                //在recipe目录下，读取文件并将其中文件上传至ftp
                List<String> result;
                //List<String> parts = iSecsHost.executeCommand("dos $dir /ad/b \"" + equipRecipePath + File.separator + recipeName + "\"$");
                String recipePartName = "MOTOR";
                for (int i = 1; i < 21; i++) {
                    //本地创建对应文件
                    File tempPartFile = new File(clientAbsoluteFtpPath + recipeName + File.separator + recipePartName + i);
                    if (!tempPartFile.exists()) {
                        tempPartFile.mkdirs();
                    }
                    //上传至指定文件下
                    iSecsHost.executeCommand("ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " "
                            + "\"" + equipRecipePath + File.separator + recipeName + File.separator + recipePartName + i + "\" \""
                            + clientRelativeFtpPath + recipeName + File.separator + recipePartName + i + "\" \"mput *\"");
                }
                //将非文件夹上传至客户端
                String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\""
                        + equipRecipePath + File.separator + recipeName + "\" \""
                        + clientRelativeFtpPath + recipeName + "\" \"mput *\"";
                result = iSecsHost.executeCommand(cmd);
                List<RecipePara> recipeParaList =new ArrayList<>();
                recipeParaList = GinRecipeUtil.getRecipePara(clientAbsoluteFtpPath+recipeName+ File.separator+"DeviceData.grd",deviceType);
//                ZipOutputStream zos = null;
                try {
//                    zos = new ZipOutputStream(new FileOutputStream(clientAbsoluteFtpPath + recipeName + ".zip"));
                    /**
                     * 压缩文件夹
                     */
                    ZipUtil.zipByApache(clientAbsoluteFtpPath + recipeName, clientAbsoluteFtpPath + recipeName + ".zip");
//                    ZipUtil.compress(new File(clientAbsoluteFtpPath + recipeName), zos, recipeName, true);
//                    zos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (String str : result) {
                    if (str != null && str.contains("success")) {
                        //TODO 解析recipe,将参数保存至db
                        map.put("recipe", setRecipe(recipeName));
                        map.put("deviceCode", deviceCode);
                        map.put("recipeFTPPath", getHostUploadFtpRecipePath(setRecipe(recipeName)));
                        map.put("recipeParaList", recipeParaList);
                        break;
                    }
                    if (str != null && str.contains("error")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                        map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.debug("设备：" + deviceCode + "上传" + recipeName + ".rcp失败！", e);
            }
        }
        return map;
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        //todo 给个默认值
        String downloadResult = "1";
        String localftpip = GlobalConstants.clientInfo.getClientIp();//工控机ftpip
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String equipRecipePath = this.equipRecipePath;
        String recipeName = recipe.getRecipeName();
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipe.getRecipeName());
        String clientAbsoluteFtpPath = this.getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        //List<String> recipes = getRecipeByNameFromClient(recipeName);
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }

            if (recipe.getVersionType().equalsIgnoreCase("Engineer")) {
                FtpUtil.downloadFile(clientAbsoluteFtpPath + recipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
            } else {
                FtpUtil.downloadFile(clientAbsoluteFtpPath + recipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
                //gold版本
                if (RecipeEdit.hasGoldPara(deviceType)) {
//                    RecipeService recipeService = new RecipeService(sqlSession);
//                    List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipe.getRecipeName(), null, "GOLD");
//                    List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode(recipes.get(0).getId(), null);
//                    FileUtil.renameFile(GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD", GlobalConstants.localRecipePath + GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD");
//                    List list = RecipeEdit.setGoldPara(recipeParas, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/002.DFD", deviceType);
//                    RecipeEdit.writeRecipeFile(list, GlobalConstants.ftpPath + deviceCode + recipe.getRecipeName() + "temp/" + recipeName + ".DFD");
                }
            }
        }
        sqlSession.close();
        //将从serverFtp中下载的recipe解压至temp中
        try {
//            ZipUtil.unZip(clientAbsoluteFtpPath + recipeName + ".zip");
            ZipUtil.unzipByApache(clientAbsoluteFtpPath + recipeName + ".zip", clientAbsoluteFtpPath + recipeName);
        } catch (Exception ex) {
            ex.printStackTrace();
            downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            logger.debug(deviceCode + "==========解压文件失败!", ex);
        }
        //将文件下载至机台
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            //ftp ip user pwd lpath rpath \"mget recipeName.*\"
            String recipePartName = "MOTOR";
            for (int i = 1; i < 21; i++) {
                //机台上创建对应文件
                iSecsHost.executeCommand("dos $md \"" + equipRecipePath + File.separator + recipeName + File.separator + recipePartName + i + "\"$");
//                File tempPartFile = new File(clientAbsoluteFtpPath + recipeName + File.separator + recipePartName + i);
//                if (!tempPartFile.exists()) {
//                    tempPartFile.mkdirs();
//                }
                //上传至指定文件下
                iSecsHost.executeCommand("ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " "
                        + "\"" + equipRecipePath + File.separator + recipeName + File.separator + recipePartName + i + "\" \""
                        + clientRelativeFtpPath + recipeName + File.separator + recipePartName + i + "\" \"mget *\"");
            }
            //将非文件夹上传至客户端
            String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\""
                    + equipRecipePath + File.separator + recipeName + "\" \""
                    + clientRelativeFtpPath + recipeName + "\" \"mget *\"";
            List<String> result = iSecsHost.executeCommand(cmd);
//            String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " \"" + equipRecipePath + File.separator + recipeName + "\" \""
//                    + clientRelativeFtpPath + "temp" + "\" \"mget " + recipeName + ".*\"";
//            result = iSecsHost.executeCommand(cmd);
            for (String str : result) {
                if (str.contains("success")) {
                    downloadResult = "0";
                    break;
                }
                if (str.contains("Error")) {
                    downloadResult = "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
                    break;
                }
            }
        }
        return downloadResult;
    }

    @Override
    public String deleteRecipe(String recipeName) {
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
                    //删除文件及文件夹
                    List<String> result = iSecsHost.executeCommand("dos $rd /s/q \"" + equipLocalRecipePath + "\\" + recipeName + "\"$");
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

    /**
     * 获取机台本地Recipe路径
     *
     * @return
     */
    public String getEquipLocalRecipePath() {
        return equipRecipePath;
    }

    @Override
    public String selectRecipe(String recipeName) {
        List<String> recipeUniqueList = getRecipeList();
        String idx = "";
        for (int i = 0; i < recipeUniqueList.size(); i++) {
            if (recipeName.equals(recipeUniqueList.get(i))) {
                idx = (i + 1) + "";
            }
        }

        if ("".equals(idx)) {
            logger.info("要选中的recipe不存在");
            return "failed";
        }
        String recipeAtLine = "r" + idx + "_sel.txt";
        //选择recipe，recipe有一定的排序规则（ASCII），可以按序号选取（先在机台中模拟数量足够大的recipe列表）
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> selectResultList = iSecsHost.executeCommand("goto select");
                Thread.sleep(500);
                selectResultList = iSecsHost.executeCommand("playback " + recipeAtLine);
                Thread.sleep(300);
                selectResultList = iSecsHost.executeCommand("action ok_sel");
                Thread.sleep(300);
                selectResultList = iSecsHost.executeCommand("playback login.txt");
                Thread.sleep(300);
                selectResultList = iSecsHost.executeCommand("playback confirm_sel.txt");
                for (String s : selectResultList) {
                    if (s.contains("done")) {
                        logger.info("选择recipe:" + recipeName + "成功");
                        return "0";
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                logger.info("选取recipe:" + recipeName + "失败." + e.getMessage());
                return "failed";
            }
        }
        return "failed";
    }

    @Override
    public Map getEquipRealTimeState() {
        equipStatus = getEquipStatus();
        ppExecName = getCurrentRecipeName();
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        map.put("EquipStatus", equipStatus);
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
            List<String> recipeList = getRecipeList();
            eppd.put("eppd", recipeList);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("设备:" + deviceCode + "repipe列表获取失败！" + e.getMessage());
        }
        return eppd;
    }

    @Override
    public String getEquipStatus() {
        System.out.println("开始获取状态");
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> result = iSecsHost.executeCommand("read status");
                if (result != null && !result.isEmpty()) {
                    //TODO 状态需要现场确认
                    String cr = result.get(0);
                    if (!cr.contains("error")) {
                        if ("Stand-By".equals(cr.replace(" ", ""))) {
                            equipStatus = "IDLE";
                        } else if ("".equals(cr)) {
                            equipStatus = "RUN";
                        } else {
                            equipStatus = "ERROR";
                        }
                        equipStatus = result.get(0);
                    } else {
                        //当前页不在主页
                        equipStatus = "SETUP";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("设备：" + deviceCode + "状态获取失败！");
        } finally {
            Map<String, String> map = new HashMap<>();
            map.put("PPExecName", ppExecName);
            map.put("EquipStatus", equipStatus);
            changeEquipPanel(map);
        }
        return equipStatus;
    }

    @Override
    public Object clone() {
        A2255Host newHost = new A2255Host(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newHost.startUp = startUp;
        this.clear();
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
        List<String> alarmStrings = new ArrayList<>();
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> result = iSecsHost.executeCommand("read status");
                for (String flag : result) {
                    if ("ALARM RESET".equalsIgnoreCase(flag)) {
                        logger.info("The equip state changged to alarm...");
                        List<String> alidresult = iSecsHost.executeCommand("read alarmid");
                        if (alidresult.size() > 1) {
                            alarmStrings.add(alidresult.get(0));
                            logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Get EquipAlarm error:" + e.getMessage());
            }
        }
        return alarmStrings;
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

    /**
     * 重写上传至服务端代码
     *
     * @param localRcpPath
     * @param remoteRcpPath
     * @param recipe
     * @return 上传结果
     */
    @Override
    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
        String ftpip = GlobalConstants.ftpIP;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String ftpPort = GlobalConstants.ftpPort;
        //压缩文件后的目录
        String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(recipeName);
        //String clientZipFilePath = GlobalConstants.localRecipePath + GlobalConstants.ftpPath + recipeName ;
        //工控机本地绝对路径
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        if (!FtpUtil.uploadFile(clientAbsoluteFtpPath + recipeName + ".zip", remoteRcpPath, recipeName + "_V" + recipe.getVersionNo() + ".txt", ftpip, ftpPort, ftpUser, ftpPwd)) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传ftp失败,文件名:" + recipeName + ".txt 工控路径:" + clientFtpRecipeAbsolutePath);
            return false;
        }
        UiLogUtil.getInstance().appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + clientAbsoluteFtpPath);
        //this.deleteTempFile(recipeName);
        return true;
    }

    /**
     * 创建并获取recipe上传相对路径
     */
    private String getClientFtpRecipeRelativePath(String recipeName) {
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        DeviceInfoMapper deviceInfoMapper = sqlSession.getMapper(DeviceInfoMapper.class);
//        SysOfficeMapper sysOfficeMapper = sqlSession.getMapper(SysOfficeMapper.class);
//        DeviceInfo deviceInfo = deviceInfoMapper.selectDeviceInfoByDeviceCode(deviceCode);
//        SysOffice sysOffice = sysOfficeMapper.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
//        StringBuilder returnPath = new StringBuilder();
//        returnPath.append("/RECIPE/");
//        returnPath.append(sysOffice.getPlant());
//        returnPath.append("/");
//        returnPath.append(deviceInfo.getDeviceType());
//        returnPath.append("/");
//        returnPath.append(recipeName.replace("/", "@").replace("\\", "@"));
//        returnPath.append("/");
//        File recipeDir = new File(GlobalConstants.localRecipePath + returnPath.toString());
//        if (!recipeDir.exists()) {
//            recipeDir.mkdirs();
//        }
//        return returnPath.toString();
        return GlobalConstants.ftpPath + deviceCode + recipeName + "temp/";
    }

    /**
     * 获取工控机recipe绝对路径
     */
    private String getClientFtpRecipeAbsolutePath(String recipeName) {

        String filePath = GlobalConstants.localRecipePath + "/RECIPE" + getClientFtpRecipeRelativePath(recipeName);
        File recipeDir = new File(filePath);
        if (!recipeDir.exists()) {
            recipeDir.mkdirs();
        }
        return filePath;
    }

    private String getHostDownloadFtpRecipeFilePath(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String downloadFullFilePath = new RecipeService(sqlSession).organizeRecipeDownloadFullFilePath(recipe);
        sqlSession.close();
        return downloadFullFilePath;
    }

    /**
     * 根据recipe名称获取recipe文件
     *
     * @param recipeName
     * @return
     */
    private List<String> getReclipeByName(String recipeName) {
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                List<String> recipeList = iSecsHost.executeCommand("dos $dir " + "\"" + this.equipRecipePath + File.separator + recipeName + ".*" + "\" /b$");
                return recipeList.size() > 1 ? recipeList.subList(0, recipeList.size() - 1) : new ArrayList<String>();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("设备:" + deviceCode + "repipe列表获取失败！" + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 根据recipeName从客户端ftp服务器获取recipe
     *
     * @param recipeName
     * @return
     */
    private List<String> getRecipeByNameFromClient(String recipeName) {
        try {
            List<String> recipeList = new ArrayList<>();
            File file = new File(getClientFtpRecipeAbsolutePath(recipeName));
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.getName().startsWith(recipeName) && !f.getName().endsWith(".zip")) {
                    recipeList.add(f.getName());
                }
            }
            return recipeList;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("设备:" + deviceCode + "repipe列表获取失败！" + e.getMessage());
            /**
             * 为避免NullPointException，不返回null
             */
            return new ArrayList<>();
        }
    }

    /**
     * 获取设备的recipe列表
     *
     * @return
     */
    private List<String> getRecipeList() {
        List<String> result = new ArrayList<>();
        List<String> recipeList = new ArrayList<>();
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                result = iSecsHost.executeCommand("dos $type \"" + equipRecipePath + "\\DeviceTable.grd\"$");
                result = result.subList(0, 20);
                for (int i = 0; i < result.size(); i++) {
                    String recipeName = result.get(i);
                    if (!"".equals(recipeName.replace("\n", "").replace("\r", "").trim())) {
                        recipeList.add(recipeName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("设备：" + deviceCode + "获取recipe列表失败!", e);
        }
        return recipeList;
    }

    /**
     * 检查用户是否已最高权限登录,若没有则登录
     */
    private boolean authUserAndLogin() {
        //TODO suppervisor用户登录的权限问题
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            try {
                List<String> currentUser = iSecsHost.executeCommand("read user");
                if (currentUser != null && !currentUser.isEmpty()) {
                    if (!"HonTech".equals(currentUser.get(0))) {
                        List<String> loginStateResult = iSecsHost.executeCommand("read loginState");
                        /**
                         * 如果不是以HonTech登录，则先退出登录
                         */
                        if (loginStateResult != null && !loginStateResult.isEmpty() && "LogOut".equals(loginStateResult.get(0))) {
                            iSecsHost.executeCommand("playback logout.txt");
                        }
                        List<String> loginResult = iSecsHost.executeCommand("replay login123.exe");
                        if (loginResult != null && !loginResult.isEmpty() && "done".equals(loginResult.get(0))) {
                            /**
                             * 登陆后read uer再次检查
                             */

                            try {
                                Thread.sleep(500);
                                List<String> currentUserAfterLogin = iSecsHost.executeCommand("read user");
                                if (currentUserAfterLogin != null && !currentUserAfterLogin.isEmpty() && "HonTech".equals(currentUserAfterLogin.get(0))) {
                                    logger.debug("用户HonTech登录成功.");
                                    return true;
                                } else {
                                    logger.debug("无法确定当前用户.");
                                    return false;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                logger.debug("确认身份发生错误，请检查连接是否正常." + e);
                                return false;
                            }
                        } else {
                            logger.debug("用户登录失败.");
                            return false;
                        }
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                logger.debug("发生异常，用户登录失败.", e);
                return false;
            }
        }
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
