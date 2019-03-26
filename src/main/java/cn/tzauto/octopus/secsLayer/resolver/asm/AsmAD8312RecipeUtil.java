/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.asm;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
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
import org.apache.log4j.Logger;

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
    public static List<RecipePara> transferRcpFromDB(String filePath, String deviceType) {
        ZipInputStream zis = null;
        List<RecipePara> recipeParas = new ArrayList<>();

        Map<Integer, String> hadReadLines = new HashMap<Integer, String>();
        try {
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains("McPara_Export.txt")) {

                    SqlSession sqlSession = MybatisSqlSession.getSqlSession();
                    RecipeService recipeService = new RecipeService(sqlSession);
                    List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");

                    int teamplateSize = recipeTemplates.size();
                    for (int i = 0; i < teamplateSize; i++) {
                        String paraName = recipeTemplates.get(i).getParaName();
                        String groupName = recipeTemplates.get(i).getGroupName();
//                        logger.info("Template total size:" + teamplateSize + ";Current index:" + i + ";Current para:" + paraName);
                        RecipePara recipePara = new RecipePara();
                        String value = "";
                        int currentLineNO = 0;
                        InputStream is = zipFile.getInputStream(ze);
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = "";
                        String currentGroup = "";
                        while ((line = br.readLine()) != null) {
                            currentLineNO++;
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
//                                logger.info("get paraName at line " + currentLineNO);
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
//                                logger.info(groupName + paraName + "***" + value + "****" + i + "******" + recipeTemplates.get(i).getParaCode() + "***total:" + recipeTemplates.size());
                                recipePara.setSetValue(value);
                                recipePara.setParaName(recipeTemplates.get(i).getParaName());
                                recipePara.setParaCode(recipeTemplates.get(i).getParaCode());
                                recipePara.setParaMeasure(recipeTemplates.get(i).getParaUnit());
                                recipePara.setParaShotName(recipeTemplates.get(i).getParaShotName());
                                recipeParas.add(recipePara);
                                break;
                            } else {
//                                i++;s
                            }
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

    public static void main(String[] args) {

        transferRcpFromDB("D:\\htauto\\YYA13FCQ016012_V0.txt", "ASMAD8312FC");
// Pattern pattern = Pattern.compile("-?[0-9]+\\\\.s?[0-9]*");  
//         System.out.println(StringUtils.isNumeric("1"));;  

    }
}
