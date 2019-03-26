/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.aurigin;

/**
 *
 * @author 陈佳能
 */
    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.IOUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author Administrator
 */
public class BTURecipeUtilTest {
    
    private static final Logger logger = Logger.getLogger(BTURecipeUtilTest.class.getName());
    
    public static List transferBTURcp(String recipePath) {
        FileReader fr = null;
        BufferedReader bfr = null;
        OutputStream os = null;
        
        File file = new File(recipePath);
        List<RecipePara> recipeParas = new ArrayList<>();
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("BTUPYRAM125NZ3", "RecipePara");
        try {
            fr = new FileReader(file);
            bfr = new BufferedReader(fr);
            String ss = null;
            String para = null;
            for (RecipeTemplate recipeTemp : recipeTemplates) {
                while ((ss = bfr.readLine()) != null) {
                    RecipePara recipePara = new RecipePara();
                    if (ss.contains(recipeTemp.getParaName())) {
                        if (ss.contains("href")) {
                            para = ss.substring(ss.indexOf("\"") + 1, ss.lastIndexOf("\""));
                        } else {
                            String[] str1 = ss.split(">");
                            String[] str2 = str1[1].split("<");
                            para = str2[0];
                        }
                        
                        recipePara.setParaCode(recipeTemp.getParaCode());
                        recipePara.setParaDesc(recipeTemp.getParaDesc());
                        recipePara.setParaMeasure(recipeTemp.getParaUnit());
                        recipePara.setParaName(recipeTemp.getParaName());
                        recipePara.setParaShotName(recipeTemp.getParaShotName());
                        recipePara.setSetValue(para);
                        recipeParas.add(recipePara);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(os, bfr, fr);
        }
        sqlSession.close();
        return recipeParas;
    }
    public static void main (String[] args) {
        List<RecipePara> recipeParas = transferBTURcp("E:\\FCCSP(0808-0.65)121.xml");
        System.out.println(recipeParas.size());
        for (RecipePara para : recipeParas) {
            System.out.println(para.getParaName() + "   " + para.getSetValue());
        }
        
    }
}


