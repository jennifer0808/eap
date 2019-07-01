package cn.tzauto.octopus.isecsLayer.equipImpl.hontech;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.*;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class HT7045HISIHost extends EquipModel {

    private static final Logger logger = LoggerFactory.getLogger(HT7045HISIHost.class.getName());
    //本地存放recipe的共享文件夹
    private static final String recipeFileSrcPath = "D:\\TEST_RECIPE\\test\\HT7045_Recipe\\";
    String equipLocalRecipeLogPath = "D:\\newtesthander\\BackUp\\LastData\\";
    //    private static Socket findWindowssocket = null;
    private ISecsHost plcCtrlHost;

    public HT7045HISIHost(String devId, String remoteIpAddress, int remoteTcpPort, String deviceType, String iconPath, String equipRecipePath) {
        super(devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath);
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, devId);
    }

    @Override
    public void initialize() {
        iSecsHost = null;
        iSecsHost = new ISecsHost(remoteIPAddress, String.valueOf(remoteTCPPort), deviceType, deviceCode);
        plcCtrlHost = new ISecsHost(remoteIPAddress, String.valueOf(12005), deviceType, deviceCode);
        iSecsHostList.clear();
        iSecsHostList.add(iSecsHost);
        iSecsHostList.add(plcCtrlHost);
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
        BufferedReader br = null;
        try {
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                List<String> recipeNameResult = iSecsHost.executeCommand("read recipeName");
//                if (recipeNameResult != null && !recipeNameResult.isEmpty()) {
//                    ppExecName = recipeNameResult.get(0);
//                }
                //读取文件内容获取当前recipe的名称
//                List<String> recipeNameResult = iSecsHost.executeCommand("dos $findstr \".BLD\" D:\\newtesthander\\system\\lastdata.dat$");
                List<String> recipeNameResult = iSecsHost.executeCommand("dos $type  D:\\newtesthander\\BackUp\\LastData\\lastdata_backup.dat$");

                if (recipeNameResult != null && !recipeNameResult.isEmpty()) {
                    if (!recipeNameResult.get(0).contains(".BLD")) {
                        //上传文件
                        String localLogPath = "D:\\RECIPE" + GlobalConstants.ftpPath + "recipelog/" + deviceCode + "/";
                        File file = new File(localLogPath);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        String command = "ftp " + GlobalConstants.clientInfo.getClientIp() + " " + GlobalConstants.ftpUser + " " + GlobalConstants.ftpPwd + " " + "\"" + equipLocalRecipeLogPath + "\" \"" + localLogPath + "\" \"mput lastdata_backup.dat\"";
                        List<String> result = iSecsHost.executeCommand(command);
                        File recipelogFile = new File(localLogPath + "lastdata_backup.dat");
                        br = new BufferedReader(new FileReader(recipelogFile));
                        String line = "";
                        while ((line = br.readLine()) != null) {
                            if (line.contains(".BLD")) {
                                ppExecName = line.split(".BLD")[0];
                                break;
                            }
                        }
                    } else {
                        ppExecName = recipeNameResult.get(0).split(".BLD")[0];
                    }
                }
//                //上传文件
//                String localLogPath = GlobalConstants.ftpPath + "/recipelog/" + deviceCode + "/";
//                File file = new File(localLogPath);
//                if (!file.exists()) {
//                    file.mkdirs();
//                }
//                String command = "ftp " + GlobalConstants.clientInfo.getClientIp() + GlobalConstants.ftpUser + " " + GlobalConstants.ftpPwd + " " + "\"" + equipLocalRecipeLogPath + "\" \"" + localLogPath + "\" \"mput lastdata_backup.dat\"";
//                List<String> result = iSecsHost.executeCommand(command);
//                File recipelogFile = new File(localLogPath + "lastdata_backup.dat");
//                br = new BufferedReader(new FileReader(recipelogFile));
//                String line = "";
//                while ((line = br.readLine()) != null) {
//                    if (line.contains(".BLD")) {
//                        ppExecName = line.split(".BLD")[0];
//                        break;
//                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("设备：" + deviceCode + "获取recipe失败！" + e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Map<String, String> map = new HashMap<>();
        map.put("PPExecName", ppExecName);
        changeEquipPanel(map);
        return ppExecName;
    }


    /**
     * @return
     */
    public String startEquip() {
        try {
            synchronized (plcCtrlHost) {
//                String result = SocketUtil.doCommanAndGetReply(findWindowssocket, "START");
                List<String> result = plcCtrlHost.executePLCCommand("START");
                if ("Y".equalsIgnoreCase(result.get(0))) {
                    return "0";
                } else if ("ResetFail".equalsIgnoreCase(result.get(0))) {
                    logger.info("{}:PLC设备已断线，重连未果！", deviceCode);
                    return "ResetFail";
                } else {
                    logger.info("{}:当前设备发送START指令开始失败！", deviceCode);
                    return "1";
                }
            }
        } catch (Exception e) {
            logger.error("", e);
            return "1";
        } finally {

        }
    }

    @Override
    public String pauseEquip() {
        return null;
    }

    @Override
    public String stopEquip() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
            sqlSession.close();
            if ("Y".equals(deviceInfoExt.getLockSwitch())) {
//                String result = SocketUtil.doCommanAndGetReply(findWindowssocket, "STOP");
                List<String> result = plcCtrlHost.executePLCCommand("STOP");
                if ("Y".equalsIgnoreCase(result.get(0))) {
                    return "0";
                } else {
                    logger.info("{}:当前设备锁机失败！", deviceCode);
                    return "1";
                }
            } else {
                logger.info("{}:当前设备未设置锁机！", deviceCode);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, ":当前设备未设置锁机！");
                return "1";
            }
        } catch (Exception e) {
            logger.error("", e);
            return "1";
        } finally {

        }
    }

    @Override
    public String lockEquip() {
        return null;
    }

    //    @Override
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
        //List<String> recipes = getReclipeByName(recipeName);
        //先将原有文件放入D:\RECIPE\RECIPE\E3200-033399999999temp\temp
        File tempFile = new File(recipeFileSrcPath + this.deviceCode);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
//        String cmd = "ftp " + localftpip + " " + ftpUser + " " + ftpPwd + " " + "\"" + equipRecipePath + "\" \"" + clientRelativeFtpPath + "temp" + "\" \"mput " + recipeName + ".*\"";
        //先将文件上传至共享文件夹中
        if (this.equipStatus.contains("RUN") || this.equipStatus.contains("Run")) {
            map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,机台正在运行！.");
            return map;
        }
        /**
         * 检查用户是否已最高权限登录,若没有则登录
         */
        boolean login = authUserAndLogin();
        if (!login) {
            logger.debug("无法以最高权限登录，不能选择recipe。");
            map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,无法登录");
            return map;
        }
        //从共享文件夹中获取recipe文件

        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {

            List<String> result = null;
            try {
//                result = iSecsHost.executeCommand("goto any");
//                result = iSecsHost.executeCommand("action gotoftphd");
                result = iSecsHost.executeCommand("playback clickftp.txt");
                result = iSecsHost.executeCommand("playback clickhd.txt");
                result = iSecsHost.executeCommand("goto ftphd");
                Thread.sleep(1500);
                result = iSecsHost.executeCommand("write ftphdrecipe " + recipeName);
//                result = iSecsHost.executeCommand("goto ftphd");
//                result = iSecsHost.executeCommand("action ftphdr1");
                result = iSecsHost.executeCommand("playback clickr1.txt");
                result = iSecsHost.executeCommand("playback clickr1.txt");
//                result = iSecsHost.executeCommand("goto ftphd");
//                result = iSecsHost.executeCommand("action upload");
                result = iSecsHost.executeCommand("playback uploadrecipe.txt");
                Date oldDate1 = new Date();
                Date newDate1 = null;
                while (true) {
                    newDate1 = new Date();
                    logger.info(newDate1.getTime() - oldDate1.getTime() + "检测时间为：");
                    if ((newDate1.getTime() - oldDate1.getTime()) < (4 * 1000)) {
                        //间隔0.5s
                        Thread.sleep(1000);
                        //读取弹出窗的标识“OK”
                        result = iSecsHost.executeCommand("read okflag");
                        if ("Message".equals(result.get(0))) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
//                result = iSecsHost.executeCommand("goto ftphd");
//                result = iSecsHost.executeCommand("action ftphdok");
                result = iSecsHost.executeCommand("playback clickftphdok.txt");
                //从本地共享文件中获取对应recipe
                int smbDownloadResult = -1;
//                smbDownloadResult = SMBFileUtil.smbDownload(clientAbsoluteFtpPath + recipeName, "smb://192.168.127.133/testF/", recipeName + ".zip", "192.168.127.133", "WDF2", "1234");

//                File file = new File(clientAbsoluteFtpPath +recipeName+".zip");

                //解压
//                File srcFile = new File(clientAbsoluteFtpPath  +recipeName+".zip");
//                ZipUtil.unZip(srcFile, clientAbsoluteFtpPath + "\\temp");
//                FileUtil.deleteAllFilesOfDir(new File(clientAbsoluteFtpPath + "\\temp"));

                //下载recipe在获取参数
                Thread.sleep(1000);
                if (!recipeName.equals(getCurrentRecipeName())) {
                    result = iSecsHost.executeCommand("playback clickftp.txt");
                    result = iSecsHost.executeCommand("playback clickserver.txt");
                    result = iSecsHost.executeCommand("goto ftpsvr");
                    Thread.sleep(1800);
                    result = iSecsHost.executeCommand("write ftpsvrrecipe " + recipeName);
                    result = iSecsHost.executeCommand("playback clickr1.txt");
                    result = iSecsHost.executeCommand("playback clickr1.txt");
                    result = iSecsHost.executeCommand("playback selectrecipe.txt");

                    Date oldDate = new Date();
                    Date newDate = null;
                    //region 查询窗口弹出
                    while (true) {
                        newDate = new Date();
                        logger.info(newDate1.getTime() - oldDate1.getTime() + "检测时间为：");
                        if ((newDate.getTime() - oldDate.getTime()) < (7 * 1000)) {
                            //间隔0.5s
                            Thread.sleep(1000);
                            //读取弹出窗的标识“OK”
                            result = iSecsHost.executeCommand("read okflag");
                            if ("Message".equals(result.get(0))) {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
//                    Thread.sleep(6000);
                    result = iSecsHost.executeCommand("playback clickftpsvrok.txt");
                    Thread.sleep(2500);
                }
            } catch (Exception e) {
                logger.debug("", e);
                result.clear();
                result.add("error");
            }
            try {
                FileUtil.copyFile(recipeFileSrcPath + this.deviceCode + "\\" + recipeName + ".zip", clientAbsoluteFtpPath + recipeName + ".zip");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //TODO 获取机台界面的参数
            List<RecipePara> recipeParaList = null;
//          recipeParaList = HT7045RecipeUtil.transferRcpFromDB(clientAbsoluteFtpPath + "temp\\" + recipeName + ".temp", this.deviceType);
            recipeParaList = getRecipeParaFromScrren(recipeName);
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logout();

            if (new File(clientAbsoluteFtpPath + recipeName + ".zip").exists()) {
                //TODO 解析recipe,将参数保存至db
                map.put("recipe", setRecipe(recipeName));
                map.put("deviceCode", deviceCode);
                map.put("recipeFTPPath", getHostUploadFtpRecipePath(setRecipe(recipeName)));
                map.put("recipeParaList", recipeParaList);
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
            }
//            for (String str : result) {
//                if (str != null && str.contains("done")) {
//                    //TODO 解析recipe,将D参数保存至db
//                    map.put("recipe", setRecipe(recipeName));
//                    map.put("deviceCode", deviceCode);
//                    map.put("recipeFTPPath", getHostUploadFtpRecipePath(setRecipe(recipeName)));
//                    map.put("recipeParaList", recipeParaList);
//                    break;
//                }
//                if (str != null && str.contains("error")) {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
//                    map.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
//                    break;
//                }
//            }
        }
        return map;
    }

    public Map uploadRecipe2(String recipeName) {
        Map<String, Object> map = new HashMap<>();
        getRecipeParaFromScrren(recipeName);
        return map;
    }

    /**
     * 通过ocr点击至对应界面读取参数，上传recipe时使用
     *
     * @param recipeName
     * @return
     */
    public List<RecipePara> getRecipeParaFromScrren(String recipeName) {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        Map<String, String> paraMap = new HashMap();
        boolean login = authUserAndLogin();
        if (!login) {
            logger.debug("无法以最高权限登录，不能选择recipe。");
            return null;
        }
        try {
            //TODO 抓取测试
            List<String> result = iSecsHost.executeCommand("playback clicktools.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("playback clicktester.txt");
            /**
             *获取sitemap中的参数
             */
            result = iSecsHost.executeCommand("playback clicksitemap.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"sitemap".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen sitemap");
            Map<String, String> sitemapMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
            paraMap.putAll(sitemapMap);

            /**
             *获取bin1-8中的参数
             */
            result = iSecsHost.executeCommand("playback clickalarm1.txt");
            result = iSecsHost.executeCommand("playback clickftmode.txt");
            result = iSecsHost.executeCommand("playback clickalarm1bin1-8.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"alarm1bin1-8".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen alarm1bin1-8");
            Map<String, String> a11_8Map = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
            paraMap.putAll(a11_8Map);
            /**
             * 获取bin9-15中的参数
             */
            result = iSecsHost.executeCommand("playback clickalarm1bin9-15.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"alarm1bin9-15".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen alarm1bin9-15");
            Map<String, String> a19_15Map = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
            paraMap.putAll(a19_15Map);
            /**
             *
             */
            result = iSecsHost.executeCommand("playback clickalarm2.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"alarm2".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen alarm2");
            Map<String, String> a2Map = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
            paraMap.putAll(a2Map);
            /**
             * 获取RT数据
             */
            result = iSecsHost.executeCommand("playback clickalarm1.txt");
            result = iSecsHost.executeCommand("playback clickrtmode.txt");
            result = iSecsHost.executeCommand("playback clickalarm1bin1-8.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"alarm1bin1-8".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen alarm1bin1-8");
            Map<String, String> a11_8MapRT = getRTMap((HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class));
            paraMap.putAll(a11_8MapRT);
            /**
             * 获取
             */
            result = iSecsHost.executeCommand("playback clickalarm1bin9-15.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"alarm1bin9-15".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen alarm1bin9-15");
            Map<String, String> a19_15MapRT = getRTMap((HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class));
            paraMap.putAll(a19_15MapRT);
            /**
             *
             */
            result = iSecsHost.executeCommand("playback clickalarm2.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"alarm2".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen alarm2");
            Map<String, String> a2MapRT = getRTMap((HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class));
            paraMap.putAll(a2MapRT);
            /**
             *
             */
            result = iSecsHost.executeCommand("playback clickbin.txt");
            Thread.sleep(100);
            result = iSecsHost.executeCommand("curscreen");
            if (!"bin".equals(result.get(0))) {
                result = iSecsHost.executeCommand("playback clickexittool.txt");
                return recipeParas;
            }
            result = iSecsHost.executeCommand("readbyscreen bin");
            Map<String, String> binMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
            paraMap.putAll(binMap);
            result = iSecsHost.executeCommand("playback clickexittool.txt");
            //读取文件中的trayName
//            String trayName = this.getTrayNameFromZIP(getClientFtpRecipeAbsolutePath(recipeName) + recipeName + ".zip").trim();
//            if (!"".equalsIgnoreCase(trayName)) {
            result = plcCtrlHost.executeCommand("gettray " + recipeName);
            Map<String, String> trayMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
            paraMap.putAll(trayMap);
//            }

        } catch (Exception e) {
            logger.error("", e);
        }
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            RecipePara recipePara = new RecipePara();
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                String paraCode = recipeTemplate.getParaCode().trim();
                if (entry.getKey().equals(paraCode)) {
                    recipePara.setParaCode(paraCode);
                    recipePara.setParaName(recipeTemplate.getParaName());
                    recipePara.setSetValue(entry.getValue());
                    recipeParas.add(recipePara);
                    break;
                }
            }
        }
        return recipeParas;
    }

    /**
     * 通过PLC控制软件弹出对应窗口，读取界面参数，用于开机check时读取参数
     *
     * @return
     */
    public List<RecipePara> getRecipeParasFromMonitorMap() {
        //todo 远程指令跳转界面

        List<RecipePara> recipeParas = new ArrayList<>();
        List<String> result = null;
        Map<String, String> paraMap = new HashMap();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();

        //发送远程指令跳转界面
        try {
//            boolean login = authUserAndLogin();
//        if (!login) {
//            logger.debug("无法以最高权限登录，不能选择recipe。");
//            return null;
//        }

            //TODO 抓取测试
            synchronized (plcCtrlHost) {
                plcCtrlHost.executeCommand("SaveState");
                iSecsHost.executeCommand("playback clickmain.txt");
                result = iSecsHost.executeCommand("read testmode");
                /**
                 * 读取bin参数
                 */
                Map<String, String> binMap = getScrrenParaByPlc("ToPara1", "bin");
                paraMap.putAll(binMap);
                /**
                 * 读取sitemap参数
                 */
                Map<String, String> sitemapMap = getScrrenParaByPlc("ToPara5", "sitemap");
                paraMap.putAll(sitemapMap);
                if ("RT".equalsIgnoreCase(result.get(0))) {

                    /**
                     * alarm1 topara2,3
                     */
                    Map<String, String> a11_8Map = getScrrenParaByPlc("ToPara2 RT", "alarm1bin1-8");
                    paraMap.putAll(getRTMap(a11_8Map));
                    /**
                     * 获取
                     */
                    Map<String, String> a19_15Map = getScrrenParaByPlc("ToPara3 RT", "alarm1bin9-15");
                    paraMap.putAll(getRTMap(a19_15Map));
                    /**
                     *  alarm2 topara4
                     */
                    Map<String, String> a2Map = getScrrenParaByPlc("ToPara4 RT", "alarm2");
                    paraMap.putAll(getRTMap(a2Map));
                } else {
                    /**
                     * alarm1 topara2,3
                     */
                    Map<String, String> a11_8Map = getScrrenParaByPlc("ToPara2", "alarm1bin1-8");
                    paraMap.putAll(a11_8Map);
                    /**
                     * 获取
                     */
                    Map<String, String> a19_15Map = getScrrenParaByPlc("ToPara3", "alarm1bin9-15");
                    paraMap.putAll(a19_15Map);
                    /**
                     *  alarm2 topara4
                     */
                    Map<String, String> a2Map = getScrrenParaByPlc("ToPara4", "alarm2");
                    paraMap.putAll(a2Map);
                }
//                String clientAbsoluteFtpPath = getClientFtpRecipeAbsolutePath(ppExecName);
//                String cmd = "ftp " + GlobalConstants.clientInfo.getClientIp() + " "
//                        + GlobalConstants.ftpUser + " "
//                        + GlobalConstants.ftpPwd + " "
//                        + "\"" + equipRecipePath + "\" \""
//                        + getClientFtpRecipeRelativePath(ppExecName)
//                        + "\" \"mput " + ppExecName + ".TRY\"";
//                iSecsHost.executeCommand(cmd);
//                String trayName = getTrayNameFromTRY(clientAbsoluteFtpPath + ppExecName + ".TRY").trim();
//                if (!"".equalsIgnoreCase(trayName)) {
//                    result = plcCtrlHost.executeCommand("gettray " + trayName);
//                    Map<String, String> trayMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
//                    paraMap.putAll(trayMap);
//                }
                result = plcCtrlHost.executeCommand("gettray " + ppExecName);
                Map<String, String> trayMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
                paraMap.putAll(trayMap);
            }
//            return recipeParas;
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                RecipePara recipePara = new RecipePara();
                if (paraMap.get(recipeTemplate.getParaCode()) != null) {
                    recipePara.setParaCode(recipeTemplate.getParaCode());
                    recipePara.setParaName(recipeTemplate.getParaName());
                    recipePara.setSetValue(paraMap.get(recipeTemplate.getParaCode()));
                    recipeParas.add(recipePara);
                }
            }
            plcCtrlHost.executeCommand("RestoreState");
            return recipeParas;
        }

    }

    public String getTrayNameFromZIP(String recipeTrayPath) {
        File file = new File(recipeTrayPath);
        StringBuilder trayName = new StringBuilder();
        if (file.exists()) {
            InputStream is = null;
            ZipArchiveInputStream zais = null;
            try {
                is = new FileInputStream(file);
                zais = new ZipArchiveInputStream(is);
                ArchiveEntry archiveEntry = null;
                while ((archiveEntry = zais.getNextEntry()) != null) {
                    // 获取文件名
                    String entryFileName = archiveEntry.getName();
                    if (entryFileName.endsWith(".TRY")) {
                        byte[] buffer = new byte[8];
                        int len = -1;
                        int i = 0;
                        while ((len = zais.read(buffer)) != -1) {
                            i++;
                            if (i > 9) {
                                break;
                            } else if (i < 5) {
                                continue;
                            }
                            trayName.append(new String(buffer));
//                            System.out.println(new String(buffer));
                        }
                    } else {
                        continue;
                    }
                }
                String trayNametemp = trayName.toString().trim();
                trayName = new StringBuilder(trayName.toString().trim());
                trayName = new StringBuilder(trayName.substring(trayNametemp.lastIndexOf("\0") + 1));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (zais != null) {
                        zais.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return trayName.toString();
            }
        }
        return trayName.toString();
    }

    public static String getTrayNameFromTRY(String recipeTrayPath) {
        File file = new File(recipeTrayPath);
        StringBuilder trayName = new StringBuilder();
        if (file.exists()) {
            FileReader fr = null;
            try {
                fr = new FileReader(file);
                char[] buffer = new char[8];
                int len = -1;
                int i = 0;
                while ((len = fr.read(buffer)) != -1) {
                    i++;
                    if (i > 9) {
                        break;
                    } else if (i < 5) {
                        continue;
                    }
                    trayName.append(buffer);
//                            System.out.println(new String(buffer));
                }
                String trayNametemp = trayName.toString().trim();
                trayName = new StringBuilder(trayName.toString().trim());
                trayName = new StringBuilder(trayName.substring(trayNametemp.lastIndexOf("\0") + 1));
            } catch (Exception e) {
                logger.error("", e);
            } finally {
                try {
                    if (fr != null) {
                        fr.close();
                    }
                } catch (IOException e) {
                    logger.error("", e);
                }
                return trayName.toString();
            }
        }
        return trayName.toString();
    }


    public Map<String, String> getRTMap(Map<String, String> map) {
        Map<String, String> rtMap = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            rtMap.put(entry.getKey() + "RT", entry.getValue());
        }
        return rtMap;
    }

    public void refreshParaFromScrren() {
        List<String> result;
        try {
            Date oldDate = new Date();
            Date newDate = null;
            //region 查询窗口弹出
            while (true) {
                newDate = new Date();
                logger.info((newDate.getTime() - oldDate.getTime()) + "，检测时间为：");
                if ((newDate.getTime() - oldDate.getTime()) < (5 * 1000)) {
                    //间隔0.5s
                    Thread.sleep(500);
                    //读取弹出窗的标识“OK”
                    result = iSecsHost.executeCommand("read user");
                    if ("HonTech".equals(result.get(0))) {
                        break;
                    }
                    result = iSecsHost.executeCommand("read okflag");
                    if ("Message".equals(result.get(0))) {
                        result = iSecsHost.executeCommand("playback clickftpsvrok.txt");
                        break;
                    }
                } else {
                    break;
                }
            }
            boolean login = authUserAndLogin();
            if (!login) {
                logger.debug("无法以最高权限登录，不能选择recipe。");
                return;
            }
            //TODO 抓取测试

            result = iSecsHost.executeCommand("playback clicktools.txt");
            result = iSecsHost.executeCommand("playback clicktester.txt");
            result = iSecsHost.executeCommand("playback clickalarm1.txt");
            result = iSecsHost.executeCommand("playback clickftmode.txt");
            result = iSecsHost.executeCommand("playback clickalarm1bin1-8.txt");
            result = iSecsHost.executeCommand("playback clickalarm1bin9-15.txt");
            result = iSecsHost.executeCommand("playback clickalarm2.txt");
            /**
             * 开始点击RT的界面参数
             */
            result = iSecsHost.executeCommand("playback clickalarm1.txt");
            result = iSecsHost.executeCommand("playback clickrtmode.txt");
            result = iSecsHost.executeCommand("playback clickalarm1bin1-8.txt");
            result = iSecsHost.executeCommand("playback clickalarm1bin9-15.txt");
            result = iSecsHost.executeCommand("playback clickalarm2.txt");
            result = iSecsHost.executeCommand("playback clickexittool.txt");
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            result = iSecsHost.executeCommand("playback clickexittool.txt");
        }
    }

    @Override
    public String downloadRecipe(Recipe recipe) {
        String downloadResult = "1";
        String localftpip = GlobalConstants.clientInfo.getClientIp();//工控机ftpip
        String ftpip = GlobalConstants.ftpIP;
        String ftpPort = GlobalConstants.ftpPort;
        String ftpUser = GlobalConstants.ftpUser;
        String ftpPwd = GlobalConstants.ftpPwd;
        String equipRecipePath = this.equipRecipePath;
        String recipeName = recipe.getRecipeName();
        String clientRelativeFtpPath = getClientFtpRecipeRelativePath(recipe.getRecipeName());
        String clientFtpRecipeAbsolutePath = this.getClientFtpRecipeAbsolutePath(recipeName);
        String hostDownloadFtpRecipeFilePath = this.getHostDownloadFtpRecipeFilePath(recipe);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        //List<String> recipes = getRecipeByNameFromClient(recipeName);
        if (!GlobalConstants.isLocalMode) {
            if (!FtpUtil.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd)) {
                return "下载Recipe:" + recipe.getRecipeName() + "时,FTP连接失败,请检查FTP服务是否开启.";
            }
            File file = new File(clientFtpRecipeAbsolutePath + recipeName + ".zip");
            if (file.exists()) {
                file.delete();
                logger.info("Delete Host Local Old Recipe:{}", recipeName);
                logger.info("Local Old RecipePath:{}", clientFtpRecipeAbsolutePath + recipeName + ".zip");
            }
            FtpUtil.downloadFile(clientFtpRecipeAbsolutePath + recipeName + ".zip", hostDownloadFtpRecipeFilePath, ftpip, ftpPort, ftpUser, ftpPwd);
        }
        sqlSession.close();
        //将从serverFtp中下载的recipe put到机台共享文件中
//        int smbUploadResult = -1;
        try {
//            smbUploadResult = SMBFileUtil.smbUploading(clientFtpRecipeAbsolutePath + recipeName + ".zip", "smb://192.168.127.133/testF/", "192.168.127.133", "WDF2", "1234");
//            if (smbUploadResult != 0) {
//                downloadResult = "上传Recipe:[" + recipe.getRecipeName() + "]至共享文件失败！";
//            }
//            File file = new File (recipeFileSrcPath+recipe+".zip");
            File tempFile = new File(recipeFileSrcPath + this.deviceCode);
            if (!tempFile.exists()) {
                tempFile.mkdirs();
            }
            File equipfile = new File(recipeFileSrcPath + this.deviceCode + "\\" + recipeName + ".zip");
            if (equipfile.exists()) {
                equipfile.delete();
                logger.info("Delete Equip FTP Old Recipe:{}", recipeName);
                logger.info("Equip FTP Old RecipePath:{}", recipeFileSrcPath + this.deviceCode + "\\" + recipeName + ".zip");
            }
            FileUtil.copyFile(clientFtpRecipeAbsolutePath + recipeName + ".zip", recipeFileSrcPath + this.deviceCode + "\\" + recipeName + ".zip");
            File file = new File(recipeFileSrcPath + this.deviceCode + "\\" + recipeName + ".zip");
            if (file.exists()) {
                downloadResult = "0";

            }
        } catch (Exception e) {
            logger.debug(deviceCode + "==========下载至机台FTP失败!", e);
            return "下载Recipe:" + recipe.getRecipeName() + "时,下载至机台FTP失败";
        }
        //不需要删除
//        this.deleteTempFile(recipeName);
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
                } else if (recipeName.equals(this.ppExecName)) {
                    logger.info("Recipe:[" + recipeName + "]为当前程序,无需删除");
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
        try {
            /**
             * 检查用户是否已最高权限登录,若没有则登录
             */
            boolean login = authUserAndLogin();
            if (!login) {
                logger.debug("无法以最高权限登录，不能选择recipe。");
                return "登录权限失败，请将机台界面退回至主界面！";
            }
            List<String> result = null;
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//                result = iSecsHost.executeCommand("goto any");
//                result = iSecsHost.executeCommand("action gotoftphd");
                result = iSecsHost.executeCommand("playback clickftp.txt");
                result = iSecsHost.executeCommand("playback clickserver.txt");

//                Thread.sleep(2000);
                Date oldDate1 = new Date();
                Date newDate1 = null;
                //region 查询窗口弹出
                while (true) {
                    newDate1 = new Date();
                    logger.info((newDate1.getTime() - oldDate1.getTime()) + "，检测时间为：");
                    if ((newDate1.getTime() - oldDate1.getTime()) < (7 * 1000)) {
                        //间隔0.5s
                        Thread.sleep(500);
                        //读取弹出窗的标识“OK”
                        result = iSecsHost.executeCommand("curscreen");
                        if ("ftpsvr".equalsIgnoreCase(result.get(0))) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                result = iSecsHost.executeCommand("goto ftpsvr");
                result = iSecsHost.executeCommand("write ftpsvrrecipe " + recipeName);
                result = iSecsHost.executeCommand("playback clickr1.txt");
                result = iSecsHost.executeCommand("playback clickr1.txt");
                result = iSecsHost.executeCommand("playback selectrecipe.txt");
                Date oldDate = new Date();
                Date newDate = null;
                //region 查询窗口弹出
                while (true) {
                    newDate = new Date();
                    logger.info((newDate.getTime() - oldDate.getTime()) + "，检测时间为：");
                    if ((newDate.getTime() - oldDate.getTime()) < (12 * 1000)) {
                        //间隔0.5s
                        Thread.sleep(500);
                        //读取弹出窗的标识“OK”
                        result = iSecsHost.executeCommand("read okflag");
                        if ("Message".equals(result.get(0))) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                result = iSecsHost.executeCommand("playback clickftpsvrok.txt");
                Thread.sleep(500);
                Thread refreshPara = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        refreshParaFromScrren();
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        iSecsHost.executeCommand("playback logout.txt");
                    }
                });
                refreshPara.start();
                ppExecName = getCurrentRecipeName();
                if (recipeName.equals(ppExecName)) {
                    return "0";
                } else {
                    return "failed";
                }
            }
        } catch (Exception e) {
            logger.debug("", e);
            return "failed";
        }
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

//    public List<RecipePara> getRecipeParasFromMonitorMap() {
//        //todo 远程指令跳转界面
//
//        List<RecipePara> recipeParas = new ArrayList<>();
//        List<String> result = null;
//        Map<String, String> paraMap = new HashMap();
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
//        sqlSession.close();
//
//        //发送远程指令跳转界面
//        try {
////            boolean login = authUserAndLogin();
////        if (!login) {
////            logger.debug("无法以最高权限登录，不能选择recipe。");
////            return null;
////        }
//
//            //TODO 抓取测试
//            synchronized (findWindowssocket) {
//                SocketUtil.doCommanAndGetReply(findWindowssocket, "SaveState");
//                /**
//                 *to bin
//                 */
////            result = iSecsHost.executeCommand("playback clickbin.txt");
//                SocketUtil.doCommanAndGetReply(findWindowssocket, "ToPara1");
//                Thread.sleep(200);
//                result = iSecsHost.executeCommand("curscreen");
//                if (!"bin".equals(result.get(0))) {
//                    SocketUtil.doCommanAndGetReply(findWindowssocket, "RestoreState");
//                    return recipeParas;
//                }
//                result = iSecsHost.executeCommand("readbyscreen bin");
//                Map<String, String> binMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
//                paraMap.putAll(binMap);
//
//                /**
//                 * alarm1 topara2,3
//                 */
//                SocketUtil.doCommanAndGetReply(findWindowssocket, "ToPara2");
//                Thread.sleep(300);
//                result = iSecsHost.executeCommand("curscreen");
//                if (!"alarm1bin1-8".equals(result.get(0))) {
//                    SocketUtil.doCommanAndGetReply(findWindowssocket, "RestoreState");
//                    return recipeParas;
//                }
//                result = iSecsHost.executeCommand("readbyscreen alarm1bin1-8");
//                Map<String, String> a11_8Map = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
//                paraMap.putAll(a11_8Map);
//
//                /**
//                 * 获取
//                 */
//                SocketUtil.doCommanAndGetReply(findWindowssocket, "ToPara3");
//                Thread.sleep(300);
//                result = iSecsHost.executeCommand("curscreen");
//                if (!"alarm1bin9-15".equals(result.get(0))) {
//                    SocketUtil.doCommanAndGetReply(findWindowssocket, "RestoreState");
//                    return recipeParas;
//                }
//                result = iSecsHost.executeCommand("readbyscreen alarm1bin9-15");
//                Map<String, String> a19_15Map = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
//                paraMap.putAll(a19_15Map);
//                /**
//                 *  alarm2 topara2
//                 */
//                SocketUtil.doCommanAndGetReply(findWindowssocket, "ToPara4");
//                Thread.sleep(300);
//                result = iSecsHost.executeCommand("curscreen");
//                if (!"alarm2".equals(result.get(0))) {
//                    SocketUtil.doCommanAndGetReply(findWindowssocket, "RestoreState");
//                    return recipeParas;
//                }
//                result = iSecsHost.executeCommand("readbyscreen alarm2");
//                Map<String, String> a2Map = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
//                paraMap.putAll(a2Map);
//            }
//            return recipeParas;
//        } catch (Exception e) {
//            logger.error("", e);
//        } finally {
//            for (Map.Entry<String, String> entry : paraMap.entrySet()) {
//                RecipePara recipePara = new RecipePara();
//                for (RecipeTemplate recipeTemplate : recipeTemplates) {
//                    String paraCode = recipeTemplate.getParaCode().trim();
//                    if (entry.getKey().equals(paraCode)) {
//                        recipePara.setParaCode(paraCode);
//                        recipePara.setParaName(recipeTemplate.getParaName());
//                        recipePara.setSetValue(entry.getValue());
//                        recipeParas.add(recipePara);
//                        break;
//                    }
//                }
//            }
//            SocketUtil.doCommanAndGetReply(findWindowssocket, "RestoreState");
////            result = iSecsHost.executeCommand("playback overcheck.txt");
//        }
//        return recipeParas;
//
////        return getRecipeParaFromScrren();
//    }


    public Map<String, String> getScrrenParaByPlc(String ctrlCommand, String ocrCommand) throws InterruptedException {
        List<String> result = null;
        Map<String, String> resultMap = new HashMap<>();
        List<String> resultPlc = plcCtrlHost.executeCommand(ctrlCommand);
        Thread.sleep(130);
        result = iSecsHost.executeCommand("curscreen");
        if ((!ocrCommand.equals(result.get(0))) && (!"Y".equals(resultPlc.get(0)))) {
//            plcCtrlHost.executeCommand("RestoreState");
            return resultMap;
        }
        result = iSecsHost.executeCommand("readbyscreen " + ocrCommand);
        resultMap = (HashMap<String, String>) JsonMapper.fromJsonString(result.get(0), Map.class);
        return resultMap;
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
                    String cr = result.get(0);
                    if (!cr.contains("error") && !cr.equals("done")) {
//                        if ("HALT".equals(cr.replace(" ", ""))) {
//                            equipStatus = "IDLE";
//                        } else if("RUNNING".equals(cr)) {
//                            equipStatus = "RUN";
//                        }else{
//                            equipStatus = "ERROR";
//                        }
                        equipStatus = cr;
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
        HT7045HISIHost newHost = new HT7045HISIHost(deviceId, remoteIPAddress, remoteTCPPort, deviceType, iconPath, equipRecipePath);
        newHost.startUp = startUp;
        this.clear();
        return newHost;
    }

    @Override
    public void run() {
//        findWindowssocket = SocketUtil.connectToClient(this.remoteIPAddress, 12005);
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
                List<String> result = iSecsHost.executeCommand("read alarmflag ");
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
//                for (String colorstr : result) {
//                    if ("0xc0c0c0".equals(colorstr)) {
//                        alarmStrings.add("");
//                    }
//                    if ("0xff0000".equals(colorstr)) {
//                        logger.info("The equip state changged to alarm...");
//                        List<String> alidresult = iSecsHost.executeCommand("read alarmid");
//                        if (alidresult.size() > 1) {
//                            alarmStrings.add(alidresult.get(0));
//                            logger.info("Get alarm ALID=[" + alidresult.get(0) + "]");
//                        }
//                    }
//                }
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
        this.deleteTempFile(recipeName);
        return true;
    }

    /**
     * 创建并获取recipe上传相对路径
     */
    private String getClientFtpRecipeRelativePath(String recipeName) {
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
        List<String> uniqueList = new ArrayList<>();
        Map<String, Integer> map = new HashMap<>();
        try {
            List<String> recipeList = new ArrayList<>();
            synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
                recipeList = iSecsHost.executeCommand("dos $dir " + "\""
                        + this.equipRecipePath + File.separator + "*.BLD" + "\" /b$");
            }
//                for (String recipe : recipeList) {
//                    if (!"done".equals(recipe)||!"".equals(recipe)) {
//                        if (recipe.contains(".") && !recipe.startsWith(".")
//                                && !recipe.startsWith("+") && !recipe.endsWith(".txt")
//                                && !recipe.endsWith(".zip") && !recipe.endsWith(".temp")) {
//                            String recipeName = recipe.substring(0, recipe.lastIndexOf("."));
//                            if (map.get(recipeName) == null) {
//                                map.put(recipeName, 0);
//                            } else {
//                                map.put(recipeName, map.get(recipeName) + 1);
//                            }
//                        }
//                    }
//
//                }
//                for (Map.Entry<String, Integer> entry : map.entrySet()) {
//                    if (entry.getValue() > 2) {
//                        uniqueList.add(entry.getKey());
//                    }
//                }
            for (String recipeName : recipeList) {
                if (!"".equals(recipeName) && !"done".equals(recipeName)) {
                    recipeName = recipeName.replace(".BLD", "").replace(".bld", "");
                    if (!"".equals(recipeName)) {
                        uniqueList.add(recipeName);
                    }
                }
            }
//                uniqueList.sort(new Comparator<String>() {
//                    @Override
//                    public int compare(String s1, String s2) {
//                        return s1.compareTo(s2);
//                    }
//                });

            /**
             * 将recipe列表排序显示
             */
//            uniqueList.sort(new Comparator<String>() {
//                @Override
//                public int compare(String s1, String s2) {
//                    return s1.compareTo(s2);
//                }
//            });
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("设备：" + deviceCode + "获取recipe列表失败!", e);
        }
        return uniqueList;
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
                        if (loginStateResult != null && !loginStateResult.isEmpty() && "Logout".equalsIgnoreCase(loginStateResult.get(0))) {
                            iSecsHost.executeCommand("playback logout.txt");
                        }
                        List<String> loginResult = iSecsHost.executeCommand("replay login893.exe");
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

    public void logout() {
        List<String> currentUser = iSecsHost.executeCommand("read user");
        if (currentUser != null && !currentUser.isEmpty()) {
            if ("HonTech".equalsIgnoreCase(currentUser.get(0))) {
                iSecsHost.executeCommand("playback logout.txt");
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

    public boolean startCheck() {
        if (specialCheck()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 接受alert直接卡控
     *
     * @return
     */
//    public boolean startCheck2() {
//        if (specialCheck()) {
//            return true;
//        } else {
////            return false;
//        }
////        if (AxisUtility.isEngineerMode(deviceCode)) {
////            UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
////            return true;
////        }
//        boolean pass = true;
//        String checkRecultDesc = "";
//        if (this.checkLockFlagFromServerByWS(deviceCode)) {
//            checkRecultDesc = "检测到设备被Server要求锁机,设备将被锁!";
//            UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
//            pass = false;
//        }
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        DeviceService deviceService = new DeviceService(sqlSession);
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//
//        if (deviceInfoExt == null) {
//            logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
//            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！");
//            checkRecultDesc = "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！";
//            pass = false;
//        } else {
//            String trackInRcpName = deviceInfoExt.getRecipeName();
//            if (!ppExecName.equals(trackInRcpName)) {
//                UiLogUtil.appendLog2EventTab(deviceCode, "已选程序与领料程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " 已选程序 " + ppExecName);
//                pass = false;
//                checkRecultDesc = "已选程序与领料程序不一致,设备被锁定！请联系ME处理！领料程序:" + trackInRcpName + " 已选程序:" + ppExecName;
//            }
//        }
//        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
//        if (execRecipe == null) {
//            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理！");
//            checkRecultDesc = "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理!";
//            pass = false;
//        }
//        Map mqMap = new HashMap();
//        mqMap.put("msgName", "eqpt.StartCheckWI");
//        mqMap.put("deviceCode", deviceCode);
//        mqMap.put("recipeName", ppExecName);
//        mqMap.put("EquipStatus", equipStatus);
//        mqMap.put("lotId", lotId);
//        if (pass) {
//            //MonitorService monitorService = new MonitorService(sqlSession);
//            // List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
//            if (!deviceInfoExt.getLockSwitch().equals("Y") || !deviceInfoExt.getStartCheckMod().equals("C")) {
//                return pass;
//            }
//            List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
//            List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(deviceInfoExt.getRecipeId(), deviceCode, equipRecipeParas, "");
//            try {
//                String eventDesc = "";
//                if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
//                    this.stopEquip();
//                    UiLogUtil.appendLog2EventTab(deviceCode, "开机参数检查未通过!");
//                    for (RecipePara recipePara : recipeParasdiff) {
//                        eventDesc = "开机Check参数异常参数编码为:" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为:" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
//                        UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
//                        checkRecultDesc = checkRecultDesc + eventDesc;
//                    }
//                    //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
//                    StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass:");
//                    for (RecipePara recipePara : recipeParasdiff) {
//                        recipeParasDiffText.append(",Error Para Name:");
//                        recipeParasDiffText.append(recipePara.getParaName());
//                        recipeParasDiffText.append(",Recipe Set Value:");
//                        recipeParasDiffText.append(recipePara.getSetValue());
//                        recipeParasDiffText.append(",Gold Recipe Set Value;");
//                        recipeParasDiffText.append(recipePara.getDefValue());
//                    }
//                    sendMessage2Eqp(recipeParasDiffText.toString());
//                    pass = false;
//                } else {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "开机参数检查通过！");
//                    eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
//                    logger.info("设备：" + deviceCode + " 开机Check成功");
//                    pass = true;
//                    checkRecultDesc = eventDesc;
//                }
//                sqlSession.commit();
//            } catch (Exception e) {
//                logger.error("Exception:", e);
//            } finally {
//                sqlSession.close();
//            }
//        } else {
//            UiLogUtil.appendLog2EventTab(deviceCode, "开机检查条件不具备,检查未通过!");
//        }
//        mqMap.put("eventDesc", checkRecultDesc);
//        GlobalConstants.C2SLogQueue.sendMessage(mqMap);
//        return pass;
//    }

    /**
     * 先停机在check
     *
     * @return
     */
//    public boolean specialCheck() {
////        if (AxisUtility.isEngineerMode(deviceCode)) {
//////            UiLogUtil.appendLog2EventTab(deviceCode, "工程模式，取消开机Check卡控！");
//////            return true;
//////        }
////            if(this.startCheckOver1st){
////                return true;
////            }
//        //先暂停机台
//        stopEquip();
//        boolean pass = true;
//        String checkRecultDesc = "";
//        if (this.checkLockFlagFromServerByWS(deviceCode)) {
//            checkRecultDesc = "检测到设备被Server要求锁机,设备将被锁!";
//            UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
//            pass = false;
//        }
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        DeviceService deviceService = new DeviceService(sqlSession);
//        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
//
//        if (deviceInfoExt == null) {
//            logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
//            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！");
//            checkRecultDesc = "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！";
//            pass = false;
//        } else {
//            String trackInRcpName = deviceInfoExt.getRecipeName();
//            if (!ppExecName.equals(trackInRcpName)) {
//                UiLogUtil.appendLog2EventTab(deviceCode, "已选程序与领料程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " 已选程序 " + ppExecName);
//                pass = false;
//                checkRecultDesc = "已选程序与领料程序不一致,设备被锁定！请联系ME处理！领料程序:" + trackInRcpName + " 已选程序:" + ppExecName;
//            }
//        }
//        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
//        if (execRecipe == null) {
//            UiLogUtil.appendLog2EventTab(deviceCode, "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理！");
//            checkRecultDesc = "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理!";
//            pass = false;
//        }
//        Map mqMap = new HashMap();
//        mqMap.put("msgName", "eqpt.StartCheckWI");
//        mqMap.put("deviceCode", deviceCode);
//        mqMap.put("recipeName", ppExecName);
//        mqMap.put("EquipStatus", equipStatus);
//        mqMap.put("lotId", lotId);
//
//        if (pass && (deviceInfoExt.getLockSwitch().equals("Y") && deviceInfoExt.getStartCheckMod().equals("C"))) {
//            //MonitorService monitorService = new MonitorService(sqlSession);
//            // List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
//            List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
//            List<RecipePara> recipeParasdiff = recipeService.checkRcpPara(deviceInfoExt.getRecipeId(), deviceCode, equipRecipeParas, "");
//            try {
//                String eventDesc = "";
//                if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
//                    this.stopEquip();
//                    UiLogUtil.appendLog2EventTab(deviceCode, "开机参数检查未通过!");
//                    for (RecipePara recipePara : recipeParasdiff) {
//                        eventDesc = "开机Check参数异常参数编码为:" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为:" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
//                        UiLogUtil.appendLog2EventTab(deviceCode, eventDesc);
//                        checkRecultDesc = checkRecultDesc + eventDesc;
//                    }
//                    //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
//                    StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass:");
//                    for (RecipePara recipePara : recipeParasdiff) {
//                        recipeParasDiffText.append(",Error Para Name:");
//                        recipeParasDiffText.append(recipePara.getParaName());
//                        recipeParasDiffText.append(",Recipe Set Value:");
//                        recipeParasDiffText.append(recipePara.getSetValue());
//                        recipeParasDiffText.append(",Gold Recipe Set Value;");
//                        recipeParasDiffText.append(recipePara.getDefValue());
//                    }
//                    sendMessage2Eqp(recipeParasDiffText.toString());
//                    pass = false;
//                } else {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "开机参数检查通过！");
//                    eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
//                    logger.info("设备：" + deviceCode + " 开机Check成功");
//                    //开始作业
//                    pass = true;
//                    checkRecultDesc = eventDesc;
//                }
//                sqlSession.commit();
//            } catch (Exception e) {
//                logger.error("Exception:", e);
//            } finally {
//                sqlSession.close();
//            }
//        } else {
//            UiLogUtil.appendLog2EventTab(deviceCode, "开机检查条件不具备,检查未通过!");
//        }
//        mqMap.put("eventDesc", checkRecultDesc);
//        GlobalConstants.C2SLogQueue.sendMessage(mqMap);
//        if (pass) {
//            logger.info("{}:发送指令开始开机作业！", deviceCode);
//            startCheckOver1st = true;
//            startEquip();
//        }
//        return pass;
//    }

    /**
     * 接受plc start指令进行开机check
     *
     * @return
     */
    public boolean specialCheck() {
//        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//            //耗时动作
//            List<String> curscreenNow = iSecsHost.executeCommand("curscreen");
//            if ("Contact".equalsIgnoreCase(curscreenNow.get(0))) {
//                logger.info("当前为调高界面，暂不比对！");
//                this.startEquip();
//                return true;
//            }
//        }
        logger.debug("设备:" + deviceCode + " PreEquipstatus: " + this.preEquipStatus + " Equipstatus: " + this.curEquipStatus);
        if ("contact".equalsIgnoreCase(this.preEquipStatus)) {
            logger.info("当前为调高界面，暂不比对！");
            this.startEquip();
            return true;
        }
        boolean pass = true;
        StringBuilder recipeParasDiffText = new StringBuilder("StartCheck not pass:");
        String checkRecultDesc = "";
        if (this.checkLockFlagFromServerByWS(deviceCode)) {
            checkRecultDesc = "检测到设备被Server要求锁机,设备将被锁!";
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
            pass = false;
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);


        if (deviceInfoExt == null) {
            logger.error("数据库中确少该设备模型配置；DEVICE_CODE:" + deviceCode);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！");
            checkRecultDesc = "工控上不存在设备:" + deviceCode + "模型信息，不允许开机！请联系ME处理！";
            pass = false;
        } else {
            String trackInRcpName = deviceInfoExt.getRecipeName();
            if (!ppExecName.equals(trackInRcpName)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "已选程序与领料程序不一致，设备被锁定！请联系ME处理！领料程序：" + trackInRcpName + " 已选程序 " + ppExecName);
                pass = false;
                checkRecultDesc = "已选程序与领料程序不一致,设备被锁定！请联系ME处理！领料程序:" + trackInRcpName + " 已选程序:" + ppExecName;
                recipeParasDiffText.append("Recipe Error!MES Download Recipe:[" + deviceInfoExt.getRecipeName() + "]");
            }
        }
        Recipe execRecipe = recipeService.getExecRecipe(ppExecName, deviceCode);
        if (execRecipe == null) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理！");
            checkRecultDesc = "工控上不存在: " + ppExecName + " 的Unique或Gold版本,将无法执行开机检查.请联系PE处理!";
            pass = false;
        }
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.StartCheckWI");
        mqMap.put("deviceCode", deviceCode);

        mqMap.put("recipeName", ppExecName);
        mqMap.put("EquipStatus", equipStatus);
        mqMap.put("lotId", lotId);
        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
            if (pass && (deviceInfoExt.getLockSwitch().equals("Y") && deviceInfoExt.getStartCheckMod().equals("C"))) {
                //MonitorService monitorService = new MonitorService(sqlSession);
                // List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
                List<RecipePara> equipRecipeParas = getRecipeParasFromMonitorMap();
                List<RecipePara> recipeParasdiff = this.checkRcpPara(deviceInfoExt.getRecipeId(), deviceCode, equipRecipeParas, "");
                try {
                    String eventDesc = "";
                    if (recipeParasdiff != null && recipeParasdiff.size() > 0) {
                        this.stopEquip();
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机参数检查未通过!");
                        for (RecipePara recipePara : recipeParasdiff) {
                            eventDesc = "开机Check参数异常参数编码为:" + recipePara.getParaCode() + ",参数名:" + recipePara.getParaName() + "其异常设定值为:" + recipePara.getSetValue() + ",默认值为：" + recipePara.getDefValue() + "其最小设定值为：" + recipePara.getMinValue() + ",其最大设定值为：" + recipePara.getMaxValue();
                            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, eventDesc);
                            checkRecultDesc = checkRecultDesc + eventDesc;
                        }
                        //monitorService.saveStartCheckErroPara2DeviceRealtimePara(recipeParasdiff, deviceCode);//保存开机check异常参数
                        for (RecipePara recipePara : recipeParasdiff) {
                            recipeParasDiffText.append(",Error Para Name:");
                            recipeParasDiffText.append(recipePara.getParaName());
                            recipeParasDiffText.append(",Recipe Set Value:");
                            recipeParasDiffText.append(recipePara.getSetValue());
                            recipeParasDiffText.append(",Gold Recipe Set Value;");
                            recipeParasDiffText.append(recipePara.getDefValue());
                        }
//                        sendMessage2Eqp(recipeParasDiffText.toString());
                        pass = false;
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开机参数检查通过！");
                        eventDesc = "设备：" + deviceCode + " 开机Check参数没有异常";
                        logger.info("设备：" + deviceCode + " 开机Check成功");
                        //开始作业
                        pass = true;
                        checkRecultDesc = eventDesc;
                    }
                    sqlSession.commit();
                } catch (Exception e) {
                    logger.error("Exception:", e);
                } finally {
                    sqlSession.close();
                }
            } else {
//                UiLogUtil.appendLog2EventTab(deviceCode, "开机检查条件不具备,检查未通过!");
                logger.info("设备：" + deviceCode + " 未设置卡机检查参数！");
            }
            mqMap.put("eventDesc", checkRecultDesc);
            GlobalConstants.C2SLogQueue.sendMessage(mqMap);
            if ("Y".equals(deviceInfoExt.getLockSwitch()) && !pass) {
                logger.info("{}:机台开机检查不通过，不发送指令开始开机作业！", deviceCode);
                sendMessage2Eqp(recipeParasDiffText.toString());
            } else {
                logger.info("{}:发送指令开始开机作业！", deviceCode);
                this.startEquip();
                //开机check通过
                pass = true;
            }
        }

        return pass;
    }

    /**
     * 参数检查
     *
     * @param recipeRowid
     * @param deviceCode
     * @param equipRecipeParas
     * @return
     */
    public List<RecipePara> checkRcpPara(String recipeRowid, String deviceCode, List<RecipePara> equipRecipeParas, String masterCompareType) {
        SqlSession sqlSession = null;
        List<RecipePara> wirecipeParaDiff = new ArrayList<>();
        try {
            sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            //获取Gold版本的参数(只有gold才有wi信息)
            List<RecipePara> goldRecipeParas = recipeService.searchRecipeParaByRcpRowId(recipeRowid);
            //确定管控参数
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateMonitor(deviceCode);
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                for (RecipePara recipePara : goldRecipeParas) {
                    if (recipePara.getParaCode() != null && recipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                        recipeTemplate.setMinValue(recipePara.getMinValue());
                        recipeTemplate.setMaxValue(recipePara.getMaxValue());
                        recipeTemplate.setSetValue(recipePara.getSetValue());
                    }
                }
            }
            List<String> codeList = new ArrayList<>();
            //找出设备当前recipe参数中超出wi范围的参数
            for (RecipePara equipRecipePara : equipRecipeParas) {
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    if (recipeTemplate.getParaCode().equals(equipRecipePara.getParaCode())) {
                        equipRecipePara.setRecipeRowId(recipeRowid);
                        String currentRecipeValue = equipRecipePara.getSetValue();
                        String setValue = recipeTemplate.getSetValue();
                        String minValue = recipeTemplate.getMinValue();
                        String maxValue = recipeTemplate.getMaxValue();
                        equipRecipePara.setDefValue(setValue);//默认值，recipe参数设定值
                        //判断开关是否false，若默认关闭则之后数值不需要卡控，并将之后code放置到规则中
                        //如果code在规则中则不参与比对,next
                        DecimalFormat decimalFormat = new DecimalFormat("##########.##########");
                        if (!"".equals(setValue)) {
                            if (setValue.matches("^-?[0-9]+(.[0-9]+)?$")) {
                                if (setValue.contains(".")) {
                                    setValue = decimalFormat.format(Double.parseDouble(setValue));
                                } else {
                                    setValue = decimalFormat.format(Integer.parseInt(setValue));
                                }
                            }
                        }
                        if (!"".equals(currentRecipeValue)) {
                            if (currentRecipeValue.matches("^-?[0-9]+(.[0-9]+)?$")) {
                                if (currentRecipeValue.contains(".")) {
                                    currentRecipeValue = decimalFormat.format(Double.parseDouble(currentRecipeValue));
                                } else {
                                    currentRecipeValue = decimalFormat.format(Integer.parseInt(currentRecipeValue));
                                }
                            }
                        }
//                        if(codeList.contains(recipeTemplate.getParaCode())){
//                            System.out.println(recipeTemplate.getParaCode());
//                            break;
//                        }
//                        if(recipeTemplate.getParaName().contains("Alarm1_Gategory")&&recipeTemplate.getParaName().contains("_Enable")&&"false".equals(setValue)){
//                            codeList.add(recipeTemplate.getParaCode().replace("0","1"));
//                            codeList.add(recipeTemplate.getParaCode().replace("0","3"));
//                        }
                        try {
                            if (recipeTemplate.getParaName().contains("_") && codeList.contains(recipeTemplate.getParaName().substring(0, recipeTemplate.getParaName().lastIndexOf("_")))) {
                                logger.info("此值不需要比对,开关默认关闭:" + recipeTemplate.getParaName());
                                break;
                            }
                            if (recipeTemplate.getParaName().contains("_Enable") && "false".equals(setValue)) {
                                codeList.add(recipeTemplate.getParaName().substring(0, recipeTemplate.getParaName().lastIndexOf("_")));
                                logger.info("此值不需要比对,开关默认关闭:" + recipeTemplate.getParaName());
                                break;
                            }
                            logger.info("开始对比参数Name:" + recipeTemplate.getParaName() + ",RecipeValue：" + currentRecipeValue + ",goldRecipeValue:" + setValue + ",ParaCode:" + recipeTemplate.getParaCode() + "************************************");
                        } catch (Exception ex) {
                            logger.info("", ex);
                        }
                        boolean paraIsNumber = false;
                        try {
                            Double.parseDouble(currentRecipeValue);
                            paraIsNumber = true;
                        } catch (Exception e) {
                        }
                        try {
                            if ("abs".equals(masterCompareType)) {
                                if ("".equals(setValue) || " ".equals(setValue) || "".equals(currentRecipeValue) || " ".equals(currentRecipeValue)) {
                                    continue;
                                }
                                equipRecipePara.setMinValue(setValue);
                                equipRecipePara.setMaxValue(setValue);
                                if (paraIsNumber) {
                                    if (Double.parseDouble(currentRecipeValue) != Double.parseDouble(setValue)) {
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                } else if (!currentRecipeValue.equals(setValue)) {
                                    wirecipeParaDiff.add(equipRecipePara);
                                }
                            } else//spec
                                if ("1".equals(recipeTemplate.getSpecType())) {
                                    if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                        continue;
                                    }
                                    if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                        equipRecipePara.setMinValue(minValue);
                                        equipRecipePara.setMaxValue(maxValue);
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                    //abs
                                } else if ("2".equals(recipeTemplate.getSpecType())) {
                                    if ("".equals(setValue) || " ".equals(setValue) || "".equals(currentRecipeValue) || " ".equals(currentRecipeValue)) {
                                        continue;
                                    }
                                    //按顺序来
                                    if (!currentRecipeValue.equals(setValue)) {
                                        equipRecipePara.setMinValue(setValue);
                                        equipRecipePara.setMaxValue(setValue);
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                } else {
                                    if ("".equals(minValue) || "".equals(maxValue) || minValue == null || maxValue == null) {
                                        continue;
                                    }
                                    if ((Double.parseDouble(equipRecipePara.getSetValue()) < Double.parseDouble(minValue)) || (Double.parseDouble(equipRecipePara.getSetValue()) > Double.parseDouble(maxValue))) {
                                        equipRecipePara.setMinValue(minValue);
                                        equipRecipePara.setMaxValue(maxValue);
                                        wirecipeParaDiff.add(equipRecipePara);
                                    }
                                }
                        } catch (Exception e) {
                            logger.error("Exception:", e);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
            return wirecipeParaDiff;
        }

    }

//    @Override
//    public String checkPPExecName(String recipeName) {
//        if (ppExecName.equals(recipeName)) {
//            logger.info(deviceCode + "预下载recipe相同，取消下载操作！");
//            return "1";
//        }
//        return "0";
//    }

    public static void main(String[] args) throws IOException {
//        try {
//            FileUtil.copyFile("D:\\99999999.zip", "D:\\TEST.zip");
//            Thread.sleep(100000000);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

//        Map<String,String> map = new HashMap<>();
//        map.put("111","1");
//        map.put("222","2");
//        map.put("22","2");
//        map.put("3333","3");
//        Map<String,String> rtMap = new HashMap<>();
//        for(Map.Entry<String,String> entry:map.entrySet()){
//            rtMap.put(entry.getKey()+"RT",entry.getValue());
//        }
//        for(Map.Entry<String,String> entry:rtMap.entrySet()){
//            System.out.println(entry.getKey()+":"+entry.getValue());
//        }
//File file =new File("C:\\Users\\WDF\\Desktop\\7045_tray\\");
//
//        for(File file1:file.listFiles()){
//            System.out.println(HT7045HISIHost.getTrayNameFromTRY(file1.getAbsolutePath()).trim());
//        }
//        File file1 = new File("C:\\Users\\WDF\\Desktop\\AKJ-7.6X8.1-MT6261-FT.TRY");
//        System.out.println(HT7045HISIHost.getTrayNameFromTRY(file1.getAbsolutePath()).trim());
//        System.out.println("kaishi");
//        int i = (int) (Math.random() * 10);
//        if (i % 2 == 0) {
//            System.out.println(i);
//            return;
//        }
//        try {
//            System.out.println("try:" + i);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        } finally {
//            System.out.println("finally:" + i);
//        }
    }


}
