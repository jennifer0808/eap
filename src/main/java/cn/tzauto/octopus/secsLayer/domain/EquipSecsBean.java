package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Created by luosy
 */
public class EquipSecsBean {
    public static Logger sysLogger = Logger.getLogger(EquipSecsBean.class);
    public Map<String, Process> collectionReports;
    public Map<String, SecsParameter> secsParameterMap;
    public String deviceType;
    public String deviceCode;

    public EquipSecsBean(String deviceCode, String deviceType) {
        this.deviceCode = deviceCode;
        this.deviceType = deviceType;
    }

    private boolean init() {
        loadSecsBeanConfig();
        return true;
    }

    private void loadSecsBeanConfig() {
        Properties prop = new Properties();
        try {
            InputStream in = GlobalConstants.class.getClassLoader().getResourceAsStream(deviceCode + ".properties");
            if (in == null) {
                in = GlobalConstants.class.getClassLoader().getResourceAsStream(deviceType + ".properties");
            }
            prop.load(in);
        } catch (Exception e) {
            sysLogger.error("Exception:", e);
        }
    }
}
