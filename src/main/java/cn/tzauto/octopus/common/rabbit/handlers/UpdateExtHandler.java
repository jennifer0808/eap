/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import com.alibaba.fastjson.JSONArray;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author luosy
 */
public class UpdateExtHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UpdateExtHandler.class.getName());
    private String deviceCode = "";
    private String lockSwitch = "";

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);

        deviceCode = msgMap.get("deviceCode");
        lockSwitch = msgMap.get("lockSwitch");

        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
        try {
            int updateCount = 0;
            if (deviceInfoExt == null) {
                logger.error("数据库中确少该设备模型配置，DEVICE_CODE:" + deviceCode);
                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "工控上不存在该设备模型信息，请联系ME处理！");
            } else {
                deviceInfoExt.setLockSwitch(lockSwitch);
                updateCount = deviceService.modifyDeviceInfoExt(deviceInfoExt);
            }
            sqlSession.commit();

            Map mqMap = new HashMap();
            mqMap.put("msgName", "UpdateExt");
            mqMap.put("updateCount", String.valueOf(updateCount));
//TODO 原队列JMSReplyTo获得，待验证
            GlobalConstants.C2SSpecificDataQueue.sendMessage(mqMap);
            logger.info("向服务端[C2S.Q.SPECIFIC_DATA]回复锁机开关更改结果:" + JSONArray.toJSONString(mqMap));
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "Server修改锁机开关为：[" + lockSwitch + "]");
        } catch (Exception e) {
            logger.error("Exception:", e);
            sqlSession.rollback();
        } finally {
            sqlSession.close();
        }
    }
}
