package cn.tzauto.octopus.common.ws;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;
import java.util.Date;


/**
 * Created by leo
 */
@WebService(targetNamespace = "http://www.tzinfo.com")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ClientWebservice {
    @WebMethod
    public String getServerTime() {
        //返回服务器时间的方法
        return new Date(System.currentTimeMillis()).toString();

    }

    public static void main(String[] args) {
        //可以做到不借助web容器（如GlassFish或者Tomcat）发布Web Service应用
        //访问：
        //http://localhost:8088/ClientWebservice
        //http://localhost:8088/ClientWebservice?wsdl
        Endpoint.publish("http://localhost:8088/ClientWebservice", new ClientWebservice());
    }
}
