/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Queue;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

public class SvTransferHandler implements MessageHandler {

    private static final Logger logger = Logger.getLogger(SvTransferHandler.class);
    private String deviceCode = "";

    @Override
    public void handle(Message message) {
        logger.info("服务端请求获得设备SVList");
        MapMessage mapMessage = (MapMessage) message;
        try {
            deviceCode = mapMessage.getString("deviceCode");
            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求获得该设备SVList");
        } catch (JMSException e) {
            logger.error("JMSException:", e);
        }

        Map mqMap = new HashMap<>();
        mqMap.put("msgName", "SvTransfer");
        mqMap.put("deviceCode", deviceCode);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        String svValueString = "";
        int svSize = 0;
        try {
            List<DeviceInfo> deviceInfos = deviceService.searchDeviceInfoByPara(GlobalConstants.getProperty("clientId"), deviceCode);
            String deviceId = deviceInfos.get(0).getDeviceId();
            EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceInfos.get(0).getDeviceCode());

            if (equipHost.getEquipState().isCommOn()) {
                List<RecipeTemplate> recipeTemplatesAll = recipeService.searchRecipeTemplateByDeviceCode(deviceCode, "RecipeParaCheck");
                if (recipeTemplatesAll == null || recipeTemplatesAll.isEmpty()) {
                    mqMap.put("svList", svValueString);
                    mqMap.put("eventDesc", "没有获取到设备sv参数信息");
                    UiLogUtil.appendLog2SeverTab(deviceCode, "没有获取到设备sv参数信息");
                } else {
                    List svIdListAll = getSvIdList(recipeTemplatesAll);
                    Map resultMap = GlobalConstants.stage.hostManager.getDeviceRcpParaCheck(deviceId, svIdListAll);
                    if (resultMap != null && resultMap.size() > 0) {
                        ArrayList svListAll = (ArrayList) resultMap.get("SVList");
                        Map svValueMap = new HashMap();
                        for (int i = 0; i < svIdListAll.size(); i++) {
                            svValueMap.put(String.valueOf(svIdListAll.get(i)), String.valueOf(svListAll.get(i)));
                        }
                        svValueString = JsonMapper.toJsonString(svValueMap);
                        mqMap.put("svList", svValueString);
                        mqMap.put("eventDesc", "成功获取到设备sv参数信息");
                        svSize = svValueMap.size();
                        UiLogUtil.appendLog2SeverTab(deviceCode, "获取到设备sv参数信息成功");
                    } else {
                        mqMap.put("svList", svValueString);
                        mqMap.put("eventDesc", "获取到设备sv参数信息失败");
                        UiLogUtil.appendLog2SeverTab(deviceCode, "获取到设备sv参数信息失败");
                    }
                }
            } else {
                mqMap.put("svList", svValueString);
                mqMap.put("eventDesc", "没有获取到设备sv参数信息");
                UiLogUtil.appendLog2SeverTab(deviceCode, "设备未正常连接，没有获取到设备sv参数信息");
            }
            sendMsg2Server(message, mqMap);
            UiLogUtil.appendLog2SeverTab(deviceCode, "成功发送设备sv参数信息到服务端，svSize为" + svSize);
        } catch (Exception e) {
            logger.error("Exception:", e);
            mqMap.put("svList", svValueString);
            mqMap.put("eventDesc", "没有获取到设备sv参数信息");
            sendMsg2Server(message, mqMap);
            UiLogUtil.appendLog2SeverTab(deviceCode, "成功发送设备sv参数信息到服务端，但中途出现异常，svSize为" + svSize);
        } finally {
            logger.info("设备sv参数信息:" + svValueString);
            sqlSession.close();
        }
    }

    public static List getSvIdList(List<RecipeTemplate> recipeTemplates) {
        List svIdList = new ArrayList();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            svIdList.add(recipeTemplates.get(i).getDeviceVariableId());
        }
        return svIdList;
    }

    public void sendMsg2Server(Message message, Map mqMap) {
        try {
            Destination destination = message.getJMSReplyTo();
            String topicName = "";
            if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            GlobalConstants.C2SSvDataQueue.sendMessage(topicName, mqMap);
        } catch (JMSException e) {
            logger.error("JMSException:", e);
        }
    }
}
