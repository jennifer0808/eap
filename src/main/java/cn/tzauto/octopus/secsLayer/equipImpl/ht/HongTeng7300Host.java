package cn.tzauto.octopus.secsLayer.equipImpl.ht;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;
import cn.tzauto.generalDriver.entity.msg.FormatCode;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.resolver.TransferUtil;
import cn.tzauto.octopus.secsLayer.resolver.ht.HongTengRecipeUtil;
import cn.tzauto.octopus.secsLayer.resolver.icos.TrRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;


public class HongTeng7300Host extends EquipHost {

    private static final Logger logger = Logger.getLogger(HongTeng7300Host.class.getName());

    public HongTeng7300Host(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = FormatCode.SECS_4BYTE_UNSIGNED_INTEGER;
        EquipStateChangeCeid=102L;
    }


    @Override
    public Object clone() {
        HongTeng7300Host newEquip = new HongTeng7300Host(deviceId,
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
            LastComDate = new Date().getTime();
            secsMsgTimeoutTime = 0;
            DataMsgMap data = event.removeMessageFromQueue();
            if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
                setCommState(COMMUNICATING);
            } else if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
                replyS6F12WithACK(data, (byte) 0);
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
        long ceid = 0L;
        try {
            if (data.get("CEID") != null) {
                ceid = (long) data.get("CEID");
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            //TODO 根据ceid分发处理事件
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else if (ceid == EquipStateChangeCeid) {//102L
                processS6F11EquipStatusChange(data);
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
            } else if (ceid == 201) {
                processS6F11LotCheck(data);
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
            } else if (ceid == 202) {
               UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "批次已结批！");
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
            } else {
                activeWrapper.sendS6F12out((byte) 0, data.getTransactionId());
            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    protected void processS6F11LotCheck(DataMsgMap data) {
        String lotId = "";
        try {
            ArrayList list = (ArrayList) data.get("REPORT");
            lotId = (String) ((ArrayList)list.get(1)).get(1);
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "设备当前批次号为" + lotId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Boolean flag = true;
        if (flag == true) {
            sendS2f41Cmd("NEWLOT-OK");
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "批次号匹配一致，执行开机");
        } else {
            sendS2f41Cmd("NEWLOT-NG");
           UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "检测到设备当前批次号与MES系统不匹配，不允许执行开机");
        }
    }

    @Override
    protected void processS6F11EquipStatusChange(DataMsgMap data) {
        long ceid = 0L;
        try {
            ceid = (long) data.get("CEID");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map map = new HashMap();
        map.put("EquipStatus", equipStatus);
        if (equipStatus.equalsIgnoreCase("run")) {
            //首先从服务端获取机台是否处于锁机状态
            //如果设备应该是锁机，那么首先发送锁机命令给机台
            if (this.checkLockFlagFromServerByWS(deviceCode)) {
               UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "检测到设备被设置为锁机，设备将被锁!");
                this.sendS2f41Cmd("PAUSE");
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

    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        Map resultMap = super.sendS7F3out(localRecipeFilePath,targetRecipeName.replace("@", "/"));
        resultMap.put("ppid",targetRecipeName);
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) {
        Recipe recipe = setRecipe(recipeName);
        recipePath = super.getRecipePathByConfig(recipe);
        String ppbody = (String) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 0, recipePath);
        //logger.debug("Recive S7F6, and the recipe " + ppid + " has been saved at " + recipePath);
        //Recipe解析
//        if(ppbody.equalsIgnoreCase("OK")){}
        List<RecipePara> recipeParaList = new ArrayList<>();
        try {
            recipeParaList = HongTengRecipeUtil.transferRcpFromDB(recipePath);
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
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }
    // </editor-fold>

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

}
