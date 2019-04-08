/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.secsLayer.resolver;


import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author SunTao
 */
public class LaserRelinkUtil {

    private static String doUncompressFile(String inFileName) {

        String outFileName = "";

        try {

//            if (!getExtension(inFileName).equalsIgnoreCase("gz")) {
//                System.err.println("File name must have extension of \".gz\"");
//                System.exit(1);
//            }
            System.out.println("Opening the compressed file.");
            GZIPInputStream in = null;
            File inFile = null;
            try {
                inFile = new File(inFileName);
                in = new GZIPInputStream(new FileInputStream(inFile));
            } catch (FileNotFoundException e) {
                System.err.println("File not found. " + inFileName);
                System.exit(1);
            }

            System.out.println("Open the output file.");
//            String outFileName = "";
            String fname = "";
            int i = inFileName.lastIndexOf('.');
            if (i > 0 && i < inFileName.length() - 1) {
                fname = inFileName.substring(0, i);
            }
            outFileName = fname;
            //System.out.println(outFileName);
//            String outFileName=inFileName+"copy"+File.separator+inFile.getName();
//            System.out.println(outFileName);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Could not write to file. " + outFileName);
                System.exit(1);
            }

            System.out.println("Transfering bytes from compressed file to the output file.");
            byte[] buf = new byte[1024];
            int len;
            //BufferedReader br=new BufferedReader(in);

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
//                String s=new String(buf,0,len);
//                System.out.println(s);
            }

            System.out.println("Closing the file and stream");
            in.close();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return outFileName;
    }
    static String winrarPath = "C:\\Program Files (x86)\\WinRAR\\WinRAR.exe";

//    public static void ForLaserRelink(String filePath) {
//        ZipFile zipFile = null;
//        ZipInputStream zis = null;
//        InputStream is = null;
//        FileOutputStream fos = null;
//        //FileInputStream fis = null;
//        try {
//            //解压到新文件中
//            String newFileName = filePath + "copy";
//            File file = new File(filePath + "copy");
//            System.out.println(file.getName());
//            if (!file.exists()) {
//                file.mkdir();
//            }
//            File files = new File(filePath);
//            zipFile = new ZipFile(files);
//            zis = new ZipInputStream(new FileInputStream(filePath));
//            ZipEntry ze = null;
//            while ((ze = zis.getNextEntry()) != null) {
//                System.out.println(ze.getName());
//                //File Zefile = new File(filePath, ze.getName());
//                //fis = new FileInputStream(Zefile);
//                is = zipFile.getInputStream(ze);
//                fos = new FileOutputStream(newFileName + File.separator + ze.getName());
//                byte[] buff = new byte[1024 * 1024];
//                int len = 0;
//                while ((len = is.read(buff)) != -1) {
//                    fos.write(buff, 0, len);
//                }
//                //zis.closeEntry();
//            }
//
//        } catch (IOException ex) {
//            Logger.getLogger(LaserRelinkUtil.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                zis.close();
//                fos.close();
////                if (fis != null) {
////                    fis.close();
////                }
//            } catch (IOException ex) {
//                Logger.getLogger(LaserRelinkUtil.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
//    public static LaserEntity ForLaserRelinkxml(String filePath) {
//        LaserEntity laserEntity = null;
//        try {
//            SAXReader saxr = new SAXReader();
//            Document doc = saxr.read(new File(filePath));
//            //读取根结点
//            Element root = doc.getRootElement();
//            //System.out.println(root.getName()
//            //读取根节点下的元素
//            laserEntity = new LaserEntity();
//            List<Element> elements = new ArrayList<>();
//            elements = root.elements();
//            SqlSession sqlSession = MybatisSqlSession.getSqlSession();
//            RecipeService recipeService = new RecipeService(sqlSession);
//            List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("LaserReink", "RecipePara");
//            for (Element element : elements) {
////                  <LaserFrequency>100000</LaserFrequency>
////    <LaserPower>0.5</LaserPower>
////    <MarkSpeed>100</MarkSpeed>
////    <MarkingSizeRatio>0.5</MarkingSizeRatio>
//                //System.out.println(element);
//                if ("RecipeSetting".equals(element.getName())) {
//
//                    laserEntity.setLaserFrequency(Double.parseDouble(element.element("LaserFrequency").getText()));
//                    laserEntity.setLaserPower(Double.parseDouble(element.element("LaserPower").getText()));
//                    laserEntity.setMarkSpeed(Double.parseDouble(element.element("MarkSpeed").getText()));
//
//                    List<LaserEntity> laserEntitys = new ArrayList<LaserEntity>();
//                    laserEntitys.add(laserEntity);
//                    Iterator it = laserEntitys.iterator();
//                    while (it.hasNext()) {
//                        System.out.println(it.next());
//                    }
//
//                    //laserEntitys.add(LaserFrequency);
//                    //laserEntitys.add(LaserPower);
//                    //laserEntitys.add(MarkSpeed);
//
//                    //System.out.println(laserEntity);
//                }
//                //System.out.println(element.getName());
//
//            }
//
//        } catch (DocumentException ex) {
//            Logger.getLogger(LaserRelinkUtil.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        return laserEntity;
//    }
    public static List<RecipePara> ForLaserRelinkxml2(String filePath) {

        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode("HylaxLaserReink", "RecipePara");
        List<RecipePara> recipeParas = new ArrayList<>();
        try {
            SAXReader saxr = new SAXReader();
            Document doc = saxr.read(new File(filePath));
            //读取根结点
            Element root = doc.getRootElement();
            //System.out.println(root.getName()
            //读取根节点下的元素
            List<Element> elements = new ArrayList<>();
            elements = root.elements();
            List<Attribute> attributes = new ArrayList<>();
            attributes = root.attributes();
            for (Element attribute : elements) {
                // System.out.println("attribute:" + attribute.getName());
                if ("RecipeSetting".equals(attribute.getName())) {
                    //attributeValue(paraname);//attribute.getValue();
                    for (RecipeTemplate recipeTemplate : recipeTemplates) {
                        String paracode = recipeTemplate.getParaCode();
                        String paraname = recipeTemplate.getParaName();
                        String paravalue = attribute.element(paraname).getText();
                        RecipePara recipePara = new RecipePara();
                        //TODO添加完整属性
                        recipePara.setParaCode(paracode);
                        recipePara.setParaName(paraname);
                        recipePara.setSetValue(paravalue);
                        recipeParas.add(recipePara);
                        System.out.println("paraName:" + paraname);
                        System.out.println("paraValue:" + paravalue);
                    }

                }
            }

        } catch (DocumentException ex) {
            ex.printStackTrace();
        }finally{
            sqlSession.close();
        }
        return recipeParas;
    }

    public void elementXml(Element e) {
    }

    public static String unzip(String filePath) {
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {

            File file = new File(filePath + "1\\");
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (File f : files) {
                        f.delete();
                    }
                }
                file.delete();
                System.out.println("删除" + file.getAbsolutePath() + " " + file.delete());
            }

            String s = winrarPath + " x " + filePath + " " + filePath + "1\\";
            p = r.exec(s);

        } catch (Exception e) {
            System.out.println("错误:" + e.getMessage());
            e.printStackTrace();
        }
        pasue();
        return filePath + "1";
    }

//    public LaserEntity getLaserEntity(String filePath) {
//        LaserEntity laserEntity = new LaserEntity();
//        String newFilePath = unzip(filePath);
//        System.out.println(newFilePath);
//        File file = new File(newFilePath + File.separator);
//        if (file.exists()) {
//            File[] files = file.listFiles();
//            for (File filea : files) {
//                String path = filea.getAbsolutePath();
//                System.out.println(path);
//                laserEntity = ForLaserRelinkxml(path);
//            }
////            String[] strs=file.list();
////            System.out.println(strs);
////        for (String str : strs) {
////            System.out.println(str);
////
////            //laserEntity = ForLaserRelinkxml(newFilePath + File.separator + str);
////            System.out.println(newFilePath + File.separator + str);
////        }
//        } else {
//            System.out.println(newFilePath + "不存在");
//        }
//
//        return laserEntity;
//    }
    public static List<RecipePara> transferHylaxLaserReinkRcp(String filePath) {
        List<RecipePara> recipeParas = new ArrayList<>();


//        String newFilePath = unzip(filePath);
        String newFilePath = doUncompressFile(filePath);

        System.out.println(newFilePath);
        File file = new File(newFilePath + File.separator);
        if (file.exists()) {


            String path = file.getAbsolutePath();
            recipeParas = ForLaserRelinkxml2(path);
            file.delete();

//            File[] files = file.listFiles();
//            for (File filea : files) {
//                String path = filea.getAbsolutePath();
//                System.out.println(path);
//                recipeParas = ForLaserRelinkxml2(path);
//            }


//            String[] strs=file.list();
//            System.out.println(strs);
//        for (String str : strs) {
//            System.out.println(str);
//
//            //laserEntity = ForLaserRelinkxml(newFilePath + File.separator + str);
//            System.out.println(newFilePath + File.separator + str);
//        }
        } else {
            System.out.println(newFilePath + "不存在");
        }

        return recipeParas;
    }

    public static void pasue() {
        try {
            Thread thread = Thread.currentThread();
            thread.sleep(3000);//暂停1.5秒后程序继续执行
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        //LaserEntity laserEntity = new LaserEntity();
        //LaserRelinkUtil.ForLaserRelinkxml("C:\\recipe\\laser\\12-gk7101s-90.rcp1\\12-gk7101s-90");
        //laserEntity = new LaserRelinkUtil().getLaserEntity("D:\\8Inch Dummy with Small Chip test (2).rcp");
//        transferHylaxLaserReinkRcp("D:\\8-120824CB-D175-150-90-ST9.rcp");
//        transferHylaxLaserReinkRcp("D:\\8-CG60173A-5104-25-200-270.rcp_V0.txt");
        transferHylaxLaserReinkRcp("D:\\12-AM10250E-5104T-25-150-90_V2");

        //         File file = new File("C:\\recipe\\laser\\8-120824CB-D175-150-90-ST9.rcp1\\");
//       String[] strs=file.list();
//        for (String str : strs) {
//            System.out.println(str);
//
//            //laserEntity = ForLaserRelinkxml(newFilePath + File.separator + str);
//            System.out.println("C:\\recipe\\laser\\8-120824CB-D175-150-90-ST9.rcp1\\" + File.separator + str);
//        }
    }
}
