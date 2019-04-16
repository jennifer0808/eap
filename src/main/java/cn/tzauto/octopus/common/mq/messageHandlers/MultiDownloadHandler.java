/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.List;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author luosy
 */
public class MultiDownloadHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(MultiDownloadHandler.class.getName());
    private DeviceInfo deviceInfo = new DeviceInfo();
    private List<Recipe> recipes = null;

    private String deviceCode = "";

    @Override
    public void handle(Message message) {
        try {
            MapMessage mapMessage = (MapMessage) message;
            deviceCode = mapMessage.getString("deviceCode");
            recipes = (List<Recipe>) JsonMapper.fromJsonString(mapMessage.getString("arrecipeList"), Recipe.class);
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "收到MQ消息，服务端请求批量下载recipe到设备");
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            DeviceService deviceService = new DeviceService(sqlSession);
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            new RecipeService(sqlSession).multiDownLoadRcp2DeviceByTypeAutomatic(deviceInfo, recipes);
            sqlSession.close();
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "批量下载recipe到设备结束");
        } catch (JMSException e) {
            logger.error("Exception:", e);
        }
    }

}
