/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.eo;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
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

public class EORecipeUtil {

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map<String, String> map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            String groupName = "";
            String groupNameC = "";
            int i = 0;
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {

                if (cfgline.contains("]") || cfgline.contains("=") || cfgline.contains("END") || cfgline.contains("START")) {

//                    System.out.println(cfgline.length());
                    if (cfgline.contains("(") || cfgline.contains("（") || cfgline.contains("<")) {
                        continue;
                    }
                    if (cfgline.length() > 60) {
                        continue;
                    }

                    if (cfgline.contains("Vision") || cfgline.contains("END")) {
                        groupNameC = "";
                        continue;
                    }
                    if (cfgline.contains("[")) { //去除[]
                        if (cfgline.contains("START")) {
                            groupName = cfgline.replaceAll("START", "").replaceAll("]", "").replaceAll("\\[", "").trim();
                            continue;
                        }
                    }
                    if (cfgline.contains("//")) { //去除[]
                        if (cfgline.contains("START")) {
                            groupName = cfgline.replaceAll("START", "").replaceAll("//", "").trim();
                            continue;
                        }
                    }
                    if (cfgline.contains("/") && !cfgline.contains("//")) { //去除[]
                        if (cfgline.contains("START")) {
                            groupNameC = groupName + "-" + cfgline.replaceAll("START", "").replaceAll("/", "").trim();
                            continue;
                        }
                    }

                    if (!"".equals(groupName) && cfgline.contains(groupName)) {
                        String[] cfgsTmp = cfgline.split("=");
                        groupNameC = cfgsTmp[0] + cfgsTmp[1];
                    }
                    if ((cfgline.contains("COATING STEP") || cfgline.contains("CLEANING STEP"))) {
                        String[] cfgsTmp = cfgline.split("=");
                        groupNameC = cfgsTmp[0] + cfgsTmp[1];
                    }
                    if (cfgline.contains("CUT PASS NO")) { //去除[]
                        String[] cfgsTmp = cfgline.split("=");
                        groupNameC = groupNameC + "-" + cfgsTmp[0] + cfgsTmp[1];
                    }
                    System.out.println(cfgline + "||||" + isMessyCode(cfgline));
                    if (isMessyCode(cfgline)) {
                        continue;
                    }
                    String[] cfg = null;
                    if (cfgline.contains("=") && !cfgline.equals("=")) {
                        cfg = cfgline.split("=");
                    }
                    key = cfg[0];
                    value = cfg[1];
                    if (!"".equals(groupName) && "".equals(groupNameC)) {
                        key = groupName + "-" + cfg[0];
//                    
                    }
                    if (!"".equals(groupNameC)) {
                        key = groupNameC + "-" + cfg[0];
//                    
                    }
                    System.out.println(i + "  " + key);
                    map.put(key.trim(), value);
                    i++;
                }
            }
            br.close();
//            int p = 0;
//            for (Map.Entry<String, String> entry : map.entrySet()) {
//                java.lang.Object key1 = entry.getKey();
//                java.lang.Object value1 = entry.getValue();
//
//                System.out.println( key1);
//                p++;
//            }

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
        Map paraMap = EORecipeUtil.transferFromFile("D:\\RECIPE\\SN98671EJG-S3-P-ST58660E-GD18-12_V0.txt");

        List<RecipePara> list = EORecipeUtil.transferFromDB(paraMap, "EOLMC3200G3");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
    }

    /**
     * 判断是否为乱码
     *
     * @param str
     * @return
     */
    public static boolean isMessyCode(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            // 当从Unicode编码向某个字符集转换时，如果在该字符集中没有对应的编码，则得到0x3f（即问号字符?）
            //从其他字符集向Unicode编码转换时，如果这个二进制数在该字符集中没有标识任何的字符，则得到的结果是0xfffd
            //System.out.println("--- " + (int) c);
            if (Integer.valueOf(c) > 127 || Integer.valueOf(c) < 32) {
                return true;
            }
            if ((int) c == 0xfffd) {
                // 存在乱码
                //System.out.println("存在乱码 " + (int) c);
                return true;
            }
        }
        return false;
    }
}
