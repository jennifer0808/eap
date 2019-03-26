package cn.tzauto.octopus.isecsLayer.socket;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class SocketUtil {

    private static final Logger logger = Logger.getLogger(SocketUtil.class);

    public static Socket connectToClient(String ip, int port) {
        try {
//            Socket socketClient = new Socket(ip, port);
            Socket socketClient = new Socket();
            socketClient.connect(new InetSocketAddress(ip, port), 5000);
            logger.error("Create socket success!!");
            socketClient.setSoTimeout(5000);
            return socketClient;
        } catch (Exception e) {
            logger.error("Create socket fail==>" + ip + "==>" + e.getMessage());
            return null;
        }

    }

    /**
     * 根据传入的命令执行，并且返回参数
     *
     * @param socketClient
     * @param actionCommon
     * @return
     */
    public static String doCommanAndGetReply(Socket socketClient, String actionCommon) {
        List<String> result = new ArrayList<String>();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream()));
            logger.info("Ready to execute command==>" + actionCommon);
            writer.write(actionCommon);
            writer.flush();
//			socketClient.shutdownOutput();
//			Thread.sleep(3000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            String lineContent = "";//reader.readLine();
//            result.add(lineContent);
            while ((lineContent = reader.readLine()) != null) {
                logger.info("success get reply==>" + lineContent);
                result.add(lineContent);
                if ("done".equals(lineContent)) {
                    break;
                }
//                logger.info("get done flag,and ignore");
            }
            //reader.close();
            //socketClient.shutdownInput();
            logger.info(" execute command [" + actionCommon + "] success and get reply==>" + result);
            return result.get(0);
        } catch (Exception e) {
            logger.error("Execuet command fail==>" + e.getMessage());
            return "socket error";
        }

    }

    /**
     * 根据传入的命令执行，并且返回多行结果
     *
     * @param socketClient
     * @param actionCommon
     * @return
     */
    public static List<String> doCommanAndGetMultReply(Socket socketClient, String actionCommon) {
        List<String> result = new ArrayList<String>();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketClient.getOutputStream()));
            logger.info("Ready to execute command==>" + actionCommon);
            writer.write(actionCommon);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            logger.info(" execute command [" + actionCommon + "] success ");
            String lineContent = "";
            while ((lineContent = reader.readLine()) != null) {
                logger.info("success get reply==>" + lineContent);
                if (!"done".equals(lineContent.trim())) {
                    result.add(lineContent);
                } else {
                    logger.info("get done flag,and ignore");
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Execuet command fail==>" + e.getMessage());
            return result;
        }

    }

    public static void main(String[] args) {
        long time1 = 0, time2 = 0;
        Socket socket = new Socket();
        try {
            if (args.length < 4) {
                System.out.println("参数错误!");
                return;
            }
            time1 = System.currentTimeMillis();
            socket.connect(new InetSocketAddress(args[0], Integer.parseInt(args[1])), Integer.parseInt(args[2]));
            socket.setSoTimeout(Integer.parseInt(args[3]));
            time1 = System.currentTimeMillis();
            socket.getInputStream().read();
        } catch (SocketTimeoutException e) {
            if (!socket.isClosed() && socket.isConnected()) {
                System.out.println("读取数据超时!");
            } else {
                System.out.println("连接超时");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            time2 = System.currentTimeMillis();
            System.out.println(time2 - time1);
        }
    }  
}
