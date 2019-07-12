/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.ws;

import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.ClientInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceType;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.sys.domain.SysUser;
import cn.tzauto.octopus.biz.sys.service.SysService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;


public class InitService {

    private static final Logger logger = Logger.getLogger(InitService.class);

    public static void init(String clientId) {
        Map resultMap = new HashMap();
        resultMap = AxisUtility.init(clientId);
        if (resultMap == null) {
            return;
        }
        String MdClientInfoString = (String) resultMap.get("mdClientInfo");
        String MdDeviceInfoListString = (String) resultMap.get("mdDeviceInfo");
        String MdDeviceTypeListString = (String) resultMap.get("mdDeviceType");
        String SysUserString = (String) resultMap.get("sysUser");
        String MdDeviceInfoExtListString = (String) resultMap.get("mdDeviceInfoExt");
//        String ArRecipeTemplateListString = (String) resultMap.get("arRecipeTemplate");
        //以下是从webservice获取的数据
        List<ClientInfo> clientInfoList = (List<ClientInfo>) JsonMapper.String2List(MdClientInfoString, ClientInfo.class);//json字符串-->list
        List<DeviceInfo> deviceInfoList = (List<DeviceInfo>) JsonMapper.String2List(MdDeviceInfoListString, DeviceInfo.class);
        List<DeviceType> deviceTypeList = (List<DeviceType>) JsonMapper.String2List(MdDeviceTypeListString, DeviceType.class);
        List<SysUser> sysUserList = (List<SysUser>) JsonMapper.String2List(SysUserString, SysUser.class);
        List<DeviceInfoExt> deviceInfoExtList = (List<DeviceInfoExt>) JsonMapper.String2List(MdDeviceInfoExtListString, DeviceInfoExt.class);
//        List<RecipeTemplate> recipeTemplateList = (List<RecipeTemplate>) JsonMapper.String2List(ArRecipeTemplateListString, RecipeTemplate.class);
        //操作数据库                                                                      
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        SysService sysService = new SysService(sqlSession);
        RecipeService recipeService = new RecipeService(sqlSession);
        try {
            //TODO对clientInfo进行处理 
            ClientInfo clientInfoFromDB = null;
            ClientInfo clientInfoFromWs = null;
            if (clientInfoList != null && !clientInfoList.isEmpty()) {
                clientInfoFromWs = clientInfoList.get(0);
                clientInfoFromDB = deviceService.searchClientInfoByClientCode(clientId);
                if (clientInfoFromDB == null) {
                    deviceService.saveClientInfo(clientInfoFromWs);
                    logger.info("ClientInfo直接保存成功");
                } else {
                    deviceService.deleteClientInfo(clientInfoFromDB.getId());
                    logger.info("ClientInfo执行删除成功");
                    deviceService.saveClientInfo(clientInfoFromWs);
                    logger.info("ClientInfo先删除再保存成功");
                }
            }
            logger.info("clientInfo处理成功");

            //TODO对DeviceInfo进行处理
            if (deviceInfoList != null && !deviceInfoList.isEmpty()) {
                deviceService.batchDeleteDeviceInfo(deviceInfoList);
                List<DeviceInfo> deviceInfoListFromDB = deviceService.getDeviceInfoByClientId(clientId);
                if (deviceInfoListFromDB != null && !deviceInfoListFromDB.isEmpty()) {
                    deviceService.batchDeleteDeviceInfo(deviceInfoListFromDB);
                    //TODO删除服务端传来的设备信息中，在本地数据库中存在的记录
                    for (DeviceInfo deviceInfo : deviceInfoList) {
                        DeviceInfo deviceInfoFromDb = null;
                        List<DeviceInfo> deviceInfoListFromDBByDeviceCode = deviceService.getDeviceInfoByDeviceCode(deviceInfo.getDeviceCode());
                        if (deviceInfoListFromDBByDeviceCode != null && !deviceInfoListFromDBByDeviceCode.isEmpty()) {
                            deviceInfoFromDb = deviceInfoListFromDBByDeviceCode.get(0);
                            deviceService.deleteDeviceInfo(deviceInfoFromDb.getId());
                        }
                    }
                    logger.info("DeviceInfo删除成功");
                    deviceService.saveDeviceInfoBatch(deviceInfoList);
                    logger.info("DeviceInfo成功删除后再批量保存成功");
                } else {
                    deviceService.saveDeviceInfoBatch(deviceInfoList);
                    logger.info("DeviceInfo直接批量保存成功");
                }
            }
            logger.info("DeviceInfo处理完毕");

            //对DeviceType进行处理
            //TODO待定是否适应一台工控机管理多个机型
            DeviceType deviceTypeFromDB = null;
//            DeviceType deviceTypeFromWs = null;
            if (deviceTypeList != null && !deviceTypeList.isEmpty()) {
                for (DeviceType deviceTypeFromWs : deviceTypeList) {
                    deviceTypeFromDB = deviceService.getDeviceType(deviceTypeFromWs.getTypeCode(), deviceTypeFromWs.getManufacturerName());
                    if (deviceTypeFromDB == null) {
                        deviceService.saveDeviceType(deviceTypeFromWs);
                        logger.info("DeviceType直接保存成功");
                    } else {
                        deviceService.deleteDeviceType(deviceTypeFromDB.getId());
                        logger.info("DeviceType执行删除成功");
                        deviceService.saveDeviceType(deviceTypeFromWs);
                        logger.info("DeviceType先删除再保存成功");
                    }
                }
//                deviceTypeFromWs = deviceTypeList.get(0);

            }
            logger.info("deviceType处理完毕");

            //TODO对sysUser进行处理
            if (sysUserList != null && !sysUserList.isEmpty()) {
                sysService.deleteSysUserBatch(sysUserList);
                logger.debug("sysUser删除成功");
                sysService.insertSysUserBatch(sysUserList);
                logger.debug("sysUser新增成功");
            }
            logger.info("sysUser处理完毕");
            
            //对DeviceInfoExt进行处理
//            if (deviceInfoExtList != null && !deviceInfoExtList.isEmpty()) {
//                deviceService.deleteDeviceInfoExtByIdBatch(deviceInfoExtList);
//                logger.info("deviceInfoExt根据Id批量删除成功");
//                deviceService.deleteDeviceInfoExtByDeviceRowIdBatch(deviceInfoExtList);
//                logger.info("deviceInfoExt根据deviceRowId批量删除成功");
//                deviceService.saveDeviceInfoExtBatch(deviceInfoExtList);
//                logger.info("deviceInfoExt新增成功");
//            }
//            logger.info("deviceInfoExt处理完毕");
            
            //对RecipeTemplate进行处理
//            if (recipeTemplateList != null && !recipeTemplateList.isEmpty()) {
//                recipeService.deleteRecipeTemplateByIdBatch(recipeTemplateList);
//                logger.info("根据Id批量删除成功");
//                recipeService.deleteRecipeTemplateByDeviceTypeIdBatch(recipeTemplateList);
//                logger.info("根据deviceTypeId删除成功");
//                recipeService.saveRecipeTemplateBatch(recipeTemplateList);
//                logger.info("recipeService新增成功");
//            }
            sqlSession.commit();
        } catch (Exception e) {
            e.printStackTrace();
            sqlSession.rollback();
            logger.info("处理数据异常,请联系相关人员进行解决！");
           UiLogUtil.getInstance().appendLog2SeverTab(null, "处理数据异常,请联系相关人员进行解决！");
        } finally {
            sqlSession.close();
        }
    }

    public static void main(String[] args) {
        init("A6-WS-H1");
    }
}
