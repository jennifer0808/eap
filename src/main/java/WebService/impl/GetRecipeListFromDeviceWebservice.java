package WebService.impl;


import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.messageHandlers.FindEqptRecipeListHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.secsLayer.domain.MultipleEquipHostManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//获取设备recipe列表
public class GetRecipeListFromDeviceWebservice implements BaseWebservice {
    private static Logger logger = Logger.getLogger(FindEqptRecipeListHandler.class.getName());

    @Override
    public String handle(String message) {
        HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);
        MultipleEquipHostManager hostManager = GlobalConstants.stage.hostManager;
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfo deviceInfo = null;
        Map webMap = new HashMap();
        try {
            String deviceCode = String.valueOf(map.get("deviceCode"));
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "服务端请求获取设备上的Recipe列表...");
            deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            List equipRecipeList = new ArrayList();
            Map equipRecipeListState = hostManager.getRecipeListFromDevice(deviceInfo.getDeviceCode());
            if (equipRecipeListState != null) {
                equipRecipeList = (List) equipRecipeListState.get("eppd");
            }

            webMap.put("msgName", "FindEqptRecipeList");
            webMap.put("EqptRecipeList", JSONArray.toJSONString(equipRecipeList));
            webMap.put("eventDesc", "向服务端发送设备的当前Recipe列表" + equipRecipeList);
           UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "向服务端发送设备的当前Recipe列表:" + equipRecipeList);
        } catch (Exception ex) {
            logger.error("Exception:", ex);
            webMap.put("eventDesc", "向服务端发送设备的当前Recipe列表失败");
        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
    }
}





