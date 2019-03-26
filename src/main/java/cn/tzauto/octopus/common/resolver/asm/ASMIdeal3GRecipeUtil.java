/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.resolver.asm;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.resolver.IOUtil;
import cn.tzauto.octopus.secsLayer.domain.ppBodyItem.asm.ASM3GPPbody;
import cn.tzauto.octopus.secsLayer.domain.ppBodyItem.asm.ASMPPbody;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
 * @author njtz
 */
public class ASMIdeal3GRecipeUtil {
    
    private static Logger logger = Logger.getLogger(ASMIdeal3GRecipeUtil.class.getName());
    public static List<ASM3GPPbody> unzipAndGet3GRcpPPBody(String srcFile) {
        File source = new File(srcFile);
        ZipInputStream zis = null;
        InputStream is = null;
        ZipFile zipFile = null;
        BufferedReader br = null;
        List<ASM3GPPbody> asmPPBodylist = new ArrayList();
        try {
            zis = new ZipInputStream(new FileInputStream(source));
            zipFile = new ZipFile(srcFile);
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String filename = entry.getName();
                    System.out.println(filename);
                    if (filename.contains(".csv")) {
                        is = zipFile.getInputStream(entry);
                        br = new BufferedReader(new InputStreamReader(is));
                        String tempString = null;
                        while ((tempString = br.readLine()) != null) {
                            ASM3GPPbody asmPPBody = new ASM3GPPbody();
                            String[] cfg = tempString.split(",");
                            //System.out.println(cfg.length);
                            if (cfg.length == 0) {
                                continue;
                            } else if (cfg.length == 1) {
                                asmPPBody.setStation(cfg[0].trim());
                                asmPPBody.setType("");
                                asmPPBody.setParameter("");
                                asmPPBody.setFileType("");
                                asmPPBody.setValue("");
                                asmPPBody.setUnit("");
                            } else if (cfg.length == 2) {
                                asmPPBody.setStation(cfg[0].trim());
                                asmPPBody.setType(cfg[1].trim());
                                asmPPBody.setParameter("");
                                asmPPBody.setFileType("");
                                asmPPBody.setValue("");
                                asmPPBody.setUnit("");
                            } else if (cfg.length == 3) {
                                asmPPBody.setStation(cfg[0].trim());
                                asmPPBody.setType(cfg[1].trim());
                                asmPPBody.setParameter(cfg[2].trim());
                                asmPPBody.setFileType("");
                                asmPPBody.setValue("");
                                asmPPBody.setUnit("");
                            } else if (cfg.length == 5) {
                                asmPPBody.setStation(cfg[0].trim());
                                asmPPBody.setType(cfg[1].trim());
                                asmPPBody.setParameter(cfg[2].trim());
                                asmPPBody.setFileType(cfg[3].trim());
                                asmPPBody.setValue(cfg[4].trim());
                                asmPPBody.setUnit("");
                            } else if (cfg.length == 6) {
                                asmPPBody.setStation(cfg[0].trim());
                                asmPPBody.setType(cfg[1].trim());
                                asmPPBody.setParameter(cfg[2].trim());
                                asmPPBody.setFileType(cfg[3].trim());
                                asmPPBody.setValue(cfg[4].trim());
                                asmPPBody.setUnit(cfg[5].trim());
                            }
                            asmPPBodylist.add(asmPPBody);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(is, zis, br);
        }
        return asmPPBodylist;
    }

    public static List<ASMPPbody> handlePPbody(List<ASM3GPPbody> aSM3GPPbodys) {
        List<ASMPPbody> aSMPPbodys = new ArrayList();
        String station = "";
        String type = "";
        for (int i = 1; i < aSM3GPPbodys.size(); i++) {
            ASM3GPPbody aSM3GPPbody = aSM3GPPbodys.get(i);
            if (aSM3GPPbody.getStation() != null && !"".equals(aSM3GPPbody.getStation())) {
                station = aSM3GPPbody.getStation();
            }
            if (aSM3GPPbody.getType() != null && !"".equals(aSM3GPPbody.getType())) {
                type = aSM3GPPbody.getType();
            }
            if ("".equals(aSM3GPPbody.getParameter())) {
                continue;
            }
            String paraName = station + "-" + type + "-" + aSM3GPPbody.getParameter();
            String setValue = aSM3GPPbody.getValue();
            ASMPPbody aSMPPbody = new ASMPPbody();
            aSMPPbody.setParaName(paraName);
            aSMPPbody.setParaValue(setValue);
            aSMPPbodys.add(aSMPPbody);
        }
        return aSMPPbodys;
    }

    public static List<RecipePara> transRcpParaFromDB(String filePath, String deviceType) {
        List<ASMPPbody> aSMPPbodys = handlePPbody(unzipAndGet3GRcpPPBody(filePath));
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchMonitorByMap(deviceType, "RecipePara", "Y");
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

    public static void main(String[] args) throws Exception {
        String fileStr = "C:/Users/wjy/Desktop/weiqiuyu/3G/1111 3g ppbody/PSSO DG 12"; //TSOT-23-XL(12R)(FC)-G600    TSOT-23-XL(16R)(FC)-G600   TSOT-23-XL(16R)-G600     PSSO DG 12
        List<RecipePara> recipeParas = transRcpParaFromDB(fileStr, "ASMIDEAL3G");
//        List<RecipePara> recipeParas = transRcpParaFromDB(asmRcpCfg(fileCsv),null);
        for (RecipePara recipePara : recipeParas) {
            System.out.println(recipePara.getParaName() + "------" + recipePara.getSetValue());
        }
        System.out.println(recipeParas.size());
//        for (RecipePara recipePara : recipeParas) {
//            System.out.println(recipePara.getParaName());
//        }
    }
}
