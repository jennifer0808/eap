package WebService.Interface;

import cn.tzauto.octopus.common.util.tool.JsonMapper;
import org.apache.log4j.Logger;

import javax.jws.WebService;
import java.util.HashMap;
import java.util.Map;

import static WebService.Basepublish.BaseWebservicePublish.useFlagMap;

@WebService(endpointInterface="WebService.Interface.BaseWebservice",serviceName="BaseWebservices")//指定webservice所实现的接口以及服务名称
public class BaseWebservices implements BaseWebservice {
        private  String IsUser;
        private String deviceTypeId;
        private String deviceCode;
        private String MethodName;
        private String SysName;
        private static Logger logger = Logger.getLogger(BaseWebservices.class);
        private static Logger mqLogger = Logger.getLogger("mqLog");
        HashMap isUseFlagmap=useFlagMap;
       String strs="11";
    String token = "";
    String  ReqTimes="";
    BaseWebservice baseWebservice;
    String encrypt="";
        @Override
        public String handle (String message){
            Map map=new HashMap<>();
                try {

                    HashMap maps = (HashMap) JsonMapper.fromJsonString(message.replace("\n", ""), HashMap.class);
                    deviceTypeId = String.valueOf(maps.get("deviceTypeId") + "");
                    deviceCode = String.valueOf(maps.get("deviceCode") + "");
                    MethodName = String.valueOf(maps.get("MethodName") + "");
                    SysName=String.valueOf(maps.get("SysName"+""));
                    token=String.valueOf(maps.get("token") + "");
                    ReqTimes=String.valueOf(maps.get("ReqTimes") + "");
                    String aesInfo=SysName+"@"+ReqTimes;
                    String aesKey = "370b15019219dfc81a6cc4f1192cf623";
                   if("".equals(token)){
                        if(SysName.equals(isUseFlagmap.get("SysName"))){
                             encrypt= AESUtil.AesEncrypt(aesInfo, aesKey);
                        }else{
                            return "0003";
                        }

                   }else{
                       String aesDecrypt = null;
                       try {
                           aesDecrypt = AESUtil.AesDecrypt(token, aesKey);
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                       if(!"".equals(aesDecrypt)){
                        String ss=   aesDecrypt.substring(0,aesDecrypt.indexOf("@"));
                        if(ss.equals(isUseFlagmap.get("SysName"))){

                        }
                       }else{
                           return "0003";
                       }
                   }


                    IsUser = String.valueOf(isUseFlagmap.get(MethodName + "UseFlag") + "");
//                    mqLogger.info("接收到webservice==================================,deviceCode：" + deviceCode + "，MethodName:" + MethodName + "，deviceTypeId:" + deviceTypeId);
                    //todo 根据properties 判断ws是否可反射调用
                    if ("1".equals(IsUser)) {

                        try {
                            baseWebservice = (BaseWebservice) Class.forName("WebService.impl." + String.valueOf(MethodName).replaceAll("\"", "") + "Webservice").newInstance();
                            if(baseWebservice==null){
                                return "0004";
                            }else{
                                strs = baseWebservice.handle(message);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            mqLogger.error(ex);
                        }
                    }
                    logger.info("Webservice请求处理结束...");
                } catch (Exception ex) {
                    logger.error("Exception", ex);
                    mqLogger.error("接收到mq==================================,Exception:" + deviceCode, ex);
                    ex.printStackTrace();
                }
            HashMap returnmap = (HashMap) JsonMapper.fromJsonString(strs.replace("\n",""),HashMap.class);
                if(!"".equals(encrypt)){
                    returnmap.put("token",  encrypt);
                }

                return returnmap.toString();


    }
    }

