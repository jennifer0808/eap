package cn.tzauto.octopus.common.resolver.asm;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.ibatis.session.SqlSession;

public class ASMAD9212RcpTest {

    private static final Logger logger = Logger.getLogger(ASMAD9212RcpTest.class.getName());

    public static List<RecipePara> transferRcpFromDB(String filePath, String recipeName) {
        //将文件名称中的所有空格都替换为带有双引号的空格
        filePath = filePath.replaceAll(" ", "\" \"");
//        String command = "cmd /c start /B D:\\jcauto\\7z.exe x " + filePath + " -oD:\\RECIPE\\Z3\\FCDA\\ASMAD9212PLUSFC\\Engineer\\FC-019" + recipeName + " -aoa ";
        String command = "cmd /c start /B D:\\htauto\\7Z\\7-Zip\\7z.exe x " + filePath + " -oD:\\RECIPE\\RESOLVE\\" + recipeName + " -aoa ";
//        String command = "cmd /c start /B D:\\jcauto\\7z.exe x " + filePath + " -oD:\\RECIPE\\RESOLVE\\" + recipeName + " -aoa ";
        System.out.println("command===" + command);
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
//            Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        File file = new File("D:\\RECIPE\\RESOLVE\\" + recipeName);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("ASMAD9212PLUSFC", "RecipePara");
        List<RecipePara> recipeParas = new ArrayList<>();
        int temp = 0;
        if (file.exists()) {
            File[] files = file.listFiles();
            
            for (File f : files) {
                if (f.getName().equalsIgnoreCase("EJModule_ProcessParam.xml") || f.getName().equalsIgnoreCase("LBAModule_ProcessParam.xml") || f.getName().equalsIgnoreCase("LCAMModule_ProcessParam.xml")
                        || f.getName().equalsIgnoreCase("RBAModule_ProcessParam.xml") || f.getName().equalsIgnoreCase("RCAMModule_ProcessParam.xml") || f.getName().equalsIgnoreCase("WTModule_ProcessParam.xml")) {
                    try {
                        FileReader reader = new FileReader(f);
                        BufferedReader br = new BufferedReader(reader);
                        String line = "";
                        String paraName = "";
                        String paraValue = "";
                        String defaultValue = "";
                        try {
                            while ((line = br.readLine()) != null) {
                                RecipePara recipePara = new RecipePara();
                                System.out.println(line);
                                if (!line.contains("<Attribute")) {
                                    continue;
                                }
                                if (line.contains("<Attribute") && line.contains("</Attribute")) {
                                    paraName = line.split("name=")[1].split("array")[0].replace("\"", "").replace(" ", "");
                                    paraValue = line.split("\\>")[1].split("<")[0];
                                } else if (line.contains("<Attribute") && !line.contains("</Attribute")) {
                                    paraName = line.split("name=")[1].split("array")[0].replace("\"", "").replace(" ", "");
                                    paraValue = "";
                                }
                                for (int j = temp; j < recipeTemplates.size();) {
                                    if (paraName.equalsIgnoreCase(recipeTemplates.get(j).getParaName())) {
                                        recipePara.setParaName(recipeTemplates.get(j).getParaName());
                                        recipePara.setSetValue(paraValue);
                                        temp = j + 1;
                                        recipeParas.add(recipePara);
                                        break;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                        }
                    } catch (FileNotFoundException ex) {
                    }
                    continue ;
                }

            }

        }
        return recipeParas;
    }

    public static void main(String args[]) {
        List<RecipePara> recipeParas = transferRcpFromDB("E:\\YZ22_BD_160713B_01_V0.txt", "YZ22_BD_160713B_01_V0");
        System.out.println(recipeParas.size());
        for (int i = 0; i < recipeParas.size(); i++) {
            System.out.println(recipeParas.get(i).getParaName().toString() + "=" + recipeParas.get(i).getSetValue().toString());
        }
    }
}
