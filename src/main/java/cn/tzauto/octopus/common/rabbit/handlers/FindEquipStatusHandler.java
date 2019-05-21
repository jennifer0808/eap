/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rain
 */
public class FindEquipStatusHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(FindEquipStatusHandler.class.getName());
    private String deviceCode = "";
    private String equipStatus = "";

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        try {
            deviceCode = msgMap.get("deviceCode");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            equipStatus = hostManager.getEquipStatus(deviceInfo.getDeviceCode());
            logger.info("设备:" + deviceCode + "当前运行状态为:" + equipStatus);
            Map mqMap = new HashMap();
            mqMap.put("msgName", "FindEquipStatus");
            mqMap.put("EquipStatus", equipStatus);
            //TODO 原队列JMSReplyTo获得，待验证
            GlobalConstants.C2SCheckRcpNameQueue.sendMessage(mqMap);
            logger.info("向服务端[ C2S.Q.CHECKRCPNAME ]回复校验设备运行状态" + JSONArray.toJSONString(mqMap));
        } catch (Exception ex) {
            logger.error("Exception:" + ex);
        } finally {
            sqlSession.close();
        }

    }
}
