/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.dh.bg;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.isecsLayer.resolver.IOUtil;
import cn.tzauto.octopus.secsLayer.domain.ppBodyItem.dh.bg.OCRPPBody;
import org.apache.ibatis.session.SqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class OCRRecipeUtil {

    public static void main(String[] args) {
        String filePath = "C://ocr//1-12-RDA8809J-0.ini";
        List<RecipePara> recipeParas = transRcpParaFromDB(filePath, "OCRDH-OBCP5000");
        for (RecipePara recipePara : recipeParas) {
            System.out.println(recipePara.getParaName() + "---" + recipePara.getSetValue());
        }
        System.out.println(recipeParas.size());

//        List<OCRPPBody> oCRPPBodys = unOcrRecipe(filePath);
//        oCRPPBodys = handleOcrBody(oCRPPBodys);
//        for (OCRPPBody oCRPPBody : oCRPPBodys) {
//            System.out.println(oCRPPBody.getKey());
//        }
//        System.out.println(oCRPPBodys.size());
    }

    public static List<RecipePara> transRcpParaFromDB(String filePath, String deviceType) {
        List<OCRPPBody> oCRPPBodys = unOcrRecipe(filePath);
        oCRPPBodys = handleOcrBody(oCRPPBodys);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (OCRPPBody oCRPPBody : oCRPPBodys) {
                if (recipeTemplate.getParaName().equals(oCRPPBody.getKey())) {
                    RecipePara recipePara = new RecipePara();
                    recipePara.setParaCode(recipeTemplate.getParaCode());
                    recipePara.setParaName(recipeTemplate.getParaName());
                    recipePara.setSetValue(oCRPPBody.getValue());
                    recipePara.setMinValue(recipeTemplate.getMinValue());
                    recipePara.setMaxValue(recipeTemplate.getMaxValue());
                    recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                    recipePara.setParaShotName(recipeTemplate.getParaShotName());
                    recipeParas.add(recipePara);
                    break;
                }
            }
        }
        return recipeParas;
    }

    public static List<OCRPPBody> unOcrRecipe(String filePath) {
        List<OCRPPBody> list = new ArrayList<>();
        File source = new File(filePath);
        String line = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        try {
            isr = new InputStreamReader(new FileInputStream(source));
            reader = new BufferedReader(isr);
            while ((line = reader.readLine()) != null) {
                OCRPPBody body = new OCRPPBody();
                if (line.contains("[")) {
                    String type = line.substring(line.indexOf("[") + 1, line.lastIndexOf("]")) + "-";
                    body.setType(type);
//				System.out.println(type);
                } else {
                    String key = line.substring(0, line.lastIndexOf("="));
                    String value = line.substring(line.indexOf("=") + 1, line.lastIndexOf(""));
                    body.setKey(key);
                    body.setValue(value);
//				System.out.println(key+":"+value);
                }
                list.add(body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(isr, reader);
        }

        return list;
    }

    public static List<OCRPPBody> handleOcrBody(List<OCRPPBody> oCRPPBodys) {
        String type = "";
        //把参数的key加上type
        for (OCRPPBody oCRPPBody : oCRPPBodys) {
            if ((oCRPPBody.getType()) != null) {
                type = oCRPPBody.getType();
                continue;
            }
            String key = type + oCRPPBody.getKey();
            oCRPPBody.setKey(key);
        }
        //去除没有Count2、3、4的参数
        for (int i = 0; i < oCRPPBodys.size(); i++) {
            String key = oCRPPBodys.get(i).getKey();
            if (key == null || "".equals(key)) {
                oCRPPBodys.remove(i);
                i--;
            }
        }
        return oCRPPBodys;
    }
}
