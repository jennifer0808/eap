/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.asm;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 * 解析ASMAD8312系列机台的recipe
 *
 * @author luosy
 */
public class AsmAD8312RecipeUtil {

    private static final Logger logger = Logger.getLogger(AsmAD8312RecipeUtil.class);

    /**
     * 将recipe文件按照设备类型解析成参数
     *
     * @param filePath
     * @param deviceType
     * @return List<RecipePara>
     */
    public static List<RecipePara> transferRecipeParaFromDB(String filePath, String deviceType) {
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

    public static List<String> setGoldPara(List<RecipePara> goldRecipeParas, String uniqueFile, String deviceType) {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        Map<String, String> editMap = new HashMap<>();
        List<RecipePara> goldParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            if (recipeTemplate.getGoldPara() != null && recipeTemplate.getGoldPara().equals("Y")) {
                for (RecipePara goldRecipePara : goldRecipeParas) {
                    if (goldRecipePara.getParaCode().equals(recipeTemplate.getParaCode())) {
                        goldParas.add(goldRecipePara);
                        editMap.put(recipeTemplate.getParaName(), goldRecipePara.getSetValue());
                    }
                }
            }
        }
        List<String> uniqueRecipePara = getUniqueRecipePara(uniqueFile);
        List<String> resultList = new ArrayList<>();
        String groupName = "";
        String uniqueStringPara = "";
        String paraName = "";
        String[] uniqueStringParas;
        for (int i = 0; i < uniqueRecipePara.size(); i++) {
            uniqueStringPara = uniqueRecipePara.get(i);
            if (uniqueStringPara == null || uniqueStringPara.equals("")) {
                resultList.add(uniqueStringPara);
                continue;
            }
            try {
                if (uniqueRecipePara.get(i + 1).contains("----------")) {
                    groupName = uniqueStringPara;
                    resultList.add(uniqueStringPara);
                    continue;
                }
            } catch (Exception e) {
            }
            if (uniqueRecipePara.get(i).contains("----------")) {
                resultList.add(uniqueStringPara);
                continue;
            }
            if (uniqueRecipePara.get(i).contains(":")) {
                resultList.add(uniqueStringPara);
                continue;
            }
            uniqueStringParas = uniqueStringPara.split("\\s");
            paraName = groupName + "-" + uniqueStringParas[0];
            for (RecipePara goldPara : goldParas) {
                String key = goldPara.getParaName();
                String value = goldPara.getSetValue();
                String unit = goldPara.getParaMeasure();
                if (unit != null && !"".equals(unit)) {
                    uniqueStringPara = uniqueStringPara.replaceAll(unit, "");
                }

                if (key.equals(paraName.trim().replaceAll(" ", ""))) {
                    uniqueStringPara = setPara2String(groupName, value, uniqueStringPara);
                }
            }
            for (Map.Entry<String, String> entry : editMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    value = "";
                }
                if (key.equals(paraName.trim().replaceAll(" ", ""))) {
                    uniqueStringPara = setPara2String(groupName, value, uniqueStringPara);
                }
            }
            resultList.add(uniqueStringPara);
        }
        return resultList;
    }

    private static String setPara2String(String groupName, String goldValue, String uniqueParaStr) {
        String result = "";
        String uniqueParaStrTmp = uniqueParaStr.replaceAll(groupName, "");
        String[] cfg = uniqueParaStrTmp.split("\\s");
        String value = "";
        if (cfg.length != 2) {
            value = uniqueParaStr;
        } else {
            value = cfg[0] + " " + goldValue;
        }
        // System.out.println("---------------------------" + paraName);
        result = result + value;
        return result;
    }

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static List<String> getUniqueRecipePara(String filePath) {
        List list = new ArrayList();
        try {
            ZipInputStream zis = null;
            ZipFile zipFile = new ZipFile(filePath);
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry ze = null;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().contains("McPara_Export.txt")) {
                    InputStream is = zipFile.getInputStream(ze);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String cfgline = "";
                    while ((cfgline = br.readLine()) != null) {
                        list.add(cfgline);
                    }
                    br.close();
                    return list;
                }
            }
        } catch (IOException e) {

        }
        return list;
    }

    public static void main(String[] args) {
        // getUniqueRecipePara("D:\\RECIPE\\YYA13FCQ016016.txt");
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipePara> recipeParas = recipeService.searchRcpParaByRcpRowIdAndParaCode("1525bf38-f30d-4759-b9f7-6eee9aa73091", null);
        List<String> list = setGoldPara(recipeParas, "D:\\RECIPE\\YYA13FCQ016016.txt", "ASMAD8312FC");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(i + "||||" + list.get(i));
        }
//        writ(list, "D:\\RECIPE\\YYA13FCQ016016.txt");
//        writeZipFileTest("D:\\RECIPE\\YYA13FCQ016016.txt", "D:\\RECIPE\\TMP.txt");
        copyTest();
    }

    public static void writeZipFileTest(String srcPath, String... paths) {
        ZipOutputStream zos = null;
        FileOutputStream fos = null;
        BufferedOutputStream out = null;
        CheckedOutputStream csum = null;
        try {
            fos = new FileOutputStream(srcPath);
            csum = new CheckedOutputStream(fos, new Adler32());
            zos = new ZipOutputStream(csum);
            zos.setComment("hello");
            out = new BufferedOutputStream(zos);
            for (String str : paths) {
                BufferedReader in = new BufferedReader(new FileReader(str));
                zos.putNextEntry(new ZipEntry(str.substring(str
                        .lastIndexOf("\\") + 1)));
                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                out.flush();
                zos.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static final byte[] BUFFER = new byte[4096];

    public static void copy(InputStream input, OutputStream output) throws IOException {
        int bytesRead;
        while ((bytesRead = input.read(BUFFER)) != -1) {
            output.write(BUFFER, 0, bytesRead);
        }
    }

    public static void copyTest() {
        try {
            //"D:\\RECIPE\\YYA13FCQ016016.txt", "D:\\RECIPE\\TMP.txt"
            // read war.zip and write to append.zip
            ZipFile war = new ZipFile("D:\\RECIPE\\YYA13FCQ016016.txt");
            ZipOutputStream append = new ZipOutputStream(new FileOutputStream("D:\\RECIPE\\YYA13FCQ016016-copy.txt"));

            // first, copy contents from existing war
            Enumeration<? extends ZipEntry> entries = war.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                System.out.println("copy: " + e.getName());
                append.putNextEntry(e);
                if (!e.isDirectory()) {
                    copy(war.getInputStream(e), append);
                }
                append.closeEntry();
            }

            // now append some extra content
            ZipEntry e = new ZipEntry("D:\\RECIPE\\TMP.txt");
            System.out.println("append: " + e.getName());
            append.putNextEntry(e);
            append.write("42\n".getBytes());
            append.closeEntry();

            // close
            war.close();
            append.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<RecipePara> transfer8312PlusRecipeParaFromDB(String filePath, String deviceType) {
        ZipInputStream zis = null;
        List<RecipePara> recipeParas = new ArrayList<>();
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
                            for (int i = temp; i < recipeTemplates.size();) {
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
}
