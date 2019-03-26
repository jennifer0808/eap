package WebService.Test;

import WebService.Interface.BaseWebservices;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Test {
    public static  void main(String[] args) {
        BaseWebservices baseWebservices=new BaseWebservices();
        Map map=new HashMap();
        map.put("deviceTypeId","D7400-6013");
        map.put("deviceCode","D7400-6013");
         map.put("MethodName","BackUpRecipe");
      //  map.put("MethodName","ChangDeviceState");
       // map.put("MethodName","BackUpRecipe");
      //  map.put("MethodName","CheckBeforeDownload");
        map.put("MethodName","DownloadRecipe");
        map.put("type","aaa");
        map.put("state","on");
        Object o = JSONObject.toJSON(map);

      String  s= baseWebservices.handle(o.toString());
        System.out.println(s);

        //1.初始化全局变量 hostManager
        //2.publish
    }
}
