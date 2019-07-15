/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.EquipHost;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.jms.MapMessage;
import javax.jms.Message;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class UphTellHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(UphTellHandler.class);

    @Override
    public void handle(Message message) throws IOException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, InterruptedException, StateException, IntegrityException, InvalidDataException {
        MapMessage mapMessage = (MapMessage) message;
        UiLogUtil.getInstance().appendLog2SeverTab(null, "收到服务端请求获取UPH参数");
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        List<DeviceInfo> deviceInfos = deviceService.getDeviceInfo(GlobalConstants.clientId);
        sqlSession.close();
        if (deviceInfos != null && !deviceInfos.isEmpty()) {
            for (DeviceInfo deviceInfo : deviceInfos) {
                try {
                    EquipHost equipHost = hostManager.getAllEquipHosts().get(deviceInfo.getDeviceCode());
                    if (equipHost == null || !equipHost.getEquipState().isCommOn()) {
                        continue;
                    }
                    equipHost.sendUphData2Server();
                } catch (Exception e) {
                    logger.info("处理uph失败" + e);
                }
            }
        }
    }
}
