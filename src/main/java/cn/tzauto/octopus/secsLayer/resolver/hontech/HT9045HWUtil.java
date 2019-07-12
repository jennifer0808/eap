/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.hontech;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author WangDanfeng
 * @version V1.0
 * @desc
 */
public class HT9045HWUtil {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HT9045HWUtil.class);

    public static Map<String, String> transferFromFile(String filePath) {
        Map<String, String> resultMap = new HashMap<String, String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String line = "";
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                //String newLine = line.replaceAll("\\\\r\\\\n", "\r\n");
                //System.out.println(newLine);
                String[] newLineList = line.split("\\\\r\\\\n");
                String classesName = "";
                String groupName = "";
                String paraName = "";
                String paraValue = "";
//                List<String> list= new ArrayList();
                for (String s : newLineList) {
                    //System.out.println(s);
                    if (s.contains(".Data")) {
                        classesName = s;
                    } else if (s.contains("]")) {
                        groupName = s.trim().replaceAll("\\s+", "_");
                    } else {
                        String[] ss = s.split("=");
                        if (ss.length > 1) {
                            paraName = ss[0].trim().replaceAll("\\s+", "_");
                            paraValue = ss[1].trim();
                            if (paraValue.length() > 99) {
                                System.out.println(paraName + ":" + paraValue.length());
                                paraValue = paraValue.substring(0, 99);
                            }
                        } else {
                            paraName = ss[0].trim().replaceAll("\\s+", "_");
                            paraValue = "";
                            if (paraValue.length() > 99) {
                                System.out.println(paraName + ":" + paraValue.length());
                                paraValue = paraValue.substring(0, 99);
                            }
                        }
                        resultMap.put(classesName + groupName + paraName, paraValue);
//                        boolean flag = list.contains(groupName+paraName);
//                        if(flag){
//                            System.out.println(groupName+paraName+"*********");
//                        }else{
//                            list.add(groupName+paraName);
//                        }
                        // System.out.println(classesName+groupName+paraName + ":" +"                "+ paraValue);
//                        System.out.println(classesName);
                        //System.out.println(groupName + paraName);
//                        System.out.println(paraValue);                        
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return null;
                }
            }
            return resultMap;
        }
    }

    public static List transferFromDB(Map<String, String> paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            String key = recipeTemplate.getGroupName() + recipeTemplate.getParaName();
            if (key == null || "".equals(key)) {
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue(paraMap.get(key));
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }

    public static List getRecipePara(String filePath, String deviceType) {
        return transferFromDB(transferFromFile(filePath), deviceType);
    }

    public static void main(String[] args) {
        String filePath = "";
        filePath = "C:\\Users\\SunTao\\Desktop\\长电\\Test HT9045HW\\HT9045-recipe.txt";
        Map<String, String> recipeParaMap = HT9045HWUtil.transferFromFile(filePath);
        for (String s : recipeParaMap.keySet()) {
            if (recipeParaMap.get(s).length() > 100) {
                System.out.println(s + ":" + recipeParaMap.get(s).length());
            }
        }
    }

}
