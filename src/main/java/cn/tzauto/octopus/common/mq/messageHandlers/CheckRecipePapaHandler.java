/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import java.util.List;

/**
 * 
 */
public class CheckRecipePapaHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(ChangeEqptStateHandler.class);
    private DeviceInfo deviceInfo = null;
    private Recipe recipe = null;
    private List<RecipePara> recipeParas = null;
    private List<Attach> attachs = null;
    private String deviceCode = "";

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        String eventId = "";
        try {
            eventId = mapMessage.getString("eventId").toString();
            deviceCode = mapMessage.getString("deviceCode").toString();
            recipe = (Recipe) JsonMapper.fromJsonString(mapMessage.getString("recipe"), Recipe.class);
            recipeParas = (List<RecipePara>) JsonMapper.String2List(mapMessage.getString("recipePara"), RecipePara.class);
            attachs = (List<Attach>) JsonMapper.String2List(mapMessage.getString("arAttach"), Attach.class);
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "收到MQ消息，服务端请求检查recipe[" + recipe.getRecipeName() + "]");
        } catch (JMSException e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        EquipHost equipHost = GlobalConstants.stage.equipHosts.get(deviceCode);
        if (equipHost.getEquipState().isCommOn()) {
            
        }
    }
}
