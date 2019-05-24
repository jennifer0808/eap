package cn.tzauto.octopus.secsLayer.resolver.cctech;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class C6800Util {

    private static Logger logger = Logger.getLogger(C6800Util.class);

    public static List<RecipePara> getRecipePara(String recipePath, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        return RecipeUtil.transfer2DB(new RecipeFileHandler() {
            @Override
            public Map<String, String> handler(File file) {
                Map<String, String> map = new LinkedHashMap<>();
                BufferedReader bufferedReader = null;
                try {
                    bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                    String line = bufferedReader.readLine();
                    String prefix = "";
                    while (line != null) {
                        if (line.startsWith("[")) {
                            prefix = line;
                        }
                        if (line.contains("=")) {
                            String[] kv;
                            if (prefix.startsWith("[ProductParam") || prefix.startsWith("[TestParam")) {
                                line = bufferedReader.readLine();
                                String key = line.split("=")[0] + prefix;
                                //line = bufferedReader.readLine();
                                String val = line.split("=")[1];
                                map.put(key, val);
                            } else {
                                kv = line.split("=");
                                if (kv.length == 1) {
                                    map.put(kv[0], null);
                                } else {
                                    map.put(kv[0] + prefix, kv[1]);
                                }
                            }
                        }
                        line = bufferedReader.readLine();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return map;
            }
        }, new File(recipePath), recipeTemplates, false);
    }

}
