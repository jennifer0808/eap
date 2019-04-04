/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.tool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author WangDanFeng
 * @version V1.0
 * @date 2018-8-10 16:20:25
 * @desc
 */
public class MdbUtil {

    public static Connection getMdbConnection(String filePath, String useName, String passWord) throws ClassNotFoundException, SQLException {
        Properties prop = new Properties();
        prop.put("charSet", "UTF-8"); // 这里是解决中文乱码
        prop.put("user", useName);
        prop.put("password", passWord);
        Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
        Connection con = DriverManager.getConnection("jdbc:ucanaccess://" + filePath, prop);

        return con;
    }

    public static Map<String, Map<String, Map<String, String>>> getRecipeItem(String filePath, String useName, String passWord) {
        Map<String, Map<String, Map<String, String>>> recipeItemMap = null;
        Connection con = null;
        try {
            recipeItemMap = new HashMap<>();
            con = getMdbConnection(filePath, useName, passWord);
            Statement stmt = con.createStatement();

            // 查找数据
            ResultSet tables = con.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
            while (tables.next()) {
                //表
                String tableName = tables.getString("TABLE_NAME");
                //System.out.println(tableName);

                ResultSet rs = stmt.executeQuery("select * from " + tableName);
                int colConunt = rs.getMetaData().getColumnCount();
                //System.out.println(colConunt = rs.getMetaData().getColumnCount());
                Map<String, Map<String, String>> tableMap = new HashMap<>();

                while (rs.next()) {
                    //每行的数据
                    Map<String, String> lineMap = new HashMap<>();
                    for (int i = 0; i < colConunt; i++) {
                        String key = rs.getMetaData().getColumnName(i + 1);
                        String value = rs.getString(i + 1);
//                        System.out.println(key + ":" + value);
                        lineMap.put(key, value);
                    }
                    tableMap.put(rs.getString(1), lineMap);

                }
                //插入整个数据
                recipeItemMap.put(tableName, tableMap);
            }

        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(MdbUtil.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(MdbUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
            return recipeItemMap;
        }


    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException {

        String mdbPath1 = "D:\\RECIPE\\RECIPE\\E3200-9999BGA-10X10-2X2(10X24)temp\\BGA-10X10-2X2(10X24).mdb";
        String mdbPath = "D:\\桌面文件\\长电\\CCTECH\\C6430\\changchuan\\Handler\\C6Q430\\C6SysData.mdb";
        String userName = "";
        String passWord = "cc";
        Map<String, Map<String, Map<String, String>>> map = getRecipeItem(mdbPath, userName, passWord);
//        for (Map.Entry<String, Map<String, Map<String, String>>> entry : map.entrySet()) {
//            
//            String key = entry.getKey();
//            System.out.println("---------------------"+key+"---------------------");
//            if(!"ProductParam".equals(key)&&!"TestParam".equals(key)){
//                continue;
//            }
//            Map<String, Map<String, String>> value = entry.getValue();
//            for (Map.Entry<String, Map<String, String>> entry1 : value.entrySet()) {
//                String key1 = entry1.getKey();
//                System.out.print(key1+"**");
//                Map<String, String> value1 = entry1.getValue();
//                
////                for (Map.Entry<String, String> entry2 : value1.entrySet()) {
////                    String key2 = entry2.getKey();
////                    String value2 = entry2.getValue();
////                    System.out.print("|"+key2+":"+value2);   
////                    
////                } 
//                System.out.println("");
//            }
//            System.out.println("");
//            
//        }
//        Map<String, Map<String, String>> valueProductParam = map.get("ProductParam");
//        for (Map.Entry<String, Map<String, String>> entry1 : valueProductParam.entrySet()) {
//            String key1 = entry1.getKey();
//            System.out.print(key1 + "\t\t");
//            Map<String, String> value1 = entry1.getValue();
//            String paramCode = value1.get("ParamCode");
//            String paramName = value1.get("ParamName");
//            String paramValue = value1.get("ParamValue");
//            char[] chars = new char[40];
//            char[] charS = paramCode.toCharArray();
//            for (int i = 0; i < chars.length; i++) {
//                if (i < charS.length) {
//                    chars[i] = charS[i];
//                } else {
//                    chars[i] = ' ';
//                }
//            }
//            paramCode = String.valueOf(chars);
//            System.out.println(paramCode + paramName);
//                System.out.println(paramValue);

//                for (Map.Entry<String, String> entry2 : value1.entrySet()) {
//                    String key2 = entry2.getKey();
//                    String value2 = entry2.getValue();
//                    System.out.print("|"+key2+":"+value2);   
//                    
//                } 
//        }

//        System.out.println("+++++++++++++++++++++++++++");
//        Map<String, Map<String, String>> valueTestParam = map.get("TestParam");
//        for (Map.Entry<String, Map<String, String>> entry1 : valueTestParam.entrySet()) {
//            String key1 = entry1.getKey();
//            System.out.print(key1 + "\t\t");
//            Map<String, String> value1 = entry1.getValue();
//            String paramCode = value1.get("ParamCode");
//            String paramName = value1.get("ParamName");
//            String paramValue = value1.get("ParamValue");
//            char[] chars = new char[40];
//            char[] charS = paramCode.toCharArray();
//            for (int i = 0; i < chars.length; i++) {
//                if (i < charS.length) {
//                    chars[i] = charS[i];
//                } else {
//                    chars[i] = ' ';
//                }
//            }
//            paramCode = String.valueOf(chars);
//
//            System.out.println(paramCode + paramName);

//                System.out.println(paramValue);
//                for (Map.Entry<String, String> entry2 : value1.entrySet()) {
//                    String key2 = entry2.getKey();
//                    String value2 = entry2.getValue();
//                    System.out.print("|"+key2+":"+value2);   
//                    
//                }
//        }
        Map<String, Map<String, String>> valueProductParam = map.get("ProductInfo");
        for (Map.Entry<String, Map<String, String>> entry1 : valueProductParam.entrySet()) {
            String key1 = entry1.getKey();
//            System.out.print(key1 + "\t\t");
            Map<String, String> value1 = entry1.getValue();
            String productName = value1.get("ProductName");
            String isUsed = value1.get("IsUsed");
//            String paramValue = value1.get("ParamValue");
//            char[] chars = new char[40];
//            char[] charS = paramCode.toCharArray();
//            for (int i = 0; i < chars.length; i++) {
//                if (i < charS.length) {
//                    chars[i] = charS[i];
//                } else {
//                    chars[i] = ' ';
//                }
//            }
//            paramCode = String.valueOf(chars);
            if(isUsed!=null&&"1".equals(isUsed)){
                System.out.println(productName+"********");
                System.out.println(isUsed+"========");
            }


        }


    }

}
