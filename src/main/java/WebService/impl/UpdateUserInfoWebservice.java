package WebService.impl;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.sys.domain.SysUser;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.mq.messageHandlers.TransferSysUserHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 12631 on 2019/3/1.
 */
public class UpdateUserInfoWebservice implements BaseWebservice {
    private SysUser sysUser;
    private String deviceCode = "";
    private static Logger logger = Logger.getLogger(TransferSysUserHandler.class);
    @Override
    public String handle(String message) {
        Map webMap = new HashMap();
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);
        try {
            deviceCode = String.valueOf(map.get("deviceCode"));
            sysUser = (SysUser) JsonMapper.fromJsonString(JSONObject.toJSON(map.get("user")).toString(), SysUser.class);

            logger.info("接收到服务端发送的sysUser" + sysUser);
           UiLogUtil.getInstance().appendLog2SeverTab(null, "设备" + deviceCode +"接收到服务端更新SysUser配置请求");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        SysService sysService = new SysService(sqlSession);

        webMap.put("msgName", "TransferSysUser");
        webMap.put("eventName", "更新SysUser");
        try {
            if( sysUser != null){

                sysService.deleteByPrimaryKey(sysUser);
                sysService.insert(sysUser);
            }
            sqlSession.commit();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "SysUser配置更新成功");
            webMap.put("eventDesc", "SysUser配置更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
           UiLogUtil.getInstance().appendLog2SeverTab(null, "SysUser配置更新失败");
            webMap.put("eventDesc", "SysUser配置更新失败");
        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
    }
}
