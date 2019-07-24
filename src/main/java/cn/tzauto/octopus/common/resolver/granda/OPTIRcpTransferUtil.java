/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.granda;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * @author 陈佳能
 */
public class OPTIRcpTransferUtil {

    private static Logger logger = Logger.getLogger(OPTIRcpTransferUtil.class.getName());

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
    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        if (filePath.contains(".jif") || filePath.contains(".sin")) {
            return transferJobFile(filePath);
        }
        if (filePath.contains("Criteria.csv")) {
            return transferCriteriaFile(filePath);
        }
        return new LinkedHashMap();
    }

    private static Map transferCriteriaFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("Item,")) {
                    continue;
                }
                String[] cfg = cfgline.split(",");
                String key = cfg[0];

                if (cfg.length < 2) {
                    map.put(key + "-MIN", "");
//                    System.out.println(key + "-MIN");
                } else {
                    map.put(key + "-MIN", cfg[1]);
//                    System.out.println(key + "-MIN");
                }

                if (cfg.length < 3) {
                    map.put(key + "-MAX", "");
//                    System.out.println(key + "-MAX");
                } else {
                    map.put(key + "-MAX", cfg[2]);
//                    System.out.println(key + "-MAX");
                }
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return map;
        }
    }

    private static Map transferJobFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            String groupName = "";
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("[JobInfo]")) {
                    groupName = "JobInfo-";
                    continue;
                }
                if (cfgline.contains("[StripInfo]")) {
                    groupName = "StripInfo-";
                    continue;
                }
                String[] cfg = cfgline.split("=");
                String key = cfg[0];
                if (cfg.length < 2) {
                    map.put(groupName + key, "");
                } else {
                    map.put(groupName + key, cfg[1]);
                }
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return map;
        }
    }

    public static List transferFromDB(Map paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<String> paraNameList = new ArrayList<>();
        for (int i = 0; i < recipeTemplates.size(); i++) {
            paraNameList.add(recipeTemplates.get(i).getParaName());
        }
        List<RecipePara> recipeParaList = new ArrayList<>();
        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
        for (Map.Entry<String, String> e : entry) {
            if (paraNameList.contains(e.getKey())) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaCode());
                recipePara.setParaName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaName());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipePara.setSetValue(e.getValue());
                recipePara.setMinValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMinValue());
                recipePara.setMaxValue(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getMaxValue());
                recipePara.setParaMeasure(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaUnit());
                recipePara.setParaShotName(recipeTemplates.get(paraNameList.indexOf(e.getKey())).getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
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
