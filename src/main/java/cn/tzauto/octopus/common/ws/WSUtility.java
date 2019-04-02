/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.ws;


import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.log4j.Logger;


@SuppressWarnings("JavaDoc")
public class WSUtility {

    private static final Logger logger = Logger.getLogger(WSUtility.class);

    /**
     * Method binGet is used to get information of 2dbin
     *
     * @param stripid: Stripid of expected 2dbin
     * @return 2dbin information in Json format
     */
    public static String binGet(String stripid, String deviceCode) {
        try {
            Object[] params = new Object[2];
            params[0] = stripid;
            params[1] = deviceCode;
            Object[] results = GlobalConstants.mapBinClient.invoke("downLoad2DBin", params);
            if (results != null && results.length > 0) {
                return results[0].toString();
            } else {
                return "";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (GlobalConstants.mapBinClient == null) {
                logger.error("GlobalConstants.mapBinClient is null");
            }
            logger.error(ex.getMessage(), ex);
            logger.error("设备号：" + deviceCode + ", strip id:" + stripid);
            return "Error";
        }
    }

    /**
     * @param xmlStr
     * @param deviceCode
     * @return
     */
    @SuppressWarnings("JavaDoc")
    public static String binSet(String xmlStr, String deviceCode) {
        try {
            Object[] params = new Object[2];
            params[0] = xmlStr;
            params[1] = deviceCode;
            Object[] results = GlobalConstants.mapBinClient.invoke("upload2DBin", params);
            if (results != null && results.length > 0) {
                return results[0].toString();
            } else {
                return "";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage(), ex);
            return "Error";
        }
    }
}




