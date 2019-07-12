/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver.icos;

import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.secsLayer.domain.ppBodyItem.icos.IcosPPBodyItem;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.secsLayer.resolver.IOUtil;
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
 */
public class TrRecipeUtil {

    private static List<IcosPPBodyItem> pPBodyItemList = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(TrRecipeUtil.class);

    /**
     * 解压文件
     *
     * @param srcfilepath 为源文件路径,saveexcelfilepath 为保存excel文件路径
     *
     */
    public static List<IcosPPBodyItem> unZipFiles(String srcfilepath) {
        pPBodyItemList.clear();
        File file = new File(srcfilepath);
        String parentFilePath = file.getParentFile().getAbsolutePath();
        List<String> list = readRCP(srcfilepath);
        String lastStr = srcfilepath.substring(srcfilepath.lastIndexOf("_"));
        for (String str : list) {
            String zipfilepath = parentFilePath + File.separator + str + lastStr;
            unzip(zipfilepath);
        }
        for (IcosPPBodyItem icosPPBodyItem : pPBodyItemList) {
            System.out.println(icosPPBodyItem.getParaName() + "----" + icosPPBodyItem.getParaValue());
        }
        return pPBodyItemList;
    }
    //根据路径名解压文件

    public static void unzip(String zipfilepath) {
        File source = new File(zipfilepath);//待解压的文件
        if (source.exists()) {
            ZipInputStream zis = null;
            InputStream is = null;
            ZipFile zipFile = null;
            BufferedReader br = null;
            try {
                zis = new ZipInputStream(new FileInputStream(source));
                zipFile = new ZipFile(zipfilepath);
                ZipEntry entry = null;
                while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                    List<String> list = new ArrayList();
                    String fileName = entry.getName();
//                    if (fileName.contains("OCV_")) {
//                        logger.info(entry.getName() + ".......该文件乱码,不用解析");
//                        continue;
//                    }
                    //只解析这三个文件来获取数据
                    if (fileName.contains(".han") || fileName.contains(".BGA") || fileName.contains(".CMO_1") || fileName.contains(".LLI")) {
                        is = zipFile.getInputStream(entry);
                        br = new BufferedReader(new InputStreamReader(is));
                        String lineString = null;
                        boolean tageJobFlag = false;
                        while ((lineString = br.readLine()) != null) {
                            if (fileName.contains(".CMO_1")) {
                                if (lineString.contains("Marking 2")) {
                                    break;
                                }
                                list.add(lineString);
                            } else if (fileName.contains(".han")) {
                                if (lineString.contains("TapeJob")) {
                                    tageJobFlag = true;
                                }
                                if (tageJobFlag && lineString.contains("Segment")) {
                                    list.add(lineString);
                                }
                            } else if (fileName.contains(".BGA")) {
                                list.add(lineString);
                            } else if (fileName.contains(".LLI")) {
                                list.add(lineString);
                            }
                        }
                        readLine(list, fileName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                IOUtil.closeQuietly(zis, is, br);
            }
        }
    }

    public static void readLine(List<String> list, String fileName) {
        //一个一个文件一行一行分割
        if (list != null && !list.isEmpty()) {
            for (int j = 0; j < list.size(); j++) {
                if (fileName.contains(".BGA") || fileName.contains(".LLI")) {
                    String[] ssStrings = list.get(j).split(":");
                    if (ssStrings.length == 2) {
                        String key = ssStrings[0].trim();
                        String value = ssStrings[1].trim();
                        if (key.length() == 2 && value.contains("ON")) {
                            String[] values = value.split("\\s");
                            int i = 1;
                            for (String str : values) {
                                if (str.matches("([+-]?)\\d+\\.?\\d*")) {
                                    IcosPPBodyItem icosItem = new IcosPPBodyItem();
                                    icosItem.setParaName(key + i);
                                    icosItem.setParaValue(str);
                                    pPBodyItemList.add(icosItem);
                                    i++;
                                }
                            }

                        }
                    }
                } else if (fileName.contains(".CMO_1")) {
                    String[] ssStrings = list.get(j).split(":");
                    if (ssStrings.length == 2) {
                        IcosPPBodyItem icosItem = new IcosPPBodyItem();
                        String key = ssStrings[0].trim();
                        String value = ssStrings[1].trim();
                        if (key.contains("accept char x")) {
                            icosItem.setParaName("Marking_XO");
                            icosItem.setParaValue(value);
                        } else if (key.contains("accept char y")) {
                            icosItem.setParaName("Marking_YO");
                            icosItem.setParaValue(value);
                        } else if (key.contains("accept char underprint")) {
                            icosItem.setParaName("Marking_UP");
                            icosItem.setParaValue(value);
                        } else if (key.contains("accept char overprint")) {
                            icosItem.setParaName("Marking_OP");
                            icosItem.setParaValue(value);
                        } else if (key.contains("accept char blob size")) {
                            icosItem.setParaName("Marking_BL");
                            icosItem.setParaValue(value);
                        } else {
                            continue;
                        }
                        pPBodyItemList.add(icosItem);
                    }
                } else if (fileName.contains(".han")) {
                    String[] ssStrings = list.get(j).split("=");
                    if (ssStrings.length == 2) {
                        IcosPPBodyItem icosItem = new IcosPPBodyItem();
                        String value = ssStrings[1].trim();
                        icosItem.setParaName("PartCounters");
                        icosItem.setParaValue(value);
                        pPBodyItemList.add(icosItem);
                    }
                }
            }
        }
    }
    public static List readRCP(String filePath) {

        ZipInputStream zis = null;
        BufferedReader br = null;
        List<String> list = new ArrayList();
        InputStream is = null;
        try {
            ZipFile zipFile = new ZipFile(filePath);
//            File file = new File(filePath);
//            rcpName = file.getName();
            zis = new ZipInputStream(new FileInputStream(filePath));
            ZipEntry entry = null;
//            list.add(rcpName);
            while ((entry = zis.getNextEntry()) != null && !entry.isDirectory()) {
                is = zipFile.getInputStream(entry);
                br = new BufferedReader(new InputStreamReader(is));
                String lineString = null;
                while ((lineString = br.readLine()) != null) {
                    if (lineString.contains("component") || lineString.contains("handler") || lineString.contains("AGD")) {
                        String[] str = lineString.split("=");
                        list.add("@" + str[0].replace("\"", "").trim() + "@" + str[1].replace("\"", "").trim());
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            IOUtil.closeQuietly(zis, is, br);
            return list;
        }
    }

    public static List<RecipePara> transferParaFromDB(String deviceType, String recipePath) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> rcpTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<IcosPPBodyItem> icosPPBodyItems = unZipFiles(recipePath);
        List<RecipePara> recipeParas = new ArrayList();
        for (RecipeTemplate recipeTemplate : rcpTemplates) {
            for (IcosPPBodyItem item : icosPPBodyItems) {
                if (item.getParaName().equalsIgnoreCase(recipeTemplate.getParaName())) {
                    RecipePara recipePara = new RecipePara();
                    recipePara.setParaCode(recipeTemplate.getParaCode());
                    recipePara.setParaName(recipeTemplate.getParaName());
                    recipePara.setParaShotName(recipeTemplate.getParaShotName());
                    recipePara.setSetValue(item.getParaValue());
//                    recipePara.setMinValue(recipeTemplate.getMinValue());
//                    recipePara.setMaxValue(recipeTemplate.getMaxValue());
                    recipePara.setParaMeasure(recipeTemplate.getParaUnit());
                    recipeParas.add(recipePara);
                    break;
                }
            }
        }
        return recipeParas;
    }

    //获取文件同级目录的所有文件名
    public static List<String> getFileFromDir(String localFileName) {
        List<String> fileList = new ArrayList();
        File localFile = new File(localFileName);
        String parentFilePath = localFile.getParent();
        File parentFile = new File(parentFilePath);
        if (parentFile.isDirectory()) {
            File[] files = parentFile.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String srcfilepath = "D:\\RECIPE\\T6\\TR\\ICOST640\\Engineer\\E4400-0007\\@recipe@AKJ-T-QFN-5X5-40-3000\\@recipe@AKJ-T-QFN-5X5-40-3000_V0.txt";//源文件路径     F:\\T640Recipe\\@recipe@ABG-T-BGA-4X4-47
////        String srcfilepath = "E:\\T640\\@recipe@ABG-T-BGA-4X4-47";
//        List<String> list = readRCP(srcfilepath);
//        for (String str : list) {
//            System.out.println(str);

//        }
//        List<IcosPPBodyItem> list = unZipFiles(srcfilepath);
//        for (IcosPPBodyItem icosPPBodyItem : list) {
//            System.out.println(icosPPBodyItem);
//        }
        List<RecipePara> recipeParas = transferParaFromDB("ICOST640", srcfilepath);
        System.out.println(recipeParas.size());
        for (RecipePara recipePara : recipeParas) {
            System.out.println(recipePara.getParaName() + "---" + recipePara.getSetValue());
        }
//        System.out.println(list.size() + list2.size() + list3.size());
//        unZipFiles(srcfilepath);
//        System.out.println(listRecipe.size() + "--" + listHandler.size() + "--" + listComponent.size());

//        List<String> list = getFileFromDir("F:\\T640Recipe\\");
//        for (String str : list) {
//            System.out.println(str);
//        }
    }
}
