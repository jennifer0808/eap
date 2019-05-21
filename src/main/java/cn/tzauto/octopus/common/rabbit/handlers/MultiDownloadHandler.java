/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.rabbit.handlers;

import cn.tzauto.generalDriver.exceptions.*;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author luosy
 */
public class MultiDownloadHandler implements MessageHandler {

    private static Logger logger = Logger.getLogger(MultiDownloadHandler.class.getName());
    private DeviceInfo deviceInfo = new DeviceInfo();
    private List<Recipe> recipes = null;

    private String deviceCode = "";

    @Override
    public void handle(HashMap<String, String> msgMap) throws IOException, HsmsProtocolNotSelectedException, T6TimeOutException, BrokenProtocolException, T3TimeOutException, ItemIntegrityException, StreamFunctionNotSupportException, MessageDataException, InterruptedException {
        deviceCode = msgMap.get("deviceCode");
        recipes = (List<Recipe>) JsonMapper.fromJsonString(msgMap.get("arrecipeList"), Recipe.class);
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "收到MQ消息，服务端请求批量下载recipe到设备");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        deviceInfo = deviceService.selectDeviceInfoByDeviceCode(deviceCode);
        new RecipeService(sqlSession).multiDownLoadRcp2DeviceByTypeAutomatic(deviceInfo, recipes);
        sqlSession.close();
        UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "批量下载recipe到设备结束");
    }
}
