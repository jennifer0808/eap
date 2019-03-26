package WebService.Basepublish;


import WebService.Interface.BaseWebservices;

import javax.jws.soap.SOAPBinding;
import javax.xml.ws.Endpoint;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;


@javax.jws.soap.SOAPBinding(style= SOAPBinding.Style.DOCUMENT,parameterStyle= SOAPBinding.ParameterStyle.WRAPPED)

public class BaseWebservicePublish {

    public static HashMap<String, String> useFlagMap = new HashMap<String, String>();
   public void publish() {
       try {
           InputStream in = BaseWebservices.class.getClassLoader().getResourceAsStream("webservice.properties");
           Properties prop = new Properties();
           prop.load(in);
           useFlagMap.put("PublishUseFlag",prop.getProperty("PublishUseFlag"));
           useFlagMap.put("BackUpRecipeUseFlag",prop.getProperty("BackUpRecipeUseFlag"));
           useFlagMap.put("ChangDeviceStateUseFlag",prop.getProperty("ChangDeviceStateUseFlag"));
           useFlagMap.put("CheckBeforeDownloadUseFlag",prop.getProperty("CheckBeforeDownloadUseFlag"));
           useFlagMap.put("DownloadRecipeUseFlag",prop.getProperty("DownloadRecipeUseFlag"));
           useFlagMap.put("GetSpecificDataUseFlag",prop.getProperty("GetSpecificDataUseFlag"));
           useFlagMap.put("GetRecipeListFromDeviceUseFlag",prop.getProperty("GetRecipeListFromDeviceUseFlag"));
           useFlagMap.put("UpdataUserInfoUseFlag",prop.getProperty("UpdataUserInfoUseFlag"));
           useFlagMap.put("UpdateRecipeTemplateUseFlag",prop.getProperty("UpdateRecipeTemplateUseFlag"));
           useFlagMap.put("UpdateVerNoUseFlag",prop.getProperty("UpdateVerNoUseFlag"));
           useFlagMap.put("UpGradeRecipeUseFlag",prop.getProperty("UpGradeRecipeUseFlag"));
           useFlagMap.put("UploadRecipeUseFlag",prop.getProperty("UploadRecipeUseFlag"));
           useFlagMap.put("SysName",prop.getProperty("SysName"));
       } catch (Exception e) {
           throw new ExceptionInInitializerError(e);
       }
       if("1".equals(useFlagMap.get("PublishUseFlag"))){
           Endpoint.publish("http://localhost:8089/BaseWebServicePublish?wsdl", new BaseWebservices());
           System.out.println("Web Service暴露成功！");
       }

}




}