/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.asm;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * 解析ASMAD8312系列机台的recipe
 *
 * @author luosy
 */
public class AsmAD8312RecipeUtil {

    private static final Logger logger = Logger.getLogger(AsmAD8312RecipeUtil.class.getName());

    /**
     * 将recipe文件按照设备类型解析成参数
     *
     * @param filePath
     * @param deviceType
     * @return List<RecipePara>
     */
    public static List<RecipePara>  transferRcpFromDB(String filePath, String deviceType) {
        if (deviceType.contains("PLUS")) {
            return transfer8312PlusRecipeParaFromDB(filePath, deviceType);
        }

        ZipInputStream zis = null;
        List<RecipePara> recipeParas = new ArrayList<>();

        Map<Integer, String> hadReadLines = new HashMap<Integer, String>();
        try {
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));

            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                logger.info("ze:"+zis.getNextEntry()+";"+ze.getName()+"进来了");
                if (ze.getName().contains("McPara_Export.txt")) {
                    logger.info("ze.getName().contains(McPara_Export.txt)========================>"+ze.getName().contains("McPara_Export.txt"));
                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    RecipeService recipeService = new RecipeService(sqlSession);
                    List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");

                    int teamplateSize = recipeTemplates.size();
                    for (int i = 0; i < teamplateSize; i++) {
                        String paraName = recipeTemplates.get(i).getParaName();
                        String groupName = recipeTemplates.get(i).getGroupName();

                        RecipePara recipePara = new RecipePara();
                        String value = "";
                        int currentLineNO = 0;
                        InputStream is = zipFile.getInputStream(ze);
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = "";
                        String currentGroup = "";
                        while ((line = br.readLine()) != null) {

                            currentLineNO++;
                            logger.info("currentLineNO===================================================>"+line);

                            //排除“------”
                            if (line.contains("----") || line.equals("")) {
                                continue;
                            }

                            if (line.equals(groupName)) {
                                currentGroup = line;
                                continue;
                            }
                            paraName = paraName.replace(groupName + "-", "");
                            if (currentGroup.trim().equals(groupName) && line.contains(paraName)) {

                                logger.info("get paraName at line " + currentLineNO);

                                String[] lines = line.split("\\s");
                                int size = lines.length;
                                for (int j = 0; j < size; j++) {
                                    value = value + lines[j];
                                }
                                value = value.replace(":", "");
                                value = value.replace(paraName, "");
                                value = value.replace(paraName.replaceAll(" ", ""), "");
                                if (recipeTemplates.get(i).getParaUnit() != null) {
                                    value = value.replace(recipeTemplates.get(i).getParaUnit(), "");
                                }
                                if (paraName.trim().equals("GlobalAngle")) {
                                    String tempStr = "";
                                    for (char tempChar : value.toCharArray()) {
                                        if (Character.isDigit(tempChar) || '.' == tempChar || '-' == tempChar) {
                                            tempStr += tempChar;
                                        }
                                    }
                                    value = tempStr;

                                }
//                                if(paraName.trim().equals("GlobalAngle")) {
//                                    String var38 = "";
//                                    char[] arr$ = value.toCharArray();
//                                    int len$ = arr$.length;
//
//                                    for(int i$ = 0; i$ < len$; ++i$) {
//                                        char tempChar = arr$[i$];
//                                        if(Character.isDigit(tempChar) || 46 == tempChar || 45 == tempChar) {
//                                            var38 = var38 + tempChar;
//                                        }
//                                    }
//
//                                    value = var38;
//                                }

                                logger.info(groupName + paraName + "***" +  value.replace(",", "") + "****" + i + "******" + recipeTemplates.get(i).getParaCode() + "***total:" + recipeTemplates.size());
                                recipePara.setSetValue( value.replace(",", ""));
                                recipePara.setParaName(recipeTemplates.get(i).getParaName());
                                recipePara.setParaCode(recipeTemplates.get(i).getParaCode());
                                recipePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
                                recipePara.setParaShotName(recipeTemplates.get(i).getParaShotName());
                                recipeParas.add(recipePara);
                                break;
                            }
//                            else {
//                                i++;s
//                            }
                        }
                    }
                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Excetion occur,transfer fail.Exception info：" + ex.getMessage());
            recipeParas = null;
        } finally {
            try {
                zis.close();
            } catch (IOException ex) {
            }
        }
        return recipeParas;

    }



    public static List<RecipePara> transfer8312PlusRecipeParaFromDB(String filePath, String deviceType) {
        ZipInputStream zis = null;
        ArrayList recipeParas = new ArrayList();

        try {
            ZipFile ex = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            int temp = 0;

            while(true) {
                do {
                    if((ze = zis.getNextEntry()) == null) {
                        return recipeParas;
                    }
                } while(!ze.getName().contains("Parameter.txt"));

                SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                RecipeService recipeService = new RecipeService(sqlSession);
                List recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
                InputStream is = ex.getInputStream(ze);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = "";
                String paraName = "";
                String paraValue = "";

                while((line = br.readLine()) != null) {
                    RecipePara recipePara = new RecipePara();
                    if(line.contains(":")) {
                        paraName = line.split(":")[0].replace(" ", "");
                        paraValue = line.split(":")[1].replace(" ", "");
                        if(temp < recipeTemplates.size() && paraName.equals(((RecipeTemplate)recipeTemplates.get(temp)).getParaDesc())) {
                            System.out.println(paraName);
                            recipePara.setParaCode(((RecipeTemplate)recipeTemplates.get(temp)).getParaCode());
                            recipePara.setParaName(((RecipeTemplate)recipeTemplates.get(temp)).getParaName());
                            recipePara.setSetValue(paraValue);
                            recipeParas.add(recipePara);
                            ++temp;
                        }
                    }
                }
            }
        } catch (IOException var17) {
            return recipeParas;
        }
    }

    public static List<RecipePara> transferRcpFromDBForPlus(String filePath, String deviceType) {
        ZipInputStream zis = null;
        InputStream is = null;
        BufferedReader br = null;
        List<RecipePara> recipeParas = new ArrayList<>();
        List<String> recipeParaReadLines = new ArrayList<>();
        try {
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains("Parameter.txt")) {
                    is = zipFile.getInputStream(ze);
                    br = new BufferedReader(new InputStreamReader(is));
                    boolean startFlag = false;
                    String line = "";
                    String tempGroupName = "";
                    String groupName = "";
                    String partName = "";
                    while ((line = br.readLine()) != null) {
                        if (line.contains("==============")) {
                            startFlag = true;
                        }
                        if (!startFlag) {
                            continue;
                        }
                        if ("".equals(line) || " ".equals(line)) {
                            continue;
                        }
                        if (line.contains("*******")) {
                            partName = line.replace("*", "");
                            groupName = "";
                            continue;
                        }
                        //如果此行包含"-----",则将之前行当成groupName;
                        if (line.contains("-----")) {
                            recipeParaReadLines.remove(recipeParaReadLines.size() - 1);
                            groupName = tempGroupName.trim();
                            continue;
                        } else {
                            //记录每行的读取
                            tempGroupName = line.trim();
                        }
                        //真正解析数据
                        //将每行保存
//                        System.out.println(groupName + "-" + line.replace(":", "").replaceAll("\\s", "").trim());
                        if (!"".equals(groupName) && !groupName.contains("-")) {
                            groupName += "-";
                        }
//                        recipeParaReadLines.add(groupName + line.replace(":", "").replaceAll("\\s", "").trim());
                        recipeParaReadLines.add((partName + "-" + groupName + line.replace(":", "")).replaceAll("\\s", "").trim());
                    }
                }
            }
            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
            sqlSession.close();
//            for(RecipeTemplate recipeTemplatetemp : recipeTemplates){
//                System.out.println(recipeTemplatetemp);
//            }
            for (String readStr : recipeParaReadLines) {
                System.out.println(readStr);
                //System.out.println(readStr.split("\\-")[0]);
//                RecipeTemplate recipeTemplate = new RecipeTemplate();
                for (RecipeTemplate recipeTemplatetemp : recipeTemplates) {
                    //System.out.println(recipeTemplatetemp.getGroupName());
                    if (readStr.contains(recipeTemplatetemp.getGroupName() + "-" + recipeTemplatetemp.getParaName())) {
                        String s = "";
                        //System.out.println( s=recipeTemplatetemp.getParaName());
//                        recipeTemplate = recipeTemplatetemp;
                        //设置返回参数
                        RecipePara recipePara = new RecipePara();
                        String value = readStr.replace(recipeTemplatetemp.getGroupName() + "-" + recipeTemplatetemp.getParaName(), "");
//                        System.out.println(recipeTemplatetemp.getGroupName());
                        if (recipeTemplatetemp.getParaUnit() != null) {
                            value = value.replace(recipeTemplatetemp.getParaUnit(), "");
                        }
                        recipePara.setSetValue(value);
//                        System.out.println(value);
                        recipePara.setParaName(recipeTemplatetemp.getParaName());
                        recipePara.setParaCode(recipeTemplatetemp.getParaCode());
                        recipePara.setParaMeasure(recipeTemplatetemp.getParaUnit());
                        recipePara.setParaShotName(recipeTemplatetemp.getParaShotName());
                        recipeParas.add(recipePara);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Excetion occur,transfer fail.Exception info：" + ex.getMessage());
            recipeParas = null;
        } finally {
            try {
                if (br == null) {
                    logger.info("Recipe 内为未找到Parameter.txt文件,请确认设备软件已升级,并且已重新对recipe执行保存操作");

                }
                br.close();
                is.close();
                zis.close();
            } catch (IOException ex) {
            }
            return recipeParas;
        }

    }

    public static void main(String[] args) {

        List<RecipePara>   recipeParas=    transferRcpFromDB("D:\\htauto\\YXJ88QFN032015-D1-1B-8312_V0.zip", "ASMAD8312Z1");
        System.out.println(recipeParas.size());
        // Pattern pattern = Pattern.compile("-?[0-9]+\\\\.s?[0-9]*");
//         System.out.println(StringUtils.isNumeric("1"));;  

    }
}
