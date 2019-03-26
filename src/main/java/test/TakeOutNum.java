/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 陈佳能
 */
public class TakeOutNum {

    public static void main(String[] args) {
        String a = "45毫米/秒";
        String b = "-21毫米/秒";
        String c = "7.1毫米/秒";
        String d = "-9.3公斤";

        System.out.println(takeNumber(a));
        System.out.println(takeNumber(b));
        System.out.println(takeNumber(c));
        System.out.println(takeNumber(d));

        String result2 = "鍋滄";
        byte[] SS;
        try {
//            SS = result2.getBytes("UTF-8");
            SS = result2.getBytes("GBK");
            String result3 = new String(SS, "UTF-8");
            System.out.println("=====" + result3);
            System.out.println(new String(result3.getBytes(), "GBK"));
            System.out.println(new String(result3.getBytes("UTF-8"), "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

    }

    public static String takeNumber(String str) {
        List<String> recipeNameList = new ArrayList<>();
        List<String> result2 = new ArrayList<>(); 
//        result2.add = ("");
//        result2.add = ("");
        if (result2 != null && result2.size() > 1) {
                for (String str1 : result2) {
                    if (!str1.contains(".xml")) {
                        continue;
                    }
                    if (str1.contains("xml") && str1.contains("]")) {
                        String[] recipeNameTemps = str1.split("]");
                        for (String temp : recipeNameTemps) {
                            if (temp.contains(".xml")) {
                                String[] temps = temp.split(".xml");
                                recipeNameList.add(temps[0].replace(" ", ""));
                                break;
                            }
                        }
                    }
                    if (str.contains(".xml.lnk")) {
                        String[] recipeNameTmps = str.split(".xml.lnk");
                        for (String temp : recipeNameTmps) {
                            if (temp.contains(".xml")) {
                                String[] temps = temp.split(".xml");
                                recipeNameList.add(temps[0].replace(" ", ""));
                                continue;
                            }
                        }
                    }
                    if (str.contains(".xml")) {
                        String[] recipeNameTmps = str.split(".xml");
                        for (String temp : recipeNameTmps) {
                            recipeNameList.add(temp.replace(" ", ""));
                        }
                    }
                }
            }
        
        
        String result = "";
        if (str != null && !"".equals(str)) {
            char[] strArray = str.toCharArray();
            for (char c : strArray) {
                if (c >= 45 && c <= 57 && c != 47) {
                    result += c;
                }
            }
        }
        return result;
    }
}
