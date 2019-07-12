/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.aurigin;

import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;


public class BTURecipeUtil {

    private static Logger logger = Logger.getLogger(BTURecipeUtil.class);

    public static List transferBTURcp(String recipePath, String deviceType) {
        File file = new File(recipePath);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        if (file.exists()) {
            try {
                logger.info("==========进入解析！=====");
                FileReader reader = new FileReader(file);
                BufferedReader br = new BufferedReader(reader);
                String line = "";
//                String paraName = "";
                String paraValue = "";
                List<String> paraNameList = new ArrayList<>();
                for (int i = 0; i < recipeTemplates.size(); i++) {
                    paraNameList.add(recipeTemplates.get(i).getParaName());
                }
                try {
                    for (int j = 0; j < recipeTemplates.size(); j++) {
                        while ((line = br.readLine()) != null) {
//                            String line1 = new String(line.getBytes("GB2312"));
                            RecipePara recipePara = new RecipePara();
//                            System.out.println("1====="+line);
                            if (line == null) {
                                continue;
                            }
                            if (line.contains("[")) {
                                continue;
                            }
                            if (line.contains("=") && !line.contains(",")) {
                                String[] values = line.split("\\=");
                                paraValue = values[1].replace(" ", "");
                                
                                String value = trimnull(paraValue);
                                
                                if (!"".equals(value) && !value.isEmpty()) {
                                    recipePara.setParaCode(recipeTemplates.get(j).getParaCode());
                                    recipePara.setParaName(recipeTemplates.get(j).getParaName());
                                    recipePara.setSetValue(value);
                                    recipeParas.add(recipePara);
                                    break;
                                } else {
                                    recipePara.setParaCode(recipeTemplates.get(j).getParaCode());
                                    recipePara.setParaName(recipeTemplates.get(j).getParaName());
                                    recipePara.setSetValue("");
                                    recipeParas.add(recipePara);
                                    break;
                                }
                            } else if (line.contains("=") && line.contains(",")) {
                                String[] values = line.split("\\=");
                                paraValue = values[1];
                                String[] valueSingles = paraValue.split("\\,");
                                int i = 0;
                                for (; i < valueSingles.length; i++) {
                                    RecipePara recipeParaTemp = new RecipePara();
                                    String paraNameSingle = "";
                                    String paraValueSingle = "";
                                    recipeParaTemp.setParaCode(recipeTemplates.get(j + i).getParaCode());
                                    recipeParaTemp.setParaName(recipeTemplates.get(j + i).getParaName());
                                    paraValueSingle = trimnull(valueSingles[i]);
                                    recipeParaTemp.setSetValue(paraValueSingle);
                                    recipeParas.add(recipeParaTemp);
                                }
                                j = j + i - 1;
                                break;
                            }
                            continue;
                        }
                    }
                } catch (IOException ex) {
                    logger.info("IOEXECTPTION=================" + ex);
                    ex.printStackTrace();
                }
            } catch (FileNotFoundException ex) {
                logger.info("FileNotFoundException====================" + ex);
                ex.printStackTrace();
            }
        }
//        for (int i = 0; i < recipeParas.size(); i++) {
//            logger.info(recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString());
//        }
        logger.info("解析完成");
        logger.info("size=========" + recipeParas.size());
//        for (int i = 0; i < recipeParas.size(); i++) {
//            logger.info("+++++++" + recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString() + "+++++++");
//        }
        return recipeParas;
    }
public static String trimnull(String string) throws UnsupportedEncodingException  
    {  
        ArrayList<Byte> list = new ArrayList<>();  
        byte[] bytes = string.getBytes("UTF-8");  
        for(int i=0;bytes!=null&&i<bytes.length;i++){  
            if(0!=bytes[i]){  
                list.add(bytes[i]);  
            }  
        }  
        byte[] newbytes = new byte[list.size()];  
        for(int i = 0 ; i<list.size();i++){  
            newbytes[i]=(Byte) list.get(i);   
        }  
        String str = new String(newbytes,"UTF-8");  
        return str;  
    }  
    public static void main(String args[]) {
        String recipePath = "D:\\RECIPE\\LEADFRAMECUPILLAR.rcp";
        List<RecipePara> recipeParas = transferBTURcp(recipePath, "BTUPYRAM125NZ3");
        System.out.println(recipeParas.size());
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "======" + recipeParas.get(i).getSetValue().toString());
        }

    }
}
