package cn.tzauto.octopus.common.ws;

import cn.tzauto.octopus.common.globalConfig.GlobalConstants;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.log4j.Logger;

public class AvaryAxisUtil {

    private static final Logger logger = Logger.getLogger(AxisUtility.class);

    public static String webServicesToCRM(String method, Object[] parms ) {

        String endPoint ="szecpw014.eavarytech.com:8001/WebServiceForSZ/Service1.asmx";
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endPoint));
            call.setOperationName(method);
            String jsonResult = String.valueOf(call.invoke(parms));
            logger.info("调用CRM的接口中方法--》"+method+"：的结果：" + jsonResult);
            return jsonResult;
        } catch (Exception e) {
            logger.error("调用CRM的接口中方法--》"+method+"：方法异常：", e);
            return "Error";
        }
    }
}
