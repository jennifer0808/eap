package cn.tzauto.octopus.secsLayer.resolver.ismeca;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import cn.tzauto.octopus.common.util.tool.FileUtil;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class NY20RecipeUtil {

    private static final Logger logger = Logger.getLogger(NY20RecipeUtil.class.getName());

    public static List<RecipePara> transferRcpFromDB(String filePath, String recipeName) {
        //将文件名称中的所有空格都替换为带有双引号的空格
        filePath = filePath.replaceAll(" ", "\" \"");
        String command = "cmd /c start /B D:\\jcauto\\7z.exe x " + filePath + " -oD:\\RECIPE\\RESOLVE\\" + recipeName + " -aoa ";
        System.out.println(command);
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
//            Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        File file = new File("D:\\RECIPE\\RESOLVE\\" + recipeName);
        List<RecipePara> recipeParas = new ArrayList<>();
        if (file.exists()) {
            File[] files = file.listFiles();
            for (File f : files) {
                if (f.getName().contains("REC")) {
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
                                if (line.contains("//")) {
                                    continue;
                                }
                                if (line.contains(",")) {
                                    String[] paras = line.split("\\,");
                                    if (paras.length == 6) {
                                        paraName = paras[0] + "_" + paras[1] + "_" + paras[2];
                                        paraValue = paras[4];
                                        defaultValue = paras[5];
                                        recipePara.setParaName(paraName);
                                        recipePara.setSetValue(paraValue);
                                        recipePara.setDefValue(defaultValue);
                                        recipeParas.add(recipePara);
                                    }
                                    if (paras.length == 5) {
                                        paraName = paras[0] + "_" + paras[1] + "_" + paras[2];
                                        paraValue = paras[4];
                                        recipePara.setParaName(paraName);
                                        recipePara.setSetValue(paraValue);
                                        recipeParas.add(recipePara);
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                    }

                };
            }
        }
        return recipeParas;
    }

    public static Map<String, Ny20RecipeItem> getItemParaMap(Element root) {

        Map<String, Ny20RecipeItem> itemMap = new HashMap<>();
        itemMap.putAll(getItemLastParaMap(root));
        /*
           XML路径：BasicItem为根节点
        BasicItem/Children/BasicItem(id=i8)/Children(此时判断是否为空)/BasicItem(i9)/Measurands/List（判断是否为空）/KeyValuePairOfMeasurandKeyMeasurandYxVEEYjo/value/
         */

        Element childrenOfRoot2 = root.element("Children");
        List<Element> basicItemOfRoot2s = childrenOfRoot2.elements("BasicItem");
        if (basicItemOfRoot2s.size() > 0) {
            for (Element basicItemOfRoot2 : basicItemOfRoot2s) {
                Element cOfR2 = basicItemOfRoot2.element("Children");
                List<Element> bOfR2s = cOfR2.elements("BasicItem");
                if (bOfR2s.size() > 0) {
                    for (Element bOfR2 : bOfR2s) {
                        itemMap.putAll(getItemLastParaMap(bOfR2));
                    }
                }

            }

        }

        return itemMap;
    }

    public static Map<String, Ny20RecipeItem> getItemLastParaMap(Element root) {
        /*
        XML:路径：BasicItem为根节点
        BasicItem/Measurands/List/KeyValuePairOfMeasurandKeyMeasurandYxVEEYjo/value/MeasurandsGroup/Name
									/LowLimitFail
									/HighLimitFail
									/NominalValue
         */
        Map<String, Ny20RecipeItem> itemMap = new HashMap<>();
        String basicItem = "";
        if ("BasicItem".equals(root.getName())) {
            basicItem = root.elementText("Name");
        }
        Element measurandsOfRoot = root.element("Measurands");
        Element listOfRoot = measurandsOfRoot.element("List");
        List<Element> keyValueOfRoots = listOfRoot.elements("KeyValuePairOfMeasurandKeyMeasurandYxVEEYjo");
        if (keyValueOfRoots.size() > 0) {
            for (Element keyValueOfRoot : keyValueOfRoots) {
                Ny20RecipeItem ny20RecipeItem = new Ny20RecipeItem();
                if (keyValueOfRoot.element("value").element("MeasurandsGroup") == null) {
                    continue;
                }
//                System.out.println(keyValueOfRoot.element("value").attribute("Id").getText());
                String name = keyValueOfRoot.element("value").element("MeasurandsGroup").elementText("Name");
                String nominalValue = keyValueOfRoot.element("value").elementText("NominalValue");
                /**
                 * LowLimitFail，HighLimitFail有些包含在Tolerances中
                 */
                Element tolerances = keyValueOfRoot.element("value").element("Tolerances");
                String lowLimitFail = "";
                String highLimitFail = "";
                if (tolerances != null) {
                    lowLimitFail = tolerances.elementText("LowLimitFail");
                    highLimitFail = tolerances.elementText("HighLimitFail");
                } else {
                    lowLimitFail = keyValueOfRoot.element("value").elementText("LowLimitFail");
                    highLimitFail = keyValueOfRoot.element("value").elementText("HighLimitFail");
                }

                //将basicItem的名称修改为black/white(黑白两种模式，但是名称可能有变)
                if (basicItem.contains("black") || basicItem.contains("Black")) {
                    basicItem = "Black";
                } else if (basicItem.contains("white") || basicItem.contains("White")) {
                    basicItem = "White";
                }
                ny20RecipeItem.setName(name + "_" + basicItem);
                ny20RecipeItem.setNominalValue(nominalValue);
                ny20RecipeItem.setLowLimitFail(lowLimitFail);
                ny20RecipeItem.setHighLimitFail(highLimitFail);
                ny20RecipeItem.setBasicItem(basicItem);
                itemMap.put(name + "_" + basicItem, ny20RecipeItem);
            }
        }

//        System.out.println(itemMap.size());
        return itemMap;
    }

    /**
     * 解压并读取其中handler及vision文件
     *
     * @param filePath
     * @param recipeName
     * @param deviceType
     * @return
     */
    public static List<RecipePara> transferRcpFromDB2(String filePath, String recipeName, String deviceType) {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParas = new ArrayList<>();
        File file = null;
        try {
            //将文件名称中的所有空格都替换为带有双引号的空格
            //filePath = filePath.replaceAll(" ", "\" \"");
//        String command = "cmd /c start /B D:\\jcauto\\7z.exe x " + filePath + " -oD:\\RECIPE\\RESOLVE\\" + recipeName + " -aoa ";
//        System.out.println(command);
//        try {
//            Runtime.getRuntime().exec(command);
//        } catch (IOException ex) {
////            Logger.getLogger(MainTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//暂做解压处理
            File tempFile = new File(filePath + "TEMP");
            //删除Temp文件
            if (tempFile.exists()) {
                FileUtil.deleteDirectory(tempFile);
            }

            un7zM(filePath, filePath + "TEMP", "");
            //recipeName与文件名不一致
            String recipeNameTop = new File(filePath + "TEMP\\Station1 Top\\").list()[0].replace(".7z", "");
            String recipeNameTape = new File(filePath + "TEMP\\Station3 Tape\\").list()[0].replace(".7z", "");

            if (new File(filePath + "TEMP\\Station1 Top\\" + recipeNameTop + ".7z").exists()) {
                un7zM(filePath + "TEMP\\Station1 Top\\" + recipeNameTop + ".7z", filePath + "TEMP\\Station1 Top\\" + recipeNameTop, "");
            }
            if (new File(filePath + "TEMP\\Station3 Tape\\" + recipeNameTape + ".7z").exists()) {
                un7zM(filePath + "TEMP\\Station3 Tape\\" + recipeNameTape + ".7z", filePath + "TEMP\\Station3 Tape\\" + recipeNameTape, "");
            }

        

            file = new File(filePath + "TEMP");
            /*
            解析HandlerREC文件
            */
            if (file.exists()) {
                File[] files = file.listFiles();
                for (File f : files) {
                    if (f.getName().contains("REC")) {
                        FileReader reader = new FileReader(f);
                        BufferedReader br = new BufferedReader(reader);
                        String line = "";
                        String paraName = "";
                        String paraValue = "";
                        String defaultValue = "";
                        while ((line = br.readLine()) != null) {
                            RecipePara recipePara = new RecipePara();
                            //System.out.println(line);
                            if (line.contains("//")) {
                                continue;
                            }
                            if (line.contains(",")) {
                                String[] paras = line.replace(";", "").split("\\,");
                                if (paras.length == 6) {
                                    paraName = paras[2] + "_" + paras[1] + "_" + paras[0];
                                    paraValue = paras[4];
                                    defaultValue = paras[5];
                                    recipePara.setParaName(paraName);
                                    recipePara.setSetValue(paraValue);
                                    recipePara.setDefValue(defaultValue);
                                    recipeParas.add(recipePara);
                                }
                                if (paras.length == 5) {
                                    paraName = paras[2] + "_" + paras[1] + "_" + paras[0];
                                    paraValue = paras[4];
                                    recipePara.setParaName(paraName);
                                    recipePara.setSetValue(paraValue);
                                    recipeParas.add(recipePara);
                                }
                            }
                        }
                        br.close();
                    };
                }
            }

            for (RecipePara recipePara : recipeParas) {
                System.out.println(recipePara.getParaName());
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    if (recipePara.getParaName().equals(recipeTemplate.getParaName())) {
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                    }
                }
            }
            /*
            解析visionXML文件
            */
            File devcieMoldFile1 = new File(filePath + "TEMP\\Station1 Top\\" + recipeNameTop + "\\Data\\DeviceModel.xml");
            if (devcieMoldFile1.exists()) {
                Map<String, Ny20RecipeItem> mapTop = getParaFromVision(devcieMoldFile1);
//                for (String s : mapTop.keySet()) {
//                    System.out.println(s + "_Top");
//                }
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    Ny20RecipeItem ny20RecipeItem = null;
                    // System.out.println(recipeTemplate.getParaName().replace("_Top", ""));
                    if ((ny20RecipeItem = mapTop.get(recipeTemplate.getParaName().replace("_Top", ""))) != null) {
                        RecipePara recipePara = new RecipePara();
                        recipePara.setParaName(recipeTemplate.getParaName());
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        if (recipeTemplate.getParaName().contains("Body Length") || recipeTemplate.getParaName().contains("Body Width")) {
                            recipePara.setSetValue(ny20RecipeItem.getNominalValue());
                        } else {
                            recipePara.setSetValue(ny20RecipeItem.getLowLimitFail());
                        }
                        recipeParas.add(recipePara);
                    }
                }
            }
            File devcieMoldFile2 = new File(filePath + "TEMP\\Station3 Tape\\" + recipeNameTape + "\\Data\\DeviceModel.xml");
            if (devcieMoldFile2.exists()) {
                Map<String, Ny20RecipeItem> mapTape = getParaFromVision(devcieMoldFile2);
//                for (String s : mapTape.keySet()) {
//                    System.out.println(s + "_Tape");
//                }
                for (RecipeTemplate recipeTemplate : recipeTemplates) {
                    //System.out.println(recipeTemplate.getParaName().replace("_Tape", ""));
                    Ny20RecipeItem ny20RecipeItem = null;
                    if ((ny20RecipeItem = mapTape.get(recipeTemplate.getParaName().replace("_Tape", ""))) != null) {
                        RecipePara recipePara = new RecipePara();
                        recipePara.setParaName(recipeTemplate.getParaName());
                        recipePara.setParaCode(recipeTemplate.getParaCode());
                        if (recipeTemplate.getParaName().contains("Body Length") || recipeTemplate.getParaName().contains("Body Width")) {
                            recipePara.setSetValue(ny20RecipeItem.getNominalValue());
                        } else {
                            recipePara.setSetValue(ny20RecipeItem.getLowLimitFail());
                        }
                        recipeParas.add(recipePara);
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            logger.info("解析文件出错" + ex);
        } catch (IOException ex) {
            logger.info("解析文件出错" + ex);
        } catch (DocumentException ex) {
            ex.printStackTrace();
            logger.info("解析XML文件出错" + ex);
        } catch (Exception ex) {
           logger.info("解压文件出错" + ex);
        } finally {
            //删除Temp文件
            if (file.exists()) {
                FileUtil.deleteDirectory(file);
            }
        }

        return recipeParas;
    }

    /**
     * @param file7zPath(7z文件路径)
     * @param outPutPath(解压路径)
     * @param passWord(文件密码.没有可随便写,或空)
     * @return
     * @throws Exception
     * @Description (解压7z)
     */
    public static int un7zM(String file7zPath, final String outPutPath, String passWord) throws Exception {
        IInArchive archive;
        RandomAccessFile randomAccessFile;
        randomAccessFile = new RandomAccessFile(file7zPath, "r");
        archive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile), passWord);
        int numberOfItems = archive.getNumberOfItems();
        ISimpleInArchive simpleInArchive = archive.getSimpleInterface();
        for (final ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
            if (item.getPath().split("\\\\").length > 2 || (item.getPath().contains("Data") && !item.getPath().contains("DeviceModel"))) {
                //System.out.println(String.format(" %10s | %s", item.getPath().split("\\\\").length, item.getPath()));
                continue;
            }
            final int[] hash = new int[]{0};
            if (!item.isFolder()) {
                ExtractOperationResult result;
                final long[] sizeArray = new long[1];
                result = item.extractSlow(new ISequentialOutStream() {
                    public int write(byte[] data) throws SevenZipException {
                        try {
                            //判断压缩包内的文件是否存在
                            String parentFilePath = "";
                            if (item.getPath().contains("\\") && item.getPath().split("\\\\").length < 3) {
                                parentFilePath = outPutPath + File.separator + item.getPath().substring(0, item.getPath().lastIndexOf(File.separator));
                                if (!new File(parentFilePath).exists()) {
                                    new File(parentFilePath).mkdirs();
                                }
                            }
                            if (item.getPath().split("\\\\").length < 3) {
                                //System.out.println("写入" + item.getPath());
                                FileOutputStream fos = new FileOutputStream(new File(outPutPath + File.separator + item.getPath()), true);
                                IOUtils.write(data, fos);
                                fos.close();
                            }

//                            if (item.getPath().endsWith(".7z")) {
//                                // un7zM( outPutPath + File.separator + item.getPath(),outPutPath + File.separator + item.getPath().replace(".7z", ""),"");
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        hash[0] ^= Arrays.hashCode(data); // Consume data
                        sizeArray[0] += data.length;
                        return data.length; // Return amount of consumed
                    }
                }, passWord);
                if (result == ExtractOperationResult.OK) {
                    System.out.println(String.format("%9X | %10s | %s", hash[0], sizeArray[0], item.getPath()));
                } else {
                    System.out.printf("Error extracting item: " + result);
                }
            }
        }
        archive.close();
        randomAccessFile.close();

        return numberOfItems;
    }

    private static Map<String, Ny20RecipeItem> getParaFromVision(File file) throws DocumentException {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(file);
        Element root = document.getRootElement();
        return getItemParaMap(root);
    }

    public static void main(String args[]) {
//        String filePath = "C:\\Users\\SunTao\\Desktop\\长电\\NY20\\AGD_LGA4X6.8_V0";
        String filePath = "C:\\Users\\leo\\Documents\\WeChat Files\\Spurs-Leo\\FileStorage\\File\\2019-04\\AAI-T-LGA-3X4.2-3000-5633_V0.txt";
        // transferRcpFromDB(filePath, filePath);
        List<RecipePara> recipeParas = transferRcpFromDB2(filePath, "AAI-T-LGA-3X4.2-3000-5633_V0", "IsmecaNY20");
        for (RecipePara r : recipeParas) {
            System.out.println(r.getParaName() + ":" + r.getSetValue());
        }
        //System.out.println("Blob Length_White_Top".replace("_Top", ""));

    }

}
