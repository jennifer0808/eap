/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.security;

import cn.tzauto.octopus.common.util.tool.TimeUtils;


/**
 * 
 */
public class BaseEncrypt {
    public static String encode(String src)
    {
        byte[] encodeBytes = Base64.getEncoder().encode(src.getBytes());
        return new String(encodeBytes);
    }

    public static String decode(String src)
    {
        byte[] decodeBytes = Base64.getDecoder().decode(src.getBytes());
        return new String(decodeBytes);
    }
    
    public static String findKey(){
        //JCET加上日期
        return encode("JCET"+ TimeUtils.gettoDay());
    }
    
    public static boolean correctKey(String key){
        String nowday= "JCET"+TimeUtils.gettoDay();
        String nowdaywithoutZero="JCET"+TimeUtils.gettoDaywithoutZero();
        if(nowday.equals(BaseEncrypt.decode(key))||nowdaywithoutZero.equals(BaseEncrypt.decode(key))){
            return true;
        }
        return false;
    }
    
    public static void main(String[] args) {
        String key = findKey();
        System.out.println(key);
        System.out.println(correctKey(key));
        
    }
}
