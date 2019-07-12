/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *
 */
public class TestJDBC {
     public static void main(String[] args) {
        Jdbcoracle();
    }

    public static void Jdbcoracle() {
        Connection conn = null;
        PreparedStatement pre = null;
        ResultSet result = null;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:" + "thin:@172.17.255.102:1521:b2btestdb";
            String user = "b2b";
            String password = "b2b";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("连接成功");
//            String sql = "select * from  where name=?";
//            pre = conn.prepareStatement(sql);
//            pre.setString(1, "?");
//            result = pre.executeQuery();
           // System.out.print(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {

                if (result != null) {
                    result.close();
                }
                if (pre != null) {
                    pre.close();
                }
                if (conn != null) {
                    conn.close();
                }
                System.out.println("数据库连接已关闭！");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
