/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.granda;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 陈佳能
 */
public class OPTIRcpTransferUtil {

    private static Logger logger = Logger.getLogger(OPTIRcpTransferUtil.class);

    public static List transferOptiRcp(String recipePath) {
        File file = new File(recipePath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("OPTI730", "RecipePara");
        List<RecipePara> recipeParas = new ArrayList<>();
        try {
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            String line = "";
            String paraName = "";
            String paraValue = "";
            List<String> paraNameList = new ArrayList<>();
            for (int i = 0; i < recipeTemplates.size(); i++) {
                paraNameList.add(recipeTemplates.get(i).getParaName());
            }
            try {
                for (int j = 0; j < recipeTemplates.size(); j++) {
                    while ((line = br.readLine()) != null) {
                        RecipePara recipePara = new RecipePara();
                        System.out.println(line);
                        if (line.contains("[")) {
                            continue;
                        }
                        if (line.contains("=")) {
                            if ("=".equals(line.substring(line.length() - 1))) {
                                recipePara.setParaName(recipeTemplates.get(j).getParaCode());
                                recipePara.setParaName(recipeTemplates.get(j).getParaName());
                                recipePara.setSetValue("");
                                recipeParas.add(recipePara);
                                break;
                            } else {
                                String[] values = line.split("\\=");
                                paraValue = values[1];
                                paraName = values[0];
                                if (paraName.equalsIgnoreCase(recipeTemplates.get(j).getParaName())) {
//                                if (!"".equals(paraValue) && !paraValue.isEmpty()) {
                                    recipePara.setParaCode(recipeTemplates.get(j).getParaCode());
                                    recipePara.setParaName(recipeTemplates.get(j).getParaName());
                                    recipePara.setSetValue(paraValue);
                                    recipeParas.add(recipePara);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
            }
        } catch (FileNotFoundException ex) {
        }
        logger.info("解析recipe成功");
        return recipeParas;
    }

    public static void main(String args[]) {
        String recipePath = "E:\\YSH35DFN002038-4B.jif";
        List<RecipePara> recipeParas = transferOptiRcp(recipePath);
        System.out.println(recipeParas.size());
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString());
        }

    }
}
