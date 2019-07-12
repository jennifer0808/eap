/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.apt;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.IOUtil;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

public class APTRecipeUtil {

    private static final Logger logger = Logger.getLogger(APTRecipeUtil.class);

    public static void main(String[] args) {
        String filePath = "C:\\Users\\wjy\\Desktop\\压力烤箱\\P121";
//        List<APTPPbody> aPTPPbodys = handleAPTBody(unAptRecipe(filePath));
        List<RecipePara> recipeParas = transRcpParaFromDB(filePath, "APT-VTS-85A");
//        for (APTPPbody aPTPPbody : aPTPPbodys) {
//            System.out.println(aPTPPbody.getKey()+"----"+aPTPPbody.getValue());
//        } 
        System.out.println(recipeParas.size());
    }

    public static List<RecipePara> transRcpParaFromDB(String filePath, String deviceType) {
        List<APTPPbody> aPTPPbodys = unAptRecipe(filePath);
        aPTPPbodys = handleAPTBody(aPTPPbodys);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (APTPPbody aPTPPbody : aPTPPbodys) {
                if (recipeTemplate.getParaName().equals(aPTPPbody.getKey())) {
                    RecipePara recipePara = new RecipePara();
                    recipePara.setParaCode(recipeTemplate.getParaCode());
                    recipePara.setParaName(aPTPPbody.getKey());
                    recipePara.setSetValue(aPTPPbody.getValue());
                    recipePara.setMinValue(recipeTemplate.getMinValue());
                    recipePara.setMaxValue(recipeTemplate.getMaxValue());
                    recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                    recipePara.setParaShotName(recipeTemplate.getParaShotName());
                    recipeParas.add(recipePara);
                    break;
                }
            }
        }
        return recipeParas;
    }

    public static List unAptRecipe(String filePath) {
        File source = new File(filePath);
        List<APTPPbody> aPTPPbodys = new ArrayList<>();
        String line = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(new FileInputStream(source));
            br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                APTPPbody aPTPPbody = new APTPPbody();
                if (line.contains("[")) {
                    String type = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                    aPTPPbody.setType(type);
                } else if (line.contains("=")) {
                    String key = line.substring(0, line.indexOf("="));
                    String value = line.substring(line.indexOf("=") + 1);
                    aPTPPbody.setKey(key.trim());
                    aPTPPbody.setValue(value.trim());
                }
                aPTPPbodys.add(aPTPPbody);
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(isr, br);
        }
        return aPTPPbodys;
    }

    public static List<APTPPbody> handleAPTBody(List<APTPPbody> aPTPPbodys) {
        String type = "";
        //把参数的name加上type
        for (APTPPbody aptbody : aPTPPbodys) {
            if ((aptbody.getType()) != null) {
                type = aptbody.getType();
                continue;
            }
            String key = type + aptbody.getKey();
            aptbody.setKey(key);
        }
        //去除没有key的参数
        for (int i = 0; i < aPTPPbodys.size(); i++) {
            String key = aPTPPbodys.get(i).getKey();
            if (key == null || "".equals(key)) {
                aPTPPbodys.remove(i);
                i--;
            }
        }
        return aPTPPbodys;
    }
}
