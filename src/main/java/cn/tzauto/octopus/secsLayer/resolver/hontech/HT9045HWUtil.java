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

import java.io.*;
import java.util.*;

/**
 *
 * @author WangDanfeng
 * @date 2018-6-15 9:57:36
 * @version V1.0
 * @desc
 */
public class HT9045HWUtil {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HT9045HWUtil.class.getName());

    public static Map<String, String> transferFromFile(String filePath) {
        Map<String, String> resultMap = new HashMap<String, String>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String line = "";
            while ((line = br.readLine()) != null) {
//                System.out.println(line);
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
                    } else if (s.startsWith("[")&&!s.contains("=")) {
                        groupName = s.trim().replaceAll("\\s+", "_");
                    } else {
                        String[] ss = s.split("=");
                        if (ss.length > 1) {
                            paraName = ss[0].trim().replaceAll("\\s+", "_");
                            paraValue = ss[1].trim();
                            if (paraValue.length() > 99) {
//                                System.out.println(paraName + ":" + paraValue.length());
                                paraValue = paraValue.substring(0, 99);
                            }
                        } else {
                            paraName = ss[0].trim().replaceAll("\\s+", "_");
                            paraValue = "";
                            if (paraValue.length() > 99) {
//                                System.out.println(paraName + ":" + paraValue.length());
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
//                         System.out.println(classesName+groupName+paraName + ":" +"                "+ paraValue);
//                        System.out.println(classesName);
//                        System.out.println(groupName + paraName);
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

    public static List getRecipeEcidPara(String filePath, String deviceType) {


        return transferFromDB(transferFromFile(filePath), deviceType);
    }

    public static void main(String[] args) throws IOException {
        String filePath = "";
        filePath = "D:\\RECIPE\\A6\\FT\\HONTECHHT9045HW\\Engineer\\E3200-0362\\AKJ-5X4.6-MT6177-FT\\AKJ-5X4.6-MT6177-FT_V4.txt";
//        Map<String, String> recipeParaMap = HT9045HWUtil.transferFromFile(filePath);
//        for (String s : recipeParaMap.keySet()) {
//            if (recipeParaMap.get(s).length() > 100) {
//                System.out.println(s + ":" + recipeParaMap.get(s).length());
//            }
//        }
//        List<RecipePara> recipeParaList = getRecipePara(filePath,"HONTECHHT9045HW");
//        for(RecipePara r:recipeParaList){
//            System.out.println(r.getParaName()+"====="+r.getSetValue());
//        }

        String shotNames = "Alarm-Consecutive Filure Alarm(Socket),Alarm-Filure Count(Socket),Alarm-Consecutive Filure Alarm(Head),Alarm-Filure Count(Head),Yield Alarm-By Arm Per Site Differ Yield%-Enable,Yield Alarm-By Arm Per Site Differ Yield%-after per site count,Yield Alarm-By Arm Per Site Differ Yield%-Limit,Yield Alarm-By Site Compare Yield%-Enable,Yield Alarm-By Site Compare Yield%-Limit,Yield Alarm-By Site Compare Yield%-After per site count,Yield Alarm-LowYeilds%(By Site)-Enable,Yield Alarm-LowYeilds%(By Site)-Ignore IC count,Yield Alarm-LowYeilds%(By Site)-Limit,LowYield%(ByTotal)-Enable,LowYield%(ByTotal)-Limit,LowYield%(ByTotal)-Ignore IC count,ALL Site Fail-Enable,ALL Site Fail-Count limitation,Tray Name,X_Start,Y_Start,X_Pitch,Y_Pitch,Think,X_Division,Y_Division,Pick_UP,BybinlimitFT-Cateaory00-Enable,BybinlimitFT-Cateaory00-Precent,BybinlimitFT-Cateaory01-Enable,BybinlimitFT-Cateaory01-Precent,BybinlimitFT-Cateaory02-Enable,BybinlimitFT-Cateaory02-Precent,BybinlimitFT-Cateaory03-Enable,BybinlimitFT-Cateaory03-Precent,BybinlimitFT-Cateaory04-Enable,BybinlimitFT-Cateaory04-Precent,BybinlimitFT-Cateaory05-Enable,BybinlimitFT-Cateaory05-Precent,BybinlimitFT-Cateaory06-Enable,BybinlimitFT-Cateaory06-Precent,BybinlimitFT-Cateaory07-Enable,BybinlimitFT-Cateaory07-Precent,BybinlimitFT-Cateaory08-Enable,BybinlimitFT-Cateaory08-Precent,BybinlimitFT-Cateaory09-Enable,BybinlimitFT-Cateaory09-Precent,BybinlimitFT-Cateaory10-Enable,BybinlimitFT-Cateaory10-Precent,BybinlimitFT-Cateaory11-Enable,BybinlimitFT-Cateaory11-Precent,BybinlimitFT-Cateaory12-Enable,BybinlimitFT-Cateaory12-Precent,BybinlimitFT-Cateaory13-Enable,BybinlimitFT-Cateaory13-Precent,BybinlimitFT-Cateaory14-Enable,BybinlimitFT-Cateaory14-Precent,BybinlimitFT-Cateaory15-Enable,BybinlimitFT-Cateaory15-Precent,BybinlimitFT-CountIgnore,BybinSiteGapFT-Cateaory00-Enable,BybinSiteGapFT-Cateaory00-Percent,BybinSiteGapFT-Cateaory01-Enable,BybinSiteGapFT-Cateaory01-Percent,BybinSiteGapFT-Cateaory02-Enable,BybinSiteGapFT-Cateaory02-Percent,BybinSiteGapFT-Cateaory03-Enable,BybinSiteGapFT-Cateaory03-Percent,BybinSiteGapFT-Cateaory04-Enable,BybinSiteGapFT-Cateaory04-Percent,BybinSiteGapFT-Cateaory05-Enable,BybinSiteGapFT-Cateaory05-Percent,BybinSiteGapFT-Cateaory06-Enable,BybinSiteGapFT-Cateaory06-Percent,BybinSiteGapFT-Cateaory07-Enable,BybinSiteGapFT-Cateaory07-Percent,BybinSiteGapFT-Cateaory08-Enable,BybinSiteGapFT-Cateaory08-Percent,BybinSiteGapFT-Cateaory09-Enable,BybinSiteGapFT-Cateaory09-Percent,BybinSiteGapFT-Cateaory10-Enable,BybinSiteGapFT-Cateaory10-Percent,BybinSiteGapFT-Cateaory11-Enable,BybinSiteGapFT-Cateaory11-Percent,BybinSiteGapFT-Cateaory12-Enable,BybinSiteGapFT-Cateaory12-Percent,BybinSiteGapFT-Cateaory13-Enable,BybinSiteGapFT-Cateaory13-Percent,BybinSiteGapFT-Cateaory14-Enable,BybinSiteGapFT-Cateaory14-Percent,BybinSiteGapFT-Cateaory15-Enable,BybinSiteGapFT-Cateaory15-Percent,BybinSiteGapFT-CountIgnore,";
        List<String> list = Arrays.asList(shotNames.split(","));
        List<RecipePara> recipeParaList = getRecipePara(filePath,"HONTECHHT9045HW");
        for(String shotName:list){
            for(RecipePara r:recipeParaList){
                if(shotName.equals(r.getParaShotName())){
//                    System.out.println(r.getParaShotName()+"====="+r.getSetValue());
                    System.out.println(r.getSetValue());
                }

            }
        }




    }

}
