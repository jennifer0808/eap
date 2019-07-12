/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.asm;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.apache.ibatis.session.SqlSession;


public class ASMAD8312PlusRcpUtil {

    public static List<RecipePara> transferRecipeParaFromDB(String filePath, String deviceType) {
        ZipInputStream zis = null;
        List<RecipePara> recipeParas = new ArrayList<>();
        Map<Integer, String> hadReadLines = new HashMap<>();
        try {
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            int temp = 0;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains("Parameter.txt")) {
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    RecipeService recipeService = new RecipeService(sqlSession);
                    List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                    InputStream is = zipFile.getInputStream(ze);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line = "";
                    String paraName = "";
                    String paraValue = "";
                    while ((line = br.readLine()) != null) {
                        RecipePara recipePara = new RecipePara();
                        if (!line.contains(":")) {
                            continue;
                        } else {
                            paraName = line.split(":")[0].replace(" ", "");
                            paraValue = line.split(":")[1].replace(" ", "");
                            for ( int i = temp; i < recipeTemplates.size();) {
                                if (paraName.equals(recipeTemplates.get(i).getParaDesc())) {
                                    System.out.println(paraName);
                                    recipePara.setParaCode(recipeTemplates.get(i).getParaCode());
                                    recipePara.setParaName(recipeTemplates.get(i).getParaName());
                                    recipePara.setSetValue(paraValue);
                                    recipeParas.add(recipePara);
                                    temp = i + 1;
                                    break;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
        }
        return recipeParas;
    }
    public static void main(String [] args){
        List<RecipePara> list = transferRecipeParaFromDB("E:\\8312Plus\\YBB59QFN012001-D1-1B-8312P_V0.txt","ASMAD8312Plus");
        System.out.println(list.size());
        for(int i = 0 ;i < list.size();i++){
            System.out.println(list.get(i).getParaName()+"="+list.get(i).getSetValue());
        }
    }
}