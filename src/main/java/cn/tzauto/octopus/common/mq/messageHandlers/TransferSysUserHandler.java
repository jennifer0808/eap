package cn.tzauto.octopus.common.mq.messageHandlers;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.sys.domain.SysUser;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.common.MessageHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import org.apache.ibatis.session.SqlSession;

public class TransferSysUserHandler implements MessageHandler {
    private SysUser sysUsers;
    private String deviceCode = "";
    private static Logger logger = Logger.getLogger(TransferSysUserHandler.class.getName());
    @Override
    public void handle(Message message) {
        MapMessage mapMessage = (MapMessage) message;
        try {
            deviceCode = mapMessage.getString("deviceCode");
            sysUsers = (SysUser) JsonMapper.fromJsonString(mapMessage.getString("user"), SysUser.class);
            logger.info("接收到服务端发送的sysUser" + sysUsers);
           UiLogUtil.getInstance().appendLog2SeverTab(null, "设备" + deviceCode +"接收到服务端更新SysUser配置请求");
        } catch (JMSException e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SysService sysService = new SysService(sqlSession);
        Map mqMap = new HashMap();
        mqMap.put("msgName", "TransferSysUser");
        mqMap.put("eventName", "更新SysUser");
        try {
            if( sysUsers != null){
               
                sysService.deleteByPrimaryKey(sysUsers);
                sysService.insert(sysUsers);
            }
            sqlSession.commit();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "SysUser配置更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "SysUser配置更新失败");
        } finally {
            sqlSession.close();
        }
    }
}
