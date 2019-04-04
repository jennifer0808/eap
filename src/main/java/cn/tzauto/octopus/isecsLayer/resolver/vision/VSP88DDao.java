package cn.tzauto.octopus.isecsLayer.resolver.vision;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shenk
 */
public class VSP88DDao {

    private String mdbPath;

    private String user;

    private String password;

    public VSP88DDao(String mdbPath, String user, String password) {
        this.mdbPath = mdbPath;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws Exception {
        Class.forName("com.hxtt.sql.access.AccessDriver").newInstance();
        String url = "jdbc:Access:///" + mdbPath;
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * get recipe list
     *
     * @return
     */
    public List<String> getRecipeList() throws Exception {
        List<String> recipes = new ArrayList<>();
        String sql = "select Recipe from RecipeTbl where TYPE = '2'";
        Connection connection = getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        while (resultSet.next()) {
            recipes.add(resultSet.getString("Recipe"));
        }
        connection.close();
        return recipes;
    }

    /**
     * insert a new recipe
     *
     * @param recipeName
     * @param clean
     * @param motion
     * @return
     */
    public boolean insert(String recipeName, String clean, String motion) throws Exception {
        String sql = "insert into RecipeTbl(Recipe,Clean,Motion,Type,DateTime) values('" + recipeName + "','" + clean + "','" + motion + "','2',now())";
        Connection connection = getConnection();
        boolean flag = connection.createStatement().execute(sql);
        connection.commit();
        return flag;
    }

    /**
     * delete recipe
     *
     * @param recipeName
     * @return
     */
    public boolean delete(String recipeName) throws Exception {
        String sql = "delete from RecipeTbl where Recipe = '" + recipeName + "' and Type = '2'";
        Connection connection = getConnection();
        boolean flag = connection.createStatement().execute(sql);
        connection.commit();
        return flag;
    }

    /**
     * 获取recipe对应的clean和motion
     * @param recipe
     * @return String[] 0.clean 1.motion
     */
    public String[] getCleanAndMotionByRecipe(String recipe) throws Exception {
        String[] result = new String[2];
        String sql = "select Clean,Motion from RecipeTbl where Recipe = '" + recipe +"' and Type = '2'";
        Connection connection = getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        while(resultSet.next()) {
           result[0] = resultSet.getString("Clean").trim() + ".pls";
           result[1] = resultSet.getString("Motion").trim() + ".svr";
        }
        return result;
    }

    /**
     * update recipe mapping
     * @param recipe
     * @param clean
     * @param motion
     * @return
     * @throws Exception
     */
    public boolean updateRecipe(String recipe,String clean,String motion) throws Exception {
        String sql = "select Clean,Motion from RecipeTbl where Recipe = '" + recipe +"' and Type = '2'";
        Connection connection = getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery(sql);
        String updateSql;
        if(resultSet.next()) {
            updateSql = "update RecipeTbl set Clean = '" + clean + "' and Motion = '" + motion + "'";
        }else {
            updateSql = "insert into RecipeTbl(Recipe,Clean,Motion,Type,DateTime) values('" + recipe + "','" + clean + "','" + motion + "','2',now())";
        }
        boolean flag = connection.createStatement().execute(updateSql);
        connection.commit();
        connection.close();
        return flag;
    }

    public static void main(String[] args) throws Exception {
        VSP88DDao dao = new VSP88DDao("D:/vsp.mdb", "", "");
        List<String> recipeList = dao.getRecipeList();
        for (String s : recipeList) {
            System.out.println(s);
        }
        //dao.insert("test1", "test1", "test1");
        dao.delete("test");
    }
}
