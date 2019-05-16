package cn.tzauto.octopus.common.resolver.hitachi;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

import java.io.*;
import java.util.*;

/**
 *
 * @author luosy
 */
public class LaserDrillUtil {
//filePath 是PPBODY原文件的存储路径(非文件夹)

    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            String groupName = "";
            int i = 1;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));

            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("(") && !cfgline.contains("BLOCK")) {
                    continue;
                }
                if (cfgline.contains(",") && !cfgline.contains("BLOCK")) {
                    String[] cfg = cfgline.split(",");
                    if (cfg.length == 2) {
                        key = cfg[0];
                        value = cfg[1];
                    }
                    if (cfg.length == 3) {
                        key = cfg[0] + cfg[1];
                        value = cfg[2];
                    }
                    if (cfg.length == 4) {
                        key = cfg[0] + cfg[1] + cfg[2];
                        value = cfg[3];
                    }
                    if (cfg.length == 5) {
                        key = cfg[0] + cfg[1] + cfg[2] + cfg[3];
                        value = cfg[4];
                    }
//                    System.out.println(key + "||" + value);
//                    System.out.println(key);
                    map.put(key, value);
                    continue;
                }

                if (cfgline.contains("BLOCK")) {
                    groupName = cfgline;
                    i = 1;
                    continue;
                }

                key = groupName + "-" + i;
                if (key.length() < 3) {
                    continue;
                }
                value = cfgline;
                map.put(key, value);
                i++;
//                System.out.println(key + "||" + value);
//                System.out.println(key);
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    public static void editRecipeName(String filePath, String recipeName, String uoidRecipeName) {
        List<String> list = new ArrayList();
        BufferedReader br = null;
        try {
            String cfgline = null;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "utf-8"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains(recipeName)) {
                    cfgline = cfgline.replaceAll(recipeName, uoidRecipeName);
                }
                list.add(cfgline);
            }
            br.close();

            File file = new File(filePath);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            for (String string : list) {
                bw.write(string);
                bw.newLine();
            }
            //bw.write((String) ppbody);
            bw.close();
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
