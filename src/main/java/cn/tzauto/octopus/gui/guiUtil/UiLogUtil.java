/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.gui.guiUtil;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author gavin
 */
public class UiLogUtil {

    private PropertyChangeSupport propertySupport;
    private String eventmsgProperty;
    private String servermsgProperty;
    private String secsmsgProperty;

    public final static String EVENT_LOG_PROPERTY = "EVENT_LOG_PROPERTY";
    public final static String SECS_LOG_PROPERTY = "SECS_LOG_PROPERTY";
    public final static String SERVER_LOG_PROPERTY = "SERVER_LOG_PROPERTY";


    //为UILog配置单独的日志文件
    private Logger logger = Logger.getLogger("UILog");

    private static volatile UiLogUtil singleton;

    private UiLogUtil() {
        propertySupport = new PropertyChangeSupport(this);
    }

    public static UiLogUtil getInstance() {
        if (singleton == null) {
            synchronized (UiLogUtil.class) {
                if (singleton == null) {
                    singleton = new UiLogUtil();
                }
            }
        }
        return singleton;
    }

    private String formateMsg(String deviceCode, String msg) {
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


    public void appendLog2SeverTab(String deviceCode, String msg) {
//        TextArea secsLog = (TextArea) EapClient.root.lookup("#severLog");
        String finalMsg = formateMsg(deviceCode, msg);
//        secsLog.appendText(finalMsg + "\n");
        logger.info("[ServerLog]" + finalMsg);
        setServerMsgProperty(finalMsg);
    }

    public void appendLog2SecsTab(String deviceCode, String msg) {
//        TextArea secsLog = (TextArea) EapClient.root.lookup("#secsLog");
        String finalMsg = formateMsg(deviceCode, msg);
        logger.info("[SecsLog]" + finalMsg);
        setSecsMsgProperty(finalMsg);
    }

    public void appendLog2EventTab(String deviceCode, String msg) {
        String finalMsg = formateMsg(deviceCode, msg);
        logger.info("[EventLog]" + finalMsg);
        setEventMsgProperty(finalMsg);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    private void setEventMsgProperty(String value) {
        String oldValue = eventmsgProperty;
        eventmsgProperty = value;
        logger.debug(EVENT_LOG_PROPERTY + "Property has been changed, old value = " + oldValue + " new value =" + value);
        propertySupport.firePropertyChange(EVENT_LOG_PROPERTY, oldValue, eventmsgProperty);
    }

    private void setSecsMsgProperty(String value) {
        String oldValue = secsmsgProperty;
        secsmsgProperty = value;
        logger.debug(SECS_LOG_PROPERTY + "Property has been changed, old value = " + oldValue + " new value =" + value);
        propertySupport.firePropertyChange(SECS_LOG_PROPERTY, oldValue, secsmsgProperty);
    }

    private void setServerMsgProperty(String value) {
        String oldValue = servermsgProperty;
        servermsgProperty = value;
        logger.debug(SERVER_LOG_PROPERTY + "Property has been changed, old value = " + oldValue + " new value =" + value);
        propertySupport.firePropertyChange(SERVER_LOG_PROPERTY, oldValue, servermsgProperty);
    }

    public String getEventmsgProperty() {
        return eventmsgProperty;
    }

    public void setEventmsgProperty(String eventmsgProperty) {
        this.eventmsgProperty = eventmsgProperty;
    }

    public static void appendText(TextArea logArea, String msg) {
        ObservableList<CharSequence> logs = logArea.getParagraphs();
        StringBuilder builder = new StringBuilder();
        if (logs.size() > 100) {
            ArrayList<CharSequence> tmpLogs = new ArrayList<>();
            tmpLogs.addAll(logs);
            tmpLogs.remove(logs.size() - 1);
            //超过100删30
            ArrayList<CharSequence> tmpList = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                tmpList.add(logs.get(i));
            }
            tmpLogs.removeAll(tmpList);
            for (CharSequence str : tmpLogs) {
                builder.append(str.toString() + "\n");
            }
            logArea.clear();
        }
        builder.append(msg);
        logArea.appendText(builder.toString());

    }

    public String getServermsgProperty() {
        return servermsgProperty;
    }

    public void setServermsgProperty(String servermsgProperty) {
        this.servermsgProperty = servermsgProperty;
    }

    public String getSecsmsgProperty() {
        return secsmsgProperty;
    }

    public void setSecsmsgProperty(String secsmsgProperty) {
        this.secsmsgProperty = secsmsgProperty;
    }
}
