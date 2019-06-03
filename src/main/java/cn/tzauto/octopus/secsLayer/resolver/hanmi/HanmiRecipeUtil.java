/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.hanmi;

//import cn.tfinfo.jcauto.octopus.biz.recipe.domain.hanmiBody;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.secsLayer.resolver.IOUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ccy
 */
public class HanmiRecipeUtil {

    private static final Logger logger = Logger.getLogger(HanmiRecipeUtil.class);

    public static void main(String[] args) {
        String filepath = "C:\\Users\\SunTao\\Desktop\\189X68-2-3X3-0.25-1-001_V0.txt";  //F:\\D6500-6072\\WBQFN5X5-LL-32L-S2\\WBQFN5X5-LL-32L-S2_V0.txt     F:\\D6500-6072\\EEE\\EEE_V0.txt
//                    List s=new HanmiRecipeUtil().hanmiRecipe("C://Users//ccy//Documents//Tencent Files//1063198025//FileRecv//TEST1//");
        Map<String, String> map = hanmiRecipeHandler(filepath);
//        List<RecipePara> recipeParas = transRcpParaFromDB(filepath, "HANMI20000D");
//        for (RecipePara recipePara : recipeParas) {
//            System.out.println(recipePara.getParaName() + "----" + recipePara.getSetValue());
//        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "-----" + entry.getValue());
        }
        System.out.println(map.size());
    }

    public static List<RecipePara> transRcpParaFromDB(String filePath, String deviceType) {
        Map<String, String> paraMap = hanmiRecipeHandler(filePath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
//        Set<Map.Entry<String, String>> entry = paraMap.entrySet();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if(paraMap.get(recipeTemplate.getParaName())!=null){
                RecipePara recipePara = new RecipePara();
                    recipePara.setParaCode(recipeTemplate.getParaCode());
                    recipePara.setParaName(recipeTemplate.getParaName());
                    recipePara.setSetValue(paraMap.get(recipeTemplate.getParaName()));
                    recipePara.setMinValue(recipeTemplate.getMinValue());
                    recipePara.setMaxValue(recipeTemplate.getMaxValue());
                    recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                    recipePara.setParaShotName(recipeTemplate.getParaShotName());
                    recipeParas.add(recipePara);
            }
//            for (Map.Entry<String, String> e : entry) {
//                if (recipeTemplate.getParaName().equals(e.getKey())) {
//                    RecipePara recipePara = new RecipePara();
//                    recipePara.setParaCode(recipeTemplate.getParaCode());
//                    recipePara.setParaName(e.getKey());
//                    recipePara.setSetValue(e.getValue());
//                    recipePara.setMinValue(recipeTemplate.getMinValue());
//                    recipePara.setMaxValue(recipeTemplate.getMaxValue());
//                    recipePara.setParaMeasure(recipeTemplate.getParaUnit());
//                    recipePara.setParaShotName(recipeTemplate.getParaShotName());
//                    recipeParas.add(recipePara);
//                    break;
//                }
//            }
        }

        return recipeParas;
    }

    public static Map<String, String> hanmiRecipeHandler(String recipePath) {
        Map<String, String> map = new LinkedHashMap<>();
        BufferedReader br = null;
        try {
            String line = null;
            String key = "";
            String value = "";
            File source = new File(recipePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(source), "UTF-8"));
            while ((line = br.readLine()) != null) {
                if (line.contains("Item No")) {
                    key = line.substring(line.lastIndexOf("Name=") + 6, line.lastIndexOf(" ") - 1);
                    value = line.substring(line.lastIndexOf("Value=") + 7, line.lastIndexOf("/>") - 1);
                    map.put(key.trim(), value.trim());
                } else if (line.contains("$Now") || line.contains("DSC")) {
                    //参数是数组的先不考虑
//                    if (line.contains(",")) {
//                        continue;
//                    }
                    String[] cfg = line.split("=");
                    key = cfg[0];
                    if (cfg[0].contains("DSC")) {
                        key = "DEV_ID";
                    }
                    String[] cfg2 = cfg[1].split("\\$");
                    value = cfg2[0];
                    if (value.contains("{")) {  //去除{}
                        value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
                    }
                    if (value.contains("\"")) { //去除引号
                        value = value.replaceAll("\"", "");
                    }
                    key = key.replaceAll(" ", ""); //去除空格
                    value = value.replaceAll(" ", "");
                    if (value == null) {
                        value = "";
                    }
                    if (value.contains(",")) {
                        String[] values = value.split(",");
                        String keyTemp = "";
                        //如果参数值为数组，参数名后面加数字并分别列出
                        for (int j = 0; j < values.length; j++) {
//                            if (j == 0) { //数组第一个的参数名不带数字
//                                map.put(key, values[j]);
//                            } else {
                                keyTemp = key + String.valueOf(j + 1);
                                map.put(keyTemp, values[j]);
//                            }
                        }
                    } else {
                        map.put(key, value);
                    }
//                    map.put(key, value);
                }
            }
        } catch (IOException e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(br);
        }
        return map;
    }
}
