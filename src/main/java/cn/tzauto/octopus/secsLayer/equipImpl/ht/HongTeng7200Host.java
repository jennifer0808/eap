package cn.tzauto.octopus.secsLayer.equipImpl.ht;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.ht.HongTengRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.icos.TrRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class HongTeng7200Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(HongTeng7200Host.class.getName());

    public HongTeng7200Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        EquipStateChangeCeid=102;
    }


    @Override
    public Object clone() {
        HongTeng7200Host newEquip = new HongTeng7200Host(deviceId,
                this.iPAddress,
                this.tCPPort, this.connectMode,
                this.deviceType, this.deviceCode);
        newEquip.startUp = this.startUp;
        newEquip.description = this.description;
        newEquip.activeWrapper = this.activeWrapper;
        //newEquip.equipState = this.equipState;
        newEquip.inputMsgQueue = this.inputMsgQueue;
        newEquip.activeWrapper.addInputMessageListenerToAll(newEquip);
        this.clear();
        return newEquip;
    }

    @Override
    public void run() {
//        String localRecipeFilePath="D:\\HongTengRecipe\\RECIPE\\A6\\BackGrind\\HT\\Engineer\\D7400-6010\\JCET-A-TQFP-14X14-100L-1.4\\JCET-A-TQFP-14X14-100L-1.4.txt";
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    this.sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sleep(3000);
                    sendS1F17out();
                    sendS1F1out();
                    //为了能调整为online remote
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                }
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else if(msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")){
                    processS6F11in(msg);
                }else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11equipstatuschange")) {
                    processS6F11EquipStatusChange(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equals("s6f11checklot")) {
                    processS6F11LotCheck(msg);
                } else {
                    //logger.debug("A message in queue with tag = " + msg.getMsgSfName()
                    //      + " which I do not want to process! ");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void inputMessageArrived(MsgArrivedEvent event) {
        String tagName = event.getMessageTag();
        if (tagName == null) {
            return;
        }
        try {
            LastComDate = System.currentTimeMillis();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s7f20in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.contains("F0") || tagName.contains("f0")) {
                controlState = FengCeConstant.CONTROL_OFFLINE;
                equipStatus = "SECS-OFFLINE";
                Map panelMap = new HashMap();
                panelMap.put("EquipStatus", equipStatus);
                panelMap.put("PPExecName", ppExecName);
                panelMap.put("ControlState", controlState);
                changeEquipPanel(panelMap);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void processS6F11in(DataMsgMap data) {
        long ceid = -12345679;
        try {
            if (data.get("CEID") != null) {
                ceid = (long) data.get("CEID");
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            //TODO 根据ceid分发处理事件
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else {
                replyS6F12WithACK(data, (byte) 0);
                if (ceid == EquipStateChangeCeid) {//102L
                    processS6F11EquipStatusChange(data);
                } else if (ceid == 201) {
                    processS6F11LotCheck(data);
                } else if (ceid == 202) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "批次已结批！");
                }
            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="S6F11 Code">


    protected List lotIDRead(String lotId) {
        List lotList = new ArrayList();
        int count = 0;
        int len = lotId.length();
        //取lotId的最后两位，判断是否为返工批次
        String newNo = lotId.substring(len - 2);
        if (newNo.equals(" R")) {
            logger.info("该批次:" + lotId + "为rework批次！");
            lotId = lotId.substring(0, len - 2);
        }
        for (int i = 0; i < lotId.length(); i++) {
            if (lotId.charAt(i) == '.') {
                count++;
            }
            if (count > 1) {
                logger.info("该批次:" + lotId + "为合批的批次");
            }
        }
        String newLotId[] = new String[count];
        for (int i = 0; i < count; i++) {
            newLotId[i] = lotId.split("\\.")[0] + '.' + lotId.split("\\.")[i + 1];
            logger.info("子批批次号为" + newLotId[i]);
            lotList.add(newLotId[i]);
        }
        return lotList;
    }

    protected void processS6F11LotCheck(DataMsgMap data) {
        String lotId = "";
        String workLot = "";
        try {
            ArrayList list = (ArrayList) data.get("REPORT");
            lotId = (String) ((ArrayList)list.get(1)).get(1);

           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备当前输入的批次号为" + lotId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Boolean flag = true;
        if (flag == true) {
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "批次号匹配一致，执行开机");
            sendS2f41Cmd("NEWLOT-OK");
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备当前批次号与MES系统不匹配，不允许执行开机");
            sendS2f41Cmd("NEWLOT-NG");
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long nowStatus = -1L;
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
            ArrayList  list = (ArrayList) data.get("REPORT");
            nowStatus = (long) ((ArrayList)list.get(1)).get(1);


        } catch (Exception e) {
            e.printStackTrace();
        }
        equipStatus = ACKDescription.descriptionStatus(String.valueOf(nowStatus), deviceType);
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);

        if (equipStatus.equalsIgnoreCase("run")) {
            //首先从服务端获取机台是否处于锁机状态
            //如果设备应该是锁机，那么首先发送锁机命令给机台
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                this.sendS2f41Cmd("PAUSE");
                //pauseDevice();
            }
        }
//        String preStatusStr = ACKDescription.descriptionStatus(preStatus, deviceType);

        if (equipStatus.equalsIgnoreCase("READY")) {
            findDeviceRecipe();
        }
        super.changeEquipPanel(map);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        if (equipStatus.equalsIgnoreCase("READY")) {
            if ("Engineer".equals(deviceInfoExt.getBusinessMod())) {
               UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "工程模式，取消开机卡控！");
            } else {
                if (this.checkLockFlagFromServerByWS(deviceCode)) {
                   UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                    holdDevice();
                }
                //检查领料程序与设备在用程序是否一致
                boolean recipeNameOk = checkRecipeName(deviceInfoExt.getRecipeName());
                //检查程序版本
                Recipe goldRecipe = checkRecipeHasGoldFlag(deviceInfoExt.getRecipeName());
                if (recipeNameOk && goldRecipe != null) {
                    Recipe downLoadRecipe = recipeService.getRecipe(deviceInfoExt.getRecipeId());
                    //首先判断下载的Recipe类型
                    //1、如果下载的是Unique版本，那么执行完全比较
                    String downloadRcpVersionType = downLoadRecipe.getVersionType();
                    if ("Unique".equals(downloadRcpVersionType)) {
                       UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始执行Recipe[" + ppExecName + "]参数绝对值Check");
                        startCheckRecipePara(downLoadRecipe, "abs");
                    } else {
                        //2、如果下载的Gold版本，那么根据EXT中保存的版本号获取当时的Gold版本号，比较参数
                        startCheckRecipePara(goldRecipe);
                    }
                } else {
                    holdDevice();
                }
            }
        }
        try {
            //更新模型表设备状态
            deviceInfoExt.setDeviceStatus(equipStatus);
            deviceInfoExt.setLockFlag(null);
            deviceService.modifyDeviceInfoExt(deviceInfoExt);
            sqlSession.commit();
            //保存设备操作记录到数据库
            saveOplogAndSend2Server(ceid, deviceService, deviceInfoExt);
        } catch (Exception e) {
        } finally {
            sqlSession.close();
        }
    }
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

    /**
     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
     *
     * @param localRecipeFilePath
     * @return
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map resultMap = super.sendS7F3out(localRecipeFilePath,targetRecipeName.replace("@", "/"));
        resultMap.put("ppid",targetRecipeName);
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        String ppbody = (String) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 0, recipePath);
//        logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
//        Recipe解析
//        if(ppbody.equalsIgnoreCase("OK")){}
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = HongTengRecipeUtil.transfer7200RcpFromDB(recipePath);
            for (int i = 0; i < recipeParaList.size(); i++) {
                String paraName = recipeParaList.get(i).getParaName();
                if (paraName.equals("") || paraName.equals("NULL")) {
                    recipeParaList.remove(i);
                    i--;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //TODO 实现存储，机台发来的recipe要存储到文件数据库要有记录，区分版本
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        File file = new File("D://7200-6010/SHARERECIPE/recipeShare/" + deviceCode + "/" + recipeName + "/" + recipeName + ".txt");
        Boolean result = copyToFile("D://7200-6010/SHARERECIPE/recipeShare/" + deviceCode + "/" + recipeName + "/" + recipeName + ".txt", recipePath);
        logger.info(result);
        file.delete();
        return resultMap;
    }
    // </editor-fold>

    public boolean copyToFile(String srcFile, String desFile) {
        File scrfile = new File(srcFile);
        if (scrfile.isFile() == true) {
            int length;
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(scrfile);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            File desfile = new File(desFile);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(desfile, false);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            desfile = null;
            length = (int) scrfile.length();
            byte[] b = new byte[length];
            try {
                fis.read(b);
                fis.close();
                fos.write(b);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            scrfile = null;
            return false;
        }
        scrfile = null;
        return true;
    }

    //    public void sendSharedFileToFTP(String recipeName, Recipe recipe) throws IOException {
//        InputStream in = null;
//        OutputStream out = null;
////        smb://xxx:xxx@192.168.3.3/testIndex/  xxx:xxx是共享盘的用户名和密码
//        String remoteURL = "D://7200Recipe/ShareRecipe/recipeShare/" + deviceCode + "/" + recipeName + "/" + recipeName + ".txt";//共享盘路径
//        String user = "Administrator";
//        String password = "123";
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        String recipeFilePath = recipeService.organizeRecipeDownloadFullFilePath(recipe);
//        String localRecipeFilePath = GlobalConstants.localRecipePath + recipeFilePath;//FTP路径
//        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", user, password);
//        //读取共享盘文件并下载至FTP
//        SmbFile files = new SmbFile(remoteURL, auth);
//        //TODO  
//        if (!files.exists()) {
//            logger.info("该路径下没有recipe文件！");
//        } else {
//            SmbFile[] fileLists = files.listFiles();
//            for (SmbFile f : fileLists) {
//                String fileName = f.getName();
//                File localFile = new File(localRecipeFilePath);
//                try {
//                    in = new BufferedInputStream(new SmbFileInputStream(remoteURL));
//                    out = new BufferedOutputStream(new FileOutputStream(localRecipeFilePath));
//                    byte[] bytes = new byte[1024];
//                    while (in.read(bytes) != -1) {
//                        out.write(bytes);
//                    }
//                    logger.info("recipe由共享盘转至FTP！");
//                    //下载成功后将共享文件夹下的文件删除
//                    SmbFile files1 = new SmbFile(remoteURL, auth);
//                    files1.delete();
//                } catch (FileNotFoundException ex) {
//                    ex.printStackTrace();
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    in.close();
//                    out.close();
//                }
//            }
//        }
//    }
    @SuppressWarnings("unchecked")
    public Map sendS7F17out(String recipeName) {
        ArrayList list = new ArrayList();
        list.add(recipeName);
        byte ackc7 = -1;
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(list);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            ackc7 = (byte) data.get("ACKC7");
            sleep(1000);
            if (ackc7 == 0 || ackc7 == 6) {
                logger.debug("The recipe " + recipeName + " has been delete from " + deviceCode);
            } else {
                logger.error("Delete recipe " + recipeName + " from " + deviceCode + " failure whit ACKC7=" + ackc7 + " means " + ACKDescription.description(ackc7, "ACKC7"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList recipeList = new ArrayList();
        recipeList.add(recipeName.replace("recipe", "component"));
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(recipeList);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList handlerList = new ArrayList();
        handlerList.add(recipeName.replace("recipe", "handler"));
        try {
            DataMsgMap data = activeWrapper.sendS7F17out(handlerList);
            logger.debug("Request delete recipe " + recipeName + " on " + deviceCode);
            sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f18");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipeName", recipeName);
        resultMap.put("ACKC7", ackc7);
        resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
        return resultMap;
    }


    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="RemoteCommand Code">
    /*
     * tr t640支持的命令有:
     * PP-SELECT
     * START
     * STOP
     * PAUSE
     * RESUME
     * ABORT
     */
    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("STOP");
            String holdResult = cmdMap.get("HCACK").toString();
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = true;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    public Map pauseDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("PAUSE");
            String holdResult = cmdMap.get("HCACK").toString();
//            if (holdResult.equals("0") || holdResult.equals("4")) {
////                cmdMap = sendS2f41Cmd("STOP");
////                holdResult = cmdMap.get("HCACK").toString();
            if (holdResult.equals("0") || holdResult.equals("4")) {
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = true;
            }
//            } else {
//                holdSuccessFlag = false;
//            }
            return cmdMap;
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    @Override
    public Map releaseDevice() {
        return null;//this.sendS2f41Cmd("RESUME");
//        return this.setEqptOffline();
    }
    // </editor-fold> 

    @Override
    public Map getRelativeFileInfo(String localFilePath, String targetRecipeName) {
        Map map = new HashMap();
        List<String> hanAndComp = TrRecipeUtil.readRCP(localFilePath);
        String hanRcpName = "";
        String hanRcpPath = "";
        String compRcpName = "";
        String compRcpPath = "";
        for (String str : hanAndComp) {
            if (str.contains("@handler@")) {
                hanRcpName = str.replace("@", "/");
                if (localFilePath.contains("_")) {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    hanRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("hanRcpName", hanRcpName);
                map.put("hanRcpPath", hanRcpPath);
            } else if (str.contains("@component@")) {
                compRcpName = str.replace("@", "/");
//                compRcpName = str + targetRecipeName.substring(targetRecipeName.lastIndexOf("_")).replace("@", "/");
                if (localFilePath.contains("_")) {
                    compRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + localFilePath.substring(localFilePath.lastIndexOf("_"));
                } else {
                    compRcpPath = localFilePath.substring(0, localFilePath.lastIndexOf("/") + 1) + str + ".txt";
                }
                map.put("compRcpName", compRcpName);
                map.put("compRcpPath", compRcpPath);
            } else {
                return null;
            }
        }
        return map;
    }

    private Recipe checkRecipeHasGoldFlag(String recipeName) {
        Recipe checkResult = null;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<Recipe> downLoadGoldRecipe = recipeService.searchRecipeGoldByPara(recipeName, deviceType, "GOLD", null);
        if (downLoadGoldRecipe == null || downLoadGoldRecipe.isEmpty()) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在：" + ppExecName + " 的Gold版本，无法执行开机检查，设备被锁定！请联系PE处理！");
            //不允许开机
            this.holdDeviceAndShowDetailInfo();
        } else {
            checkResult = downLoadGoldRecipe.get(0);
        }
        sqlSession.close();
        return checkResult;
    }

    @Override
    protected boolean checkRecipeName(String recipeName) {
        boolean checkResult = false;
        if (recipeName.equals(ppExecName)) {
            checkResult = true;
        }
        if (!checkResult) {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序不一致，核对不通过，设备被锁定！请联系PE处理！");
            holdDeviceAndShowDetailInfo();
        } else {
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "Recipe名称为：" + ppExecName + "，与改机后程序一致，核对通过！");
        }
        return checkResult;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendS1F13out() {
     super.sendS1F13out();
    }


    @Override
    public String checkEquipStatus() {
        findDeviceRecipe();
        if (FengCeConstant.STATUS_RUN.equalsIgnoreCase(equipStatus)) {
            return "设备正在运行，不可调整Recipe！";
        }
        if (!FengCeConstant.STATUS_IDLE.equalsIgnoreCase(equipStatus)) {
            return "设备未处于" + FengCeConstant.STATUS_IDLE + "状态，不可调整Recipe！";
        }
        return "0";
    }

    @Override
    public String testRUThere() {
        return super.testRUThere();
    }
}
