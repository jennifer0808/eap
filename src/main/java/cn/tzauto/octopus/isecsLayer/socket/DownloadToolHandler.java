package cn.tzauto.octopus.isecsLayer.socket;


import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.common.ws.AvaryAxisUtil;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
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
        if (command.equals("download")) {
            SqlSession sqlSession = MybatisSqlSession.getBatchSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            DeviceService deviceService = new DeviceService(sqlSession);

            String downloadresult = "";
            message = message.replaceAll("download", "");
            String[] temps = message.split(";");
            String deviceCode = String.valueOf(downloadMessageMap.get("machineno"));
            String userId = String.valueOf(downloadMessageMap.get("userid"));
            String partNo = String.valueOf(downloadMessageMap.get("partno"));
            String lotNo = String.valueOf(downloadMessageMap.get("lotno"));

            String recipeName = "";
            // {"command":"download","lotno":"PH22","machineno":"JTH44","partno":"LH11","userid":"YGH33"}
            logger.info("download request userId:" + userId + " deviceCode" + deviceCode + " partNo:" + partNo + " lotNo:" + lotNo);
            List<DeviceInfo> deviceInfos = deviceService.getDeviceInfoByDeviceCode(deviceCode);
            if (deviceInfos != null && !deviceInfos.isEmpty()) {
                DeviceInfo deviceInfo = deviceInfos.get(0);
                if (!"0".equals(AvaryAxisUtil.workLicense(deviceInfo.getDeviceName(), userId))) {
                    UiLogUtil.getInstance().appendLog2SeverTab(deviceCode, "上岗证验证失败!!");
                    return;
                }
                String partNoTemp = AvaryAxisUtil.getPartNumVersion(lotNo);
                if (deviceInfo.getDeviceType().contains("SCREEN")) {
                    try {
                        GlobalConstants.stage.equipModels.get(deviceCode).lotCount = AvaryAxisUtil.getLotQty(lotNo);
                        if ("1".equals(GlobalConstants.getProperty("FIRST_PRODUCTION_NEED_CHECK")) && AvaryAxisUtil.isInitialPart(partNoTemp, deviceCode, "0")) {
                            if ("1".equals(GlobalConstants.getProperty("FIRST_PRODUCTION_CHECK")) && !AvaryAxisUtil.firstProductionIsOK(deviceInfo.getDeviceName(), lotNo, partNoTemp, "SFCZ4_ZD_DIExposure")) {
                                UiLogUtil.getInstance().appendLog2EventTab(deviceCode, "初件检查未通过!!");
                                new ISecsHost(GlobalConstants.stage.equipModels.get(deviceCode).remoteIPAddress, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", "").executeCommand("初件检查未通过");
                                return;
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (ServiceException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

                }
                recipeName = GlobalConstants.stage.equipModels.get(deviceCode).organizeRecipe(partNoTemp, lotNo);
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
                    GlobalConstants.stage.equipModels.get(deviceCode).lotCount = AvaryAxisUtil.getLotQty(lotNo);
                    deviceInfoExt.setLotId(lotNo);
                    deviceInfoExt.setPartNo(partNo);
                    deviceInfoExt.setRecipeName(recipeName);
                    deviceInfoExt.setRecipeId(recipe.getId());
                    deviceService.modifyDeviceInfoExt(deviceInfoExt);
                    sqlSession.commit();
                }
            } else {
                downloadresult = "Can not find any device by MachineNo " + deviceCode;
            }

            sqlSession.close();

            Channel channel = ctx.channel();
            AttributeKey attrKey = AttributeKey.valueOf("123456789");
            Attribute<Object> attr = channel.attr(attrKey);
            String eqpIp = ctx.channel().remoteAddress().toString().split(":")[0].replaceAll("/", "");
            attr.set(downloadresult);
            buf = channel.alloc().buffer(downloadresult.getBytes().length);
            buf.writeBytes(downloadresult.getBytes());
            channel.writeAndFlush(buf);
            new ISecsHost(eqpIp, GlobalConstants.getProperty("DOWNLOAD_TOOL_RETURN_PORT"), "", "").executeCommand(downloadresult);

        }
        if (command.equals("getRecipeName")) {
            message = message.replaceAll("getRecipeName", "");
            String deviceCode = message.trim();
            String CurrentRecipeName = GlobalConstants.stage.hostManager.getEquipCurrentRecipeName(deviceCode) + "getRecipeName" + "done";
            Channel channel = ctx.channel();
            buf = channel.alloc().buffer(CurrentRecipeName.getBytes().length);
            buf.writeBytes(CurrentRecipeName.getBytes());
            channel.writeAndFlush(buf);
        }
        if (command.equals("getEquipStatus")) {
            message = message.replaceAll("getEquipStatus", "");
            String deviceCode = message.trim();
            String EquipStatus = GlobalConstants.stage.hostManager.getEquipStatus(deviceCode) + "getEquipStatus" + "done";
            Channel channel = ctx.channel();
            buf = channel.alloc().buffer(EquipStatus.getBytes().length);
            buf.writeBytes(EquipStatus.getBytes());
            channel.writeAndFlush(buf);
        }
    }

}
