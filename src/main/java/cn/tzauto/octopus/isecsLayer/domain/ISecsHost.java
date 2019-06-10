package cn.tzauto.octopus.isecsLayer.domain;

import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.secsLayer.util.FengCeConstant;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ISecsHost implements ISecsInterface {

    private static Logger logger = Logger.getLogger(ISecsHost.class);

    public ISecsConnection iSecsConnection;
    private List<ISecsPara> paraList;
    private Map<String, ScreenDomain> screenMap;
    public boolean isConnect;
    public String ip;
    private String port;
    public String deviceCode;
    public String deviceTypeCode;
    private int tryCount = 0;

    public ISecsHost(String ip, String port, String deviceTypeCode, String deviceCode) {
        this.iSecsConnection = new ISecsConnection(ip, port);
        //初始化paraList
        initParaList(deviceTypeCode);
        //初始化screenMap
        initScreenInfo(deviceTypeCode);
        if (iSecsConnection != null && iSecsConnection.getSocketClient() != null) {
            this.isConnect = iSecsConnection.getSocketClient().isConnected();
        }
        this.ip = ip;
        this.port = port;
        this.deviceCode = deviceCode;
        this.deviceTypeCode = deviceTypeCode;
    }

    /**
     * 根据传入的命令执行，并且返回参数
     *
     * @param command
     * @return
     */
    @Override
    public List<String> executeCommand(String command) {
        List<String> result = new ArrayList<String>();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(iSecsConnection.getSocketClient().getOutputStream()));
            logger.info(deviceCode + " Ready to execute command==>" + command);
            writer.write(command);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(iSecsConnection.getSocketClient().getInputStream()));
            String lineContent = "";
            while ((lineContent = reader.readLine()) != null) {
                logger.info(deviceCode + " success get reply==>" + lineContent);
                if (lineContent.trim().contains("done")) {
                    result.add(lineContent);
                    logger.info("get done flag,and ignore");
                    break;
                }
                if (!"done".equals(lineContent.trim())) {
                    result.add(lineContent);
                } else {
                    logger.info("get done flag,and ignore");
                    break;
                }
            }
            logger.info(deviceCode + " execute command [" + command + "] success and get reply==>" + result);
            for (String str : result) {
                if (str.contains("Error 0001: Invalid Command")) {
                    iSecsConnection.getSocketClient().close();
                    tryCount++;
                    logger.info(deviceCode + " execute command [" + command + "]error ,try again");
                    if (tryCount > 0) {
                        tryCount = 0;
                        result.clear();
                        result.add("error");
                        return result;
                    }
                    try {
                        iSecsConnection = new ISecsConnection(ip, port);
                        tryCount++;
                        if (iSecsConnection.getSocketClient() == null) {
                            result.clear();
                            result.add("error");
                            return result;
                        }
                    } catch (Exception ex) {
                    }
                    return executeCommand(command);
                }
            }
            return result;
        } catch (IOException e) {
            logger.error(deviceCode + " execute command fail==>" + e.getMessage());
            // result.add("socket error");
            try {
                iSecsConnection = new ISecsConnection(ip, port);
                tryCount++;
                if (iSecsConnection.getSocketClient() == null) {
                    result.clear();
                    result.add("error");
                    return result;
                }
            } catch (Exception ex) {
            }
            if (tryCount > 0) {
                tryCount = 0;
                result.clear();
                result.add("error");
                return result;
            }
            return executeCommand(command);
            // return result;
        }

    }


    /**
     * 根据传入的命令执行，并且返回参数
     *
     * @param command
     * @param charsetName
     * @return
     */
    public List<String> executeCommand(String command, String charsetName) {
        List<String> result = new ArrayList<String>();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(iSecsConnection.getSocketClient().getOutputStream()));
            logger.info(deviceCode + " Ready to execute command==>" + command);
            writer.write(command);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(iSecsConnection.getSocketClient().getInputStream(), charsetName));
            String lineContent = "";
            while ((lineContent = reader.readLine()) != null) {
                logger.info(deviceCode + " success get reply==>" + lineContent);
                if (lineContent.trim().contains("done")) {
                    result.add(lineContent);
                    logger.info("get done flag,and ignore");
                    break;
                }
                if (!"done".equals(lineContent.trim())) {
                    result.add(lineContent);
                } else {
                    logger.info("get done flag,and ignore");
                    break;
                }
            }
            logger.info(deviceCode + " execute command [" + command + "] success and get reply==>" + result);
            for (String str : result) {
                if (str.contains("Error")) {
                    tryCount++;
                    logger.info(deviceCode + " execute command [" + command + "]error ,try again");
                    if (tryCount > 0) {
                        tryCount = 0;
                        result.clear();
                        result.add("error");
                        return result;
                    }
                    return executeCommand(command, charsetName);
                }
            }
            return result;
        } catch (IOException e) {
            logger.error(deviceCode + " Execuet command fail==>" + e.getMessage());
            // result.add("socket error");
            try {
                iSecsConnection = new ISecsConnection(ip, port);
                tryCount++;
                if (iSecsConnection.getSocketClient() == null) {
                    result.clear();
                    result.add("error");
                    return result;
                }
            } catch (Exception ex) {
            }
            if (tryCount > 0) {
                tryCount = 0;
                result.clear();
                result.add("error");
                return result;
            }
            return executeCommand(command, charsetName);
            // return result;
        }

    }

    private String gotoScreen(String screenName, int repeatCnt) {
        if (repeatCnt <= 0) {
            repeatCnt = 1;
        } else if (repeatCnt > 5) {
            repeatCnt = 5;
        }
        int i = 0;
        String actionResult = "";
        while (i < repeatCnt) {
            actionResult = gotoScreen(screenName);
            if (!"done".equals(actionResult)) {
                actionResult = gotoScreen(screenName);
            }
        }
        return actionResult;
    }

    @Override
    public String gotoScreen(String screenName) {
        try {
            return executeCommand("goto " + screenName).get(0);
        } catch (Exception e) {
            return "";
        }

    }

    @Override
    public String getPara(String paraName) {
        try {
            return executeCommand("read " + paraName).get(0);
        } catch (Exception e) {
            return "";
        }

    }

    public Map getMuiltiPara(Map<String, String> paraMap) {
        Map<String, String> resultMap = new HashMap<>();
        //首先查询参数对应的IsecsPara的屏幕所在
        Map<String, List<String>> screenParaMap = new HashMap<>();
        for (Map.Entry<String, String> entry : paraMap.entrySet()) {
            String paraCode = entry.getKey();
            String paraType = entry.getValue();
            if (paraType.equalsIgnoreCase("isecs")) {
                //获取isecsPara的属性信息
                ISecsPara targetPara = getParaByCode(paraCode);
                if (targetPara != null) {
                    if (screenParaMap.get(targetPara.getScreenName()) != null) {
                        screenParaMap.get(targetPara.getScreenName()).add(targetPara.getIsecsParaName());
                    } else {
                        List paraList = new ArrayList<String>();
                        paraList.add(targetPara.getIsecsParaName());
                        screenParaMap.put(targetPara.getScreenName(), paraList);
                    }
                }
            }
        }
        //遍历屏幕，并取值
        for (Map.Entry<String, List<String>> screenPara : screenParaMap.entrySet()) {
            String screenName = screenPara.getKey();
            List<String> paraList = screenPara.getValue();
            //获取isecsPara的属性信息
            ScreenDomain screenDomain = screenMap.get(screenName);
            if (screenDomain != null) {
                //首先跳转到指定屏幕
                String gotoResult = gotoScreen(screenName, screenDomain.getCommandRepeatNum());
                //然后去屏幕上读取数据
                if (gotoResult.equals("done")) {
                    resultMap.putAll(readDataMuilti(screenDomain.getReadMode(), paraList, screenDomain.getBeforeReadDelay(), 500));
                }
            } else {
                logger.error("can not find this screen info==>" + screenName);
            }
        }
        return resultMap;
    }

    public Map<String, String> readDataMuilti(String readMode, List<String> paraList, long beforeRead, long readInteval) {
        Map<String, String> resultMap = new HashMap<String, String>();
        try {
            int i = 0;
            Thread.sleep(beforeRead);
            //采用一次读取多行模式
            if ("M".equals(readMode.toUpperCase())) {
                StringBuffer readCommand = new StringBuffer("readm");
                for (String paraCode : paraList) {
                    readCommand.append(" " + paraCode);
                }
                List<String> readResult = executeCommand(readCommand.toString());
                if (readResult.size() > 0 && readResult.size() == paraList.size()) {
                    logger.info("成功获取到读取结果长度为" + readResult.size());
                    for (int j = 0; j < paraList.size(); j++) {
                        resultMap.put(paraList.get(i), readResult.get(i));
                    }
                } else {
                    logger.info("获取到结果长度异常,");
                }
            } else {
                StringBuffer readCommand = new StringBuffer("read");
                for (String paraCode : paraList) {
                    readCommand.append(" " + paraCode);
                    List<String> readResult = executeCommand(readCommand.toString());
                    if (readResult.size() > 0) {
                        resultMap.put(paraCode, readResult.get(0));
                    } else {
                        logger.info("获取到结果长度异常,");
                    }
                }

            }
        } catch (Exception e) {
            logger.error("获取数据失败===>" + e.getMessage());
        }
        return resultMap;
    }

    private ISecsPara getParaByCode(String paraCode) {
        for (ISecsPara iSecsPara : paraList) {
            if (paraCode.equals(iSecsPara.getParaCode())) {
                return iSecsPara;
            }
        }
        return null;
    }

    @Override
    public boolean sendMessage(String msgContent) {
        String messageResult = executeCommand("message " + msgContent).get(0);
        if ("done".equals(messageResult)) {
            logger.info("message " + msgContent + " success!");
            return true;
        } else {
            logger.error("message " + msgContent + " fail!");
            return false;
        }
    }

    private void initParaList(String deviceTypeCode) {
    }

    private void initScreenInfo(String deviceTypeCode) {
    }

    public static void main(String[] args) {
        ISecsHost iSecsHost = new ISecsHost("192.168.103.128", "12002", "", "test");
        System.out.println(iSecsHost.isConnect);
//        iSecsHost.executeCommand("curscreesn");
////        new Thread(new Runnable() {
////            @Override
////            public void run() {
////                for (int i = 0; i < 1000; i++) {
////                    iSecsHost.executeCommand("curscreesn");
////                    iSecsHost.executeCommand("curscreen");
////                    iSecsHost.executeCommand("read name");
////                    iSecsHost.executeCommand("curscreen");
////                    iSecsHost.executeCommand("read name");
////                    iSecsHost.executeCommand("curscreen");
////                    iSecsHost.executeCommand("read name");
////
////                }
////            }
////        }).start();
////        synchronized (iSecsHost.iSecsConnection.getSocketClient()) {
//
//        for (int i = 0; i < 3000; i++) {
//            iSecsHost.executeCommand("curscreesn");
//            iSecsHost.executeCommand("curscreen");
//            iSecsHost.executeCommand("read name");
//            iSecsHost.executeCommand("curscreen");
//            iSecsHost.executeCommand("read name");
//            iSecsHost.executeCommand("curscreen");
//            iSecsHost.executeCommand("read name");
//
////            }
//        }

//        iSecsHost.checkConenctionStatus();
//         iSecsHost.executeCommand("dos  $start  D:\\RECIPE\\spyxx.exe $");
    }

    public Map<String, String> readJasonData(List<String> paraNameList) {
        if (paraNameList == null || paraNameList.isEmpty()) {
            return null;
        }
        Map<String, String> resultMap = new HashMap<>();
        StringBuilder readCommand = new StringBuilder("readjson");
        for (String paraCode : paraNameList) {
            readCommand.append(" ");
            readCommand.append(paraCode);
        }
        List<String> readResult = executeCommand(readCommand.toString());
        if (readResult.size() > 0) {
            for (int j = 0; j < readResult.size(); j++) {
                String readResultStr = readResult.get(j);
                if (readResultStr.contains("done")) {
                    readResultStr = readResultStr.replaceAll("done", "");
                }
                readResultStr = readResultStr.trim();
                if ("".equals(readResultStr)) {
                    continue;
                }
                try {
                    resultMap = (HashMap) JsonMapper.fromJsonString(readResultStr, HashMap.class);
                } catch (Exception e) {
                    logger.error(deviceCode + " jsonstring parse to hashmap error");
                    return resultMap;
                }
            }
        }
        return resultMap;
    }

    public Map<String, String> readJasonData(String[] paraNameList) {
        Map<String, String> resultMap = new HashMap<>();
        StringBuilder readCommand = new StringBuilder("readjson");
        for (String paraCode : paraNameList) {
            readCommand.append(" ");
            readCommand.append(paraCode);
        }
        List<String> readResult = executeCommand(readCommand.toString());
        if (readResult.size() > 0) {
            for (int j = 0; j < readResult.size(); j++) {
                String readResultStr = readResult.get(j);
                if (readResultStr.contains("done")) {
                    readResultStr = readResultStr.replaceAll("done", "");
                }
                readResultStr = readResultStr.trim();
                if ("".equals(readResultStr)) {
                    continue;
                }
                try {
                    resultMap = (HashMap) JsonMapper.fromJsonString(readResultStr, HashMap.class);
                } catch (Exception e) {
                    logger.error(deviceCode + " jsonstring parse to hashmap error");
                    return resultMap;
                }
            }
        }
        return resultMap;
    }

    public Map<String, String> readJasonData(String paraListStr) {
        Map<String, String> resultMap = new HashMap<>();
        List<String> readResult = executeCommand("readjson " + paraListStr);
        if (readResult.size() > 0) {
            for (int j = 0; j < readResult.size(); j++) {
                String readResultStr = readResult.get(j);
                if (readResultStr.contains("done")) {
                    readResultStr = readResultStr.replaceAll("done", "");
                }
                readResultStr = readResultStr.trim();
                if ("".equals(readResultStr)) {
                    continue;
                }
                try {
                    resultMap = (HashMap) JsonMapper.fromJsonString(readResultStr, HashMap.class);
                } catch (Exception e) {
                    logger.error(deviceCode + " jsonstring parse to hashmap error");
                    return resultMap;
                }
            }
        }
        return resultMap;
    }

    public Map<String, String> readJasonData(Map<String, String> paraNameMap) {
        Map<String, String> resultMap = new HashMap<>();
        StringBuilder readCommand = new StringBuilder("readjson");
        for (Map.Entry<String, String> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            readCommand.append(" ");
            readCommand.append(key);
        }
        List<String> readResult = executeCommand(readCommand.toString());
        if (readResult.size() > 0) {
            for (int j = 0; j < readResult.size(); j++) {
                String readResultStr = readResult.get(j);
                if (readResultStr.contains("done")) {
                    readResultStr = readResultStr.replaceAll("done", "");
                }
                readResultStr = readResultStr.trim();
                if ("".equals(readResultStr)) {
                    continue;
                }
                try {
                    resultMap = (HashMap) JsonMapper.fromJsonString(readResultStr, HashMap.class);
                } catch (Exception e) {
                    logger.error(deviceCode + " jsonstring parse to hashmap error");
                    return resultMap;
                }
            }
        }
        return resultMap;
    }

    public boolean checkConenctionStatus() {
        synchronized (iSecsConnection.getSocketClient()) {
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(iSecsConnection.getSocketClient().getOutputStream()));
                writer.write("curscreen");
                writer.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(iSecsConnection.getSocketClient().getInputStream()));
                String lineContent = "";
                while ((lineContent = reader.readLine()) != null) {
                    return true;
                }
            } catch (Exception e) {
                logger.error(deviceCode + " jsonstring parse to hashmap error");
                logger.error(e);
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public Map<String, String> readAllParaByScreen(String screenName) {
        List<String> screenResult = executeCommand("curscreen");
        String screenNameNow = screenResult.get(0);
        if (!screenName.equals(screenNameNow)) {
            logger.info("欲取值屏幕与当前屏幕不一致,当前屏幕:" + screenNameNow + "欲取值屏幕:" + screenName);
            return new HashMap<>();
        }
        Map<String, String> resultMap = new HashMap<>();
        List<String> readResult = executeCommand("readbyscreen " + screenName);
        if (readResult.size() > 0) {
            for (int j = 0; j < readResult.size(); j++) {
                String readResultStr = readResult.get(j);
                if (readResultStr.contains("done")) {
                    readResultStr = readResultStr.replaceAll("done", "");
                }
                readResultStr = readResultStr.trim();
                if ("".equals(readResultStr)) {
                    continue;
                }
                try {
                    resultMap = (HashMap) JsonMapper.fromJsonString(readResultStr, HashMap.class);
                    logger.debug("readAllParaByScreen： " + readResult);
                } catch (Exception e) {
                    logger.error(deviceCode + " jsonstring parse to hashmap error");
                    return resultMap;
                }
            }
        }
        return resultMap;
    }
    public byte[] executeCommand2(String command) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(iSecsConnection.getSocketClient().getOutputStream()));
            logger.info(deviceCode + " Ready to execute command==>" + command);
            writer.write(command);
            writer.flush();
            byte[] bytes = new byte[1024];
            InputStream inputStream = iSecsConnection.getSocketClient().getInputStream();
            inputStream.read(bytes);
            System.out.println("=======================");
            System.out.println(new String(bytes, "GBK"));
        } catch (Exception e) {

        }
        return null;
    }

    public String executeCommand3(String command) {
        MDC.put(FengCeConstant.WHICH_EQUIPHOST_CONTEXT, deviceCode);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(iSecsConnection.getSocketClient().getOutputStream()));
            logger.info(deviceCode + " Ready to execute command==>" + command);
            writer.write(command.toString());
            writer.flush();
            byte[] bytes = new byte[1024];
            InputStream inputStream = iSecsConnection.getSocketClient().getInputStream();
            System.out.println("=======================");
            System.out.println(new String(bytes, "GBK"));
            logger.info("已经将参数发送至wafer软件:"+command);
        } catch (Exception e) {
            System.out.print("111111");
        }
        return null;
    }
}
