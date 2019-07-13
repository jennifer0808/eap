package cn.tzauto.octopus.secsLayer.equipImpl.shinkawa;


import cn.tzauto.generalDriver.api.MsgArrivedEvent;
import cn.tzauto.generalDriver.entity.msg.DataMsgMap;

import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeNameMapping;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.remoteCommand.CommandDomain;
import cn.tzauto.octopus.secsLayer.exception.UploadRecipeErrorException;
import cn.tzauto.octopus.secsLayer.resolver.shinkawa.ShinkawaRecipeUtil;
import cn.tzauto.octopus.secsLayer.util.ACKDescription;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.util.*;

public class SHINKAWAHost extends EquipHost {

    private static final long serialVersionUID = -8427516257654563776L;
    private static final Logger logger = Logger.getLogger(SHINKAWAHost.class.getName());

    public SHINKAWAHost(String devId, String IpAddress, int TcpPort, String connectMode, String deviceType, String deviceCode) {
        super(devId, IpAddress, TcpPort, connectMode, deviceType, deviceCode);
        svFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ecFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        ceFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        rptFormat = SecsFormatValue.SECS_4BYTE_UNSIGNED_INTEGER;
        RCMD_PPSELECT = "SLCT_PP";
    }


    @Override
    public Object clone() {
        SHINKAWAHost newEquip = new SHINKAWAHost(deviceId,
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
        threadUsed = true;
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, this.deviceCode);
        while (!this.isInterrupted()) {
            try {
                while (!this.isSdrReady()) {
                    this.sleep(200);
                }
                if (this.getCommState() != this.COMMUNICATING) {
                    sendS1F13out();
                }
                if (rptDefineNum < 1) {
                    sendS1F1out();
                    //为了能调整为online remote
//                    sendS1F17out();
                    super.findDeviceRecipe();
                    rptDefineNum++;
                    sendS5F3out(true);
                    initRptPara();
                    initRemoteCommand();
                }
                //设备在下一个可能停止的点才能停止
                if (!holdSuccessFlag) {
                    holdDevice();
                }
                DataMsgMap msg = null;
                msg = this.inputMsgQueue.take();
                if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s14f1in")) {
                    processS14F1in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s6f11in")) {
                    processS6F11in(msg);
                } else if (msg.getMsgSfName() != null && msg.getMsgSfName().equalsIgnoreCase("s5f1in")) {
                    this.processS5F1in(msg);
                } else {
                    logger.info("A message in queue with tag = " + msg.getMsgSfName()
                            + " which I do not want to process! ");
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.fatal("Caught Interruption", e);
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
            if (tagName.equalsIgnoreCase("s1f1in")) {
                processS1F1in(data);
            } else if (tagName.equalsIgnoreCase("s1f2in")) {
                processS1F2in(data);
            } else if (tagName.equalsIgnoreCase("s1f13in")) {
                processS1F13in(data);
            } else if (tagName.equalsIgnoreCase("s1f14in")) {
                processS1F14in(data);
            } else if (tagName.equalsIgnoreCase("s1f4in")) {
                putDataIntoWaitMsgValueMap(data);
            } else if (tagName.equalsIgnoreCase("s2f34in")) {
                processS2F34in(data);
            } else if (tagName.equalsIgnoreCase("s2f36in")) {
                processS2F36in(data);
            } else if (tagName.equalsIgnoreCase("s2f38in")) {
                processS2F38in(data);
            } else if (tagName.equalsIgnoreCase("s6f11in")) {
//                processS6F11in(data);
                super.replyS6F12WithACK(data, (byte) 0);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s14f1in")) {
//                processS14F1in(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s5f1in")) {
                replyS5F2Directly(data);
                this.inputMsgQueue.put(data);
            } else if (tagName.equalsIgnoreCase("s10f1in")) {
                processS10F1in(data);
            } else if (tagName.equalsIgnoreCase("s12f1in")) {
                processS12F1in(data);
            } else if (tagName.equalsIgnoreCase("s12f3in")) {
                processS12F3in(data);
            } else if (tagName.equalsIgnoreCase("s12f5in")) {
                processS12F5in(data);
            } else if (tagName.equalsIgnoreCase("s12f7in")) {
                processS12F7in(data);
            } else if (tagName.equalsIgnoreCase("s12f9in")) {
                processS12F9in(data);
            } else if (tagName.equalsIgnoreCase("s12f11in")) {
                processS12F11in(data);
            } else if (tagName.equalsIgnoreCase("s12f13in")) {
                processS12F13in(data);
            } else if (tagName.equalsIgnoreCase("s12f15in")) {
                processS12F15in(data);
            } else if (tagName.equalsIgnoreCase("s12f17in")) {
                processS12F17in(data);
            } else if (tagName.equalsIgnoreCase("s12f19in")) {
                processS12F19in(data);
            } else if (tagName.equalsIgnoreCase("s12f67in")) {
                processS12F67in(data);
            } else {
                logger.info("Received a message with tag = " + tagName
                        + " which I do not want to process! ");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }


    public String initRptPara() {
        try {
            logger.debug("initRptPara+++++++++++++++++++");
//            this.sendS2F33clear();
//            this.sendS2F35clear();
//            //重新定义Learn Device事件
//            List<Long> svidlist = new ArrayList<>();
//            svidlist.add(56L);
//            svidlist.add(60L);
//            svidlist.add(11L);
//            sendS2F33out(9L, 9L, svidlist);
//            sendS2F35out(9L, 9L, 9L);
//            sendS2F37out(9L);
//            sendS2F37outAll();
            return "1";

        } catch (Exception ex) {
//            java.util.logging.Logger.getLogger(EsecDB2100Host.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Exception:", ex);
            return "0";
        }
    }

    public void initRemoteCommand() {
        String commandKey = "start";
        CommandDomain startCommand = new CommandDomain();
        startCommand.setRcmd("HOST_STR");
        this.remoteCommandMap.put(commandKey, startCommand);

        commandKey = "stop";
        CommandDomain stopCommand = new CommandDomain();
        stopCommand.setRcmd("HOST_STP");
        this.remoteCommandMap.put(commandKey, stopCommand);

        commandKey = "pasuse";
        CommandDomain pauseCommand = new CommandDomain();
        pauseCommand.setRcmd("PAUSE");
        this.remoteCommandMap.put(commandKey, pauseCommand);

        commandKey = "resume";
        CommandDomain resumeCommand = new CommandDomain();
        resumeCommand.setRcmd("RESUME");
        this.remoteCommandMap.put(commandKey, resumeCommand);

        commandKey = "local";
        CommandDomain localCommand = new CommandDomain();
        localCommand.setRcmd("LOCAL");
        this.remoteCommandMap.put(commandKey, localCommand);

        commandKey = "remote";
        CommandDomain remoteCommand = new CommandDomain();
        remoteCommand.setRcmd("REMOTE");
        this.remoteCommandMap.put(commandKey, remoteCommand);
    }

    @Override
    protected List getNcessaryData() {
        DataMsgMap data = null;
        try {
            List statusList = new ArrayList<>();

            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            String equipStatussvid = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "EquipStatus").get(0).getDeviceVariableId();
            String pPExecNamesvid = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "PPExecName").get(0).getDeviceVariableId();
            String controlStatesvid = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "ControlState").get(0).getDeviceVariableId();
            statusList.add(equipStatussvid);
            statusList.add(pPExecNamesvid);
            statusList.add(controlStatesvid);
            data = activeWrapper.sendS1F3out(statusList, (short) 16);
        } catch (Exception e) {
            logger.error("Wait for get meessage directly error：" + e);
        }
        if (data == null || data.get("SV") == null) {
            return null;
        }
        logger.info("get date from s1f4 reply :" + JsonMapper.toJsonString(data));
        return (ArrayList) data.get("SV");

    }

    // <editor-fold defaultstate="collapsed" desc="S1FX Code">
//    @SuppressWarnings("unchecked")
//    @Override
//    public Map sendS1F3Check() {
//        List listtmp = getNcessaryData();
//        equipStatus = ACKDescription.descriptionStatus(listtmp.get(0).toString(), deviceType);
//        ppExecName = (String) listtmp.get(1);
//        ppExecName = ppExecName.replaceAll(".dbrcp", "");
//        Map panelMap = new HashMap();
//        panelMap.put("EquipStatus", equipStatus);
//        panelMap.put("PPExecName", ppExecName);
//        controlState = ACKDescription.describeControlState(listtmp.get(2), deviceType);
//        panelMap.put("ControlState", controlState);
//        changeEquipPanel(panelMap);
//        // sendS2F15outLearnDevice(151126402L, "disabled");
//        return panelMap;
//    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S2FX Code">

    public Map sendS2F41outPPselect(String recipeName) {
        recipeName = recipeName.replace("00P00", "");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s2f42");
        resultMap.put("deviceCode", deviceCode);
        try {
            Map cpmap = new HashMap();
            cpmap.put(CPN_PPID, recipeName);
            Map cpNameFromatMap = new HashMap();
            cpNameFromatMap.put(CPN_PPID, SecsFormatValue.SECS_ASCII);
            Map cpValueFromatMap = new HashMap();
            cpValueFromatMap.put(recipeName, SecsFormatValue.SECS_ASCII);
            List cplist = new ArrayList();
            cplist.add(CPN_PPID);
            DataMsgMap data = activeWrapper.sendS2F41out(RCMD_PPSELECT, cplist, cpmap, cpNameFromatMap, cpValueFromatMap);
            logger.info("The equip " + deviceCode + " request to PP-select the ppid: " + recipeName);
            byte hcack = (byte) data.get("HCACK");
            logger.info("Receive s2f42in,the equip " + deviceCode + "' requestion get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
            resultMap.put("HCACK", hcack);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=" + hcack + " means " + ACKDescription.description(hcack, "HCACK"));
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("HCACK", 9);
            resultMap.put("Description", "Remote cmd PP-SELECT at equip " + deviceCode + " get a result with HCACK=9 means " + e.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map holdDevice() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        sqlSession.close();
        if (deviceInfoExt != null && "Y".equals(deviceInfoExt.getLockSwitch())) {
            Map cmdMap = this.sendS2f41Cmd("HOST_STP");
            if (cmdMap.get("HCACK").toString().equals("0") || (byte) cmdMap.get("HCACK") == 4) {
                logger.info("锁机成功！");
                this.setAlarmState(2);
                holdSuccessFlag = true;
            } else {
                holdSuccessFlag = false;
            }
            return cmdMap;
        } else {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "在系统中未开启锁机功能！");
            return null;
        }
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="S6FX Code">

    //比对Recipe名称
    @Override
    protected boolean checkRecipeName(String tragetRecipeName) {
//        logger.info("localRecipeName:" + tragetRecipeName.replace("00P00", ""));
//        logger.info(ppExecName);
        return tragetRecipeName.replace("00P00", "").equals(ppExecName);
    }


    @Override
    public void processS6F11in(DataMsgMap data) {
        long ceid = -12345679;
        try {
            if (data.get("CEID") != null) {
                ceid = Long.parseLong(data.get("CEID").toString());
                logger.info("Received a s6f11in with CEID = " + ceid);
            }
            if (ceid == StripMapUpCeid) {
                processS6F11inStripMapUpload(data);
            } else if (ceid == 6 || ceid == 8 || ceid == 9 || ceid == 17
                    || ceid == 18 || ceid == 19 || ceid == 20 || ceid == 21
                    || ceid == 22 || ceid == 45 || ceid == 46) {
                processS6F11EquipStatusChange(data);
            }

            if (commState != 1) {
                this.setCommState(1);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="S7FX Code">

//    /**
//     * 获取下载Recipe的许可，将原有的recipe使用新的名字下载，主要用于测试
//     *
//     * @param targetRecipeName
//     * @return
//     */
//    @Override
//    public Map sendS7F1out(String localFilePath, String targetRecipeName) {
//        Map resultMap = new HashMap();
//        resultMap.put("msgType", "s7f2");
//        resultMap.put("deviceCode", deviceCode);
//        resultMap.put("ppid", targetRecipeName);
//        if (!targetRecipeName.endsWith("csv")) {
//            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "csv格式的文件位解析用文件，不需要下载，请下载prd格式的recipe文件");
//            resultMap.put("ppgnt", 9);
//            resultMap.put("Description", "csv格式的文件位解析用文件，不需要下载，请下载prd格式的recipe文件");
//            return resultMap;
//        }
//
//        long length = TransferUtil.getPPLength(localFilePath);
//        if (length == 0) {
//            resultMap.put("ppgnt", 9);
//            resultMap.put("Description", "读取到的Recipe为空,请联系IT处理...");
//            return resultMap;
//        }
//
//        DataMsgMap data = null;
//
//        try {
//            data = activeWrapper.sendS7F1out(targetRecipeName, length, lengthFormat);
//            byte ppgnt = (byte) data.get("PPGNT");
//            logger.info("Request send ppid= " + targetRecipeName + " to Device " + deviceCode);
//            resultMap.put("ppgnt", ppgnt);
//            resultMap.put("Description", ACKDescription.description(ppgnt, "PPGNT"));
//        } catch (Exception e) {
//            logger.error("Exception:", e);
//            resultMap.put("ppgnt", 9);
//            resultMap.put("Description", e.getMessage());
//        }
//        return resultMap;
//    }


    /**
     * 下载Recipe，将原有的recipe使用指定的PPID下载到机台
     *
     * @param localRecipeFilePath
     * @param targetRecipeName
     * @return
     */
    @Override
    public Map sendS7F3out(String localRecipeFilePath, String targetRecipeName) {
        DataMsgMap data = null;
        byte[] ppbody = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
        targetRecipeName = targetRecipeName.replace("@", "/");
        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f4");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("ppid", targetRecipeName);
        try {
            data = activeWrapper.sendS7F3out(targetRecipeName, ppbody, SecsFormatValue.SECS_BINARY);
            byte ackc7 = (byte) data.get("ACKC7");
            resultMap.put("ACKC7", ackc7);
            resultMap.put("Description", ACKDescription.description(ackc7, "ACKC7"));
            if (0 == ackc7) {
                String targetRecipeName2Analysis = "";
                if (targetRecipeName.endsWith("csv")) {
                    targetRecipeName2Analysis = targetRecipeName.replace("00V00", "00P00").replace("csv", "prd");
                } else {
                    targetRecipeName2Analysis = targetRecipeName.replace("00P00", "00V00").replace("prd", "csv");
                }

                String localRecipe2AnalysisFilePath = localRecipeFilePath.replace(targetRecipeName, targetRecipeName2Analysis);
                Map requestMap = sendS7F1out(localRecipe2AnalysisFilePath, targetRecipeName2Analysis);
                if ("0".equals(String.valueOf(requestMap.get("ppgnt")))) {
                    byte[] ppbody2Analysis = (byte[]) TransferUtil.getPPBody(recipeType, localRecipeFilePath).get(0);
                    requestMap = activeWrapper.sendS7F3out(targetRecipeName2Analysis, ppbody2Analysis, SecsFormatValue.SECS_BINARY);
                    if (requestMap != null) {
                        if ("0".equals(String.valueOf(requestMap.get("ACKC7")))) {
                            UiLogUtil.getInstance().appendLog2EventTab(this.getDeviceCode(), "下载成功！PPID=" + targetRecipeName2Analysis);
                        } else {
                            UiLogUtil.getInstance().appendLog2EventTab(this.getDeviceCode(), "下载失败，PPID=" + targetRecipeName2Analysis + "；原因：" + String.valueOf(requestMap.get("Description")));
                        }
                    } else {
                        UiLogUtil.getInstance().appendLog2EventTab(this.getDeviceCode(), "下载失败，PPID=" + targetRecipeName2Analysis + "；原因：设备未正常回复消息，请检查通讯");
                    }
                } else {
                    UiLogUtil.getInstance().appendLog2EventTab(this.getDeviceCode(), "获取设备下载许可失败，PPID=" + targetRecipeName2Analysis + "，原因：" + String.valueOf(requestMap.get("Description")));
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            resultMap.put("ACKC7", 9);
            resultMap.put("Description", e.getMessage());
        }
        return resultMap;
    }

    @Override
    public Map sendS7F5out(String recipeName) throws UploadRecipeErrorException {
        // 因为csv格式的recipe文件可能不存在，所以先上传csv格式的recipe文件，成功后再上传prd格式的
        String recipeName2Analysis = recipeName.replace("00P00", "00V00").replace("prd", "csv");
        byte[] ppbody2Analysis = (byte[]) getPPBODY(recipeName2Analysis);
        if (null == ppbody2Analysis || ppbody2Analysis.length == 0) {
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "未从设备上获取到" + recipeName + "的参数文件，请确认此参数文件是否已导出");
            Map resultMap = new HashMap();
            resultMap.put("msgType", "s7f6");
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("rcpAnalyseSucceed", "N");
//            resultMap.put("recipe", recipe2Analysis);
//            resultMap.put("recipeParaList", recipeParaList);
//            resultMap.put("recipeFTPPath", this.getRecipeRemotePath(recipe));
            resultMap.put("Descrption", " Can't recive the recipe " + recipeName + " from equip " + deviceCode);
            return null;
        }
        Recipe recipe2Analysis = setRecipe(recipeName2Analysis);
        String recipePath2Analysis = getRecipePathByConfig(recipe2Analysis);
        TransferUtil.setPPBody(ppbody2Analysis, 1, recipePath2Analysis);

        // 上传prd格式的recipe文件
        Recipe recipe = setRecipe(recipeName);
        recipePath = getRecipePathByConfig(recipe);
        byte[] ppbody = (byte[]) getPPBODY(recipeName);
        TransferUtil.setPPBody(ppbody, 1, recipePath);

        logger.debug("Recive S7F6, and the recipe " + recipeName + " has been saved at " + recipePath);

        List<RecipePara> recipeParaList = null;
        try {
            Map paraMap = ShinkawaRecipeUtil.transferFromFile(recipePath2Analysis);
            recipeParaList = ShinkawaRecipeUtil.transferFromDB(paraMap, deviceType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        recipeService.saveUpLoadRcpInfo(recipe2Analysis, recipeParaList, deviceCode);

        String recipeFTPPath = this.getRecipeRemotePath(recipe);
//        uploadRcpFile2FTP(recipePath2Analysis,recipeFTPPath,recipe);

        Map resultMap = new HashMap();
        resultMap.put("msgType", "s7f6");
        resultMap.put("deviceCode", deviceCode);
        resultMap.put("recipe", recipe);
        resultMap.put("recipeNameMapping", null);
        resultMap.put("recipeParaList", recipeParaList);
        resultMap.put("recipeFTPPath", recipeFTPPath);
        resultMap.put("Descrption", " Recive the recipe " + recipeName + " from equip " + deviceCode);
        return resultMap;
    }

    // </editor-fold>
}
