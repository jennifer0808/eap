/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.asm;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.secsLayer.domain.ppBodyItem.asm.ASMPPbody;
import cn.tzauto.octopus.secsLayer.resolver.IOUtil;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

/**
 *
 * @author wjy
 */
public class ASMRecipeUtil {
    private static Logger logger = Logger.getLogger(ASMRecipeUtil.class.getName());

    public static void main(String[] args) {
        String filePath = "C:/Users/wjy/Desktop/120T/120t new/TSOT-23-XL(16R)-G600.zip";
        //C:/Users/wjy/Desktop/80T/80t new/SOP-8L-G600FB-01test.zip    C:/Users/wjy/Desktop/120T/120t new/TSOT-23-XL(16R)-G600.zip

        List<RecipePara> recipeParas = transRcpParaFromDB(filePath, "ASM120T");
        for (RecipePara recipePara : recipeParas) {
            System.out.println(recipePara.getParaName() + "====" + recipePara.getSetValue());
        }
        System.out.println(recipeParas.size());
    }

    public static List<RecipePara> transRcpParaFromDB(String filePath, String deviceType) {
        List<ASMPPbody> aSMPPbodys = unzipPPbody(filePath);
        aSMPPbodys = handlePPbody(aSMPPbodys);
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
//        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            for (ASMPPbody aSMPPbody : aSMPPbodys) {
                if (recipeTemplate.getParaName().equals(aSMPPbody.getParaName())) {
                    RecipePara recipePara = new RecipePara();
                    recipePara.setParaCode(recipeTemplate.getParaCode());
                    recipePara.setParaName(aSMPPbody.getParaName());
                    recipePara.setSetValue(aSMPPbody.getParaValue());
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

    public static List handlePPbody(List<ASMPPbody> aSMPPbodys) {
        String paraType = "";
        //把参数的paraName加上type 
        for (ASMPPbody aSMPPbody : aSMPPbodys) {
            if (aSMPPbody.getParaType() != null) {
                paraType = aSMPPbody.getParaType();
                continue;
            }
            String paraName = paraType + "-" + aSMPPbody.getParaName();
            aSMPPbody.setParaName(paraName);
        }
        //去除没有paraName的参数
        for (int i = 0; i < aSMPPbodys.size(); i++) {
            String paraName = aSMPPbodys.get(i).getParaName();
            if (paraName == null || "".equals(paraName) || "NULL".equals(paraName)) {
                aSMPPbodys.remove(i);
                i--;
            }
        }
        return aSMPPbodys;
    }

    public static List unzipPPbody(String zipfilepath) {
        List<ASMPPbody> pPbodys = new ArrayList<>();
        File source = new File(zipfilepath);
        ZipInputStream zis = null;
        InputStream is = null;
        ZipFile zipFile = null;
        BufferedReader br = null;

        try {
            zis = new ZipInputStream(new FileInputStream(source));
            zipFile = new ZipFile(zipfilepath);
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String filename = entry.getName();
                    if (filename.contains(".TXT")) {
                        is = zipFile.getInputStream(entry);
                        br = new BufferedReader(new InputStreamReader(is));
                        String tempString = null;
                        while ((tempString = br.readLine()) != null) {
                            ASMPPbody aSMPPbody = new ASMPPbody();
                            if (tempString.length() != 0) {
                                if (tempString.contains("==")) {
                                    continue;
                                } else if (tempString.contains("=")) {
                                    String key = (tempString.substring(tempString.indexOf(""), tempString.indexOf("="))).trim();
                                    String value = (tempString.substring(tempString.indexOf("=") + 1)).trim();
                                    aSMPPbody.setParaName(key.replace(" ", ""));
                                    aSMPPbody.setParaValue(value);
                                    pPbodys.add(aSMPPbody);
                                } else if (tempString.contains("[") || tempString.contains("]") || (tempString.contains("Seg0")
                                        && tempString.contains("Seg1")) || tempString.contains("PROFILE SETUP")
                                        || tempString.contains("PRP File Name")) {
                                    aSMPPbody.setParaType(tempString.replace(" ", "").trim());
                                    pPbodys.add(aSMPPbody);
                                } else if (!tempString.contains("(") && tempString.matches(".*\\d+.*")) {
                                    String[] data = tempString.split("\\t+");
                                    ASMPPbody aSMPPbody1 = new ASMPPbody();
                                    aSMPPbody1.setParaName("Zone" + data[0] + "Posn");//PosnSpeedPressureTime
                                    aSMPPbody1.setParaValue(data[1]);
                                    aSMPPbody1.setParaUnit("0.1mm");
                                    pPbodys.add(aSMPPbody1);
                                    ASMPPbody aSMPPbody2 = new ASMPPbody();
                                    aSMPPbody2.setParaName("Zone" + data[0] + "Speed");
                                    aSMPPbody2.setParaValue(data[2]);
                                    aSMPPbody2.setParaUnit("0.1mm/s");
                                    pPbodys.add(aSMPPbody2);
                                    ASMPPbody aSMPPbody3 = new ASMPPbody();
                                    aSMPPbody3.setParaName("Zone" + data[0] + "Pressure");
                                    aSMPPbody3.setParaValue(data[3]);
                                    aSMPPbody3.setParaUnit("kg/cm2");
                                    pPbodys.add(aSMPPbody3);
                                    ASMPPbody aSMPPbody4 = new ASMPPbody();
                                    aSMPPbody4.setParaName("Zone" + data[0] + "Time");
                                    aSMPPbody4.setParaValue(data[4]);
                                    aSMPPbody4.setParaUnit("sec");
                                    pPbodys.add(aSMPPbody4);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(zis, is, br);
        }
        return pPbodys;
    }
}

