/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.isecsLayer.domain;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author gavin
 */
public class ISecsConnection {

    private final Logger logger = Logger.getLogger(ISecsConnection.class);
    private String ip;
    private String port;
    private Socket socketClient;

    public ISecsConnection(String ip, String port) {
        this.ip = ip;
        this.port = port;
        if (this.ip != null && this.port != null) {
            this.socketClient = initConnection();
            if (this.socketClient == null) {
                logger.info("init socket error");
            }
        } else {
            logger.error("Ip or port is null");
        }
    }

    public Socket initConnection() {
        try {
            Socket socketClient = new Socket();
            socketClient.connect(new InetSocketAddress(this.ip, Integer.parseInt(this.port)), 5000);
            logger.info("Create socket success!!");
            // socketClient.setSoTimeout(5000);
            socketClient.setKeepAlive(true);
            return socketClient;
        } catch (Exception e) {
            logger.error("Create socket fail==>" + ip + ":" + port + "==>" + e.getMessage());
            return null;
        }

    }

    public Socket getSocketClient() {
        if (socketClient != null) {
            return socketClient;
        } else {
            return initConnection();
        }
    }

    public Socket setSocketClient(Socket socket) {
        return socketClient = socket;
    }

    public Socket getNewSocketClient() {
        return initConnection();
    }

    public void dispose(Socket socket) {
        try {
            socket.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ISecsConnection.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Socket关闭异常 ====== >" + ex);
        }
        socket = null;
    }

    public boolean checkConenctionStatus() {
        synchronized (this.socketClient) {
            try {
                if (this.socketClient == null) {
                    logger.info("连接中断，开始重连===>");
                    this.socketClient = initConnection();
                    if (this.socketClient == null) {
                        logger.info("init socket error");
                        return false;
                    }
                }
                int failedCount = 0;
                if (this.socketClient != null) {
                    for (int i = 0; i < 4; i++) {

                        if (sendUrgentData()) {
                            return true;
                        } else {
                            failedCount++;
                            Thread.sleep(1000);
                        }
                    }
                    if (failedCount > 3) {
                        return false;
                    }
                } else {
                    logger.info("无法建立连接====>");
                    return false;
                }
                return true;
            } catch (Exception e) {
                logger.error(e);
                logger.info("Socket异常，重新连接");
                if (this.socketClient != null && !this.socketClient.isClosed()) {
                    try {
                        this.socketClient.close();
                        this.socketClient = initConnection();
                        if (this.socketClient != null) {
                            return true;
                        }
                    } catch (IOException e1) {
                        logger.error(e1);
                        return false;
                    }
                }
                return false;
            }
        }
    }

    private boolean sendUrgentData() {
        try {
            this.socketClient.sendUrgentData(0xFF);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public static void main(String[] args) {
        ISecsConnection iSecsConnection = new ISecsConnection("192.168.103.128", "12000");
        for (int i = 0; i < 200; i++) {
            System.out.println("ISecsConnection.main()" + iSecsConnection.checkConenctionStatus());
        }

    }
}
