package WebService.impl;

import WebService.Interface.BaseWebservice;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.mq.messageHandlers.SpecificDataTransferHandler;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by 12631 on 2019/3/1.
 */
public class GetSpecificDataWebservice implements BaseWebservice {
    private static final Logger logger = Logger.getLogger(SpecificDataTransferHandler.class);
    private String deviceCode = "";
    private Map<String, String> dataIdMap = null;

    @Override
    public String handle(String message) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        Map webMap = new HashMap();
        try {
            HashMap map = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);
            deviceCode = String.valueOf(map.get("deviceCode"));
            dataIdMap = (HashMap<String, String>) JsonMapper.fromJsonString(JSONObject.toJSON(map.get("dataIdList")).toString(), HashMap.class);
            logger.info("服务端请求获取设备[" + deviceCode + "]的指定数据，数据ID:" + JsonMapper.toJsonString(dataIdMap));
//            UiLogUtil.appendLog2SeverTab(deviceCode, "服务端请求从设备获取数据...");
            DeviceService deviceService = new DeviceService(sqlSession);
            DeviceInfo deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
            Map resultMap = GlobalConstants.stage.hostManager.getSpecificData(deviceInfo.getDeviceCode(), dataIdMap);

            String resultMapString = "";
            if (resultMap != null) {
                if (dataIdMap.size() > resultMap.size()) {
                    resultMapString = JsonMapper.toJsonString(resultMap);
                    logger.info("设备 " + deviceCode + " 部分数据ID无法取值,需要逐个测试");
                } else {
                    resultMapString = JsonMapper.toJsonString(resultMap);
                    webMap.put("eventDesc", "获取到设备的数据");
                }
            } else {
                webMap.put("eventDesc", "从设备数据失败，请重试！");
            }
            webMap.put("SpecificData", resultMapString);


            logger.info("向服务端发送获取到的数据:[" + resultMapString + "]");
//            UiLogUtil.appendLog2SeverTab(deviceCode, "向服务端发送获取到的数据");
        } catch (Exception ex) {
            webMap.put("eventDesc", "从设备数据失败，请重试！");
            logger.error("Execption occur:" + ex);
        } finally {
            sqlSession.close();
        }
        return JSONObject.toJSON(webMap).toString();
    }
}
