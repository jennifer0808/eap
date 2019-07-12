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
import java.util.HashMap;
import java.util.List;

public class GrandaRcpTransferUtil {

    private static Logger logger = Logger.getLogger(GrandaRcpTransferUtil.class);

    public static List transferGrandaRcp(String recipePath) {
        File file = new File(recipePath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("GRANDGIS126Z1", "RecipePara");
        List<RecipePara> recipeParas = new ArrayList<>();
        try {
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            String line = "";
//                String paraName = "";
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
                                if (!"".equals(paraValue) && !paraValue.isEmpty()) {
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

    public static List transferGrandaRcp(String deviceType, String recipePath) {
        File file = new File(recipePath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        HashMap<String, String> paraMap = new HashMap();
        try {
            FileReader reader = new FileReader(file);
            BufferedReader br = new BufferedReader(reader);
            String line = "";
            String groupName = "";
            String paraValue = "";
            String paraName = "";
            while ((line = br.readLine()) != null) {
                if (line.contains("[")) {
                    groupName = line.replace("[", "").replace("]", "");
                    continue;
                }
                if (line.contains("=")) {
                    String[] values = line.split("=");
                    paraName = groupName + "-" + values[0];
                    paraValue = "";
                    if (values.length > 1) {
                        paraValue = values[1];
                    }
                }
                System.out.println(paraName);
                paraMap.put(paraName, paraValue);
            }
        } catch (IOException ex) {
        }
        List<RecipePara> recipeParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setSetValue(paraMap.get(recipeTemplate.getParaName()));
            recipeParas.add(recipePara);
        }
        logger.info("解析recipe成功");
        return recipeParas;
    }

    public static void main(String args[]) {
        //BB27DFN008031-4B.pkg_V0  YXJ88DFN008133.pkg_V0
        String recipePath = "D:\\RECIPE\\YXJ88DFN008133.pkg_V0";
//         String recipePath = "D:\\RECIPE\\BB27DFN008031-4B.pkg_V0";
        List<RecipePara> recipeParas = transferGrandaRcp("GIS126CZ1", recipePath);
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString());
        }

    }
}
