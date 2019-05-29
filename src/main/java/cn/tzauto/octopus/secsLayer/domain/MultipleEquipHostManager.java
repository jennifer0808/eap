package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.generalDriver.api.SecsDriverFactory;
import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.generalDriver.utils.DriverConfig;
import cn.tzauto.generalDriver.utils.PropInitializer;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.resolver.RecipeTransfer;
import cn.tzauto.octopus.common.util.ftp.FtpUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.EquipmentEventDealer;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.secsLayer.exception.NotInitializedException;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import cn.tzauto.octopus.secsLayer.util.UtilityFengCe;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultipleEquipHostManager {

    private static final Logger logger = Logger.getLogger(MultipleEquipHostManager.class.getName());
    private HashMap<String, EquipHost> equipHosts; //store pairs <deviceId, Equip>
    private String clientId = ""; //used for naming DB connection Cache
    private String hostXmlFilePath = null;
    private String equipListStr = "";
    private EquipModel equipModel;
    private EquipHost equipHost;
    private ConcurrentHashMap<String, EquipModel> equipModels;
    public Map<String, DeviceType> deviceTypeDic;
    public List<DeviceInfo> deviceInfos;

    public boolean initializeSecs(List<DeviceInfo> deviceInfos)
            throws ParserConfigurationException, SAXException, IOException, SecurityException,
            IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        equipHosts = new HashMap<String, EquipHost>();
        boolean pass = false;
        pass = loadDeviceInfoAndInstanciateEquips(deviceInfos);
        this.deviceInfos = deviceInfos;
        if (!pass) {
            logger.fatal("Error during loading host.xnl file - returned false. Exit!");
            //System.out.println("Error during loading host.xnl file - returned false. Exit!");
            return false;
        }

        PropInitializer.debug = true;
        PropInitializer.instance(); //load the jsip property into PropertyServer

        SecsDriverFactory.loadProperties(); //must be called
        DriverConfig.setDumpAllSettings(true);
        DriverConfig.printAllSettings();

        for (EquipHost value : this.equipHosts.values()) {
            try {
                value.initialize();
            } catch (Exception e) {
                logger.error("Exception:", e);
                logger.fatal(value.deviceId + "启动失败...");
            }
        }

        return pass;
    }

    /**
     * @return init equipList
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private boolean loadDeviceInfoAndInstanciateEquips(List<DeviceInfo> deviceInfos) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        boolean result = true;
        for (DeviceInfo deviceInfo : deviceInfos) {
            DeviceType deviceTypeObj = deviceTypeDic.get(deviceInfo.getDeviceTypeId());
            // new DeviceTypeDao().queryDeviceTypeById(deviceInfo.getDeviceTypeId());
            String deviceCode = deviceInfo.getDeviceCode();
            String deviceName = deviceInfo.getDeviceCode();
            String tDeviceId = deviceInfo.getDeviceId();
            String startUp = deviceInfo.getStartUp();
            String smlPath = GlobalConstants.getProperty("SML_PRE_PATH") + deviceTypeObj.getSmlPath();
            String ipAddress = GlobalConstants.clientInfo.getClientIp();
            String remoteIp = deviceInfo.getDeviceIp();
            String remotePort = deviceInfo.getDevicePort();
            String localPort = deviceInfo.getClientPort();
            String activeMode = deviceInfo.getActiveMode();
            String protocolType = "hsms";
            String hostJavaClass = deviceTypeObj.getHostJavaClass();
            String remark = deviceInfo.getRemarks();
            String deviceType = deviceInfo.getDeviceType();
            int recipeType = deviceTypeObj.getRecipeType();
            String iconPath = deviceTypeObj.getIconPath();
            boolean skip = false;
            logger.info("deviceCode : " + deviceCode);
            logger.info("deviceName : " + deviceName);
            logger.info("deviceId : " + tDeviceId);
            logger.info("startUp : " + startUp);
            logger.info("smlPath : " + smlPath);
            logger.info("ipAddress : " + ipAddress);
            logger.info("port : " + remotePort);
            logger.info("activeMode : " + activeMode);
            logger.info("protocolType : " + protocolType);
            logger.info("hostJavaClass : " + hostJavaClass);
            logger.info("remark : " + remark);
            logger.info("localTcpPort:" + localPort);

            /**
             * 如果不为空就获取设备的名字
             */
            //parse those data
//            if (deviceName == null || deviceName.trim().isEmpty()) {
//                deviceName = "defaul " + i;
//            } else {
//                deviceName = deviceName.trim();
//            }
            int deviceId = -1;//初始化设备的id
            if (tDeviceId == null || tDeviceId.trim().isEmpty()) {
                logger.fatal("Found Equip " + tDeviceId + " with no device Id.");
                //System.out.println("Found Equip " + equipId + " with no device Id.");
                skip = true;
            } else {
                try {
                    deviceId = Integer.parseInt(tDeviceId.trim());
                } catch (NumberFormatException e) {
                    logger.fatal("NumberFormatException:", e);
                    skip = true;
                }
                if (deviceId < 0) {
                    logger.fatal("Found Equip " + deviceName + " with negative device Id.");
                    skip = true;
                }
            }

            int localTcpPort = -1;
            if (localPort == null || localPort.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no local TCP Port info.");
                //System.out.println("Found Equip " + equipId + " with no local TCP Port info.");
                skip = true;
            } else {
                try {
                    localTcpPort = Integer.parseInt(localPort.trim());
                } catch (NumberFormatException e) {
                    logger.fatal("NumberFormatException:", e);
                    skip = true;
                }
                if (localTcpPort < 100) {
                    logger.fatal("Found Equip " + deviceName + " with wrong localTcpPort.");
                    //System.out.println("Found Equip " + equipId + " with wrong localTcpPort.");
                    skip = true;
                }
            }

            int remoteTcpPort = -1;
            if (remotePort == null || remotePort.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no Remote TCP Port info.");
                //System.out.println("Found Equip " + equipId + " with no Remote TCP Port info.");
                skip = true;
            } else {
                try {
                    remoteTcpPort = Integer.parseInt(remotePort.trim());
                } catch (NumberFormatException en) {
                    logger.fatal("Exception:", en);
                    skip = true;
                }
                if (remoteTcpPort < 100) {
                    logger.fatal("Found Equip " + deviceName + " with wrong remoteTcpPort.");
                    //System.out.println("Found Equip " + equipId + " with wrong remoteTcpPort.");
                    skip = true;
                }
            }

            if (ipAddress == null || ipAddress.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no localIPAddress.");
                //System.out.println("Found Equip " + equipId + " with no localIPAddress.");
                skip = true;
            } else {
                ipAddress = ipAddress.trim();
                skip = !UtilityFengCe.isIpAddress(ipAddress);
            }

            if (remoteIp == null || remoteIp.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no remoteIPAddress.");
                //System.out.println("Found Equip " + equipId + " with no remoteIPAddress.");
                skip = true;
            } else {
                remoteIp = remoteIp.trim();
                skip = !UtilityFengCe.isIpAddress(remoteIp);
            }
            if (smlPath == null || smlPath.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no smlFilePath.");
                skip = true;
            } else {
                smlPath = smlPath.trim();
            }
            if (activeMode == null || activeMode.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with null connectMode. Use default.");
                //System.out.println("Found Equip " + equipId + " with null connectMode. Use default");
                activeMode = "active";
            } else {
                if (activeMode.equals("1")) {
                    activeMode = "active";
                }
                activeMode = activeMode.trim();
                if ((!activeMode.equalsIgnoreCase("active"))
                        && (!activeMode.equalsIgnoreCase("passive"))) {
                    logger.fatal("Found Equip " + deviceName + " with wrong connectMode. Use default.");//ll错误点链接模式，使用默认的
                    //System.out.println("Found Equip " + equipId + " with wrong connectMode. Use default");
                    activeMode = "active";
                }
            }
            if (protocolType == null || protocolType.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with null protocolType. Use default.");
                //System.out.println("Found Equip " + equipId + " with null protocolType. Use default");
                protocolType = "hsms";
            } else {
                protocolType = protocolType.trim();
                if ((!protocolType.equalsIgnoreCase("hsms"))
                        && (!protocolType.equalsIgnoreCase("rs232"))) {
                    logger.fatal("Found Equip " + deviceName + " with wrong protocolType. Use default.");
                    //System.out.println("Found Equip " + equipId + " with wrong protocolType. Use default");
                    protocolType = "hsms";
                }
            }
            boolean isStart = true;
            if (startUp != null && startUp.equalsIgnoreCase("0")) {
                isStart = false;
            }

            if (hostJavaClass == null) {
                logger.fatal("Found Equip " + deviceName + " with no corresponding Java class name.");
                skip = true;
            }
            if (deviceType == null || deviceType.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no deviceType.");
                //System.out.println("Found Equip " + equipId + " with no smlFilePath.");
                skip = true;
            } else {
                deviceType = deviceType.trim();
            }

            if (!skip) {
                try {
                    EquipHost equip = (EquipHost) instanciateEquipHost(
                            String.valueOf(deviceId), remoteIp,
                            remoteTcpPort, activeMode, hostJavaClass, deviceType, deviceCode);
                    equip.setStartUp(isStart);
                    equip.setDaemon(true);
                    equip.setIconPath(iconPath);
                    equipHosts.put(deviceCode, equip);
                } catch (ClassNotFoundException cnfe) {
                    logger.error("Device " + deviceCode + " config error,(ClassNotFoundException) can't be initialized ");
                    continue;
                }

            } else {
                result = false;
            }
            skip = false;

        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object instanciateEquipHost(String devId, String remoteIpAddress, int remoteTcpPort,
                                        String connectMode, String hostClassName, String deviceType, String deviceCode)
            throws ClassNotFoundException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class hostClass = Class.forName(hostClassName);
        Constructor<?> cons = hostClass.getConstructor(
                new Class[]{
                        String.class,
                        String.class,
                        Integer.TYPE,
                        String.class,
                        String.class,
                        String.class
                });
        return (Object) cons.newInstance(
                new Object[]{
                        devId, remoteIpAddress, remoteTcpPort, connectMode, deviceType, deviceCode
                });

    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue.getNodeValue();
    }

    /**
     * get set 方法
     *
     * @return
     */
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getHostXmlFilePath() {
        return hostXmlFilePath;
    }

    public void setHostXmlFilePath(String hostXmlFilePath) {
        this.hostXmlFilePath = hostXmlFilePath;
    }

    public HashMap<String, EquipHost> getAllEquipHosts() {
        return equipHosts;
    }

    public ConcurrentHashMap<String, EquipModel> getAllEquipModels() {
        return equipModels;
    }

    public String getEquipListStr() {
        return equipListStr;
    }

    public void setEquipListStr(String equipListStr) {
        this.equipListStr = equipListStr;
    }

    /**
     * 开启第一步，开启某个设备的通信线程，等待接收处理消息
     *
     * @param deviceId
     */
    public void startHostThread(String deviceId) //throws ParameterErrorException, DeviceNotRegisteredException
    {
        EquipHost equip = equipHosts.get(deviceId);
        if (equip != null) {
            if (equip.isThreadUsed()) //need to instantiate a new object
            {
//                EquipHost newEquip = (EquipHost) equip.clone();
//                equipHosts.remove(deviceId);
//                equipHosts.put(deviceId, newEquip);
//                newEquip.start();
//                logger.info("调用了start方法" + deviceId);
//                equip = newEquip;
            } else {
                equip.start();
            }
            logger.info("Equipment Host " + equip.deviceCode + " has been started.");
        }
    }

    /**
     * 开启第二步，开启某个设备的SECS线程，开发发送和接收消息
     *
     * @param deviceId
     * @throws NotInitializedException
     */
    public void startSECS(String deviceId, EquipmentEventDealer eqpEventDealer) throws NotInitializedException, InterruptedException, InvalidHsmsHeaderDataException, T3TimeOutException, T6TimeOutException, HsmsProtocolNotSelectedException, IllegalStateTransitionException {
        if (equipHosts.get(deviceId) != null) {
            equipHosts.get(deviceId).startSecs(eqpEventDealer);
        }
    }

    /**
     * 关闭通信第一步，首先关闭底层SECS通信
     *
     * @param deviceId
     */
    public void terminateSECS(String deviceId) {
        if (equipHosts.get(deviceId) != null) {
            equipHosts.get(deviceId).terminateSecs();
        }
    }

    /**
     * 关闭通信第二步，关闭处理线程
     *
     * @param deviceId
     */
    public void terminateHostThread(String deviceId) {
        if (equipHosts.get(deviceId) != null && !equipHosts.get(deviceId).isInterrupted()) {
            //发出中断信号
            equipHosts.get(deviceId).interrupt();
            try {
                //等待线程执行run后中断
                equipHosts.get(deviceId).join();
            } catch (InterruptedException e) {
                logger.fatal("Caught an exception while waiting the Host Thread dying", e);
            }
        }
    }

    /*
     * This function only can be called by Event Dispatch Thread.
     */
    public void terminateServer() {
        for (EquipHost value : equipHosts.values()) {
            try {
                String deviceId = value.getDeviceId();
                if (value.isAlive()) {
                    //首先中断SECS通信，然后interrupt线程
                    value.terminateSecs();
                    this.terminateHostThread(deviceId);
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
                logger.fatal("Error occured while trying initializing Equip Host object of machine " + value.getName(), e);
            }
        }
//	this.closeDBConnetions();
        equipHosts.clear();
    }

    /**
     * @param deviceId
     */
    public void notifyHostOfJsipReady(String deviceId) {
        if (equipHosts.get(deviceId) != null) {
            equipHosts.get(deviceId).setSdrReady(true);
        }
    }

    /**
     * @param deviceId
     */
    public void notifyHostOfJsipDown(String deviceId) {
        if (equipHosts.get(deviceId) != null) {
            equipHosts.get(deviceId).setSdrReady(false);
        }
    }

    /**
     * 获取设备上传的所有Recipe附件列表
     *
     * @param deviceId
     * @param recipe
     * @return
     */
    public List<Attach> getRecipeAttachInfo(String deviceId, Recipe recipe) {
        if (equipModels.get(deviceId) != null) {
            return equipModels.get(deviceId).getRecipeAttachInfo(recipe);
        } else if (equipHosts.get(deviceId) != null) {
            return equipHosts.get(deviceId).getRecipeAttachInfo(recipe);
        } else {
            return null;
        }
    }

    /*
     * 获取机台recipe列表
     */
    public Map getRecipeListFromDevice(String deviceId) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.sendS7F19out();
        }
        if (equipModels.get(deviceId) != null) {
            boolean pass = equipModels.get(deviceId).getPassport();
            if (pass) {
                Map map = equipModels.get(deviceId).getEquipRecipeList();
                equipModels.get(deviceId).returnPassport();
                return map;
            } else {
                logger.debug("直接获取设备recipe列表失败,尝试循环多次获取...");
                for (int i = 0; i < 5; i++) {
                    logger.debug("第" + (i + 1) + "次获取...");
                    pass = equipModels.get(deviceId).getPassport(1);
                    if (!pass) {
                        logger.debug("获取失败,等待1秒后再次获取");
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    } else {
                        Map map = equipModels.get(deviceId).getEquipRecipeList();
                        equipModels.get(deviceId).returnPassport();
                        logger.debug("获取成功,结束循环");
                        return map;
                    }
                }
                UiLogUtil.getInstance().appendLog2EventTab(deviceId, "获取Recipe列表失败,通讯资源正在被占用,请稍后重试");
            }
        }
        return null;
    }

    /*
     * 获取指定recipe的详情（解析）
     */
    public Map getRecipeParaFromDevice(String deviceCode, String recipeName) throws UploadRecipeErrorException {
        if (equipHosts.get(deviceCode) != null) {
            EquipHost equipHost = equipHosts.get(deviceCode);
            if (equipHost.deviceType.contains("DEKHorizon03ix")) {
                equipHost.sendS1F3Check();
                if (!equipHost.ppExecName.equalsIgnoreCase(recipeName)) {
                    Map resultMap = new HashMap();
                    resultMap.put("checkResult", "只可以上传机台正在使用的Recipe，其他Recipe无法上传！");
                    UiLogUtil.getInstance().appendLog2EventTab(equipHost.deviceCode, "只可以上传机台正在使用的Recipe，其他Recipe无法上传！");
                    return resultMap;
                }
            }
            return equipHost.sendS7F5out(recipeName);
        }
        if (equipModels.get(deviceCode) != null) {
            boolean pass = equipModels.get(deviceCode).getPassport();
            if (pass) {
                Map map = equipModels.get(deviceCode).uploadRecipe(recipeName);
                equipModels.get(deviceCode).returnPassport();
                logger.info("map==========>" + map);
                return map;
            } else {
                logger.debug("直接获取设备recipe信息失败,尝试循环多次处理...");
                for (int i = 0; i < 5; i++) {
                    logger.debug("第" + (i + 1) + "次尝试...");
                    pass = equipModels.get(deviceCode).getPassport(1);
                    if (!pass) {
                        logger.debug("获取失败,等待1秒后再次获取");
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    } else {
                        Map map = equipModels.get(deviceCode).uploadRecipe(recipeName);
                        equipModels.get(deviceCode).returnPassport();
                        logger.debug("获取成功,结束循环");
                        return map;
                    }
                }
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "上传Recipe失败,通讯资源正在被占用,请稍后重试");
            }
        }
        return null;
    }

    /*
     * 下载Recipe到机台
     */
    public String downLoadRcp2DeviceComplete(String recipeFilePath, DeviceInfo deviceInfo, Recipe recipe) {
        if (equipHosts.get(deviceInfo.getDeviceCode()) != null) {
            EquipHost equipHost = equipHosts.get(deviceInfo.getDeviceCode());
            //直接从FTP下载，如果本地有就覆盖，如果FTP不存在，那么下载失败
            String localRecipeFilePath = GlobalConstants.localRecipePath + recipeFilePath;

            if (!GlobalConstants.isLocalMode) {
                //从Ftp 下载到本地
                String downLoadFileResult = FtpUtil.connectServerAndDownloadFile(localRecipeFilePath, recipeFilePath, GlobalConstants.ftpIP,
                        GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                //从Ftp 下载到本地
//        boolean downLoadResult = FtpUtil.downloadFile(localRecipeFilePath, recipeFilePath, GlobalConstants.ftpIP,
//                GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                if (RecipeTransfer.hasGoldPara(equipHost.deviceType)) {
                    new RecipeTransfer().edit(recipe, equipHost.deviceType, localRecipeFilePath);
                }
                if (!"0".equals(downLoadFileResult)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "RMS服务器不存在该Recipe，无法完成下载.PPID=" + recipe.getRecipeName());
                    return "RMS服务器不存在该Recipe，无法完成下载.PPID=" + recipe.getRecipeName();
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "从FTP下载Recipe成功.PPID=" + recipe.getRecipeName());
                }
                if (deviceInfo.getDeviceType().contains("ICOST")) {
                    Map map = equipHost.getRelativeFileInfo(localRecipeFilePath, recipe.getRecipeName());
                    String localHanRcpPath = String.valueOf(map.get("hanRcpPath"));
                    String hanRcpPath = localHanRcpPath.substring(localHanRcpPath.indexOf("/"));
                    String localCompRcpPath = String.valueOf(map.get("compRcpPath"));
                    String compRcpPath = localCompRcpPath.substring(localCompRcpPath.indexOf("/"));
                    //从Ftp 下载到本地
                    String downLoadHanResult = FtpUtil.connectServerAndDownloadFile(localHanRcpPath, hanRcpPath, GlobalConstants.ftpIP,
                            GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                    String downLoadCompResult = FtpUtil.connectServerAndDownloadFile(localCompRcpPath, compRcpPath, GlobalConstants.ftpIP,
                            GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                    if (!"0".equals(downLoadHanResult) || !"0".equals(downLoadCompResult)) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), downLoadHanResult + "-->" + downLoadCompResult + ".PPID=" + recipe.getRecipeName());
                        return downLoadHanResult + "-->" + downLoadCompResult + ".PPID=" + recipe.getRecipeName();
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "从FTP下载Recipe成功.PPID=" + recipe.getRecipeName());
                    }
                }
                if (deviceInfo.getDeviceType().contains("8760inline")) {
                    FtpUtil.connectServerAndDownloadFile(localRecipeFilePath.replaceAll(".txt", ".DFM"), recipeFilePath.replaceAll(".txt", ".DFM"), GlobalConstants.ftpIP,
                            GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                }
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "Local 模式,仅读取本地文件...");
            }

            //获取设备下载许可
            Map resultMap = equipHost.sendS7F1out(localRecipeFilePath, recipe.getRecipeName());
            if ("0".equals(String.valueOf(resultMap.get("ppgnt")))) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "获取设备下载许可成功，开始下载.....PPID=" + recipe.getRecipeName());
            } else {
                String failReason = "";
                if (deviceInfo.getDeviceName().contains("TOWA") && "6".equals(String.valueOf(resultMap.get("ppgnt")))) {
                    failReason = "设备通信状态为 Online Local，无法下载";
                } else {
                    failReason = String.valueOf(resultMap.get("Description"));
                }
                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "获取设备下载许可失败，PPID=" + recipe.getRecipeName() + "，原因：" + String.valueOf(resultMap.get("Description")));
                return "获取设备下载许可失败，PPID=" + recipe.getRecipeName() + "，原因：" + failReason;
            }
            //下载将recipe下载到机台
            resultMap = equipHost.sendS7F3out(localRecipeFilePath, recipe.getRecipeName());
            if (resultMap != null) {
                if ("0".equals(String.valueOf(resultMap.get("ACKC7")))) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "下载成功！PPID=" + recipe.getRecipeName());
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "下载失败，PPID=" + recipe.getRecipeName() + "；原因：" + String.valueOf(resultMap.get("Description")));
                    return "下载失败，PPID=" + recipe.getRecipeName() + "；原因：" + String.valueOf(resultMap.get("Description"));
                }
            } else {
                return "下载失败，PPID=" + recipe.getRecipeName() + "；原因：设备未正常回复消息，请检查通讯";
            }
        }
        if (equipModels.get(deviceInfo.getDeviceCode()) != null) {

            String downloadResult = equipModels.get(deviceInfo.getDeviceCode()).downloadRecipe(recipe);
            equipModels.get(deviceInfo.getDeviceCode()).returnPassport();
            return downloadResult;

        }
        return "0";
    }

    /*
     * 下载Recipe到机台for TP
     */
    public String downLoadRcp2DeviceCompleteForTP(String recipeFilePath, DeviceInfo deviceInfo, Recipe recipe) {
        EquipHost equipHost = equipHosts.get(deviceInfo.getDeviceCode());
        //直接从FTP下载，如果本地有就覆盖，如果FTP不存在，那么下载失败
        String localRecipeFilePath = GlobalConstants.localRecipePath + recipeFilePath;
        if (!GlobalConstants.isLocalMode) {
            //从Ftp 下载到本地
            String downLoadFileResult = FtpUtil.connectServerAndDownloadFile(localRecipeFilePath, recipeFilePath, GlobalConstants.ftpIP,
                    GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
            //从Ftp 下载到本地
//        boolean downLoadResult = FtpUtil.downloadFile(localRecipeFilePath, recipeFilePath, GlobalConstants.ftpIP,
//                GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);

            if (!"0".equals(downLoadFileResult)) {
                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "RMS服务器不存在该Recipe，无法完成下载.PPID=" + recipe.getRecipeName());
                return "RMS服务器不存在该Recipe，无法完成下载.PPID=" + recipe.getRecipeName();
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "从FTP下载Recipe成功.PPID=" + recipe.getRecipeName());
            }
            if (deviceInfo.getDeviceType().contains("ICOS")) {
                Map map = equipHost.getRelativeFileInfo(localRecipeFilePath, recipe.getRecipeName());
                String localHanRcpPath = String.valueOf(map.get("hanRcpPath"));
                String hanRcpPath = localHanRcpPath.substring(localHanRcpPath.indexOf("/"));
                String localCompRcpPath = String.valueOf(map.get("compRcpPath"));
                String compRcpPath = localCompRcpPath.substring(localCompRcpPath.indexOf("/"));
                //从Ftp 下载到本地
                String downLoadHanResult = FtpUtil.connectServerAndDownloadFile(localHanRcpPath, hanRcpPath, GlobalConstants.ftpIP,
                        GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                String downLoadCompResult = FtpUtil.connectServerAndDownloadFile(localCompRcpPath, compRcpPath, GlobalConstants.ftpIP,
                        GlobalConstants.ftpPort, GlobalConstants.ftpUser, GlobalConstants.ftpPwd);
                if (!"0".equals(downLoadHanResult) || !"0".equals(downLoadCompResult)) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), downLoadHanResult + "-->" + downLoadCompResult + ".PPID=" + recipe.getRecipeName());
                    return downLoadHanResult + "-->" + downLoadCompResult + ".PPID=" + recipe.getRecipeName();
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "从FTP下载Recipe成功.PPID=" + recipe.getRecipeName());
                }
            }
        }

        //获取设备下载许可
        Map resultMap = equipHost.sendS7F1out(localRecipeFilePath, recipe.getRecipeName());
        if ("0".equals(String.valueOf(resultMap.get("ppgnt")))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "获取设备下载许可成功，开始下载.....PPID=" + recipe.getRecipeName());
        } else if ("1".equals(String.valueOf(resultMap.get("ppgnt")))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "设备已存在，直接覆盖，开始下载.....PPID=" + recipe.getRecipeName());
        } else {
            String failReason = "";
            if (deviceInfo.getDeviceName().contains("TOWA") && "6".equals(String.valueOf(resultMap.get("ppgnt")))) {
                failReason = "设备通信状态为 Online Local，无法下载";
            } else {
                failReason = String.valueOf(resultMap.get("Description"));
            }
            UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "获取设备下载许可失败，PPID=" + recipe.getRecipeName() + "，原因：" + String.valueOf(resultMap.get("Description")));
            return "获取设备下载许可失败，PPID=" + recipe.getRecipeName() + "，原因：" + failReason;
        }
        //下载将recipe下载到机台
        resultMap = equipHost.sendS7F3out(localRecipeFilePath, recipe.getRecipeName());
        if ("0".equals(String.valueOf(resultMap.get("ACKC7")))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "下载成功！PPID=" + recipe.getRecipeName());
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceInfo.getDeviceCode(), "下载失败，PPID=" + recipe.getRecipeName() + "；原因：" + String.valueOf(resultMap.get("Description")));
            return "下载失败，PPID=" + recipe.getRecipeName() + "；原因：" + String.valueOf(resultMap.get("Description"));
        }
        return "0";
    }


    /*
     * 命令机台使用指定Recipe
     */
    public Map makeDeviceUseSpecifiedRcp(String deviceId, Recipe recipe) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.sendS2F41outPPselect(recipe.getRecipeName());
        }
        Map resultMap = null;
        return resultMap;
    }

    /**
     * 选中指定的Recipe
     *
     * @param deviceId
     * @return
     */
    public String selectSpecificRecipe(String deviceId, String recipeName) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            Map resultMap = equipHost.sendS2F41outPPselect(recipeName);
            if (resultMap != null && !resultMap.isEmpty()) {
                if ("0".equals(String.valueOf(resultMap.get("HCACK"))) || "4".equals(String.valueOf(resultMap.get("HCACK")))) {
                    UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), "PPSelect成功，PPID=" + recipeName);
                    return "0";
                } else {
                    Map eqptStateMap = equipHost.findEqptStatus();//失败上报机台状态
                    UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), "选中Recipe失败,PPID=" + recipeName + ";原因：" + String.valueOf(resultMap.get("Description")) + ",机台状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                    return "选中Recipe失败,PPID=" + recipeName + ",原因：" + String.valueOf(resultMap.get("Description") + ",设备状态为 " + String.valueOf(eqptStateMap.get("EquipStatus")) + "/" + String.valueOf(eqptStateMap.get("ControlState")));
                }
            } else {
                return "选中Recipe失败，请确认设备是否在通讯状态.";
            }
        }
        if (equipModels.get(deviceId) != null) {
            if (equipModels.get(deviceId).getPassport(1)) {
                String selectResult = equipModels.get(deviceId).selectRecipe(recipeName);
                UiLogUtil.getInstance().appendLog2EventTab(deviceId, "[" + recipeName + "]" + selectResult);
                equipModels.get(deviceId).returnPassport();
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(deviceId, "[" + recipeName + "]选中失败,通讯资源正在被占用,请稍后重试");
            }
        }
        return "";

    }

    /**
     * 删除设备上所有的recipe，返回结果 如果成功，返回0;如果失败，返回成功和失败信息
     *
     * @param deviceId
     * @return
     */
    public String deleteAllRcpFromDevice(String deviceId, String currentRecipeName) {
        String resultString = "0";
        List<String> recipeNameList = (List<String>) getRecipeListFromDevice(deviceId).get("eppd");
        for (String recipeName : recipeNameList) {
            if (recipeName.equals(currentRecipeName)) {
                continue;
            }
            if (equipHosts.get(deviceId) != null) {
                String deviceType = equipHosts.get(deviceId).getDeviceType();
                if (deviceType.contains("ICOS")) {
                    if (!recipeName.contains("/recipe/")) {
                        continue;
                    }
                }
                if (deviceType.contains("8312")) {
                    if (recipeName.equals(currentRecipeName + ".rcp")) {
                        continue;
                    }
                }
                if (deviceType.contains("2100")) {
                    if (recipeName.equals(currentRecipeName + ".dbrcp")) {
                        continue;
                    }
                }
                if (deviceType.contains("FICO")) {
                    if (!recipeName.contains("/PR/")) {
                        continue;
                    }
                }
                if (deviceType.contains("DB730") || deviceType.contains("DB-800HSD")) {
                    if (recipeName.contains("SYSDATA")) {
                        continue;
                    }
                }
            }
            deleteRecipeFromDevice(deviceId, recipeName);
        }

        return resultString;
    }

    /*
     * 从设备删除Recipe
     */
    public Map deleteRecipeFromDevice(String deviceId, String recipeName) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            Map resultMap = equipHost.sendS7F17out(recipeName);
            if ("0".equals(String.valueOf(resultMap.get("ACKC7")))) {
                UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), recipeName + "在设备上删除成功!");
            } else if ("4".equals(String.valueOf(resultMap.get("ACKC7")))) { //这里4表示 PPID not found
                UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), recipeName + "在设备上不存在，无需删除!");
            } else {
                UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), recipeName + "删除失败,原因：" + String.valueOf(resultMap.get("Description")));
            }
            return resultMap;
        }
        if (equipModels.get(deviceId) != null) {
            String deleteResult = equipModels.get(deviceId).deleteRecipe(recipeName);
            UiLogUtil.getInstance().appendLog2EventTab(deviceId, "[" + recipeName + "]" + deleteResult);
        }
        Map resultMap = null;
        return resultMap;
    }

    /*
     * 获取设备使用recipe所需要监控参数的对应svid
     */
    public Map getDeviceRcpParaCheck(String deviceId, List svIdList) throws IOException, T6TimeOutException, HsmsProtocolNotSelectedException, T3TimeOutException, MessageDataException, BrokenProtocolException, StreamFunctionNotSupportException, ItemIntegrityException, InterruptedException {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.activeWrapper.sendS1F3out(svIdList, equipHost.svFormat);
        }
        Map resultMap = null;
        return resultMap;
    }

    /*
     * 获取设备使用recipe所需要监控参数的对应svid
     */
    public Map getMonitorParaBySV(String deviceId, List svIdList) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.getSpecificSVData(svIdList);
        }
        Map resultMap = null;
        return resultMap;
    }

    public Map getMonitorParaByEC(String deviceId, List ecIdList) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.getSpecificECData(ecIdList);
        }
        Map resultMap = null;
        return resultMap;
    }

    /*
     * 获取设备使用recipe所需要监控参数的对应svid
     */
    public Map getSVValueBySVID(String deviceId, String svid) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.sendS1F3SingleCheck(svid);
        }
        Map resultMap = null;
        return resultMap;
    }

    /*
     * 获取设备使用recipe所需要监控参数的对应ecid
     */
    public Map getECValueByECID(String deviceId, String ecid) {
        Map resultMap = null;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.sendS2F13ECSingleCheckout(ecid);
        }

        return resultMap;
    }

    public void sendTerminalMsg2Eqpt(String deviceId, String msg) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            equipHost.sendTerminalMsg2EqpSingle(msg);
        }

    }

    public String sendCommand2Eqp(String deviceId, String commandKey) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            UiLogUtil.getInstance().appendLog2EventTab(equipHost.getDeviceCode(), "向设备发送" + commandKey);
            Map resultMap = equipHost.sendCommandByDymanic(commandKey);
            byte[] ack = (byte[]) resultMap.get("HCACK");
            if (ack[0] == 0l) {
                return "0";
            } else {
                return String.valueOf(resultMap.get("Description"));
            }
        }
        return "";
    }

    /*
     * 停住设备
     */
    public boolean stopDevice(String deviceId) {
        boolean pass = false;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            Map resultMap = equipHost.stopDevice();
            if (resultMap != null && !resultMap.isEmpty()) {
                if ("0".equals(String.valueOf(resultMap.get("HCACK"))) || "4".equals(String.valueOf(resultMap.get("HCACK")))) {
                    pass = true;
                } else {
                    pass = false;
                }
                UiLogUtil.getInstance().appendLog2SecsTab(equipHost.getDeviceCode(), "HCACK:" + resultMap.get("HCACK") + " Description:" + resultMap.get("Description").toString());
            }
        }
        if (equipModels.get(deviceId) != null) {
            String stopResult = equipModels.get(deviceId).stopEquip();
            if ("0".equals(stopResult)) {
                pass = true;
            } else {
                UiLogUtil.getInstance().appendLog2SecsTab(deviceId, stopResult);
                pass = false;
            }
        }
        return pass;
    }

    /*
     * 锁定设备
     */
    public boolean holdDevice(String deviceId) {
        boolean pass = false;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.holdDeviceAndShowDetailInfo();
        }

        return pass;
    }

    /*
     * 解锁设备
     */
    public boolean releaseDevice(String deviceId) {
        boolean pass = false;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.releaseDeviceAndShowDetailInfo();
        }
        return pass;
    }

    /*
     * 锁定设备
     */
    public boolean changeEqptState(String deviceId, String state) {
        boolean pass = false;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            try {
                if ("LOCK".equals(state)) {
                    pass = equipHost.holdDeviceAndShowDetailInfo();
                } else if ("RELEASE".equals(state)) {
                    pass = equipHost.releaseDeviceAndShowDetailInfo();
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        return pass;
    }

    /*
     * 锁定设备
     */
    public boolean changeEqptState(String deviceId, String state, String type) {
        boolean pass = false;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            try {
                if ("LOCK".equals(state)) {
                    pass = equipHost.holdDeviceAndShowDetailInfo(type);
                } else if ("RELEASE".equals(state)) {
                    pass = equipHost.releaseDeviceAndShowDetailInfo(type);
                }
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        if (equipModels.get(deviceId) != null) {
            String stopResult = "";
            if ("LOCK".equals(state)) {
                stopResult = equipModels.get(deviceId).stopEquip();
                if ("0".equals(stopResult)) {
                    pass = true;
                } else {
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceId, stopResult);
                    pass = false;
                }
            } else if ("RELEASE".equals(state)) {
                return true;
            }
        }
        return pass;
    }

    /**
     * 获取设备的初始化状态包括：equipstatus，ppexecname，controlstate
     *
     * @param deviceId
     * @return
     */
    public Map getEquipInitState(String deviceId) {
        Map resultMap = null;
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            if (FengCeConstant.CONTROL_OFFLINE.equalsIgnoreCase(equipHost.getControlState())) {
                UiLogUtil.getInstance().appendLog2SecsTab(equipHost.getDeviceCode(), "设备处于Offline状态...");
                return null;
            }
            return equipHost.findDeviceRecipe();
        }
        if (equipModels.get(deviceId) != null) {
            return equipModels.get(deviceId).getEquipRealTimeState();
        }

        return resultMap;
    }

    public String getEquipStatus(String deviceId) {
        String equipStatus = "";
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            if (FengCeConstant.CONTROL_OFFLINE.equalsIgnoreCase(equipHost.getControlState())) {
                UiLogUtil.getInstance().appendLog2SecsTab(equipHost.getDeviceCode(), "设备处于Offline状态...");
                return "Offline";
            }
            return equipHost.findDeviceRecipe().get("EquipStatus").toString();
        }
        if (equipModels.get(deviceId) != null) {
//            if (equipModels.get(deviceId).getPassport()) {
            return equipModels.get(deviceId).getEquipStatus();
//                equipModels.get(deviceId).returnPassport();
//                return equipStatus;
//            }
        }

        return equipStatus;
    }

    /**
     * 获取刀片刀片寿命阀值：针对划片机
     *
     * @param deviceId
     * @return
     */
    public Map getEquipBladeLifeThreshold(String deviceId) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
//            return equipHost.getBladeThreshold();
            return null;
        } else {
            return null;
        }
    }

    public List<Attach> getEquipRecipeAttarch(String deviceId, Recipe recipe) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.getRecipeAttachInfo(recipe);
        }
        if (equipModels.get(deviceId) != null) {
            return equipModels.get(deviceId).getRecipeAttachInfo(recipe);
        }
        return null;
    }

    public Map getSVShotCountValue(String deviceId) throws IOException, T6TimeOutException, HsmsProtocolNotSelectedException, T3TimeOutException, MessageDataException, BrokenProtocolException, StreamFunctionNotSupportException, ItemIntegrityException, InterruptedException {
        Map resultMap = new HashMap();
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List svidlist = recipeService.searchShotSVByDeviceType(equipHost.getDeviceType());
            sqlSession.close();
            if (svidlist != null && !svidlist.isEmpty()) {
                resultMap = equipHost.activeWrapper.sendS1F3out(svidlist, equipHost.svFormat);
            }
        } else {

        }

        return resultMap;
    }


    public String changeEqptStateAndReturnDetail(String deviceId, String state) {
        String result = "";
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            if (equipHost.getEquipState().isCommOn()) {
                try {
                    if ("LOCK".equals(state)) {
//                pass = equipHost.holdDeviceAndShowDetailInfo();
                    } else if ("RELEASE".equals(state)) {
//                pass = equipHost.releaseDeviceAndShowDetailInfo();
                    } else if ("START".equals(state)) {
                        result = equipHost.startDeviceAndShowDetailInfo();
                    }
                } catch (Exception e) {
                    logger.error("Exception:", e);
                }
            } else {
                result = "设备当前不在通讯状态!无法执行控制命令!请重试！";
            }
        }
        if (equipModels.get(deviceId) != null) {
            if ("LOCK".equals(state)) {
                result = equipModels.get(deviceId).lockEquip();
            } else if ("RELEASE".equals(state)) {

            } else if ("START".equals(state)) {
                result = equipModels.get(deviceId).startEquip();
            }
        }
        return result;
    }

    /**
     * @param deviceId
     * @param dataIdMap
     * @return
     */
    public Map getSpecificData(String deviceId, Map<String, String> dataIdMap) throws UploadRecipeErrorException {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            if (equipHost.getEquipState().isCommOn()) {
                return equipHost.getSpecificData(dataIdMap);
            }
        } else if (equipModels.get(deviceId) != null) {
            if (equipModels.get(deviceId).isConnect()) {
                if (equipModels.get(deviceId).getPassport(20)) {
                    Map resultMap = equipModels.get(deviceId).getSpecificData(dataIdMap);
                    equipModels.get(deviceId).returnPassport();
                    return resultMap;
                }
            }
        }
        return new HashMap();
    }

    public String checkBeforeDownload(String deviceId, String recipeName) {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            return equipHost.checkBeforeDownload(recipeName);
        } else if (equipModels.get(deviceId) != null) {
            if (equipModels.get(deviceId).isConnect()) {
                return equipModels.get(deviceId).checkBeforeDownload(recipeName);
            }
        }
        return "0";
    }

    public ArrayList<EquipNodeBean> initEquipNodeBeans() {
        if ((equipHosts == null || equipHosts.isEmpty()) && (equipModels == null || equipModels.isEmpty())) {
            return null;
        }
        ArrayList<EquipNodeBean> result = new ArrayList<>();
        for (EquipHost value : equipHosts.values()) {
            EquipNodeBean nBean = new EquipNodeBean(value.getEquipState());
            nBean.setDeviceIdProperty(String.valueOf(value.getDeviceId()));
            nBean.setStartUpProperty(value.isStartUp());
            nBean.setiPAddressProperty(value.iPAddress);
            nBean.settCPPortProperty(value.tCPPort);
            nBean.setConnectModeProperty(value.getConnectMode());
            nBean.setDeviceCode(value.getDeviceCode());
            nBean.setDeviceType(value.deviceType);
            nBean.setProtocolTypeProperty("SECS");
            nBean.setIconPath(value.getIconPath());
            result.add(nBean);
        }
        for (EquipModel value : equipModels.values()) {
            EquipNodeBean nBean = new EquipNodeBean(value.getEquipState());
            nBean.setDeviceIdProperty(value.deviceCode);
            nBean.setStartUpProperty(value.isStartUp());
            nBean.setProtocolTypeProperty("ISECS");
            nBean.setDeviceCode(value.deviceCode);
            nBean.setDeviceType(value.deviceType);
            nBean.setIconPath(value.getIconPath());
            result.add(nBean);
        }
        logger.info("Extracted " + result.size() + " EquipNodeBean objects.");
        return result;
    }

    private boolean initializeIsecs(List<DeviceInfo> deviceInfos) throws ClassNotFoundException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        equipModels = new ConcurrentHashMap<String, EquipModel>();
        boolean pass = false;
        pass = instanciateEquipModels(deviceInfos);
        if (!pass) {
            logger.fatal("Error during loading host.xnl file - returned false. Exit!");
            return false;
        }
        for (EquipModel value : this.equipModels.values()) {
            try {
                value.initialize();
            } catch (Exception e) {
                logger.error("Exception:", e);
                logger.fatal(value.deviceCode + "启动失败...");
            }
        }

        return pass;
    }

    private boolean instanciateEquipModels(List<DeviceInfo> deviceInfos) throws ClassNotFoundException, SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        boolean result = true;
        for (DeviceInfo deviceInfo : deviceInfos) {
            DeviceType deviceTypeObj = deviceTypeDic.get(deviceInfo.getDeviceTypeId());
            String deviceCode = deviceInfo.getDeviceCode();
            String deviceName = deviceInfo.getDeviceCode();
            String tDeviceId = deviceInfo.getDeviceCode();
            String startUp = deviceInfo.getStartUp();
            String ipAddress = GlobalConstants.clientInfo.getClientIp();
            String remoteIp = deviceInfo.getDeviceIp();
            String remotePort = deviceInfo.getDevicePort();
            String localPort = deviceInfo.getClientPort();
            String activeMode = deviceInfo.getActiveMode();
            String protocolType = "isecs";
            String hostJavaClass = deviceTypeObj.getHostJavaClass();
            String remark = deviceInfo.getRemarks();
            String deviceType = deviceInfo.getDeviceType();
            int recipeType = deviceTypeObj.getRecipeType();
            String iconPath = deviceTypeObj.getIconPath();
            String equipRecipePath = deviceTypeObj.getSmlPath();
            boolean skip = false;
            logger.info("deviceCode : " + deviceCode);
            logger.info("deviceName : " + deviceName);
            logger.info("deviceId : " + tDeviceId);
            logger.info("startUp : " + startUp);

            logger.info("ipAddress : " + ipAddress);
            logger.info("port : " + remotePort);

            logger.info("protocolType : " + protocolType);
            logger.info("hostJavaClass : " + hostJavaClass);
            logger.info("remark : " + remark);
            logger.info("localTcpPort:" + localPort);

            /**
             * 如果不为空就获取设备的名字
             */
            //parse those data
//            if (deviceName == null || deviceName.trim().isEmpty()) {
//                deviceName = "defaul " + i;
//            } else {
//                deviceName = deviceName.trim();
//            }
            int localTcpPort = -1;
            if (localPort == null || localPort.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no local TCP Port info.");
                //System.out.println("Found Equip " + equipId + " with no local TCP Port info.");
                skip = true;
            } else {
                try {
                    localTcpPort = Integer.parseInt(localPort.trim());
                } catch (NumberFormatException e) {
                    logger.fatal("NumberFormatException:", e);
                    skip = true;
                }
                if (localTcpPort < 100) {
                    logger.fatal("Found Equip " + deviceName + " with wrong localTcpPort.");
                    //System.out.println("Found Equip " + equipId + " with wrong localTcpPort.");
                    skip = true;
                }
            }

            int remoteTcpPort = -1;
            if (remotePort == null || remotePort.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no Remote TCP Port info.");
                //System.out.println("Found Equip " + equipId + " with no Remote TCP Port info.");
                skip = true;
            } else {
                try {
                    remoteTcpPort = Integer.parseInt(remotePort.trim());
                } catch (NumberFormatException en) {
                    logger.fatal("Exception:", en);
                    skip = true;
                }
                if (remoteTcpPort < 100) {
                    logger.fatal("Found Equip " + deviceName + " with wrong remoteTcpPort.");
                    //System.out.println("Found Equip " + equipId + " with wrong remoteTcpPort.");
                    skip = true;
                }
            }

            if (ipAddress == null || ipAddress.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no localIPAddress.");
                //System.out.println("Found Equip " + equipId + " with no localIPAddress.");
                skip = true;
            } else {
                ipAddress = ipAddress.trim();
                skip = !UtilityFengCe.isIpAddress(ipAddress);
            }

            if (remoteIp == null || remoteIp.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no remoteIPAddress.");
                //System.out.println("Found Equip " + equipId + " with no remoteIPAddress.");
                skip = true;
            } else {
                remoteIp = remoteIp.trim();
                skip = !UtilityFengCe.isIpAddress(remoteIp);
            }

            if (activeMode == null || activeMode.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with null connectMode. Use default.");
                //System.out.println("Found Equip " + equipId + " with null connectMode. Use default");
                activeMode = "active";
            } else {
                if (activeMode.equals("1")) {
                    activeMode = "active";
                }
                activeMode = activeMode.trim();
                if ((!activeMode.equalsIgnoreCase("active"))
                        && (!activeMode.equalsIgnoreCase("passive"))) {
                    logger.fatal("Found Equip " + deviceName + " with wrong connectMode. Use default.");//ll错误点链接模式，使用默认的
                    //System.out.println("Found Equip " + equipId + " with wrong connectMode. Use default");
                    activeMode = "active";
                }
            }
            if (protocolType == null || protocolType.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with null protocolType. Use default.");
                //System.out.println("Found Equip " + equipId + " with null protocolType. Use default");
                protocolType = "isecs";
            } else {
                protocolType = protocolType.trim();
                if (!protocolType.equalsIgnoreCase("isecs")) {
                    logger.fatal("Found Equip " + deviceName + " with wrong protocolType. Use default.");
                    //System.out.println("Found Equip " + equipId + " with wrong protocolType. Use default");
                    protocolType = "isecs";
                }
            }
            boolean isStart = true;
            if (startUp != null && startUp.equalsIgnoreCase("0")) {
                isStart = false;
            }

            if (hostJavaClass == null) {
                logger.fatal("Found Equip " + deviceName + " with no corresponding Java class name.");
                skip = true;
            }
            if (deviceType == null || deviceType.trim().isEmpty()) {
                logger.fatal("Found Equip " + deviceName + " with no deviceType.");
                //System.out.println("Found Equip " + equipId + " with no smlFilePath.");
                skip = true;
            } else {
                deviceType = deviceType.trim();
            }

            if (!skip) {
                try {
                    EquipModel equip = (EquipModel) instanciateEquipModel(
                            deviceCode, remoteIp, remoteTcpPort, hostJavaClass, deviceType, iconPath, equipRecipePath);
                    equip.setStartUp(isStart);
                    equip.setDaemon(true);
                    equipModels.put(deviceCode, equip);
                } catch (ClassNotFoundException cnfe) {
                    logger.error("Device " + deviceCode + " config error,(ClassNotFoundException) can't be initialized ");
                    continue;
                }
            } else {
                result = false;
            }
            skip = false;

        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object instanciateEquipModel(String devId, String remoteIpAddress, int remoteTcpPort,
                                         String hostClassName, String deviceType, String iconPath, String equipRecipePath)
            throws ClassNotFoundException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class hostClass = Class.forName(hostClassName);
        Constructor<?> cons = hostClass.getConstructor(
                new Class[]{
                        String.class,
                        String.class,
                        Integer.TYPE,
                        String.class,
                        String.class,
                        String.class
                });
        return (Object) cons.newInstance(
                new Object[]{
                        devId, remoteIpAddress, remoteTcpPort, deviceType, iconPath, equipRecipePath
                });

    }

    private Map<String, List<DeviceInfo>> getDeviceInfos() {
        clientId = GlobalConstants.getProperty("clientId");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        deviceTypeDic = deviceService.getDeviceTypeMap();
        List<DeviceInfo> deviceInfoList = deviceService.getDeviceInfoByClientId(clientId);
        //new DeviceInfoDao().queryDeviceInfoByClientId(clientId);
        sqlSession.close();
        if (deviceInfoList == null || deviceInfoList.size() < 1) {
            logger.error("工控下未配置任何设备,启动已取消...");
            return null;
        }
        GlobalConstants.deviceInfos = deviceInfoList;
        this.deviceInfos = deviceInfoList;
        GlobalConstants.stage.hostManager.deviceInfos = deviceInfoList;
        List<DeviceInfo> deviceInfoSecs = new ArrayList<>();
        List<DeviceInfo> deviceInfoIsecs = new ArrayList<>();
        for (DeviceInfo deviceInfo : deviceInfoList) {
            if (deviceTypeDic.containsKey(deviceInfo.getDeviceTypeId())) {
                String protocol = String.valueOf(deviceTypeDic.get(deviceInfo.getDeviceTypeId()).getProtocolType());
                if ("2".equals(protocol)) {
                    deviceInfoIsecs.add(deviceInfo);
                } else {
                    deviceInfoSecs.add(deviceInfo);
                }
            } else {
                logger.error("devicetype " + deviceInfo.getDeviceType() + " id" + deviceInfo.getDeviceId() + " not found. ");
                logger.error("device " + deviceInfo.getDeviceCode() + " can not instantiation. ");
            }
        }
        Map<String, List<DeviceInfo>> deviceInfoMap = new HashMap<>();
        deviceInfoMap.put("SECS", deviceInfoSecs);
        deviceInfoMap.put("ISECS", deviceInfoIsecs);
        return deviceInfoMap;
    }

    public boolean initialize() throws ParserConfigurationException, SAXException, IOException,
            SecurityException,
            IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        boolean pass = false;
        Map<String, List<DeviceInfo>> deviceInfoMap = getDeviceInfos();
        List<DeviceInfo> deviceInfoSecs = deviceInfoMap.get("SECS");
        List<DeviceInfo> deviceInfoIsecs = deviceInfoMap.get("ISECS");
        pass = initializeSecs(deviceInfoSecs);
        if (!pass) {
            logger.fatal("Error during initializeSecs - returned false. Exit!");
            return false;
        }
        pass = initializeIsecs(deviceInfoIsecs);
        if (!pass) {
            logger.fatal("Error during initializeIsecs - returned false. Exit!");
            return false;
        }

        return pass;
    }

    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        if (equipModels.get(recipe.getDeviceCode()) != null) {
            return equipModels.get(recipe.getDeviceCode()).uploadRcpFile2FTP(localRcpPath, remoteRcpPath, recipe);
        } else {
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(recipe.getDeviceCode());
            sqlSession.close();
            if (equipHosts.get(deviceInfo.getDeviceCode()) != null) {
                return equipHosts.get(deviceInfo.getDeviceCode()).uploadRcpFile2FTP(localRcpPath, remoteRcpPath, recipe);
            } else {
                return false;
            }
        }
    }

    public void sendStatus2Server(String deviceId, String deviceStatus) {
        if (!GlobalConstants.isLocalMode) {
            String deviceCode = "";
            String ppExecName = "";
            String preEquipStatus = "";
            String lotId = "";
            if (equipModels.get(deviceId) != null) {
                deviceCode = deviceId;
                ppExecName = equipModels.get(deviceId).ppExecName;
                preEquipStatus = equipModels.get(deviceId).preEquipStatus;
                lotId = equipModels.get(deviceId).lotId;
            } else {
                deviceCode = equipHosts.get(deviceId).getDeviceCode();
                ppExecName = equipHosts.get(deviceId).ppExecName;
                preEquipStatus = equipHosts.get(deviceId).preEquipStatus;
                lotId = equipHosts.get(deviceId).lotId;
            }
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            try {
                DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                deviceInfoExt.setDeviceStatus(deviceStatus);
                DeviceOplog deviceOplog = setDeviceOplog(0, ppExecName, deviceStatus, preEquipStatus, lotId, deviceCode);
                deviceOplog.setOpDesc("equip status report" + deviceStatus);
                sendDeviceInfoExtAndOplog2Server(deviceInfoExt, deviceOplog);
                logger.info("*************equip status report*********");
                sqlSession.commit();
            } catch (Exception e) {
                logger.error(e.getMessage());
            } finally {
                sqlSession.close();
            }
        }
    }

    /**
     * 发送设备操作日志到服务端
     *
     * @param deviceInfoExt
     * @param deviceOplog
     */
    public void sendDeviceInfoExtAndOplog2Server(DeviceInfoExt deviceInfoExt, DeviceOplog deviceOplog) {
        Map mqMap = new HashMap();
        mqMap.put("msgName", "eqpt.EqptStatusChange");
        mqMap.put("deviceCode", deviceInfoExt.getDeviceRowid());
        mqMap.put("eventName", "eqpt.EqptStatusChange");
        mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
        mqMap.put("deviceCeid", deviceOplog.getDeviceCeid());
        mqMap.put("eventDesc", deviceOplog.getOpDesc());
        mqMap.put("eventDate", GlobalConstants.dateFormat.format(new Date()));
        mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
        GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
    }

    protected DeviceOplog setDeviceOplog(long ceid, String PPExecName, String equipStatus, String formerDeviceStatus, String lotId, String deviceCode) {
        DeviceOplog deviceOplog = new DeviceOplog();
        deviceOplog.setId(UUID.randomUUID().toString());
        deviceOplog.setDeviceCode(deviceCode);
        deviceOplog.setCurrRecipeName(PPExecName);
        deviceOplog.setDeviceCeid(String.valueOf(ceid));
        deviceOplog.setCurrLotId(lotId);
        deviceOplog.setOpTime(new Date());
        deviceOplog.setOpDesc("机台状态从" + formerDeviceStatus + "切换为" + equipStatus);
        deviceOplog.setOpType("eqpt.EqptStatusChange");
        deviceOplog.setFormerDeviceStatus(formerDeviceStatus);
        deviceOplog.setCurrDeviceStatus(equipStatus);
        deviceOplog.setCreateBy("System");
        deviceOplog.setCreateDate(new Date());
        deviceOplog.setUpdateBy("System");
        deviceOplog.setUpdateDate(new Date());
        deviceOplog.setDelFlag("0");
        deviceOplog.setVerNo(0);
        return deviceOplog;
    }

    public void isecsUploadMultiRecipe(String deviceId, List recipeNames) {
        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        if (equipModels.get(deviceId).getPassport(1)) {
            for (Object recipeName : recipeNames) {
                Map recipeMap = equipModels.get(deviceId).uploadRecipe(String.valueOf(recipeName));
                if (recipeMap != null && recipeMap.size() > 0) {
                    Recipe recipe = null;
                    if (recipeMap.get("recipe") != null) {
                        recipe = (Recipe) recipeMap.get("recipe");
                    }
                    List<RecipePara> recipeParaList = (List<RecipePara>) recipeMap.get("recipeParaList");
                    //保存数据
                    recipeService.saveUpLoadRcpInfo(recipe, recipeParaList, deviceId);

                    sqlSession.commit();
                    UiLogUtil.getInstance().appendLog2EventTab(deviceId, "Recipe[" + recipeName + "]上传成功！");
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceId, "Recipe[" + recipeName + "]上传失败！");
                }
            }
            equipModels.get(deviceId).returnPassport();
        }
        sqlSession.close();
    }

    /**
     * 获取设备的连接状态，如果该DviceID还未被初始化，也返回false
     *
     * @param deviceId
     * @return
     */
    public boolean hostIsConnected(String deviceId) {
        EquipHost equip = equipHosts.get(deviceId);
        if (equip != null) {
            return equip.getCommState() == EquipHost.COMMUNICATING;
        } else {
            return false;
        }
    }

    /**
     * @param deviceId
     * @param dataIdMap
     * @return
     */
    public Map getEDCData(String deviceId, Map<String, String> dataIdMap) throws UploadRecipeErrorException {
        if (equipHosts.get(deviceId) != null) {
            EquipHost equipHost = equipHosts.get(deviceId);
            if (equipHost.getEquipState().isCommOn()) {
                return equipHost.getSpecificData(dataIdMap);
            }
        } else if (equipModels.get(deviceId) != null) {
            if (equipModels.get(deviceId).isConnect()) {
                return null;//equipModels.get(deviceId).getSpecificData(dataIdMap);
            }
        }
        return new HashMap();
    }


    public String deleteRcpFromDeviceAndShowLog(String deviceId, String recipeName) {


        Map resultMap = deleteRecipeFromDevice(deviceId, recipeName);
        if ("0".equals(String.valueOf(resultMap.get("ACKC7")))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceId, recipeName + "在设备上删除成功!");
            return "0";
        } else if ("4".equals(String.valueOf(resultMap.get("ACKC7")))) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceId, recipeName + "在设备上不存在，无需删除!");
            return "0";
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceId, recipeName + "删除失败,原因：" + String.valueOf(resultMap.get("Description")));
            return recipeName + "删除失败,原因：" + String.valueOf(resultMap.get("Description"));
        }
    }

    public DeviceInfo getDeviceInfo(String deviceId, String deviceCode) {
        if (deviceId != null) {
            for (DeviceInfo deviceInfo : GlobalConstants.deviceInfos) {
                if (deviceId.equals(deviceInfo.getDeviceId())) {
                    return deviceInfo;
                }
            }
        }
        if (deviceCode != null) {
            for (DeviceInfo deviceInfo : GlobalConstants.deviceInfos) {
                if (deviceCode.equals(deviceInfo.getDeviceCode())) {
                    return deviceInfo;
                }
            }
        }
        return null;
    }

    public String getEquipCurrentRecipeName(String deviceId) {
        String equipStatus = "";
        if (equipModels.get(deviceId) != null) {
            return equipModels.get(deviceId).getCurrentRecipeName();
        }
        return equipStatus;
    }
}
