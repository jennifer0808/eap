/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.laser;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.resolver.IOUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author njtz
 */
public class Asm1205RecipeUtil {

    private static Logger logger = Logger.getLogger(Asm1205RecipeUtil.class.getName());

    // 解析recipe文件
    public static List<RecipePara> transferRCP(String recipeName, String recipePath, String deviceType) {
//        List<String> recipeParaListFromFile = unZipRecipeFileAndAnalysis(recipeName, recipePath);
//        Map paraMap = transferFromList(recipeParaListFromFile);
//        List<RecipePara> recipeParaList = transferFromDB(paraMap, deviceType);
        return null;
    }

    // 从recipe文件（zip）中获取参数文件并解析
    public static List<String> unZipRecipeFileAndAnalysis(String zipfilepath) {
        List<String> recipeParaList = new ArrayList<>();
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
                if (entry.isDirectory() || entry.getName().endsWith(".ini") || entry.getName().endsWith(".svg")
                        || entry.getName().endsWith(".bmp")) {
                    continue;
                }
                String filename = entry.getName();
                System.out.println(filename);
//                is = zipFile.getInputStream(entry);
//                br = new BufferedReader(new InputStreamReader(is));
//                recipeParaList = getAllValue(br);
            }
        } catch (IOException e) {
            logger.error("Exception:", e);
        } finally {
            IOUtil.closeQuietly(zis, is, br);
        }
        return recipeParaList;
    }

    private static List<String> getAllValue(BufferedReader bufferedReader) {
        List<String> list = new LinkedList();
        try {
            String cfgline = null;
            // 跳过第一行
            bufferedReader.readLine();
            while ((cfgline = bufferedReader.readLine()) != null) {
                if (cfgline.trim().equals("L1") || cfgline.trim().equals("L2") || cfgline.trim().equals("L4") || cfgline.trim().equals("[") || cfgline.trim().equals("]")) {
                    continue;
                }
                String value = "";
                String[] cfg = cfgline.split(":");
                if (cfg.length > 1) {
                    value = cfg[1];
                }
                value = value.replaceAll(" ", "");
                list.add(value);
                if ("49600".equals(value)) {
                    list.add("");
                }
            }
            bufferedReader.close();
            return list;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private static Map transferFromList(List<String> valueList) {
        LinkedHashMap<String, String> paraMap = new LinkedHashMap<String, String>();
        if (valueList != null && !valueList.isEmpty()) {
            for (int i = 0; i < valueList.size(); i++) {
                String key = valueList.get(i++);
                String value = valueList.get(i);
                if (value.contains(",")) {
                    String[] values = value.split(",");
                    for (int j = 0; j < values.length; j++) {
                        DecimalFormat df = new DecimalFormat("000");
                        String str = df.format(j);
                        paraMap.put(key + str, values[j]);
//                        System.out.println(key + str + "=" + values[j]);
                    }
                } else {
                    if ("0".equals(key) || "1".equals(key) || "2".equals(key) || "3".equals(key) || "4".equals(key)
                            || "5".equals(key) || "6".equals(key) || "7".equals(key) || "8".equals(key)) {
                        paraMap.put(key, value);
//                        System.out.println(key + "=" + value);
                    } else {
                        if (!"".equals(key)) {
                            paraMap.put(key + "000", value);
//                            System.out.println(key + "000=" + value);
                        }
                    }
                }
            }
        }
        return paraMap;
    }

    private static List<RecipePara> transferFromDB(Map paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        if (recipeTemplates != null && !recipeTemplates.isEmpty()) {
            for (RecipeTemplate recipeTemplate : recipeTemplates) {
                RecipePara recipePara = new RecipePara();
                recipePara.setParaCode(recipeTemplate.getParaCode());
                recipePara.setParaName(recipeTemplate.getParaName());
                recipePara.setParaShotName(recipeTemplate.getParaShotName());
                if (null != paraMap.get(recipeTemplate.getDeviceVariableId())) {
                    recipePara.setSetValue(paraMap.get(recipeTemplate.getDeviceVariableId()).toString());
                }
//                else {
//                    System.out.println(recipeTemplate.getDeviceVariableId());
//                    recipePara.setSetValue(paraMap.get(recipeTemplate.getDeviceVariableId()).toString());
//                }
                recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                recipePara.setParaShotName(recipeTemplate.getParaShotName());
                recipeParaList.add(recipePara);
            }
        }
        return recipeParaList;
    }


    public static void main(String[] args) {
//        List<RecipePara> recipeParaList = transferRCP("MB4", "D:\\MB4_V0.txt", "ACCRETECHAD3000Z1");
        List<String> recipeParaListFromFile = unZipRecipeFileAndAnalysis("D:\\Production@Main@XR819-AW1698-GD06-12_V1.txt");
//        Map paraMap = transferFromList(recipeParaListFromFile);
//        Map nameMap = transferFromFile("D:\\AD3000T_recipe_para_name.txt");
//        boolean flag = saveRecipeTemplateList(paraMap, nameMap);
//        if (!flag) {
//            System.out.println("保存失败");
//        }
//        List<RecipePara> recipeParaList = transferFromDB(paraMap, "AD3000T");
//        for (int i = 0; i < recipeParaList.size(); i++) {
//            System.out.println(recipeParaList.get(i).getParaCode() + "=====" + recipeParaList.get(i).getParaName() + "=====" + recipeParaList.get(i).getSetValue());
//        }
    }

    public static boolean saveRecipeTemplateList(Map paraMap, Map nameMap) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        try {
            String deviceTypeId = "4AFD9962300901B4E053AC11AD667829";
            String deviceTypeCode = "AD3000T";
            String deviceTypeName = "AD3000T";
            RecipeService recipeService = new RecipeService(sqlSession);
            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceTypeCode, "RecipePara");
//        sqlSession.close();
            List<String> paraNameList = new ArrayList<>();
            for (int i = 0; i < recipeTemplates.size(); i++) {
                paraNameList.add(recipeTemplates.get(i).getParaName());
            }
            // 需要保存到数据库的recipe参数（数据库中没有的部分）
            List<RecipeTemplate> recipeParaList2Save = new ArrayList<>();
            Set<Map.Entry<String, String>> entry = paraMap.entrySet();
            int k = 1;
            // AD3000T供应商提供的文档中缺失的参数map
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : entry) {
                if (!paraNameList.contains(e.getKey())) {
                    RecipeTemplate recipeTemplate = new RecipeTemplate();
                    recipeTemplate.setDeviceTypeId(deviceTypeId);
                    recipeTemplate.setDeviceTypeCode(deviceTypeCode);
                    recipeTemplate.setDeviceTypeName(deviceTypeName);
                    recipeTemplate.setParaCode(String.valueOf(k));
                    recipeTemplate.setDeviceVariableId(e.getKey());
//                    String value = e.getValue();
//                    if (null != value && !"".equals(value) && value.length() > 99) {
//                        value = value.substring(0, 100);
//                    }
//                    recipeTemplate.setSetValue(value);
                    Object paraNameValue = nameMap.get(e.getKey());
                    if (null != paraNameValue) {
                        String paraName = paraNameValue.toString();
                        recipeTemplate.setParaName(paraName);
                    }
//                    else {
//                        if (e.getKey().length() < 5) {
//                            map.put(e.getKey(), "");
//                        } else {
//                            map.put(e.getKey().substring(0, 5), "");
//                        }
//                    }
                    recipeTemplate.setDeviceVariableType("RecipePara");
                    recipeTemplate.setDelFlag("0");
                    recipeTemplate.setCreateBy("1");
                    recipeTemplate.setCreateDate(new Date());
                    recipeTemplate.setUpdateBy("1");
                    recipeTemplate.setUpdateDate(new Date());
                    if (null != paraNameValue) {
                        recipeParaList2Save.add(recipeTemplate);
                        k++;
                    }
                }
            }
//            for (String s : map.keySet()) {
//                System.out.println(s);
//            }

            if (recipeParaList2Save != null && !recipeParaList2Save.isEmpty()) {
//                recipeService.deleteRecipeTemplateByDeviceTypeCodeBatch(recipeParaList2Save);
//                for (RecipeTemplate recipeTemplate : recipeParaList2Save) {
//                    recipeTemplate.setDeviceTypeId(deviceTypeId);
//                }
                List<RecipeTemplate> recipeTemplatesTmp = new ArrayList<>();
                for (int i = 0; i < recipeParaList2Save.size(); i++) {
                    recipeTemplatesTmp.add(recipeParaList2Save.get(i));
                    if (recipeTemplatesTmp.size() % 500 == 0) {
                        recipeService.saveRcpTemplateBatch(recipeTemplatesTmp);
                        recipeTemplatesTmp.clear();
                        sqlSession.commit();
                    }
                }
                recipeService.saveRcpTemplateBatch(recipeTemplatesTmp);
                sqlSession.commit();
            }
        } catch (Exception e) {
            System.out.println("something error while save template recipe!!");
            sqlSession.rollback();
        }
        return true;
    }

    //filePath 是PPBODY原文件的存储路径(非文件夹)
    public static Map transferFromFile(String filePath) {
        Map map = new LinkedHashMap();
        BufferedReader br = null;
        try {
            String cfgline = null;
            String key = "";
            String value = "";
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "GBK"));
            while ((cfgline = br.readLine()) != null) {
                if (cfgline.contains("=")) {
                    String[] cfg = cfgline.split("=");
                    //因为文件的第一行有乱码，如果读取的是第一行，要把乱码去除
                    key = cfg[0];
                    if (cfg.length < 2) {
                        value = "";
                    } else {
                        value = cfg[1];
                    }
                    map.put(key.trim(), value.trim());
                }
            }
            br.close();
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
