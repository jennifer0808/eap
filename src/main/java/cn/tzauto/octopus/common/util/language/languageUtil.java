package cn.tzauto.octopus.common.util.language;

import java.util.Locale;

/**
 * Created by wj_co on 2019/3/15.
 */
public class languageUtil {
    public static final String CN = "zh_CN";
    public static final String EN = "en_US";
    public static final String TW = "zh_TW";

    public Locale getLocale(){
        //Locale locale =new Locale("zh", "TW");//new Locale("zh", "TW")
       Locale locale = Locale.getDefault();

        if(locale!=null && CN.equals(locale.toString())){
           locale = new Locale("zh","CN");
        }else  if(locale!=null && EN.equals(locale.toString())){
            locale = new Locale("en","US");
        }else  if(locale!=null && TW.equals(locale.toString())){
            locale = new Locale("zh","TW");
        }else if(locale!=null){
            locale = new Locale("zh","CN");
        }


        return locale;

    }


    public static void main(String[] args) {
        languageUtil l =new languageUtil();
        System.out.println(l.getLocale());

    }
}
