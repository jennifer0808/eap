package cn.tzauto.octopus.isecsLayer.socket;


import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.material.Material;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.tooling.Tooling;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AvaryAxisUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import javax.xml.rpc.ServiceException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DownloadToolHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(DownloadToolHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws UnsupportedEncodingException {
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String message = new String(req, "UTF-8");
        logger.info("DownloadTool message =====> " + message);
        LinkedHashMap downloadMessageMap = (LinkedHashMap) JsonMapper.fromJsonString(message, Map.class);
        String command = String.valueOf(downloadMessageMap.get("command"));
        String deviceCode = String.valueOf(downloadMessageMap.get("machineno"));
        if (command.equals("download")) {
            SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            DeviceService deviceService = new DeviceService(sqlSession);

            String downloadresult = "";
            String userId = String.valueOf(downloadMessageMap.get("userid"));
            String partNo = String.valueOf(downloadMessageMap.get("partno"));
            String lotNo = String.valueOf(downloadMessageMap.get("lotno"));
            String lottype = String.valueOf(downloadMessageMap.get("lottype"));
            String fixtureno = String.valueOf(downloadMessageMap.get("fixtureno"));
            String materialno = String.valueOf(downloadMessageMap.get("materialno"));
            String faceno = String.valueOf(downloadMessageMap.get("faceno"));

            String lotNo2 = String.valueOf(downloadMessageMap.get("lotno2"));
            String fixtureno2 = String.valueOf(downloadMessageMap.get("fixtureno2"));
            String materialno2 = String.valueOf(downloadMessageMap.get("materialno2"));


            String recipeName = "";
            // {"command":"download","lotno":"PH22","machineno":"JTH44","partno":"LH11","userid":"YGH33"}
            logger.info("download request userId:" + userId + " deviceCode" + deviceCode + " lotNo:" + lotNo + " lottype:" + lottype);
            List<DeviceInfo> deviceInfos = deviceService.getDeviceInfoByDeviceCode(deviceCode);
            if (deviceInfos != null && !deviceInfos.isEmpty()) {
                DeviceInfo deviceInfo = deviceInfos.get(0);
                if (!"0".equals(AvaryAxisUtil.workLicense(deviceInfo.getDeviceName(), userId))) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上岗证验证失败!!");
                    new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("上岗证验证失败!!work permit not been grant");
                    return;
                }
                //验证原材料
                String mstr = AvaryAxisUtil.getMaterialInfo(deviceInfo.getDeviceType(), lotNo);
                if (mstr.contains("|")) {
                    String[] mstrs = mstr.split("\\|");
                    Material material = new Material();
                    material.setCode(mstrs[0]);
                    material.setId(mstrs[0]);
                    material.setName(mstrs[1]);
                    GlobalConstants.stage.equipModels.get(deviceCode).materials.add(material);
                    if ((GlobalConstants.getProperty("MATERIAL_CHECK").equals("1") && !materialno.equals(mstrs[1]))) {
                        new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("材料验证失败!Material check error!");
                        return;
                    }
                }
                String mstr2 = AvaryAxisUtil.getMaterialInfo(deviceInfo.getDeviceType(), lotNo2);
                if (mstr2.contains("|")) {
                    String[] mstrs = mstr2.split("\\|");
                    Material material = new Material();
                    material.setCode(mstrs[0]);
                    material.setId(mstrs[0]);
                    material.setName(mstrs[1]);
                    GlobalConstants.stage.equipModels.get(deviceCode).materials.add(material);
                    if ((GlobalConstants.getProperty("MATERIAL_CHECK").equals("1") && !materialno.equals(mstrs[1]))) {
                        new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("材料验证失败!Material check error!");
                        return;
                    }
                }
                //验证治具
                if (AvaryAxisUtil.checkTooling(deviceInfo.getDeviceType(), lotNo, fixtureno)) {
                    Tooling tooling = new Tooling();
                    tooling.setId(fixtureno);
                    tooling.setCode(fixtureno);
                    GlobalConstants.stage.equipModels.get(deviceCode).toolings.add(tooling);
                } else {
                    new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("治具验证失败!Tooling check error!");
                    return;
                }
                //串联sfc系统，确认产品在当站
                if ("1".equals(GlobalConstants.getProperty("SFC_CHECK")) && AvaryAxisUtil.getProductionMap(lotNo, GlobalConstants.stage.equipModels.get(deviceCode).tableNum, deviceCode) == null) {
                    UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "串联SFC系统失败，确认产品是否在当站!!批号：" + lotNo);
                    new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("SFC Check failed!");
                    return;
                }
                if (lotNo2 != null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (AvaryAxisUtil.checkTooling(deviceInfo.getDeviceType(), lotNo2, fixtureno2)) {
                        Tooling tooling = new Tooling();
                        tooling.setId(fixtureno2);
                        tooling.setCode(fixtureno2);
                        GlobalConstants.stage.equipModels.get(deviceCode).toolings.add(tooling);
                    } else {
                        new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("治具验证失败!Tooling check error!");
                        return;
                    }
                    //串联sfc系统，确认产品在当站

                    if ("1".equals(GlobalConstants.getProperty("SFC_CHECK")) && AvaryAxisUtil.getProductionMap(lotNo2, GlobalConstants.stage.equipModels.get(deviceCode).tableNum, deviceCode) == null) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "串联SFC系统失败，确认产品是否在当站!!批号：" + lotNo2);
                        new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("SFC Check failed!");
                        return;
                    }
                }
                String partNoTemp = AvaryAxisUtil.getPartNumVersion(lotNo);
//                if (deviceInfo.getDeviceType().contains("SCREEN")) {
                try {
                    EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceCode);
                    equipModel.lotCount = AvaryAxisUtil.getLotQty(lotNo);
                    equipModel.lotId = lotNo;
                    equipModel.isFirstPro = "0".equals(lottype);
                    equipModel.equipState.setWorkLot(lotNo);
                    if ("1".equals(GlobalConstants.getProperty("FIRST_PRODUCTION_NEED_CHECK")) && AvaryAxisUtil.isInitialPart(partNoTemp, deviceCode, "0")) {
                        if ("1".equals(lottype)) {
                            UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "需要开初件!!");
                            new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("Need check isfirst!");
                            return;
                        }
                    }
                    if ("1".equals(GlobalConstants.getProperty("FIRST_PRODUCTION_CHECK")) && !AvaryAxisUtil.firstProductionIsOK(deviceInfo.getDeviceName(), lotNo, partNoTemp, "SFCZ4_ZD_DIExposure")) {
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "初件检查未通过!!");
                        new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode).sendSocketMsg("初件检查未通过");
                        return;
                    }
                } catch (Exception e) {
                    logger.error("Exception", e);
                    e.printStackTrace();
                }

                if (deviceInfo.getDeviceType().equals("HITACHI-LASERDRILL")) {
                    recipeName = GlobalConstants.stage.equipModels.get(deviceCode).organizeRecipe(faceno, lotNo);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String recipeName2 = GlobalConstants.stage.equipModels.get(deviceCode).organizeRecipe(faceno, lotNo2);
                    if (!recipeName.equals(recipeName2)) {
                        logger.error("两个批号关联的程序名不一致！");
                        UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "LOT1:" + lotNo + "-->" + recipeName + "LOT2:" + lotNo2 + "-->" + recipeName2);
                        return;
                    }
                } else {
                    recipeName = GlobalConstants.stage.equipModels.get(deviceCode).organizeRecipe(partNoTemp, lotNo);
                }
                if (recipeName.contains("Can not")) {
                    downloadresult = recipeName;
                } else {
                    Recipe recipe = new Recipe();
                    DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(deviceCode);
                    if ("1".equals(GlobalConstants.getProperty("DOWNLOAD_RCP_FROM_CIM"))) {
                        downloadresult = AvaryAxisUtil.downLoadRecipeFormCIM(deviceCode, recipeName);
                        if (downloadresult.contains("PASS")) {
                            return;
                        }
                    } else {
                        if (GlobalConstants.getProperty("EQUIP_NO_RECIPE").contains(deviceInfo.getDeviceType())) {
                            recipe.setRecipeName(recipeName);
                            recipe.setId(recipeName);
                        } else {
                            List<Recipe> recipes = recipeService.searchRecipeOrderByVerNo(recipeName, deviceCode, "Unique");
                            if (recipes == null || recipes.isEmpty()) {
                                recipes = recipeService.searchRecipeOrderByVerNo(recipeName, deviceCode, "GOLD");
                                if (recipes == null || recipes.isEmpty()) {
                                    recipes = recipeService.searchRecipeOrderByVerNo(recipeName, deviceCode, "Engineer");
                                }
                            }
                            if (recipes != null && !recipes.isEmpty()) {
                                recipe = recipes.get(0);
                                downloadresult = recipeService.downLoadRcp2ISECSDeviceByTypeAutomatic(deviceInfo, recipe, deviceInfoExt.getRecipeDownloadMod());
                                if (downloadresult.contains("下载Recipe失败,设备通讯异常,请稍后重试")) {
                                    downloadresult = "Connect error,please check it and try later.";
                                }
                            } else {
                                downloadresult = "Can not find any recipe,please upload recipe" + recipeName;
                            }
                        }
                    }
                    logger.info("downloadresult:" + downloadresult);
                    if ("0".equals(downloadresult)) {
                        GlobalConstants.stage.equipModels.get(deviceCode).partNo = partNoTemp;
                        GlobalConstants.stage.equipModels.get(deviceCode).lotId = lotNo;
                        GlobalConstants.stage.equipModels.get(deviceCode).equipState.setWorkLot(lotNo);
                        GlobalConstants.stage.equipModels.get(deviceCode).lotCount = AvaryAxisUtil.getLotQty(lotNo);
                        deviceInfoExt.setLotId(lotNo);
                        deviceInfoExt.setPartNo(partNo);
                        deviceInfoExt.setRecipeName(recipeName);
                        deviceInfoExt.setRecipeId(recipe.getId());
                        deviceService.modifyDeviceInfoExt(deviceInfoExt);
                        sqlSession.commit();
                    }
                }
            } else {
                downloadresult = "Can not find any device by MachineNo " + deviceCode;
            }

            sqlSession.close();
            if (downloadresult.contains("请联系该工段的")) {
                downloadresult = "The recipe:" + recipeName + " was not approved";
            }
//            Channel channel = ctx.channel();
//            AttributeKey attrKey = AttributeKey.valueOf("123456789");
//            Attribute<Object> attr = channel.attr(attrKey);
//            String eqpIp = ctx.channel().remoteAddress().toString().split(":")[0].replaceAll("/", "");
//            attr.set(downloadresult);
//            buf = channel.alloc().buffer(downloadresult.getBytes().length);
//            buf.writeBytes(downloadresult.getBytes());
//            channel.writeAndFlush(buf);
            ISecsHost iSecsHost = new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", deviceCode);
            iSecsHost.sendSocketMsg(downloadresult);


        }
        if (command.equals("startmiantain")) {
            String time = String.valueOf(downloadMessageMap.get("time"));
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setPM(true);
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setStartTime(time);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "开始保养.");
        }
        if (command.equals("endmiantain")) {
            String time = String.valueOf(downloadMessageMap.get("time"));
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setPM(false);
            GlobalConstants.stage.equipModels.get(deviceCode).pmState.setEndTime(time);
            UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "结束保养.");
        }
    }

}
