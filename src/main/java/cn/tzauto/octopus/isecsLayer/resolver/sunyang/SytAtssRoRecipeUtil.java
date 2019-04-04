package cn.tzauto.octopus.isecsLayer.resolver.sunyang;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SytAtssRoRecipeUtil {

    public static final String[] paramName = {"[MSPEED]","[DISTANCE]","[SSPEED]","[ATIME]","[DTIME]","[MotorTimes]"};

    public static Connection getConnection(String url) throws Exception {
        Class.forName("com.hxtt.sql.access.AccessDriver").newInstance();
        Connection conn = DriverManager.getConnection(url, "", "");
        return conn;
    }

    public static List<RecipePara> transfer2DB(String url, String tableName, List<RecipeTemplate> recipeTemplates) throws Exception{
        List<RecipePara> list = new ArrayList<>();
        Connection connection = getConnection(url);
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("select * from " + tableName + " order by POS_NO asc");
        ResultSetMetaData metaData = rs.getMetaData();
        Map<String,RecipeTemplate> paraMap = new HashMap<>();
        for(RecipeTemplate recipeTemplate : recipeTemplates) {
            paraMap.put(recipeTemplate.getParaName(),recipeTemplate);
        }
        int i = 0;
        while(rs.next()) {
            String position = rs.getString("position");
            for(String s : paramName) {
                RecipePara recipePara = new RecipePara();
                String paraName = position + s;
                RecipeTemplate recipeTemplate = paraMap.get(paraName);
                recipePara.setParaCode(recipeTemplate.getParaCode());
                if("SPARE".equals(position)) {
                    recipePara.setParaName(position + i + s);
                }else{
                    recipePara.setParaName(recipeTemplate.getParaName());
                }
                recipePara.setParaShotName(recipeTemplate.getParaShotName());
                recipePara.setSetValue(rs.getString(s.substring(1,s.length()-1)));
                recipePara.setMinValue(recipeTemplate.getMinValue());
                recipePara.setMaxValue(recipeTemplate.getMaxValue());
                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                list.add(recipePara);
            }
            if("SPARE".equals(position)) {
                i++;
            }
        }
        return list;
    }

    public static void main(String[] args) throws Exception {
//        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//        RecipeService recipeService = new RecipeService(sqlSession);
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("SYT-ATSS-RO", "RecipePara");
//        sqlSession.close();
//        List<RecipePara> list = transfer2DB("jdbc:Access:///d://C0032.mdb","DEVICE2",recipeTemplates);
//        System.out.println(list.size());
//        for(RecipePara l : list) {
//            System.out.println(l);
//        }
        for(int i=0;i<10;i++) {

        }

    }
}
