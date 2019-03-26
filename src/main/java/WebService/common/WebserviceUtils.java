package WebService.common;

import WebService.Interface.BaseWebservice;


import java.io.InputStream;
import java.util.Properties;

public class WebserviceUtils {
    private BaseWebservice baseWebservice;
    //webservice开关
    private static boolean IsUseFlag;

    static {
        try {
            InputStream in = WebserviceUtils.class.getClassLoader().getResourceAsStream("webservice.properties");
//            File f = new File(MessageUtils.class.getResource("").getPath()+"/"+"mq.properties");
//            InputStream in =new FileInputStream(f);
            Properties prop = new Properties();
            prop.load(in);

        }catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
