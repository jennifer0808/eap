/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.guiUtil;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;

import java.util.Date;

import cn.tzauto.octopus.gui.main.EapClient;
import javafx.scene.control.*;
import org.apache.log4j.Logger;

/**
 * @author gavin
 */
public class UiLogUtil {


    //为UILog配置单独的日志文件
    private static Logger logger = Logger.getLogger("UILog");

    private static String formateMsg(String deviceCode, String msg) {
        StringBuilder outMsg = new StringBuilder();
        outMsg.append("");
        String deviceInfoMsg = "";
        if (deviceCode != null && !"".equals(deviceCode)) {
            deviceInfoMsg = "<设备:" + deviceCode + ">";
        }
//        outMsg.append("[2017-04-17 17:00:24] ").append(deviceInfoMsg).append(" ").append(msg);
        outMsg.append("[").append(GlobalConstants.dateFormat.format(new Date())).append("] ").append(deviceInfoMsg).append(" ").append(msg);
        return outMsg.toString();
    }

    public static synchronized void appendLog2SeverTab(String deviceCode, String msg) {
        TextArea secsLog = (TextArea) EapClient.root.lookup("#severLog");
        String finalMsg = formateMsg(deviceCode, msg);
        secsLog.appendText(finalMsg + "\n");
//        DialogUtil.AutoNewLine1(secsLog);
        logger.info("[ServerLog]" + finalMsg);
    }

    public static synchronized void appendLog2SecsTab(String deviceCode, String msg) {
        TextArea secsLog = (TextArea) EapClient.root.lookup("#secsLog");
        String finalMsg = formateMsg(deviceCode, msg);
        secsLog.appendText(finalMsg + "\n");
//        DialogUtil.AutoNewLine1(secsLog);
        logger.info("[SecsLog]" + finalMsg);
    }

    public static synchronized void appendLog2EventTab(String deviceCode, String msg) {
        TextArea eventLog = (TextArea) EapClient.root.lookup("#eventLog");
        String finalMsg = formateMsg(deviceCode, msg);
        eventLog.appendText(finalMsg + "\n");
//        DialogUtil.AutoNewLine1(eventLog);
        logger.info("[EventLog]" + finalMsg);
    }



}
