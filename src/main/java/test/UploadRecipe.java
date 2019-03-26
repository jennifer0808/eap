/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipeOperationLog;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.sys.domain.SysOffice;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.MessageUtils;
import cn.tzauto.octopus.common.mq.common.MQConstants;
import cn.tzauto.octopus.common.resolver.TransferUtil;
import cn.tzauto.octopus.common.resolver.disco.DiscoRecipeUtil;
import static cn.tzauto.octopus.common.util.ftp.FtpUtil.connectFtp;
import static cn.tzauto.octopus.common.util.ftp.FtpUtil.mkDirs;
import com.alibaba.fastjson.JSON;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author luosy
 */
public class UploadRecipe {

    private static Map<String, String> recipeNameMappingMap = new LinkedHashMap<>();
    String deviceCode = "DS-002";
    private static FTPClient ftp;
    static String ftpip = "192.168.99.137";
    static String ftpPort = "21";
    static String ftpUser = "rms";
    static String ftpPwd = "rms123!";
    static String localRecipePath = "D:\\RECIPE\\636120180525";
    static String ftpPath = "/";
    static MessageUtils C2SRcpUpLoadQueue = new MessageUtils("C2S.Q.RCPUPLOAD");
    static DeviceInfo deviceInfo = null;
    static SysOffice sysOffice = null;

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        uploadRecipe();
//        UploadRecipe up = new UploadRecipe();
//        up.getDeviceInfo();
//        up.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd);
//        String body = "123456,testtest,";
//        TransferUtil.setPPBody(body, 0, "D:\\RECIPE\\DEV.LST_V");
//        //  String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
//        uploadFile("D:\\RECIPE\\DEVID.LST_V", "/RECIPE/123temp/", "DEV.LST_V", "192.168.103.128", ftpPort, ftpUser, ftpPwd);
//        TransferUtil.setPPBody(body, 0, "D:\\RECIPE\\DEVID.LST_V");
//        uploadFile("D:\\RECIPE\\DEVID.LST_V", "/RECIPE/123temp/", "DEVID.LST_V", "192.168.103.128", ftpPort, ftpUser, ftpPwd);

    }

    private static void uploadRecipe() {
        UploadRecipe up = new UploadRecipe();
        up.getDeviceInfo();
        up.connectFtp(ftpip, ftpPort, ftpUser, ftpPwd);
        MQConstants.initConenction();
        C2SRcpUpLoadQueue = new MessageUtils("C2S.Q.RCPUPLOAD");
        try {
            getEquipRecipeList();
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(UploadRecipe.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UploadRecipe.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Map.Entry<String, String> entry : recipeNameMappingMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            up.uploadRecipe(key);
        }
        System.out.println("over");
        if (ftp.isConnected()) {
            try {
                ftp.logout();
                ftp.disconnect();
            } catch (Exception e) {
//                    logger.error(e.getMessage());
            }
        }
        System.exit(0);
    }

    public void getDeviceInfo() {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        deviceInfo = deviceService.getDeviceInfoByDeviceCode(deviceCode).get(0);
        sysOffice = new RecipeService(sqlSession).sysOfficeMapper.selectSysOfficeByPrimaryKey(deviceInfo.getOfficeId());
        sqlSession.close();
    }

    public Map uploadRecipe(String recipeName) {
        Map resultMap = new HashMap();

        try {

            String equipRecipeName = recipeNameMappingMap.get(recipeName);
            // ftp ip user pwd lpath rpath "mput 001.ALU 001.CLN 001.DFD" 
            Recipe recipe = setRecipe(recipeName);
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            String ftpRecipePath = organizeUploadRecipePath(recipe);

            sqlSession.close();
            String body = equipRecipeName + "," + recipeName + ",";
            TransferUtil.setPPBody(body, 0, localRecipePath + "\\" + "DEV.LST_V" + recipe.getVersionNo());
            //  String recipePathTem = recipePath.substring(0, recipePath.lastIndexOf("/") + 1) + str + "_V" + recipe.getVersionNo() + ".txt";
//           uploadFile(localRecipePath + ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo(), ftpPath + deviceCode + recipeName + "temp/", "DEV.LST_V" + recipe.getVersionNo(), localftpip, ftpPort, ftpUser, ftpPwd);
            TransferUtil.setPPBody(body, 0, localRecipePath + "\\" + "DEVID.LST_V" + recipe.getVersionNo());
//           uploadFile(localRecipePath + ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo(), ftpPath + deviceCode + recipeName + "temp/", "DEVID.LST_V" + recipe.getVersionNo(), localftpip, ftpPort, ftpUser, ftpPwd);

            boolean ocrUploadOk = true;

            List<RecipePara> recipeParaList = new ArrayList<>();
            try {
                Map paraMap = DiscoRecipeUtil.transferFromFile(localRecipePath + "\\" + equipRecipeName + ".DFD");
                if (paraMap != null && !paraMap.isEmpty()) {
                    recipeParaList = DiscoRecipeUtil.transferFromDB(paraMap, "DISCODFD6361Z1WS");
                } else {
                    System.out.println("解析recipe时出错,recipe文件不存在");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            resultMap.put("recipe", recipe);
            resultMap.put("deviceCode", deviceCode);
            resultMap.put("recipeFTPPath", ftpRecipePath);
            resultMap.put("recipeParaList", setParasRCProwId(recipeParaList, recipe.getId()));

            if (!ocrUploadOk) {
//                    UiLogUtil.appendLog2EventTab(deviceCode, "上传Recipe:" + recipeName + " 时,FTP连接失败,请检查FTP服务是否开启.");
                resultMap.put("uploadResult", "上传失败,上传Recipe:" + recipeName + " 时,FTP连接失败.");
            }

            uploadRcpFile2FTP(localRecipePath, ftpRecipePath, recipe);
            RecipeOperationLog recipeOperationLog = setRcpOperationLog(recipe, "Upload");
            List<RecipeOperationLog> recipeOperationLogs = new ArrayList<>();
            recipeOperationLogs.add(recipeOperationLog);
            List<Recipe> recipeList = new ArrayList();
            recipeList.add(recipe);
            sendUploadInfo2Server(deviceCode, recipeList, recipeParaList, recipeOperationLogs, getRecipeAttachInfo(recipe));
        } catch (Exception e) {
            System.out.println("Get equip status error:" + e.getMessage());

        }

        return resultMap;

    }

    public List<Attach> getRecipeAttachInfo(Recipe recipe) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        String recipeName = recipe.getRecipeName();
        String ftpRecipePath = organizeUploadRecipePath(recipe);
        List<Attach> attachs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Attach attach = new Attach();
            attach.setId(UUID.randomUUID().toString());
            attach.setRecipeRowId(recipe.getId());
            attach.setAttachPath(ftpRecipePath);
            if (i == 0) {
                attach.setAttachName("DEV.LST_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + "DEV.LST_V" + recipe.getVersionNo());
            } else if (i == 1) {
                attach.setAttachName("DEVID.LST_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + "DEVID.LST_V" + recipe.getVersionNo());
            } else if (i == 2) {
                attach.setAttachName(recipeName + ".ALU_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".ALU_V" + recipe.getVersionNo());
            } else if (i == 3) {
                attach.setAttachName(recipeName + ".CLN_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".CLN_V" + recipe.getVersionNo());
            } else if (i == 4) {
                attach.setAttachName(recipeName + ".DFD_V" + recipe.getVersionNo());
//                attach.setAttachPath(ftpRecipePath + recipeName + ".DFD_V" + recipe.getVersionNo());
            }
            attach.setAttachType("");
            attach.setSortNo(0);
            attach.setCreateBy("System");
            attach.setUpdateBy("System");
            attachs.add(attach);
        }
        sqlSession.close();
        return attachs;
    }

    public static Map getEquipRecipeList() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        List<String> recipeNameList = new ArrayList<>();
        Map eppd = new HashMap();
        eppd.put("eppd", recipeNameList);
        File file = new File("D:\\RECIPE\\636120180525\\devid.LST");

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
        String cfgline = "";
        while ((cfgline = br.readLine()) != null) {
            if (cfgline.contains(",")) {
                String[] recipeNameMappings = cfgline.split(",");
                for (int i = 0; i < recipeNameMappings.length; i++) {
                    if (i == recipeNameMappings.length - 1) {
                        break;
                    }
                    recipeNameMappingMap.put(recipeNameMappings[i + 1], recipeNameMappings[i]);
                    recipeNameList.add(recipeNameMappings[i + 1]);
                    i = i + 1;

                }
            }
        }

        eppd.put("eppd", recipeNameList);
        return eppd;
    }

    protected Recipe setRecipe(String recipeName) {
        Recipe recipe = new Recipe();

//        DeviceType deviceType = deviceService.getDeviceTypeMap().get(deviceInfo.getDeviceTypeId());
        recipe.setClientId("");
        String rcpNewId = UUID.randomUUID().toString();
        recipe.setId(rcpNewId);
        recipe.setCreateBy("System");
        recipe.setUpdateBy("System");

        recipe.setDeviceCode(deviceInfo.getDeviceCode());
        recipe.setDeviceId(deviceInfo.getId());
        recipe.setDeviceName(deviceInfo.getDeviceName());
        recipe.setDeviceTypeCode(deviceInfo.getDeviceType());
        recipe.setDeviceTypeId(deviceInfo.getDeviceTypeId());
        recipe.setDeviceTypeName(deviceInfo.getDeviceType());
        recipe.setProdCode("");//LGA
        recipe.setProdId("");//LGA001-12138
        recipe.setProdName("");//LGA_0.48*0.68
        recipe.setRecipeCode("");
        recipe.setRecipeDesc("");
        recipe.setRecipeName(recipeName);
        recipe.setRecipeStatus("Create");
        recipe.setRecipeType("N");
        recipe.setSrcDeviceId("");
        //如果已存在，版本号加1   

        recipe.setVersionNo(0);
        recipe.setTotalCnt(0);
        recipe.setUpdateCnt(0);
        recipe.setVersionType("Engineer");
        recipe.setCreateDate(new Date());
        return recipe;
    }

    public boolean uploadRcpFile2FTP(String localRcpPath, String remoteRcpPath, Recipe recipe) {
        String recipeName = recipe.getRecipeName();
//        String[] equipRecipeNames = recipeNameMappingMap.get(recipeName).split(":");
        String equipRecipeName = recipeNameMappingMap.get(recipeName);

        uploadFile(localRecipePath + "\\" + "DEV.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEV.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

        uploadFile(localRecipePath + "\\" + "DEVID.LST_V" + recipe.getVersionNo(), remoteRcpPath, "DEVID.LST_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);

        uploadFile(localRecipePath + ftpPath + equipRecipeName + ".ALU", remoteRcpPath, recipeName + ".ALU_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        uploadFile(localRecipePath + ftpPath + equipRecipeName + ".CLN", remoteRcpPath, recipeName + ".CLN_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
        uploadFile(localRecipePath + ftpPath + equipRecipeName + ".DFD", remoteRcpPath, recipeName + ".DFD_V" + recipe.getVersionNo(), ftpip, ftpPort, ftpUser, ftpPwd);
//        UiLogUtil.appendLog2EventTab(recipe.getDeviceCode(), "Recipe文件存储位置：" + localRecipePath + remoteRcpPath);

        return true;
    }

    public RecipeOperationLog setRcpOperationLog(Recipe recipe, String operationType) {
        RecipeOperationLog recipeOperationLog = new RecipeOperationLog();

        recipeOperationLog.setOperatorBy("System");

        recipeOperationLog.setOperationDate(new Date());
        recipeOperationLog.setDeviceCode(recipe.getDeviceCode());
        recipeOperationLog.setDeviceStatus("");
        recipeOperationLog.setRecipeRowId(recipe.getId());
        switch (operationType) {
            case "upload":
                recipeOperationLog.setOperationType("Upload");
                recipeOperationLog.setOperationResultDesc("上传Recipe： " + recipe.getRecipeName() + " 到工控机：" + GlobalConstants.getProperty("clientId"));
                break;
            case "download":
                recipeOperationLog.setOperationType("Download");
                recipeOperationLog.setOperationResultDesc("手动下载Recipe： " + recipe.getRecipeName() + " 到机台：" + recipe.getDeviceCode());
                break;
            case "mesdownload":
                recipeOperationLog.setOperationType("MESDownload");
                recipeOperationLog.setOperationResultDesc("自动下载Recipe： " + recipe.getRecipeName() + " 到机台：" + recipe.getDeviceCode());
                break;
        }
        return recipeOperationLog;
    }

    private void sendUploadInfo2Server(String deviceCode, List<Recipe> recipes, List<RecipePara> recipeParas, List<RecipeOperationLog> recipeOperationLogs, List<Attach> attachs) {
        Map mqMap = new HashMap();
        mqMap.put("msgName", "Upload");
        mqMap.put("deviceCode", deviceCode);
        mqMap.put("recipe", JSON.toJSONString(recipes));
        mqMap.put("recipePara", JSON.toJSONString(recipeParas));
        mqMap.put("recipeOperationLog", JSON.toJSONString(recipeOperationLogs));
        mqMap.put("attach", JSON.toJSONString(attachs));
        mqMap.put("eventId", getReplyMessage());

        C2SRcpUpLoadQueue.sendMessage(mqMap);
//        GlobalConstants.sysLogger.info(" MQ发送记录：Recipe= " + JSON.toJSONString(recipes) + " recipePara= " + JSON.toJSONString(recipeParas) + " recipeOperationLog= " + JSON.toJSONString(recipeOperationLogs));
    }

    public static String getReplyMessage() {

        try {
            String endpoint = "http://192.168.99.130/cim/services/idGenService";
//            String endpoint =  "/idGenService"; 
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            String sendMessage = "22";
            call.setOperationName("genId");
            String replyMeaage = String.valueOf(call.invoke(new Object[]{sendMessage}));
            return replyMeaage;
        } catch (Exception e) {

            return null;
        }
    }

    public String organizeUploadRecipePath(Recipe recipe) {
        String returnPath = "";
        returnPath = "/RECIPE/" + sysOffice.getPlant() + "/" + sysOffice.getName() + "/" + deviceInfo.getDeviceType() + "/" + recipe.getVersionType() + "/" + deviceInfo.getDeviceCode() + "/" + recipe.getRecipeName().replace("/", "@").replace("\\", "@") + "/";
        return returnPath;
    }

    private List<RecipePara> setParasRCProwId(List<RecipePara> recipeParas, String recipeRowId) {
        for (RecipePara recipePara : recipeParas) {
            recipePara.setRecipeRowId(recipeRowId);
//            recipePara.setCreateBy(GlobalConstants.sysUser.getId());
//            recipePara.setUpdateBy(GlobalConstants.sysUser.getId());
            recipePara.setCreateBy("SYSTEM");
            recipePara.setUpdateBy("SYSTEM");
            recipePara.setId(UUID.randomUUID().toString());
        }
        return recipeParas;
    }

    public static boolean uploadFile(String localFilePath, String remoteFilePath, String fileName, String serverIp, String serverPort, String userName, String password) {

        File file = new File(localFilePath);
        if (!file.exists() && !file.isFile()) {
            System.out.println("本地文件不存在！>>" + localFilePath);
            return false;
        }
//        if (file.length() == 0) {
//            logger.debug("本地文件为空！>>" + localFilePath);
//            return false;
//        }
//        logger.debug("FTP连接成功!");
        boolean uploadflag = false;
        try {
            FileInputStream input = new FileInputStream(file);
//            logger.info("开始上传文件，远程路径为:" + remoteFilePath + "，文件名称为:" + fileName);
            boolean mkdirFlag = mkDirs(remoteFilePath, ftp);
//            logger.info("创建路径完毕:" + remoteFilePath + "结果:>>" + mkdirFlag);
            ftp.enterLocalPassiveMode();//切换FTP工作方式为passive，此行代码很重要。
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
//            ftp.makeDirectory(remoteFilePath);
            ftp.changeWorkingDirectory(remoteFilePath);

            uploadflag = ftp.storeFile(remoteFilePath + fileName, input);
            int replycode = ftp.getReplyCode();
//            logger.debug("保存文件Ftp-Reply:" + replycode);
            input.close();
        } catch (Exception e) {
            //logger.error(e.getMessage());
        } finally {

        }
        return uploadflag;
    }

    public static boolean connectFtp(String serverIp, String serverPort, String userName, String password) {
        try {
            ftp = new FTPClient();
            int reply;
            ftp.connect(InetAddress.getByName(serverIp), Integer.parseInt(serverPort));
            ftp.login(userName, password);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return false;
            }
            return true;
        } catch (Exception e) {
//            logger.error("Exception:", e);
            return false;
        }
    }

}
