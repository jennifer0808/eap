package cn.tzauto.octopus.secsLayer.resolver.sti;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class TR48MK5RecipeUtil {

    private static final Logger logger = Logger.getLogger(TR48MK5RecipeUtil.class);

    public static List<RecipePara> transferRcpFromDB(String filePath, String recipeName, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        ZipInputStream zis = null;
        BufferedReader br = null;
        List<RecipePara> recipeParas = new ArrayList<>();
        try {
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains(recipeName + "_TAPE.ini")) {
                    InputStream is = zipFile.getInputStream(ze);
                    br = new BufferedReader(new InputStreamReader(is));
                    String line = "";
                    String paraName = "";
                    String paraValue = "";
                    String groupName = "";
                    while ((line = br.readLine()) != null) {
                        RecipePara recipePara = new RecipePara();
                        if (line.contains("[")&&!line.contains("=")) {
                            groupName = line;
                            continue;
                        }
                        if (line.contains("=")) {
                            String[] paras = line.split("=");
                            if (paras.length > 1) {
                                paraName = paras[0];
                                paraValue = paras[1];
                            } else {
                                paraName = paras[0];
                                paraValue = "";
                            }
                            paraName = paraName+groupName;
                            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                                if (paraName.equals(recipeTemplate.getParaName())) {
                                    recipePara.setParaName(paraName);
                                    System.out.println(paraName);
                                    recipePara.setParaCode(recipeTemplate.getParaCode());
                                    recipePara.setSetValue(paraValue);
                                    recipeParas.add(recipePara);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                zis.close();
                br.close();
            } catch (IOException ex) {
                logger.error("ex："+ex);
            }
        }
        return recipeParas;
    }

    public static void main(String args[]) {
        String path = "C:\\Users\\SunTao\\Desktop\\长电\\STI-TR48MK5\\AGS-T-LGA-5X5-3000_V1.txt";
        List<RecipePara> recipeParas = transferRcpFromDB(path, "AGS-T-LGA-5X5-3000","STI-TR48MK5");
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "=" + recipeParas.get(i).getSetValue().toString()+":"+recipeParas.get(i).getParaCode());
//            System.out.println(recipeParas.get(i).getParaName().toString());
        }
        System.out.println(recipeParas.size());
    }
}
