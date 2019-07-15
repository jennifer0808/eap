package cn.tzauto.octopus.common.resolver;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * the common util class of recipe param resolve
 */
public class RecipeFileUtil {

    public static List<RecipePara> transfer2DB(RecipeFileResolver recipeFileHandler, File file, List<RecipeTemplate> recipeTemplates, boolean useShortName) {
        if (recipeFileHandler == null) {
            recipeFileHandler = new DefaultRecipeFileResolver();
        }
        Map<String, String> paramMap = recipeFileHandler.resolve(file);
        return transfer2DB(paramMap,recipeTemplates,useShortName);
    }

    public static List<RecipePara> transfer2DB(Map<String,String> paramMap, List<RecipeTemplate> recipeTemplates, boolean useShortName) {
        List<RecipePara> recipeParaList = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            String key = useShortName ? recipeTemplate.getParaShotName() : recipeTemplate.getParaName();
            if (key == null || "".equals(key)) {
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue(paramMap.get(key));
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }


    static class DefaultRecipeFileResolver implements RecipeFileResolver {

        @Override
        public Map<String, String> resolve(File file) {
            Map<String, String> map = new LinkedHashMap<>();
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line = bufferedReader.readLine();
                while (line != null) {
                    if (line.contains("=")) {
                        String[] kv = line.split("=");
                        if (kv.length == 1) {
                            map.put(kv[0], null);
                        } else {
                            map.put(kv[0], kv[1]);
                        }
                    }
                    line = bufferedReader.readLine();
                }
            } catch (Exception e) {

            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return map;
        }
    }
}
