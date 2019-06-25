package cn.tzauto.octopus.isecsLayer.socket;

import cn.tzauto.octopus.biz.alarm.domain.AlarmRecord;
import cn.tzauto.octopus.biz.device.dao.DeviceInfoMapper;
import cn.tzauto.octopus.biz.device.domain.DeviceInfo;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class Alarm7045Handler extends ChannelInboundHandlerAdapter {

    private static Map<String, String> ipCodeMap = new HashMap<>();

    static Properties properties = new Properties();

    static Map<String, String> map = new HashMap<>();

    /**
     * 读取机台ip和deviceCode对应关系
     */
    static {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceInfoMapper mapper = sqlSession.getMapper(DeviceInfoMapper.class);
        List<DeviceInfo> deviceInfos = mapper.getCurClientDeviceInfo();
       for (DeviceInfo deviceInfo : deviceInfos) {
            ipCodeMap.put(deviceInfo.getDeviceIp(), deviceInfo.getDeviceCode());
        }
        sqlSession.close();
        try {
            properties.load(Alarm7045Handler.class.getClassLoader().getResourceAsStream("alarm7045.properties"));
            for(Map.Entry<Object, Object> entry : properties.entrySet()){
                map.put(String.valueOf(entry.getValue()), String.valueOf(entry.getKey()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = inetSocketAddress.getAddress().getHostAddress();
        String deviceCode = ipCodeMap.get(ip);
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String message = new String(req, "UTF-8");
        if (message != null && message.startsWith("alert alarm")) {
            List<String> results = GlobalConstants.stage.equipModels.get(deviceCode).sendMsg2Equip("read altx");
            if (results != null && !results.isEmpty() && !results.get(0).contains("error")) {
                String altx = results.get(0);
                UiLogUtil.getInstance().appendLog2SecsTab(deviceCode,"收到报警信息：" + altx);
               String alid = map.get(altx);
                List<AlarmRecord> alarmRecordList = new ArrayList<>();
                if (alid != null) {
                    UiLogUtil.getInstance().appendLog2SecsTab(deviceCode, "收到报警信息 " + " 报警ID:" + alid + " 报警详情: " + altx);
                    DeviceInfo deviceInfo = getDeviceInfo( deviceCode, GlobalConstants.stage.deviceInfos);
                    AlarmRecord alarmRecord = new AlarmRecord();
                    String id = UUID.randomUUID().toString();
                    alarmRecord.setId(id);
                    alarmRecord.setClientCode(GlobalConstants.clientInfo.getClientCode());
                    alarmRecord.setClientId(GlobalConstants.clientInfo.getId());
                    alarmRecord.setClientName(GlobalConstants.clientInfo.getClientName());
                    alarmRecord.setDeviceId(deviceInfo.getId());
                    alarmRecord.setDeviceCode(deviceInfo.getDeviceCode());
                    alarmRecord.setDevcieName(deviceInfo.getDeviceName());
                    alarmRecord.setAlarmId(String.valueOf(alid));
                    alarmRecord.setAlarmCode("");
                    alarmRecord.setAlarmName(altx);
                    alarmRecord.setAlarmDate(new Date());
                    alarmRecord.setDeviceTypeCode(deviceInfo.getDeviceType());
                    alarmRecord.setDeviceTypeId(deviceInfo.getDeviceTypeId());
                    alarmRecord.setRepeatFlag("N");
                    alarmRecord.setStepCode("");
                    alarmRecord.setStepId("");
                    alarmRecord.setStepName("");
                    alarmRecord.setVerNo(0);
                    alarmRecord.setDelFlag("0");
                    String officeId = deviceInfo.getOfficeId();
                    alarmRecord.setStationId(officeId);
                    alarmRecord.setStationCode("");
                    alarmRecord.setStationName("");
                }
                //send alarm record to server
                if (!GlobalConstants.isLocalMode && alarmRecordList.size() != 0) {
                    Map alarmRecordMap = new HashMap();
                    alarmRecordMap.put("msgName", "ArAlarmRecord");
                    alarmRecordMap.put("deviceCode", deviceCode);
                    alarmRecordMap.put("alarmRecord", JsonMapper.toJsonString(alarmRecordList));
                    alarmRecordMap.put("alarmDate", GlobalConstants.dateFormat.format(new Date()));
                    GlobalConstants.C2SAlarmQueue.sendMessage(alarmRecordMap);
                }
            }
        }
    }

    private DeviceInfo getDeviceInfo(String deviceCode,List<DeviceInfo> deviceInfos) {
        if (deviceCode != null) {
            for (DeviceInfo deviceInfo : deviceInfos) {
                if (deviceCode.equals(deviceInfo.getDeviceCode())) {
                    return deviceInfo;
                }
            }
        }
        return null;
    }

}
