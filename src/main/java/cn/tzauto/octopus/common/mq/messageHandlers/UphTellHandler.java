/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import java.util.List;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author Weiqy
 */
public class UphTellHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UphTellHandler.class.getName());

    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        UiLogUtil.appendLog2SeverTab(null, "收到服务端请求获取UPH参数");
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        List<DeviceInfo> deviceInfos = deviceService.getDeviceInfo(GlobalConstants.clientId);
        sqlSession.close();
        if (deviceInfos != null && !deviceInfos.isEmpty()) {
            for (DeviceInfo deviceInfo : deviceInfos) {
                EquipHost equipHost = hostManager.getAllEquipHosts().get(deviceInfo.getDeviceId());
                if (equipHost == null || !equipHost.getEquipState().isCommOn()) {
                    continue;
                }
                equipHost.sendUphData2Server();
            }
        }
    }
}
