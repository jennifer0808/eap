/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.htm;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.SqlSession;

public class HtmRecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        String[] groupNames = new String[]{"软化#1", "软化#2", "清洗#1", "高压泵", "去皮氧化#1", "清洗#2", "去皮氧化#2", "清洗#3", "预浸1#", "预浸2#",
            "锡化#1", "锡化#2", "锡化#3", "锡化#4", "清洗#4", "中和", "热纯水清洗#5", "热纯水清洗#6", "烘箱", "退锡1", "清洗#7", "退锡2"};
        String[] items = new String[]{"使用", "电压", "电流", "选项", "温度", "压力", "脉冲On", "脉冲Off", "屏蔽板位置", "A喷嘴", "B喷嘴"};
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            int i = 0;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));

            while ((cfgline = br.readLine()) != null) {
                if (i == groupNames.length) {
                    break;
                }
                String groupName = groupNames[i];
                String[] cfg = cfgline.split("\",");
                for (int j = 0; j < items.length; j++) {
//                    System.out.println(groupName + items[j]);
                    //System.out.println( cfg[j].replaceAll("\"", ""));
                    map.put(groupName + items[j], cfg[j].replaceAll("\"", ""));
                }
                i++;

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

    public static void main(String[] args) {
        //8-260PG-200-180-D175-OS10  8-260PG-200-180-D175-OS10TEST   8-575B-200-90-F125E   8-E8180-75P-0-9011-20   DISCO-AP-8-thin   IC-8-575B-90P-0-DU-2385KS
//        Map map = transferFromFile("D:\\MT6580DOE.txt");//MCXT-010.txt   AMIT-552
//        Set<Map.Entry<String, String>> entry = map.entrySet();
//        for (Map.Entry<String, String> e : entry) {
//            System.out.println(e.getKey() + "——" + e.getValue());
//        }
//        System.out.println(entry.size());
//        List<RecipePara> list = transferFromDB(map, "DISCODFL7161");
//        System.out.println(list.size());
//        Map paraMap = DiscoRecipeUtil.transferFromFile("D:\\RECIPE\\AKJ@12009.txt");
        Map paraMap = HtmRecipeUtil.transferFromFile("D:\\RECIPE\\2.rcp_V0");

        List<RecipePara> list = HtmRecipeUtil.transferFromDB(paraMap, "HTM5022");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }
}
