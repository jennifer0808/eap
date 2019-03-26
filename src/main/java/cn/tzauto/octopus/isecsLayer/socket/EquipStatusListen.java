/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.socket;

import cn.tzauto.octopus.biz.device.service.DeviceService;
import cn.tzauto.octopus.gui.guiUtil.UiLogUtil;
import cn.tzauto.octopus.biz.device.domain.DeviceInfoExt;
import cn.tzauto.octopus.biz.device.domain.DeviceOplog;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import cn.tzauto.octopus.isecsLayer.domain.EquipModel;
import cn.tzauto.octopus.isecsLayer.domain.ISecsHost;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 * @author luosy
 */
public class EquipStatusListen {

    private static final Logger logger = Logger.getLogger(EquipStatusListen.class);

    public static void startListen() {
        new EquipStatusListen().start();
//        monitorEquipStatus();
        logger.info("开启alert状态监听...");
    }

    public static void startAlarmListen() {
        new EquipStatusListen().startAlarm();
        logger.info("开启log报警监听...");
    }

    public static void main(String[] args) {
        monitorEquipStatus();
//        new EquipStatusListen().start();
        logger.info("开启alert状态监听...");
    }

    private static void monitorEquipStatus() {

        try {
            ServerSocket server = null;
            try {
                server = new ServerSocket(Integer.parseInt(GlobalConstants.getProperty("ISECS_EQUIPSTATUS_LISTEN_PORT")));
                //b)指定绑定的端口，并监听此端口。
                logger.info("monitorEquipStatus服务器启动成功");
                //创建一个ServerSocket在端口5209监听客户请求
            } catch (Exception e) {
                logger.error("monitorEquipStatus启动监听失败：" + e);
                //出错，打印出错信息
            }
            Socket socket = null;
            try {
                socket = server.accept();
                socket.setKeepAlive(true);

                //2、调用accept()方法开始监听，等待客户端的连接 
                //使用accept()阻塞等待客户请求，有客户
                //请求到来则产生一个Socket对象，并继续执行
            } catch (Exception e) {
                logger.error("Error." + e);
                //出错，打印出错信息
            }
            //3、获取输入流，并读取客户端信息 
            String line;
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //由Socket对象得到输入流，并构造相应的BufferedReader对象
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            //由Socket对象得到输出流，并构造PrintWriter对象
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            //由系统标准输入设备构造BufferedReader对象
            //  logger.info("Client:" + in.readLine());
            String lineContent = "";
            while ((lineContent = in.readLine()) != null) {
                System.out.println("SocketUtil.monitorEquipStatus()" + lineContent);
            }
            //在标准输出上打印从客户端读入的字符串
            line = br.readLine();
            //从标准输入读入一字符串
            //4、获取输出流，响应客户端的请求 
            while (!line.equals("done")) {
                //如果该字符串为 "bye"，则停止循环
                writer.println(line);
                //向客户端输出该字符串
                writer.flush();
                //刷新输出流，使Client马上收到该字符串
                logger.info("Server:" + line);
                //在系统标准输出上打印读入的字符串
                logger.info("Client:" + in.readLine());
                //从Client读入一字符串，并打印到标准输出上
                line = br.readLine();
                //从系统标准输入读入一字符串
            } //继续循环

            //5、关闭资源 
//            writer.close(); //关闭Socket输出流
//            in.close(); //关闭Socket输入流
//            socket.close(); //关闭Socket
//            server.close(); //关闭ServerSocket
        } catch (Exception e) {//出错，打印出错信息
            logger.error("Error." + e);
        }
    }

    static String equipstatus = "--";
    static String preEquipstatus = "--";

    private static void monitorEquipStatus1() {
        new Thread() {
            @Override
            public void run() {

                Map<String, String> map = new HashMap();
                for (Map.Entry<String, EquipModel> equipmodelEntry : GlobalConstants.stage.equipModels.entrySet()) {
                    EquipModel equipModel = equipmodelEntry.getValue();
                    ConcurrentLinkedQueue<ISecsHost> isecsHosts = equipModel.iSecsHostList;
                    for (ISecsHost isecsHost : isecsHosts) {
                        map.put(isecsHost.ip, isecsHost.deviceCode);
                    }
                }
                while (true) {

                    try {
                        //为了简单起见，所有的异常信息都往外抛
                        int port = 12002;
                        //定义一个ServerSocket监听在端口12002上
                        ServerSocket server = new ServerSocket(port);
                        //server尝试接收其他Socket的连接请求，server的accept方法是阻塞式的
                        Socket socket = server.accept();
                        //跟客户端建立好连接之后，我们就可以获取socket的InputStream，并从中读取客户端发过来的信息了。
                        Reader reader = new InputStreamReader(socket.getInputStream());
                        char chars[] = new char[64];
                        int len;
                        StringBuilder sb = new StringBuilder();
                        String temp;
                        int index;
                        while ((len = reader.read(chars)) != -1) {
                            temp = new String(chars, 0, len);
                            String ipStatus[] = temp.split(";");
                            if (ipStatus.length > 1) {
                                String eqpIp = socket.getRemoteSocketAddress().toString();
                                eqpIp = eqpIp.split(":")[0].replaceAll("/", "");
                                //ipStatus[1].replaceAll("done", "").trim();
                                String status = transferStatus(ipStatus[0]);
                                String prestatus = transferStatus(ipStatus[1]);
                                String deviceCode = map.get(eqpIp);
                                logger.debug("设备:" + deviceCode + "设备进入" + status + "状态.");
                                UiLogUtil.appendLog2EventTab(deviceCode, "设备进入" + status + "状态...");
                                Map statusmap = new HashMap();
                                statusmap.put("EquipStatus", status);
                                EquipModel equipModel = GlobalConstants.stage.equipModels.get(deviceCode);
                                if (equipModel != null) {
                                    equipModel.changeEquipPanel(statusmap);
                                    equipModel.preEquipStatus = prestatus;
                                    equipModel.equipStatus = status;
                                    preEquipstatus = prestatus;
                                    equipstatus = status;
                                    if ("pause".equalsIgnoreCase(preEquipstatus) && "RUN".equalsIgnoreCase(equipstatus)) {
                                        if (equipModel.checkLockFlagFromServerByWS(deviceCode)) {
                                            String stopResult = equipModel.pauseEquip();
                                            UiLogUtil.appendLog2SeverTab(deviceCode, "检测到设备被Server要求锁机,设备将被锁!");
                                        }
                                    }
                                    if (("Ready".equalsIgnoreCase(preEquipstatus) && "RUN".equalsIgnoreCase(equipstatus)) || "RUNning".equalsIgnoreCase(equipstatus)) {
                                        logger.info("设备:" + deviceCode + "开机作业.");
                                        UiLogUtil.appendLog2EventTab(deviceCode, "设备进入运行状态...");
                                        if (!GlobalConstants.stage.equipModels.get(deviceCode).startCheck()) {
                                            String stopResult = GlobalConstants.stage.equipModels.get(deviceCode).stopEquip();
                                            UiLogUtil.appendLog2EventTab(deviceCode, "设备将被锁机...");
                                            String holdDesc = "";
                                            Map mqMap = new HashMap();
                                            if ("0".equals(stopResult)) {
                                                holdDesc = "当前设备已经被锁机";
                                                Map mapTmp = new HashMap();
                                                mapTmp.put("EquipStatus", "Idle");
                                                equipModel.changeEquipPanel(mapTmp);
                                                UiLogUtil.appendLog2EventTab(deviceCode, "锁机成功...");
                                                mqMap.put("holdResult", "锁机成功");
                                            } else {
                                                mqMap.put("holdResult", "锁机失败");
                                                holdDesc = stopResult;
                                            }
                                            mqMap.put("holdDesc", holdDesc);
                                            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
                                        }
                                    }
                                    sendDeviceInfoExtAndOplog2Server(equipModel, preEquipstatus);
                                }
                            }
                            if ((index = temp.indexOf("done")) != -1) {//遇到done时就结束接收
                                sb.append(temp.substring(0, index));
                                break;
                            }
                            sb.append(temp);
                        }
                        //   System.out.println("from client: " + sb);
                        //读完后写一句
//            Writer writer = new OutputStreamWriter(socket.getOutputStream());
//            writer.write("Hello Client.");
//            writer.flush();
//            writer.close();
                        reader.close();
                        socket.close();
                        server.close();
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        //  System.out.println(".run()" + e.getMessage());
                    }
                }
            }
        }.start();

    }

    private static void sendDeviceInfoExtAndOplog2Server(EquipModel equipModel, String preEquipstatus) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        DeviceService deviceService = new DeviceService(sqlSession);
        DeviceInfoExt deviceInfoExt = deviceService.getDeviceInfoExtByDeviceCode(equipModel.deviceCode);
        if (deviceInfoExt == null) {
            logger.error("未配置:" + equipModel.deviceCode + "的模型信息.");
            return;
        }
        deviceInfoExt.setDeviceStatus(equipModel.equipStatus);
        DeviceOplog deviceOplog = new DeviceOplog();
        List<DeviceOplog> deviceOplogList = deviceService.getDeviceOplog(equipModel.deviceCode);
        if (deviceOplogList == null || deviceOplogList.isEmpty()) {
            deviceOplog = equipModel.setDeviceOplog(preEquipstatus, deviceInfoExt.getLotId());
//            deviceService.saveDeviceOplog(deviceOplog);
        } else {
            String formerDeviceStatus = deviceOplogList.get(0).getCurrDeviceStatus();
            if (!formerDeviceStatus.equals(equipModel.equipStatus)) {
                deviceOplog = equipModel.setDeviceOplog(preEquipstatus, deviceInfoExt.getLotId());
//                deviceService.saveDeviceOplog(deviceOplog);
            }
        }
        sqlSession.close();
        if (!GlobalConstants.isLocalMode) {
            Map mqMap = new HashMap();
            mqMap.put("msgName", "eqpt.EqptStatusChange");
            mqMap.put("deviceCode", equipModel.deviceCode);
            mqMap.put("eventName", "eqpt.EqptStatusChange");
            mqMap.put("deviceInfoExt", JsonMapper.toJsonString(deviceInfoExt));
            mqMap.put("deviceCeid", "0");
            mqMap.put("eventDesc", deviceOplog.getOpDesc());
            mqMap.put("eventDate", GlobalConstants.dateFormat.format(new Date()));
            mqMap.put("deviceOplog", JsonMapper.toJsonString(deviceOplog));
            GlobalConstants.C2SEqptLogQueue.sendMessage(mqMap);
        }
    }

    private static String transferStatus(String status) {
        if (status.contains("START") || status.contains("tart") || status.contains("WORK") || status.contains("ork") || status.contains("un")) {
            status = "Run";
        }
        if (status.contains("IDLE") || status.contains("dle")) {
            status = "Idle";
        }
        if (status.contains("READY") || status.contains("eady")) {
            status = "Ready";
        }
        if (status.contains("STOP") || status.contains("top")) {
            status = "Stop";
        }
        if (status.contains("pause")) {
            status = "Pause";
        }
        return status;
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChildChannelHandler())
                            .option(ChannelOption.SO_BACKLOG, 1024)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);
                    //绑定端口、同步等待  
                    ChannelFuture futrue = bootstrap.bind(12002).sync();

                    //等待服务监听端口关闭  
                    futrue.channel().closeFuture().sync();
                } catch (Exception e) {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        }).start();

    }

    public void startAlarm() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new FileChannelHandler())
                            .option(ChannelOption.SO_BACKLOG, 1024)
                            .childOption(ChannelOption.SO_KEEPALIVE, true);
                    //绑定端口、同步等待  
                    ChannelFuture futrue = bootstrap.bind(Integer.parseInt(GlobalConstants.getProperty("ISECS_EQUIPALARM_LISTEN_PORT"))).sync();
                    //等待服务监听端口关闭  
                    futrue.channel().closeFuture().sync();
                } catch (Exception e) {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        }).start();

    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new EquipStatusHandler());
        }
    }

    private class FileChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new EquipAlarmHandler());
        }
    }
}
