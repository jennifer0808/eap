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
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author
 */
public class PPSelectHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(PPSelectHandler.class.getName());
    private String deviceCode = "";
    private String recipeName = "";


    //同步发送
    public void sendMsgToServer(Map mqMap) {
        //TODO 原队列JMSReplyTo获得，待验证
        GlobalConstants.C2SRcpSelectQueue.sendMessage(mqMap); //使用 C2S.Q.PPSELECT
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "发送MVP PPSelect结果至服务端");

    }

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        String result = "";
        try {
            deviceCode = msgMap.get("deviceCode"); //获取服务端发送的数据
            recipeName = msgMap.get("recipeName");
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求将设备 " + deviceCode + " 的程序更换为 " + recipeName);
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            if (deviceInfo.getDeviceType().contains("MVP")) {
                result = hostManager.selectSpecificRecipe(deviceInfo.getDeviceCode(), recipeName);
            }
        } catch (Exception e) {
            logger.error("Exception", e);
        } finally {
            sqlSession.close();
        }
        Map mqMap = new HashMap(); //消息体
        mqMap.put("msgName", "MVPSelect");
        mqMap.put("deviceCode", deviceCode);
        String flag = "";
        if ("0".equals(result)) {
            flag = "Y";
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "PPSelect成功");
        } else {
            flag = "N";
            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "PPSelect失败, 结果为:" + result);
        }
        mqMap.put("flag", flag);
        mqMap.put("reason", result);
        sendMsgToServer(mqMap);
    }
}
