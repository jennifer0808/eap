package cn.tzauto.octopus.secsLayer.resolver.cctech;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeFileHandler;
import cn.tzauto.octopus.isecsLayer.resolver.vision.RecipeUtil;
import cn.tzauto.octopus.secsLayer.equipImpl.cctech.C6800SECSHost;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class C6800Util {

    private static Logger logger = Logger.getLogger(C6800SECSHost.class);

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
                            if (prefix.startsWith("[ProductParam") || prefix.startsWith("[TestParam")||prefix.startsWith("[SysTestParam")) {
                                String key = line.split("=")[1];
                                line = bufferedReader.readLine();
                                String[] vals = line.split("=");
                                if(vals.length == 2) {
                                    map.put(key,vals[1]);
                                }else {
                                    map.put(key, null);
                                }
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

    public static void main(String[] args) {
       List<RecipePara> list =  C6800Util.getRecipePara("E:\\AIB-11X11-S805X-B-FT_V5.txt","CCTECHC6Q420");
       for(RecipePara recipePara:list){
           System.out.println(recipePara.getParaName()+":"+recipePara.getSetValue());
       }
    }


}
